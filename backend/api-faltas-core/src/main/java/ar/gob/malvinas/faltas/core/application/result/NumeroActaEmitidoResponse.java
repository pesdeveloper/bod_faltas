package ar.gob.malvinas.faltas.core.application.result;

public record NumeroActaEmitidoResponse(
        Long movimientoId,
        Long idTalonario,
        int nroTalonarioUsado,
        String nroActa
) {}
