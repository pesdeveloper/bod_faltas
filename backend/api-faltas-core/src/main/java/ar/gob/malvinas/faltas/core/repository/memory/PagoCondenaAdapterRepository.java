package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacion Spring-managed de PagoCondenaRepository.
 * Reemplaza InMemoryPagoCondenaRepository como Spring bean.
 * Preserva compatibilidad con PagoCondenaService y SnapshotRecalculador.
 * La persistencia primaria de obligaciones esta en InMemoryObligacionPagoRepository.
 */
@Repository
@Primary
public class PagoCondenaAdapterRepository implements PagoCondenaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalPagoCondena> store = new ConcurrentHashMap<>();

    @Override
    public FalPagoCondena guardar(FalPagoCondena pago) {
        store.put(pago.getActaId(), pago);
        return pago;
    }

    @Override
    public Optional<FalPagoCondena> buscarPorActa(Long actaId) {
        return Optional.ofNullable(store.get(actaId));
    }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "pagos-condena-adapter"; }

    @Override
    public int size() { return store.size(); }
}
