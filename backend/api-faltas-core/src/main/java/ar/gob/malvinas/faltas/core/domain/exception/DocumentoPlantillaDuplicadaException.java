package ar.gob.malvinas.faltas.core.domain.exception;

public class DocumentoPlantillaDuplicadaException extends RuntimeException {
    public DocumentoPlantillaDuplicadaException(String codigo) {
        super("Ya existe una plantilla documental con codigo: " + codigo);
    }
}
