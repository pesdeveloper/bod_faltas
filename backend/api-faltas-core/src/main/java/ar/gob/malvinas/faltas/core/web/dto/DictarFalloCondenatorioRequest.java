package ar.gob.malvinas.faltas.core.web.dto;

import java.math.BigDecimal;

public record DictarFalloCondenatorioRequest(
        BigDecimal montoCondena,
        String fundamentos,
        String observaciones
) {}
