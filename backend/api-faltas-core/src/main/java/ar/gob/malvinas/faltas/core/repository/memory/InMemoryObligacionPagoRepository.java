package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryObligacionPagoRepository
        implements ObligacionPagoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaObligacionPago> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalActaObligacionPago save(FalActaObligacionPago o) {
        FalActaObligacionPago existing = store.get(o.getId());
        if (existing == null) {
            if (o.isSiVigente()) {
                boolean otraVigente = store.values().stream()
                        .anyMatch(v -> v.getActaId().equals(o.getActaId()) && v.isSiVigente());
                if (otraVigente)
                    throw new PrecondicionVioladaException(
                            "Ya existe una obligacion vigente para actaId=" + o.getActaId()
                                    + ". Usar crearVigenteAtomico.");
            }
            FalActaObligacionPago copia = o.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existing.getVersionRow() != o.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaObligacionPago", o.getId(),
                    existing.getVersionRow(), o.getVersionRow());
        FalActaObligacionPago copia = o.copia();
        copia.setVersionRow(existing.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaObligacionPago> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaObligacionPago::copia);
    }

    @Override
    public List<FalActaObligacionPago> findByActaId(Long actaId) {
        return store.values().stream()
                .filter(o -> o.getActaId().equals(actaId))
                .map(FalActaObligacionPago::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaObligacionPago> findAllVigentes() {
        return store.values().stream()
                .filter(FalActaObligacionPago::isSiVigente)
                .map(FalActaObligacionPago::copia)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Optional<FalActaObligacionPago> findVigenteByActaId(Long actaId) {
        return store.values().stream()
                .filter(o -> o.getActaId().equals(actaId) && o.isSiVigente())
                .findFirst()
                .map(FalActaObligacionPago::copia);
    }

    @Override
    public synchronized FalActaObligacionPago crearVigenteAtomico(
            FalActaObligacionPago nueva, FalActaObligacionPago anteriorONull) {
        if (anteriorONull != null) {
            FalActaObligacionPago existing = store.get(anteriorONull.getId());
            if (existing == null)
                throw new PrecondicionVioladaException("Obligacion anterior no encontrada: " + anteriorONull.getId());
            if (existing.getVersionRow() != anteriorONull.getVersionRow())
                throw new ConcurrenciaConflictoException("FalActaObligacionPago (anterior)", anteriorONull.getId(),
                        existing.getVersionRow(), anteriorONull.getVersionRow());
            FalActaObligacionPago copiaAnterior = anteriorONull.copia();
            copiaAnterior.setSiVigente(false);
            copiaAnterior.setVersionRow(existing.getVersionRow() + 1);
            store.put(copiaAnterior.getId(), copiaAnterior);
        }
        FalActaObligacionPago copiaNueva = nueva.copia();
        copiaNueva.setVersionRow(0);
        copiaNueva.setSiVigente(true);
        store.put(copiaNueva.getId(), copiaNueva);
        return copiaNueva;
    }

    @Override
    public void reset() { store.clear(); idCounter.set(1); }

    @Override
    public String nombre() { return "obligaciones-pago"; }

    @Override
    public int size() { return store.size(); }
}
