package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentoPlantillaDefaultRepository implements DocumentoPlantillaDefaultRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDocumentoPlantillaDefault> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public Long nextId() { return counter.getAndIncrement(); }

    @Override
    public FalDocumentoPlantillaDefault guardar(FalDocumentoPlantillaDefault d) {
        store.put(d.getId(), d);
        return d;
    }

    @Override
    public Optional<FalDocumentoPlantillaDefault> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FalDocumentoPlantillaDefault> buscarDefaultsVigentes(
            AccionDocumental accionDocumental, TipoActa tipoActa,
            Long idDependencia, LocalDateTime en) {
        return store.values().stream()
                .filter(d -> d.vigente(en))
                .filter(d -> d.getAccionDocumental() == accionDocumental)
                .filter(d -> tipoActa == null
                        ? d.getTipoActa() == null
                        : d.getTipoActa() == null || d.getTipoActa() == tipoActa)
                .filter(d -> idDependencia == null
                        ? d.getIdDependencia() == null
                        : d.getIdDependencia() == null || d.getIdDependencia().equals(idDependencia))
                .toList();
    }

    @Override
    public List<FalDocumentoPlantillaDefault> listar() {
        return List.copyOf(store.values());
    }

    @Override
    public void reset() { store.clear(); }

    @Override
    public String nombre() { return "DocumentoPlantillaDefault"; }

    @Override
    public int size() { return store.size(); }
}