package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.web.dto.EnviarNotificacionRequest;
import ar.gob.malvinas.faltas.core.web.dto.RegistrarResultadoNotificacionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faltas")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @PostMapping("/actas/{idActa}/notificaciones/enviar")
    public ResponseEntity<ComandoResultado> enviar(
            @PathVariable Long idActa,
            @Valid @RequestBody EnviarNotificacionRequest req) {
        EnviarNotificacionCommand cmd = new EnviarNotificacionCommand(
                idActa, req.idDocumento(), req.canal(), req.observaciones());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificacionService.enviarNotificacion(cmd));
    }

    @PostMapping("/notificaciones/{id}/positiva")
    public ResponseEntity<ComandoResultado> positiva(
            @PathVariable String id,
            @RequestBody(required = false) RegistrarResultadoNotificacionRequest req) {
        RegistrarNotificacionPositivaCommand cmd = new RegistrarNotificacionPositivaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(notificacionService.registrarPositiva(cmd));
    }

    @PostMapping("/notificaciones/{id}/negativa")
    public ResponseEntity<ComandoResultado> negativa(
            @PathVariable String id,
            @RequestBody(required = false) RegistrarResultadoNotificacionRequest req) {
        RegistrarNotificacionNegativaCommand cmd = new RegistrarNotificacionNegativaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(notificacionService.registrarNegativa(cmd));
    }

    @PostMapping("/notificaciones/{id}/vencida")
    public ResponseEntity<ComandoResultado> vencida(
            @PathVariable String id,
            @RequestBody(required = false) RegistrarResultadoNotificacionRequest req) {
        RegistrarNotificacionVencidaCommand cmd = new RegistrarNotificacionVencidaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(notificacionService.registrarVencida(cmd));
    }

    @GetMapping("/actas/{idActa}/notificaciones")
    public ResponseEntity<List<FalNotificacion>> listar(@PathVariable Long idActa) {
        return ResponseEntity.ok(notificacionService.obtenerNotificaciones(idActa));
    }

    @ExceptionHandler(ActaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ActaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DocumentoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleDocNotFound(DocumentoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NotificacionNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleNotifNotFound(NotificacionNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}

