package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.application.service.RegistroMovimientoOutcome;
import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryPagoMovimientoRepository
        implements PagoMovimientoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaPagoMovimiento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized RegistroMovimientoOutcome append(FalActaPagoMovimiento m) {
        if (store.containsKey(m.getId())) {
            FalActaPagoMovimiento existente = store.get(m.getId());
            if (existente.payloadEquivalenteA(m)) {
                return new RegistroMovimientoOutcome(MovimientoRegistroResult.ALREADY_EXISTS, existente.copia());
            }
            return new RegistroMovimientoOutcome(MovimientoRegistroResult.CONFLICT, existente.copia());
        }
        if (m.getReferenciaExterna() != null) {
            Optional<FalActaPagoMovimiento> porRef = store.values().stream()
                    .filter(x -> m.getOrigenMovimiento() == x.getOrigenMovimiento()
                            && m.getReferenciaExterna().equals(x.getReferenciaExterna()))
                    .findFirst();
            if (porRef.isPresent()) {
                FalActaPagoMovimiento dup = porRef.get();
                if (dup.payloadEquivalenteA(m)) {
                    return new RegistroMovimientoOutcome(MovimientoRegistroResult.ALREADY_EXISTS, dup.copia());
                }
                return new RegistroMovimientoOutcome(MovimientoRegistroResult.CONFLICT, dup.copia());
            }
        }
        FalActaPagoMovimiento copia = m.copia();
        store.put(copia.getId(), copia);
        return new RegistroMovimientoOutcome(MovimientoRegistroResult.CREATED, copia.copia());
    }

    @Override
    public Optional<FalActaPagoMovimiento> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaPagoMovimiento::copia);
    }

    @Override
    public List<FalActaPagoMovimiento> findByObligacionPagoId(Long obligacionPagoId) {
        return store.values().stream()
                .filter(m -> m.getObligacionPagoId().equals(obligacionPagoId))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByFormaPagoId(Long formaPagoId) {
        return store.values().stream()
                .filter(m -> formaPagoId.equals(m.getFormaPagoId()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByPlanPagoRefId(Long planPagoRefId) {
        return store.values().stream()
                .filter(m -> planPagoRefId.equals(m.getPlanPagoRefId()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<FalActaPagoMovimiento> findByReferenciaExterna(String referenciaExterna) {
        if (referenciaExterna == null) return Optional.empty();
        return store.values().stream()
                .filter(m -> referenciaExterna.equals(m.getReferenciaExterna()))
                .findFirst()
                .map(FalActaPagoMovimiento::copia);
    }

    @Override
    public Optional<FalActaPagoMovimiento> findByOrigenAndReferenciaExterna(OrigenMovimiento origen, String referenciaExterna) {
        if (referenciaExterna == null || origen == null) return Optional.empty();
        return store.values().stream()
                .filter(m -> origen == m.getOrigenMovimiento() && referenciaExterna.equals(m.getReferenciaExterna()))
                .findFirst()
                .map(FalActaPagoMovimiento::copia);
    }

    @Override
    public List<FalActaPagoMovimiento> findByReferenciaEM(String cmteEM, short prefEM, int nroEM) {
        return store.values().stream()
                .filter(m -> cmteEM.equals(m.getCmteEM())
                        && m.getPrefEM() != null && m.getPrefEM() == prefEM
                        && m.getNroEM() != null && m.getNroEM() == nroEM)
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByReferenciaPG(String cmtePG, short prefPG, int nroPG) {
        return store.values().stream()
                .filter(m -> cmtePG.equals(m.getCmtePG())
                        && m.getPrefPG() != null && m.getPrefPG() == prefPG
                        && m.getNroPG() != null && m.getNroPG() == nroPG)
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByIdCierre(Long idCierre) {
        return store.values().stream()
                .filter(m -> idCierre.equals(m.getIdCierre()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByIdOpe(Long idOpe) {
        return store.values().stream()
                .filter(m -> idOpe.equals(m.getIdOpe()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void reset() { store.clear(); idCounter.set(1); }

    @Override
    public String nombre() { return "pago-movimientos"; }

    @Override
    public int size() { return store.size(); }
}
