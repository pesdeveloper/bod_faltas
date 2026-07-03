package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.web.dto.DictarFalloAbsolutorioRequest;
import ar.gob.malvinas.faltas.core.web.dto.DictarFalloCondenatorioRequest;
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
public class FalloController {

    private final FalloActaService falloActaService;

    public FalloController(FalloActaService falloActaService) {
        this.falloActaService = falloActaService;
    }

    @PostMapping("/{id}/fallos/absolutorio")
    public ResponseEntity<ComandoResultado> dictarAbsolutorio(
            @PathVariable Long id,
            @RequestBody(required = false) DictarFalloAbsolutorioRequest req) {
        DictarFalloAbsolutorioCommand cmd = new DictarFalloAbsolutorioCommand(
                id,
                req != null ? req.fundamentos() : null,
                req != null ? req.observaciones() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(falloActaService.dictarAbsolutorio(cmd));
    }

    @PostMapping("/{id}/fallos/condenatorio")
    public ResponseEntity<ComandoResultado> dictarCondenatorio(
            @PathVariable Long id,
            @RequestBody DictarFalloCondenatorioRequest req) {
        DictarFalloCondenatorioCommand cmd = new DictarFalloCondenatorioCommand(
                id,
                req.montoCondena(),
                req.fundamentos(),
                req.observaciones());
        return ResponseEntity.status(HttpStatus.CREATED).body(falloActaService.dictarCondenatorio(cmd));
    }

    @GetMapping("/{id}/fallo")
    public ResponseEntity<FalActaFallo> obtenerFallo(@PathVariable Long id) {
        Optional<FalActaFallo> fallo = falloActaService.obtenerFallo(id);
        return fallo.map(ResponseEntity::ok)
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

