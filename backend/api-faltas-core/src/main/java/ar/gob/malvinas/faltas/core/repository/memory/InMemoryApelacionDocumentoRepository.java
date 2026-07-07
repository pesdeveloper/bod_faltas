package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacionDocumento;
import ar.gob.malvinas.faltas.core.repository.ApelacionDocumentoRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementacion InMemory de ApelacionDocumentoRepository (append-only).
 */
@Repository
public class InMemoryApelacionDocumentoRepository
        implements ApelacionDocumentoRepository, ResettableInMemoryRepository {

    private final ConcurrentHashMap<Long, FalActaApelacionDocumento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public FalActaApelacionDocumento guardar(FalActaApelacionDocumento doc) {
        FalActaApelacionDocumento copia = doc.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaApelacionDocumento> findById(Long id) {
        FalActaApelacionDocumento d = store.get(id);
        return d == null ? Optional.empty() : Optional.of(d.copia());
    }

    @Override
    public List<FalActaApelacionDocumento> findByApelacionId(Long apelacionId) {
        return store.values().stream()
                .filter(d -> d.getApelacionId().equals(apelacionId))
                .sorted(Comparator.comparing(FalActaApelacionDocumento::getId))
                .map(FalActaApelacionDocumento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "apelacion-documentos"; }

    @Override
    public int size() { return store.size(); }
}