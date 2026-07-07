package ar.gob.malvinas.faltas.core.repository.memory;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryMedidaPreventivaRepository
        implements MedidaPreventivaRepository, ResettableInMemoryRepository {

    private final Map<Long, FalMedidaPreventiva> store = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long nextId() {
        return idCounter.getAndIncrement();
    }

    /**
     * Guarda con validacion atomica de UK (codigo, versionMedida).
     */
    @Override
    public synchronized FalMedidaPreventiva save(FalMedidaPreventiva medida) {
        boolean duplicado = store.values().stream()
                .anyMatch(m -> !m.getId().equals(medida.getId())
                        && medida.getCodigo().equals(m.getCodigo())
                        && medida.getVersionMedida() == m.getVersionMedida());
        if (duplicado) {
            throw new PrecondicionVioladaException(
                    "Ya existe medida con codigo=" + medida.getCodigo()
                            + " version=" + medida.getVersionMedida());
        }
        FalMedidaPreventiva copia = medida.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    @Override
    public Optional<FalMedidaPreventiva> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(FalMedidaPreventiva::copia);
    }

    @Override
    public Optional<FalMedidaPreventiva> findByCodigoAndVersion(String codigo, short version) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo())
                        && m.getVersionMedida() == version)
                .map(FalMedidaPreventiva::copia)
                .findFirst();
    }

    @Override
    public Optional<FalMedidaPreventiva> findActivaByCodigo(String codigo) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        List<FalMedidaPreventiva> activas = store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo()) && m.isSiActiva())
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
        if (activas.size() > 1) {
            throw new IllegalStateException(
                    "Invariante rota: " + activas.size()
                            + " versiones activas para codigo=" + codigoNorm);
        }
        return activas.isEmpty() ? Optional.empty() : Optional.of(activas.get(0));
    }

    @Override
    public List<FalMedidaPreventiva> findVersionesByCodigo(String codigo) {
        String codigoNorm = codigo == null ? null : codigo.trim().toUpperCase();
        return store.values().stream()
                .filter(m -> codigoNorm != null && codigoNorm.equals(m.getCodigo()))
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalMedidaPreventiva> findActivas() {
        return store.values().stream()
                .filter(FalMedidaPreventiva::isSiActiva)
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    @Override
    public List<FalMedidaPreventiva> findActivasParaDependencia(Long idDep, Short verDep) {
        return store.values().stream()
                .filter(m -> m.isSiActiva()
                        && idDep.equals(m.getIdDep())
                        && verDep.equals(m.getVerDep()))
                .map(FalMedidaPreventiva::copia)
                .collect(Collectors.toList());
    }

    /**
     * Operacion atomica:
     *   1. Verifica que el codigo existe (falla si no).
     *   2. Verifica que la version no existe ya (previene concurrencia).
     *   3. Desactiva la version activa anterior si existe.
     *   4. Guarda la nueva version activa.
     */
    @Override
    public synchronized FalMedidaPreventiva crearNuevaVersionAtomico(FalMedidaPreventiva nuevaVersion) {
        String codigo = nuevaVersion.getCodigo();
        short version = nuevaVersion.getVersionMedida();

        List<FalMedidaPreventiva> versiones = store.values().stream()
                .filter(m -> codigo.equals(m.getCodigo()))
                .collect(Collectors.toList());

        if (versiones.isEmpty()) {
            throw new PrecondicionVioladaException(
                    "No existe medida con codigo=" + codigo + ". Usar crearPrimeraVersion.");
        }

        boolean yaExiste = versiones.stream().anyMatch(m -> m.getVersionMedida() == version);
        if (yaExiste) {
            throw new PrecondicionVioladaException(
                    "Ya existe version=" + version + " para codigo=" + codigo
                            + ". Posible alta concurrente.");
        }

        // Desactivar version activa anterior
        versiones.stream()
                .filter(FalMedidaPreventiva::isSiActiva)
                .forEach(m -> {
                    FalMedidaPreventiva copia = m.copia();
                    copia.setSiActiva(false);
                    store.put(copia.getId(), copia);
                });

        // Guardar nueva version
        FalMedidaPreventiva copia = nuevaVersion.copia();
        store.put(copia.getId(), copia);
        return copia;
    }

    public void cargarSeed(List<FalMedidaPreventiva> lista) {
        long maxId = 0;
        for (FalMedidaPreventiva m : lista) {
            store.put(m.getId(), m.copia());
            if (m.getId() > maxId) maxId = m.getId();
        }
        idCounter.set(maxId + 1);
    }

    @Override
    public void reset() {
        store.clear();
        idCounter.set(1);
    }

    @Override
    public String nombre() { return "medidas-preventivas"; }

    @Override
    public int size() { return store.size(); }
}
