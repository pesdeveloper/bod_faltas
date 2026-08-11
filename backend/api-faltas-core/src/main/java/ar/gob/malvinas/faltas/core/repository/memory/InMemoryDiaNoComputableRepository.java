package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableSaveResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion in-memory de DiaNoComputableRepository.
 *
 * Garantias:
 *  - Exactamente un registro activo por fecha (atomicidad via ConcurrentHashMap.compute).
 *  - Copias defensivas en entrada y salida.
 *  - Reset limpia registros, indices y reinicia la secuencia.
 *
 * Identificadores: la secuencia AtomicLong simula el AUTO_INCREMENT de
 * fal_dia_no_computable. Es un generador de test, no productivo: el puerto ya no
 * expone nextId() y el id se asigna al insertar.
 */
@Repository
@Profile("!jdbc")
public class InMemoryDiaNoComputableRepository
        implements DiaNoComputableRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDiaNoComputable> store = new ConcurrentHashMap<>();
    private final Map<LocalDate, FalDiaNoComputable> activeByDate = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * Con id nulo inserta; con id presente actualiza esa fila.
     *
     * Un id que no corresponde a ninguna fila se rechaza en vez de insertarse:
     * elegir la PK desde el llamador contradice DECISION_ID-01 y el adaptador JDBC
     * tampoco puede hacerlo (D-1A1-01). La excepcion y el mensaje son los mismos en
     * ambos adaptadores.
     */
    @Override
    public synchronized FalDiaNoComputable guardar(FalDiaNoComputable dia) {
        if (dia.getId() != null && !store.containsKey(dia.getId())) {
            throw new PrecondicionVioladaException("Dia no computable no encontrado: id=" + dia.getId());
        }
        FalDiaNoComputable copia = dia.getId() == null
                ? dia.conId(idCounter.getAndIncrement())
                : dia.copia();
        store.put(copia.getId(), copia);
        if (copia.isSiActivo()) {
            activeByDate.put(copia.getFecha(), copia);
        } else {
            activeByDate.remove(copia.getFecha(), copia);
            FalDiaNoComputable enStore = store.get(copia.getId());
            if (enStore != null && !enStore.isSiActivo()) {
                activeByDate.computeIfPresent(copia.getFecha(), (k, v) -> {
                    if (v.getId().equals(copia.getId())) return null;
                    return v;
                });
            }
        }
        return store.get(copia.getId()).copia();
    }

    /**
     * El id se asigna dentro del compute, solo cuando esta llamada es la que inserta,
     * de modo que un candidato que pierde la carrera no consume identificador.
     *
     * Es una propiedad de esta simulacion, no del motor: en MariaDB un INSERT
     * rechazado por el UNIQUE de fecha_activa no deja fila pero si puede consumir un
     * valor AUTO_INCREMENT. La paridad exigida entre ambos adaptadores es unicidad,
     * identidad asignada y resultado explicito; la numeracion no es gapless.
     */
    @Override
    public DiaNoComputableSaveResult guardarActivoSiAusentePorFecha(FalDiaNoComputable candidato) {
        FalDiaNoComputable[] resultado = new FalDiaNoComputable[1];
        boolean[] yaExistia = new boolean[1];

        activeByDate.compute(candidato.getFecha(), (fecha, existing) -> {
            if (existing != null) {
                resultado[0] = existing;
                yaExistia[0] = true;
                return existing;
            }
            FalDiaNoComputable persistido = candidato.getId() == null
                    ? candidato.conId(idCounter.getAndIncrement())
                    : candidato.copia();
            store.put(persistido.getId(), persistido);
            resultado[0] = persistido;
            yaExistia[0] = false;
            return persistido;
        });

        return new DiaNoComputableSaveResult(resultado[0].copia(), yaExistia[0]);
    }

    @Override
    public Optional<FalDiaNoComputable> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalDiaNoComputable::copia);
    }

    @Override
    public Optional<FalDiaNoComputable> buscarActivoPorFecha(LocalDate fecha) {
        return Optional.ofNullable(activeByDate.get(fecha)).map(FalDiaNoComputable::copia);
    }

    @Override
    public Optional<FalDiaNoComputable> buscarPorOrigenYReferenciaExterna(
            OrigenDiaNoComputable origen,
            String referenciaExterna) {
        if (origen == null || referenciaExterna == null || referenciaExterna.isBlank())
            return Optional.empty();
        String refNorm = referenciaExterna.trim();
        return store.values().stream()
                .filter(d -> d.getOrigen() == origen
                        && refNorm.equals(d.getReferenciaExterna()))
                .findFirst()
                .map(FalDiaNoComputable::copia);
    }

    @Override
    public List<FalDiaNoComputable> listarActivosOrdenados() {
        return store.values().stream()
                .filter(FalDiaNoComputable::isSiActivo)
                .sorted(Comparator.comparing(FalDiaNoComputable::getFecha)
                        .thenComparing(FalDiaNoComputable::getId))
                .map(FalDiaNoComputable::copia)
                .toList();
    }

    @Override
    public void reset() {
        store.clear();
        activeByDate.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "dias-no-computables"; }

    @Override
    public int size() { return store.size(); }
}
