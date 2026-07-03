package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de politicas, talonarios, ambitos,
 * asignaciones de inspectores y movimientos de numeros.
 * Reemplazable por implementacion MariaDB/JDBC sin tocar servicios (Slice 9).
 */
public interface TalonarioRepository {

    NumPolitica guardarPolitica(NumPolitica politica);
    Optional<NumPolitica> buscarPoliticaPorId(Long id);
    Optional<NumPolitica> buscarPoliticaPorCodigo(String codigo);
    List<NumPolitica> listarPoliticasActivas();
    boolean existePoliticaCodigo(String codigo);

    NumTalonario guardarTalonario(NumTalonario talonario);
    Optional<NumTalonario> buscarTalonarioPorId(Long id);
    Optional<NumTalonario> buscarTalonarioPorCodigo(String codigo);
    Optional<NumTalonario> buscarTalonarioPorNombreSecuencia(String nombreSecuencia);
    List<NumTalonario> listarTalonariosActivos();
    boolean existeTalonarioCodigo(String codigo);
    boolean existeNombreSecuencia(String nombreSecuencia);

    NumTalonarioAmbito guardarAmbito(NumTalonarioAmbito ambito);
    Optional<NumTalonarioAmbito> buscarAmbitoPorId(Long id);
    List<NumTalonarioAmbito> listarAmbitosPorTalonario(Long talonarioId);
    List<NumTalonarioAmbito> listarAmbitosActivos();

    NumTalonarioInspector guardarAsignacionInspector(NumTalonarioInspector asignacion);
    Optional<NumTalonarioInspector> buscarAsignacionInspectorPorId(Long id);
    Optional<NumTalonarioInspector> buscarAsignacionActivaPorTalonario(Long idTalonario);
    List<NumTalonarioInspector> listarAsignacionesActivas();
    List<NumTalonarioInspector> listarAsignacionesPorInspector(Long idInsp);
    List<NumTalonarioInspector> listarAsignacionesPorTalonario(Long idTalonario);
    NumTalonarioInspector actualizarAsignacionInspector(NumTalonarioInspector asignacion);

    // Movimientos (Slice 8B-4)
    NumTalonarioMovimiento guardarMovimiento(NumTalonarioMovimiento movimiento);
    Optional<NumTalonarioMovimiento> buscarMovimientoPorId(Long id);
    Optional<NumTalonarioMovimiento> buscarMovimientoPorTalonarioYNro(Long idTalonario, int nroTalonario);
    List<NumTalonarioMovimiento> listarMovimientosPorTalonario(Long idTalonario);
    boolean existeMovimientoTalonarioNumero(Long idTalonario, int nroTalonario);

    // Movimientos - control de huecos (Slice 8B-6)
    List<Integer> buscarNumerosFaltantesEnRango(Long idTalonario, int nroDesde, int nroHasta);
}

