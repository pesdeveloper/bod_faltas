package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.extraerBloquesDdl;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.extraerCuerpo;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.extraerNombreTabla;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.leer;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.quitarComentariosLinea;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.resolveDbRoot;
import static ar.gob.malvinas.faltas.core.application.DdlTestSupport.resolveDocsRoot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails del modelo mínimo aprobado del bus de eventos municipal
 * (SPEC-FIRMAS-EDA-CLOSURE-001, simplificación definitiva).
 *
 * <p>Verifica sin conexión a base de datos que el paquete DDL de
 * {@code database/ddl/alter-firmas-eda-closure-001-forward.sql} y el paquete
 * separado de Firmas ({@code database/ddl/firmas/create-firmas-eda-outbox-inbox.sql})
 * implementan exclusivamente el modelo mínimo:
 *
 * <ul>
 *   <li>BUS-G1: {@code bus_evento} es la única tabla central, append-only, con
 *       exactamente sus columnas y constraints aprobados.</li>
 *   <li>BUS-G2: {@code fal_outbox}/{@code fal_bus_checkpoint}/{@code fal_inbox}/
 *       {@code fal_inbox_error} existen con la estructura mínima aprobada.</li>
 *   <li>BUS-G3: el paquete de Firmas ({@code fir_*}) espeja exactamente los
 *       nombres de columna de su contraparte {@code fal_*} (mismo contrato,
 *       distinto prefijo).</li>
 *   <li>BUS-G4: ausencia total, en todo {@code database/}, de la arquitectura
 *       eliminada: {@code bus_evento_entrega}, {@code bus_suscripcion},
 *       {@code bus_publicacion_permiso}, {@code bus_definer}, rutinas
 *       {@code SQL SECURITY DEFINER}, {@code sp_bus_*}, vistas {@code v_bus_*}.</li>
 *   <li>BUS-G5: los grants de aplicación otorgan únicamente privilegios
 *       directos mínimos sobre {@code bus_evento} —{@code INSERT} column-level
 *       (excluyendo {@code bus_evento_id}) para publishers, {@code SELECT}/
 *       {@code SELECT+DELETE} de tabla completa para consumers/maintenance—,
 *       sin {@code GRANT EXECUTE}, sin {@code CREATE USER}.</li>
 *   <li>BUS-G6: el rollback de tablas y de grants del bus es simétrico exacto
 *       al forward (mismas 6 tablas nuevas y mismos 5 GRANT, ninguno extra).</li>
 *   <li>BUS-G7: la ingestión (Inbox + checkpoint) y el procesamiento por fila
 *       son transacciones locales independientes; la lectura central usa
 *       {@code START TRANSACTION READ ONLY} + {@code LOCK IN SHARE MODE}.</li>
 *   <li>BUS-G8: {@code evento_id} (identidad lógica, {@code UNIQUE} en ambas
 *       Inbox) es distinto de {@code bus_evento_id} (cursor técnico); un
 *       rechazo funcional de dominio cierra la Inbox en {@code PROCESADO}
 *       sin fila en {@code *_inbox_error}; sin cuarentena central ni
 *       {@code X-Api-Key} para el bus.</li>
 *   <li>BUS-G9: el reintento manual de Outbox conserva {@code evento_id}; la
 *       retención de {@code bus_evento} es exclusivamente por antigüedad; el
 *       publisher reconcilia por {@code duplicate key}, sin {@code SELECT}.</li>
 * </ul>
 *
 * <p>No usa Mockito, no usa base de datos. Solo lectura de archivos en
 * {@code database/}.
 */
@DisplayName("Guardrails BUS: modelo mínimo aprobado del bus de eventos municipal")
class FirmasEdaBusSimpleContractTest {

    private static Path dbRoot;
    private static Path docsRoot;

    /**
     * Patrones de sintaxis SQL <b>ejecutable</b> que materializarían la
     * arquitectura eliminada (fan-out central, rutinas SECURITY DEFINER,
     * cuenta {@code bus_definer}). No son simples substrings de texto: los
     * comentarios normativos vigentes mencionan legítimamente estos nombres
     * en forma negada (ej. "sin rutinas SQL SECURITY DEFINER"), por lo que la
     * detección exige la sintaxis real de creación/otorgamiento, no la sola
     * presencia del nombre.
     */
    private static final List<Pattern> PATRONES_ARQUITECTURA_ELIMINADA = List.of(
            Pattern.compile("(?i)CREATE\\s+TABLE\\s+bus_evento_entrega"),
            Pattern.compile("(?i)CREATE\\s+TABLE\\s+bus_suscripcion"),
            Pattern.compile("(?i)CREATE\\s+TABLE\\s+bus_publicacion_permiso"),
            Pattern.compile("(?i)CREATE\\s+(OR\\s+REPLACE\\s+)?DEFINER\\s*="),
            Pattern.compile("(?i)CREATE\\s+PROCEDURE\\s+sp_bus"),
            Pattern.compile("(?i)CREATE\\s+VIEW\\s+v_bus"),
            Pattern.compile("(?i)'bus_definer'"),
            Pattern.compile("(?i)CREATE\\s+USER\\s+.*bus_definer")
    );

    @BeforeAll
    static void localizarDatabaseRoot() {
        dbRoot = resolveDbRoot();
        docsRoot = resolveDocsRoot();
    }

    private static Path eventingOperationsPath() {
        return docsRoot.resolve("30-integrations/eventing-operations.md");
    }

    private static Path physicalModelPath() {
        return docsRoot.resolve("50-persistence/firma-eda-physical-model.md");
    }

    private static Path firmaNotificacionFalloPath() {
        return docsRoot.resolve("10-domain/firma-notificacion-fallo.md");
    }

    private static Path firmaEventContractsPath() {
        return docsRoot.resolve("40-api/firma-event-contracts.md");
    }

    private static Path forwardPath() {
        return dbRoot.resolve("ddl/alter-firmas-eda-closure-001-forward.sql");
    }

    private static Path rollbackPath() {
        return dbRoot.resolve("ddl/alter-firmas-eda-closure-001-rollback.sql");
    }

    private static Path grantsPath() {
        return dbRoot.resolve("ddl/grants-firmas-eda-bus.sql");
    }

    private static Path firmasCreatePath() {
        return dbRoot.resolve("ddl/firmas/create-firmas-eda-outbox-inbox.sql");
    }

    private static Path firmasRollbackPath() {
        return dbRoot.resolve("ddl/firmas/rollback-firmas-eda-outbox-inbox.sql");
    }

    /** Bloque CREATE TABLE de {@code nombreTabla} dentro del script indicado. */
    private static String bloqueTabla(Path script, String nombreTabla) throws IOException {
        String sql = leer(script);
        return extraerBloquesDdl(sql).stream()
                .filter(b -> extraerNombreTabla(b).equals(nombreTabla))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontro CREATE TABLE " + nombreTabla + " en " + script));
    }

    /** Nombres de columna física (snake_case en inicio de línea, indentación libre) del cuerpo de un bloque. */
    private static List<String> columnasDe(String cuerpo) {
        Pattern p = Pattern.compile("^\\s*([a-z][a-z0-9_]*)\\s+\\S", Pattern.MULTILINE);
        return p.matcher(cuerpo).results()
                .map(m -> m.group(1))
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("BUS-G1: bus_evento es la única tabla central, append-only")
    class G1_BusEventoCentral {

        @Test
        @DisplayName("bus_evento existe con sus columnas mínimas aprobadas")
        void busEventoTieneColumnasMinimas() throws IOException {
            String cuerpo = extraerCuerpo(bloqueTabla(forwardPath(), "bus_evento"));
            List<String> columnas = columnasDe(cuerpo);
            assertThat(columnas).containsExactly(
                    "bus_evento_id", "evento_id", "productor_codigo", "evento_codigo",
                    "schema_version", "agregado_codigo", "agregado_id", "agregado_version",
                    "correlation_id", "causation_id", "payload", "ocurrido_en", "publicado_en");
        }

        @Test
        @DisplayName("bus_evento tiene PK bus_evento_id y UNIQUE(evento_id)")
        void busEventoTienePkYUniqueDeIdentidad() throws IOException {
            String bloque = bloqueTabla(forwardPath(), "bus_evento");
            assertThat(bloque).contains("PRIMARY KEY (bus_evento_id)");
            assertThat(bloque).contains("UNIQUE KEY uq_bus_evento_evento_id (evento_id)");
        }

        @Test
        @DisplayName("bus_evento no tiene UPDATE ni DELETE definido en el forward (solo INSERT/SELECT vía grants)")
        void busEventoSoloSeInsertaEnElForward() throws IOException {
            String sql = leer(forwardPath());
            assertThat(sql).doesNotContain("UPDATE bus_evento");
            assertThat(sql).doesNotContain("DELETE FROM bus_evento");
        }

        @Test
        @DisplayName("productor_codigo y agregado_codigo no tienen CHECK IN cerrado de aplicaciones")
        void sinCheckInCerradoDeAplicaciones() throws IOException {
            String bloque = bloqueTabla(forwardPath(), "bus_evento");
            assertThat(bloque).doesNotContainIgnoringCase("CHECK (productor_codigo IN");
            assertThat(bloque).doesNotContainIgnoringCase("CHECK (agregado_codigo IN");
        }
    }

    @Nested
    @DisplayName("BUS-G2: Outbox/checkpoint/Inbox/Inbox error locales de Faltas")
    class G2_TablasLocalesFaltas {

        @Test
        @DisplayName("fal_outbox existe con sus columnas mínimas aprobadas")
        void falOutboxTieneColumnasMinimas() throws IOException {
            String cuerpo = extraerCuerpo(bloqueTabla(forwardPath(), "fal_outbox"));
            assertThat(columnasDe(cuerpo)).containsExactly(
                    "outbox_id", "evento_id", "evento_codigo", "schema_version",
                    "agregado_codigo", "agregado_id", "agregado_version",
                    "correlation_id", "causation_id", "payload", "status", "si_error",
                    "attempts", "next_attempt_at", "ocurrido_en", "creado_en", "publicado_en");
        }

        @Test
        @DisplayName("fal_bus_checkpoint es singleton (CHECK checkpoint_id = 1) y tiene seed")
        void falBusCheckpointEsSingletonConSeed() throws IOException {
            String sql = leer(forwardPath());
            String bloque = bloqueTabla(forwardPath(), "fal_bus_checkpoint");
            assertThat(bloque).contains("CHECK (checkpoint_id = 1)");
            assertThat(sql).contains(
                    "INSERT INTO fal_bus_checkpoint (checkpoint_id, ultimo_evento_id, actualizado_en)");
        }

        @Test
        @DisplayName("fal_inbox tiene PK = bus_evento_id (sin id técnico propio)")
        void falInboxTienePkBusEventoId() throws IOException {
            String bloque = bloqueTabla(forwardPath(), "fal_inbox");
            String cuerpo = extraerCuerpo(bloque);
            assertThat(bloque).contains("PRIMARY KEY (bus_evento_id)");
            assertThat(columnasDe(cuerpo)).contains(
                    "bus_evento_id", "evento_codigo", "payload", "status", "si_error",
                    "attempts", "next_attempt_at", "recibido_en", "procesado_en");
            assertThat(columnasDe(cuerpo)).doesNotContain("inbox_id");
        }

        @Test
        @DisplayName("fal_inbox conserva el envelope completo (evento_id/productor_codigo/schema_version/correlation_id/causation_id/ocurrido_en) con UNIQUE(evento_id)")
        void falInboxTieneEnvelopeCompletoConUniqueDeEventoId() throws IOException {
            String bloque = bloqueTabla(forwardPath(), "fal_inbox");
            String cuerpo = extraerCuerpo(bloque);
            assertThat(columnasDe(cuerpo)).containsExactly(
                    "bus_evento_id", "evento_id", "productor_codigo", "evento_codigo",
                    "schema_version", "agregado_codigo", "agregado_id", "agregado_version",
                    "correlation_id", "causation_id", "payload", "status", "si_error",
                    "attempts", "next_attempt_at", "ocurrido_en", "recibido_en", "procesado_en");
            assertThat(bloque).contains("UNIQUE KEY uq_fal_inbox_evento_id (evento_id)");
        }

        @Test
        @DisplayName("fal_inbox_error tiene FK RESTRICT a fal_inbox y error_detail acotado")
        void falInboxErrorTieneFkYDetalleAcotado() throws IOException {
            String bloque = bloqueTabla(forwardPath(), "fal_inbox_error");
            assertThat(bloque).contains(
                    "REFERENCES fal_inbox (bus_evento_id) ON DELETE RESTRICT ON UPDATE RESTRICT");
            assertThat(bloque).containsPattern("(?i)error_detail\\s+VARCHAR\\(255\\)");
            assertThat(bloque.toUpperCase()).doesNotContain(" TEXT ");
            assertThat(bloque).doesNotContainIgnoringCase("stack_trace");
        }
    }

    @Nested
    @DisplayName("BUS-G3: paquete Firmas espeja exactamente la estructura Faltas")
    class G3_PaqueteFirmasEspejo {

        @Test
        @DisplayName("fir_outbox tiene las mismas columnas que fal_outbox")
        void firOutboxEspejaFalOutbox() throws IOException {
            List<String> faltas = columnasDe(extraerCuerpo(bloqueTabla(forwardPath(), "fal_outbox")));
            List<String> firmas = columnasDe(extraerCuerpo(bloqueTabla(firmasCreatePath(), "fir_outbox")));
            assertThat(firmas).containsExactlyElementsOf(faltas);
        }

        @Test
        @DisplayName("fir_bus_checkpoint tiene las mismas columnas que fal_bus_checkpoint y es singleton con seed")
        void firBusCheckpointEspejaFalBusCheckpoint() throws IOException {
            List<String> faltas = columnasDe(extraerCuerpo(bloqueTabla(forwardPath(), "fal_bus_checkpoint")));
            List<String> firmas = columnasDe(extraerCuerpo(bloqueTabla(firmasCreatePath(), "fir_bus_checkpoint")));
            assertThat(firmas).containsExactlyElementsOf(faltas);

            String bloqueFirmas = bloqueTabla(firmasCreatePath(), "fir_bus_checkpoint");
            assertThat(bloqueFirmas).contains("CHECK (checkpoint_id = 1)");
            String sqlFirmas = leer(firmasCreatePath());
            assertThat(sqlFirmas).contains(
                    "INSERT INTO fir_bus_checkpoint (checkpoint_id, ultimo_evento_id, actualizado_en)");
        }

        @Test
        @DisplayName("fir_inbox tiene las mismas columnas que fal_inbox (envelope completo incluido), con PK = bus_evento_id y UNIQUE(evento_id)")
        void firInboxEspejaFalInbox() throws IOException {
            List<String> faltas = columnasDe(extraerCuerpo(bloqueTabla(forwardPath(), "fal_inbox")));
            List<String> firmas = columnasDe(extraerCuerpo(bloqueTabla(firmasCreatePath(), "fir_inbox")));
            assertThat(firmas).containsExactlyElementsOf(faltas);
            assertThat(faltas).contains("evento_id", "productor_codigo", "schema_version",
                    "correlation_id", "causation_id", "ocurrido_en");
            String bloqueFirmas = bloqueTabla(firmasCreatePath(), "fir_inbox");
            assertThat(bloqueFirmas).contains("PRIMARY KEY (bus_evento_id)");
            assertThat(bloqueFirmas).contains("UNIQUE KEY uq_fir_inbox_evento_id (evento_id)");
        }

        @Test
        @DisplayName("fir_inbox_error tiene las mismas columnas que fal_inbox_error, con FK RESTRICT")
        void firInboxErrorEspejaFalInboxError() throws IOException {
            List<String> faltas = columnasDe(extraerCuerpo(bloqueTabla(forwardPath(), "fal_inbox_error")));
            List<String> firmas = columnasDe(extraerCuerpo(bloqueTabla(firmasCreatePath(), "fir_inbox_error")));
            assertThat(firmas).containsExactlyElementsOf(faltas);
            assertThat(bloqueTabla(firmasCreatePath(), "fir_inbox_error"))
                    .contains("REFERENCES fir_inbox (bus_evento_id) ON DELETE RESTRICT ON UPDATE RESTRICT");
        }

        @Test
        @DisplayName("el script de Firmas no ejecuta USE (la base de Firmas no es un objeto de este repositorio)")
        void firmasNoEjecutaUse() throws IOException {
            String sql = leer(firmasCreatePath());
            assertThat(sql).doesNotContainPattern("(?i)USE\\s+\\w+;");
        }
    }

    @Nested
    @DisplayName("BUS-G4: ausencia total de la arquitectura eliminada en el SQL ejecutable")
    class G4_SinArquitecturaEliminada {

        @Test
        @DisplayName("ningún .sql de database/ contiene sintaxis ejecutable de la arquitectura eliminada")
        void sinSintaxisEjecutableDeArquitecturaEliminada() throws IOException {
            List<Path> archivosSql;
            try (Stream<Path> stream = Files.walk(dbRoot, FileVisitOption.FOLLOW_LINKS)) {
                archivosSql = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".sql"))
                        .collect(Collectors.toList());
            }
            List<String> hallazgos = new java.util.ArrayList<>();
            for (Path archivo : archivosSql) {
                String contenido = leer(archivo);
                for (Pattern patron : PATRONES_ARQUITECTURA_ELIMINADA) {
                    if (patron.matcher(contenido).find()) {
                        hallazgos.add(archivo + " :: " + patron.pattern());
                    }
                }
            }
            assertThat(hallazgos).isEmpty();
        }

        @Test
        @DisplayName("ningún script del bus contiene CREATE PROCEDURE, CREATE FUNCTION ni CREATE VIEW")
        void sinRutinasNiVistas() throws IOException {
            List<Path> scripts = List.of(
                    forwardPath(), rollbackPath(), grantsPath(),
                    dbRoot.resolve("ddl/rollback-grants-firmas-eda-bus.sql"),
                    firmasCreatePath(), firmasRollbackPath());
            for (Path script : scripts) {
                String sql = leer(script).toUpperCase();
                assertThat(sql).as(script.toString()).doesNotContain("CREATE PROCEDURE");
                assertThat(sql).as(script.toString()).doesNotContain("CREATE FUNCTION");
                assertThat(sql).as(script.toString()).doesNotContain("CREATE VIEW");
                assertThat(sql).as(script.toString()).doesNotContain("CREATE DEFINER");
            }
        }
    }

    /**
     * Las 12 columnas de {@code bus_evento} distintas de {@code bus_evento_id}
     * (orden exacto del {@code GRANT INSERT (...)}/{@code REVOKE INSERT (...)}
     * de ambos publishers).
     */
    private static final List<String> COLUMNAS_INSERTABLES_BUS_EVENTO = List.of(
            "evento_id", "productor_codigo", "evento_codigo", "schema_version",
            "agregado_codigo", "agregado_id", "agregado_version", "correlation_id",
            "causation_id", "payload", "ocurrido_en", "publicado_en");

    @Nested
    @DisplayName("BUS-G5: grants directos y mínimos, sin definer ni CREATE USER")
    class G5_GrantsMinimos {

        @Test
        @DisplayName("los publishers reciben INSERT column-level sobre bus_evento, nunca de tabla completa")
        void publishersRecibenInsertColumnLevel() throws IOException {
            String sql = leer(grantsPath());
            String columnas = String.join(",\\s*", COLUMNAS_INSERTABLES_BUS_EVENTO.stream()
                    .map(Pattern::quote).collect(Collectors.toList()));
            Pattern grantFaltas = Pattern.compile(
                    "(?i)GRANT\\s+INSERT\\s*\\(\\s*" + columnas
                            + "\\s*\\)\\s*ON\\s+sb_faltas_db\\.bus_evento\\s+TO\\s+'bus_publisher_faltas'@'%'");
            Pattern grantFirmas = Pattern.compile(
                    "(?i)GRANT\\s+INSERT\\s*\\(\\s*" + columnas
                            + "\\s*\\)\\s*ON\\s+sb_faltas_db\\.bus_evento\\s+TO\\s+'bus_publisher_firmas'@'%'");
            assertThat(grantFaltas.matcher(sql).find()).as("GRANT INSERT column-level a bus_publisher_faltas").isTrue();
            assertThat(grantFirmas.matcher(sql).find()).as("GRANT INSERT column-level a bus_publisher_firmas").isTrue();

            // Nunca INSERT de tabla completa (sin lista de columnas): permitiria
            // que el publisher escriba manualmente bus_evento_id.
            String ejecutable = quitarComentariosLinea(sql);
            assertThat(ejecutable).doesNotContainPattern(
                    "(?i)GRANT\\s+INSERT\\s+ON\\s+sb_faltas_db\\.bus_evento");
        }

        @Test
        @DisplayName("bus_evento_id nunca aparece en la lista de columnas insertables del publisher")
        void publisherNuncaPuedeInsertarBusEventoId() throws IOException {
            String ejecutable = quitarComentariosLinea(leer(grantsPath()));
            Pattern grantInsert = Pattern.compile(
                    "(?i)GRANT\\s+INSERT\\s*\\(([^)]*)\\)\\s*ON\\s+sb_faltas_db\\.bus_evento");
            var matcher = grantInsert.matcher(ejecutable);
            int encontrados = 0;
            while (matcher.find()) {
                encontrados++;
                assertThat(matcher.group(1)).doesNotContainIgnoringCase("bus_evento_id");
            }
            assertThat(encontrados).as("cantidad de GRANT INSERT(...) sobre bus_evento").isEqualTo(2);
        }

        @Test
        @DisplayName("consumers reciben SELECT y maintenance SELECT+DELETE, ambos de tabla completa")
        void consumersYMaintenanceReceivenPrivilegioDeTablaCompleta() throws IOException {
            String sql = leer(grantsPath());
            assertThat(sql).contains("GRANT SELECT ON sb_faltas_db.bus_evento TO 'bus_consumer_faltas'@'%'");
            assertThat(sql).contains("GRANT SELECT ON sb_faltas_db.bus_evento TO 'bus_consumer_firmas'@'%'");
            assertThat(sql).contains("GRANT SELECT, DELETE ON sb_faltas_db.bus_evento TO 'bus_maintenance'@'%'");

            // Los comentarios normativos mencionan legitimamente, en forma negada,
            // "sin CREATE USER"/"sin IDENTIFIED BY": la verificacion de ausencia debe
            // recaer sobre el SQL ejecutable (sin lineas de comentario), no sobre el
            // texto completo del archivo.
            String ejecutable = quitarComentariosLinea(sql).toUpperCase();
            assertThat(ejecutable).doesNotContain("GRANT EXECUTE");
            assertThat(ejecutable).doesNotContain("CREATE USER");
            assertThat(ejecutable).doesNotContain("IDENTIFIED BY");
        }

        @Test
        @DisplayName("ninguna cuenta bus_* recibe privilegios sobre tablas locales de Outbox/Inbox")
        void ningunaCuentaBusAccedeATablasLocales() throws IOException {
            // Los comentarios normativos mencionan legitimamente fal_outbox/fal_inbox
            // en forma negada ("ninguna cuenta bus_* tiene acceso a..."); la
            // verificacion recae sobre el SQL ejecutable (sin comentarios).
            String ejecutable = quitarComentariosLinea(leer(grantsPath()));
            assertThat(ejecutable).doesNotContain("fal_outbox");
            assertThat(ejecutable).doesNotContain("fal_bus_checkpoint");
            assertThat(ejecutable).doesNotContain("fal_inbox");
            assertThat(ejecutable).doesNotContain("fir_outbox");
            assertThat(ejecutable).doesNotContain("fir_inbox");
        }

        @Test
        @DisplayName("el rollback de grants revoca exactamente lo otorgado (column-level en publishers), sin DROP USER")
        void rollbackDeGrantsEsSimetrico() throws IOException {
            String sql = leer(dbRoot.resolve("ddl/rollback-grants-firmas-eda-bus.sql"));
            String columnas = String.join(",\\s*", COLUMNAS_INSERTABLES_BUS_EVENTO.stream()
                    .map(Pattern::quote).collect(Collectors.toList()));
            Pattern revokeFaltas = Pattern.compile(
                    "(?i)REVOKE\\s+INSERT\\s*\\(\\s*" + columnas
                            + "\\s*\\)\\s*ON\\s+sb_faltas_db\\.bus_evento\\s+FROM\\s+'bus_publisher_faltas'@'%'");
            Pattern revokeFirmas = Pattern.compile(
                    "(?i)REVOKE\\s+INSERT\\s*\\(\\s*" + columnas
                            + "\\s*\\)\\s*ON\\s+sb_faltas_db\\.bus_evento\\s+FROM\\s+'bus_publisher_firmas'@'%'");
            assertThat(revokeFaltas.matcher(sql).find()).as("REVOKE INSERT column-level de bus_publisher_faltas").isTrue();
            assertThat(revokeFirmas.matcher(sql).find()).as("REVOKE INSERT column-level de bus_publisher_firmas").isTrue();
            assertThat(sql).contains("REVOKE SELECT ON sb_faltas_db.bus_evento FROM 'bus_consumer_faltas'@'%'");
            assertThat(sql).contains("REVOKE SELECT ON sb_faltas_db.bus_evento FROM 'bus_consumer_firmas'@'%'");
            assertThat(sql).contains("REVOKE SELECT, DELETE ON sb_faltas_db.bus_evento FROM 'bus_maintenance'@'%'");

            // "NO INCLUYE `DROP USER`" es un comentario negado legitimo; se excluye
            // de la verificacion junto con el resto de las lineas de comentario.
            String ejecutable = quitarComentariosLinea(sql).toUpperCase();
            assertThat(ejecutable).doesNotContain("DROP USER");
        }
    }

    @Nested
    @DisplayName("BUS-G6: rollback de tablas del bus es simétrico exacto al forward")
    class G6_RollbackSimetrico {

        @Test
        @DisplayName("el rollback elimina las 5 tablas nuevas de la sección 05 del bus, en orden de dependencias")
        void rollbackEliminaLasCincoTablasDelBus() throws IOException {
            String sql = leer(rollbackPath());
            List<String> esperado = List.of(
                    "fal_inbox_error", "fal_inbox", "fal_bus_checkpoint", "bus_evento", "fal_outbox");
            int ultimaPosicion = -1;
            for (String tabla : esperado) {
                String stmt = "DROP TABLE IF EXISTS " + tabla + ";";
                int pos = sql.indexOf(stmt);
                assertThat(pos).as("DROP de " + tabla).isGreaterThan(ultimaPosicion);
                ultimaPosicion = pos;
            }
            // fal_inbox_error cae antes de fal_inbox por su FK.
            assertThat(sql.indexOf("DROP TABLE IF EXISTS fal_inbox_error;"))
                    .isLessThan(sql.indexOf("DROP TABLE IF EXISTS fal_inbox;"));
        }

        @Test
        @DisplayName("el rollback de Firmas elimina exactamente las 4 tablas locales, en orden de dependencias")
        void rollbackFirmasEliminaLasCuatroTablas() throws IOException {
            String sql = leer(firmasRollbackPath());
            assertThat(sql.indexOf("DROP TABLE IF EXISTS fir_inbox_error;"))
                    .isLessThan(sql.indexOf("DROP TABLE IF EXISTS fir_inbox;"));
            assertThat(sql).contains("DROP TABLE IF EXISTS fir_bus_checkpoint;");
            assertThat(sql).contains("DROP TABLE IF EXISTS fir_outbox;");
        }
    }

    @Nested
    @DisplayName("BUS-G7: ingestión y procesamiento son transacciones locales separadas; lectura central con LOCK IN SHARE MODE")
    class G7_TransaccionesSeparadasYLecturaBloqueante {

        @Test
        @DisplayName("eventing-operations.md documenta la ingestión (Inbox+checkpoint) y el procesamiento por fila como transacciones locales independientes")
        void ingestionYProcesamientoSonTransaccionesIndependientes() throws IOException {
            String md = leer(eventingOperationsPath());
            assertThat(md).containsIgnoringCase("dos transacciones locales independientes");
            assertThat(md).contains("Transacción 1 (ingestión, por lote):");
            assertThat(md).contains("Transacción 2 (procesamiento, por fila,");
            assertThat(md).doesNotContainIgnoringCase("una única transacción de todo el lote");
        }

        @Test
        @DisplayName("la lectura central usa START TRANSACTION READ ONLY + LOCK IN SHARE MODE, nunca fuera de una transacción explícita")
        void lecturaCentralUsaTransaccionExplicitaConLockInShareMode() throws IOException {
            String md = leer(eventingOperationsPath());
            assertThat(md).contains("START TRANSACTION READ ONLY;");
            assertThat(md).contains("LOCK IN SHARE MODE;");
            assertThat(md).containsIgnoringCase("checkpoint avance más allá de un hueco todavía");
        }

        @Test
        @DisplayName("el modelo físico documenta la misma separación de transacciones y el mismo LOCK IN SHARE MODE que eventing-operations.md")
        void modeloFisicoDocumentaLaMismaSeparacionYLock() throws IOException {
            String md = leer(physicalModelPath());
            assertThat(md).containsIgnoringCase("independiente del");
            assertThat(md).containsIgnoringCase("procesamiento funcional");
            assertThat(md).containsIgnoringCase("transacción local de procesamiento");
            assertThat(md).contains("LOCK IN SHARE MODE");
            assertThat(md).contains("START TRANSACTION READ ONLY;");
        }
    }

    @Nested
    @DisplayName("BUS-G8: idempotencia y rechazo funcional: evento_id vs bus_evento_id, sin Inbox error para rechazos de negocio")
    class G8_IdempotenciaYRechazoFuncional {

        @Test
        @DisplayName("UNIQUE(consumer, event_id) solo puede aparecer en forma negada (confirmando su ausencia), nunca como regla vigente")
        void sinUniqueConsumerEventIdAfirmativo() throws IOException {
            // "No existe `UNIQUE(consumer, event_id)`" (physical-model.md §6.4) es
            // una negación legítima que documenta la ausencia deliberada de esa
            // clave, igual que "sin bus_definer"/"sin claim ni lease" en el resto
            // de la spec. Lo prohibido es la forma AFIRMATIVA (que la Inbox
            // reserva/usa esa UNIQUE), no la mención negada de su ausencia.
            Pattern mencion = Pattern.compile("(?i)UNIQUE\\s*\\(consumer,\\s*event_id\\)");
            Pattern negacionCercana = Pattern.compile("(?i)(no existe|nunca (se usa|existe)|nunca)\\s*[`\"]?$");
            List<Path> documentos = List.of(
                    eventingOperationsPath(), physicalModelPath(),
                    firmaNotificacionFalloPath(), firmaEventContractsPath());
            for (Path doc : documentos) {
                String md = leer(doc);
                var matcher = mencion.matcher(md);
                while (matcher.find()) {
                    String contexto = md.substring(Math.max(0, matcher.start() - 30), matcher.start());
                    assertThat(negacionCercana.matcher(contexto).find())
                            .as(doc + " :: mención de UNIQUE(consumer, event_id) sin negación previa: \""
                                    + contexto + "\"")
                            .isTrue();
                }
            }
        }

        @Test
        @DisplayName("el modelo físico distingue explícitamente bus_evento_id (cursor técnico) de evento_id (identidad lógica)")
        void distincionCursorTecnicoVsIdentidadLogica() throws IOException {
            String md = leer(physicalModelPath());
            assertThat(md).containsIgnoringCase("cursor técnico");
            assertThat(md).containsIgnoringCase("identidad lógica");
        }

        @Test
        @DisplayName("un rechazo funcional (ej. PKI inválida) cierra la Inbox en PROCESADO/si_error=FALSE, sin insertar fila en *_inbox_error")
        void rechazoFuncionalCierraEnProcesadoSinInboxError() throws IOException {
            String fallo = leer(firmaNotificacionFalloPath());
            assertThat(fallo).containsIgnoringCase("rechazo funcional");
            assertThat(fallo).contains("fal_inbox.status = PROCESADO");
            assertThat(fallo).contains("fal_inbox.si_error = FALSE");
            assertThat(fallo).containsIgnoringCase("no** se inserta ninguna fila en `fal_inbox_error`");
            assertThat(fallo).doesNotContainIgnoringCase("fal_inbox.status = ERROR_DEFINITIVO");

            String contratos = leer(firmaEventContractsPath());
            assertThat(contratos).containsIgnoringCase("rechazo funcional de dominio");
        }

        @Test
        @DisplayName("no queda ninguna referencia afirmativa a entrega central en cuarentena / QUARANTINED del bus central")
        void sinEntregaCentralEnCuarentena() throws IOException {
            // "sin cuarentena central"/"no existe...cuarentena central" son
            // negaciones legitimas (confirman la ausencia); solo la forma
            // afirmativa ("la entrega central queda QUARANTINED/en cuarentena")
            // representaria una reintroduccion de la arquitectura eliminada.
            Pattern entregaCentralQuedaEnCuarentena = Pattern.compile(
                    "(?i)entrega central[^.]{0,40}queda[^.]{0,40}(QUARANTINED|cuarentena)");
            List<Path> documentos = List.of(
                    eventingOperationsPath(), physicalModelPath(),
                    firmaNotificacionFalloPath(), firmaEventContractsPath(),
                    docsRoot.resolve("30-integrations/firma-binary-transfer.md"),
                    docsRoot.resolve("30-integrations/municipal-eventing.md"));
            for (Path doc : documentos) {
                String md = leer(doc);
                assertThat(md).as(doc.toString()).doesNotContainIgnoringCase("entrega central queda");
                assertThat(md).as(doc.toString()).doesNotContainIgnoringCase("entrega central del evento queda");
                assertThat(entregaCentralQuedaEnCuarentena.matcher(md).find())
                        .as(doc.toString()).isFalse();
            }
        }

        @Test
        @DisplayName("X-Api-Key no autentica el bus central: solo aparece asociado a transferencia binaria HTTP")
        void xApiKeyNuncaAutenticaElBusCentral() throws IOException {
            List<Path> documentos = List.of(eventingOperationsPath(), physicalModelPath(),
                    docsRoot.resolve("30-integrations/municipal-eventing.md"),
                    firmaEventContractsPath());
            // Ninguno de estos documentos debe afirmar que X-Api-Key autentica el bus
            // central; su unico uso legitimo es la transferencia binaria HTTP.
            for (Path doc : documentos) {
                String md = leer(doc);
                assertThat(md).as(doc.toString()).doesNotContainPattern(
                        "(?i)X-Api-Key\\s+autentica\\s+(exclusivamente\\s+)?(el|una operaci[oó]n sobre el)\\s+bus");
            }
        }
    }

    @Nested
    @DisplayName("BUS-G9: retry técnico de Outbox conserva evento_id; retención central solo por antigüedad")
    class G9_RetryYRetencion {

        @Test
        @DisplayName("el reintento manual de Outbox conserva el mismo evento_id y payload; un evento_id nuevo solo surge de un hecho de dominio nuevo")
        void retryDeOutboxConservaEventoId() throws IOException {
            String md = leer(eventingOperationsPath());
            assertThat(md).containsIgnoringCase("conserva el mismo");
            assertThat(md).containsIgnoringCase("evento_id");
            assertThat(md).doesNotContainIgnoringCase("después de ERROR_DEFINITIVO una corrección usa un nuevo");
            assertThat(md).containsIgnoringCase("nunca reescribe el `payload` ni genera un `evento_id` distinto");
        }

        @Test
        @DisplayName("la retención de bus_evento es exclusivamente por antigüedad de publicado_en, sin conocer el checkpoint de ningún consumidor")
        void retencionEsSoloPorAntiguedad() throws IOException {
            String md = leer(eventingOperationsPath());
            assertThat(md).containsIgnoringCase("purga es la");
            assertThat(md).containsIgnoringCase("antigüedad");
            assertThat(md).containsIgnoringCase("no tiene ni necesita ningún privilegio sobre");
            assertThat(md).doesNotContainIgnoringCase("todos los consumidores ya examinaron");
        }

        @Test
        @DisplayName("el relay confirma publicación por duplicate key sobre uq_bus_evento_evento_id, sin SELECT adicional del publisher")
        void reconciliacionDePublicacionSinSelectDelPublisher() throws IOException {
            String md = leer(eventingOperationsPath());
            assertThat(md).containsIgnoringCase("reutilizando el mismo");
            assertThat(md).containsIgnoringCase("evento_id");
            assertThat(md).doesNotContainIgnoringCase("verificando opcionalmente con un select posterior");
            String grants = leer(grantsPath());
            String ejecutable = quitarComentariosLinea(grants);
            // Los publishers nunca reciben SELECT sobre bus_evento (solo INSERT
            // column-level): confirmar publicacion nunca depende de leer la tabla.
            assertThat(ejecutable).doesNotContainPattern(
                    "(?i)GRANT\\s+SELECT\\s+ON\\s+sb_faltas_db\\.bus_evento\\s+TO\\s+'bus_publisher_(faltas|firmas)'");
        }
    }
}
