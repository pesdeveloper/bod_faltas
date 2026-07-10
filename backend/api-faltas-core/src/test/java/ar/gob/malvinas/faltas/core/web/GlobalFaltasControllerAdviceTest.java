package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.AcuseDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.web.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios del GlobalFaltasControllerAdvice.
 * No levanta contexto Spring: invoca el advice directamente.
 */
@DisplayName("GlobalFaltasControllerAdvice — contratos HTTP de error")
class GlobalFaltasControllerAdviceTest {

    private final GlobalFaltasControllerAdvice advice = new GlobalFaltasControllerAdvice();

    @Test
    @DisplayName("ActaNoEncontradaException -> 404 ACTA_NO_ENCONTRADA")
    void actaNotFound_returns404() {
        ResponseEntity<ErrorResponse> resp = advice.handleNotFound(new ActaNoEncontradaException(99L));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().codigoError()).isEqualTo("ACTA_NO_ENCONTRADA");
        assertThat(resp.getBody().mensaje()).isNotBlank();
        assertThat(resp.getBody().detalle()).isNull();
        assertThat(resp.getBody().correlacionId()).isNull();
    }

    @Test
    @DisplayName("DocumentoNoEncontradoException -> 404 DOCUMENTO_NO_ENCONTRADO")
    void documentoNotFound_returns404() {
        ResponseEntity<ErrorResponse> resp = advice.handleNotFound(new DocumentoNoEncontradoException("doc-1"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().codigoError()).isEqualTo("DOCUMENTO_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("PrecondicionVioladaException -> 422 PRECONDICION_VIOLADA")
    void precondicionViolada_returns422() {
        ResponseEntity<ErrorResponse> resp =
                advice.handlePrecondicion(new PrecondicionVioladaException("estado invalido"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().codigoError()).isEqualTo("PRECONDICION_VIOLADA");
        assertThat(resp.getBody().mensaje()).isEqualTo("estado invalido");
    }

    @Test
    @DisplayName("DocumentoPlantillaInvalidaException -> 422 PLANTILLA_INVALIDA")
    void plantillaInvalida_returns422() {
        ResponseEntity<ErrorResponse> resp =
                advice.handlePlantillaInvalida(new DocumentoPlantillaInvalidaException("fallo"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().codigoError()).isEqualTo("PLANTILLA_INVALIDA");
    }

    @Test
    @DisplayName("ConcurrenciaConflictoException -> 409 CONFLICTO_CONCURRENCIA")
    void occConflicto_returns409() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleOcc(new ConcurrenciaConflictoException("Acta", 1L, 1, 2));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().codigoError()).isEqualTo("CONFLICTO_CONCURRENCIA");
        // mensaje no expone internos del OCC
        assertThat(resp.getBody().mensaje()).doesNotContain("version almacenada");
    }

    @Test
    @DisplayName("MovimientoPagoDuplicadoException -> 409 MOVIMIENTO_PAGO_DUPLICADO")
    void movimientoDuplicado_returns409() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleMovimientoDuplicado(new MovimientoPagoDuplicadoException("ref-1"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().codigoError()).isEqualTo("MOVIMIENTO_PAGO_DUPLICADO");
    }

    @Test
    @DisplayName("AcuseDuplicadoException -> 409 CONFLICTO_DUPLICADO")
    void acuseDuplicado_returns409() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleDuplicado(new AcuseDuplicadoException(1L, 2L, "POSTAL"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().codigoError()).isEqualTo("CONFLICTO_DUPLICADO");
    }

    @Test
    @DisplayName("QrTokenInvalidoException -> 400 QR_TOKEN_INVALIDO")
    void qrTokenInvalido_returns400() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleQrToken(new QrTokenInvalidoException("token-x"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().codigoError()).isEqualTo("QR_TOKEN_INVALIDO");
        // no exponer token en respuesta
        assertThat(resp.getBody().mensaje()).doesNotContain("token-x");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException -> 400 JSON_INVALIDO")
    void jsonInvalido_returns400() {
        byte[] bytes = "invalid json".getBytes();
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", new MockHttpInputMessage(bytes));
        ResponseEntity<ErrorResponse> resp = advice.handleJsonInvalido(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().codigoError()).isEqualTo("JSON_INVALIDO");
        // no exponer stack trace
        assertThat(resp.getBody().mensaje()).doesNotContain("JSON parse error");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException -> 405 METODO_NO_SOPORTADO")
    void metodoNoSoportado_returns405() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleMetodoNoSoportado(new HttpRequestMethodNotSupportedException("DELETE"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resp.getBody().codigoError()).isEqualTo("METODO_NO_SOPORTADO");
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException -> 415 MEDIA_TYPE_NO_SOPORTADO")
    void mediaTypeNoSoportado_returns415() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleMediaTypeNoSoportado(new HttpMediaTypeNotSupportedException("text/plain"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(resp.getBody().codigoError()).isEqualTo("MEDIA_TYPE_NO_SOPORTADO");
    }

    @Test
    @DisplayName("Exception inesperada -> 500 ERROR_INTERNO sin stack trace ni mensaje interno")
    void excepcionInesperada_returns500Seguro() {
        ResponseEntity<ErrorResponse> resp =
                advice.handleInterno(new RuntimeException("error_interno_secreto"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().codigoError()).isEqualTo("ERROR_INTERNO");
        // no filtrar el mensaje interno al cliente
        assertThat(resp.getBody().mensaje()).doesNotContain("error_interno_secreto");
        assertThat(resp.getBody().detalle()).isNull();
        assertThat(resp.getBody().correlacionId()).isNull();
    }

    @Test
    @DisplayName("codigoError nunca es nulo ni vacio en ningun handler")
    void codigoError_nunca_nulo_ni_vacio() {
        assertCodigoError(advice.handleNotFound(new ActaNoEncontradaException(1L)));
        assertCodigoError(advice.handlePrecondicion(new PrecondicionVioladaException("x")));
        assertCodigoError(advice.handleOcc(new ConcurrenciaConflictoException("E", 1L, 1, 2)));
        assertCodigoError(advice.handleMovimientoDuplicado(new MovimientoPagoDuplicadoException("r")));
        assertCodigoError(advice.handleDuplicado(new AcuseDuplicadoException(1L, 2L, "POSTAL")));
        assertCodigoError(advice.handleQrToken(new QrTokenInvalidoException("t")));
        assertCodigoError(advice.handleInterno(new RuntimeException("x")));
    }

    @Test
    @DisplayName("Mapa NOT_FOUND_CODES cubre todas las excepciones del handler handleNotFound")
    void notFoundCodes_map_not_empty() {
        assertThat(GlobalFaltasControllerAdvice.NOT_FOUND_CODES).isNotEmpty();
        assertThat(GlobalFaltasControllerAdvice.NOT_FOUND_CODES).containsKey(ActaNoEncontradaException.class);
        assertThat(GlobalFaltasControllerAdvice.NOT_FOUND_CODES).containsKey(DocumentoNoEncontradoException.class);
    }

    private void assertCodigoError(ResponseEntity<ErrorResponse> resp) {
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().codigoError()).isNotNull().isNotBlank();
        // codigos en mayusculas A-Z, 0-9, _
        assertThat(resp.getBody().codigoError()).matches("[A-Z0-9_]+");
    }
}
