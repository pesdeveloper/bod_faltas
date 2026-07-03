package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para dictar fallo absolutorio sobre un acta.
 *
 * Precondiciones: acta en bloque ANAL, sin fallo activo previo,
 * sin pago voluntario confirmado.
 * Efectos: crea FalActaFallo (ABSOLUTORIO), crea documento FALLO_ABSOLUTORIO,
 * registra FALABS + DOCGEN. No cierra el acta.
 */
public record DictarFalloAbsolutorioCommand(
        Long actaId,
        String fundamentos,
        String observaciones
) {}

