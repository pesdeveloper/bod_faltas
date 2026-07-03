package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPagoVoluntarioRepository implements PagoVoluntarioRepository {

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
}
