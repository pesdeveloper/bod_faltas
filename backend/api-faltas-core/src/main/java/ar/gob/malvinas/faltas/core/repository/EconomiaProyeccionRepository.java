package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;

import java.util.Optional;

public interface EconomiaProyeccionRepository {
    FalActaEconomiaProyeccion save(FalActaEconomiaProyeccion proyeccion);
    Optional<FalActaEconomiaProyeccion> findByActaId(Long actaId);
    void deleteByActaId(Long actaId);
}
