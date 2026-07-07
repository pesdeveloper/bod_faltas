package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaParalizacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_acta_paralizacion.
 *
 * Invariantes a proteger:
 * - Una sola fila activa por acta (siActiva=true).
 * - crearActivaAtomicamente verifica unicidad antes de insertar.
 * - cerrarActivaAtomicamente usa OCC (versionRow).
 */
public interface ActaParalizacionRepository {
    FalActaParalizacion guardar(FalActaParalizacion paralizacion);
    Optional<FalActaParalizacion> buscarActivaPorActa(Long actaId);
    List<FalActaParalizacion> listarHistoricoPorActa(Long actaId);
    FalActaParalizacion crearActivaAtomicamente(FalActaParalizacion paralizacion);
    FalActaParalizacion cerrarActivaAtomicamente(Long actaId, FalActaParalizacion cierre);
    Long nextId();
}
