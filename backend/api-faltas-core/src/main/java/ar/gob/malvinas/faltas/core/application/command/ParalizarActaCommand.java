package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;

/**
 * Comando para paralizar un acta activa.
 *
 * Precondiciones: acta existe, no esta cerrada, no esta paralizada ya.
 * Efectos: situacion -> PARALIZADA, registra ACTPAR, recalcula snapshot.
 */
public record ParalizarActaCommand(
        Long actaId,
        MotivoParalizacion motivoParalizacion,
        String observacionTexto,
        String idUserOperacion
) {}
