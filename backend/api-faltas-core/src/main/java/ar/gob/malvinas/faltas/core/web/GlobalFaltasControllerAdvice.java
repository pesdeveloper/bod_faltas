package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.domain.exception.ActaArticuloInfringidoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaContravencionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaDocumentoYaExisteException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaMedidaPreventivaAplicadaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaSustanciasNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoAlcoholemiaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaVehiculoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.AcuseDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.BloqueanteMaterialNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoFirmaReqYaMaterializadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaDuplicadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoRedaccionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DomicilioPersonaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.FormaPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.GestionExternaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.InspectorNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCodigoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCorreoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.MedidaPreventivaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.MotivoArchivoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionAcuseNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PagoMovimientoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlanPagoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaContenidoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultAmbiguaException;
import ar.gob.malvinas.faltas.core.domain.exception.PlantillaDefaultNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.domain.exception.ResolucionPagoAnteriorConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.RubroVersionNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.TarifarioNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoModeloNoEncontradoException;
import ar.gob.malvinas.faltas.core.web.error.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advice global que centraliza el mapeo de excepciones de dominio a respuestas HTTP canonicas.
 * Orden: handlers especificos primero; handler generico (Exception) al final.
 * No captura AuthenticationException ni AccessDeniedException.
 */
@RestControllerAdvice
public class GlobalFaltasControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalFaltasControllerAdvice.class);

    /**
     * Mapa explicito excepcion -> codigoError para todos los recursos no encontrado (HTTP 404).
     */
    static final Map<Class<?>, String> NOT_FOUND_CODES;

    static {
        Map<Class<?>, String> m = new LinkedHashMap<>();
        m.put(ActaNoEncontradaException.class,                        "ACTA_NO_ENCONTRADA");
        m.put(ActaTransitoNoEncontradaException.class,                "ACTA_NO_ENCONTRADA");
        m.put(ActaContravencionNoEncontradaException.class,           "ACTA_NO_ENCONTRADA");
        m.put(ActaArticuloInfringidoNoEncontradoException.class,      "ACTA_NO_ENCONTRADA");
        m.put(ActaMedidaPreventivaAplicadaNoEncontradaException.class,"ACTA_NO_ENCONTRADA");
        m.put(ActaSustanciasNoEncontradaException.class,              "ACTA_NO_ENCONTRADA");
        m.put(ActaTransitoAlcoholemiaNoEncontradaException.class,     "ACTA_NO_ENCONTRADA");
        m.put(ActaVehiculoNoEncontradoException.class,                "ACTA_NO_ENCONTRADA");
        m.put(ActaDocumentoNoEncontradaException.class,               "ACTA_NO_ENCONTRADA");
        m.put(DocumentoNoEncontradoException.class,                   "DOCUMENTO_NO_ENCONTRADO");
        m.put(DocumentoPlantillaNoEncontradaException.class,          "PLANTILLA_NO_ENCONTRADA");
        m.put(DocumentoRedaccionNoEncontradaException.class,          "PLANTILLA_NO_ENCONTRADA");
        m.put(PlantillaContenidoNoEncontradaException.class,          "PLANTILLA_NO_ENCONTRADA");
        m.put(PlantillaDefaultNoEncontradaException.class,            "PLANTILLA_NO_ENCONTRADA");
        m.put(PersonaNoEncontradaException.class,                     "PERSONA_NO_ENCONTRADA");
        m.put(DomicilioPersonaNoEncontradoException.class,            "PERSONA_NO_ENCONTRADA");
        m.put(InspectorNoEncontradoException.class,                   "INSPECTOR_NO_ENCONTRADO");
        m.put(FirmanteNoEncontradoException.class,                    "INSPECTOR_NO_ENCONTRADO");
        m.put(DependenciaNoEncontradaException.class,                 "DEPENDENCIA_NO_ENCONTRADA");
        m.put(NormativaNoEncontradaException.class,                   "NORMATIVA_NO_ENCONTRADA");
        m.put(ArticuloNormativaNoEncontradoException.class,           "NORMATIVA_NO_ENCONTRADA");
        m.put(NotificacionNoEncontradaException.class,                "NOTIFICACION_NO_ENCONTRADA");
        m.put(NotificacionAcuseNoEncontradoException.class,           "NOTIFICACION_NO_ENCONTRADA");
        m.put(NotificacionIntentoNoEncontradoException.class,         "NOTIFICACION_NO_ENCONTRADA");
        m.put(FormaPagoNoEncontradaException.class,                   "PAGO_NO_ENCONTRADO");
        m.put(ObligacionPagoNoEncontradaException.class,              "PAGO_NO_ENCONTRADO");
        m.put(PagoMovimientoNoEncontradoException.class,              "PAGO_NO_ENCONTRADO");
        m.put(PlanPagoNoEncontradoException.class,                    "PAGO_NO_ENCONTRADO");
        m.put(ValorizacionNoEncontradaException.class,                "PAGO_NO_ENCONTRADO");
        m.put(BloqueanteMaterialNoEncontradoException.class,          "RECURSO_NO_ENCONTRADO");
        m.put(GestionExternaNoEncontradaException.class,              "RECURSO_NO_ENCONTRADO");
        m.put(LoteCorreoNoEncontradoException.class,                  "RECURSO_NO_ENCONTRADO");
        m.put(MedidaPreventivaNoEncontradaException.class,            "RECURSO_NO_ENCONTRADO");
        m.put(MotivoArchivoNoEncontradoException.class,               "RECURSO_NO_ENCONTRADO");
        m.put(RubroVersionNoEncontradoException.class,                "RECURSO_NO_ENCONTRADO");
        m.put(TarifarioNoEncontradoException.class,                   "RECURSO_NO_ENCONTRADO");
        m.put(VehiculoMarcaNoEncontradaException.class,               "RECURSO_NO_ENCONTRADO");
        m.put(VehiculoModeloNoEncontradoException.class,              "RECURSO_NO_ENCONTRADO");
        NOT_FOUND_CODES = Map.copyOf(m);
    }

    // -------------------------------------------------------------------------
    // 404 NOT FOUND
    // -------------------------------------------------------------------------

    @ExceptionHandler({
        ActaNoEncontradaException.class,
        ActaTransitoNoEncontradaException.class,
        ActaContravencionNoEncontradaException.class,
        ActaArticuloInfringidoNoEncontradoException.class,
        ActaMedidaPreventivaAplicadaNoEncontradaException.class,
        ActaSustanciasNoEncontradaException.class,
        ActaTransitoAlcoholemiaNoEncontradaException.class,
        ActaVehiculoNoEncontradoException.class,
        ActaDocumentoNoEncontradaException.class,
        DocumentoNoEncontradoException.class,
        DocumentoPlantillaNoEncontradaException.class,
        DocumentoRedaccionNoEncontradaException.class,
        PlantillaContenidoNoEncontradaException.class,
        PlantillaDefaultNoEncontradaException.class,
        PersonaNoEncontradaException.class,
        DomicilioPersonaNoEncontradoException.class,
        InspectorNoEncontradoException.class,
        FirmanteNoEncontradoException.class,
        DependenciaNoEncontradaException.class,
        NormativaNoEncontradaException.class,
        ArticuloNormativaNoEncontradoException.class,
        NotificacionNoEncontradaException.class,
        NotificacionAcuseNoEncontradoException.class,
        NotificacionIntentoNoEncontradoException.class,
        FormaPagoNoEncontradaException.class,
        ObligacionPagoNoEncontradaException.class,
        PagoMovimientoNoEncontradoException.class,
        PlanPagoNoEncontradoException.class,
        ValorizacionNoEncontradaException.class,
        BloqueanteMaterialNoEncontradoException.class,
        GestionExternaNoEncontradaException.class,
        LoteCorreoNoEncontradoException.class,
        MedidaPreventivaNoEncontradaException.class,
        MotivoArchivoNoEncontradoException.class,
        RubroVersionNoEncontradoException.class,
        TarifarioNoEncontradoException.class,
        VehiculoMarcaNoEncontradaException.class,
        VehiculoModeloNoEncontradoException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        String codigo = NOT_FOUND_CODES.getOrDefault(ex.getClass(), "RECURSO_NO_ENCONTRADO");
        log.debug("Recurso no encontrado [{}]: {}", codigo, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(codigo, "Recurso no encontrado"));
    }

    // -------------------------------------------------------------------------
    // 422 UNPROCESSABLE ENTITY
    // -------------------------------------------------------------------------

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<ErrorResponse> handlePrecondicion(PrecondicionVioladaException ex) {
        log.debug("Precondicion violada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("PRECONDICION_VIOLADA", ex.getMessage()));
    }

    @ExceptionHandler({
        DocumentoPlantillaInvalidaException.class,
        PlantillaContenidoAmbiguaException.class,
        PlantillaDefaultAmbiguaException.class
    })
    public ResponseEntity<ErrorResponse> handlePlantillaInvalida(RuntimeException ex) {
        log.debug("Plantilla invalida o ambigua: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("PLANTILLA_INVALIDA", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 409 CONFLICT
    // -------------------------------------------------------------------------

    @ExceptionHandler(ConcurrenciaConflictoException.class)
    public ResponseEntity<ErrorResponse> handleOcc(ConcurrenciaConflictoException ex) {
        log.info("Conflicto de concurrencia OCC: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICTO_CONCURRENCIA", "Conflicto de concurrencia: reintente la operacion"));
    }

    @ExceptionHandler(MovimientoPagoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleMovimientoDuplicado(MovimientoPagoDuplicadoException ex) {
        log.debug("Movimiento de pago duplicado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("MOVIMIENTO_PAGO_DUPLICADO", "Movimiento de pago duplicado"));
    }

    @ExceptionHandler(ConciliacionIncompatibleException.class)
    public ResponseEntity<ErrorResponse> handleConciliacion(ConciliacionIncompatibleException ex) {
        log.debug("Conciliacion incompatible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONCILIACION_INCOMPATIBLE", ex.getMessage()));
    }

    @ExceptionHandler(ResolucionPagoAnteriorConflictoException.class)
    public ResponseEntity<ErrorResponse> handleResolucionPagoAnteriorConflicto(ResolucionPagoAnteriorConflictoException ex) {
        log.debug("Conflicto al resolver pago anterior: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("RESOLUCION_PAGO_ANTERIOR_CONFLICTO", ex.getMessage()));
    }

    @ExceptionHandler({
        AcuseDuplicadoException.class,
        ActaDocumentoYaExisteException.class,
        DocumentoFirmaReqYaMaterializadaException.class,
        DocumentoPlantillaDuplicadaException.class,
        LoteCodigoDuplicadoException.class
    })
    public ResponseEntity<ErrorResponse> handleDuplicado(RuntimeException ex) {
        log.debug("Conflicto por duplicado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICTO_DUPLICADO", ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 400 BAD REQUEST
    // -------------------------------------------------------------------------

    @ExceptionHandler(QrTokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleQrToken(QrTokenInvalidoException ex) {
        log.debug("QR token invalido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("QR_TOKEN_INVALIDO", "Token QR invalido o expirado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .sorted((a, b) -> a.getField().compareTo(b.getField()))
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        log.debug("Validacion de request fallida: {}", detalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDACION_REQUEST", "Error de validacion en la solicitud", detalle));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .sorted((a, b) -> a.getPropertyPath().toString().compareTo(b.getPropertyPath().toString()))
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        log.debug("ConstraintViolation: {}", detalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDACION_REQUEST", "Error de validacion", detalle));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex) {
        log.debug("JSON no legible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("JSON_INVALIDO", "El cuerpo de la solicitud no es JSON valido o tiene formato incorrecto"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroRequerido(MissingServletRequestParameterException ex) {
        log.debug("Parametro requerido faltante: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("PARAMETRO_REQUERIDO", "Parametro requerido ausente: " + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        log.debug("Tipo de parametro invalido: {} = {}", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("PARAMETRO_INVALIDO", "Valor invalido para el parametro: " + ex.getName()));
    }

    // -------------------------------------------------------------------------
    // 405 / 415
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METODO_NO_SOPORTADO", "Metodo HTTP no soportado: " + ex.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("MEDIA_TYPE_NO_SOPORTADO", "Content-Type no soportado"));
    }

    // -------------------------------------------------------------------------
    // Recurso estatico no encontrado (demo deshabilitado, ruta inexistente)
    // -------------------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        log.debug("Recurso estatico no encontrado: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RECURSO_NO_ENCONTRADO", "Recurso no encontrado"));
    }

    // -------------------------------------------------------------------------
    // ResponseStatusException: pass-through de status HTTP (incluye NoHandlerFoundException)
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        if (status >= 500) {
            log.error("ResponseStatusException [{}]", status, ex);
        } else {
            log.debug("ResponseStatusException [{}]: {}", status, ex.getReason());
        }
        String codigo = "HTTP_" + status;
        String mensaje = ex.getReason() != null ? ex.getReason() : "Error HTTP " + status;
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorResponse.of(codigo, mensaje));
    }

    // -------------------------------------------------------------------------
    // 500 fallback seguro — no captura Throwable, no intercepta Spring Security
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInterno(Exception ex) {
        log.error("Error interno no esperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("ERROR_INTERNO", "Error interno del servidor"));
    }
}
