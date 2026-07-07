package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;

import java.util.Optional;

/**
 * Puerto de persistencia para apelaciones de acta.
 * Implementacion actual: InMemoryApelacionActaRepository.
 * Reemplazable por JdbcClient/MariaDB sin tocar servicios de dominio.
 */
public interface ApelacionActaRepository {

    Long nextId();

    void guardar(FalActaApelacion apelacion);

    Optional<FalActaApelacion> buscarActiva(Long actaId);

    /**
     * Devuelve la ultima apelacion del acta, activa o resuelta.
     * Permite al snapshot detectar el estado post-resolucion (RECHAZADA / ACEPTADA_ABSUELVE).
     */
    Optional<FalActaApelacion> buscarUltima(Long actaId);

    Optional<FalActaApelacion> findById(Long id);
}