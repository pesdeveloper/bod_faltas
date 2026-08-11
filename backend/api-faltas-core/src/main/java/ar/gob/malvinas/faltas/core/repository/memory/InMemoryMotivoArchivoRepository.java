package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
@Profile("!jdbc")
public class InMemoryMotivoArchivoRepository implements MotivoArchivoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalMotivoArchivo> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Object writeLock = new Object();

    /**
     * El id se asigna al insertar, simulando el AUTO_INCREMENT de fal_motivo_archivo.
     * La secuencia es un generador de test: el puerto ya no expone nextId().
     *
     * Un id que no corresponde a ninguna fila se rechaza en vez de insertarse:
     * elegir la PK desde el llamador contradice DECISION_ID-01 y el adaptador JDBC
     * tampoco puede hacerlo (D-1A1-01). La existencia se verifica antes que la
     * unicidad del codigo porque en JDBC el UPDATE sobre un id inexistente no
     * alcanza a evaluar el UNIQUE.
     */
    @Override
    public synchronized FalMotivoArchivo guardar(FalMotivoArchivo motivo) {
        synchronized (writeLock) {
            exigirExistenciaSiTraeId(motivo);
            validarUnicidadCodigo(motivo);
            FalMotivoArchivo copia = motivo.copia();
            if (copia.getId() == null) {
                copia.setId(idCounter.getAndIncrement());
            }
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

    /** Orden por id ascendente, igual que el adaptador JDBC (D-1A1-03). */
    @Override
    public List<FalMotivoArchivo> listarActivos() {
        return store.values().stream()
                .filter(FalMotivoArchivo::isSiActivo)
                .sorted(Comparator.comparing(FalMotivoArchivo::getId))
                .map(FalMotivoArchivo::copia)
                .collect(Collectors.toList());
    }

    /** Orden por id ascendente, igual que el adaptador JDBC (D-1A1-03). */
    @Override
    public List<FalMotivoArchivo> listarTodos() {
        return store.values().stream()
                .sorted(Comparator.comparing(FalMotivoArchivo::getId))
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

    private void exigirExistenciaSiTraeId(FalMotivoArchivo motivo) {
        if (motivo.getId() != null && !store.containsKey(motivo.getId())) {
            throw new PrecondicionVioladaException("MotivoArchivo no encontrado: " + motivo.getId());
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
