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

    /**
     * Busca una firma por su referencia externa unica emitida por la aplicacion de Firmas.
     * Usado para idempotencia del callback firmar-real.
     */
    Optional<FalDocumentoFirma> buscarPorReferenciaFirmaExt(String referenciaFirmaExt);

    /**
     * Guarda la firma de forma atomica solo si no existe ninguna con la misma referenciaFirmaExt.
     *
     * Si ya existe: devuelve la firma existente con yaExistia=true, sin modificar nada.
     * Si no existe: persiste la firma y devuelve yaExistia=false.
     *
     * La verificacion y la escritura son atomicas: dos llamadas concurrentes con la misma
     * referenciaFirmaExt generan exactamente una firma persistida.
     *
     * FIX-FALLO-NOTI-01-R2: idempotencia concurrente.
     */
    DocumentoFirmaSaveResult guardarSiAusentePorReferencia(FalDocumentoFirma firma);
}
