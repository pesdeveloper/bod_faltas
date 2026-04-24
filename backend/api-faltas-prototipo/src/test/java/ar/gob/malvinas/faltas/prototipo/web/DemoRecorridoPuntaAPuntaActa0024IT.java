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
 * Recorrido demo mínimo ACTA-0024: datos de tránsito (flags mock) con anclas
 * al nacimiento → análisis → pago informado → comprobante → confirmación →
 * resolución + cumplimiento material → cierre. Incluye un GET intermedio con
 * los tres resolutorios en expediente, sin cumplimientos material aún; hechos
 * en {@code RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL}, bloqueantes
 * activos, {@code cerrable} false. Alineado con
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

    @Test
    void recorridoActa0024_pagoYcierreConBloqueosMateriales() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_TEMPRANAS))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[1].ejeBloqueanteCierre")
                                .value("LIBERACION_RODADO"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[2].ejeBloqueanteCierre")
                                .value("ENTREGA_DOCUMENTACION"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "SECUESTRO_RODADO"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-solicitud-pago-voluntario"))
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
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_TEMPRANAS));

        String[] pendientes = {
            "ENTREGA_DOCUMENTACION", "LIBERACION_RODADO", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
        };
        for (String p : pendientes) {
            mvc.perform(
                            post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                    .param("tipo", p))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3))
                .andExpect(jsonPath("$.hechosMateriales.lecturaOperativa").value(LECTURA_SIN_CUMPLIMIENTO))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[1].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[2].fase")
                                .value("RESOLUTORIO_EN_EXPEDIENTE_SIN_HECHO_MATERIAL"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[0].ejeBloqueanteCierre")
                                .value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[1].ejeBloqueanteCierre")
                                .value("LIBERACION_RODADO"))
                .andExpect(
                        jsonPath("$.hechosMateriales.ejes[2].ejeBloqueanteCierre")
                                .value("ENTREGA_DOCUMENTACION"));

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
