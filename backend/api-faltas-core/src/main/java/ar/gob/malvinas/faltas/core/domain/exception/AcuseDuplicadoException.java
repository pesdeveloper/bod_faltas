package ar.gob.malvinas.faltas.core.domain.exception;

public class AcuseDuplicadoException extends RuntimeException {
    public AcuseDuplicadoException(Long notificacionId, Long intentoId, String tipoAcuse) {
        super("Acuse duplicado: notificacionId=" + notificacionId + ", intentoId=" + intentoId + ", tipo=" + tipoAcuse);
    }
}
