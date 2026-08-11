package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando una fotografia de evidencia (Wave 4 Parte 3D) no puede
 * procesarse: formato no soportado, contenido corrupto, archivo vacio,
 * dimensiones invalidas, limite de tamaño/pixeles excedido o contenido que el
 * decoder no puede validar como imagen real. Mapea a HTTP 422
 * (Parte 3D S6, misma familia que {@link PrecondicionVioladaException}).
 */
public class ImagenEvidenciaInvalidaException extends RuntimeException {
    public ImagenEvidenciaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public ImagenEvidenciaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
