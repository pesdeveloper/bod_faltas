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
 * Slice 10: el infractor siempre puede solicitar pago voluntario mientras
 * el expediente esté en una bandeja interna operable. La precondición
 * deja de estar atada a {@code ACTAS_EN_ENRIQUECIMIENTO} y pasa a
 * delegarse en el helper {@code bandejaPermitePagoVoluntario}, que sólo
 * excluye {@code ARCHIVO}, {@code CERRADAS} y {@code GESTION_EXTERNA}.
 *
 * <p>La lista expuesta por {@code accionesPagoVoluntarioDisponibles} y la
 * validación del endpoint {@code registrar-solicitud-pago-voluntario}
 * deben ser simétricas: si el endpoint acepta el caso, la lista contiene
 * {@code SOLICITAR}; si la lista está vacía, el endpoint responde 409.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PagoVoluntarioBandejasOperablesIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void actaEnPendientesResolucionRedaccion_sinPago_solicitarDisponibleYAceptado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0013";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTES_RESOLUCION_REDACCION"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(1))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("SOLICITAR"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_PAGO_VOLUNTARIO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(1))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("INFORMAR"));
    }

    @Test
    void actaEnPendienteAnalisis_sinPago_solicitarDisponibleYAceptado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0016";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(1))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("SOLICITAR"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"));
    }

    @Test
    void actaEnEnriquecimiento_sinPago_solicitarSigueDisponibleYAceptado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0024";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(1))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("SOLICITAR"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isOk());
    }

    @Test
    void actaEnArchivo_listaVaciaYEndpoint409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0007";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(0));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void actaCerrada_listaVaciaYEndpoint409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0008";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estaCerrada").value(true))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(0));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void actaEnGestionExterna_listaVaciaYEndpoint409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0017";

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("GESTION_EXTERNA"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles.length()").value(0));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isConflict());
    }

    @Test
    void solicitarDosVeces_segundaVezNoEsListadaYEndpoint409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        String id = "ACTA-0024";

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionPago").value("SOLICITADO"))
                .andExpect(jsonPath("$.accionesPagoVoluntarioDisponibles[0]").value("INFORMAR"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isConflict());
    }
}
