package ar.gob.malvinas.faltas.core.application.command;

import java.math.BigDecimal;

/**
 * Comando para dictar fallo condenatorio sobre un acta.
 *
 * Precondiciones: acta en bloque ANAL, sin fallo activo previo,
 * sin pago voluntario confirmado, montoCondena > 0.
 * Efectos: crea FalActaFallo (CONDENATORIO), crea documento FALLO_CONDENATORIO,
 * registra FALCON + DOCGEN. No cierra el acta. No asigna condena firme.
 */
public record DictarFalloCondenatorioCommand(
        Long actaId,
        BigDecimal montoCondena,
        String fundamentos,
        String observaciones
) {}

