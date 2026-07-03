package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion en memoria de FirmezaCondenaRepository.
 * Almacena registros de firmeza por id tecnico.
 * Reemplazable por JdbcClient/MariaDB sin tocar servicios.
 */
@Repository
public class InMemoryFirmezaCondenaRepository implements FirmezaCondenaRepository {

    private final ConcurrentHashMap<String, FalActaFirmezaCondena> store = new ConcurrentHashMap<>();

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
