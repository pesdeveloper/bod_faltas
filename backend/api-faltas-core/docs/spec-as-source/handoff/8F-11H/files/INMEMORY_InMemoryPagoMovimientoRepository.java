package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Repositorio append-only para movimientos de pago.
 * Los movimientos no se modifican ni eliminan.
 * Las copias defensivas garantizan inmutabilidad externa.
 */
@Repository
public class InMemoryPagoMovimientoRepository
        implements PagoMovimientoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaPagoMovimiento> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalActaPagoMovimiento append(FalActaPagoMovimiento m) {
        if (store.containsKey(m.getId()))
            throw new MovimientoPagoDuplicadoException("id", m.getId());
        if (m.getReferenciaExterna() != null) {
            Optional<FalActaPagoMovimiento> existente = findByReferenciaExterna(m.getReferenciaExterna());
            if (existente.isPresent()) {
                FalActaPagoMovimiento dup = existente.get();
                if (dup.getTipoMovimiento() == m.getTipoMovimiento()
                        && dup.getObligacionPagoId().equals(m.getObligacionPagoId())) {
                    return dup.copia();
                }
                throw new MovimientoPagoDuplicadoException(m.getReferenciaExterna());
            }
        }
        FalActaPagoMovimiento copia = m.copia();
        store.put(copia.getId(), copia);
        return copia;
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
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByFormaPagoId(Long formaPagoId) {
        return store.values().stream()
                .filter(m -> formaPagoId.equals(m.getFormaPagoId()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByPlanPagoRefId(Long planPagoRefId) {
        return store.values().stream()
                .filter(m -> planPagoRefId.equals(m.getPlanPagoRefId()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
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
    public List<FalActaPagoMovimiento> findByReferenciaEM(String cmteEM, short prefEM, int nroEM) {
        return store.values().stream()
                .filter(m -> cmteEM.equals(m.getCmteEM())
                        && m.getPrefEM() != null && m.getPrefEM() == prefEM
                        && m.getNroEM() != null && m.getNroEM() == nroEM)
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByReferenciaPG(String cmtePG, short prefPG, int nroPG) {
        return store.values().stream()
                .filter(m -> cmtePG.equals(m.getCmtePG())
                        && m.getPrefPG() != null && m.getPrefPG() == prefPG
                        && m.getNroPG() != null && m.getNroPG() == nroPG)
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByIdCierre(Long idCierre) {
        return store.values().stream()
                .filter(m -> idCierre.equals(m.getIdCierre()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaPagoMovimiento> findByIdOpe(Long idOpe) {
        return store.values().stream()
                .filter(m -> idOpe.equals(m.getIdOpe()))
                .map(FalActaPagoMovimiento::copia)
                .collect(Collectors.toList());
    }

    @Override
    public void reset() { store.clear(); idCounter.set(1); }

    @Override
    public String nombre() { return "pago-movimientos"; }

    @Override
    public int size() { return store.size(); }
}
