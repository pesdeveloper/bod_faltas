package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.CalculadorPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionIntentoService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.PlazosAdministrativosService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.infrastructure.config.PlazosAdministrativosProperties;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CMD-FALLO-004 (variante portal): resultado notificatorio positivo via portal infractor.
 *
 * Cablea NotificacionIntentoService a mano (sin Mockito) con un reloj contador propio y un
 * reloj fijo separado para el SnapshotRecalculador. Verifica pieza previa, fallo condenatorio
 * con plazo de apelacion, fallo absolutorio con y sin bloqueantes, validaciones previas al
 * tiempo, atomicidad ante fallo de plazo y repeticion secuencial/concurrente.
 *
 * Nunca emite NOTPOS. Emite NOTSUP/PORPOS/CIERRA segun el orden normativo.
 * El instante canonico de test es 2026-07-10 (America/Argentina/Buenos_Aires).
 */
@DisplayName("CMD-FALLO-004 portal: resultado notificatorio positivo via portal infractor")
class NotificacionPortalPositivaCanonicaTest {

    private static final String ACTOR = "infractor-testuser";
    private static final String DESTINO = "CUIT-20123456780";
    private static final LocalDate FECHA_ORIGEN = LocalDate.of(2026, 7, 10);
    private static final Instant INSTANTE = Instant.parse("2026-07-10T12:00:00Z");

    private FaltasClock relojFijo;
    private CountingClock relojPortal;

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
    private FalloActaService falloService;
    private NotificacionService notifService;
    private NotificacionIntentoService intentoService;

    private int contadorNroActa;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        relojFijo = new FaltasClock(Clock.fixed(INSTANTE, FaltasClock.ZONE));
        relojPortal = new CountingClock(relojFijo);
        contadorNroActa = 70_000_000;

        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        falloRepo = new InMemoryFalloActaRepository();
        loteRepo = new InMemoryLoteCorreoRepository();

        var firmaRepo = new InMemoryDocumentoFirmaRepository();
        var pagoRepo = new InMemoryPagoVoluntarioRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
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
                falloRepo, new NoOpBloqueantesMaterialesChecker(), relojFijo,
                intentoRepo, new InMemoryPersonaDomicilioRepository(),
                PlazosTestSupport.conCalendarioVacio(relojFijo));

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoRepo, recalc, relojFijo);

        intentoService = new NotificacionIntentoService(
                intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc,
                loteRepo, relojPortal, falloRepo, docRepo,
                new NoOpBloqueantesMaterialesChecker(),
                PlazosTestSupport.conCalendarioVacio(relojFijo));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // =========================================================================
    // 1. Pieza previa con intento previo activo
    // =========================================================================

    @Test
    @DisplayName("01. Pieza previa con intento previo: NOTSUP+PORPOS; intento previo SUPERADA_POR_PORTAL; intento portal PORTAL_INFRACTOR; clock=1")
    void pieza_previa_con_intento_previo_emite_notsup_y_porpos() {
        Fixture fx = piezaPrevia();
        Long intentoPrevioId = intentoActivo(fx.idNotif);

        relojPortal.calls = 0;
        FalNotificacionIntento portal =
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);

        assertThat(relojPortal.calls).isEqualTo(1);

        // intento portal creado correctamente
        assertThat(portal.getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
        assertThat(portal.getDestinoDigital()).isEqualTo(DESTINO);
        assertThat(portal.getDomicilioNotifId()).isNull();
        assertThat(portal.getLoteId()).isNull();
        assertThat(portal.getReferenciaExterna()).isNull();
        assertThat(portal.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(portal.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        assertThat(portal.getFhResultado()).isEqualTo(relojFijo.now());
        assertThat(portal.getIdUserUltMod()).isEqualTo(ACTOR);

        // intento previo marcado SUPERADA_POR_PORTAL
        FalNotificacionIntento previo = intentoRepo.buscarPorId(intentoPrevioId).orElseThrow();
        assertThat(previo.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
        assertThat(previo.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
        assertThat(previo.getFhResultado()).isEqualTo(relojFijo.now());
        assertThat(previo.getIdUserUltMod()).isEqualTo(ACTOR);

        // cabecera notificacion positiva
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        assertThat(notif.getFechaResultado()).isEqualTo(relojFijo.now());
        assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

        // acta: ANAL, no cerrada, sin fallo activo
        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(acta.estaCerrada()).isFalse();
        assertThat(falloRepo.buscarActivo(fx.idActa)).isEmpty();

        // no NOTPOS ni CIERRA para pieza previa
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();

        // NOTSUP: exactamente uno, atributos portal
        List<FalActaEvento> notsups = eventosDe(fx.idActa, TipoEventoActa.NOTSUP);
        assertThat(notsups).hasSize(1);
        assertEventoPortal(notsups.get(0), fx.idNotif);

        // PORPOS: exactamente uno, atributos portal, referencia a la notificacion
        List<FalActaEvento> porpos = eventosDe(fx.idActa, TipoEventoActa.PORPOS);
        assertThat(porpos).hasSize(1);
        assertEventoPortal(porpos.get(0), fx.idNotif);

        // orden de insercion: NOTSUP antes de PORPOS
        List<FalActaEvento> todos = eventoRepo.buscarPorActa(fx.idActa);
        assertThat(indexOfLast(todos, TipoEventoActa.NOTSUP))
                .isLessThan(indexOfLast(todos, TipoEventoActa.PORPOS));

        // mismo instante canonico en ambos eventos
        assertThat(notsups.get(0).fhEvt()).isEqualTo(porpos.get(0).fhEvt());

        // snapshot
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
    }

    // =========================================================================
    // 2. Pieza previa sin intento previo activo
    // =========================================================================

    @Test
    @DisplayName("02. Pieza previa sin intento previo: solo PORPOS, sin NOTSUP; clock=1")
    void pieza_previa_sin_intento_previo_emite_solo_porpos() {
        Fixture fx = piezaPreviaSinIntentoPrevio();

        relojPortal.calls = 0;
        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);

        assertThat(relojPortal.calls).isEqualTo(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).isEmpty();
    }

    // =========================================================================
    // 3. Fallo condenatorio
    // =========================================================================

    @Test
    @DisplayName("03. Condenatorio default 30: 2026-07-10 -> 2026-08-14; fallo NOTIFICADO; NOTSUP+PORPOS; sin CIERRA; clock=1")
    void condenatorio_calcula_vto_apelacion_y_emite_eventos() {
        Fixture fx = condenatorio();
        Long intentoPrevioId = intentoActivo(fx.idNotif);

        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        relojPortal.calls = 0;
        FalNotificacionIntento portal =
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);

        assertThat(relojPortal.calls).isEqualTo(1);

        // fallo: NOTIFICADO, fhVtoApelacion calculado, sin firmeza
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
        assertThat(fallo.getFhNotificacion()).isEqualTo(relojFijo.now());
        assertThat(fallo.getFhVtoApelacion()).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(fallo.isSiFirme()).isFalse();

        // acta: ANAL, no cerrada
        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(acta.estaCerrada()).isFalse();

        // intento previo superado
        FalNotificacionIntento previo = intentoRepo.buscarPorId(intentoPrevioId).orElseThrow();
        assertThat(previo.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);

        // portal intento correcto
        assertThat(portal.getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
        assertThat(portal.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);

        // portal no emite NOTPOS (el del setup es del registrarPositiva de avanzarAAnalisis)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).hasSize(1);
        List<FalActaEvento> porpos = eventosDe(fx.idActa, TipoEventoActa.PORPOS);
        assertThat(porpos).hasSize(1);
        assertEventoPortal(porpos.get(0), fx.idNotif);

        // snapshot
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTES_FALLO);
    }

    // =========================================================================
    // 4. Fallo absolutorio sin bloqueantes
    // =========================================================================

    @Test
    @DisplayName("04. Absolutorio sin bloqueantes: ABSUELTO, CERRADA, NOTSUP+PORPOS+CIERRA en orden; snapshot CERRADAS; clock=1")
    void absolutorio_sin_bloqueantes_cierra() {
        Fixture fx = absolutorio(new NoOpBloqueantesMaterialesChecker());

        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        relojPortal.calls = 0;
        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);

        assertThat(relojPortal.calls).isEqualTo(1);

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
        assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        assertThat(acta.estaCerrada()).isTrue();

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
        assertThat(fallo.getFhVtoApelacion()).isNull();

        // portal no emite NOTPOS (el del setup es del registrarPositiva de avanzarAAnalisis)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).hasSize(1);

        // todos los eventos del flujo portal comparten exactamente el mismo instante
        LocalDateTime ahoraPortal = relojFijo.now();
        List<FalActaEvento> evtPortal = eventoRepo.buscarPorActa(fx.idActa).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.NOTSUP
                        || e.tipoEvt() == TipoEventoActa.PORPOS
                        || e.tipoEvt() == TipoEventoActa.CIERRA)
                .toList();
        assertThat(evtPortal).allMatch(e -> ahoraPortal.equals(e.fhEvt()));
        assertThat(evtPortal).allMatch(e -> OrigenEvento.PORTAL_INFRACTOR.equals(e.origenEvt()));
        assertThat(evtPortal).allMatch(e -> ActorTipoEvento.INFRACTOR.equals(e.actorTipo()));

        // orden de insercion: NOTSUP < PORPOS < CIERRA
        List<FalActaEvento> todos = eventoRepo.buscarPorActa(fx.idActa);
        assertThat(indexOfLast(todos, TipoEventoActa.NOTSUP))
                .isLessThan(indexOfLast(todos, TipoEventoActa.PORPOS));
        assertThat(indexOfLast(todos, TipoEventoActa.PORPOS))
                .isLessThan(indexOfLast(todos, TipoEventoActa.CIERRA));

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
    }

    // =========================================================================
    // 5. Fallo absolutorio con bloqueantes
    // =========================================================================

    @Test
    @DisplayName("05. Absolutorio con bloqueantes: ABSUELTO, no cierra, NOTSUP+PORPOS, cero CIERRA; clock=1")
    void absolutorio_con_bloqueantes_no_cierra() {
        BloqueantesMaterialesChecker conBloqueantes = actaId -> true;
        Fixture fx = absolutorio(new NoOpBloqueantesMaterialesChecker());

        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        NotificacionIntentoService svcConBloqueantes = svcCon(
                PlazosTestSupport.conCalendarioVacio(relojFijo), conBloqueantes);

        relojPortal.calls = 0;
        svcConBloqueantes.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);

        assertThat(relojPortal.calls).isEqualTo(1);

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
        assertThat(acta.estaCerrada()).isFalse();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getResultadoFallo()).isEqualTo(ResultadoFalloActa.ABSUELVE);

        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();
        // portal no emite NOTPOS (el del setup es del registrarPositiva de avanzarAAnalisis)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes);

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
    }

    // =========================================================================
    // 6. Normalizacion de actor y destino (trim)
    // =========================================================================

    @Test
    @DisplayName("06. Actor y destino con espacios: normalizados (trim) en intento, cabecera y eventos; clock=1")
    void actor_y_destino_con_espacios_normalizados() {
        Fixture fx = piezaPrevia();

        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, "  " + DESTINO + "  ", "  " + ACTOR + "  ");

        FalNotificacionIntento portal = intentoRepo.buscarPorNotificacion(fx.idNotif).stream()
                .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR)
                .findFirst().orElseThrow();
        assertThat(portal.getDestinoDigital()).isEqualTo(DESTINO);
        assertThat(portal.getIdUserUltMod()).isEqualTo(ACTOR);

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

        eventosDe(fx.idActa, TipoEventoActa.PORPOS)
                .forEach(e -> assertThat(e.idUserEvt()).isEqualTo(ACTOR));
    }

    // =========================================================================
    // 7..9. Validaciones de input estructural (clock=0)
    // =========================================================================

    @Test
    @DisplayName("07. notificacionId null -> PrecondicionVioladaException; clock=0")
    void notificacion_id_null() {
        relojPortal.calls = 0;
        assertThatThrownBy(() -> intentoService.registrarPortalPositivo(null, 1L, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    @Test
    @DisplayName("08. destinoPortal null/vacio/>120 -> PrecondicionVioladaException; clock=0")
    void destinoPortal_invalido() {
        Fixture fx = piezaPrevia();
        relojPortal.calls = 0;
        assertThatThrownBy(() -> intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, null, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() -> intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, "   ", ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, "x".repeat(121), ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    @Test
    @DisplayName("09. actor null/blanco/>36 -> PrecondicionVioladaException; clock=0")
    void actor_invalido() {
        Fixture fx = piezaPrevia();
        relojPortal.calls = 0;
        assertThatThrownBy(() -> intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, null))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() -> intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, "   "))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, "x".repeat(37)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    // =========================================================================
    // 10..15. Validaciones de dominio (clock=0)
    // =========================================================================

    @Test
    @DisplayName("10. Notificacion inexistente -> NotificacionNoEncontradaException; clock=0")
    void notificacion_inexistente() {
        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(999_999L, 1L, DESTINO, ACTOR))
                .isInstanceOf(NotificacionNoEncontradaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    @Test
    @DisplayName("11. Cabecera ya POSITIVA -> PrecondicionVioladaException; clock=0; sin efectos nuevos")
    void cabecera_ya_positiva_rechaza_sin_efectos() {
        Fixture fx = piezaPrevia();
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notifRepo.guardar(notif);

        int porposAntes = eventosDe(fx.idActa, TipoEventoActa.PORPOS).size();
        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(porposAntes);
    }

    @Test
    @DisplayName("12. Acta cerrada -> PrecondicionVioladaException; clock=0")
    void acta_cerrada() {
        Fixture fx = piezaPrevia();
        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
        actaRepo.guardar(acta);

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    @Test
    @DisplayName("13. Incoherencias de fallo (estado / tipo-resultado / fhVtoApelacion precargado) -> PrecondicionVioladaException; clock=0; cero efectos")
    void fallo_incoherencias() {
        // (a) estadoFallo incompatible (NOTIFICADO en lugar de PENDIENTE_NOTIFICACION)
        Fixture estadoMal = condenatorio();
        FalActaFallo falloEstado = falloRepo.buscarActivo(estadoMal.idActa).orElseThrow();
        falloEstado.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        falloRepo.guardar(falloEstado);
        assertRechazoSinEfectos(estadoMal);

        // (b) tipo CONDENATORIO + resultado ABSUELVE
        Fixture condAbsuelve = condenatorio();
        FalActaFallo falloCondAbsuelve = falloRepo.buscarActivo(condAbsuelve.idActa).orElseThrow();
        falloCondAbsuelve.setResultadoFallo(ResultadoFalloActa.ABSUELVE);
        falloRepo.guardar(falloCondAbsuelve);
        assertRechazoSinEfectos(condAbsuelve);

        // (c) tipo ABSOLUTORIO + resultado CONDENA
        Fixture absCondena = absolutorio(new NoOpBloqueantesMaterialesChecker());
        FalActaFallo falloAbsCondena = falloRepo.buscarActivo(absCondena.idActa).orElseThrow();
        falloAbsCondena.setResultadoFallo(ResultadoFalloActa.CONDENA);
        falloRepo.guardar(falloAbsCondena);
        assertRechazoSinEfectos(absCondena);

        // (d) fhVtoApelacion precargado en condenatorio
        Fixture condPrec = condenatorio();
        FalActaFallo falloPrec = falloRepo.buscarActivo(condPrec.idActa).orElseThrow();
        falloPrec.setFhVtoApelacion(LocalDate.of(2026, 8, 14));
        falloRepo.guardar(falloPrec);
        assertRechazoSinEfectos(condPrec);
        assertThat(falloRepo.buscarActivo(condPrec.idActa).orElseThrow().getFhVtoApelacion())
                .isEqualTo(LocalDate.of(2026, 8, 14)); // no mutado

        // (e) fhVtoApelacion precargado en absolutorio
        Fixture absPrec = absolutorio(new NoOpBloqueantesMaterialesChecker());
        FalActaFallo falloAbsPrec = falloRepo.buscarActivo(absPrec.idActa).orElseThrow();
        falloAbsPrec.setFhVtoApelacion(LocalDate.of(2026, 8, 14));
        falloRepo.guardar(falloAbsPrec);
        assertRechazoSinEfectos(absPrec);
    }

    @Test
    @DisplayName("14. Fallo sin fhFirma -> PrecondicionVioladaException; clock=0")
    void fallo_sin_firma() {
        Fixture fx = condenatorio();
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        fallo.setFhFirma(null);
        falloRepo.guardar(fallo);

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
    }

    @Test
    @DisplayName("15. Documento del fallo no firmado -> PrecondicionVioladaException; clock=0")
    void documento_fallo_no_firmado() {
        Fixture fx = condenatorio();
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        FalDocumento docFallo = docRepo.buscarPorId(fallo.getDocumentoId()).orElseThrow();
        docFallo.setEstadoDocu(ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.PENDIENTE_FIRMA);
        docRepo.guardar(docFallo);

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
        // fallo sin mutacion
        assertThat(falloRepo.buscarActivo(fx.idActa).orElseThrow().getFhNotificacion()).isNull();
    }

    // =========================================================================
    // 16. Atomicidad post-reloj: calculo de plazo incompatible (clock=1, cero efectos)
    // =========================================================================

    @Test
    @DisplayName("16. Condenatorio: calculo incompatible (lanza / null / origen distinto) -> IllegalStateException; clock=1; cero efectos")
    void condenatorio_calculo_incompatible_cero_efectos() {
        // (a) el servicio de plazos lanza tras capturar ahora
        PlazosAdministrativosService plazosBoom = plazosApelacion(fd -> {
            throw new IllegalStateException("fallo simulado del servicio de plazos");
        });
        assertCalculoIncompatibleSinEfectos(condenatorio(), svcCon(plazosBoom));

        // (b) el servicio devuelve null (contrato interno violado)
        PlazosAdministrativosService plazosNull = plazosApelacion(fd -> null);
        assertCalculoIncompatibleSinEfectos(condenatorio(), svcCon(plazosNull));

        // (c) calculo con fechaOrigen distinta
        PlazosAdministrativosService plazosOrigenMalo = plazosApelacion(fd ->
                new CalculoPlazoAdministrativo(
                        TipoPlazoAdministrativo.APELACION_FALLO,
                        fd.minusDays(1), 30, fd.plusDays(30)));
        assertCalculoIncompatibleSinEfectos(condenatorio(), svcCon(plazosOrigenMalo));
    }

    // =========================================================================
    // 17 y 18. Repeticion secuencial y concurrente
    // =========================================================================

    @Test
    @DisplayName("17. Duplicado secuencial: segunda invocacion rechazada; un solo conjunto de efectos; clock=1")
    void duplicado_secuencial() {
        Fixture fx = piezaPrevia();

        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);
        int porposDespues = eventosDe(fx.idActa, TipoEventoActa.PORPOS).size();

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(relojPortal.calls).isZero();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(porposDespues);
    }

    @Test
    @DisplayName("18. Concurrencia coordinada: un exito, un rechazo; exactamente un PORPOS, un NOTSUP, cero NOTPOS, un intento PORTAL_INFRACTOR; clock=1")
    void concurrencia_coordinada() throws Exception {
        Fixture fx = piezaPrevia();
        relojPortal.calls = 0;

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();

        Callable<Void> tarea = () -> {
            barrier.await();
            try {
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);
                exitos.incrementAndGet();
            } catch (PrecondicionVioladaException e) {
                rechazos.incrementAndGet();
            }
            return null;
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Void> f1 = pool.submit(tarea);
            Future<Void> f2 = pool.submit(tarea);
            f1.get();
            f2.get();
        } finally {
            pool.shutdownNow();
        }

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(rechazos.get()).isEqualTo(1);
        assertThat(relojPortal.calls).isEqualTo(1);

        // exactamente un intento PORTAL_INFRACTOR
        assertThat(intentoRepo.buscarPorNotificacion(fx.idNotif).stream()
                .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR).count())
                .isEqualTo(1);

        // exactamente un PORPOS, exactamente un NOTSUP (habia intento activo previo), cero NOTPOS
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).isEmpty();

        FalNotificacion notifFinal = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notifFinal.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(notifFinal.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);

        // CIERRA = 0 para pieza previa
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();

        // acta en ANAL, no cerrada
        FalActa actaFinal = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(actaFinal.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(actaFinal.estaCerrada()).isFalse();

        // snapshot final exacto: bandeja PENDIENTE_ANALISIS, accion DICTAR_FALLO
        FalActaSnapshot snapFinal = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snapFinal.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
        assertThat(snapFinal.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
    }

    // =========================================================================
    // 19..22. Asociacion QR-acta-notificacion y cobertura faltante
    // =========================================================================

    @Test
    @DisplayName("19. actaIdQrEsperada null -> PrecondicionVioladaException; clock=0; cero efectos")
    void actaIdQrEsperada_null() {
        Fixture fx = piezaPrevia();
        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, null, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).isEmpty();
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isNull();
    }

    @Test
    @DisplayName("20. Mismatch QR-acta-notificacion: PrecondicionVioladaException; clock=0; notificacion B intacta; cero PORPOS/NOTSUP/CIERRA en B")
    void mismatch_qr_acta_notificacion() {
        Fixture fxA = piezaPrevia();
        Fixture fxB = piezaPrevia();

        int porposBAntes = eventosDe(fxB.idActa, TipoEventoActa.PORPOS).size();
        int notsupBAntes = eventosDe(fxB.idActa, TipoEventoActa.NOTSUP).size();
        int cierraBAntes = eventosDe(fxB.idActa, TipoEventoActa.CIERRA).size();
        FalActaSnapshot snapBAntes = snapshotRepo.buscarPorActa(fxB.idActa).orElseThrow();

        relojPortal.calls = 0;
        // QR resuelto para acta A, pero notificacion pertenece a acta B -> mismatch
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fxB.idNotif, fxA.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(relojPortal.calls).isZero();

        // notificacion B intacta
        FalNotificacion notifB = notifRepo.buscarPorId(fxB.idNotif).orElseThrow();
        assertThat(notifB.getResultado()).isNull();
        assertThat(notifB.getEstado()).isNotEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);

        // cero intentos PORTAL_INFRACTOR en notif B
        assertThat(intentoRepo.buscarPorNotificacion(fxB.idNotif).stream()
                .filter(i -> i.getCanalNotif() == CanalNotificacion.PORTAL_INFRACTOR).count())
                .isZero();

        // acta B: sin eventos nuevos ni snapshot cambiado
        assertThat(eventosDe(fxB.idActa, TipoEventoActa.PORPOS)).hasSize(porposBAntes);
        assertThat(eventosDe(fxB.idActa, TipoEventoActa.NOTSUP)).hasSize(notsupBAntes);
        assertThat(eventosDe(fxB.idActa, TipoEventoActa.CIERRA)).hasSize(cierraBAntes);
        FalActaSnapshot snapBDespues = snapshotRepo.buscarPorActa(fxB.idActa).orElseThrow();
        assertThat(snapBDespues.getCodBandeja()).isEqualTo(snapBAntes.getCodBandeja());
    }

    @Test
    @DisplayName("21. Dos intentos activos previos: ambos SUPERADA_POR_PORTAL/SIN_EFECTO; exactamente un NOTSUP; clock=1")
    void dos_intentos_previos_ambos_superados() {
        Fixture fx = piezaPrevia();
        // piezaPrevia ya tiene 1 intento activo; agregar un segundo
        Long intentoPrevio1 = intentoRepo.buscarPorNotificacion(fx.idNotif).get(0).getId();
        Long intentoPrevio2 = intentoActivo(fx.idNotif);

        relojPortal.calls = 0;
        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);
        assertThat(relojPortal.calls).isEqualTo(1);

        FalNotificacionIntento previo1 = intentoRepo.buscarPorId(intentoPrevio1).orElseThrow();
        FalNotificacionIntento previo2 = intentoRepo.buscarPorId(intentoPrevio2).orElseThrow();
        assertThat(previo1.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
        assertThat(previo1.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
        assertThat(previo2.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
        assertThat(previo2.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);

        // exactamente un NOTSUP (no uno por intento superado)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).hasSize(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
    }

    @Test
    @DisplayName("22. Intento con resultado previo (NEGATIVO): no se sobreescribe; conserva todos los campos; clock=1")
    void intento_terminal_previo_no_reescrito() {
        Fixture fx = piezaPrevia();
        Long intentoId = intentoRepo.buscarPorNotificacion(fx.idNotif).get(0).getId();
        // cerrar el intento existente con NEGATIVO (ya tiene resultado; no es activo)
        intentoService.registrarResultadoIntento(intentoId, ResultadoNotificacion.NEGATIVO, "seed");

        // capturar todos los campos del intento terminal ANTES del portal positivo
        FalNotificacionIntento terminalAntes = intentoRepo.buscarPorId(intentoId).orElseThrow();
        assertThat(terminalAntes.getResultadoIntento()).isEqualTo(ResultadoNotificacion.NEGATIVO);

        Long   captId              = terminalAntes.getId();
        Long   captNotificacionId  = terminalAntes.getNotificacionId();
        short  captNroIntento      = terminalAntes.getNroIntento();
        CanalNotificacion captCanal = terminalAntes.getCanalNotif();
        Long   captDomicilio       = terminalAntes.getDomicilioNotifId();
        String captDestino         = terminalAntes.getDestinoDigital();
        Long   captLote            = terminalAntes.getLoteId();
        String captRef             = terminalAntes.getReferenciaExterna();
        LocalDateTime captFhIntento   = terminalAntes.getFhIntento();
        LocalDateTime captFhAlta      = terminalAntes.getFhAlta();
        String captUserAlta           = terminalAntes.getIdUserAlta();
        EstadoNotificacion captEstado = terminalAntes.getEstadoIntento();
        ResultadoNotificacion captRes = terminalAntes.getResultadoIntento();
        LocalDateTime captFhResultado = terminalAntes.getFhResultado();
        LocalDateTime captFhUltMod    = terminalAntes.getFhUltMod();
        String captUserUltMod         = terminalAntes.getIdUserUltMod();

        relojPortal.calls = 0;
        intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR);
        assertThat(relojPortal.calls).isEqualTo(1);

        // intento terminal: byte-semanticamente intacto como entidad de dominio
        FalNotificacionIntento terminalDespues = intentoRepo.buscarPorId(intentoId).orElseThrow();
        assertThat(terminalDespues.getId()).isEqualTo(captId);
        assertThat(terminalDespues.getNotificacionId()).isEqualTo(captNotificacionId);
        assertThat(terminalDespues.getNroIntento()).isEqualTo(captNroIntento);
        assertThat(terminalDespues.getCanalNotif()).isEqualTo(captCanal);
        assertThat(terminalDespues.getDomicilioNotifId()).isEqualTo(captDomicilio);
        assertThat(terminalDespues.getDestinoDigital()).isEqualTo(captDestino);
        assertThat(terminalDespues.getLoteId()).isEqualTo(captLote);
        assertThat(terminalDespues.getReferenciaExterna()).isEqualTo(captRef);
        assertThat(terminalDespues.getFhIntento()).isEqualTo(captFhIntento);
        assertThat(terminalDespues.getFhAlta()).isEqualTo(captFhAlta);
        assertThat(terminalDespues.getIdUserAlta()).isEqualTo(captUserAlta);
        assertThat(terminalDespues.getEstadoIntento()).isEqualTo(captEstado);
        assertThat(terminalDespues.getResultadoIntento()).isEqualTo(captRes);
        assertThat(terminalDespues.getFhResultado()).isEqualTo(captFhResultado);
        assertThat(terminalDespues.getFhUltMod()).isEqualTo(captFhUltMod);
        assertThat(terminalDespues.getIdUserUltMod()).isEqualTo(captUserUltMod);

        // sin NOTSUP (no habia intentos activos)
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTSUP)).isEmpty();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(1);
    }

    // =========================================================================
    // 23-24. Contratos del constructor CalculoPlazoAdministrativo
    // (invariantes del record que hacen inalcanzables los branches defensivos del servicio)
    // =========================================================================

    /**
     * 23. El constructor de CalculoPlazoAdministrativo rechaza tipo null.
     *
     * El branch del servicio "calculo.tipo() != APELACION_FALLO" es inalcanzable mediante
     * construccion normal porque:
     *   - el constructor valida tipo != null (IllegalArgumentException);
     *   - TipoPlazoAdministrativo solo tiene el valor APELACION_FALLO, por lo que un objeto
     *     correctamente construido siempre cumple calculo.tipo() == APELACION_FALLO.
     * La validacion defensiva del servicio permanece como defensa en profundidad para futuras
     * extensiones del enum sin retiro.
     */
    @Test
    @DisplayName("23. Constructor CalculoPlazoAdministrativo: tipo null -> IllegalArgumentException (branch de tipo incorrecto en servicio es inalcanzable con implementacion real)")
    void calculo_constructor_rechaza_tipo_null() {
        LocalDate hoy = FECHA_ORIGEN;
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo(
                        null, hoy, 30, hoy.plusDays(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo");
    }

    /**
     * 24. El constructor de CalculoPlazoAdministrativo rechaza fechaVencimiento null.
     *
     * El branch del servicio "calculo.fechaVencimiento() == null" es inalcanzable mediante
     * construccion normal porque el constructor valida fechaVencimiento != null.
     * La validacion defensiva del servicio permanece como defensa en profundidad.
     */
    @Test
    @DisplayName("24. Constructor CalculoPlazoAdministrativo: fechaVencimiento null -> IllegalArgumentException (branch defensivo del servicio es inalcanzable con implementacion real)")
    void calculo_constructor_rechaza_fecha_vencimiento_null() {
        LocalDate hoy = FECHA_ORIGEN;
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo(
                        TipoPlazoAdministrativo.APELACION_FALLO, hoy, 30, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechaVencimiento");
    }

    // =========================================================================
    // Helpers de fixture
    // =========================================================================

    private record Fixture(Long idActa, Long idNotif) {}

    /** Lleva un acta a ANAL via notificacion positiva de la pieza previa ordinaria. */
    private Long avanzarAAnalisis() {
        String nro = String.valueOf(++contadorNroActa);
        Long idActa = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001", FECHA_ORIGEN, "Av. Argentina 123", "San Martin 456",
                null, null, null, "Maria Lopez", nro,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));

        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "seed"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotif),
                ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)),
                null, "seed"));
        return idActa;
    }

    /** Deja el acta con una notificacion del ACTA_INFRACCION EN_PROCESO (variante pieza previa para portal). */
    private Fixture piezaPrevia() {
        String nro = String.valueOf(++contadorNroActa);
        Long idActa = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001", FECHA_ORIGEN, "Av. Argentina 123", "San Martin 456",
                null, null, null, "Maria Lopez", nro,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "seed"))
                .idEntidadAfectada();
        return new Fixture(idActa, Long.parseLong(idNotif));
    }

    /**
     * Deja el acta con una notificacion del ACTA_INFRACCION pero sin intentos activos.
     * El intento creado por enviarNotificacion se cierra con resultado NEGATIVO.
     */
    private Fixture piezaPreviaSinIntentoPrevio() {
        Fixture fx = piezaPrevia();
        Long intentoId = intentoActivo(fx.idNotif);
        intentoService.registrarResultadoIntento(intentoId, ResultadoNotificacion.NEGATIVO, "seed");
        return fx;
    }

    /** Deja la acta en ANAL con un fallo condenatorio PENDIENTE_NOTIFICACION y su notificacion EN_PROCESO. */
    private Fixture condenatorio() {
        Long idActa = avanzarAAnalisis();
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                idActa, new BigDecimal("5000.00"), "Condenatorio por infraccion grave", null));
        return firmarYNotificarFallo(idActa);
    }

    /** Deja la acta en ANAL con un fallo absolutorio PENDIENTE_NOTIFICACION y su notificacion EN_PROCESO. */
    private Fixture absolutorio(BloqueantesMaterialesChecker checker) {
        Long idActa = avanzarAAnalisis();
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(idActa, "Absolutorio", null));
        return firmarYNotificarFallo(idActa);
    }

    private Fixture firmarYNotificarFallo(Long idActa) {
        FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
        Long idDocFallo = fallo.getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "seed"))
                .idEntidadAfectada();
        return new Fixture(idActa, Long.parseLong(idNotif));
    }

    private Long intentoActivo(Long idNotif) {
        return ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, idNotif);
    }

    private List<FalActaEvento> eventosDe(Long idActa, TipoEventoActa tipo) {
        return eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == tipo)
                .toList();
    }

    private int indexOfLast(List<FalActaEvento> todos, TipoEventoActa tipo) {
        int last = -1;
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).tipoEvt() == tipo) last = i;
        }
        return last;
    }

    /** Verifica atributos de origen/actor/instante de un evento del flujo portal. */
    private void assertEventoPortal(FalActaEvento e, Long idNotifEsperado) {
        assertThat(e.origenEvt()).isEqualTo(OrigenEvento.PORTAL_INFRACTOR);
        assertThat(e.actorTipo()).isEqualTo(ActorTipoEvento.INFRACTOR);
        assertThat(e.fhEvt()).isEqualTo(relojFijo.now());
        assertThat(e.idUserEvt()).isEqualTo(ACTOR);
        if (idNotifEsperado != null) {
            assertThat(e.idNotifRel()).isEqualTo(idNotifEsperado);
        }
    }

    /**
     * Verifica que una invocacion rechazada no produce efectos: clock=0, sin PORPOS nuevo,
     * cabecera sin resultado, fallo sin mutacion (fhNotificacion null, fhVtoApelacion invariante).
     */
    private void assertRechazoSinEfectos(Fixture fx) {
        int porposAntes = eventosDe(fx.idActa, TipoEventoActa.PORPOS).size();
        int cierraAntes = eventosDe(fx.idActa, TipoEventoActa.CIERRA).size();
        FalActaFallo falloAntes = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        EstadoFalloActa estadoAntes = falloAntes.getEstadoFallo();
        LocalDate vtoAntes = falloAntes.getFhVtoApelacion();
        FalActaSnapshot snapAntes = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                intentoService.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojPortal.calls).isZero();

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isNull();

        FalActaFallo falloDespues = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(falloDespues.getEstadoFallo()).isEqualTo(estadoAntes);
        assertThat(falloDespues.getFhNotificacion()).isNull();
        assertThat(falloDespues.getFhVtoApelacion()).isEqualTo(vtoAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(porposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).hasSize(cierraAntes);

        FalActaSnapshot snapDespues = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snapDespues.getCodBandeja()).isEqualTo(snapAntes.getCodBandeja());
    }

    /**
     * Verifica que un calculo de plazo incompatible tras capturar el reloj produce
     * IllegalStateException, clock=1 y cero efectos.
     */
    private void assertCalculoIncompatibleSinEfectos(Fixture fx, NotificacionIntentoService svc) {
        int porposAntes = eventosDe(fx.idActa, TipoEventoActa.PORPOS).size();
        int cierraAntes = eventosDe(fx.idActa, TipoEventoActa.CIERRA).size();

        relojPortal.calls = 0;
        assertThatThrownBy(() ->
                svc.registrarPortalPositivo(fx.idNotif, fx.idActa, DESTINO, ACTOR))
                .isInstanceOf(IllegalStateException.class);

        assertThat(relojPortal.calls).isEqualTo(1);

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isNull();

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
        assertThat(fallo.getFhNotificacion()).isNull();
        assertThat(fallo.getFhVtoApelacion()).isNull();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.PORPOS)).hasSize(porposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).hasSize(cierraAntes);
    }

    /** Servicio de plazos cuyo calcularVencimientoApelacion se define por la funcion dada. */
    private PlazosAdministrativosService plazosApelacion(
            java.util.function.Function<LocalDate, CalculoPlazoAdministrativo> fn) {
        return new PlazosAdministrativosService(
                new PlazosAdministrativosProperties(),
                new CalculadorPlazosAdministrativos(
                        new CalendarioAdministrativoService(new InMemoryDiaNoComputableRepository(), relojFijo))) {
            @Override
            public CalculoPlazoAdministrativo calcularVencimientoApelacion(LocalDate fechaNotificacion) {
                return fn.apply(fechaNotificacion);
            }
        };
    }

    /** Construye un NotificacionIntentoService con los repos del test y las colaboraciones dadas. */
    private NotificacionIntentoService svcCon(PlazosAdministrativosService plazos) {
        return svcCon(plazos, new NoOpBloqueantesMaterialesChecker());
    }

    private NotificacionIntentoService svcCon(
            PlazosAdministrativosService plazos, BloqueantesMaterialesChecker checker) {
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, new InMemoryPagoVoluntarioRepository(),
                falloRepo, new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), relojFijo, snapshotRepo);
        return new NotificacionIntentoService(
                intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc,
                loteRepo, relojPortal, falloRepo, docRepo, checker, plazos);
    }

    // =========================================================================
    // Reloj contador del servicio portal
    // =========================================================================

    private static final class CountingClock extends FaltasClock {
        int calls = 0;
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
