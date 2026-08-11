package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails del contrato del script de rollback del dominio BOD Faltas.
 *
 * <p>Verifica sin conexión a base de datos que
 * {@code database/ddl/rollback-bod-faltas-domain.sql} cumple el contrato taxativo
 * exigido por el slice {@code MARIADB-BASELINE-EXEC-001}:
 *
 * <ul>
 *   <li>ROLLBACK-G1: existe exactamente 1 {@code USE sb_faltas_db} y control de
 *       {@code FOREIGN_KEY_CHECKS} (0 antes del primer DROP, 1 al final).</li>
 *   <li>ROLLBACK-G2: exactamente 64 {@code DROP TABLE IF EXISTS}, nombres únicos,
 *       conjunto exactamente igual al TO_CREATE del inventario canónico.</li>
 *   <li>ROLLBACK-G3: el orden de los DROP es exactamente el inverso del orden de
 *       creación documentado en {@code database/design/dependency-order.md}.</li>
 *   <li>ROLLBACK-G4: ninguno de los 20 objetos protegidos del baseline (incluida
 *       {@code fal_rubro_version}) aparece en el archivo, bajo ninguna forma.</li>
 *   <li>ROLLBACK-G5: el archivo no contiene ninguna sentencia ejecutable distinta
 *       de {@code USE}, {@code SET FOREIGN_KEY_CHECKS} y {@code DROP TABLE IF EXISTS}
 *       sobre las 64 tablas autorizadas (sin {@code DROP DATABASE}, {@code DROP VIEW},
 *       {@code TRUNCATE}, {@code DELETE}, {@code UPDATE}, {@code INSERT}, {@code ALTER},
 *       {@code RENAME}, {@code CREATE} ni SQL dinámico).</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en {@code database/}.
 */
@DisplayName("Guardrails ROLLBACK: contrato del script de rollback del dominio BOD Faltas")
class DdlRollbackScriptContractTest {

    private static Path dbRoot;

    @BeforeAll
    static void localizarDatabaseRoot() {
        dbRoot = resolveDbRoot();
    }

    private static Path rollbackPath() {
        return dbRoot.resolve("ddl/rollback-bod-faltas-domain.sql");
    }

    private static String contenidoRollback() throws IOException {
        return leer(rollbackPath());
    }

    /** Extrae, en orden de aparición, los nombres de las 64 tablas TO_CREATE de dependency-order.md. */
    private static List<String> ordenCreacionEsperado() throws IOException {
        String contenido = leer(dbRoot.resolve("design/dependency-order.md"));
        List<String> orden = new ArrayList<>();
        Matcher m = Pattern.compile("\\|\\s*G\\d+-\\d+\\s*\\|\\s*`([^`]+)`").matcher(contenido);
        while (m.find()) {
            orden.add(m.group(1));
        }
        return orden;
    }

    /** Extrae, en orden de aparición, los nombres de las tablas DROP TABLE IF EXISTS del rollback. */
    private static List<String> ordenDropsRollback() throws IOException {
        List<String> nombres = new ArrayList<>();
        Matcher m = Pattern.compile("(?i)DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+(\\w+)\\s*;")
                .matcher(quitarComentariosLinea(contenidoRollback()));
        while (m.find()) {
            nombres.add(m.group(1).toLowerCase());
        }
        return nombres;
    }

    private static Set<String> toCreateDesdeInventario() throws IOException {
        return parsearInventario(leer(dbRoot.resolve("design/canonical-table-inventory.md")))
                .stream()
                .filter(f -> "TO_CREATE".equals(f.estado()))
                .map(FilaInventario::nombre)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ROLLBACK-G1: USE y control de FOREIGN_KEY_CHECKS")
    class G1_UseYForeignKeyChecks {

        @Test
        @DisplayName("rollback-bod-faltas-domain.sql existe")
        void scriptExiste() {
            assertThat(rollbackPath()).exists().isRegularFile();
        }

        @Test
        @DisplayName("USE sb_faltas_db aparece exactamente una vez")
        void useDeclaradoUnaVez() throws IOException {
            String contenido = contenidoRollback();
            long count = contenido.lines()
                    .filter(l -> l.strip().toUpperCase().matches("USE\\s.*"))
                    .count();
            assertThat(count).as("USE <database> debe aparecer exactamente una vez").isEqualTo(1);
            assertThat(contenido).containsIgnoringCase("USE sb_faltas_db");
        }

        @Test
        @DisplayName("SET FOREIGN_KEY_CHECKS = 0 antes del primer DROP; = 1 al final")
        void foreignKeyChecksControlado() throws IOException {
            String sinComentarios = quitarComentariosLinea(contenidoRollback());
            int idxOff = sinComentarios.indexOf("FOREIGN_KEY_CHECKS = 0");
            int idxOn = sinComentarios.indexOf("FOREIGN_KEY_CHECKS = 1");
            int idxFirstDrop = sinComentarios.toUpperCase().indexOf("DROP TABLE IF EXISTS");
            assertThat(idxOff).as("Debe existir SET FOREIGN_KEY_CHECKS = 0").isGreaterThanOrEqualTo(0);
            assertThat(idxOn).as("Debe existir SET FOREIGN_KEY_CHECKS = 1").isGreaterThanOrEqualTo(0);
            assertThat(idxOff).as("FOREIGN_KEY_CHECKS = 0 debe preceder al primer DROP").isLessThan(idxFirstDrop);
            assertThat(idxOn).as("FOREIGN_KEY_CHECKS = 1 debe ser posterior al último DROP")
                    .isGreaterThan(sinComentarios.toUpperCase().lastIndexOf("DROP TABLE IF EXISTS"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ROLLBACK-G2: 64 DROP TABLE IF EXISTS únicos = TO_CREATE del inventario")
    class G2_DropsUnicosEIgualesAlInventario {

        @Test
        @DisplayName("exactamente 65 DROP TABLE IF EXISTS")
        void exactamente64Drops() throws IOException {
            assertThat(ordenDropsRollback()).hasSize(65);
        }

        @Test
        @DisplayName("ninguna tabla aparece dos veces en DROP TABLE IF EXISTS")
        void sinDuplicados() throws IOException {
            List<String> nombres = ordenDropsRollback();
            Set<String> unicos = new LinkedHashSet<>(nombres);
            assertThat(unicos).as("Existen DROP TABLE duplicados: " + nombres).hasSize(nombres.size());
        }

        @Test
        @DisplayName("conjunto de tablas en el rollback == TO_CREATE del inventario canónico")
        void conjuntoIgualToCreate() throws IOException {
            Set<String> enRollback = new LinkedHashSet<>(ordenDropsRollback());
            Set<String> toCreate = toCreateDesdeInventario();
            assertThat(enRollback)
                    .as("Tablas en rollback pero no en TO_CREATE: "
                            + enRollback.stream().filter(t -> !toCreate.contains(t)).collect(Collectors.toSet())
                            + ". En TO_CREATE pero no en rollback: "
                            + toCreate.stream().filter(t -> !enRollback.contains(t)).collect(Collectors.toSet()))
                    .isEqualTo(toCreate);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ROLLBACK-G3: orden exactamente inverso a dependency-order.md")
    class G3_OrdenInverso {

        @Test
        @DisplayName("el orden de DROP es exactamente el inverso del orden de creación documentado")
        void ordenExactamenteInverso() throws IOException {
            List<String> creacion = ordenCreacionEsperado();
            assertThat(creacion).as("dependency-order.md debe documentar 65 tablas").hasSize(65);
            List<String> esperadoInverso = new ArrayList<>(creacion);
            java.util.Collections.reverse(esperadoInverso);
            List<String> actual = ordenDropsRollback();
            assertThat(actual)
                    .as("El orden de DROP TABLE IF EXISTS debe ser exactamente el inverso de "
                            + "dependency-order.md (G12→G1, inverso dentro de cada grupo)")
                    .isEqualTo(esperadoInverso);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ROLLBACK-G4: ningún objeto protegido aparece en el archivo, bajo ninguna forma")
    class G4_SinObjetosProtegidos {

        @Test
        @DisplayName("ninguno de los 20 objetos protegidos (incluida fal_rubro_version) aparece en el archivo")
        void sinObjetosProtegidosEnArchivo() throws IOException {
            String contenido = contenidoRollback();
            for (String obj : OBJETOS_PROTEGIDOS) {
                assertThat(contenido)
                        .as("El script de rollback no debe mencionar el objeto protegido '" + obj + "' bajo ninguna forma")
                        .doesNotContain(obj);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ROLLBACK-G5: contenido taxativo, sin sentencias ajenas")
    class G5_ContenidoTaxativo {

        @Test
        @DisplayName("toda sentencia ejecutable es USE, SET FOREIGN_KEY_CHECKS o DROP TABLE IF EXISTS")
        void soloSentenciasPermitidas() throws IOException {
            String sinComentarios = quitarComentariosLinea(contenidoRollback());
            for (String stmt : sinComentarios.split(";")) {
                String trimmed = stmt.strip();
                if (trimmed.isEmpty()) continue;
                String upper = trimmed.toUpperCase();
                boolean esUse = upper.matches("(?s)USE\\s+SB_FALTAS_DB");
                boolean esForeignKeyChecks = upper.matches("(?s)SET\\s+FOREIGN_KEY_CHECKS\\s*=\\s*[01]");
                boolean esDropTable = upper.matches("(?s)DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+\\w+");
                assertThat(esUse || esForeignKeyChecks || esDropTable)
                        .as("Sentencia no permitida en el rollback (solo USE/SET FOREIGN_KEY_CHECKS/DROP TABLE IF EXISTS): "
                                + trimmed)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("no contiene DROP DATABASE, DROP VIEW, TRUNCATE, DELETE, UPDATE, INSERT, ALTER, RENAME, CREATE ni SQL dinámico")
        void sinSentenciasProhibidas() throws IOException {
            String sinComentarios = quitarComentariosLinea(contenidoRollback()).toUpperCase();
            String[] prohibidas = {
                    "DROP DATABASE", "DROP VIEW", "TRUNCATE", "DELETE", "UPDATE", "INSERT",
                    "ALTER", "RENAME", "CREATE", "PREPARE", "EXECUTE"
            };
            for (String p : prohibidas) {
                assertThat(sinComentarios)
                        .as("El rollback no debe contener la sentencia prohibida '" + p + "'")
                        .doesNotContain(p);
            }
        }
    }
}
