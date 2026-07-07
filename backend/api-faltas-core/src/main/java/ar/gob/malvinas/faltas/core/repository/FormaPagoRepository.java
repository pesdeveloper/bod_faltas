package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para FalActaFormaPago.
 * Una sola forma vigente por obligacion garantizada por el repository.
 */
public interface FormaPagoRepository {
    Long nextId();
    FalActaFormaPago save(FalActaFormaPago forma);
    Optional<FalActaFormaPago> findById(Long id);
    List<FalActaFormaPago> findByObligacionPagoId(Long obligacionPagoId);
    Optional<FalActaFormaPago> findVigenteByObligacionPagoId(Long obligacionPagoId);
    Optional<FalActaFormaPago> findByObligacionPagoIdAndNroForma(Long obligacionPagoId, short nroForma);
    List<FalActaFormaPago> findByReferenciaEM(String cmteEM, short prefEM, int nroEM);
    List<FalActaFormaPago> findByReferenciaPG(String cmtePG, short prefPG, int nroPG);
    /** Operacion atomica: reemplaza vigente anterior y guarda nueva. */
    FalActaFormaPago reemplazarVigenteAtomico(FalActaFormaPago nueva, FalActaFormaPago anteriorVigente);
}
