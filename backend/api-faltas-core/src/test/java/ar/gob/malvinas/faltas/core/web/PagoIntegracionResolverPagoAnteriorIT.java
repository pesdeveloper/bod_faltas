package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.PagoEconomicoService;
import ar.gob.malvinas.faltas.core.domain.enums.ClasificacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT de contexto Spring real para POST /api/faltas/pagos/resolver-pago-anterior:
 * seguridad (401), validacion de body (400), recursos inexistentes (404),
 * precondiciones de negocio (422), conflicto de reintento incompatible (409)
 * e idempotencia/exito (200), bajo el modelo R1 de movimiento unico. Ver
 * backend/api-faltas-core/docs/spec-as-source/05-api-core-endpoints.md.
 *
 * Spring Security activo. No desactivar. Cada test siembra sus propios datos
 * usando actaRepo.nextId()/obligacionRepo.nextId() para no colisionar con
 * datos de otros tests que comparten el mismo contexto Spring.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT: POST /api/faltas/pagos/resolver-pago-anterior")
class PagoIntegracionResolverPagoAnteriorIT {

    private static final String URL = "/api/faltas/pagos/resolver-pago-anterior";
    private static final String URL_NOTIFICAR_MOVIMIENTO = "/api/faltas/pagos/notificar-movimiento";
    private static final String ACTOR_SUB = "usuario-it-resolver-pago-anterior";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ActaRepository actaRepo;

    @Autowired
    private ObligacionPagoRepository obligacionRepo;

    @Autowired
    private PagoEconomicoService pagoEconomicoService;

    @Autowired
    private ActaEventoRepository eventoRepo;

    // -----------------------------------------------------------------
    // Seguridad (401)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("01. sin Bearer -> 401")
    void sinBearer_responde_401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(1L, 1L, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("02. con JWT firma incorrecta -> 401")
    void conFirmaIncorrecta_responde_401() throws Exception {
        String token = JwtTestSupport.tokenFirmaIncorrecta(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(1L, 1L, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("03. con JWT alg=none -> 401")
    void conAlgNone_responde_401() throws Exception {
        String token = JwtTestSupport.tokenAlgNone(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(1L, 1L, null)))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------
    // Validacion de body (400)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("04. JWT valido + body ausente -> 400")
    void conJwtValido_bodyAusente_responde_400() throws Exception {
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("05. JWT valido + actaId ausente -> 400")
    void conJwtValido_actaIdAusente_responde_400() throws Exception {
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"movimientoPagoId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("06. JWT valido + movimientoPagoId ausente -> 400")
    void conJwtValido_movimientoPagoIdAusente_responde_400() throws Exception {
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actaId": 1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------
    // Recursos inexistentes (404)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("07. acta inexistente -> 404")
    void actaInexistente_responde_404() throws Exception {
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(999_999_001L, 1L, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("08. movimiento de pago inexistente -> 404")
    void movimientoInexistente_responde_404() throws Exception {
        Long actaId = sembrarActaConObligacionVigente(new BigDecimal("1000")).getId();
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(actaId, 999_999_002L, null)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------
    // Precondicion de negocio (422)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("09. movimiento no clasificado como OBLIGACION_ANTERIOR -> 422")
    void movimientoNoClasificado_responde_422() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaPagoMovimiento normal = notificarPagoNormal(vigente.getId(), new BigDecimal("100"), "IT-NORMAL-" + vigente.getId());

        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), normal.getId(), null)))
                .andExpect(status().isUnprocessableEntity());
    }

    // -----------------------------------------------------------------
    // Exito (200), idempotencia y conflicto (409)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("10. exito: aplica el pago anterior contra la vigente y devuelve el movimiento aplicado")
    void exito_responde_200_conMovimientoAplicado() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaObligacionPago anterior = sembrarObligacionAnterior(vigente.getActaId(), vigente.getPersonaId(), new BigDecimal("500"));
        FalActaPagoMovimiento pagant = notificarPagoNormal(anterior.getId(), new BigDecimal("400"), "IT-PAGANT-" + anterior.getId());

        String token = JwtTestSupport.token(ACTOR_SUB);
        MvcResult result = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "resolucion IT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.obligacionOrigenId").value(anterior.getId()))
                .andExpect(jsonPath("$.obligacionAplicadaId").value(vigente.getId()))
                .andExpect(jsonPath("$.movimientoAplicado.clasificacionPago").value("NORMAL"))
                .andExpect(jsonPath("$.movimientoAplicado.movimientoOrigenId").value(pagant.getId()))
                .andReturn();

        Map<String, Object> raiz = leerRespuesta(result);
        assertThat(new BigDecimal(raiz.get("importeAplicado").toString())).isEqualByComparingTo("400.00");
        assertThat(new BigDecimal(raiz.get("saldoResultante").toString())).isEqualByComparingTo("600.00");
        assertThat(new BigDecimal(raiz.get("importeExcedente").toString())).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("11. reintento compatible (mismo motivo) -> 200 con el mismo movimiento aplicado")
    void reintentoCompatible_responde_200_conMismoResultado() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaObligacionPago anterior = sembrarObligacionAnterior(vigente.getActaId(), vigente.getPersonaId(), new BigDecimal("500"));
        FalActaPagoMovimiento pagant = notificarPagoNormal(anterior.getId(), new BigDecimal("300"), "IT-PAGANT-RETRY-" + anterior.getId());

        String token = JwtTestSupport.token(ACTOR_SUB);
        MvcResult primero = mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "motivo-retry")))
                .andExpect(status().isOk())
                .andReturn();
        Long idMovimientoAplicado = extraerLong(primero, "movimientoAplicado.id");

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "motivo-retry")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movimientoAplicado.id").value(idMovimientoAplicado));
    }

    @Test
    @DisplayName("12. reintento con motivo distinto -> 409")
    void reintentoIncompatiblePorMotivo_responde_409() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaObligacionPago anterior = sembrarObligacionAnterior(vigente.getActaId(), vigente.getPersonaId(), new BigDecimal("500"));
        FalActaPagoMovimiento pagant = notificarPagoNormal(anterior.getId(), new BigDecimal("300"), "IT-PAGANT-CONFLICT-" + anterior.getId());

        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "motivo-A")))
                .andExpect(status().isOk());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "motivo-B")))
                .andExpect(status().isConflict());
    }

    // -----------------------------------------------------------------
    // R2-07: actor JWT en movimiento/resultado/evento; sin cambios administrativos
    // -----------------------------------------------------------------

    @Test
    @DisplayName("13. tras el exito, movimientoAplicado.idUserAlta, resultado.actor y PAGRES.idUserEvt coinciden con el sub del JWT")
    void exito_actorCoincideConSubJwtEnMovimientoResultadoYEvento() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaObligacionPago anterior = sembrarObligacionAnterior(vigente.getActaId(), vigente.getPersonaId(), new BigDecimal("500"));
        FalActaPagoMovimiento pagant = notificarPagoNormal(anterior.getId(), new BigDecimal("300"), "IT-PAGANT-ACTOR-" + anterior.getId());

        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "resolucion actor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movimientoAplicado.idUserAlta").value(ACTOR_SUB))
                .andExpect(jsonPath("$.actor").value(ACTOR_SUB));

        FalActaEvento pagres = eventoRepo.buscarPorActa(vigente.getActaId()).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES
                        && ("PAGRES-" + pagant.getId()).equals(e.correlacionId()))
                .findFirst().orElseThrow();
        assertThat(pagres.idUserEvt())
                .as("el idUserEvt del evento PAGRES debe coincidir con el sub del JWT autenticado")
                .isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("14. tras resolver, los campos administrativos del acta (bloqueActual/situacionAdministrativa/resultadoFinal) permanecen sin cambios")
    void exito_noAlteraCamposAdministrativosDelActa() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        FalActaObligacionPago anterior = sembrarObligacionAnterior(vigente.getActaId(), vigente.getPersonaId(), new BigDecimal("500"));
        FalActaPagoMovimiento pagant = notificarPagoNormal(anterior.getId(), new BigDecimal("300"), "IT-PAGANT-ADMIN-" + anterior.getId());

        FalActa actaAntes = actaRepo.buscarPorId(vigente.getActaId()).orElseThrow();
        var bloqueAntes = actaAntes.getBloqueActual();
        var situacionAntes = actaAntes.getSituacionAdministrativa();
        var resultadoAntes = actaAntes.getResultadoFinal();

        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson(vigente.getActaId(), pagant.getId(), "resolucion sin efecto administrativo")))
                .andExpect(status().isOk());

        FalActa actaDespues = actaRepo.buscarPorId(vigente.getActaId()).orElseThrow();
        assertThat(actaDespues.getBloqueActual()).isEqualTo(bloqueAntes);
        assertThat(actaDespues.getSituacionAdministrativa()).isEqualTo(situacionAntes);
        assertThat(actaDespues.getResultadoFinal()).isEqualTo(resultadoAntes);
    }

    // -----------------------------------------------------------------
    // R2-07: recibo obligatorio/parcial/completo via /notificar-movimiento
    // -----------------------------------------------------------------

    @Test
    @DisplayName("15. PAGO_CONFIRMADO sin cmtePG/prefPG/nroPG -> 422")
    void notificarMovimiento_pagoConfirmadoSinTerna_responde_422() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL_NOTIFICAR_MOVIMIENTO)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificarMovimientoBodyJson(vigente.getId(), new BigDecimal("100"),
                                "IT-SINTERNA-" + vigente.getId(), null, null, null)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("16. PAGO_CONFIRMADO con terna parcial (cmtePG sin prefPG/nroPG) -> 422")
    void notificarMovimiento_pagoConfirmadoTernaParcial_responde_422() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL_NOTIFICAR_MOVIMIENTO)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificarMovimientoBodyJson(vigente.getId(), new BigDecimal("100"),
                                "IT-PARCIAL-" + vigente.getId(), "PA", null, null)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("17. PAGO_CONFIRMADO con terna completa -> 200")
    void notificarMovimiento_pagoConfirmadoTernaCompleta_responde_200() throws Exception {
        FalActaObligacionPago vigente = sembrarActaConObligacionVigente(new BigDecimal("1000"));
        String token = JwtTestSupport.token(ACTOR_SUB);
        mockMvc.perform(post(URL_NOTIFICAR_MOVIMIENTO)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notificarMovimientoBodyJson(vigente.getId(), new BigDecimal("100"),
                                "IT-COMPLETA-" + vigente.getId(), "CO", (short) 1, 777)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cmtePG").value("CO"));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private String bodyJson(Long actaId, Long movimientoPagoId, String motivo) throws Exception {
        return mapper.writeValueAsString(new java.util.LinkedHashMap<>(Map.of(
                "actaId", actaId,
                "movimientoPagoId", movimientoPagoId,
                "motivo", motivo != null ? motivo : "")));
    }

    /**
     * Cuerpo JSON de NotificarMovimientoPagoRequest para un PAGO_CONFIRMADO
     * contra la obligacion dada, con cmtePG/prefPG/nroPG opcionales (R2-02:
     * la ausencia total o parcial de la terna es lo que se quiere ejercitar).
     */
    private String notificarMovimientoBodyJson(
            Long obligacionPagoId, BigDecimal importe, String ref, String cmtePG, Short prefPG, Integer nroPG) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("obligacionPagoId", obligacionPagoId);
        body.put("tipoMovimiento", "PAGO_CONFIRMADO");
        body.put("origenMovimiento", "INGRESOS");
        body.put("origenConfirmacion", "INGRESOS");
        body.put("clasificacionPago", "NORMAL");
        body.put("importeCapital", importe);
        body.put("importeTotal", importe);
        body.put("cmtePG", cmtePG);
        body.put("prefPG", prefPG);
        body.put("nroPG", nroPG);
        body.put("referenciaExterna", ref);
        return mapper.writeValueAsString(body);
    }

    private FalActaObligacionPago sembrarActaConObligacionVigente(BigDecimal monto) {
        Long actaId = actaRepo.nextId();
        Long personaId = 500_000L + actaId;
        LocalDateTime ahora = LocalDateTime.now();
        FalActa acta = new FalActa(actaId, "UUID-IT-" + actaId, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.now(), ahora, "Domicilio IT " + actaId, null,
                null, null, ResultadoFirmaInfractor.FIRMADA, personaId, ahora, "SYS");
        actaRepo.guardar(acta);

        Long obligacionId = obligacionRepo.nextId();
        FalActaObligacionPago vigente = new FalActaObligacionPago(
                obligacionId, actaId, personaId, TipoObligacionPago.PAGO_VOLUNTARIO, monto, ahora, "SYS", ahora, "SYS");
        return obligacionRepo.save(vigente);
    }

    private FalActaObligacionPago sembrarObligacionAnterior(Long actaId, Long personaId, BigDecimal monto) {
        Long obligacionId = obligacionRepo.nextId();
        LocalDateTime ahora = LocalDateTime.now();
        FalActaObligacionPago anterior = new FalActaObligacionPago(
                obligacionId, actaId, personaId, TipoObligacionPago.PAGO_VOLUNTARIO, monto, ahora, "SYS", ahora, "SYS");
        anterior.setSiVigente(false);
        return obligacionRepo.save(anterior);
    }

    /** Secuencia deterministica para sintetizar cmtePG/prefPG/nroPG unico entre tests (R2-02: recibo obligatorio). */
    private static final java.util.concurrent.atomic.AtomicInteger RECIBO_SEQ =
            new java.util.concurrent.atomic.AtomicInteger(1);

    /**
     * Notifica un PAGO_CONFIRMADO contra la obligacion dada (vigente o no).
     * Sintetiza una terna de recibo cmtePG/prefPG/nroPG unica: desde R2-02,
     * PagoMovimientoService.registrar exige terna completa para todo
     * PAGO_CONFIRMADO original.
     */
    private FalActaPagoMovimiento notificarPagoNormal(Long obligacionId, BigDecimal importe, String ref) {
        Integer nroPG = RECIBO_SEQ.getAndIncrement();
        return pagoEconomicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(
                obligacionId, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, OrigenConfirmacion.INGRESOS, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, "IT", (short) 1, nroPG,
                null, null, null, null, ref, LocalDateTime.now(), "SYS"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leerRespuesta(MvcResult result) throws Exception {
        return mapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    private Long extraerLong(MvcResult result, String jsonPath) throws Exception {
        Object valor = leerRespuesta(result);
        for (String parte : jsonPath.split("\\.")) {
            assertThat(valor).isInstanceOf(Map.class);
            valor = ((Map<?, ?>) valor).get(parte);
        }
        assertThat(valor).isNotNull();
        return ((Number) valor).longValue();
    }
}
