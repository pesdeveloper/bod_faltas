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
    /** Indice secundario atomico por loteCodigo. Garantia concurrente para guardarSiAusentePorCodigo. */
    private final Map<String, FalLoteCorreo> byCode = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalLoteCorreo guardar(FalLoteCorreo lote) {
        FalLoteCorreo copia = lote.copia();
        store.put(copia.getId(), copia);
        byCode.put(copia.getLoteCodigo(), copia);
        return copia;
    }

    /**
     * Persiste el candidato solo si no existe un lote con el mismo loteCodigo.
     * Usa ConcurrentHashMap.compute para garantizar atomicidad sin check-then-act vulnerable.
     *
     * Devuelve el candidato si fue el ganador, o el lote existente si ya habia uno.
     */
    @Override
    public FalLoteCorreo guardarSiAusentePorCodigo(FalLoteCorreo candidato) {
        FalLoteCorreo copia = candidato.copia();
        FalLoteCorreo[] resultado = new FalLoteCorreo[1];
        byCode.compute(copia.getLoteCodigo(), (k, existing) -> {
            if (existing != null) {
                resultado[0] = existing;
                return existing;
            }
            store.put(copia.getId(), copia);
            resultado[0] = copia;
            return copia;
        });
        return resultado[0].copia();
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalLoteCorreo::copia);
    }

    @Override
    public Optional<FalLoteCorreo> buscarPorCodigo(String loteCodigo) {
        if (loteCodigo == null) return Optional.empty();
        return Optional.ofNullable(byCode.get(loteCodigo)).map(FalLoteCorreo::copia);
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
        if (loteCodigo == null) return false;
        return byCode.containsKey(loteCodigo);
    }

    public void cargarSeed(List<FalLoteCorreo> lista) {
        long maxId = 0;
        for (FalLoteCorreo l : lista) {
            FalLoteCorreo copia = l.copia();
            store.put(copia.getId(), copia);
            byCode.put(copia.getLoteCodigo(), copia);
            if (l.getId() > maxId) maxId = l.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        byCode.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "lotes-correo"; }

    @Override
    public int size() { return store.size(); }
}
