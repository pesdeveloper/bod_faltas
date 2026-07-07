package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPlanPagoRefRepository
        implements PlanPagoRefRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaPlanPagoRef> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalActaPlanPagoRef save(FalActaPlanPagoRef p) {
        FalActaPlanPagoRef existing = store.get(p.getId());
        if (existing == null) {
            boolean parDuplicado = store.values().stream()
                    .anyMatch(v -> v.getIdTdocPlan() == p.getIdTdocPlan()
                            && v.getIdDocPlan() == p.getIdDocPlan());
            if (parDuplicado)
                throw new PrecondicionVioladaException(
                        "Par (idTdocPlan=" + p.getIdTdocPlan() + ",idDocPlan=" + p.getIdDocPlan() + ") ya existe.");
            if (p.isSiVigente()) {
                boolean otraVigente = store.values().stream()
                        .anyMatch(v -> v.getObligacionPagoId().equals(p.getObligacionPagoId()) && v.isSiVigente());
                if (otraVigente)
                    throw new PrecondicionVioladaException(
                            "Ya existe un plan vigente para obligacionPagoId=" + p.getObligacionPagoId()
                                    + ". Usar refinanciarAtomico.");
            }
            FalActaPlanPagoRef copia = p.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existing.getVersionRow() != p.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaPlanPagoRef", p.getId(),
                    existing.getVersionRow(), p.getVersionRow());
        FalActaPlanPagoRef copia = p.copia();
        copia.setVersionRow(existing.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaPlanPagoRef> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaPlanPagoRef::copia);
    }

    @Override
    public List<FalActaPlanPagoRef> findByObligacionPagoId(Long obligacionPagoId) {
        return store.values().stream()
                .filter(p -> p.getObligacionPagoId().equals(obligacionPagoId))
                .map(FalActaPlanPagoRef::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPlanPagoRef> findByFormaPagoId(Long formaPagoId) {
        return store.values().stream()
                .filter(p -> p.getFormaPagoId().equals(formaPagoId))
                .map(FalActaPlanPagoRef::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaPlanPagoRef> findVigenteByObligacionPagoId(Long obligacionPagoId) {
        return store.values().stream()
                .filter(p -> p.getObligacionPagoId().equals(obligacionPagoId) && p.isSiVigente())
                .findFirst()
                .map(FalActaPlanPagoRef::copia);
    }

    @Override
    public Optional<FalActaPlanPagoRef> findByIdTdocPlanAndIdDocPlan(short idTdocPlan, long idDocPlan) {
        return store.values().stream()
                .filter(p -> p.getIdTdocPlan() == idTdocPlan && p.getIdDocPlan() == idDocPlan)
                .findFirst()
                .map(FalActaPlanPagoRef::copia);
    }

    @Override
    public synchronized FalActaPlanPagoRef refinanciarAtomico(
            FalActaPlanPagoRef nuevo, FalActaPlanPagoRef anteriorVigente) {
        FalActaPlanPagoRef existing = store.get(anteriorVigente.getId());
        if (existing == null)
            throw new PrecondicionVioladaException("Plan anterior no encontrado: " + anteriorVigente.getId());
        if (existing.getVersionRow() != anteriorVigente.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaPlanPagoRef (anterior)", anteriorVigente.getId(),
                    existing.getVersionRow(), anteriorVigente.getVersionRow());
        FalActaPlanPagoRef copiaAnterior = anteriorVigente.copia();
        copiaAnterior.setSiVigente(false);
        copiaAnterior.setEstadoPlan(EstadoPlanPago.REFINANCIADO);
        copiaAnterior.setVersionRow(existing.getVersionRow() + 1);
        store.put(copiaAnterior.getId(), copiaAnterior);

        FalActaPlanPagoRef copiaNuevo = nuevo.copia();
        copiaNuevo.setVersionRow(0);
        copiaNuevo.setSiVigente(true);
        store.put(copiaNuevo.getId(), copiaNuevo);
        return copiaNuevo;
    }

    @Override
    public void reset() { store.clear(); idCounter.set(1); }

    @Override
    public String nombre() { return "planes-pago-ref"; }

    @Override
    public int size() { return store.size(); }
}
