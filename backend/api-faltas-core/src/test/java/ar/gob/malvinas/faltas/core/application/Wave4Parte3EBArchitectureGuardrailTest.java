package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails arquitectónicos de Wave 4 Parte 3E-B (prompt §11): certifican por lectura directa
 * del árbol fuente real (no por confiar en un informe anterior) que el retiro físico de
 * InMemory del alta, del runner de demo y del modo memoria productivo no vuelve a
 * reintroducirse silenciosamente.
 *
 * <p>Los items del §11 que requieren un contexto Spring real (un puerto del alta sin adapter
 * JDBC bajo {@code jdbc}, o con un bean InMemory activo bajo {@code jdbc}) están en
 * {@code JdbcOnlySieteAdaptersGuardrailIT}, no aquí: este archivo es deliberadamente Java plano
 * (sin Spring), para que corra en milisegundos como parte de la suite unitaria.
 */
@DisplayName("Wave 4 Parte 3E-B §11: guardrails arquitectonicos JDBC-only")
class Wave4Parte3EBArchitectureGuardrailTest {

    private static final Path SRC_MAIN_JAVA = Paths.get(
            "src/main/java/ar/gob/malvinas/faltas/core");
    private static final Path APPLICATION_YML = Paths.get(
            "src/main/resources/application.yml");

    private static final List<String> SIETE_INMEMORY_RETIRADOS = List.of(
            "InMemoryNormativaRepository",
            "InMemoryTalonarioRepository",
            "InMemoryTarifarioUnidadFaltasRepository",
            "InMemoryValorUnidadFaltasRepository",
            "InMemoryActaValorizacionRepository",
            "InMemoryActaValorizacionItemRepository",
            "InMemoryActaArticuloInfringidoRepository");

    @Nested
    @DisplayName("1. Los siete InMemory retirados no vuelven a src/main")
    class SieteInMemoryAusentesDeSrcMain {

        @Test
        @DisplayName("ninguno de los siete existe como archivo .java bajo src/main")
        void ningunArchivoBajoSrcMain() throws IOException {
            try (Stream<Path> archivos = Files.walk(SRC_MAIN_JAVA)) {
                List<String> encontrados = archivos
                        .filter(p -> p.toString().endsWith(".java"))
                        .map(p -> p.getFileName().toString().replace(".java", ""))
                        .filter(SIETE_INMEMORY_RETIRADOS::contains)
                        .collect(Collectors.toList());
                assertThat(encontrados)
                        .as("ninguno de los siete InMemory retirados debe reaparecer en src/main")
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("ninguno de los siete existe como .class compilado en target/classes")
        void ningunClasecompiladoEnTargetClasses() {
            for (String nombre : SIETE_INMEMORY_RETIRADOS) {
                Path clase = Paths.get(
                        "target/classes/ar/gob/malvinas/faltas/core/repository/memory/" + nombre + ".class");
                assertThat(Files.exists(clase))
                        .as(nombre + " no debe estar compilado como parte del artefacto productivo"
                                + " (target/classes)")
                        .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("2-3. Runner y materializador de demo ausentes de src/main")
    class RunnerYMaterializadorAusentes {

        @Test
        @DisplayName("CasoUsoFuncionalRunner.java no existe bajo src/main")
        void casoUsoFuncionalRunnerAusente() throws IOException {
            assertThat(existeArchivoLlamado("CasoUsoFuncionalRunner.java")).isFalse();
        }

        @Test
        @DisplayName("DemoActaMaterializadorService.java no existe bajo src/main")
        void demoActaMaterializadorServiceAusente() throws IOException {
            assertThat(existeArchivoLlamado("DemoActaMaterializadorService.java")).isFalse();
        }

        private boolean existeArchivoLlamado(String nombreArchivo) throws IOException {
            try (Stream<Path> archivos = Files.walk(SRC_MAIN_JAVA)) {
                return archivos.anyMatch(p -> p.getFileName().toString().equals(nombreArchivo));
            }
        }
    }

    @Nested
    @DisplayName("4. src/main no instancia InMemory* manualmente (fuera del propio paquete memory)")
    class SinInstanciacionManualDeInMemoryEnSrcMain {

        private static final Pattern NEW_INMEMORY = Pattern.compile("new\\s+InMemory\\w+\\s*\\(");

        @Test
        @DisplayName("ningun archivo de src/main fuera de repository/memory/ hace 'new InMemory...('")
        void ningunNewInMemoryFueraDelPaquetePropio() throws IOException {
            Path paqueteMemory = SRC_MAIN_JAVA.resolve("repository/memory");
            try (Stream<Path> archivos = Files.walk(SRC_MAIN_JAVA)) {
                List<String> violaciones = archivos
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.startsWith(paqueteMemory))
                        .filter(this::contieneNewInMemory)
                        .map(Path::toString)
                        .collect(Collectors.toList());
                assertThat(violaciones)
                        .as("ningun archivo de src/main fuera de repository/memory/ debe"
                                + " instanciar un adaptador InMemory manualmente")
                        .isEmpty();
            }
        }

        private boolean contieneNewInMemory(Path archivo) {
            try {
                Matcher m = NEW_INMEMORY.matcher(Files.readString(archivo));
                return m.find();
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + archivo, e);
            }
        }
    }

    @Nested
    @DisplayName("6-7. application.yml productivo sin modo memoria ni exclusion de DataSource")
    class ApplicationYmlSinMemoriaProductiva {

        private String leer() throws IOException {
            return Files.readString(APPLICATION_YML);
        }

        @Test
        @DisplayName("application.yml no declara 'mode: memory'")
        void sinModoMemory() throws IOException {
            assertThat(leer()).doesNotContain("mode: memory");
        }

        @Test
        @DisplayName("application.yml no excluye DataSourceAutoConfiguration")
        void sinExclusionDeDataSourceAutoConfiguration() throws IOException {
            // Verifica la directiva real de exclusion (FQCN completo bajo un item de lista
            // YAML), no una mencion textual: el propio comentario que documenta la ausencia de
            // esta exclusion nombra la clase, y una coincidencia de substring ingenua sobre esa
            // prosa daria un falso positivo.
            assertThat(leer()).doesNotContain(
                    "- org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
        }

        @Test
        @DisplayName("application.yml no declara spring.autoconfigure.exclude en absoluto")
        void sinBloqueDeExclusionDeAutoconfiguracion() throws IOException {
            assertThat(leer()).doesNotContain("autoconfigure:");
        }
    }

    @Nested
    @DisplayName("10. El controller de alta sigue usando ActaAltaTransientRetryService")
    class ControllerDeAltaUsaElRetryService {

        private static final Path ACTA_CONTROLLER = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/web/ActaController.java");

        @Test
        @DisplayName("ActaController depende del tipo ActaAltaTransientRetryService")
        void dependeDelTipo() throws IOException {
            assertThat(Files.readString(ACTA_CONTROLLER)).contains("ActaAltaTransientRetryService");
        }

        @Test
        @DisplayName("ActaController invoca labrarConReintento (no llama ActaService.labrar directo"
                + " para el alta)")
        void invocaLabrarConReintento() throws IOException {
            assertThat(Files.readString(ACTA_CONTROLLER)).contains(".labrarConReintento(");
        }
    }
}
