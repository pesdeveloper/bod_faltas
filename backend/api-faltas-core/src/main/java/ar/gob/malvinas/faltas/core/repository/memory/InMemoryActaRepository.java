package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryActaRepository implements ActaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActa> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActa guardar(FalActa acta) {
        Long id = acta.getId();
        // compute() garantiza atomicidad del CAS: lectura, comparacion y escritura
        // ocurren en una unica seccion critica por clave.
        // Equivalente InMemory del: UPDATE fal_acta SET ... WHERE id=? AND version_row=?
        FalActa[] resultado = new FalActa[1];
        store.compute(id, (clave, existente) -> {
            if (existente == null) {
                FalActa copia = acta.copia();
                copia.setVersionRow(0);
                resultado[0] = copia;
                return copia;
            }
            if (existente.getVersionRow() != acta.getVersionRow()) {
                throw new ConcurrenciaConflictoException("FalActa", id,
                        existente.getVersionRow(), acta.getVersionRow());
            }
            FalActa copia = acta.copia();
            copia.setVersionRow(existente.getVersionRow() + 1);
            resultado[0] = copia;
            return copia;
        });
        return resultado[0];
    }

    @Override
    public Optional<FalActa> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActa::copia);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico) {
        if (uuidTecnico == null) return Optional.empty();
        return store.values().stream()
                .filter(a -> uuidTecnico.equals(a.getUuidTecnico()))
                .map(FalActa::copia)
                .findFirst();
    }

    @Override
    public List<FalActa> listarTodas() {
        return store.values().stream().map(FalActa::copia).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "actas"; }

    @Override
    public int size() { return store.size(); }
}