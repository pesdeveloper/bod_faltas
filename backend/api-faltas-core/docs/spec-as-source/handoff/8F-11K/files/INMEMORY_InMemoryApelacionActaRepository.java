package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryApelacionActaRepository implements ApelacionActaRepository, ResettableInMemoryRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final ConcurrentHashMap<Long, FalActaApelacion> store = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public synchronized void guardar(FalActaApelacion apelacion) {
        FalActaApelacion stored = store.get(apelacion.getId());
        if (stored != null && stored.getVersionRow() != apelacion.getVersionRow()) {
            throw new ConcurrenciaConflictoException(
                    "FalActaApelacion", apelacion.getId(), stored.getVersionRow(), apelacion.getVersionRow());
        }
        FalActaApelacion copia = apelacion.copia();
        copia.setVersionRow(stored == null ? 0 : stored.getVersionRow() + 1);
        store.put(copia.getId(), copia);
    }

    @Override
    public Optional<FalActaApelacion> buscarActiva(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId) && a.isSiActiva())
                .findFirst()
                .map(FalActaApelacion::copia);
    }

    @Override
    public Optional<FalActaApelacion> buscarUltima(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId))
                .findFirst()
                .map(FalActaApelacion::copia);
    }

    @Override
    public Optional<FalActaApelacion> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaApelacion::copia);
    }

    @Override
    public void reset() {
        store.clear();
        idGen.set(1);
    }

    @Override
    public String nombre() { return "apelaciones"; }

    @Override
    public int size() { return store.size(); }
}