package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryActaEvidenciaRepository implements ActaEvidenciaRepository {

    private final Map<Long, FalActaEvidencia> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaEvidencia guardar(FalActaEvidencia evidencia) {
        store.put(evidencia.getId(), evidencia);
        return evidencia;
    }

    @Override
    public List<FalActaEvidencia> listarPorActa(Long idActa) {
        return store.values().stream()
                .filter(e -> e.getIdActa().equals(idActa))
                .collect(Collectors.toList());
    }
}
