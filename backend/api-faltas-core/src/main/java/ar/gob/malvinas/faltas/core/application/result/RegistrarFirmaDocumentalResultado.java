package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;

/**
 * Resultado de registrar una firma documental real.
 *
 * yaExistia = true  -> idempotencia: la firma ya habia sido registrada con la misma referenciaFirmaExt.
 *                      El callback debe responder HTTP 200.
 * yaExistia = false -> primera creacion. El callback debe responder HTTP 201.
 */
public record RegistrarFirmaDocumentalResultado(
        FalDocumentoFirma firma,
        boolean yaExistia
) {}
