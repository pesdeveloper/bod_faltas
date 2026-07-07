package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.ResettableInMemoryRepository;
import ar.gob.malvinas.faltas.core.web.dto.DevResetResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de reset in-memory para entorno dev/test/demo.
 *
 * Responsabilidades:
 *  - Limpiar todos los repositorios in-memory via ResettableInMemoryRepository.
 *  - Resembrar plantillas mock via PlantillasMockSeeder.seedar(...).
 *  - Dejar el sistema en estado conocido, limpio y reproducible.
 *
 * Garantias:
 *  - Idempotente: ejecutar N veces produce el mismo estado.
 *  - No toca JDBC ni MariaDB.
 *  - No escribe archivos.
 *  - No borra datos reales.
 *
 * Slice 8F-5.
 */
@Service
public class DevInMemoryResetService {

    private static final int PLANTILLAS_MOCK = 8;

    private final List<ResettableInMemoryRepository> repositorios;
    private final DocumentoPlantillaRepository plantillaRepo;
    private final DocumentoPlantillaContenidoRepository contenidoRepo;
    private final DocumentoPlantillaDefaultRepository defaultRepo;

    public DevInMemoryResetService(
            List<ResettableInMemoryRepository> repositorios,
            DocumentoPlantillaRepository plantillaRepo,
            DocumentoPlantillaContenidoRepository contenidoRepo,
            DocumentoPlantillaDefaultRepository defaultRepo) {
        this.repositorios = repositorios;
        this.plantillaRepo = plantillaRepo;
        this.contenidoRepo = contenidoRepo;
        this.defaultRepo = defaultRepo;
    }

    /**
     * Ejecuta el reset completo.
     *
     * Secuencia:
     *  1. Limpia todos los repositorios in-memory (clear + reset secuencias).
     *  2. Resembrada de plantillas mock (8 plantillas, 8 contenidos, 8 defaults).
     *  3. Devuelve resumen del reset.
     *
     * El DatasetFuncionalDominioCatalog (37 actas) es estatico y no requiere reset.
     * El graph demo puede ejecutarse inmediatamente despues del reset.
     */
    public DevResetResponse ejecutarReset() {
        List<String> nombresReseteados = new ArrayList<>();
        List<String> acciones = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        for (ResettableInMemoryRepository repo : repositorios) {
            repo.reset();
            nombresReseteados.add(repo.nombre());
            acciones.add("reset: " + repo.nombre());
        }

        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);
        acciones.add("seed: plantillas-mock (" + PLANTILLAS_MOCK + " plantillas, "
                + PLANTILLAS_MOCK + " contenidos, " + PLANTILLAS_MOCK + " defaults)");

        advertencias.add("Las actas no se recrean automaticamente; usar GET /demo/documentos/graph o GET /demo/actas/dataset-funcional.");

        int casosDataset = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().size();

        return new DevResetResponse(
                true,
                "memory",
                LocalDateTime.now(),
                repositorios.size(),
                PLANTILLAS_MOCK,
                0,
                casosDataset,
                0,
                List.copyOf(nombresReseteados),
                List.copyOf(acciones),
                List.copyOf(advertencias)
        );
    }
}
