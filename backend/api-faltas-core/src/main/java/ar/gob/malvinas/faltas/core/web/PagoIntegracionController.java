package ar.gob.malvinas.faltas.core.web;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.PagoEconomicoService;
import ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.web.dto.NotificarMovimientoPagoRequest;
import ar.gob.malvinas.faltas.core.web.dto.RevertirMovimientoPagoRequest;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/faltas/pagos")
public class PagoIntegracionController {

    private final PagoEconomicoService pagoEconomicoService;

    public PagoIntegracionController(PagoEconomicoService pagoEconomicoService) {
        this.pagoEconomicoService = pagoEconomicoService;
    }

    @PostMapping("/notificar-movimiento")
    public ResponseEntity<FalActaPagoMovimiento> notificarMovimiento(@RequestBody NotificarMovimientoPagoRequest req) {
        String actor = ActorContextHolder.subOr(null);
        if (actor == null) {
            throw new PrecondicionVioladaException("Actor autenticado requerido");
        }
        NotificarMovimientoPagoCommand cmd = new NotificarMovimientoPagoCommand(
                req.obligacionPagoId(), req.formaPagoId(), req.planPagoRefId(),
                req.tipoMovimiento(), req.origenMovimiento(), req.origenConfirmacion(), req.evidenciaDocumentoId(),
                req.clasificacionPago(), req.nroCuota(), req.importeCapital(), req.importeRima(), req.importeTotal(),
                req.cmteEM(), req.prefEM(), req.nroEM(), req.cmtePG(), req.prefPG(), req.nroPG(),
                req.idCierre(), req.idOpe(), req.fhPagoProcesado(), req.fhPagoConfirmado(),
                req.referenciaExterna(), req.fhMovimiento(), actor);
        return ResponseEntity.ok(pagoEconomicoService.notificarMovimiento(cmd));
    }

    @org.springframework.web.bind.annotation.PostMapping("/revertir-movimiento")
    public ResponseEntity<FalActaPagoMovimiento> revertirMovimiento(@RequestBody RevertirMovimientoPagoRequest req) {
        String actor = ActorContextHolder.subOr(null);
        if (actor == null) {
            throw new PrecondicionVioladaException("Actor autenticado requerido");
        }
        return ResponseEntity.ok(pagoEconomicoService.revertirMovimiento(
                req.movimientoOriginalId(), req.motivo(), req.referenciaExterna(), req.origenMovimiento(), actor));
    }

}
