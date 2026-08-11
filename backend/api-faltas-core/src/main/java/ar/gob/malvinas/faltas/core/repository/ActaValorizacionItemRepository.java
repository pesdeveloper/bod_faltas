package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso al historial versionado de valorizacion de cada articulo
 * infringido. Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.5: hijo de
 * {@code fal_acta_articulo_infringido}, no de la cabecera. Los items son
 * inmutables: {@code save} siempre inserta una fila nueva.
 */
public interface ActaValorizacionItemRepository {

    Long nextId();

    /** Inserta la version. No actualiza filas existentes: son inmutables. */
    FalActaValorizacionItem save(FalActaValorizacionItem item);

    Optional<FalActaValorizacionItem> findById(Long id);

    /** La version {@code siActual=true} del articulo, si existe. */
    Optional<FalActaValorizacionItem> findActualByActaArticulo(Long actaArticuloId);

    /**
     * Bookkeeping puro: marca la version dada como no-actual (para reemplazarla por
     * una nueva version actual). No modifica el contenido economico de la fila.
     */
    void marcarNoActual(Long id);

    /** Historial completo (todas las versiones) del articulo, ordenado por versionItem. */
    List<FalActaValorizacionItem> findAllByActaArticulo(Long actaArticuloId);

    /** Las versiones actuales de un conjunto de articulos infringidos (uso batch: snapshot). */
    List<FalActaValorizacionItem> findActualesByActaArticuloIds(List<Long> actaArticuloIds);
}
