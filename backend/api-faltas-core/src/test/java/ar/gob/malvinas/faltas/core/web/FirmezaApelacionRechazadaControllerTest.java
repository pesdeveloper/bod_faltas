package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Test de adaptador para FirmezaController.firmezaPorApelacionRechazada
 * (POST /api/faltas/actas/{id}/firmeza/por-apelacion-rechazada).
 *
 * FirmezaCondenaService mockeado. ActorContextHolder controlado por el test.
 *
 * Verifica: actor del comando = ctx.sub(); observaciones pasan sin modificar;
 * sin contexto JWT -> 401; exito -> 200.
 *
 * Slice GAP-CONFORMIDAD-FIRMEZA-APELACION-RECHAZADA-001 (CMD-FALLO-006).
 */
@DisplayName("FirmezaController.firmezaPorApelacionRechazada unit test")
class FirmezaApelacionRechazadaControllerTest {

    private static final String ACTOR_SUB = "sub-apelacion-rechazada-test";
    private static final String URL_BASE = "/api/faltas/actas/{id}/firmeza/por-apelacion-rechazada";

    private FirmezaCondenaService firmezaCondenaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        firmezaCondenaService = mock(FirmezaCondenaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new FirmezaController(firmezaCondenaService)).build();
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    @Test
    @DisplayName("01. actor del comando proviene de ActorContextHolder.sub")
    void actor_viene_del_contexto() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.declararFirmePorApelacionRechazada(any())).thenReturn(
                ComandoResultado.de(5L, "10", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        ArgumentCaptor<DeclararCondenaFirmePorApelacionRechazadaCommand> captor =
                ArgumentCaptor.forClass(DeclararCondenaFirmePorApelacionRechazadaCommand.class);

        mockMvc.perform(post(URL_BASE, 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).declararFirmePorApelacionRechazada(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("02. actaId proviene del path, observaciones pasan del body")
    void actaId_y_observaciones_correctos() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.declararFirmePorApelacionRechazada(any())).thenReturn(
                ComandoResultado.de(42L, "20", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        ArgumentCaptor<DeclararCondenaFirmePorApelacionRechazadaCommand> captor =
                ArgumentCaptor.forClass(DeclararCondenaFirmePorApelacionRechazadaCommand.class);

        mockMvc.perform(post(URL_BASE, 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observaciones": "apelacion rechazada por tribunal"}
                                """))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).declararFirmePorApelacionRechazada(captor.capture());
        DeclararCondenaFirmePorApelacionRechazadaCommand cmd = captor.getValue();
        assertThat(cmd.actaId()).isEqualTo(42L);
        assertThat(cmd.observaciones()).isEqualTo("apelacion rechazada por tribunal");
    }

    @Test
    @DisplayName("03. campo JSON 'actor' no reemplaza el contexto")
    void body_actor_ignorado() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.declararFirmePorApelacionRechazada(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        ArgumentCaptor<DeclararCondenaFirmePorApelacionRechazadaCommand> captor =
                ArgumentCaptor.forClass(DeclararCondenaFirmePorApelacionRechazadaCommand.class);

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actor": "hacker-injected"}
                                """))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).declararFirmePorApelacionRechazada(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("04. sin ActorContext (null) -> HTTP 401; servicio no invocado")
    void sin_contexto_responde_401() throws Exception {
        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(firmezaCondenaService);
    }

    @Test
    @DisplayName("05. exito -> HTTP 200")
    void exito_retorna_200() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.declararFirmePorApelacionRechazada(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("06. body ausente (no content-type) no falla el endpoint; observaciones == null")
    void sin_body_no_explota() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.declararFirmePorApelacionRechazada(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        ArgumentCaptor<DeclararCondenaFirmePorApelacionRechazadaCommand> captor =
                ArgumentCaptor.forClass(DeclararCondenaFirmePorApelacionRechazadaCommand.class);

        mockMvc.perform(post(URL_BASE, 1))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).declararFirmePorApelacionRechazada(captor.capture());
        assertThat(captor.getValue().observaciones()).isNull();
    }
}
