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
 * Recorrido demo ACTA-0021: tránsito con pago voluntario ya confirmado en
 * precarga, pero con bloqueantes materiales pendientes.
 *
 * <p>Representa una variante distinta a ACTA-0024:
 * <ul>
 *   <li>ACTA-0024 valida el circuito completo desde SIN_PAGO hasta cierre.</li>
 *   <li>ACTA-0021 valida que aunque el pago esté confirmado desde el inicio,
 *       el acta no es cerrable si faltan bloqueantes materiales
 *       ({@code ENTREGA_DOCUMENTACION} y {@code LIBERACION_RODADO}).</li>
 * </ul>
 *
 * <p>Estado inicial precargado: {@code PENDIENTE_ANALISIS} /
 * {@code PENDIENTE_CIERRE_MATERIAL}, {@code situacionPago=CONFIRMADO},
 * {@code montoPagoVoluntario=8750.00}, {@code resultadoFinal=PAGO_CONFIRMADO}.
 * Sin medida preventiva ({@code MEDIDA_PREVENTIVA=NO_APLICA}).
 *
 * <p>Como ya existe pago confirmado: permite emitir DOC_LIBERACION_RODADO y
 * registrar retiro de rodado desde el estado inicial (sin ciclo de pago).
 *
 * <p>Alineado con
 * {@code MockDataFactory#cargarActa0021CerrabilidadMaterialesPagoConfirmadoDemo}.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class DemoRecorridoPuntaAPuntaActa0021IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0021";

    @Autowired
    private MockMvc mvc;

    /**
     * Estado inicial: pago confirmado con materiales pendientes.
     * situacionPago=CONFIRMADO, montoPagoVoluntario>0, resultadoFinal=PAGO_CONFIRMADO,
     * cerrable=false, dos bloqueantes activos, MEDIDA_PREVENTIVA=NO_APLICA.
     */
    @Test
    void acta0021_estadoInicial_pagoConfirmadoConMaterialesPendientes() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(8750.00))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]").value("LIBERACION_RODADO"))
                // MEDIDA_PREVENTIVA NO_APLICA — sin bloqueante ni documento artificial
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].clave").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre").doesNotExist())
                // RODADO pendiente resolutorio
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].ejeBloqueanteCierre").value("LIBERACION_RODADO"))
                // DOCUMENTACION pendiente resolutorio
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].clave").value("DOCUMENTACION"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].ejeBloqueanteCierre").value("ENTREGA_DOCUMENTACION"));
    }

    /**
     * Aunque el pago esté confirmado, no es cerrable si tiene bloqueantes.
     * motivoCerrabilidad menciona bloqueantes pendientes.
     */
    @Test
    void acta0021_noEsCerrableAunquePagoConfirmadoSiTieneBloqueantes() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable").isNotEmpty());

        // Intentar cerrar directamente debe devolver conflict
        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());
    }

    /**
     * Flujo completo: resolutorio → firma → cumplimiento para ambos bloqueantes.
     * El pago ya está confirmado, así que no hace falta ciclo de pago.
     */
    @Test
    void acta0021_documentacionYRodado_resuelvenYHabilitanCierre() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // 1. Resolutorio de ENTREGA_DOCUMENTACION → DOC-0021-03
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("ENTREGA_DOCUMENTACION"));

        // Acta en PENDIENTE_FIRMA, cumplimientoMaterial=false
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(true))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false));

        // 2. Firmar resolutorio → acta vuelve a análisis
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0021-03"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(true));

        // 3. Cumplimiento de ENTREGA_DOCUMENTACION
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"));

        // 4. Resolutorio de LIBERACION_RODADO → DOC-0021-04
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LIBERACION_RODADO"));

        // Rodado pasa a resolutorio sin hecho material; sigue bloqueando
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase")
                        .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"));

        // 5. Firmar resolutorio de rodado → acta vuelve a análisis
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0021-04"))
                .andExpect(status().isOk());

        // 6. Registrar retiro efectivo
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));
    }

    /**
     * Recorrido e2e completo: resolver materiales (resolutorio→firma→cumplimiento) y cerrar.
     */
    @Test
    void acta0021_cierreFinal_luegoDe_resolverMateriales() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // ENTREGA_DOCUMENTACION: resolutorio → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0021-03"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        // LIBERACION_RODADO: resolutorio → firma → cumplimiento
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/firmar-documento/DOC-0021-04"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        // Verificar cerrable antes del cierre
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        // Cerrar
        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        // Estado final
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true))
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    /**
     * Verifica que los documentos iniciales de ACTA-0021 no incluyen
     * MEDIDA_PREVENTIVA, LEVANTAMIENTO_MEDIDA_PREVENTIVA ni
     * DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA.
     */
    @Test
    void acta0021_documentosIniciales_sinMedidaPreventiva() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipoDocumento").value("ACTA_RETENCION"))
                .andExpect(jsonPath("$[0].estadoDocumento").value("ADJUNTO"))
                .andExpect(jsonPath("$[1].tipoDocumento").value("CONSTATACION_RETENCION_DOCUMENTACION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("ADJUNTO"));
    }
}
