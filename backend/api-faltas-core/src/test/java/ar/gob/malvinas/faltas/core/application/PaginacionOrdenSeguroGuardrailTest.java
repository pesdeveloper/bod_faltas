package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.DireccionOrden;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails de seguridad del {@code ORDER BY} y del {@code WHERE} de las consultas paginadas
 * ({@code DECISION-LIST-PAGE-SORT-01}), sobre <b>codigo ejecutable</b>.
 *
 * <p>Los escaneos excluyen comentarios y Javadoc a proposito: la documentacion nombra los
 * antipatrones justamente para declararlos prohibidos, de modo que buscar sobre el texto completo
 * produciria falsos positivos.
 *
 * <p>Lo que estos casos protegen no lo puede proteger un test de comportamiento. Un adaptador
 * puede devolver la pagina correcta y, al mismo tiempo, estar construyendo su {@code ORDER BY}
 * concatenando una cadena recibida: el resultado seria indistinguible hasta que alguien envie una
 * cadena maliciosa. La propiedad que hace imposible ese caso es estructural —que el unico origen
 * de una expresion SQL sea una constante interna del adaptador— y se verifica sobre el codigo.
 */
@DisplayName("DECISION-LIST-PAGE-SORT-01: seguridad de ORDER BY, WHERE y allowlists")
class PaginacionOrdenSeguroGuardrailTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Path RAIZ = Paths.get("ar/gob/malvinas/faltas/core");

    private static final Path JDBC_ACTA = MAIN.resolve(RAIZ.resolve(
            "infrastructure/jdbc/JdbcActaRepository.java"));
    private static final Path JDBC_PERSONA = MAIN.resolve(RAIZ.resolve(
            "infrastructure/jdbc/JdbcPersonaRepository.java"));
    private static final Path CAMPO_ACTA = MAIN.resolve(RAIZ.resolve(
            "application/query/CampoOrdenActa.java"));
    private static final Path CAMPO_PERSONA = MAIN.resolve(RAIZ.resolve(
            "application/query/CampoOrdenPersona.java"));
    private static final Path PAGINA_SOLICITADA = MAIN.resolve(RAIZ.resolve(
            "application/query/PaginaSolicitada.java"));
    private static final Path WEB = MAIN.resolve(RAIZ.resolve("web"));

    private static final List<Path> ADAPTADORES_PAGINADOS = List.of(JDBC_ACTA, JDBC_PERSONA);

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

    /** Cuerpo de un metodo, delimitado contando llaves desde la firma. */
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
    @Nested
    @DisplayName("La expresion de ORDER BY sale de una allowlist interna, nunca del cliente")
    class OrdenSeguro {

        /**
         * El {@code switch} exhaustivo sin {@code default} es la propiedad estructural clave: si
         * alguien agrega una constante a la allowlist sin decidir su columna, el modulo no
         * compila. Un mapa con busqueda por clave fallaria en ejecucion y lo haria produciendo un
         * {@code ORDER BY} con la palabra {@code null}.
         */
        @Test
        @DisplayName("1. la traduccion de campo a columna es un switch exhaustivo sin default")
        void traduccionPorSwitchExhaustivo() throws IOException {
            String acta = cuerpoDeMetodo(ejecutable(JDBC_ACTA),
                    "private static String columnaDeOrden(CampoOrdenActa campo)");
            assertThat(acta)
                    .as("cada campo funcional resuelve a una columna constante")
                    .contains("switch (campo)");
            assertThat(acta)
                    .as("sin default: la exhaustividad la verifica el compilador")
                    .doesNotContain("default ->")
                    .doesNotContain("default:");

            String persona = cuerpoDeMetodo(ejecutable(JDBC_PERSONA),
                    "private static String columnaDeOrden(CampoOrdenPersona campo)");
            assertThat(persona).contains("switch (campo)");
            assertThat(persona).doesNotContain("default ->").doesNotContain("default:");
        }

        /**
         * Toda constante de la allowlist tiene que tener una rama en el {@code switch}. La
         * exhaustividad la garantiza el compilador, pero este caso lo verifica ademas por
         * reflexion para que el guardrail siga siendo cierto si alguien introdujera un
         * {@code default}.
         */
        @Test
        @DisplayName("2. cada constante de las dos allowlists tiene su rama en el switch")
        void allowlistCubiertaPorElSwitch() throws IOException {
            String acta = ejecutable(JDBC_ACTA);
            for (CampoOrdenActa campo : CampoOrdenActa.values()) {
                assertThat(acta)
                        .as("CampoOrdenActa.%s debe traducirse a una columna", campo.name())
                        .contains("case " + campo.name() + " ->");
            }
            String persona = ejecutable(JDBC_PERSONA);
            for (CampoOrdenPersona campo : CampoOrdenPersona.values()) {
                assertThat(persona)
                        .as("CampoOrdenPersona.%s debe traducirse a una columna", campo.name())
                        .contains("case " + campo.name() + " ->");
            }
        }

        /**
         * El cuerpo de {@code expresionOrden} solo puede componer con la traduccion de la
         * allowlist, el {@code sql()} del enum de direccion y la constante de desempate. Que no
         * reciba ninguna cadena es lo que hace imposible inyectar por el orden.
         */
        @Test
        @DisplayName("3. expresionOrden no recibe cadenas y solo compone constantes internas")
        void expresionOrdenSinCadenasRecibidas() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String codigo = ejecutable(adaptador);
                int firma = codigo.indexOf("private static String expresionOrden(");
                assertThat(firma).as("%s debe declarar expresionOrden", adaptador.getFileName())
                        .isNotNegative();

                // Solo la lista de parametros: el tipo de retorno es String a proposito.
                int abre = codigo.indexOf('(', firma);
                String parametros = codigo.substring(abre + 1, codigo.indexOf(')', abre));
                assertThat(parametros)
                        .as("expresionOrden recibe criterios tipados, nunca una cadena")
                        .contains("List<CriterioOrden<")
                        .doesNotContain("String");

                String cuerpo = cuerpoDeMetodo(codigo, "private static String expresionOrden(");
                assertThat(cuerpo)
                        .as("la columna sale de la allowlist y la direccion del enum")
                        .contains("columnaDeOrden(criterio.campo())")
                        .contains("criterio.direccion().sql()");
                assertThat(cuerpo)
                        .as("y la constante de desempate es interna del adaptador")
                        .contains("COLUMNA_DESEMPATE");
            }
        }

        /**
         * El fragmento {@code ASC}/{@code DESC} vive en el enum, no en el adaptador ni en una
         * cadena del cliente.
         */
        @Test
        @DisplayName("4. ASC y DESC provienen del enum validado, no de texto recibido")
        void direccionDesdeEnum() {
            assertThat(DireccionOrden.ASC.sql()).isEqualTo("ASC");
            assertThat(DireccionOrden.DESC.sql()).isEqualTo("DESC");
            assertThat(DireccionOrden.values()).hasSize(2);
        }

        /**
         * El desempate por PK no es opcional: se agrega siempre, tanto al orden predeterminado
         * como a cualquier orden solicitado, y ninguna allowlist expone la PK como campo pedible.
         */
        @Test
        @DisplayName("5. la PK se agrega siempre como desempate y no es un campo pedible")
        void desempatePorPkSiempre() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String cuerpo = cuerpoDeMetodo(ejecutable(adaptador),
                        "private static String expresionOrden(");
                assertThat(cuerpo)
                        .as("%s agrega el desempate fuera del bucle de criterios",
                                adaptador.getFileName())
                        .contains("sb.append(COLUMNA_DESEMPATE)");
                assertThat(ejecutable(adaptador))
                        .as("%s usa la PK como columna de desempate", adaptador.getFileName())
                        .contains("COLUMNA_DESEMPATE = \"id\"");
            }
            assertThat(CampoOrdenActa.values())
                    .extracting(CampoOrdenActa::nombreFuncional)
                    .as("la PK no se expone como campo funcional ordenable")
                    .doesNotContain("id");
            assertThat(CampoOrdenPersona.values())
                    .extracting(CampoOrdenPersona::nombreFuncional)
                    .doesNotContain("id");
        }

        @Test
        @DisplayName("6. las allowlists no contienen texto SQL ni nombres fisicos de columna")
        void allowlistSinTextoSql() throws IOException {
            for (Path allowlist : List.of(CAMPO_ACTA, CAMPO_PERSONA)) {
                String codigo = ejecutable(allowlist);
                assertThat(codigo)
                        .as("%s nombra conceptos funcionales, no columnas", allowlist.getFileName())
                        .doesNotContain("ORDER BY")
                        .doesNotContain("SELECT")
                        .doesNotContain("est_proc_act")
                        .doesNotContain("nombre_mostrar")
                        .doesNotContain("fh_alta");
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Los valores del filtro viajan enlazados, nunca concatenados")
    class FiltroSeguro {

        /**
         * El {@code WHERE} se compone con fragmentos que son constantes del adaptador. Lo que este
         * caso verifica es que el bucle de composicion tome el fragmento de la constante y el
         * valor lo entregue por {@code param(...)}, que son dos caminos separados.
         */
        @Test
        @DisplayName("7. el WHERE se compone con fragmentos constantes y los valores van por param")
        void whereConFragmentosConstantes() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String codigo = ejecutable(adaptador);

                String clausula = cuerpoDeMetodo(codigo,
                        "private static String clausulaWhere(List<PredicadoFiltro> activos)");
                assertThat(clausula)
                        .as("%s compone solo con el fragmento del predicado", adaptador.getFileName())
                        .contains("activos.get(i).sql()");

                String vincular = cuerpoDeMetodo(codigo,
                        "private static JdbcClient.StatementSpec vincularFiltro(");
                assertThat(vincular)
                        .as("%s entrega cada valor como parametro enlazado", adaptador.getFileName())
                        .contains("resultado.param(")
                        .contains("predicado.parametro()")
                        .contains("predicado.tipo()");
            }
        }

        /**
         * El limite y el desplazamiento tambien viajan enlazados. Concatenarlos seria inofensivo
         * en apariencia, porque son enteros ya validados, pero abriria el precedente de armar la
         * sentencia con valores en vez de con parametros.
         */
        @Test
        @DisplayName("8. LIMIT y OFFSET viajan enlazados y no interpolados")
        void limiteYDesplazamientoEnlazados() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String codigo = ejecutable(adaptador);
                assertThat(codigo)
                        .as("%s usa parametros nombrados para el recorte", adaptador.getFileName())
                        .contains("LIMIT :paginaLimite OFFSET :paginaDesplazamiento")
                        .contains(".param(\"paginaLimite\"")
                        .contains(".param(\"paginaDesplazamiento\"");
                assertThat(codigo)
                        .as("%s no interpola el recorte en el texto", adaptador.getFileName())
                        .doesNotContain("LIMIT \" +")
                        .doesNotContain("OFFSET \" +");
            }
        }

        @Test
        @DisplayName("9. los adaptadores paginados no usan SELECT *, upsert de motor ni SQL dinamico de valores")
        void sinSqlProhibido() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String codigo = ejecutable(adaptador);
                assertThat(codigo)
                        .as("%s sin SQL prohibido", adaptador.getFileName())
                        .doesNotContain("SELECT *")
                        .doesNotContain("ON DUPLICATE KEY")
                        .doesNotContain("REPLACE INTO")
                        .doesNotContain("Statement.RETURN_GENERATED_KEYS");
            }
        }

        /**
         * Ninguna concatenacion del SQL de la pagina puede tener como operando una expresion
         * derivada del filtro o del orden recibidos. Se verifica sobre el cuerpo de
         * {@code listarPagina}: lo unico que se concatena son constantes, la clausula compuesta de
         * fragmentos constantes y la expresion de orden compuesta de la allowlist.
         */
        @Test
        @DisplayName("10. listarPagina no concatena ningun valor del filtro al texto del SQL")
        void listarPaginaSinValoresConcatenados() throws IOException {
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                String cuerpo = cuerpoDeMetodo(ejecutable(adaptador), "public PaginaResultado<");
                assertThat(cuerpo)
                        .as("%s no interpola valores del filtro", adaptador.getFileName())
                        .doesNotContain("f.nroDoc()) +")
                        .doesNotContain("+ f.")
                        .doesNotContain("String.format")
                        .doesNotContain("\"'\"");
                assertThat(cuerpo)
                        .as("%s compone el SQL con where y orden derivados de constantes",
                                adaptador.getFileName())
                        .contains("clausulaWhere(activos)")
                        .contains("expresionOrden(p.orden())");
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Contratos del puerto y de la aplicacion")
    class ContratosDelPuerto {

        /**
         * Obligar a las dos implementaciones a escribir la consulta paginada es lo que impide que
         * un adaptador la resuelva leyendo todo y recortando en memoria sin que nadie lo note. Es
         * el mismo criterio con el que la operacion batch de observaciones no es {@code default}.
         */
        @Test
        @DisplayName("11. listarPagina no es un metodo default del puerto")
        void listarPaginaNoEsDefault() throws Exception {
            for (Class<?> puerto : List.of(ActaRepository.class, PersonaRepository.class)) {
                Method metodo = null;
                for (Method m : puerto.getDeclaredMethods()) {
                    if (m.getName().equals("listarPagina")) {
                        metodo = m;
                    }
                }
                assertThat(metodo)
                        .as("%s debe declarar listarPagina", puerto.getSimpleName())
                        .isNotNull();
                assertThat(metodo.isDefault())
                        .as("%s.listarPagina no puede ser default", puerto.getSimpleName())
                        .isFalse();
                assertThat(Modifier.isAbstract(metodo.getModifiers())).isTrue();
            }
        }

        /**
         * {@code listarTodas} sobrevive como operacion interna, pero no puede ser el camino con el
         * que un adaptador resuelva una pagina: seria exactamente el recorte en memoria que la
         * decision prohibe.
         */
        @Test
        @DisplayName("12. listarPagina no se apoya en listarTodas en ningun adaptador")
        void listarPaginaNoUsaListarTodas() throws IOException {
            List<Path> todos = new ArrayList<>(ADAPTADORES_PAGINADOS);
            todos.add(MAIN.resolve(RAIZ.resolve("repository/memory/InMemoryActaRepository.java")));
            todos.add(MAIN.resolve(RAIZ.resolve("repository/memory/InMemoryPersonaRepository.java")));
            for (Path adaptador : todos) {
                String cuerpo = cuerpoDeMetodo(ejecutable(adaptador), "public PaginaResultado<");
                assertThat(cuerpo)
                        .as("%s no puede resolver la pagina con la lectura ilimitada",
                                adaptador.getFileName())
                        .doesNotContain("listarTodas()");
            }
        }

        /**
         * El batch de observaciones tiene que quedar acotado a los ids de la pagina. Si
         * {@code listarPagina} invocara la composicion de {@code listarTodas}, el batch se pediria
         * para toda la tabla.
         */
        @Test
        @DisplayName("13. el batch de observaciones de la pagina se limita a los ids de la pagina")
        void batchLimitadoALaPagina() throws IOException {
            String codigo = ejecutable(JDBC_ACTA);
            String cuerpoPagina = cuerpoDeMetodo(codigo, "public PaginaResultado<FalActa> listarPagina(");
            assertThat(cuerpoPagina)
                    .as("la composicion recibe solo las filas materializadas de esta pagina")
                    .contains("componerBatch(mapeadas)");

            String cuerpoBatch = cuerpoDeMetodo(codigo,
                    "private List<FalActa> componerBatch(List<ActaMapeada> mapeadas)");
            assertThat(cuerpoBatch)
                    .as("una sola invocacion batch, con los ids recibidos")
                    .contains("observaciones.listarPorEntidades(EntidadTipoObservada.ACTA, ids)");
            assertThat(cuerpoBatch)
                    .as("y ninguna lectura individual por acta")
                    .doesNotContain("listarPorEntidad(");
        }

        @Test
        @DisplayName("14. el tamano maximo y los defaults viven en una sola sede")
        void constantesEnUnaSolaSede() throws IOException {
            String codigo = ejecutable(PAGINA_SOLICITADA);
            assertThat(codigo)
                    .contains("PAGE_DEFAULT = 0")
                    .contains("SIZE_DEFAULT = 50")
                    .contains("SIZE_MAX = 100");
            assertThat(PaginaSolicitada.SIZE_MAX).isEqualTo(100);

            // Ningun adaptador puede tener su propio maximo: dos limites divergentes harian que
            // la validacion dependiera del perfil activo.
            Pattern literal = Pattern.compile("\\b(100|50)\\b\\s*[;)]");
            for (Path adaptador : ADAPTADORES_PAGINADOS) {
                Matcher m = literal.matcher(cuerpoDeMetodo(ejecutable(adaptador),
                        "public PaginaResultado<"));
                assertThat(m.find())
                        .as("%s no puede declarar su propio limite de tamano",
                                adaptador.getFileName())
                        .isFalse();
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Ningun endpoint publico expone un listado ilimitado")
    class SinListadoPublicoIlimitado {

        /**
         * La decision prohibe exponer publicamente una lectura ilimitada. Hoy la propiedad se
         * cumple de la forma mas fuerte posible: ningun controller nombra {@code listarTodas}. El
         * guardrail existe para que siga siendo asi cuando la wave de exposicion HTTP agregue los
         * endpoints de bandeja.
         */
        @Test
        @DisplayName("15. ningun controller invoca listarTodas de Acta ni de Persona")
        void ningunControllerUsaListarTodas() throws IOException {
            List<String> hallazgos = new ArrayList<>();
            try (var archivos = Files.walk(WEB)) {
                for (Path controller : archivos
                        .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                        .toList()) {
                    String codigo = ejecutable(controller);
                    if (codigo.contains("actaRepository.listarTodas")
                            || codigo.contains("personaRepository.listarTodas")
                            || codigo.contains("listarActasTodas")) {
                        hallazgos.add(controller.getFileName().toString());
                    }
                }
            }
            assertThat(hallazgos)
                    .as("un endpoint no puede devolver una lectura ilimitada")
                    .isEmpty();
        }

        /**
         * Los casos de uso de consulta de coleccion tienen que devolver una pagina. Si alguno
         * devolviera {@code List}, la capa web podria exponerlo tal cual y el contrato quedaria
         * roto sin que ningun test de paginacion fallara.
         */
        @Test
        @DisplayName("16. los casos de uso de coleccion de Acta y Persona devuelven una pagina")
        void casosDeUsoDevuelvenPagina() throws Exception {
            Method listarActas = Class
                    .forName("ar.gob.malvinas.faltas.core.application.service.ActaService")
                    .getMethod("listarActas",
                            ar.gob.malvinas.faltas.core.application.query.FiltroActa.class,
                            PaginaSolicitada.class);
            assertThat(listarActas.getReturnType())
                    .isEqualTo(ar.gob.malvinas.faltas.core.application.query.PaginaResultado.class);

            Method listarPersonas = Class
                    .forName("ar.gob.malvinas.faltas.core.application.service.PersonaService")
                    .getMethod("listarPersonas",
                            ar.gob.malvinas.faltas.core.application.query.FiltroPersona.class,
                            PaginaSolicitada.class);
            assertThat(listarPersonas.getReturnType())
                    .isEqualTo(ar.gob.malvinas.faltas.core.application.query.PaginaResultado.class);
        }

        /**
         * Ningun servicio de aplicacion puede exponer la lectura ilimitada como caso de uso: eso
         * la convertiria en contrato publico por la puerta de atras.
         */
        @Test
        @DisplayName("17. ningun servicio de aplicacion expone listarTodas como caso de uso")
        void ningunServicioExponeListarTodas() throws IOException {
            for (String servicio : List.of("ActaService", "PersonaService")) {
                String codigo = ejecutable(MAIN.resolve(RAIZ.resolve(
                        "application/service/" + servicio + ".java")));
                assertThat(codigo)
                        .as("%s no puede delegar en la lectura ilimitada", servicio)
                        .doesNotContain("listarTodas()");
            }
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Sin dependencias nuevas para paginar")
    class SinDependenciasNuevas {

        @Test
        @DisplayName("18. no se agrego Spring Data ni JPA para obtener Pageable o Sort")
        void sinSpringData() throws IOException {
            String pom = Files.readString(Paths.get("pom.xml"), StandardCharsets.UTF_8);
            assertThat(pom)
                    .as("la paginacion se implementa con contratos propios")
                    .doesNotContain("spring-boot-starter-data-jpa")
                    .doesNotContain("spring-data-commons")
                    .doesNotContain("spring-boot-starter-data-jdbc");

            for (Path fuente : List.of(JDBC_ACTA, JDBC_PERSONA, PAGINA_SOLICITADA)) {
                assertThat(ejecutable(fuente))
                        .as("%s no importa tipos de Spring Data", fuente.getFileName())
                        .doesNotContain("org.springframework.data")
                        .doesNotContain("Pageable")
                        .doesNotContain("org.springframework.data.domain.Sort");
            }
        }

        @Test
        @DisplayName("19. los contratos de consulta no dependen de la infraestructura")
        void contratosSinInfraestructura() throws IOException {
            for (Path contrato : List.of(PAGINA_SOLICITADA, CAMPO_ACTA, CAMPO_PERSONA,
                    MAIN.resolve(RAIZ.resolve("application/query/PaginaResultado.java")),
                    MAIN.resolve(RAIZ.resolve("application/query/CriterioOrden.java")),
                    MAIN.resolve(RAIZ.resolve("application/query/DireccionOrden.java")),
                    MAIN.resolve(RAIZ.resolve("application/query/FiltroActa.java")),
                    MAIN.resolve(RAIZ.resolve("application/query/FiltroPersona.java")))) {
                assertThat(ejecutable(contrato))
                        .as("%s no puede conocer JDBC", contrato.getFileName())
                        .doesNotContain("JdbcClient")
                        .doesNotContain("java.sql")
                        .doesNotContain("javax.sql");
            }
        }

        @Test
        @DisplayName("20. el criterio de orden es un tipo, no un mapa de cadenas")
        void criterioTipadoNoMapa() throws Exception {
            Method campo = CriterioOrden.class.getMethod("campo");
            assertThat(campo.getReturnType())
                    .as("el campo es un enum de la allowlist, nunca un String")
                    .isNotEqualTo(String.class);
            Method direccion = CriterioOrden.class.getMethod("direccion");
            assertThat(direccion.getReturnType()).isEqualTo(DireccionOrden.class);

            Method orden = PaginaSolicitada.class.getMethod("orden");
            assertThat(orden.getReturnType())
                    .as("el orden es una lista ordenada, no un Map que no garantiza precedencia")
                    .isEqualTo(List.class);
        }
    }
}
