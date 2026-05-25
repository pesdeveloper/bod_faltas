package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

public record GenerarLoteCorreoPostalRequest(String tipo, List<String> notificacionIds) {
}
