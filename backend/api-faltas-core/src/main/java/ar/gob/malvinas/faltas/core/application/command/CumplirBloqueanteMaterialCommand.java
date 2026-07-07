package ar.gob.malvinas.faltas.core.application.command;

public record CumplirBloqueanteMaterialCommand(Long bloqueanteId) {
    public CumplirBloqueanteMaterialCommand(String bloqueanteId) {
        this(bloqueanteId != null ? Long.parseLong(bloqueanteId) : null);
    }
}