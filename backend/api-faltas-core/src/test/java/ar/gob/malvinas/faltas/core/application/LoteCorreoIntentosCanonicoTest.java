package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.GenerarLoteCorreoCommand;
import ar.gob.malvinas.faltas.core.application.service.LoteCorreoService;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * CMD-FALLO-003 - 20 tests canonicos sin Mockito.
 *
 * CountingClock controla LoteCorreoService; FaltasClockTestSupport.FIXED
 * controla SnapshotRecalculador (relojes desacoplados por contrato).
 */
@DisplayName("CMD-FALLO-003: LoteCorreoIntentosCanonicoTest")
class LoteCorreoIntentosCanonicoTest {

    private static final String ACTOR = "test-batch";
    private static final Instant BASE  = Instant.parse("2026-07-09T18:00:00Z");

    private InMemoryActaRepository          actaRepo;
    private InMemoryActaEventoRepository    eventoRepo;
    private InMemoryActaSnapshotRepository  snapshotRepo;
    private InMemoryNotificacionRepository  notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryLoteCorreoRepository    loteRepo;
    private InMemoryPersonaDomicilioRepository domicilioRepo;
    private LoteCorreoService               loteService;
    private CountingClock                   clock;
    private SnapshotRecalculador            snapshotRecalc;

    @BeforeEach
    void setUp() {
        actaRepo     = new InMemoryActaRepository();
        eventoRepo   = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        notifRepo    = new InMemoryNotificacionRepository();
        intentoRepo  = new InMemoryNotificacionIntentoRepository();
        loteRepo     = new InMemoryLoteCorreoRepository();
        domicilioRepo = new InMemoryPersonaDomicilioRepository();
        clock        = CountingClock.startingAt(BASE);

        var docRepo      = new InMemoryDocumentoRepository();
        var pagoVolRepo  = new InMemoryPagoVoluntarioRepository();
        var falloRepo    = new InMemoryFalloActaRepository();
        var apelRepo     = new InMemoryApelacionActaRepository();
        var pagoCondRepo = new InMemoryPagoCondenaRepository();

        snapshotRecalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelRepo, pagoCondRepo,
                FaltasClockTestSupport.FIXED, snapshotRepo);

        loteService = new LoteCorreoService(
                loteRepo, notifRepo, intentoRepo, actaRepo, eventoRepo,
                snapshotRepo, snapshotRecalc, domicilioRepo, clock);
    }

    private FalActa crearActaConDomicilio(Long actaId) {
        Long domId = domicilioRepo.nextId();
        domicilioRepo.guardar(new FalPersonaDomicilio(
                domId, 1L, null,
                TipoDomicilio.CONSTITUIDO, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                true, true, true,
                null, null, null, null, null,
                null, null, null, null,
                "Av. Libertad 100", 100, false,
                null, null, "Av. Libertad 100, Malvinas",
                null, false, null, null, null,
                FaltasClockTestSupport.FIXED.now(), ACTOR));
        FalActa acta = new FalActa(actaId, "UUID-" + actaId, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 9), FaltasClockTestSupport.FIXED.now(),
                "Av. Libertad 100", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), ACTOR);
        acta.setBloqueActual(BloqueActual.ANAL);
        acta.setIdDomicilioNotifAct(domId);
        return actaRepo.guardar(acta);
    }

    private FalNotificacion crearNotifPendiente(Long notifId, Long actaId, Long docId) {
        return notifRepo.guardar(FalNotificacion.preparar(
                notifId, actaId, docId, TipoDocu.NOTIFICACION_ACTA,
                FaltasClockTestSupport.FIXED.now(), ACTOR));
    }

    private GenerarLoteCorreoCommand cmd(String codigo) {
        return new GenerarLoteCorreoCommand(codigo, null, null, ACTOR);
    }

    @Test
    @DisplayName("T01: una notif -> lote GENERADO, intento postal, domicilio, EN_PROCESO, NOTI, LOTGEN, snapshot")
    void t01_unaNotificacion() {
        FalActa acta = crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);

        FalLoteCorreo lote = loteService.generarLoteDesdePendientes(cmd("LOT-T01"));

        // Lote
        assertThat(clock.invocationCount()).isEqualTo(1);
        LocalDateTime ahora = clock.nthInstant(0);
        assertThat(lote.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);
        assertThat(lote.getLoteCodigo()).isEqualTo("LOT-T01");
        assertThat(lote.getFhGeneracion()).isEqualTo(ahora);
        assertThat(lote.getFhAlta()).isEqualTo(ahora);
        assertThat(lote.getIdUserAlta()).isEqualTo(ACTOR);

        // Intento
        List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(10L);
        assertThat(intentos).hasSize(1);
        FalNotificacionIntento intento = intentos.get(0);
        assertThat(intento.getCanalNotif()).isEqualTo(CanalNotificacion.CORREO_POSTAL);
        assertThat(intento.getDomicilioNotifId()).isEqualTo(acta.getIdDomicilioNotifAct());
        assertThat(intento.getNroIntento()).isEqualTo((short) 1);
        assertThat(intento.getLoteId()).isEqualTo(lote.getId());
        assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(intento.getResultadoIntento()).isNull();
        assertThat(intento.getDestinoDigital()).isNull();
        assertThat(intento.getReferenciaExterna()).isNull();
        assertThat(intento.getFhIntento()).isEqualTo(ahora);
        assertThat(intento.getFhAlta()).isEqualTo(ahora);
        assertThat(intento.getIdUserAlta()).isEqualTo(ACTOR);

        // Cabecera (FalNotificacion)
        FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(notif.getResultado()).isNull();
        assertThat(notif.getCanal()).isEqualTo("CORREO_POSTAL");
        assertThat(notif.getIntentos()).isEqualTo(1);
        assertThat(notif.getFechaEnvio()).isEqualTo(ahora);
        assertThat(notif.getFhUltMod()).isEqualTo(ahora);
        assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

        // Acta ANAL -> NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);

        // Exactamente un LOTGEN
        List<FalActaEvento> eventos = eventoRepo.buscarPorActa(1L);
        assertThat(eventos.stream().filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);
        FalActaEvento lotgen = eventos.stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).findFirst().orElseThrow();
        assertThat(lotgen.fhEvt()).isEqualTo(ahora);
        assertThat(lotgen.idUserEvt()).isEqualTo(ACTOR);

        // Snapshot exacto
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
    }

    @Test
    @DisplayName("T02: dos notifs misma acta -> dos intentos, un LOTGEN, un snapshot")
    void t02_dosNotifsMismaActa() {
        crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);
        crearNotifPendiente(20L, 1L, 20L);

        loteService.generarLoteDesdePendientes(cmd("LOT-T02"));

        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
        assertThat(intentoRepo.buscarPorNotificacion(20L)).hasSize(1);

        // Acta ANAL -> NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);

        long lotgenCount = eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count();
        assertThat(lotgenCount).isEqualTo(1);

        // Snapshot exacto
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
    }

    @Test
    @DisplayName("T03: notifs de dos actas -> un LOTGEN y snapshot por acta")
    void t03_dosActasDistintas() {
        crearActaConDomicilio(1L);
        crearActaConDomicilio(2L);
        crearNotifPendiente(10L, 1L, 10L);
        crearNotifPendiente(20L, 2L, 20L);

        FalLoteCorreo lote = loteService.generarLoteDesdePendientes(cmd("LOT-T03"));

        // Cada acta ANAL -> NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);
        assertThat(actaRepo.buscarPorId(2L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);

        assertThat(eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);
        assertThat(eventoRepo.buscarPorActa(2L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);

        // Snapshots exactos
        FalActaSnapshot snap1 = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap1.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap1.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
        FalActaSnapshot snap2 = snapshotRepo.buscarPorActa(2L).orElseThrow();
        assertThat(snap2.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap2.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);

        // Dos intentos totales y un solo lote
        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
        assertThat(intentoRepo.buscarPorNotificacion(20L)).hasSize(1);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);
    }

    @Test
    @DisplayName("T04: intento previo nro=1 -> nuevo intento nro=2, contador cabecera incrementado una vez")
    void t04_intentoPrevioExistente() {
        FalActa acta = crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);
        intentoRepo.guardar(new FalNotificacionIntento(
                99L, 10L, (short) 1, CanalNotificacion.PRESENCIAL,
                acta.getIdDomicilioNotifAct(), null, null, null,
                FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), ACTOR));

        FalLoteCorreo lote = loteService.generarLoteDesdePendientes(cmd("LOT-T04"));

        FalNotificacionIntento nuevoIntento = intentoRepo.buscarPorNotificacion(10L).stream()
                .filter(i -> i.getCanalNotif() == CanalNotificacion.CORREO_POSTAL)
                .findFirst().orElseThrow();
        assertThat(nuevoIntento.getNroIntento()).isEqualTo((short) 2);
        assertThat(nuevoIntento.getLoteId()).isEqualTo(lote.getId());
        assertThat(notifRepo.buscarPorId(10L).orElseThrow().getIntentos()).isEqualTo(1);

        // Acta ANAL -> NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);
        assertThat(eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);

        // Snapshot exacto
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
    }

    @Test
    @DisplayName("T05: CountingClock.calls=1, mismo ahora en lote/intentos/cabeceras/eventos, actor identico")
    void t05_instanteYActorUnicos() {
        crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);

        FalLoteCorreo lote = loteService.generarLoteDesdePendientes(cmd("LOT-T05"));

        assertThat(clock.invocationCount()).isEqualTo(1);
        LocalDateTime ahora = clock.nthInstant(0);

        assertThat(lote.getFhGeneracion()).isEqualTo(ahora);
        assertThat(lote.getFhAlta()).isEqualTo(ahora);
        assertThat(lote.getIdUserAlta()).isEqualTo(ACTOR);

        FalNotificacionIntento intento = intentoRepo.buscarPorNotificacion(10L).get(0);
        assertThat(intento.getFhIntento()).isEqualTo(ahora);
        assertThat(intento.getFhAlta()).isEqualTo(ahora);
        assertThat(intento.getIdUserAlta()).isEqualTo(ACTOR);

        FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
        assertThat(notif.getFechaEnvio()).isEqualTo(ahora);
        assertThat(notif.getFhUltMod()).isEqualTo(ahora);
        assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

        FalActaEvento lotgen = eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN)
                .findFirst().orElseThrow();
        assertThat(lotgen.fhEvt()).isEqualTo(ahora);
        assertThat(lotgen.idUserEvt()).isEqualTo(ACTOR);

        // Snapshot exacto
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
    }

    @Test
    @DisplayName("T06: actorTecnico blanco -> excepcion, reloj=0, sin efectos")
    void t06_actorBlanco() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("LOT-T06", null, null, "  ")))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
    }

    @Test
    @DisplayName("T07: loteCodigo blanco -> excepcion, reloj=0, sin efectos")
    void t07_loteCodigoBlanco() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("   ", null, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
    }

    @Test
    @DisplayName("T08: loteCodigo mayor a 30 -> excepcion, reloj=0")
    void t08_loteCodigoLargo() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("L".repeat(31), null, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("T09: referenciaExterna blanca -> excepcion, reloj=0")
    void t09_refExtBlanca() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("LOT-T09", "   ", null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("T10: referenciaExterna mayor a 60 -> excepcion, reloj=0")
    void t10_refExtLarga() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("LOT-T10", "R".repeat(61), null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("T11: guidLoteExt invalido -> excepcion, reloj=0")
    void t11_guidInvalido() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                new GenerarLoteCorreoCommand("LOT-T11", null, "no-es-uuid", ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("T12: sin notificaciones PENDIENTE_ENVIO -> excepcion, reloj=0, sin lote")
    void t12_sinPendientes() {
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T12")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("PENDIENTE_ENVIO");
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
    }

    @Test
    @DisplayName("T13: notif PENDIENTE_ENVIO con resultado no nulo -> excepcion, reloj=0, sin efectos")
    void t13_notifConResultado() {
        crearActaConDomicilio(1L);
        FalNotificacion notif = FalNotificacion.preparar(10L, 1L, 10L, TipoDocu.NOTIFICACION_ACTA,
                FaltasClockTestSupport.FIXED.now(), ACTOR);
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notifRepo.guardar(notif);

        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T13")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("ya tiene resultado");
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
        assertThat(intentoRepo.buscarPorNotificacion(10L)).isEmpty();
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.ANAL);
    }

    @Test
    @DisplayName("T14: acta asociada inexistente -> excepcion, reloj=0, sin efectos")
    void t14_actaInexistente() {
        notifRepo.guardar(FalNotificacion.preparar(10L, 999L, 10L, TipoDocu.NOTIFICACION_ACTA,
                FaltasClockTestSupport.FIXED.now(), ACTOR));
        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T14")))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
        assertThat(intentoRepo.buscarPorNotificacion(10L)).isEmpty();
    }

    @Test
    @DisplayName("T15: idDomicilioNotifAct nulo en acta -> excepcion, reloj=0, sin efectos")
    void t15_domicilioNulo() {
        FalActa actaSinDom = new FalActa(1L, "UUID-1", TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 9), FaltasClockTestSupport.FIXED.now(),
                "Av. Libertad 100", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), ACTOR);
        actaSinDom.setBloqueActual(BloqueActual.ANAL);
        actaRepo.guardar(actaSinDom);
        crearNotifPendiente(10L, 1L, 10L);

        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T15")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("domicilioNotifAct");
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
        assertThat(intentoRepo.buscarPorNotificacion(10L)).isEmpty();
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(eventoRepo.buscarPorActa(1L)).isEmpty();
        assertThat(snapshotRepo.buscarPorActa(1L)).isEmpty();
    }

    @Test
    @DisplayName("T16: domicilio referenciado inexistente -> excepcion, reloj=0, sin efectos")
    void t16_domicilioReferenciadoInexistente() {
        FalActa actaDomBad = new FalActa(1L, "UUID-1", TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 9), FaltasClockTestSupport.FIXED.now(),
                "Av. Libertad 100", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), ACTOR);
        actaDomBad.setBloqueActual(BloqueActual.ANAL);
        actaDomBad.setIdDomicilioNotifAct(9999L);
        actaRepo.guardar(actaDomBad);
        crearNotifPendiente(10L, 1L, 10L);

        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T16")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("9999");
        assertThat(clock.invocationCount()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
        assertThat(intentoRepo.buscarPorNotificacion(10L)).isEmpty();
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(eventoRepo.buscarPorActa(1L)).isEmpty();
        assertThat(snapshotRepo.buscarPorActa(1L)).isEmpty();
    }

    @Test
    @DisplayName("T17: loteCodigo duplicado secuencial -> LoteCodigoDuplicadoException, sin efectos extras")
    void t17_loteCodigoDuplicadoSecuencial() {
        crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);
        loteService.generarLoteDesdePendientes(cmd("LOT-T17"));
        int clockAfterFirst = clock.invocationCount();

        crearActaConDomicilio(2L);
        crearNotifPendiente(20L, 2L, 20L);

        assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(cmd("LOT-T17")))
                .isInstanceOf(LoteCodigoDuplicadoException.class);

        assertThat(clock.invocationCount()).isEqualTo(clockAfterFirst);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);
        assertThat(intentoRepo.buscarPorNotificacion(20L)).isEmpty();
        assertThat(eventoRepo.buscarPorActa(2L)).isEmpty();
        assertThat(snapshotRepo.buscarPorActa(2L)).isEmpty();
        // Segunda acta permanece ANAL
        assertThat(actaRepo.buscarPorId(2L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        // Notificacion permanece PENDIENTE_ENVIO
        assertThat(notifRepo.buscarPorId(20L).orElseThrow().getEstado())
                .isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
    }

    @Test
    @DisplayName("T18: dos ejecuciones concurrentes mismo loteCodigo -> 1 exito, 1 LoteCodigoDuplicadoException, reloj=1")
    void t18_concurrenciaMismoCodigo() throws InterruptedException {
        crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);
        crearActaConDomicilio(2L);
        crearNotifPendiente(20L, 2L, 20L);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(2);

        AtomicInteger okCount     = new AtomicInteger();
        AtomicInteger dupCount    = new AtomicInteger();
        AtomicInteger otherErrors = new AtomicInteger();

        Runnable tarea = () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                loteService.generarLoteDesdePendientes(cmd("LOT-T18"));
                okCount.incrementAndGet();
            } catch (LoteCodigoDuplicadoException e) {
                dupCount.incrementAndGet();
            } catch (Exception e) {
                otherErrors.incrementAndGet();
            } finally {
                done.countDown();
            }
        };

        new Thread(tarea).start();
        new Thread(tarea).start();
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        assertThat(okCount.get()).isEqualTo(1);
        assertThat(dupCount.get()).isEqualTo(1);
        assertThat(otherErrors.get()).isEqualTo(0);
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);

        // Exactamente dos intentos totales, uno por notificacion
        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
        assertThat(intentoRepo.buscarPorNotificacion(20L)).hasSize(1);

        // Cada cabecera EN_PROCESO, intentos=1
        assertThat(notifRepo.buscarPorId(10L).orElseThrow().getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(notifRepo.buscarPorId(10L).orElseThrow().getIntentos()).isEqualTo(1);
        assertThat(notifRepo.buscarPorId(20L).orElseThrow().getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(notifRepo.buscarPorId(20L).orElseThrow().getIntentos()).isEqualTo(1);

        // Cada acta NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);
        assertThat(actaRepo.buscarPorId(2L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);

        // Exactamente un LOTGEN por acta
        assertThat(eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);
        assertThat(eventoRepo.buscarPorActa(2L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);

        // Exactamente un snapshot por acta, exacto
        FalActaSnapshot snap1 = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap1.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap1.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
        FalActaSnapshot snap2 = snapshotRepo.buscarPorActa(2L).orElseThrow();
        assertThat(snap2.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap2.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);

        // Reloj invocado exactamente una vez: el perdedor detecta duplicado antes del reloj
        assertThat(clock.invocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("T19: dos codigos diferentes concurrentes -> 1 exito, 1 PrecondicionVioladaException, reloj=1")
    void t19_concurrenciaCodigosDiferentes() throws InterruptedException {
        crearActaConDomicilio(1L);
        crearNotifPendiente(10L, 1L, 10L);
        crearActaConDomicilio(2L);
        crearNotifPendiente(20L, 2L, 20L);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(2);

        AtomicInteger okCount     = new AtomicInteger();
        AtomicInteger precondCount = new AtomicInteger();
        AtomicInteger dupCount    = new AtomicInteger();
        AtomicInteger otherErrors = new AtomicInteger();

        Runnable tareaA = () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                loteService.generarLoteDesdePendientes(cmd("LOT-A"));
                okCount.incrementAndGet();
            } catch (PrecondicionVioladaException e) {
                precondCount.incrementAndGet();
            } catch (LoteCodigoDuplicadoException e) {
                dupCount.incrementAndGet();
            } catch (Exception e) {
                otherErrors.incrementAndGet();
            } finally {
                done.countDown();
            }
        };

        Runnable tareaB = () -> {
            try {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                loteService.generarLoteDesdePendientes(cmd("LOT-B"));
                okCount.incrementAndGet();
            } catch (PrecondicionVioladaException e) {
                precondCount.incrementAndGet();
            } catch (LoteCodigoDuplicadoException e) {
                dupCount.incrementAndGet();
            } catch (Exception e) {
                otherErrors.incrementAndGet();
            } finally {
                done.countDown();
            }
        };

        new Thread(tareaA).start();
        new Thread(tareaB).start();
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        // Exactamente 1 exito, 1 PrecondicionVioladaException, 0 LoteCodigoDuplicadoException
        assertThat(okCount.get()).isEqualTo(1);
        assertThat(precondCount.get()).isEqualTo(1);
        assertThat(dupCount.get()).isEqualTo(0);
        assertThat(otherErrors.get()).isEqualTo(0);

        // Exactamente 1 lote total
        assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);

        // Exactamente 2 intentos totales, 1 por notificacion
        assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
        assertThat(intentoRepo.buscarPorNotificacion(20L)).hasSize(1);

        // Cada cabecera EN_PROCESO, intentos=1
        assertThat(notifRepo.buscarPorId(10L).orElseThrow().getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(notifRepo.buscarPorId(10L).orElseThrow().getIntentos()).isEqualTo(1);
        assertThat(notifRepo.buscarPorId(20L).orElseThrow().getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        assertThat(notifRepo.buscarPorId(20L).orElseThrow().getIntentos()).isEqualTo(1);

        // Cada acta NOTI
        assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);
        assertThat(actaRepo.buscarPorId(2L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.NOTI);

        // Exactamente un LOTGEN por acta
        assertThat(eventoRepo.buscarPorActa(1L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);
        assertThat(eventoRepo.buscarPorActa(2L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.LOTGEN).count()).isEqualTo(1);

        // Exactamente un snapshot por acta, EN_NOTIFICACION / EVALUAR_NOTIFICACION
        FalActaSnapshot snap1 = snapshotRepo.buscarPorActa(1L).orElseThrow();
        assertThat(snap1.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap1.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
        FalActaSnapshot snap2 = snapshotRepo.buscarPorActa(2L).orElseThrow();
        assertThat(snap2.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
        assertThat(snap2.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);

        // Reloj invocado exactamente una vez: el perdedor no llega al reloj
        assertThat(clock.invocationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("T20: precheck obsoleto y lista vacia conserva LoteCodigoDuplicadoException")
    void t20_precheckObsoletoListaVaciaConservaDuplicado() {
        // Subclase local: simula precheck obsoleto para LOT-T20.
        // Primera llamada a existeCodigo -> false (lectura inicial stale de otra instancia).
        // Llamadas siguientes -> comportamiento real del repositorio.
        class RepoConPrecheckObsoleto extends InMemoryLoteCorreoRepository {
            private int existeCodigoCalls = 0;

            @Override
            public boolean existeCodigo(String codigo) {
                existeCodigoCalls++;
                if (existeCodigoCalls == 1) return false;
                return super.existeCodigo(codigo);
            }
        }

        RepoConPrecheckObsoleto repoSimulado = new RepoConPrecheckObsoleto();

        // guardarSiAusentePorCodigo usa byCode.compute() internamente, no llama existeCodigo.
        // El contador queda en 0 despues del setup.
        FalLoteCorreo loteExistente = new FalLoteCorreo(
                repoSimulado.nextId(), "LOT-T20",
                FaltasClockTestSupport.FIXED.now(),
                FaltasClockTestSupport.FIXED.now(), ACTOR);
        repoSimulado.guardarSiAusentePorCodigo(loteExistente);

        CountingClock relojLocal = CountingClock.startingAt(BASE);
        LoteCorreoService servicioLocal = new LoteCorreoService(
                repoSimulado, notifRepo, intentoRepo, actaRepo, eventoRepo,
                snapshotRepo, snapshotRecalc, domicilioRepo, relojLocal);

        // Cero notificaciones PENDIENTE_ENVIO: la lista vacia activa el recheck.
        // El recheck encuentra LOT-T20 -> LoteCodigoDuplicadoException.
        assertThatThrownBy(() -> servicioLocal.generarLoteDesdePendientes(cmd("LOT-T20")))
                .isInstanceOf(LoteCodigoDuplicadoException.class);

        // El reloj no fue consultado: la excepcion se lanza antes de capturar ahora.
        assertThat(relojLocal.invocationCount()).isEqualTo(0);

        // Un solo lote total (el preexistente); ningun nuevo lote creado.
        assertThat(repoSimulado.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);

        // Cero intentos: ninguna notificacion fue procesada.
        // Cero LOTGEN y cero snapshots: aplicarEfectos nunca fue invocado.
        assertThat(eventoRepo.buscarPorActa(1L)).isEmpty();
        assertThat(snapshotRepo.buscarPorActa(1L)).isEmpty();
    }
}
