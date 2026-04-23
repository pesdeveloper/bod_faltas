package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;

public record PagoInformadoResponse(
        LocalDateTime fechaInformado,
        ComprobanteMockResponse comprobante) {
}

