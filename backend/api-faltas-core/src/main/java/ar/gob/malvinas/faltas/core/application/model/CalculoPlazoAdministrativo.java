package ar.gob.malvinas.faltas.core.application.model;

import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;

import java.time.LocalDate;

/**
 * Resultado de un calculo de plazo administrativo.
 *
 * Permite que el consumidor persista la fecha calculada y conozca la cantidad
 * global de dias computables aplicada para trazabilidad.
 *
 * Un vencimiento ya persistido no se recalcula automaticamente por cambios
 * posteriores en el calendario o la configuracion.
 */
public record CalculoPlazoAdministrativo(
        TipoPlazoAdministrativo tipo,
        LocalDate fechaOrigen,
        int diasComputablesAplicados,
        LocalDate fechaVencimiento) {

    public CalculoPlazoAdministrativo {
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        if (fechaOrigen == null) throw new IllegalArgumentException("fechaOrigen es obligatoria");
        if (diasComputablesAplicados < 1 || diasComputablesAplicados > 3650)
            throw new IllegalArgumentException(
                    "diasComputablesAplicados debe estar entre 1 y 3650");
        if (fechaVencimiento == null) throw new IllegalArgumentException("fechaVencimiento es obligatoria");
        if (!fechaVencimiento.isAfter(fechaOrigen))
            throw new IllegalArgumentException(
                    "fechaVencimiento debe ser posterior a fechaOrigen");
    }
}
