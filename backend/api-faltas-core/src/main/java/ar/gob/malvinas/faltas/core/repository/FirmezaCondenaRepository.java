package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;

import java.util.Optional;

/**
 * Puerto de persistencia para firmeza de condena.
 * Implementacion actual: InMemoryFirmezaCondenaRepository.
 * Reemplazable por JdbcClient/MariaDB sin tocar servicios de dominio.
 */
public interface FirmezaCondenaRepository {

    void guardar(FalActaFirmezaCondena firmeza);

    /**
     * Devuelve la firmeza activa del acta, si existe.
     * Un acta puede tener a lo sumo una firmeza activa.
     */
    Optional<FalActaFirmezaCondena> buscarActivaPorActa(Long actaId);
}
