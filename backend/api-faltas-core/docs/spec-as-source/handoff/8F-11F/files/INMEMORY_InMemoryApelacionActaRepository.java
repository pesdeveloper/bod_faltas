package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryApelacionActaRepository implements ApelacionActaRepository, ResettableInMemoryRepository {

    private final ConcurrentHashMap<Long, FalActaApelacion> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public void guardar(FalActaApelacion apelacion) {
        FalActaApelacion existing = store.get(apelacion.getId());
        if (existing == null) {
            FalActaApelacion copia = apelacion.copia();
            copia.setVersionRow(0);
            store.put(apelacion.getId(), copia);
        } else {
            if (existing.getVersionRow() != apelacion.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActaApelacion", apelacion.getId(),
                        existing.getVersionRow(), apelacion.getVersionRow());
            }
            FalActaApelacion copia = apelacion.copia();
            copia.setVersionRow(existing.getVersionRow() + 1);
            store.put(apelacion.getId(), copia);
        }
    }

    @Override
    public Optional<FalActaApelacion> findById(Long id) {
        FalActaApelacion a = store.get(id);
        return a == null ? Optional.empty() : Optional.of(a.copia());
    }

    @Override
    public List<FalActaApelacion> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId))
                .sorted(Comparator.comparing(FalActaApelacion::getId))
                .map(FalActaApelacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaApelacion> findByFalloId(Long falloId) {
        return store.values().stream()
                .filter(a -> a.getFalloId().equals(falloId))
                .sorted(Comparator.comparing(FalActaApelacion::getId))
                .map(FalActaApelacion::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaApelacion> buscarActiva(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId)
                        && (a.getEstadoApelacion() == EstadoApelacionActa.PRESENTADA
                            || a.getEstadoApelacion() == EstadoApelacionActa.EN_ANALISIS))
                .findFirst()
                .map(FalActaApelacion::copia);
    }

    @Override
    public Optional<FalActaApelacion> buscarUltima(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId))
                .max(Comparator.comparing(FalActaApelacion::getId))
                .map(FalActaApelacion::copia);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "apelaciones"; }

    @Override
    public int size() { return store.size(); }
}