package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails documentales de spec-as-source (auditoria transversal final).
 *
 * Verifica, contra el arbol de archivos real de
 * {@code docs/spec-as-source} (fuera de {@code handoff/**}):
 *
 * <ul>
 *   <li>G-1: el registro documental esta completo, sin duplicados y con clasificacion/autoridad validas.</li>
 *   <li>G-2: todos los links relativos Markdown resuelven a un archivo existente.</li>
 *   <li>G-3: cero identificadores "GAP-" en cualquier documento fuera de handoff.</li>
 *   <li>G-4: los siete comandos canonicos CMD-FALLO-001..007 aparecen exactamente una vez
 *       como heading de nivel 2 en {@code fallo-command-contracts.md}, y no existe un octavo.</li>
 *   <li>G-5: los documentos NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN no usan lenguaje
 *       de diario de implementacion por slices.</li>
 *   <li>G-6: los documentos NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN no usan identificadores
 *       obsoletos derivados de {@code EnumGuardrailTest} y los enums vigentes.</li>
 *   <li>G-7: el gate formal {@code READY_FOR_DDL} esta declarado exactamente una vez en
 *       {@code 101-auditoria-pre-jdbc-mariadb.md} y cero veces {@code Estado: BLOCKED}.</li>
 *   <li>G-8: el README enlaza al registro documental y a los documentos de preparacion MariaDB.</li>
 *   <li>G-9: paridad exacta (valores y, cuando el enum expone {@code codigo()}, codigos)
 *       entre siete enums productivos y su tabla propietaria en la spec.</li>
 *   <li>G-10: {@code APERAZ}/{@code RECHAZADA} nunca se documentan como firmeza automatica.</li>
 *   <li>G-11: {@code 03-comandos-precondiciones-efectos.md} no duplica los contratos legacy
 *       de firmeza y enlaza a la familia canonica de comandos de fallo.</li>
 *   <li>G-12: {@code 109} y {@code 110} se declaran complementarios, sin categorias
 *       historicas de discrepancia ni afirmaciones de fuente unica/reemplazo.</li>
 *   <li>G-13: la politica de seguridad de {@code CMD-FALLO-003} esta documentada como
 *       identidad tecnica, no JWT de usuario final; no hay una regla comun a los siete comandos.</li>
 *   <li>G-14: {@code FALLO_CONDENATORIO_PAGADO} esta documentado como {@code LEGACY_RESERVED},
 *       con {@code CONDENA_FIRME_PAGADA} como resultado canonico y {@code DECISION_DDL-RF-005} en {@code 110}.</li>
 *   <li>G-15: el contrato de pago de condena es coherente entre {@code 02}, {@code 03} y
 *       {@code 05}: {@code PCOCNF} se registra siempre que se superan las precondiciones,
 *       los bloqueantes materiales activos solo impiden {@code CIERRA} (nunca rechazan la
 *       confirmacion), {@code 03} no duplica el contrato de {@code InformarPagoCondenaCommand}
 *       y {@code 05} no exige {@code 422} por bloqueantes activos en confirmar.</li>
 *   <li>G-16: la estrategia de enums persistibles documentada en {@code 109}/{@code 110}/{@code 102}
 *       es coherente, por reflexion, con los enums verificados en G-9: los enums sin
 *       {@code codigo()} se clasifican {@code NO_EXPLICIT_CODE} y quedan sujetos a
 *       {@code DECISION_DDL-ENUM-01}; ningun documento afirma que "todo enum persistible
 *       tiene codigo numerico" ni fija columnas de esos enums como {@code SMALLINT} cerrado;
 *       {@code ordinal()} solo aparece como prohibicion explicita en la misma linea.</li>
 *   <li>G-17: los documentos clasificados vigentes ({@code 99}, {@code 102}, {@code 103}, {@code 03})
 *       estan libres de cronologia historica de slices/fases/builds, y {@code 103} esta
 *       clasificado {@code SUPPORTING_CURRENT} en su banner y en el registro.</li>
 *   <li>G-18: el gate {@code READY_FOR_DDL} de {@code 101} solo se considera valido si las
 *       condiciones de G-15, G-16 y G-17 se cumplen simultaneamente; la mera presencia del
 *       literal no es suficiente.</li>
 * </ul>
 *
 * No usa Mockito. Solo lee archivos reales del arbol de la spec.
 */
@DisplayName("Guardrails documentales de spec-as-source")
class SpecAsSourceGuardrailTest {

    private static final Set<String> CLASIFICACIONES_VALIDAS = Set.of(
            "NORMATIVE", "SUPPORTING_CURRENT", "HISTORICAL", "PRE_DDL_PLAN");

    private static final Set<String> AUTORIDADES_VALIDAS = Set.of("YES", "SUPPORTING", "NO");

    private static final Set<String> CLASIFICACIONES_SIN_TEMPORALIDAD_SLICE = Set.of(
            "NORMATIVE", "SUPPORTING_CURRENT", "PRE_DDL_PLAN");

    private static final List<String> TERMINOS_PROHIBIDOS = List.of(
            "PAGCON", "ACTCER", "APELAC", "DRVEXT", "D3_DOCUMENTAL");

    /**
     * Marcadores de negacion/prohibicion. Un termino obsoleto solo esta documentado
     * legitimamente (como prohibicion explicita) si aparece cerca de uno de estos
     * marcadores; de lo contrario se interpreta como uso semantico vigente y el test falla.
     */
    private static final List<String> MARCADORES_NEGACION = List.of(
            "no existe", "no es ", "no se ", "prohibid", "inexistente",
            "rechaz", "eliminad", "reemplazad", "legacy", "cero ocurrencias", "no aplica");

    private static final Pattern PATRON_SLICE_NUMERO =
            Pattern.compile("\\bSlice\\s+\\d", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_SLICE_FUTURO =
            Pattern.compile("\\bslice\\s+futur", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_SLICE_POSTERIOR =
            Pattern.compile("\\bslice\\s+posterior", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_ESTADO_ACTUAL_SLICE =
            Pattern.compile("\\bestado\\s+actual\\s+del\\s+slice", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATRON_LINK_MARKDOWN = Pattern.compile("\\[[^\\]]*]\\(([^)]+)\\)");
    private static final Pattern PATRON_FENCE = Pattern.compile("^\\s*```");
    private static final Pattern PATRON_HEADING_CMD_FALLO =
            Pattern.compile("^##\\s+(CMD-FALLO-\\d{3})\\b");
    private static final Pattern PATRON_FILA_TABLA = Pattern.compile("^\\s*\\|.*\\|\\s*$");

    private static final Pattern PATRON_8F = Pattern.compile("\\b8F-");
    private static final Pattern PATRON_CERRADO_EN = Pattern.compile("(?i)cerrado en\\b");
    private static final Pattern PATRON_REEMPLAZA_109 = Pattern.compile("(?i)(?<!no )reemplaza a 109\\b");
    private static final Pattern PATRON_109_HISTORIAL = Pattern.compile("(?i)109 queda como historial");
    private static final Pattern PATRON_FECHA_ORIGINAL = Pattern.compile("(?i)^Fecha original:");
    private static final Pattern PATRON_BUILD_BASE_HISTORICO =
            Pattern.compile("(?i)Build base confirmado en esa auditor");

    /** Tokens de trabajo prohibidos (G-3 adicional). Sensible a mayusculas para no rechazar "Todo". */
    private static final List<String> TOKENS_DE_TRABAJO_PROHIBIDOS = List.of("GAP-", "TODO", "FIXME", "TBD");

    /** Categorias historicas de discrepancia (110, G-12). */
    private static final List<String> CATEGORIAS_HISTORICAS_DISCREPANCIA = List.of(
            "FALTA_EN_INMEMORY", "IDENTIDAD_INCOMPATIBLE", "SEMANTICA_INCOMPATIBLE",
            "TIPO_INCOMPATIBLE", "RELACION_INCOMPLETA");

    private static final Pattern PATRON_HEADING_CIERRE = Pattern.compile("^#{1,6}\\s*CIERRE-");
    private static final Pattern PATRON_HEADING_FIX = Pattern.compile("^#{1,6}\\s*FIX-");
    private static final Pattern PATRON_HEADING_R_HISTORICO = Pattern.compile("^#{1,6}\\s*R-[0-9]");
    private static final Pattern PATRON_HEADING_CUALQUIERA = Pattern.compile("^#{1,6}\\s");

    private static Path specRoot;

    @BeforeAll
    static void localizarSpecRoot() {
        Path candidato = Paths.get("docs", "spec-as-source");
        if (!Files.isDirectory(candidato)) {
            throw new IllegalStateException(
                    "No se encontro 'docs/spec-as-source' desde el working directory Maven actual: "
                            + Paths.get("").toAbsolutePath()
                            + ". Este test debe ejecutarse con cwd = backend/api-faltas-core.");
        }
        specRoot = candidato;
    }

    // -------------------------------------------------------------------
    // Utilidades de arbol de archivos
    // -------------------------------------------------------------------

    private static List<Path> listarMarkdownFueraDeHandoff() throws IOException {
        try (Stream<Path> stream = Files.walk(specRoot)) {
            return stream
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !esRutaHandoff(p))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static boolean esRutaHandoff(Path p) {
        Path relativo = specRoot.relativize(p);
        for (Path parte : relativo) {
            if (parte.toString().equals("handoff")) {
                return true;
            }
        }
        return false;
    }

    private static String leer(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    private static String pathRegistroDe(Path archivoAbsoluto) {
        return specRoot.relativize(archivoAbsoluto).toString().replace('\\', '/');
    }

    /**
     * Extrae el texto desde la primera linea que matchea {@code inicio} (inclusive) hasta
     * la siguiente linea que es un heading Markdown (exclusive), o el fin del archivo.
     */
    private static String extraerSeccion(List<String> lineas, Pattern inicio) {
        int idx = -1;
        for (int i = 0; i < lineas.size(); i++) {
            if (inicio.matcher(lineas.get(i)).find()) {
                idx = i;
                break;
            }
        }
        assertThat(idx).as("No se encontro el marcador de seccion '%s'", inicio.pattern()).isGreaterThanOrEqualTo(0);
        StringBuilder sb = new StringBuilder();
        sb.append(lineas.get(idx)).append('\n');
        for (int i = idx + 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (PATRON_HEADING_CUALQUIERA.matcher(linea).find()) {
                break;
            }
            sb.append(linea).append('\n');
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------
    // Parseo del registro documental
    // -------------------------------------------------------------------

    private record FilaRegistro(String path, String clasificacion, String autoridad, String estado) {
    }

    private static List<FilaRegistro> parsearRegistro() throws IOException {
        Path registro = specRoot.resolve("00-governance").resolve("spec-document-registry.md");
        assertThat(Files.isRegularFile(registro))
                .as("Debe existir 00-governance/spec-document-registry.md")
                .isTrue();
        String contenido = leer(registro);
        Pattern filaPattern = Pattern.compile(
                "^\\|\\s*`([^`]+)`\\s*\\|\\s*(\\S+)\\s*\\|\\s*(\\S+)\\s*\\|\\s*(\\S+)\\s*\\|",
                Pattern.MULTILINE);
        Matcher m = filaPattern.matcher(contenido);
        List<FilaRegistro> filas = new ArrayList<>();
        while (m.find()) {
            String path = m.group(1).trim();
            String clasificacion = m.group(2).trim();
            String autoridad = m.group(3).trim();
            String estado = m.group(4).trim();
            filas.add(new FilaRegistro(path, clasificacion, autoridad, estado));
        }
        return filas;
    }

    // -------------------------------------------------------------------
    // Banner documental ("> **Estado documental:** X" / "> **Autoridad DDL:** Y")
    // -------------------------------------------------------------------

    private record Banner(String clasificacion, String autoridad) {
    }

    private static final Pattern PATRON_BANNER_CLASIFICACION =
            Pattern.compile("\\*\\*Estado documental:\\*\\*\\s*(\\S+)");
    private static final Pattern PATRON_BANNER_AUTORIDAD =
            Pattern.compile("\\*\\*Autoridad DDL:\\*\\*\\s*(\\S+)");

    /** Devuelve null si el archivo no tiene banner (formato especial, p. ej. README.md). */
    private static Banner leerBanner(Path archivo) throws IOException {
        List<String> primerasLineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        String clasificacion = null;
        String autoridad = null;
        int limite = Math.min(primerasLineas.size(), 10);
        for (int i = 0; i < limite; i++) {
            Matcher mc = PATRON_BANNER_CLASIFICACION.matcher(primerasLineas.get(i));
            if (mc.find()) {
                clasificacion = mc.group(1);
            }
            Matcher ma = PATRON_BANNER_AUTORIDAD.matcher(primerasLineas.get(i));
            if (ma.find()) {
                autoridad = ma.group(1);
            }
        }
        if (clasificacion == null || autoridad == null) {
            return null;
        }
        return new Banner(clasificacion, autoridad);
    }

    private static String autoridadEsperadaPara(String clasificacion) {
        return switch (clasificacion) {
            case "NORMATIVE" -> "YES";
            case "SUPPORTING_CURRENT", "PRE_DDL_PLAN" -> "SUPPORTING";
            case "HISTORICAL" -> "NO";
            default -> null;
        };
    }

    // -------------------------------------------------------------------
    // Parseo generico de tablas "propietarias" de un enum (G-9)
    // -------------------------------------------------------------------

    private record FilaTablaEnum(String valor, String codigo) {
    }

    private static String limpiarCelda(String celda) {
        return celda.trim().replace("`", "");
    }

    private static List<String> partirCeldas(String filaTabla) {
        String sinBordes = filaTabla.trim();
        if (sinBordes.startsWith("|")) {
            sinBordes = sinBordes.substring(1);
        }
        if (sinBordes.endsWith("|")) {
            sinBordes = sinBordes.substring(0, sinBordes.length() - 1);
        }
        return Arrays.stream(sinBordes.split("\\|")).collect(Collectors.toList());
    }

    /**
     * Busca la primera tabla Markdown que aparece despues de una linea que contiene
     * {@code marcador}, y devuelve sus filas de datos (sin header ni separador) como
     * pares (valor, codigo). {@code codigo} es null si la tabla no tiene columna "Codigo".
     */
    private static List<FilaTablaEnum> parseTablaEnumDespuesDe(Path archivo, String marcador) throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        int indiceMarcador = -1;
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).contains(marcador)) {
                indiceMarcador = i;
                break;
            }
        }
        assertThat(indiceMarcador)
                .as("Marcador '%s' no encontrado en %s", marcador, archivo)
                .isGreaterThanOrEqualTo(0);

        int i = indiceMarcador + 1;
        while (i < lineas.size() && !PATRON_FILA_TABLA.matcher(lineas.get(i)).matches()) {
            i++;
        }
        assertThat(i)
                .as("No se encontro una tabla Markdown despues de '%s' en %s", marcador, archivo)
                .isLessThan(lineas.size());

        List<String> headerCols = partirCeldas(lineas.get(i));
        int indiceCodigo = -1;
        for (int c = 0; c < headerCols.size(); c++) {
            if (headerCols.get(c).trim().toLowerCase(Locale.ROOT).startsWith("codigo")) {
                indiceCodigo = c;
            }
        }
        i += 2; // header + separador

        List<FilaTablaEnum> filas = new ArrayList<>();
        while (i < lineas.size() && PATRON_FILA_TABLA.matcher(lineas.get(i)).matches()) {
            List<String> celdas = partirCeldas(lineas.get(i));
            String valor = limpiarCelda(celdas.get(0));
            String codigo = (indiceCodigo >= 0 && indiceCodigo < celdas.size())
                    ? limpiarCelda(celdas.get(indiceCodigo))
                    : null;
            filas.add(new FilaTablaEnum(valor, codigo));
            i++;
        }
        return filas;
    }

    private static <E extends Enum<E>> void verificarParidadEnum(
            Class<E> claseEnum, ToIntFunction<E> codigoFn, boolean tieneCodigo,
            Path archivo, String marcador) throws IOException {
        List<FilaTablaEnum> filas = parseTablaEnumDespuesDe(archivo, marcador);
        Set<String> valoresDoc = filas.stream()
                .map(FilaTablaEnum::valor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> valoresJava = Arrays.stream(claseEnum.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertThat(valoresDoc)
                .as("Valores de %s en %s (marcador '%s') deben coincidir exactamente con %s.values()",
                        claseEnum.getSimpleName(), archivo.getFileName(), marcador, claseEnum.getSimpleName())
                .containsExactlyInAnyOrderElementsOf(valoresJava);

        if (tieneCodigo) {
            Map<String, String> codigoPorValorDoc = filas.stream()
                    .collect(Collectors.toMap(FilaTablaEnum::valor, f -> f.codigo(), (a, b) -> a));
            for (E constante : claseEnum.getEnumConstants()) {
                String codigoJava = String.valueOf(codigoFn.applyAsInt(constante));
                String codigoDoc = codigoPorValorDoc.get(constante.name());
                assertThat(codigoDoc)
                        .as("Codigo documentado de %s.%s en %s debe coincidir con codigo() = %s",
                                claseEnum.getSimpleName(), constante.name(), archivo.getFileName(), codigoJava)
                        .isEqualTo(codigoJava);
            }
        }
    }

    // =====================================================================
    // G-1: Registro completo
    // =====================================================================

    @Nested
    @DisplayName("G-1: registro documental completo")
    class G1RegistroCompleto {

        @Test
        @DisplayName("cada .md fuera de handoff aparece exactamente una vez en el registro")
        void cada_md_aparece_exactamente_una_vez() throws IOException {
            List<Path> archivos = listarMarkdownFueraDeHandoff();
            List<FilaRegistro> filas = parsearRegistro();
            Map<String, Long> conteoPorPath = filas.stream()
                    .collect(Collectors.groupingBy(FilaRegistro::path, Collectors.counting()));

            for (Path archivo : archivos) {
                String pathEsperado = pathRegistroDe(archivo);
                Long conteo = conteoPorPath.get(pathEsperado);
                assertThat(conteo)
                        .as("El archivo '%s' debe aparecer exactamente una vez en spec-document-registry.md", pathEsperado)
                        .isNotNull()
                        .isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("ningun path del registro esta duplicado")
        void ningun_path_duplicado() throws IOException {
            List<FilaRegistro> filas = parsearRegistro();
            Set<String> vistos = new LinkedHashSet<>();
            Set<String> duplicados = new LinkedHashSet<>();
            for (FilaRegistro fila : filas) {
                if (!vistos.add(fila.path())) {
                    duplicados.add(fila.path());
                }
            }
            assertThat(duplicados).as("Paths duplicados en el registro").isEmpty();
        }

        @Test
        @DisplayName("cada path registrado existe fisicamente con case exacto")
        void cada_path_registrado_existe() throws IOException {
            List<FilaRegistro> filas = parsearRegistro();
            for (FilaRegistro fila : filas) {
                Path resuelto = specRoot.resolve(fila.path());
                assertThat(Files.isRegularFile(resuelto))
                        .as("El path registrado '%s' debe existir fisicamente (case exacto)", fila.path())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("cada fila del registro tiene clasificacion valida")
        void clasificacion_valida() throws IOException {
            for (FilaRegistro fila : parsearRegistro()) {
                assertThat(CLASIFICACIONES_VALIDAS)
                        .as("Clasificacion invalida '%s' para '%s'", fila.clasificacion(), fila.path())
                        .contains(fila.clasificacion());
            }
        }

        @Test
        @DisplayName("cada fila del registro tiene autoridad DDL valida")
        void autoridad_valida() throws IOException {
            for (FilaRegistro fila : parsearRegistro()) {
                assertThat(AUTORIDADES_VALIDAS)
                        .as("Autoridad DDL invalida '%s' para '%s'", fila.autoridad(), fila.path())
                        .contains(fila.autoridad());
            }
        }

        @Test
        @DisplayName("el registro se incluye a si mismo")
        void registro_se_incluye_a_si_mismo() throws IOException {
            List<FilaRegistro> filas = parsearRegistro();
            assertThat(filas.stream().map(FilaRegistro::path))
                    .contains("00-governance/spec-document-registry.md");
        }

        @Test
        @DisplayName("cada fila tiene Estado = VIGENTE")
        void estado_de_fila_es_vigente() throws IOException {
            for (FilaRegistro fila : parsearRegistro()) {
                assertThat(fila.estado())
                        .as("Estado de la fila '%s' debe ser VIGENTE", fila.path())
                        .isEqualTo("VIGENTE");
            }
        }

        @Test
        @DisplayName("la autoridad DDL de cada fila coincide con la clasificacion (NORMATIVE->YES, SUPPORTING_CURRENT/PRE_DDL_PLAN->SUPPORTING, HISTORICAL->NO)")
        void autoridad_coincide_con_clasificacion() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (FilaRegistro fila : parsearRegistro()) {
                String esperada = autoridadEsperadaPara(fila.clasificacion());
                if (esperada != null && !esperada.equals(fila.autoridad())) {
                    violaciones.add(fila.path() + ": clasificacion=" + fila.clasificacion()
                            + " autoridad=" + fila.autoridad() + " (esperada " + esperada + ")");
                }
            }
            assertThat(violaciones).as("Filas con autoridad DDL inconsistente con su clasificacion").isEmpty();
        }

        @Test
        @DisplayName("el banner de cada documento (fuera de README.md) coincide con su fila del registro")
        void banner_coincide_con_registro() throws IOException {
            Map<String, FilaRegistro> filasPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, f -> f, (a, b) -> a));
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String pathRegistro = pathRegistroDe(archivo);
                if (pathRegistro.equals("README.md")) {
                    continue;
                }
                Banner banner = leerBanner(archivo);
                FilaRegistro fila = filasPorPath.get(pathRegistro);
                if (fila == null) {
                    continue;
                }
                if (banner == null) {
                    violaciones.add(pathRegistro + ": sin banner de Estado documental/Autoridad DDL");
                    continue;
                }
                if (!banner.clasificacion().equals(fila.clasificacion())) {
                    violaciones.add(pathRegistro + ": banner.clasificacion=" + banner.clasificacion()
                            + " != registro.clasificacion=" + fila.clasificacion());
                }
                if (!banner.autoridad().equals(fila.autoridad())) {
                    violaciones.add(pathRegistro + ": banner.autoridad=" + banner.autoridad()
                            + " != registro.autoridad=" + fila.autoridad());
                }
            }
            assertThat(violaciones).as("Documentos cuyo banner no coincide con el registro").isEmpty();
        }
    }

    // =====================================================================
    // G-2: Links relativos
    // =====================================================================

    @Nested
    @DisplayName("G-2: links relativos Markdown resuelven")
    class G2LinksRelativos {

        @Test
        @DisplayName("todos los links relativos fuera de bloques de codigo apuntan a un archivo existente")
        void links_relativos_resuelven() throws IOException {
            List<String> rotos = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
                boolean enBloqueCodigo = false;
                for (int i = 0; i < lineas.size(); i++) {
                    String linea = lineas.get(i);
                    if (PATRON_FENCE.matcher(linea).find()) {
                        enBloqueCodigo = !enBloqueCodigo;
                        continue;
                    }
                    if (enBloqueCodigo) {
                        continue;
                    }
                    Matcher m = PATRON_LINK_MARKDOWN.matcher(linea);
                    while (m.find()) {
                        String destino = m.group(1).trim();
                        if (destino.isEmpty()) {
                            continue;
                        }
                        if (destino.startsWith("http://") || destino.startsWith("https://")
                                || destino.startsWith("mailto:") || destino.startsWith("#")) {
                            continue;
                        }
                        String rutaSinAncla = destino.contains("#")
                                ? destino.substring(0, destino.indexOf('#'))
                                : destino;
                        if (rutaSinAncla.isEmpty()) {
                            continue;
                        }
                        if (rutaSinAncla.matches("^[A-Za-z]:\\\\.*") || rutaSinAncla.startsWith("\\\\")) {
                            rotos.add(archivo + ":" + (i + 1) + " -> " + destino
                                    + " (link absoluto de Windows prohibido)");
                            continue;
                        }
                        Path resuelto = archivo.getParent().resolve(rutaSinAncla).normalize();
                        if (!Files.isRegularFile(resuelto)) {
                            rotos.add(archivo + ":" + (i + 1) + " -> " + destino + " (resuelto: " + resuelto + ")");
                        }
                    }
                }
            }
            assertThat(rotos).as("Links relativos rotos o absolutos de Windows").isEmpty();
        }
    }

    // =====================================================================
    // G-3: Cero IDs GAP
    // =====================================================================

    @Nested
    @DisplayName("G-3: cero identificadores GAP-")
    class G3CeroGap {

        @Test
        @DisplayName("ningun .md fuera de handoff contiene el prefijo GAP-")
        void cero_gap_ids() throws IOException {
            List<String> encontrados = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String contenido = leer(archivo);
                if (contenido.contains("GAP-")) {
                    encontrados.add(archivo.toString());
                }
            }
            assertThat(encontrados).as("Archivos con el prefijo prohibido GAP-").isEmpty();
        }

        @Test
        @DisplayName("ningun .md fuera de handoff contiene tokens de trabajo GAP-/TODO/FIXME/TBD (case-sensitive)")
        void cero_tokens_de_trabajo() throws IOException {
            List<String> encontrados = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String contenido = leer(archivo);
                for (String token : TOKENS_DE_TRABAJO_PROHIBIDOS) {
                    Pattern patron = token.endsWith("-")
                            ? Pattern.compile(Pattern.quote(token))
                            : Pattern.compile("\\b" + Pattern.quote(token) + "\\b");
                    if (patron.matcher(contenido).find()) {
                        encontrados.add(pathRegistroDe(archivo) + " contiene '" + token + "'");
                    }
                }
            }
            assertThat(encontrados)
                    .as("Archivos con tokens de trabajo prohibidos (sensible a mayusculas; 'Todo' en espanol no aplica)")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-4: Comandos canonicos
    // =====================================================================

    @Nested
    @DisplayName("G-4: CMD-FALLO-001..007 exactamente una vez, sin un octavo")
    class G4ComandosCanonicos {

        private String leerContratos() throws IOException {
            Path archivo = specRoot.resolve("20-application").resolve("fallo-command-contracts.md");
            assertThat(Files.isRegularFile(archivo)).isTrue();
            return leer(archivo);
        }

        @Test
        @DisplayName("cada CMD-FALLO-001..007 aparece exactamente una vez como heading de nivel 2")
        void siete_comandos_exactamente_una_vez() throws IOException {
            String contenido = leerContratos();
            List<String> lineas = contenido.lines().collect(Collectors.toList());
            Map<String, Integer> conteo = new LinkedHashMap<>();
            for (String linea : lineas) {
                Matcher m = PATRON_HEADING_CMD_FALLO.matcher(linea);
                if (m.find()) {
                    conteo.merge(m.group(1), 1, Integer::sum);
                }
            }
            for (int i = 1; i <= 7; i++) {
                String id = String.format("CMD-FALLO-%03d", i);
                assertThat(conteo.getOrDefault(id, 0))
                        .as("%s debe aparecer exactamente una vez como heading de nivel 2", id)
                        .isEqualTo(1);
            }
        }

        @Test
        @DisplayName("no existe un octavo comando de la familia fallo")
        void no_existe_comando_ocho() throws IOException {
            String contenido = leerContratos();
            String idOctavo = "CMD-FALLO-" + "008";
            assertThat(contenido).doesNotContain(idOctavo);
        }
    }

    // =====================================================================
    // G-5: Sin temporalidad de slices en documentos vigentes
    // =====================================================================

    @Nested
    @DisplayName("G-5: documentos vigentes sin lenguaje de diario de slices")
    class G5SinTemporalidadSlice {

        @Test
        @DisplayName("NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN no usan patrones de diario de slices")
        void sin_patrones_de_slice() throws IOException {
            Map<String, String> clasificacionPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, FilaRegistro::clasificacion, (a, b) -> a));
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String pathRegistro = pathRegistroDe(archivo);
                String clasificacion = clasificacionPorPath.get(pathRegistro);
                if (clasificacion == null || !CLASIFICACIONES_SIN_TEMPORALIDAD_SLICE.contains(clasificacion)) {
                    continue;
                }
                String contenido = leer(archivo);
                if (PATRON_SLICE_NUMERO.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'Slice N')");
                }
                if (PATRON_SLICE_FUTURO.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'slice futuro')");
                }
                if (PATRON_SLICE_POSTERIOR.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'slice posterior')");
                }
                if (PATRON_ESTADO_ACTUAL_SLICE.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'estado actual del slice')");
                }
                if (PATRON_8F.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron '8F-')");
                }
                if (PATRON_CERRADO_EN.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'cerrado en')");
                }
                if (PATRON_REEMPLAZA_109.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'reemplaza a 109' sin negacion)");
                }
                if (PATRON_109_HISTORIAL.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron '109 queda como historial')");
                }
                if (contenido.lines().anyMatch(l -> PATRON_FECHA_ORIGINAL.matcher(l.trim()).find())) {
                    violaciones.add(pathRegistro + " (patron 'Fecha original:' de anexo historico)");
                }
                if (PATRON_BUILD_BASE_HISTORICO.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'Build base confirmado en esa auditoria')");
                }
            }
            assertThat(violaciones)
                    .as("Documentos NORMATIVE/SUPPORTING_CURRENT/PRE_DDL_PLAN con lenguaje de diario de slices")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-6: Terminos prohibidos
    // =====================================================================

    @Nested
    @DisplayName("G-6: terminos prohibidos ausentes en documentos vigentes")
    class G6TerminosProhibidos {

        @Test
        @DisplayName("NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN solo mencionan terminos obsoletos como prohibicion explicita")
        void sin_terminos_prohibidos() throws IOException {
            Map<String, String> clasificacionPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, FilaRegistro::clasificacion, (a, b) -> a));
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String pathRegistro = pathRegistroDe(archivo);
                String clasificacion = clasificacionPorPath.get(pathRegistro);
                if (clasificacion == null || !CLASIFICACIONES_SIN_TEMPORALIDAD_SLICE.contains(clasificacion)) {
                    continue;
                }
                List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
                for (String termino : TERMINOS_PROHIBIDOS) {
                    Pattern patronTermino = Pattern.compile("\\b" + Pattern.quote(termino) + "\\b");
                    for (int i = 0; i < lineas.size(); i++) {
                        if (!patronTermino.matcher(lineas.get(i)).find()) {
                            continue;
                        }
                        if (!lineaTieneMarcador(lineas.get(i)) && !dentroDeSeccionLegacyEstructurada(lineas, i)) {
                            violaciones.add(pathRegistro + ":" + (i + 1) + " contiene '" + termino
                                    + "' sin marcador explicito en la misma linea ni en una seccion legacy estructurada");
                        }
                    }
                }
            }
            assertThat(violaciones)
                    .as("Documentos vigentes con identificadores obsoletos usados sin marcarlos explicitamente como prohibidos")
                    .isEmpty();
        }

        /**
         * G-6 tightened: solo dos formas legitiman un termino obsoleto: (a) la misma linea
         * lo marca explicitamente (NO EXISTE/PROHIBIDO/LEGACY_RESERVED/etc.), o (b) la linea
         * esta dentro de una seccion cuyo heading (## o ###) mas cercano hacia arriba contiene
         * un marcador de negacion/legado. Ya no basta una negacion a N lineas de distancia sin
         * relacion estructural.
         */
        private boolean lineaTieneMarcador(String linea) {
            String lineaMinuscula = linea.toLowerCase(Locale.ROOT);
            for (String marcador : MARCADORES_NEGACION) {
                if (lineaMinuscula.contains(marcador)) {
                    return true;
                }
            }
            return false;
        }

        private boolean dentroDeSeccionLegacyEstructurada(List<String> lineas, int indiceLinea) {
            for (int i = indiceLinea; i >= 0; i--) {
                String linea = lineas.get(i).trim();
                if (linea.startsWith("##")) {
                    return lineaTieneMarcador(linea);
                }
            }
            return false;
        }

        @Test
        @DisplayName("FALLO_CONDENATORIO_PAGADO es valor vigente de ResultadoFinalActa, no un termino prohibido")
        void fallo_condenatorio_pagado_es_vigente() {
            boolean existe = false;
            for (ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa valor
                    : ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa.values()) {
                if (valor.name().equals("FALLO_CONDENATORIO_PAGADO")) {
                    existe = true;
                    break;
                }
            }
            assertThat(existe)
                    .as("FALLO_CONDENATORIO_PAGADO debe existir en ResultadoFinalActa (no es un termino obsoleto)")
                    .isTrue();
        }
    }

    // =====================================================================
    // G-7: Gate DDL
    // =====================================================================

    @Nested
    @DisplayName("G-7: gate formal READY_FOR_DDL declarado una unica vez")
    class G7GateDdl {

        @Test
        @DisplayName("101-auditoria-pre-jdbc-mariadb.md declara exactamente una linea 'Estado: READY_FOR_DDL' y cero 'Estado: BLOCKED'")
        void gate_declarado_una_vez() throws IOException {
            Path archivo = specRoot.resolve("101-auditoria-pre-jdbc-mariadb.md");
            assertThat(Files.isRegularFile(archivo)).isTrue();
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            Pattern patronGateReady = Pattern.compile("^#{0,6}\\s*Estado:\\s*READY_FOR_DDL\\s*$");
            Pattern patronGateBlocked = Pattern.compile("^#{0,6}\\s*Estado:\\s*BLOCKED\\s*$");
            long readyCount = lineas.stream().filter(l -> patronGateReady.matcher(l.trim()).matches()).count();
            long blockedCount = lineas.stream().filter(l -> patronGateBlocked.matcher(l.trim()).matches()).count();
            assertThat(readyCount)
                    .as("Debe existir exactamente una linea 'Estado: READY_FOR_DDL' en 101-auditoria-pre-jdbc-mariadb.md")
                    .isEqualTo(1L);
            assertThat(blockedCount)
                    .as("No debe existir ninguna linea 'Estado: BLOCKED' en 101-auditoria-pre-jdbc-mariadb.md")
                    .isEqualTo(0L);
        }

        @Test
        @DisplayName("el gate READY_FOR_DDL esta condicionado: cero bloqueadores internos declarados")
        void gate_condicionado_sin_bloqueadores() throws IOException {
            Path archivo = specRoot.resolve("101-auditoria-pre-jdbc-mariadb.md");
            String contenido = leer(archivo);
            assertThat(contenido)
                    .as("101 debe declarar explicitamente que no quedan bloqueadores internos")
                    .containsPattern("(?i)Bloqueadores internos");
            assertThat(contenido).containsIgnoringCase("Ninguno");
        }
    }

    // =====================================================================
    // G-8: README y registro
    // =====================================================================

    @Nested
    @DisplayName("G-8: README enlaza al registro y a los documentos de preparacion MariaDB")
    class G8ReadmeYRegistro {

        @Test
        @DisplayName("README enlaza spec-document-registry, 101, 109 y 110")
        void readme_enlaza_documentos_requeridos() throws IOException {
            Path readme = specRoot.resolve("README.md");
            assertThat(Files.isRegularFile(readme)).isTrue();
            String contenido = leer(readme);
            assertThat(contenido).contains("spec-document-registry.md");
            assertThat(contenido).contains("101-auditoria-pre-jdbc-mariadb.md");
            assertThat(contenido).contains("109-delta-modelo-mariadb-inmemory.md");
            assertThat(contenido).contains("110-matriz-maestra-paridad-mariadb-inmemory.md");
        }
    }

    // =====================================================================
    // G-9: Paridad de enums contra su tabla propietaria
    // =====================================================================

    @Nested
    @DisplayName("G-9: paridad exacta de enums contra su tabla propietaria en la spec")
    class G9ParidadEnums {

        @Test
        @DisplayName("EstadoFalloActa (sin codigo) coincide con lifecycle-states.md [FALLO-STATE-001]")
        void estado_fallo_acta() throws IOException {
            verificarParidadEnum(EstadoFalloActa.class, e -> 0, false,
                    specRoot.resolve("10-domain").resolve("lifecycle-states.md"),
                    "[FALLO-STATE-001]");
        }

        @Test
        @DisplayName("ResultadoFinalActa (con codigo) coincide con lifecycle-states.md")
        void resultado_final_acta() throws IOException {
            verificarParidadEnum(ResultadoFinalActa.class, ResultadoFinalActa::codigo, true,
                    specRoot.resolve("10-domain").resolve("lifecycle-states.md"),
                    "`resultadoFinal` -- `ResultadoFinalActa`");
        }

        @Test
        @DisplayName("EstadoApelacionActa (sin codigo) coincide con 02-estados-bloques-eventos.md")
        void estado_apelacion_acta() throws IOException {
            verificarParidadEnum(EstadoApelacionActa.class, e -> 0, false,
                    specRoot.resolve("02-estados-bloques-eventos.md"),
                    "## EstadoApelacionActa");
        }

        @Test
        @DisplayName("ResultadoResolucionApelacion (con codigo) coincide con 02-estados-bloques-eventos.md")
        void resultado_resolucion_apelacion() throws IOException {
            verificarParidadEnum(ResultadoResolucionApelacion.class, ResultadoResolucionApelacion::codigo, true,
                    specRoot.resolve("02-estados-bloques-eventos.md"),
                    "## ResultadoResolucionApelacion");
        }

        @Test
        @DisplayName("EstadoPagoCondena (sin codigo) coincide con 02-estados-bloques-eventos.md")
        void estado_pago_condena() throws IOException {
            verificarParidadEnum(EstadoPagoCondena.class, e -> 0, false,
                    specRoot.resolve("02-estados-bloques-eventos.md"),
                    "### Estado: EstadoPagoCondena");
        }

        @Test
        @DisplayName("ActorTipoEvento (con codigo) coincide con 02-estados-bloques-eventos.md")
        void actor_tipo_evento() throws IOException {
            verificarParidadEnum(ActorTipoEvento.class, ActorTipoEvento::codigo, true,
                    specRoot.resolve("02-estados-bloques-eventos.md"),
                    "### ActorTipoEvento");
        }

        @Test
        @DisplayName("OrigenEvento (con codigo) coincide con 02-estados-bloques-eventos.md")
        void origen_evento() throws IOException {
            verificarParidadEnum(OrigenEvento.class, OrigenEvento::codigo, true,
                    specRoot.resolve("02-estados-bloques-eventos.md"),
                    "### OrigenEvento");
        }
    }

    // =====================================================================
    // G-10: APERAZ/RECHAZADA nunca es firmeza automatica
    // =====================================================================

    @Nested
    @DisplayName("G-10: APERAZ/RECHAZADA nunca se documentan como firmeza automatica")
    class G10Aperaz {

        private static final Pattern PATRON_TOKEN_APELACION = Pattern.compile("\\b(APERAZ|RECHAZADA)\\b");
        // Excluye "DECLARAR_CONDENA_FIRME" (nombre de accion/comando pendiente, no un hecho consumado).
        private static final Pattern PATRON_SEMANTICA_FIRMEZA =
                Pattern.compile("(?i)queda firme|declara firmeza|(?<!DECLARAR_)CONDENA_FIRME\\b");
        private static final Pattern PATRON_NEGACION_INLINE =
                Pattern.compile("(?i)no declara|no genera|no habilita|no es firme|no queda firme");

        @Test
        @DisplayName("ninguna linea de documento vigente afirma APERAZ/RECHAZADA = firmeza sin negacion en la misma linea")
        void cero_aperaz_igual_firmeza() throws IOException {
            Map<String, String> clasificacionPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, FilaRegistro::clasificacion, (a, b) -> a));
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownFueraDeHandoff()) {
                String pathRegistro = pathRegistroDe(archivo);
                String clasificacion = clasificacionPorPath.get(pathRegistro);
                if (clasificacion == null || !CLASIFICACIONES_SIN_TEMPORALIDAD_SLICE.contains(clasificacion)) {
                    continue;
                }
                List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
                for (int i = 0; i < lineas.size(); i++) {
                    String linea = lineas.get(i);
                    if (PATRON_TOKEN_APELACION.matcher(linea).find()
                            && PATRON_SEMANTICA_FIRMEZA.matcher(linea).find()
                            && !PATRON_NEGACION_INLINE.matcher(linea).find()) {
                        violaciones.add(pathRegistro + ":" + (i + 1)
                                + " afirma APERAZ/RECHAZADA = firmeza sin negacion en la misma linea");
                    }
                }
            }
            assertThat(violaciones)
                    .as("Lineas que afirman firmeza automatica por APERAZ/RECHAZADA sin negacion explicita")
                    .isEmpty();
        }

        @Test
        @DisplayName("02-estados-bloques-eventos.md referencia CMD-FALLO-006 para la apelacion rechazada")
        void referencia_cmd_fallo_006() throws IOException {
            Path archivo = specRoot.resolve("02-estados-bloques-eventos.md");
            String contenido = leer(archivo);
            assertThat(contenido).contains("CMD-FALLO-006");
        }
    }

    // =====================================================================
    // G-11: 03 no duplica contratos legacy de firmeza
    // =====================================================================

    @Nested
    @DisplayName("G-11: 03-comandos-precondiciones-efectos.md sin contratos legacy de firmeza")
    class G11PropiedadContratos {

        private String leer03() throws IOException {
            Path archivo = specRoot.resolve("03-comandos-precondiciones-efectos.md");
            assertThat(Files.isRegularFile(archivo)).isTrue();
            return leer(archivo);
        }

        @Test
        @DisplayName("03 enlaza a 20-application/fallo-command-contracts.md")
        void enlaza_a_contratos_canonicos() throws IOException {
            assertThat(leer03()).contains("20-application/fallo-command-contracts.md");
        }

        @Test
        @DisplayName("03 no duplica tokens legacy de firmeza")
        void cero_tokens_legacy_de_firmeza() throws IOException {
            String contenido = leer03();
            List<String> tokensProhibidos = List.of(
                    "FalActaFirmezaCondena",
                    "FirmezaCondenaRepository",
                    "ninguna ultima apelacion",
                    "existe ultima apelacion",
                    "VencerPlazoApelacionCommand(actaId, observaciones)",
                    "DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, observaciones)");
            List<String> encontrados = new ArrayList<>();
            for (String token : tokensProhibidos) {
                if (contenido.contains(token)) {
                    encontrados.add(token);
                }
            }
            assertThat(encontrados)
                    .as("03-comandos-precondiciones-efectos.md no debe contener contratos legacy de firmeza")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-12: 109/110 complementarios, sin categorias historicas de discrepancia
    // =====================================================================

    @Nested
    @DisplayName("G-12: 109 y 110 complementarios, sin fuente unica ni categorias historicas")
    class G12Delta109110 {

        private String leer110() throws IOException {
            return leer(specRoot.resolve("110-matriz-maestra-paridad-mariadb-inmemory.md"));
        }

        private String leer109() throws IOException {
            return leer(specRoot.resolve("109-delta-modelo-mariadb-inmemory.md"));
        }

        @Test
        @DisplayName("109 y 110 se declaran complementarios entre si")
        void se_declaran_complementarios() throws IOException {
            assertThat(leer109()).containsIgnoringCase("complementari");
            assertThat(leer110()).containsIgnoringCase("complementari");
        }

        @Test
        @DisplayName("110 no afirma ser fuente unica de verdad ni reemplazar a 109")
        void sin_fuente_unica_ni_reemplazo() throws IOException {
            String contenido = leer110();
            assertThat(PATRON_REEMPLAZA_109.matcher(contenido).find())
                    .as("110 no debe afirmar 'reemplaza a 109' sin negacion")
                    .isFalse();
            assertThat(PATRON_109_HISTORIAL.matcher(contenido).find())
                    .as("110 no debe afirmar '109 queda como historial'")
                    .isFalse();
            // "fuente unica de verdad" solo es aceptable cuando la misma linea la niega
            // explicitamente (ninguno/no es/ni sustituye/complementario).
            Pattern patronFuenteUnica = Pattern.compile("(?i)fuente unica de verdad");
            List<String> negadores = List.of("ninguno", "no es", "ni ", "complementari");
            List<String> violaciones = new ArrayList<>();
            for (String linea : contenido.lines().collect(Collectors.toList())) {
                if (!patronFuenteUnica.matcher(linea).find()) {
                    continue;
                }
                String lineaMinuscula = linea.toLowerCase(Locale.ROOT);
                boolean negada = negadores.stream().anyMatch(lineaMinuscula::contains);
                if (!negada) {
                    violaciones.add(linea.trim());
                }
            }
            assertThat(violaciones)
                    .as("110 no debe afirmarse a si mismo como fuente unica de verdad sin negacion en la misma linea")
                    .isEmpty();
        }

        @Test
        @DisplayName("110 contiene DECISION_DDL")
        void contiene_decision_ddl() throws IOException {
            assertThat(leer110()).contains("DECISION_DDL");
        }

        @Test
        @DisplayName("110 documenta las columnas estructurales obligatorias por agregado")
        void contiene_columnas_obligatorias() throws IOException {
            String contenido = leer110();
            List<String> columnasEsperadas = List.of(
                    "PK", "Entidad Java", "Puerto", "Auditoria", "versionRow", "DECISION_DDL", "Estado");
            List<String> faltantes = columnasEsperadas.stream()
                    .filter(c -> !contenido.contains(c))
                    .collect(Collectors.toList());
            assertThat(faltantes).as("110 debe documentar estas columnas/atributos estructurales").isEmpty();
        }

        @Test
        @DisplayName("110 no contiene 8F- ni categorias historicas de discrepancia")
        void sin_8f_ni_categorias_historicas() throws IOException {
            String contenido = leer110();
            assertThat(PATRON_8F.matcher(contenido).find()).as("110 no debe contener '8F-'").isFalse();
            List<String> encontradas = CATEGORIAS_HISTORICAS_DISCREPANCIA.stream()
                    .filter(contenido::contains)
                    .collect(Collectors.toList());
            assertThat(encontradas)
                    .as("110 no debe usar categorias historicas de discrepancia como estado vigente")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-13: Seguridad de CMD-FALLO-003
    // =====================================================================

    @Nested
    @DisplayName("G-13: seguridad de CMD-FALLO-003 documentada como identidad tecnica")
    class G13SeguridadCmdFallo003 {

        @Test
        @DisplayName("101 documenta CMD-FALLO-003 con identidad tecnica, sin JWT de usuario final")
        void cmd_fallo_003_identidad_tecnica() throws IOException {
            String contenido = leer(specRoot.resolve("101-auditoria-pre-jdbc-mariadb.md"));
            assertThat(contenido).contains("CMD-FALLO-003");
            assertThat(contenido).containsIgnoringCase("identidad t");
            assertThat(contenido).containsPattern("(?i)no aplica JWT de usuario final");
        }

        @Test
        @DisplayName("101 no afirma una regla JWT comun a los siete comandos")
        void sin_jwt_comun_a_los_siete() throws IOException {
            Path archivo = specRoot.resolve("101-auditoria-pre-jdbc-mariadb.md");
            assertThat(leer(archivo))
                    .doesNotContain("JWT Bearer obligatorio y actor = `sub` verificado para los siete comandos");
            Pattern patronJwtSiete = Pattern.compile("(?i)JWT[^\\n]{0,40}para los siete comandos");
            List<String> violaciones = new ArrayList<>();
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (!patronJwtSiete.matcher(linea).find()) {
                    continue;
                }
                if (!lineaTieneNegacionExplicita(linea)) {
                    violaciones.add(linea.trim());
                }
            }
            assertThat(violaciones)
                    .as("No debe afirmarse (sin negacion explicita) una politica JWT unica para los siete comandos")
                    .isEmpty();
        }

        private boolean lineaTieneNegacionExplicita(String linea) {
            String minuscula = linea.toLowerCase(Locale.ROOT);
            return minuscula.contains("no existe") || minuscula.contains("no hay")
                    || minuscula.contains("no es ") || minuscula.contains("no aplica");
        }
    }

    // =====================================================================
    // G-14: ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO es LEGACY_RESERVED
    // =====================================================================

    @Nested
    @DisplayName("G-14: FALLO_CONDENATORIO_PAGADO documentado como LEGACY_RESERVED")
    class G14ResultadoFinalLegacy {

        @Test
        @DisplayName("el codigo 5 (FALLO_CONDENATORIO_PAGADO) sigue compilado en ResultadoFinalActa")
        void codigo_5_sigue_compilado() {
            boolean existe = Arrays.stream(ResultadoFinalActa.values())
                    .anyMatch(v -> v.name().equals("FALLO_CONDENATORIO_PAGADO") && v.codigo() == 5);
            assertThat(existe)
                    .as("FALLO_CONDENATORIO_PAGADO (codigo 5) debe seguir compilado en ResultadoFinalActa")
                    .isTrue();
        }

        @Test
        @DisplayName("glossary.md y lifecycle-states.md documentan FALLO_CONDENATORIO_PAGADO como LEGACY_RESERVED")
        void documentado_como_legacy_reserved() throws IOException {
            String glosario = leer(specRoot.resolve("00-governance").resolve("glossary.md"));
            String lifecycle = leer(specRoot.resolve("10-domain").resolve("lifecycle-states.md"));
            for (String contenido : List.of(glosario, lifecycle)) {
                assertThat(contenido).contains("FALLO_CONDENATORIO_PAGADO");
                assertThat(contenido).contains("LEGACY_RESERVED");
            }
        }

        @Test
        @DisplayName("la documentacion aclara que ningun comando vigente asigna FALLO_CONDENATORIO_PAGADO")
        void ningun_comando_vigente_lo_asigna() throws IOException {
            String glosario = leer(specRoot.resolve("00-governance").resolve("glossary.md"));
            assertThat(glosario).containsPattern("(?i)ningun comando vigente lo asigna");
        }

        @Test
        @DisplayName("la documentacion identifica CONDENA_FIRME_PAGADA como resultado canonico del pago de condena")
        void condena_firme_pagada_es_canonico() throws IOException {
            String glosario = leer(specRoot.resolve("00-governance").resolve("glossary.md"));
            assertThat(glosario).containsPattern("(?i)CONDENA_FIRME_PAGADA.{0,80}canonic");
        }

        @Test
        @DisplayName("110 contiene DECISION_DDL-RF-005 para el tratamiento fisico de FALLO_CONDENATORIO_PAGADO")
        void decision_ddl_rf_005_presente() throws IOException {
            String contenido = leer(specRoot.resolve("110-matriz-maestra-paridad-mariadb-inmemory.md"));
            assertThat(contenido).contains("DECISION_DDL-RF-005");
        }
    }

    // =====================================================================
    // G-15: Coherencia del contrato de pago de condena en 02/03/05
    // =====================================================================

    @Nested
    @DisplayName("G-15: coherencia del contrato de pago de condena en 02/03/05")
    class G15CoherenciaPago {

        @Test
        @DisplayName("02 afirma que PCOCNF se registra siempre y que los bloqueantes solo impiden CIERRA")
        void doc02_pcocnf_siempre_bloqueantes_solo_impiden_cierra() throws IOException {
            String contenido = leer(specRoot.resolve("02-estados-bloques-eventos.md"));
            assertThat(Pattern.compile("(?i)PCOCNF se registra siempre").matcher(contenido).find())
                    .as("02 debe afirmar que PCOCNF se registra siempre que se superan las precondiciones")
                    .isTrue();
            assertThat(Pattern.compile("(?i)impiden que se registre[^\\n]*CIERRA").matcher(contenido).find())
                    .as("02 debe afirmar que los bloqueantes unicamente impiden que se registre CIERRA")
                    .isTrue();
        }

        @Test
        @DisplayName("02 no contiene afirmaciones contradictorias sobre PCOCNF y bloqueantes")
        void doc02_sin_contradicciones() throws IOException {
            String contenido = leer(specRoot.resolve("02-estados-bloques-eventos.md"));
            List<String> prohibidos = List.of(
                    "Confirmar pago solo si no hay bloqueantes",
                    "no registrar PCOCNF",
                    "PCOCNF + CIERRA como unica transicion",
                    "PCOCNF + CIERRA como única transición");
            List<String> encontrados = prohibidos.stream().filter(contenido::contains).collect(Collectors.toList());
            assertThat(encontrados)
                    .as("02 no debe contener afirmaciones contradictorias sobre pago de condena")
                    .isEmpty();
            assertThat(Pattern.compile("(?i)CONFIRMADO\\s*->\\s*cierre del acta").matcher(contenido).find())
                    .as("02 no debe afirmar 'CONFIRMADO -> cierre del acta' como regla unica sin bifurcacion")
                    .isFalse();
        }

        @Test
        @DisplayName("03 no duplica InformarPagoCondenaCommand y enlaza CMD-FALLO-007 a 20")
        void doc03_sin_duplicado_informar() throws IOException {
            String contenido = leer(specRoot.resolve("03-comandos-precondiciones-efectos.md"));
            assertThat(contenido).doesNotContain("### InformarPagoCondenaCommand");
            assertThat(Pattern.compile("(?i)fallo condenatorio con estado `NOTIFICADO`").matcher(contenido).find())
                    .as("03 no debe duplicar la precondicion de estado NOTIFICADO propia de 20")
                    .isFalse();
            assertThat(contenido).contains("CMD-FALLO-007");
            assertThat(contenido).contains("20-application/fallo-command-contracts.md");
        }

        @Test
        @DisplayName("03 no exige ausencia de bloqueantes como precondicion de ConfirmarPagoCondena")
        void doc03_bloqueantes_no_son_precondicion() throws IOException {
            String contenido = leer(specRoot.resolve("03-comandos-precondiciones-efectos.md"));
            assertThat(contenido).doesNotContain("No hay bloqueantes materiales activos");
            assertThat(Pattern.compile("(?i)bloqueantes materiales activos NO es una precondicion").matcher(contenido).find())
                    .as("03 debe aclarar explicitamente que los bloqueantes no son precondicion de la confirmacion")
                    .isTrue();
        }

        @Test
        @DisplayName("05 documenta 401/400/404/422 y JWT en la seccion de informar pago")
        void doc05_seccion_informar_completa() throws IOException {
            List<String> lineas = Files.readAllLines(
                    specRoot.resolve("05-api-core-endpoints.md"), StandardCharsets.UTF_8);
            String seccion = extraerSeccion(lineas,
                    Pattern.compile("^###\\s+POST\\s+/api/faltas/actas/\\{id}/pago-condena/informar"));
            assertThat(seccion).contains("JWT");
            assertThat(seccion).contains("401");
            assertThat(seccion).contains("400");
            assertThat(seccion).contains("404");
            assertThat(seccion).contains("422");
        }

        @Test
        @DisplayName("05 no exige 422 por bloqueantes activos en confirmar pago")
        void doc05_confirmar_sin_422_por_bloqueantes() throws IOException {
            String contenido = leer(specRoot.resolve("05-api-core-endpoints.md"));
            assertThat(Pattern.compile("(?i)422[^\\n]{0,40}bloqueantes activos").matcher(contenido).find())
                    .as("05 no debe exigir 422 por bloqueantes activos al confirmar pago de condena")
                    .isFalse();
        }
    }

    // =====================================================================
    // G-16: Estrategia de enums persistibles coherente con el codigo
    // =====================================================================

    @Nested
    @DisplayName("G-16: estrategia de enums persistibles coherente con el codigo")
    class G16EstrategiaEnums {

        private final List<String> archivosEnumStrategia = List.of(
                "109-delta-modelo-mariadb-inmemory.md",
                "110-matriz-maestra-paridad-mariadb-inmemory.md",
                "102-slice-9-estrategia-jdbc-mariadb.md");

        private final Map<Class<?>, String> categoriaEsperadaPorEnum = Map.of(
                EstadoFalloActa.class, "NO_EXPLICIT_CODE",
                EstadoApelacionActa.class, "NO_EXPLICIT_CODE",
                EstadoPagoCondena.class, "NO_EXPLICIT_CODE",
                ResultadoFinalActa.class, "EXPLICIT_NUMERIC_CODE",
                ActorTipoEvento.class, "EXPLICIT_NUMERIC_CODE",
                OrigenEvento.class, "EXPLICIT_NUMERIC_CODE");

        private boolean tieneMetodoCodigoPublicoNumerico(Class<?> claseEnum) {
            try {
                Method metodo = claseEnum.getMethod("codigo");
                if (!Modifier.isPublic(metodo.getModifiers())) {
                    return false;
                }
                Class<?> retorno = metodo.getReturnType();
                return retorno == short.class || retorno == Short.class
                        || retorno == int.class || retorno == Integer.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }

        @Test
        @DisplayName("por reflexion, EstadoFalloActa/EstadoApelacionActa/EstadoPagoCondena no exponen codigo() numerico")
        void enums_no_explicit_code_no_tienen_codigo() {
            for (Class<?> claseEnum : List.of(EstadoFalloActa.class, EstadoApelacionActa.class, EstadoPagoCondena.class)) {
                assertThat(tieneMetodoCodigoPublicoNumerico(claseEnum))
                        .as("%s no debe exponer codigo() numerico publico (categoria NO_EXPLICIT_CODE)",
                                claseEnum.getSimpleName())
                        .isFalse();
            }
        }

        @Test
        @DisplayName("por reflexion, ResultadoFinalActa/ActorTipoEvento/OrigenEvento exponen codigo() numerico")
        void enums_explicit_numeric_code_tienen_codigo() {
            for (Class<?> claseEnum : List.of(ResultadoFinalActa.class, ActorTipoEvento.class, OrigenEvento.class)) {
                assertThat(tieneMetodoCodigoPublicoNumerico(claseEnum))
                        .as("%s debe exponer codigo() numerico publico (categoria EXPLICIT_NUMERIC_CODE)",
                                claseEnum.getSimpleName())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("110 clasifica correctamente cada enum de G-9 en su categoria de persistencia")
        void doc110_clasifica_categorias() throws IOException {
            String contenido = leer(specRoot.resolve("110-matriz-maestra-paridad-mariadb-inmemory.md"));
            List<String> violaciones = new ArrayList<>();
            for (Map.Entry<Class<?>, String> entry : categoriaEsperadaPorEnum.entrySet()) {
                String nombre = entry.getKey().getSimpleName();
                String categoria = entry.getValue();
                Pattern patron = Pattern.compile(Pattern.quote(nombre) + "[^\\n]*" + Pattern.quote(categoria));
                if (!patron.matcher(contenido).find()) {
                    violaciones.add(nombre + " -> " + categoria);
                }
            }
            assertThat(violaciones)
                    .as("110 debe clasificar cada enum en la misma linea que su categoria de persistencia")
                    .isEmpty();
        }

        @Test
        @DisplayName("DECISION_DDL-ENUM-01 esta presente en 109, 110 y 102")
        void decision_ddl_enum_01_presente() throws IOException {
            List<String> faltantes = new ArrayList<>();
            for (String archivo : archivosEnumStrategia) {
                String contenido = leer(specRoot.resolve(archivo));
                if (!contenido.contains("DECISION_DDL-ENUM-01")) {
                    faltantes.add(archivo);
                }
            }
            assertThat(faltantes).as("Documentos que deben contener DECISION_DDL-ENUM-01").isEmpty();
        }

        @Test
        @DisplayName("109/110/102 no afirman una regla universal falsa sobre codigos de enum ni fijan SMALLINT cerrado")
        void sin_afirmacion_universal_falsa() throws IOException {
            Pattern patronUniversal = Pattern.compile("(?i)todo enum[^\\n]{0,40}c[oó]digo num[eé]rico");
            List<String> violaciones = new ArrayList<>();
            for (String archivo : archivosEnumStrategia) {
                String contenido = leer(specRoot.resolve(archivo));
                if (patronUniversal.matcher(contenido).find()) {
                    violaciones.add(archivo + ": afirmacion universal de codigo numerico");
                }
                if (contenido.contains("estado_fallo SMALLINT")) {
                    violaciones.add(archivo + ": estado_fallo SMALLINT como decision cerrada");
                }
                if (contenido.contains("estado_apelacion SMALLINT")) {
                    violaciones.add(archivo + ": estado_apelacion SMALLINT como decision cerrada");
                }
            }
            assertThat(violaciones).isEmpty();
        }

        @Test
        @DisplayName("ordinal() solo aparece como prohibicion explicita en la misma linea, en 109/110/102")
        void ordinal_solo_como_prohibicion() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (String archivo : archivosEnumStrategia) {
                List<String> lineas = Files.readAllLines(specRoot.resolve(archivo), StandardCharsets.UTF_8);
                for (int i = 0; i < lineas.size(); i++) {
                    String linea = lineas.get(i);
                    if (!linea.contains("ordinal()")) {
                        continue;
                    }
                    String minuscula = linea.toLowerCase(Locale.ROOT);
                    boolean prohibido = minuscula.contains("prohibid") || minuscula.contains("no inferir")
                            || minuscula.contains("no usar");
                    if (!prohibido) {
                        violaciones.add(archivo + ":" + (i + 1) + " -> " + linea.trim());
                    }
                }
            }
            assertThat(violaciones)
                    .as("ordinal() debe aparecer solo como prohibicion explicita en la misma linea")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-17: Documentos vigentes reales, sin cronologia historica
    // =====================================================================

    @Nested
    @DisplayName("G-17: documentos vigentes reales, sin cronologia historica")
    class G17DocumentosVigentesReales {

        @Test
        @DisplayName("99 no contiene el patron '8F-'")
        void doc99_sin_8f() throws IOException {
            String contenido = leer(specRoot.resolve("99-pendientes-siguientes-slices.md"));
            assertThat(PATRON_8F.matcher(contenido).find()).isFalse();
        }

        @Test
        @DisplayName("102 sin fases/builds/IDs historicos")
        void doc102_sin_historicos() throws IOException {
            String contenido = leer(specRoot.resolve("102-slice-9-estrategia-jdbc-mariadb.md"));
            assertThat(PATRON_8F.matcher(contenido).find()).as("102 no debe contener '8F-'").isFalse();
            assertThat(contenido).doesNotContain("8C-");
            assertThat(contenido).doesNotContain("**Fase:**");
            assertThat(Pattern.compile("(?i)build[^\\n]{0,20}tests").matcher(contenido).find())
                    .as("102 no debe contener conteos de build historicos")
                    .isFalse();
            assertThat(contenido).doesNotContain("Tests run:");
            assertThat(contenido).doesNotContain("FalActaFirmezaCondena");
            assertThat(contenido).doesNotContain("FirmezaCondenaRepository");
        }

        @Test
        @DisplayName("103 clasificado SUPPORTING_CURRENT en banner y registro")
        void doc103_clasificado_supporting_current() throws IOException {
            Path archivo = specRoot.resolve("103-slice-9-1-infraestructura-jdbc.md");
            Banner banner = leerBanner(archivo);
            assertThat(banner).as("103 debe tener banner de Estado documental/Autoridad DDL").isNotNull();
            assertThat(banner.clasificacion()).isEqualTo("SUPPORTING_CURRENT");

            Map<String, FilaRegistro> filasPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, f -> f, (a, b) -> a));
            FilaRegistro fila = filasPorPath.get("103-slice-9-1-infraestructura-jdbc.md");
            assertThat(fila).as("103 debe estar en el registro documental").isNotNull();
            assertThat(fila.clasificacion()).isEqualTo("SUPPORTING_CURRENT");
        }

        @Test
        @DisplayName("03 sin headings historicos CIERRE-/FIX-/R-N")
        void doc03_sin_headings_historicos() throws IOException {
            List<String> lineas = Files.readAllLines(
                    specRoot.resolve("03-comandos-precondiciones-efectos.md"), StandardCharsets.UTF_8);
            List<String> violaciones = new ArrayList<>();
            for (String linea : lineas) {
                if (PATRON_HEADING_CIERRE.matcher(linea).find()
                        || PATRON_HEADING_FIX.matcher(linea).find()
                        || PATRON_HEADING_R_HISTORICO.matcher(linea).find()) {
                    violaciones.add(linea.trim());
                }
            }
            assertThat(violaciones)
                    .as("03 no debe contener headings historicos CIERRE-/FIX-/R-N")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-18: Gate READY_FOR_DDL condicionado a G-15, G-16 y G-17
    // =====================================================================

    @Nested
    @DisplayName("G-18: READY_FOR_DDL condicionado a G-15, G-16 y G-17")
    class G18GateCondicionado {

        @Test
        @DisplayName("el literal READY_FOR_DDL solo es valido si las condiciones de G-15/G-16/G-17 se cumplen simultaneamente")
        void gate_condicionado() throws IOException {
            List<String> violaciones = new ArrayList<>();

            String doc02 = leer(specRoot.resolve("02-estados-bloques-eventos.md"));
            if (!Pattern.compile("(?i)PCOCNF se registra siempre").matcher(doc02).find()) {
                violaciones.add("G-15: 02 no afirma 'PCOCNF se registra siempre'");
            }
            if (doc02.contains("Confirmar pago solo si no hay bloqueantes") || doc02.contains("no registrar PCOCNF")) {
                violaciones.add("G-15: 02 contiene afirmaciones contradictorias sobre pago de condena");
            }

            String doc03 = leer(specRoot.resolve("03-comandos-precondiciones-efectos.md"));
            if (doc03.contains("### InformarPagoCondenaCommand")
                    || doc03.contains("No hay bloqueantes materiales activos")) {
                violaciones.add("G-15: 03 duplica InformarPagoCondenaCommand o exige ausencia de bloqueantes");
            }

            String doc05 = leer(specRoot.resolve("05-api-core-endpoints.md"));
            if (Pattern.compile("(?i)422[^\\n]{0,40}bloqueantes activos").matcher(doc05).find()) {
                violaciones.add("G-15: 05 exige 422 por bloqueantes activos en confirmar");
            }

            String doc109 = leer(specRoot.resolve("109-delta-modelo-mariadb-inmemory.md"));
            String doc110 = leer(specRoot.resolve("110-matriz-maestra-paridad-mariadb-inmemory.md"));
            String doc102 = leer(specRoot.resolve("102-slice-9-estrategia-jdbc-mariadb.md"));
            for (Map.Entry<String, String> entry : Map.of(
                    "109", doc109, "110", doc110, "102", doc102).entrySet()) {
                if (!entry.getValue().contains("DECISION_DDL-ENUM-01")) {
                    violaciones.add("G-16: falta DECISION_DDL-ENUM-01 en " + entry.getKey());
                }
            }
            if (doc110.contains("estado_fallo SMALLINT") || doc110.contains("estado_apelacion SMALLINT")) {
                violaciones.add("G-16: 110 fija estado_fallo/estado_apelacion SMALLINT como decision cerrada");
            }

            String doc99 = leer(specRoot.resolve("99-pendientes-siguientes-slices.md"));
            if (PATRON_8F.matcher(doc99).find()) {
                violaciones.add("G-17: 99 contiene '8F-'");
            }
            if (PATRON_8F.matcher(doc102).find() || doc102.contains("8C-")) {
                violaciones.add("G-17: 102 contiene identificadores historicos");
            }
            Banner banner103 = leerBanner(specRoot.resolve("103-slice-9-1-infraestructura-jdbc.md"));
            if (banner103 == null || !"SUPPORTING_CURRENT".equals(banner103.clasificacion())) {
                violaciones.add("G-17: 103 no esta clasificado SUPPORTING_CURRENT en su banner");
            }

            assertThat(violaciones)
                    .as("El gate READY_FOR_DDL no puede considerarse valido: hay violaciones de G-15/G-16/G-17")
                    .isEmpty();

            String doc101 = leer(specRoot.resolve("101-auditoria-pre-jdbc-mariadb.md"));
            assertThat(doc101)
                    .as("Solo una vez confirmado que no hay violaciones, el literal READY_FOR_DDL debe estar declarado en 101")
                    .contains("READY_FOR_DDL");
        }
    }
}
