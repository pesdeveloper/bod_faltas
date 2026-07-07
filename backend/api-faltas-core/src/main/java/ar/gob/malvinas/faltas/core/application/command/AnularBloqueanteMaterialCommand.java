package ar.gob.malvinas.faltas.core.application.command;

public record AnularBloqueanteMaterialCommand(Long bloqueanteId) {
    public AnularBloqueanteMaterialCommand(String bloqueanteId) {
        this(bloqueanteId != null ? Long.parseLong(bloqueanteId) : null);
    }
}