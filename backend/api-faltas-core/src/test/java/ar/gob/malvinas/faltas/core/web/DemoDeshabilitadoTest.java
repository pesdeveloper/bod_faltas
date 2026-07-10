package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que los endpoints demo responden 404 cuando faltas.demo.enabled es false
 * o esta ausente (valor por defecto: false).
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
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "faltas.demo.enabled=false")
    @DisplayName("Con faltas.demo.enabled=false explicito")
    class DemoExplicitamenteFalse {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("EP-012: GET /demo/actas/dataset-funcional devuelve 404")
        void dataset_devuelve_404() throws Exception {
            mvc.perform(get("/demo/actas/dataset-funcional"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-013: GET /demo/actas/{codigo} devuelve 404")
        void detalle_acta_devuelve_404() throws Exception {
            mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-014: GET /demo/health devuelve 404")
        void health_devuelve_404() throws Exception {
            mvc.perform(get("/demo/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-032: GET /demo/documentos/graph devuelve 404")
        void graph_devuelve_404() throws Exception {
            mvc.perform(get("/demo/documentos/graph"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-019: POST /demo/dev/reset sigue devolviendo 404 por su propio guard")
        void reset_devuelve_404_por_su_guard_especifico() throws Exception {
            mvc.perform(post("/demo/dev/reset"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("Con propiedad ausente (default false)")
    class DemoPropiedadAusente {

        @Autowired
        MockMvc mvc;

        @Test
        @DisplayName("EP-012: GET /demo/actas/dataset-funcional devuelve 404 sin propiedad")
        void dataset_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/actas/dataset-funcional"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-013: GET /demo/actas/{codigo} devuelve 404 sin propiedad")
        void detalle_acta_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/actas/ACT-001-LABRADA"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-014: GET /demo/health devuelve 404 sin propiedad")
        void health_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/health"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("EP-032: GET /demo/documentos/graph devuelve 404 sin propiedad")
        void graph_devuelve_404_sin_propiedad() throws Exception {
            mvc.perform(get("/demo/documentos/graph"))
                    .andExpect(status().isNotFound());
        }
    }
}
