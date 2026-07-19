package ar.gob.malvinas.faltas.core.domain.exception;

public class DocumentoPlantillaNoEncontradaException extends RuntimeException {
    public DocumentoPlantillaNoEncontradaException(Long id) {
        super("Plantilla documental no encontrada: " + id);
    }
    public DocumentoPlantillaNoEncontradaException(String codigo) {
        super("Plantilla documental no encontrada con codigo: " + codigo);
    }
}
