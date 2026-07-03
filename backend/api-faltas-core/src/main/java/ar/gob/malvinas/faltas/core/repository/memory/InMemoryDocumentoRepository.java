package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentoRepository implements DocumentoRepository {

    private final Map<Long, FalDocumento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public FalDocumento guardar(FalDocumento documento) {
        store.put(documento.getId(), documento);
        return documento;
    }

    @Override
    public Optional<FalDocumento> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumento> buscarPorActa(Long idActa) {
        return store.values().stream()
                .filter(d -> idActa.equals(d.getIdActa()))
                .toList();
    }

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }
}