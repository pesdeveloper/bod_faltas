package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a las relaciones artículo-medida preventiva.
 */
public interface ArticuloMedidaPreventivaRepository {

    FalArticuloMedidaPreventiva save(FalArticuloMedidaPreventiva rel);

    Optional<FalArticuloMedidaPreventiva> findById(ArticuloMedidaPreventivaId id);

    List<FalArticuloMedidaPreventiva> findByArticuloId(Long articuloId);

    List<FalArticuloMedidaPreventiva> findActivasByArticuloId(Long articuloId);

    List<FalArticuloMedidaPreventiva> findByMedidaPreventivaId(Long medidaPreventivaId);

    boolean existsActiva(ArticuloMedidaPreventivaId id);
}
