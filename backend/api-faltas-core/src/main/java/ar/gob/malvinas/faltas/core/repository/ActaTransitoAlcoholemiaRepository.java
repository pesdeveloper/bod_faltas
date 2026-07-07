package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;

import java.util.List;
import java.util.Optional;

public interface ActaTransitoAlcoholemiaRepository {
    Long nextId();
    FalActaTransitoAlcoholemia guardar(FalActaTransitoAlcoholemia medicion);
    Optional<FalActaTransitoAlcoholemia> findById(Long id);
    List<FalActaTransitoAlcoholemia> findByActaId(Long actaId);
    Optional<FalActaTransitoAlcoholemia> findResultadoFinalByActaId(Long actaId);
    boolean existsOrdenByActaId(Long actaId, short orden);
    /**
     * Operacion atomica: desmarca el resultado final actual (si existe) y marca el nuevo.
     */
    FalActaTransitoAlcoholemia marcarResultadoFinalAtomicamente(Long actaId, Long medicionId);
}
