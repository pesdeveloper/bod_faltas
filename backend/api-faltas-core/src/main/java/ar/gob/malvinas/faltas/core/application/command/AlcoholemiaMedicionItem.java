package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoCualitativoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadMedidaAlcoholemia;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bloque tipado de una medición de alcoholemia del acta de tránsito, agregado en Wave 4
 * Parte 3A. N mediciones por acta; {@code ordenMedicion} positivo y único por acta
 * ({@code fal_acta_transito_alcoholemia}). Como máximo una con
 * {@code siResultadoFinal=true} por acta (lo enforcea el repositorio JDBC).
 */
public record AlcoholemiaMedicionItem(
        @NotNull Short ordenMedicion,
        @NotNull TipoPruebaAlcoholemia tipoPrueba,
        ResultadoCualitativoAlcoholemia resultadoCualitativo,
        BigDecimal resultadoNumerico,
        UnidadMedidaAlcoholemia unidadMedida,
        Long idAlcoholimetro,
        Short verAlcoholimetro,
        boolean siResultadoFinal,
        LocalDateTime fhMedicion
) {}
