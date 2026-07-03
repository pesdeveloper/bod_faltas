package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirma;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion in-memory de DocumentoFirmaRepository.
 *
 * Slice 8C-6B-1: Long id, sin idActa, con metodos alineados al modelo.
 */
@Repository
public class InMemoryDocumentoFirmaRepository implements DocumentoFirmaRepository {

    private final AtomicLong sequence = new AtomicLong(1);
    private final Map<Long, FalDocumentoFirma> store = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return sequence.getAndIncrement();
    }

    @Override
    public FalDocumentoFirma guardar(FalDocumentoFirma firma) {
        store.put(firma.getId(), firma);
        return firma;
    }

    @Override
    public Optional<FalDocumentoFirma> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoFirma> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(f -> idDocumento.equals(f.getIdDocumento()))
                .toList();
    }

    @Override
    public Optional<FalDocumentoFirma> buscarPorDocumentoYSeq(Long idDocumento, short seqFirmaReq) {
        return store.values().stream()
                .filter(f -> idDocumento.equals(f.getIdDocumento()) && f.getSeqFirmaReq() == seqFirmaReq)
                .findFirst();
    }
}
