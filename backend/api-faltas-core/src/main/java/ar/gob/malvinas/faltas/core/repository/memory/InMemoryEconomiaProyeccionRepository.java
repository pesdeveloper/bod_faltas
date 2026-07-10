package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryEconomiaProyeccionRepository
        implements EconomiaProyeccionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaEconomiaProyeccion> store = new ConcurrentHashMap<>();

    @Override
    public synchronized FalActaEconomiaProyeccion save(FalActaEconomiaProyeccion proyeccion) {
        FalActaEconomiaProyeccion copia = proyeccion.copia();
        store.put(copia.getActaId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaEconomiaProyeccion> findByActaId(Long actaId) {
        return Optional.ofNullable(store.get(actaId)).map(FalActaEconomiaProyeccion::copia);
    }

    @Override
    public void deleteByActaId(Long actaId) { store.remove(actaId); }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "economia-proyeccion"; }

    @Override
    public int size() { return store.size(); }
}