package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails del contrato del inventario canónico de tablas.
 *
 * <p>Verifica sin conexión a base de datos que {@code database/design/canonical-table-inventory.md}
 * es consistente y coincide con el modelo lógico canónico.
 *
 * <ul>
 *   <li>INV-G1: 65 filas, numeradas 1..65, sin duplicados.</li>
 *   <li>INV-G2: exactamente 1 PREEXISTING_CANONICAL_ADOPTED (fal_rubro_version)
 *               y exactamente 64 TO_CREATE.</li>
 *   <li>INV-G3: tablas específicas esperadas presentes y sin duplicado.</li>
 *   <li>INV-G4: el conjunto exacto del inventario coincide con las secciones de
 *               {@code mariadb-logical-model.md}.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails INV: contrato del inventario canónico de tablas")
class DdlInventoryContractTest {

    private static Path dbRoot;

    @BeforeAll
    static void localizarDatabaseRoot() {
        dbRoot = resolveDbRoot();
    }

    private List<FilaInventario> filas() throws IOException {
        return parsearInventario(leer(dbRoot.resolve("design/canonical-table-inventory.md")));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("INV-G1: 65 filas, numeradas 1..65, sin duplicados")
    class G1_ConteoYNumeracion {

        @Test
        @DisplayName("inventario tiene exactamente 65 filas")
        void exactamente65Filas() throws IOException {
            assertThat(filas()).hasSize(65);
        }

        @Test
        @DisplayName("números de fila consecutivos 1..65")
        void numerosConsecutivos() throws IOException {
            List<Integer> numeros = filas().stream()
                    .map(FilaInventario::numero)
                    .sorted()
                    .collect(Collectors.toList());
            for (int i = 0; i < 65; i++) {
                assertThat(numeros.get(i))
                        .as("Fila %d debe tener número %d", i + 1, i + 1)
                        .isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("65 nombres únicos (sin duplicados)")
        void nombresSinDuplicados() throws IOException {
            List<String> nombres = filas().stream()
                    .map(FilaInventario::nombre)
                    .collect(Collectors.toList());
            Set<String> unicos = new HashSet<>(nombres);
            assertThat(unicos)
                    .as("El inventario tiene nombres duplicados: " + nombres)
                    .hasSize(65);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("INV-G2: 1 PREEXISTING + 64 TO_CREATE")
    class G2_EstadosConteo {

        @Test
        @DisplayName("exactamente 1 PREEXISTING_CANONICAL_ADOPTED (fal_rubro_version)")
        void exactamenteUnPreexistingFalRubroVersion() throws IOException {
            List<FilaInventario> preexisting = filas().stream()
                    .filter(f -> "PREEXISTING_CANONICAL_ADOPTED".equals(f.estado()))
                    .collect(Collectors.toList());
            assertThat(preexisting).hasSize(1);
            assertThat(preexisting.get(0).nombre()).isEqualTo("fal_rubro_version");
        }

        @Test
        @DisplayName("exactamente 64 TO_CREATE")
        void exactamente64ToCreate() throws IOException {
            long toCreate = filas().stream()
                    .filter(f -> "TO_CREATE".equals(f.estado()))
                    .count();
            assertThat(toCreate).isEqualTo(64);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("INV-G3: tablas específicas esperadas presentes y sin duplicado")
    class G3_TablasEspecificas {

        @Test
        @DisplayName("fal_documento_plantilla aparece exactamente una vez")
        void fal_documento_plantillaNoDuplicada() throws IOException {
            long count = filas().stream()
                    .filter(f -> "fal_documento_plantilla".equals(f.nombre()))
                    .count();
            assertThat(count)
                    .as("fal_documento_plantilla debe aparecer exactamente una vez en el inventario")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("fal_acta_economia_proyeccion está numerada en el inventario")
        void fal_acta_economia_proyeccionNumerada() throws IOException {
            boolean presente = filas().stream()
                    .anyMatch(f -> "fal_acta_economia_proyeccion".equals(f.nombre()));
            assertThat(presente)
                    .as("fal_acta_economia_proyeccion debe aparecer como fila numerada en el inventario")
                    .isTrue();
        }

        @Test
        @DisplayName("fal_acta está en el inventario")
        void fal_actaPresente() throws IOException {
            boolean presente = filas().stream()
                    .anyMatch(f -> "fal_acta".equals(f.nombre()));
            assertThat(presente).isTrue();
        }

        @Test
        @DisplayName("fal_acta_snapshot está en el inventario")
        void fal_acta_snapshotPresente() throws IOException {
            boolean presente = filas().stream()
                    .anyMatch(f -> "fal_acta_snapshot".equals(f.nombre()));
            assertThat(presente).isTrue();
        }

        @Test
        @DisplayName("fal_persona está en el inventario como TO_CREATE (FULL-R1, GAP cerrado)")
        void fal_personaEstaEnToCreate() throws IOException {
            boolean presente = filas().stream()
                    .anyMatch(f -> "fal_persona".equals(f.nombre()) && "TO_CREATE".equals(f.estado()));
            assertThat(presente)
                    .as("fal_persona debe estar en el inventario con estado TO_CREATE — GAP-DDL-R1-PERSONA-LONGITUD-01 CERRADO en FULL-R1")
                    .isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("INV-G5: fal_persona en FULL-R1 — cierre de drift residual CORRECCION-09")
    class G5_FalPersonaFullR1 {

        @Test
        @DisplayName("inventario no contiene referencia a R-PERS (slice obsoleto eliminado)")
        void inventarioSinRPers() throws IOException {
            String contenido = leer(dbRoot.resolve("design/canonical-table-inventory.md"));
            assertThat(contenido)
                    .as("El inventario no debe contener 'R-PERS' — fal_persona ya pertenece a FULL-R1")
                    .doesNotContain("R-PERS");
        }

        @Test
        @DisplayName("inventario no contiene 'reconciliación física pendiente' (gap cerrado)")
        void inventarioSinReconciliacionPendiente() throws IOException {
            String contenido = leer(dbRoot.resolve("design/canonical-table-inventory.md"));
            assertThat(contenido)
                    .as("El inventario no debe contener 'reconciliación física pendiente' — GAP-DDL-R1-PERSONA-LONGITUD-01 fue cerrado")
                    .doesNotContain("reconciliación física pendiente");
        }

        @Test
        @DisplayName("inventario no contiene GAP-DDL-R1-PERSONA-LONGITUD-01 como gap pendiente")
        void inventarioSinGapAbierto() throws IOException {
            String contenido = leer(dbRoot.resolve("design/canonical-table-inventory.md"));
            assertThat(contenido)
                    .as("El inventario no debe referenciar GAP-DDL-R1-PERSONA-LONGITUD-01 sin el estado CERRADO")
                    .doesNotContainPattern("GAP-DDL-R1-PERSONA-LONGITUD-01(?!\\s+CERRADO)");
        }

        @Test
        @DisplayName("ddl-full-scope.md declara GAP-DDL-R1-PERSONA-LONGITUD-01 como cerrado")
        void fullScopeDeclaraGapCerrado() throws IOException {
            String contenido = leer(dbRoot.resolve("design/ddl-full-scope.md"));
            assertThat(contenido)
                    .as("ddl-full-scope.md debe declarar GAP-DDL-R1-PERSONA-LONGITUD-01 como cerrado")
                    .contains("GAP cerrado")
                    .contains("GAP-DDL-R1-PERSONA-LONGITUD-01");
        }

        @Test
        @DisplayName("inventario no declara 'slice R-PERS futuro'")
        void inventarioSinSliceRPersFuturo() throws IOException {
            String contenido = leer(dbRoot.resolve("design/canonical-table-inventory.md"));
            assertThat(contenido)
                    .as("El inventario no debe mencionar 'slice R-PERS futuro'")
                    .doesNotContain("slice R-PERS futuro");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("INV-G4: conjunto exacto del inventario coincide con modelo lógico")
    class G4_CoincidenciaModeloLogico {

        @Test
        @DisplayName("conjunto exacto del inventario == secciones de mariadb-logical-model.md")
        void conjuntoCoincideConModeloLogico() throws IOException {
            Path modeloLogico = Paths.get(
                    "docs", "spec-as-source", "50-persistence", "mariadb-logical-model.md");
            assertThat(modeloLogico)
                    .as("El modelo lógico debe existir como archivo regular (fuente normativa obligatoria)")
                    .exists().isRegularFile();
            Set<String> inventario = filas().stream()
                    .map(FilaInventario::nombre)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> modeloTables = parsearTablasModeloLogico(leer(modeloLogico));
            assertThat(inventario)
                    .as("El conjunto del inventario debe coincidir exactamente con las tablas del modelo lógico. "
                            + "En inventario pero no en modelo: "
                            + inventario.stream().filter(t -> !modeloTables.contains(t)).collect(Collectors.toSet())
                            + ". En modelo pero no en inventario: "
                            + modeloTables.stream().filter(t -> !inventario.contains(t)).collect(Collectors.toSet()))
                    .isEqualTo(modeloTables);
        }
    }
}
