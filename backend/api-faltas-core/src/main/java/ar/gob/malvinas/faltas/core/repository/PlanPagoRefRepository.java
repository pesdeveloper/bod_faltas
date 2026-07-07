package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para FalActaPlanPagoRef.
 * Un plan vigente por obligacion garantizado por el repository.
 */
public interface PlanPagoRefRepository {
    Long nextId();
    FalActaPlanPagoRef save(FalActaPlanPagoRef plan);
    Optional<FalActaPlanPagoRef> findById(Long id);
    List<FalActaPlanPagoRef> findByObligacionPagoId(Long obligacionPagoId);
    List<FalActaPlanPagoRef> findByFormaPagoId(Long formaPagoId);
    Optional<FalActaPlanPagoRef> findVigenteByObligacionPagoId(Long obligacionPagoId);
    Optional<FalActaPlanPagoRef> findByIdTdocPlanAndIdDocPlan(short idTdocPlan, long idDocPlan);
    /** Operacion atomica: marca anterior como REFINANCIADO y guarda nuevo como vigente. */
    FalActaPlanPagoRef refinanciarAtomico(FalActaPlanPagoRef nuevo, FalActaPlanPagoRef anteriorVigente);
}
