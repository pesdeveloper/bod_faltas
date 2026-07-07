package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaVehiculoNoEncontradoException extends RuntimeException {
    public ActaVehiculoNoEncontradoException(Long actaId) { super("Vehiculo de acta no encontrado: actaId=" + actaId); }
}
