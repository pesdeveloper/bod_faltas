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
 * Regla de archivo: en {@code ARCHIVO} con {@code motivoArchivo=NULIDAD},
 * solo {@code accionesUi.archivoReingreso=true}; todas las demás acciones
 * internas ({@code pagoVoluntario}, {@code cierre}, {@code cumplimientoMaterial},
 * {@code resolucionBloqueante}, {@code enviarANotificacion},
 * {@code anularActa}) deben estar apagadas.
 *
 * <p>Ciclo: anular acta → verificar acciones en ARCHIVO/NULIDAD.
 * Caso: {@code ACTA-0009}.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ArchivoNulidadNoPermiteAccionesInternasNormalesIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0009";

    @Autowired
    private MockMvc mvc;

    @Test
    void archivoNulidad_soloArchivoReingreso_restanteApagado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/anular-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoArchivo").value("NULIDAD"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.motivoArchivo").value("NULIDAD"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(false))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(false))
                .andExpect(jsonPath("$.accionesUi.consentirCondenaYRegistrarPago").value(false));
    }

    @Test
    void archivoNulidad_accionesOperativasRechazan409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/anular-acta"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/anular-acta"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/enviar-a-notificacion"))
                .andExpect(status().isConflict());
    }
}
