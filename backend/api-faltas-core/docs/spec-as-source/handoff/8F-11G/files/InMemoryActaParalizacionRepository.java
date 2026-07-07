package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaParalizacion;
import ar.gob.malvinas.faltas.core.repository.ActaParalizacionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementacion InMemory de ActaParalizacionRepository.
 *
 * Usa un Map<Long, List<FalActaParalizacion>> donde la clave es actaId.
 * ConcurrentHashMap + synchronized por actaId para invariantes multi-fila.
 * CopyOnWriteArrayList no basta para unicidad activa.
 */
@Repository
public class InMemoryActaParalizacionRepository implements ActaParalizacionRepository, ResettableInMemoryRepository {

    private final Map<Long, List<FalActaParalizacion>> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaParalizacion guardar(FalActaParalizacion paralizacion) {
        synchronized (lockFor(paralizacion.getActaId())) {
            List<FalActaParalizacion> lista = store.computeIfAbsent(paralizacion.getActaId(), k -> new ArrayList<>());
            FalActaParalizacion existing = lista.stream()
                    .filter(p -> p.getId().equals(paralizacion.getId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                FalActaParalizacion copia = paralizacion.copia();
                copia.setVersionRow(0);
                lista.add(copia);
                return copia.copia();
            } else {
                if (existing.getVersionRow() != paralizacion.getVersionRow()) {
                    throw new ConcurrenciaConflictoException("FalActaParalizacion", paralizacion.getId(),
                            existing.getVersionRow(), paralizacion.getVersionRow());
                }
                int idx = lista.indexOf(existing);
                FalActaParalizacion copia = paralizacion.copia();
                copia.setVersionRow(existing.getVersionRow() + 1);
                lista.set(idx, copia);
                return copia.copia();
            }
        }
    }

    @Override
    public Optional<FalActaParalizacion> buscarActivaPorActa(Long actaId) {
        List<FalActaParalizacion> lista = store.get(actaId);
        if (lista == null) return Optional.empty();
        return lista.stream()
                .filter(FalActaParalizacion::isSiActiva)
                .findFirst()
                .map(FalActaParalizacion::copia);
    }

    @Override
    public List<FalActaParalizacion> listarHistoricoPorActa(Long actaId) {
        List<FalActaParalizacion> lista = store.get(actaId);
        if (lista == null) return new ArrayList<>();
        return lista.stream().map(FalActaParalizacion::copia).collect(Collectors.toList());
    }

    @Override
    public FalActaParalizacion crearActivaAtomicamente(FalActaParalizacion paralizacion) {
        synchronized (lockFor(paralizacion.getActaId())) {
            List<FalActaParalizacion> lista = store.computeIfAbsent(paralizacion.getActaId(), k -> new ArrayList<>());
            boolean existeActiva = lista.stream().anyMatch(FalActaParalizacion::isSiActiva);
            if (existeActiva) {
                throw new PrecondicionVioladaException(
                        "Ya existe una paralizacion activa para el acta: " + paralizacion.getActaId());
            }
            FalActaParalizacion copia = paralizacion.copia();
            copia.setVersionRow(0);
            lista.add(copia);
            return copia.copia();
        }
    }

    @Override
    public FalActaParalizacion cerrarActivaAtomicamente(Long actaId, FalActaParalizacion cierre) {
        synchronized (lockFor(actaId)) {
            List<FalActaParalizacion> lista = store.get(actaId);
            if (lista == null) {
                throw new PrecondicionVioladaException("No hay ciclos de paralizacion para el acta: " + actaId);
            }
            FalActaParalizacion activa = lista.stream()
                    .filter(FalActaParalizacion::isSiActiva)
                    .findFirst()
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe paralizacion activa para el acta: " + actaId));
            if (activa.getVersionRow() != cierre.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActaParalizacion", activa.getId(),
                        activa.getVersionRow(), cierre.getVersionRow());
            }
            int idx = lista.indexOf(activa);
            FalActaParalizacion copia = cierre.copia();
            copia.setVersionRow(activa.getVersionRow() + 1);
            lista.set(idx, copia);
            return copia.copia();
        }
    }

    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    private Object lockFor(Long actaId) {
        return locks.computeIfAbsent(actaId, k -> new Object());
    }

    @Override
    public void reset() {
        store.clear();
        locks.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "paralizaciones"; }

    @Override
    public int size() {
        return store.values().stream().mapToInt(List::size).sum();
    }
}
