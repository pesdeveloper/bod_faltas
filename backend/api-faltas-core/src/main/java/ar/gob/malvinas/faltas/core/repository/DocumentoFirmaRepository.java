package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de firmas documentales.
 *
 * Slice 8C-6B-1: alineado a Long id, sin idActa en la entidad.
 * Reemplazable por implementacion JDBC sin tocar servicios (Slice 9).
 */
public interface DocumentoFirmaRepository {

    Long nextId();

    FalDocumentoFirma guardar(FalDocumentoFirma firma);

    Optional<FalDocumentoFirma> buscarPorId(Long id);

    List<FalDocumentoFirma> buscarPorDocumento(Long idDocumento);

    Optional<FalDocumentoFirma> buscarPorDocumentoYSeq(Long idDocumento, short seqFirmaReq);
}
