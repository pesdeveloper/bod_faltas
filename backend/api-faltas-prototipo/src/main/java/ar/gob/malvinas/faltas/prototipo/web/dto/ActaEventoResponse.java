package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;

public record ActaEventoResponse(
        String id,
        LocalDateTime fechaHora,
        String tipoEvento,
        String bloqueOrigen,
        String bloqueDestino,
        String descripcion) {
}
