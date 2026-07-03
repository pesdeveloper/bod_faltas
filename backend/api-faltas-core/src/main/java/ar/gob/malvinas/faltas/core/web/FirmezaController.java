package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.web.dto.DeclararFirmezaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/faltas/actas")
public class FirmezaController {

    private final FirmezaCondenaService firmezaCondenaService;

    public FirmezaController(FirmezaCondenaService firmezaCondenaService) {
        this.firmezaCondenaService = firmezaCondenaService;
    }

    @PostMapping("/{id}/firmeza/vencer-plazo-apelacion")
    public ResponseEntity<ComandoResultado> vencerPlazoApelacion(
            @PathVariable Long id,
            @RequestBody(required = false) DeclararFirmezaRequest req) {
        VencerPlazoApelacionCommand cmd = new VencerPlazoApelacionCommand(
                id,
                req != null ? req.observaciones() : null);
        return ResponseEntity.ok(firmezaCondenaService.vencerPlazoApelacion(cmd));
    }

    @PostMapping("/{id}/firmeza/por-apelacion-rechazada")
    public ResponseEntity<ComandoResultado> firmezaPorApelacionRechazada(
            @PathVariable Long id,
            @RequestBody(required = false) DeclararFirmezaRequest req) {
        DeclararCondenaFirmePorApelacionRechazadaCommand cmd =
                new DeclararCondenaFirmePorApelacionRechazadaCommand(
                        id,
                        req != null ? req.observaciones() : null);
        return ResponseEntity.ok(firmezaCondenaService.declararFirmePorApelacionRechazada(cmd));
    }

    @ExceptionHandler(ActaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ActaNoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PrecondicionVioladaException.class)
    public ResponseEntity<Map<String, String>> handlePrecondicion(PrecondicionVioladaException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }
}
