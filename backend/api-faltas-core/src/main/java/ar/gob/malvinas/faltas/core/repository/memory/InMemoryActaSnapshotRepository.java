package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryActaSnapshotRepository implements ActaSnapshotRepository {

    private final Map<Long, FalActaSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
        store.put(snapshot.getIdActa(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
        return Optional.ofNullable(store.get(idActa));
    }
}

