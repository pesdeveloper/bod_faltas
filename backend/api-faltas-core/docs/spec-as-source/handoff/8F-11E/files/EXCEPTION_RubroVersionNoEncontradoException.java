package ar.gob.malvinas.faltas.core.domain.exception;
public class RubroVersionNoEncontradoException extends RuntimeException {
    public RubroVersionNoEncontradoException(Long rubroId) { super("RubroVersion no encontrado: rubroId=" + rubroId); }
    public RubroVersionNoEncontradoException(int idRub) { super("RubroVersion actual no encontrado para Id_Rub=" + idRub); }
    public RubroVersionNoEncontradoException(String msg) { super(msg); }
}
