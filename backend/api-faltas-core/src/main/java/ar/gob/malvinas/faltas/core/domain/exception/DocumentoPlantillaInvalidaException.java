package ar.gob.malvinas.faltas.core.domain.exception;

public class DocumentoPlantillaInvalidaException extends RuntimeException {
    public DocumentoPlantillaInvalidaException(String mensaje) {
        super(mensaje);
    }
}