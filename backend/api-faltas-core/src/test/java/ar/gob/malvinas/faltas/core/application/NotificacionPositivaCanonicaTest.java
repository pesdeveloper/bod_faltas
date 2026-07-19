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
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.CalculadorPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
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
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
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
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
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
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
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
 * CMD-FALLO-004 (variante ordinaria): resultado notificatorio positivo sobre un intento concreto.
 *
 * Cablea NotificacionService a mano (sin Mockito) con un reloj contador propio del servicio y un
 * reloj fijo separado para el SnapshotRecalculador. Verifica pieza previa, fallo condenatorio con
 * plazo de apelacion, fallo absolutorio con y sin bloqueantes, validaciones previas al tiempo,
 * atomicidad ante fallo de plazo y repeticion secuencial/concurrente.
 *
 * El instante canonico de test es 2026-07-10 (America/Argentina/Buenos_Aires).
 */
@DisplayName("CMD-FALLO-004 ordinaria: resultado notificatorio positivo sobre intento")
class NotificacionPositivaCanonicaTest {

    private static final String ACTOR = "test-user";
    private static final LocalDate FECHA_ORIGEN = LocalDate.of(2026, 7, 10);
    private static final Instant INSTANTE = Instant.parse("2026-07-10T12:00:00Z");

    private FaltasClock relojFijo;
    private CountingClock relojServicio;

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryFalloActaRepository falloRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private FalloActaService falloService;
    private NotificacionService notifService;

    private int contadorNroActa;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        relojFijo = new FaltasClock(Clock.fixed(INSTANTE, FaltasClock.ZONE));
        relojServicio = new CountingClock(relojFijo);
        contadorNroActa = 90_000_000;

        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        falloRepo = new InMemoryFalloActaRepository();

        var firmaRepo = new InMemoryDocumentoFirmaRepository();
        var pagoRepo = new InMemoryPagoVoluntarioRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        // Reloj fijo separado del CountingClock del servicio para el recalculo de snapshot.
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
                falloRepo, new NoOpBloqueantesMaterialesChecker(), relojServicio,
                intentoRepo, new InMemoryPersonaDomicilioRepository(),
                plazosConCalendarioVacio(30));

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoRepo, recalc, relojFijo);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // =========================================================================
    // 1. Pieza previa
    // =========================================================================

    @Test
    @DisplayName("01. Pieza previa con actor con espacios: se normaliza a 'test-user' en intento, cabecera y NOTPOS; clock=1")
    void pieza_previa_completa() {
        Fixture fx = notificarPiezaPrevia();

        relojServicio.calls = 0;
        // Actor con espacios: produccion debe normalizar (trim) antes de persistir/registrar.
        ComandoResultado res = notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, "  " + ACTOR + "  "));

        assertThat(relojServicio.calls).isEqualTo(1);
        assertThat(res.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

        FalNotificacionIntento intento = intentoRepo.buscarPorId(fx.intentoId).orElseThrow();
        assertThat(intento.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        assertThat(intento.getFhResultado()).isEqualTo(relojFijo.now());
        assertThat(intento.getFhUltMod()).isEqualTo(relojFijo.now());
        assertThat(intento.getIdUserUltMod()).isEqualTo(ACTOR);

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(notif.getFechaResultado()).isEqualTo(relojFijo.now());
        assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(acta.estaCerrada()).isFalse();

        assertThat(falloRepo.buscarActivo(fx.idActa)).isEmpty();

        FalActaEvento notpos = unicoEvento(fx.idActa, TipoEventoActa.NOTPOS, fx.idNotif);
        assertThat(notpos.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
        assertThat(notpos.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);
        assertThat(notpos.fhEvt()).isEqualTo(relojFijo.now());
        assertThat(notpos.idUserEvt()).isEqualTo(ACTOR);
        assertThat(notpos.idDocuRel()).isEqualTo(notif.getIdDocumento());
        assertThat(notpos.idNotifRel()).isEqualTo(fx.idNotif);

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
    }

    // =========================================================================
    // 2 y 3. Fallo condenatorio con calculo de plazo
    // =========================================================================

    @Test
    @DisplayName("02. Condenatorio default 30: 2026-07-10 -> 2026-08-14, fallo NOTIFICADO, snapshot PENDIENTES_FALLO")
    void condenatorio_default_30() {
        Fixture fx = notificarCondenatorio();

        relojServicio.calls = 0;
        ComandoResultado res = notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));

        assertThat(relojServicio.calls).isEqualTo(1);
        assertThat(res.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
        assertThat(fallo.getFhNotificacion()).isEqualTo(relojFijo.now());
        assertThat(fallo.getFhVtoApelacion()).isEqualTo(LocalDate.of(2026, 8, 14));
        assertThat(fallo.isSiFirme()).isFalse();

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.estaCerrada()).isFalse();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.CONDENA_FIRME);

        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();
        FalActaEvento notpos = unicoEvento(fx.idActa, TipoEventoActa.NOTPOS, fx.idNotif);
        assertThat(notpos.fhEvt()).isEqualTo(relojFijo.now());
        assertThat(notpos.idUserEvt()).isEqualTo(ACTOR);

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTES_FALLO);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
    }

    @Test
    @DisplayName("03. Condenatorio config 45 + excepcion 2026-08-01: 2026-07-10 -> 2026-09-02")
    void condenatorio_config_45_con_excepcion() {
        // Calendario con excepcion activa en 2026-08-01 y configuracion global de 45 dias.
        InMemoryDiaNoComputableRepository diaRepo = new InMemoryDiaNoComputableRepository();
        CalendarioAdministrativoService calendario =
                new CalendarioAdministrativoService(diaRepo, relojFijo);
        calendario.registrarDiaNoComputable(
                LocalDate.of(2026, 8, 1), TipoDiaNoComputable.ASUETO_ADMINISTRATIVO,
                "Asueto administrativo especial", OrigenDiaNoComputable.MANUAL, null, "seed");

        PlazosAdministrativosProperties props = new PlazosAdministrativosProperties();
        props.setApelacionDiasComputables(45);
        PlazosAdministrativosService plazos45 = new PlazosAdministrativosService(
                props, new CalculadorPlazosAdministrativos(calendario));

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, new InMemoryPagoVoluntarioRepository(),
                falloRepo, new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), relojFijo, snapshotRepo);
        NotificacionService notif45 = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), relojServicio,
                intentoRepo, new InMemoryPersonaDomicilioRepository(), plazos45);

        Fixture fx = notificarCondenatorio();

        relojServicio.calls = 0;
        notif45.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));

        assertThat(relojServicio.calls).isEqualTo(1);
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getFhVtoApelacion()).isEqualTo(LocalDate.of(2026, 9, 2));

        // El calculo respeta el tipo y la fecha de origen del instante canonico.
        CalculoPlazoAdministrativo calc = plazos45.calcularVencimientoApelacion(FECHA_ORIGEN);
        assertThat(calc.tipo()).isEqualTo(TipoPlazoAdministrativo.APELACION_FALLO);
        assertThat(calc.fechaOrigen()).isEqualTo(FECHA_ORIGEN);
        assertThat(calc.diasComputablesAplicados()).isEqualTo(45);
    }

    // =========================================================================
    // 4 y 5. Fallo absolutorio
    // =========================================================================

    @Test
    @DisplayName("04. Absolutorio sin bloqueantes: ABSUELTO, CERRADA, NOTPOS+CIERRA, snapshot CERRADAS")
    void absolutorio_sin_bloqueantes_cierra() {
        Fixture fx = notificarAbsolutorio(new NoOpBloqueantesMaterialesChecker());

        relojServicio.calls = 0;
        ComandoResultado res = notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));

        assertThat(relojServicio.calls).isEqualTo(1);
        assertThat(res.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
        assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        assertThat(acta.estaCerrada()).isTrue();

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
        assertThat(fallo.getFhVtoApelacion()).isNull();

        FalActaEvento notpos = unicoEvento(fx.idActa, TipoEventoActa.NOTPOS, fx.idNotif);
        assertThat(notpos.fhEvt()).isEqualTo(relojFijo.now());
        List<FalActaEvento> cierras = eventosDe(fx.idActa, TipoEventoActa.CIERRA);
        assertThat(cierras).hasSize(1);
        assertThat(cierras.get(0).fhEvt()).isEqualTo(relojFijo.now());
        assertThat(cierras.get(0).idUserEvt()).isEqualTo(ACTOR);
        // Orden real de insercion (mismo instante): penultimo evento = NOTPOS de la notificacion,
        // ultimo evento = CIERRA. isBeforeOrEqual no prueba orden con instantes iguales.
        List<FalActaEvento> secuencia = eventoRepo.buscarPorActa(fx.idActa);
        FalActaEvento penultimo = secuencia.get(secuencia.size() - 2);
        FalActaEvento ultimo = secuencia.get(secuencia.size() - 1);
        assertThat(penultimo.tipoEvt()).isEqualTo(TipoEventoActa.NOTPOS);
        assertThat(penultimo.idNotifRel()).isEqualTo(fx.idNotif);
        assertThat(ultimo.tipoEvt()).isEqualTo(TipoEventoActa.CIERRA);
        // Ambos conservan el mismo instante y actor.
        assertThat(ultimo.fhEvt()).isEqualTo(penultimo.fhEvt());
        assertThat(ultimo.idUserEvt()).isEqualTo(penultimo.idUserEvt());

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
    }

    @Test
    @DisplayName("05. Absolutorio con bloqueantes: ABSUELTO, no cierra, un NOTPOS, cero CIERRA")
    void absolutorio_con_bloqueantes_no_cierra() {
        BloqueantesMaterialesChecker conBloqueantes = actaId -> true;
        Fixture fx = notificarAbsolutorio(conBloqueantes);

        NotificacionService notifBloq = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo,
                new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, new InMemoryPagoVoluntarioRepository(),
                        falloRepo, new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), relojFijo, snapshotRepo),
                falloRepo, conBloqueantes, relojServicio,
                intentoRepo, new InMemoryPersonaDomicilioRepository(), plazosConCalendarioVacio(30));

        // Situacion administrativa operativa previa: no debe cambiar por bloqueantes activos.
        SituacionAdministrativaActa situacionPrevia =
                actaRepo.buscarPorId(fx.idActa).orElseThrow().getSituacionAdministrativa();

        relojServicio.calls = 0;
        notifBloq.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));

        assertThat(relojServicio.calls).isEqualTo(1);

        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
        assertThat(acta.estaCerrada()).isFalse();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        // La situacion administrativa permanece en el valor operativo previo (no se cierra).
        assertThat(acta.getSituacionAdministrativa()).isEqualTo(situacionPrevia);

        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getTipoFallo()).isEqualTo(TipoFalloActa.ABSOLUTORIO);
        assertThat(fallo.getResultadoFallo()).isEqualTo(ResultadoFalloActa.ABSUELVE);

        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS).stream()
                .filter(e -> fx.idNotif.equals(e.idNotifRel())).count()).isEqualTo(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).isEmpty();

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
    }

    // =========================================================================
    // 6 y 7. Observaciones
    // =========================================================================

    @Test
    @DisplayName("06. Observaciones null preservan las de la cabecera")
    void observaciones_null_preservan() {
        Fixture fx = notificarPiezaPrevia("observacion-de-envio");

        relojServicio.calls = 0;
        notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getObservaciones()).isEqualTo("observacion-de-envio");
    }

    @Test
    @DisplayName("07. Observaciones informadas reemplazan las existentes")
    void observaciones_informadas_reemplazan() {
        Fixture fx = notificarPiezaPrevia("observacion-de-envio");

        relojServicio.calls = 0;
        notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, "acuse recibido en persona", ACTOR));

        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getObservaciones()).isEqualTo("acuse recibido en persona");
    }

    // =========================================================================
    // 8 y 9. Validacion estructural del comando (antes del tiempo)
    // =========================================================================

    @Test
    @DisplayName("08. Comando null -> IllegalArgumentException, sin tocar el reloj")
    void comando_null() {
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("09. Actor null/blanco/>36 -> PrecondicionVioladaException, sin tocar el reloj")
    void actor_invalido() {
        Fixture fx = notificarPiezaPrevia();
        relojServicio.calls = 0;

        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, null)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, "   ")))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, "x".repeat(37))))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(relojServicio.calls).isZero();
        assertIntentoSinResultado(fx);
    }

    // =========================================================================
    // 10..19. Validaciones de dominio (antes del tiempo)
    // =========================================================================

    @Test
    @DisplayName("10. Intento inexistente -> NotificacionIntentoNoEncontradoException, clock=0")
    void intento_inexistente() {
        Fixture fx = notificarPiezaPrevia();
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, 999_999L, null, ACTOR)))
                .isInstanceOf(NotificacionIntentoNoEncontradoException.class);
        assertThat(relojServicio.calls).isZero();
        assertIntentoSinResultado(fx);
    }

    @Test
    @DisplayName("11. Intento de otra notificacion -> PrecondicionVioladaException, clock=0")
    void intento_de_otra_notificacion() {
        Fixture a = notificarPiezaPrevia();
        Fixture b = notificarPiezaPrevia();
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(a.idNotif, b.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
        assertIntentoSinResultado(a);
        assertIntentoSinResultado(b);
    }

    @Test
    @DisplayName("12. Intento con estado incompatible -> PrecondicionVioladaException, clock=0")
    void intento_estado_incompatible() {
        Fixture fx = notificarPiezaPrevia();
        FalNotificacionIntento intento = intentoRepo.buscarPorId(fx.intentoId).orElseThrow();
        intento.setEstadoIntento(EstadoNotificacion.CON_ACUSE_NEGATIVO);
        intentoRepo.guardar(intento);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("13. Cabecera con estado incompatible -> PrecondicionVioladaException, clock=0")
    void cabecera_estado_incompatible() {
        Fixture fx = notificarPiezaPrevia();
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        notif.setEstado(EstadoNotificacion.VENCIDA);
        notifRepo.guardar(notif);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("14. Cabecera con resultado previo -> PrecondicionVioladaException, clock=0")
    void cabecera_resultado_previo() {
        Fixture fx = notificarPiezaPrevia();
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notifRepo.guardar(notif);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("15. Acta inexistente -> ActaNoEncontradaException, clock=0")
    void acta_inexistente() {
        // Cabecera e intento sembrados directamente apuntando a un acta inexistente.
        LocalDateTime ahora = relojFijo.now();
        Long idNotif = notifRepo.nextId();
        FalNotificacion notif = new FalNotificacion(
                idNotif, 999_999L, 12345L, TipoDocu.ACTA_INFRACCION, "EMAIL", ahora, ahora, "seed");
        notifRepo.guardar(notif);
        Long idIntento = intentoRepo.nextId();
        short nro = intentoRepo.siguienteNroIntento(idNotif);
        FalNotificacionIntento intento = new FalNotificacionIntento(
                idIntento, idNotif, nro, CanalNotificacion.EMAIL, null, "x@y.com", null, null, ahora, ahora, "seed");
        intentoRepo.guardar(intento);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(idNotif, idIntento, null, ACTOR)))
                .isInstanceOf(ActaNoEncontradaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("16. Acta cerrada -> PrecondicionVioladaException, clock=0")
    void acta_cerrada() {
        Fixture fx = notificarPiezaPrevia();
        FalActa acta = actaRepo.buscarPorId(fx.idActa).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
        actaRepo.guardar(acta);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("17. Incoherencias de fallo (estado / tipo-resultado / fhVtoApelacion precargado) -> PrecondicionVioladaException, clock=0, cero efectos")
    void fallo_incoherencias() {
        // (a) estadoFallo incompatible (NOTIFICADO en lugar de PENDIENTE_NOTIFICACION).
        Fixture estado = notificarCondenatorio();
        FalActaFallo falloEstado = falloRepo.buscarActivo(estado.idActa).orElseThrow();
        falloEstado.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        falloRepo.guardar(falloEstado);
        assertRechazoFalloSinEfectos(estado);

        // (b) tipo CONDENATORIO + resultado ABSUELVE (invariante CONDENA violada). Fixture nuevo.
        Fixture condAbsuelve = notificarCondenatorio();
        FalActaFallo falloCondAbsuelve = falloRepo.buscarActivo(condAbsuelve.idActa).orElseThrow();
        falloCondAbsuelve.setResultadoFallo(ResultadoFalloActa.ABSUELVE);
        falloRepo.guardar(falloCondAbsuelve);
        assertRechazoFalloSinEfectos(condAbsuelve);

        // (c) tipo ABSOLUTORIO + resultado CONDENA (invariante ABSUELVE violada). Fixture nuevo.
        Fixture absCondena = notificarAbsolutorio(new NoOpBloqueantesMaterialesChecker());
        FalActaFallo falloAbsCondena = falloRepo.buscarActivo(absCondena.idActa).orElseThrow();
        falloAbsCondena.setResultadoFallo(ResultadoFalloActa.CONDENA);
        falloRepo.guardar(falloAbsCondena);
        assertRechazoFalloSinEfectos(absCondena);

        // (d) condenatorio con fhVtoApelacion precargado: el vencimiento no puede existir antes del
        // resultado positivo. Fixture nuevo.
        Fixture condPrecargado = notificarCondenatorio();
        FalActaFallo falloCondPre = falloRepo.buscarActivo(condPrecargado.idActa).orElseThrow();
        falloCondPre.setFhVtoApelacion(LocalDate.of(2026, 8, 14));
        falloRepo.guardar(falloCondPre);
        assertRechazoFalloSinEfectos(condPrecargado);
        assertThat(falloRepo.buscarActivo(condPrecargado.idActa).orElseThrow().getFhVtoApelacion())
                .isEqualTo(LocalDate.of(2026, 8, 14));

        // (e) absolutorio con fhVtoApelacion precargado: el campo debe permanecer null toda su vida.
        // Fixture nuevo.
        Fixture absPrecargado = notificarAbsolutorio(new NoOpBloqueantesMaterialesChecker());
        FalActaFallo falloAbsPre = falloRepo.buscarActivo(absPrecargado.idActa).orElseThrow();
        falloAbsPre.setFhVtoApelacion(LocalDate.of(2026, 8, 14));
        falloRepo.guardar(falloAbsPre);
        assertRechazoFalloSinEfectos(absPrecargado);
        assertThat(falloRepo.buscarActivo(absPrecargado.idActa).orElseThrow().getFhVtoApelacion())
                .isEqualTo(LocalDate.of(2026, 8, 14));
    }

    @Test
    @DisplayName("18. Fallo sin fhFirma -> PrecondicionVioladaException, clock=0")
    void fallo_sin_firma() {
        Fixture fx = notificarCondenatorio();
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        fallo.setFhFirma(null);
        falloRepo.guardar(fallo);

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
    }

    @Test
    @DisplayName("19. Documento de fallo no firmado o inexistente -> excepcion, clock=0")
    void documento_fallo_no_firmado_o_inexistente() {
        // Documento del fallo en estado no firmado.
        Fixture noFirmado = notificarCondenatorio();
        FalActaFallo falloNoFirmado = falloRepo.buscarActivo(noFirmado.idActa).orElseThrow();
        FalDocumento docFallo = docRepo.buscarPorId(falloNoFirmado.getDocumentoId()).orElseThrow();
        docFallo.setEstadoDocu(ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.PENDIENTE_FIRMA);
        docRepo.guardar(docFallo);
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(noFirmado.idNotif, noFirmado.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();

        // Documento del fallo inexistente.
        Fixture inexistente = notificarCondenatorio();
        FalActaFallo falloInexistente = falloRepo.buscarActivo(inexistente.idActa).orElseThrow();
        // La cabecera notifica el documento del fallo: se mueve el documento del fallo y de la
        // cabecera a un id inexistente para mantener la variante de fallo y forzar el fallo de carga.
        FalNotificacion notif = notifRepo.buscarPorId(inexistente.idNotif).orElseThrow();
        Long idDocInexistente = 777_777L;
        falloInexistente.setDocumentoId(idDocInexistente);
        falloRepo.guardar(falloInexistente);
        FalActaFallo recargado = falloRepo.buscarActivo(inexistente.idActa).orElseThrow();
        assertThat(recargado.getDocumentoId()).isEqualTo(idDocInexistente);
        // Reasigna la cabecera al mismo documento para que la variante siga siendo FALLO.
        FalNotificacion notifReasignada = new FalNotificacion(
                notif.getId(), notif.getIdActa(), idDocInexistente, notif.getTipoDocumentoNotificado(),
                notif.getCanal(), notif.getFechaEnvio(), notif.getFhAlta(), notif.getIdUserAlta());
        notifRepo.guardar(notifReasignada);
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(inexistente.idNotif, inexistente.intentoId, null, ACTOR)))
                .isInstanceOf(DocumentoNoEncontradoException.class);
        assertThat(relojServicio.calls).isZero();

        // Documento del fallo firmado pero perteneciente a otra acta.
        // La cabecera y el fallo deben referenciar ese mismo documento ajeno para conservar la
        // variante de fallo; el rechazo debe ser por no pertenecer al acta, antes del reloj.
        Fixture ajeno = notificarCondenatorio();
        FalActaFallo falloAjeno = falloRepo.buscarActivo(ajeno.idActa).orElseThrow();
        Long docOtraActa = docFirmadoEnOtraActa();
        falloAjeno.setDocumentoId(docOtraActa);
        falloRepo.guardar(falloAjeno);
        FalNotificacion notifAjeno = notifRepo.buscarPorId(ajeno.idNotif).orElseThrow();
        FalNotificacion notifReasignadoAjeno = new FalNotificacion(
                notifAjeno.getId(), notifAjeno.getIdActa(), docOtraActa, notifAjeno.getTipoDocumentoNotificado(),
                notifAjeno.getCanal(), notifAjeno.getFechaEnvio(), notifAjeno.getFhAlta(), notifAjeno.getIdUserAlta());
        notifRepo.guardar(notifReasignadoAjeno);
        int notposAntesAjeno = eventosDe(ajeno.idActa, TipoEventoActa.NOTPOS).size();
        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(ajeno.idNotif, ajeno.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
        assertIntentoSinResultado(ajeno);
        assertThat(eventosDe(ajeno.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntesAjeno);
    }

    // =========================================================================
    // 20. Atomicidad previa: fallo del servicio de plazos despues de capturar ahora
    // =========================================================================

    @Test
    @DisplayName("20. Condenatorio: calculo incompatible (lanza / null / origen distinto) -> IllegalStateException, clock=1, cero efectos")
    void condenatorio_calculo_incompatible_cero_efectos() {
        // (a) el servicio de plazos lanza tras capturar ahora.
        PlazosAdministrativosService plazosBoom = plazosApelacion(fechaNotificacion -> {
            throw new IllegalStateException("fallo simulado del servicio de plazos");
        });
        assertCalculoIncompatibleSinEfectos(notificarCondenatorio(), notifServiceCon(plazosBoom));

        // (b) el servicio devuelve null (contrato interno violado).
        PlazosAdministrativosService plazosNull = plazosApelacion(fechaNotificacion -> null);
        assertCalculoIncompatibleSinEfectos(notificarCondenatorio(), notifServiceCon(plazosNull));

        // (c) el servicio devuelve un calculo con fechaOrigen distinta a la esperada.
        PlazosAdministrativosService plazosOrigenMalo = plazosApelacion(fechaNotificacion ->
                new CalculoPlazoAdministrativo(
                        TipoPlazoAdministrativo.APELACION_FALLO,
                        fechaNotificacion.minusDays(1),
                        30,
                        fechaNotificacion.plusDays(30)));
        assertCalculoIncompatibleSinEfectos(notificarCondenatorio(), notifServiceCon(plazosOrigenMalo));
    }

    // =========================================================================
    // 21 y 22. Repeticion secuencial y concurrente
    // =========================================================================

    @Test
    @DisplayName("21. Duplicado secuencial: segunda ejecucion rechazada, un solo conjunto de efectos")
    void duplicado_secuencial() {
        Fixture fx = notificarPiezaPrevia();

        notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));
        int notposDespuesPrimera = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(relojServicio.calls).isZero();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposDespuesPrimera);
    }

    @Test
    @DisplayName("22. Concurrencia coordinada: un exito, un rechazo, sin duplicar efectos, clock=1")
    void concurrencia_coordinada() throws Exception {
        Fixture fx = notificarPiezaPrevia();
        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        relojServicio.calls = 0;

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();

        Callable<Void> tarea = () -> {
            barrier.await();
            try {
                notifService.registrarPositiva(
                        new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR));
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
        assertThat(relojServicio.calls).isEqualTo(1);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes + 1);

        // Estado final: exactamente un resultado positivo aplicado en intento y cabecera.
        FalNotificacionIntento intentoFinal = intentoRepo.buscarPorId(fx.intentoId).orElseThrow();
        assertThat(intentoFinal.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(intentoFinal.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        FalNotificacion notifFinal = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notifFinal.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        assertThat(notifFinal.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
    }

    // =========================================================================
    // Helpers de fixture
    // =========================================================================

    private record Fixture(Long idActa, Long idNotif, Long intentoId) {}

    /** Lleva un acta a ANAL: labrar -> captura -> enriquecer -> generar doc -> firmar -> notificar -> positiva. */
    private Long avanzarAAnalisis() {
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
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, ACTOR))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotif), intentoActivo(Long.parseLong(idNotif)), null, ACTOR));
        return idActa;
    }

    private Fixture notificarPiezaPrevia() {
        return notificarPiezaPrevia(null);
    }

    /** Labra un acta y notifica el ACTA_INFRACCION dejando la cabecera EN_PROCESO (variante pieza previa). */
    private Fixture notificarPiezaPrevia(String observaciones) {
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
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, observaciones, ACTOR))
                .idEntidadAfectada();
        Long idNotifL = Long.parseLong(idNotif);
        return new Fixture(idActa, idNotifL, intentoActivo(idNotifL));
    }

    private Fixture notificarCondenatorio() {
        Long idActa = avanzarAAnalisis();
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                idActa, new BigDecimal("5000.00"), "Condenatorio por infraccion grave", null));
        return firmarYNotificarFallo(idActa);
    }

    private Fixture notificarAbsolutorio(BloqueantesMaterialesChecker checker) {
        Long idActa = avanzarAAnalisis();
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(idActa, "Absolutorio", null));
        return firmarYNotificarFallo(idActa);
    }

    private Fixture firmarYNotificarFallo(Long idActa) {
        FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
        Long idDocFallo = fallo.getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(new EnviarNotificacionCommand(
                idActa, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, ACTOR))
                .idEntidadAfectada();
        Long idNotifL = Long.parseLong(idNotif);
        return new Fixture(idActa, idNotifL, intentoActivo(idNotifL));
    }

    private Long intentoActivo(long idNotif) {
        return ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, idNotif);
    }

    private PlazosAdministrativosService plazosConCalendarioVacio(int dias) {
        PlazosAdministrativosProperties props = new PlazosAdministrativosProperties();
        props.setApelacionDiasComputables(dias);
        return new PlazosAdministrativosService(props, new CalculadorPlazosAdministrativos(
                new CalendarioAdministrativoService(new InMemoryDiaNoComputableRepository(), relojFijo)));
    }

    /** Servicio de plazos cuyo calculo de apelacion se define por la funcion dada (incluye lanzar). */
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

    /** Construye un NotificacionService que usa los repos del test y el servicio de plazos dado. */
    private NotificacionService notifServiceCon(PlazosAdministrativosService plazos) {
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, new InMemoryPagoVoluntarioRepository(),
                falloRepo, new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), relojFijo, snapshotRepo);
        return new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), relojServicio,
                intentoRepo, new InMemoryPersonaDomicilioRepository(), plazos);
    }

    /** Genera y firma un ACTA_INFRACCION en un acta distinta y devuelve su id de documento. */
    private Long docFirmadoEnOtraActa() {
        String nro = String.valueOf(++contadorNroActa);
        Long idActa = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001", FECHA_ORIGEN, "Av. Argentina 123", "San Martin 456",
                null, null, null, "Otro Titular", nro,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Doc de otra acta"))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        return Long.parseLong(idDoc);
    }

    /**
     * Verifica el rechazo de un fallo incoherente: PrecondicionVioladaException, clock=0 e
     * inexistencia de efectos. Ademas comprueba que el fallo no avanza (estadoFallo y fhVtoApelacion
     * sin cambio, fhNotificacion sigue null), la cabecera sigue EN_PROCESO y el snapshot conserva
     * codBandeja y accionPendiente previos.
     */
    private void assertRechazoFalloSinEfectos(Fixture fx) {
        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        int cierraAntes = eventosDe(fx.idActa, TipoEventoActa.CIERRA).size();
        FalActaFallo falloAntes = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        EstadoFalloActa estadoAntes = falloAntes.getEstadoFallo();
        LocalDate vtoAntes = falloAntes.getFhVtoApelacion();
        FalActaSnapshot snapAntes = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        CodigoBandeja bandejaAntes = snapAntes.getCodBandeja();
        AccionPendiente accionAntes = snapAntes.getAccionPendiente();

        relojServicio.calls = 0;
        assertThatThrownBy(() -> notifService.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
        assertThat(relojServicio.calls).isZero();
        assertIntentoSinResultado(fx);
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isNull();
        assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        FalActaFallo falloDespues = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(falloDespues.getEstadoFallo()).isEqualTo(estadoAntes);
        assertThat(falloDespues.getFhNotificacion()).isNull();
        assertThat(falloDespues.getFhVtoApelacion()).isEqualTo(vtoAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).hasSize(cierraAntes);
        FalActaSnapshot snapDespues = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow();
        assertThat(snapDespues.getCodBandeja()).isEqualTo(bandejaAntes);
        assertThat(snapDespues.getAccionPendiente()).isEqualTo(accionAntes);
    }

    /**
     * Verifica que un calculo de plazo incompatible (lanza, null u origen distinto) produce
     * IllegalStateException, clock=1 y cero efectos: intento/cabecera sin resultado, fallo intacto en
     * PENDIENTE_NOTIFICACION, sin NOTPOS ni CIERRA nuevos y snapshot sin frontera positiva nueva.
     */
    private void assertCalculoIncompatibleSinEfectos(Fixture fx, NotificacionService svc) {
        int notposAntes = eventosDe(fx.idActa, TipoEventoActa.NOTPOS).size();
        int cierraAntes = eventosDe(fx.idActa, TipoEventoActa.CIERRA).size();
        CodigoBandeja bandejaAntes = snapshotRepo.buscarPorActa(fx.idActa).orElseThrow().getCodBandeja();

        relojServicio.calls = 0;
        assertThatThrownBy(() -> svc.registrarPositiva(
                new RegistrarNotificacionPositivaCommand(fx.idNotif, fx.intentoId, null, ACTOR)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(relojServicio.calls).isEqualTo(1);
        assertIntentoSinResultado(fx);
        FalNotificacion notif = notifRepo.buscarPorId(fx.idNotif).orElseThrow();
        assertThat(notif.getResultado()).isNull();
        FalActaFallo fallo = falloRepo.buscarActivo(fx.idActa).orElseThrow();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
        assertThat(fallo.getFhNotificacion()).isNull();
        assertThat(fallo.getFhVtoApelacion()).isNull();
        assertThat(eventosDe(fx.idActa, TipoEventoActa.NOTPOS)).hasSize(notposAntes);
        assertThat(eventosDe(fx.idActa, TipoEventoActa.CIERRA)).hasSize(cierraAntes);
        assertThat(snapshotRepo.buscarPorActa(fx.idActa).orElseThrow().getCodBandeja()).isEqualTo(bandejaAntes);
    }

    private List<FalActaEvento> eventosDe(Long idActa, TipoEventoActa tipo) {
        return eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == tipo)
                .toList();
    }

    private FalActaEvento unicoEvento(Long idActa, TipoEventoActa tipo, Long idNotif) {
        List<FalActaEvento> matches = eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == tipo && idNotif.equals(e.idNotifRel()))
                .toList();
        assertThat(matches).hasSize(1);
        return matches.get(0);
    }

    private void assertIntentoSinResultado(Fixture fx) {
        FalNotificacionIntento intento = intentoRepo.buscarPorId(fx.intentoId).orElseThrow();
        assertThat(intento.getResultadoIntento()).isNull();
        assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
    }

    // =========================================================================
    // Reloj contador del servicio (separado del reloj de snapshot)
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
