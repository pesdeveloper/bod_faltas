package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_motivo_archivo.
 *
 * El catalogo es administrable. Baja logica (siActivo=false).
 * La unicidad de codMotivoArchivo es concurrente.
 */
public interface MotivoArchivoRepository {
    FalMotivoArchivo guardar(FalMotivoArchivo motivo);
    Optional<FalMotivoArchivo> buscarPorId(Long id);
    Optional<FalMotivoArchivo> buscarPorCodigo(String codMotivoArchivo);
    List<FalMotivoArchivo> listarActivos();
    List<FalMotivoArchivo> listarTodos();
    FalMotivoArchivo actualizarAtomicamente(FalMotivoArchivo motivo);
    Long nextId();
}
