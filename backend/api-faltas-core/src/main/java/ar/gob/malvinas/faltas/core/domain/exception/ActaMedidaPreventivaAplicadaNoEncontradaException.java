package ar.gob.malvinas.faltas.core.domain.exception;
public class ActaMedidaPreventivaAplicadaNoEncontradaException extends RuntimeException {
    public ActaMedidaPreventivaAplicadaNoEncontradaException(Long id) { super("Medida preventiva aplicada no encontrada: id=" + id); }
}
