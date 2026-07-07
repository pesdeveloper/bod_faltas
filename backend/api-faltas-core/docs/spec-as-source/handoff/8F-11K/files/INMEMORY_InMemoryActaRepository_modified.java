package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryActaRepository implements ActaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActa> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActa guardar(FalActa acta) {
        FalActa existing = store.get(acta.getId());
        if (existing == null) {
            FalActa copia = acta.copia();
            copia.setVersionRow(0);
            store.put(acta.getId(), copia);
            return copia;
        } else {
            if (existing.getVersionRow() != acta.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActa", acta.getId(),
                        existing.getVersionRow(), acta.getVersionRow());
            }
            FalActa copia = acta.copia();
            copia.setVersionRow(existing.getVersionRow() + 1);
            store.put(acta.getId(), copia);
            return copia;
        }
    }

    @Override
    public Optional<FalActa> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActa::copia);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico) {
        if (uuidTecnico == null) return Optional.empty();
        return store.values().stream()
                .filter(a -> uuidTecnico.equals(a.getUuidTecnico()))
                .map(FalActa::copia)
                .findFirst();
    }

    @Override
    public List<FalActa> listarTodas() {
        return store.values().stream().map(FalActa::copia).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "actas"; }

    @Override
    public int size() { return store.size(); }
}
