package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.FijarMontoPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.SolicitarPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPagoVoluntarioCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.PagoVoluntarioService;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.web.dto.ConfirmarPagoVoluntarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.FijarMontoPagoVoluntarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.InformarPagoVoluntarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.ObservarPagoVoluntarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.SolicitarPagoVoluntarioRequest;
import ar.gob.malvinas.faltas.core.web.dto.VencerPagoVoluntarioRequest;
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
public class PagoVoluntarioController {

    private final PagoVoluntarioService pagoVoluntarioService;

    public PagoVoluntarioController(PagoVoluntarioService pagoVoluntarioService) {
        this.pagoVoluntarioService = pagoVoluntarioService;
    }

    @PostMapping("/{id}/pago-voluntario/solicitar")
    public ResponseEntity<ComandoResultado> solicitar(
            @PathVariable Long id,
            @RequestBody(required = false) SolicitarPagoVoluntarioRequest req) {
        SolicitarPagoVoluntarioCommand cmd = new SolicitarPagoVoluntarioCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(pagoVoluntarioService.solicitar(cmd));
    }

    @PostMapping("/{id}/pago-voluntario/fijar-monto")
    public ResponseEntity<ComandoResultado> fijarMonto(
            @PathVariable Long id,
            @RequestBody FijarMontoPagoVoluntarioRequest req) {
        FijarMontoPagoVoluntarioCommand cmd = new FijarMontoPagoVoluntarioCommand(
                id, req.monto(), req.observaciones());
        return ResponseEntity.ok(pagoVoluntarioService.fijarMonto(cmd));
    }

    @PostMapping("/{id}/pago-voluntario/informar")
    public ResponseEntity<ComandoResultado> informar(
            @PathVariable Long id,
            @RequestBody InformarPagoVoluntarioRequest req) {
        InformarPagoVoluntarioCommand cmd = new InformarPagoVoluntarioCommand(
                id, req.referenciaPago(), req.observaciones());
        return ResponseEntity.ok(pagoVoluntarioService.informar(cmd));
    }

    @PostMapping("/{id}/pago-voluntario/confirmar")
    public ResponseEntity<ComandoResultado> confirmar(
            @PathVariable Long id,
            @RequestBody(required = false) ConfirmarPagoVoluntarioRequest req) {
        ConfirmarPagoVoluntarioCommand cmd = new ConfirmarPagoVoluntarioCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(pagoVoluntarioService.confirmar(cmd));
    }

    @PostMapping("/{id}/pago-voluntario/observar")
    public ResponseEntity<ComandoResultado> observar(
            @PathVariable Long id,
            @RequestBody ObservarPagoVoluntarioRequest req) {
        ObservarPagoVoluntarioCommand cmd = new ObservarPagoVoluntarioCommand(
                id, req.motivoObservacion(), req.observaciones());
        return ResponseEntity.ok(pagoVoluntarioService.observar(cmd));
    }

    @PostMapping("/{id}/pago-voluntario/vencer")
    public ResponseEntity<ComandoResultado> vencer(
            @PathVariable Long id,
            @RequestBody(required = false) VencerPagoVoluntarioRequest req) {
        VencerPagoVoluntarioCommand cmd = new VencerPagoVoluntarioCommand(
                id, req != null ? req.observaciones() : null);
        return ResponseEntity.ok(pagoVoluntarioService.vencer(cmd));
    }

    @GetMapping("/{id}/pago-voluntario")
    public ResponseEntity<FalPagoVoluntario> obtener(@PathVariable Long id) {
        Optional<FalPagoVoluntario> pago = pagoVoluntarioService.obtenerPago(id);
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
