package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class BandejaListadoConcurrenteIT {

    private static final String B = "/api/prototipo";
    private static final int ITERACIONES = 25;

    @Autowired
    private MockMvc mvc;

    @Test
    void listadosBandejasYSubBandejas_repetidosEnParalelo_noDevuelven500() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        List<String> rutas = rutasListadoOperativo();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Void>> tareas = new ArrayList<>();
            for (int i = 0; i < ITERACIONES; i++) {
                for (String ruta : rutas) {
                    tareas.add(() -> {
                        mvc.perform(get(B + ruta)).andExpect(status().isOk());
                        return null;
                    });
                }
            }
            List<Future<Void>> resultados = pool.invokeAll(tareas);
            for (Future<Void> f : resultados) {
                f.get();
            }
        }
    }

    @Test
    void listadosBandejasYSubBandejas_repetidosSecuencialmente_noDevuelven500() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        ResultMatcher ok = status().isOk();
        for (int i = 0; i < ITERACIONES; i++) {
            for (String ruta : rutasListadoOperativo()) {
                mvc.perform(get(B + ruta)).andExpect(ok);
            }
        }
    }

    @Test
    void bandejas_resumenOperativo_estableTrasListadosRepetidos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        var baseline = mvc.perform(get(B + "/bandejas"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        for (int i = 0; i < ITERACIONES; i++) {
            for (String ruta : rutasListadoOperativo()) {
                mvc.perform(get(B + ruta)).andExpect(status().isOk());
            }
        }

        var despues = mvc.perform(get(B + "/bandejas"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(baseline, despues, "El resumen operativo no debe mutar por lecturas repetidas");
    }

    private static List<String> rutasListadoOperativo() {
        return List.of(
                "/bandejas",
                "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas",
                "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas?subBandeja=CAPTURA_INICIAL",
                "/bandejas/ACTAS_EN_ENRIQUECIMIENTO/actas?subBandeja=ENRIQUECIMIENTO_GENERAL",
                "/bandejas/PENDIENTE_PREPARACION_DOCUMENTAL/actas",
                "/bandejas/PENDIENTE_PREPARACION_DOCUMENTAL/actas?subBandeja=GENERACION_ACTA_PENDIENTE",
                "/bandejas/PENDIENTE_FIRMA/actas",
                "/bandejas/PENDIENTE_FIRMA/actas?subBandeja=FIRMA_FALLO_CONDENATORIO",
                "/bandejas/PENDIENTE_ANALISIS/actas",
                "/bandejas/PENDIENTE_ANALISIS/actas?subBandeja=ANALISIS_NOTIF_POSITIVA",
                "/bandejas/PENDIENTES_RESOLUCION_REDACCION/actas",
                "/bandejas/PENDIENTE_NOTIFICACION/actas",
                "/bandejas/EN_NOTIFICACION/actas",
                "/bandejas/GESTION_EXTERNA/actas",
                "/bandejas/ARCHIVO/actas",
                "/bandejas/CERRADAS/actas");
    }
}
