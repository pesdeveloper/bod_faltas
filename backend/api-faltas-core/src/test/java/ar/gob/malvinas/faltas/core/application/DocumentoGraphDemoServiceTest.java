package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.demo.PlantillasMockSeeder;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGraphDemoCasoResultado;
import ar.gob.malvinas.faltas.core.application.result.DocumentoGraphDemoResultado;
import ar.gob.malvinas.faltas.core.application.service.DocumentoGeneracionMockService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoGraphDemoService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPdfMockRenderer;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoRedaccionService;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Slice 8F-4: DocumentoGraphDemoService - graph demo documental completo")
class DocumentoGraphDemoServiceTest {

    private DocumentoGraphDemoService graphDemoService;

    @BeforeEach
    void setUp() {
        ActaRepository actaRepo = new InMemoryActaRepository();
        FalloActaRepository falloRepo = new InMemoryFalloActaRepository();
        PagoVoluntarioRepository pagoRepo = new InMemoryPagoVoluntarioRepository();
        DocumentoRepository docRepo = new InMemoryDocumentoRepository();
        DocumentoPlantillaRepository plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        DocumentoPlantillaContenidoRepository contenidoRepo =
                new InMemoryDocumentoPlantillaContenidoRepository();
        DocumentoPlantillaDefaultRepository defaultRepo =
                new InMemoryDocumentoPlantillaDefaultRepository();
        DocumentoRedaccionRepository redaccionRepo = new InMemoryDocumentoRedaccionRepository();

        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);

        DocumentoCombinacionService combinacion =
                new DocumentoCombinacionService(new DocumentoVariableRegistry());
        DocumentoPlantillaDefaultService defaultSvc =
                new DocumentoPlantillaDefaultService(defaultRepo);
        DocumentoRedaccionService redaccionService = new DocumentoRedaccionService(
                docRepo, defaultSvc, contenidoRepo, redaccionRepo, combinacion,
                actaRepo, falloRepo, pagoRepo, FaltasClockTestSupport.FIXED);
        DocumentoGeneracionMockService generacionService = new DocumentoGeneracionMockService(
                redaccionRepo, docRepo, new DocumentoPdfMockRenderer(FaltasClockTestSupport.FIXED), FaltasClockTestSupport.FIXED);

        graphDemoService = new DocumentoGraphDemoService(
                actaRepo, falloRepo, pagoRepo, docRepo, redaccionService, generacionService, FaltasClockTestSupport.FIXED);
    }

    @Test
    @DisplayName("1. Ejecuta los 8 casos y devuelve resultado completo")
    void ejecuta_8_casos_completos() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        assertThat(resultado.totalCasos()).isEqualTo(8);
        assertThat(resultado.casosExitosos()).isEqualTo(8);
        assertThat(resultado.casosFallidos()).isEqualTo(0);
        assertThat(resultado.completo()).isTrue();
        assertThat(resultado.fhEjecucion()).isNotNull();
        assertThat(resultado.casos()).hasSize(8);
    }

    @Test
    @DisplayName("2. Todos los casos quedan en estadoRedaccion CONFIRMADA")
    void todos_los_casos_quedan_confirmados() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.estadoRedaccion())
                        .as("Caso %s debe ser CONFIRMADA", c.codigoCaso())
                        .isEqualTo(EstadoRedaccionDocumento.CONFIRMADA));
    }

    @Test
    @DisplayName("3. Todos los casos tienen storageKey con esquema mock://")
    void todos_tienen_storage_key_mock() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.storageKey())
                        .as("Caso %s debe tener storageKey mock://", c.codigoCaso())
                        .isNotNull()
                        .startsWith("mock://"));
    }

    @Test
    @DisplayName("4. Todos los casos tienen hashDocu con prefijo sha256-mock-")
    void todos_tienen_hash_mock() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.hashDocu())
                        .as("Caso %s debe tener hash sha256-mock-", c.codigoCaso())
                        .isNotNull()
                        .startsWith("sha256-mock-"));
    }

    @Test
    @DisplayName("5. Todos los casos tienen fhGeneracion no null")
    void todos_tienen_fh_generacion() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.fhGeneracion())
                        .as("Caso %s debe tener fhGeneracion", c.codigoCaso())
                        .isNotNull());
    }

    @Test
    @DisplayName("6. Todos los casos son mock=true")
    void todos_son_mock() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.mock())
                        .as("Caso %s debe ser mock=true", c.codigoCaso())
                        .isTrue());
    }

    @Test
    @DisplayName("7. Todos los casos tienen redaccionCompleta=true")
    void todos_con_redaccion_completa() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.redaccionCompleta())
                        .as("Caso %s debe tener redaccionCompleta=true", c.codigoCaso())
                        .isTrue());
    }

    @Test
    @DisplayName("8. Todos los casos tienen actaId, documentoId y redaccionId no nulos")
    void todos_tienen_ids_no_nulos() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c -> {
            assertThat(c.actaId()).as("Caso %s actaId", c.codigoCaso()).isNotNull();
            assertThat(c.documentoId()).as("Caso %s documentoId", c.codigoCaso()).isNotNull();
            assertThat(c.redaccionId()).as("Caso %s redaccionId", c.codigoCaso()).isNotNull();
        });
    }

    @Test
    @DisplayName("9. Los codigos de caso son CASO-01 a CASO-08 en orden")
    void codigos_caso_en_orden() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        List<String> codigos = resultado.casos().stream()
                .map(DocumentoGraphDemoCasoResultado::codigoCaso)
                .toList();

        assertThat(codigos).containsExactly(
                "CASO-01", "CASO-02", "CASO-03", "CASO-04",
                "CASO-05", "CASO-06", "CASO-07", "CASO-08");
    }

    @Test
    @DisplayName("10. Todos los casos tienen exitoso=true y errorMensaje null")
    void todos_exitosos_sin_error() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c -> {
            assertThat(c.exitoso()).as("Caso %s debe ser exitoso", c.codigoCaso()).isTrue();
            assertThat(c.errorMensaje()).as("Caso %s no debe tener errorMensaje", c.codigoCaso()).isNull();
        });
    }

    @Test
    @DisplayName("11. Dos ejecuciones son independientes (actaId distinto)")
    void dos_ejecuciones_son_independientes() {
        DocumentoGraphDemoResultado r1 = graphDemoService.ejecutar();
        DocumentoGraphDemoResultado r2 = graphDemoService.ejecutar();

        Long actaId1 = r1.casos().get(0).actaId();
        Long actaId2 = r2.casos().get(0).actaId();
        assertThat(actaId1).isNotEqualTo(actaId2);
    }

    @Test
    @DisplayName("12. storageKey no apunta a storage real")
    void storage_key_no_apunta_a_storage_real() {
        DocumentoGraphDemoResultado resultado = graphDemoService.ejecutar();

        resultado.casos().forEach(c ->
                assertThat(c.storageKey())
                        .doesNotContain("s3://")
                        .doesNotContain("file://")
                        .doesNotContain("/var/")
                        .startsWith("mock://"));
    }
}