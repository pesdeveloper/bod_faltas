package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryFirmezaCondenaRepository implements FirmezaCondenaRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final ConcurrentHashMap<Long, FalActaFirmezaCondena> store = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public void guardar(FalActaFirmezaCondena firmeza) {
        store.put(firmeza.getId(), firmeza);
    }

    @Override
    public Optional<FalActaFirmezaCondena> buscarActivaPorActa(Long actaId) {
        return store.values().stream()
                .filter(f -> f.getActaId().equals(actaId) && f.isSiActiva())
                .findFirst();
    }
}