package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port de acceso al catálogo de tarifario de unidades de faltas.
 */
public interface TarifarioUnidadFaltasRepository {

    Long nextId();

    FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas tarifario);

    Optional<FalTarifarioUnidadFaltas> findById(Long id);

    List<FalTarifarioUnidadFaltas> findAll();

    List<FalTarifarioUnidadFaltas> findByTipoUnidad(TipoUnidadFaltas tipoUnidad);

    /** Todos los tarifarios vigentes en la fecha para el tipo dado. */
    List<FalTarifarioUnidadFaltas> findVigentes(TipoUnidadFaltas tipoUnidad, LocalDate fecha);

    /** El tarifario más reciente (mayor fhVigDesde) vigente en la fecha para el tipo dado. */
    Optional<FalTarifarioUnidadFaltas> findUltimoVigente(TipoUnidadFaltas tipoUnidad, LocalDate fecha);
}
