package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails de alcance de la composicion batch y del cierre de unicidades sobre
 * <b>codigo ejecutable</b> y sobre la spec canonica.
 *
 * <p>Los escaneos excluyen comentarios y Javadoc a proposito: la documentacion nombra lo
 * prohibido justamente para declararlo prohibido, de modo que buscar sobre el texto
 * completo produciria falsos positivos.
 *
 * <p>Cubre dos decisiones: {@code DECISION-OBS-BATCH-01}, que elimina la consulta por
 * acta de la lectura completa, y {@code DECISION-ACTA-UNIQUE-01}, que declara tecnicas
 * las dos unicidades de {@code fal_acta} y prohibe traducirlas a contrato funcional.
 */
@DisplayName("B1.5C/B2: guardrails del batch, las unicidades y la activacion transaccional")
class ActaBatchAlcanceGuardrailTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Path TEST = Paths.get("src/test/java");
    private static final Path SPEC = Paths.get("docs/spec-as-source");
    private static final Path RAIZ_REPO = Paths.get("../..");

    private static final Path PUERTO_OBS = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/repository/ObservacionRepository.java");
    private static final Path MEMORIA_OBS = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/repository/memory/InMemoryObservacionRepository.java");
    private static final Path JDBC_OBS = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcObservacionRepository.java");
    private static final Path JDBC_ACTA = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcActaRepository.java");
    private static final Path JDBC_PERSONA = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcPersonaRepository.java");
    private static final Path ACTA_SERVICE = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/application/service/ActaService.java");
    private static final Path QR_SERVICE = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/application/service/QrActaService.java");
    private static final Path ADVICE = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/web/GlobalFaltasControllerAdvice.java");
    private static final Path LABRAR_COMMAND = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/application/command/LabrarActaCommand.java");
    private static final Path PROFILE_IT = TEST.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/JdbcProfileContextIT.java");

    /** Lineas de codigo, sin comentarios de linea ni bloques de Javadoc. */
    private static String ejecutable(Path fuente) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String linea : Files.readAllLines(fuente, StandardCharsets.UTF_8)) {
            String t = linea.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            sb.append(linea).append('\n');
        }
        return sb.toString();
    }

    /**
     * Texto de la spec con los saltos de linea y los espacios repetidos colapsados a un
     * espacio. Asi las afirmaciones no dependen del punto exacto donde el documento corta
     * el parrafo ni del fin de linea del archivo.
     */
    private static String texto(Path fuente) throws IOException {
        return Files.readString(fuente, StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    /**
     * Cuerpo de un metodo, delimitado contando llaves desde la firma. Permite afirmar
     * sobre un metodo puntual en vez de sobre el archivo completo, que es lo que evita
     * confundir una invocacion legitima de otro metodo con la que se quiere prohibir.
     */
    private static String cuerpoDeMetodo(String codigo, String firma) {
        int inicio = codigo.indexOf(firma);
        assertThat(inicio).as("no se encontro la firma '%s'", firma).isNotNegative();
        int apertura = codigo.indexOf('{', inicio);
        assertThat(apertura).as("no se encontro el cuerpo de '%s'", firma).isNotNegative();
        int profundidad = 0;
        for (int i = apertura; i < codigo.length(); i++) {
            char c = codigo.charAt(i);
            if (c == '{') profundidad++;
            if (c == '}') {
                profundidad--;
                if (profundidad == 0) {
                    return codigo.substring(apertura, i + 1);
                }
            }
        }
        throw new AssertionError("cuerpo sin cerrar para la firma '" + firma + "'");
    }

    // =====================================================================
    @Nested @DisplayName("El puerto incorpora el batch sin perder las lecturas individuales")
    class ContratoDelPuerto {

        @Test @DisplayName("ObservacionRepository declara exactamente sus seis operaciones")
        void puertoConSeisOperaciones() {
            assertThat(Arrays.stream(ObservacionRepository.class.getDeclaredMethods())
                    .map(Method::getName))
                    .containsExactlyInAnyOrder("guardar", "buscarPorId", "listarPorEntidad",
                            "listarActivasPorEntidad", "listarPorEntidades", "desactivar");
        }

        @Test @DisplayName("las dos lecturas individuales siguen existiendo con su firma")
        void lecturasIndividualesIntactas() throws IOException {
            String puerto = ejecutable(PUERTO_OBS);
            assertThat(puerto)
                    .contains("List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId)")
                    .contains("List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId)");
        }

        @Test @DisplayName("el batch devuelve un mapa de propietario a historial, no una lista plana")
        void batchDevuelveMapaAgrupado() throws IOException {
            assertThat(ejecutable(PUERTO_OBS))
                    .contains("Map<Long, List<FalObservacion>> listarPorEntidades");
        }

        /**
         * El puerto no declara la operacion como {@code default}: obligar a las dos
         * implementaciones a escribirla es lo que impide que un adaptador quede resolviendo
         * el batch con N lecturas individuales sin que nadie lo note.
         */
        @Test @DisplayName("el batch no es un metodo default del puerto")
        void batchNoEsDefault() throws IOException {
            assertThat(ejecutable(PUERTO_OBS))
                    .doesNotContain("default Map<Long, List<FalObservacion>> listarPorEntidades");
        }

        @Test @DisplayName("los dos adaptadores de produccion implementan el batch")
        void ambosAdaptadoresImplementanElBatch() throws IOException {
            for (Path adaptador : List.of(MEMORIA_OBS, JDBC_OBS)) {
                assertThat(ejecutable(adaptador))
                        .as("%s debe implementar el batch", adaptador.getFileName())
                        .contains("public Map<Long, List<FalObservacion>> listarPorEntidades(");
            }
        }

        @Test @DisplayName("no hay una tercera implementacion del puerto en produccion")
        void soloDosImplementaciones() throws IOException {
            List<String> implementaciones = new ArrayList<>();
            for (Path p : fuentesDeProduccion()) {
                if (ejecutable(p).contains("implements ObservacionRepository")) {
                    implementaciones.add(p.getFileName().toString());
                }
            }
            assertThat(implementaciones).containsExactlyInAnyOrder(
                    "InMemoryObservacionRepository.java", "JdbcObservacionRepository.java");
        }
    }

    // =====================================================================
    @Nested @DisplayName("El SQL del batch es explicito y no dinamico")
    class SqlDelBatch {

        @Test @DisplayName("el batch usa la expansion de colecciones y no concatena ids")
        void batchConParametroDeColeccion() throws IOException {
            String codigo = ejecutable(JDBC_OBS);
            assertThat(codigo)
                    .as("el IN se resuelve con un parametro nombrado de coleccion")
                    .contains("entidad_id IN (:entidadIds)");
            assertThat(codigo)
                    .as("los ids nunca se concatenan al texto de la sentencia")
                    .doesNotContain("String.join")
                    .doesNotContain("StringBuilder")
                    .doesNotContain("collect(Collectors.joining");
        }

        @Test @DisplayName("el batch ordena por entidad_id y despues por el orden canonico")
        void batchConOrdenCanonico() throws IOException {
            assertThat(ejecutable(JDBC_OBS))
                    .contains("ORDER BY entidad_id ASC, fh_alta ASC, id ASC");
        }

        @Test @DisplayName("el adaptador de observaciones no usa SELECT *, REPLACE ni upsert")
        void sinSqlProhibido() throws IOException {
            String codigo = ejecutable(JDBC_OBS);
            for (String prohibido : new String[] {
                    "SELECT *", "ON DUPLICATE KEY", "REPLACE INTO", "DELETE FROM", "TRUNCATE"}) {
                assertThat(codigo)
                        .as("SQL prohibido en el adaptador de observaciones: %s", prohibido)
                        .doesNotContain(prohibido);
            }
        }

        @Test @DisplayName("el batch reutiliza la unica lista de columnas del adaptador")
        void batchReutilizaLaListaDeColumnas() throws IOException {
            String codigo = ejecutable(JDBC_OBS);
            assertThat(codigo).contains("SQL_POR_ENTIDADES = \"SELECT \" + COLUMNAS");
        }
    }

    // =====================================================================
    @Nested @DisplayName("listarTodas dejo de consultar acta por acta")
    class SinConsultaPorActa {

        /**
         * La afirmacion se hace sobre el cuerpo real de {@code listarTodas}, no sobre el
         * archivo completo: {@code componer} sigue existiendo legitimamente para las dos
         * lecturas por clave, de modo que buscar el identificador en todo el archivo no
         * distinguiria el caso correcto del incorrecto.
         */
        @Test @DisplayName("el cuerpo de listarTodas no invoca la lectura individual de observaciones")
        void listarTodasSinLecturaIndividual() throws IOException {
            String cuerpo = cuerpoDeMetodo(ejecutable(JDBC_ACTA), "public List<FalActa> listarTodas()");

            assertThat(cuerpo)
                    .as("listarTodas resuelve todas las observaciones con la operacion batch")
                    .contains("observaciones.listarPorEntidades(EntidadTipoObservada.ACTA, ids)");
            assertThat(cuerpo)
                    .as("listarTodas no pide el historial de un propietario por vez")
                    .doesNotContain("listarPorEntidad(")
                    .doesNotContain("listarActivasPorEntidad(")
                    .doesNotContain("componer(");
        }

        @Test @DisplayName("listarTodas ejecuta una sola sentencia propia y compone fuera del recorrido")
        void composicionFueraDelRecorrido() throws IOException {
            String cuerpo = cuerpoDeMetodo(ejecutable(JDBC_ACTA), "public List<FalActa> listarTodas()");

            assertThat(cuerpo.split("jdbc\\.sql\\(", -1).length - 1)
                    .as("una unica sentencia de fal_acta dentro de listarTodas")
                    .isOne();
            assertThat(cuerpo)
                    .as("el batch se pide despues de materializar las actas, no dentro del RowMapper")
                    .contains("List<ActaMapeada> mapeadas = jdbc.sql(SQL_TODAS)");
            assertThat(cuerpo.indexOf("mapeadas = jdbc.sql"))
                    .as("la lectura de actas ocurre antes de la de observaciones")
                    .isLessThan(cuerpo.indexOf("listarPorEntidades"));
        }

        @Test @DisplayName("sin actas, listarTodas corta antes de pedir observaciones")
        void sinActasNoPideObservaciones() throws IOException {
            String cuerpo = cuerpoDeMetodo(ejecutable(JDBC_ACTA), "public List<FalActa> listarTodas()");

            assertThat(cuerpo)
                    .contains("if (mapeadas.isEmpty()) {")
                    .contains("return List.of();");
            assertThat(cuerpo.indexOf("mapeadas.isEmpty()"))
                    .as("el corte por lista vacia precede a la invocacion batch")
                    .isLessThan(cuerpo.indexOf("listarPorEntidades"));
        }

        @Test @DisplayName("el adaptador del acta no escribe SQL propio sobre fal_observacion")
        void sinSqlDeObservacionEnElAdaptadorDelActa() throws IOException {
            assertThat(ejecutable(JDBC_ACTA))
                    .as("el contrato de fal_observacion tiene una sola sede")
                    .doesNotContain("fal_observacion");
        }

        @Test @DisplayName("las dos lecturas por clave conservan la composicion individual")
        void lecturasPorClaveConservanLaComposicionIndividual() throws IOException {
            String codigo = ejecutable(JDBC_ACTA);
            String componer = cuerpoDeMetodo(codigo, "private FalActa componer(ActaMapeada mapeada)");

            assertThat(componer)
                    .as("una sola acta se compone con la lectura individual")
                    .contains("observaciones.listarPorEntidad(EntidadTipoObservada.ACTA, mapeada.id())");
            for (String porClave : new String[] {
                    "public Optional<FalActa> buscarPorId(Long id)",
                    "public Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico)"}) {
                assertThat(cuerpoDeMetodo(codigo, porClave))
                        .as("%s conserva la composicion individual", porClave)
                        .contains(".map(this::componer)")
                        .doesNotContain("listarPorEntidades");
            }
        }
    }

    // =====================================================================
    @Nested @DisplayName("Las unicidades del acta no se traducen a contrato funcional")
    class UnicidadesTecnicas {

        @Test @DisplayName("no existe ninguna excepcion de dominio de acta duplicada")
        void sinExcepcionFuncionalDeDuplicado() throws IOException {
            for (String prohibida : new String[] {
                    "ActaDuplicadaException", "ConflictoActaException",
                    "IdTecnicoDuplicadoException", "CodigoQrDuplicadoException"}) {
                assertThat(ocurrenciasEnProduccion(prohibida))
                        .as("excepcion funcional prohibida para una unicidad tecnica: %s", prohibida)
                        .isEmpty();
            }
        }

        @Test @DisplayName("el advice global no traduce DuplicateKeyException a un conflicto HTTP")
        void adviceSinMapeoDeDuplicateKey() throws IOException {
            assertThat(ejecutable(ADVICE))
                    .as("una violacion de estos UNIQUE no es un 409 del contrato publico")
                    .doesNotContain("DuplicateKeyException")
                    .doesNotContain("DataIntegrityViolationException");
        }

        @Test @DisplayName("el comando de alta no expone id_tecnico ni codigo_qr")
        void comandoDeAltaSinValoresGeneradosPorElSistema() throws IOException {
            String comando = ejecutable(LABRAR_COMMAND);
            assertThat(comando)
                    .as("idTecnico es opcional para el cliente (DECISION-ACTA-ID-TECNICO-QR-01); "
                            + "codigoQr sigue siendo exclusivamente del servidor")
                    .doesNotContain("uuidTecnico")
                    .doesNotContain("codigoQr");
        }

        @Test @DisplayName("el alta resuelve id_tecnico: genera si esta ausente, valida y conserva si vino informado")
        void altaGeneraElIdTecnico() throws IOException {
            assertThat(ejecutable(ACTA_SERVICE))
                    .contains("String uuidTecnico = resolverIdTecnico(cmd.idTecnico());")
                    .contains("UUID.randomUUID().toString()");
        }

        @Test @DisplayName("no se reintenta con otro UUID ante una colision")
        void sinReintentoSilencioso() throws IOException {
            String codigo = ejecutable(ACTA_SERVICE);
            assertThat(codigo)
                    .as("una colision es un defecto, no un caso de uso con reintento")
                    .doesNotContain("catch (DuplicateKeyException");
            assertThat(ocurrenciasEnProduccion("reintentarConNuevoUuid")).isEmpty();
        }

        @Test @DisplayName("el adaptador del acta no traduce las violaciones de los dos UNIQUE")
        void adaptadorNoTraduceLosUnique() throws IOException {
            String codigo = ejecutable(JDBC_ACTA);
            assertThat(codigo)
                    .as("la DuplicateKeyException del motor viaja tal cual, con el nombre de la constraint")
                    .doesNotContain("DuplicateKeyException");
        }

        /**
         * El delta del doble de prueba queda declarado, no descubierto: el adaptador
         * InMemory no verifica estas dos unicidades y eso es parte de la decision.
         */
        @Test @DisplayName("el adaptador InMemory del acta no simula las dos unicidades tecnicas")
        void inMemoryNoSimulaLasUnicidades() throws IOException {
            String codigo = ejecutable(MAIN.resolve(
                    "ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaRepository.java"));
            assertThat(codigo)
                    .as("la unicidad la sostiene el motor; el doble no la imita")
                    .doesNotContain("uq_fal_acta_id_tecnico")
                    .doesNotContain("uq_fal_acta_codigo_qr");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Alcance: la activacion de WAVE 1A2-B2 quedo completa y acotada")
    class AlcanceDelSlice {

        /**
         * Asercion invertida en WAVE 1A2-B2. Hasta B1.5C este caso verificaba que los dos
         * candidatos siguieran sin ser beans; desde B2 verifica que sean beans
         * <b>exclusivamente</b> bajo el perfil {@code jdbc}, que es la propiedad que reemplaza
         * a la anterior. Sin la restriccion de perfil el modo InMemory quedaria con dos
         * implementaciones por puerto.
         */
        @Test @DisplayName("los dos adaptadores JDBC son beans solo bajo el perfil jdbc")
        void candidatosActivadosSoloBajoPerfilJdbc() throws IOException {
            for (Path candidato : List.of(JDBC_ACTA, JDBC_PERSONA)) {
                String codigo = ejecutable(candidato);
                assertThat(codigo)
                        .as("%s debe ser bean exclusivamente bajo jdbc", candidato.getFileName())
                        .contains("@Repository")
                        .contains("@Profile(\"jdbc\")");
                assertThat(codigo)
                        .as("%s no puede esconder ambiguedad con @Primary", candidato.getFileName())
                        .doesNotContain("@Primary");
            }
        }

        /**
         * Contraparte InMemory de la activacion: los cuatro puertos que B2 migro quedan
         * restringidos a {@code !jdbc}. Con {@code jdbc} debe existir exactamente una
         * implementacion por puerto, y esa unicidad es lo que hace innecesario {@code @Primary}.
         */
        @Test @DisplayName("los InMemory de los puertos activados quedaron restringidos a !jdbc")
        void inMemoryDeLosPuertosActivadosRestringidos() throws IOException {
            for (String ruta : List.of(
                    "ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaRepository.java",
                    "ar/gob/malvinas/faltas/core/repository/memory/InMemoryPersonaRepository.java",
                    "ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaEvidenciaRepository.java",
                    "ar/gob/malvinas/faltas/core/repository/memory/InMemoryActaSnapshotRepository.java")) {
                String codigo = ejecutable(MAIN.resolve(ruta));
                assertThat(codigo)
                        .as("%s debe quedar fuera del perfil jdbc", ruta)
                        .contains("@Repository")
                        .contains("@Profile(\"!jdbc\")");
            }
        }

        /**
         * Los dos adaptadores JDBC que B2 creo para completar la frontera transaccional. Sin
         * ellos el alta habria escrito evidencias y snapshot en memoria bajo perfil
         * {@code jdbc}, y esas escrituras habrian sobrevivido al rollback.
         */
        @Test @DisplayName("los participantes que faltaban tienen adaptador JDBC transaccional")
        void participantesFaltantesImplementados() throws IOException {
            for (String ruta : List.of(
                    "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcActaEvidenciaRepository.java",
                    "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcActaSnapshotRepository.java")) {
                String codigo = ejecutable(MAIN.resolve(ruta));
                assertThat(codigo)
                        .as("%s debe ser bean solo bajo jdbc", ruta)
                        .contains("@Repository")
                        .contains("@Profile(\"jdbc\")");
                assertThat(codigo)
                        .as("%s usa el JdbcClient del DataSource Spring, sin conexiones manuales", ruta)
                        .contains("JdbcClient")
                        .doesNotContain("DriverManager")
                        .doesNotContain("getConnection(");
                assertThat(codigo)
                        .as("%s no abre transacciones propias ni introduce otro transaction manager", ruta)
                        .doesNotContain("@Transactional")
                        .doesNotContain("PlatformTransactionManager")
                        .doesNotContain("TransactionTemplate");
                assertThat(codigo)
                        .as("%s sin SELECT *, sin upsert de motor", ruta)
                        .doesNotContain("SELECT *")
                        .doesNotContain("ON DUPLICATE KEY")
                        .doesNotContain("REPLACE INTO");
            }
        }

        /**
         * Asercion invertida en WAVE 1A2-B2, el slice que implementa
         * {@code DECISION_OBS-TX-01}.
         */
        @Test @DisplayName("ActaService declara la frontera transaccional sobre labrar")
        void fronteraTransaccionalPresente() throws IOException {
            String codigo = ejecutable(ACTA_SERVICE);
            assertThat(codigo)
                    .as("la frontera transaccional del alta debe estar sobre el metodo publico"
                            + " (Wave 4 PARTE-3A ajusta el isolation level a READ_COMMITTED,"
                            + " ver Javadoc de ActaService.labrar; sigue sobre el mismo metodo)")
                    .contains("@Transactional(isolation = org.springframework.transaction.annotation"
                            + ".Isolation.READ_COMMITTED)\n    public ComandoResultado labrar(LabrarActaCommand cmd)");
            assertThat(codigo)
                    .as("sin propagacion nueva, transacciones anidadas ni exclusion de rollback")
                    .doesNotContain("REQUIRES_NEW")
                    .doesNotContain("noRollbackFor")
                    .doesNotContain("TransactionSynchronization");
        }

        @Test @DisplayName("la topologia de perfiles de los adaptadores de observaciones no cambio")
        void topologiaDePerfilesIntacta() throws IOException {
            assertThat(ejecutable(JDBC_OBS))
                    .contains("@Repository")
                    .contains("@Profile(\"jdbc\")");
            assertThat(ejecutable(MEMORIA_OBS))
                    .contains("@Repository")
                    .contains("@Profile(\"!jdbc\")");
        }

        /**
         * Sello relevado en WAVE 1A2-B2. Hasta B1.5C el IT de topologia no podia cambiar y su
         * SHA-256 lo verificaba; la activacion de los cuatro puertos <b>exige</b> modificarlo,
         * porque hasta B1.5C afirmaba que {@code ActaRepository} lo resolvia el adaptador
         * InMemory. Un hash congelado y un IT que debe cambiar no pueden ser ciertos a la vez,
         * de modo que el sello se reemplaza por afirmaciones sobre lo que el IT debe verificar.
         */
        @Test @DisplayName("JdbcProfileContextIT verifica la topologia activada, no la anterior")
        void itDeTopologiaActualizado() throws Exception {
            String codigo = ejecutable(PROFILE_IT);
            assertThat(codigo)
                    .as("el IT debe afirmar que los cuatro puertos activados resuelven a JDBC")
                    .contains("JdbcActaRepository")
                    .contains("JdbcPersonaRepository")
                    .contains("JdbcActaEvidenciaRepository")
                    .contains("JdbcActaSnapshotRepository");
            assertThat(codigo)
                    .as("y que la frontera transaccional del alta esta efectivamente proxificada")
                    .contains("ActaService");
        }

        @Test @DisplayName("el QR sigue cerrado: sin rotacion ni regeneracion")
        void qrSigueCerrado() throws IOException {
            String codigo = ejecutable(QR_SERVICE);
            assertThat(codigo)
                    .doesNotContain("rotarQr")
                    .doesNotContain("generarQr");
            assertThat(codigo).contains("public QrTokenEmitido emitirTokenPara(");
        }

        @Test @DisplayName("la remediacion fisica de fal_observacion sigue pendiente y sin tocar")
        void remediacionFisicaNoAdelantada() throws IOException {
            List<String> ddlNuevo = new ArrayList<>();
            for (String prohibido : new String[] {
                    "DROP TRIGGER", "DROP FOREIGN KEY", "MODIFY COLUMN origen_observacion",
                    "ALTER TABLE fal_observacion", "ALTER TABLE fal_acta"}) {
                ddlNuevo.addAll(ocurrenciasEnProduccion(prohibido));
            }
            assertThat(ddlNuevo)
                    .as("este alcance no ejecuta ni prepara DDL")
                    .isEmpty();
        }

        @Test @DisplayName("no se agrego DDL nuevo al paquete de scripts")
        void sinScriptsDdlNuevos() throws IOException {
            Path ddl = RAIZ_REPO.resolve("database/ddl");
            if (!Files.isDirectory(ddl)) {
                return;
            }
            try (var s = Files.list(ddl)) {
                List<String> conBatch = s
                        .filter(p -> p.getFileName().toString().toLowerCase().contains("batch")
                                || p.getFileName().toString().toLowerCase().contains("unique-acta"))
                        .map(p -> p.getFileName().toString())
                        .toList();
                assertThat(conBatch)
                        .as("ni la composicion batch ni el cierre de unicidades requieren DDL")
                        .isEmpty();
            }
        }
    }

    // =====================================================================
    @Nested @DisplayName("Las dos decisiones estan registradas en la spec canonica")
    class DecisionesEnLaSpec {

        @Test @DisplayName("observaciones.md registra el contrato batch con sus diez reglas")
        void specRegistraElContratoBatch() throws IOException {
            String spec = texto(SPEC.resolve("40-domain/observaciones.md"));
            assertThat(spec).contains("DECISION-OBS-BATCH-01");
            assertThat(spec).contains("listarPorEntidades");
            assertThat(spec)
                    .as("la spec fija el orden canonico del batch")
                    .contains("cada historial conserva el orden canonico fh_alta ASC, id ASC");
            assertThat(spec)
                    .as("la spec fija que la coleccion vacia no consulta")
                    .contains("una coleccion vacia no ejecuta consulta alguna contra el motor");
            assertThat(spec)
                    .as("la spec fija que se incluyen activas e inactivas")
                    .contains("se incluyen activas e inactivas");
            assertThat(spec)
                    .as("la spec fija que la ausencia representa el historial vacio")
                    .contains("puede estar ausente del mapa");
            assertThat(spec)
                    .as("la spec declara que no se crea indice ni se ejecuta DDL")
                    .contains("No se crea ningún índice nuevo y no se ejecuta DDL por esta decisión.");
            assertThat(spec)
                    .as("la spec nombra el indice que sirve el batch sin modificarlo")
                    .contains("ix_obs_entidad");
            assertThat(spec)
                    .as("la spec prohibe el SQL dinamico y el SELECT *")
                    .contains("Quedan prohibidos el `SELECT *`, la concatenación de identificadores");
        }

        @Test @DisplayName("ddl-decisions.md registra DECISION-ACTA-UNIQUE-01 con sus prohibiciones")
        void specRegistraLasUnicidades() throws IOException {
            String spec = texto(SPEC.resolve("50-persistence/ddl-decisions.md"));
            assertThat(spec).contains("DECISION-ACTA-UNIQUE-01");
            assertThat(spec).contains("uq_fal_acta_id_tecnico");
            assertThat(spec).contains("uq_fal_acta_codigo_qr");
            assertThat(spec)
                    .as("la decision prohibe crear excepciones de dominio")
                    .contains("ActaDuplicadaException")
                    .contains("ConflictoActaException");
            assertThat(spec)
                    .as("la decision declara que no se agrega contrato HTTP")
                    .contains("No existe ni se agrega un mapeo a `409 CONFLICT`");
            assertThat(spec)
                    .as("la decision declara el delta aceptado del doble de prueba")
                    .contains("`InMemoryActaRepository` **no simula** estas dos unicidades");
            assertThat(spec)
                    .as("la decision acota el alcance de la unicidad")
                    .contains("**No se agrega** `UNIQUE` sobre `nro_acta`");
            assertThat(spec)
                    .as("la decision prohibe el reintento silencioso")
                    .contains("**no se reintenta** con otro UUID");
        }
    }

    // =====================================================================
    @Nested @DisplayName("El contrato batch se cumple sobre el oraculo de paridad")
    class ContratoEjercidoSobreElOraculo {

        @Test @DisplayName("el contrato completo se verifica sobre el adaptador InMemory")
        void contratoEjercido() {
            ObservacionRepository repo = new InMemoryObservacionRepository();
            repo.guardar(new FalObservacion(null, EntidadTipoObservada.ACTA, 7L, null,
                    "inicial", ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion.USUARIO,
                    java.time.LocalDateTime.of(2026, 1, 1, 0, 0), "U"));

            assertThat(repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of())).isEmpty();

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(7L, 7L, 8L));
            assertThat(historiales).containsOnlyKeys(7L);
            assertThat(historiales.get(7L)).hasSize(1);

            assertThatThrownBy(() -> repo.listarPorEntidades(null, List.of(7L)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repo.listarPorEntidades(EntidadTipoObservada.ACTA, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repo.listarPorEntidades(
                    EntidadTipoObservada.ACTA, Arrays.asList(7L, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =====================================================================

    private static List<Path> fuentesDeProduccion() throws IOException {
        try (var s = Files.walk(MAIN)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    /** Archivos de produccion cuyo codigo ejecutable contiene el identificador. */
    private static List<String> ocurrenciasEnProduccion(String identificador) throws IOException {
        List<String> encontrados = new ArrayList<>();
        for (Path p : fuentesDeProduccion()) {
            if (ejecutable(p).contains(identificador)) {
                encontrados.add(p.toString());
            }
        }
        return encontrados;
    }
}
