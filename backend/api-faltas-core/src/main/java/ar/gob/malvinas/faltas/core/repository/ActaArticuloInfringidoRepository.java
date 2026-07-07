package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a artículos imputados a actas.
 */
public interface ActaArticuloInfringidoRepository {

    Long nextId();

    FalActaArticuloInfringido save(FalActaArticuloInfringido articulo);

    Optional<FalActaArticuloInfringido> findById(Long id);

    List<FalActaArticuloInfringido> findByActaId(Long actaId);

    List<FalActaArticuloInfringido> findActivosByActaId(Long actaId);

    Optional<FalActaArticuloInfringido> findActivoByActaAndArticulo(Long actaId, Long articuloId);

    boolean existsActivo(Long actaId, Long articuloId);
}
