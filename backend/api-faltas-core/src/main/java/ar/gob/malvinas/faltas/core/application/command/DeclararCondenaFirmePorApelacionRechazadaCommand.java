package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Comando para declarar la firmeza de condena cuando la apelacion fue rechazada.
 *
 * La apelacion debe estar asociada al fallo activo (buscarPorFallo) y encontrarse
 * en estado RECHAZADA, o RESUELTA con resultado RECHAZADA.
 *
 * Genera exactamente un CONFIR (sin PLAVNC) usando el actor del JWT sub.
 * No cierra el acta ni inicia pago condena.
 *
 * La apelacion no es modificada por este comando.
 */
public record DeclararCondenaFirmePorApelacionRechazadaCommand(
        @NotNull Long actaId,
        String observaciones,
        @NotBlank @Size(max = 36) String actor
) {}
