package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
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
 * Regresión: ACTA-0008 cerrada por pago voluntario confirmado debe tener
 * {@code situacionPago=CONFIRMADO} y {@code tipoPago=VOLUNTARIO} consistentes
 * con {@code resultadoFinal=PAGO_CONFIRMADO}.
 *
 * <p>Antes de la corrección, el mock precargaba {@code resultadoFinal=PAGO_CONFIRMADO}
 * pero dejaba {@code situacionPago=SIN_PAGO} (valor por defecto), lo que producía
 * una inconsistencia observable en el detalle del acta.
 *
 * <p>Contratos verificados:
 * <ul>
 *   <li>{@code resultadoFinal=PAGO_CONFIRMADO}</li>
 *   <li>{@code situacionPago=CONFIRMADO}</li>
 *   <li>{@code tipoPago=VOLUNTARIO}</li>
 *   <li>{@code bandejaActual=CERRADAS}</li>
 *   <li>{@code estadoProcesoActual=CERRADA}</li>
 *   <li>{@code situacionAdministrativaActual=CERRADA}</li>
 *   <li>{@code accionesUi.cierre=false} (ya cerrada)</li>
 *   <li>{@code accionesUi.pagoVoluntario=false} (ya confirmado)</li>
 *   <li>{@code accionesUi.paralizarActa=false} (cerrada, no operable)</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class Acta0008CerradaPorPagoConfirmadoIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0008";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    @Test
    void acta0008_cerradaPorPago_situacionPagoConsistente() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.situacionPago").value("CONFIRMADO"))
                .andExpect(jsonPath("$.tipoPago").value("VOLUNTARIO"))
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("CERRADA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("CERRADA"))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(false));
    }
}
