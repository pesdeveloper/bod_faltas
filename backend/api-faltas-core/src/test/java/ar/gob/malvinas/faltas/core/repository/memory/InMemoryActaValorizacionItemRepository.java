package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.5: historial versionado hijo de
 * {@code fal_acta_articulo_infringido}. Los items son inmutables (siempre INSERT).
 */
@Repository
@Profile("test")
public class InMemoryActaValorizacionItemRepository
        implements ActaValorizacionItemRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaValorizacionItem> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public synchronized FalActaValorizacionItem save(FalActaValorizacionItem item) {
        if (store.containsKey(item.getId())) {
            throw new PrecondicionVioladaException(
                    "fal_acta_valorizacion_item es inmutable: ya existe id=" + item.getId());
        }
        if (item.isSiActual()) {
            boolean otraActualExiste = store.values().stream()
                    .anyMatch(i -> i.getActaArticuloId().equals(item.getActaArticuloId()) && i.isSiActual());
            if (otraActualExiste) {
                throw new PrecondicionVioladaException(
                        "Ya existe un item actual para actaArticuloId=" + item.getActaArticuloId());
            }
        }
        store.put(item.getId(), item);
        return item;
    }

    @Override
    public Optional<FalActaValorizacionItem> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<FalActaValorizacionItem> findActualByActaArticulo(Long actaArticuloId) {
        List<FalActaValorizacionItem> actuales = store.values().stream()
                .filter(i -> actaArticuloId.equals(i.getActaArticuloId()) && i.isSiActual())
                .toList();
        if (actuales.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + actuales.size() + " items actuales para actaArticuloId=" + actaArticuloId);
        }
        return actuales.isEmpty() ? Optional.empty() : Optional.of(actuales.get(0));
    }

    @Override
    public synchronized void marcarNoActual(Long id) {
        FalActaValorizacionItem item = store.get(id);
        if (item != null) item.marcarNoActual();
    }

    @Override
    public List<FalActaValorizacionItem> findAllByActaArticulo(Long actaArticuloId) {
        return store.values().stream()
                .filter(i -> actaArticuloId.equals(i.getActaArticuloId()))
                .sorted(Comparator.comparingInt(FalActaValorizacionItem::getVersionItem))
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaValorizacionItem> findActualesByActaArticuloIds(List<Long> actaArticuloIds) {
        return store.values().stream()
                .filter(i -> i.isSiActual() && actaArticuloIds.contains(i.getActaArticuloId()))
                .collect(Collectors.toList());
    }

    public void cargarSeed(List<FalActaValorizacionItem> lista) {
        long maxId = 0;
        for (FalActaValorizacionItem i : lista) {
            store.put(i.getId(), i);
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
