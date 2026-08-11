package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de la frontera transaccional de {@code ActaService.enriquecer}
 * ({@code POST-B2-TX-CONSUMERS-01}).
 *
 * <p>El alta es integral y el acta nace directamente en {@code ENRI}
 * ({@code DECISION-ACTA-LIFECYCLE-SIN-CAPT-01}); {@code CompletarCaptura} y {@code CAPT} ya no
 * existen. Este IT conserva la certificacion transaccional de {@code enriquecer}: antes de la
 * wave que introdujo {@code @Transactional} sus escrituras (registrar el evento y recalcular el
 * snapshot) commiteaban por separado. Este IT verifica lo que un test unitario no puede: que
 * ahora ocurren dentro de una unica transaccion real contra MariaDB, con rollback total ante
 * cualquier fallo posterior a la primera escritura.
 *
 * <p>Mismo motivo que {@code ActaLabrarTransaccionalIT} para no envolver los escenarios en una
 * transaccion de test: si el test abriera una, la del comando se uniria por propagacion
 * {@code REQUIRED} y nunca commitearia. Cada caso deja que el comando commitee o revierta por su
 * cuenta, verifica despues del retorno y limpia en {@code @AfterEach}.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@Import(ActaCicloVidaTransaccionalIT.DecoradoresDeFallo.class)
@DisplayName("IT POST-B2-TX-CONSUMERS-01: frontera transaccional de enriquecer")
class ActaCicloVidaTransaccionalIT {

    private static final String MARCA = "POSTB2TX_";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 4, 12, 0, 0);
    private static final LocalDate F0 = LocalDate.of(2026, 8, 4);
    private static final String FALLO = "FALLO_INYECTADO_POSTB2TX";

    enum Etapa { NINGUNA, ACTA, EVENTO, SNAPSHOT }

    static volatile Etapa fallaEn = Etapa.NINGUNA;

    @Autowired
    private ActaService actaService;

    @Autowired
    private ActaRepository actaRepository;

    @Autowired
    private JdbcClient jdbc;

    private Long idDependencia;

    @BeforeEach
    void prepararPadresYActor() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.set(new ActorContext(UUID.randomUUID().toString()));
        insertarDependencia();
    }

    @AfterEach
    void limpiar() {
        fallaEn = Etapa.NINGUNA;
        ActorContextHolder.clear();
        borrarResiduos();
    }

    private static String actor() {
        return ActorContextHolder.subOr("SYS");
    }

    private void insertarDependencia() {
        jdbc.sql("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """)
                .param("nom", MARCA + "DEP")
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
        idDependencia = jdbc.sql("SELECT MAX(id) FROM fal_dependencia WHERE nom_dep = :nom")
                .param("nom", MARCA + "DEP")
                .query(Long.class)
                .single();
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                VALUES (:idDep, 1, :tipoActa, :vigDesde, :fhAlta, :usuario)
                """)
                .param("idDep", idDependencia)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", T0)
                .param("usuario", actor())
                .update();
    }

    private void borrarResiduos() {
        List<Long> actas = jdbc.sql("SELECT id FROM fal_acta WHERE id_dep = :idDep")
                .param("idDep", idDependencia).query(Long.class).list();
        for (Long idActa : actas) {
            jdbc.sql("DELETE FROM fal_acta_snapshot WHERE id_acta = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta_evento WHERE acta_id = :id").param("id", idActa).update();
            jdbc.sql("DELETE FROM fal_acta WHERE id = :id").param("id", idActa).update();
        }
        jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :idDep")
                .param("idDep", idDependencia).update();
        jdbc.sql("DELETE FROM fal_dependencia WHERE id = :idDep")
                .param("idDep", idDependencia).update();
    }

    /**
     * Crea el acta seed <b>antes</b> de que el test active {@code fallaEn}: en ese momento el
     * decorador de {@code ActaRepository} delega sin interferir, de modo que esta creacion nunca
     * es la escritura que un caso de rollback observa.
     */
    private Long crearActaEnBloque(BloqueActual bloque) {
        FalActa nueva = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO,
                idDependencia, null, F0, T0,
                MARCA + "domicilio", null, null, null,
                ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE,
                SituacionIdentificacionInfractor.AUSENTE, null, T0, actor());
        nueva.setCodigoQr(MARCA + "QR-" + UUID.randomUUID());
        nueva.setQrPayloadVersion(0);
        nueva.setBloqueActual(bloque);
        return actaRepository.guardar(nueva).getId();
    }

    private String bloqueDe(Long idActa) {
        return jdbc.sql("SELECT bloque_actual FROM fal_acta WHERE id = :id")
                .param("id", idActa).query(String.class).single().trim();
    }

    private long contarEventos(Long idActa, String tipoEvt) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM fal_acta_evento WHERE acta_id = :id AND tipo_evt = :tipo
                """).param("id", idActa).param("tipo", tipoEvt).query(Long.class).single();
    }

    private long contarSnapshots(Long idActa) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_acta_snapshot WHERE id_acta = :id")
                .param("id", idActa).query(Long.class).single();
    }

    // =====================================================================
    @Nested
    @DisplayName("enriquecer")
    class Enriquecer {

        @Test
        @DisplayName("9. Exito: se registra ACTENR y se crea el snapshot, bloque permanece ENRI")
        void exitoCompleto() {
            Long idActa = crearActaEnBloque(BloqueActual.ENRI);

            ComandoResultado r = actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "obs"));

            assertThat(r.idActa()).isEqualTo(idActa);
            assertThat(bloqueDe(idActa)).isEqualTo("ENRI");
            assertThat(contarEventos(idActa, "ACTENR")).isEqualTo(1L);
            assertThat(contarSnapshots(idActa)).isEqualTo(1L);
        }

        @Test
        @DisplayName("10. Precondicion invalida: acta cerrada no muta nada")
        void precondicionActaCerrada() {
            Long idActa = crearActaEnBloque(BloqueActual.ENRI);
            jdbc.sql("UPDATE fal_acta SET sit_adm_act = :sit WHERE id = :id")
                    .param("sit", SituacionAdministrativaActa.CERRADA.codigo())
                    .param("id", idActa)
                    .update();

            assertThatThrownBy(() -> actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "obs")))
                    .isInstanceOf(PrecondicionVioladaException.class);

            assertThat(contarEventos(idActa, "ACTENR")).isZero();
            assertThat(contarSnapshots(idActa)).isZero();
        }

        @Test
        @DisplayName("11. Precondicion invalida: bloque distinto de ENRI no muta nada")
        void precondicionBloqueIncorrecto() {
            Long idActa = crearActaEnBloque(BloqueActual.NOTI);

            assertThatThrownBy(() -> actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "obs")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ENRI");

            assertThat(contarEventos(idActa, "ACTENR")).isZero();
        }

        @Test
        @DisplayName("12. Fallo al registrar ACTENR: cero mutaciones")
        void falloEnEventoNoMutaNada() {
            Long idActa = crearActaEnBloque(BloqueActual.ENRI);
            fallaEn = Etapa.EVENTO;

            assertThat(catchThrowable(() -> actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "obs"))))
                    .hasMessageContaining(FALLO);

            assertThat(contarEventos(idActa, "ACTENR")).isZero();
            assertThat(contarSnapshots(idActa)).isZero();
        }

        @Test
        @DisplayName("13. Fallo al guardar el snapshot: el evento ACTENR tambien se revierte")
        void falloEnSnapshotRevierteElEvento() {
            Long idActa = crearActaEnBloque(BloqueActual.ENRI);
            fallaEn = Etapa.SNAPSHOT;

            assertThat(catchThrowable(() -> actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "obs"))))
                    .hasMessageContaining(FALLO);

            assertThat(contarEventos(idActa, "ACTENR"))
                    .as("el evento ya insertado se revierte junto con el fallo del snapshot")
                    .isZero();
            assertThat(contarSnapshots(idActa)).isZero();
        }

        @Test
        @DisplayName("14. Despues de un rollback, el comando siguiente commitea normalmente")
        void despuesDeRollbackElComandoSiguienteFunciona() {
            Long idActa = crearActaEnBloque(BloqueActual.ENRI);
            fallaEn = Etapa.SNAPSHOT;
            assertThat(catchThrowable(() -> actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "primero"))))
                    .hasMessageContaining(FALLO);

            fallaEn = Etapa.NINGUNA;
            ComandoResultado ok = actaService.enriquecer(
                    new EnriquecerActaCommand(idActa, MARCA + "segundo"));

            assertThat(ok.idActa()).isEqualTo(idActa);
            assertThat(contarEventos(idActa, "ACTENR")).isEqualTo(1L);
            assertThat(contarSnapshots(idActa)).isEqualTo(1L);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Concurrencia: sin interbloqueo entre comandos sobre actas distintas")
    class Concurrencia {

        @Test
        @DisplayName("16. Cuatro enriquecer concurrentes sobre actas distintas, sin interbloqueo")
        void enriquecerConcurrente() throws Exception {
            int hilos = 4;
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                ids.add(crearActaEnBloque(BloqueActual.ENRI));
            }

            List<Throwable> fallos = ejecutarEnParalelo(hilos, indice ->
                    actaService.enriquecer(
                            new EnriquecerActaCommand(ids.get(indice), MARCA + "conc-" + indice)));

            exigirSinInterbloqueo(fallos);
            assertThat(fallos).isEmpty();
            for (Long idActa : ids) {
                assertThat(contarEventos(idActa, "ACTENR")).isEqualTo(1L);
                assertThat(contarSnapshots(idActa)).isEqualTo(1L);
            }
        }

        private void exigirSinInterbloqueo(List<Throwable> fallos) {
            for (Throwable fallo : fallos) {
                assertThat(fallo)
                        .as("ningun fallo puede ser un interbloqueo")
                        .isNotInstanceOf(DeadlockLoserDataAccessException.class)
                        .isNotInstanceOf(CannotAcquireLockException.class);
                assertThat(String.valueOf(fallo.getMessage()).toLowerCase(Locale.ROOT))
                        .doesNotContain("deadlock")
                        .doesNotContain("lock wait timeout");
            }
        }

        private List<Throwable> ejecutarEnParalelo(int n, java.util.function.IntConsumer accion)
                throws Exception {
            String sub = actor();
            CyclicBarrier barrera = new CyclicBarrier(n);
            ExecutorService pool = Executors.newFixedThreadPool(n);
            try {
                List<Future<Throwable>> futuros = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int indice = i;
                    Callable<Throwable> tarea = () -> {
                        ActorContextHolder.set(new ActorContext(sub));
                        try {
                            barrera.await(20, TimeUnit.SECONDS);
                            accion.accept(indice);
                            return null;
                        } catch (Throwable t) {
                            return t;
                        } finally {
                            ActorContextHolder.clear();
                        }
                    };
                    futuros.add(pool.submit(tarea));
                }
                List<Throwable> fallos = new ArrayList<>();
                for (Future<Throwable> futuro : futuros) {
                    Throwable t = futuro.get(60, TimeUnit.SECONDS);
                    if (t != null) fallos.add(t);
                }
                return fallos;
            } finally {
                pool.shutdownNow();
            }
        }
    }

    // =====================================================================
    // DECORADORES DE FALLO
    // =====================================================================

    @TestConfiguration
    static class DecoradoresDeFallo {
        @org.springframework.context.annotation.Bean
        static org.springframework.beans.factory.config.BeanPostProcessor fallosControlados() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof ActaRepository r) return new ActaConFallo(r);
                    if (bean instanceof ActaEventoRepository r) return new EventoConFallo(r);
                    if (bean instanceof ActaSnapshotRepository r) return new SnapshotConFallo(r);
                    return bean;
                }
            };
        }
    }

    private static void fallarSi(Etapa etapa) {
        if (fallaEn == etapa) {
            throw new IllegalStateException(FALLO + " en etapa " + etapa);
        }
    }

    private record ActaConFallo(ActaRepository delegado) implements ActaRepository {
        @Override public FalActa guardar(FalActa acta) {
            FalActa persistida = delegado.guardar(acta);
            fallarSi(Etapa.ACTA);
            return persistida;
        }
        @Override public Optional<FalActa> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public Optional<FalActa> buscarPorUuidTecnico(String uuid) {
            return delegado.buscarPorUuidTecnico(uuid);
        }
        @Override public Optional<FalActa> buscarPorUuidTecnicoConLock(String uuid) {
            return delegado.buscarPorUuidTecnicoConLock(uuid);
        }
        @Override public Optional<FalActa> buscarPorIdConLock(Long id) {
            return delegado.buscarPorIdConLock(id);
        }
        @Override public List<FalActa> listarTodas() { return delegado.listarTodas(); }
        @Override public ar.gob.malvinas.faltas.core.application.query.PaginaResultado<FalActa>
                listarPagina(ar.gob.malvinas.faltas.core.application.query.FiltroActa filtro,
                             ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada<
                                     ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa> pagina) {
            return delegado.listarPagina(filtro, pagina);
        }
    }

    private record EventoConFallo(ActaEventoRepository delegado) implements ActaEventoRepository {
        @Override public FalActaEvento registrar(FalActaEvento evento) {
            FalActaEvento persistido = delegado.registrar(evento);
            fallarSi(Etapa.EVENTO);
            return persistido;
        }
        @Override public List<FalActaEvento> buscarPorActa(Long idActa) {
            return delegado.buscarPorActa(idActa);
        }
        @Override public Optional<FalActaEvento> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public boolean existeCorrelacion(Long idActa, String correlacionId) {
            return delegado.existeCorrelacion(idActa, correlacionId);
        }
    }

    private record SnapshotConFallo(ActaSnapshotRepository delegado) implements ActaSnapshotRepository {
        @Override public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
            FalActaSnapshot persistido = delegado.guardar(snapshot);
            fallarSi(Etapa.SNAPSHOT);
            return persistido;
        }
        @Override public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
            return delegado.buscarPorActa(idActa);
        }
    }
}
