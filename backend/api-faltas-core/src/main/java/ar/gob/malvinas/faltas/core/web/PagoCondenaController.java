package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.web.dto.ConfirmarPagoCondenaRequest;
import ar.gob.malvinas.faltas.core.web.dto.InformarPagoCondenaRequest;
import ar.gob.malvinas.faltas.core.web.dto.ObservarPagoCondenaRequest;
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
public class PagoCondenaController {

    private final PagoCondenaService pagoCondenaService;

    public PagoCondenaController(PagoCondenaService pagoCondenaService) {
        this.pagoCondenaService = pagoCondenaService;
    }

    @PostMapping("/{id}/pago-condena/informar")
    public ResponseEntity<ComandoResultado> informar(
            @PathVariable Long id,
            @RequestBody InformarPagoCondenaRequest req) {
        InformarPagoCondenaCommand cmd = new InformarPagoCondenaCommand(
                id, req.monto(), req.referenciaPago(), req.observaciones());
        return ResponseEntity.ok(pagoCondenaService.informar(cmd));
    }

    @PostMapping("/{id}/pago-condena/confirmar")
    public ResponseEntity<ComandoResultado> confirmar(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarPagoCondenaRequest req) {
        ConfirmarPagoCondenaCommand cmd = new ConfirmarPagoCondenaCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(pagoCondenaService.confirmar(cmd));
    }

    @PostMapping("/{id}/pago-condena/observar")
    public ResponseEntity<ComandoResultado> observar(
            @PathVariable Long id,
            @RequestBody ObservarPagoCondenaRequest req) {
        ObservarPagoCondenaCommand cmd = new ObservarPagoCondenaCommand(
                id, req.motivoObservacion(), req.observaciones());
        return ResponseEntity.ok(pagoCondenaService.observar(cmd));
    }

    @GetMapping("/{id}/pago-condena")
    public ResponseEntity<FalPagoCondena> obtener(@PathVariable Long id) {
        Optional<FalPagoCondena> pago = pagoCondenaService.obtenerPago(id);
        return pago.map(ResponseEntity::ok)
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

