package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación end-to-end del circuito de continuidad operativa tras
 * reingresar un acta desde la macro-bandeja ARCHIVO.
 *
 * <p>Principio: reingresar desde archivo debe devolver el expediente a un
 * estado activo operable. La acción pendiente post-reingreso se infiere del
 * estado documental y de los eventos previos al archivo; no debe ser siempre
 * la marca genérica {@code REVISION_POST_REINGRESO} cuando el estado material
 * permite inferir la continuación concreta.
 *
 * <p>Cubre los 7 casos especificados en el diseño:
 * <ul>
 *   <li>A: fallo condenatorio firmado + notificación no entregada → REINTENTAR_NOTIFICACION.</li>
 *   <li>B: fallo condenatorio firmado + notificación vencida → EVALUAR_NOTIFICACION_VENCIDA.</li>
 *   <li>C: análisis sin fallo dictado → REVISION_POST_REINGRESO (sin trabas operativas).</li>
 *   <li>D: absuelto (precargado en archivo) → cerrable=true tras reingreso.</li>
 *   <li>E: condena firme, pago pendiente → gestión externa disponible.</li>
 *   <li>F: condena firme, pago informado → confirmar/observar disponibles.</li>
 *   <li>G: condena firme, pago confirmado (precargado) → cerrable=true.</li>
 * </ul>
 *
 * <p>Las actas ACTA-0027 (A), ACTA-0028 (B) y ACTA-0029 (C) se navegan
 * mediante acciones REST desde el dataset demo. Las actas ACTA-0131 (D),
 * ACTA-0132 (G), ACTA-0133 (E) y ACTA-0134 (F) son precargadas directamente
 * en ARCHIVO porque el circuito normal bloquea archivar actas con resultado
 * final ABSUELTO, CONDENA_FIRME o CONDENADO (deben cerrarse o resolverse por
 * pago/gestión externa, no archivarse).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ArchivoReingresoContinuidadIT {

    private static final String B = "/api/prototipo";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    // -------------------------------------------------------------------------
    // Caso A: fallo condenatorio firmado + notificación no entregada
    // -------------------------------------------------------------------------

    /**
     * Caso A: acta con FALLO_CONDENATORIO:FIRMADO cuya notificación no fue
     * entregada. Después de archivar y reingresar, el accionPendiente debe ser
     * REINTENTAR_NOTIFICACION y el endpoint reintentar-notificacion debe
     * devolver 200.
     */
    @Test
    void casoA_falloFirmadoNotifFallida_postReingreso_habilitaReintentarNotificacion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0027";

        // Dictar fallo condenatorio
        MvcResult rFallo = mvc.perform(post(B + "/actas/" + id + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":5000}"))
                .andExpect(status().isOk())
                .andReturn();
        String docId = MAPPER.readTree(rFallo.getResponse().getContentAsString()).path("documentoId").asText();

        // Firmar el fallo → bandeja pasa a PENDIENTE_NOTIFICACION
        mvc.perform(post(B + "/actas/" + id + "/acciones/firmar-documento/" + docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        // Registrar notificación no entregada → vuelve a PENDIENTE_ANALISIS con REINTENTAR_NOTIFICACION
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-notificacion-negativa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"));

        // Archivar desde análisis
        mvc.perform(post(B + "/actas/" + id + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));

        // Reingresar: la acción pendiente debe ser REINTENTAR_NOTIFICACION (inferida)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"));

        // GET detalle: confirmar estado post-reingreso
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        // reintentar-notificacion debe ser ejecutable (200 OK)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reintentar-notificacion"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Caso B: fallo condenatorio firmado + notificación vencida
    // -------------------------------------------------------------------------

    /**
     * Caso B: acta con FALLO_CONDENATORIO:FIRMADO cuya notificación venció.
     * Después de archivar y reingresar, el accionPendiente debe ser
     * EVALUAR_NOTIFICACION_VENCIDA y el endpoint reintentar-notificacion-vencida
     * debe devolver 200.
     */
    @Test
    void casoB_falloFirmadoNotifVencida_postReingreso_habilitaEvaluarNotificacionVencida() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0028";

        // Dictar fallo condenatorio
        MvcResult rFallo = mvc.perform(post(B + "/actas/" + id + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":7500}"))
                .andExpect(status().isOk())
                .andReturn();
        String docId = MAPPER.readTree(rFallo.getResponse().getContentAsString()).path("documentoId").asText();

        // Firmar el fallo
        mvc.perform(post(B + "/actas/" + id + "/acciones/firmar-documento/" + docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_NOTIFICACION"));

        // Registrar notificación vencida → PENDIENTE_ANALISIS con EVALUAR_NOTIFICACION_VENCIDA
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-notificacion-vencida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"));

        // Archivar desde análisis
        mvc.perform(post(B + "/actas/" + id + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));

        // Reingresar: la acción pendiente debe ser EVALUAR_NOTIFICACION_VENCIDA (inferida)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"));

        // GET detalle: confirmar estado
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));

        // reintentar-notificacion-vencida debe ser ejecutable (200 OK)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reintentar-notificacion-vencida"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Caso C: análisis sin fallo dictado
    // -------------------------------------------------------------------------

    /**
     * Caso C: acta en análisis sin fallo dictado. Tras archivar y reingresar,
     * el accionPendiente es REVISION_POST_REINGRESO (informativa, no bloqueante)
     * y el endpoint reintentar-notificacion devuelve 409 (no aplica).
     */
    @Test
    void casoC_sinFallo_postReingreso_accionPendienteRevisionSinTrabas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0029";

        // Archivar directamente (sin fallo dictado)
        mvc.perform(post(B + "/actas/" + id + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));

        // Reingresar
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REINGRESO"));

        // GET detalle: acta activa, no cerrable, no trabada para análisis
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REINGRESO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"));

        // reintentar-notificacion no aplica (no hay fallo firmado con notif fallida)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reintentar-notificacion"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // Caso D: absuelto en archivo (precarga ACTA-0131)
    // -------------------------------------------------------------------------

    /**
     * Caso D: acta con resultadoFinal=ABSUELTO precargada en ARCHIVO.
     * Tras reingresar, la acta debe quedar cerrable=true y sin pendientes
     * bloqueantes. El endpoint cerrar-acta debe devolver 200.
     */
    @Test
    void casoD_absuelto_postReingreso_cerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0131";

        // Estado inicial: ARCHIVO con resultadoFinal=ABSUELTO
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"));

        // Reingresar
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        // GET detalle: cerrable=true, sin pendientes bloqueantes
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        // Cerrar el acta: debe devolver 200
        mvc.perform(post(B + "/actas/" + id + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    // -------------------------------------------------------------------------
    // Caso E: condena firme, pago pendiente (ACTA-0133 precargada en ARCHIVO)
    // -------------------------------------------------------------------------

    /**
     * Caso E: acta con resultadoFinal=CONDENA_FIRME y situacionPagoCondena=PENDIENTE
     * precargada en ARCHIVO. El circuito normal bloquea archivar actas con
     * CONDENA_FIRME (deben resolverse por pago/gestión externa), por lo que este
     * escenario se representa con una acta precargada. Tras reingresar, la gestión
     * externa debe estar disponible (APREMIO y JUZGADO_DE_PAZ) y la acta no debe
     * ser cerrable.
     */
    @Test
    void casoE_condenaFirmePendiente_postReingreso_gestionExternaDisponible() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0133";

        // Estado inicial: ARCHIVO con CONDENA_FIRME + PENDIENTE
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"));

        // Reingresar
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        // GET detalle: condena firme, pago pendiente, gestión externa disponible
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")));

        // derivar-a-apremio debe ser ejecutable
        mvc.perform(post(B + "/actas/" + id + "/acciones/derivar-a-apremio"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Caso F: condena firme, pago informado (ACTA-0134 precargada en ARCHIVO)
    // -------------------------------------------------------------------------

    /**
     * Caso F: acta con resultadoFinal=CONDENA_FIRME y situacionPagoCondena=INFORMADO
     * precargada en ARCHIVO. Tras reingresar, deben estar disponibles las acciones
     * CONFIRMAR y OBSERVAR el pago; la gestión externa no debe estar disponible.
     */
    @Test
    void casoF_condenaFirmeInformado_postReingreso_confirmarObservarDisponibles() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0134";

        // Estado inicial: ARCHIVO con CONDENA_FIRME + INFORMADO
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"));

        // Reingresar
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        // GET detalle: pago informado, acciones CONFIRMAR/OBSERVAR disponibles, sin gestion externa
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[*]")
                        .value(containsInAnyOrder("CONFIRMAR", "OBSERVAR")))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Caso G: condena firme, pago confirmado en archivo (precarga ACTA-0132)
    // -------------------------------------------------------------------------

    /**
     * Caso G: acta con resultadoFinal=CONDENA_FIRME y situacionPagoCondena=CONFIRMADO
     * precargada en ARCHIVO. Tras reingresar, la acta debe quedar cerrable=true y
     * sin gestión externa disponible. El endpoint cerrar-acta debe devolver 200.
     */
    @Test
    void casoG_condenaFirmeConfirmado_postReingreso_cerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0132";

        // Estado inicial: ARCHIVO con CONDENA_FIRME + CONFIRMADO
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));

        // Reingresar
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        // GET detalle: cerrable=true, pago confirmado, sin gestion externa
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());

        // Cerrar el acta: debe devolver 200
        mvc.perform(post(B + "/actas/" + id + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));
    }

    // =========================================================================
    // Tests adicionales: semántica de permiteReingreso y disponibilidad de
    // acciones operativas según bandeja (corresponden a casos A-F del diseño).
    // =========================================================================

    // -------------------------------------------------------------------------
    // Caso A / D: ACTA en ARCHIVO + permiteReingreso=true
    // acciones operativas normales apagadas (pago, gestión externa) desde backend
    // -------------------------------------------------------------------------

    /**
     * Caso A / D: ACTA-0133 (CONDENA_FIRME + PENDIENTE en ARCHIVO) expone
     * permiteReingreso=true en el GET. Las acciones operativas de pago condena
     * y gestión externa devuelven listas vacías mientras la acta está en ARCHIVO:
     * la UI solo debe ofrecer reingreso, no acciones de análisis.
     */
    @Test
    void casoAD_acta0133_archivo_permiteReingreso_accionesOperativasApagadas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0133";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                // Mientras está en ARCHIVO el backend no expone acciones de pago ni gestión
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles").isEmpty())
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());

        // El endpoint de reingreso debe estar disponible (200 OK)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));
    }

    // -------------------------------------------------------------------------
    // Caso B: ACTA en ARCHIVO + permiteReingreso consumido (false)
    // -------------------------------------------------------------------------

    /**
     * Caso B: tras consumir el reingreso, la acta queda en PENDIENTE_ANALISIS
     * con permiteReingreso=false. Un segundo intento de reingresar devuelve 409
     * porque la acta ya no está en ARCHIVO.
     */
    @Test
    void casoB_reingresoConsumido_segundoIntento_409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0133";

        // Consumir el reingreso
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        // Después del reingreso: permiteReingreso=false
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.permiteReingreso").value(false));

        // Segundo intento de reingresar: 409 (acta ya no en ARCHIVO)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // Caso C: ACTA cerrada
    // -------------------------------------------------------------------------

    /**
     * Caso C: un acta cerrada expone estaCerrada=true y el intento de reingresar
     * devuelve 409 (el circuito de reingreso solo aplica a la macro-bandeja
     * ARCHIVO, no a CERRADAS).
     */
    @Test
    void casoC_actaCerrada_noPermiteReingreso() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        // ACTA-0131: ABSUELTO en ARCHIVO → la cerramos primero reingresando y cerrando
        String id = "ACTA-0131";

        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + id + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        // Acta cerrada: estaCerrada=true, permiteReingreso=false
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true))
                .andExpect(jsonPath("$.permiteReingreso").value(false));

        // Intentar reingresar: 409 (no está en ARCHIVO)
        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // Caso E expandido: ACTA-0133 post-reingreso expone pago y gestión externa
    // -------------------------------------------------------------------------

    /**
     * Caso E (extendido): tras reingresar ACTA-0133 (CONDENA_FIRME + PENDIENTE),
     * el backend expone accionesPagoCondena=[INFORMAR] y
     * accionesGestionExterna=[APREMIO, JUZGADO_DE_PAZ]. El cierre no está
     * disponible porque el pago no fue confirmado.
     */
    @Test
    void casoE_extendido_acta0133_postReingreso_pagoYGestionExternaDisponibles() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0133";

        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("PENDIENTE"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                // accionesUi.pagoCondena=true → INFORMAR disponible
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[0]").value("INFORMAR"))
                // accionesUi.gestionExterna=true → APREMIO + JUZGADO_DE_PAZ disponibles
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles[*]")
                        .value(containsInAnyOrder("APREMIO", "JUZGADO_DE_PAZ")));
    }

    // -------------------------------------------------------------------------
    // Caso F expandido: ACTA-0134 post-reingreso expone confirmar/observar
    // -------------------------------------------------------------------------

    /**
     * Caso F (extendido): tras reingresar ACTA-0134 (CONDENA_FIRME + INFORMADO),
     * el backend expone accionesPagoCondena=[CONFIRMAR, OBSERVAR] y
     * accionesGestionExterna=[]. El cierre no está disponible.
     */
    @Test
    void casoF_extendido_acta0134_postReingreso_confirmarObservarSinGestion() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0134";

        mvc.perform(post(B + "/actas/" + id + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"))
                .andExpect(jsonPath("$.situacionPagoCondena").value("INFORMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                // accionesUi.confirmarPagoCondena=true y accionesUi.observarPagoCondena=true
                .andExpect(jsonPath("$.accionesPagoCondenaDisponibles[*]")
                        .value(containsInAnyOrder("CONFIRMAR", "OBSERVAR")))
                // accionesUi.gestionExterna=false
                .andExpect(jsonPath("$.accionesGestionExternaDisponibles").isEmpty());
    }
}
