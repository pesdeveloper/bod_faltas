package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentoPlantillaContenidoRepository implements DocumentoPlantillaContenidoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumentoPlantillaContenido> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public Long nextId() { return counter.getAndIncrement(); }

    @Override
    public FalDocumentoPlantillaContenido guardar(FalDocumentoPlantillaContenido c) {
        store.put(c.getId(), c);
        return c;
    }

    @Override
    public Optional<FalDocumentoPlantillaContenido> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoPlantillaContenido> buscarContenidoVigente(Long plantillaId, LocalDateTime en) {
        return store.values().stream()
                .filter(c -> c.getPlantillaId().equals(plantillaId))
                .filter(c -> c.vigente(en))
                .toList();
    }

    @Override
    public List<FalDocumentoPlantillaContenido> listarPorPlantilla(Long plantillaId) {
        return store.values().stream()
                .filter(c -> c.getPlantillaId().equals(plantillaId))
                .toList();
    }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "DocumentoPlantillaContenido"; }

    @Override
    public int size() { return store.size(); }
}