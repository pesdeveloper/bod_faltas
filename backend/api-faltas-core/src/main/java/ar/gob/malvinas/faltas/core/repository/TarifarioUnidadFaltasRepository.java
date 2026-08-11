package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a la formula tarifaria (por articulo) usada en la valorizacion.
 * Rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.3: la clave es {@code id_articulo}, no
 * un tipo de unidad global.
 */
public interface TarifarioUnidadFaltasRepository {

    Long nextId();

    FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas tarifario);

    Optional<FalTarifarioUnidadFaltas> findById(Long id);

    List<FalTarifarioUnidadFaltas> findAllByArticulo(Long articuloId);

    /** Lookup normal: la formula activa (operativa) del articulo, si existe. */
    Optional<FalTarifarioUnidadFaltas> findActivaByArticulo(Long articuloId);

    /** Lookup historico: la formula vigente del articulo en una fecha dada. */
    Optional<FalTarifarioUnidadFaltas> findVigenteByArticulo(Long articuloId, LocalDate fecha);
}
