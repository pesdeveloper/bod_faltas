package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para reactivar un acta paralizada.
 *
 * Precondiciones: acta existe, esta paralizada.
 * Efectos: situacion -> ACTIVA, registra ACTREA, recalcula snapshot.
 */
public record ReactivarActaCommand(
        Long actaId,
        String motivoReactivacion,
        String observacionTexto,
        String idUserOperacion
) {
    /** Backward-compatible constructor for tests using only actaId. */
    public ReactivarActaCommand(Long actaId) {
        this(actaId, null, null, null);
    }

    /** Backward-compatible constructor for tests using actaId and motivoReactivacion. */
    public ReactivarActaCommand(Long actaId, String motivoReactivacion) {
        this(actaId, motivoReactivacion, null, null);
    }
}