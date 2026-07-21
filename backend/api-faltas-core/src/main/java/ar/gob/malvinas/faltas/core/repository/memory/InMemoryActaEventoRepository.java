package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementacion InMemory de ActaEventoRepository.
 *
 * Garantias:
 * - append-only: registrar() es la unica mutacion permitida.
 * - id Long auto-generado monotonamente creciente (AtomicLong).
 * - buscarPorActa() devuelve eventos ordenados por fhEvt + id (timeline canonico).
 * - existeCorrelacion() soporta idempotencia para eventos externos.
 * - Copia defensiva: el caller no puede mutar el evento guardado.
 * - reset() solo disponible para infraestructura de tests (ResettableInMemoryRepository).
 *
 * Ordenamiento del timeline: fhEvt (principal) + id (desempate para mismo instante).
 */
@Repository
public class InMemoryActaEventoRepository implements ActaEventoRepository, ResettableInMemoryRepository {

    private final Map<Long, List<FalActaEvento>> storeByActa = new ConcurrentHashMap<>();
    private final Map<Long, FalActaEvento> storeById = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1L);

    private static final Comparator<FalActaEvento> TIMELINE_ORDER =
            Comparator.comparing(FalActaEvento::fhEvt)
                      .thenComparingLong(e -> e.getId() != null ? e.getId() : 0L);

    @Override
    public FalActaEvento registrar(FalActaEvento evento) {
        FalActaEvento conId = evento.conId(idSeq.getAndIncrement());
        storeByActa.computeIfAbsent(conId.actaId(), k -> new CopyOnWriteArrayList<>())
                   .add(conId);
        storeById.put(conId.getId(), conId);
        return conId;
    }

    @Override
    public List<FalActaEvento> buscarPorActa(Long idActa) {
        return storeByActa.getOrDefault(idActa, List.of())
                .stream()
                .sorted(TIMELINE_ORDER)
                .toList();
    }

    @Override
    public Optional<FalActaEvento> buscarPorId(Long id) {
        return Optional.ofNullable(storeById.get(id));
    }

    @Override
    public boolean existeCorrelacion(Long idActa, String correlacionId) {
        if (correlacionId == null || correlacionId.isBlank()) return false;
        return storeByActa.getOrDefault(idActa, List.of()).stream()
                .anyMatch(e -> correlacionId.equals(e.correlacionId()));
    }

    @Override
    public void reset() {
        storeByActa.clear();
        storeById.clear();
        idSeq.set(1L);
    }

    @Override
    public String nombre() { return "ActaEvento"; }

    @Override
    public int size() {
        return (int) storeByActa.values().stream().mapToInt(List::size).sum();
    }
}
