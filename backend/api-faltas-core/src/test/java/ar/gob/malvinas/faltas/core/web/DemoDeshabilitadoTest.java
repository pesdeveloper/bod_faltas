package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que los endpoints demo responden 404 cuando faltas.demo.enabled es false
 * o esta ausente (valor por defecto: false).
 *
 * Se agrega JWT valido a los endpoints no-health para que la capa de seguridad
 * no bloquee antes del dispatcher. Los beans estan deshabilitados (ConditionalOnProperty),
 * por lo que Spring devuelve 404 (no handler).
 *
 * /demo/health: publico (permitAll), el bean no existe cuando demo=false -> 404 sin JWT.
 * /demo/dev/reset: bean siempre existe, controller retorna 404 cuando resetEnabled=false.
 *
 * Cubre D-12: proteccion de endpoints demo/dev.
 *
 * EP-012 GET /demo/actas/dataset-funcional
 * EP-013 GET /demo/actas/{codigo}
 * EP-014 GET /demo/health
 * EP-032 GET /demo/documentos/graph
 * EP-019 POST /demo/dev/reset (conserva su guard especifico)
 */
@DisplayName("D-12: Endpoints demo deshabilitados por defecto")
class DemoDeshabilitadoTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "faltas.demo.enabled=false")
    @DisplayName("Con faltas.demo.enabled=false explicito")
    class DemoExplicitamenteFalse {

        @Autowired
        MockMvc mvc;

        private String demoToken;

        @BeforeEach
        void setUp() {
            demoToken = JwtTestSupport.token("demo-test");
        }

        @Test
        @DisplayName("EP-012: GET /demo/actas/dataset-funcional devuelve 404 (bean deshabilitado)")
        void dataset_devuelve_404() throws Exception {
            mvc.perform(get("/demo/actas/dataset-funcional")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-013: GET /demo/actas/{codigo} devuelve 404 (bean deshabilitado)")
        void detalle_acta_devuelve_404() throws Exception {
            mvc.perform(get("/demo/actas/ACT-001-LABRADA")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-014: GET /demo/health devuelve 404 (bean deshabilitado, endpoint publico)")
        void health_devuelve_404() throws Exception {
            mvc.perform(get("/demo/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-032: GET /demo/documentos/graph devuelve 404 (bean deshabilitado)")
        void graph_devuelve_404() throws Exception {
            mvc.perform(get("/demo/documentos/graph")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-019: POST /demo/dev/reset devuelve 404 por guard especifico (con JWT)")
        void reset_devuelve_404_por_su_guard_especifico() throws Exception {
            mvc.perform(post("/demo/dev/reset")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @AutoConfigureMockMvc
    @DisplayName("Con propiedad ausente (default false)")
    class DemoPropiedadAusente {

        @Autowired
        MockMvc mvc;

        private String demoToken;

        @BeforeEach
        void setUp() {
            demoToken = JwtTestSupport.token("demo-test");
        }

        @Test
        @DisplayName("EP-012: GET /demo/actas/dataset-funcional devuelve 404 sin propiedad (bean deshabilitado)")
        void dataset_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/actas/dataset-funcional")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-013: GET /demo/actas/{codigo} devuelve 404 sin propiedad (bean deshabilitado)")
        void detalle_acta_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/actas/ACT-001-LABRADA")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-014: GET /demo/health devuelve 404 sin propiedad (publico, bean deshabilitado)")
        void health_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-032: GET /demo/documentos/graph devuelve 404 sin propiedad (bean deshabilitado)")
        void graph_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/documentos/graph")
                            .header("Authorization", "Bearer " + demoToken))
                    .andExpect(status().isNotFound());
        }
    }
}
