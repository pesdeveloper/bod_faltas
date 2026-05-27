package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.ApiFaltasPrototipoApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest(classes = ApiFaltasPrototipoApplication.class)
@AutoConfigureMockMvc
class CorreoPostalNotificacionIT {

    private static final String B = "/api/prototipo";
    private static final String MONTO_CONDENA_VALIDO_JSON = "{\"montoCondena\":12345.67}";
    private static final int CANDIDATAS_CORREO_BASE = 5;
    private static final List<String> RESPUESTA_DEMO_BASE = List.of(
            "loteId,notificacionId,resultado,fechaResultado,observacion",
            "LOTE-CORREO-DEMO,NOT-0028-03,POSITIVA,2026-05-25T10:05:00,Fallo absolutorio entregado por correo postal demo",
            "LOTE-CORREO-DEMO,NOT-0004-01,NEGATIVA,2026-05-25T10:10:00,Correo devuelve pieza por domicilio insuficiente",
            "LOTE-CORREO-DEMO,NOT-0030-03,VENCIDA,2026-05-25T10:15:00,Sin constancia de entrega dentro del plazo demo");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void restaurarRespuestaDemoBase() throws Exception {
        escribirRespuestaDemo(RESPUESTA_DEMO_BASE);
    }

    @Test
    void buscarTrazabilidadCorreo_porNumeroActaOActaId() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

        mvc.perform(get(B + "/notificaciones/correo/trazabilidad").param("acta", "A-2026-0030"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0030-03')].loteId")
                        .value(hasItem(loteId)))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0030-03')].estadoLote")
                        .value(hasItem("PENDIENTE_RESPUESTA")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0030-03')].tipo")
                        .value(hasItem("FALLO_CONDENATORIO")));

        mvc.perform(get(B + "/notificaciones/correo/trazabilidad").param("acta", "ACTA-0030"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0030-03')].acta")
                        .value(hasItem("A-2026-0030")));
    }

    @Test
    void generarLoteCorreo_porNotificacionIds_soloIncluyeSeleccionadas() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificacionIds\":[\"NOT-0030-03\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.notificaciones[0].notificacionId").value("NOT-0030-03"));

        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].estado")
                        .value(hasItem("LISTA_PARA_ENVIO")));
    }

    @Test
    void procesarRespuestaCorreo_porLoteId_marcaLoteProcesado() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificacionIds\":[\"NOT-0004-01\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo").param("loteId", loteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.errores").value(0));

        mvc.perform(get(B + "/notificaciones/correo/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].estado")
                        .value(hasItem("PROCESADO")))
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].positivas").value(hasItem(1)));
    }

    @Test
    void procesarRespuestaCorreo_loteYaProcesado_devuelveConflict() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo").param("loteId", loteId))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo").param("loteId", loteId))
                .andExpect(status().isConflict());
    }

    @Test
    void generarLoteCorreo_porTipo_soloIncluyeEseTipo() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        mvc.perform(post(B + "/notificaciones/correo/lotes/generar").param("tipo", "FALLO_CONDENATORIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.notificaciones[0].tipo").value("FALLO_CONDENATORIO"));

        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].estado")
                        .value(hasItem("LISTA_PARA_ENVIO")));
    }

    @Test
    void listarNotificacionesListasParaLote_yLotesGenerados_reflejanEstadoDemo() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(CANDIDATAS_CORREO_BASE))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].canal")
                        .value(hasItem("CORREO_POSTAL")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].estado")
                        .value(hasItem("LISTA_PARA_ENVIO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].resultado")
                        .value(hasItem("SIN_RESULTADO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0037-01')].tipo")
                        .value(hasItem("FALLO_CONDENATORIO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0039-01')].tipo")
                        .value(hasItem("FALLO_CONDENATORIO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0040-01')].tipo")
                        .value(hasItem("FALLO_ABSOLUTORIO")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0042-01')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0043-01')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0044-01')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0045-01')]").doesNotExist());

        mvc.perform(get(B + "/notificaciones/correo/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificacionIds\":[\"NOT-0004-01\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

        mvc.perform(get(B + "/notificaciones/correo/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].estado")
                        .value(hasItem("PENDIENTE_RESPUESTA")))
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].tiposIncluidos[0]")
                        .value(hasItem("ACTA_INFRACCION")))
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].notificaciones[0].notificacionId")
                        .value(hasItem("NOT-0004-01")))
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].notificaciones[0].estadoNotificacion")
                        .value(hasItem("ENVIADA")));

        mvc.perform(post(B + "/notificaciones/correo/lotes/" + loteId + "/anular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));

        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0004-01')].estado")
                        .value(hasItem("LISTA_PARA_ENVIO")));

        mvc.perform(get(B + "/notificaciones/correo/lotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.loteId == '" + loteId + "')].estado")
                        .value(hasItem("ANULADO")));
    }

    @Test
    void generarLoteCorreo_tomaNotificacionesPostalesListasYCreaCsv() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loteId").exists())
                .andExpect(jsonPath("$.cantidad").value(CANDIDATAS_CORREO_BASE))
                .andExpect(jsonPath("$.nombreArchivo").exists())
                .andExpect(jsonPath("$.rutaArchivo").exists())
                .andExpect(jsonPath("$.notificaciones[?(@.notificacionId == 'NOT-0004-01')]").exists())
                .andReturn();

        JsonNode lote = objectMapper.readTree(loteResult.getResponse().getContentAsString());
        Path archivo = Path.of(lote.get("rutaArchivo").asText());
        assertTrue(Files.exists(archivo));
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains(
                "loteId,notificacionId,actaId,acta,tipo,canal,destinatario,domicilio,referenciaExterna,fechaGeneracion"));
        assertTrue(contenido.contains("NOT-0004-01"));
        assertTrue(contenido.contains("ACTA_INFRACCION"));
        assertTrue(contenido.contains("CORREO_POSTAL"));

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bandejaActual").value("EN_NOTIFICACION"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].loteId")
                        .value(hasItem(lote.get("loteId").asText())))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].referenciaExterna").exists())
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].estado").value(hasItem("ENVIADA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].resultado")
                        .value(hasItem("SIN_RESULTADO")));
    }

    @Test
    void procesarRespuestaCorreo_aplicaReglasJuridicasSoloParaFallosPositivos() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0027", "DOC-0027-02");
        prepararFalloAbsolutorioFirmado("ACTA-0028", "DOC-0028-02");
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notificacionIds":["NOT-0027-03","NOT-0028-03","NOT-0004-01","NOT-0030-03"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(4))
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

        escribirRespuestaDemo(List.of(
                "loteId,notificacionId,resultado,fechaResultado,observacion",
                loteId + ",NOT-0027-03,POSITIVA,2026-05-25T10:00:00,Fallo condenatorio entregado por correo",
                loteId + ",NOT-0028-03,POSITIVA,2026-05-25T10:05:00,Fallo absolutorio entregado por correo",
                loteId + ",NOT-0004-01,NEGATIVA,2026-05-25T10:10:00,Domicilio insuficiente",
                loteId + ",NOT-0030-03,VENCIDA,2026-05-25T10:15:00,Sin constancia dentro del plazo"));

        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.positivas").value(2))
                .andExpect(jsonPath("$.negativas").value(1))
                .andExpect(jsonPath("$.vencidas").value(1))
                .andExpect(jsonPath("$.errores").value(0))
                .andExpect(jsonPath("$.rutaArchivoProcesado").exists());

        mvc.perform(get(B + "/actas/ACTA-0027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0027-03')].estado").value(hasItem("ENTREGADA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0027-03')].resultado").value(hasItem("POSITIVA")))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0027-03')].observacion")
                        .value(hasItem("Fallo condenatorio entregado por correo")));

        mvc.perform(get(B + "/actas/ACTA-0028"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("ABSUELTO"))
                .andExpect(jsonPath("$.cerrabilidad.cerrable").value(true))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0028-03')].resultado").value(hasItem("POSITIVA")));

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionPendiente").value("REINTENTAR_NOTIFICACION"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].resultado").value(hasItem("NEGATIVA")));

        mvc.perform(get(B + "/actas/ACTA-0030"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.accionPendiente").value("EVALUAR_NOTIFICACION_VENCIDA"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0030-03')].resultado").value(hasItem("VENCIDA")));
    }

    @Test
    void procesarRespuestaPositivaDeActaInfraccion_noCondenaNiAbsuelve() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificacionIds\":[\"NOT-0004-01\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();
        escribirRespuestaDemo(List.of(
                "loteId,notificacionId,resultado,fechaResultado,observacion",
                loteId + ",NOT-0004-01,POSITIVA,2026-05-25T10:00:00,Acta entregada por correo"));

        mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.positivas").value(1))
                .andExpect(jsonPath("$.errores").value(0));

        mvc.perform(get(B + "/actas/ACTA-0004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("SIN_RESULTADO_FINAL"))
                .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0004-01')].resultado").value(hasItem("POSITIVA")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("CONDENADO")))
                .andExpect(jsonPath("$.cerrabilidad.resultadoFinal", not("ABSUELTO")));
    }

    @Test
    void procesarRespuestaCorreo_sinArchivoRespuesta_noDevuelve404() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        Path respuesta = respuestaDemoPath();
        Path backup = respuesta.resolveSibling("respuesta-correo-demo.csv.bak-test");
        Files.createDirectories(respuesta.getParent());
        Files.deleteIfExists(backup);
        if (Files.exists(respuesta)) {
            Files.move(respuesta, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.errores").value(1))
                    .andExpect(jsonPath("$.detalleErrores[0]").exists());
        } finally {
            if (Files.exists(backup)) {
                Files.move(backup, respuesta, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    void procesarRespuestaCorreo_sinArchivoRespuesta_generadaDesdeUltimoLote() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());
        prepararFalloCondenatorioFirmado("ACTA-0030", "DOC-0030-02");

        Path respuesta = respuestaDemoPath();
        Path backup = respuesta.resolveSibling("respuesta-correo-demo.csv.bak-test");
        Files.createDirectories(respuesta.getParent());
        Files.deleteIfExists(backup);
        if (Files.exists(respuesta)) {
            Files.move(respuesta, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            MvcResult loteResult = mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"notificacionIds\":[\"NOT-0030-03\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidad").value(1))
                    .andExpect(jsonPath("$.notificaciones[?(@.notificacionId == 'NOT-0030-03')]").exists())
                    .andReturn();
            String loteId = objectMapper.readTree(loteResult.getResponse().getContentAsString()).get("loteId").asText();

            mvc.perform(post(B + "/notificaciones/correo/respuestas/procesar-demo").param("loteId", loteId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.positivas").value(1))
                    .andExpect(jsonPath("$.errores").value(0));

            mvc.perform(get(B + "/actas/ACTA-0030"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cerrabilidad.resultadoFinal").value("CONDENADO"))
                    .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0030-03')].estado").value(hasItem("ENTREGADA")))
                    .andExpect(jsonPath("$.notificaciones[?(@.id == 'NOT-0030-03')].resultado").value(hasItem("POSITIVA")));
        } finally {
            Files.deleteIfExists(respuesta);
            if (Files.exists(backup)) {
                Files.move(backup, respuesta, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    void enviarIndividualCorreo_listaParaEnvio_pasaAEnviadaSinLote() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/NOT-0037-01/enviar-individual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"))
                .andExpect(jsonPath("$.notificacionId").value("NOT-0037-01"))
                .andExpect(jsonPath("$.notificacion.estadoNotificacion").value("ENVIADA"))
                .andExpect(jsonPath("$.notificacion.resultadoNotificacion").value("SIN_RESULTADO"));

        mvc.perform(get(B + "/notificaciones/correo/listas-para-lote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0037-01')]").doesNotExist());

        mvc.perform(get(B + "/notificaciones/correo/trazabilidad").param("acta", "ACTA-0037"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0037-01')].estadoNotificacion")
                        .value(hasItem("ENVIADA")))
                .andExpect(jsonPath("$[?(@.notificacionId == 'NOT-0037-01')].resultadoNotificacion")
                        .value(hasItem("SIN_RESULTADO")));

        mvc.perform(get(B + "/actas/ACTA-0037/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.tipoEvento == 'CORREO_ENVIO_INDIVIDUAL')]").exists());
    }

    @Test
    void enviarIndividualCorreo_noListaParaEnvio_devuelve409() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/NOT-0037-01/enviar-individual"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/NOT-0037-01/enviar-individual"))
                .andExpect(status().isConflict());
    }

    @Test
    void enviarIndividualCorreo_noRompeGenerarLote() throws Exception {
        mvc.perform(post(B + "/reset")).andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/NOT-0037-01/enviar-individual"))
                .andExpect(status().isOk());

        mvc.perform(post(B + "/notificaciones/correo/lotes/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificacionIds\":[\"NOT-0038-01\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.notificaciones[0].notificacionId").value("NOT-0038-01"));
    }

    private void prepararFalloCondenatorioFirmado(String actaId, String documentoId) throws Exception {
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/dictar-fallo-condenatorio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONTO_CONDENA_VALIDO_JSON))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/firmar-documento/" + documentoId))
                .andExpect(status().isOk());
    }

    private void prepararFalloAbsolutorioFirmado(String actaId, String documentoId) throws Exception {
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/dictar-fallo-absolutorio"))
                .andExpect(status().isOk());
        mvc.perform(post(B + "/actas/" + actaId + "/acciones/firmar-documento/" + documentoId))
                .andExpect(status().isOk());
    }

    private void escribirRespuestaDemo(List<String> lineas) throws Exception {
        Path respuesta = respuestaDemoPath();
        Files.createDirectories(respuesta.getParent());
        Files.write(respuesta, lineas, StandardCharsets.UTF_8);
    }

    private Path respuestaDemoPath() {
        return Path.of(
                System.getProperty("user.dir"),
                "var",
                "demo",
                "notificaciones",
                "correo",
                "respuesta",
                "respuesta-correo-demo.csv");
    }
}
