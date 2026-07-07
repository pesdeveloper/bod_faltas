package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.repository.ActaContravencionRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryActaContravencionRepository implements ActaContravencionRepository, ResettableInMemoryRepository {
    private final java.util.List<FalActaContravencion> store = new CopyOnWriteArrayList<>();

    @Override
    public FalActaContravencion guardar(FalActaContravencion ctv) {
        store.removeIf(c -> c.getActaId().equals(ctv.getActaId()));
        store.add(ctv.copia());
        return findByActaId(ctv.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaContravencion> findByActaId(Long actaId) {
        return store.stream().filter(c -> c.getActaId().equals(actaId)).findFirst().map(FalActaContravencion::copia);
    }

    @Override public boolean existsByActaId(Long actaId) {
        return store.stream().anyMatch(c -> c.getActaId().equals(actaId));
    }

    @Override public void reset() { store.clear(); }
    @Override public String nombre() { return "acta-contravenciones"; }
    @Override public int size() { return store.size(); }
}
