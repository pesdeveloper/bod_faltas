package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPersonaRepository implements PersonaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalPersona> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalPersona guardar(FalPersona persona) {
        FalPersona copia = persona.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalPersona> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalPersona::copia);
    }

    @Override
    public List<FalPersona> listarTodas() {
        return store.values().stream().map(FalPersona::copia).collect(Collectors.toList());
    }

    @Override
    public List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc) {
        return store.values().stream()
                .filter(p -> tipoDoc == p.getTipoDoc()
                        && nroDoc != null && nroDoc.equals(p.getNroDoc()))
                .map(FalPersona::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalPersona> buscarPorIdSujBie(Long idSuj, Long idBie) {
        return store.values().stream()
                .filter(p -> idSuj != null && idSuj.equals(p.getIdSuj())
                        && idBie != null && idBie.equals(p.getIdBie()))
                .map(FalPersona::copia)
                .collect(Collectors.toList());
    }

    /** Seed: recibe personas ya construidas con IDs estables. Actualiza el contador. */
    public void cargarSeed(List<FalPersona> personas) {
        long maxId = 0;
        for (FalPersona p : personas) {
            store.put(p.getId(), p.copia());
            if (p.getId() > maxId) maxId = p.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "personas"; }

    @Override
    public int size() { return store.size(); }
}
