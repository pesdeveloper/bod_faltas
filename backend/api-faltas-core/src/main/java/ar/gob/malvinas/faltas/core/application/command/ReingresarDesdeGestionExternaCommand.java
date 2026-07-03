package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.ModoReingresoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoGestionExterna;

import java.math.BigDecimal;

/**
 * Comando para reingresar un expediente desde gestion externa al circuito interno.
 *
 * Slice 6B: habilita REINGRESO_PARA_REVISION y REINGRESO_SIN_PAGO.
 * Slice 6D-1: habilita SIN_PAGO + REINGRESO_SIN_PAGO y SIN_CAMBIOS + REINGRESO_PARA_REVISION.
 * Slice 6D-2: habilita ABSUELVE + REINGRESO_PARA_NUEVO_FALLO,
 *             CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN,
 *             MODIFICA_MONTO + REINGRESO_CON_DICTAMEN.
 * Reservados: REINGRESO_PARA_CIERRE, REINGRESO_CON_PAGO.
 *
 * motivoReingreso es obligatorio y no puede estar vacio.
 * resultadoGestionExterna es opcional (nullable); si se informa, se persiste en el ciclo externo.
 *   REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN requieren resultado explicito no nulo.
 * montoResultado es obligatorio y mayor a cero cuando resultado == MODIFICA_MONTO.
 *   Para todos los demas resultados es nullable.
 *   Corresponde al campo monto_resultado de fal_acta_gestion_externa en MariaDB.
 * observaciones es opcional (nullable).
 */
public record ReingresarDesdeGestionExternaCommand(
        Long actaId,
        ModoReingresoGestionExterna modoReingresoGestionExterna,
        String motivoReingreso,
        ResultadoGestionExterna resultadoGestionExterna,
        String observaciones,
        BigDecimal montoResultado
) {}
