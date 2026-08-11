package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración con contexto Spring real.
 * Cubre el flujo HTTP completo: labrar → firma → notificación positiva.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("IT: endpoints REST del flujo de faltas")
class FlujoCoreIT {

    private static final String ACTOR_SUB = "usuario-it-flujo-core";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    /**
     * GAP-017: el registro de notificacion positiva exige intentoId, pero ninguna
     * respuesta HTTP lo expone (el envio devuelve el id de la cabecera). El IT lo
     * resuelve consultando el puerto de dominio para no cortar la cobertura del
     * flujo; eso NO cierra el gap: queda abierto para decidir si algun contrato
     * publico debe exponer el identificador.
     */
    @Autowired
    NotificacionIntentoRepository notificacionIntentoRepository;

    @Autowired
    ar.gob.malvinas.faltas.core.application.service.TalonarioService talonarioService;

    @Autowired
    ar.gob.malvinas.faltas.core.repository.DependenciaRepository dependenciaRepository;

    /**
     * Wave 4 Parte 3C (DECISION-ACTA-TALONARIO-REQUIRED-01): {@code idDependencia="DEP-001"}
     * cae por {@code parseLongId} en 1L; sin talonario ACTA ELECTRONICO activo para esa
     * dependencia el alta rechaza completa (antes "0 activos" se toleraba). Este contexto usa
     * repositorios InMemory compartidos por todo el contexto Spring de este test (perfil por
     * defecto, sin {@code jdbc}): idempotente entre los 3 metodos de esta clase, guardado con
     * un flag estatico para no reintentar codigos ya usados si el contexto se reutiliza.
     */
    private static boolean talonarioSeedeado = false;

    @BeforeEach
    void asegurarTalonarioElectronicoDependenciaPorDefecto() {
        if (talonarioSeedeado) {
            return;
        }
        LocalDate vigenciaAmplia = LocalDate.of(2000, 1, 1);
        if (dependenciaRepository.findById(1L).isEmpty()) {
            dependenciaRepository.guardar(new ar.gob.malvinas.faltas.core.domain.model.FalDependencia(
                    1L, "DEPENDENCIA_FLUJOCOREIT_DEFAULT", FaltasClockTestSupport.FIXED.now(), "IT_SEED"));
            dependenciaRepository.guardarVersion(new ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion(
                    1L, 1, "DEPENDENCIA_FLUJOCOREIT_DEFAULT", null, null, null, vigenciaAmplia));
        }
        ar.gob.malvinas.faltas.core.support.TalonarioElectronicoITSeedSupport
                .seedTalonarioElectronicoActivo(talonarioService, 1L, (short) 1, vigenciaAmplia);
        talonarioSeedeado = true;
    }

    /** Todos los endpoints /api/faltas exigen Bearer (SecurityConfig). */
    private static String bearer() {
        return "Bearer " + JwtTestSupport.token(ACTOR_SUB);
    }

    @Test
    @DisplayName("Flujo HTTP completo: labrar (alta integral, nace en ENRI) â†’ generar doc â†’ firmar â†’ enviar â†’ positiva")
    void flujo_http_completo() throws Exception {
        // 1. Labrar acta
        String labrarBody = mapper.writeValueAsString(Map.of(
                "tipoActa", "TRANSITO",
                "idDependencia", "DEP-001",
                "idInspector", "INS-001",
                "fechaActa", FaltasClockTestSupport.FIXED.now().toLocalDate().toString(),
                "domicilioHecho", "Belgrano 200",
                "tipoDocumentoInfractor", "DNI",
                "infractorDocumento", "87654321",
                "resultadoFirmaInfractor", "SE_NIEGA_A_FIRMAR",
                "situacionIdentificacionInfractor", "IDENTIFICADO_COMPLETO",
                // Wave 4 Parte 3C (DECISION-ACTA-SATELITES-STRICT-01): tipoActa=TRANSITO exige
                // siempre el bloque 'transito' (sin opt-in). Bloque minimo, sin alcoholemia.
                "transito", Map.of(
                        "siRetLicencia", false,
                        "siRetVehiculo", false,
                        "siControlAlcoholemia", false)
        ));

        MvcResult labrarResult = mvc.perform(post("/api/faltas/actas/labrar")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(labrarBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("ACTLAB"))
                .andReturn();

        String idActa = extractField(labrarResult, "idActa");
        assertThat(idActa).isNotBlank();

        // 2. Verificar snapshot inicial: alta integral, el acta ya nace en ENRI con bandeja
        // ACTA_NUEVA (DECISION-ACTA-LIFECYCLE-SIN-CAPT-01). No hay captura previa que completar.
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("ACTA_NUEVA"))
                .andExpect(jsonPath("$.bloqueActual").value("ENRI"));

        // 3. Generar documento
        String docBody = mapper.writeValueAsString(Map.of(
                "tipoDocumento", TipoDocu.ACTA_INFRACCION.name(),
                "descripcion", "Acta de infraccion"
        ));

        MvcResult docResult = mvc.perform(post("/api/faltas/actas/{id}/documentos", idActa)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(docBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("DOCGEN"))
                .andReturn();

        String idDoc = extractField(docResult, "idEntidadAfectada");

        // 5. Verificar bandeja PENDIENTE_FIRMA
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_FIRMA"));

        // 6. Firmar documento
        String firmarBody = mapper.writeValueAsString(Map.of(
                "firmante", "Inspector Rodriguez",
                "tipoFirma", "DIGITAL"
        ));

        mvc.perform(post("/api/faltas/documentos/{id}/firmar", idDoc)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firmarBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("DOCFIR"));

        // 7. Verificar bandeja PENDIENTE_NOTIFICACION
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_NOTIFICACION"));

        // 8. Enviar notificación
        String notifBody = mapper.writeValueAsString(Map.of(
                "idDocumento", idDoc,
                "canal", "EMAIL",
                "destinoDigital", "infractor@ejemplo.com"
        ));

        MvcResult notifResult = mvc.perform(post("/api/faltas/actas/{id}/notificaciones/enviar", idActa)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notifBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoEvento").value("NOTENV"))
                .andReturn();

        String idNotif = extractField(notifResult, "idEntidadAfectada");

        // 9. Registrar notificación positiva.
        // El intentoId no viaja por HTTP (GAP-017): se obtiene del puerto de dominio.
        // El escenario hizo un unico envio, asi que la notificacion debe tener
        // exactamente un intento; se afirma esa unicidad en vez de asumir un orden.
        List<FalNotificacionIntento> intentos =
                notificacionIntentoRepository.buscarPorNotificacion(Long.valueOf(idNotif));
        assertThat(intentos)
                .as("el escenario genero un unico intento para la notificacion %s", idNotif)
                .hasSize(1);
        Long idIntento = intentos.get(0).getId();

        String positivaBody = mapper.writeValueAsString(Map.of("intentoId", idIntento));

        mvc.perform(post("/api/faltas/notificaciones/{id}/positiva", idNotif)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(positivaBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoEvento").value("NOTPOS"));

        // 10. Verificar estado final: ANAL
        mvc.perform(get("/api/faltas/actas/{id}/snapshot", idActa)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codBandeja").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.bloqueActual").value("ANAL"));

        // 11. Verificar timeline: un evento append-only por cada comando del flujo.
        // Los cinco tipos quedaron verificados individualmente en los pasos previos:
        // ACTLAB, DOCGEN, DOCFIR, NOTENV y NOTPOS. ACTCAP ya no existe
        // (DECISION-ACTA-LIFECYCLE-SIN-CAPT-01): el alta es integral, sin captura previa.
        mvc.perform(get("/api/faltas/actas/{id}/timeline", idActa)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @DisplayName("Error 422 al intentar notificar documento sin firma")
    void error_al_notificar_sin_firma() throws Exception {
        String labrarBody = mapper.writeValueAsString(Map.of(
                "tipoActa", "TRANSITO", "idDependencia", "DEP-001",
                "idInspector", "INS-001",
                "tipoDocumentoInfractor", "DNI", "infractorDocumento", "99999999",
                "resultadoFirmaInfractor", "SE_NIEGA_A_FIRMAR",
                "situacionIdentificacionInfractor", "IDENTIFICADO_COMPLETO",
                "transito", Map.of(
                        "siRetLicencia", false,
                        "siRetVehiculo", false,
                        "siControlAlcoholemia", false)
        ));
        MvcResult labrarResult = mvc.perform(post("/api/faltas/actas/labrar")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(labrarBody))
                .andReturn();
        String idActa = extractField(labrarResult, "idActa");

        mvc.perform(post("/api/faltas/actas/{id}/completar-captura", idActa)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        String docBody = mapper.writeValueAsString(Map.of(
                "tipoDocumento", "ACTA_INFRACCION", "descripcion", "test"));
        MvcResult docResult = mvc.perform(post("/api/faltas/actas/{id}/documentos", idActa)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(docBody)).andReturn();
        String idDoc = extractField(docResult, "idEntidadAfectada");

        String notifBody = mapper.writeValueAsString(Map.of(
                "idDocumento", idDoc, "canal", "EMAIL",
                "destinoDigital", "infractor@ejemplo.com"));
        mvc.perform(post("/api/faltas/actas/{id}/notificaciones/enviar", idActa)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(notifBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").exists());
    }

    @Test
    @DisplayName("Error 404 para acta inexistente")
    void error_404_acta_inexistente() throws Exception {
        // El id de acta es Long: un id inexistente pero numerico da 404.
        // Un id no numerico daria 400 por type mismatch, que es otro contrato.
        mvc.perform(get("/api/faltas/actas/{id}", 999_999_999L)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    private String extractField(MvcResult result, String field) throws Exception {
        String json = result.getResponse().getContentAsString();
        return mapper.readTree(json).get(field).asText();
    }
}

