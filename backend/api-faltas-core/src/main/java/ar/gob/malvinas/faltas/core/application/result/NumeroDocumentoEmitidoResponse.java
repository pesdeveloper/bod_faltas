package ar.gob.malvinas.faltas.core.application.result;

/**
 * Resultado de la emision de un numero de talonario DOCUMENTO.
 *
 * Analogo a NumeroActaEmitidoResponse pero para documentos.
 * Contiene el correlativo tecnico (nroTalonario) y el numero visible (nroDocu).
 *
 * Slice 8C-5A: numeracion documental reusable.
 */
public record NumeroDocumentoEmitidoResponse(
        Long movimientoId,
        Long idTalonario,
        int nroTalonario,
        String nroDocu
) {}
