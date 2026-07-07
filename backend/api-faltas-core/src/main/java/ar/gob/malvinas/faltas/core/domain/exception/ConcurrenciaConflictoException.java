package ar.gob.malvinas.faltas.core.domain.exception;

public class ConcurrenciaConflictoException extends RuntimeException {
    public ConcurrenciaConflictoException(String entidad, Object id, int versionAlmacenada, int versionEntrante) {
        super("Conflicto de concurrencia en " + entidad + " id=" + id
                + ": version almacenada=" + versionAlmacenada
                + ", version entrante=" + versionEntrante);
    }
}
