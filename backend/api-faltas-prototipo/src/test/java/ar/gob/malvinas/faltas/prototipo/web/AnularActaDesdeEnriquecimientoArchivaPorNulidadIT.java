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
 * Regla operativa: {@code POST /actas/{id}/acciones/anular-acta} mueve el
 * acta desde {@code ACTAS_EN_ENRIQUECIMIENTO} a la macro-bandeja
 * {@code ARCHIVO} con {@code motivoArchivo=NULIDAD} y
 * {@code permiteReingreso=true}.
 *
 * <p>Las acciones internas ({@code enviarANotificacion}, {@code anularActa},
 * {@code pagoVoluntario}, {@code cierre}) quedan apagadas. La única acción
 * disponible es {@code archivoReingreso=true}. Se registra evento
 * {@code ACTA_ANULADA_POR_NULIDAD}.
 *
 * <p>Caso: {@code ACTA-0009} (enriquecimiento sin bloqueantes materiales).
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class AnularActaDesdeEnriquecimientoArchivaPorNulidadIT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0009";

    @Autowired
    private MockMvc mvc;

    @Test
    void anularActa_desdeEnriquecimiento_archivaPorNulidad() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ACTAS_EN_ENRIQUECIMIENTO"))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(true));

        mvc.perform(post(B + "/actas/" + ID + "/acciones/anular-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.actaId").value(ID))
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("ARCHIVADA_OPERATIVA"))
                .andExpect(jsonPath("$.motivoArchivo").value("NULIDAD"));

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("ARCHIVADA_OPERATIVA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ARCHIVO"))
                .andExpect(jsonPath("$.motivoArchivo").value("NULIDAD"))
                .andExpect(jsonPath("$.permiteReingreso").value(true))
                .andExpect(jsonPath("$.accionPendiente").isEmpty())
                .andExpect(jsonPath("$.accionesUi.archivoReingreso").value(true))
                .andExpect(jsonPath("$.accionesUi.enviarANotificacion").value(false))
                .andExpect(jsonPath("$.accionesUi.anularActa").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.resolucionBloqueante").value(false));

        mvc.perform(get(B + "/actas/" + ID + "/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'ACTA_ANULADA_POR_NULIDAD')]").isNotEmpty());
    }
}
