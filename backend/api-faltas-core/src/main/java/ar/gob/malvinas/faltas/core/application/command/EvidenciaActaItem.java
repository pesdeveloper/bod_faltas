package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Item de evidencia para registrar al labrar un acta.
 * storageKey: referencia al archivo en storage (no binario).
 * No crea FalDocumentoFirma ni FalDocumentoFirmaReq.
 */
public record EvidenciaActaItem(
        @NotNull TipoEvidenciaActa tipoEvid,
        @NotBlank String storageKey
) {}
