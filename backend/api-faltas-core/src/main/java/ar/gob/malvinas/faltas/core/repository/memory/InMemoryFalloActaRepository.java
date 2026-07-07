package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryFalloActaRepository implements FalloActaRepository, ResettableInMemoryRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final Map<Long, FalActaFallo> storeById = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public synchronized FalActaFallo guardar(FalActaFallo fallo) {
        FalActaFallo stored = storeById.get(fallo.getId());
        if (stored != null && stored.getVersionRow() != fallo.getVersionRow()) {
            throw new ConcurrenciaConflictoException("FalActaFallo", fallo.getId(), stored.getVersionRow(), fallo.getVersionRow());
        }
        FalActaFallo copia = fallo.copia();
        copia.setVersionRow(stored == null ? 0 : stored.getVersionRow() + 1);
        storeById.put(copia.getId(), copia);
        return copia.copia();
    }

    @Override
    public synchronized FalActaFallo guardarComoVigente(FalActaFallo fallo) {
        storeById.values().stream()
                .filter(f -> f.getActaId().equals(fallo.getActaId()) && f.isSiVigente())
                .forEach(f -> {
                    f.setSiVigente(false);
                    f.setEstadoFallo(EstadoFalloActa.REEMPLAZADO);
                });
        fallo.setSiVigente(true);
        fallo.setVersionRow(0);
        storeById.put(fallo.getId(), fallo.copia());
        return fallo.copia();
    }

    @Override
    public void rechazarSiYaExisteVigente(long actaId) {
        boolean existeVigente = storeById.values().stream()
                .anyMatch(f -> f.getActaId().equals(actaId) && f.isSiVigente());
        if (existeVigente) {
            throw new PrecondicionVioladaException(
                    "Ya existe un fallo vigente para el acta " + actaId);
        }
    }

    @Override
    public Optional<FalActaFallo> buscarActivo(Long actaId) {
        return storeById.values().stream()
                .filter(f -> f.getActaId().equals(actaId) && f.isSiVigente())
                .findFirst()
                .map(FalActaFallo::copia);
    }

    @Override
    public Optional<FalActaFallo> findById(Long id) {
        return Optional.ofNullable(storeById.get(id)).map(FalActaFallo::copia);
    }

    @Override
    public List<FalActaFallo> findByActaId(Long actaId) {
        return storeById.values().stream()
                .filter(f -> f.getActaId().equals(actaId))
                .map(FalActaFallo::copia)
                .collect(Collectors.toList());
    }

    @Override
    public void reset() { storeById.clear(); idGen.set(1); }

    @Override
    public String nombre() { return "FalloActa"; }

    @Override
    public int size() { return storeById.size(); }
}