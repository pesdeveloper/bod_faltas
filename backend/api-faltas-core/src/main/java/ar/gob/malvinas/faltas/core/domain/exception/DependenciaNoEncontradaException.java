package ar.gob.malvinas.faltas.core.domain.exception;

public class DependenciaNoEncontradaException extends RuntimeException {
    public DependenciaNoEncontradaException(Long idDep) {
        super("Dependencia no encontrada: " + idDep);
    }
}
