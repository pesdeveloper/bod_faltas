package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class PortalInfractorDomicilioElectronicoNotificacionIT {

    private static final String B = "/api/prototipo";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PrototipoStore store;

    @Test
    void getPortal_devuelveAccionCuandoHayNotificacionDomicilioElectronicoPendiente() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/infractor/actas/QR-ACTA-0034-DEMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeConfirmarVisualizacionNotificacion").value(true))
                .andExpect(jsonPath("$.domicilioElectronicoVerificado").value(false))
                .andExpect(jsonPath("$.notificacionPortalPendiente.id").value("NOT-0034-01"))
                .andExpect(jsonPath("$.notificacionPortalPendiente.tipo").value("ACTA_INFRACCION"))
                .andExpect(jsonPath("$.notificacionPortalPendiente.canal").value("DOMICILIO_ELECTRONICO"))
                .andExpect(jsonPath("$.notificacionPortalPendiente.estado").value("LISTA_PARA_ENVIO"))
                .andExpect(jsonPath("$.notificacionPortalPendiente.resultado").value("SIN_RESULTADO"));
    }

    @Test
    void confirmarVisualizacionActaInfraccion_marcaNotificacionPositivaSinResultadoFinalJuridico() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0034-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeConfirmarVisualizacionNotificacion").value(false))
                .andExpect(jsonPath("$.domicilioElectronicoVerificado").value(true))
                .andExpect(jsonPath("$.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.notificacionPortalPendiente").doesNotExist());

        mvc.perform(get(B + "/actas/ACTA-0034"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0034-01')].estado").value(hasItem("ENTREGADA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0034-01')].resultado").value(hasItem("POSITIVA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0034-01')].domicilioElectronicoVerificado")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0034-01')].observacion")
                        .value(hasItem("Visualización confirmada desde portal infractor")));
    }

    @Test
    void confirmarVisualizacionFalloCondenatorio_marcaPositivaYPasaACondenadoConApelacionDisponible()
            throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0035-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(true))
                .andExpect(jsonPath("$.domicilioElectronicoVerificado").value(true));

        mvc.perform(get(B + "/actas/ACTA-0035"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0035-01')].estado").value(hasItem("ENTREGADA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0035-01')].resultado").value(hasItem("POSITIVA")));
    }

    @Test
    void confirmarVisualizacionFalloAbsolutorio_marcaPositivaYPasaAAbsuelto() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0036-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.puedePresentarApelacion").value(false))
                .andExpect(jsonPath("$.domicilioElectronicoVerificado").value(true));

        mvc.perform(get(B + "/actas/ACTA-0036"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0036-01')].resultado").value(hasItem("POSITIVA")));
    }

    @Test
    void confirmarVisualizacionSinNotificacionPortalPendiente_devuelve400YNoModifica() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0034-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0034-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(B + "/actas/ACTA-0034"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0034-01')].resultado").value(hasItem("POSITIVA")));
    }

    @Test
    void endpointPortalNoConfirmaCorreoPostalNiNotificadorMunicipal() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0004-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0031-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].resultado")
                        .value(hasItem("SIN_RESULTADO")));
        mvc.perform(get(B + "/actas/ACTA-0031"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0031-01')].resultado")
                        .value(hasItem("SIN_RESULTADO")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("CONDENADO")));
    }

    @Test
    void confirmarVisualizacion_confirmaSoloLaNotificacionMasRelevantePendiente() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        store.getNotificacionesPorActa().get("ACTA-0035").add(ActaNotificacionMock.preparada(
                "NOT-0035-02",
                "ACTA-0035",
                TipoNotificacion.ACTA_INFRACCION,
                CanalNotificacion.DOMICILIO_ELECTRONICO,
                "Demo Portal Condenatorio — acta también pendiente en portal",
                "Demo Portal Condenatorio",
                "Domicilio electrónico demo DNI 42.001.035",
                LocalDateTime.of(2026, 3, 22, 10, 0),
                "PUESTA_DISPOSICION_PORTAL"));

        mvc.perform(post(B + "/infractor/actas/QR-ACTA-0035-DEMO/acciones/confirmar-visualizacion-notificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.puedeConfirmarVisualizacionNotificacion").value(true))
                .andExpect(jsonPath("$.notificacionPortalPendiente.id").value("NOT-0035-02"));

        mvc.perform(get(B + "/actas/ACTA-0035"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0035-01')].resultado").value(hasItem("POSITIVA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0035-02')].resultado")
                        .value(hasItem("SIN_RESULTADO")));
    }
}
