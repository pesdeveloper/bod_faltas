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
 *
 * Identificadores: la PK la asigna la persistencia
 * (fal_dia_no_computable.id es BIGINT AUTO_INCREMENT). El puerto no expone
 * nextId(): las altas entregan la entidad sin id y reciben la entidad persistida
 * con el id asignado.
 */
public interface DiaNoComputableRepository {

    /**
     * Persiste el dia. Con id nulo inserta y devuelve la entidad con el id asignado;
     * con id presente actualiza el registro existente.
     */
    FalDiaNoComputable guardar(FalDiaNoComputable dia);

    /**
     * Persiste el candidato solo si no existe un registro activo para la misma fecha.
     * Si ya existe un registro activo: no persiste y devuelve el existente con
     * yaExistia=true.
     * La operacion debe ser atomica respecto de la fecha activa.
     *
     * El candidato se entrega sin id: la persistencia lo asigna cuando inserta.
     */
    DiaNoComputableSaveResult guardarActivoSiAusentePorFecha(FalDiaNoComputable candidato);

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
