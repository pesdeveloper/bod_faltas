package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion in-memory de DiaNoComputableRepository.
 *
 * Garantias:
 *  - Exactamente un registro activo por fecha (atomicidad via ConcurrentHashMap.compute).
 *  - Copias defensivas en entrada y salida.
 *  - Reset limpia registros, indices y reinicia la secuencia.
 */
@Repository
public class InMemoryDiaNoComputableRepository
        implements DiaNoComputableRepository, ResettableInMemoryRepository {

    private final Map<Long, FalDiaNoComputable> store = new ConcurrentHashMap<>();
    private final Map<LocalDate, FalDiaNoComputable> activeByDate = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public synchronized FalDiaNoComputable guardar(FalDiaNoComputable dia) {
        FalDiaNoComputable copia = dia.copia();
        store.put(copia.getId(), copia);
        if (copia.isSiActivo()) {
            activeByDate.put(copia.getFecha(), copia);
        } else {
            activeByDate.remove(copia.getFecha(), copia);
            FalDiaNoComputable enStore = store.get(copia.getId());
            if (enStore != null && !enStore.isSiActivo()) {
                activeByDate.computeIfPresent(copia.getFecha(), (k, v) -> {
                    if (v.getId().equals(copia.getId())) return null;
                    return v;
                });
            }
        }
        return store.get(copia.getId()).copia();
    }

    @Override
    public FalDiaNoComputable guardarActivoSiAusentePorFecha(FalDiaNoComputable candidato) {
        FalDiaNoComputable copiaCandidato = candidato.copia();
        FalDiaNoComputable[] resultado = new FalDiaNoComputable[1];

        activeByDate.compute(copiaCandidato.getFecha(), (fecha, existing) -> {
            if (existing != null) {
                resultado[0] = existing;
                return existing;
            }
            store.put(copiaCandidato.getId(), copiaCandidato);
            resultado[0] = copiaCandidato;
            return copiaCandidato;
        });

        return resultado[0].copia();
    }

    @Override
    public Optional<FalDiaNoComputable> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalDiaNoComputable::copia);
    }

    @Override
    public Optional<FalDiaNoComputable> buscarActivoPorFecha(LocalDate fecha) {
        return Optional.ofNullable(activeByDate.get(fecha)).map(FalDiaNoComputable::copia);
    }

    @Override
    public Optional<FalDiaNoComputable> buscarPorOrigenYReferenciaExterna(
            OrigenDiaNoComputable origen,
            String referenciaExterna) {
        if (origen == null || referenciaExterna == null || referenciaExterna.isBlank())
            return Optional.empty();
        String refNorm = referenciaExterna.trim();
        return store.values().stream()
                .filter(d -> d.getOrigen() == origen
                        && refNorm.equals(d.getReferenciaExterna()))
                .findFirst()
                .map(FalDiaNoComputable::copia);
    }

    @Override
    public List<FalDiaNoComputable> listarActivosOrdenados() {
        return store.values().stream()
                .filter(FalDiaNoComputable::isSiActivo)
                .sorted(Comparator.comparing(FalDiaNoComputable::getFecha)
                        .thenComparing(FalDiaNoComputable::getId))
                .map(FalDiaNoComputable::copia)
                .toList();
    }

    @Override
    public void reset() {
        store.clear();
        activeByDate.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "dias-no-computables"; }

    @Override
    public int size() { return store.size(); }
}
