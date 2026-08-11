package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.DireccionOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contrato de {@code DECISION-LIST-PAGE-SORT-01} ejercido sobre el oraculo de paridad
 * InMemory.
 *
 * <p>El adaptador InMemory es el oraculo declarado del proyecto: reproduce el
 * <b>comportamiento observable</b> del contrato —defaults, limites, precedencia de criterios,
 * politica de nulos y desempate por PK— sin prometer eficiencia de motor ni ACID. Verificar el
 * contrato aca, y no solo contra el motor, es lo que permite que estos casos corran en Surefire
 * sin variables de ambiente y que un cambio de contrato falle antes de llegar a la integracion.
 *
 * <p>La contraparte contra MariaDB esta en {@code JdbcPaginacionIT}, que verifica ademas lo que
 * este nivel no puede: que el recorte y el orden ocurran realmente en el motor.
 */
@DisplayName("DECISION-LIST-PAGE-SORT-01: contrato de pagina, filtros y orden")
class ConsultaPaginadaTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    private ActaRepository actas;
    private PersonaRepository personas;

    @BeforeEach
    void nuevoAlmacenamiento() {
        actas = new InMemoryActaRepository();
        personas = new InMemoryPersonaRepository();
    }

    // =====================================================================
    // Construccion de datos
    // =====================================================================

    /** Acta minima valida: sin persona infractora, para no arrastrar coherencia de identificacion. */
    private FalActa acta(LocalDate fechaActa, LocalDateTime fhAlta) {
        return new FalActa(
                null,
                UUID.randomUUID().toString(),
                TipoActa.TRANSITO,
                1L,
                1L,
                fechaActa,
                fhAlta,
                "Domicilio del hecho",
                null,
                null,
                null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE,
                null,
                fhAlta,
                "actor-test");
    }

    private FalActa guardarActa(LocalDate fechaActa, LocalDateTime fhAlta) {
        return actas.guardar(acta(fechaActa, fhAlta));
    }

    private FalPersona guardarPersona(String apellido, LocalDateTime fhAlta) {
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, fhAlta, "actor-test");
        persona.setApellido(apellido);
        return personas.guardar(persona);
    }

    private FalPersona guardarPersonaConDni(String nroDoc, LocalDateTime fhAlta) {
        FalPersona persona = new FalPersona(null, TipoPersona.FISICA, fhAlta, "actor-test");
        persona.setDocumento(DocumentoPersona.normalizar(TipoDocumentoPersona.DNI, nroDoc));
        return personas.guardar(persona);
    }

    private static List<Long> idsDe(PaginaResultado<FalActa> pagina) {
        return pagina.contenido().stream().map(FalActa::getId).toList();
    }

    // =====================================================================
    @Nested
    @DisplayName("Defaults: la ausencia de parametros no es consulta ilimitada")
    class Defaults {

        @Test
        @DisplayName("1. sin parametros la pagina es 0, el tamano 50 y el orden el predeterminado")
        void sinParametrosDefaults() {
            PaginaSolicitada<CampoOrdenActa> primera = PaginaSolicitada.primeraPagina();
            assertThat(primera.page()).isZero();
            assertThat(primera.size()).isEqualTo(50);
            assertThat(primera.orden()).isEmpty();
            assertThat(primera.incluirTotal()).isTrue();
            assertThat(primera.offset()).isZero();
        }

        @Test
        @DisplayName("2. las constantes del contrato son 0, 50 y 100")
        void constantesDelContrato() {
            assertThat(PaginaSolicitada.PAGE_DEFAULT).isZero();
            assertThat(PaginaSolicitada.SIZE_DEFAULT).isEqualTo(50);
            assertThat(PaginaSolicitada.SIZE_MAX).isEqualTo(100);
        }

        @Test
        @DisplayName("3. un filtro nulo y una pagina nula equivalen a sin filtro y primera pagina")
        void argumentosNulosEquivalenALosDefaults() {
            for (int i = 0; i < 3; i++) {
                guardarActa(LocalDate.of(2026, 1, 1 + i), T0.plusHours(i));
            }
            PaginaResultado<FalActa> pagina = actas.listarPagina(null, null);
            assertThat(pagina.contenido()).hasSize(3);
            assertThat(pagina.page()).isZero();
            assertThat(pagina.size()).isEqualTo(50);
            assertThat(pagina.total()).isEqualTo(3L);
            assertThat(pagina.haySiguiente()).isFalse();
        }

        @Test
        @DisplayName("4. el orden predeterminado de Acta es fechaActa DESC con desempate por PK")
        void ordenPredeterminadoDeActa() {
            assertThat(CampoOrdenActa.ordenPredeterminado())
                    .containsExactly(CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA));

            FalActa vieja = guardarActa(LocalDate.of(2026, 1, 1), T0);
            FalActa nueva = guardarActa(LocalDate.of(2026, 3, 1), T0.plusDays(1));
            FalActa media = guardarActa(LocalDate.of(2026, 2, 1), T0.plusDays(2));

            assertThat(idsDe(actas.listarPagina(null, null)))
                    .containsExactly(nueva.getId(), media.getId(), vieja.getId());
        }

        @Test
        @DisplayName("5. el orden predeterminado de Persona es fechaAlta DESC con desempate por PK")
        void ordenPredeterminadoDePersona() {
            assertThat(CampoOrdenPersona.ordenPredeterminado())
                    .containsExactly(CriterioOrden.desc(CampoOrdenPersona.FECHA_ALTA));

            FalPersona primera = guardarPersona("PRIMERA", T0);
            FalPersona tercera = guardarPersona("TERCERA", T0.plusHours(2));
            FalPersona segunda = guardarPersona("SEGUNDA", T0.plusHours(1));

            assertThat(personas.listarPagina(null, null).contenido())
                    .extracting(FalPersona::getId)
                    .containsExactly(tercera.getId(), segunda.getId(), primera.getId());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Limites de tamano y de pagina")
    class Limites {

        @Test
        @DisplayName("6. size=100 se acepta")
        void tamanoMaximoAceptado() {
            assertThat(PaginaSolicitada.de(0, 100).size()).isEqualTo(100);
        }

        @Test
        @DisplayName("7. size=101 se rechaza explicitamente y no se recorta a 100")
        void tamanoExcedidoRechazado() {
            assertThatThrownBy(() -> PaginaSolicitada.de(0, 101))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("excede el maximo permitido de 100")
                    .hasMessageContaining("101");
        }

        @Test
        @DisplayName("8. page negativa se rechaza")
        void paginaNegativaRechazada() {
            assertThatThrownBy(() -> PaginaSolicitada.de(-1, 50))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("page no puede ser negativo");
        }

        @Test
        @DisplayName("9. size cero y size negativo se rechazan")
        void tamanoNoPositivoRechazado() {
            for (int invalido : new int[] {0, -1, -50}) {
                assertThatThrownBy(() -> PaginaSolicitada.de(0, invalido))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("size debe ser mayor que cero");
            }
        }

        @Test
        @DisplayName("10. una pagina mas alla del total devuelve contenido vacio y el total real")
        void paginaFueraDeRango() {
            guardarActa(LocalDate.of(2026, 1, 1), T0);
            PaginaResultado<FalActa> pagina = actas.listarPagina(null, PaginaSolicitada.de(5, 10));
            assertThat(pagina.contenido()).isEmpty();
            assertThat(pagina.total()).isEqualTo(1L);
            assertThat(pagina.haySiguiente()).isFalse();
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Orden simple, compuesto y precedencia")
    class Orden {

        @Test
        @DisplayName("11. orden simple ascendente y descendente sobre el mismo campo se invierten")
        void ordenSimple() {
            FalActa a = guardarActa(LocalDate.of(2026, 1, 1), T0);
            FalActa b = guardarActa(LocalDate.of(2026, 2, 1), T0.plusDays(1));

            PaginaSolicitada<CampoOrdenActa> asc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));
            PaginaSolicitada<CampoOrdenActa> desc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA)));

            assertThat(idsDe(actas.listarPagina(null, asc))).containsExactly(a.getId(), b.getId());
            assertThat(idsDe(actas.listarPagina(null, desc))).containsExactly(b.getId(), a.getId());
        }

        /**
         * Las tres actas comparten dependencia deliberadamente: si la precedencia no se
         * respetara, el segundo criterio ordenaria el conjunto completo y el resultado seria el
         * mismo que pedir solo ese criterio.
         */
        @Test
        @DisplayName("12. el orden compuesto respeta la precedencia de aparicion")
        void ordenCompuestoRespetaPrecedencia() {
            FalActa a = guardarActa(LocalDate.of(2026, 1, 1), T0.plusHours(3));
            FalActa b = guardarActa(LocalDate.of(2026, 1, 1), T0.plusHours(1));
            FalActa c = guardarActa(LocalDate.of(2026, 2, 1), T0.plusHours(2));

            PaginaSolicitada<CampoOrdenActa> orden = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(
                            CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA),
                            CriterioOrden.asc(CampoOrdenActa.FECHA_ALTA)));

            assertThat(idsDe(actas.listarPagina(null, orden)))
                    .as("primero por fecha del acta y, dentro del empate, por fecha de alta")
                    .containsExactly(b.getId(), a.getId(), c.getId());

            PaginaSolicitada<CampoOrdenActa> invertido = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(
                            CriterioOrden.asc(CampoOrdenActa.FECHA_ALTA),
                            CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));

            assertThat(idsDe(actas.listarPagina(null, invertido)))
                    .as("invertir la precedencia cambia el resultado")
                    .containsExactly(b.getId(), c.getId(), a.getId());
        }

        @Test
        @DisplayName("13. el maximo de criterios es 3 y un cuarto criterio se rechaza")
        void maximoDeCriterios() {
            assertThat(PaginaSolicitada.MAX_CRITERIOS_ORDEN).isEqualTo(3);

            List<CriterioOrden<CampoOrdenActa>> tres = List.of(
                    CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA),
                    CriterioOrden.asc(CampoOrdenActa.FECHA_ALTA),
                    CriterioOrden.asc(CampoOrdenActa.NRO_ACTA));
            assertThat(PaginaSolicitada.<CampoOrdenActa>primeraPagina().conOrden(tres).orden())
                    .hasSize(3);

            List<CriterioOrden<CampoOrdenActa>> cuatro = List.of(
                    CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA),
                    CriterioOrden.asc(CampoOrdenActa.FECHA_ALTA),
                    CriterioOrden.asc(CampoOrdenActa.NRO_ACTA),
                    CriterioOrden.asc(CampoOrdenActa.TIPO_ACTA));
            assertThatThrownBy(() -> PaginaSolicitada.<CampoOrdenActa>primeraPagina().conOrden(cuatro))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("hasta 3 criterios");
        }

        @Test
        @DisplayName("14. un campo repetido se rechaza")
        void campoRepetidoRechazado() {
            List<CriterioOrden<CampoOrdenActa>> repetido = List.of(
                    CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA),
                    CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA));
            assertThatThrownBy(() -> PaginaSolicitada.<CampoOrdenActa>primeraPagina().conOrden(repetido))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("el mismo campo dos veces")
                    .hasMessageContaining("FECHA_ACTA");
        }

        @Test
        @DisplayName("15. un criterio nulo y una lista nula se rechazan")
        void criterioNuloRechazado() {
            assertThatThrownBy(() ->
                    new PaginaSolicitada<CampoOrdenActa>(0, 50, java.util.Arrays.asList(
                            CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA), null), true))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no admite criterios nulos");

            assertThatThrownBy(() -> new PaginaSolicitada<CampoOrdenActa>(0, 50, null, true))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("orden es obligatorio");
        }

        @Test
        @DisplayName("16. un campo o una direccion nulos en el criterio se rechazan")
        void criterioIncompletoRechazado() {
            assertThatThrownBy(() -> new CriterioOrden<CampoOrdenActa>(null, DireccionOrden.ASC))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("campo de orden es obligatorio");
            assertThatThrownBy(() -> new CriterioOrden<>(CampoOrdenActa.FECHA_ACTA, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("direccion de orden es obligatoria");
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Allowlist: el cliente solo puede nombrar un campo funcional")
    class Allowlist {

        @Test
        @DisplayName("17. un campo desconocido de Acta se rechaza y el mensaje enumera los admitidos")
        void campoDesconocidoDeActaRechazado() {
            for (String desconocido : new String[] {
                    "titular", "nro_acta; DROP TABLE fal_acta", "id_dep", "(SELECT 1)", "rand()"}) {
                assertThatThrownBy(() -> CampoOrdenActa.desdeNombreFuncional(desconocido))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("campo de orden desconocido para Acta")
                        .hasMessageContaining("Campos admitidos");
            }
        }

        @Test
        @DisplayName("18. un campo desconocido de Persona se rechaza, incluida la columna generada")
        void campoDesconocidoDePersonaRechazado() {
            for (String desconocido : new String[] {"doc_key", "nombre_mostrar; --", "id"}) {
                assertThatThrownBy(() -> CampoOrdenPersona.desdeNombreFuncional(desconocido))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("campo de orden desconocido para Persona");
            }
        }

        @Test
        @DisplayName("19. los nombres funcionales resuelven sin distinguir mayusculas")
        void nombresFuncionalesResuelven() {
            assertThat(CampoOrdenActa.desdeNombreFuncional("fechaActa")).isEqualTo(CampoOrdenActa.FECHA_ACTA);
            assertThat(CampoOrdenActa.desdeNombreFuncional("FECHAACTA")).isEqualTo(CampoOrdenActa.FECHA_ACTA);
            assertThat(CampoOrdenActa.desdeNombreFuncional("FECHA_ACTA")).isEqualTo(CampoOrdenActa.FECHA_ACTA);
            assertThat(CampoOrdenPersona.desdeNombreFuncional("nombreMostrar"))
                    .isEqualTo(CampoOrdenPersona.NOMBRE_MOSTRAR);
        }

        @Test
        @DisplayName("20. un campo nulo o blanco se rechaza")
        void campoNuloRechazado() {
            for (String vacio : new String[] {null, "", "   "}) {
                assertThatThrownBy(() -> CampoOrdenActa.desdeNombreFuncional(vacio))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("campo de orden es obligatorio");
            }
        }

        @Test
        @DisplayName("21. una direccion invalida se rechaza y no se degrada a ASC")
        void direccionInvalidaRechazada() {
            assertThat(DireccionOrden.desdeTexto("asc")).isEqualTo(DireccionOrden.ASC);
            assertThat(DireccionOrden.desdeTexto(" DESC ")).isEqualTo(DireccionOrden.DESC);
            for (String invalida : new String[] {"ascending", "DESCENDING", "up", "1", "ASC DESC"}) {
                assertThatThrownBy(() -> DireccionOrden.desdeTexto(invalida))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("direccion de orden invalida");
            }
            for (String vacia : new String[] {null, "", "  "}) {
                assertThatThrownBy(() -> DireccionOrden.desdeTexto(vacia))
                        .isInstanceOf(PrecondicionVioladaException.class)
                        .hasMessageContaining("direccion de orden es obligatoria");
            }
        }

        @Test
        @DisplayName("22. la allowlist de Acta no expone titular y la de Persona no expone doc_key")
        void allowlistSinCamposInexistentes() {
            assertThat(CampoOrdenActa.nombresFuncionales()).doesNotContain("titular");
            assertThat(CampoOrdenPersona.nombresFuncionales()).doesNotContain("docKey");
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Desempate estable y politica de nulos")
    class Estabilidad {

        /**
         * Las tres actas comparten {@code fecha_acta}: sin el desempate por PK el orden entre
         * ellas seria indefinido y la paginacion podria repetir u omitir filas.
         */
        @Test
        @DisplayName("23. los empates se resuelven por PK y el orden es total")
        void empatesEstablesPorPk() {
            LocalDate misma = LocalDate.of(2026, 5, 5);
            FalActa a = guardarActa(misma, T0);
            FalActa b = guardarActa(misma, T0);
            FalActa c = guardarActa(misma, T0);

            PaginaSolicitada<CampoOrdenActa> asc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));
            PaginaSolicitada<CampoOrdenActa> desc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.FECHA_ACTA)));

            assertThat(idsDe(actas.listarPagina(null, asc)))
                    .as("ASC: el desempate sigue la direccion del ultimo criterio")
                    .containsExactly(a.getId(), b.getId(), c.getId());
            assertThat(idsDe(actas.listarPagina(null, desc)))
                    .as("DESC: el desempate se invierte con el criterio")
                    .containsExactly(c.getId(), b.getId(), a.getId());
        }

        @Test
        @DisplayName("24. dos ejecuciones de la misma consulta devuelven la misma secuencia")
        void secuenciaReproducible() {
            for (int i = 0; i < 12; i++) {
                guardarActa(LocalDate.of(2026, 6, 1), T0);
            }
            List<Long> primera = idsDe(actas.listarPagina(null, PaginaSolicitada.de(0, 5)));
            List<Long> repetida = idsDe(actas.listarPagina(null, PaginaSolicitada.de(0, 5)));
            assertThat(repetida).isEqualTo(primera);
        }

        /**
         * Politica declarada: nulos primero en {@code ASC} y ultimos en {@code DESC}, que es el
         * orden nativo de MariaDB. {@code nro_acta} es nullable y las actas nuevas no la traen.
         */
        @Test
        @DisplayName("25. los nulos van primero en ASC y ultimos en DESC")
        void politicaDeNulos() {
            FalActa conNro = guardarActa(LocalDate.of(2026, 1, 1), T0);
            conNro.setNroActa("A-0001");
            conNro = actas.guardar(conNro);
            FalActa sinNro = guardarActa(LocalDate.of(2026, 1, 2), T0.plusDays(1));

            PaginaSolicitada<CampoOrdenActa> asc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.NRO_ACTA)));
            PaginaSolicitada<CampoOrdenActa> desc = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.desc(CampoOrdenActa.NRO_ACTA)));

            assertThat(idsDe(actas.listarPagina(null, asc)))
                    .containsExactly(sinNro.getId(), conNro.getId());
            assertThat(idsDe(actas.listarPagina(null, desc)))
                    .containsExactly(conNro.getId(), sinNro.getId());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Recorrido de paginas sin repetir ni omitir")
    class Recorrido {

        @Test
        @DisplayName("26. las paginas de un conjunto estable cubren el total sin repetir ni omitir")
        void paginasCubrenElTotalSinRepetir() {
            for (int i = 0; i < 12; i++) {
                guardarActa(LocalDate.of(2026, 7, 1).plusDays(i), T0.plusHours(i));
            }
            List<Long> recorridas = new java.util.ArrayList<>();
            for (int page = 0; page < 3; page++) {
                PaginaResultado<FalActa> pagina =
                        actas.listarPagina(null, PaginaSolicitada.de(page, 5));
                assertThat(pagina.total()).isEqualTo(12L);
                assertThat(pagina.haySiguiente()).isEqualTo(page < 2);
                recorridas.addAll(idsDe(pagina));
            }
            assertThat(recorridas).hasSize(12).doesNotHaveDuplicates();
            assertThat(recorridas)
                    .as("el recorrido completo equivale al conjunto ordenado de una sola pagina")
                    .isEqualTo(idsDe(actas.listarPagina(null, PaginaSolicitada.de(0, 100))));
        }

        @Test
        @DisplayName("27. la modalidad scroll omite el total y resuelve haySiguiente igual")
        void scrollSinTotal() {
            for (int i = 0; i < 7; i++) {
                guardarActa(LocalDate.of(2026, 8, 1).plusDays(i), T0.plusHours(i));
            }
            PaginaResultado<FalActa> primera =
                    actas.listarPagina(null, PaginaSolicitada.<CampoOrdenActa>de(0, 5).sinTotal());
            assertThat(primera.contenido()).hasSize(5);
            assertThat(primera.total()).isNull();
            assertThat(primera.tieneTotal()).isFalse();
            assertThat(primera.haySiguiente()).isTrue();

            PaginaResultado<FalActa> segunda =
                    actas.listarPagina(null, PaginaSolicitada.<CampoOrdenActa>de(1, 5).sinTotal());
            assertThat(segunda.contenido()).hasSize(2);
            assertThat(segunda.total()).isNull();
            assertThat(segunda.haySiguiente()).isFalse();
        }

        @Test
        @DisplayName("28. scroll y paginacion tradicional devuelven el mismo contenido")
        void scrollYPaginacionTradicionalCoinciden() {
            for (int i = 0; i < 9; i++) {
                guardarActa(LocalDate.of(2026, 9, 1).plusDays(i), T0.plusHours(i));
            }
            PaginaSolicitada<CampoOrdenActa> base = PaginaSolicitada.de(1, 4);
            assertThat(idsDe(actas.listarPagina(null, base.sinTotal())))
                    .isEqualTo(idsDe(actas.listarPagina(null, base)));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Filtros tipados de Acta")
    class FiltrosDeActa {

        @Test
        @DisplayName("29. un filtro vacio no recorta y ninguno() es equivalente")
        void filtroVacio() {
            guardarActa(LocalDate.of(2026, 1, 1), T0);
            assertThat(FiltroActa.ninguno().estaVacio()).isTrue();
            assertThat(actas.listarPagina(FiltroActa.ninguno(), null).total()).isEqualTo(1L);
        }

        @Test
        @DisplayName("30. el rango de fechaActa es inclusivo en los dos extremos")
        void rangoDeFechaInclusivo() {
            FalActa antes = guardarActa(LocalDate.of(2026, 1, 1), T0);
            FalActa desde = guardarActa(LocalDate.of(2026, 2, 1), T0.plusDays(1));
            FalActa hasta = guardarActa(LocalDate.of(2026, 3, 1), T0.plusDays(2));
            FalActa despues = guardarActa(LocalDate.of(2026, 4, 1), T0.plusDays(3));

            FiltroActa rango = new FiltroActa(null, null, null, null, null, null, null,
                    LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
            List<Long> ids = idsDe(actas.listarPagina(rango, null));
            assertThat(ids).containsExactlyInAnyOrder(desde.getId(), hasta.getId());
            assertThat(ids).doesNotContain(antes.getId(), despues.getId());
        }

        @Test
        @DisplayName("31. un rango invertido se rechaza en el contrato del filtro")
        void rangoInvertidoRechazado() {
            assertThatThrownBy(() -> new FiltroActa(null, null, null, null, null, null, null,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no puede ser posterior");
        }

        @Test
        @DisplayName("32. un nroActa blanco se rechaza en lugar de filtrar por cadena vacia")
        void nroActaBlancoRechazado() {
            assertThatThrownBy(() -> new FiltroActa(null, null, null, null, null, null, "   ",
                    null, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("omitalo para no filtrar");
        }

        @Test
        @DisplayName("33. el filtro combina con el orden y el recorte")
        void filtroCombinaConOrdenYRecorte() {
            for (int i = 0; i < 6; i++) {
                guardarActa(LocalDate.of(2026, 1, 1).plusDays(i), T0.plusHours(i));
            }
            FiltroActa rango = new FiltroActa(null, null, null, null, null, null, null,
                    LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 5));
            PaginaSolicitada<CampoOrdenActa> pagina = PaginaSolicitada.<CampoOrdenActa>de(0, 2)
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));

            PaginaResultado<FalActa> resultado = actas.listarPagina(rango, pagina);
            assertThat(resultado.total()).as("el total cuenta las filtradas, no todas").isEqualTo(4L);
            assertThat(resultado.contenido()).hasSize(2);
            assertThat(resultado.contenido().get(0).getFechaActa()).isEqualTo(LocalDate.of(2026, 1, 2));
            assertThat(resultado.haySiguiente()).isTrue();
        }

        @Test
        @DisplayName("34. un filtro sin coincidencias devuelve pagina vacia con total cero")
        void filtroSinCoincidencias() {
            guardarActa(LocalDate.of(2026, 1, 1), T0);
            FiltroActa sinCoincidencia = new FiltroActa(TipoActa.TRANSITO, null, null, null,
                    999L, null, null, null, null);
            PaginaResultado<FalActa> resultado = actas.listarPagina(sinCoincidencia, null);
            assertThat(resultado.contenido()).isEmpty();
            assertThat(resultado.total()).isZero();
            assertThat(resultado.haySiguiente()).isFalse();
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Filtros tipados y orden de Persona")
    class FiltrosDePersona {

        @Test
        @DisplayName("35. el filtro por tipo y numero de documento recorta")
        void filtroPorDocumento() {
            FalPersona buscada = guardarPersonaConDni("30111222", T0);
            guardarPersonaConDni("30333444", T0.plusHours(1));

            FiltroPersona filtro = new FiltroPersona(null, TipoDocumentoPersona.DNI, "30111222",
                    null, null, null, null);
            PaginaResultado<FalPersona> resultado = personas.listarPagina(filtro, null);
            assertThat(resultado.total()).isEqualTo(1L);
            assertThat(resultado.contenido()).extracting(FalPersona::getId)
                    .containsExactly(buscada.getId());
        }

        @Test
        @DisplayName("36. el rango de fhAlta es inclusivo y el rango invertido se rechaza")
        void rangoDeAlta() {
            guardarPersona("UNO", T0);
            FalPersona dentro = guardarPersona("DOS", T0.plusHours(5));
            guardarPersona("TRES", T0.plusHours(10));

            FiltroPersona filtro = new FiltroPersona(null, null, null, null, null,
                    T0.plusHours(5), T0.plusHours(5));
            assertThat(personas.listarPagina(filtro, null).contenido())
                    .extracting(FalPersona::getId)
                    .containsExactly(dentro.getId());

            assertThatThrownBy(() -> new FiltroPersona(null, null, null, null, null,
                    T0.plusHours(10), T0))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no puede ser posterior");
        }

        @Test
        @DisplayName("37. el orden por apellido respeta la politica de nulos y el desempate")
        void ordenPorApellido() {
            FalPersona conApellido = guardarPersona("ZETA", T0);
            FalPersona sinApellido = guardarPersona(null, T0.plusHours(1));

            PaginaSolicitada<CampoOrdenPersona> asc = PaginaSolicitada.<CampoOrdenPersona>primeraPagina()
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenPersona.APELLIDO)));
            assertThat(personas.listarPagina(null, asc).contenido())
                    .extracting(FalPersona::getId)
                    .containsExactly(sinApellido.getId(), conApellido.getId());
        }

        @Test
        @DisplayName("38. la modalidad scroll de Persona omite el total")
        void scrollDePersona() {
            for (int i = 0; i < 5; i++) {
                guardarPersona("P" + i, T0.plusHours(i));
            }
            PaginaResultado<FalPersona> pagina = personas.listarPagina(
                    null, PaginaSolicitada.<CampoOrdenPersona>de(0, 3).sinTotal());
            assertThat(pagina.contenido()).hasSize(3);
            assertThat(pagina.total()).isNull();
            assertThat(pagina.haySiguiente()).isTrue();
        }

        @Test
        @DisplayName("39. un nroDoc blanco se rechaza en lugar de filtrar por cadena vacia")
        void nroDocBlancoRechazado() {
            assertThatThrownBy(() -> new FiltroPersona(null, null, "  ", null, null, null, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("omitalo para no filtrar");
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Inmutabilidad de los contratos")
    class Inmutabilidad {

        @Test
        @DisplayName("40. la lista de criterios y el contenido de la pagina son inmodificables")
        void contratosInmodificables() {
            PaginaSolicitada<CampoOrdenActa> pagina = PaginaSolicitada.<CampoOrdenActa>primeraPagina()
                    .conOrden(List.of(CriterioOrden.asc(CampoOrdenActa.FECHA_ACTA)));
            assertThatThrownBy(() -> pagina.orden().add(CriterioOrden.asc(CampoOrdenActa.NRO_ACTA)))
                    .isInstanceOf(UnsupportedOperationException.class);

            guardarActa(LocalDate.of(2026, 1, 1), T0);
            PaginaResultado<FalActa> resultado = actas.listarPagina(null, null);
            assertThatThrownBy(() -> resultado.contenido().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("41. un total negativo y un contenido nulo se rechazan")
        void resultadoInvalidoRechazado() {
            assertThatThrownBy(() -> new PaginaResultado<FalActa>(List.of(), 0, 50, -1L, false))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("total no puede ser negativo");
            assertThatThrownBy(() -> new PaginaResultado<FalActa>(null, 0, 50, 0L, false))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contenido es obligatorio");
        }

        @Test
        @DisplayName("42. la pagina vacia declara total cero y ninguna siguiente")
        void paginaVacia() {
            PaginaResultado<FalActa> vacia = PaginaResultado.vacia(0, 50);
            assertThat(vacia.contenido()).isEmpty();
            assertThat(vacia.total()).isZero();
            assertThat(vacia.haySiguiente()).isFalse();
            assertThat(vacia.tieneTotal()).isTrue();
        }
    }
}
