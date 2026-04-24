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
 * ACTA-0026: medida preventiva nacida en trámite (contravención), con ancla
 * distinta a constatación temprana D1/D2, sin {@code ActaTransitoMock}.
 * Recorrido: cierre bloqueado con resultado compatible, resolutorio y
 * cumplimiento material.
 *
 * <p>Semilla: {@code MockDataFactory#cargarActa0026MedidaPreventivaPosteriorContravencionDemo}.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class MedidaPreventivaPosteriorContravencionIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0026";
    /** Fase sin resolutorio, medida por trazabilidad posterior (no "tempranas"). */
    private static final String LECTURA_SIN_RESOLUTORIO_MEDIDA_POSTERIOR =
            "Existe una medida preventiva activa generada en trámite, posterior al labrado. Requiere"
                    + " resolutorio documental y cumplimiento material efectivo antes del cierre (mock).";
    private static final String LECTURA_SIN_CUMPLIMIENTO =
            "Existen resolutorios documentales registrados, pero todavía falta cumplimiento material"
                    + " efectivo.";

    @Autowired
    private MockMvc mvc;

    @Test
    void acta0026_medidaPosteriorBloqueaCierreHastaCumplimiento() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("PAGO_CONFIRMADO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").isEmpty())
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].clave").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("NO_APLICA"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-medida-preventiva-posterior"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoDocumento").value("MEDIDA_PREVENTIVA"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(
                        jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(
                        jsonPath("$.hechosMateriales.lecturaOperativa")
                                .value(LECTURA_SIN_RESOLUTORIO_MEDIDA_POSTERIOR))
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("SITUACION_PENDIENTE_DE_RESOLUTORIO"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"));

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-medida-preventiva-posterior"))
                .andExpect(status().isConflict());

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_SIN_CUMPLIMIENTO))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"));

        mvc.perform(
                        post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                .param("tipo", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").isEmpty())
                .andExpect(jsonPath("$.hechosMateriales.ejes[0].fase").value("CUMPLIMIENTO_MATERIAL_VERIFICADO"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }
}
