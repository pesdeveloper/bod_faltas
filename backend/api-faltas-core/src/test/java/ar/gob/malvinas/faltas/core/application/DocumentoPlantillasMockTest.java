package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionResultado;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableContextBuilder;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.application.demo.PlantillasMockSeeder;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaContenido;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaDefault;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Micro-slice 8F-2: PlantillasMock - 8 casos operativos")
class DocumentoPlantillasMockTest {

    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoPlantillaContenidoRepository contenidoRepo;
    private DocumentoPlantillaDefaultRepository defaultRepo;
    private DocumentoPlantillaDefaultService defaultService;
    private DocumentoCombinacionService combinacionService;
    private DocumentoVariableRegistry registry;

    @BeforeEach
    void setUp() {
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        contenidoRepo = new InMemoryDocumentoPlantillaContenidoRepository();
        defaultRepo = new InMemoryDocumentoPlantillaDefaultRepository();
        defaultService = new DocumentoPlantillaDefaultService(defaultRepo);
        registry = new DocumentoVariableRegistry();
        combinacionService = new DocumentoCombinacionService(registry);

        PlantillasMockSeeder.seedar(plantillaRepo, contenidoRepo, defaultRepo);
    }

    @Test
    @DisplayName("1. Existen exactamente 8 plantillas mock cargadas")
    void existen_8_plantillas() {
        assertThat(plantillaRepo.listar()).hasSize(8);
    }

    @Test
    @DisplayName("2. Existen exactamente 8 contenidos vigentes")
    void existen_8_contenidos() {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        long count = plantillaRepo.listar().stream()
                .mapToLong(p -> contenidoRepo.buscarContenidoVigente(p.getId(), ahora).size())
                .sum();
        assertThat(count).isEqualTo(8);
    }

    @Test
    @DisplayName("3. Existen exactamente 8 plantilla defaults")
    void existen_8_defaults() {
        assertThat(defaultRepo.listar()).hasSize(8);
    }

    @ParameterizedTest(name = "4. Existe default para accion: {0}")
    @EnumSource(value = AccionDocumental.class, names = {
        "EMITIR_FALLO",
        "EMITIR_NOTIFICACION_ACTA",
        "EMITIR_NOTIFICACION_FALLO",
        "EMITIR_INTIMACION_PAGO",
        "EMITIR_MEDIDA_PREVENTIVA",
        "EMITIR_CONSTANCIA",
        "EMITIR_ANEXO",
        "EMITIR_RESOLUTORIO_BLOQUEANTE"
    })
    void existe_default_para_cada_accion(AccionDocumental accion) {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalDocumentoPlantillaDefault def = defaultService.resolverDefault(accion, null, null, ahora);
        assertThat(def).isNotNull();
        assertThat(def.getAccionDocumental()).isEqualTo(accion);
    }

    @ParameterizedTest(name = "5. Default resuelve sin ambiguedad para: {0}")
    @EnumSource(value = AccionDocumental.class, names = {
        "EMITIR_FALLO",
        "EMITIR_NOTIFICACION_ACTA",
        "EMITIR_NOTIFICACION_FALLO",
        "EMITIR_INTIMACION_PAGO",
        "EMITIR_MEDIDA_PREVENTIVA",
        "EMITIR_CONSTANCIA",
        "EMITIR_ANEXO",
        "EMITIR_RESOLUTORIO_BLOQUEANTE"
    })
    void default_resuelve_sin_ambiguedad(AccionDocumental accion) {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalDocumentoPlantillaDefault def = defaultService.resolverDefault(accion, null, null, ahora);
        assertThat(def.getPlantillaId()).isNotNull();
    }

    @ParameterizedTest(name = "6. Contenido vigente existe para plantilla de: {0}")
    @EnumSource(value = AccionDocumental.class, names = {
        "EMITIR_FALLO",
        "EMITIR_NOTIFICACION_ACTA",
        "EMITIR_NOTIFICACION_FALLO",
        "EMITIR_INTIMACION_PAGO",
        "EMITIR_MEDIDA_PREVENTIVA",
        "EMITIR_CONSTANCIA",
        "EMITIR_ANEXO",
        "EMITIR_RESOLUTORIO_BLOQUEANTE"
    })
    void contenido_vigente_existe(AccionDocumental accion) {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalDocumentoPlantillaDefault def = defaultService.resolverDefault(accion, null, null, ahora);
        List<FalDocumentoPlantillaContenido> contenidos =
                contenidoRepo.buscarContenidoVigente(def.getPlantillaId(), ahora);
        assertThat(contenidos).isNotEmpty();
    }

    @ParameterizedTest(name = "7. Template de {0} solo usa variables registradas")
    @EnumSource(value = AccionDocumental.class, names = {
        "EMITIR_FALLO",
        "EMITIR_NOTIFICACION_ACTA",
        "EMITIR_NOTIFICACION_FALLO",
        "EMITIR_INTIMACION_PAGO",
        "EMITIR_MEDIDA_PREVENTIVA",
        "EMITIR_CONSTANCIA",
        "EMITIR_ANEXO",
        "EMITIR_RESOLUTORIO_BLOQUEANTE"
    })
    void template_usa_solo_variables_registradas(AccionDocumental accion) {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalDocumentoPlantillaDefault def = defaultService.resolverDefault(accion, null, null, ahora);
        FalDocumentoPlantillaContenido contenido =
                contenidoRepo.buscarContenidoVigente(def.getPlantillaId(), ahora).get(0);

        Map<String, Object> contextoVacio = Map.of();
        DocumentoCombinacionResultado resultado =
                combinacionService.combinar(contenido.getCuerpoTemplate(), contextoVacio);

        assertThat(resultado.variablesDesconocidas())
                .as("El template de %s no debe tener variables desconocidas", accion)
                .isEmpty();
    }

    @ParameterizedTest(name = "8. Template de {0} combina correctamente con contexto demo")
    @EnumSource(value = AccionDocumental.class, names = {
        "EMITIR_FALLO",
        "EMITIR_NOTIFICACION_ACTA",
        "EMITIR_NOTIFICACION_FALLO",
        "EMITIR_INTIMACION_PAGO",
        "EMITIR_MEDIDA_PREVENTIVA",
        "EMITIR_CONSTANCIA",
        "EMITIR_ANEXO",
        "EMITIR_RESOLUTORIO_BLOQUEANTE"
    })
    void template_combina_con_contexto_demo(AccionDocumental accion) {
        FalActa acta = GraphDemoActaFactory.crearActaDemo(1L);
        Map<String, Object> contexto = DocumentoVariableContextBuilder.buildDesdeActa(
                acta,
                GraphDemoActaFactory.crearDocumentoDemo(10L, 1L, ar.gob.malvinas.faltas.core.domain.enums.TipoDocu.ACTO_ADMINISTRATIVO, FaltasClockTestSupport.FIXED.now()),
                GraphDemoActaFactory.crearFalloCondenatorioDemo(1L),
                GraphDemoActaFactory.crearPagoVoluntarioDemo(1L),
                null, FaltasClockTestSupport.FIXED.now());

        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalDocumentoPlantillaDefault def = defaultService.resolverDefault(accion, null, null, ahora);
        FalDocumentoPlantillaContenido contenido =
                contenidoRepo.buscarContenidoVigente(def.getPlantillaId(), ahora).get(0);

        DocumentoCombinacionResultado resultado =
                combinacionService.combinar(contenido.getCuerpoTemplate(), contexto);

        assertThat(resultado.variablesDesconocidas())
                .as("No debe haber variables desconocidas en %s", accion)
                .isEmpty();
        assertThat(resultado.variablesFaltantes())
                .as("No debe haber variables requeridas faltantes en %s", accion)
                .isEmpty();
        assertThat(resultado.completo())
                .as("La combinacion de %s debe ser completa", accion)
                .isTrue();
        assertThat(resultado.contenidoCombinado())
                .as("El contenido combinado de %s no debe ser blank", accion)
                .isNotBlank();
    }

    @Test
    @DisplayName("9. Todos los defaults estan activos y vigentes ahora")
    void todos_defaults_activos_y_vigentes() {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        defaultRepo.listar().forEach(d ->
            assertThat(d.vigente(ahora))
                .as("Default %s debe estar vigente", d.getAccionDocumental())
                .isTrue());
    }

    @Test
    @DisplayName("10. No hay dos defaults con igual prioridad para la misma accion")
    void no_hay_ambiguedad_en_defaults() {
        defaultRepo.listar()
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getAccionDocumental().name() + "_" + d.getPrioridad()))
                .forEach((key, lista) ->
                    assertThat(lista)
                        .as("No debe haber duplicados de prioridad para %s", key)
                        .hasSize(1));
    }
}
