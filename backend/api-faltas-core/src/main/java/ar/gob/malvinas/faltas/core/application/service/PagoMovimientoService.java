package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para registrar movimientos de pago.
 * Append-only: los movimientos no se modifican.
 * Reversos y anulaciones agregan nuevos movimientos.
 * Idempotencia por referenciaExterna.
 */
@Service
public class PagoMovimientoService {

    private final PagoMovimientoRepository movimientoRepo;
    private final ObligacionPagoRepository obligacionRepo;

    public PagoMovimientoService(
            PagoMovimientoRepository movimientoRepo,
            ObligacionPagoRepository obligacionRepo) {
        this.movimientoRepo = movimientoRepo;
        this.obligacionRepo = obligacionRepo;
    }

    /**
     * Registra un movimiento de pago.
     * Idempotente si referenciaExterna es unica y el payload es identico.
     * Conflicto si misma referenciaExterna con payload diferente.
     */
    public FalActaPagoMovimiento registrar(
            Long obligacionPagoId,
            Long formaPagoId,
            Long planPagoRefId,
            TipoMovimientoPago tipoMovimiento,
            Short nroCuota,
            BigDecimal importeCapital,
            BigDecimal importeRima,
            BigDecimal importeTotal,
            String cmteEM, Short prefEM, Integer nroEM,
            String cmtePG, Short prefPG, Integer nroPG,
            Long idCierre,
            Long idOpe,
            LocalDateTime fhPagoProcesado,
            LocalDateTime fhPagoConfirmado,
            String referenciaExterna,
            LocalDateTime fhMovimiento,
            String idUser) {
        obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(obligacionPagoId));

        if (nroCuota != null && planPagoRefId == null)
            throw new PrecondicionVioladaException("nroCuota requiere planPagoRefId");

        LocalDateTime fhAlta = LocalDateTime.now();
        LocalDateTime fhMov = fhMovimiento != null ? fhMovimiento : fhAlta;

        Long nuevoId = movimientoRepo.nextId();
        FalActaPagoMovimiento m = new FalActaPagoMovimiento.Builder(
                nuevoId, obligacionPagoId, tipoMovimiento, fhMov, fhAlta, idUser)
                .formaPagoId(formaPagoId)
                .planPagoRefId(planPagoRefId)
                .nroCuota(nroCuota)
                .importes(importeCapital, importeRima, importeTotal)
                .referenciaEM(cmteEM, prefEM, nroEM)
                .referenciaPG(cmtePG, prefPG, nroPG)
                .idCierre(idCierre)
                .idOpe(idOpe)
                .fhPagoProcesado(fhPagoProcesado)
                .fhPagoConfirmado(fhPagoConfirmado)
                .referenciaExterna(referenciaExterna)
                .build();
        return movimientoRepo.append(m);
    }

    /**
     * Registra reverso/anulacion de un movimiento existente.
     * El movimiento original debe pertenecer a la misma obligacion.
     * No puede anular un movimiento ya anulado salvo reproceso explícito.
     */
    public FalActaPagoMovimiento anular(
            Long movimientoOriginalId,
            TipoMovimientoPago tipoAnulacion,
            MotivoAnulacionPago motivo,
            String referenciaExterna,
            String idUser) {
        FalActaPagoMovimiento original = movimientoRepo.findById(movimientoOriginalId)
                .orElseThrow(() -> new PrecondicionVioladaException("Movimiento no encontrado: " + movimientoOriginalId));

        boolean yaAnulado = movimientoRepo.findByObligacionPagoId(original.getObligacionPagoId()).stream()
                .anyMatch(m -> movimientoOriginalId.equals(m.getMovimientoAnuladoId())
                        && m.getTipoMovimiento() != TipoMovimientoPago.MOVIMIENTO_REPROCESADO);
        if (yaAnulado)
            throw new PrecondicionVioladaException(
                    "El movimiento id=" + movimientoOriginalId + " ya fue anulado. Usar MOVIMIENTO_REPROCESADO si corresponde.");

        if (motivo == null)
            throw new PrecondicionVioladaException("El motivo de anulacion es obligatorio");

        LocalDateTime ahora = LocalDateTime.now();
        Long nuevoId = movimientoRepo.nextId();
        FalActaPagoMovimiento reverso = new FalActaPagoMovimiento.Builder(
                nuevoId, original.getObligacionPagoId(), tipoAnulacion, ahora, ahora, idUser)
                .formaPagoId(original.getFormaPagoId())
                .planPagoRefId(original.getPlanPagoRefId())
                .movimientoAnuladoId(movimientoOriginalId)
                .motivoAnulacionPago(motivo)
                .referenciaExterna(referenciaExterna)
                .build();
        return movimientoRepo.append(reverso);
    }

    public List<FalActaPagoMovimiento> buscarPorObligacion(Long obligacionPagoId) {
        return movimientoRepo.findByObligacionPagoId(obligacionPagoId);
    }

    public Optional<FalActaPagoMovimiento> buscarPorReferenciaExterna(String refExterna) {
        return movimientoRepo.findByReferenciaExterna(refExterna);
    }
}
