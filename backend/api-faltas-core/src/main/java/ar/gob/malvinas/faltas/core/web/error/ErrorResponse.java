package ar.gob.malvinas.faltas.core.web.error;

/**
 * Cuerpo canonico de respuesta de error para todos los endpoints de Faltas.
 */
public record ErrorResponse(
        String codigoError,
        String mensaje,
        String detalle,
        String correlacionId
) {

    public static ErrorResponse of(String codigoError, String mensaje) {
        return new ErrorResponse(codigoError, mensaje, null, null);
    }

    public static ErrorResponse of(String codigoError, String mensaje, String detalle) {
        return new ErrorResponse(codigoError, mensaje, detalle, null);
    }
}
