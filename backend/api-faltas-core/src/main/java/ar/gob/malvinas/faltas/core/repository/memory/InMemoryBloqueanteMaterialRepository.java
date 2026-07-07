package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryBloqueanteMaterialRepository implements BloqueanteMaterialRepository {

    private final AtomicLong idGen = new AtomicLong(1);
    private final List<FalBloqueanteMaterial> store = new CopyOnWriteArrayList<>();

    @Override
    public Long nextId() {
        return idGen.getAndIncrement();
    }

    @Override
    public FalBloqueanteMaterial guardar(FalBloqueanteMaterial bloqueante) {
        store.removeIf(b -> b.getId().equals(bloqueante.getId()));
        store.add(bloqueante);
        return bloqueante;
    }

    @Override
    public Optional<FalBloqueanteMaterial> findById(Long id) {
        return store.stream()
                .filter(b -> id.equals(b.getId()))
                .findFirst();
    }

    @Override
    public List<FalBloqueanteMaterial> findByActaId(Long actaId) {
        return store.stream()
                .filter(b -> actaId.equals(b.getActaId()))
                .toList();
    }

    @Override
    public boolean existsActivoByActaId(Long actaId) {
        return store.stream()
                .anyMatch(b -> actaId.equals(b.getActaId()) && b.isSiActivo());
    }
}