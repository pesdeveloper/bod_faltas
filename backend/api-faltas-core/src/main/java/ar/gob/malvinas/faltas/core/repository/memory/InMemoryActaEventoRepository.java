package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryActaEventoRepository implements ActaEventoRepository {

    private final Map<Long, List<FalActaEvento>> store = new ConcurrentHashMap<>();

    @Override
    public FalActaEvento registrar(FalActaEvento evento) {
        store.computeIfAbsent(evento.idActa(), k -> new CopyOnWriteArrayList<>())
             .add(evento);
        return evento;
    }

    @Override
    public List<FalActaEvento> buscarPorActa(Long idActa) {
        List<FalActaEvento> eventos = store.getOrDefault(idActa, List.of());
        return eventos.stream()
                .sorted(Comparator.comparingInt(FalActaEvento::ordenLogico))
                .toList();
    }

    @Override
    public int proximoOrdenLogico(Long idActa) {
        return store.getOrDefault(idActa, List.of()).size() + 1;
    }
}

