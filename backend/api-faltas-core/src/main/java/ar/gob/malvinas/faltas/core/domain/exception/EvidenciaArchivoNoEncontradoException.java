package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Se lanza cuando se solicita leer o descargar el archivo final de una
 * evidencia (Wave 4 Parte 3D) y la clave de almacenamiento no corresponde a
 * ningun archivo existente. Mapea a HTTP 404 (Parte 3D S14).
 */
public class EvidenciaArchivoNoEncontradoException extends RuntimeException {
    public EvidenciaArchivoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
