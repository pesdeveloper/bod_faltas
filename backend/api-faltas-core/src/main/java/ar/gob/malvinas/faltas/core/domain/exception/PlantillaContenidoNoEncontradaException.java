package ar.gob.malvinas.faltas.core.domain.exception;

public class PlantillaContenidoNoEncontradaException extends RuntimeException {
    public PlantillaContenidoNoEncontradaException(Long plantillaId) {
        super("No se encontro contenido vigente para la plantilla id=" + plantillaId);
    }
    public PlantillaContenidoNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}