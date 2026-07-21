package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio InMemory de FalBloqueanteMaterial con semantica OCC real (copy-on-read/write).
 *
 * Contrato OCC (DECISION_DDL-BLOQ-01):
 *   1. guardar version 0 (nueva).
 *   2. leer devuelve copia detached — mutar la copia no afecta el store.
 *   3. guardar existente: verifica versionRow; si coincide, incrementa y persiste copia.
 *   4. guardar con versionRow desactualizado lanza ConcurrenciaConflictoException.
 */
@Repository
public class InMemoryBloqueanteMaterialRepository implements BloqueanteMaterialRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final List<FalBloqueanteMaterial> store = new CopyOnWriteArrayList<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public synchronized FalBloqueanteMaterial guardar(FalBloqueanteMaterial bloqueante) {
        java.util.Optional<FalBloqueanteMaterial> existingOpt = store.stream()
                .filter(b -> b.getId().equals(bloqueante.getId()))
                .findFirst();
        if (existingOpt.isEmpty()) {
            if (bloqueante.getVersionRow() != 0) {
                throw new ConcurrenciaConflictoException("FalBloqueanteMaterial", bloqueante.getId(),
                        0, bloqueante.getVersionRow());
            }
            store.add(bloqueante.copia());
            return bloqueante;
        }
        FalBloqueanteMaterial existing = existingOpt.get();
        if (existing.getVersionRow() != bloqueante.getVersionRow())
            throw new ConcurrenciaConflictoException("FalBloqueanteMaterial", bloqueante.getId(),
                    existing.getVersionRow(), bloqueante.getVersionRow());
        store.removeIf(b -> b.getId().equals(bloqueante.getId()));
        bloqueante.setVersionRow(existing.getVersionRow() + 1);
        store.add(bloqueante.copia());
        return bloqueante;
    }

    @Override
    public Optional<FalBloqueanteMaterial> findById(Long id) {
        return store.stream()
                .filter(b -> id.equals(b.getId()))
                .map(FalBloqueanteMaterial::copia)
                .findFirst();
    }

    @Override
    public List<FalBloqueanteMaterial> findByActaId(Long actaId) {
        return store.stream()
                .filter(b -> actaId.equals(b.getActaId()))
                .map(FalBloqueanteMaterial::copia)
                .toList();
    }

    @Override
    public boolean existsActivoByActaId(Long actaId) {
        return store.stream()
                .anyMatch(b -> actaId.equals(b.getActaId()) && b.isSiActivo());
    }
}
