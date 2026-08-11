package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.service.ActaAltaTransientRetryService;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Wave 4 Parte 3F-B, §6: congela por test el contrato del fix de Parte 3F para
 * {@code tipoActa} desconocido en {@code POST /api/faltas/actas/labrar}.
 *
 * <p>Antes de Parte 3F, {@code ActaController} llamaba {@code TipoActa.valueOf(req.tipoActa())}
 * directamente: un valor no reconocido propagaba {@link IllegalArgumentException} sin
 * {@code @ExceptionHandler} dedicado, cayendo en el 500 genérico de
 * {@link GlobalFaltasControllerAdvice}. Este test falla si ese fix
 * ({@code ActaController.parseTipoActa}) se elimina o se revierte.
 *
 * <p>No repite ni modifica el comportamiento deliberado y ya testeado (Parte 3F,
 * {@code ActaLabrarTransaccionalIT} caso 19) de "dependencia inexistente -&gt; excepción
 * genérica": eso queda fuera de esta corrección a propósito.
 */
@DisplayName("ActaController — regresion Wave 4 Parte 3F: tipoActa desconocido -> 422, no 500")
class ActaControllerTipoActaInvalidoRegressionTest {

    private static final String URL = "/api/faltas/actas/labrar";

    private ActaAltaTransientRetryService altaTransientRetryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ActaService actaService = mock(ActaService.class);
        altaTransientRetryService = mock(ActaAltaTransientRetryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ActaController(actaService, altaTransientRetryService, mock(ActaEventoRepository.class),
                                mock(FotografiaEvidenciaService.class)))
                .setControllerAdvice(new GlobalFaltasControllerAdvice())
                .build();
        ActorContextHolder.set(new ActorContext("sub-regresion-tipoacta-test"));
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    @Test
    @DisplayName("tipoActa desconocido -> HTTP 422 PRECONDICION_VIOLADA, sin invocar el orquestador de alta")
    void tipoActaDesconocidoRetorna422SinPersistir() throws Exception {
        String body = """
                {
                  "tipoActa": "TIPO_QUE_NO_EXISTE",
                  "idDependencia": "1",
                  "idInspector": "1",
                  "domicilioHecho": "Dom. de test",
                  "resultadoFirmaInfractor": "SE_NIEGA_A_FIRMAR",
                  "situacionIdentificacionInfractor": "DATOS_INSUFICIENTES"
                }
                """;

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("PRECONDICION_VIOLADA"))
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("TIPO_QUE_NO_EXISTE")));

        verifyNoInteractions(altaTransientRetryService);
    }

    @Test
    @DisplayName("tipoActa valido (TRANSITO) sigue llegando al orquestador de alta (no rompe el camino feliz)")
    void tipoActaValidoSigueInvocandoElOrquestador() throws Exception {
        String body = """
                {
                  "tipoActa": "TRANSITO",
                  "idDependencia": "1",
                  "idInspector": "1",
                  "domicilioHecho": "Dom. de test",
                  "resultadoFirmaInfractor": "SE_NIEGA_A_FIRMAR",
                  "situacionIdentificacionInfractor": "DATOS_INSUFICIENTES",
                  "transito": {"siRetLicencia": false, "siRetVehiculo": false, "siControlAlcoholemia": false}
                }
                """;
        org.mockito.Mockito.when(altaTransientRetryService.labrarConReintento(any())).thenReturn(
                ar.gob.malvinas.faltas.core.application.result.ComandoResultado.de(
                        1L, "1", ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa.ACTLAB.codigo(), "ok"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verify(altaTransientRetryService).labrarConReintento(any());
    }
}
