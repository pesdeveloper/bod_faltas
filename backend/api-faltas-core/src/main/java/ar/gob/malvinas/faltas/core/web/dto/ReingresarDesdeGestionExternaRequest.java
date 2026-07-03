package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.enums.ModoReingresoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoGestionExterna;

import java.math.BigDecimal;

/**
 * DTO para la solicitud de reingreso desde gestion externa.
 *
 * Slice 6B: habilita REINGRESO_PARA_REVISION y REINGRESO_SIN_PAGO.
 * Slice 6D-2: agrega montoResultado para MODIFICA_MONTO.
 */
public record ReingresarDesdeGestionExternaRequest(
        ModoReingresoGestionExterna modoReingresoGestionExterna,
        String motivoReingreso,
        ResultadoGestionExterna resultadoGestionExterna,
        String observaciones,
        BigDecimal montoResultado
) {}