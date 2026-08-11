package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_motivo_archivo.
 *
 * El catalogo es administrable. Baja logica (siActivo=false).
 * La unicidad de codMotivoArchivo es concurrente.
 *
 * Identificadores: la PK la asigna la persistencia
 * (fal_motivo_archivo.id es BIGINT AUTO_INCREMENT). El puerto no expone nextId().
 */
public interface MotivoArchivoRepository {

    /**
     * Persiste el motivo. Con id nulo inserta y devuelve la entidad con el id
     * asignado; con id presente actualiza por ID el registro existente.
     *
     * La actualizacion es un UPDATE por clave primaria. La implementacion JDBC
     * futura nunca debe usar SQL REPLACE: REPLACE borra e inserta, cambiaria el
     * id y perderia silenciosamente los datos de auditoria de la fila original.
     */
    FalMotivoArchivo guardar(FalMotivoArchivo motivo);

    Optional<FalMotivoArchivo> buscarPorId(Long id);
    Optional<FalMotivoArchivo> buscarPorCodigo(String codMotivoArchivo);
    List<FalMotivoArchivo> listarActivos();
    List<FalMotivoArchivo> listarTodos();
    FalMotivoArchivo actualizarAtomicamente(FalMotivoArchivo motivo);
}
