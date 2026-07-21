package ar.gob.malvinas.faltas.core.infrastructure.security;

public record ActorContext(String sub) {
    public ActorContext {
        if (sub == null || sub.isBlank()) throw new IllegalArgumentException("sub requerido");
        if (sub.length() > 36) throw new IllegalArgumentException("sub max 36 caracteres");
    }
}
