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
 * Recorrido demo ACTA-0024: datos de tránsito (flags mock) con anclas al
 * nacimiento → análisis → pago informado → comprobante → confirmación →
 * resolución + cumplimiento material (dos ejes) → cierre.
 *
 * <p>Slate 24A: {@code medidaPreventivaAplicable = false}. La retención de
 * rodado/documentación en tránsito no usa {@code MEDIDA_PREVENTIVA}. Dos
 * bloqueantes: {@code LIBERACION_RODADO} y {@code ENTREGA_DOCUMENTACION}.
 * El eje {@code MEDIDA_PREVENTIVA} queda {@code NO_APLICA} en
 * {@code hechosMateriales.ejes[0]}.
 *
 * <p>Slate 24B: regla de dominio — liberar rodado requiere pago confirmado
 * o absolución. El cumplimiento material de {@code LIBERACION_RODADO} devuelve
 * 409 si no existe ninguna de esas condiciones. La entrega de documentación
 * sí puede resolverse antes del pago. El pago voluntario requiere monto > 0
 * en {@code registrar-solicitud-pago-voluntario} (ya validado en el
 * controller).
 *
 * <p>Slate 24C: extiende la protección al paso documental. Sin pago confirmado
 * o absolución, tampoco se puede emitir {@code DOC_LIBERACION_RODADO} ni
 * registrar el evento {@code LIBERACION_RODADO_EMITIDA}. Ambas capas
 * (documental y material) comparten el helper
 * {@code tieneResultadoHabilitanteParaLiberarRodado}. La entrega de
 * documentación sigue siendo permitida sin pago.
 *
 * <p>Nota: tránsito puede tener medidas preventivas reales por
 * ordenanza/artículo; lo que no corresponde es usar {@code MEDIDA_PREVENTIVA}
 * para representar automáticamente la retención de rodado/documentación.
 *
 * <p>Alineado con
 * {@code MockDataFactory#cargarActa0024NacimientoCondicionesMaterialesPorConstatacionTempranaDemo}.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class DemoRecorridoPuntaAPuntaActa0024IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0024";
    private static final String LECTURA_TEMPRANAS =
            "Existen condiciones materiales tempranas activas. Deberán resolverse documentalmente y"
                    + " cumplirse materialmente antes del cierre.";
    private static final String LECTURA_SIN_CUMPLIMIENTO =
            "Existen resolutorios documentales registrados, pero todavía falta cumplimiento material"
                    + " efectivo.";

    @Autowired
    private MockMvc mvc;

    /**
     * Estado inicial tras reset: dos bloqueantes (LIBERACION_RODADO,
     * ENTREGA_DOCUMENTACION). El eje MEDIDA_PREVENTIVA es NO_APLICA:
     * ni documento ni bloqueante artificial.
     */
    @Test
    void estadoInicial_dosBloqueantes_sinMedidaPreventiva() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_TEMPRANAS))
                // Eje 0: MEDIDA_PREVENTIVA — NO_APLICA; sin bloqueante artificial
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].clave").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre").doesNotExist())
                // Eje 1: RODADO — pendiente resolutorio
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].ejeBloqueanteCierre").value("LIBERACION_RODADO"))
                // Eje 2: DOCUMENTACION — pendiente resolutorio
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].clave").value("DOCUMENTACION"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].ejeBloqueanteCierre").value("ENTREGA_DOCUMENTACION"));
    }

    /**
     * Verifica que los documentos iniciales de ACTA-0024 no incluyen
     * ningún documento de tipo {@code MEDIDA_PREVENTIVA}.
     */
    @Test
    void documentosIniciales_noContienenMedidaPreventiva() throws Exception {
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

    /**
     * Constatación temprana de SECUESTRO_RODADO rechazada (ya existe ACTA_RETENCION).
     */
    @Test
    void constatacionTemprana_rodadoYaPresente_conflict() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "SECUESTRO_RODADO"))
                .andExpect(status().isConflict());
    }

    /**
     * Slate 24C — Regla principal: emitir DOC_LIBERACION_RODADO sin pago confirmado
     * ni absolución debe devolver 409. No se crea el documento ni el evento; el
     * bloqueante LIBERACION_RODADO sigue pendiente y el eje RODADO sigue en
     * SITUACION_PENDIENTE_DE_RESOLUTORIO.
     */
    @Test
    void noPermiteEmitirLiberacionRodadoSinPagoNiAbsolucion() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Solicitar pago para pasar a PENDIENTE_ANALISIS
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());

        // Entregar documentación (permitido sin pago)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        // Sin pago confirmado: emitir DOC_LIBERACION_RODADO debe devolver 409
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        // RODADO sigue SITUACION_PENDIENTE_DE_RESOLUTORIO; bloqueante activo
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"));

        // No existe DOC_LIBERACION_RODADO: solo 2 docs originales + DOC_RESTITUCION_DOCUMENTACION
        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    /**
     * Slate 24C — El cumplimiento material de LIBERACION_RODADO también devuelve
     * 409 sin pago confirmado, incluso si el bloqueo documental ya estaba activo.
     * La protección opera en ambas capas.
     */
    @Test
    void noPermiteRetiroEfectivoRodadoSinPagoNiAbsolucion() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Solicitar pago para pasar a PENDIENTE_ANALISIS
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());

        // Sin pago confirmado: cumplimiento material para RODADO debe devolver 409
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        // RODADO no pasa a CUMPLIMIENTO_MATERIAL_VERIFICADO
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2));
    }

    /**
     * Slate 24C — Con pago confirmado: DOC_LIBERACION_RODADO puede emitirse.
     * El eje RODADO pasa a RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL; el
     * bloqueante LIBERACION_RODADO sigue pendiente hasta el retiro efectivo.
     */
    @Test
    void permiteEmitirLiberacionRodadoConPagoConfirmado() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Ciclo de pago completo
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0024.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // Con PAGO_CONFIRMADO: emitir DOC_LIBERACION_RODADO debe funcionar
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LIBERACION_RODADO"));

        // RODADO pasa a RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL; bloqueante sigue
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].clave").value("RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase").value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"));
    }

    /**
     * Slate 24B — La entrega de documentación puede resolverse antes del pago.
     * Solo el bloqueante LIBERACION_RODADO queda condicionado al pago.
     */
    @Test
    void documentacionPuedeEntregarseSinPago() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Solicitar pago para transición a PENDIENTE_ANALISIS
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());

        // Cumplimiento de ENTREGA_DOCUMENTACION sin pago confirmado: debe ser permitido
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1));

        // RODADO sigue pendiente
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"));
    }

    /**
     * Slate 24B — Pago voluntario requiere monto > 0.
     * {@code registrar-solicitud-pago-voluntario} es la acción de autorizar/liquidar
     * el pago con monto obligatorio. El controller rechaza monto nulo o <= 0 con 400.
     */
    @Test
    void pagoVoluntarioRequiereMonto() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Sin body: 400
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isBadRequest());

        // Monto nulo: 400
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":null}"))
                .andExpect(status().isBadRequest());

        // Monto cero: 400
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":0}"))
                .andExpect(status().isBadRequest());

        // Monto negativo: 400
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":-1}"))
                .andExpect(status().isBadRequest());

        // Con monto > 0: OK y montoPagoVoluntario persiste
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagoVoluntario").value(500.00));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoPagoVoluntario").value(500.00))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"));
    }

    /**
     * Slate 24B — Después de pago confirmado, el cumplimiento material de
     * LIBERACION_RODADO debe ser permitido. Los bloqueantes quedan vacíos.
     */
    @Test
    void permiteLiberarRodadoConPagoConfirmado() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // Ciclo de pago completo
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_0024.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // Con PAGO_CONFIRMADO: emitir resolutorio documental y luego cumplimiento para ambos
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("LIBERACION_RODADO"));

        // Resolver ENTREGA_DOCUMENTACION (atómica)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk());

        // Bloqueantes vacíos
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));
    }

    /**
     * Slate 24C — Recorrido e2e con rodado condicionado al pago:
     * documentación resuelta primero; intento de liberar rodado antes del pago
     * devuelve 409 (documental bloqueada); luego pago; luego documental de
     * liberación; luego retiro; luego cierre. Sin LEVANTAMIENTO_MEDIDA_PREVENTIVA.
     */
    @Test
    void recorridoActa0024_pagoYcierreConRodadoCondicionado() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        // 1. Transición a análisis con pago habilitado (monto obligatorio)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());

        // 2. Entregar documentación antes del pago: permitido
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "ENTREGA_DOCUMENTACION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("ENTREGA_DOCUMENTACION"));

        // Verificar: rodado sigue bloqueando
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"));

        // 3. Intentar emitir DOC_LIBERACION_RODADO ANTES del pago: 409
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isConflict());

        // 4. Pago informado + comprobante + confirmación
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                        .param("nombreArchivo", "comprobante_demo_0024.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        // Pago confirmado; rodado sigue en SITUACION_PENDIENTE_DE_RESOLUTORIO
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_TEMPRANAS));

        // 5. Ahora sí: emitir DOC_LIBERACION_RODADO (pago confirmado)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteAtendido").value("LIBERACION_RODADO"));

        // RODADO pasa a RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL
        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_SIN_CUMPLIMIENTO));

        // 6. Registrar retiro efectivo (app)
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                        .param("tipo", "LIBERACION_RODADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendienteCumplido").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        // 7. Cerrar acta
        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"));
    }

    /**
     * Recorrido e2e legado (pago primero, luego resolucion + cumplimiento para ambos ejes).
     * Sigue siendo válido porque el pago está confirmado antes del cumplimiento de RODADO.
     * Sin LEVANTAMIENTO_MEDIDA_PREVENTIVA en ningún paso.
     */
    @Test
    void recorridoActa0024_pagoYcierreConDosBloqueantes() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/adjuntar-comprobante-pago-informado")
                                .param("nombreArchivo", "comprobante_demo_0024.pdf"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_TEMPRANAS));

        String[] pendientes = {"ENTREGA_DOCUMENTACION", "LIBERACION_RODADO"};
        for (String p : pendientes) {
            mvc.perform(
                            post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                    .param("tipo", p))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(2))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_SIN_CUMPLIMIENTO))
                // MEDIDA_PREVENTIVA sigue NO_APLICA — sin resolutorio artificial
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre").doesNotExist())
                // RODADO y DOCUMENTACION: resolutorio en expediente, falta hecho material
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].fase")
                        .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[1].ejeBloqueanteCierre").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].fase")
                        .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[2].ejeBloqueanteCierre").value("ENTREGA_DOCUMENTACION"));

        // PAGO_CONFIRMADO ya activo: cumplimiento permitido para ambos
        for (String p : pendientes) {
            mvc.perform(
                            post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                    .param("tipo", p))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").isEmpty());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }
}
