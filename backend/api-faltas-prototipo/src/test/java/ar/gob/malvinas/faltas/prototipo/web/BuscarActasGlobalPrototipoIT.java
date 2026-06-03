package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IT de búsqueda global explicable de actas.
 *
 * <p>Cubre los escenarios del Slice 23B (originales) y Slice 23F (nuevos):
 * <ol>
 *   <li>q vacío devuelve 200 OK y lista vacía.</li>
 *   <li>q ACTA-0019 devuelve ACTA-0019.</li>
 *   <li>q A-2026-0019 devuelve ACTA-0019.</li>
 *   <li>q 0019 devuelve ACTA-0019.</li>
 *   <li>q 19 devuelve ACTA-0019.</li>
 *   <li>q 120 devuelve ACTA-0120.</li>
 *   <li>q 30 devuelve ACTA-0030 como primer resultado.</li>
 *   <li>Respuesta incluye bandeja real.</li>
 *   <li>Respuesta incluye dependencia.</li>
 *   <li>Búsqueda es case-insensitive.</li>
 *   <li>Resultados tienen límite máximo.</li>
 *   <li>No muta estado.</li>
 *   <li>23F: q=118 no devuelve ACTA-0014 (doc menor 7 dígitos).</li>
 *   <li>23F: q=0014 devuelve ACTA-0014 por Número de acta.</li>
 *   <li>23F: q de 7+ dígitos devuelve match DOCUMENTO_INFRACTOR.</li>
 *   <li>23F: q de nombre/apellido devuelve match NOMBRE_INFRACTOR.</li>
 *   <li>23F: q alfanumérico no-dominio no busca nombre.</li>
 *   <li>23F: q=AB1 no se considera dominio.</li>
 *   <li>23F: q=1234567 no se considera dominio; busca como doc si coincide.</li>
 *   <li>23F: q=ABC123 devuelve ACTA-0001 con match DOMINIO.</li>
 *   <li>23F: todos los resultados traen matches no vacío.</li>
 *   <li>23F: todos los resultados traen score y scoreLabel.</li>
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
        mvc.perform(get(B + "/actas/buscar").param("q", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(ActaBusquedaHelper.MAX_RESULTADOS)));
    }

    // ── Escenario 12: no muta estado ─────────────────────────────────────────

    @Test
    void buscar_noMutaEstado() throws Exception {
        mvc.perform(get(B + "/actas/ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estaCerrada").value(false));

        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk());

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
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0019"));
    }

    // ── 23F: reglas de falsos positivos ──────────────────────────────────────

    @Test
    void buscar_q118_noDevuelveActa0014_porDocumentoMenorSieteDigitos() throws Exception {
        // ACTA-0014 tiene "DNI 33.770.118" → dígitos "33770118" contiene "118",
        // pero q="118" tiene solo 3 dígitos → puedeBuscarDocumento=false → no match por doc.
        // El número de acta "ACTA-0014" tampoco contiene "118" como substring.
        mvc.perform(get(B + "/actas/buscar").param("q", "118"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", not(hasItem("ACTA-0014"))));
    }

    @Test
    void buscar_q0014_devuelveActa0014_porNumeroActa() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "0014"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0014")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0014')].matches[0].tipo",
                        hasItem("NUMERO_ACTA")));
    }

    @Test
    void buscar_q14_devuelveActa0014_porNumeroActa() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0014")));
    }

    // ── 23F: búsqueda por documento (7+ dígitos) ─────────────────────────────

    @Test
    void buscar_qDocumento7digitos_devuelveMatchDocumentoInfractor() throws Exception {
        // ACTA-0001 tiene "DNI 28.441.992" → dígitos "28441992"
        mvc.perform(get(B + "/actas/buscar").param("q", "28441992"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0001")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0001')].matches[*].tipo",
                        hasItem("DOCUMENTO_INFRACTOR")));
    }

    @Test
    void buscar_qDocumentoConGuiones_devuelveMatchDocumento() throws Exception {
        // Normalización: "28.441.992" → dígitos "28441992"
        mvc.perform(get(B + "/actas/buscar").param("q", "2844199"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0001")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0001')].matches[*].tipo",
                        hasItem("DOCUMENTO_INFRACTOR")));
    }

    @Test
    void buscar_qDocumentoMenorSieteDigitos_noBuscaPorDocumento() throws Exception {
        // q="118" (3 dígitos) → no busca por documento aunque "118" aparezca en algún doc
        mvc.perform(get(B + "/actas/buscar").param("q", "118"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matches[*].tipo", not(hasItem("DOCUMENTO_INFRACTOR"))));
    }

    // ── 23F: búsqueda por nombre/apellido ────────────────────────────────────

    @Test
    void buscar_qNombre_devuelveMatchNombreInfractor() throws Exception {
        // ACTA-0001 tiene infractorNombre="García, Laura"
        mvc.perform(get(B + "/actas/buscar").param("q", "García"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0001")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0001')].matches[*].tipo",
                        hasItem("NOMBRE_INFRACTOR")));
    }

    @Test
    void buscar_qConNumeros_noBuscaPorNombre() throws Exception {
        // q alfanumérico con números: no debe buscar por nombre/apellido
        mvc.perform(get(B + "/actas/buscar").param("q", "Garcia118"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matches[*].tipo", not(hasItem("NOMBRE_INFRACTOR"))));
    }

    // ── 23F: búsqueda por dominio vehicular ──────────────────────────────────

    @Test
    void buscar_qDominioValido_devuelveActa0001ConMatchDominio() throws Exception {
        // ACTA-0001 tiene patente "ABC123" registrada en mock
        mvc.perform(get(B + "/actas/buscar").param("q", "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0001")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0001')].matches[*].tipo",
                        hasItem("DOMINIO")));
    }

    @Test
    void buscar_qDominioConGuion_devuelveActa0001ConMatchDominio() throws Exception {
        // "ABC-123" normalizado = "ABC123" → debe matchear patente "ABC123"
        mvc.perform(get(B + "/actas/buscar").param("q", "ABC-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0001")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0001')].matches[*].tipo",
                        hasItem("DOMINIO")));
    }

    @Test
    void buscar_qAB1_noEsDominioPlausible() throws Exception {
        // "AB1" normalizado tiene 3 chars < 5 → no es dominio plausible
        mvc.perform(get(B + "/actas/buscar").param("q", "AB1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matches[*].tipo", not(hasItem("DOMINIO"))));
    }

    @Test
    void buscar_q1234567_noEsDominio_esDocumentoCandidato() throws Exception {
        // "1234567" → numérico puro → no tiene letras → no es dominio plausible
        // Puede ser doc si coincide con alguno, pero no debe matchear DOMINIO
        mvc.perform(get(B + "/actas/buscar").param("q", "1234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matches[*].tipo", not(hasItem("DOMINIO"))));
    }

    // ── 23G: score numérico con ceros ────────────────────────────────────────

    @Test
    void buscar_q24_devuelveActa0024_conScoreAlta() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0024")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0024')].scoreLabel", hasItem("ALTA")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0024')].score",
                        hasItem(greaterThanOrEqualTo(85))));
    }

    @Test
    void buscar_q0024_devuelveActa0024_conScoreAlta() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "0024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0024")))
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0024')].scoreLabel", hasItem("ALTA")));
    }

    @Test
    void buscar_q30_devuelveActa0030_conScoreAlta() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.actaId == 'ACTA-0030')].scoreLabel", hasItem("ALTA")));
    }

    @Test
    void buscar_q30_actaConCerosDistintos_tieneScoreMenorQueActaPrincipal() throws Exception {
        // ACTA-0130 tiene número operativo 130, distinto a 30 → score menor → no lidera
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actaId").value("ACTA-0030"));
    }

    // ── 23F: score y scoreLabel en todos los resultados ──────────────────────

    @Test
    void buscar_todosLosResultadosTienen_score_y_scoreLabel() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].score", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].scoreLabel", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].scoreLabel",
                        everyItem(anyOf(is("ALTA"), is("MEDIA"), is("BAJA")))));
    }

    // ── 23F: todos los resultados tienen matches no vacío ────────────────────

    @Test
    void buscar_todosLosResultadosTienen_matchesNoVacio() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches").isArray())
                .andExpect(jsonPath("$[0].matches.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void buscar_q30_todosLosResultadosTienen_matchesNoVacio() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].matches", everyItem(not(empty()))));
    }

    // ── 23F: match de número de acta visible ─────────────────────────────────

    @Test
    void buscar_matchNumeroActa_muestraNumeroVisible() throws Exception {
        // El campo "valor" del match NUMERO_ACTA debe ser el número visible (no actaId interno)
        mvc.perform(get(B + "/actas/buscar").param("q", "ACTA-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[?(@.tipo == 'NUMERO_ACTA')].valor",
                        hasItem("A-2026-0019")));
    }

    // ── 23F: q=30 devuelve ACTA-0030 y ACTA-0130 ────────────────────────────

    @Test
    void buscar_q30_devuelveActa0030_y_Acta0130() throws Exception {
        mvc.perform(get(B + "/actas/buscar").param("q", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0030")))
                .andExpect(jsonPath("$[*].actaId", hasItem("ACTA-0130")));
    }
}
