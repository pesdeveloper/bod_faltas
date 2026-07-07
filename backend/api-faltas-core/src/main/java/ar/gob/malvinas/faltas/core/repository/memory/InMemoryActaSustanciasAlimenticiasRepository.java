package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;
import ar.gob.malvinas.faltas.core.repository.ActaSustanciasAlimenticiasRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryActaSustanciasAlimenticiasRepository
        implements ActaSustanciasAlimenticiasRepository, ResettableInMemoryRepository {
    private final java.util.List<FalActaSustanciasAlimenticias> store = new CopyOnWriteArrayList<>();

    @Override
    public FalActaSustanciasAlimenticias guardar(FalActaSustanciasAlimenticias s) {
        store.removeIf(x -> x.getActaId().equals(s.getActaId()));
        store.add(s.copia());
        return findByActaId(s.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaSustanciasAlimenticias> findByActaId(Long actaId) {
        return store.stream().filter(x -> x.getActaId().equals(actaId)).findFirst().map(FalActaSustanciasAlimenticias::copia);
    }

    @Override public boolean existsByActaId(Long actaId) {
        return store.stream().anyMatch(x -> x.getActaId().equals(actaId));
    }

    @Override public void reset() { store.clear(); }
    @Override public String nombre() { return "acta-sustancias-alimenticias"; }
    @Override public int size() { return store.size(); }
}
