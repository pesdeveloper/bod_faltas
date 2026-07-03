package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna;

/**
 * Comando para derivar un expediente a gestion externa (apremio, juzgado de paz u otro).
 *
 * Slice 6A: derivacion unicamente. No implementa reingreso ni pago apremio.
 *
 * tipoGestionExterna y motivoDerivacion son obligatorios.
 */
public record DerivarGestionExternaCommand(
        Long actaId,
        TipoGestionExterna tipoGestionExterna,
        String motivoDerivacion,
        String observaciones
) {}

