package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para registrar el vencimiento del plazo de apelacion.
 *
 * Representa el hecho operativo de que el infractor no presento apelacion
 * dentro del plazo habilitado. Genera PLAVNC + CONFIR y asigna CONDENA_FIRME.
 *
 * No implementa calculo real de fechas/plazos en Slice 3D.
 * El calculo de plazo queda para un slice posterior de integracion.
 */
public record VencerPlazoApelacionCommand(
        Long actaId,
        String observaciones
) {}
