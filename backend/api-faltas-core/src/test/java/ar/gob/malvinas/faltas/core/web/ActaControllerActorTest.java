package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que ActaController exige actor autenticado (JWT sub) en las tres
 * operaciones humanas: labrar, completarCaptura y enriquecer.
 *
 * Sin contexto: HTTP 401, servicio no invocado.
 * Con contexto: actor propagado exactamente desde ActorContextHolder.
 * idInspector del payload NO se usa como actor.
 * No existe fallback UNKNOWN/SYS.
 */
@DisplayName("ActaController — exigencia de actor JWT (F-actor)")
class ActaControllerActorTest {

    private static final String ACTOR_SUB = "sub-acta-actor-test";

    private ActaService actaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        actaService = mock(ActaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ActaController(actaService, mock(ActaEventoRepository.class))).build();
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    // -------------------------------------------------------------------------
    // labrar
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/faltas/actas/labrar")
    class LabrarTests {

        private static final String URL = "/api/faltas/actas/labrar";
        private static final String BODY = """
                {
                  "tipoActa": "TRANSITO",
                  "idDependencia": "1",
                  "idInspector": "42",
                  "infractorDocumento": "12345678",
                  "resultadoFirmaInfractor": "SE_NIEGA_A_FIRMAR"
                }
                """;

        @Test
        @DisplayName("01. sin ActorContext (null) -> HTTP 401; servicio no invocado")
        void labrar_sin_contexto_retorna_401() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(actaService);
        }

        @Test
        @DisplayName("02. con ActorContext -> servicio invocado; respuesta 201")
        void labrar_con_contexto_invoca_servicio() throws Exception {
            ActorContextHolder.set(new ActorContext(ACTOR_SUB));
            when(actaService.labrar(any())).thenReturn(
                    ComandoResultado.de(1L, "1", TipoEventoActa.ACTLAB.codigo(), "ok"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().isCreated());
            verify(actaService).labrar(any());
        }

        @Test
        @DisplayName("03. idInspector del body no es el actor — actor viene del JWT sub")
        void labrar_idInspector_no_es_actor() throws Exception {
            ActorContextHolder.set(new ActorContext(ACTOR_SUB));
            when(actaService.labrar(any())).thenReturn(
                    ComandoResultado.de(1L, "1", TipoEventoActa.ACTLAB.codigo(), "ok"));

            ArgumentCaptor<ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand> captor =
                    ArgumentCaptor.forClass(ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand.class);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(BODY))
                    .andExpect(status().isCreated());

            verify(actaService).labrar(captor.capture());
            assertThat(captor.getValue().idInspector()).isEqualTo(42L);
            assertThat(ActorContextHolder.subOr("fallback")).isEqualTo(ACTOR_SUB);
        }
    }

    // -------------------------------------------------------------------------
    // completarCaptura
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/faltas/actas/{id}/completar-captura")
    class CompletarCapturaTests {

        private static final String URL_TPL = "/api/faltas/actas/{id}/completar-captura";

        @Test
        @DisplayName("04. sin ActorContext (null) -> HTTP 401; servicio no invocado")
        void completar_sin_contexto_retorna_401() throws Exception {
            mockMvc.perform(post(URL_TPL, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(actaService);
        }

        @Test
        @DisplayName("05. con ActorContext -> servicio invocado; respuesta 200")
        void completar_con_contexto_invoca_servicio() throws Exception {
            ActorContextHolder.set(new ActorContext(ACTOR_SUB));
            when(actaService.completarCaptura(any())).thenReturn(
                    ComandoResultado.de(1L, "1", TipoEventoActa.ACTCAP.codigo(), "ok"));

            mockMvc.perform(post(URL_TPL, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
            verify(actaService).completarCaptura(any());
        }
    }

    // -------------------------------------------------------------------------
    // enriquecer
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/faltas/actas/{id}/enriquecer")
    class EnriquecerTests {

        private static final String URL_TPL = "/api/faltas/actas/{id}/enriquecer";

        @Test
        @DisplayName("06. sin ActorContext (null) -> HTTP 401; servicio no invocado")
        void enriquecer_sin_contexto_retorna_401() throws Exception {
            mockMvc.perform(post(URL_TPL, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(actaService);
        }

        @Test
        @DisplayName("07. con ActorContext -> servicio invocado; respuesta 200")
        void enriquecer_con_contexto_invoca_servicio() throws Exception {
            ActorContextHolder.set(new ActorContext(ACTOR_SUB));
            when(actaService.enriquecer(any())).thenReturn(
                    ComandoResultado.de(1L, "1", TipoEventoActa.ACTENR.codigo(), "ok"));

            mockMvc.perform(post(URL_TPL, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
            verify(actaService).enriquecer(any());
        }
    }
}
