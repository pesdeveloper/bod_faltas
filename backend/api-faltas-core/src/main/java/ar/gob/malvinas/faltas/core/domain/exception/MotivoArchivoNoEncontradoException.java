package ar.gob.malvinas.faltas.core.domain.exception;

public class MotivoArchivoNoEncontradoException extends RuntimeException {
    public MotivoArchivoNoEncontradoException(Long id) {
        super("MotivoArchivo no encontrado para id: " + id);
    }
    public MotivoArchivoNoEncontradoException(String codigo) {
        super("MotivoArchivo no encontrado para codigo: " + codigo);
    }
}
