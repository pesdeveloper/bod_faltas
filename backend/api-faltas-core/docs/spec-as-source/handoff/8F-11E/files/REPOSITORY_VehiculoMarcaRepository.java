package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;

import java.util.List;
import java.util.Optional;

public interface VehiculoMarcaRepository {
    Long nextId();
    FalVehiculoMarca guardar(FalVehiculoMarca marca);
    Optional<FalVehiculoMarca> findById(Long id);
    Optional<FalVehiculoMarca> findByCodigo(String codigo);
    Optional<FalVehiculoMarca> findByNombre(String nombre);
    List<FalVehiculoMarca> findAllActivas();
    List<FalVehiculoMarca> findAll();
}
