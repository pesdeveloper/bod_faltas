package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Referencia opcional al repositorio de valorizaciones para verificar
     * la inmutabilidad post-confirmacion de items. Null cuando se usa en tests
     * que no prueban inmutabilidad.
     */
    private ActaValorizacionRepository valorizacionRepo;

    /** Constructor sin-arg para tests de compatibilidad (valorizacionRepo=null). */
    public InMemoryActaValorizacionItemRepository() {
        this.valorizacionRepo = null;
    }

    /** Constructor usado por Spring con inyeccion de dependencia. */
    @Autowired
    public InMemoryActaValorizacionItemRepository(ActaValorizacionRepository valorizacionRepo) {
        this.valorizacionRepo = valorizacionRepo;
    }

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaValorizacionItem save(FalActaValorizacionItem item) {
        // Guard de inmutabilidad: items de madre CONFIRMADA/REEMPLAZADA/ANULADA son inmutables
        if (valorizacionRepo != null) {
            FalActaValorizacion val = valorizacionRepo.findById(item.getValorizacionId())
                    .orElseThrow(() -> new ValorizacionNoEncontradaException(item.getValorizacionId()));
            if (val.getEstadoValorizacion() != EstadoValorizacion.PRELIMINAR) {
                throw new PrecondicionVioladaException(
                        "Los items de una valorizacion " + val.getEstadoValorizacion()
                                + " son inmutables. valorizacionId=" + item.getValorizacionId());
            }
        }
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
