package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryArticuloMedidaPreventivaRepository
        implements ArticuloMedidaPreventivaRepository, ResettableInMemoryRepository {

    private final Map<ArticuloMedidaPreventivaId, FalArticuloMedidaPreventiva> store = new ConcurrentHashMap<>();

    /**
     * Guarda con validacion atomica: no permite activar una relacion ya activa.
     * La reactivacion debe ser explicita via un nuevo registro o una operacion dedicada.
     * La desactivacion (siActiva=false) siempre esta permitida.
     */
    @Override
    public synchronized FalArticuloMedidaPreventiva save(FalArticuloMedidaPreventiva rel) {
        FalArticuloMedidaPreventiva existing = store.get(rel.getId());
        if (rel.isSiActiva() && existing != null && !existing.isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "Reactivacion de relacion articulo-medida no permitida via save(). "
                            + "articuloId=" + rel.getArticuloId()
                            + " medidaId=" + rel.getMedidaPreventivaId()
                            + ". Use vincularArticulo().");
        }
        FalArticuloMedidaPreventiva copia = rel.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalArticuloMedidaPreventiva> findById(ArticuloMedidaPreventivaId id) {
        return Optional.ofNullable(store.get(id)).map(FalArticuloMedidaPreventiva::copia);
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findByArticuloId(Long articuloId) {
        return store.values().stream()
                .filter(r -> articuloId.equals(r.getArticuloId()))
                .map(FalArticuloMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findActivasByArticuloId(Long articuloId) {
        return store.values().stream()
                .filter(r -> articuloId.equals(r.getArticuloId()) && r.isSiActiva())
                .map(FalArticuloMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findByMedidaPreventivaId(Long medidaPreventivaId) {
        return store.values().stream()
                .filter(r -> medidaPreventivaId.equals(r.getMedidaPreventivaId()))
                .map(FalArticuloMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsActiva(ArticuloMedidaPreventivaId id) {
        FalArticuloMedidaPreventiva r = store.get(id);
        return r != null && r.isSiActiva();
    }

    public void cargarSeed(List<FalArticuloMedidaPreventiva> lista) {
        for (FalArticuloMedidaPreventiva r : lista) {
            store.put(r.getId(), r.copia());
        }
    }

    @Override
    public void reset() {
        store.clear();
    }

    @Override
    public String nombre() { return "articulo-medidas-preventivas"; }

    @Override
    public int size() { return store.size(); }
}
