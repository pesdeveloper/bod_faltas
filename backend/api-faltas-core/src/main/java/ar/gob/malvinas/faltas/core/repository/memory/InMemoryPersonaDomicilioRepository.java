package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPersonaDomicilioRepository implements PersonaDomicilioRepository, ResettableInMemoryRepository {

    private final Map<Long, FalPersonaDomicilio> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final Object principalLock = new Object();

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalPersonaDomicilio guardar(FalPersonaDomicilio domicilio) {
        FalPersonaDomicilio copia = domicilio.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalPersonaDomicilio> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalPersonaDomicilio::copia);
    }

    @Override
    public List<FalPersonaDomicilio> buscarPorPersonaId(Long personaId) {
        return store.values().stream()
                .filter(d -> personaId.equals(d.getPersonaId()))
                .map(FalPersonaDomicilio::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalPersonaDomicilio> buscarActivosPorPersonaId(Long personaId) {
        return store.values().stream()
                .filter(d -> personaId.equals(d.getPersonaId()) && d.isSiActivo())
                .map(FalPersonaDomicilio::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalPersonaDomicilio> buscarNotificablesPorPersonaId(Long personaId) {
        return store.values().stream()
                .filter(d -> personaId.equals(d.getPersonaId()) && d.isSiActivo() && d.isSiNotificable())
                .map(FalPersonaDomicilio::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalPersonaDomicilio> buscarPrincipalActivo(Long personaId, TipoDomicilio tipoDomicilio) {
        return store.values().stream()
                .filter(d -> personaId.equals(d.getPersonaId())
                        && tipoDomicilio == d.getTipoDomicilio()
                        && d.isSiActivo()
                        && d.isSiPrincipal())
                .map(FalPersonaDomicilio::copia)
                .findFirst();
    }

    /**
     * Marca un domicilio como principal de forma atomica.
     * Desmarca el principal anterior de la misma persona y tipo.
     * Rechaza domicilios inactivos.
     */
    public void marcarPrincipal(Long domicilioId) {
        synchronized (principalLock) {
            FalPersonaDomicilio dom = store.get(domicilioId);
            if (dom == null) throw new IllegalArgumentException("Domicilio no encontrado: " + domicilioId);
            if (!dom.isSiActivo()) throw new IllegalStateException("No se puede marcar principal un domicilio inactivo: " + domicilioId);

            store.values().stream()
                    .filter(d -> dom.getPersonaId().equals(d.getPersonaId())
                            && dom.getTipoDomicilio() == d.getTipoDomicilio()
                            && d.isSiPrincipal()
                            && !d.getId().equals(domicilioId))
                    .forEach(d -> {
                        d.setSiPrincipal(false);
                        store.put(d.getId(), d);
                    });

            dom.setSiPrincipal(true);
            store.put(domicilioId, dom);
        }
    }

    /** Seed: carga domicilios con IDs estables y actualiza el contador. */
    public void cargarSeed(List<FalPersonaDomicilio> domicilios) {
        long maxId = 0;
        for (FalPersonaDomicilio d : domicilios) {
            store.put(d.getId(), d.copia());
            if (d.getId() > maxId) maxId = d.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "domicilios-persona"; }

    @Override
    public int size() { return store.size(); }
}
