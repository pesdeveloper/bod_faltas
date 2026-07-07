package ar.gob.malvinas.faltas.core.domain.exception;
public class VehiculoMarcaNoEncontradaException extends RuntimeException {
    public VehiculoMarcaNoEncontradaException(Long id) { super("Marca de vehiculo no encontrada: id=" + id); }
    public VehiculoMarcaNoEncontradaException(String msg) { super(msg); }
}
