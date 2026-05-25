package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

public record BandejaResponse(
        String codigo,
        String label,
        int cantidad,
        List<SubBandejaResumenResponse> subBandejas) {
}
