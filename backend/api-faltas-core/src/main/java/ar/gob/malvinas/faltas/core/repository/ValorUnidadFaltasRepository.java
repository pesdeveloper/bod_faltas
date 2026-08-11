package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Port de acceso al valor monetario historico/vigente de una unidad economica
 * (SALARIO/UNIDAD_FIJA). Nueva (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.2).
 */
public interface ValorUnidadFaltasRepository {

    Long nextId();

    FalValorUnidadFaltas save(FalValorUnidadFaltas valorUnidad);

    Optional<FalValorUnidadFaltas> findById(Long id);

    List<FalValorUnidadFaltas> findAllByTipo(TipoUnidadFaltas tipo);

    /** Lookup directo por indice: el valor activo (operativo) del tipo, si existe. */
    Optional<FalValorUnidadFaltas> findActivoByTipo(TipoUnidadFaltas tipo);

    /** Lookup historico: el valor vigente del tipo en una fecha dada. */
    Optional<FalValorUnidadFaltas> findVigenteByTipo(TipoUnidadFaltas tipo, LocalDate fecha);
}
