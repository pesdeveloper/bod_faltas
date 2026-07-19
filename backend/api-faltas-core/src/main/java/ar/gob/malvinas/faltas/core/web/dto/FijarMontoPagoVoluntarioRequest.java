package ar.gob.malvinas.faltas.core.web.dto;

import java.math.BigDecimal;

public record FijarMontoPagoVoluntarioRequest(BigDecimal monto, String observaciones) {}
