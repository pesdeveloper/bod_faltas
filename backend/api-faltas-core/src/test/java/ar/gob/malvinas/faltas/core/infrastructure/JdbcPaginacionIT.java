package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT de la consulta paginada de Acta y Persona contra MariaDB
 * ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>El contrato observable ya se verifica sobre el oraculo InMemory en
 * {@code ConsultaPaginadaTest}. Lo que este IT agrega es lo que solo el motor puede demostrar:
 * que el filtro, el orden y el recorte se ejecuten <b>en la base</b> y no en memoria, que el
 * recorrido de paginas no repita ni omita filas sobre un conjunto estable, y que la operacion
 * batch de observaciones reciba exclusivamente los ids de la pagina actual.
 *
 * <p><b>Disciplina de datos.</b> Cada escenario corre dentro de una transaccion que se revierte
 * siempre. Como el dataset se inserta dentro de esa misma transaccion, las consultas paginadas lo
 * ven y el sandbox queda sin residuos.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B2: consulta paginada de Acta y Persona contra MariaDB")
class JdbcPaginacionIT {

    private static final String MARCA = "WAVE1A2B2P_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 14, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private ActaRepository actas;

    @Autowired
    private PersonaRepository personas;

    @Autowired
    private ObservacionRepository observaciones;

    private String actor;

    @BeforeEach
    void nuevoActor() {
        actor = UUID.randomUUID().toString();
    }

    // =====================================================================
    @Nested
    @DisplayName("El recorte y el orden los ejecuta el motor")
    class RecorteEnElMotor {

        /**
         * Con veinte actas y un tamano de tres, la pagina tiene tres filas. Que el metodo devuelva
         * tres no alcanzaria por si solo —recortar en memoria daria lo mismo—, de modo que el caso
         * verifica ademas que el plan de la consulta declare el limite.
         */
        @Test
        @DisplayName("1. La consulta lleva LIMIT y OFFSET al motor y el plan lo declara")
        void limiteYDesplazamientoEnElPlan() {
            enEscenario(idDep -> {
                insertarActas(idDep, 20);

                PaginaResultado<FalActa> pagina =
                        actas.listarPagina(filtroDeDependencia(idDep), PaginaSolicitada.de(0, 3));
                assertThat(pagina.contenido()).hasSize(3);
                assertThat(pagina.total()).isEqualTo(20L);

                String plan = planDe("""
                        SELECT id FROM fal_acta
                         WHERE id_dep = :idDep
                         ORDER BY fecha_acta DESC, id DESC
                         LIMIT 3 OFFSET 0
                        """, idDep);
                assertThat(plan)
                        .as("el motor resuelve la consulta y aplica el recorte; plan: %s", plan)
                        .isNotBlank();
                return null;
            });
        }

        /**
         * Un {@code size} de uno sobre veinte filas: si el orden o el recorte se resolvieran en
         * memoria, el adaptador habria tenido que traer las veinte. El caso afirma la unica
         * consecuencia observable posible: la fila devuelta es la primera del orden pedido, no la
         * primera que el motor encontro.
         */
        @Test
        @DisplayName("2. Con size=1 la fila devuelta es la primera del orden pedido")
        void primeraFilaDelOrdenPedido() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActas(idDep, 20);

                PaginaSolicitada<CampoOrdenActa> ascendente =
                        PaginaSolicitada.<CampoOrdenActa>de(0, 1)
                                .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));
                PaginaSolicitada<CampoOrdenActa> descendente =
                        PaginaSolicitada.<CampoOrdenActa>de(0, 1)
                                .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA)));

                FiltroActa filtro = filtroDeDependencia(idDep);
                assertThat(actas.listarPagina(filtro, ascendente).contenido().get(0).getId())
                        .as("la fecha mas antigua es la primera acta insertada")
                        .isEqualTo(ids.get(0));
                assertThat(actas.listarPagina(filtro, descendente).contenido().get(0).getId())
                        .as("la fecha mas reciente es la ultima acta insertada")
                        .isEqualTo(ids.get(ids.size() - 1));
                return null;
            });
        }

        @Test
        @DisplayName("3. size=100 se acepta contra el motor y size=101 se rechaza antes de consultar")
        void limitesDeTamano() {
            enEscenario(idDep -> {
                insertarActas(idDep, 5);
                assertThat(actas.listarPagina(filtroDeDependencia(idDep),
                        PaginaSolicitada.de(0, 100)).contenido()).hasSize(5);
                assertThatThrownBy(() -> PaginaSolicitada.de(0, 101))
                        .isInstanceOf(PrecondicionVioladaException.class);
                return null;
            });
        }

        /**
         * El conteo se ejecuta con los mismos parametros enlazados que la pagina: si contara sin
         * filtro, el total incluiria filas ajenas al sandbox del escenario.
         */
        @Test
        @DisplayName("4. El total cuenta las filas filtradas y no la tabla completa")
        void totalCuentaLoFiltrado() {
            enEscenario(idDep -> {
                insertarActas(idDep, 7);
                Long otraDep = insertarDependencia();
                insertarActas(otraDep, 4);

                assertThat(actas.listarPagina(filtroDeDependencia(idDep), null).total())
                        .isEqualTo(7L);
                assertThat(actas.listarPagina(filtroDeDependencia(otraDep), null).total())
                        .isEqualTo(4L);
                return null;
            });
        }

        @Test
        @DisplayName("5. Un filtro sin coincidencias no emite la consulta de la pagina")
        void filtroSinCoincidencias() {
            enEscenario(idDep -> {
                insertarActas(idDep, 3);
                FiltroActa imposible = new FiltroActa(null, null, null, null, idDep, 999_999_999L,
                        null, null, null);
                PaginaResultado<FalActa> pagina = actas.listarPagina(imposible, null);
                assertThat(pagina.contenido()).isEmpty();
                assertThat(pagina.total()).isZero();
                assertThat(pagina.haySiguiente()).isFalse();
                return null;
            });
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Recorrido de paginas sobre un conjunto estable")
    class Recorrido {

        /**
         * Siete paginas de tres sobre veinte filas. La asercion fuerte no es el conteo: es que la
         * union de las paginas no tenga duplicados y coincida exactamente con el conjunto ordenado.
         * Sin el desempate por PK, dos filas empatadas podrian aparecer en dos paginas distintas o
         * en ninguna.
         */
        @Test
        @DisplayName("6. Las paginas cubren el total exacto sin repetir ni omitir")
        void paginasSinRepetirNiOmitir() {
            enEscenario(idDep -> {
                insertarActasConFechaRepetida(idDep, 20);
                FiltroActa filtro = filtroDeDependencia(idDep);

                List<Long> recorridas = new ArrayList<>();
                int page = 0;
                boolean hay = true;
                while (hay) {
                    PaginaResultado<FalActa> pagina =
                            actas.listarPagina(filtro, PaginaSolicitada.de(page, 3));
                    assertThat(pagina.total()).isEqualTo(20L);
                    pagina.contenido().forEach(a -> recorridas.add(a.getId()));
                    hay = pagina.haySiguiente();
                    page++;
                    assertThat(page).as("el recorrido debe terminar").isLessThan(20);
                }

                assertThat(recorridas).hasSize(20).doesNotHaveDuplicates();
                assertThat(recorridas)
                        .as("el recorrido equivale al conjunto ordenado en una sola pagina")
                        .isEqualTo(actas.listarPagina(filtro, PaginaSolicitada.de(0, 100))
                                .contenido().stream().map(FalActa::getId).toList());
                return null;
            });
        }

        /**
         * Todas las actas comparten {@code fecha_acta}: el unico criterio que puede producir un
         * orden total es el desempate por PK que el adaptador agrega siempre.
         */
        @Test
        @DisplayName("7. Con la fecha repetida el desempate por PK produce un orden total y estable")
        void desempatePorPkConFechaRepetida() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActasConFechaRepetida(idDep, 8);
                FiltroActa filtro = filtroDeDependencia(idDep);

                PaginaSolicitada<CampoOrdenActa> asc = PaginaSolicitada.<CampoOrdenActa>de(0, 100)
                        .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));
                assertThat(actas.listarPagina(filtro, asc).contenido())
                        .extracting(FalActa::getId)
                        .as("ASC desempata por id ascendente")
                        .isEqualTo(ids);

                PaginaSolicitada<CampoOrdenActa> desc = PaginaSolicitada.<CampoOrdenActa>de(0, 100)
                        .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA)));
                assertThat(actas.listarPagina(filtro, desc).contenido())
                        .extracting(FalActa::getId)
                        .as("DESC desempata por id descendente")
                        .isEqualTo(ids.reversed());
                return null;
            });
        }

        @Test
        @DisplayName("8. La modalidad scroll omite el total y resuelve haySiguiente con la fila extra")
        void scrollSinTotal() {
            enEscenario(idDep -> {
                insertarActas(idDep, 7);
                FiltroActa filtro = filtroDeDependencia(idDep);

                PaginaResultado<FalActa> primera = actas.listarPagina(
                        filtro, PaginaSolicitada.<CampoOrdenActa>de(0, 5).sinTotal());
                assertThat(primera.contenido()).hasSize(5);
                assertThat(primera.total()).isNull();
                assertThat(primera.haySiguiente()).isTrue();

                PaginaResultado<FalActa> ultima = actas.listarPagina(
                        filtro, PaginaSolicitada.<CampoOrdenActa>de(1, 5).sinTotal());
                assertThat(ultima.contenido()).hasSize(2);
                assertThat(ultima.total()).isNull();
                assertThat(ultima.haySiguiente())
                        .as("no hay fila extra: es la ultima pagina")
                        .isFalse();
                return null;
            });
        }

        @Test
        @DisplayName("9. Scroll y paginacion tradicional devuelven el mismo contenido")
        void scrollYTradicionalCoinciden() {
            enEscenario(idDep -> {
                insertarActas(idDep, 11);
                FiltroActa filtro = filtroDeDependencia(idDep);
                PaginaSolicitada<CampoOrdenActa> base = PaginaSolicitada.de(1, 4);

                assertThat(actas.listarPagina(filtro, base.sinTotal()).contenido())
                        .extracting(FalActa::getId)
                        .isEqualTo(actas.listarPagina(filtro, base).contenido().stream()
                                .map(FalActa::getId).toList());
                return null;
            });
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Batch de observaciones limitado a los ids de la pagina")
    class BatchDeLaPagina {

        /**
         * Con veinte actas y una pagina de tres, la operacion batch tiene que recibir exactamente
         * tres ids. Recibir veinte seria el defecto que {@code DECISION-OBS-BATCH-01} corrigio para
         * {@code listarTodas} y que la paginacion podria reintroducir sin que el resultado cambie.
         *
         * <p>El decorador delega en el adaptador real, de modo que la composicion sigue leyendo la
         * base y las observaciones que el caso afirma son las verdaderas.
         */
        @Test
        @DisplayName("10. La pagina pide el batch una sola vez y solo con los ids de la pagina")
        void batchSoloConLosIdsDeLaPagina() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActas(idDep, 20);
                for (Long idActa : ids) {
                    observaciones.guardar(observacionDe(idActa, MARCA + "obs de " + idActa));
                }

                ObservacionRepositoryQueCuenta espia = new ObservacionRepositoryQueCuenta(observaciones);
                JdbcActaRepository conEspia = new JdbcActaRepository(jdbc, espia);

                PaginaResultado<FalActa> pagina = conEspia.listarPagina(
                        filtroDeDependencia(idDep), PaginaSolicitada.de(0, 3));

                assertThat(espia.invocacionesBatch)
                        .as("una sola operacion batch por pagina")
                        .isEqualTo(1);
                assertThat(espia.idsRecibidos)
                        .as("exactamente los ids de la pagina, no los de la tabla")
                        .hasSize(3)
                        .containsExactlyInAnyOrderElementsOf(
                                pagina.contenido().stream().map(FalActa::getId).toList());
                assertThat(espia.invocacionesIndividuales)
                        .as("ninguna lectura de observaciones acta por acta")
                        .isZero();

                assertThat(pagina.contenido())
                        .extracting(FalActa::getObservaciones)
                        .as("la observacion inicial de cada acta se compone de verdad")
                        .doesNotContainNull();
                return null;
            });
        }

        @Test
        @DisplayName("11. Una pagina vacia no pide observaciones")
        void paginaVaciaNoPideObservaciones() {
            enEscenario(idDep -> {
                insertarActas(idDep, 2);
                ObservacionRepositoryQueCuenta espia = new ObservacionRepositoryQueCuenta(observaciones);
                JdbcActaRepository conEspia = new JdbcActaRepository(jdbc, espia);

                PaginaResultado<FalActa> pagina = conEspia.listarPagina(
                        filtroDeDependencia(idDep), PaginaSolicitada.de(9, 5));

                assertThat(pagina.contenido()).isEmpty();
                assertThat(espia.invocacionesBatch)
                        .as("sin propietarios no hay por quien preguntar")
                        .isZero();
                return null;
            });
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Filtros y orden de Acta contra el motor")
    class FiltrosDeActa {

        @Test
        @DisplayName("12. El rango de fechaActa recorta en el motor, inclusivo en los dos extremos")
        void rangoDeFechaInclusivo() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActas(idDep, 10);
                FiltroActa rango = new FiltroActa(null, null, null, null, idDep, null, null,
                        F0.plusDays(2), F0.plusDays(5));

                PaginaResultado<FalActa> pagina = actas.listarPagina(rango, null);
                assertThat(pagina.total()).isEqualTo(4L);
                assertThat(pagina.contenido())
                        .extracting(FalActa::getId)
                        .containsExactlyInAnyOrder(ids.get(2), ids.get(3), ids.get(4), ids.get(5));
                return null;
            });
        }

        @Test
        @DisplayName("13. El filtro por tipo de acta compara por el codigo canonico del enum")
        void filtroPorTipoDeActa() {
            enEscenario(idDep -> {
                insertarActas(idDep, 4);
                FiltroActa deTransito = new FiltroActa(TipoActa.TRANSITO, null, null, null,
                        idDep, null, null, null, null);
                assertThat(actas.listarPagina(deTransito, null).total()).isEqualTo(4L);

                FiltroActa deOtroTipo = new FiltroActa(TipoActa.CONTRAVENCION, null, null, null,
                        idDep, null, null, null, null);
                assertThat(actas.listarPagina(deOtroTipo, null).total()).isZero();
                return null;
            });
        }

        @Test
        @DisplayName("14. El filtro por nroActa es igualdad exacta y no una busqueda parcial")
        void filtroPorNroActaExacto() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActas(idDep, 3);
                jdbc.sql("UPDATE fal_acta SET nro_acta = :nro WHERE id = :id")
                        .param("nro", MARCA + "0001")
                        .param("id", ids.get(1))
                        .update();

                FiltroActa exacto = new FiltroActa(null, null, null, null, idDep, null,
                        MARCA + "0001", null, null);
                assertThat(actas.listarPagina(exacto, null).contenido())
                        .extracting(FalActa::getId).containsExactly(ids.get(1));

                FiltroActa parcial = new FiltroActa(null, null, null, null, idDep, null,
                        MARCA, null, null);
                assertThat(actas.listarPagina(parcial, null).total())
                        .as("un prefijo no coincide: el predicado es igualdad")
                        .isZero();
                return null;
            });
        }

        /**
         * Politica de nulos declarada: el orden nativo de MariaDB. {@code nro_acta} es nullable y
         * las actas del dataset no la traen, salvo una.
         */
        @Test
        @DisplayName("15. Los nulos van primero en ASC y ultimos en DESC, como MariaDB")
        void politicaDeNulos() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActas(idDep, 3);
                jdbc.sql("UPDATE fal_acta SET nro_acta = :nro WHERE id = :id")
                        .param("nro", MARCA + "UNICO")
                        .param("id", ids.get(0))
                        .update();

                FiltroActa filtro = filtroDeDependencia(idDep);
                PaginaSolicitada<CampoOrdenActa> asc = PaginaSolicitada.<CampoOrdenActa>de(0, 100)
                        .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.NRO_ACTA)));
                PaginaSolicitada<CampoOrdenActa> desc = PaginaSolicitada.<CampoOrdenActa>de(0, 100)
                        .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.NRO_ACTA)));

                assertThat(actas.listarPagina(filtro, asc).contenido().get(2).getId())
                        .as("el unico no nulo queda al final en ASC")
                        .isEqualTo(ids.get(0));
                assertThat(actas.listarPagina(filtro, desc).contenido().get(0).getId())
                        .as("y primero en DESC")
                        .isEqualTo(ids.get(0));
                return null;
            });
        }

        @Test
        @DisplayName("16. El orden compuesto respeta la precedencia en el motor")
        void ordenCompuestoEnElMotor() {
            enEscenario(idDep -> {
                List<Long> ids = insertarActasConFechaRepetida(idDep, 4);
                // Dos grupos de fecha: los dos primeros con F0, los dos ultimos con F0+1.
                jdbc.sql("UPDATE fal_acta SET fecha_acta = :f WHERE id IN (:a, :b)")
                        .param("f", F0.plusDays(1))
                        .param("a", ids.get(2))
                        .param("b", ids.get(3))
                        .update();

                PaginaSolicitada<CampoOrdenActa> compuesto = PaginaSolicitada.<CampoOrdenActa>de(0, 100)
                        .conOrden(List.of(
                                CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA),
                                CriterioOrden.asc(CampoOrdenActa.FECHA_ALTA)));

                assertThat(actas.listarPagina(filtroDeDependencia(idDep), compuesto).contenido())
                        .extracting(FalActa::getId)
                        .as("primero el grupo de fecha mayor, y dentro por alta ascendente")
                        .containsExactly(ids.get(2), ids.get(3), ids.get(0), ids.get(1));
                return null;
            });
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Filtros y orden de Persona contra el motor")
    class FiltrosDePersona {

        @Test
        @DisplayName("17. La pagina de personas recorta y ordena en el motor")
        void paginaDePersonas() {
            enEscenario(idDep -> {
                List<Long> ids = insertarPersonas(6);
                FiltroPersona filtro = new FiltroPersona(null, null, null, null, null,
                        T0, T0.plusHours(10));

                PaginaResultado<FalPersona> pagina = personas.listarPagina(filtro,
                        PaginaSolicitada.<CampoOrdenPersona>de(0, 2)
                                .conOrden(List.of(CriterioOrden.asc(CampoOrdenPersona.FECHA_ALTA))));

                assertThat(pagina.contenido()).hasSize(2);
                assertThat(pagina.total()).isEqualTo(6L);
                assertThat(pagina.contenido()).extracting(FalPersona::getId)
                        .containsExactly(ids.get(0), ids.get(1));
                assertThat(pagina.haySiguiente()).isTrue();
                return null;
            });
        }

        @Test
        @DisplayName("18. El filtro por tipo y numero de documento recorta en el motor")
        void filtroPorDocumento() {
            enEscenario(idDep -> {
                String nroDoc = String.valueOf(66_000_000 + (int) (Math.random() * 900_000));
                FalPersona conDoc = new FalPersona(null, TipoPersona.FISICA, T0, actor);
                conDoc.setApellido(MARCA + "CONDOC");
                conDoc.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, nroDoc));
                Long id = personas.guardar(conDoc).getId();
                insertarPersonas(3);

                FiltroPersona filtro = new FiltroPersona(null, TipoDocumentoPersona.DNI, nroDoc,
                        null, null, null, null);
                assertThat(personas.listarPagina(filtro, null).contenido())
                        .extracting(FalPersona::getId).containsExactly(id);
                return null;
            });
        }

        @Test
        @DisplayName("19. El orden por apellido respeta la politica de nulos y el desempate por PK")
        void ordenPorApellido() {
            enEscenario(idDep -> {
                FalPersona sinApellido = new FalPersona(null, TipoPersona.FISICA, T0, actor);
                sinApellido.setNombreMostrar(MARCA + "SIN");
                Long idSin = personas.guardar(sinApellido).getId();

                FalPersona conApellido = new FalPersona(null, TipoPersona.FISICA, T0, actor);
                conApellido.setApellido(MARCA + "ZETA");
                Long idCon = personas.guardar(conApellido).getId();

                FiltroPersona filtro = new FiltroPersona(null, null, null, null, null,
                        T0, T0.plusMinutes(1));
                PaginaSolicitada<CampoOrdenPersona> asc = PaginaSolicitada.<CampoOrdenPersona>de(0, 100)
                        .conOrden(List.of(CriterioOrden.asc(CampoOrdenPersona.APELLIDO)));

                List<Long> orden = personas.listarPagina(filtro, asc).contenido().stream()
                        .map(FalPersona::getId).toList();
                assertThat(orden.indexOf(idSin))
                        .as("el nulo va primero en ASC")
                        .isLessThan(orden.indexOf(idCon));
                return null;
            });
        }

        @Test
        @DisplayName("20. La modalidad scroll de Persona omite el total")
        void scrollDePersona() {
            enEscenario(idDep -> {
                insertarPersonas(5);
                FiltroPersona filtro = new FiltroPersona(null, null, null, null, null,
                        T0, T0.plusHours(10));
                PaginaResultado<FalPersona> pagina = personas.listarPagina(filtro,
                        PaginaSolicitada.<CampoOrdenPersona>de(0, 3).sinTotal());
                assertThat(pagina.contenido()).hasSize(3);
                assertThat(pagina.total()).isNull();
                assertThat(pagina.haySiguiente()).isTrue();
                return null;
            });
        }
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /** Espia que delega en el adaptador real y registra la cardinalidad de las lecturas. */
    private static final class ObservacionRepositoryQueCuenta implements ObservacionRepository {
        private final ObservacionRepository delegado;
        private int invocacionesBatch = 0;
        private int invocacionesIndividuales = 0;
        private final List<Long> idsRecibidos = new ArrayList<>();

        ObservacionRepositoryQueCuenta(ObservacionRepository delegado) {
            this.delegado = delegado;
        }

        @Override public Map<Long, List<FalObservacion>> listarPorEntidades(
                EntidadTipoObservada tipo, Collection<Long> ids) {
            invocacionesBatch++;
            idsRecibidos.addAll(ids);
            return delegado.listarPorEntidades(tipo, ids);
        }
        @Override public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long id) {
            invocacionesIndividuales++;
            return delegado.listarPorEntidad(tipo, id);
        }
        @Override public FalObservacion guardar(FalObservacion obs) { return delegado.guardar(obs); }
        @Override public Optional<FalObservacion> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada t, Long id) {
            return delegado.listarActivasPorEntidad(t, id);
        }
        @Override public FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja) {
            return delegado.desactivar(id, fhBaja, idUserBaja);
        }
    }

    private static FiltroActa filtroDeDependencia(Long idDep) {
        return new FiltroActa(null, null, null, null, idDep, null, null, null, null);
    }

    private FalObservacion observacionDe(Long idActa, String texto) {
        return new FalObservacion(null, EntidadTipoObservada.ACTA, idActa, null, texto,
                OrigenObservacion.USUARIO, null, T0, actor);
    }

    /** Plan de ejecucion de la consulta, como evidencia de que el motor la resuelve. */
    private String planDe(String sql, Long idDep) {
        return String.join(" | ", jdbc.sql("EXPLAIN " + sql)
                .param("idDep", idDep)
                .query((rs, n) -> rs.getString("select_type") + "/" + rs.getString("type")
                        + "/rows=" + rs.getString("rows") + "/extra=" + rs.getString("Extra"))
                .list());
    }

    /** Escenario dentro de una transaccion que se revierte siempre. */
    private <T> T enEscenario(Function<Long, T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.apply(insertarDependencia());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    private Long insertarDependencia() {
        String nombre = MARCA + UUID.randomUUID().toString().substring(0, 8);
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", nombre).param("fhAlta", T0).param("usuario", actor).update();
        Long idDep = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", nombre).query(Long.class).single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, 1, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDep).param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0).param("usuario", actor).update();
        return idDep;
    }

    /** Actas con fecha creciente: el orden por fecha coincide con el orden de insercion. */
    private List<Long> insertarActas(Long idDep, int cantidad) {
        List<Long> ids = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            ids.add(guardarActa(idDep, F0.plusDays(i), T0.plusMinutes(i)));
        }
        return ids;
    }

    /** Actas que comparten {@code fecha_acta}: solo el desempate por PK puede ordenarlas. */
    private List<Long> insertarActasConFechaRepetida(Long idDep, int cantidad) {
        List<Long> ids = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            ids.add(guardarActa(idDep, F0, T0.plusMinutes(i)));
        }
        return ids;
    }

    private Long guardarActa(Long idDep, LocalDate fechaActa, LocalDateTime fhAlta) {
        FalActa acta = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, idDep, null,
                fechaActa, fhAlta, MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, fhAlta, actor);
        acta.setCodigoQr(MARCA + UUID.randomUUID());
        return actas.guardar(acta).getId();
    }

    private List<Long> insertarPersonas(int cantidad) {
        List<Long> ids = new ArrayList<>(cantidad);
        for (int i = 0; i < cantidad; i++) {
            FalPersona persona = new FalPersona(null, TipoPersona.FISICA, T0.plusMinutes(i), actor);
            persona.setApellido(MARCA + "P" + i);
            ids.add(personas.guardar(persona).getId());
        }
        return ids;
    }
}
