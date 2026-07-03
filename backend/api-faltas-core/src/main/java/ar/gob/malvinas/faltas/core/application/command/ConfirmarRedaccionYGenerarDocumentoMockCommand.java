package ar.gob.malvinas.faltas.core.application.command;

import java.time.LocalDateTime;

/**
 * Command para confirmar una redaccion BORRADOR o REABIERTA y generar la salida mock final.
 *
 * fhOperacion: opcional. Si null, el servicio usa LocalDateTime.now().
 *              Inyectable para facilitar tests deterministicos con reloj controlado.
 *
 * Slice 8F-3.
 */
public record ConfirmarRedaccionYGenerarDocumentoMockCommand(
        Long redaccionId,
        String idUserOperacion,
        LocalDateTime fhOperacion
) {
    public ConfirmarRedaccionYGenerarDocumentoMockCommand {
        if (redaccionId == null)
            throw new IllegalArgumentException("redaccionId requerido");
        if (idUserOperacion == null || idUserOperacion.isBlank())
            throw new IllegalArgumentException("idUserOperacion requerido");
    }
}