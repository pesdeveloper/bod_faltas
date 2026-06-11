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
 * Regla operativa: reingreso desde archivo por nulidad vuelve al acta a
 * {@code ACTAS_EN_ENRIQUECIMIENTO} con {@code accionPendiente=COMPLETAR_ENRIQUECIMIENTO}
 * y {@code motivoArchivo=null}. {@code permiteReingreso} permanece
 * {@code true}. Se registra evento {@code ACTA_REINGRESADA_DESDE_ARCHIVO_NULIDAD}.
 *
 * <p>Ciclo completo: anular desde enriquecimiento → reingresar desde archivo.
 * Caso: {@code ACTA-0009}.
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ReingresarArchivoPorNulidadIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0009";

    @Autowired
    private MockMvc mvc;

    @Test
    void reingresarDesdeArchivoNulidad_vuelveAEnriquecimiento() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/anular-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.motivoArchivo").value("NULIDAD"));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/reingresar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.actaId").value(ID))
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionPendiente").value("COMPLETAR_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.motivoArchivoPrevio").value("NULIDAD"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("EN_CURSO"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.accionPendiente").value("COMPLETAR_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.motivoArchivo").isEmpty())
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(true))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(true))
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'ACTA_REINGRESADA_DESDE_ARCHIVO_NULIDAD')]").isNotEmpty());
    }
}
