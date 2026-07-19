package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionIntentoService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryLoteCorreoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.PlazosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrencia cruzada CMD-FALLO-004: ordinario (NotificacionService.registrarPositiva) versus
 * portal (NotificacionIntentoService.registrarPortalPositivo) sobre la misma FalNotificacion.
 *
 * Verifica que el monitor compartido ResultadoPositivoInMemoryMonitor.INSTANCE serializa ambos
 * caminos: exactamente un ganador, exactamente un perdedor (PrecondicionVioladaException),
 * exactamente una llamada al reloj de resultado positivo, y ningun evento/intento/snapshot
 * duplicado independientemente de quien gane.
 *
 * Prueba de comportamiento: no compara identidades de objeto ni usa reflexion.
 * El escenario es pieza previa (ACTA_INFRACCION) para aislar el resultado positivo del
 * camino de fallo, garantizando CIERRA=0.
 */
@DisplayName("CMD-FALLO-004 concurrencia cruzada: ordinario vs portal")
class NotificacionResultadoPositivoConcurrenciaCruzadaTest {

    private static final String ACTOR_ORD     = "inspector-01";
    private static final String ACTOR_POR     = "infractor-01";
    private static final String DESTINO_PORTAL = "CUIT-20123456780";
    private static final LocalDate FECHA_ORIGEN = LocalDate.of(2026, 7, 10);
    private static final Instant INSTANTE = Instant.parse("2026-07-10T12:00:00Z");

    private FaltasClock relojFijo;
    private CountingClock relojCompartido;

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryFalloActaRepository falloRepo;
    private InMemoryLoteCorreoRepository loteRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private NotificacionIntentoService intentoService;

    private int contadorNroActa;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        relojFijo = new FaltasClock(Clock.fixed(INSTANTE, FaltasClock.ZONE));
        relojCompartido = new CountingClock(relojFijo);
        contadorNroActa = 60_000_000;

        actaRepo     = new InMemoryActaRepository();
        eventoRepo   = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo      = new InMemoryDocumentoRepository();
        notifRepo    = new InMemoryNotificacionRepository();
        intentoRepo  = new InMemoryNotificacionIntentoRepository();
        falloRepo    = new InMemoryFalloActaRepository();
        loteRepo     = new InMemoryLoteCorreoRepository();

        var firmaRepo       = new InMemoryDocumentoFirmaRepository();
        var pagoRepo        = new InMemoryPagoVoluntarioRepository();
        var apelacionRepo   = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo, relojFijo, snapshotRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), relojFijo);

        TalonarioService talonarioService = new TalonarioService(
                new InMemoryTalonarioRepository(), new InMemoryDependenciaRepository(),
                new InMemoryInspectorRepository(), relojFijo);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                new InMemoryDocumentoPlantillaRepository(), talonarioService,
                new InMemoryDependenciaRepository(), new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(), new InMemoryNotificacionRepository(), relojFijo);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), relojCompartido,
                intentoRepo, new InMemoryPersonaDomicilioRepository(),
                PlazosTestSupport.conCalendarioVacio(relojFijo));

        intentoService = new NotificacionIntentoService(
                intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc,
                loteRepo, relojCompartido, falloRepo, docRepo,
                new NoOpBloqueantesMaterialesChecker(),
                PlazosTestSupport.conCalendarioVacio(relojFijo));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    /**
     * Repetido 5 veces para aumentar la probabilidad de observar ambos ordenes de llegada.
     * El resultado debe ser identico independientemente de quien gane.
     */
    @RepeatedTest(5)
    @DisplayName("ordinario vs portal: 1 exito, 1 PrecondicionVioladaException, 1 tick de reloj; CIERRA=0; snapshot PENDIENTE_ANALISIS/DICTAR_FALLO")
    void ordinario_vs_portal_exactamente_un_ganador() throws Exception {
        Fixture fx = piezaPrevia();
        Long intentoId = intentoRepo.buscarPorNotificacion(fx.idNotif).get(0).getId();

        RegistrarNotificacionPositivaCommand cmdOrd = new RegistrarNotificacionPositivaCommand(
                fx.idNotif, intentoId, null, ACTOR_ORD);

        relojCompartido.calls = 0;

        CyclicBarrier barrier  = new CyclicBarrier(2);
        AtomicInteger exitos   = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();

        Callable<Void> ordinario = () -> {
            barrier.await();
            try {
                notifService.registrarPositiva(cmdOrd);
                exitos.incrementAndGet();
            } catch (PrecondicionVioladaException e) {
                rechazos.incrementAndGet();
            }
            return null;
        };

        Callable<Void> portalCallable = () -> {
            barrier.await();
            try {
                intentoService.registrarPortalPositivo(
                        fx.idNotif, fx.idActa, DESTINO_PORTAL, ACTOR_POR);
                exitos.incrementAndGet();
            } catch (PrecondicionVioladaException e) {
                rechazos.incrementAndGet();
            }
            return null;
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Void> f1 = pool.submit(ordinario);
            Future<Void> f2 = pool.submit(portalCallable);
            f1.get();
            f2.get();
        } finally {
            pool.shutdownNow();
        }

        // Exactamente un ganador, un perdedor, un tick de reloj de resultado positivo
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazos.get()).isEqualTo(1);
        assertThat(relojCompartido.calls).isEqualTo(1);

        // Cabecera en estado positivo final
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);

        // Exactamente un resultado positivo efectivo; no dos intentos positivos
        List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(fx.idNotif);
        long positivos = intentos.stream()
                .filter(i -> i.getResultadoIntento() == ResultadoNotificacion.POSITIVO).count();
        assertThat(positivos).isEqualTo(1);

        // NOTPOS + PORPOS = 1; nunca ambos simultaneamente
        List<?> notposEvt = eventosDe(fx.idActa, TipoEventoActa.NOTPOS);
        List<?> porposEvt = eventosDe(fx.idActa, TipoEventoActa.PORPOS);
        assertThat(notposEvt.size() + porposEvt.size()).isEqualTo(1);
        assertThat(notposEvt.isEmpty() || porposEvt.isEmpty()).isTrue();

        // CIERRA = 0 (pieza previa)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();

        // Acta en ANAL, no cerrada
        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(acta.estaCerrada()).isFalse();

        // Snapshot exacto: bandeja PENDIENTE_ANALISIS, accion DICTAR_FALLO
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);

        // Intento original y portal segun quien gano
        boolean ganoOrdinario = !notposEvt.isEmpty();
        boolean ganoPortal    = !porposEvt.isEmpty();
        assertThat(ganoOrdinario ^ ganoPortal).isTrue();

        FalNotificacionIntento intentoOriginal = intentoRepo.buscarPorId(intentoId).orElseThrow();
        long portalInfractorCount = intentos.stream()
                .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR).count();

        if (ganoOrdinario) {
            assertThat(intentoOriginal.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(intentoOriginal.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
            assertThat(portalInfractorCount).isZero();
        } else {
            assertThat(intentoOriginal.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
            assertThat(intentoOriginal.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
            assertThat(portalInfractorCount).isEqualTo(1);
            FalNotificacionIntento portalIntento = intentos.stream()
                    .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR)
                    .findFirst().orElseThrow();
            assertThat(portalIntento.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(portalIntento.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        }
    }

    // =========================================================================
    // Fixture
    // =========================================================================

    private record Fixture(Long idActa, Long idNotif) {}

    private Fixture piezaPrevia() {
        String nro = String.valueOf(++contadorNroActa);
        Long idActa = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001", FECHA_ORIGEN, "Av. Argentina 123", "San Martin 456",
                null, null, null, "Maria Lopez", nro,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));

        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Acta principal"))
                .idEntidadAfectada();
        docService.firmarDocumento(
                new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));

        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL,
                "test@malvinas.gob.ar", null, null, "seed"))
                .idEntidadAfectada();

        return new Fixture(idActa, Long.parseLong(idNotif));
    }

    private List<?> eventosDe(Long idActa, TipoEventoActa tipo) {
        return eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == tipo)
                .toList();
    }

    // =========================================================================
    // Reloj compartido con contador de llamadas al camino de resultado positivo
    // =========================================================================

    private static final class CountingClock extends FaltasClock {
        volatile int calls = 0;
        private final FaltasClock delegate;

        CountingClock(FaltasClock delegate) {
            super(delegate.clock());
            this.delegate = delegate;
        }

        @Override
        public LocalDateTime now() {
            calls++;
            return delegate.now();
        }
    }
}
