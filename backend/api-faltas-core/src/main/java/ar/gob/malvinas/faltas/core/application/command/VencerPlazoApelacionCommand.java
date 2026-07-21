package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Comando para declarar firmeza por vencimiento del plazo de apelacion.
 *
 * El plazo real esta persistido en FalActaFallo.fhVtoApelacion.
 * Este comando no calcula el plazo; verifica que la fecha actual sea posterior
 * a la fecha limite almacenada.
 *
 * Genera PLAVNC + CONFIR y asigna CONDENA_FIRME al acta.
 * No cierra el acta ni inicia pago de condena.
 */
public record VencerPlazoApelacionCommand(
        @NotNull Long actaId,
        String observaciones,
        @NotBlank @Size(max = 36) String actor
) {}
