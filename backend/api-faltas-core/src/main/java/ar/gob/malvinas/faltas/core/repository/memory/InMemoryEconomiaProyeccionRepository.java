package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio InMemory de FalActaEconomiaProyeccion con semantica OCC estricta (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-ECPR-01):
 *   INSERT: entidad inexistente; versionRow debe ser 0; persistida queda versionRow 0.
 *   UPDATE: version recibida coincide exactamente con almacenada; persistida incrementa en 1.
 *   Mismatch -> ConcurrenciaConflictoException.
 *   No existe bypass para version 0.
 *   fhUltMod no puede ser null antes de persistir.
 */
@Repository
public class InMemoryEconomiaProyeccionRepository
        implements EconomiaProyeccionRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaEconomiaProyeccion> store = new ConcurrentHashMap<>();

    @Override
    public synchronized FalActaEconomiaProyeccion save(FalActaEconomiaProyeccion proyeccion) {
        if (proyeccion.getFhUltMod() == null)
            throw new IllegalStateException(
                    "FalActaEconomiaProyeccion.fhUltMod no puede ser null antes de persistir (DECISION_DDL-ECPR-01)");
        FalActaEconomiaProyeccion existing = store.get(proyeccion.getActaId());
        if (existing == null) {
            if (proyeccion.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalActaEconomiaProyeccion", proyeccion.getActaId(),
                        0, proyeccion.getVersionRow());
            }
            store.put(proyeccion.getActaId(), proyeccion.copia());
            return proyeccion;
        }
        if (existing.getVersionRow() != proyeccion.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaEconomiaProyeccion", proyeccion.getActaId(),
                    existing.getVersionRow(), proyeccion.getVersionRow());
        proyeccion.setVersionRow(existing.getVersionRow() + 1);
        store.put(proyeccion.getActaId(), proyeccion.copia());
        return proyeccion;
    }

    @Override
    public Optional<FalActaEconomiaProyeccion> findByActaId(Long actaId) {
        return Optional.ofNullable(store.get(actaId)).map(FalActaEconomiaProyeccion::copia);
    }

    @Override
    public void deleteByActaId(Long actaId) { store.remove(actaId); }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "economia-proyeccion"; }

    @Override
    public int size() { return store.size(); }
}
