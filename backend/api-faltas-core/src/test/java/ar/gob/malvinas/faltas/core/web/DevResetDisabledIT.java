package ar.gob.malvinas.faltas.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que POST /demo/dev/reset devuelve 404 cuando
 * faltas.demo.reset.enabled no es true (valor por defecto: false).
 *
 * Slice 8F-5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IT 8F-5: POST /demo/dev/reset - deshabilitado por defecto")
class DevResetDisabledIT {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("POST /demo/dev/reset devuelve 404 cuando reset esta deshabilitado")
    void reset_devuelve_404_cuando_deshabilitado() throws Exception {
        mvc.perform(post("/demo/dev/reset"))
                .andExpect(status().isNotFound());
    }
}
