package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia append-only para FalActaPagoMovimiento.
 * Los movimientos no se modifican ni eliminan.
 */
public interface PagoMovimientoRepository {
    Long nextId();
    FalActaPagoMovimiento append(FalActaPagoMovimiento movimiento);
    Optional<FalActaPagoMovimiento> findById(Long id);
    List<FalActaPagoMovimiento> findByObligacionPagoId(Long obligacionPagoId);
    List<FalActaPagoMovimiento> findByFormaPagoId(Long formaPagoId);
    List<FalActaPagoMovimiento> findByPlanPagoRefId(Long planPagoRefId);
    Optional<FalActaPagoMovimiento> findByReferenciaExterna(String referenciaExterna);
    List<FalActaPagoMovimiento> findByReferenciaEM(String cmteEM, short prefEM, int nroEM);
    List<FalActaPagoMovimiento> findByReferenciaPG(String cmtePG, short prefPG, int nroPG);
    List<FalActaPagoMovimiento> findByIdCierre(Long idCierre);
    List<FalActaPagoMovimiento> findByIdOpe(Long idOpe);
}
