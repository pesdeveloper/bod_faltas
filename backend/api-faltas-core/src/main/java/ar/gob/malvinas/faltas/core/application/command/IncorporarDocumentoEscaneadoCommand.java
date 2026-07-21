package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;

/**
 * Comando para incorporar un documento escaneado/adjunto externo al expediente.
 * Slice 8C-6D-1.
 *
 * El documento externo no se genera desde plantilla (plantillaId puede ser null).
 * No se numera automaticamente. No se emite automaticamente.
 * storageKey, hashDocu obligatorios.
 */
public record IncorporarDocumentoEscaneadoCommand(
        Long idActa,
        TipoDocu tipoDocu,
        String storageKey,
        String hashDocu,
        String idUserAlta,
        Long plantillaId
) {}
