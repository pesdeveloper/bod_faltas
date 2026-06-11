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
 * Regresión: ACTA-0022 en ACTAS_EN_ENRIQUECIMIENTO no debe tener documentos
 * MEDIDA_PREVENTIVA en estado PENDIENTE_FIRMA.
 *
 * <p>El seed precarga el acta con {@code accionPendiente=COMPLETAR_ENRIQUECIMIENTO}
 * y {@code accionesUi.firmaPendiente=false}. Un documento {@code MEDIDA_PREVENTIVA}
 * en {@code PENDIENTE_FIRMA} contradice ese estado: la UI mostraría firma pendiente
 * cuando el circuito de enriquecimiento no la requiere en este punto.
 *
 * <p>Contratos verificados:
 * <ul>
 *   <li>{@code bandejaActual=ACTAS_EN_ENRIQUECIMIENTO}</li>
 *   <li>{@code accionPendiente=COMPLETAR_ENRIQUECIMIENTO}</li>
 *   <li>{@code accionesUi.firmaPendiente=false}</li>
 *   <li>{@code accionesUi.paralizarActa=true}</li>
 *   <li>Documento MEDIDA_PREVENTIVA existe con estado FIRMADO (no PENDIENTE_FIRMA)</li>
 *   <li>Bloqueantes materiales presentes: LIBERACION_RODADO,
 *       ENTREGA_DOCUMENTACION, LEVANTAMIENTO_MEDIDA_PREVENTIVA</li>
 * </ul>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class Acta0022MedidaPreventivaNoPendienteFirmaIT {

    private static final String B = "/api/prototipo";
    private static final String ACTA = "ACTA-0022";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    @Test
    void acta0022_enriquecimiento_medidaPreventivaNoPendienteFirma() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionPendiente").value("COMPLETAR_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(false))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(true));
    }

    @Test
    void acta0022_documentoMedidaPreventiva_estaFirmadoNoEsPendienteFirma() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoDocumento=='MEDIDA_PREVENTIVA')].estadoDocumento")
                        .value("FIRMADO"))
                .andExpect(jsonPath("$[?(@.tipoDocumento=='MEDIDA_PREVENTIVA' && @.estadoDocumento=='PENDIENTE_FIRMA')]")
                        .isEmpty());
    }

    @Test
    void acta0022_bloqueantesMaterialesPresentes() throws Exception {
        mvc.perform(get(B + "/actas/" + ACTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[?(@=='LEVANTAMIENTO_MEDIDA_PREVENTIVA')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[?(@=='LIBERACION_RODADO')]")
                        .isNotEmpty())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[?(@=='ENTREGA_DOCUMENTACION')]")
                        .isNotEmpty());
    }
}
