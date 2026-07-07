package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a valorizaciones de acta.
 * Implementa optimistic locking real: guarda compara versionRow.
 * Solo una vigente por acta+tipo en todo momento.
 */
public interface ActaValorizacionRepository {

    Long nextId();

    /**
     * Alta si no existe; update con control optimista si ya existe.
     * En update: compara versionRow; si no coincide lanza ConcurrenciaConflictoException.
     * Si coincide, incrementa versionRow en la copia almacenada.
     */
    FalActaValorizacion save(FalActaValorizacion valorizacion);

    Optional<FalActaValorizacion> findById(Long id);

    List<FalActaValorizacion> findByActaId(Long actaId);

    List<FalActaValorizacion> findByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo);

    Optional<FalActaValorizacion> findVigenteByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo);

    List<FalActaValorizacion> findAll();
}
