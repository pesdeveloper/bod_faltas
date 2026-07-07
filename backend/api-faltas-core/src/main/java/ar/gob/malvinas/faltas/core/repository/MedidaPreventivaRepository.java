package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso al catalogo de medidas preventivas.
 * Solo una version activa por codigo en todo momento.
 * UK logica: (codigo, versionMedida).
 */
public interface MedidaPreventivaRepository {

    Long nextId();

    /**
     * Guarda. No permite duplicar la combinacion (codigo, versionMedida).
     */
    FalMedidaPreventiva save(FalMedidaPreventiva medida);

    Optional<FalMedidaPreventiva> findById(Long id);

    Optional<FalMedidaPreventiva> findByCodigoAndVersion(String codigo, short version);

    Optional<FalMedidaPreventiva> findActivaByCodigo(String codigo);

    List<FalMedidaPreventiva> findVersionesByCodigo(String codigo);

    List<FalMedidaPreventiva> findActivas();

    List<FalMedidaPreventiva> findActivasParaDependencia(Long idDep, Short verDep);

    /**
     * Crea una nueva version de forma atomica:
     *   1. Valida que no exista ya esa version para el codigo.
     *   2. Desactiva la version activa anterior si existe.
     *   3. Guarda la nueva version.
     * Lanza PrecondicionVioladaException si ya existe la version.
     */
    FalMedidaPreventiva crearNuevaVersionAtomico(FalMedidaPreventiva nuevaVersion);
}
