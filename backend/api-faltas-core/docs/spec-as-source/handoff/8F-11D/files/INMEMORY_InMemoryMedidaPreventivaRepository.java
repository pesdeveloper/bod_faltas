package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryMedidaPreventivaRepository
        implements MedidaPreventivaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalMedidaPreventiva> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalMedidaPreventiva save(FalMedidaPreventiva medida) {
        FalMedidaPreventiva copia = medida.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalMedidaPreventiva> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalMedidaPreventiva::copia);
    }

    @Override
    public Optional<FalMedidaPreventiva> findByCodigoAndVersion(String codigo, short version) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo())
                        && m.getVersionMedida() == version)
                .map(FalMedidaPreventiva::copia)
                .findFirst();
    }

    @Override
    public Optional<FalMedidaPreventiva> findActivaByCodigo(String codigo) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo()) && m.isSiActiva())
                .map(FalMedidaPreventiva::copia)
                .findFirst();
    }

    @Override
    public List<FalMedidaPreventiva> findVersionesByCodigo(String codigo) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo()))
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalMedidaPreventiva> findActivas() {
        return store.values().stream()
                .filter(FalMedidaPreventiva::isSiActiva)
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalMedidaPreventiva> findActivasParaDependencia(Long idDep, Short verDep) {
        return store.values().stream()
                .filter(m -> m.isSiActiva()
                        && idDep.equals(m.getIdDep())
                        && verDep.equals(m.getVerDep()))
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    public void cargarSeed(List<FalMedidaPreventiva> lista) {
        long maxId = 0;
        for (FalMedidaPreventiva m : lista) {
            store.put(m.getId(), m.copia());
            if (m.getId() > maxId) maxId = m.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "medidas-preventivas"; }

    @Override
    public int size() { return store.size(); }
}
