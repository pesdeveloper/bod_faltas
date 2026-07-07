package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryFormaPagoRepository
        implements FormaPagoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaFormaPago> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() { return idCounter.getAndIncrement(); }

    @Override
    public synchronized FalActaFormaPago save(FalActaFormaPago f) {
        FalActaFormaPago existing = store.get(f.getId());
        if (existing == null) {
            if (f.isSiVigente()) {
                boolean otraVigente = store.values().stream()
                        .anyMatch(v -> v.getObligacionPagoId().equals(f.getObligacionPagoId()) && v.isSiVigente());
                if (otraVigente)
                    throw new PrecondicionVioladaException(
                            "Ya existe una forma vigente para obligacionPagoId=" + f.getObligacionPagoId()
                                    + ". Usar reemplazarVigenteAtomico.");
            }
            boolean nroDuplicado = store.values().stream()
                    .anyMatch(v -> v.getObligacionPagoId().equals(f.getObligacionPagoId())
                            && v.getNroForma() == f.getNroForma());
            if (nroDuplicado)
                throw new PrecondicionVioladaException(
                        "nroForma=" + f.getNroForma() + " ya existe para obligacionPagoId=" + f.getObligacionPagoId());
            FalActaFormaPago copia = f.copia();
            copia.setVersionRow(0);
            store.put(copia.getId(), copia);
            return copia;
        }
        if (existing.getVersionRow() != f.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaFormaPago", f.getId(),
                    existing.getVersionRow(), f.getVersionRow());
        FalActaFormaPago copia = f.copia();
        copia.setVersionRow(existing.getVersionRow() + 1);
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalActaFormaPago> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaFormaPago::copia);
    }

    @Override
    public List<FalActaFormaPago> findByObligacionPagoId(Long obligacionPagoId) {
        return store.values().stream()
                .filter(f -> f.getObligacionPagoId().equals(obligacionPagoId))
                .map(FalActaFormaPago::copia)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaFormaPago> findVigenteByObligacionPagoId(Long obligacionPagoId) {
        return store.values().stream()
                .filter(f -> f.getObligacionPagoId().equals(obligacionPagoId) && f.isSiVigente())
                .findFirst()
                .map(FalActaFormaPago::copia);
    }

    @Override
    public Optional<FalActaFormaPago> findByObligacionPagoIdAndNroForma(Long obligacionPagoId, short nroForma) {
        return store.values().stream()
                .filter(f -> f.getObligacionPagoId().equals(obligacionPagoId) && f.getNroForma() == nroForma)
                .findFirst()
                .map(FalActaFormaPago::copia);
    }

    @Override
    public List<FalActaFormaPago> findByReferenciaEM(String cmteEM, short prefEM, int nroEM) {
        return store.values().stream()
                .filter(f -> cmteEM.equals(f.getCmteEM())
                        && f.getPrefEM() != null && f.getPrefEM() == prefEM
                        && f.getNroEM() != null && f.getNroEM() == nroEM)
                .map(FalActaFormaPago::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaFormaPago> findByReferenciaPG(String cmtePG, short prefPG, int nroPG) {
        return store.values().stream()
                .filter(f -> cmtePG.equals(f.getCmtePG())
                        && f.getPrefPG() != null && f.getPrefPG() == prefPG
                        && f.getNroPG() != null && f.getNroPG() == nroPG)
                .map(FalActaFormaPago::copia)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized FalActaFormaPago reemplazarVigenteAtomico(
            FalActaFormaPago nueva, FalActaFormaPago anteriorVigente) {
        FalActaFormaPago existing = store.get(anteriorVigente.getId());
        if (existing == null)
            throw new PrecondicionVioladaException("Forma anterior no encontrada: " + anteriorVigente.getId());
        if (existing.getVersionRow() != anteriorVigente.getVersionRow())
            throw new ConcurrenciaConflictoException("FalActaFormaPago (anterior)", anteriorVigente.getId(),
                    existing.getVersionRow(), anteriorVigente.getVersionRow());
        FalActaFormaPago copiaAnterior = anteriorVigente.copia();
        copiaAnterior.setSiVigente(false);
        copiaAnterior.setVersionRow(existing.getVersionRow() + 1);
        store.put(copiaAnterior.getId(), copiaAnterior);

        boolean nroDuplicado = store.values().stream()
                .filter(v -> !v.getId().equals(nueva.getId()))
                .anyMatch(v -> v.getObligacionPagoId().equals(nueva.getObligacionPagoId())
                        && v.getNroForma() == nueva.getNroForma());
        if (nroDuplicado)
            throw new PrecondicionVioladaException("nroForma=" + nueva.getNroForma() + " ya existe.");
        FalActaFormaPago copiaNueva = nueva.copia();
        copiaNueva.setVersionRow(0);
        store.put(copiaNueva.getId(), copiaNueva);
        return copiaNueva;
    }

    @Override
    public void reset() { store.clear(); idCounter.set(1); }

    @Override
    public String nombre() { return "formas-pago"; }

    @Override
    public int size() { return store.size(); }
}
