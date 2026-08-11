package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
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

    // Generadores de id provisional (JDBC: AUTO_INCREMENT real; el valor devuelto aqui es
    // descartable, save() resuelve alta/actualizacion por PK real, igual patron que el resto
    // de los adapters JDBC de este proyecto).
    long nextPoliticaId();
    long nextTalonarioId();
    long nextAmbitoId();
    long nextAsignacionInspectorId();
    long nextMovimientoId();

    /**
     * Reclama el siguiente numero del talonario (Wave 4 PARTE-2 S8.3): en JDBC, hace
     * {@code SELECT ultimo_nro_emitido FROM num_talonario WHERE id=? FOR UPDATE}, calcula
     * {@code GREATEST(ultimo_nro_emitido, nroDesde-1)+1}, valida contra {@code nroHasta} y
     * actualiza la fila en la MISMA transaccion del llamador (sin @Transactional propio: la
     * frontera es del caller). Debe invocarse en una seccion critica breve, al final.
     */
    int nextNumeroTalonario(Long idTalonario, int nroDesde, Integer nroHasta);

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

    /**
     * Serializa, dentro de la transaccion del llamador, la activacion de un ambito ACTA
     * ELECTRONICO ({@code DECISION-NUM-UNICO-ACTIVO-POR-DEPENDENCIA-01}, Wave 4 Parte 3C §8):
     * bloquea (en JDBC, {@code SELECT ... FOR UPDATE}) las filas de {@code fal_dependencia}
     * afectadas por el nuevo ambito, para que dos activaciones concurrentes que dejarian dos
     * talonarios ACTA ELECTRONICO activos aplicables a la misma dependencia se serialicen: la
     * segunda espera a que la primera comitee y luego re-lee un estado ya consistente.
     *
     * <p>{@code alcance=DEPENDENCIA} bloquea unicamente la fila de esa dependencia.
     * {@code alcance=GLOBAL} bloquea todas las dependencias existentes, porque una activacion
     * global es aplicable a cualquiera de ellas (operacion administrativa, poco frecuente: el
     * costo de bloquear la tabla completa es aceptable). No hay restriccion declarativa (UNIQUE)
     * que modele correctamente esta regla sin conocer vigencia y alcance en tiempo de escritura;
     * este guardrail transaccional es la proteccion real.
     *
     * <p>Implementacion InMemory: no-op (sin concurrencia real que serializar; la prueba de
     * carrera de Wave 4 Parte 3C §8.3 se ejecuta contra MariaDB real).
     */
    void bloquearParaExclusividadElectronicaActa(AlcanceTalonario alcance, Long idDep);

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

