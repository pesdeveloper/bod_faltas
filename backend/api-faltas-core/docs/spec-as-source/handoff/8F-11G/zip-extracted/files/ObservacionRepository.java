package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_observacion.
 * Copias defensivas en todas las operaciones de lectura.
 */
public interface ObservacionRepository {
    FalObservacion guardar(FalObservacion obs);
    Optional<FalObservacion> buscarPorId(Long id);
    List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId);
    List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId);
    void desactivar(Long id);
    Long nextId();
}
