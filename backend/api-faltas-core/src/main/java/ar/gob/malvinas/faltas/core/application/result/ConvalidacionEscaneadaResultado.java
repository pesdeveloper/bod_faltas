package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;

/**
 * Resultado de convalidacion de firma escaneada.
 * Slice 8C-6D-1.
 *
 * firma: null si la convalidacion fue simple (sin seqFirmaReq).
 * firma: FalDocumentoFirma si la convalidacion cumplio un FalDocumentoFirmaReq.
 */
public record ConvalidacionEscaneadaResultado(
        FalDocumento documento,
        FalDocumentoFirma firma
) {}