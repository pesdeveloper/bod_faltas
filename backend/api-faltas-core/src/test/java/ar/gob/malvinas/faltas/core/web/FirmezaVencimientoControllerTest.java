package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de adaptador para FirmezaController.vencerPlazoApelacion
 * (POST /api/faltas/actas/{id}/firmeza/vencer-plazo-apelacion).
 *
 * FirmezaCondenaService mockeado. ActorContextHolder controlado por el test.
 *
 * Verifica: actor del comando = ctx.sub(); observaciones pasan sin modificar;
 * sin contexto JWT -> 401; exito -> 200.
 */
@DisplayName("FirmezaController.vencerPlazoApelacion unit test")
class FirmezaVencimientoControllerTest {

    private static final String ACTOR_SUB = "sub-vencimiento-test";

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
        when(firmezaCondenaService.vencerPlazoApelacion(any())).thenReturn(
                ComandoResultado.de(5L, "10", TipoEventoActa.CONFIR.codigo(), "ok"));

        ArgumentCaptor<VencerPlazoApelacionCommand> captor =
                ArgumentCaptor.forClass(VencerPlazoApelacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/5/firmeza/vencer-plazo-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).vencerPlazoApelacion(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("02. actaId proviene del path, observaciones pasan del body")
    void actaId_y_observaciones_correctos() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.vencerPlazoApelacion(any())).thenReturn(
                ComandoResultado.de(42L, "20", TipoEventoActa.CONFIR.codigo(), "ok"));

        ArgumentCaptor<VencerPlazoApelacionCommand> captor =
                ArgumentCaptor.forClass(VencerPlazoApelacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/42/firmeza/vencer-plazo-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observaciones": "plazo vencido por omision"}
                                """))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).vencerPlazoApelacion(captor.capture());
        VencerPlazoApelacionCommand cmd = captor.getValue();
        assertThat(cmd.actaId()).isEqualTo(42L);
        assertThat(cmd.observaciones()).isEqualTo("plazo vencido por omision");
    }

    @Test
    @DisplayName("03. campo JSON 'actor' no reemplaza el contexto")
    void body_actor_ignorado() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.vencerPlazoApelacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "ok"));

        ArgumentCaptor<VencerPlazoApelacionCommand> captor =
                ArgumentCaptor.forClass(VencerPlazoApelacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/firmeza/vencer-plazo-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actor": "hacker-injected"}
                                """))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).vencerPlazoApelacion(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("04. sin ActorContext (null) -> HTTP 401")
    void sin_contexto_responde_401() throws Exception {
        mockMvc.perform(post("/api/faltas/actas/1/firmeza/vencer-plazo-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("05. exito -> HTTP 200")
    void exito_retorna_200() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.vencerPlazoApelacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "condena firme"));

        mockMvc.perform(post("/api/faltas/actas/1/firmeza/vencer-plazo-apelacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("06. body ausente (no content-type) no falla el endpoint; observaciones == null")
    void sin_body_no_explota() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(firmezaCondenaService.vencerPlazoApelacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.CONFIR.codigo(), "ok"));

        ArgumentCaptor<VencerPlazoApelacionCommand> captor =
                ArgumentCaptor.forClass(VencerPlazoApelacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/firmeza/vencer-plazo-apelacion"))
                .andExpect(status().isOk());

        verify(firmezaCondenaService).vencerPlazoApelacion(captor.capture());
        assertThat(captor.getValue().observaciones()).isNull();
    }
}
