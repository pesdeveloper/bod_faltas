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
 * Recorrido demo mínimo ACTA-0024: constatación temprana → análisis → pago
 * informado → comprobante → confirmación → resolución + cumplimiento material
 * → cierre. Debe alinearse con
 * {@code MockDataFactory#cargarActa0024NacimientoCondicionesMaterialesPorConstatacionTempranaDemo}
 * (documentación de secuencia API en el javadoc de ese método).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class DemoRecorridoPuntaAPuntaActa0024IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0024";

    @Autowired
    private MockMvc mvc;

    @Test
    void recorridoActa0024_pagoYcierreConBloqueosMateriales() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "SECUESTRO_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "RETENCION_DOCUMENTAL"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + ID + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "MEDIDA_PREVENTIVA_APLICABLE"))
                .andExpect(status().isOk());

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
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3));

        String[] pendientes = {
            "ENTREGA_DOCUMENTACION", "LIBERACION_RODADO", "LEVANTAMIENTO_MEDIDA_PREVENTIVA"
        };
        for (String p : pendientes) {
            mvc.perform(
                            post(B + "/actas/" + ID + "/acciones/registrar-resolucion-bloqueo-cierre")
                                    .param("tipo", p))
                    .andExpect(status().isOk());
            mvc.perform(
                            post(B + "/actas/" + ID + "/acciones/registrar-cumplimiento-material-bloqueo-cierre")
                                    .param("tipo", p))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(0));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("CERRADAS"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estaCerrada").value(true));
    }
}
