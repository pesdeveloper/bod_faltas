package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArchivo;
import ar.gob.malvinas.faltas.core.repository.ActaArchivoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementacion InMemory de ActaArchivoRepository.
 *
 * Usa synchronized por actaId para proteger invariante de unicidad de ciclo activo.
 */
@Repository
public class InMemoryActaArchivoRepository implements ActaArchivoRepository, ResettableInMemoryRepository {

    private final Map<Long, List<FalActaArchivo>> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaArchivo guardar(FalActaArchivo archivo) {
        synchronized (lockFor(archivo.getActaId())) {
            List<FalActaArchivo> lista = store.computeIfAbsent(archivo.getActaId(), k -> new ArrayList<>());
            FalActaArchivo existing = lista.stream()
                    .filter(a -> a.getId().equals(archivo.getId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                FalActaArchivo copia = archivo.copia();
                copia.setVersionRow(0);
                lista.add(copia);
                return copia.copia();
            } else {
                if (existing.getVersionRow() != archivo.getVersionRow()) {
                    throw new ConcurrenciaConflictoException("FalActaArchivo", archivo.getId(),
                            existing.getVersionRow(), archivo.getVersionRow());
                }
                int idx = lista.indexOf(existing);
                FalActaArchivo copia = archivo.copia();
                copia.setVersionRow(existing.getVersionRow() + 1);
                lista.set(idx, copia);
                return copia.copia();
            }
        }
    }

    @Override
    public Optional<FalActaArchivo> buscarActivoPorActa(Long actaId) {
        List<FalActaArchivo> lista = store.get(actaId);
        if (lista == null) return Optional.empty();
        return lista.stream()
                .filter(FalActaArchivo::isSiActivo)
                .findFirst()
                .map(FalActaArchivo::copia);
    }

    @Override
    public List<FalActaArchivo> listarHistoricoPorActa(Long actaId) {
        List<FalActaArchivo> lista = store.get(actaId);
        if (lista == null) return new ArrayList<>();
        return lista.stream().map(FalActaArchivo::copia).collect(Collectors.toList());
    }

    @Override
    public FalActaArchivo crearActivoAtomicamente(FalActaArchivo archivo) {
        synchronized (lockFor(archivo.getActaId())) {
            List<FalActaArchivo> lista = store.computeIfAbsent(archivo.getActaId(), k -> new ArrayList<>());
            boolean existeActivo = lista.stream().anyMatch(FalActaArchivo::isSiActivo);
            if (existeActivo) {
                throw new PrecondicionVioladaException(
                        "Ya existe un archivo activo para el acta: " + archivo.getActaId());
            }
            FalActaArchivo copia = archivo.copia();
            copia.setVersionRow(0);
            lista.add(copia);
            return copia.copia();
        }
    }

    @Override
    public FalActaArchivo cerrarActivoAtomicamente(Long actaId, FalActaArchivo cierre) {
        synchronized (lockFor(actaId)) {
            List<FalActaArchivo> lista = store.get(actaId);
            if (lista == null) {
                throw new PrecondicionVioladaException("No hay ciclos de archivo para el acta: " + actaId);
            }
            FalActaArchivo activo = lista.stream()
                    .filter(FalActaArchivo::isSiActivo)
                    .findFirst()
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe archivo activo para el acta: " + actaId));
            if (activo.getVersionRow() != cierre.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActaArchivo", activo.getId(),
                        activo.getVersionRow(), cierre.getVersionRow());
            }
            int idx = lista.indexOf(activo);
            FalActaArchivo copia = cierre.copia();
            copia.setVersionRow(activo.getVersionRow() + 1);
            lista.set(idx, copia);
            return copia.copia();
        }
    }

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
    public String nombre() { return "archivos"; }

    @Override
    public int size() {
        return store.values().stream().mapToInt(List::size).sum();
    }
}
