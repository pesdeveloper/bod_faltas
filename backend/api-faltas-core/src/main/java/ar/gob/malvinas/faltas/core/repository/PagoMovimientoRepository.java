package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.application.service.RegistroMovimientoOutcome;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;

import java.util.List;
import java.util.Optional;

public interface PagoMovimientoRepository {
    Long nextId();
    RegistroMovimientoOutcome append(FalActaPagoMovimiento movimiento);
    Optional<FalActaPagoMovimiento> findById(Long id);
    List<FalActaPagoMovimiento> findByObligacionPagoId(Long obligacionPagoId);
    List<FalActaPagoMovimiento> findByFormaPagoId(Long formaPagoId);
    List<FalActaPagoMovimiento> findByPlanPagoRefId(Long planPagoRefId);
    Optional<FalActaPagoMovimiento> findByReferenciaExterna(String referenciaExterna);
    Optional<FalActaPagoMovimiento> findByOrigenAndReferenciaExterna(OrigenMovimiento origen, String referenciaExterna);
    List<FalActaPagoMovimiento> findByReferenciaEM(String cmteEM, short prefEM, int nroEM);
    List<FalActaPagoMovimiento> findByReferenciaPG(String cmtePG, short prefPG, int nroPG);
    List<FalActaPagoMovimiento> findByIdCierre(Long idCierre);
    List<FalActaPagoMovimiento> findByIdOpe(Long idOpe);
}
