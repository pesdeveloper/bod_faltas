package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTarifarioUnidadFaltasRepository
        implements TarifarioUnidadFaltasRepository, ResettableInMemoryRepository {

    private final Map<Long, FalTarifarioUnidadFaltas> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas tarifario) {
        FalTarifarioUnidadFaltas copia = tarifario.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalTarifarioUnidadFaltas::copia);
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findAll() {
        return store.values().stream().map(FalTarifarioUnidadFaltas::copia).collect(Collectors.toList());
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findByTipoUnidad(TipoUnidadFaltas tipoUnidad) {
        return store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad())
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findVigentes(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        return store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad() && t.esVigenteEn(fecha))
                .map(FalTarifarioUnidadFaltas::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findUltimoVigente(TipoUnidadFaltas tipoUnidad, LocalDate fecha) {
        return store.values().stream()
                .filter(t -> tipoUnidad == t.getTipoUnidad() && t.esVigenteEn(fecha))
                .max(Comparator.comparing(FalTarifarioUnidadFaltas::getFhVigDesde))
                .map(FalTarifarioUnidadFaltas::copia);
    }

    public void cargarSeed(List<FalTarifarioUnidadFaltas> lista) {
        long maxId = 0;
        for (FalTarifarioUnidadFaltas t : lista) {
            store.put(t.getId(), t.copia());
            if (t.getId() > maxId) maxId = t.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "tarifarios"; }

    @Override
    public int size() { return store.size(); }
}
