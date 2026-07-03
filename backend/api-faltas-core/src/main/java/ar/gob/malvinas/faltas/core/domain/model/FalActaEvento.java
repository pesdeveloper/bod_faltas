package ar.gob.malvinas.faltas.core.domain.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;

import java.time.LocalDateTime;

/**
 * Evento de dominio del expediente. Inmutable y append-only.
 */
public record FalActaEvento(
        String id,
        Long idActa,
        TipoEventoActa tipoEvt,
        LocalDateTime fechaEvento,
        int ordenLogico,
        String idDocumento,
        String idNotificacion,
        String idOperador,
        String descripcion,
        String payload
) {
    public FalActaEvento {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id del evento requerido");
        if (idActa == null) throw new IllegalArgumentException("idActa requerido");
        if (tipoEvt == null) throw new IllegalArgumentException("tipoEvt requerido");
        if (fechaEvento == null) throw new IllegalArgumentException("fechaEvento requerido");
    }
}
