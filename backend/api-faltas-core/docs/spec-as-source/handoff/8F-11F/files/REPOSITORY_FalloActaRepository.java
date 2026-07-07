package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de acceso a FalActaFallo.
 * Un acta puede tener historial de fallos; solo uno es vigente (siVigente = true).
 */
public interface FalloActaRepository {

    Long nextId();

    /** Guarda o actualiza con optimistic locking (versionRow). */
    FalActaFallo guardar(FalActaFallo fallo);

    Optional<FalActaFallo> findById(Long id);

    /** Todos los fallos (historial) de un acta, ordenados por id asc. */
    List<FalActaFallo> findByActaId(Long actaId);

    /** El unico fallo vigente (siVigente = true) de un acta, si existe. */
    Optional<FalActaFallo> findVigenteByActaId(Long actaId);

    /**
     * Operacion atomica de alta del primer fallo o reemplazo del vigente:
     *   - si no existe fallo vigente: guarda nuevo como vigente
     *   - si existe vigente: lo marca REEMPLAZADO (siVigente=false), guarda el nuevo como vigente
     * Garantiza que solo un fallo quede vigente por acta.
     * Lanza ConcurrenciaConflictoException si hay conflicto de versionRow.
     */
    FalActaFallo guardarComoVigente(FalActaFallo nuevo);

    /** Rechazo de alta directa que viole unicidad de vigente (guardrail de repositorio). */
    void rechazarSiYaExisteVigente(Long actaId);

    // -----------------------------------------------------------------------
    // Compatibilidad backward
    // -----------------------------------------------------------------------

    /** @deprecated Usar findVigenteByActaId(). */
    @Deprecated
    default Optional<FalActaFallo> buscarActivo(Long actaId) { return findVigenteByActaId(actaId); }
}