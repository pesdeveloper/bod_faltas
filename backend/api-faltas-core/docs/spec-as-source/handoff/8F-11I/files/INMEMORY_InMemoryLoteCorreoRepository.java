package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoLote;
import ar.gob.malvinas.faltas.core.domain.model.FalLoteCorreo;
import ar.gob.malvinas.faltas.core.repository.LoteCorreoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryLoteCorreoRepository
        implements LoteCorreoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalLoteCorreo> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalLoteCorreo guardar(FalLoteCorreo lote) {
        FalLoteCorreo copia = lote.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalLoteCorreo::copia);
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorCodigo(String loteCodigo) {
        if (loteCodigo == null) return Optional.empty();
        return store.values().stream()
                .filter(l -> loteCodigo.equals(l.getLoteCodigo()))
                .map(FalLoteCorreo::copia)
                .findFirst();
    }

    @Override
    public List<FalLoteCorreo> buscarPorEstado(EstadoLote estadoLote) {
        return store.values().stream()
                .filter(l -> estadoLote == l.getEstadoLote())
                .map(FalLoteCorreo::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorReferenciaExterna(String referenciaExterna) {
        if (referenciaExterna == null) return Optional.empty();
        return store.values().stream()
                .filter(l -> referenciaExterna.equals(l.getReferenciaExterna()))
                .map(FalLoteCorreo::copia)
                .findFirst();
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorGuid(String guidLoteExt) {
        if (guidLoteExt == null) return Optional.empty();
        return store.values().stream()
                .filter(l -> guidLoteExt.equals(l.getGuidLoteExt()))
                .map(FalLoteCorreo::copia)
                .findFirst();
    }

    @Override
    public boolean existeCodigo(String loteCodigo) {
        return store.values().stream().anyMatch(l -> l.getLoteCodigo().equals(loteCodigo));
    }

    public void cargarSeed(List<FalLoteCorreo> lista) {
        long maxId = 0;
        for (FalLoteCorreo l : lista) {
            store.put(l.getId(), l.copia());
            if (l.getId() > maxId) maxId = l.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "lotes-correo"; }

    @Override
    public int size() { return store.size(); }
}
