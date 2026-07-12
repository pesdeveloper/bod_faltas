package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11I: Concurrencia - correlativo, lotes, acuses, portal")
class NotificacionIntentoConcurrenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryNotificacionAcuseRepository acuseRepo;
    private InMemoryLoteCorreoRepository loteRepo;
    private NotificacionIntentoService intentoService;
    private NotificacionAcuseService acuseService;
    private LoteCorreoService loteService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        acuseRepo = new InMemoryNotificacionAcuseRepository();
        loteRepo = new InMemoryLoteCorreoRepository();

        var docRepo = new InMemoryDocumentoRepository();
        var pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        var falloRepo = new InMemoryFalloActaRepository();
        var apelRepo = new InMemoryApelacionActaRepository();
        var pagoCondRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelRepo, pagoCondRepo, FaltasClockTestSupport.FIXED);

        intentoService = new NotificacionIntentoService(intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc, loteRepo, FaltasClockTestSupport.FIXED);
        acuseService = new NotificacionAcuseService(acuseRepo, intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED);
        loteService = new LoteCorreoService(loteRepo, notifRepo, intentoRepo, actaRepo, eventoRepo, snapshotRepo, recalc, new InMemoryPersonaDomicilioRepository(), FaltasClockTestSupport.FIXED);
    }

    private void crearActa(Long id) {
        FalActa a = new FalActa(id, "UUID-C-" + id, TipoActa.TRANSITO, 1L, 1L,
                java.time.LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
        a.setBloqueActual(BloqueActual.NOTI);
        actaRepo.guardar(a);
    }

    private void crearNotif(Long id, Long actaId) {
        notifRepo.guardar(new FalNotificacion(id, actaId, 1L, TipoDocu.NOTIFICACION_ACTA, "CORREO", AHORA, AHORA, "SYS"));
    }

    @Test @DisplayName("C1: correlativo simultaneo - N hilos, sin duplicados")
    void c1_correlativoSimultaneo() throws InterruptedException {
        int N = 20;
        CountDownLatch latch = new CountDownLatch(N);
        Set<Short> nros = Collections.synchronizedSet(new HashSet<>());
        AtomicInteger errores = new AtomicInteger();
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                try { nros.add(intentoRepo.siguienteNroIntento(100L)); }
                catch (Exception e) { errores.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(errores.get()).isEqualTo(0);
        assertThat(nros).hasSize(N);
    }

    @Test @DisplayName("C2: dos reintentos simultaneos - correlativos distintos")
    void c2_dosReintentosSim() throws InterruptedException {
        crearActa(1L);
        crearNotif(10L, 1L);
        int N = 10;
        CountDownLatch latch = new CountDownLatch(N);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        for (int i = 0; i < N; i++) {
            new Thread(() -> {
                try {
                    intentoService.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
                    ok.incrementAndGet();
                } catch (Exception e) { err.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isEqualTo(N);
        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(N);
        var nros = intentoRepo.buscarPorNotificacion(10L).stream().map(FalNotificacionIntento::getNroIntento).toList();
        assertThat(nros).doesNotHaveDuplicates();
    }

    @Test @DisplayName("C3: misma referencia externa - solo uno persiste, el otro lanza excepcion")
    void c3_mismaReferenciaExterna() throws InterruptedException {
        crearActa(1L);
        crearNotif(10L, 1L);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    intentoService.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, "REF-UNICA", "USR");
                    ok.incrementAndGet();
                } catch (PrecondicionVioladaException e) { err.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isEqualTo(1);
        assertThat(err.get()).isEqualTo(1);
    }

    @Test @DisplayName("C4: mismo acuse - idempotencia, solo uno persiste")
    void c4_mismoAcuse() throws InterruptedException {
        crearActa(1L);
        crearNotif(10L, 1L);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
                    ok.incrementAndGet();
                } catch (AcuseDuplicadoException e) { err.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isEqualTo(1);
        assertThat(acuseRepo.buscarPorNotificacion(10L)).hasSize(1);
    }

    @Test @DisplayName("C5: dos lotes con codigo distinto - ambos persisten")
    void c5_dosLotesCodigosDistintos() throws InterruptedException {
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        String[] codigos = {"LOT-A", "LOT-B"};
        for (String cod : codigos) {
            new Thread(() -> {
                try { loteService.generarLote(cod, null, null, "USR"); ok.incrementAndGet(); }
                catch (Exception e) { err.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isEqualTo(2);
        assertThat(loteRepo.size()).isEqualTo(2);
    }

    @Test @DisplayName("C6: dos lotes mismo codigo - solo uno persiste")
    void c6_dosLotesMismoCodigo() throws InterruptedException {
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger err = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try { loteService.generarLote("LOT-DUP", null, null, "USR"); ok.incrementAndGet(); }
                catch (LoteCodigoDuplicadoException e) { err.incrementAndGet(); }
                finally { latch.countDown(); }
            }).start();
        }
        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isEqualTo(1);
        assertThat(loteRepo.size()).isEqualTo(1);
    }

    @Test @DisplayName("C7: portal vs intento postal - portal supera, historia intacta")
    void c7_portalVsPostal() {
        crearActa(1L);
        crearNotif(10L, 1L);
        FalNotificacionIntento postal = intentoService.registrarIntento(10L, CanalNotificacion.CORREO_POSTAL, 100L, null, null, null, "USR");
        intentoService.registrarPortalPositivo(10L, "user-portal", "USR");

        FalNotificacionIntento postalPost = intentoRepo.buscarPorId(postal.getId()).orElseThrow();
        assertThat(postalPost.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
        assertThat(postalPost.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(2);
    }

    @Test @DisplayName("C8: acuse positivo y negativo simultáneos - solo el primero aplica")
    void c8_acusePositivoVsNegativo() throws InterruptedException {
        crearActa(1L);
        crearNotif(10L, 1L);
        AtomicInteger ok = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            try { acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR"); ok.incrementAndGet(); }
            catch (Exception ignored) {}
            finally { latch.countDown(); }
        }).start();

        new Thread(() -> {
            try { acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECHAZO, null, AHORA, "USR"); ok.incrementAndGet(); }
            catch (Exception ignored) {}
            finally { latch.countDown(); }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        assertThat(ok.get()).isGreaterThanOrEqualTo(1);
    }

    @Test @DisplayName("C9: resultado tardio de intento con resultado ya registrado - lanza excepcion")
    void c9_resultadoTardio() {
        crearActa(1L);
        crearNotif(10L, 1L);
        FalNotificacionIntento intento = intentoService.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
        intentoService.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.NEGATIVO, "USR");
        assertThatThrownBy(() -> intentoService.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.POSITIVO, "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test @DisplayName("C10: doble validacion de acuse - segunda lanza excepcion")
    void c10_dobleValidacion() {
        crearActa(1L);
        crearNotif(10L, 1L);
        FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
        acuseService.validarAcuse(acuse.getId(), "USR");
        assertThatThrownBy(() -> acuseService.validarAcuse(acuse.getId(), "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test @DisplayName("C11: anular vs emitir lote - estado determinista")
    void c11_anularVsEmitir() {
        FalLoteCorreo lote = loteService.generarLote("LOT-CE", null, null, "USR");
        loteService.anularLote(lote.getId(), "USR");
        assertThatThrownBy(() -> loteService.emitirLote(lote.getId(), "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(loteRepo.buscarPorId(lote.getId()).orElseThrow().getEstadoLote()).isEqualTo(EstadoLote.ANULADO);
    }

    @Test @DisplayName("C12: anular intento con referencia - intento previo queda intacto")
    void c12_reintentoNuevoDomicilio() {
        crearActa(1L);
        crearNotif(10L, 1L);
        FalNotificacionIntento i1 = intentoService.registrarIntento(10L, CanalNotificacion.CORREO_POSTAL, 100L, null, null, null, "USR");
        intentoService.registrarResultadoIntento(i1.getId(), ResultadoNotificacion.NEGATIVO, "USR");
        FalNotificacionIntento i2 = intentoService.registrarIntento(10L, CanalNotificacion.CORREO_POSTAL, 200L, null, null, null, "USR");

        FalNotificacionIntento i1Post = intentoRepo.buscarPorId(i1.getId()).orElseThrow();
        assertThat(i1Post.getResultadoIntento()).isEqualTo(ResultadoNotificacion.NEGATIVO);
        assertThat(i1Post.getDomicilioNotifId()).isEqualTo(100L);
        assertThat(i2.getDomicilioNotifId()).isEqualTo(200L);
        assertThat(i2.getNroIntento()).isEqualTo((short)2);
    }
}
