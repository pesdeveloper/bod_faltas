package ar.gob.malvinas.faltas.prototipo.domain;

import java.time.LocalDateTime;

public record ActaEventoMock(
        String id,
        String actaId,
        LocalDateTime fechaHora,
        String tipoEvento,
        String bloqueOrigen,
        String bloqueDestino,
        String descripcion) {
}
