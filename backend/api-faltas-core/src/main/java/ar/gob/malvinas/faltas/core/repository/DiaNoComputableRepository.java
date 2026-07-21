package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de dias no computables del calendario administrativo local.
 *
 * La fecha es unica entre registros activos.
 * El historial inactivo puede conservar fechas repetidas.
 */
public interface DiaNoComputableRepository {

    Long nextId();

    FalDiaNoComputable guardar(FalDiaNoComputable dia);

    /**
     * Persiste el candidato solo si no existe un registro activo para la misma fecha.
     * Si ya existe un registro activo: no persiste y devuelve el existente.
     * La operacion debe ser atomica respecto de la fecha activa.
     */
    FalDiaNoComputable guardarActivoSiAusentePorFecha(FalDiaNoComputable candidato);

    Optional<FalDiaNoComputable> buscarPorId(Long id);

    Optional<FalDiaNoComputable> buscarActivoPorFecha(LocalDate fecha);

    /**
     * Prepara idempotencia de una futura sincronizacion externa.
     * En este slice no se implementa ningún sincronizador.
     */
    Optional<FalDiaNoComputable> buscarPorOrigenYReferenciaExterna(
            OrigenDiaNoComputable origen,
            String referenciaExterna);

    /** Devuelve todos los registros activos ordenados por fecha ASC, id ASC. */
    List<FalDiaNoComputable> listarActivosOrdenados();
}
