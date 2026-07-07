package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryActaValorizacionRepository
        implements ActaValorizacionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaValorizacion> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public synchronized FalActaValorizacion save(FalActaValorizacion valorizacion) {
        FalActaValorizacion existing = store.get(valorizacion.getId());
        if (existing == null) {
            FalActaValorizacion copia = valorizacion.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existing.getVersionRow() != valorizacion.getVersionRow()) {
            throw new ConcurrenciaConflictoException("FalActaValorizacion", valorizacion.getId(),
                    existing.getVersionRow(), valorizacion.getVersionRow());
        }
        FalActaValorizacion copia = valorizacion.copia();
        copia.setVersionRow(existing.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaValorizacion> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaValorizacion::copia);
    }

    @Override
    public List<FalActaValorizacion> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId()))
                .map(FalActaValorizacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaValorizacion> findByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId()) && tipo == v.getTipoValorizacionActa())
                .map(FalActaValorizacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaValorizacion> findVigenteByActaIdAndTipo(Long actaId, TipoValorizacionActa tipo) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId())
                        && tipo == v.getTipoValorizacionActa()
                        && v.isSiVigente())
                .map(FalActaValorizacion::copia)
                .findFirst();
    }

    @Override
    public List<FalActaValorizacion> findAll() {
        return store.values().stream().map(FalActaValorizacion::copia).collect(Collectors.toList());
    }

    public void cargarSeed(List<FalActaValorizacion> lista) {
        long maxId = 0;
        for (FalActaValorizacion v : lista) {
            store.put(v.getId(), v.copia());
            if (v.getId() > maxId) maxId = v.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "valorizaciones"; }

    @Override
    public int size() { return store.size(); }
}
