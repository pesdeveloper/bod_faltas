package ar.gob.malvinas.faltas.core.domain.exception;

public class GestionExternaNoEncontradaException extends RuntimeException {
    public GestionExternaNoEncontradaException(Long actaId) {
        super("No se encontro gestion externa activa para el acta: " + actaId);
    }
}
