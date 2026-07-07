package ar.gob.malvinas.faltas.core.domain.exception;
public class VehiculoModeloNoEncontradoException extends RuntimeException {
    public VehiculoModeloNoEncontradoException(Long id) { super("Modelo de vehiculo no encontrado: id=" + id); }
    public VehiculoModeloNoEncontradoException(String msg) { super(msg); }
}
