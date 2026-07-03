package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
public class InMemoryTalonarioRepository implements TalonarioRepository {

    private final List<NumPolitica> politicas = new CopyOnWriteArrayList<>();
    private final List<NumTalonario> talonarios = new CopyOnWriteArrayList<>();
    private final List<NumTalonarioAmbito> ambitos = new CopyOnWriteArrayList<>();
    private final List<NumTalonarioInspector> asignacionesInspector = new CopyOnWriteArrayList<>();
    private final List<NumTalonarioMovimiento> movimientos = new CopyOnWriteArrayList<>();

    private final AtomicLong secPolitica = new AtomicLong(1);
    private final AtomicLong secTalonario = new AtomicLong(1);
    private final AtomicLong secAmbito = new AtomicLong(1);
    private final AtomicLong secAsignacionInspector = new AtomicLong(1);
    private final AtomicLong secMovimiento = new AtomicLong(1);
    private final ConcurrentHashMap<Long, AtomicInteger> contadoresNumeracion = new ConcurrentHashMap<>();

    @Override
    public NumPolitica guardarPolitica(NumPolitica politica) {
        politicas.removeIf(p -> p.getId().equals(politica.getId()));
        politicas.add(politica);
        return politica;
    }
    @Override
    public Optional<NumPolitica> buscarPoliticaPorId(Long id) {
        return politicas.stream().filter(p -> p.getId().equals(id)).findFirst();
    }
    @Override
    public Optional<NumPolitica> buscarPoliticaPorCodigo(String codigo) {
        return politicas.stream().filter(p -> p.getCodigo().equals(codigo)).findFirst();
    }
    @Override
    public List<NumPolitica> listarPoliticasActivas() {
        return politicas.stream().filter(NumPolitica::isSiActiva).toList();
    }
    @Override
    public boolean existePoliticaCodigo(String codigo) {
        return politicas.stream().anyMatch(p -> p.getCodigo().equals(codigo));
    }
    public long nextPoliticaId() { return secPolitica.getAndIncrement(); }

    @Override
    public NumTalonario guardarTalonario(NumTalonario talonario) {
        talonarios.removeIf(t -> t.getId().equals(talonario.getId()));
        talonarios.add(talonario);
        return talonario;
    }
    @Override
    public Optional<NumTalonario> buscarTalonarioPorId(Long id) {
        return talonarios.stream().filter(t -> t.getId().equals(id)).findFirst();
    }
    @Override
    public Optional<NumTalonario> buscarTalonarioPorCodigo(String codigo) {
        return talonarios.stream().filter(t -> t.getCodigo().equals(codigo)).findFirst();
    }
    @Override
    public Optional<NumTalonario> buscarTalonarioPorNombreSecuencia(String nombreSecuencia) {
        return talonarios.stream().filter(t -> nombreSecuencia.equals(t.getNombreSecuencia())).findFirst();
    }
    @Override
    public List<NumTalonario> listarTalonariosActivos() {
        return talonarios.stream().filter(NumTalonario::isSiActivo).toList();
    }
    @Override
    public boolean existeTalonarioCodigo(String codigo) {
        return talonarios.stream().anyMatch(t -> t.getCodigo().equals(codigo));
    }
    @Override
    public boolean existeNombreSecuencia(String nombreSecuencia) {
        return talonarios.stream().anyMatch(t -> nombreSecuencia.equals(t.getNombreSecuencia()));
    }
    public long nextTalonarioId() { return secTalonario.getAndIncrement(); }

    @Override
    public NumTalonarioAmbito guardarAmbito(NumTalonarioAmbito ambito) {
        ambitos.removeIf(a -> a.getId().equals(ambito.getId()));
        ambitos.add(ambito);
        return ambito;
    }
    @Override
    public Optional<NumTalonarioAmbito> buscarAmbitoPorId(Long id) {
        return ambitos.stream().filter(a -> a.getId().equals(id)).findFirst();
    }
    @Override
    public List<NumTalonarioAmbito> listarAmbitosPorTalonario(Long talonarioId) {
        return ambitos.stream().filter(a -> a.getTalonarioId().equals(talonarioId)).toList();
    }
    @Override
    public List<NumTalonarioAmbito> listarAmbitosActivos() {
        return ambitos.stream().filter(NumTalonarioAmbito::isSiActivo).toList();
    }
    public long nextAmbitoId() { return secAmbito.getAndIncrement(); }

    @Override
    public NumTalonarioInspector guardarAsignacionInspector(NumTalonarioInspector asignacion) {
        asignacionesInspector.removeIf(a -> a.getId().equals(asignacion.getId()));
        asignacionesInspector.add(asignacion);
        return asignacion;
    }
    @Override
    public Optional<NumTalonarioInspector> buscarAsignacionInspectorPorId(Long id) {
        return asignacionesInspector.stream().filter(a -> a.getId().equals(id)).findFirst();
    }
    @Override
    public Optional<NumTalonarioInspector> buscarAsignacionActivaPorTalonario(Long idTalonario) {
        return asignacionesInspector.stream()
                .filter(a -> a.getIdTalonario().equals(idTalonario) && a.isSiActiva()).findFirst();
    }
    @Override
    public List<NumTalonarioInspector> listarAsignacionesActivas() {
        return asignacionesInspector.stream().filter(NumTalonarioInspector::isSiActiva).toList();
    }
    @Override
    public List<NumTalonarioInspector> listarAsignacionesPorInspector(Long idInsp) {
        return asignacionesInspector.stream().filter(a -> a.getIdInsp().equals(idInsp)).toList();
    }
    @Override
    public List<NumTalonarioInspector> listarAsignacionesPorTalonario(Long idTalonario) {
        return asignacionesInspector.stream().filter(a -> a.getIdTalonario().equals(idTalonario)).toList();
    }
    @Override
    public NumTalonarioInspector actualizarAsignacionInspector(NumTalonarioInspector asignacion) {
        return guardarAsignacionInspector(asignacion);
    }
    public long nextAsignacionInspectorId() { return secAsignacionInspector.getAndIncrement(); }

    @Override
    public NumTalonarioMovimiento guardarMovimiento(NumTalonarioMovimiento movimiento) {
        if (movimiento.getId() == null) throw new IllegalArgumentException("movimiento.id no puede ser null.");
        movimientos.removeIf(m -> m.getId().equals(movimiento.getId()));
        movimientos.add(movimiento);
        return movimiento;
    }
    @Override
    public Optional<NumTalonarioMovimiento> buscarMovimientoPorId(Long id) {
        return movimientos.stream().filter(m -> m.getId().equals(id)).findFirst();
    }
    @Override
    public Optional<NumTalonarioMovimiento> buscarMovimientoPorTalonarioYNro(Long idTalonario, int nroTalonario) {
        return movimientos.stream()
                .filter(m -> m.getIdTalonario().equals(idTalonario) && m.getNroTalonario() == nroTalonario)
                .findFirst();
    }
    @Override
    public List<NumTalonarioMovimiento> listarMovimientosPorTalonario(Long idTalonario) {
        return movimientos.stream().filter(m -> m.getIdTalonario().equals(idTalonario)).toList();
    }
    @Override
    public boolean existeMovimientoTalonarioNumero(Long idTalonario, int nroTalonario) {
        return movimientos.stream()
                .anyMatch(m -> m.getIdTalonario().equals(idTalonario) && m.getNroTalonario() == nroTalonario);
    }
    public long nextMovimientoId() { return secMovimiento.getAndIncrement(); }

    @Override
    public List<Integer> buscarNumerosFaltantesEnRango(Long idTalonario, int nroDesde, int nroHasta) {
        Set<Integer> conMovimiento = movimientos.stream()
                .filter(m -> m.getIdTalonario().equals(idTalonario))
                .map(NumTalonarioMovimiento::getNroTalonario)
                .collect(Collectors.toSet());
        return IntStream.rangeClosed(nroDesde, nroHasta)
                .filter(n -> !conMovimiento.contains(n))
                .boxed()
                .toList();
    }

    /** Simula NEXT VALUE FOR nombreSecuencia con contador in-memory por talonario. */
    public int nextNumeroTalonario(Long idTalonario, int nroDesde, Integer nroHasta) {
        AtomicInteger contador = contadoresNumeracion.computeIfAbsent(
                idTalonario, k -> new AtomicInteger(nroDesde));
        int numero = contador.get();
        if (nroHasta != null && numero > nroHasta) {
            throw new PrecondicionVioladaException(
                    "El talonario " + idTalonario + " ha agotado su rango de numeracion (nroHasta=" + nroHasta + ").");
        }
        contador.incrementAndGet();
        return numero;
    }
}
