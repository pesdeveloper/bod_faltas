package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso al catálogo de medidas preventivas.
 */
public interface MedidaPreventivaRepository {

    Long nextId();

    FalMedidaPreventiva save(FalMedidaPreventiva medida);

    Optional<FalMedidaPreventiva> findById(Long id);

    Optional<FalMedidaPreventiva> findByCodigoAndVersion(String codigo, short version);

    Optional<FalMedidaPreventiva> findActivaByCodigo(String codigo);

    List<FalMedidaPreventiva> findVersionesByCodigo(String codigo);

    List<FalMedidaPreventiva> findActivas();

    List<FalMedidaPreventiva> findActivasParaDependencia(Long idDep, Short verDep);
}
