package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;

import java.util.Optional;

public interface ActaTransitoRepository {
    FalActaTransito guardar(FalActaTransito transito);
    Optional<FalActaTransito> findByActaId(Long actaId);
    boolean existsByActaId(Long actaId);
}
