package ar.gob.malvinas.faltas.core.domain.exception;

public class FirmanteNoEncontradoException extends RuntimeException {
    public FirmanteNoEncontradoException(Long idFirmante) {
        super("Firmante no encontrado: " + idFirmante);
    }
}