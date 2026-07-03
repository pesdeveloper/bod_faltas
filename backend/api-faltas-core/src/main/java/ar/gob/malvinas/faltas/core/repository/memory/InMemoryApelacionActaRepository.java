package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion en memoria de ApelacionActaRepository.
 * Almacena apelaciones por actaId para lookup O(1).
 * Reemplazable por JdbcClient/MariaDB sin tocar servicios.
 */
@Repository
public class InMemoryApelacionActaRepository implements ApelacionActaRepository {

    private final ConcurrentHashMap<String, FalActaApelacion> store = new ConcurrentHashMap<>();

    @Override
    public void guardar(FalActaApelacion apelacion) {
        store.put(apelacion.getId(), apelacion);
    }

    @Override
    public Optional<FalActaApelacion> buscarActiva(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId) && a.isSiActiva())
                .findFirst();
    }

    @Override
    public Optional<FalActaApelacion> buscarUltima(Long actaId) {
        return store.values().stream()
                .filter(a -> a.getActaId().equals(actaId))
                .findFirst();
    }
}

