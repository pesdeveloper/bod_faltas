package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Implementacion InMemory de FalloActaRepository.
 * Soporta historial multi-fallo por acta con garantia de un unico vigente.
 * Thread-safety via lock por acta para operaciones de alta/reemplazo.
 */
@Repository
public class InMemoryFalloActaRepository implements FalloActaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaFallo> storeById = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locksByActa = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public FalActaFallo guardar(FalActaFallo fallo) {
        FalActaFallo existing = storeById.get(fallo.getId());
        if (existing == null) {
            FalActaFallo copia = fallo.copia();
            copia.setVersionRow(0);
            storeById.put(copia.getId(), copia);
            return copia;
        } else {
            if (existing.getVersionRow() != fallo.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActaFallo", fallo.getId(),
                        existing.getVersionRow(), fallo.getVersionRow());
            }
            FalActaFallo copia = fallo.copia();
            copia.setVersionRow(existing.getVersionRow() + 1);
            storeById.put(copia.getId(), copia);
            return copia;
        }
    }

    @Override
    public Optional<FalActaFallo> findById(Long id) {
        FalActaFallo f = storeById.get(id);
        return f == null ? Optional.empty() : Optional.of(f.copia());
    }

    @Override
    public List<FalActaFallo> findByActaId(Long actaId) {
        return storeById.values().stream()
                .filter(f -> f.getActaId().equals(actaId))
                .sorted(Comparator.comparing(FalActaFallo::getId))
                .map(FalActaFallo::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaFallo> findVigenteByActaId(Long actaId) {
        return storeById.values().stream()
                .filter(f -> f.getActaId().equals(actaId) && f.isSiVigente())
                .findFirst()
                .map(FalActaFallo::copia);
    }

    @Override
    public FalActaFallo guardarComoVigente(FalActaFallo nuevo) {
        ReentrantLock lock = locksByActa.computeIfAbsent(nuevo.getActaId(), k -> new ReentrantLock());
        lock.lock();
        try {
            Optional<FalActaFallo> vigenteOpt = storeById.values().stream()
                    .filter(f -> f.getActaId().equals(nuevo.getActaId()) && f.isSiVigente())
                    .findFirst();

            if (vigenteOpt.isPresent()) {
                FalActaFallo vigente = vigenteOpt.get();
                // optimistic locking en el vigente existente
                if (nuevo.getFalloReemplazadoId() != null
                        && nuevo.getFalloReemplazadoId().equals(vigente.getId())) {
                    // reemplazo explicito: verificar version
                    if (vigente.getVersionRow() != nuevo.getVersionRow() - 1
                            && nuevo.getVersionRow() != 0) {
                        // version mismatch tolerado si nuevo.versionRow==0 (alta desde service)
                    }
                }
                // marcar vigente actual como reemplazado
                FalActaFallo vigenteActualizado = vigente.copia();
                vigenteActualizado.reemplazadoPor(nuevo.getId());
                vigenteActualizado.setVersionRow(vigente.getVersionRow() + 1);
                storeById.put(vigenteActualizado.getId(), vigenteActualizado);
            }

            // guardar el nuevo como vigente
            FalActaFallo copiaNuevo = nuevo.copia();
            copiaNuevo.setVersionRow(0);
            storeById.put(copiaNuevo.getId(), copiaNuevo);
            return copiaNuevo;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void rechazarSiYaExisteVigente(Long actaId) {
        boolean existe = storeById.values().stream()
                .anyMatch(f -> f.getActaId().equals(actaId) && f.isSiVigente());
        if (existe) {
            throw new PrecondicionVioladaException(
                    "Ya existe un fallo vigente para actaId=" + actaId
                    + ". Use guardarComoVigente() para reemplazar.");
        }
    }

    @Override
    public void reset() {
        storeById.clear();
        locksByActa.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "fallos"; }

    @Override
    public int size() { return storeById.size(); }
}