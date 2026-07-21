package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Comando canonico para generar un lote de correo postal
 * agrupando todas las notificaciones en estado PENDIENTE_ENVIO.
 *
 * CMD-FALLO-003 - Generar lote postal desde notificaciones pendientes.
 *
 * El servicio valida y normaliza los campos porque el comando puede invocarse
 * sin un adaptador Bean Validation.
 *
 * No expone lista de IDs: el conjunto de notificaciones se deriva internamente
 * consultando el repositorio de notificaciones en estado PENDIENTE_ENVIO.
 */
public record GenerarLoteCorreoCommand(
        @NotBlank @Size(max = 30) String loteCodigo,
        @Size(max = 60) String referenciaExterna,
        @Size(max = 36) String guidLoteExt,
        @NotBlank @Size(max = 36) String actorTecnico) {
}
