package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryActaValorizacionItemRepository
        implements ActaValorizacionItemRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaValorizacionItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaValorizacionItem save(FalActaValorizacionItem item) {
        FalActaValorizacionItem copia = item.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaValorizacionItem> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaValorizacionItem::copia);
    }

    @Override
    public List<FalActaValorizacionItem> findByValorizacionId(Long valorizacionId) {
        return store.values().stream()
                .filter(i -> valorizacionId.equals(i.getValorizacionId()))
                .map(FalActaValorizacionItem::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaValorizacionItem> findByActaArticuloId(Long actaArticuloId) {
        return store.values().stream()
                .filter(i -> actaArticuloId.equals(i.getActaArticuloId()))
                .map(FalActaValorizacionItem::copia)
                .collect(Collectors.toList());
    }

    public void cargarSeed(List<FalActaValorizacionItem> lista) {
        long maxId = 0;
        for (FalActaValorizacionItem i : lista) {
            store.put(i.getId(), i.copia());
            if (i.getId() > maxId) maxId = i.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "valorizacion-items"; }

    @Override
    public int size() { return store.size(); }
}
