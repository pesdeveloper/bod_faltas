package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;

import java.util.Optional;

public interface ActaContravencionRepository {
    FalActaContravencion guardar(FalActaContravencion contravencion);
    Optional<FalActaContravencion> findByActaId(Long actaId);
    boolean existsByActaId(Long actaId);
}
