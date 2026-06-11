package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class ParalizarActaIT {

    private static final String B = "/api/prototipo";
    private static final String BODY_DOCUMENTAL = """
            {
              "motivo": "ESPERA_DOCUMENTAL",
              "observacion": "Falta documentación respaldatoria"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
    }

    @Test
    void puedeParalizarActaActivaEnAnalisis() throws Exception {
        paralizarOk("ACTA-0016", BODY_DOCUMENTAL)
                .andExpect(jsonPath("$.bandeja").value("PARALIZADAS"))
                .andExpect(jsonPath("$.estadoProceso").value("PARALIZADA"))
                .andExpect(jsonPath("$.situacionAdministrativa").value("PARALIZADA"))
                .andExpect(jsonPath("$.accionPendiente").value("PARALIZACION_ESPERA_DOCUMENTAL"));

        mvc.perform(get(B + "/actas/ACTA-0016"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PARALIZADAS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PARALIZADA"))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("PARALIZADA"))
                .andExpect(jsonPath("$.accionPendiente").value("PARALIZACION_ESPERA_DOCUMENTAL"))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoVoluntario").value(false))
                .andExpect(jsonPath("$.accionesUi.notificacion").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false))
                .andExpect(jsonPath("$.accionesUi.pagoCondena").value(false))
                .andExpect(jsonPath("$.accionesUi.cierre").value(false))
                .andExpect(jsonPath("$.accionesUi.gestionExterna").value(false))
                .andExpect(jsonPath("$.accionesUi.cumplimientoMaterial").value(false))
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(false));

        mvc.perform(get(B + "/actas/ACTA-0016/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'PARALIZACION')]").isNotEmpty());
    }

    @Test
    void reactivarLuegoDeParalizarDevuelveElActaAAnalisisOperable() throws Exception {
        paralizarOk("ACTA-0016", BODY_DOCUMENTAL);

        mvc.perform(post(B + "/actas/ACTA-0016/acciones/reactivar-acta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REACTIVACION"));

        mvc.perform(get(B + "/actas/ACTA-0016"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacionAdministrativaActual").value("ACTIVA"))
                .andExpect(jsonPath("$.bandejaActual").value("PENDIENTE_ANALISIS"))
                .andExpect(jsonPath("$.estadoProcesoActual").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.accionPendiente").value("REVISION_POST_REACTIVACION"))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(true));
    }

    @Test
    void noPuedeParalizarCerrada() throws Exception {
        rechazaSinCambiar("ACTA-0008", "CERRADAS", "CERRADA");
    }

    @Test
    void noPuedeParalizarArchivo() throws Exception {
        rechazaSinCambiar("ACTA-0007", "ARCHIVO", "ARCHIVO");
    }

    @Test
    void noPuedeParalizarGestionExterna() throws Exception {
        rechazaSinCambiar("ACTA-0017", "GESTION_EXTERNA", "GESTION_EXTERNA");
    }

    @Test
    void noPuedeParalizarYaParalizada() throws Exception {
        rechazaSinCambiar("ACTA-0114", "PARALIZADAS", "PARALIZADA");
    }

    @Test
    void puedeParalizarDesdePendienteNotificacion() throws Exception {
        paralizarOk("ACTA-0004", """
                {
                  "motivo": "ESPERA_INFORME_EXTERNO"
                }
                """)
                .andExpect(jsonPath("$.bandeja").value("PARALIZADAS"))
                .andExpect(jsonPath("$.situacionAdministrativa").value("PARALIZADA"))
                .andExpect(jsonPath("$.accionPendiente").value("PARALIZACION_ESPERA_INFORME_EXTERNO"));

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.notificacion").value(false))
                .andExpect(jsonPath("$.accionesUi.falloFondo").value(false))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(false));
    }

    @Test
    void puedeParalizarDesdePendienteFirmaSinPerderDocumentos() throws Exception {
        mvc.perform(get(B + "/actas/ACTA-0003/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'DOC-0003-01')]").exists())
                .andExpect(jsonPath("$[?(@.estadoDocumento == 'PENDIENTE_FIRMA')]").exists());

        paralizarOk("ACTA-0003", """
                {
                  "motivo": "ESPERA_OTRA_DEPENDENCIA"
                }
                """)
                .andExpect(jsonPath("$.bandeja").value("PARALIZADAS"))
                .andExpect(jsonPath("$.situacionAdministrativa").value("PARALIZADA"))
                .andExpect(jsonPath("$.accionPendiente").value("PARALIZACION_ESPERA_OTRA_DEPENDENCIA"));

        mvc.perform(get(B + "/actas/ACTA-0003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accionesUi.firmaPendiente").value(false))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(false));
        mvc.perform(get(B + "/actas/ACTA-0003/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'DOC-0003-01')]").exists())
                .andExpect(jsonPath("$[?(@.estadoDocumento == 'PENDIENTE_FIRMA')]").exists());
    }

    private org.springframework.test.web.servlet.ResultActions paralizarOk(String actaId, String body) throws Exception {
        return mvc.perform(post(B + "/actas/" + actaId + "/acciones/paralizar-acta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("OK"))
                .andExpect(jsonPath("$.actaId").value(actaId));
    }

    private void rechazaSinCambiar(String actaId, String bandeja, String situacion) throws Exception {
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/paralizar-acta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY_DOCUMENTAL))
                .andExpect(status().isConflict());

        mvc.perform(get(B + "/actas/" + actaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value(bandeja))
                .andExpect(jsonPath("$.situacionAdministrativaActual").value(situacion))
                .andExpect(jsonPath("$.accionesUi.paralizarActa").value(false));
    }
}
