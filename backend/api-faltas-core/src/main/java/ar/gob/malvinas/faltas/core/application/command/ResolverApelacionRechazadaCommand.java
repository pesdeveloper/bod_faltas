package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para rechazar la apelacion presentada sobre el fallo condenatorio.
 *
 * Genera evento APERAZ.
 * Precondiciones: acta abierta, apelacion PRESENTADA, fallo CONDENATORIO NOTIFICADO.
 * Efectos: apelacion queda RECHAZADA, snapshot encaminado a firmeza/condena.
 * No genera CONFIR ni CONDENA_FIRME. No cierra el acta.
 */
public record ResolverApelacionRechazadaCommand(
        Long actaId,
        String fundamentosResolucion,
        String observaciones
) {}

