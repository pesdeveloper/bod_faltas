package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoMedidaAplicada;
import ar.gob.malvinas.faltas.core.repository.ActaMedidaPreventivaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryActaMedidaPreventivaRepository
        implements ActaMedidaPreventivaRepository, ResettableInMemoryRepository {
    private final List<FalActaMedidaPreventiva> store = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public FalActaMedidaPreventiva guardar(FalActaMedidaPreventiva medida) {
        store.removeIf(m -> m.getId().equals(medida.getId()));
        store.add(medida.copia());
        return findById(medida.getId()).orElseThrow();
    }

    @Override
    public Optional<FalActaMedidaPreventiva> findById(Long id) {
        return store.stream().filter(m -> id.equals(m.getId())).findFirst().map(FalActaMedidaPreventiva::copia);
    }

    @Override
    public List<FalActaMedidaPreventiva> findByActaId(Long actaId) {
        return store.stream().filter(m -> actaId.equals(m.getActaId())).map(FalActaMedidaPreventiva::copia).toList();
    }

    @Override
    public List<FalActaMedidaPreventiva> findActivasByActaId(Long actaId) {
        return store.stream()
                .filter(m -> actaId.equals(m.getActaId()) && m.getEstadoMedida() == EstadoMedidaAplicada.APLICADA)
                .map(FalActaMedidaPreventiva::copia).toList();
    }

    @Override public void reset() { store.clear(); idCounter.set(1); }
    @Override public String nombre() { return "acta-medidas-preventivas"; }
    @Override public int size() { return store.size(); }
}
