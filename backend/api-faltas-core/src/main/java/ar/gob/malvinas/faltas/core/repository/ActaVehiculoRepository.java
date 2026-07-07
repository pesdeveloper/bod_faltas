package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;

import java.util.Optional;

public interface ActaVehiculoRepository {
    FalActaVehiculo guardar(FalActaVehiculo vehiculo);
    Optional<FalActaVehiculo> findByActaId(Long actaId);
    boolean existsByActaId(Long actaId);
}
