package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para FalActaObligacionPago.
 * Una sola obligacion vigente por acta garantizada por el repository.
 */
public interface ObligacionPagoRepository {
    Long nextId();
    FalActaObligacionPago save(FalActaObligacionPago obligacion);
    Optional<FalActaObligacionPago> findById(Long id);
    List<FalActaObligacionPago> findByActaId(Long actaId);
    Optional<FalActaObligacionPago> findVigenteByActaId(Long actaId);
    /** Operacion atomica: desactiva vigente anterior y guarda nueva vigente. */
    FalActaObligacionPago crearVigenteAtomico(FalActaObligacionPago nueva, FalActaObligacionPago anteriorONull);
}
