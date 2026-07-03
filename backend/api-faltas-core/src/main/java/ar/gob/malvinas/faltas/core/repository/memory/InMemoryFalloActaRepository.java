package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryFalloActaRepository implements FalloActaRepository {

    private final Map<Long, FalActaFallo> storeByActa = new ConcurrentHashMap<>();

    @Override
    public FalActaFallo guardar(FalActaFallo fallo) {
        storeByActa.put(fallo.getActaId(), fallo);
        return fallo;
    }

    @Override
    public Optional<FalActaFallo> buscarActivo(Long actaId) {
        FalActaFallo fallo = storeByActa.get(actaId);
        if (fallo != null && fallo.isSiActivo()) {
            return Optional.of(fallo);
        }
        return Optional.empty();
    }
}

