package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;

import java.util.Optional;

/**
 * Contrato de persistencia del fallo del acta.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios de dominio.
 */
public interface FalloActaRepository {

    FalActaFallo guardar(FalActaFallo fallo);

    /**
     * Devuelve el fallo activo (siActivo=true) del acta, si existe.
     * Cada acta tiene como maximo un fallo activo por vez.
     */
    Optional<FalActaFallo> buscarActivo(Long actaId);
}

