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
 * D1/D2: constatación material temprana solo con bandeja
 * {@code ACTAS_EN_ENRIQUECIMIENTO} y bloque de labrado/enriquecimiento; rechazos
 * 409 no alteran expediente.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ConstatacionMaterialTempranaEtapaIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Test
    void actaPendienteFirma_noAdmiteConstatacionTemprana_409_sinCambioExpediente() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        String id = "ACTA-0003";

        mvc.perform(get(B + "/actas/" + id + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_FIRMA"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "SECUESTRO_RODADO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("La constatación material temprana solo puede registrarse en etapa de labrado o"
                                + " enriquecimiento (D1/D2, bandeja de actas en enriquecimiento)."));

        mvc.perform(get(B + "/actas/" + id + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void luegoDeSolicitudPagoVoluntario_enAnalisis_409NuevaConstatacionTipoDistinto() throws Exception {
        mvc.perform(post(B + "/reset"))
                .andExpect(status().isOk());

        String id = "ACTA-0025";
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "SECUESTRO_RODADO"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-solicitud-pago-voluntario"))
                .andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + id + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"));

        mvc.perform(post(B + "/actas/" + id + "/acciones/registrar-constatacion-material-temprana")
                        .param("tipo", "RETENCION_DOCUMENTAL"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("La constatación material temprana solo puede registrarse en etapa de labrado o"
                                + " enriquecimiento (D1/D2, bandeja de actas en enriquecimiento)."));

        mvc.perform(get(B + "/actas/" + id + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get(B + "/actas/" + id + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(B + "/actas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(1))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("LIBERACION_RODADO"));
    }
}
