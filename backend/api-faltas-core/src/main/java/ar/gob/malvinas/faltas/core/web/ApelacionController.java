package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionAceptaAbsuelveCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ApelacionActaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.web.dto.RegistrarApelacionRequest;
import ar.gob.malvinas.faltas.core.web.dto.ResolverApelacionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/faltas/actas")
public class ApelacionController {

    private final ApelacionActaService apelacionActaService;

    public ApelacionController(ApelacionActaService apelacionActaService) {
        this.apelacionActaService = apelacionActaService;
    }

    @PostMapping("/{id}/apelaciones")
    public ResponseEntity<ComandoResultado> registrarApelacion(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrarApelacionRequest req) {
        RegistrarApelacionCommand cmd = new RegistrarApelacionCommand(
                id,
                req != null ? req.presentante() : null,
                req != null ? req.fundamentos() : null,
                req != null ? req.observaciones() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(apelacionActaService.registrarApelacion(cmd));
    }

    @PostMapping("/{id}/apelaciones/rechazar")
    public ResponseEntity<ComandoResultado> rechazarApelacion(
            @PathVariable Long id,
            @RequestBody(required = false) ResolverApelacionRequest req) {
        ResolverApelacionRechazadaCommand cmd = new ResolverApelacionRechazadaCommand(
                id,
                req != null ? req.fundamentosResolucion() : null,
                req != null ? req.observaciones() : null);
        return ResponseEntity.ok(apelacionActaService.resolverRechazada(cmd));
    }

    @PostMapping("/{id}/apelaciones/aceptar-absuelve")
    public ResponseEntity<ComandoResultado> aceptarApelacionAbsuelve(
            @PathVariable Long id,
            @RequestBody(required = false) ResolverApelacionRequest req) {
        ResolverApelacionAceptaAbsuelveCommand cmd = new ResolverApelacionAceptaAbsuelveCommand(
                id,
                req != null ? req.fundamentosResolucion() : null,
                req != null ? req.observaciones() : null);
        return ResponseEntity.ok(apelacionActaService.resolverAceptaAbsuelve(cmd));
    }

    @GetMapping("/{id}/apelacion")
    public ResponseEntity<FalActaApelacion> obtenerApelacion(@PathVariable Long id) {
        Optional<FalActaApelacion> apelacion = apelacionActaService.obtenerApelacionActiva(id);
        return apelacion.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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

