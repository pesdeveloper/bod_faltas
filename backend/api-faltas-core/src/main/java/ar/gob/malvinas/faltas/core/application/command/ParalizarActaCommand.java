package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para paralizar un acta activa.
 *
 * Precondiciones: acta existe, no esta cerrada, no esta paralizada ya.
 * Efectos: situacion -> PARALIZADA, registra ACTPAR, recalcula snapshot.
 *
 * Slice 8F-4C: gap cubierto para ACT-020-PARALIZADA.
 */
public record ParalizarActaCommand(
        Long actaId,
        String motivoParalizacion,
        String observaciones
) {}
