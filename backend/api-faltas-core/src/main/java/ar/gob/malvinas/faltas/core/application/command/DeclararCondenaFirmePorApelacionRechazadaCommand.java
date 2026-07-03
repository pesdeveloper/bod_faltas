package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para declarar la firmeza de condena cuando la apelacion fue rechazada.
 *
 * La apelacion previa debe estar en estado RECHAZADA (APERAZ ya registrado).
 * Genera CONFIR (sin PLAVNC) y asigna CONDENA_FIRME.
 *
 * No inicia pago condena ni cierra el acta en Slice 3D.
 */
public record DeclararCondenaFirmePorApelacionRechazadaCommand(
        Long actaId,
        String observaciones
) {}
