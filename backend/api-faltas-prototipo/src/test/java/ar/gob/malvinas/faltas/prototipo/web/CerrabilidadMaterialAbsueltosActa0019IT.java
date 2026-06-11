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
 * Regresion: ACTA-0019 — resultado ABSUELTO con dos bloqueantes materiales activos.
 *
 * <p>ACTA-0019 representa un circuito de Tránsito con retención de rodado y documentación.
 * En tránsito, RODADO y DOCUMENTACION son ejes materiales propios; el secuestro/retiro
 * de rodado y la retención de documentación no son medidas preventivas.
 * MEDIDA_PREVENTIVA queda NO_APLICA; no existe LEVANTAMIENTO_MEDIDA_PREVENTIVA.
 *
 * <p>Contratos verificados:
 * <ol>
 *   <li>Estado inicial: {@code PENDIENTE_ANALISIS}, {@code PENDIENTE_CIERRE_MATERIAL},
 *       {@code resultadoFinal=ABSUELTO}, {@code cerrable=false}, dos bloqueantes activos.</li>
 *   <li>Documentos materiales preexistentes: ACTA_RETENCION, CONSTATACION_RETENCION_DOCUMENTACION.
 *       Sin documento MEDIDA_PREVENTIVA.</li>
 *   <li>Eje MEDIDA_PREVENTIVA en estado NO_APLICA.</li>
 *   <li>{@code POST cerrar-acta} rechazado con 409 mientras existan bloqueantes activos.</li>
 *   <li>DOCUMENTACION es atómica: un solo paso resuelve el bloqueante.</li>
 *   <li>RODADO requiere dos pasos: resolutorio documental + cumplimiento material.</li>
 *   <li>Al resolver ambos bloqueantes: cerrable=true y cierre habilitado.</li>
 * </ol>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CerrabilidadMaterialAbsueltosActa0019IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0019";

    @Autowired
    private MockMvc mvc;

    @Test
    void acta0019_absueltoConBloqueantesActivos_estadoInicialNoEsCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_CIERRE_MATERIAL"))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable").isNotEmpty());
    }

    @Test
    void acta0019_transitoNoTieneMedidaPreventivaArtificial() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].clave").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].bloqueaCierre").value(false))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].clave").value("DOCUMENTACION"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"));
    }

    @Test
    void acta0019_absueltoConBloqueantes_cerrarActaRechazado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void acta0019_absueltoConBloqueantes_documentosMateriales() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipoDocumento").value("ACTA_RETENCION"))
                .andExpect(jsonPath("$[0].estadoDocumento").value("ADJUNTO"))
                .andExpect(jsonPath("$[1].tipoDocumento").value("CONSTATACION_RETENCION_DOCUMENTACION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("ADJUNTO"));
    }

    @Test
    void acta0019_absueltoConBloqueantes_archivoPosiblePorNoCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));
    }

    /**
     * Regla nueva: ENTREGA_DOCUMENTACION requiere resolutorio firmado.
     * Cumplimiento directo sin resolutorio previo devuelve 409.
     */
    @Test
    void acta0019_entregaDocumentacion_sinResolutorio_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isConflict());
    }

    /**
     * Flujo completo: resolutorio → firma → cumplimiento para ENTREGA_DOCUMENTACION.
     * El resolutorio queda PENDIENTE_FIRMA, tras firma el acta vuelve a análisis
     * y el cumplimiento material ya es ejecutable.
     */
    @Test
    void acta0019_entregaDocumentacion_resolutorioFirmaYCumplimiento() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0019-03"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0019-03"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(true));

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].clave").value("DOCUMENTACION"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].fase").value("CUMPLIMIENTO_MATERIAL_VERIFICADO"));
    }

    /**
     * Flujo completo para ambos bloqueantes: resolutorio → firma → cumplimiento
     * para LIBERACION_RODADO y ENTREGA_DOCUMENTACION → habilita cierre.
     */
    @Test
    void acta0019_resolverDosBloqueantes_habilitaCierre() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // LIBERACION_RODADO: resolutorio → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0019-03"));
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0019-03"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        // ENTREGA_DOCUMENTACION: resolutorio → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0019-04"));
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0019-04"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    /**
     * LIBERACION_RODADO requiere resolutorio + firma antes del cumplimiento.
     * Sin resolutorio: 409. Con resolutorio PENDIENTE_FIRMA: acta en PENDIENTE_FIRMA,
     * eje en RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL.
     */
    @Test
    void acta0019_liberacionRodado_requiereResolutorioFirmado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(true))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase")
                        .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].clave").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"));

        // Cumplimiento antes de firmar → 409
        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());
    }
}
