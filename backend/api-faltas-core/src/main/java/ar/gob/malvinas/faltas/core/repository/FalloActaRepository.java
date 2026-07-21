package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del fallo del acta.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios de dominio.
 */
public interface FalloActaRepository {

    Long nextId();

    FalActaFallo guardar(FalActaFallo fallo);

    /** Guarda el fallo como el vigente del acta; marca el anterior vigente como REEMPLAZADO. */
    FalActaFallo guardarComoVigente(FalActaFallo fallo);

    /** Lanza PrecondicionVioladaException si ya existe un fallo vigente para el acta. */
    void rechazarSiYaExisteVigente(long actaId);

    /** Devuelve el fallo vigente (siVigente=true) del acta, si existe. */
    Optional<FalActaFallo> buscarActivo(Long actaId);

    /** Devuelve un fallo por su id. */
    Optional<FalActaFallo> findById(Long id);

    /** Devuelve todos los fallos del acta (historial completo). */
    List<FalActaFallo> findByActaId(Long actaId);

    /** Alias of buscarActivo for test compatibility. */
    default Optional<FalActaFallo> findVigenteByActaId(Long actaId) {
        return buscarActivo(actaId);
    }
}
