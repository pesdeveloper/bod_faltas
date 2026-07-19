package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio InMemory de FalActaSnapshot con semantica OCC estricta (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-SNAP-01):
 *   INSERT: entidad inexistente; versionRow debe ser 0; persistida queda versionRow 0.
 *   UPDATE: version recibida coincide exactamente con almacenada; persistida incrementa en 1.
 *   Mismatch -> ConcurrenciaConflictoException.
 *   No existe bypass para version 0.
 *
 * El recalculador (SnapshotRecalculador) debe leer el snapshot existente para preservar
 * su versionRow antes de llamar a guardar. No debe crear new FalActaSnapshot() si la
 * entidad ya existe en el store.
 */
@Repository
public class InMemoryActaSnapshotRepository implements ActaSnapshotRepository {

    private final Map<Long, FalActaSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public synchronized FalActaSnapshot guardar(FalActaSnapshot snapshot) {
        FalActaSnapshot existing = store.get(snapshot.getIdActa());
        if (existing == null) {
            if (snapshot.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalActaSnapshot", snapshot.getIdActa(),
                        0, snapshot.getVersionRow());
            }
            store.put(snapshot.getIdActa(), snapshot.copia());
            return snapshot;
        }
        if (existing.getVersionRow() != snapshot.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaSnapshot", snapshot.getIdActa(),
                    existing.getVersionRow(), snapshot.getVersionRow());
        snapshot.setVersionRow(existing.getVersionRow() + 1);
        store.put(snapshot.getIdActa(), snapshot.copia());
        return snapshot;
    }

    @Override
    public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
        FalActaSnapshot stored = store.get(idActa);
        return stored == null ? Optional.empty() : Optional.of(stored.copia());
    }
}
