package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para aceptar la apelacion presentada, absolviendo al infractor.
 *
 * Genera evento APEABS.
 * Precondiciones: acta abierta, apelacion PRESENTADA, fallo CONDENATORIO NOTIFICADO.
 * Efectos: apelacion queda ACEPTADA_ABSUELVE, resultadoFinal = ABSUELTO.
 * Si no hay bloqueantes activos: cierra acta y registra CIERRA.
 * Si hay bloqueantes activos: no cierra, queda pendiente operativo.
 */
public record ResolverApelacionAceptaAbsuelveCommand(
        Long actaId,
        String fundamentosResolucion,
        String observaciones
) {}

