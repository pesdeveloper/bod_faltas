package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Repositorio InMemory de registros de acceso via QR.
 * Append-only; no hay update ni delete funcional.
 * Orden estable por fhAcceso + id.
 *
 * <p><b>Restringido a {@code !jdbc} desde {@code POST-B2-TX-CONSUMERS-01}.</b> Con el perfil
 * {@code jdbc} el puerto lo resuelve {@code JdbcQrAccesoRepository}. Antes de esta wave este
 * adaptador seguia activo tambien bajo {@code jdbc} (sin {@code @Profile}), de modo que el
 * acceso QR quedaba en memoria mientras el evento {@code QRACCA} y el snapshot de la misma
 * operacion iban a MariaDB: era una mezcla JDBC/InMemory dentro de una unica operacion logica.
 */
@Repository
@Profile("!jdbc")
public class InMemoryQrAccesoRepository
        implements QrAccesoRepository, ResettableInMemoryRepository {

    private final Map<Long, FalActaQrAcceso> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaQrAcceso registrar(FalActaQrAcceso acceso) {
        FalActaQrAcceso copia = acceso.copia();
        store.put(copia.getId(), copia);
        return copia.copia();
    }

    @Override
    public Optional<FalActaQrAcceso> buscarPorId(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalActaQrAcceso::copia);
    }

    @Override
    public List<FalActaQrAcceso> listarPorActa(Long actaId) {
        return store.values().stream()
                .filter(a -> actaId.equals(a.getActaId()))
                .sorted(Comparator.comparing(FalActaQrAcceso::getFhAcceso)
                        .thenComparingLong(FalActaQrAcceso::getId))
                .map(FalActaQrAcceso::copia)
                .collect(Collectors.toList());
    }

    @Override
    public int contarPorActa(Long actaId) {
        return (int) store.values().stream()
                .filter(a -> actaId.equals(a.getActaId()))
                .count();
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "qr-accesos"; }

    @Override
    public int size() { return store.size(); }
}
