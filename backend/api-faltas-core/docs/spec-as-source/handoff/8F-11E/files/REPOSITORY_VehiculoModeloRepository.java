package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;

import java.util.List;
import java.util.Optional;

public interface VehiculoModeloRepository {
    Long nextId();
    FalVehiculoModelo guardar(FalVehiculoModelo modelo);
    Optional<FalVehiculoModelo> findById(Long id);
    Optional<FalVehiculoModelo> findByMarcaAndCodigo(Long marcaId, String codigo);
    Optional<FalVehiculoModelo> findByMarcaAndNombre(Long marcaId, String nombre);
    List<FalVehiculoModelo> findActivasByMarca(Long marcaId);
    List<FalVehiculoModelo> findByMarca(Long marcaId);
}
