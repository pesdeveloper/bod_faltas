package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resolutorios de cierre: bandeja operativa amplia, exclusiones. El eje
 * material de medida es siempre {@code LEVANTAMIENTO_MEDIDA_PREVENTIVA};
 * firma/notificación de resolutorio se eligen con
 * {@code documentoConCircuitoFirmaNotif}, no con otro bloqueante.
 *
 * <p>Casos de bandeja: {@code ACTA-0024} (enriquecimiento / análisis),
 * {@code ACTA-0020} (tras piezas: queda en {@code PENDIENTE_FIRMA} con
 * orígenes de cierre), exclusiones 0017/0007/0008.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ResolucionBloqueoCierreOperativaIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void resolutorioBloqueoCierre_enActasEnEnriquecimiento_200YbandejaConservada() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"));
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
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
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

    @Test
    void resolutorioBloqueoCierre_noLiberaCumplimientoMaterialNialone() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3));
    }

    @Test
    void levantamientoCircuitoFirmaNotif_pendienteFirmaSinNotificacionesActa() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA")
                                .param("documentoConCircuitoFirmaNotif", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"));
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].tipoDocumento")
                        .value("DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF"))
                .andExpect(jsonPath("$[3].estadoDocumento").value("PENDIENTE_FIRMA"));
        mvc.perform(get(B + "/actas/" + id + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void levantamientoCircuitoFirmaNotif_despuesFirmaInSitu_documentoFirmadoYsinNotifActa() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA")
                                .param("documentoConCircuitoFirmaNotif", "true"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(jsonPath("$[3].id").value("DOC-0024-04"));
        mvc.perform(post(B + "/actas/" + id + "/acciones/firmar-documento/DOC-0024-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"));
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(jsonPath("$[3].estadoDocumento").value("FIRMADO"));
        mvc.perform(get(B + "/actas/" + id + "/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void levantamientoMismoBloqueante_enEnriquecimiento_hechosMaterialesYcumplimientoLiberaEje() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        String id = "ACTA-0024";
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA")
                                .param("documentoConCircuitoFirmaNotif", "true"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre")
                        .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                        .value("ENTREGA_DOCUMENTACION"));
        mvc.perform(post(B + "/actas/" + id + "/acciones/firmar-documento/DOC-0024-04"))
                .andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/" + id + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase")
                        .value("CUMPLIMIENTO_MATERIAL_VERIFICADO"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2));
    }
}
