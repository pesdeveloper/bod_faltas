package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador InMemory de ObservacionRepository. Oraculo de paridad del adaptador
 * JDBC.
 *
 * El contador simula el AUTO_INCREMENT de fal_observacion y es exclusivamente
 * interno: el puerto ya no expone nextId(). La secuencia no se promete sin huecos,
 * igual que en el motor.
 */
@Repository
@Profile("!jdbc")
public class InMemoryObservacionRepository implements ObservacionRepository, ResettableInMemoryRepository {

    /**
     * Orden canonico por propietario (DECISION_OBS-01): el id desempata instantes
     * DATETIME(6) iguales. nullsFirst reproduce el tratamiento que MariaDB da a un
     * fh_alta nulo, que fisicamente no puede existir porque la columna es NOT NULL.
     */
    private static final Comparator<FalObservacion> ORDEN_CANONICO =
            Comparator.comparing(FalObservacion::getFhAlta,
                            Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(FalObservacion::getId,
                            Comparator.nullsFirst(Comparator.naturalOrder()));

    private final Map<Long, FalObservacion> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * Alta exclusivamente. Un id informado se rechaza en vez de insertarse o de
     * sobrescribir la fila: con id inexistente seria preasignar la PK, prohibido por
     * DECISION_ID-01, y con id existente seria reescribir una fila append-only,
     * prohibido por DECISION_OBS-01. El adaptador JDBC no podria hacer ninguna de
     * las dos cosas sin violar el modelo, de modo que tampoco lo hace este.
     */
    @Override
    public FalObservacion guardar(FalObservacion obs) {
        if (obs.getId() != null) {
            throw new PrecondicionVioladaException(
                    "Una observacion persistida no se edita: para corregirla, darla de baja"
                    + " logicamente e insertar una nueva. id recibido=" + obs.getId());
        }
        FalObservacion persistida = obs.conId(idCounter.getAndIncrement());
        store.putIfAbsent(persistida.getId(), persistida);
        return persistida.copia();
    }

    @Override
    public Optional<FalObservacion> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalObservacion::copia);
    }

    @Override
    public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return listar(tipo, entidadId, false);
    }

    @Override
    public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return listar(tipo, entidadId, true);
    }

    private List<FalObservacion> listar(EntidadTipoObservada tipo, Long entidadId, boolean soloActivas) {
        return store.values().stream()
                .filter(o -> o.getEntidadTipo() == tipo && o.getEntidadId().equals(entidadId))
                .filter(o -> !soloActivas || o.isSiActiva())
                .sorted(ORDEN_CANONICO)
                .map(FalObservacion::copia)
                .toList();
    }

    /**
     * Batch del historial (DECISION-OBS-BATCH-01). Ordena una vez el conjunto completo y
     * agrupa despues: como el agrupado preserva el orden de encuentro, cada historial
     * queda en el orden canonico sin necesidad de reordenar grupo por grupo.
     *
     * <p>Los elementos son copias defensivas y las listas son inmodificables, igual que
     * en las dos lecturas individuales. Un propietario sin filas no recibe entrada.
     */
    @Override
    public Map<Long, List<FalObservacion>> listarPorEntidades(
            EntidadTipoObservada tipo, Collection<Long> entidadIds) {
        Set<Long> ids = idsDePropietarios(tipo, entidadIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<FalObservacion>> historiales = new LinkedHashMap<>();
        store.values().stream()
                .filter(o -> o.getEntidadTipo() == tipo && ids.contains(o.getEntidadId()))
                .sorted(ORDEN_CANONICO)
                .forEach(o -> historiales
                        .computeIfAbsent(o.getEntidadId(), clave -> new ArrayList<>())
                        .add(o.copia()));

        historiales.replaceAll((clave, historial) -> List.copyOf(historial));
        return Map.copyOf(historiales);
    }

    /**
     * Valida los argumentos del batch y deduplica los ids. La deduplicacion es lo que
     * hace que un id repetido en la entrada no produzca filas ni grupos duplicados.
     *
     * <p>Los mensajes son identicos a los del adaptador JDBC: ninguno de los dos puede
     * resolver un tipo nulo o un id nulo, de modo que ambos rechazan igual.
     */
    private static Set<Long> idsDePropietarios(EntidadTipoObservada tipo, Collection<Long> entidadIds) {
        if (tipo == null) {
            throw new IllegalArgumentException("tipo es obligatorio");
        }
        if (entidadIds == null) {
            throw new IllegalArgumentException("entidadIds es obligatoria");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : entidadIds) {
            if (id == null) {
                throw new IllegalArgumentException("entidadIds no admite ids nulos");
            }
            ids.add(id);
        }
        return ids;
    }

    /**
     * Baja logica atomica dentro de la seccion critica por clave: cuando la funcion
     * de compute lanza, ConcurrentHashMap deja el mapeo sin tocar, de modo que un id
     * inexistente no inserta y una segunda baja no altera la fila.
     *
     * La transicion la aplica FalObservacion.desactivar, que conserva el texto y los
     * datos de alta y exige ambos datos de baja.
     */
    @Override
    public FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja) {
        FalObservacion dadaDeBaja = store.compute(id, (clave, existente) -> {
            if (existente == null) {
                throw new PrecondicionVioladaException("Observacion no encontrada: " + id);
            }
            FalObservacion baja = existente.copia();
            baja.desactivar(fhBaja, idUserBaja);
            return baja;
        });
        return dadaDeBaja.copia();
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "observaciones"; }

    @Override
    public int size() { return store.size(); }
}
