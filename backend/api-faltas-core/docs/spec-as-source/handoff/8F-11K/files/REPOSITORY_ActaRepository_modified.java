package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActa;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia del acta.
 * La implementacion in-memory puede reemplazarse por MariaDB sin tocar dominio.
 */
public interface ActaRepository {
    Long nextId();
    FalActa guardar(FalActa acta);
    Optional<FalActa> buscarPorId(Long id);
    Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico);
    List<FalActa> listarTodas();
}
