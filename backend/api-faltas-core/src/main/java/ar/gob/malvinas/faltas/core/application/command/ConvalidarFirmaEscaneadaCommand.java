package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para convalidar la firma escaneada/olografa visible en un documento adjunto.
 * Slice 8C-6D-1.
 *
 * Si seqFirmaReq es null: convalidacion simple de trazabilidad.
 *   - No cumple FalDocumentoFirmaReq.
 *   - No crea FalDocumentoFirma (seqFirmaReq es primitivo, no puede ser 0 ni null).
 *   - Registra evento DOCFIR para auditoria.
 *
 * Si seqFirmaReq no es null: convalidacion que cumple requisito.
 *   - Crea FalDocumentoFirma con TipoFirma.OLOGRAFA.
 *   - Marca FalDocumentoFirmaReq como FIRMADO.
 *   - Si todos los obligatorios activos quedan FIRMADO, documento pasa ADJUNTO -> FIRMADO.
 */
public record ConvalidarFirmaEscaneadaCommand(
        Long documentoId,
        Short seqFirmaReq,
        Long idFirmante,
        String idUserFirma,
        String referenciaFirmaExt
) {}