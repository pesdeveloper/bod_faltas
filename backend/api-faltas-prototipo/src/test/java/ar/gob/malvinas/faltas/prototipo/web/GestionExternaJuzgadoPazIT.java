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
 * Circuito JUZGADO_DE_PAZ diferenciado: absolución, condena confirmada,
 * monto modificado.
 *
 * <p>Usa ACTA-0017 (GESTION_EXTERNA/JUZGADO_DE_PAZ, CONDENA_FIRME,
 * monto=75000) para probar las tres resoluciones judiciales.
 *
 * <p>Criterios de aceptación:
 * <ul>
 *   <li>Absolución: resultadoFinal=ABSUELTO, tipoGestionExterna=null,
 *       pagoCondena=false, cerrable=true.</li>
 *   <li>Condena confirmada: resultadoFinal=CONDENA_FIRME,
 *       tipoGestionExterna=null, pagoCondena=true, cerrable=false.</li>
 *   <li>Monto modificado: resultadoFinal=CONDENA_FIRME, montoCondena=nuevoMonto,
 *       tipoGestionExterna=null, pagoCondena=true, cerrable=false.</li>
 *   <li>No se modela pago en juzgado (no existe esa acción).</li>
 *   <li>Eventos correctos en cada caso.</li>
 * </ul>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class GestionExternaJuzgadoPazIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0017";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // -- Estado inicial ----------------------------------------------------------

    @Test
    void actaExisteEnDatasetConEstadoInicialJuzgado() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTA))
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.tipoGestionExterna").value("JUZGADO_DE_PAZ"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(75000));
    }

    // -- Absolucion propuesta ----------------------------------------------------

    @Test
    void reingresarAbsuelto_registraResultadoExternoYRequiereResolucion() throws Exception {
        registrarResultadoJuzgadoAbsuelve();

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.resultadoExternoPostGestion").value("ABSUELVE"))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true));
    }

    @Test
    void reingresarAbsuelto_registraEventoCorrecto() throws Exception {
        registrarResultadoJuzgadoAbsuelve();

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("RESULTADO_GESTION_EXTERNA_PROPONE_ABSOLVER")));
    }

    @Test
    void reingresarAbsuelto_dictarResolucionFirmaNotificaYQuedaAbsuelto() throws Exception {
        registrarResultadoJuzgadoAbsuelve();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0017-03"))
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_ABSOLUTORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0017-03"))
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

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    // -- Condena confirmada sin cambio de fondo ---------------------------------

    @Test
    void reingresarCondenaConfirmada_mantieneCondenaYPagoCondena() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/juzgado-reingresar-condena-confirmada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.resolucion").value("CONFIRMA_CONDENA"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_GESTION_EXTERNA"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(true))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(true));
    }

    @Test
    void reingresarCondenaConfirmada_registraEventoCorrecto() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/juzgado-reingresar-condena-confirmada"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("RESOLUCION_JUZGADO_CONFIRMA_CONDENA")));
    }

    // -- Monto modificado propuesto ---------------------------------------------

    @Test
    void reingresarMontoModificado_requiereNuevoFallo() throws Exception {
        registrarResultadoJuzgadoModificaMonto(2500);

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.tipoGestionExterna").isEmpty())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.montoCondena").value(75000))
                .andExpect(jsonPath("$.montoCondenaSugeridoPostGestionExterna").value(2500))
                .andExpect(jsonPath("$.resultadoExternoPostGestion").value("MODIFICA_MONTO"))
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(true));
    }

    @Test
    void reingresarMontoModificado_registraEventoCorrecto() throws Exception {
        registrarResultadoJuzgadoModificaMonto(2500);

        mvc.perform(get(B + "/actas/" + ACTA + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento",
                        hasItem("RESULTADO_GESTION_EXTERNA_PROPONE_MODIFICAR_MONTO")));
    }

    @Test
    void reingresarMontoModificado_dictarNuevoFalloFirmaNotificaYQuedaCondenado() throws Exception {
        registrarResultadoJuzgadoModificaMonto(2500);

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value("DOC-0017-03"))
                .andExpect(jsonPath("$.tipoDocumento").value("FALLO_CONDENATORIO"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0017-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_ENVIO"));

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.montoCondena").value(2500))
                .andExpect(jsonPath("$.accionesUi.apelacionPresencial").value(true))
                .andExpect(jsonPath("$.accionesUi.vencimientoPlazoApelacion").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false));
    }

    @Test
    void reingresarMontoModificado_conMontoInvalido_devuelveConflict() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/juzgado-reingresar-monto-modificado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoMonto\": 0}"))
                .andExpect(status().is4xxClientError());
    }

    // -- Conflictos --------------------------------------------------------------

    @Test
    void juzgadoAcciones_noAplicanAActaApremio() throws Exception {
        mvc.perform(post(B + "/actas/ACTA-0130/acciones/juzgado-reingresar-absuelto"))
                .andExpect(status().is4xxClientError());

        mvc.perform(post(B + "/actas/ACTA-0130/acciones/juzgado-reingresar-condena-confirmada"))
                .andExpect(status().is4xxClientError());

        mvc.perform(post(B + "/actas/ACTA-0130/acciones/juzgado-reingresar-monto-modificado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoMonto\": 1000}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void noPagoEnJuzgado_noExisteEndpoint() throws Exception {
        // Verificar que no hay endpoint de pago en juzgado
        // La acción de apremio no aplica a acta de juzgado
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/apremio-registrar-pago"))
                .andExpect(status().is4xxClientError());
    }

    private void registrarResultadoJuzgadoModificaMonto(int monto) throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/juzgado-reingresar-monto-modificado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoMonto\":" + monto + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"));
    }

    private void registrarResultadoJuzgadoAbsuelve() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/juzgado-reingresar-absuelto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.accionPendiente").value("DICTAR_FALLO_POST_GESTION_EXTERNA"));
    }
}
