package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryMotivoArchivoRepository implements MotivoArchivoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalMotivoArchivo> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Object writeLock = new Object();

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public synchronized FalMotivoArchivo guardar(FalMotivoArchivo motivo) {
        synchronized (writeLock) {
            validarUnicidadCodigo(motivo);
            FalMotivoArchivo copia = motivo.copia();
            store.put(copia.getId(), copia);
            return copia.copia();
        }
    }

    @Override
    public Optional<FalMotivoArchivo> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalMotivoArchivo::copia);
    }

    @Override
    public Optional<FalMotivoArchivo> buscarPorCodigo(String codMotivoArchivo) {
        if (codMotivoArchivo == null) return Optional.empty();
        String normalizado = codMotivoArchivo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> m.getCodMotivoArchivo().equals(normalizado))
                .findFirst()
                .map(FalMotivoArchivo::copia);
    }

    @Override
    public List<FalMotivoArchivo> listarActivos() {
        return store.values().stream()
                .filter(FalMotivoArchivo::isSiActivo)
                .map(FalMotivoArchivo::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalMotivoArchivo> listarTodos() {
        return store.values().stream()
                .map(FalMotivoArchivo::copia)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized FalMotivoArchivo actualizarAtomicamente(FalMotivoArchivo motivo) {
        synchronized (writeLock) {
            FalMotivoArchivo existing = store.get(motivo.getId());
            if (existing == null) {
                throw new PrecondicionVioladaException("MotivoArchivo no encontrado: " + motivo.getId());
            }
            validarUnicidadCodigo(motivo);
            FalMotivoArchivo copia = motivo.copia();
            store.put(copia.getId(), copia);
            return copia.copia();
        }
    }

    private void validarUnicidadCodigo(FalMotivoArchivo motivo) {
        String normalizado = motivo.getCodMotivoArchivo().trim().toUpperCase();
        boolean duplicado = store.values().stream()
                .anyMatch(m -> !m.getId().equals(motivo.getId())
                        && m.getCodMotivoArchivo().equals(normalizado));
        if (duplicado) {
            throw new PrecondicionVioladaException(
                    "Ya existe un motivo con codigo: " + normalizado);
        }
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "motivos-archivo"; }

    @Override
    public int size() { return store.size(); }
}
