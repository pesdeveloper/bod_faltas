package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.repository.GestionExternaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio InMemory de FalGestionExterna con semantica OCC real (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-GEXT-01):
 *   1. guardar version 0 (nueva).
 *   2. leer devuelve copia detached — mutar la copia no afecta el store.
 *   3. guardar existente: verifica versionRow; si coincide, incrementa y persiste copia.
 *   4. guardar con versionRow desactualizado lanza ConcurrenciaConflictoException.
 */
@Repository
public class InMemoryGestionExternaRepository implements GestionExternaRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final Map<Long, FalGestionExterna> store = new ConcurrentHashMap<>();

    @Override
    public synchronized FalGestionExterna guardar(FalGestionExterna gestion) {
        FalGestionExterna existing = store.get(gestion.getActaId());
        if (existing == null) {
            if (gestion.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalGestionExterna", gestion.getActaId(),
                        0, gestion.getVersionRow());
            }
            store.put(gestion.getActaId(), gestion.copia());
            return gestion;
        }
        if (existing.getVersionRow() != gestion.getVersionRow())
            throw new ConcurrenciaConflictoException("FalGestionExterna", gestion.getActaId(),
                    existing.getVersionRow(), gestion.getVersionRow());
        gestion.setVersionRow(existing.getVersionRow() + 1);
        store.put(gestion.getActaId(), gestion.copia());
        return gestion;
    }

    @Override
    public Optional<FalGestionExterna> buscarActiva(Long actaId) {
        FalGestionExterna stored = store.get(actaId);
        if (stored != null && stored.isSiActiva()) return Optional.of(stored.copia());
        return Optional.empty();
    }

    @Override
    public boolean existeActiva(Long actaId) {
        FalGestionExterna stored = store.get(actaId);
        return stored != null && stored.isSiActiva();
    }

    @Override
    public Optional<FalGestionExterna> buscarPorHistorico(Long actaId) {
        FalGestionExterna stored = store.get(actaId);
        return stored == null ? Optional.empty() : Optional.of(stored.copia());
    }

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }
}
