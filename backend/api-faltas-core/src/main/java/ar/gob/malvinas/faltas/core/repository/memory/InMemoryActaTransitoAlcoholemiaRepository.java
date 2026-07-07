package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoAlcoholemiaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryActaTransitoAlcoholemiaRepository
        implements ActaTransitoAlcoholemiaRepository, ResettableInMemoryRepository {

    private final List<FalActaTransitoAlcoholemia> store = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Object lockFinal = new Object();

    @Override public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public FalActaTransitoAlcoholemia guardar(FalActaTransitoAlcoholemia medicion) {
        store.removeIf(m -> m.getId().equals(medicion.getId()));
        store.add(medicion.copia());
        return findById(medicion.getId()).orElseThrow();
    }

    @Override
    public Optional<FalActaTransitoAlcoholemia> findById(Long id) {
        return store.stream().filter(m -> id.equals(m.getId())).findFirst().map(FalActaTransitoAlcoholemia::copia);
    }

    @Override
    public List<FalActaTransitoAlcoholemia> findByActaId(Long actaId) {
        return store.stream().filter(m -> actaId.equals(m.getActaId()))
                .map(FalActaTransitoAlcoholemia::copia).toList();
    }

    @Override
    public Optional<FalActaTransitoAlcoholemia> findResultadoFinalByActaId(Long actaId) {
        return store.stream()
                .filter(m -> actaId.equals(m.getActaId()) && m.isSiResultadoFinal())
                .findFirst().map(FalActaTransitoAlcoholemia::copia);
    }

    @Override
    public boolean existsOrdenByActaId(Long actaId, short orden) {
        return store.stream().anyMatch(m -> actaId.equals(m.getActaId()) && m.getOrdenMedicion() == orden);
    }

    @Override
    public FalActaTransitoAlcoholemia marcarResultadoFinalAtomicamente(Long actaId, Long medicionId) {
        synchronized (lockFinal) {
            // Desmarcar final anterior
            store.stream()
                    .filter(m -> actaId.equals(m.getActaId()) && m.isSiResultadoFinal())
                    .forEach(m -> {
                        m.setSiResultadoFinal(false);
                        store.removeIf(x -> x.getId().equals(m.getId()));
                        store.add(m.copia());
                    });
            // Marcar nuevo final
            FalActaTransitoAlcoholemia target = store.stream()
                    .filter(m -> medicionId.equals(m.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ar.gob.malvinas.faltas.core.domain.exception
                            .ActaTransitoAlcoholemiaNoEncontradaException(medicionId));
            target.setSiResultadoFinal(true);
            store.removeIf(m -> m.getId().equals(target.getId()));
            store.add(target.copia());
            return findById(medicionId).orElseThrow();
        }
    }

    @Override public void reset() { store.clear(); idCounter.set(1); }
    @Override public String nombre() { return "acta-transito-alcoholemia"; }
    @Override public int size() { return store.size(); }
}
