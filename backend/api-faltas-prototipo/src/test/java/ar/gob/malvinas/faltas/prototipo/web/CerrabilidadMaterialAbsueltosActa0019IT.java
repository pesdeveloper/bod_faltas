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
 * Regresion: ACTA-0019 — resultado ABSUELTO con tres bloqueantes materiales activos.
 *
 * <p>Cubre el caso distinto de {@code ACTA-0021} (que tiene {@code PAGO_CONFIRMADO}):
 * {@code ABSUELTO} con retencion de rodado, documentacion y medida preventiva no
 * permite cierre mientras los bloqueantes esten activos. El resultado final compatible
 * con cierre no es suficiente si quedan pendientes materiales sin resolutorio ni
 * cumplimiento material efectivo.
 *
 * <p>Contratos verificados:
 * <ol>
 *   <li>Estado inicial: {@code PENDIENTE_ANALISIS}, {@code PENDIENTE_CIERRE_MATERIAL},
 *       {@code resultadoFinal=ABSUELTO}, {@code cerrable=false}, tres bloqueantes activos.</li>
 *   <li>Documentos materiales preexistentes: anclas MEDIDA_PREVENTIVA, ACTA_RETENCION,
 *       CONSTATACION_RETENCION_DOCUMENTACION.</li>
 *   <li>{@code POST cerrar-acta} rechazado con 409 mientras existan bloqueantes activos.</li>
 *   <li>{@code POST archivar-acta} habilitado cuando {@code cerrable=false}
 *       (cierre y archivo son salidas mutuamente excluyentes desde analisis).</li>
 * </ol>
 */
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CerrabilidadMaterialAbsueltosActa0019IT {

    private static final String B = "/api/prototipo";
    private static final String ID = "ACTA-0019";

    @Autowired
    private MockMvc mvc;

    @Test
    void acta0019_absueltoConBloqueantesActivos_estadoInicialNoEsCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_CIERRE_MATERIAL"))
                .andExpect(jsonPath("$.estaCerrada").value(false))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(false))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre.length()").value(3))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[0]").value("ENTREGA_DOCUMENTACION"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[1]").value("LEVANTAMIENTO_MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$.cerrabilidad.pendientesBloqueantesCierre[2]").value("LIBERACION_RODADO"))
                .andExpect(jsonPath("$.cerrabilidad.motivoNoCerrable").isNotEmpty());
    }

    @Test
    void acta0019_absueltoConBloqueantes_cerrarActaRechazado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    @Test
    void acta0019_absueltoConBloqueantes_documentosMateriales() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/actas/" + ID + "/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tipoDocumento").value("MEDIDA_PREVENTIVA"))
                .andExpect(jsonPath("$[0].estadoDocumento").value("PENDIENTE_FIRMA"))
                .andExpect(jsonPath("$[1].tipoDocumento").value("ACTA_RETENCION"))
                .andExpect(jsonPath("$[1].estadoDocumento").value("ADJUNTO"))
                .andExpect(jsonPath("$[2].tipoDocumento").value("CONSTATACION_RETENCION_DOCUMENTACION"))
                .andExpect(jsonPath("$[2].estadoDocumento").value("ADJUNTO"));
    }

    @Test
    void acta0019_absueltoConBloqueantes_archivoPosiblePorNoCerrable() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/cerrar-acta"))
                .andExpect(status().isConflict());

        mvc.perform(post(B + "/actas/" + ID + "/acciones/archivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("ARCHIVO"));
    }
}
