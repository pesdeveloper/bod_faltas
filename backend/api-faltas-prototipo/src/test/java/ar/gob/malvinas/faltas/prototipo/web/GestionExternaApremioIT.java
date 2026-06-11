package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuito APREMIO diferenciado: pago en apremio y reingreso sin pago.
 *
 * <p>Usa ACTA-0130 (GESTION_EXTERNA/APREMIO, CONDENA_FIRME, monto=75000)
 * para probar ambas salidas del circuito de apremio sin necesidad de
 * derivar primero.
 *
 * <p>Criterios de aceptación:
 * <ul>
 *   <li>Pago en apremio: situacionPagoCondena=INFORMADO, tipoGestionExterna=null,
 *       resultadoFinal=CONDENA_FIRME, confirmación/observación habilitadas.</li>
 *   <li>Reingreso sin pago: vuelve a análisis, tipoGestionExterna=null,
 *       situacionPagoCondena=PENDIENTE, pagoCondena=true (acción INFORMAR disponible),
 *       cerrable=false.</li>
 *   <li>Eventos correctos en ambos casos.</li>
 *   <li>No rompe ACTA-0029 (pago condena directo sin gestión externa).</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class GestionExternaApremioIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0130";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // -- Estado inicial ----------------------------------------------------------

    @Test
    void actaExisteEnDatasetConEstadoInicialApremio() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTA))
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("EN_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.tipoGestionExterna").value("APREMIO"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(75000))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"));
    }

    // -- Pago informado desde gestion externa -----------------------------------

    @Test
    void registrarPagoEnApremio_dejaPagoInformadoPendienteConfirmacion() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPago").value("PENDIENTE_CONFIRMACION"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.accionesUi.confirmarPagoCondena").value(true))
                .andExpect(jsonPath("$.accionesUi.observarPagoCondena").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false));
    }

    @Test
    void registrarPagoEnApremio_registraEventoCorrecto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", hasItem("PAGO_EN_APREMIO_REGISTRADO")));
    }

    @Test
    void registrarPagoEnApremio_noPermiteCerrarSinConfirmacion() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());
    }

    // -- Reingreso sin pago ------------------------------------------------------

    @Test
    void apremioReingresarSinPago_vuelveAAnalisisConPagoCondena() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_GESTION_EXTERNA"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(true))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(true))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false));
    }

    @Test
    void apremioReingresarSinPago_registraEventoCorrecto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("ACTA_REINGRESADA_DESDE_APREMIO_SIN_PAGO")));
    }

    // -- Resultado externo sustantivo -------------------------------------------

    @Test
    void apremioResultadoExternoModificaMonto_requiereNuevoFallo() throws Exception {
        registrarResultadoApremioModificaMonto(9000);

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.resultadoExternoPostGestion").value("MODIFICA_MONTO"))
                .andExpect(jsonPath("$.montoCondenaSugeridoPostGestionExterna").value(9000))
                .andExpect(jsonPath("$.montoCondena").value(75000))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("RESULTADO_GESTION_EXTERNA_PROPONE_MODIFICAR_MONTO")));
    }

    @Test
    void apremioDictaNuevoFalloModificatorioFirmaNotificaYQuedaCondenado() throws Exception {
        registrarResultadoApremioModificaMonto(9000);

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":9000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0130-03"))
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.montoCondena").value(9000))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(true))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0130-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.notificacion").value(true))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.montoCondena").value(9000))
                .andExpect(jsonPath("$.accionesUi.apelacionPresencial").value(true))
                .andExpect(jsonPath("$.accionesUi.vencimientoPlazoApelacion").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false));
    }

    @Test
    void apremioResultadoExternoProponeAbsolucion_requiereResolucion() throws Exception {
        registrarResultadoApremioAbsuelve();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.resultadoExternoPostGestion").value("ABSUELVE"))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true));

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("RESULTADO_GESTION_EXTERNA_PROPONE_ABSOLVER")));
    }

    @Test
    void apremioDictaResolucionAbsolutoriaFirmaNotificaYQuedaAbsuelto() throws Exception {
        registrarResultadoApremioAbsuelve();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0130-03"))
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_ABSOLUTORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0130-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("NO_APLICA"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(true));
    }

    // -- Conflictos --------------------------------------------------------------

    @Test
    void registrarPagoEnApremio_desdeApremio_segundaVezConflict() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void apremioAcciones_noAplicanAActaJuzgado() throws Exception {
        mvc.perform(post(B + "/actas/ACTA-0017/acciones/apremio-registrar-pago"))
                .andExpect(status().is4xxClientError());

        mvc.perform(post(B + "/actas/ACTA-0017/acciones/apremio-reingresar-sin-pago"))
                .andExpect(status().is4xxClientError());
    }

    // ── Sanidad: no rompe ACTA-0029 ─────────────────────────────────────────────

    @Test
    void acta0029_circuito_no_usa_gestion_externa() throws Exception {
        mvc.perform(get(B + "/actas/ACTA-0029"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty());
    }

    private void registrarResultadoApremioModificaMonto(int monto) throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-monto-modificado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoMonto\":" + monto + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"));
    }

    private void registrarResultadoApremioAbsuelve() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-reingresar-absuelto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"));
    }
}
