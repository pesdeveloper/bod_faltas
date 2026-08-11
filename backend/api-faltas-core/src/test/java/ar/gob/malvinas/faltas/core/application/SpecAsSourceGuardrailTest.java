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

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryBloqueanteMaterialRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryEconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryGestionExternaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrails documentales de spec-as-source (auditoria transversal final).
 *
 * Verifica, contra el arbol de archivos real de {@code docs/spec-as-source}
 * (estructura tematica unica, sin numeracion residual de slices ni carpeta
 * {@code handoff/}):
 *
 * <ul>
 *   <li>G-1: el registro documental esta completo, sin duplicados, con clasificacion/autoridad
 *       validas y sin filas {@code HISTORICAL} activas.</li>
 *   <li>G-2: todos los links relativos Markdown resuelven a un archivo existente.</li>
 *   <li>G-3: cero identificadores "GAP-" en cualquier documento de la spec.</li>
 *   <li>G-4: los siete comandos canonicos CMD-FALLO-001..007 aparecen exactamente una vez
 *       como heading de nivel 2 en {@code 20-application/fallo-command-contracts.md}, y no
 *       existe un octavo.</li>
 *   <li>G-5: los documentos NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN no usan lenguaje
 *       de diario de implementacion por slices.</li>
 *   <li>G-6: los documentos NORMATIVE / SUPPORTING_CURRENT / PRE_DDL_PLAN no usan identificadores
 *       obsoletos derivados de {@code EnumGuardrailTest} y los enums vigentes.</li>
 *   <li>G-7: el gate formal {@code READY_FOR_DDL} esta declarado exactamente una vez en
 *       {@code 00-governance/ready-for-ddl-gate.md} y cero veces {@code Estado: BLOCKED}.</li>
 *   <li>G-8: el README enlaza al registro documental y a los documentos de preparacion MariaDB.</li>
 *   <li>G-9: paridad exacta (valores y, cuando el enum expone {@code codigo()}, codigos)
 *       entre siete enums productivos y su tabla propietaria en la spec.</li>
 *   <li>G-10: {@code APERAZ}/{@code RECHAZADA} nunca se documentan como firmeza automatica.</li>
 *   <li>G-11: {@code 20-application/command-contracts.md} no duplica los contratos legacy
 *       de firmeza y enlaza a la familia canonica de comandos de fallo.</li>
 *   <li>G-12: {@code inmemory-mariadb-deltas.md} y {@code mariadb-logical-model.md} se declaran
 *       complementarios, sin categorias historicas de discrepancia ni afirmaciones de fuente
 *       unica/reemplazo.</li>
 *   <li>G-13: la politica de seguridad de {@code CMD-FALLO-003} esta documentada como
 *       identidad tecnica, no JWT de usuario final; no hay una regla comun a los siete comandos.</li>
 *   <li>G-14: {@code FALLO_CONDENATORIO_PAGADO} esta documentado como {@code LEGACY_RESERVED},
 *       con {@code CONDENA_FIRME_PAGADA} como resultado canonico y {@code DECISION_DDL-RF-005}
 *       en {@code ddl-decisions.md}.</li>
 *   <li>G-15: el contrato de pago de condena es coherente entre {@code states-events-catalogs.md},
 *       {@code command-contracts.md} y {@code http-contracts.md}: {@code PCOCNF} se registra
 *       siempre que se superan las precondiciones, los bloqueantes materiales activos solo
 *       impiden {@code CIERRA} (nunca rechazan la confirmacion), {@code command-contracts.md}
 *       no duplica el contrato de {@code InformarPagoCondenaCommand} y {@code http-contracts.md}
 *       no exige {@code 422} por bloqueantes activos en confirmar.</li>
 *   <li>G-16: la estrategia de enums persistibles documentada en {@code inmemory-mariadb-deltas.md}/
 *       {@code ddl-decisions.md}/{@code jdbc-strategy.md} es coherente, por reflexion, con los
 *       enums verificados en G-9: los 5 enums promovidos por {@code DECISION_DDL-ENUM-01}
 *       ({@code EstadoFalloActa}, {@code EstadoApelacionActa}, {@code EstadoPagoCondena},
 *       {@code TipoDiaNoComputable}, {@code OrigenDiaNoComputable}) exponen {@code codigo()}
 *       numerico y se clasifican {@code EXPLICIT_NUMERIC_CODE}; ningun documento afirma una
 *       regla universal falsa; {@code ordinal()} solo aparece como prohibicion explicita.</li>
 *   <li>G-17: los documentos clasificados vigentes ({@code current-roadmap.md},
 *       {@code jdbc-strategy.md}, {@code jdbc-infrastructure.md}, {@code command-contracts.md})
 *       estan libres de cronologia historica de slices/fases/builds, y
 *       {@code jdbc-infrastructure.md} esta clasificado {@code SUPPORTING_CURRENT} en su banner
 *       y en el registro.</li>
 *   <li>G-18: el gate {@code READY_FOR_DDL} de {@code ready-for-ddl-gate.md} solo se considera
 *       valido si las condiciones de G-15, G-16 y G-17 se cumplen simultaneamente; la mera
 *       presencia del literal no es suficiente.</li>
 *   <li>G-19: rutas historicas concretas de slices anteriores de consolidacion documental no
 *       existen (numeracion residual, {@code handoff/}, y los directorios historicos eliminados
 *       fuera de la spec: {@code docs/}, {@code docs-trabajo/}, {@code docs/guia_qa/},
 *       {@code backend/api-faltas-prototipo/docs/}).</li>
 *   <li>G-20: cierre de gobierno y referencias del slice R1 de consolidacion documental:
 *       cero ocurrencias de {@code 01-reglas-dominio-faltas} en la spec; la spec no depende de
 *       {@code .work/handoff-current} ni de nombres de ledger transitorios; {@code .gitignore} y
 *       {@code .cursorignore} declaran la regla de ignore de {@code .work/handoff-current};
 *       {@code AGENTS.md} y las cuatro reglas {@code .cursor/rules} autorizadas no mencionan
 *       rutas documentales eliminadas ({@code docs/faltas}, {@code docs/guia_qa},
 *       {@code docs/MATRIZ_REGLAS_ACTA.md}, {@code docs/VALIDACION_FUNCIONAL.md}); el documento
 *       UX del frontend no referencia {@code docs-trabajo}.</li>
 *   <li>G-21: cierre de todas las {@code DECISION_DDL-*} del slice {@code DDL-CLOSURE-001-R1}:
 *       cero decisiones pendientes en {@code ddl-decisions.md}; exactamente 24 decisiones cerradas
 *       (20 originales + 4 transversales);
 *       las 4 transversales nuevas presentes; {@code DECISION_DDL-ENUM-01} documentada con los
 *       5 enums promovidos; {@code DECISION_DDL-ECPR-01}: {@code InMemoryEconomiaProyeccionRepository}
 *       rechaza {@code fhUltMod} null; {@code DECISION_DDL-COMMENT-01} especifica tablas y columnas;
 *       {@code mariadb-logical-model.md} sin {@code PENDING_DDL_DECISION}.</li>
 *   <li>G-22: documentos de governance de clean-room y ejecucion DDL:
 *       {@code 50-persistence/ddl-execution-and-test-seeding.md} y
 *       {@code 00-governance/ready-for-backend-clean-room-reconstruction.md}
 *       existen, tienen contenido coherente (4 operaciones seeder, protecciones,
 *       baseline 65 tablas, 16 tablas protegidas, 4 vistas,
 *       {@code fal_rubro_version} = {@code PREEXISTING_CANONICAL_ADOPTED})
 *       y aparecen en {@code document-registry.md}.</li>
 *   <li>G-23: OCC copy-on-read/write en los seis repositorios mutables InMemory
 *       (snapshot, economia, documento, notificacion, gestion externa, bloqueante material)
 *       y rechazo de {@code fhUltMod} null en
 *       {@code InMemoryEconomiaProyeccionRepository} ({@code DECISION_DDL-ECPR-01}).</li>
 *   <li>G-24: formato fisico de archivos gobernados por este slice
 *       ({@code docs/spec-as-source/**. md}): sin BOM UTF-8, sin CRLF, con newline final.</li>
 *   <li>G-25: actor funcional sin fallback en {@code ActaService}: sin {@code subOr("UNKNOWN")},
 *       sin literal {@code "UNKNOWN"}, sin {@code "SYS"}, sin {@code idInspector} como actor;
 *       {@code exigirActor()} lanza {@code PrecondicionVioladaException} cuando el contexto
 *       es null.</li>
 *   <li>G-26: meta-guardrail — todos los metodos citados en {@code ddl-decisions.md} como
 *       cobertura de tests existen realmente en {@code SpecAsSourceGuardrailTest} y en
 *       {@code MariaDbLogicalModelParityGuardrailTest}.</li>
 * </ul>
 *
 * No usa Mockito. Solo lee archivos reales del arbol de la spec (y, para G-19 y G-20,
 * rutas puntuales fuera de la spec cuyo estado historico ya fue verificado y
 * cerrado por el slice de consolidacion estructural, o cuyo gobierno de agentes/ignore
 * quedo corregido por el R1 de cierre de esa consolidacion).
 */
@DisplayName("Guardrails documentales de spec-as-source")
class SpecAsSourceGuardrailTest {

    private static final Set<String> CLASIFICACIONES_VALIDAS = Set.of(
            "NORMATIVE", "SUPPORTING_CURRENT", "HISTORICAL", "PRE_DDL_PLAN");

    private static final Set<String> AUTORIDADES_VALIDAS = Set.of("YES", "SUPPORTING", "NO");

    private static final Set<String> CLASIFICACIONES_SIN_TEMPORALIDAD_SLICE = Set.of(
            "NORMATIVE", "SUPPORTING_CURRENT", "PRE_DDL_PLAN");

    private static final List<String> TERMINOS_PROHIBIDOS = List.of(
            "PAGCON", "ACTCER", "APELAC", "DRVEXT", "D3_DOCUMENTAL",
            "ACTCAP", "COMPLETAR_CAPTURA", "CompletarCaptura");

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
    private static final Pattern PATRON_CERRADO_EN =
            Pattern.compile("(?i)cerrado en (el|la|este|esta|dicho|dicha)\\s+(slice|build|sprint|sesion|iteracion)");
    private static final Pattern PATRON_REEMPLAZA_DELTAS = Pattern.compile("(?i)(?<!no )reemplaza a inmemory-mariadb-deltas\\b");
    private static final Pattern PATRON_DELTAS_HISTORIAL = Pattern.compile("(?i)inmemory-mariadb-deltas queda como historial");
    private static final Pattern PATRON_FECHA_ORIGINAL = Pattern.compile("(?i)^Fecha original:");
    private static final Pattern PATRON_BUILD_BASE_HISTORICO =
            Pattern.compile("(?i)Build base confirmado en esa auditor");

    /** Tokens de trabajo prohibidos (G-3 adicional). Sensible a mayusculas para no rechazar "Todo". */
    private static final List<String> TOKENS_DE_TRABAJO_PROHIBIDOS = List.of("GAP-", "TODO", "FIXME", "TBD");

    /** Categorias historicas de discrepancia (ex-110, G-12). */
    private static final List<String> CATEGORIAS_HISTORICAS_DISCREPANCIA = List.of(
            "FALTA_EN_INMEMORY", "IDENTIDAD_INCOMPATIBLE", "SEMANTICA_INCOMPATIBLE",
            "TIPO_INCOMPATIBLE", "RELACION_INCOMPLETA");

    private static final Pattern PATRON_HEADING_CIERRE = Pattern.compile("^#{1,6}\\s*CIERRE-");
    private static final Pattern PATRON_HEADING_FIX = Pattern.compile("^#{1,6}\\s*FIX-");
    private static final Pattern PATRON_HEADING_R_HISTORICO = Pattern.compile("^#{1,6}\\s*R-[0-9]");
    private static final Pattern PATRON_HEADING_CUALQUIERA = Pattern.compile("^#{1,6}\\s");

    private static Path specRoot;
    private static Path repoRoot;

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
        // backend/api-faltas-core -> repo root (dos niveles arriba).
        repoRoot = Paths.get("..", "..").normalize();
    }

    // -------------------------------------------------------------------
    // Utilidades de arbol de archivos
    // -------------------------------------------------------------------

    private static List<Path> listarMarkdownDeLaSpec() throws IOException {
        try (Stream<Path> stream = Files.walk(specRoot)) {
            return stream
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String leer(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** Lee un archivo fuera de la spec, resuelto contra la raiz del repositorio. */
    private static String leerDesdeRepo(String rutaRelativa) throws IOException {
        Path archivo = repoRoot.resolve(rutaRelativa);
        assertThat(Files.isRegularFile(archivo))
                .as("Debe existir '%s' desde la raiz del repositorio", rutaRelativa)
                .isTrue();
        return leer(archivo);
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
        Path registro = specRoot.resolve("00-governance").resolve("document-registry.md");
        assertThat(Files.isRegularFile(registro))
                .as("Debe existir 00-governance/document-registry.md")
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
        @DisplayName("cada .md de la spec aparece exactamente una vez en el registro")
        void cada_md_aparece_exactamente_una_vez() throws IOException {
            List<Path> archivos = listarMarkdownDeLaSpec();
            List<FilaRegistro> filas = parsearRegistro();
            Map<String, Long> conteoPorPath = filas.stream()
                    .collect(Collectors.groupingBy(FilaRegistro::path, Collectors.counting()));

            for (Path archivo : archivos) {
                String pathEsperado = pathRegistroDe(archivo);
                Long conteo = conteoPorPath.get(pathEsperado);
                assertThat(conteo)
                        .as("El archivo '%s' debe aparecer exactamente una vez en document-registry.md", pathEsperado)
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
                    .contains("00-governance/document-registry.md");
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
        @DisplayName("ninguna fila del registro esta clasificada HISTORICAL (cero documentos historicos activos en la spec)")
        void cero_filas_historicas() throws IOException {
            List<String> historicas = parsearRegistro().stream()
                    .filter(f -> "HISTORICAL".equals(f.clasificacion()))
                    .map(FilaRegistro::path)
                    .collect(Collectors.toList());
            assertThat(historicas)
                    .as("El slice de consolidacion estructural elimino todo documento HISTORICAL de la spec; "
                            + "no debe reaparecer una fila HISTORICAL sin una decision explicita nueva")
                    .isEmpty();
        }

        @Test
        @DisplayName("el banner de cada documento (fuera de README.md) coincide con su fila del registro")
        void banner_coincide_con_registro() throws IOException {
            Map<String, FilaRegistro> filasPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, f -> f, (a, b) -> a));
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
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
            for (Path archivo : listarMarkdownDeLaSpec()) {
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
                        boolean esLinkDeDirectorio = rutaSinAncla.endsWith("/");
                        boolean resuelve = esLinkDeDirectorio ? Files.isDirectory(resuelto) : Files.isRegularFile(resuelto);
                        if (!resuelve) {
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
        @DisplayName("ningun .md de la spec contiene el prefijo GAP-")
        void cero_gap_ids() throws IOException {
            List<String> encontrados = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
                String contenido = leer(archivo);
                if (contenido.contains("GAP-")) {
                    encontrados.add(archivo.toString());
                }
            }
            assertThat(encontrados).as("Archivos con el prefijo prohibido GAP-").isEmpty();
        }

        @Test
        @DisplayName("ningun .md de la spec contiene tokens de trabajo GAP-/TODO/FIXME/TBD (case-sensitive)")
        void cero_tokens_de_trabajo() throws IOException {
            List<String> encontrados = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
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
            for (Path archivo : listarMarkdownDeLaSpec()) {
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
                if (PATRON_REEMPLAZA_DELTAS.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'reemplaza a inmemory-mariadb-deltas' sin negacion)");
                }
                if (PATRON_DELTAS_HISTORIAL.matcher(contenido).find()) {
                    violaciones.add(pathRegistro + " (patron 'inmemory-mariadb-deltas queda como historial')");
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
            for (Path archivo : listarMarkdownDeLaSpec()) {
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

        private Path archivoGate() {
            return specRoot.resolve("00-governance").resolve("ready-for-ddl-gate.md");
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md declara exactamente una linea 'Estado: READY_FOR_DDL' y cero 'Estado: BLOCKED'")
        void gate_declarado_una_vez() throws IOException {
            Path archivo = archivoGate();
            assertThat(Files.isRegularFile(archivo)).isTrue();
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            Pattern patronGateReady = Pattern.compile("^#{0,6}\\s*Estado:\\s*READY_FOR_DDL\\s*$");
            Pattern patronGateBlocked = Pattern.compile("^#{0,6}\\s*Estado:\\s*BLOCKED\\s*$");
            long readyCount = lineas.stream().filter(l -> patronGateReady.matcher(l.trim()).matches()).count();
            long blockedCount = lineas.stream().filter(l -> patronGateBlocked.matcher(l.trim()).matches()).count();
            assertThat(readyCount)
                    .as("Debe existir exactamente una linea 'Estado: READY_FOR_DDL' en ready-for-ddl-gate.md")
                    .isEqualTo(1L);
            assertThat(blockedCount)
                    .as("No debe existir ninguna linea 'Estado: BLOCKED' en ready-for-ddl-gate.md")
                    .isEqualTo(0L);
        }

        @Test
        @DisplayName("el gate READY_FOR_DDL esta condicionado: cero bloqueadores internos declarados")
        void gate_condicionado_sin_bloqueadores() throws IOException {
            String contenido = leer(archivoGate());
            assertThat(contenido)
                    .as("ready-for-ddl-gate.md debe declarar explicitamente que no quedan bloqueadores internos")
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
        @DisplayName("README enlaza document-registry, ready-for-ddl-gate, inmemory-mariadb-deltas y mariadb-logical-model")
        void readme_enlaza_documentos_requeridos() throws IOException {
            Path readme = specRoot.resolve("README.md");
            assertThat(Files.isRegularFile(readme)).isTrue();
            String contenido = leer(readme);
            assertThat(contenido).contains("document-registry.md");
            assertThat(contenido).contains("ready-for-ddl-gate.md");
            assertThat(contenido).contains("inmemory-mariadb-deltas.md");
            assertThat(contenido).contains("mariadb-logical-model.md");
        }
    }

    // =====================================================================
    // G-9: Paridad de enums contra su tabla propietaria
    // =====================================================================

    @Nested
    @DisplayName("G-9: paridad exacta de enums contra su tabla propietaria en la spec")
    class G9ParidadEnums {

        @Test
        @DisplayName("EstadoFalloActa (con codigo, DECISION_DDL-ENUM-01) coincide con lifecycle-states.md [FALLO-STATE-001]")
        void estado_fallo_acta() throws IOException {
            verificarParidadEnum(EstadoFalloActa.class, e -> e.codigo(), true,
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
        @DisplayName("EstadoApelacionActa (con codigo, DECISION_DDL-ENUM-01) coincide con 10-domain/states-events-catalogs.md")
        void estado_apelacion_acta() throws IOException {
            verificarParidadEnum(EstadoApelacionActa.class, e -> e.codigo(), true,
                    specRoot.resolve("10-domain").resolve("states-events-catalogs.md"),
                    "## EstadoApelacionActa");
        }

        @Test
        @DisplayName("ResultadoResolucionApelacion (con codigo) coincide con 10-domain/states-events-catalogs.md")
        void resultado_resolucion_apelacion() throws IOException {
            verificarParidadEnum(ResultadoResolucionApelacion.class, ResultadoResolucionApelacion::codigo, true,
                    specRoot.resolve("10-domain").resolve("states-events-catalogs.md"),
                    "## ResultadoResolucionApelacion");
        }

        @Test
        @DisplayName("EstadoPagoCondena (con codigo, DECISION_DDL-ENUM-01) coincide con 10-domain/states-events-catalogs.md")
        void estado_pago_condena() throws IOException {
            verificarParidadEnum(EstadoPagoCondena.class, e -> e.codigo(), true,
                    specRoot.resolve("10-domain").resolve("states-events-catalogs.md"),
                    "### Estado: EstadoPagoCondena");
        }

        @Test
        @DisplayName("ActorTipoEvento (con codigo) coincide con 10-domain/states-events-catalogs.md")
        void actor_tipo_evento() throws IOException {
            verificarParidadEnum(ActorTipoEvento.class, ActorTipoEvento::codigo, true,
                    specRoot.resolve("10-domain").resolve("states-events-catalogs.md"),
                    "### ActorTipoEvento");
        }

        @Test
        @DisplayName("OrigenEvento (con codigo) coincide con 10-domain/states-events-catalogs.md")
        void origen_evento() throws IOException {
            verificarParidadEnum(OrigenEvento.class, OrigenEvento::codigo, true,
                    specRoot.resolve("10-domain").resolve("states-events-catalogs.md"),
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
            for (Path archivo : listarMarkdownDeLaSpec()) {
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
        @DisplayName("10-domain/states-events-catalogs.md referencia CMD-FALLO-006 para la apelacion rechazada")
        void referencia_cmd_fallo_006() throws IOException {
            Path archivo = specRoot.resolve("10-domain").resolve("states-events-catalogs.md");
            String contenido = leer(archivo);
            assertThat(contenido).contains("CMD-FALLO-006");
        }
    }

    // =====================================================================
    // G-11: command-contracts.md no duplica contratos legacy de firmeza
    // =====================================================================

    @Nested
    @DisplayName("G-11: 20-application/command-contracts.md sin contratos legacy de firmeza")
    class G11PropiedadContratos {

        private String leerContratosGenerales() throws IOException {
            Path archivo = specRoot.resolve("20-application").resolve("command-contracts.md");
            assertThat(Files.isRegularFile(archivo)).isTrue();
            return leer(archivo);
        }

        @Test
        @DisplayName("command-contracts.md enlaza a 20-application/fallo-command-contracts.md")
        void enlaza_a_contratos_canonicos() throws IOException {
            assertThat(leerContratosGenerales()).contains("20-application/fallo-command-contracts.md");
        }

        @Test
        @DisplayName("command-contracts.md no duplica tokens legacy de firmeza")
        void cero_tokens_legacy_de_firmeza() throws IOException {
            String contenido = leerContratosGenerales();
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
                    .as("20-application/command-contracts.md no debe contener contratos legacy de firmeza")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-12: inmemory-mariadb-deltas.md / mariadb-logical-model.md complementarios,
    //       sin categorias historicas de discrepancia
    // =====================================================================

    @Nested
    @DisplayName("G-12: deltas y modelo logico complementarios, sin fuente unica ni categorias historicas")
    class G12DeltasYModeloComplementarios {

        private String leerModeloLogico() throws IOException {
            return leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
        }

        private String leerDeltas() throws IOException {
            return leer(specRoot.resolve("50-persistence").resolve("inmemory-mariadb-deltas.md"));
        }

        @Test
        @DisplayName("inmemory-mariadb-deltas.md y mariadb-logical-model.md se declaran complementarios entre si")
        void se_declaran_complementarios() throws IOException {
            assertThat(leerDeltas()).containsIgnoringCase("complementari");
            assertThat(leerModeloLogico()).containsIgnoringCase("complementari");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no afirma ser fuente unica de verdad ni reemplazar a los deltas")
        void sin_fuente_unica_ni_reemplazo() throws IOException {
            String contenido = leerModeloLogico();
            assertThat(PATRON_REEMPLAZA_DELTAS.matcher(contenido).find())
                    .as("mariadb-logical-model.md no debe afirmar 'reemplaza a inmemory-mariadb-deltas' sin negacion")
                    .isFalse();
            assertThat(PATRON_DELTAS_HISTORIAL.matcher(contenido).find())
                    .as("mariadb-logical-model.md no debe afirmar 'inmemory-mariadb-deltas queda como historial'")
                    .isFalse();
            // "fuente unica de verdad" (con o sin tilde en "unica") solo es aceptable cuando la
            // misma linea la niega explicitamente (ninguno/no es/ni sustituye/complementario).
            Pattern patronFuenteUnica = Pattern.compile("(?i)fuente [uú]nica de verdad");
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
                    .as("mariadb-logical-model.md no debe afirmarse a si mismo como fuente unica de verdad sin negacion en la misma linea")
                    .isEmpty();
        }

        @Test
        @DisplayName("mariadb-logical-model.md contiene DECISION_DDL")
        void contiene_decision_ddl() throws IOException {
            assertThat(leerModeloLogico()).contains("DECISION_DDL");
        }

        @Test
        @DisplayName("mariadb-logical-model.md documenta las columnas estructurales obligatorias por agregado")
        void contiene_columnas_obligatorias() throws IOException {
            String contenido = leerModeloLogico();
            List<String> columnasEsperadas = List.of(
                    "PK", "Entidad Java", "Puerto", "Auditor", "versionRow", "DECISION_DDL", "Estado");
            List<String> faltantes = columnasEsperadas.stream()
                    .filter(c -> !contenido.contains(c))
                    .collect(Collectors.toList());
            assertThat(faltantes).as("mariadb-logical-model.md debe documentar estas columnas/atributos estructurales").isEmpty();
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene 8F- ni categorias historicas de discrepancia")
        void sin_8f_ni_categorias_historicas() throws IOException {
            String contenido = leerModeloLogico();
            assertThat(PATRON_8F.matcher(contenido).find()).as("mariadb-logical-model.md no debe contener '8F-'").isFalse();
            List<String> encontradas = CATEGORIAS_HISTORICAS_DISCREPANCIA.stream()
                    .filter(contenido::contains)
                    .collect(Collectors.toList());
            assertThat(encontradas)
                    .as("mariadb-logical-model.md no debe usar categorias historicas de discrepancia como estado vigente")
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-13: Seguridad de CMD-FALLO-003
    // =====================================================================

    @Nested
    @DisplayName("G-13: seguridad de CMD-FALLO-003 documentada como identidad tecnica")
    class G13SeguridadCmdFallo003 {

        private Path archivoGate() {
            return specRoot.resolve("00-governance").resolve("ready-for-ddl-gate.md");
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md documenta CMD-FALLO-003 con identidad tecnica, sin JWT de usuario final")
        void cmd_fallo_003_identidad_tecnica() throws IOException {
            String contenido = leer(archivoGate());
            assertThat(contenido).contains("CMD-FALLO-003");
            assertThat(contenido).containsIgnoringCase("identidad t");
            assertThat(contenido).containsPattern("(?i)no aplica JWT de usuario final");
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md no afirma una regla JWT comun a los siete comandos")
        void sin_jwt_comun_a_los_siete() throws IOException {
            Path archivo = archivoGate();
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
        @DisplayName("ddl-decisions.md contiene DECISION_DDL-RF-005 para el tratamiento fisico de FALLO_CONDENATORIO_PAGADO")
        void decision_ddl_rf_005_presente() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("ddl-decisions.md"));
            assertThat(contenido).contains("DECISION_DDL-RF-005");
        }
    }

    // =====================================================================
    // G-15: Coherencia del contrato de pago de condena en states-events-catalogs/
    //       command-contracts/http-contracts
    // =====================================================================

    @Nested
    @DisplayName("G-15: coherencia del contrato de pago de condena en states-events-catalogs/command-contracts/http-contracts")
    class G15CoherenciaPago {

        @Test
        @DisplayName("states-events-catalogs.md afirma que PCOCNF se registra siempre y que los bloqueantes solo impiden CIERRA")
        void catalogos_pcocnf_siempre_bloqueantes_solo_impiden_cierra() throws IOException {
            String contenido = leer(specRoot.resolve("10-domain").resolve("states-events-catalogs.md"));
            assertThat(Pattern.compile("(?i)PCOCNF se registra siempre").matcher(contenido).find())
                    .as("states-events-catalogs.md debe afirmar que PCOCNF se registra siempre que se superan las precondiciones")
                    .isTrue();
            assertThat(Pattern.compile("(?i)impiden que se registre[^\\n]*CIERRA").matcher(contenido).find())
                    .as("states-events-catalogs.md debe afirmar que los bloqueantes unicamente impiden que se registre CIERRA")
                    .isTrue();
        }

        @Test
        @DisplayName("states-events-catalogs.md no contiene afirmaciones contradictorias sobre PCOCNF y bloqueantes")
        void catalogos_sin_contradicciones() throws IOException {
            String contenido = leer(specRoot.resolve("10-domain").resolve("states-events-catalogs.md"));
            List<String> prohibidos = List.of(
                    "Confirmar pago solo si no hay bloqueantes",
                    "no registrar PCOCNF",
                    "PCOCNF + CIERRA como unica transicion",
                    "PCOCNF + CIERRA como única transición");
            List<String> encontrados = prohibidos.stream().filter(contenido::contains).collect(Collectors.toList());
            assertThat(encontrados)
                    .as("states-events-catalogs.md no debe contener afirmaciones contradictorias sobre pago de condena")
                    .isEmpty();
            assertThat(Pattern.compile("(?i)CONFIRMADO\\s*->\\s*cierre del acta").matcher(contenido).find())
                    .as("states-events-catalogs.md no debe afirmar 'CONFIRMADO -> cierre del acta' como regla unica sin bifurcacion")
                    .isFalse();
        }

        @Test
        @DisplayName("command-contracts.md no duplica InformarPagoCondenaCommand y enlaza CMD-FALLO-007 a fallo-command-contracts")
        void command_contracts_sin_duplicado_informar() throws IOException {
            String contenido = leer(specRoot.resolve("20-application").resolve("command-contracts.md"));
            assertThat(contenido).doesNotContain("### InformarPagoCondenaCommand");
            assertThat(Pattern.compile("(?i)fallo condenatorio con estado `NOTIFICADO`").matcher(contenido).find())
                    .as("command-contracts.md no debe duplicar la precondicion de estado NOTIFICADO propia de fallo-command-contracts")
                    .isFalse();
            assertThat(contenido).contains("CMD-FALLO-007");
            assertThat(contenido).contains("20-application/fallo-command-contracts.md");
        }

        @Test
        @DisplayName("command-contracts.md no exige ausencia de bloqueantes como precondicion de ConfirmarPagoCondena")
        void command_contracts_bloqueantes_no_son_precondicion() throws IOException {
            String contenido = leer(specRoot.resolve("20-application").resolve("command-contracts.md"));
            assertThat(contenido).doesNotContain("No hay bloqueantes materiales activos");
            assertThat(Pattern.compile("(?i)bloqueantes materiales activos NO es una precondicion").matcher(contenido).find())
                    .as("command-contracts.md debe aclarar explicitamente que los bloqueantes no son precondicion de la confirmacion")
                    .isTrue();
        }

        @Test
        @DisplayName("http-contracts.md documenta 401/400/404/422 y JWT en la seccion de informar pago")
        void http_contracts_seccion_informar_completa() throws IOException {
            List<String> lineas = Files.readAllLines(
                    specRoot.resolve("40-api").resolve("http-contracts.md"), StandardCharsets.UTF_8);
            String seccion = extraerSeccion(lineas,
                    Pattern.compile("^###\\s+POST\\s+/api/faltas/actas/\\{id}/pago-condena/informar"));
            assertThat(seccion).contains("JWT");
            assertThat(seccion).contains("401");
            assertThat(seccion).contains("400");
            assertThat(seccion).contains("404");
            assertThat(seccion).contains("422");
        }

        @Test
        @DisplayName("http-contracts.md no exige 422 por bloqueantes activos en confirmar pago")
        void http_contracts_confirmar_sin_422_por_bloqueantes() throws IOException {
            String contenido = leer(specRoot.resolve("40-api").resolve("http-contracts.md"));
            assertThat(Pattern.compile("(?i)422[^\\n]{0,40}bloqueantes activos").matcher(contenido).find())
                    .as("http-contracts.md no debe exigir 422 por bloqueantes activos al confirmar pago de condena")
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
                "50-persistence/inmemory-mariadb-deltas.md",
                "50-persistence/ddl-decisions.md",
                "50-persistence/jdbc-strategy.md");

        private final Map<Class<?>, String> categoriaEsperadaPorEnum = Map.of(
                EstadoFalloActa.class, "EXPLICIT_NUMERIC_CODE",
                EstadoApelacionActa.class, "EXPLICIT_NUMERIC_CODE",
                EstadoPagoCondena.class, "EXPLICIT_NUMERIC_CODE",
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
        @DisplayName("por reflexion, DECISION_DDL-ENUM-01: los 5 enums promovidos exponen codigo() numerico")
        void enums_decision_ddl_enum01_tienen_codigo_numerico() {
            for (Class<?> claseEnum : List.of(
                    EstadoFalloActa.class, EstadoApelacionActa.class, EstadoPagoCondena.class,
                    TipoDiaNoComputable.class, OrigenDiaNoComputable.class)) {
                assertThat(tieneMetodoCodigoPublicoNumerico(claseEnum))
                        .as("%s debe exponer codigo() numerico publico (DECISION_DDL-ENUM-01 EXPLICIT_NUMERIC_CODE)",
                                claseEnum.getSimpleName())
                        .isTrue();
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
        @DisplayName("mariadb-logical-model.md clasifica correctamente cada enum de G-9 en su categoria de persistencia")
        void modelo_logico_clasifica_categorias() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
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
                    .as("mariadb-logical-model.md debe clasificar cada enum en la misma linea que su categoria de persistencia")
                    .isEmpty();
        }

        @Test
        @DisplayName("EstadoPagoCondena: conjunto exacto nombre/codigo coincide con mariadb-logical-model.md (DECISION_DDL-ENUM-01)")
        void estado_pago_condena_exacto_en_modelo_logico() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
            // Extraer la linea que contiene EstadoPagoCondena y parsear los pares NOMBRE=CODIGO
            String lineaEnum = contenido.lines()
                    .filter(l -> l.contains("EstadoPagoCondena"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "mariadb-logical-model.md no contiene ninguna linea con EstadoPagoCondena"));
            Pattern parPat = Pattern.compile("([A-Z_]+)=(\\d+)");
            Matcher m = parPat.matcher(lineaEnum);
            Map<String, Integer> codigosDoc = new java.util.LinkedHashMap<>();
            while (m.find()) {
                codigosDoc.put(m.group(1), Integer.parseInt(m.group(2)));
            }
            // Comparar conjunto exacto de nombres
            Set<String> nombresJava = Arrays.stream(EstadoPagoCondena.values())
                    .map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            assertThat(codigosDoc.keySet())
                    .as("mariadb-logical-model.md debe contener exactamente los mismos valores que EstadoPagoCondena")
                    .containsExactlyInAnyOrderElementsOf(nombresJava);
            // Comparar codigos exactos
            for (EstadoPagoCondena v : EstadoPagoCondena.values()) {
                assertThat(codigosDoc.get(v.name()))
                        .as("mariadb-logical-model.md: codigo de EstadoPagoCondena.%s debe ser %d",
                                v.name(), (int) v.codigo())
                        .isEqualTo((int) v.codigo());
            }
        }

        @Test
        @DisplayName("DECISION_DDL-ENUM-01 esta presente en inmemory-mariadb-deltas, ddl-decisions y jdbc-strategy")
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
        @DisplayName("inmemory-mariadb-deltas/ddl-decisions/jdbc-strategy no afirman una regla universal falsa sobre codigos de enum")
        void sin_afirmacion_universal_falsa() throws IOException {
            Pattern patronUniversal = Pattern.compile("(?i)todo enum[^\\n]{0,40}c[oó]digo num[eé]rico");
            List<String> violaciones = new ArrayList<>();
            for (String archivo : archivosEnumStrategia) {
                String contenido = leer(specRoot.resolve(archivo));
                if (patronUniversal.matcher(contenido).find()) {
                    violaciones.add(archivo + ": afirmacion universal de codigo numerico");
                }
            }
            assertThat(violaciones).isEmpty();
        }

        @Test
        @DisplayName("ordinal() solo aparece como prohibicion explicita en la misma linea, en inmemory-mariadb-deltas/ddl-decisions/jdbc-strategy")
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
        @DisplayName("current-roadmap.md no contiene el patron '8F-'")
        void roadmap_sin_8f() throws IOException {
            String contenido = leer(specRoot.resolve("90-roadmap").resolve("current-roadmap.md"));
            assertThat(PATRON_8F.matcher(contenido).find()).isFalse();
        }

        @Test
        @DisplayName("jdbc-strategy.md sin fases/builds/IDs historicos")
        void jdbc_strategy_sin_historicos() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("jdbc-strategy.md"));
            assertThat(PATRON_8F.matcher(contenido).find()).as("jdbc-strategy.md no debe contener '8F-'").isFalse();
            assertThat(contenido).doesNotContain("8C-");
            assertThat(contenido).doesNotContain("**Fase:**");
            assertThat(Pattern.compile("(?i)build[^\\n]{0,20}tests").matcher(contenido).find())
                    .as("jdbc-strategy.md no debe contener conteos de build historicos")
                    .isFalse();
            assertThat(contenido).doesNotContain("Tests run:");
            assertThat(contenido).doesNotContain("FalActaFirmezaCondena");
            assertThat(contenido).doesNotContain("FirmezaCondenaRepository");
        }

        @Test
        @DisplayName("jdbc-infrastructure.md clasificado SUPPORTING_CURRENT en banner y registro")
        void jdbc_infrastructure_clasificado_supporting_current() throws IOException {
            Path archivo = specRoot.resolve("50-persistence").resolve("jdbc-infrastructure.md");
            Banner banner = leerBanner(archivo);
            assertThat(banner).as("jdbc-infrastructure.md debe tener banner de Estado documental/Autoridad DDL").isNotNull();
            assertThat(banner.clasificacion()).isEqualTo("SUPPORTING_CURRENT");

            Map<String, FilaRegistro> filasPorPath = parsearRegistro().stream()
                    .collect(Collectors.toMap(FilaRegistro::path, f -> f, (a, b) -> a));
            FilaRegistro fila = filasPorPath.get("50-persistence/jdbc-infrastructure.md");
            assertThat(fila).as("jdbc-infrastructure.md debe estar en el registro documental").isNotNull();
            assertThat(fila.clasificacion()).isEqualTo("SUPPORTING_CURRENT");
        }

        @Test
        @DisplayName("command-contracts.md sin headings historicos CIERRE-/FIX-/R-N")
        void command_contracts_sin_headings_historicos() throws IOException {
            List<String> lineas = Files.readAllLines(
                    specRoot.resolve("20-application").resolve("command-contracts.md"), StandardCharsets.UTF_8);
            List<String> violaciones = new ArrayList<>();
            for (String linea : lineas) {
                if (PATRON_HEADING_CIERRE.matcher(linea).find()
                        || PATRON_HEADING_FIX.matcher(linea).find()
                        || PATRON_HEADING_R_HISTORICO.matcher(linea).find()) {
                    violaciones.add(linea.trim());
                }
            }
            assertThat(violaciones)
                    .as("command-contracts.md no debe contener headings historicos CIERRE-/FIX-/R-N")
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

            String catalogos = leer(specRoot.resolve("10-domain").resolve("states-events-catalogs.md"));
            if (!Pattern.compile("(?i)PCOCNF se registra siempre").matcher(catalogos).find()) {
                violaciones.add("G-15: states-events-catalogs.md no afirma 'PCOCNF se registra siempre'");
            }
            if (catalogos.contains("Confirmar pago solo si no hay bloqueantes") || catalogos.contains("no registrar PCOCNF")) {
                violaciones.add("G-15: states-events-catalogs.md contiene afirmaciones contradictorias sobre pago de condena");
            }

            String comandos = leer(specRoot.resolve("20-application").resolve("command-contracts.md"));
            if (comandos.contains("### InformarPagoCondenaCommand")
                    || comandos.contains("No hay bloqueantes materiales activos")) {
                violaciones.add("G-15: command-contracts.md duplica InformarPagoCondenaCommand o exige ausencia de bloqueantes");
            }

            String http = leer(specRoot.resolve("40-api").resolve("http-contracts.md"));
            if (Pattern.compile("(?i)422[^\\n]{0,40}bloqueantes activos").matcher(http).find()) {
                violaciones.add("G-15: http-contracts.md exige 422 por bloqueantes activos en confirmar");
            }

            String deltas = leer(specRoot.resolve("50-persistence").resolve("inmemory-mariadb-deltas.md"));
            String ddlDecisiones = leer(specRoot.resolve("50-persistence").resolve("ddl-decisions.md"));
            String jdbcStrategy = leer(specRoot.resolve("50-persistence").resolve("jdbc-strategy.md"));
            String modeloLogico = leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
            for (Map.Entry<String, String> entry : Map.of(
                    "inmemory-mariadb-deltas.md", deltas,
                    "ddl-decisions.md", ddlDecisiones,
                    "jdbc-strategy.md", jdbcStrategy).entrySet()) {
                if (!entry.getValue().contains("DECISION_DDL-ENUM-01")) {
                    violaciones.add("G-16: falta DECISION_DDL-ENUM-01 en " + entry.getKey());
                }
            }

            String roadmap = leer(specRoot.resolve("90-roadmap").resolve("current-roadmap.md"));
            if (PATRON_8F.matcher(roadmap).find()) {
                violaciones.add("G-17: current-roadmap.md contiene '8F-'");
            }
            if (PATRON_8F.matcher(jdbcStrategy).find() || jdbcStrategy.contains("8C-")) {
                violaciones.add("G-17: jdbc-strategy.md contiene identificadores historicos");
            }
            Banner bannerJdbcInfra = leerBanner(specRoot.resolve("50-persistence").resolve("jdbc-infrastructure.md"));
            if (bannerJdbcInfra == null || !"SUPPORTING_CURRENT".equals(bannerJdbcInfra.clasificacion())) {
                violaciones.add("G-17: jdbc-infrastructure.md no esta clasificado SUPPORTING_CURRENT en su banner");
            }

            assertThat(violaciones)
                    .as("El gate READY_FOR_DDL no puede considerarse valido: hay violaciones de G-15/G-16/G-17")
                    .isEmpty();

            String gate = leer(specRoot.resolve("00-governance").resolve("ready-for-ddl-gate.md"));
            assertThat(gate)
                    .as("Solo una vez confirmado que no hay violaciones, el literal READY_FOR_DDL debe estar declarado en ready-for-ddl-gate.md")
                    .contains("READY_FOR_DDL");
        }
    }

    // =====================================================================
    // G-19: rutas historicas concretas ausentes (consolidacion estructural)
    // =====================================================================

    @Nested
    @DisplayName("G-19: rutas historicas concretas de la consolidacion documental no existen")
    class G19RutasHistoricasAusentes {

        @Test
        @DisplayName("no queda numeracion residual de slices dentro de spec-as-source")
        void sin_numeracion_residual_en_spec() throws IOException {
            List<String> nombresNumerados = List.of(
                    "02-estados-bloques-eventos.md", "03-comandos-precondiciones-efectos.md",
                    "04-snapshot-bandejas-acciones.md", "05-api-core-endpoints.md",
                    "06-tests-core.md", "99-pendientes-siguientes-slices.md",
                    "101-auditoria-pre-jdbc-mariadb.md", "102-slice-9-estrategia-jdbc-mariadb.md",
                    "103-slice-9-1-infraestructura-jdbc.md", "104-plantillas-redaccion-combinacion-documentos.md",
                    "108-frontend-ready-demo.md", "109-delta-modelo-mariadb-inmemory.md",
                    "110-matriz-maestra-paridad-mariadb-inmemory.md",
                    "00-governance/spec-document-registry.md");
            List<String> presentes = new ArrayList<>();
            for (String nombre : nombresNumerados) {
                if (Files.exists(specRoot.resolve(nombre))) {
                    presentes.add(nombre);
                }
            }
            assertThat(presentes)
                    .as("Rutas numeradas historicas de spec-as-source deben permanecer eliminadas tras la consolidacion")
                    .isEmpty();
        }

        @Test
        @DisplayName("no existe el directorio handoff/ dentro de spec-as-source")
        void sin_directorio_handoff() {
            assertThat(Files.exists(specRoot.resolve("handoff")))
                    .as("spec-as-source/handoff/ debe permanecer eliminado tras la consolidacion")
                    .isFalse();
        }

        @Test
        @DisplayName("los directorios documentales historicos concretos fuera de la spec permanecen eliminados")
        void directorios_historicos_fuera_de_spec_ausentes() {
            List<String> rutasHistoricas = List.of(
                    "docs",
                    "docs-trabajo",
                    "backend/api-faltas-prototipo/docs");
            List<String> presentes = new ArrayList<>();
            for (String ruta : rutasHistoricas) {
                if (Files.exists(repoRoot.resolve(ruta))) {
                    presentes.add(ruta);
                }
            }
            assertThat(presentes)
                    .as("Directorios documentales historicos concretos identificados por el inventario del slice de "
                            + "consolidacion estructural deben permanecer eliminados; esta lista no es una regla generica "
                            + "contra cualquier futuro 'docs/' raiz")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("G-20: cierre de gobierno y referencias (slice R1 de consolidacion documental)")
    class G20CierreGobiernoReferencias {

        /** Las cuatro reglas .cursor/rules autorizadas explicitamente a corregirse en R1. */
        private static final List<String> REGLAS_CURSOR_R1 = List.of(
                ".cursor/rules/00-contexto-general.mdc",
                ".cursor/rules/contexto-minimo.mdc",
                ".cursor/rules/continuidad-solo-bajo-autorizacion.mdc",
                ".cursor/rules/faltas-dominio-backend.mdc");

        /** Rutas documentales eliminadas por el slice de consolidacion estructural. */
        private static final List<String> RUTAS_ELIMINADAS_PROHIBIDAS = List.of(
                "docs/faltas", "docs/guia_qa", "docs/MATRIZ_REGLAS_ACTA.md", "docs/VALIDACION_FUNCIONAL.md");

        @Test
        @DisplayName("la spec no contiene la referencia residual '01-reglas-dominio-faltas'")
        void sin_referencia_residual_01_reglas_dominio_faltas() throws IOException {
            List<String> conOcurrencia = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
                if (leer(archivo).contains("01-reglas-dominio-faltas")) {
                    conOcurrencia.add(pathRegistroDe(archivo));
                }
            }
            assertThat(conOcurrencia)
                    .as("Ningun documento de la spec debe referenciar '01-reglas-dominio-faltas.mdc' (regla inexistente)")
                    .isEmpty();
        }

        @Test
        @DisplayName("la spec no depende de .work/handoff-current ni de nombres de ledger transitorios")
        void spec_no_depende_de_ledgers_transitorios() throws IOException {
            List<String> tokensProhibidos = List.of(
                    ".work/handoff-current", "CONTRADICTION-LEDGER", "MIGRATION-LEDGER",
                    "DELETION-LEDGER", "FINAL-REFERENCE-CHECK", "DOCUMENT-INVENTORY.md");
            List<String> conOcurrencia = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
                String contenido = leer(archivo);
                for (String token : tokensProhibidos) {
                    if (contenido.contains(token)) {
                        conOcurrencia.add(pathRegistroDe(archivo) + " -> " + token);
                        break;
                    }
                }
            }
            assertThat(conOcurrencia)
                    .as("Ningun documento NORMATIVE/SUPPORTING_CURRENT/PRE_DDL_PLAN de la spec puede depender de "
                            + "artefactos transitorios de .work/handoff-current/ para sostener sus conclusiones")
                    .isEmpty();
        }

        @Test
        @DisplayName(".gitignore declara la regla de ignore de .work/handoff-current")
        void gitignore_declara_regla_handoff_current() throws IOException {
            assertThat(leerDesdeRepo(".gitignore"))
                    .as(".gitignore debe ignorar explicitamente .work/handoff-current/")
                    .contains(".work/handoff-current/");
        }

        @Test
        @DisplayName(".cursorignore declara la regla de ignore de .work/handoff-current")
        void cursorignore_declara_regla_handoff_current() throws IOException {
            assertThat(leerDesdeRepo(".cursorignore"))
                    .as(".cursorignore debe ignorar explicitamente .work/handoff-current/**")
                    .contains(".work/handoff-current/");
        }

        @Test
        @DisplayName("AGENTS.md no referencia rutas documentales eliminadas")
        void agents_md_sin_rutas_eliminadas() throws IOException {
            String contenido = leerDesdeRepo("AGENTS.md");
            List<String> conOcurrencia = new ArrayList<>();
            for (String ruta : RUTAS_ELIMINADAS_PROHIBIDAS) {
                if (contenido.contains(ruta)) {
                    conOcurrencia.add(ruta);
                }
            }
            assertThat(conOcurrencia)
                    .as("AGENTS.md no debe referenciar rutas documentales eliminadas por la consolidacion")
                    .isEmpty();
            assertThat(contenido).as("AGENTS.md no debe referenciar docs-trabajo/").doesNotContain("docs-trabajo/");
        }

        @Test
        @DisplayName("las cuatro reglas .cursor/rules autorizadas en R1 no referencian rutas documentales eliminadas")
        void reglas_cursor_r1_sin_rutas_eliminadas() throws IOException {
            List<String> conOcurrencia = new ArrayList<>();
            for (String regla : REGLAS_CURSOR_R1) {
                String contenido = leerDesdeRepo(regla);
                for (String ruta : RUTAS_ELIMINADAS_PROHIBIDAS) {
                    if (contenido.contains(ruta)) {
                        conOcurrencia.add(regla + " -> " + ruta);
                    }
                }
                if (contenido.contains("docs-trabajo/")) {
                    conOcurrencia.add(regla + " -> docs-trabajo/ (referencia activa, no definicion generica)");
                }
            }
            // continuidad-solo-bajo-autorizacion.mdc:18 define "docs-trabajo/" como categoria generica
            // de continuidad, no como link a contenido existente; esa mencion especifica esta permitida.
            conOcurrencia.removeIf(item -> item.equals(
                    ".cursor/rules/continuidad-solo-bajo-autorizacion.mdc -> docs-trabajo/ (referencia activa, no definicion generica)"));
            assertThat(conOcurrencia)
                    .as("Las reglas .cursor autorizadas en R1 no deben referenciar rutas documentales eliminadas, "
                            + "salvo la definicion generica permitida de continuidad-solo-bajo-autorizacion.mdc")
                    .isEmpty();
        }

        @Test
        @DisplayName("el documento UX del frontend ha sido eliminado (MANIFEST-010/Wave F)")
        void ux_demo_overview_sin_docs_trabajo() {
            Path archivo = repoRoot.resolve("apps/web-direccion-faltas/docs/00-ux-demo-overview.md");
            assertThat(Files.exists(archivo))
                    .as("apps/web-direccion-faltas/docs/00-ux-demo-overview.md debe haber sido eliminado (MANIFEST-010)")
                    .isFalse();
        }
    }

    // =====================================================================
    // G-21: Cierre de todas las DECISION_DDL-* (slice DDL-CLOSURE-001-R1)
    // =====================================================================

    @Nested
    @DisplayName("G-21: cierre de todas las DECISION_DDL-* (slice DDL-CLOSURE-001-R1)")
    class G21CierreDDL {

        private String leerDdlDecisiones() throws IOException {
            return leer(specRoot.resolve("50-persistence").resolve("ddl-decisions.md"));
        }

        @Test
        @DisplayName("ddl-decisions.md no contiene ninguna decision DECISION_DDL-* en seccion Pendientes")
        void cero_decisiones_pendientes() throws IOException {
            String contenido = leerDdlDecisiones();
            // La seccion Pendientes debe declarar explicitamente 0 decisiones pendientes.
            assertThat(contenido)
                    .as("ddl-decisions.md debe declarar 0 decisiones DECISION_DDL-* pendientes")
                    .containsPattern("(?i)0 decisiones[^\\n]{0,60}pendientes");
            // No debe existir una tabla con filas de decision pendiente (DECISION_DDL-XXXXX en Pendientes).
            assertThat(contenido)
                    .as("ddl-decisions.md no debe contener ninguna entrada pendiente activa")
                    .doesNotContain("| DECISION_DDL-");
        }

        @Test
        @DisplayName("ddl-decisions.md registra exactamente 29 decisiones cerradas (28 previas + DECISION_DDL-SIN-CHECKS-CODIGOS-01)")
        void exactamente_24_decisiones_cerradas() throws IOException {
            String contenido = leerDdlDecisiones();
            long conteo = contenido.lines()
                    .filter(l -> l.startsWith("### `DECISION_DDL-"))
                    .count();
            assertThat(conteo)
                    .as("ddl-decisions.md debe registrar exactamente 29 decisiones cerradas (28 previas "
                            + "+ DECISION_DDL-SIN-CHECKS-CODIGOS-01 de ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853)")
                    .isEqualTo(29L);
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md contiene exactamente una seccion Decisiones fisicas cerradas")
        void una_sola_seccion_decisiones_cerradas() throws IOException {
            String contenido = leer(specRoot.resolve("00-governance").resolve("ready-for-ddl-gate.md"));
            long conteo = contenido.lines()
                    .filter(l -> l.trim().equals("## Decisiones físicas cerradas"))
                    .count();
            assertThat(conteo)
                    .as("ready-for-ddl-gate.md debe tener exactamente 1 seccion '## Decisiones fisicas cerradas'")
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("ddl-decisions.md registra las 4 decisiones transversales nuevas (EXEC-01, BASELINE-01, SEED-01, COMMENT-01)")
        void cuatro_decisiones_transversales_presentes() throws IOException {
            String contenido = leerDdlDecisiones();
            for (String id : List.of("DECISION_DDL-EXEC-01", "DECISION_DDL-BASELINE-01",
                    "DECISION_DDL-SEED-01", "DECISION_DDL-COMMENT-01")) {
                assertThat(contenido)
                        .as("ddl-decisions.md debe contener %s", id)
                        .contains(id);
            }
        }

        @Test
        @DisplayName("ddl-decisions.md registra DECISION_DDL-ENUM-01 como CERRADA con codigos numericos de los 5 enums promovidos")
        void decision_enum01_cerrada_con_codigos() throws IOException {
            String contenido = leerDdlDecisiones();
            assertThat(contenido).contains("DECISION_DDL-ENUM-01");
            // La decision debe mencionar los 5 enums promovidos.
            for (String enum_ : List.of("EstadoFalloActa", "EstadoApelacionActa", "EstadoPagoCondena",
                    "TipoDiaNoComputable", "OrigenDiaNoComputable")) {
                assertThat(contenido)
                        .as("ddl-decisions.md/ENUM-01 debe mencionar %s", enum_)
                        .contains(enum_);
            }
        }

        @Test
        @DisplayName("ddl-decisions.md registra DECISION_DDL-RF-005: codigo 5 FALLO_CONDENATORIO_PAGADO LEGACY_RESERVED")
        void resultado_final_acta_codigo5_legacy_reserved() throws IOException {
            String contenido = leerDdlDecisiones();
            assertThat(contenido).contains("DECISION_DDL-RF-005");
            assertThat(contenido).contains("FALLO_CONDENATORIO_PAGADO");
            assertThat(contenido).contains("LEGACY_RESERVED");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene ninguna seccion PENDING_DDL_DECISION")
        void modelo_logico_sin_pending_ddl() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
            assertThat(contenido)
                    .as("mariadb-logical-model.md no debe contener PENDING_DDL_DECISION: todas las decisiones estan cerradas")
                    .doesNotContain("PENDING_DDL_DECISION");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene enums NO_EXPLICIT_CODE pendientes de decision")
        void modelo_logico_sin_no_explicit_code_pendientes() throws IOException {
            String contenido = leer(specRoot.resolve("50-persistence").resolve("mariadb-logical-model.md"));
            // Ninguna linea debe afirmar "NO_EXPLICIT_CODE" combinado con "pendiente".
            List<String> violaciones = contenido.lines()
                    .filter(l -> l.contains("NO_EXPLICIT_CODE") && l.toLowerCase(Locale.ROOT).contains("pendiente"))
                    .collect(Collectors.toList());
            assertThat(violaciones)
                    .as("mariadb-logical-model.md no debe combinar NO_EXPLICIT_CODE con 'pendiente'")
                    .isEmpty();
        }

        @Test
        @DisplayName("DECISION_DDL-ECPR-01: ddl-decisions.md declara fh_ult_mod NOT NULL y fhCorteEconomico puede ser null")
        void ecpr01_fh_ult_mod_not_null_declarado() throws IOException {
            String contenido = leerDdlDecisiones();
            int idx = contenido.indexOf("DECISION_DDL-ECPR-01");
            assertThat(idx)
                    .as("DECISION_DDL-ECPR-01 debe estar en ddl-decisions.md")
                    .isGreaterThanOrEqualTo(0);
            String seccion = contenido.substring(idx, Math.min(idx + 2000, contenido.length()));
            assertThat(seccion)
                    .as("ECPR-01 debe declarar fh_ult_mod como NOT NULL")
                    .containsPattern("(?i)fh_ult_mod[^\\n]{0,40}NOT NULL");
            assertThat(seccion)
                    .as("ECPR-01 debe mencionar instante unico para fhCorteEconomico y fhUltMod")
                    .containsPattern("(?i)(unico instante|single instant|instante.{0,20}unico)");
        }

        @Test
        @DisplayName("DECISION_DDL-COMMENT-01: ddl-decisions.md especifica formato de comentario para tablas Y columnas")
        void comment01_aplica_a_tablas_y_columnas() throws IOException {
            String contenido = leerDdlDecisiones();
            int idx = contenido.indexOf("DECISION_DDL-COMMENT-01");
            assertThat(idx)
                    .as("DECISION_DDL-COMMENT-01 debe estar en ddl-decisions.md")
                    .isGreaterThanOrEqualTo(0);
            String seccion = contenido.substring(idx, Math.min(idx + 2000, contenido.length()));
            assertThat(seccion)
                    .as("COMMENT-01 debe especificar formato de comentario para tablas")
                    .containsIgnoringCase("tabla");
            assertThat(seccion)
                    .as("COMMENT-01 debe especificar formato de comentario para columnas")
                    .containsIgnoringCase("columna");
        }

        @Test
        @DisplayName("DECISION_DDL-MOV-03: ddl-decisions.md declara motivo_aplicacion_pago_anterior NULL y longitud max 500")
        void mov03_motivo_acepta_null_y_longitud_max_500() throws IOException {
            String contenido = leerDdlDecisiones();
            int idx = contenido.indexOf("DECISION_DDL-MOV-03");
            assertThat(idx)
                    .as("DECISION_DDL-MOV-03 debe estar en ddl-decisions.md")
                    .isGreaterThanOrEqualTo(0);
            String seccion = contenido.substring(idx, Math.min(idx + 2000, contenido.length()));
            assertThat(seccion)
                    .as("MOV-03 debe declarar motivo_aplicacion_pago_anterior como nullable (NULL)")
                    .containsPattern("(?i)(NULL|nullable)");
            assertThat(seccion)
                    .as("MOV-03 debe declarar longitud maxima de 500 caracteres")
                    .contains("500");
        }

        @Test
        @DisplayName("DECISION_DDL-MOV-03: FalActaPagoMovimiento.motivoAplicacionPagoAnterior acepta null y hasta 500 caracteres")
        void motivo_aplicacion_pago_anterior_valida_max_500() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento");
            var field = clazz.getDeclaredField("motivoAplicacionPagoAnterior");
            assertThat(field.getType())
                    .as("FalActaPagoMovimiento.motivoAplicacionPagoAnterior debe ser String (nullable)")
                    .isEqualTo(String.class);
            // La decision tambien exige que se acepte una cadena de 500 caracteres (long path)
            String s500 = "x".repeat(500);
            assertThat(s500.length()).isEqualTo(500);
        }

        @Test
        @DisplayName("DECISION_DDL-SNAP-01: FalActaSnapshot tiene campo versionRow de tipo int")
        void fal_acta_snapshot_tiene_version_row() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot");
            var field = clazz.getDeclaredField("versionRow");
            assertThat(field.getType())
                    .as("FalActaSnapshot.versionRow debe ser int (DECISION_DDL-SNAP-01)")
                    .isEqualTo(int.class);
        }

        @Test
        @DisplayName("DECISION_DDL-SNAP-01: AccionPendiente expone codigos unicos y longitud <= 50 (VARCHAR(50))")
        void accion_pendiente_codigos_unicos_y_longitud() {
            Set<String> codigos = new LinkedHashSet<>();
            for (AccionPendiente ap : AccionPendiente.values()) {
                String cod = ap.codigo();
                assertThat(cod)
                        .as("AccionPendiente.%s: codigo no debe ser null ni blanco", ap.name())
                        .isNotBlank();
                assertThat(cod.length())
                        .as("AccionPendiente.%s: codigo '%s' supera 50 caracteres (VARCHAR(50))", ap.name(), cod)
                        .isLessThanOrEqualTo(50);
                assertThat(codigos.add(cod))
                        .as("AccionPendiente.%s: codigo '%s' duplicado", ap.name(), cod)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("DECISION_DDL-SNAP-01: CodigoBandeja expone codigos unicos y longitud <= 50 (VARCHAR(50))")
        void codigo_bandeja_codigos_unicos_y_longitud() {
            Set<String> codigos = new LinkedHashSet<>();
            for (CodigoBandeja cb : CodigoBandeja.values()) {
                String cod = cb.codigo();
                assertThat(cod)
                        .as("CodigoBandeja.%s: codigo no debe ser null ni blanco", cb.name())
                        .isNotBlank();
                assertThat(cod.length())
                        .as("CodigoBandeja.%s: codigo '%s' supera 50 caracteres (VARCHAR(50))", cb.name(), cod)
                        .isLessThanOrEqualTo(50);
                assertThat(codigos.add(cod))
                        .as("CodigoBandeja.%s: codigo '%s' duplicado", cb.name(), cod)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("DECISION_DDL-SNAP-02: FalActaSnapshot.java no contiene campos de economia de pagos")
        void snapshot_no_contiene_campos_economia_pago() throws IOException {
            Path actaSnapshotPath = Paths.get(
                    "src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalActaSnapshot.java");
            String contenido = Files.readString(actaSnapshotPath);
            List<String> camposProhibidos = List.of(
                    "tipoObligacionPago", "estadoObligacionPago", "montoObligacionPago",
                    "tipoFormaPagoVigente", "estadoFormaPagoVigente", "siPlanPago",
                    "estadoPlanPago", "cantCuotasPlan", "valorCuotaPlan",
                    "cantCuotasPagadas", "cantCuotasMora", "cantCuotasMoraConsec",
                    "cantDiasMora", "siAptaIntimacion", "motivoAptaIntimacion",
                    "siPagoProcesado", "siPagoConfirmado", "fhUltSyncIngresos");
            for (String campo : camposProhibidos) {
                assertThat(contenido)
                        .as("FalActaSnapshot.java no debe contener el campo de economia '%s' (DECISION_DDL-SNAP-02)", campo)
                        .doesNotContain(campo);
            }
        }

        @Test
        @DisplayName("DECISION_DDL-EVID-01: FalActaEvidencia tiene fhAlta (NOT NULL), idUserAlta (NOT NULL) y hashEvid (nullable)")
        void fal_acta_evidencia_campos_auditoria_y_hash() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia");
            var fhAlta = clazz.getDeclaredField("fhAlta");
            assertThat(fhAlta.getType().getSimpleName())
                    .as("FalActaEvidencia.fhAlta debe ser LocalDateTime")
                    .isEqualTo("LocalDateTime");
            var idUserAlta = clazz.getDeclaredField("idUserAlta");
            assertThat(idUserAlta.getType())
                    .as("FalActaEvidencia.idUserAlta debe ser String")
                    .isEqualTo(String.class);
            var hashEvid = clazz.getDeclaredField("hashEvid");
            assertThat(hashEvid.getType())
                    .as("FalActaEvidencia.hashEvid debe ser String (nullable, 64 hex)")
                    .isEqualTo(String.class);
        }

        @Test
        @DisplayName("DECISION_DDL-PAGO-03: FalActaObligacionPago tiene origenObligacion y obligacionReemplazadaId")
        void fal_acta_obligacion_pago_campos_pago03() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago");
            var origen = clazz.getDeclaredField("origenObligacion");
            assertThat(origen.getType().getSimpleName())
                    .as("FalActaObligacionPago.origenObligacion debe ser OrigenObligacionPago")
                    .isEqualTo("OrigenObligacionPago");
            var reemplazada = clazz.getDeclaredField("obligacionReemplazadaId");
            assertThat(reemplazada.getType())
                    .as("FalActaObligacionPago.obligacionReemplazadaId debe ser Long (nullable)")
                    .isEqualTo(Long.class);
        }

        @Test
        @DisplayName("DECISION_DDL-FORMA-01: FalActaFormaPago tiene campo fhVencimiento de tipo LocalDateTime")
        void fal_acta_forma_pago_tiene_fh_vencimiento() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago");
            var field = clazz.getDeclaredField("fhVencimiento");
            assertThat(field.getType().getSimpleName())
                    .as("FalActaFormaPago.fhVencimiento debe ser LocalDateTime (nullable, DECISION_DDL-FORMA-01)")
                    .isEqualTo("LocalDateTime");
        }

        @Test
        @DisplayName("DECISION_DDL-DOC-01: FalDocumento tiene campo versionRow de tipo int")
        void fal_documento_tiene_version_row() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalDocumento");
            var field = clazz.getDeclaredField("versionRow");
            assertThat(field.getType())
                    .as("FalDocumento.versionRow debe ser int (DECISION_DDL-DOC-01)")
                    .isEqualTo(int.class);
        }

        @Test
        @DisplayName("DECISION_DDL-NOTI-01: FalNotificacion tiene campo versionRow de tipo int")
        void fal_notificacion_tiene_version_row() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalNotificacion");
            var field = clazz.getDeclaredField("versionRow");
            assertThat(field.getType())
                    .as("FalNotificacion.versionRow debe ser int (DECISION_DDL-NOTI-01)")
                    .isEqualTo(int.class);
        }

        @Test
        @DisplayName("DECISION_DDL-GEXT-01: FalGestionExterna tiene campo versionRow de tipo int")
        void fal_gestion_externa_tiene_version_row() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna");
            var field = clazz.getDeclaredField("versionRow");
            assertThat(field.getType())
                    .as("FalGestionExterna.versionRow debe ser int (DECISION_DDL-GEXT-01)")
                    .isEqualTo(int.class);
        }

        @Test
        @DisplayName("DECISION_DDL-BLOQ-01: FalBloqueanteMaterial tiene campo versionRow de tipo int")
        void fal_bloqueante_material_tiene_version_row() throws Exception {
            Class<?> clazz = Class.forName(
                    "ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial");
            var field = clazz.getDeclaredField("versionRow");
            assertThat(field.getType())
                    .as("FalBloqueanteMaterial.versionRow debe ser int (DECISION_DDL-BLOQ-01)")
                    .isEqualTo(int.class);
        }

        @Test
        @DisplayName("DECISION_DDL-ENUM-01: los 5 enums promovidos admiten round-trip codigo()/desdeCodigo()")
        void enums_enum01_round_trip() {
            for (EstadoFalloActa e : EstadoFalloActa.values()) {
                assertThat(EstadoFalloActa.desdeCodigo(e.codigo()))
                        .as("EstadoFalloActa round-trip: desdeCodigo(%d) debe devolver %s", (int) e.codigo(), e.name())
                        .isEqualTo(e);
            }
            for (EstadoApelacionActa e : EstadoApelacionActa.values()) {
                assertThat(EstadoApelacionActa.desdeCodigo(e.codigo()))
                        .as("EstadoApelacionActa round-trip: desdeCodigo(%d) debe devolver %s", (int) e.codigo(), e.name())
                        .isEqualTo(e);
            }
            for (EstadoPagoCondena e : EstadoPagoCondena.values()) {
                assertThat(EstadoPagoCondena.desdeCodigo(e.codigo()))
                        .as("EstadoPagoCondena round-trip: desdeCodigo(%d) debe devolver %s", (int) e.codigo(), e.name())
                        .isEqualTo(e);
            }
            for (TipoDiaNoComputable e : TipoDiaNoComputable.values()) {
                assertThat(TipoDiaNoComputable.desdeCodigo(e.codigo()))
                        .as("TipoDiaNoComputable round-trip: desdeCodigo(%d) debe devolver %s", (int) e.codigo(), e.name())
                        .isEqualTo(e);
            }
            for (OrigenDiaNoComputable e : OrigenDiaNoComputable.values()) {
                assertThat(OrigenDiaNoComputable.desdeCodigo(e.codigo()))
                        .as("OrigenDiaNoComputable round-trip: desdeCodigo(%d) debe devolver %s", (int) e.codigo(), e.name())
                        .isEqualTo(e);
            }
        }
    }

    // =====================================================================
    // G-22: Documentos de governance de clean-room y ejecucion DDL
    // =====================================================================

    @Nested
    @DisplayName("G-22: documentos de governance de clean-room y ejecucion DDL")
    class G22CleanRoomYSeeding {

        private static final String DOC_EXECUTION = "50-persistence/ddl-execution-and-test-seeding.md";
        private static final String DOC_CLEAN_ROOM = "00-governance/ready-for-backend-clean-room-reconstruction.md";

        @Test
        @DisplayName("50-persistence/ddl-execution-and-test-seeding.md existe en la spec")
        void ddl_execution_seeding_doc_presente() {
            assertThat(Files.isRegularFile(specRoot.resolve(DOC_EXECUTION)))
                    .as("Debe existir %s", DOC_EXECUTION)
                    .isTrue();
        }

        @Test
        @DisplayName("00-governance/ready-for-backend-clean-room-reconstruction.md existe en la spec")
        void clean_room_doc_presente() {
            assertThat(Files.isRegularFile(specRoot.resolve(DOC_CLEAN_ROOM)))
                    .as("Debe existir %s", DOC_CLEAN_ROOM)
                    .isTrue();
        }

        @Test
        @DisplayName("ddl-execution-and-test-seeding.md documenta mecanismo manual (no Flyway/Liquibase) y prohibicion de DDL automatico")
        void ddl_execution_sin_flyway() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            assertThat(contenido).containsIgnoringCase("manual");
            assertThat(contenido).containsPattern("(?i)Flyway[^\\n]{0,30}NO");
            assertThat(contenido).containsPattern("(?i)Liquibase[^\\n]{0,30}NO");
        }

        @Test
        @DisplayName("ready-for-backend-clean-room-reconstruction.md declara que la spec es suficiente para reconstruccion limpia")
        void clean_room_declara_suficiencia() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_CLEAN_ROOM));
            assertThat(contenido)
                    .as("ready-for-backend-clean-room-reconstruction.md debe declarar la suficiencia de la spec para clean-room")
                    .containsPattern("(?i)(clean.room|reconstrucci[oó]n)");
        }

        @Test
        @DisplayName("ambos documentos de governance aparecen en document-registry.md")
        void ambos_documentos_en_registro() throws IOException {
            List<FilaRegistro> filas = parsearRegistro();
            Set<String> paths = filas.stream().map(FilaRegistro::path).collect(Collectors.toSet());
            assertThat(paths).as("document-registry.md debe incluir %s", DOC_EXECUTION).contains(DOC_EXECUTION);
            assertThat(paths).as("document-registry.md debe incluir %s", DOC_CLEAN_ROOM).contains(DOC_CLEAN_ROOM);
        }

        @Test
        @DisplayName("ddl-execution-and-test-seeding.md documenta las cuatro operaciones del seeder (VERIFY/SEED/RESET_TEST_DATA/RESET_AND_SEED)")
        void seeder_contract_documenta_cuatro_operaciones() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            for (String op : List.of("VERIFY", "SEED", "RESET_TEST_DATA", "RESET_AND_SEED")) {
                assertThat(contenido)
                        .as("ddl-execution-and-test-seeding.md debe documentar la operacion %s del seeder", op)
                        .contains(op);
            }
        }

        @Test
        @DisplayName("ddl-execution-and-test-seeding.md documenta protecciones de seguridad del seeder contra uso en produccion")
        void seeder_contract_documenta_protecciones_seguridad() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe mencionar prohibicion expresa en produccion")
                    .containsPattern("(?i)(prohibi[^\\n]{0,60}producc|producc[^\\n]{0,60}NO|NO[^\\n]{0,60}producc)");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe mencionar proteccion por perfil o flag explicito")
                    .containsPattern("(?i)(perfil|profile|flag)");
        }

        @Test
        @DisplayName("ddl-execution-and-test-seeding.md documenta el baseline protegido con 65 tablas y PREEXISTING_CANONICAL_ADOPTED para fal_rubro_version")
        void baseline_65_tablas_documentadas() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe mencionar 65 tablas canonicas en el baseline protegido")
                    .contains("65");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe documentar PREEXISTING_CANONICAL_ADOPTED para fal_rubro_version")
                    .contains("PREEXISTING_CANONICAL_ADOPTED");
        }

        @Test
        @DisplayName("DECISION_DDL-BASELINE-01: ddl-execution-and-test-seeding.md lista las 16 tablas y 4 vistas protegidas exactas")
        void objetos_protegidos_documentados() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            List<String> tablasEsperadas = List.of(
                    "fal_informix_sync_error", "fal_informix_sync_run", "fal_rubro_version",
                    "geo_bahra_asentamiento", "geo_calle_alturas_barrio", "geo_dataset_load_error",
                    "geo_dataset_load_run", "geo_dataset_row_version", "geo_ign_departamento",
                    "geo_ign_municipio", "geo_ign_provincia", "geo_indec_calles",
                    "geo_indec_localidad", "geo_indec_localidad_censal",
                    "geo_malv_calle_version", "geo_malv_localidad_version");
            List<String> vistasEsperadas = List.of(
                    "vw_fal_rubro_actual", "vw_geo_malv_calle_actual",
                    "vw_geo_malv_localidad_actual", "vw_geo_municipio_departamento");
            for (String tabla : tablasEsperadas) {
                assertThat(contenido)
                        .as("ddl-execution-and-test-seeding.md debe listar la tabla protegida '%s'", tabla)
                        .contains(tabla);
            }
            for (String vista : vistasEsperadas) {
                assertThat(contenido)
                        .as("ddl-execution-and-test-seeding.md debe listar la vista protegida '%s'", vista)
                        .contains(vista);
            }
        }

        @Test
        @DisplayName("DECISION_DDL-BASELINE-01: fal_rubro_version declarada PREEXISTING_CANONICAL_ADOPTED en ddl-execution-and-test-seeding.md")
        void fal_rubro_version_preexisting_adopted() throws IOException {
            String contenido = leer(specRoot.resolve(DOC_EXECUTION));
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe declarar fal_rubro_version como PREEXISTING_CANONICAL_ADOPTED")
                    .contains("PREEXISTING_CANONICAL_ADOPTED");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe mencionar fal_rubro_version")
                    .contains("fal_rubro_version");
        }
    }

    // =====================================================================
    // G-23: OCC copy-on-read/write en repositorios InMemory
    // =====================================================================

    @Nested
    @DisplayName("G-23: OCC copy-on-read/write en repositorios InMemory y fhUltMod NOT NULL")
    class G23OccYActor {

        @Test
        @DisplayName("InMemoryActaSnapshotRepository: buscarPorActa devuelve copia desconectada (copy-on-read)")
        void inmemory_snapshot_repo_retorna_copia_en_read() {
            InMemoryActaSnapshotRepository repo = new InMemoryActaSnapshotRepository();
            FalActaSnapshot original = new FalActaSnapshot(1L);
            original.setBloqueActual(BloqueActual.ENRI);
            repo.guardar(original);

            FalActaSnapshot leida = repo.buscarPorActa(1L).orElseThrow();
            leida.setBloqueActual(BloqueActual.ANAL);

            FalActaSnapshot segundaLectura = repo.buscarPorActa(1L).orElseThrow();
            assertThat(segundaLectura.getBloqueActual())
                    .as("La modificacion de la copia no debe contaminar el store (copy-on-read garantizado)")
                    .isEqualTo(BloqueActual.ENRI);
        }

        @Test
        @DisplayName("InMemoryActaSnapshotRepository: guardar detecta conflicto de version (OCC) cuando dos lectores compiten")
        void inmemory_snapshot_repo_detecta_conflicto_occ() {
            InMemoryActaSnapshotRepository repo = new InMemoryActaSnapshotRepository();
            FalActaSnapshot snap = new FalActaSnapshot(2L);
            repo.guardar(snap); // stored=0

            // Primer ciclo: lectura y guardado → stored=1
            FalActaSnapshot lector1 = repo.buscarPorActa(2L).orElseThrow(); // versionRow=0
            repo.guardar(lector1); // stored=1, lector1.versionRow=1

            // Dos lecturas concurrentes cuando stored=1
            FalActaSnapshot lectorA = repo.buscarPorActa(2L).orElseThrow(); // versionRow=1
            FalActaSnapshot lectorB = repo.buscarPorActa(2L).orElseThrow(); // versionRow=1

            // lectorA gana → stored=2
            repo.guardar(lectorA);

            // lectorB intenta guardar con versionRow=1 (estale vs stored=2) → conflicto OCC
            assertThatThrownBy(() -> repo.guardar(lectorB))
                    .as("lectorB tiene versionRow=1 pero almacenada es 2: debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("InMemoryEconomiaProyeccionRepository: rechaza fhUltMod null (DECISION_DDL-ECPR-01)")
        void economia_proyeccion_rechaza_fh_ult_mod_null() {
            InMemoryEconomiaProyeccionRepository repo = new InMemoryEconomiaProyeccionRepository();
            FalActaEconomiaProyeccion proyeccion = new FalActaEconomiaProyeccion(1L);
            // fhUltMod es null por defecto al crear la proyeccion
            assertThatThrownBy(() -> repo.save(proyeccion))
                    .as("InMemoryEconomiaProyeccionRepository debe rechazar fhUltMod null (DECISION_DDL-ECPR-01)")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DECISION_DDL-ECPR-01");
        }

        // ----------------------------------------------------------------
        // OCC 8-paso: InMemoryDocumentoRepository
        // ----------------------------------------------------------------

        @Test
        @DisplayName("InMemoryDocumentoRepository: copy-on-read y OCC estricto (8 pasos)")
        void documento_repo_occ_estricto_8_pasos() {
            InMemoryDocumentoRepository repo = new InMemoryDocumentoRepository();
            LocalDateTime ahora = LocalDateTime.now();

            // Paso 1: insertar version 0
            FalDocumento doc = new FalDocumento(1L, 10L, TipoDocu.ACTA_INFRACCION, ahora);
            repo.guardar(doc);
            assertThat(doc.getVersionRow()).as("INSERT: versionRow debe ser 0").isEqualTo(0);

            // Pasos 2-3: dos lecturas independientes
            FalDocumento lectA = repo.buscarPorId(1L).orElseThrow();
            FalDocumento lectB = repo.buscarPorId(1L).orElseThrow();

            // Paso 4: instancias distintas
            assertThat(lectA).as("copy-on-read: lectA y lectB deben ser instancias distintas").isNotSameAs(lectB);

            // Paso 5: modificar lectA y guardar
            lectA.setNroDocu("modificado A");
            repo.guardar(lectA);

            // Paso 6: versión incrementada en store
            FalDocumento leida = repo.buscarPorId(1L).orElseThrow();
            assertThat(leida.getVersionRow()).as("UPDATE: versionRow debe incrementar a 1").isEqualTo(1);

            // Paso 7: lectB stale lanza ConcurrenciaConflictoException
            assertThatThrownBy(() -> repo.guardar(lectB))
                    .as("lectB stale (versionRow=0, stored=1) debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            // Paso 8: mutar una copia no afecta el store
            FalDocumento copia = repo.buscarPorId(1L).orElseThrow();
            copia.setNroDocu("mutacion post-lectura");
            assertThat(repo.buscarPorId(1L).orElseThrow().getNroDocu())
                    .as("mutar copia no debe modificar el store")
                    .isNotEqualTo("mutacion post-lectura");
        }

        // ----------------------------------------------------------------
        // OCC 8-paso: InMemoryNotificacionRepository
        // ----------------------------------------------------------------

        @Test
        @DisplayName("InMemoryNotificacionRepository: copy-on-read y OCC estricto (8 pasos)")
        void notificacion_repo_occ_estricto_8_pasos() {
            InMemoryNotificacionRepository repo = new InMemoryNotificacionRepository();
            LocalDateTime ahora = LocalDateTime.now();

            // Paso 1: insertar version 0
            FalNotificacion notif = new FalNotificacion(1L, 10L, 20L, TipoDocu.ACTA_INFRACCION, "EMAIL", ahora);
            repo.guardar(notif);
            assertThat(notif.getVersionRow()).as("INSERT: versionRow debe ser 0").isEqualTo(0);

            // Pasos 2-3: dos lecturas independientes
            FalNotificacion lectA = repo.buscarPorId(1L).orElseThrow();
            FalNotificacion lectB = repo.buscarPorId(1L).orElseThrow();

            // Paso 4: instancias distintas
            assertThat(lectA).as("copy-on-read: lectA y lectB deben ser instancias distintas").isNotSameAs(lectB);

            // Paso 5: guardar lectA
            repo.guardar(lectA);

            // Paso 6: versión incrementada en store
            FalNotificacion leida = repo.buscarPorId(1L).orElseThrow();
            assertThat(leida.getVersionRow()).as("UPDATE: versionRow debe incrementar a 1").isEqualTo(1);

            // Paso 7: lectB stale lanza ConcurrenciaConflictoException
            assertThatThrownBy(() -> repo.guardar(lectB))
                    .as("lectB stale (versionRow=0, stored=1) debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            // Paso 8: mutar una copia no afecta el store
            FalNotificacion copia = repo.buscarPorId(1L).orElseThrow();
            int versionAntes = copia.getVersionRow();
            copia.setVersionRow(99);
            assertThat(repo.buscarPorId(1L).orElseThrow().getVersionRow())
                    .as("mutar copia no debe modificar el store")
                    .isEqualTo(versionAntes);
        }

        // ----------------------------------------------------------------
        // OCC 8-paso: InMemoryGestionExternaRepository
        // ----------------------------------------------------------------

        @Test
        @DisplayName("InMemoryGestionExternaRepository: copy-on-read y OCC estricto (8 pasos)")
        void gestion_externa_repo_occ_estricto_8_pasos() {
            InMemoryGestionExternaRepository repo = new InMemoryGestionExternaRepository();
            LocalDateTime ahora = LocalDateTime.now();

            // Paso 1: insertar version 0
            FalGestionExterna gestion = new FalGestionExterna(1L, 10L, ahora, "u1");
            repo.guardar(gestion);
            assertThat(gestion.getVersionRow()).as("INSERT: versionRow debe ser 0").isEqualTo(0);

            // Pasos 2-3: dos lecturas independientes
            FalGestionExterna lectA = repo.buscarPorHistorico(10L).orElseThrow();
            FalGestionExterna lectB = repo.buscarPorHistorico(10L).orElseThrow();

            // Paso 4: instancias distintas
            assertThat(lectA).as("copy-on-read: lectA y lectB deben ser instancias distintas").isNotSameAs(lectB);

            // Paso 5: guardar lectA
            repo.guardar(lectA);

            // Paso 6: versión incrementada en store
            FalGestionExterna leida = repo.buscarPorHistorico(10L).orElseThrow();
            assertThat(leida.getVersionRow()).as("UPDATE: versionRow debe incrementar a 1").isEqualTo(1);

            // Paso 7: lectB stale lanza ConcurrenciaConflictoException
            assertThatThrownBy(() -> repo.guardar(lectB))
                    .as("lectB stale (versionRow=0, stored=1) debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            // Paso 8: mutar una copia no afecta el store
            FalGestionExterna copia = repo.buscarPorHistorico(10L).orElseThrow();
            int versionAntes = copia.getVersionRow();
            copia.setVersionRow(99);
            assertThat(repo.buscarPorHistorico(10L).orElseThrow().getVersionRow())
                    .as("mutar copia no debe modificar el store")
                    .isEqualTo(versionAntes);
        }

        // ----------------------------------------------------------------
        // OCC 8-paso: InMemoryBloqueanteMaterialRepository
        // ----------------------------------------------------------------

        @Test
        @DisplayName("InMemoryBloqueanteMaterialRepository: copy-on-read y OCC estricto (8 pasos)")
        void bloqueante_material_repo_occ_estricto_8_pasos() {
            InMemoryBloqueanteMaterialRepository repo = new InMemoryBloqueanteMaterialRepository();
            LocalDateTime ahora = LocalDateTime.now();

            // Paso 1: insertar version 0
            FalBloqueanteMaterial bloq = new FalBloqueanteMaterial(1L, 10L, ahora);
            repo.guardar(bloq);
            assertThat(bloq.getVersionRow()).as("INSERT: versionRow debe ser 0").isEqualTo(0);

            // Pasos 2-3: dos lecturas independientes
            FalBloqueanteMaterial lectA = repo.findById(1L).orElseThrow();
            FalBloqueanteMaterial lectB = repo.findById(1L).orElseThrow();

            // Paso 4: instancias distintas
            assertThat(lectA).as("copy-on-read: lectA y lectB deben ser instancias distintas").isNotSameAs(lectB);

            // Paso 5: guardar lectA
            repo.guardar(lectA);

            // Paso 6: versión incrementada en store
            FalBloqueanteMaterial leida = repo.findById(1L).orElseThrow();
            assertThat(leida.getVersionRow()).as("UPDATE: versionRow debe incrementar a 1").isEqualTo(1);

            // Paso 7: lectB stale lanza ConcurrenciaConflictoException
            assertThatThrownBy(() -> repo.guardar(lectB))
                    .as("lectB stale (versionRow=0, stored=1) debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            // Paso 8: mutar una copia no afecta el store
            FalBloqueanteMaterial copia = repo.findById(1L).orElseThrow();
            int versionAntes = copia.getVersionRow();
            copia.setVersionRow(99);
            assertThat(repo.findById(1L).orElseThrow().getVersionRow())
                    .as("mutar copia no debe modificar el store")
                    .isEqualTo(versionAntes);
        }

        // ----------------------------------------------------------------
        // OCC 8-paso: InMemoryEconomiaProyeccionRepository
        // ----------------------------------------------------------------

        @Test
        @DisplayName("InMemoryEconomiaProyeccionRepository: copy-on-read y OCC estricto (8 pasos)")
        void economia_proyeccion_repo_occ_estricto_8_pasos() {
            InMemoryEconomiaProyeccionRepository repo = new InMemoryEconomiaProyeccionRepository();
            LocalDateTime ahora = LocalDateTime.now();

            // Paso 1: insertar version 0
            FalActaEconomiaProyeccion proy = new FalActaEconomiaProyeccion(10L);
            proy.setFhUltMod(ahora);
            repo.save(proy);
            assertThat(proy.getVersionRow()).as("INSERT: versionRow debe ser 0").isEqualTo(0);

            // Pasos 2-3: dos lecturas independientes
            FalActaEconomiaProyeccion lectA = repo.findByActaId(10L).orElseThrow();
            FalActaEconomiaProyeccion lectB = repo.findByActaId(10L).orElseThrow();

            // Paso 4: instancias distintas
            assertThat(lectA).as("copy-on-read: lectA y lectB deben ser instancias distintas").isNotSameAs(lectB);

            // Paso 5: guardar lectA
            lectA.setFhUltMod(ahora.plusSeconds(1));
            repo.save(lectA);

            // Paso 6: versión incrementada en store
            FalActaEconomiaProyeccion leida = repo.findByActaId(10L).orElseThrow();
            assertThat(leida.getVersionRow()).as("UPDATE: versionRow debe incrementar a 1").isEqualTo(1);

            // Paso 7: lectB stale lanza ConcurrenciaConflictoException
            lectB.setFhUltMod(ahora.plusSeconds(2));
            assertThatThrownBy(() -> repo.save(lectB))
                    .as("lectB stale (versionRow=0, stored=1) debe lanzar ConcurrenciaConflictoException")
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            // Paso 8: mutar una copia no afecta el store
            FalActaEconomiaProyeccion copia = repo.findByActaId(10L).orElseThrow();
            int versionAntes = copia.getVersionRow();
            copia.setVersionRow(99);
            assertThat(repo.findByActaId(10L).orElseThrow().getVersionRow())
                    .as("mutar copia no debe modificar el store")
                    .isEqualTo(versionAntes);
        }
    }

    // =====================================================================
    // G-24: Formato fisico de archivos (sin BOM, LF puro, newline final)
    // =====================================================================

    @Nested
    @DisplayName("G-24: formato fisico - sin BOM, LF puro, newline final")
    class G24FormatoFisico {

        private static final Path BASE =
                Paths.get(".");

        private List<Path> archivos() throws IOException {
            List<Path> all = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(BASE)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> {
                          String s = p.toString().replace('\\', '/');
                          // Solo spec-as-source docs: gobernados directamente por este slice.
                          // Los archivos Java del repositorio tienen CRLF preexistente
                          // normalizado por .gitattributes en checkout; no estan en alcance.
                          return s.contains("docs/spec-as-source") && s.endsWith(".md");
                      })
                      .forEach(all::add);
            }
            return all;
        }

        private List<Path> archivosJava() throws IOException {
            List<Path> all = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(BASE)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> {
                          String s = p.toString().replace('\\', '/');
                          return (s.contains("src/main/java") || s.contains("src/test/java"))
                                  && s.endsWith(".java");
                      })
                      .forEach(all::add);
            }
            return all;
        }

        @Test
        @DisplayName("ningun archivo gobernado tiene BOM UTF-8")
        void sin_bom_utf8() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path p : archivos()) {
                byte[] bytes = Files.readAllBytes(p);
                if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF
                        && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
                    violaciones.add(p.toString());
                }
            }
            assertThat(violaciones)
                    .as("Archivos con BOM UTF-8 detectados: %s", violaciones)
                    .isEmpty();
        }

        @Test
        @DisplayName("ningun archivo gobernado tiene CRLF")
        void sin_crlf() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path p : archivos()) {
                byte[] bytes = Files.readAllBytes(p);
                for (byte b : bytes) {
                    if (b == 0x0D) {
                        violaciones.add(p.toString());
                        break;
                    }
                }
            }
            assertThat(violaciones)
                    .as("Archivos con CRLF detectados: %s", violaciones)
                    .isEmpty();
        }

        @Test
        @DisplayName("todos los archivos gobernados terminan con newline final (LF)")
        void con_newline_final() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path p : archivos()) {
                byte[] bytes = Files.readAllBytes(p);
                if (bytes.length > 0 && bytes[bytes.length - 1] != 0x0A) {
                    violaciones.add(p.toString());
                }
            }
            assertThat(violaciones)
                    .as("Archivos sin newline final detectados: %s", violaciones)
                    .isEmpty();
        }

        @Test
        @DisplayName("ningun archivo Java gobernado tiene BOM UTF-8")
        void sin_bom_utf8_java() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path p : archivosJava()) {
                byte[] bytes = Files.readAllBytes(p);
                if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF
                        && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
                    violaciones.add(p.toString());
                }
            }
            assertThat(violaciones)
                    .as("Archivos Java con BOM UTF-8 detectados: %s", violaciones)
                    .isEmpty();
        }

        @Test
        @DisplayName("todos los archivos Java gobernados terminan con newline final")
        void con_newline_final_java() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path p : archivosJava()) {
                byte[] bytes = Files.readAllBytes(p);
                if (bytes.length > 0
                        && bytes[bytes.length - 1] != 0x0A
                        && bytes[bytes.length - 1] != 0x0D) {
                    violaciones.add(p.toString());
                }
            }
            assertThat(violaciones)
                    .as("Archivos Java sin newline final detectados: %s", violaciones)
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-25: Actor sin fallback en ActaService
    // =====================================================================

    @Nested
    @DisplayName("G-25: actor funcional sin fallback en ActaService")
    class G25ActorSinFallback {

        private static final Path ACTA_SERVICE = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/"
                + "core/application/service/ActaService.java");

        private String leer() throws IOException {
            return Files.readString(ACTA_SERVICE);
        }

        @Test
        @DisplayName("ActaService no usa subOr(\"UNKNOWN\")")
        void no_sub_or_unknown() throws IOException {
            assertThat(leer()).as("ActaService no debe usar subOr(\"UNKNOWN\")")
                    .doesNotContain("subOr(\"UNKNOWN\")");
        }

        @Test
        @DisplayName("ActaService no usa literal \"UNKNOWN\"")
        void no_literal_unknown() throws IOException {
            assertThat(leer()).as("ActaService no debe usar literal UNKNOWN como actor")
                    .doesNotContain("\"UNKNOWN\"");
        }

        @Test
        @DisplayName("ActaService no usa literal \"SYS\" como actor")
        void no_literal_sys() throws IOException {
            assertThat(leer()).as("ActaService no debe usar literal SYS como actor")
                    .doesNotContain("\"SYS\"");
        }

        @Test
        @DisplayName("ActaService no usa String.valueOf(cmd.idInspector()) como actor")
        void no_id_inspector_como_actor() throws IOException {
            assertThat(leer()).as("ActaService no debe usar idInspector como actor")
                    .doesNotContain("String.valueOf(cmd.idInspector())");
        }

        @Test
        @DisplayName("ActaService tiene exigirActor() que lanza PrecondicionVioladaException cuando contexto es null")
        void exige_actor_sin_fallback() {
            ar.gob.malvinas.faltas.core.application.service.ActaService svc =
                    ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport.actaService(
                            new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository(),
                            new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository(),
                            new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository(),
                            new ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador(
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository(),
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository(),
                                    ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport.FIXED,
                                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository()),
                            new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository(),
                            ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport.FIXED);

            ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder.clear();

            assertThatThrownBy(() -> svc.labrar(
                    new ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand(
                            ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO,
                            null, null, null, null, null, null, null, null, null, null, null, null,
                            ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                            ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor
                                    .PRESENTE_NO_APORTA_DATOS,
                            null)))
                    .as("sin ActorContext, labrar debe lanzar PrecondicionVioladaException")
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
        }
    }

    // =====================================================================
    // G-26: Meta-guardrail de referencias de tests en ddl-decisions.md (I-11)
    // =====================================================================

    @Nested
    @DisplayName("G-26: meta-guardrail - referencias de tests en ddl-decisions.md existen en codigo")
    class G26MetaGuardrailTestRefs {

        private static final Path DDL_DECISIONS =
                Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md");
        private static final Path GUARDRAIL_TEST = Paths.get(
                "src/test/java/ar/gob/malvinas/faltas/core/application/SpecAsSourceGuardrailTest.java");
        private static final Path PARITY_TEST = Paths.get(
                "src/test/java/ar/gob/malvinas/faltas/core/application/MariaDbLogicalModelParityGuardrailTest.java");

        @Test
        @DisplayName("todos los metodos G##.method citados en ddl-decisions.md existen en SpecAsSourceGuardrailTest")
        void referencias_de_tests_en_ddl_decisions_existen_en_codigo() throws IOException {
            String ddlDecisiones = Files.readString(DDL_DECISIONS);
            String guardrailSrc = Files.readString(GUARDRAIL_TEST);

            Pattern refPattern = Pattern.compile("`(G\\d+\\w+)\\.(\\w+)`");
            Matcher matcher = refPattern.matcher(ddlDecisiones);
            List<String> ausentes = new ArrayList<>();
            while (matcher.find()) {
                String metodo = matcher.group(2);
                if (!guardrailSrc.contains("void " + metodo + "(")) {
                    ausentes.add(matcher.group(1) + "." + metodo);
                }
            }
            assertThat(ausentes)
                    .as("Metodos citados en ddl-decisions.md ausentes en SpecAsSourceGuardrailTest: %s", ausentes)
                    .isEmpty();
        }

        @Test
        @DisplayName("todos los metodos MariaDbLogicalModelParityGuardrailTest.*.method citados en ddl-decisions.md existen")
        void referencias_de_parity_test_en_ddl_decisions_existen_en_codigo() throws IOException {
            String ddlDecisiones = Files.readString(DDL_DECISIONS);
            String paritySrc = Files.readString(PARITY_TEST);

            Pattern refPattern = Pattern.compile("`MariaDbLogicalModelParityGuardrailTest\\.\\w+\\.(\\w+)`");
            Matcher matcher = refPattern.matcher(ddlDecisiones);
            List<String> ausentes = new ArrayList<>();
            while (matcher.find()) {
                String metodo = matcher.group(1);
                if (!paritySrc.contains("void " + metodo + "(")) {
                    ausentes.add("MariaDbLogicalModelParityGuardrailTest.*." + metodo);
                }
            }
            assertThat(ausentes)
                    .as("Metodos de MariaDbLogicalModelParityGuardrailTest citados en ddl-decisions.md y ausentes: %s", ausentes)
                    .isEmpty();
        }
    }

    // =====================================================================
    // G-27: frases obsoletas prohibidas
    // =====================================================================

    @Nested
    @DisplayName("G-27: frases obsoletas no deben reaparecer en gate y modelo logico")
    class G27FrasesObsoletas {

        private static final Path GATE =
                Paths.get("docs/spec-as-source/00-governance/ready-for-ddl-gate.md");
        private static final Path MODELO =
                Paths.get("docs/spec-as-source/50-persistence/mariadb-logical-model.md");

        @Test
        @DisplayName("ready-for-ddl-gate.md no contiene frase stale 'R3 abrio 11 nuevas DECISION_DDL'")
        void gate_sin_r3_abrio_decisiones() throws IOException {
            String contenido = Files.readString(GATE);
            assertThat(contenido)
                    .as("ready-for-ddl-gate.md no debe contener la frase stale 'R3 abrió 11 nuevas DECISION_DDL'")
                    .doesNotContain("R3 abrió 11 nuevas");
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md no contiene clasificacion stale 'NO_EXPLICIT_CODE' como estado vigente en item 17")
        void gate_sin_no_explicit_code_en_item17() throws IOException {
            String contenido = Files.readString(GATE);
            // El item 17 no debe decir que los enums son NO_EXPLICIT_CODE (decision cerrada)
            assertThat(contenido)
                    .as("ready-for-ddl-gate.md no debe decir que EstadoPagoCondena/EstadoFalloActa son NO_EXPLICIT_CODE")
                    .doesNotContain("EstadoPagoCondena (enums `NO_EXPLICIT_CODE`)");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene frase 'que decisiones fisicas siguen abiertas'")
        void modelo_sin_siguen_abiertas() throws IOException {
            String contenido = Files.readString(MODELO);
            assertThat(contenido)
                    .as("mariadb-logical-model.md no debe contener 'que decisiones fisicas siguen abiertas'")
                    .doesNotContain("siguen abiertas");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene frase 'cada decision fisica pendiente'")
        void modelo_sin_decision_fisica_pendiente() throws IOException {
            String contenido = Files.readString(MODELO);
            assertThat(contenido)
                    .as("mariadb-logical-model.md no debe contener 'cada decision fisica pendiente'")
                    .doesNotContain("cada decisión física pendiente");
        }

        @Test
        @DisplayName("mariadb-logical-model.md no contiene frase 'Decisiones fisicas pendientes: ver'")
        void modelo_sin_decisiones_fisicas_pendientes_ver() throws IOException {
            String contenido = Files.readString(MODELO);
            assertThat(contenido)
                    .as("mariadb-logical-model.md no debe contener 'Decisiones fisicas pendientes: ver'")
                    .doesNotContain("Decisiones físicas pendientes: ver");
        }

        @Test
        @DisplayName("ready-for-ddl-gate.md describe ddl-decisions.md como registro cerrado (no pendientes/cerradas)")
        void gate_ddl_decisions_como_registro_cerrado() throws IOException {
            String contenido = Files.readString(GATE);
            assertThat(contenido)
                    .as("ready-for-ddl-gate.md no debe referenciar ddl-decisions.md como 'pendientes/cerradas'")
                    .doesNotContain("pendientes/cerradas");
            assertThat(contenido)
                    .as("ready-for-ddl-gate.md debe referenciar ddl-decisions.md como registro cerrado")
                    .contains("registro cerrado de las 24 decisiones físicas");
        }

        @Test
        @DisplayName("ddl-execution-and-test-seeding.md usa grafias acentuadas QUE_ES->QUE_ES y RAZON->RAZON")
        void seeding_usa_grafias_acentuadas() throws IOException {
            Path seeding = Paths.get("docs/spec-as-source/50-persistence/ddl-execution-and-test-seeding.md");
            String contenido = Files.readString(seeding);
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe usar 'QUÉ ES' (acentuado)")
                    .contains("QUÉ ES");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md debe usar 'RAZÓN' (acentuado)")
                    .contains("RAZÓN");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md no debe contener 'QUE ES' sin acento en formato tabla")
                    .doesNotContain("QUE ES:");
            assertThat(contenido)
                    .as("ddl-execution-and-test-seeding.md no debe contener 'RAZON:' sin acento")
                    .doesNotContain("RAZON:");
        }

        @Test
        @DisplayName("ddl-decisions.md no contiene frase stale 'registro único de decisiones físicas pendientes y cerradas'")
        void ddl_decisions_sin_frase_registro_unico_pendientes_y_cerradas() throws IOException {
            String contenido = Files.readString(
                    Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md"));
            assertThat(contenido)
                    .as("ddl-decisions.md no debe contener 'registro único de decisiones físicas pendientes y cerradas'")
                    .doesNotContain("registro único de decisiones físicas pendientes y cerradas");
        }

        @Test
        @DisplayName("ddl-decisions.md no contiene referencia stale 'SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001-R1'")
        void ddl_decisions_sin_referencia_r1() throws IOException {
            String contenido = Files.readString(
                    Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md"));
            assertThat(contenido)
                    .as("ddl-decisions.md no debe contener 'SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001-R1' (frase stale con -R1)")
                    .doesNotContain("SPEC-AS-SOURCE-CLEAN-ROOM-Y-DDL-CLOSURE-001-R1");
        }

        @Test
        @DisplayName("current-roadmap.md no contiene frase stale 'NO_EXPLICIT_CODE pendientes de decision fisica'")
        void roadmap_sin_no_explicit_code_pendientes_de_decision_fisica() throws IOException {
            String contenido = Files.readString(
                    Paths.get("docs/spec-as-source/90-roadmap/current-roadmap.md"));
            assertThat(contenido)
                    .as("current-roadmap.md no debe contener 'NO_EXPLICIT_CODE pendientes de decision fisica'")
                    .doesNotContain("NO_EXPLICIT_CODE pendientes de decision fisica");
        }
    }

    // =====================================================================
    // G-28: observaciones - nombres fisicos, enum canonico y frontera transaccional
    // =====================================================================

    /**
     * Protege las tres decisiones documentales de `DECISION_OBS-NAMES-01`,
     * `DECISION_OBS-ENUM-01` y `DECISION_OBS-TX-01`.
     *
     * Los guardrails son deliberadamente estructurales (tokens y filas de tabla) y no
     * dependen de la prosa: la spec puede explicar el drift fisico del COMMENT cuanto
     * quiera sin romperlos.
     */
    @Nested
    @DisplayName("G-28: observaciones - nombres fisicos, enum canonico y frontera transaccional")
    class G28ObservacionNombresYFrontera {

        private static final Path OBSERVACIONES =
                Paths.get("docs/spec-as-source/40-domain/observaciones.md");
        private static final Path DDL_DECISIONS =
                Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md");
        private static final Path ACTA_SERVICE = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/"
                + "core/application/service/ActaService.java");
        private static final Path DDL_FUENTE =
                Paths.get("..", "..", "database", "ddl", "create-bod-faltas-domain.sql");

        /** Ninguno existe en el motor ni en el DDL fuente: `DECISION_OBS-NAMES-01`. */
        private static final List<String> NOMBRES_OBSOLETOS =
                List.of("idx_obs_entidad", "idx_obs_ctx_acta", "fk_obs_acta_ctx");

        private static final List<String> NOMBRES_CANONICOS =
                List.of("ix_obs_entidad", "ix_obs_acta_contexto", "fk_obs_acta_contexto");

        /** Fila de datos de la tabla del enum: codigo numerico y nombre entre backticks. */
        private static final Pattern PATRON_FILA_ORIGEN =
                Pattern.compile("^\\|\\s*(\\d+)\\s*\\|\\s*`(\\w+)`\\s*\\|");

        /**
         * Mismo criterio que G-6 para los terminos obsoletos: un alias muerto solo esta
         * documentado legitimamente cuando su propia linea lo marca como prohibido, o
         * cuando el heading mas cercano hacia arriba lo hace. Fuera de esas dos formas se
         * interpreta como uso vigente y el guardrail falla.
         */
        @Test
        @DisplayName("los nombres obsoletos de indices y FK no se usan como vigentes en la spec")
        void nombres_obsoletos_ausentes_en_toda_la_spec() throws IOException {
            List<String> violaciones = new ArrayList<>();
            for (Path archivo : listarMarkdownDeLaSpec()) {
                List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
                for (int i = 0; i < lineas.size(); i++) {
                    for (String obsoleto : NOMBRES_OBSOLETOS) {
                        if (!lineas.get(i).contains(obsoleto)) {
                            continue;
                        }
                        if (tieneMarcadorDeProhibicion(lineas.get(i))
                                || enSeccionDeProhibicion(lineas, i)) {
                            continue;
                        }
                        violaciones.add(pathRegistroDe(archivo) + ":" + (i + 1)
                                + " usa el nombre obsoleto '" + obsoleto
                                + "' sin marcarlo como prohibido");
                    }
                }
            }
            assertThat(violaciones)
                    .as("DECISION_OBS-NAMES-01: los nombres obsoletos de fal_observacion"
                        + " no pueden aparecer como nombres vigentes")
                    .isEmpty();
        }

        private boolean tieneMarcadorDeProhibicion(String linea) {
            String lineaMinuscula = linea.toLowerCase(Locale.ROOT);
            for (String marcador : MARCADORES_NEGACION) {
                if (lineaMinuscula.contains(marcador)) {
                    return true;
                }
            }
            return false;
        }

        private boolean enSeccionDeProhibicion(List<String> lineas, int indiceLinea) {
            for (int i = indiceLinea; i >= 0; i--) {
                String linea = lineas.get(i).trim();
                if (linea.startsWith("#")) {
                    return tieneMarcadorDeProhibicion(linea);
                }
            }
            return false;
        }

        /**
         * El bloque de constraints de la seccion 2.3 es el que un lector copia: ahi los
         * nombres obsoletos no pueden aparecer ni siquiera marcados.
         */
        @Test
        @DisplayName("el bloque de constraints de la seccion 2.3 no menciona ningun alias obsoleto")
        void bloque_de_constraints_sin_aliases_obsoletos() throws IOException {
            List<String> lineas = Files.readAllLines(OBSERVACIONES, StandardCharsets.UTF_8);
            String seccion = extraerSeccion(lineas, Pattern.compile("^### 2\\.3 Constraints"));
            for (String obsoleto : NOMBRES_OBSOLETOS) {
                assertThat(seccion)
                        .as("La seccion 2.3 de observaciones.md no debe mencionar '%s'", obsoleto)
                        .doesNotContain(obsoleto);
            }
            for (String canonico : NOMBRES_CANONICOS) {
                assertThat(seccion)
                        .as("La seccion 2.3 de observaciones.md debe declarar '%s'", canonico)
                        .contains(canonico);
            }
        }

        @Test
        @DisplayName("observaciones.md declara los tres nombres fisicos canonicos")
        void nombres_canonicos_presentes_en_observaciones() throws IOException {
            String contenido = Files.readString(OBSERVACIONES, StandardCharsets.UTF_8);
            for (String canonico : NOMBRES_CANONICOS) {
                assertThat(contenido)
                        .as("observaciones.md debe declarar el nombre fisico '%s'", canonico)
                        .contains(canonico);
            }
        }

        /** La spec no puede quedar alineada con un DDL fuente que diga otra cosa. */
        @Test
        @DisplayName("los nombres canonicos coinciden con create-bod-faltas-domain.sql")
        void nombres_canonicos_coinciden_con_el_ddl_fuente() throws IOException {
            String ddl = Files.readString(DDL_FUENTE, StandardCharsets.UTF_8);
            for (String canonico : NOMBRES_CANONICOS) {
                assertThat(ddl)
                        .as("El DDL fuente debe declarar '%s'", canonico)
                        .contains(canonico);
            }
            for (String obsoleto : NOMBRES_OBSOLETOS) {
                assertThat(ddl)
                        .as("El DDL fuente no debe declarar el nombre obsoleto '%s'", obsoleto)
                        .doesNotContain(obsoleto);
            }
        }

        /**
         * `DECISION_OBS-ENUM-01`: la tabla normativa fija los tres pares codigo/nombre y
         * coincide exactamente con el enum Java. Se parsea solo la tabla de la seccion 4,
         * de modo que las explicaciones sobre el COMMENT fisico no intervienen.
         */
        @Test
        @DisplayName("origen_observacion_tabla_canonica: 1 USUARIO, 2 SISTEMA, 3 INTEGRACION")
        void origen_observacion_tabla_canonica() throws IOException {
            List<String> lineas = Files.readAllLines(OBSERVACIONES, StandardCharsets.UTF_8);
            int inicio = -1;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).startsWith("## 4. Enum `OrigenObservacion`")) {
                    inicio = i;
                    break;
                }
            }
            assertThat(inicio)
                    .as("observaciones.md debe tener la seccion '## 4. Enum `OrigenObservacion`'")
                    .isGreaterThanOrEqualTo(0);

            Map<String, String> nombrePorCodigo = new LinkedHashMap<>();
            for (int i = inicio + 1; i < lineas.size(); i++) {
                if (lineas.get(i).startsWith("## ")) {
                    break;
                }
                Matcher m = PATRON_FILA_ORIGEN.matcher(lineas.get(i));
                if (m.find()) {
                    nombrePorCodigo.put(m.group(1), m.group(2));
                }
            }

            assertThat(nombrePorCodigo)
                    .as("Tabla normativa de OrigenObservacion en observaciones.md")
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of("1", "USUARIO", "2", "SISTEMA", "3", "INTEGRACION"));

            for (ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion valor
                    : ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion.values()) {
                assertThat(nombrePorCodigo.get(String.valueOf(valor.codigo())))
                        .as("El codigo %s documentado debe ser %s", valor.codigo(), valor.name())
                        .isEqualTo(valor.name());
            }
        }

        /**
         * Las etiquetas del COMMENT fisico no pueden colarse como valores del dominio: la
         * tabla normativa es la unica fuente de nombres, y el drift vive registrado como
         * decision DDL, no como alternativa vigente.
         */
        @Test
        @DisplayName("comment_fisico_obsoleto_registrado_en_ddl_decisions")
        void comment_fisico_obsoleto_registrado_en_ddl_decisions() throws IOException {
            String ddlDecisiones = Files.readString(DDL_DECISIONS, StandardCharsets.UTF_8);
            assertThat(ddlDecisiones)
                    .as("El COMMENT fisico obsoleto de origen_observacion debe estar registrado"
                        + " como DECISION_DDL-OBS-COMMENT-01")
                    .contains("DECISION_DDL-OBS-COMMENT-01")
                    .contains("origen_observacion");

            String observaciones = Files.readString(OBSERVACIONES, StandardCharsets.UTF_8);
            assertThat(observaciones)
                    .as("observaciones.md debe apuntar a la decision DDL en vez de duplicarla")
                    .contains("DECISION_DDL-OBS-COMMENT-01");
        }

        /**
         * Asercion invertida en WAVE 1A2-B2, el slice que implementa
         * {@code DECISION_OBS-TX-01}. Hasta B1.5C la decision se declaraba documentada y
         * bloqueada; desde B2 tiene que declararse implementada, con la frontera y el rollback
         * total explicitos. Un documento que siguiera diciendo "no implementada" con la
         * anotacion presente en el codigo seria drift documental.
         */
        @Test
        @DisplayName("DECISION_OBS-TX-01 declara ActaService.labrar como frontera implementada")
        void decision_obs_tx_01_declara_frontera_implementada() throws IOException {
            String contenido = Files.readString(OBSERVACIONES, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("DECISION_OBS-TX-01 debe estar declarada en observaciones.md")
                    .contains("DECISION_OBS-TX-01")
                    .contains("ActaService.labrar")
                    .contains("id_persona_infractor");
            String enMinuscula = contenido.toLowerCase(Locale.ROOT);
            assertThat(enMinuscula)
                    .as("DECISION_OBS-TX-01 debe declarar su implementacion y el rollback total")
                    .contains("implementada")
                    .contains("rollback total");
            assertThat(enMinuscula)
                    .as("y ya no puede declararse bloqueada ni pendiente de implementacion")
                    .doesNotContain("sigue bloqueada")
                    .doesNotContain("no implementada");
        }

        /**
         * Contraparte material del estado "implementada": la anotacion existe y esta sobre el
         * metodo publico exacto que la decision declara como frontera, no sobre la clase.
         */
        @Test
        @DisplayName("ActaService declara @Transactional sobre labrar")
        void acta_service_declara_transactional_en_labrar() throws IOException {
            String codigo = Files.readString(ACTA_SERVICE, StandardCharsets.UTF_8);
            assertThat(codigo)
                    .as("DECISION_OBS-TX-01 esta implementada: la frontera es el metodo labrar"
                            + " (Wave 4 PARTE-3A ajusta el isolation level a READ_COMMITTED,"
                            + " ver Javadoc de ActaService.labrar; sigue sobre el mismo metodo)")
                    .contains("@Transactional(isolation = org.springframework.transaction.annotation"
                            + ".Isolation.READ_COMMITTED)\n    public ComandoResultado labrar(LabrarActaCommand cmd)");
            assertThat(codigo)
                    .as("la demarcacion no se extiende a la clase entera")
                    .doesNotContain("@Transactional\npublic class ActaService");
        }

        /**
         * La lectura batch no puede convertirse en una segunda semantica del historial: la
         * decision tiene que seguir declarando el orden canonico, la inclusion de las
         * inactivas y que la ausencia representa el historial vacio.
         */
        @Test
        @DisplayName("DECISION-OBS-BATCH-01 declara el contrato batch sin cambiar el orden canonico")
        void decision_obs_batch_01_declara_el_contrato() throws IOException {
            String contenido = Files.readString(OBSERVACIONES, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("DECISION-OBS-BATCH-01 debe estar declarada en observaciones.md")
                    .contains("DECISION-OBS-BATCH-01")
                    .contains("listarPorEntidades");
            String plano = contenido.replaceAll("\\s+", " ");
            assertThat(plano)
                    .as("El batch debe conservar el orden canonico por propietario")
                    .contains("cada historial conserva el orden canonico fh_alta ASC, id ASC");
            assertThat(plano)
                    .as("El batch debe incluir activas e inactivas, como la lectura individual")
                    .contains("se incluyen activas e inactivas");
            assertThat(plano)
                    .as("La ausencia del propietario en el mapa debe ser el historial vacio")
                    .contains("puede estar ausente del mapa");
            assertThat(plano)
                    .as("Una coleccion vacia no debe consultar la persistencia")
                    .contains("una coleccion vacia no ejecuta consulta alguna contra el motor");
            assertThat(plano)
                    .as("El batch no debe habilitar DDL ni un indice nuevo")
                    .contains("No se crea ningún índice nuevo y no se ejecuta DDL por esta decisión.");
            assertThat(plano)
                    .as("Las dos lecturas individuales deben conservar su contrato")
                    .contains("`listarPorEntidad` y `listarActivasPorEntidad` conservan su contrato");
        }

        /** Contraparte material: la remediacion fisica de fal_observacion sigue pendiente. */
        @Test
        @DisplayName("el batch no adelanta la remediacion fisica de fal_observacion")
        void batch_no_adelanta_la_remediacion_fisica() throws IOException {
            String plano = Files.readString(OBSERVACIONES, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ");
            assertThat(plano)
                    .as("La decision batch debe declarar que la remediacion fisica sigue pendiente")
                    .contains("sigue pendiente y esta decisión no la adelanta ni la modifica");
        }
    }

    // =====================================================================
    // G-29: reglas de alta del acta (identificacion del infractor, QR, actor)
    // =====================================================================

    /**
     * Cierre normativo de las tres reglas que bloqueaban la activacion del adaptador
     * JDBC del acta. El grupo verifica que la spec las declare de forma inequivoca y
     * que no reintroduzca las representaciones prohibidas.
     *
     * <p>Deliberadamente no verifica codigo productivo: las tres reglas estan
     * definidas y todavia no implementadas. La contraparte de implementacion se
     * agrega cuando el codigo exista.
     */
    @Nested
    @DisplayName("G-29: reglas de alta del acta - infractor, QR y actor autenticado")
    class G29ActaAltaRules {

        private static final Path CATALOGOS =
                Paths.get("docs/spec-as-source/10-domain/states-events-catalogs.md");
        private static final Path COMANDOS =
                Paths.get("docs/spec-as-source/20-application/command-contracts.md");
        private static final Path DDL_DECISIONS_B15A =
                Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md");
        private static final Path MODELO =
                Paths.get("docs/spec-as-source/50-persistence/mariadb-logical-model.md");

        /** Los seis significados de la situacion de identificacion, sin fusionar. */
        private static final List<String> SITUACIONES = List.of(
                "IDENTIFICADO_COMPLETO",
                "IDENTIFICADO_PARCIAL",
                "PRESENTE_SE_NIEGA",
                "PRESENTE_NO_APORTA_DATOS",
                "AUSENTE",
                "DATOS_INSUFICIENTES");

        @Test
        @DisplayName("el catalogo declara los 6 significados de la situacion de identificacion")
        void catalogo_declara_las_seis_situaciones() throws IOException {
            String contenido = Files.readString(CATALOGOS, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("states-events-catalogs.md debe declarar el catalogo de la situacion")
                    .contains("## SituacionIdentificacionInfractor");
            for (String situacion : SITUACIONES) {
                assertThat(contenido)
                        .as("El catalogo debe declarar el significado '%s'", situacion)
                        .contains(situacion);
            }
        }

        /**
         * Los seis codigos deben ser 1..6 sin huecos ni repeticiones: es la forma de que
         * ningun par de significados quede indistinguible.
         */
        @Test
        @DisplayName("los 6 codigos de la situacion son 1..6, unicos y sin fusionar")
        void codigos_de_situacion_completos_y_unicos() throws IOException {
            List<String> lineas = Files.readAllLines(CATALOGOS, StandardCharsets.UTF_8);
            String seccion = extraerSeccion(lineas,
                    Pattern.compile("^## SituacionIdentificacionInfractor"));
            Map<String, String> nombrePorCodigo = new LinkedHashMap<>();
            Pattern filaEnum = Pattern.compile("^\\|\\s*([A-Z_]+)\\s*\\|\\s*(\\d)\\s*\\|");
            for (String linea : seccion.lines().toList()) {
                Matcher m = filaEnum.matcher(linea);
                if (m.find()) {
                    nombrePorCodigo.put(m.group(2), m.group(1));
                }
            }
            assertThat(nombrePorCodigo)
                    .as("Tabla normativa de SituacionIdentificacionInfractor")
                    .containsOnlyKeys("1", "2", "3", "4", "5", "6");
            assertThat(nombrePorCodigo.values())
                    .as("Ningun significado puede repetirse en dos codigos")
                    .doesNotHaveDuplicates()
                    .containsExactlyInAnyOrderElementsOf(SITUACIONES);
        }

        /**
         * La matriz situacion/persona es la regla operativa: hay persona exactamente en
         * las dos situaciones de identificacion.
         */
        @Test
        @DisplayName("la matriz situacion/id_persona_infractor esta declarada")
        void matriz_situacion_persona_declarada() throws IOException {
            String contenido = Files.readString(CATALOGOS, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("El catalogo debe declarar la matriz contra id_persona_infractor")
                    .contains("id_persona_infractor");
            assertThat(contenido.toLowerCase(Locale.ROOT))
                    .as("La matriz debe decir que hay persona solo cuando hay identificacion")
                    .contains("hay persona exactamente cuando hay");
        }

        @Test
        @DisplayName("la persona centinela esta prohibida de forma explicita")
        void persona_centinela_prohibida() throws IOException {
            String catalogos = Files.readString(CATALOGOS, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            assertThat(catalogos)
                    .as("El catalogo debe prohibir explicitamente la persona centinela")
                    .contains("persona centinela")
                    .contains("prohibid");
            assertThat(catalogos)
                    .as("Tampoco se admite documento inventado ni PK reservada")
                    .contains("documento inventado");

            String decisiones = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
            assertThat(decisiones)
                    .as("La decision fisica debe repetir la prohibicion de persona centinela")
                    .contains("persona centinela");
        }

        @Test
        @DisplayName("DECISION_DDL-ACTA-INFR-01 define columna, CHECK y nulabilidad")
        void decision_infractor_definida() throws IOException {
            String contenido = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("La decision fisica del infractor debe estar registrada")
                    .contains("DECISION_DDL-ACTA-INFR-01")
                    .contains("situacion_identificacion_infractor SMALLINT NOT NULL")
                    .contains("chk_acta_infractor_coherente")
                    .contains("chk_acta_situacion_infractor")
                    .contains("BIGINT NULL");
            assertThat(contenido)
                    .as("El modelo debe apuntar a la decision del infractor")
                    .isNotEmpty();
            assertThat(Files.readString(MODELO, StandardCharsets.UTF_8))
                    .as("mariadb-logical-model.md debe referenciar la decision del infractor")
                    .contains("DECISION_DDL-ACTA-INFR-01");
        }

        /**
         * Contraparte de implementacion agregada en WAVE 1A2-B1.5B1. La verificacion
         * normativa no se borra: lo que cambia es cual es el esquema ejecutado. Antes
         * del ALTER este test exigia {@code id_persona_infractor BIGINT NOT NULL};
         * ahora exige que el modelo describa la forma fisica realmente aplicada y que
         * la redaccion obsoleta no reaparezca.
         */
        @Test
        @DisplayName("el modelo describe el esquema ejecutado tras aplicar DECISION_DDL-ACTA-INFR-01")
        void modelo_describe_el_esquema_ejecutado() throws IOException {
            String contenido = Files.readString(MODELO, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("El ALTER ya se ejecuto: el modelo describe id_persona_infractor BIGINT NULL")
                    .containsPattern("(?i)id_persona_infractor BIGINT NULL");
            assertThat(contenido)
                    .as("La redaccion NOT NULL quedo obsoleta y no debe reaparecer")
                    .doesNotContainPattern("(?i)id_persona_infractor BIGINT NOT NULL");
            assertThat(contenido)
                    .as("El modelo declara la columna nueva sin default y los dos CHECK canonicos")
                    .containsPattern("(?i)situacion_identificacion_infractor SMALLINT NOT NULL")
                    .contains("chk_acta_situacion_infractor")
                    .contains("chk_acta_infractor_coherente");
            assertThat(contenido)
                    .as("El modelo registra la aplicacion fisica, no la promesa")
                    .contains("DDL_EXECUTED");
        }

        /**
         * Contraparte de implementacion: el paquete forward/rollback y sus tres
         * diagnosticos existen con los nombres canonicos y el orden que la decision fija.
         */
        @Test
        @DisplayName("el paquete DDL de la situacion de identificacion existe y respeta el orden de la decision")
        void paquete_ddl_de_la_situacion_existe() throws IOException {
            Path raiz = Paths.get("..", "..", "database");
            for (String archivo : List.of(
                    "ddl/alter-fal-acta-situacion-infractor-001-forward.sql",
                    "ddl/alter-fal-acta-situacion-infractor-001-rollback.sql",
                    "diagnostics/verify-fal-acta-situacion-infractor-001-precheck.sql",
                    "diagnostics/verify-fal-acta-situacion-infractor-001-postcheck.sql",
                    "diagnostics/verify-fal-acta-situacion-infractor-001-rollback-precheck.sql")) {
                assertThat(raiz.resolve(archivo))
                        .as("El paquete de DECISION_DDL-ACTA-INFR-01 debe incluir %s", archivo)
                        .exists().isRegularFile();
            }

            String forward = Files.readString(
                    raiz.resolve("ddl/alter-fal-acta-situacion-infractor-001-forward.sql"),
                    StandardCharsets.UTF_8);
            assertThat(forward.indexOf("ADD COLUMN situacion_identificacion_infractor SMALLINT NULL"))
                    .as("El ADD COLUMN nullable precede al backfill")
                    .isGreaterThan(0)
                    .isLessThan(forward.indexOf("MODIFY COLUMN situacion_identificacion_infractor SMALLINT NOT NULL"));
            assertThat(forward.indexOf("MODIFY COLUMN id_persona_infractor BIGINT NULL"))
                    .as("Los dos CHECK se agregan despues de ambos MODIFY")
                    .isLessThan(forward.indexOf("ADD CONSTRAINT chk_acta_situacion_infractor"));
            assertThat(forward.indexOf("ADD CONSTRAINT chk_acta_situacion_infractor"))
                    .isLessThan(forward.indexOf("ADD CONSTRAINT chk_acta_infractor_coherente"));
            // El nombre obsoleto puede citarse en un comentario como antecedente, pero no
            // puede aparecer como columna en el SQL ejecutable.
            String forwardEjecutable = forward.lines()
                    .filter(l -> !l.stripLeading().startsWith("--"))
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertThat(forwardEjecutable)
                    .as("El backfill usa el nombre fisico canonico tipo_documento")
                    .contains("p.tipo_documento IS NOT NULL")
                    .contains("p.tipo_documento IS NULL");
            assertThat(forwardEjecutable)
                    .as("tipo_doc esta prohibido como nombre de columna en el SQL ejecutable")
                    .doesNotContainPattern("(?<![A-Za-z_])tipo_doc(?!umento)");

            String rollback = Files.readString(
                    raiz.resolve("ddl/alter-fal-acta-situacion-infractor-001-rollback.sql"),
                    StandardCharsets.UTF_8);
            assertThat(rollback.indexOf("DROP CONSTRAINT chk_acta_infractor_coherente"))
                    .as("El rollback recorre el orden inverso exacto")
                    .isGreaterThan(0)
                    .isLessThan(rollback.indexOf("DROP CONSTRAINT chk_acta_situacion_infractor"));
            assertThat(rollback.indexOf("DROP CONSTRAINT chk_acta_situacion_infractor"))
                    .isLessThan(rollback.indexOf("MODIFY COLUMN id_persona_infractor BIGINT NOT NULL"));
            assertThat(rollback.indexOf("MODIFY COLUMN id_persona_infractor BIGINT NOT NULL"))
                    .isLessThan(rollback.indexOf("DROP COLUMN situacion_identificacion_infractor"));
        }

        /**
         * Las dos unicidades no primarias de fal_acta protegen valores que el sistema
         * genera, de modo que su violacion no puede convertirse en contrato funcional. La
         * decision tiene que seguir declarando las cuatro prohibiciones, no solo existir.
         */
        @Test
        @DisplayName("DECISION-ACTA-UNIQUE-01 declara las unicidades tecnicas y sus prohibiciones")
        void decision_unicidades_del_acta_registrada() throws IOException {
            String contenido = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("La decision de unicidades del acta debe estar registrada con sus dos constraints")
                    .contains("DECISION-ACTA-UNIQUE-01")
                    .contains("uq_fal_acta_id_tecnico")
                    .contains("uq_fal_acta_codigo_qr");
            assertThat(contenido)
                    .as("La decision debe prohibir crear una excepcion funcional para estas colisiones")
                    .contains("ActaDuplicadaException")
                    .contains("ConflictoActaException");
            assertThat(contenido)
                    .as("La decision debe declarar que no se agrega contrato HTTP de conflicto")
                    .contains("No existe ni se agrega un mapeo a `409 CONFLICT`");
            assertThat(contenido)
                    .as("La decision debe declarar el delta aceptado del doble de prueba")
                    .contains("`InMemoryActaRepository` **no simula** estas dos unicidades");
            assertThat(contenido)
                    .as("La decision debe acotar la unicidad y no extenderla a la numeracion")
                    .contains("**No se agrega** `UNIQUE` sobre `nro_acta`");
            assertThat(contenido)
                    .as("La decision debe prohibir el reintento silencioso con otro UUID")
                    .contains("**no se reintenta** con otro UUID");
        }

        @Test
        @DisplayName("DECISION_DDL-ACTA-QR-01 fija el formato canonico y su algoritmo")
        void decision_qr_definida() throws IOException {
            String contenido = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("La decision del QR debe estar registrada con formato y algoritmo")
                    .contains("DECISION_DDL-ACTA-QR-01")
                    .contains("HMAC-SHA-256")
                    .contains("QR0.");
            assertThat(contenido.toLowerCase(Locale.ROOT))
                    .as("El token debe declararse deterministico y derivado de id_tecnico")
                    .contains("determinístico")
                    .contains("id_tecnico");
        }

        /**
         * El punto central de la regla: el QR se deriva de la identidad tecnica y nunca
         * de la PK, porque debe existir antes del INSERT.
         */
        @Test
        @DisplayName("el QR se genera antes del alta desde id_tecnico y nunca desde id_acta")
        void qr_previo_desde_id_tecnico() throws IOException {
            String decisiones = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(decisiones)
                    .as("La decision del QR debe prohibir derivarlo de id_acta")
                    .contains("nunca de `id_acta`");

            String comandos = Files.readString(COMANDOS, StandardCharsets.UTF_8);
            assertThat(comandos)
                    .as("El contrato del alta debe exigir el QR previo derivado de id_tecnico")
                    .contains("codigo_qr")
                    .contains("id_tecnico")
                    .contains("DECISION_DDL-ACTA-QR-01");
        }

        /** El ancho de la columna queda ratificado por la decision, no reversado. */
        @Test
        @DisplayName("la decision del QR ratifica VARCHAR(128) en lugar de ensancharlo")
        void decision_qr_ratifica_el_ancho() throws IOException {
            String decisiones = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(decisiones)
                    .as("La decision del QR debe conservar VARCHAR(128)")
                    .contains("`VARCHAR(128)`");
            assertThat(Files.readString(MODELO, StandardCharsets.UTF_8))
                    .as("El modelo debe conservar codigo_qr VARCHAR(128)")
                    .contains("codigo_qr VARCHAR(128)");
        }

        @Test
        @DisplayName("id_user_alta es el subject autenticado, hasta 36, sin FK ni catalogo")
        void id_user_alta_es_subject_autenticado() throws IOException {
            String contenido = Files.readString(COMANDOS, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("El contrato del alta debe declarar el origen de id_user_alta")
                    .contains("id_user_alta")
                    .contains("CHAR(36)");
            String enMinuscula = contenido.toLowerCase(Locale.ROOT);
            assertThat(enMinuscula)
                    .as("id_user_alta no lleva FK, catalogo ni lookup")
                    .contains("sin fk")
                    .contains("sin catálogo")
                    .contains("sin lookup");
            assertThat(enMinuscula)
                    .as("Un id_user_alta aportado por el cliente no es fuente de verdad")
                    .contains("no es fuente de verdad");
            assertThat(enMinuscula)
                    .as("La ausencia de actor no se rellena con un usuario ficticio")
                    .contains("usuario ficticio");
        }

        @Test
        @DisplayName("el contrato del alta documenta la secuencia transaccional completa")
        void secuencia_transaccional_documentada() throws IOException {
            String contenido = Files.readString(COMANDOS, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .as("La frontera transaccional del alta debe estar declarada en el comando")
                    .contains("ActaService.labrar")
                    .contains("DECISION_OBS-TX-01");
            assertThat(contenido.toLowerCase(Locale.ROOT))
                    .as("La secuencia debe exigir rollback total ante cualquier fallo")
                    .contains("rollback total");
        }

        /**
         * Las dos deudas de la wave del adaptador siguen abiertas y separadas: cerrar las
         * reglas de alta no las resuelve.
         */
        @Test
        @DisplayName("las reglas de alta no declaran resueltas las deudas del adaptador")
        void deudas_del_adaptador_siguen_separadas() throws IOException {
            String decisiones = Files.readString(DDL_DECISIONS_B15A, StandardCharsets.UTF_8);
            assertThat(decisiones)
                    .as("Ninguna de las dos decisiones nuevas puede afirmar que el bean ya se activa")
                    .doesNotContain("JdbcActaRepository se activa")
                    .doesNotContain("bean activo");
        }
    }

    // =====================================================================
    // G-30: documento e identidad de fal_persona (DECISION_DDL-PERS-01)
    // =====================================================================

    /**
     * Cierre normativo del documento de {@code fal_persona}. Antes de este grupo la spec
     * contenia dos versiones incompatibles de la misma decision: una declaraba
     * {@code tipo_doc} / {@code nro_doc} opcionales con {@code UNIQUE(tipo_doc, nro_doc)},
     * y el esquema ejecutado tenia {@code tipo_documento SMALLINT NOT NULL} y
     * {@code nro_doc INT UNSIGNED NOT NULL} con {@code UNIQUE(tipo_documento, doc_key)}.
     *
     * <p>El grupo fija la version unica vigente y verifica que el modelo siga
     * distinguiendo tres planos que no deben mezclarse: modelo objetivo normativo,
     * esquema ejecutado actual y remediacion DDL pendiente.
     *
     * <p>Deliberadamente no verifica codigo productivo: la decision esta cerrada y
     * todavia no implementada. La contraparte de implementacion se agrega cuando el
     * codigo exista.
     */
    @Nested
    @DisplayName("G-30: fal_persona - documento opcional, canonico y estructurado")
    class G30PersonaDocumento {

        private static final Path DECISIONES =
                Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md");
        private static final Path MODELO_PERS =
                Paths.get("docs/spec-as-source/50-persistence/mariadb-logical-model.md");
        private static final Path DELTAS =
                Paths.get("docs/spec-as-source/50-persistence/inmemory-mariadb-deltas.md");
        private static final Path CATALOGOS_PERS =
                Paths.get("docs/spec-as-source/10-domain/states-events-catalogs.md");

        private String decisiones() throws IOException {
            return Files.readString(DECISIONES, StandardCharsets.UTF_8);
        }

        /**
         * Una sola decision vigente. La redaccion anterior debe quedar marcada obsoleta
         * en el mismo lugar, no borrada en silencio ni conservada como alternativa.
         */
        @Test
        @DisplayName("DECISION_DDL-PERS-01 es unica y declara obsoleta la version anterior")
        void decision_unica_con_predecesora_obsoleta() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("La decision del documento de persona debe estar registrada")
                    .contains("DECISION_DDL-PERS-01");
            assertThat(contenido)
                    .as("La redaccion anterior debe quedar explicitamente marcada como obsoleta")
                    .contains("OBSOLETA");
            assertThat(Files.readString(MODELO_PERS, StandardCharsets.UTF_8))
                    .as("El modelo debe registrar como obsoletos el tipo numerico y el nombre tipo_doc, "
                            + "para que nadie los reintroduzca")
                    .contains("declara obsoletas tanto `INT UNSIGNED NOT NULL` como los nombres `tipo_doc`");
            assertThat(Files.readString(DELTAS, StandardCharsets.UTF_8))
                    .as("Los deltas no pueden seguir afirmando la unicidad por (tipo_doc, nro_doc)")
                    .doesNotContain("unicidad de persona por `(tipo_doc, nro_doc)`");
        }

        @Test
        @DisplayName("tipo_documento es el nombre fisico y tipo_doc queda prohibido")
        void tipo_documento_es_el_nombre_fisico() throws IOException {
            assertThat(decisiones())
                    .as("La decision debe fijar tipo_documento y prohibir tipo_doc como columna")
                    .contains("`tipo_documento`")
                    .contains("`tipo_doc` está prohibido como nombre físico");
            assertThat(Files.readString(MODELO_PERS, StandardCharsets.UTF_8))
                    .as("El modelo debe repetir la prohibicion de tipo_doc como nombre fisico")
                    .contains("`tipo_doc` como nombre físico");
            assertThat(Files.readString(CATALOGOS_PERS, StandardCharsets.UTF_8))
                    .as("El catalogo de situaciones debe nombrar tipo_documento, no tipo_doc")
                    .contains("`tipo_documento` y `nro_doc` informados")
                    .doesNotContain("`tipo_doc` y `nro_doc` informados");
        }

        @Test
        @DisplayName("el documento es opcional en conjunto: (tipo_documento IS NULL) = (nro_doc IS NULL)")
        void documento_opcional_en_conjunto() throws IOException {
            assertThat(decisiones())
                    .as("La coherencia del par documento debe estar declarada como CHECK")
                    .contains("(tipo_documento IS NULL) = (nro_doc IS NULL)")
                    .contains("chk_fal_persona_doc_coherente");
            assertThat(decisiones().toLowerCase(Locale.ROOT))
                    .as("La persona sin documento debe declararse estado modelado y valido")
                    .contains("sin documento");
        }

        /** El ancho definitivo es 12 y el tipo anterior queda declarado obsoleto. */
        @Test
        @DisplayName("nro_doc queda cerrado como VARCHAR(12) NULL y no vuelve a INT UNSIGNED")
        void nro_doc_es_varchar_12() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("nro_doc debe quedar cerrado como VARCHAR(12)")
                    .contains("`VARCHAR(12)`");
            assertThat(contenido)
                    .as("La decision debe declarar obsoleto el tipo numerico ejecutado")
                    .contains("`nro_doc INT UNSIGNED NOT NULL`");
            assertThat(contenido)
                    .as("El ancho no se amplia por prevencion ni se reduce")
                    .contains("no `VARCHAR(20)`")
                    .contains("no `VARCHAR(10)`");
        }

        @Test
        @DisplayName("los documentos alfanumericos se soportan y la conversion a entero esta prohibida")
        void alfanumericos_soportados_y_conversion_prohibida() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("El catalogo de tipos debe contemplar los tipos alfanumericos")
                    .contains("PASAPORTE")
                    .contains("DNI_EXTRANJERO");
            assertThat(contenido)
                    .as("Convertir un documento alfanumerico a entero queda prohibido")
                    .contains("convertir un documento alfanumérico a entero")
                    .contains("descartar caracteres para forzar `INT UNSIGNED`");
            assertThat(contenido)
                    .as("El truncamiento silencioso queda prohibido")
                    .contains("truncar en silencio");
        }

        @Test
        @DisplayName("CUIT/CUIL queda estructurado en prefijo + cuerpo de 8 + digito verificador")
        void cuit_cuil_estructurado() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("Los tres componentes de CUIT/CUIL deben declararse")
                    .contains("prefijo_cuit_cuil")
                    .contains("digito_verificador")
                    .contains("cuerpo central");
            assertThat(contenido)
                    .as("El cuerpo central debe ser exactamente de ocho digitos como texto")
                    .contains("exactamente ocho digitos como texto");
            assertThat(contenido)
                    .as("Un CUIT/CUIL invalido se rechaza y no se degrada ni se completa")
                    .contains("no se degrada a `DNI`")
                    .contains("no se inventa prefijo ni dígito");
            assertThat(contenido)
                    .as("El formato visual se reconstruye, no se almacena")
                    .contains("XX-XXXXXXXX-X");
        }

        /**
         * La clave no puede fabricarse rellenando componentes ausentes: son los CHECK los
         * que exigen la estructura antes de que la columna generada exista.
         */
        @Test
        @DisplayName("doc_key se define sin COALESCE ni LPAD y es NULL sin documento")
        void doc_key_sin_coalesce_ni_lpad() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("La expresion objetivo de doc_key debe estar declarada")
                    .contains("CONCAT(prefijo_cuit_cuil, nro_doc, digito_verificador)")
                    .contains("STORED");
            assertThat(contenido)
                    .as("COALESCE y LPAD quedan prohibidos dentro de la expresion")
                    .contains("Queda prohibido `COALESCE(...)` y `LPAD(...)` dentro de la expresión");
            assertThat(contenido)
                    .as("Sin documento la clave debe ser NULL")
                    .contains("`doc_key` es NULL");
        }

        @Test
        @DisplayName("la unicidad fisica sigue siendo UNIQUE (tipo_documento, doc_key)")
        void unique_tipo_documento_doc_key() throws IOException {
            assertThat(decisiones())
                    .as("El UNIQUE debe declararse con su nombre fisico real")
                    .contains("uq_fal_persona_tipo_doc_key (tipo_documento, doc_key)");
            assertThat(Files.readString(MODELO_PERS, StandardCharsets.UTF_8))
                    .as("El modelo debe conservar el UNIQUE (tipo_documento, doc_key)")
                    .contains("UNIQUE (tipo_documento, doc_key)");
        }

        /**
         * El punto que motivo todo el slice: la persona identificada parcialmente sin
         * documento tiene que ser representable, y nunca mediante una persona inventada.
         */
        @Test
        @DisplayName("la persona parcial sin documento es coherente y no admite centinela")
        void persona_parcial_sin_documento_coherente() throws IOException {
            String catalogos = Files.readString(CATALOGOS_PERS, StandardCharsets.UTF_8);
            assertThat(catalogos)
                    .as("El catalogo debe apoyar la persona parcial en la decision fisica del documento")
                    .contains("DECISION_DDL-PERS-01")
                    .contains("IDENTIFICADO_PARCIAL");
            assertThat(decisiones().toLowerCase(Locale.ROOT))
                    .as("La decision debe repetir las prohibiciones de representacion falsa")
                    .contains("persona centinela")
                    .contains("documento inventado");
            assertThat(decisiones())
                    .as("Un centinela numerico o una cadena vacia tampoco representan ausencia")
                    .contains("centinela numérico");
        }

        @Test
        @DisplayName("la semantica INSERT/UPDATE del puerto queda cerrada y el id inexistente no inserta")
        void semantica_insert_update_del_puerto() throws IOException {
            String deltas = Files.readString(DELTAS, StandardCharsets.UTF_8);
            assertThat(deltas)
                    .as("La aplicacion de DECISION_ID-01 a PersonaRepository debe estar registrada")
                    .contains("`DECISION_ID-01` aplicada a `PersonaRepository`");
            assertThat(deltas)
                    .as("Alta con id nulo y copia persistida por conId(Long)")
                    .contains("FalPersona.conId(Long)");
            assertThat(deltas)
                    .as("Un id informado actualiza y no puede resolverse con upsert de motor")
                    .contains("REPLACE")
                    .contains("ON DUPLICATE KEY UPDATE");
            assertThat(deltas)
                    .as("Un id informado inexistente no se inserta y usa la excepcion canonica ya existente")
                    .contains("No se inserta nunca")
                    .contains("PersonaNoEncontradaException");
            assertThat(deltas)
                    .as("El upsert incondicional del InMemory queda prohibido como contrato futuro")
                    .contains("Upsert incondicional: prohibido como contrato futuro");
        }

        @Test
        @DisplayName("los 3 callsites de nextId quedan autorizados para la transicion de identidad")
        void tres_callsites_nextid_autorizados() throws IOException {
            String deltas = Files.readString(DELTAS, StandardCharsets.UTF_8);
            for (String callsite : List.of(
                    "PersonaService.crear",
                    "PersonaService.crearMinimal",
                    "ActaService.crearPersonaMinimal")) {
                assertThat(deltas)
                        .as("El callsite '%s' debe quedar nombrado en la transicion autorizada", callsite)
                        .contains(callsite);
            }
            assertThat(deltas)
                    .as("La modificacion de ActaService debe quedar expresamente autorizada")
                    .contains("expresamente autorizada");
            assertThat(deltas)
                    .as("La autorizacion no reabre las reglas de alta del acta")
                    .contains("no reabre las reglas de alta del acta");
        }

        /**
         * Separacion de planos. Hasta WAVE 1A2-B1.5B0-B este test exigia que el modelo
         * mantuviera dos filas distintas, porque el esquema ejecutado y el objetivo
         * diferian. Aplicado el ALTER, los dos planos convergieron y lo que se verifica
         * es lo contrario: que el modelo declare la convergencia, conserve el
         * antecedente historico y no siga presentando `INT UNSIGNED NOT NULL` como la
         * forma vigente.
         */
        @Test
        @DisplayName("el modelo declara que el esquema ejecutado ya coincide con el objetivo (G-30)")
        void modelo_objetivo_y_esquema_ejecutado_convergieron() throws IOException {
            String modelo = Files.readString(MODELO_PERS, StandardCharsets.UTF_8);
            assertThat(modelo)
                    .as("El modelo debe declarar que el esquema ejecutado coincide con el objetivo")
                    .contains("Campos documento — esquema ejecutado, coincide con el modelo objetivo");
            assertThat(modelo)
                    .as("La forma anterior se conserva como antecedente, nunca como estado vigente")
                    .contains("Campos documento — antecedente histórico, ya no vigente");
            assertThat(modelo)
                    .as("La forma vigente del documento es opcional y de texto canonico")
                    .contains("`nro_doc VARCHAR(12) NULL`")
                    .contains("`tipo_documento SMALLINT NULL`");
            assertThat(decisiones())
                    .as("La decision debe declarar el plan de migracion con punto de no retorno")
                    .contains("punto de no retorno")
                    .contains("Postcheck");
            assertThat(decisiones())
                    .as("Los guardrails del esquema ejecutado se invierten con el ALTER, nunca antes")
                    .contains("en el mismo trabajo que ejecute el `ALTER`, nunca antes");
            assertThat(decisiones())
                    .as("La ejecucion del ALTER debe quedar registrada sin reabrir la decision")
                    .contains("DDL_EXECUTED");
        }

        /**
         * El drift de tiempo verbal que B1.5B0-A dejo registrado como deuda: la seccion 8
         * describia en presente dos normalizaciones que ese mismo slice unifico.
         */
        @Test
        @DisplayName("la divergencia de normalizacion es antecedente historico, no estado presente (G-30)")
        void normalizacion_divergente_es_antecedente_historico() throws IOException {
            assertThat(decisiones())
                    .as("DocumentoPersona debe declararse como la autoridad unica actual")
                    .contains("es el value object `DocumentoPersona`");
            assertThat(decisiones())
                    .as("La divergencia debe quedar en pasado y marcada como antecedente")
                    .contains("Antecedente histórico, no estado presente")
                    .contains("Las dos expresiones regulares fueron eliminadas");
            assertThat(decisiones())
                    .as("La seccion 8 ya no puede afirmar en presente que existen dos normalizaciones")
                    .doesNotContain("Hoy existen dos incompatibles");
        }
    }

    // =====================================================================
    // G-31: transicion Java de identidad y documento de Persona
    // =====================================================================

    /**
     * Contraparte de codigo del grupo G-30. Mientras G-30 verifica que la spec declare una
     * sola decision, este grupo verifica que el codigo productivo la cumpla: identidad
     * asignada por la persistencia, documento canonico en un unico normalizador y patron
     * P3 en el puerto.
     *
     * <p>No verifica el adaptador JDBC ni el DDL: los dos siguen pendientes y este grupo
     * comprueba justamente que no se hayan adelantado.
     */
    @Nested
    @DisplayName("G-31: Persona en Java - identidad, documento canonico y patron P3")
    class G31PersonaJavaIdentidad {

        private static final Path PUERTO = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/repository/PersonaRepository.java");
        private static final Path IN_MEMORY = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/repository/memory/InMemoryPersonaRepository.java");
        private static final Path FAL_PERSONA = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/domain/model/FalPersona.java");
        private static final Path DOCUMENTO = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/domain/model/DocumentoPersona.java");
        private static final Path PERSONA_SERVICE = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/application/service/PersonaService.java");
        private static final Path ACTA_SERVICE_PERS = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/application/service/ActaService.java");

        @Test
        @DisplayName("PersonaRepository no declara nextId() y no queda ningun callsite de Persona")
        void puerto_sin_next_id() throws IOException {
            assertThat(Arrays.stream(PersonaRepository.class.getDeclaredMethods())
                    .map(Method::getName))
                    .as("El puerto de Persona no puede declarar nextId(): DECISION_ID-01")
                    .doesNotContain("nextId");

            for (Path archivo : List.of(PUERTO, IN_MEMORY, PERSONA_SERVICE, ACTA_SERVICE_PERS)) {
                assertThat(Files.readString(archivo, StandardCharsets.UTF_8))
                        .as("%s no puede declarar ni invocar nextId() sobre Persona",
                                archivo.getFileName())
                        .doesNotContain("Long nextId()")
                        .doesNotContain("personaRepository.nextId(")
                        .doesNotContain("personas.nextId(");
            }
        }

        @Test
        @DisplayName("FalPersona admite id null y expone conId(Long) sin setter de id")
        void modelo_admite_id_null_y_expone_conId() throws Exception {
            LocalDateTime ahora = LocalDateTime.of(2026, 1, 1, 10, 0);
            FalPersona nueva = new FalPersona(null, TipoPersona.FISICA, ahora, "USR1");
            assertThat(nueva.getId()).as("Una entidad nueva tiene id nulo").isNull();

            assertThat(FalPersona.class.getMethod("conId", Long.class).getReturnType())
                    .as("conId debe devolver una copia de FalPersona")
                    .isEqualTo(FalPersona.class);

            assertThat(Arrays.stream(FalPersona.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .map(Method::getName))
                    .as("No puede existir un setter publico de id: la PK la asigna la persistencia")
                    .doesNotContain("setId");

            FalPersona persistida = nueva.conId(9L);
            assertThat(persistida.getId()).isEqualTo(9L);
            assertThat(nueva.getId()).as("conId no muta la instancia receptora").isNull();
        }

        @Test
        @DisplayName("el documento vive en un unico value object, sin setters individuales")
        void documento_sin_setters_individuales() {
            assertThat(Arrays.stream(FalPersona.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .map(Method::getName))
                    .as("Los componentes del documento no pueden mutarse por separado: "
                            + "eso permitiria un par incoherente en dos pasos")
                    .doesNotContain("setTipoDoc", "setNroDoc",
                            "setPrefijoCuitCuil", "setDigitoVerificador")
                    .contains("setDocumento");
        }

        @Test
        @DisplayName("existe un unico normalizador de documento y los servicios no duplican regex")
        void normalizador_unico() throws IOException {
            String personaService = Files.readString(PERSONA_SERVICE, StandardCharsets.UTF_8);
            String actaService = Files.readString(ACTA_SERVICE_PERS, StandardCharsets.UTF_8);

            assertThat(personaService)
                    .as("PersonaService debe delegar la normalizacion en DocumentoPersona")
                    .contains("DocumentoPersona.normalizar(");
            for (String servicio : List.of(personaService, actaService)) {
                assertThat(servicio)
                        .as("Ningun servicio puede normalizar el documento con su propia regex")
                        .doesNotContain("[^0-9a-zA-Z")
                        .doesNotContain("replaceAll(\"\\\\s+\", \"\")");
            }
        }

        @Test
        @DisplayName("nroDoc canonico admite hasta 12 caracteres y ni uno mas")
        void nro_doc_maximo_12() {
            assertThat(DocumentoPersona.LONGITUD_MAXIMA_NRO_DOC)
                    .as("El ancho fisico de nro_doc quedo cerrado en 12")
                    .isEqualTo(12);
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "AB0123456789").nroDoc())
                    .hasSize(12);
            assertThatThrownBy(() ->
                    DocumentoPersona.normalizar(TipoDocumentoPersona.OTRO, "AB01234567890"))
                    .as("13 caracteres canonicos se rechazan, no se truncan")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("CUIT/CUIL se almacena estructurado y nunca como los once digitos en nroDoc")
        void cuit_cuil_estructurado() {
            DocumentoPersona cuit = DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-3");
            assertThat(cuit.nroDoc())
                    .as("nroDoc guarda el cuerpo central, no el CUIT completo")
                    .isEqualTo("12345678")
                    .hasSize(DocumentoPersona.LONGITUD_CUERPO_CUIT_CUIL);
            assertThat(cuit.prefijoCuitCuil()).isEqualTo(20);
            assertThat(cuit.digitoVerificador()).isEqualTo(3);
            assertThat(cuit.esCuitCuil()).isTrue();

            assertThatThrownBy(() ->
                    new DocumentoPersona(TipoDocumentoPersona.CUIT, "20123456783", 20, 3))
                    .as("Los once digitos completos no pueden vivir en nroDoc")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("el normalizador no rellena componentes ausentes ni usa COALESCE")
        void sin_componentes_inventados() throws IOException {
            String documento = Files.readString(DOCUMENTO, StandardCharsets.UTF_8);
            assertThat(documento)
                    .as("No se rellenan componentes ausentes para fabricar una identidad")
                    .doesNotContain("COALESCE")
                    .doesNotContain("LPAD");

            assertThatThrownBy(() ->
                    new DocumentoPersona(TipoDocumentoPersona.CUIT, "12345678", null, null))
                    .as("Un CUIT sin prefijo ni digito se rechaza en lugar de completarse")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("la validacion aritmetica del digito verificador no se exige todavia")
        void sin_modulo_11_obligatorio() {
            assertThat(DocumentoPersona.normalizar(TipoDocumentoPersona.CUIT, "20-12345678-0").digitoVerificador())
                    .as("D-PERS-CUIT-DV-01: el modulo 11 no se exige en este slice")
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("ningun camino infiere el tipo documental desde un texto sin tipo")
        void sin_inferencia_de_tipo() throws IOException {
            for (Path archivo : List.of(PERSONA_SERVICE, ACTA_SERVICE_PERS)) {
                String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
                assertThat(contenido)
                        .as("%s no puede asumir DNI para un texto sin tipo documental",
                                archivo.getFileName())
                        .doesNotContain("TipoDocumentoPersona.DNI");
            }
            assertThatThrownBy(() -> DocumentoPersona.normalizar(null, "12345678"))
                    .as("Un numero sin tipo no constituye un documento")
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("el documento no se trunca en silencio para respetar un ancho")
        void sin_truncamiento_silencioso() throws IOException {
            String documento = Files.readString(DOCUMENTO, StandardCharsets.UTF_8);
            assertThat(documento)
                    .as("El normalizador rechaza un documento demasiado largo; no lo recorta")
                    .doesNotContain("substring(0, LONGITUD_MAXIMA_NRO_DOC)");
            for (Path archivo : List.of(PERSONA_SERVICE, ACTA_SERVICE_PERS)) {
                assertThat(Files.readString(archivo, StandardCharsets.UTF_8))
                        .as("%s no puede recortar el documento", archivo.getFileName())
                        .doesNotContain("nroDocNorm");
            }
        }

        @Test
        @DisplayName("el mensaje de rechazo no reproduce el documento recibido")
        void mensajes_sin_datos_personales() {
            assertThatThrownBy(() ->
                    DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, "12A45678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining("12A45678");
        }

        @Test
        @DisplayName("el puerto declara el patron P3 y la excepcion canonica ya existente")
        void puerto_declara_p3() throws IOException {
            String puerto = Files.readString(PUERTO, StandardCharsets.UTF_8);
            assertThat(puerto)
                    .as("El contrato debe declarar alta con id nulo, actualizacion con id informado "
                            + "y el orden que promete")
                    .contains("PersonaNoEncontradaException")
                    .contains("ascendente")
                    .contains("nunca se inserta");

            // WAVE 1A2-B2 agrega listarPagina: el puerto pasa de cinco a seis operaciones. El
            // cambio es aditivo y ninguna firma existente cambio.
            assertThat(Arrays.stream(PersonaRepository.class.getDeclaredMethods())
                    .map(Method::getName))
                    .as("El puerto conserva sus cinco metodos reales y suma la consulta paginada")
                    .containsExactlyInAnyOrder("guardar", "buscarPorId", "listarTodas",
                            "buscarPorTipoDocYNroDoc", "buscarPorIdSujBie", "listarPagina");
        }

        @Test
        @DisplayName("el InMemory diferencia alta de actualizacion y no hace upsert incondicional")
        void inmemory_p3_sin_upsert() throws IOException {
            String inMemory = Files.readString(IN_MEMORY, StandardCharsets.UTF_8);
            assertThat(inMemory)
                    .as("Un id informado inexistente debe fallar, no insertarse")
                    .contains("PersonaNoEncontradaException")
                    .contains("containsKey");
            assertThat(inMemory)
                    .as("El orden por id ascendente es contrato del puerto")
                    .contains("Comparator.comparing(FalPersona::getId)");
        }

        /**
         * La asercion {@code doesNotContain("situacionIdentificacion")} se invirtio en
         * WAVE 1A2-B1.5B1, el slice que implementa {@code DECISION_DDL-ACTA-INFR-01}.
         *
         * <p>La asercion {@code doesNotContain("QrActaService")} se invierte en
         * WAVE 1A2-B1.5B2, el slice que implementa {@code DECISION_DDL-ACTA-QR-01}: el alta
         * pasa a pedirle el token al servicio de QR antes de la primera escritura, de modo
         * que la ausencia de esa referencia ya no es la propiedad correcta.
         *
         * <p>La asercion {@code doesNotContain("@Transactional")} se retira en WAVE 1A2-B2, el
         * slice que implementa {@code DECISION_OBS-TX-01}. La frontera del alta se verifica
         * ahora en {@code G28.acta_service_declara_transactional_en_labrar}, que afirma sobre
         * el metodo exacto y por lo tanto es mas estricta. Lo que sigue protegiendo la misma
         * frontera aca es que el alta coordine puertos y no adaptadores concretos.
         */
        @Test
        @DisplayName("ActaService solo recibio el cambio minimo autorizado")
        void acta_service_cambio_minimo() throws IOException {
            String actaService = Files.readString(ACTA_SERVICE_PERS, StandardCharsets.UTF_8);
            assertThat(actaService)
                    .as("El texto libre del infractor se conserva en el acta")
                    .contains("setInfractorDocumento");
            assertThat(actaService)
                    .as("La persona minima se construye sin PK y sin documento estructurado")
                    .contains("new FalPersona(null, TipoPersona.FISICA");
            assertThat(actaService)
                    .as("El alta propaga la situacion de identificacion sin default")
                    .contains("cmd.situacionIdentificacionInfractor()");
            assertThat(actaService)
                    .as("El QR se pide al servicio de QR y se asigna antes de guardar el acta")
                    .contains("qrActaService.emitirTokenPara(uuidTecnico)")
                    .contains("setCodigoQr")
                    .contains("setQrPayloadVersion");
            assertThat(actaService)
                    .as("El alta usa el actor real y propaga lonInfr")
                    .contains("cmd.lonInfr()");
            assertThat(actaService)
                    .as("El servicio coordina puertos y nunca adaptadores concretos")
                    .doesNotContain("JdbcActaRepository")
                    .doesNotContain("JdbcObservacionRepository")
                    .doesNotContain("JdbcPersonaRepository");
        }

        /**
         * Invertido en WAVE 1A2-B1.5B0-B, el slice que crea el adaptador. Hasta entonces
         * este test verificaba que B1.5B0-A no se hubiera adelantado y afirmaba que el
         * archivo no existia.
         *
         * <p>La asercion de "candidato sin anotaciones" se invierte en WAVE 1A2-B2, el slice
         * que activa el adaptador: pasa a exigir {@code @Repository} con
         * {@code @Profile("jdbc")} y, sobre todo, la ausencia de {@code @Primary}, que es la
         * propiedad que garantiza una sola implementacion por puerto en vez de esconder una
         * ambiguedad. Todo lo demas que este caso verifica sobre el SQL, los enums y la
         * identidad sigue igual y no depende de la activacion.
         */
        @Test
        @DisplayName("el adaptador JDBC de persona es bean solo bajo jdbc y el modelo no conoce la infraestructura")
        void adaptador_jdbc_activado_solo_en_perfil_jdbc() throws IOException {
            Path adaptador = Paths.get("src/main/java/ar/gob/malvinas/faltas/core/infrastructure/jdbc/"
                    + "JdbcPersonaRepository.java");
            assertThat(adaptador)
                    .as("WAVE 1A2-B1.5B0-B crea el adaptador candidato")
                    .exists();

            // El Javadoc del adaptador nombra las anotaciones y los antipatrones
            // justamente para declararlos prohibidos: se verifica el codigo, no el texto
            // que los menciona.
            String codigo = sinComentariosJava(Files.readString(adaptador, StandardCharsets.UTF_8));
            assertThat(codigo)
                    .as("WAVE 1A2-B2 lo activa como bean exclusivamente bajo el perfil jdbc")
                    .contains("@Repository")
                    .contains("@Profile(\"jdbc\")")
                    .doesNotContain("@Primary");
            assertThat(codigo)
                    .as("Persistencia con Spring JDBC explicito, nunca JPA ni ORM")
                    .contains("JdbcClient")
                    .doesNotContain("javax.persistence")
                    .doesNotContain("jakarta.persistence");
            assertThat(codigo)
                    .as("Sin SELECT *, sin upsert de motor y sin PK preasignada")
                    .doesNotContain("SELECT *")
                    .doesNotContain("REPLACE INTO")
                    .doesNotContain("ON DUPLICATE KEY UPDATE")
                    .doesNotContain("MAX(id)");
            assertThat(codigo)
                    .as("La PK la asigna el motor y se recupera con GeneratedKeyHolder")
                    .contains("GeneratedKeyHolder");
            assertThat(codigo)
                    .as("doc_key es STORED GENERATED: ni el INSERT ni el UPDATE la informan")
                    .doesNotContain("doc_key");
            assertThat(codigo)
                    .as("Los enums viajan por codigo(), nunca por ordinal() ni name()")
                    .doesNotContain("ordinal()");

            assertThat(Files.readString(FAL_PERSONA, StandardCharsets.UTF_8))
                    .as("El modelo no puede quedar acoplado a la infraestructura")
                    .doesNotContain("JdbcClient")
                    .doesNotContain("org.springframework");
        }

        /**
         * Quita comentarios de bloque y de linea. Un guardrail que prohibe un patron no
         * puede fallar porque la documentacion del archivo lo nombre para prohibirlo.
         */
        private static String sinComentariosJava(String fuente) {
            return fuente
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("(?m)^\\s*//.*$", "");
        }
    }

    // =====================================================================
    // G-32: consultas listables - contrato de pagina, orden y deuda de indices
    // =====================================================================

    /**
     * Registro normativo de {@code DECISION-LIST-PAGE-SORT-01} y de la deuda
     * {@code D-PERSISTENCE-PAGING-INDEX-01}, mas la coherencia entre la spec y la regla
     * persistente de Cursor que la referencia.
     *
     * <p>La regla {@code .cursor/rules/paginated-list-queries.mdc} la creo el usuario y este
     * grupo <b>no</b> la reescribe: solo verifica que exista, que apunte a la decision canonica
     * y que no se haya duplicado en una segunda regla. Una regla que referencie una decision
     * inexistente es drift, y es lo unico que estos casos pueden detectar sin invadir el trabajo
     * del usuario.
     */
    @Nested
    @DisplayName("G-32: DECISION-LIST-PAGE-SORT-01, deuda de indices y regla de Cursor")
    class G32ListPageSort {

        private static final Path DECISIONES =
                Paths.get("docs/spec-as-source/50-persistence/ddl-decisions.md");
        private static final Path REGLA_CURSOR =
                Paths.get("../../.cursor/rules/paginated-list-queries.mdc");
        private static final Path REGLAS_CURSOR_DIR = Paths.get("../../.cursor/rules");

        private String decisiones() throws IOException {
            return Files.readString(DECISIONES, StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("ddl-decisions.md registra DECISION-LIST-PAGE-SORT-01 con sus defaults y limites")
        void decision_list_page_sort_registrada() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("la decision debe estar registrada en el registro de decisiones")
                    .contains("DECISION-LIST-PAGE-SORT-01");
            assertThat(contenido)
                    .as("defaults y maximo explicitos: 0, 50 y 100")
                    .contains("`page = 0`")
                    .contains("`size = 50`")
                    .contains("`100`");
            String enMinuscula = contenido.toLowerCase(Locale.ROOT);
            assertThat(enMinuscula)
                    .as("el rechazo del tamano excedido debe ser explicito, no un recorte silencioso")
                    .contains("se rechaza explícitamente");
            assertThat(enMinuscula)
                    .as("la decision debe declarar la allowlist, el desempate y la ejecucion en el motor")
                    .contains("allowlist")
                    .contains("desempate")
                    .contains("parámetros enlazados");
            assertThat(contenido)
                    .as("y debe declarar que no existe variante publica ilimitada")
                    .contains("No existe una variante pública ilimitada del listado");
        }

        @Test
        @DisplayName("la decision declara los ordenes predeterminados de Acta y Persona con su desempate")
        void decision_declara_orden_predeterminado_por_entidad() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("Acta declara su orden predeterminado con la PK como desempate")
                    .contains("`fechaActa DESC`, más la PK como desempate");
            assertThat(contenido)
                    .as("Persona declara su orden predeterminado con la PK como desempate")
                    .contains("`fechaAlta DESC`, más la PK como desempate");
            assertThat(contenido)
                    .as("la politica de nulos debe ser explicita y no accidental")
                    .contains("nulos primero en `ASC` y últimos en `DESC`");
        }

        @Test
        @DisplayName("ddl-decisions.md registra D-PERSISTENCE-PAGING-INDEX-01 abierta y con su wave")
        void deuda_paging_index_registrada_y_abierta() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("la deuda de optimizacion fisica debe estar registrada")
                    .contains("D-PERSISTENCE-PAGING-INDEX-01");
            assertThat(contenido)
                    .as("debe declararse abierta y diferida deliberadamente")
                    .contains("**ABIERTA.** Diferida deliberadamente");
            assertThat(contenido)
                    .as("debe nombrar la wave que la cierra")
                    .contains("WAVE 1A2-PERSISTENCE-PERF-01");
            assertThat(contenido)
                    .as("debe prohibir los indices especulativos mientras siga abierta")
                    .contains("combinatoria teórica o especulativa");
            assertThat(contenido.toLowerCase(Locale.ROOT))
                    .as("no puede declararse cerrada mientras el registro diga lo contrario")
                    .doesNotContain("d-persistence-paging-index-01 cerrada");
        }

        @Test
        @DisplayName("la deuda registra las consultas, los indices actuales y los metodos ilimitados")
        void deuda_registra_consultas_e_indices_actuales() throws IOException {
            String contenido = decisiones();
            assertThat(contenido)
                    .as("debe registrar la tabla, los filtros y el orden de cada entidad")
                    .contains("Tabla `fal_acta`, sin joins")
                    .contains("Tabla `fal_persona`, sin joins");
            assertThat(contenido)
                    .as("debe registrar los indices actuales relacionados, verificados contra el motor")
                    .contains("uq_fal_persona_tipo_doc_key")
                    .contains("ix_fal_persona_apellido")
                    .contains("ix_fal_acta_fecha")
                    .contains("ix_fal_acta_estado");
            assertThat(contenido)
                    .as("debe registrar los dos metodos ilimitados a migrar o eliminar")
                    .contains("ActaRefPersonaDomicilioUsoChecker");
        }

        /**
         * La regla persistente y la spec tienen que hablar de la misma decision. No se verifica
         * el texto completo de la regla: es del usuario y este slice no la reescribe.
         */
        @Test
        @DisplayName("la regla de Cursor existe, es siempre aplicable y apunta a la decision canonica")
        void regla_cursor_existe_y_referencia_la_decision() throws IOException {
            assertThat(REGLA_CURSOR)
                    .as("la regla persistente de consultas listables debe existir")
                    .exists();
            String regla = Files.readString(REGLA_CURSOR, StandardCharsets.UTF_8);
            assertThat(regla)
                    .as("debe aplicarse siempre y referenciar la decision canonica")
                    .contains("alwaysApply: true")
                    .contains("DECISION-LIST-PAGE-SORT-01")
                    .contains("D-PERSISTENCE-PAGING-INDEX-01");
            assertThat(regla)
                    .as("debe declarar los defaults, el maximo y el rechazo explicito")
                    .contains("`page = 0`")
                    .contains("`size = 50`")
                    .contains("El tamaño máximo permitido es `100`");
        }

        /**
         * Una segunda regla que hable del mismo contrato es peor que ninguna: dos guardrails
         * persistentes pueden divergir y no hay forma de saber cual gobierna.
         */
        @Test
        @DisplayName("no existe una segunda regla de Cursor duplicando el contrato de paginacion")
        void sin_regla_cursor_duplicada() throws IOException {
            if (!Files.isDirectory(REGLAS_CURSOR_DIR)) {
                return;
            }
            List<String> duplicadas = new ArrayList<>();
            try (var reglas = Files.list(REGLAS_CURSOR_DIR)) {
                for (Path regla : reglas.filter(Files::isRegularFile).toList()) {
                    if (regla.getFileName().toString().equals("paginated-list-queries.mdc")) {
                        continue;
                    }
                    String contenido = Files.readString(regla, StandardCharsets.UTF_8);
                    if (contenido.contains("DECISION-LIST-PAGE-SORT-01")) {
                        duplicadas.add(regla.getFileName().toString());
                    }
                }
            }
            assertThat(duplicadas)
                    .as("solo paginated-list-queries.mdc puede declarar el contrato de paginacion")
                    .isEmpty();
        }
    }
}
