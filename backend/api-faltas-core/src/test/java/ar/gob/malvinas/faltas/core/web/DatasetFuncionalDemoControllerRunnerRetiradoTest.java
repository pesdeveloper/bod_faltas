package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Certifica el retiro de {@code GET /demo/actas/{codigo}} y de
 * {@code CasoUsoFuncionalRunner}/{@code DemoActaMaterializadorService} de {@code src/main}
 * (Wave 4 Parte 3E-B §4).
 *
 * <p>Reemplaza a {@code DemoActaDetalleContractTest} (eliminado junto con el endpoint que
 * certificaba): en vez de verificar el contrato de una ruta retirada, certifica exhaustivamente
 * que la ruta ya no existe para <b>ninguno</b> de los 37 códigos reales del catálogo (no sólo
 * uno o dos, como haría un smoke test) — evidencia de que el retiro es completo y no quedó
 * ningún caso especial residual — y que la única ruta sobreviviente del controller
 * ({@code GET /demo/actas/dataset-funcional}) sigue funcionando exactamente igual, sin depender
 * de ningún composition root InMemory.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = "faltas.demo.enabled=true")
@DisplayName("Wave 4 Parte 3E-B: GET /demo/actas/{codigo} retirado; dataset-funcional sobrevive")
class DatasetFuncionalDemoControllerRunnerRetiradoTest {

    @Autowired
    MockMvc mvc;

    private String demoToken;

    @BeforeEach
    void setUp() {
        demoToken = JwtTestSupport.token("demo-test");
    }

    static Stream<String> todosLosCodigosDelCatalogo() {
        return DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream()
                .map(ActaMockFuncionalDefinicion::codigo);
    }

    @ParameterizedTest(name = "GET /demo/actas/{0} -> 404 (ruta retirada, no 200)")
    @MethodSource("todosLosCodigosDelCatalogo")
    @DisplayName("ninguno de los 37 codigos reales del catalogo resuelve /demo/actas/{codigo}")
    void ningunCodigoRealResuelveLaRutaRetirada(String codigo) throws Exception {
        mvc.perform(get("/demo/actas/" + codigo)
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("codigo inexistente tambien devuelve 404 (misma ausencia de ruta, no 'acta no encontrada')")
    void codigoInexistenteTambienDevuelve404() throws Exception {
        mvc.perform(get("/demo/actas/CODIGO-QUE-NUNCA-EXISTIO")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el catalogo real tiene exactamente 37 codigos (precondicion de la certificacion exhaustiva)")
    void catalogoTiene37Codigos() {
        List<ActaMockFuncionalDefinicion> definiciones = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        assertThat(definiciones).hasSize(37);
        assertThat(definiciones.stream().map(ActaMockFuncionalDefinicion::codigo).distinct().count())
                .as("37 codigos unicos, sin duplicados")
                .isEqualTo(37);
    }

    @Test
    @DisplayName("GET /demo/actas/dataset-funcional sigue disponible: no depende del runner retirado")
    void datasetFuncionalSigueDisponible() throws Exception {
        mvc.perform(get("/demo/actas/dataset-funcional")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActasMock").value(37))
                .andExpect(jsonPath("$.actas").isArray())
                .andExpect(jsonPath("$.actas.length()").value(37));
    }

    @Test
    @DisplayName("GET /demo/actas/dataset-funcional es idempotente y determinista entre llamadas")
    void datasetFuncionalEsDeterministaEntreLlamadas() throws Exception {
        String primera = mvc.perform(get("/demo/actas/dataset-funcional")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String segunda = mvc.perform(get("/demo/actas/dataset-funcional")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(primera).isEqualTo(segunda);
    }
}
