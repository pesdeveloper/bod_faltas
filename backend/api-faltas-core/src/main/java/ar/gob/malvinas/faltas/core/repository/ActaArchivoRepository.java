package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaArchivo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_acta_archivo.
 *
 * Invariantes a proteger:
 * - Un solo ciclo activo por acta (siActivo=true).
 * - crearActivoAtomicamente verifica unicidad antes de insertar.
 * - cerrarActivoAtomicamente usa OCC (versionRow).
 */
public interface ActaArchivoRepository {
    FalActaArchivo guardar(FalActaArchivo archivo);
    Optional<FalActaArchivo> buscarActivoPorActa(Long actaId);
    List<FalActaArchivo> listarHistoricoPorActa(Long actaId);
    FalActaArchivo crearActivoAtomicamente(FalActaArchivo archivo);
    FalActaArchivo cerrarActivoAtomicamente(Long actaId, FalActaArchivo cierre);
    Long nextId();
}
