package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT de búsqueda global liviana de actas.
 *
 * <p>Cubre los escenarios requeridos del Slice 23B:
 * <ol>
 *   <li>q vacío devuelve 200 OK y lista vacía.</li>
 *   <li>q ACTA-0019 devuelve ACTA-0019.</li>
 *   <li>q A-2026-0019 devuelve ACTA-0019.</li>
 *   <li>q 0019 devuelve ACTA-0019.</li>
 *   <li>q 19 devuelve ACTA-0019 (y puede devolver otras si corresponden).</li>
 *   <li>q 120 devuelve ACTA-0120.</li>
 *   <li>q 30 devuelve ACTA-0030 como primer resultado.</li>
 *   <li>Respuesta incluye bandeja real.</li>
 *   <li>Respuesta incluye dependencia.</li>
 *   <li>Búsqueda es case-insensitive.</li>
 *   <li>Resultados tienen límite máximo.</li>
 *   <li>No muta estado.</li>
 * </ol>
 */
@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class BuscarActasGlobalPrototipoIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    // ── Escenario 1: q vacío ─────────────────────────────────────────────────

    @Test
    void buscar_qVacio_devuelveListaVacia() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscar_qNull_devuelveListaVacia() throws Exception {
        mvc.perform(get(B + "/actas/buscar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscar_qBlanco_devuelveListaVacia() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Escenario 2: exact match por actaId ──────────────────────────────────

    @Test
    void buscar_qActaId_devuelveActaExacta() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }

    // ── Escenario 3: exact match por número visible ──────────────────────────

    @Test
    void buscar_qNumeroVisible_devuelveActaExacta() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "A-2026-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }

    // ── Escenario 4: búsqueda por parte numérica con ceros ───────────────────

    @Test
    void buscar_q0019_devuelveActa0019() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0019")));
    }

    // ── Escenario 5: búsqueda por fragmento numérico corto ───────────────────

    @Test
    void buscar_q19_contieneActa0019() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(ActaBusquedaHelper.MAX_RESULTADOS)))
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0019")));
    }

    // ── Escenario 6: q 120 devuelve ACTA-0120 ────────────────────────────────

    @Test
    void buscar_q120_devuelveActa0120() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0120")));
    }

    // ── Escenario 7: q 30 → ACTA-0030 primero ────────────────────────────────

    @Test
    void buscar_q30_actaPrincipalEsActa0030() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(ActaBusquedaHelper.MAX_RESULTADOS)))
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0030")))
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0030"));
    }

    // ── Escenario 8: respuesta incluye bandeja real ───────────────────────────

    @Test
    void buscar_respuestaIncluye_bandeja() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bandeja").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$[0].bandejaLabel").isNotEmpty());
    }

    // ── Escenario 9: respuesta incluye dependencia ────────────────────────────

    @Test
    void buscar_respuestaIncluye_dependencia() throws Exception {
        // ACTA-0001 tiene dependencia TRANSITO registrada en el mock
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0001"))
                .andExpect(jsonPath("$[0].dependencia").value("TRANSITO"));
    }

    // ── Escenario 10: case-insensitive ───────────────────────────────────────

    @Test
    void buscar_qMinusculas_devuelveIgualQueUppercase() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "acta-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }

    @Test
    void buscar_qMixto_devuelveActaCorrectamente() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "Acta-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }

    // ── Escenario 11: resultados tienen límite máximo ────────────────────────

    @Test
    void buscar_resultadosRespetanLimiteMaximo() throws Exception {
        // q de un solo dígito coincide con muchas actas; aun así el resultado es limitado
        mvc.perform(get(B + "/actas/buscar").param("q", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(ActaBusquedaHelper.MAX_RESULTADOS)));
    }

    // ── Escenario 12: no muta estado ─────────────────────────────────────────

    @Test
    void buscar_noMutaEstado() throws Exception {
        // Verificación previa del estado
        mvc.perform(get(B + "/actas/ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        // Búsqueda
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk());

        // Estado de ACTA-0019 sin cambios
        mvc.perform(get(B + "/actas/ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));
    }

    // ── Campos del DTO de respuesta ───────────────────────────────────────────

    @Test
    void buscar_respuestaContieneCamposMinimos() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").exists())
                .andExpect(jsonPath("$[0].acta").exists())
                .andExpect(jsonPath("$[0].bandeja").exists())
                .andExpect(jsonPath("$[0].bandejaLabel").exists())
                .andExpect(jsonPath("$[0].estadoProceso").exists())
                .andExpect(jsonPath("$[0].situacionAdministrativa").exists())
                .andExpect(jsonPath("$[0].resultadoFinal").exists())
                .andExpect(jsonPath("$[0].situacionPago").exists())
                .andExpect(jsonPath("$[0].situacionPagoCondena").exists());
    }

    // ── Resultados de búsqueda con fragmento de actaId sin guion ────────────

    @Test
    void buscar_qNormalizadoSinGuion_devuelveActa() throws Exception {
        // "ACTA0019" normalizado debe coincidir con ACTA-0019
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }
}
