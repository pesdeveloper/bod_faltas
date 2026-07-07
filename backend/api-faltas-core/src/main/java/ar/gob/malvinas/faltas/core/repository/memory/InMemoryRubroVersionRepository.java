package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.repository.RubroVersionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Repositorio InMemory para versiones de rubro comercial.
 * La operacion sincronizarAtomicamente garantiza una sola version actual por Id_Rub.
 */
@Repository
public class InMemoryRubroVersionRepository implements RubroVersionRepository, ResettableInMemoryRepository {

    private final List<FalRubroVersion> store = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Object lockIdRub = new Object();

    @Override public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public FalRubroVersion guardar(FalRubroVersion version) {
        store.removeIf(v -> v.getRubroId().equals(version.getRubroId()));
        store.add(version.copia());
        return findByRubroId(version.getRubroId()).orElseThrow();
    }

    @Override
    public Optional<FalRubroVersion> findByRubroId(Long rubroId) {
        return store.stream().filter(v -> v.getRubroId().equals(rubroId)).findFirst().map(FalRubroVersion::copia);
    }

    @Override
    public Optional<FalRubroVersion> findActualByIdRub(int idRub) {
        return store.stream()
                .filter(v -> v.getIdRub() == idRub && v.isSiVersionActual() && v.getValidTo() == null)
                .findFirst().map(FalRubroVersion::copia);
    }

    @Override
    public List<FalRubroVersion> findAllActualesActivas() {
        return store.stream()
                .filter(v -> v.isSiVersionActual() && v.isSiActivo() && v.getValidTo() == null)
                .map(FalRubroVersion::copia).toList();
    }

    @Override
    public List<FalRubroVersion> findByIdRub(int idRub) {
        return store.stream().filter(v -> v.getIdRub() == idRub).map(FalRubroVersion::copia).toList();
    }

    @Override
    public FalRubroVersion sincronizarAtomicamente(FalRubroVersion nuevaVersion) {
        synchronized (lockIdRub) {
            // Cerrar version actual si existe
            Optional<FalRubroVersion> actualOpt = store.stream()
                    .filter(v -> v.getIdRub() == nuevaVersion.getIdRub()
                            && v.isSiVersionActual()
                            && v.getValidTo() == null)
                    .findFirst();
            if (actualOpt.isPresent()) {
                FalRubroVersion actual = actualOpt.get();
                // Si el hash es identico, no crear nueva version
                if (actual.getRowHash().equals(nuevaVersion.getRowHash())) {
                    return actual.copia();
                }
                // Cerrar version anterior
                actual.setSiVersionActual(false);
                actual.setValidTo(LocalDateTime.now());
                actual.setCloseOperation(nuevaVersion.getSourceOperation());
                store.removeIf(v -> v.getRubroId().equals(actual.getRubroId()));
                store.add(actual.copia());
            }
            // Guardar nueva version como actual
            store.add(nuevaVersion.copia());
            return findByRubroId(nuevaVersion.getRubroId()).orElseThrow();
        }
    }

    @Override public void reset() { store.clear(); idCounter.set(1); }
    @Override public String nombre() { return "rubro-versiones"; }
    @Override public int size() { return store.size(); }
}
