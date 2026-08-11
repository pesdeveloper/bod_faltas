package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Adaptador InMemory de {@code ActaEvidenciaRepository}.
 *
 * <p><b>Restringido a {@code !jdbc} desde WAVE 1A2-B2.</b> Con el perfil {@code jdbc} el
 * puerto lo resuelve {@code JdbcActaEvidenciaRepository}. Antes de B2 este adaptador seguia
 * activo tambien bajo {@code jdbc}, de modo que las evidencias del alta quedaban en memoria y
 * sobrevivian a un rollback de MariaDB: era precisamente una escritura observable fuera de la
 * transaccion.
 *
 * <p>Wave 4 Parte 3D: agrega los metodos de fotografia del puerto ampliado
 * (DECISION_DDL-EVID-03). No implementa locking real (no hay motor detras);
 * la exclusion mutua real de {@code maxOrdenFotografiasPorActa} bajo
 * concurrencia solo esta garantizada contra MariaDB real
 * ({@code JdbcActaEvidenciaRepository} + bloqueo de fila del acta en el
 * llamador), consistente con el resto de los adapters InMemory del proyecto.
 */
@Repository
@Profile("!jdbc")
public class InMemoryActaEvidenciaRepository implements ActaEvidenciaRepository {

    private final Map<Long, FalActaEvidencia> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    @Override
    public FalActaEvidencia guardar(FalActaEvidencia evidencia) {
        store.put(evidencia.getId(), evidencia);
        return evidencia;
    }

    @Override
    public List<FalActaEvidencia> listarPorActa(Long idActa) {
        return store.values().stream()
                .filter(e -> e.getIdActa().equals(idActa))
                .collect(Collectors.toList());
    }

    @Override
    public List<FalActaEvidencia> listarFotografiasPorActa(Long idActa) {
        return store.values().stream()
                .filter(e -> e.getIdActa().equals(idActa) && e.esFotografia())
                .sorted(Comparator.comparing(FalActaEvidencia::getOrden))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FalActaEvidencia> buscarFotografiaPorActaYId(Long idActa, Long idEvidencia) {
        return store.values().stream()
                .filter(e -> e.esFotografia()
                        && Objects.equals(e.getIdActa(), idActa)
                        && Objects.equals(e.getId(), idEvidencia))
                .findFirst();
    }

    @Override
    public int maxOrdenFotografiasPorActa(Long idActa) {
        return store.values().stream()
                .filter(e -> e.getIdActa().equals(idActa) && e.esFotografia())
                .mapToInt(FalActaEvidencia::getOrden)
                .max()
                .orElse(0);
    }

    @Override
    public boolean existeFotografiaConHash(Long idActa, String sha256Hex) {
        if (sha256Hex == null) {
            return false;
        }
        return store.values().stream()
                .anyMatch(e -> e.esFotografia()
                        && e.getIdActa().equals(idActa)
                        && sha256Hex.equalsIgnoreCase(e.getHashEvid()));
    }
}
