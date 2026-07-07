package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.demo.DevInMemoryResetService;
import ar.gob.malvinas.faltas.core.application.demo.PlantillasMockSeeder;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.ResettableInMemoryRepository;
import ar.gob.malvinas.faltas.core.web.dto.DevResetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario de DevInMemoryResetService.
 *
 * Slice 8F-5.
 */
@DisplayName("DevInMemoryResetServiceTest: reset y resembrado in-memory")
class DevInMemoryResetServiceTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryDocumentoRepository documentoRepo;
    private InMemoryDocumentoRedaccionRepository redaccionRepo;
    private InMemoryNotificacionRepository notificacionRepo;
    private InMemoryDocumentoPlantillaRepository plantillaRepo;
    private InMemoryDocumentoPlantillaContenidoRepository contenidoRepo;
    private InMemoryDocumentoPlantillaDefaultRepository defaultRepo;

    private DevInMemoryResetService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        documentoRepo = new InMemoryDocumentoRepository();
        redaccionRepo = new InMemoryDocumentoRedaccionRepository();
        notificacionRepo = new InMemoryNotificacionRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        contenidoRepo = new InMemoryDocumentoPlantillaContenidoRepository();
        defaultRepo = new InMemoryDocumentoPlantillaDefaultRepository();

        List<ResettableInMemoryRepository> repos = List.of(
                actaRepo, eventoRepo, documentoRepo, redaccionRepo,
                notificacionRepo, plantillaRepo, contenidoRepo, defaultRepo);

        service = new DevInMemoryResetService(repos, plantillaRepo, contenidoRepo, defaultRepo);
    }

    @Test
    @DisplayName("1. reset limpia repositorios con datos previos")
    void reset_limpia_repositorios() {
        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);
        assertThat(plantillaRepo.size()).isGreaterThan(0);

        DevResetResponse resp = service.ejecutarReset();

        assertThat(resp.ejecutado()).isTrue();
        assertThat(resp.repositoriosReseteados()).isEqualTo(8);
        assertThat(resp.errores()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. reset recrea las 8 plantillas mock")
    void reset_recrea_plantillas() {
        DevResetResponse resp = service.ejecutarReset();

        assertThat(plantillaRepo.listar()).hasSize(8);
        assertThat(resp.plantillasRecreadas()).isEqualTo(8);
    }

    @Test
    @DisplayName("3. reset es idempotente: ejecutar 3 veces produce el mismo estado")
    void reset_es_idempotente() {
        service.ejecutarReset();
        service.ejecutarReset();
        DevResetResponse tercero = service.ejecutarReset();

        assertThat(tercero.ejecutado()).isTrue();
        assertThat(tercero.errores()).isEqualTo(0);
        assertThat(plantillaRepo.listar()).hasSize(8);
    }

    @Test
    @DisplayName("4. no quedan documentos viejos despues del reset")
    void no_quedan_documentos_viejos() {
        service.ejecutarReset();
        assertThat(documentoRepo.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("5. no quedan redacciones viejas despues del reset")
    void no_quedan_redacciones_viejas() {
        service.ejecutarReset();
        assertThat(redaccionRepo.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("6. no quedan notificaciones viejas despues del reset")
    void no_quedan_notificaciones_viejas() {
        service.ejecutarReset();
        assertThat(notificacionRepo.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("7. no quedan eventos viejos despues del reset")
    void no_quedan_eventos_viejos() {
        service.ejecutarReset();
        assertThat(eventoRepo.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("8. el dataset funcional devuelve 37 actas post-reset (catalogo estatico)")
    void dataset_funcional_post_reset() {
        service.ejecutarReset();
        assertThat(DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones()).hasSize(37);
    }

    @Test
    @DisplayName("9. el response informa la lista de repositorios reseteados")
    void response_informa_repositorios() {
        DevResetResponse resp = service.ejecutarReset();
        assertThat(resp.repositorios()).isNotEmpty();
        assertThat(resp.repositorios()).contains("actas");
        assertThat(resp.repositorios()).contains("documentos");
        assertThat(resp.repositorios()).contains("plantillas");
    }

    @Test
    @DisplayName("10. el response informa acciones realizadas con seed")
    void response_informa_acciones() {
        DevResetResponse resp = service.ejecutarReset();
        assertThat(resp.acciones()).isNotEmpty();
        assertThat(resp.acciones()).anyMatch(a -> a.startsWith("seed:"));
    }

    @Test
    @DisplayName("11. casosDatasetFuncional es 37")
    void casos_dataset_funcional_es_37() {
        DevResetResponse resp = service.ejecutarReset();
        assertThat(resp.casosDatasetFuncional()).isEqualTo(37);
    }

    @Test
    @DisplayName("12. modo es 'memory'")
    void modo_es_memory() {
        DevResetResponse resp = service.ejecutarReset();
        assertThat(resp.modo()).isEqualTo("memory");
    }

    @Test
    @DisplayName("13. fhReset esta presente")
    void fh_reset_presente() {
        DevResetResponse resp = service.ejecutarReset();
        assertThat(resp.fhReset()).isNotNull();
    }
}
