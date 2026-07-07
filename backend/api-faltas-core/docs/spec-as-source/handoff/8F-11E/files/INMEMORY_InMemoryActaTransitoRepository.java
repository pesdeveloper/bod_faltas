package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryActaTransitoRepository implements ActaTransitoRepository, ResettableInMemoryRepository {

    private final java.util.List<FalActaTransito> store = new CopyOnWriteArrayList<>();

    @Override
    public FalActaTransito guardar(FalActaTransito transito) {
        store.removeIf(t -> t.getActaId().equals(transito.getActaId()));
        store.add(transito.copia());
        return findByActaId(transito.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaTransito> findByActaId(Long actaId) {
        return store.stream().filter(t -> t.getActaId().equals(actaId)).findFirst().map(FalActaTransito::copia);
    }

    @Override
    public boolean existsByActaId(Long actaId) {
        return store.stream().anyMatch(t -> t.getActaId().equals(actaId));
    }

    @Override public void reset() { store.clear(); }
    @Override public String nombre() { return "acta-transito"; }
    @Override public int size() { return store.size(); }
}
