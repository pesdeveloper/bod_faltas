package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.repository.VehiculoModeloRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryVehiculoModeloRepository implements VehiculoModeloRepository, ResettableInMemoryRepository {

    private final List<FalVehiculoModelo> store = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalVehiculoModelo guardar(FalVehiculoModelo modelo) {
        store.removeIf(m -> m.getId().equals(modelo.getId()));
        store.add(modelo.copia());
        return findById(modelo.getId()).orElseThrow();
    }

    @Override
    public Optional<FalVehiculoModelo> findById(Long id) {
        return store.stream().filter(m -> id.equals(m.getId())).findFirst().map(FalVehiculoModelo::copia);
    }

    @Override
    public Optional<FalVehiculoModelo> findByMarcaAndCodigo(Long marcaId, String codigo) {
        String norm = codigo != null ? codigo.trim().toUpperCase() : null;
        return store.stream()
                .filter(m -> m.getMarcaVehiculoId().equals(marcaId) && m.getCodigo().equalsIgnoreCase(norm))
                .findFirst().map(FalVehiculoModelo::copia);
    }

    @Override
    public Optional<FalVehiculoModelo> findByMarcaAndNombre(Long marcaId, String nombre) {
        return store.stream()
                .filter(m -> m.getMarcaVehiculoId().equals(marcaId)
                        && m.getNombre().equalsIgnoreCase(nombre != null ? nombre.trim() : null))
                .findFirst().map(FalVehiculoModelo::copia);
    }

    @Override
    public List<FalVehiculoModelo> findActivasByMarca(Long marcaId) {
        return store.stream()
                .filter(m -> m.getMarcaVehiculoId().equals(marcaId) && m.isSiActivo())
                .map(FalVehiculoModelo::copia).toList();
    }

    @Override
    public List<FalVehiculoModelo> findByMarca(Long marcaId) {
        return store.stream().filter(m -> m.getMarcaVehiculoId().equals(marcaId))
                .map(FalVehiculoModelo::copia).toList();
    }

    @Override public void reset() { store.clear(); idCounter.set(1); }
    @Override public String nombre() { return "vehiculo-modelos"; }
    @Override public int size() { return store.size(); }
}
