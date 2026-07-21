package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de adaptador para PagoCondenaController.informar
 * (POST /api/faltas/actas/{id}/pago-condena/informar).
 *
 * PagoCondenaService mockeado. ActorContextHolder controlado por el test.
 *
 * Verifica: actor del comando = ctx.sub(); monto/referenciaPago/observaciones del body;
 * campo 'actor' del body ignorado; sin contexto JWT -> 401; body invalido -> 400; exito -> 200;
 * resultado devuelto sin transformacion (idActa, idEntidadAfectada, tipoEvento, descripcion).
 *
 * Slice GAP-CONFORMIDAD-PAGO-CONDENA-INFORMAR-001 (CMD-FALLO-007).
 */
@DisplayName("PagoCondenaController.informar unit test")
class PagoCondenaInformarControllerTest {

    private static final String ACTOR_SUB = "sub-pago-condena-test";
    private static final String URL_BASE = "/api/faltas/actas/{id}/pago-condena/informar";

    private PagoCondenaService pagoCondenaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        pagoCondenaService = mock(PagoCondenaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PagoCondenaController(pagoCondenaService)).build();
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    @Test
    @DisplayName("01. actor del comando proviene de ActorContextHolder.sub")
    void actor_viene_del_contexto() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(pagoCondenaService.informar(any())).thenReturn(
                ComandoResultado.de(5L, "10", TipoEventoActa.PCOINF.codigo(), "informado"));

        ArgumentCaptor<InformarPagoCondenaCommand> captor =
                ArgumentCaptor.forClass(InformarPagoCondenaCommand.class);

        mockMvc.perform(post(URL_BASE, 5)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isOk());

        verify(pagoCondenaService).informar(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("02. actaId proviene del path; monto, referenciaPago y observaciones pasan del body")
    void actaId_y_campos_correctos() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(pagoCondenaService.informar(any())).thenReturn(
                ComandoResultado.de(42L, "20", TipoEventoActa.PCOINF.codigo(), "informado"));

        ArgumentCaptor<InformarPagoCondenaCommand> captor =
                ArgumentCaptor.forClass(InformarPagoCondenaCommand.class);

        mockMvc.perform(post(URL_BASE, 42)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "monto": 5000.00,
                                    "referenciaPago": "BANCO-042",
                                    "observaciones": "Pago via transferencia"
                                }
                                """))
                .andExpect(status().isOk());

        verify(pagoCondenaService).informar(captor.capture());
        InformarPagoCondenaCommand cmd = captor.getValue();
        assertThat(cmd.actaId()).isEqualTo(42L);
        assertThat(cmd.monto()).isEqualByComparingTo("5000.00");
        assertThat(cmd.referenciaPago()).isEqualTo("BANCO-042");
        assertThat(cmd.observaciones()).isEqualTo("Pago via transferencia");
    }

    @Test
    @DisplayName("03. campo JSON 'actor' del body no reemplaza el contexto")
    void body_actor_ignorado() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(pagoCondenaService.informar(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.PCOINF.codigo(), "informado"));

        ArgumentCaptor<InformarPagoCondenaCommand> captor =
                ArgumentCaptor.forClass(InformarPagoCondenaCommand.class);

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "monto": 3000.00,
                                    "referenciaPago": "REF-001",
                                    "actor": "hacker-injected"
                                }
                                """))
                .andExpect(status().isOk());

        verify(pagoCondenaService).informar(captor.capture());
        assertThat(captor.getValue().actor()).isEqualTo(ACTOR_SUB);
    }

    @Test
    @DisplayName("04. sin ActorContext (null) -> HTTP 401; servicio no invocado")
    void sin_contexto_responde_401() throws Exception {
        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pagoCondenaService);
    }

    @Test
    @DisplayName("05. exito -> HTTP 200; resultado devuelto sin transformacion (idActa, idEntidadAfectada, tipoEvento, descripcion)")
    void exito_retorna_200_con_resultado_exacto() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(pagoCondenaService.informar(any())).thenReturn(
                ComandoResultado.de(77L, "pago-uuid-42", TipoEventoActa.PCOINF.codigo(),
                        "Pago de condena informado. Pendiente de confirmacion."));

        mockMvc.perform(post(URL_BASE, 77)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idActa").value(77))
                .andExpect(jsonPath("$.idEntidadAfectada").value("pago-uuid-42"))
                .andExpect(jsonPath("$.tipoEvento").value(TipoEventoActa.PCOINF.codigo()))
                .andExpect(jsonPath("$.descripcion").value("Pago de condena informado. Pendiente de confirmacion."));
    }

    @Test
    @DisplayName("06. monto ausente del body -> HTTP 400 por @NotNull; servicio no invocado")
    void monto_null_responde_400() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pagoCondenaService);
    }

    @Test
    @DisplayName("07. referenciaPago ausente del body -> HTTP 400 por @NotBlank; servicio no invocado")
    void referenciaPago_null_responde_400() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pagoCondenaService);
    }

    @Test
    @DisplayName("08. referenciaPago blank -> HTTP 400 por @NotBlank; servicio no invocado")
    void referenciaPago_blank_responde_400() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "   "}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pagoCondenaService);
    }

    @Test
    @DisplayName("09. observaciones ausentes del body -> null en comando; exito -> 200")
    void observaciones_ausentes_son_null() throws Exception {
        ActorContextHolder.set(new ActorContext(ACTOR_SUB));
        when(pagoCondenaService.informar(any())).thenReturn(
                ComandoResultado.de(1L, "10", TipoEventoActa.PCOINF.codigo(), "informado"));

        ArgumentCaptor<InformarPagoCondenaCommand> captor =
                ArgumentCaptor.forClass(InformarPagoCondenaCommand.class);

        mockMvc.perform(post(URL_BASE, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto": 3000.00, "referenciaPago": "REF-001"}
                                """))
                .andExpect(status().isOk());

        verify(pagoCondenaService).informar(captor.capture());
        assertThat(captor.getValue().observaciones()).isNull();
    }
}
