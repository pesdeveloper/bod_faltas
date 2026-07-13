package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
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
 * Test de adaptador para NotificacionController.positiva (POST /api/faltas/notificaciones/{id}/positiva).
 * NotificacionService mockeado. ActorContextHolder controlado por el test.
 *
 * Verifica: actor del comando = ctx.sub(); intentoId y observaciones pasan sin modificar;
 * el campo JSON actor no reemplaza el contexto; intentoId ausente -> 400; exito -> 200.
 */
@DisplayName("NotificacionController.positiva unit test")
class NotificacionPositivaControllerTest {

    private static final String ACTOR_SUB = "sub-jwt-actor";

    private NotificacionService notificacionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificacionService = mock(NotificacionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new NotificacionController(notificacionService)).build();
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    @Test
    @DisplayName("01. actor del comando proviene de ActorContextHolder.sub")
    void actor_viene_del_contexto() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.registrarPositiva(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTPOS.codigo(), "ok"));

        ArgumentCaptor<RegistrarNotificacionPositivaCommand> captor =
                ArgumentCaptor.forClass(RegistrarNotificacionPositivaCommand.class);

        mockMvc.perform(post("/api/faltas/notificaciones/10/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"intentoId": 5}
                                """))
                .andExpect(status().isOk());

        verify(notificacionService).registrarPositiva(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("02. intentoId y observaciones llegan sin modificar; idNotificacion del path")
    void intento_y_observaciones_pasan_sin_modificar() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.registrarPositiva(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTPOS.codigo(), "ok"));

        ArgumentCaptor<RegistrarNotificacionPositivaCommand> captor =
                ArgumentCaptor.forClass(RegistrarNotificacionPositivaCommand.class);

        mockMvc.perform(post("/api/faltas/notificaciones/42/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"intentoId": 7, "observaciones": "acuse recibido en persona"}
                                """))
                .andExpect(status().isOk());

        verify(notificacionService).registrarPositiva(captor.capture());
        RegistrarNotificacionPositivaCommand cmd = captor.getValue();
        assertThat(cmd.idNotificacion()).isEqualTo(42L);
        assertThat(cmd.intentoId()).isEqualTo(7L);
        assertThat(cmd.observaciones()).isEqualTo("acuse recibido en persona");
    }

    @Test
    @DisplayName("03. campo JSON 'actor' no reemplaza el contexto")
    void body_actor_ignorado() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.registrarPositiva(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTPOS.codigo(), "ok"));

        ArgumentCaptor<RegistrarNotificacionPositivaCommand> captor =
                ArgumentCaptor.forClass(RegistrarNotificacionPositivaCommand.class);

        mockMvc.perform(post("/api/faltas/notificaciones/10/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"intentoId": 5, "actor": "hacker-injected"}
                                """))
                .andExpect(status().isOk());

        verify(notificacionService).registrarPositiva(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("04. intentoId ausente -> HTTP 400")
    void intento_ausente_retorna_400() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));

        mockMvc.perform(post("/api/faltas/notificaciones/10/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"observaciones": "sin intento"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("05. exitoso -> HTTP 200")
    void exito_retorna_200() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.registrarPositiva(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTPOS.codigo(), "positiva registrada"));

        mockMvc.perform(post("/api/faltas/notificaciones/10/positiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"intentoId": 5}
                                """))
                .andExpect(status().isOk());
    }
}
