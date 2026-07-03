package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPagoCondenaRepository implements PagoCondenaRepository {

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
}

