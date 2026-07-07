package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion Spring-managed de PagoVoluntarioRepository.
 * Reemplaza InMemoryPagoVoluntarioRepository como Spring bean.
 * Preserva compatibilidad con PagoVoluntarioService y SnapshotRecalculador.
 * La persistencia primaria de obligaciones esta en InMemoryObligacionPagoRepository.
 */
@Repository
@Primary
public class PagoVoluntarioAdapterRepository implements PagoVoluntarioRepository, ResettableInMemoryRepository {

    private final Map<Long, FalPagoVoluntario> store = new ConcurrentHashMap<>();

    @Override
    public FalPagoVoluntario guardar(FalPagoVoluntario pago) {
        store.put(pago.getActaId(), pago);
        return pago;
    }

    @Override
    public Optional<FalPagoVoluntario> buscarPorActa(Long actaId) {
        return Optional.ofNullable(store.get(actaId));
    }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "pagos-voluntarios-adapter"; }

    @Override
    public int size() { return store.size(); }
}
