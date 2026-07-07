package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentoRedaccionRepository implements DocumentoRedaccionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumentoRedaccion> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public Long nextId() { return counter.getAndIncrement(); }

    @Override
    public FalDocumentoRedaccion guardar(FalDocumentoRedaccion r) {
        store.put(r.getId(), r);
        return r;
    }

    @Override
    public Optional<FalDocumentoRedaccion> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoRedaccion> buscarPorDocumento(Long idDocumento) {
        return store.values().stream()
                .filter(r -> r.getIdDocumento().equals(idDocumento))
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