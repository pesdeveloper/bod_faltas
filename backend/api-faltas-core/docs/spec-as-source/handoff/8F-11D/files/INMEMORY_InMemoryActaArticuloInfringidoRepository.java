package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryActaArticuloInfringidoRepository
        implements ActaArticuloInfringidoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaArticuloInfringido> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaArticuloInfringido save(FalActaArticuloInfringido articulo) {
        FalActaArticuloInfringido copia = articulo.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaArticuloInfringido> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaArticuloInfringido::copia);
    }

    @Override
    public List<FalActaArticuloInfringido> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(a -> actaId.equals(a.getActaId()))
                .map(FalActaArticuloInfringido::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaArticuloInfringido> findActivosByActaId(Long actaId) {
        return store.values().stream()
                .filter(a -> actaId.equals(a.getActaId()) && a.isSiActivo())
                .map(FalActaArticuloInfringido::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaArticuloInfringido> findActivoByActaAndArticulo(Long actaId, Long articuloId) {
        return store.values().stream()
                .filter(a -> actaId.equals(a.getActaId())
                        && articuloId.equals(a.getArticuloId())
                        && a.isSiActivo())
                .map(FalActaArticuloInfringido::copia)
                .findFirst();
    }

    @Override
    public boolean existsActivo(Long actaId, Long articuloId) {
        return store.values().stream()
                .anyMatch(a -> actaId.equals(a.getActaId())
                        && articuloId.equals(a.getArticuloId())
                        && a.isSiActivo());
    }

    public void cargarSeed(List<FalActaArticuloInfringido> lista) {
        long maxId = 0;
        for (FalActaArticuloInfringido a : lista) {
            store.put(a.getId(), a.copia());
            if (a.getId() > maxId) maxId = a.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "acta-articulos-infringidos"; }

    @Override
    public int size() { return store.size(); }
}
