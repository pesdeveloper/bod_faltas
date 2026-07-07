package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;
import ar.gob.malvinas.faltas.core.repository.ActaVehiculoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryActaVehiculoRepository implements ActaVehiculoRepository, ResettableInMemoryRepository {
    private final java.util.List<FalActaVehiculo> store = new CopyOnWriteArrayList<>();

    @Override
    public FalActaVehiculo guardar(FalActaVehiculo vehiculo) {
        store.removeIf(v -> v.getActaId().equals(vehiculo.getActaId()));
        store.add(vehiculo.copia());
        return findByActaId(vehiculo.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaVehiculo> findByActaId(Long actaId) {
        return store.stream().filter(v -> v.getActaId().equals(actaId)).findFirst().map(FalActaVehiculo::copia);
    }

    @Override public boolean existsByActaId(Long actaId) {
        return store.stream().anyMatch(v -> v.getActaId().equals(actaId));
    }

    @Override public void reset() { store.clear(); }
    @Override public String nombre() { return "acta-vehiculos"; }
    @Override public int size() { return store.size(); }
}
