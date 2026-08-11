package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.8: cabecera unica por acta.
 */
@Repository
@Profile("test")
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
        FalActaValorizacion existente = store.get(valorizacion.getId());

        boolean otraParaMismaActa = store.values().stream()
                .anyMatch(v -> !v.getId().equals(valorizacion.getId())
                        && v.getActaId().equals(valorizacion.getActaId()));
        if (otraParaMismaActa) {
            throw new PrecondicionVioladaException(
                    "Ya existe una cabecera de valorizacion para actaId=" + valorizacion.getActaId());
        }

        if (existente == null) {
            FalActaValorizacion copia = valorizacion.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existente.getVersionRow() != valorizacion.getVersionRow()) {
            throw new ConcurrenciaConflictoException("FalActaValorizacion", valorizacion.getId(),
                    existente.getVersionRow(), valorizacion.getVersionRow());
        }
        FalActaValorizacion copia = valorizacion.copia();
        copia.setVersionRow(existente.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaValorizacion> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaValorizacion::copia);
    }

    @Override
    public Optional<FalActaValorizacion> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(v -> actaId.equals(v.getActaId()))
                .findFirst()
                .map(FalActaValorizacion::copia);
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
