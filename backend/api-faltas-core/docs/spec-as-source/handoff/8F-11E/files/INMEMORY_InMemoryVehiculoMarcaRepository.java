package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryVehiculoMarcaRepository implements VehiculoMarcaRepository, ResettableInMemoryRepository {

    private final List<FalVehiculoMarca> store = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalVehiculoMarca guardar(FalVehiculoMarca marca) {
        store.removeIf(m -> m.getId().equals(marca.getId()));
        store.add(marca.copia());
        return findById(marca.getId()).orElseThrow();
    }

    @Override
    public Optional<FalVehiculoMarca> findById(Long id) {
        return store.stream().filter(m -> id.equals(m.getId())).findFirst().map(FalVehiculoMarca::copia);
    }

    @Override
    public Optional<FalVehiculoMarca> findByCodigo(String codigo) {
        String norm = codigo != null ? codigo.trim().toUpperCase() : null;
        return store.stream().filter(m -> m.getCodigo().equalsIgnoreCase(norm)).findFirst().map(FalVehiculoMarca::copia);
    }

    @Override
    public Optional<FalVehiculoMarca> findByNombre(String nombre) {
        return store.stream()
                .filter(m -> m.getNombre().equalsIgnoreCase(nombre != null ? nombre.trim() : null))
                .findFirst().map(FalVehiculoMarca::copia);
    }

    @Override
    public List<FalVehiculoMarca> findAllActivas() {
        return store.stream().filter(FalVehiculoMarca::isSiActivo).map(FalVehiculoMarca::copia).toList();
    }

    @Override
    public List<FalVehiculoMarca> findAll() {
        return store.stream().map(FalVehiculoMarca::copia).toList();
    }

    @Override public void reset() { store.clear(); idCounter.set(1); }
    @Override public String nombre() { return "vehiculo-marcas"; }
    @Override public int size() { return store.size(); }
}
