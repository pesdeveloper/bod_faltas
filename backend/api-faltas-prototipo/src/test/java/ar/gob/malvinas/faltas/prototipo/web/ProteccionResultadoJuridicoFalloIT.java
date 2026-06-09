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
 * Guardas mínimas: acciones del flujo viejo (confirmar pago informado, marcar
 * absolución directa) no pueden pisar {@code CONDENADO} ni {@code CONDENA_FIRME}.
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ProteccionResultadoJuridicoFalloIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0006";

    @Autowired
    private MockMvc mvc;

    @Test
    void confirmarPagoInformado_despuesFalloCondenatorio_rechaza409YconservaCondensado() throws Exception {
        llevarActaACondenadoNotificado();
        prepararPagoInformadoPendienteConfirmacion();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false));
    }

    @Test
    void confirmarPagoInformado_despuesCondenaFirme_rechaza409YconservaCondenaFirme() throws Exception {
        llevarActaACondenadoNotificado();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());
        prepararPagoInformadoPendienteConfirmacion();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/confirmar-pago-informado"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));
    }

    @Test
    void marcarAbsuelto_despuesFalloCondenatorio_rechaza409YconservaCondensado() throws Exception {
        llevarActaACondenadoNotificado();

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/marcar-resultado-final-absuelto"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"));
    }

    @Test
    void marcarAbsuelto_despuesCondenaFirme_rechaza409YconservaCondenaFirme() throws Exception {
        llevarActaACondenadoNotificado();
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-vencimiento-plazo-apelacion"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/marcar-resultado-final-absuelto"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENA_FIRME"));
    }

    private void llevarActaACondenadoNotificado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoCondena\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/firmar-documento/DOC-0006-02"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-notificacion-positiva"))
                .andExpect(status().isOk());
    }

    private void prepararPagoInformadoPendienteConfirmacion() throws Exception {
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-solicitud-pago-voluntario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\":12345.67}"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/registrar-pago-informado"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ACTA + "/acciones/adjuntar-comprobante-pago-informado"))
                .andExpect(status().isOk());
    }
}
