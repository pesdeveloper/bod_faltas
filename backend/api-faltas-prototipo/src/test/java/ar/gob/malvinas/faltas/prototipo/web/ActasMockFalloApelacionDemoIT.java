package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las actas mock asignadas a recorridos demo de fallo/apelación existen y
 * arrancan en estado inicial limpio (sin fallo, apelación ni pago previo).
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ActasMockFalloApelacionDemoIT {

    private static final String B = "/api/prototipo";

    /** Fallo absolutorio → ABSUELTO → cierre. */
    private static final String ACTA_FALLO_ABSOLUTORIO = "ACTA-0006";
    /** Fallo condenatorio + apelación portal. */
    private static final String ACTA_APELACION_PORTAL = "ACTA-0027";
    /** Fallo condenatorio + apelación presencial. */
    private static final String ACTA_APELACION_PRESENCIAL = "ACTA-0028";
    /** Fallo condenatorio + vencimiento plazo → CONDENA_FIRME. */
    private static final String ACTA_VENCIMIENTO_PLAZO = "ACTA-0029";
    /** Fallo condenatorio + apelación → ACEPTADA_ABSUELVE. */
    private static final String ACTA_APELACION_ABSUELVE = "ACTA-0030";

    @Autowired
    private MockMvc mvc;

    @Test
    void actasDemoFalloApelacion_existenConEstadoInicialLimpio() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        assertActaInicialFalloApelacionDemo(ACTA_FALLO_ABSOLUTORIO);
        assertActaInicialFalloApelacionDemo(ACTA_APELACION_PORTAL);
        assertActaInicialFalloApelacionDemo(ACTA_APELACION_PRESENCIAL);
        assertActaInicialFalloApelacionDemo(ACTA_VENCIMIENTO_PLAZO);
        assertActaInicialFalloApelacionDemo(ACTA_APELACION_ABSUELVE);

        for (String actaId : new String[] {
            ACTA_APELACION_PORTAL,
            ACTA_APELACION_PRESENCIAL,
            ACTA_VENCIMIENTO_PLAZO,
            ACTA_APELACION_ABSUELVE
        }) {
            mvc.perform(get(B + "/actas/" + actaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dependenciaDemo").value("TRANSITO"));
        }
    }

    private void assertActaInicialFalloApelacionDemo(String actaId) throws Exception {
        mvc.perform(get(B + "/actas/" + actaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.situacionPago").value("SIN_PAGO"))
                .andExpect(jsonPath("$.montoPagoVoluntario").value(nullValue()))
                .andExpect(jsonPath("$.montoCondena").value(nullValue()))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre").isEmpty())
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[*].fase")
                                .value(containsInAnyOrder("NO_APLICA", "NO_APLICA", "NO_APLICA")));

        mvc.perform(get(B + "/actas/" + actaId + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoDocumento", not(hasItem("FALLO"))))
                .andExpect(jsonPath("$[*].tipoDocumento", not(hasItem("FALLO_ABSOLUTORIO"))))
                .andExpect(jsonPath("$[*].tipoDocumento", not(hasItem("FALLO_CONDENATORIO"))));

        mvc.perform(get(B + "/actas/" + actaId + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("FALLO_EMITIDO"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("FALLO_ABSOLUTORIO_NOTIFICADO"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("FALLO_CONDENATORIO_NOTIFICADO"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("APELACION_PRESENTADA"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("APELACION_RECHAZADA"))))
                .andExpect(jsonPath("$[*].tipoEvento", not(hasItem("APELACION_ACEPTADA_ABSUELVE"))));

        String codigoQr = "QR-" + actaId + "-DEMO";
        mvc.perform(get(B + "/infractor/actas/" + codigoQr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false));
    }
}
