package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaReqRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion in-memory de DocumentoFirmaReqRepository.
 *
 * Usa AtomicLong para id sintetico.
 * Usa ConcurrentHashMap<Long, FalDocumentoFirmaReq>.
 * No usa JDBC. No persiste en archivo. No toca MariaDB.
 * Slice 8C-4.
 */
@Repository
public class InMemoryDocumentoFirmaReqRepository implements DocumentoFirmaReqRepository {

    private final Map<Long, FalDocumentoFirmaReq> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalDocumentoFirmaReq guardar(FalDocumentoFirmaReq req) {
        store.put(req.getId(), req);
        return req;
    }

    @Override
    public Optional<FalDocumentoFirmaReq> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoFirmaReq> listarPorDocumento(Long documentoId) {
        return store.values().stream()
                .filter(r -> documentoId.equals(r.getDocumentoId()))
                .toList();
    }

    @Override
    public boolean existePorDocumento(Long documentoId) {
        return store.values().stream()
                .anyMatch(r -> documentoId.equals(r.getDocumentoId()));
    }

    @Override
    public Optional<FalDocumentoFirmaReq> buscarPorDocumentoYSeq(Long documentoId, short seqFirmaReq) {
        return store.values().stream()
                .filter(r -> documentoId.equals(r.getDocumentoId()) && r.getSeqFirmaReq() == seqFirmaReq)
                .findFirst();
    }
}
