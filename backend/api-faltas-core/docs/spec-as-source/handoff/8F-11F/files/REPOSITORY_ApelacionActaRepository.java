package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de acceso a FalActaApelacion.
 */
public interface ApelacionActaRepository {

    Long nextId();

    /** Guarda o actualiza con optimistic locking (versionRow). */
    void guardar(FalActaApelacion apelacion);

    Optional<FalActaApelacion> findById(Long id);

    /** Todas las apelaciones de un acta, ordenadas por id asc. */
    List<FalActaApelacion> findByActaId(Long actaId);

    /** Todas las apelaciones de un fallo especifico. */
    List<FalActaApelacion> findByFalloId(Long falloId);

    /** Apelacion activa (PRESENTADA o EN_ANALISIS) para un acta, si existe. */
    Optional<FalActaApelacion> buscarActiva(Long actaId);

    /** Ultima apelacion registrada para un acta (por id desc), si existe. */
    Optional<FalActaApelacion> buscarUltima(Long actaId);
}