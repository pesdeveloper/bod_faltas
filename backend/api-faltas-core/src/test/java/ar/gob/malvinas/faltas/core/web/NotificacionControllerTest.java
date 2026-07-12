package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test unitario para NotificacionController.enviar.
 * NotificacionService mockeado. ActorContextHolder controlado por el test.
 *
 * Verifica: actor del comando = ctx.sub(), request no contiene actor,
 * canal llega como CanalNotificacion, destinoDigital y referenciaExterna
 * pasan sin modificar, HTTP 201.
 */
@DisplayName("NotificacionController.enviar unit test")
class NotificacionControllerTest {

    private static final String ACTOR_SUB = "sub-jwt-actor";

    private NotificacionService notificacionService;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

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
    @DisplayName("01. actor del comando proviene de ActorContextHolder.sub, no del body")
    void actor_viene_del_contexto_no_del_body() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.enviarNotificacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTENV.codigo(), "ok"));

        ArgumentCaptor<EnviarNotificacionCommand> captor =
                ArgumentCaptor.forClass(EnviarNotificacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDocumento": 5, "canal": "PRESENCIAL"}
                                """))
                .andExpect(status().isCreated());

        verify(notificacionService).enviarNotificacion(captor.capture());
        EnviarNotificacionCommand cmd = captor.getValue();
        assertThat(cmd.actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("02. canal llega como CanalNotificacion (no como String libre)")
    void canal_es_enum_canonico() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.enviarNotificacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTENV.codigo(), "ok"));

        ArgumentCaptor<EnviarNotificacionCommand> captor =
                ArgumentCaptor.forClass(EnviarNotificacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDocumento": 5, "canal": "EMAIL", "destinoDigital": "user@test.com"}
                                """))
                .andExpect(status().isCreated());

        verify(notificacionService).enviarNotificacion(captor.capture());
        EnviarNotificacionCommand cmd = captor.getValue();
        assertThat(cmd.canal()).isEqualTo(CanalNotificacion.EMAIL);
    }

    @Test
    @DisplayName("03. destinoDigital y referenciaExterna llegan sin modificar")
    void destino_y_refExt_pasan_sin_modificar() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.enviarNotificacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTENV.codigo(), "ok"));

        ArgumentCaptor<EnviarNotificacionCommand> captor =
                ArgumentCaptor.forClass(EnviarNotificacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idDocumento": 5,
                                  "canal": "EMAIL",
                                  "destinoDigital": "correo@ejemplo.com",
                                  "referenciaExterna": "REF-ABC-123"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(notificacionService).enviarNotificacion(captor.capture());
        EnviarNotificacionCommand cmd = captor.getValue();
        assertThat(cmd.destinoDigital()).isEqualTo("correo@ejemplo.com");
        assertThat(cmd.referenciaExterna()).isEqualTo("REF-ABC-123");
    }

    @Test
    @DisplayName("04. body con campo 'actor' es ignorado (actor viene del contexto)")
    void body_actor_ignorado() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.enviarNotificacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTENV.codigo(), "ok"));

        ArgumentCaptor<EnviarNotificacionCommand> captor =
                ArgumentCaptor.forClass(EnviarNotificacionCommand.class);

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idDocumento": 5,
                                  "canal": "PRESENCIAL",
                                  "actor": "hacker-injected"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(notificacionService).enviarNotificacion(captor.capture());
        // actor debe venir del contexto, no del body
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("05. exitoso -> HTTP 201")
    void exito_retorna_201() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(notificacionService.enviarNotificacion(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.NOTENV.codigo(), "enviado"));

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDocumento": 5, "canal": "PRESENCIAL"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("06. canal JSON invalido -> 400 (Jackson rechaza enum desconocido)")
    void canal_invalido_retorna_400() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));

        mockMvc.perform(post("/api/faltas/actas/1/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idDocumento": 5, "canal": "CANAL_INEXISTENTE"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
