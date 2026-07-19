package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;

/**
 * Respuesta del endpoint de integracion controlada para la aplicacion de Firmas.
 *
 * Permite que Firmas conozca el numero asignado y el estado resultante
 * antes de proceder con render de contenido, calculo de hash y firma.
 *
 * yaEstabaNumerado=true indica idempotencia: el numero ya existia y no se
 * consumio un nuevo correlativo.
 *
 * Slice D-18: EmitirNumeroDocumento - endpoint controlado para Firmas.
 */
public record NumerarDocumentoParaFirmasResponse(
        Long documentoId,
        boolean yaEstabaNumerado,
        String nroDocu,
        Long idTalonario,
        Integer nroTalonarioUsado,
        MomentoNumeracionDocu momentoAplicado,
        EstadoDocu estadoDocu
) {}
