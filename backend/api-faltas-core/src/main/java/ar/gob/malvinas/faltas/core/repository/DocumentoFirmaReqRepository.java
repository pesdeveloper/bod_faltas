package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de requisitos de firma de documentos concretos.
 *
 * Refleja fal_documento_firma_req.
 * Slice 8C-4.
 */
public interface DocumentoFirmaReqRepository {

    Long nextId();

    FalDocumentoFirmaReq guardar(FalDocumentoFirmaReq req);

    Optional<FalDocumentoFirmaReq> buscarPorId(Long id);

    List<FalDocumentoFirmaReq> listarPorDocumento(Long documentoId);

    boolean existePorDocumento(Long documentoId);

    Optional<FalDocumentoFirmaReq> buscarPorDocumentoYSeq(Long documentoId, short seqFirmaReq);
}
