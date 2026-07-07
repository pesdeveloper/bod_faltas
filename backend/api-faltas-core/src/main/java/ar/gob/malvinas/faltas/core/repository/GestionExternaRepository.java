package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;

import java.util.Optional;

/**
 * Contrato de persistencia de gestion externa.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios.
 */
public interface GestionExternaRepository {
    Long nextId();

    FalGestionExterna guardar(FalGestionExterna gestion);
    Optional<FalGestionExterna> buscarActiva(Long actaId);
    boolean existeActiva(Long actaId);
    /** Busca la gestion externa del acta independientemente de si esta activa o no (historico). */
    Optional<FalGestionExterna> buscarPorHistorico(Long actaId);
}
