package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a ítems de valorización de acta.
 * Los ítems confirmados son inmutables. No se mueven entre valorizaciones.
 */
public interface ActaValorizacionItemRepository {

    Long nextId();

    FalActaValorizacionItem save(FalActaValorizacionItem item);

    Optional<FalActaValorizacionItem> findById(Long id);

    List<FalActaValorizacionItem> findByValorizacionId(Long valorizacionId);

    List<FalActaValorizacionItem> findByActaArticuloId(Long actaArticuloId);
}
