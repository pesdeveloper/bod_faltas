package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.repository.GestionExternaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGestionExternaRepository implements GestionExternaRepository {

    private final Map<Long, FalGestionExterna> store = new ConcurrentHashMap<>();

    @Override
    public FalGestionExterna guardar(FalGestionExterna gestion) {
        store.put(gestion.getActaId(), gestion);
        return gestion;
    }

    @Override
    public Optional<FalGestionExterna> buscarActiva(Long actaId) {
        FalGestionExterna g = store.get(actaId);
        if (g != null && g.isSiActiva()) return Optional.of(g);
        return Optional.empty();
    }

    @Override
    public boolean existeActiva(Long actaId) {
        return buscarActiva(actaId).isPresent();
    }

    @Override
    public Optional<FalGestionExterna> buscarPorHistorico(Long actaId) {
        return Optional.ofNullable(store.get(actaId));
    }
}
