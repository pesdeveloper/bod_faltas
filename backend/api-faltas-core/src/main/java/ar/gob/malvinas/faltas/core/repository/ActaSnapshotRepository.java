package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;

import java.util.Optional;

/**
 * Contrato de persistencia del snapshot operativo del acta.
 * El snapshot es regenerable y se reemplaza en cada recálculo.
 */
public interface ActaSnapshotRepository {
    FalActaSnapshot guardar(FalActaSnapshot snapshot);
    Optional<FalActaSnapshot> buscarPorActa(Long idActa);
}

