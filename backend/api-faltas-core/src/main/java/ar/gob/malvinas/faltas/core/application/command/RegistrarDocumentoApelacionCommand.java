package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenPresentacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoApelacion;

/**
 * Comando para registrar un documento/adjunto dentro de una apelacion.
 * Requiere al menos documentoId o storageKey (no vacio).
 */
public record RegistrarDocumentoApelacionCommand(
        Long apelacionId,
        TipoDocumentoApelacion tipoDocApelacion,
        OrigenPresentacion origenPresentacion,
        Long documentoId,
        String storageKey,
        String nombreArchivo,
        Short mimeType,
        Long tamanioBytes,
        String idUserAlta
) {}
