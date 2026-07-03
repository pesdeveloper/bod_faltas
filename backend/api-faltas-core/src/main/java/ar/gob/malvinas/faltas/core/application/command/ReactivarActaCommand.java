package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para reactivar un acta paralizada.
 *
 * Precondiciones: acta existe, esta paralizada.
 * Efectos: situacion -> ACTIVA, registra ACTREA, recalcula snapshot.
 *
 * Slice 8F-4C: gap cubierto para ACT-020-PARALIZADA.
 */
public record ReactivarActaCommand(
        Long actaId,
        String motivoReactivacion,
        String observaciones
) {}
