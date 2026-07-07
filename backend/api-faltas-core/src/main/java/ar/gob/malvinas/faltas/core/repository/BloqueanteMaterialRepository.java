package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de bloqueantes materiales.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios (Slice 9).
 */
public interface BloqueanteMaterialRepository {

    Long nextId();

    FalBloqueanteMaterial guardar(FalBloqueanteMaterial bloqueante);

    Optional<FalBloqueanteMaterial> findById(Long id);

    List<FalBloqueanteMaterial> findByActaId(Long actaId);

    boolean existsActivoByActaId(Long actaId);
}