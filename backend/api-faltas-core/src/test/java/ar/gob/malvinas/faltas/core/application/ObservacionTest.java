package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.application.service.ObservacionService;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11G: ObservacionTest")
class ObservacionTest {

    private static final LocalDateTime T0 = FaltasClockTestSupport.FIXED.now();
    private static final String ACTOR_BAJA = "11111111-2222-3333-4444-555555555555";

    private InMemoryObservacionRepository repo;
    private InMemoryActaRepository actaRepo;
    private ObservacionService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryObservacionRepository();
        actaRepo = new InMemoryActaRepository();
        service = new ObservacionService(repo, actaRepo, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActa() {
        FalActa acta = new FalActa(null, "uuid-" + java.util.UUID.randomUUID(), TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle 1", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, SituacionIdentificacionInfractor.PRESENTE_NO_APORTA_DATOS, null, FaltasClockTestSupport.FIXED.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    /** Observacion nueva: id nulo, la PK la asigna la persistencia. */
    private static FalObservacion nueva(EntidadTipoObservada tipo, Long entidadId,
                                        String texto, LocalDateTime fhAlta) {
        return new FalObservacion(null, tipo, entidadId, null, texto,
                OrigenObservacion.USUARIO, fhAlta, "U");
    }

    @Nested
    @DisplayName("EntidadTipoObservada - 22 codigos")
    class CodigosEnum {

        @Test
        @DisplayName("Todos los 22 codigos son unicos")
        void codigos_unicos() {
            long distinct = java.util.Arrays.stream(EntidadTipoObservada.values())
                    .mapToInt(EntidadTipoObservada::codigo).distinct().count();
            assertThat(distinct).isEqualTo(22);
        }

        @Test
        @DisplayName("fromCodigo resuelve todos los 22 codigos")
        void fromCodigo_todos() {
            for (EntidadTipoObservada tipo : EntidadTipoObservada.values()) {
                EntidadTipoObservada found = EntidadTipoObservada.fromCodigo(tipo.codigo());
                assertThat(found).isEqualTo(tipo);
            }
        }

        @Test
        @DisplayName("fromCodigo codigo desconocido lanza IllegalArgumentException")
        void fromCodigo_desconocido() {
            assertThatThrownBy(() -> EntidadTipoObservada.fromCodigo(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("FalObservacion - constructor y validaciones")
    class ModeloValidaciones {

        @Test
        @DisplayName("texto vacio rechazado")
        void texto_vacio_rechazado() {
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "   ", null, FaltasClockTestSupport.FIXED.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto null rechazado")
        void texto_null_rechazado() {
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, null, null, FaltasClockTestSupport.FIXED.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto de 512 caracteres aceptado")
        void texto_limite_1000() {
            String texto = "A".repeat(512);
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, texto, null, FaltasClockTestSupport.FIXED.now(), "U");
            assertThat(obs.getObservacion()).hasSize(512);
        }

        @Test
        @DisplayName("texto de 513 caracteres rechazado")
        void texto_supera_1000_rechazado() {
            String texto = "A".repeat(513);
            assertThatThrownBy(() -> new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, texto, null, FaltasClockTestSupport.FIXED.now(), "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("texto con espacios se hace trim")
        void texto_trim() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "  hola  ", null, FaltasClockTestSupport.FIXED.now(), "U");
            assertThat(obs.getObservacion()).isEqualTo("hola");
        }

        @Test
        @DisplayName("siActiva=true al crear")
        void activa_al_crear() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "texto", null, FaltasClockTestSupport.FIXED.now(), "U");
            assertThat(obs.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("copia produce objeto independiente")
        void copia_independiente() {
            FalObservacion obs = new FalObservacion(1L, EntidadTipoObservada.ACTA, 1L,
                    null, "texto", OrigenObservacion.USUARIO, FaltasClockTestSupport.FIXED.now(), "U");
            FalObservacion copia = obs.copia();
            copia.setSiActiva(false);
            assertThat(obs.isSiActiva()).isTrue();
            assertThat(copia.isSiActiva()).isFalse();
        }
    }

    // =====================================================================
    // WAVE 1A2-B0: transicion de identidad (DECISION_ID-01)
    // =====================================================================

    @Nested
    @DisplayName("WAVE 1A2-B0: identidad de FalObservacion")
    class IdentidadModelo {

        @Test
        @DisplayName("1. Una observacion nueva admite id nulo")
        void nuevaAdmiteIdNulo() {
            FalObservacion obs = nueva(EntidadTipoObservada.ACTA, 7L, "texto", T0);

            assertThat(obs.getId()).isNull();
            assertThat(obs.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("2. conId devuelve una copia con la PK y no muta el original")
        void conIdNoMutaElOriginal() {
            FalObservacion nueva = nueva(EntidadTipoObservada.ACTA, 7L, "texto", T0);

            FalObservacion persistida = nueva.conId(42L);

            assertThat(persistida.getId()).isEqualTo(42L);
            assertThat(nueva.getId()).as("el original conserva id nulo").isNull();
            assertThat(persistida.getObservacion()).isEqualTo("texto");
            assertThat(persistida.getEntidadTipo()).isEqualTo(EntidadTipoObservada.ACTA);
            assertThat(persistida.getEntidadId()).isEqualTo(7L);
            assertThat(persistida.getFhAlta()).isEqualTo(T0);
            assertThat(persistida.getIdUserAlta()).isEqualTo("U");
        }

        @Test
        @DisplayName("3. conId rechaza null y valores no positivos")
        void conIdValidaElValor() {
            FalObservacion obs = nueva(EntidadTipoObservada.ACTA, 7L, "texto", T0);

            assertThatThrownBy(() -> obs.conId(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> obs.conId(0L)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> obs.conId(-1L)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("4. conId preserva el estado de baja de una fila inactiva")
        void conIdPreservaLaBaja() {
            FalObservacion obs = nueva(EntidadTipoObservada.ACTA, 7L, "texto", T0);
            obs.desactivar(T0.plusDays(1), ACTOR_BAJA);

            FalObservacion copia = obs.conId(9L);

            assertThat(copia.isSiActiva()).isFalse();
            assertThat(copia.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(copia.getIdUserBaja()).isEqualTo(ACTOR_BAJA);
        }

        @Test
        @DisplayName("5. El puerto ya no expone un generador de identificadores")
        void puertoSinGeneradorDeIds() {
            boolean tieneNextId = java.util.Arrays.stream(ObservacionRepository.class.getMethods())
                    .anyMatch(m -> m.getName().equals("nextId"));

            assertThat(tieneNextId)
                    .as("ObservacionRepository no debe declarar nextId(): DECISION_ID-01")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("WAVE 1A2-B0: alta en InMemoryObservacionRepository")
    class AltaInMemory {

        @Test
        @DisplayName("6. Un alta con id nulo recibe identidad y el argumento no se muta")
        void altaAsignaIdSinMutarElArgumento() {
            FalObservacion candidata = nueva(EntidadTipoObservada.ACTA, 1L, "primera", T0);

            FalObservacion persistida = repo.guardar(candidata);

            assertThat(persistida.getId()).isNotNull().isPositive();
            assertThat(candidata.getId()).as("el argumento conserva id nulo").isNull();
            assertThat(repo.buscarPorId(persistida.getId())).isPresent();
        }

        @Test
        @DisplayName("7. Dos altas reciben identificadores distintos")
        void dosAltasIdsDistintos() {
            Long primera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "una", T0)).getId();
            Long segunda = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "otra", T0)).getId();

            assertThat(segunda).isNotNull().isNotEqualTo(primera);
        }

        @Test
        @DisplayName("8. Un id presente inexistente no se inserta con la PK del llamador")
        void idPresenteInexistenteNoSeInserta() {
            FalObservacion conIdElegido = nueva(EntidadTipoObservada.ACTA, 1L, "texto", T0).conId(9999L);

            assertThatThrownBy(() -> repo.guardar(conIdElegido))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no se edita");
            assertThat(repo.buscarPorId(9999L)).isEmpty();
            assertThat(repo.size()).isZero();
        }

        @Test
        @DisplayName("9. Un id presente existente tampoco reescribe la fila: append-only")
        void idPresenteExistenteNoReescribe() {
            FalObservacion persistida = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "original", T0));
            FalObservacion intento = new FalObservacion(persistida.getId(), EntidadTipoObservada.ACTA, 1L,
                    null, "texto reescrito", OrigenObservacion.USUARIO, T0, "U");

            assertThatThrownBy(() -> repo.guardar(intento))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(repo.buscarPorId(persistida.getId()).orElseThrow().getObservacion())
                    .isEqualTo("original");
            assertThat(repo.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("WAVE 1A2-B0: orden canonico y filtro de vigencia")
    class OrdenYFiltro {

        @Test
        @DisplayName("10. El historial se ordena por fh_alta ASC")
        void ordenPorFhAlta() {
            Long tardia = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "tardia", T0.plusHours(2))).getId();
            Long temprana = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "temprana", T0)).getId();
            Long media = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "media", T0.plusHours(1))).getId();

            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getId)
                    .containsExactly(temprana, media, tardia);
        }

        @Test
        @DisplayName("11. Con el mismo fh_alta el id desempata de forma determinista")
        void mismoInstanteDesempataPorId() {
            Long primera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "a", T0)).getId();
            Long segunda = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "b", T0)).getId();
            Long tercera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "c", T0)).getId();

            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, segunda, tercera);
        }

        @Test
        @DisplayName("12. La primera fila del historial es la observacion inicial del propietario")
        void primeraFilaEsLaInicial() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "posterior", T0.plusMinutes(30)));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "inicial del labrado", T0));

            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .first()
                    .extracting(FalObservacion::getObservacion)
                    .isEqualTo("inicial del labrado");
        }

        @Test
        @DisplayName("13. El propietario aisla: (tipo, entidadId) no mezcla entidades homonimas")
        void propietarioPolimorficoAisla() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "del acta 1", T0));
            repo.guardar(nueva(EntidadTipoObservada.PARALIZACION, 1L, "de la paralizacion 1", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "del acta 2", T0));

            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getObservacion)
                    .containsExactly("del acta 1");
        }

        @Test
        @DisplayName("14. listarActivasPorEntidad excluye las dadas de baja y conserva el orden")
        void filtroDeActivas() {
            Long primera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "vigente", T0)).getId();
            Long segunda = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "se da de baja", T0.plusMinutes(1))).getId();
            Long tercera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "tambien vigente", T0.plusMinutes(2))).getId();

            repo.desactivar(segunda, T0.plusDays(1), ACTOR_BAJA);

            assertThat(repo.listarActivasPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, tercera);
            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .as("el historial conserva la fila inactiva")
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, segunda, tercera);
        }
    }

    @Nested
    @DisplayName("WAVE 1A2-B0: baja logica coherente")
    class BajaLogica {

        @Test
        @DisplayName("15. La baja informa siempre si_activa, fh_baja e id_user_baja")
        void bajaCoherente() {
            Long id = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "texto", T0)).getId();

            FalObservacion baja = repo.desactivar(id, T0.plusDays(1), ACTOR_BAJA);

            assertThat(baja.isSiActiva()).isFalse();
            assertThat(baja.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(baja.getIdUserBaja()).isEqualTo(ACTOR_BAJA);
        }

        @Test
        @DisplayName("16. La baja no reescribe el texto ni los datos de alta")
        void bajaPreservaTextoYAlta() {
            Long id = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "texto original", T0)).getId();

            repo.desactivar(id, T0.plusDays(1), ACTOR_BAJA);
            FalObservacion leida = repo.buscarPorId(id).orElseThrow();

            assertThat(leida.getObservacion()).isEqualTo("texto original");
            assertThat(leida.getEntidadTipo()).isEqualTo(EntidadTipoObservada.ACTA);
            assertThat(leida.getEntidadId()).isEqualTo(1L);
            assertThat(leida.getOrigenObs()).isEqualTo(OrigenObservacion.USUARIO);
            assertThat(leida.getFhAlta()).isEqualTo(T0);
            assertThat(leida.getIdUserAlta()).isEqualTo("U");
        }

        @Test
        @DisplayName("17. Un id inexistente se rechaza con la excepcion funcional y no inserta")
        void bajaDeIdInexistente() {
            assertThatThrownBy(() -> repo.desactivar(4242L, T0, ACTOR_BAJA))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessage("Observacion no encontrada: 4242");
            assertThat(repo.size()).isZero();
        }

        @Test
        @DisplayName("18. Una segunda baja se rechaza y no altera los datos de la primera")
        void segundaBajaRechazada() {
            Long id = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "texto", T0)).getId();
            repo.desactivar(id, T0.plusDays(1), ACTOR_BAJA);

            assertThatThrownBy(() -> repo.desactivar(id, T0.plusDays(5), "otro-actor"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La observacion ya esta inactiva: id=" + id);

            FalObservacion leida = repo.buscarPorId(id).orElseThrow();
            assertThat(leida.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(leida.getIdUserBaja()).isEqualTo(ACTOR_BAJA);
        }

        @Test
        @DisplayName("19. La correccion append-only es baja logica mas fila nueva")
        void correccionAppendOnly() {
            Long original = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "dato erroneo", T0)).getId();

            repo.desactivar(original, T0.plusDays(1), ACTOR_BAJA);
            Long correccion = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "dato corregido", T0.plusDays(1))).getId();

            assertThat(repo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getId)
                    .containsExactly(original, correccion);
            assertThat(repo.listarActivasPorEntidad(EntidadTipoObservada.ACTA, 1L))
                    .extracting(FalObservacion::getObservacion)
                    .containsExactly("dato corregido");
        }

        @Test
        @DisplayName("20. La baja exige actor y fecha: el modelo rechaza una baja incompleta")
        void bajaIncompletaRechazada() {
            Long id = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "texto", T0)).getId();

            assertThatThrownBy(() -> repo.desactivar(id, null, ACTOR_BAJA))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repo.desactivar(id, T0, "  "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(repo.buscarPorId(id).orElseThrow().isSiActiva())
                    .as("una baja rechazada deja la fila vigente")
                    .isTrue();
        }
    }

    /**
     * Contrato batch del puerto (DECISION-OBS-BATCH-01) sobre el adaptador InMemory, que
     * es el oraculo de paridad. La contraparte contra el motor real vive en
     * JdbcObservacionRepositoryIT.
     */
    @Nested
    @DisplayName("WAVE 1A2-B1.5C: contrato batch de listarPorEntidades")
    class BatchPorEntidades {

        @Test
        @DisplayName("21. Una coleccion vacia devuelve un mapa vacio")
        void coleccionVaciaDevuelveMapaVacio() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "existe", T0));

            assertThat(repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of())).isEmpty();
        }

        @Test
        @DisplayName("22. Varios propietarios se resuelven en una sola operacion")
        void variosPropietariosEnUnaOperacion() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "del acta 1", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "del acta 2", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 3L, "del acta 3", T0));

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L, 2L, 3L));

            assertThat(historiales).containsOnlyKeys(1L, 2L, 3L);
            assertThat(textos(historiales.get(1L))).containsExactly("del acta 1");
            assertThat(textos(historiales.get(2L))).containsExactly("del acta 2");
            assertThat(textos(historiales.get(3L))).containsExactly("del acta 3");
        }

        @Test
        @DisplayName("23. Ids repetidos en la entrada no duplican filas ni grupos")
        void idsRepetidosNoDuplican() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "unica", T0));

            Map<Long, List<FalObservacion>> historiales = repo.listarPorEntidades(
                    EntidadTipoObservada.ACTA, List.of(1L, 1L, 1L, 1L));

            assertThat(historiales).hasSize(1);
            assertThat(historiales.get(1L)).hasSize(1);
        }

        @Test
        @DisplayName("24. Se excluyen las observaciones de otro EntidadTipoObservada")
        void excluyeOtroTipoDePropietario() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "del acta", T0));
            repo.guardar(nueva(EntidadTipoObservada.PARALIZACION, 1L, "de la paralizacion", T0));
            repo.guardar(nueva(EntidadTipoObservada.DOCUMENTO, 1L, "del documento", T0));

            assertThat(textos(repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)).get(1L)))
                    .containsExactly("del acta");
        }

        @Test
        @DisplayName("25. Se excluyen las observaciones de ids no solicitados")
        void excluyeIdsNoSolicitados() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "pedida", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "no pedida", T0));

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L));

            assertThat(historiales).containsOnlyKeys(1L);
            assertThat(textos(historiales.get(1L))).containsExactly("pedida");
        }

        /**
         * La observacion inicial del labrado es la primera historica y sigue siendolo
         * aunque despues se haya dado de baja: el batch no filtra por si_activa.
         */
        @Test
        @DisplayName("26. Se incluyen activas e inactivas: la inicial dada de baja sigue siendo la primera")
        void incluyeActivasEInactivas() {
            Long inicial = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "inicial", T0)).getId();
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "correccion", T0.plusDays(1)));
            repo.desactivar(inicial, T0.plusDays(1), ACTOR_BAJA);

            List<FalObservacion> historial =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)).get(1L);

            assertThat(textos(historial)).containsExactly("inicial", "correccion");
            assertThat(historial.get(0).isSiActiva())
                    .as("la primera fila del historial esta dada de baja y sigue siendo la inicial")
                    .isFalse();
        }

        @Test
        @DisplayName("27. Cada historial conserva el orden fh_alta ASC por propietario")
        void ordenPorFhAltaEnCadaPropietario() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "1-tardia", T0.plusHours(2)));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "2-tardia", T0.plusHours(5)));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "1-temprana", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "2-temprana", T0.plusHours(1)));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "1-media", T0.plusHours(1)));

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L, 2L));

            assertThat(textos(historiales.get(1L)))
                    .containsExactly("1-temprana", "1-media", "1-tardia");
            assertThat(textos(historiales.get(2L)))
                    .containsExactly("2-temprana", "2-tardia");
        }

        @Test
        @DisplayName("28. Con el mismo fh_alta el id desempata dentro de cada propietario")
        void mismoInstanteDesempataPorId() {
            Long primera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "a", T0)).getId();
            Long segunda = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "b", T0)).getId();
            Long tercera = repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "c", T0)).getId();

            assertThat(repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)).get(1L))
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, segunda, tercera);
        }

        /**
         * La ausencia es la unica representacion del historial vacio: el puerto no
         * sintetiza claves que la persistencia no devolvio.
         */
        @Test
        @DisplayName("29. Un propietario sin filas esta ausente del mapa y se interpreta como historial vacio")
        void propietarioSinFilasAusenteDelMapa() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "solo la 1", T0));

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L, 2L, 3L));

            assertThat(historiales).containsOnlyKeys(1L);
            assertThat(historiales.getOrDefault(2L, List.of())).isEmpty();
            assertThat(historiales.getOrDefault(3L, List.of())).isEmpty();
        }

        @Test
        @DisplayName("30. El batch devuelve copias defensivas: mutar el resultado no altera el almacen")
        void devuelveCopiasDefensivas() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "vigente", T0));

            FalObservacion copia =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)).get(1L).get(0);
            copia.desactivar(T0.plusDays(1), ACTOR_BAJA);

            FalObservacion releida =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)).get(1L).get(0);
            assertThat(releida.isSiActiva())
                    .as("la baja aplicada sobre la copia no llega al almacen")
                    .isTrue();
            assertThat(releida).isNotSameAs(copia);
        }

        @Test
        @DisplayName("31. El batch coincide con listarPorEntidad propietario por propietario")
        void paridadConLaLecturaIndividual() {
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "1-b", T0.plusHours(1)));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 1L, "1-a", T0));
            repo.guardar(nueva(EntidadTipoObservada.ACTA, 2L, "2-a", T0));

            Map<Long, List<FalObservacion>> historiales =
                    repo.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L, 2L, 3L));

            for (Long id : List.of(1L, 2L, 3L)) {
                assertThat(textos(historiales.getOrDefault(id, List.of())))
                        .as("historial del propietario %s", id)
                        .isEqualTo(textos(repo.listarPorEntidad(EntidadTipoObservada.ACTA, id)));
            }
        }

        @Test
        @DisplayName("32. Tipo nulo, coleccion nula e id nulo se rechazan con IllegalArgumentException")
        void argumentosNulosRechazados() {
            assertThatThrownBy(() -> repo.listarPorEntidades(null, List.of(1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("tipo es obligatorio");
            assertThatThrownBy(() -> repo.listarPorEntidades(EntidadTipoObservada.ACTA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("entidadIds es obligatoria");
            assertThatThrownBy(() -> repo.listarPorEntidades(
                    EntidadTipoObservada.ACTA, java.util.Arrays.asList(1L, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("entidadIds no admite ids nulos");
        }

        private List<String> textos(List<FalObservacion> historial) {
            return historial.stream().map(FalObservacion::getObservacion).toList();
        }
    }

    @Nested
    @DisplayName("ObservacionService - operaciones")
    class ServicioOperaciones {

        @Test
        @DisplayName("agregar observacion para ACTA existente")
        void agregar_acta_existente() {
            FalActa acta = crearActa();
            FalObservacion obs = service.agregar(EntidadTipoObservada.ACTA, acta.getId(),
                    "Observacion de prueba", OrigenObservacion.USUARIO, "U");
            assertThat(obs.getId()).isNotNull();
            assertThat(obs.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("tipo no disponible rechazado explicitamente")
        void tipo_no_disponible_rechazado() {
            assertThatThrownBy(() -> service.agregar(EntidadTipoObservada.PERSONA, 1L,
                    "texto", OrigenObservacion.USUARIO, "U"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no disponible");
        }

        @Test
        @DisplayName("acta inexistente rechazada")
        void acta_inexistente_rechazada() {
            assertThatThrownBy(() -> service.agregar(EntidadTipoObservada.ACTA, 9999L,
                    "texto", OrigenObservacion.USUARIO, "U"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("alta + desactivacion preserva historial")
        void alta_desactivacion_historial() {
            FalActa acta = crearActa();
            FalObservacion obs = service.agregar(EntidadTipoObservada.ACTA, acta.getId(),
                    "texto", OrigenObservacion.SISTEMA, "U");
            service.desactivar(obs.getId(), ACTOR_BAJA);

            List<FalObservacion> todas = service.listarPorEntidad(EntidadTipoObservada.ACTA, acta.getId());
            List<FalObservacion> activas = service.listarActivasPorEntidad(EntidadTipoObservada.ACTA, acta.getId());
            assertThat(todas).hasSize(1);
            assertThat(activas).isEmpty();
        }

        @Test
        @DisplayName("listarActivasPorEntidad filtra correctamente")
        void listar_activas() {
            FalActa acta = crearActa();
            service.agregar(EntidadTipoObservada.ACTA, acta.getId(), "activa", OrigenObservacion.USUARIO, "U");
            FalObservacion obs2 = service.agregar(EntidadTipoObservada.ACTA, acta.getId(), "a desactivar", OrigenObservacion.USUARIO, "U");
            service.desactivar(obs2.getId(), ACTOR_BAJA);

            assertThat(service.listarActivasPorEntidad(EntidadTipoObservada.ACTA, acta.getId())).hasSize(1);
        }

        @Test
        @DisplayName("agregarSinValidarExistencia acepta entidad nueva sin validar")
        void agregar_sin_validar_existencia() {
            FalObservacion obs = service.agregarSinValidarExistencia(
                    EntidadTipoObservada.PARALIZACION, 999L, "texto", OrigenObservacion.SISTEMA, "S");
            assertThat(obs.isSiActiva()).isTrue();
        }

        /**
         * El servicio es la fuente del reloj de baja; el puerto y el adaptador solo
         * persisten lo que reciben.
         */
        @Test
        @DisplayName("la baja del servicio toma fh_baja del reloj y el actor del parametro")
        void baja_usa_reloj_y_actor() {
            FalActa acta = crearActa();
            FalObservacion obs = service.agregar(EntidadTipoObservada.ACTA, acta.getId(),
                    "texto", OrigenObservacion.USUARIO, "U");

            FalObservacion baja = service.desactivar(obs.getId(), ACTOR_BAJA);

            assertThat(baja.isSiActiva()).isFalse();
            assertThat(baja.getFhBaja()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(baja.getIdUserBaja()).isEqualTo(ACTOR_BAJA);
        }

        @Test
        @DisplayName("la baja de una observacion inexistente se rechaza con la excepcion funcional")
        void baja_inexistente_rechazada() {
            assertThatThrownBy(() -> service.desactivar(7777L, ACTOR_BAJA))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessage("Observacion no encontrada: 7777");
        }
    }
}
