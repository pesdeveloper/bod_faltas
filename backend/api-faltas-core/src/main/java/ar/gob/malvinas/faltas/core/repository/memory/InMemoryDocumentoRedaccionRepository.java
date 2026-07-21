package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio InMemory de FalDocumentoRedaccion con semantica OCC real (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-REDAC-OCC):
 *   1. guardar version 0 (nueva).
 *   2. leer devuelve copia detached — mutar la copia no afecta el store.
 *   3. guardar existente: verifica versionRow; si coincide, incrementa y persiste copia.
 *   4. guardar con versionRow desactualizado lanza ConcurrenciaConflictoException.
 */
@Repository
public class InMemoryDocumentoRedaccionRepository implements DocumentoRedaccionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumentoRedaccion> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public Long nextId() { return counter.getAndIncrement(); }

    @Override
    public synchronized FalDocumentoRedaccion guardar(FalDocumentoRedaccion r) {
        FalDocumentoRedaccion existing = store.get(r.getId());
        if (existing == null) {
            if (r.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalDocumentoRedaccion", r.getId(),
                        0, r.getVersionRow());
            }
            store.put(r.getId(), r.copia());
            return r;
        }
        if (existing.getVersionRow() != r.getVersionRow()) {
            throw new ConcurrenciaConflictoException("FalDocumentoRedaccion", r.getId(),
                    existing.getVersionRow(), r.getVersionRow());
        }
        r.setVersionRow(existing.getVersionRow() + 1);
        store.put(r.getId(), r.copia());
        return r;
    }

    @Override
    public Optional<FalDocumentoRedaccion> buscarPorId(Long id) {
        FalDocumentoRedaccion stored = store.get(id);
        return stored == null ? Optional.empty() : Optional.of(stored.copia());
    }

    @Override
    public List<FalDocumentoRedaccion> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(r -> r.getIdDocumento().equals(idDocumento))
                .map(FalDocumentoRedaccion::copia)
                .toList();
    }

    @Override
    public void reset() {
        store.clear();
    }

    @Override
    public String nombre() { return "DocumentoRedaccion"; }

    @Override
    public int size() { return (int) store.size(); }
}
