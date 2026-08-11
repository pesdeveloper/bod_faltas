package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;

import java.util.Optional;

/**
 * Port de acceso a la cabecera unica de valorizacion de un acta.
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.8: una sola fila por acta,
 * mutable con OCC ({@code versionRow}). Reemplaza el modelo previo de
 * multiples filas por acta+tipo.
 */
public interface ActaValorizacionRepository {

    Long nextId();

    /**
     * Alta si no existe cabecera para el acta; UPDATE con control optimista
     * (compara {@code versionRow}) si ya existe.
     */
    FalActaValorizacion save(FalActaValorizacion valorizacion);

    Optional<FalActaValorizacion> findById(Long id);

    Optional<FalActaValorizacion> findByActaId(Long actaId);
}
