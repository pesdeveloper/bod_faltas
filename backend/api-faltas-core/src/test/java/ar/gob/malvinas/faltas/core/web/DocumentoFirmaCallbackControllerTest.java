package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.RegistrarFirmaDocumentalCommand;
import ar.gob.malvinas.faltas.core.application.result.RegistrarFirmaDocumentalResultado;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFirma;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirma;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;
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

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test unitario de DocumentoController.firmarReal.
 * DocumentoService mockeado, ActorContextHolder controlado por el test.
 * FIX-FALLO-NOTI-01-R1, seccion 6.
 */
@DisplayName("DocumentoController.firmarReal unit test")
class DocumentoFirmaCallbackControllerTest {

    private DocumentoService documentoService;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        documentoService = mock(DocumentoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentoController(documentoService)).build();
    }

    @AfterEach
    void clearContext() {
        ActorContextHolder.clear();
    }

    private FalDocumentoFirma firmaStub(Long docId) {
        return new FalDocumentoFirma(
                1L, docId, (short) 1, 10L, (short) 1, "sub-jwt-actor",
                (short) 1, "Firmante Test", TipoFirma.DIGITAL, EstadoFirma.FIRMADA,
                "hash-001", "ref-ext-001", null, null,
                FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "sub-jwt-actor");
    }

    private Map<String, Object> bodyValido(String refExt) {
        return Map.of(
                "seqFirmaReq", 1,
                "idFirmante", 10,
                "tipoFirma", "DIGITAL",
                "referenciaFirmaExt", refExt,
                "hashDocumento", "hash-001"
        );
    }

    @Test
    @DisplayName("01. Primera firma: service yaExistia=false -> HTTP 201")
    void primerFirma_http201() throws Exception {
        ActorContextHolder.set(new ActorContext("sub-jwt-actor"));
        when(documentoService.registrarFirmaDocumental(any()))
                .thenReturn(new RegistrarFirmaDocumentalResultado(firmaStub(1L), false));

        mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bodyValido("ref-ext-001"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("02. Reintento idempotente: service yaExistia=true -> HTTP 200")
    void reintentoIdempotente_http200() throws Exception {
        ActorContextHolder.set(new ActorContext("sub-jwt-actor"));
        when(documentoService.registrarFirmaDocumental(any()))
                .thenReturn(new RegistrarFirmaDocumentalResultado(firmaStub(1L), true));

        mockMvc.perform(post("/api/faltas/documentos/1/firmar-real")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bodyValido("ref-ext-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("03. idUserFirma enviado al service es el sub del contexto JWT")
    void idUserFirma_esSub_delContextoJwt() throws Exception {
        ActorContextHolder.set(new ActorContext("firma-service-prod"));
        FalDocumentoFirma firma = firmaStub(42L);
        ArgumentCaptor<RegistrarFirmaDocumentalCommand> captor =
                ArgumentCaptor.forClass(RegistrarFirmaDocumentalCommand.class);
        when(documentoService.registrarFirmaDocumental(captor.capture()))
                .thenReturn(new RegistrarFirmaDocumentalResultado(firma, false));

        mockMvc.perform(post("/api/faltas/documentos/42/firmar-real")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bodyValido("ref-ext-042"))))
                .andExpect(status().isCreated());

        assertThat(captor.getValue().idUserFirma()).isEqualTo("firma-service-prod");
    }

    @Test
    @DisplayName("04. RegistrarFirmaDocumentalRequest no contiene campo idUserFirma")
    void request_noContiene_idUserFirma() {
        var components = ar.gob.malvinas.faltas.core.web.dto.RegistrarFirmaDocumentalRequest.class
                .getRecordComponents();
        boolean tieneIdUserFirma = false;
        for (var c : components) {
            if (c.getName().equals("idUserFirma")) tieneIdUserFirma = true;
        }
        assertThat(tieneIdUserFirma).as("Request body no debe exponer idUserFirma").isFalse();
    }
}
