package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resolutorios de cierre: bandeja operativa amplia, exclusiones.
 *
 * <p>Casos de bandeja: {@code ACTA-0024} (enriquecimiento / análisis; solo
 * {@code LIBERACION_RODADO} y {@code ENTREGA_DOCUMENTACION} — sin medida
 * preventiva), {@code ACTA-0022} (enriquecimiento; incluye ancla
 * {@code MEDIDA_PREVENTIVA} con justificación de campo — referencia para
 * circuito de levantamiento CFN), {@code ACTA-0020} (tras piezas: queda
 * en {@code PENDIENTE_FIRMA} con origen de medida), exclusiones 0017/0007/0008.
 * {@code ACTA-0021} (tránsito; solo rodado y documentación retenida — sin
 * medida preventiva; Tránsito puede tenerlas si están justificadas por
 * ordenanza/artículo, pero este mock no representa ese caso).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ResolucionBloqueoCierreOperativaIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    /**
     * La bandeja se conserva después del resolutorio documental de
     * ENTREGA_DOCUMENTACION (permitido sin pago). LIBERACION_RODADO requiere
     * pago confirmado o absolución; se usa ENTREGA_DOCUMENTACION para validar
     * el comportamiento de conservación de bandeja.
     */
    @Test
    void resolutorioBloqueoCierre_enActasEnEnriquecimiento_200YbandejaConservada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"));
    }

    /**
     * Slate 24C: emitir DOC_LIBERACION_RODADO sin pago confirmado ni absolución
     * devuelve 409.
     */
    @Test
    void resolutorioBloqueoCierre_liberacionRodado_sinPago_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    /**
     * {@code ACTA-0020} nace con pieza requerida; la carga demo produce la medida y
     * pasa a {@code PENDIENTE_FIRMA} (no análisis ni D1/D2). La regla de
     * {@code bandejaHabilitaResolucionBloqueoCierre} aplica: un resolutorio
     * documental no despeja el eje material ni mueve la bandeja.
     */
    @Test
    void resolutorioBloqueoCierre_enPendienteFirma_200BloqueoSigueHastaCumplimiento() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0020";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(
                        jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"));

        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(
                        jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"));
    }

    @Test
    void resolutorioBloqueoCierre_conActaPendienteAnalisis_200() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());
    }

    @Test
    void resolutorioBloqueoCierre_rechazadoGestionExterna_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/ACTA-0017/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    @Test
    void resolutorioBloqueoCierre_rechazadoArchivo_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/ACTA-0007/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    @Test
    void resolutorioBloqueoCierre_rechazadoCerrada_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/ACTA-0008/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }

    /**
     * El resolutorio documental de ENTREGA_DOCUMENTACION no libera el
     * cumplimiento material por sí solo: el bloqueante permanece hasta que se
     * registre el hecho efectivo. LIBERACION_RODADO requiere pago; se usa
     * ENTREGA_DOCUMENTACION para validar el comportamiento genérico.
     */
    @Test
    void resolutorioBloqueoCierre_noLiberaCumplimientoMaterialNialone() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2));
    }

}
