package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio InMemory de FalDocumento con semantica OCC real (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-DOC-01):
 *   1. guardar version 0 (nueva).
 *   2. leer devuelve copia detached — mutar la copia no afecta el store.
 *   3. guardar existente: verifica versionRow; si coincide, incrementa y persiste copia.
 *   4. guardar con versionRow desactualizado lanza ConcurrenciaConflictoException.
 */
@Repository
public class InMemoryDocumentoRepository implements DocumentoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public synchronized FalDocumento guardar(FalDocumento documento) {
        FalDocumento existing = store.get(documento.getId());
        if (existing == null) {
            if (documento.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalDocumento", documento.getId(),
                        0, documento.getVersionRow());
            }
            store.put(documento.getId(), documento.copia());
            return documento;
        }
        if (existing.getVersionRow() != documento.getVersionRow())
            throw new ConcurrenciaConflictoException("FalDocumento", documento.getId(),
                    existing.getVersionRow(), documento.getVersionRow());
        documento.setVersionRow(existing.getVersionRow() + 1);
        store.put(documento.getId(), documento.copia());
        return documento;
    }

    @Override
    public Optional<FalDocumento> buscarPorId(Long id) {
        FalDocumento stored = store.get(id);
        return stored == null ? Optional.empty() : Optional.of(stored.copia());
    }

    @Override
    public List<FalDocumento> buscarPorActa(Long idActa) {
        return store.values().stream()
                .filter(d -> idActa.equals(d.getIdActa()))
                .map(FalDocumento::copia)
                .toList();
    }

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public void reset() {
        store.clear();
    }

    @Override
    public String nombre() { return "documentos"; }

    @Override
    public int size() { return (int) store.size(); }
}
