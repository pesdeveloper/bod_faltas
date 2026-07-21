package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.ApelacionActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository;
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
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.IntentoTestSupport;
import ar.gob.malvinas.faltas.core.support.PlazosTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests canonicos de CMD-FALLO-006: Declarar firmeza por apelacion rechazada.
 *
 * Sin Mockito. Repositorios InMemory reales. CountingClock para firmezaService.
 *
 * Cubre: camino feliz (RESUELTA+RECHAZADA y RECHAZADA legacy), validacion estructural,
 * precondiciones, asociacion de apelacion, estados invalidos, repeticion secuencial
 * y concurrencia InMemory.
 *
 * Todos los estados se capturan por valor en records inmutables antes del comando.
 * assertEstadoSinCambios usa isEqualTo sobre EstadoObservable completo.
 */
@DisplayName("GAP-CMD-FALLO-006: Firmeza por apelacion rechazada (canonical)")
class FirmezaApelacionRechazadaCanonicaTest {

    // -------------------------------------------------------------------------
    // CountingClock
    // -------------------------------------------------------------------------

    static class CountingClock extends FaltasClock {
        private final AtomicInteger invocaciones = new AtomicInteger(0);

        @Override
        public LocalDateTime now() {
            invocaciones.incrementAndGet();
            return FaltasClockTestSupport.FIXED.now();
        }

        int invocaciones() { return invocaciones.get(); }
    }

    // -------------------------------------------------------------------------
    // Campos
    // -------------------------------------------------------------------------

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoVolRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;
    private PagoCondenaRepository pagoCondRepo;

    private final InMemoryNotificacionIntentoRepository intentoRepo =
            new InMemoryNotificacionIntentoRepository();

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private ApelacionActaService apelacionService;
    private FirmezaCondenaService firmezaService;

    private CountingClock testClock;

    // -------------------------------------------------------------------------
    // setUp
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();
        pagoCondRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo,
                pagoCondRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                new InMemoryDocumentoPlantillaRepository(),
                new TalonarioService(new InMemoryTalonarioRepository(),
                        new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(),
                        FaltasClockTestSupport.FIXED),
                new InMemoryDependenciaRepository(),
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(),
                notifRepo, FaltasClockTestSupport.FIXED);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, actaId -> false, FaltasClockTestSupport.FIXED, intentoRepo,
                new InMemoryPersonaDomicilioRepository(),
                PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo,
                recalc, FaltasClockTestSupport.FIXED);

        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc,
                actaId -> false, FaltasClockTestSupport.FIXED);

        testClock = new CountingClock();

        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                recalc, testClock);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // -------------------------------------------------------------------------
    // Helpers de fixture
    // -------------------------------------------------------------------------

    private Long crearActaPreFallo(String docNum) {
        Long actaId = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 100", "San Martin 200",
                null, null, null, "Infractor Test", docNum,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "firmante1", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), CanalNotificacion.PRESENCIAL,
                        null, null, null, "test-user")).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotif),
                IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)),
                null, "test-actor"));
        return actaId;
    }

    private Long crearActaConFalloCondenatorioNotificado(String docNum) {
        Long actaId = crearActaPreFallo(docNum);
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("3000.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, CanalNotificacion.PRESENCIAL,
                        null, null, null, "test-user")).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotifFallo),
                IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotifFallo)),
                null, "test-actor"));
        return actaId;
    }

    /** Crea acta con fallo NOTIFICADO + apelacion en RESUELTA+RECHAZADA (camino canonico). */
    private Long crearActaConApelacionResueltaRechazada(String docNum) {
        Long actaId = crearActaConFalloCondenatorioNotificado(docNum);
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos apelacion", null));
        apelacionService.resolverRechazada(
                new ResolverApelacionRechazadaCommand(actaId, "Apelacion sin merito", null));
        return actaId;
    }

    /** Crea acta con fallo NOTIFICADO + apelacion en estado legacy RECHAZADA (directo). */
    @SuppressWarnings("deprecation")
    private Long crearActaConApelacionLegacyRechazada(String docNum) {
        Long actaId = crearActaConFalloCondenatorioNotificado(docNum);
        Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalActaApelacion apLegacy = new FalActaApelacion(
                apelacionRepo.nextId(), actaId, falloId,
                EstadoApelacionActa.RECHAZADA,
                ahora, "Infractor", "Fundamentos legacy", null, false,
                ahora, "test-user");
        apelacionRepo.guardar(apLegacy);
        return actaId;
    }

    private DeclararCondenaFirmePorApelacionRechazadaCommand cmd(Long actaId, String actor) {
        return new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null, actor);
    }

    private DeclararCondenaFirmePorApelacionRechazadaCommand cmdConObs(
            Long actaId, String observaciones, String actor) {
        return new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, observaciones, actor);
    }

    // -------------------------------------------------------------------------
    // Records de estado observable por valor
    // -------------------------------------------------------------------------

    private record EstadoActa(
            Long id,
            int versionRow,
            ResultadoFinalActa resultadoFinal,
            SituacionAdministrativaActa situacion,
            BloqueActual bloqueActual,
            boolean estaCerrada
    ) {}

    private record EstadoFallo(
            Long id,
            int versionRow,
            Long actaId,
            Long documentoId,
            TipoFalloActa tipoFallo,
            ResultadoFalloActa resultadoFallo,
            BigDecimal montoCondena,
            Long valorizacionId,
            String fundamentos,
            LocalDateTime fhDictado,
            String idUserDictado,
            EstadoFalloActa estadoFallo,
            LocalDateTime fhFirma,
            LocalDateTime fhNotificacion,
            LocalDate fhVtoApelacion,
            boolean siApelable,
            boolean siFirme,
            LocalDateTime fhFirmeza,
            OrigenFirmezaCondena origenFirmeza,
            boolean siVigente,
            Long falloReemplazadoId,
            LocalDateTime fhAlta,
            String idUserAlta
    ) {}

    private record EstadoApelacion(
            Long id,
            int versionRow,
            Long actaId,
            Long falloId,
            EstadoApelacionActa estadoApelacion,
            ResultadoResolucionApelacion resultadoResolucion,
            LocalDateTime fhResolucion,
            String idUserResolucion,
            Long documentoResolucionId,
            String fundamentosResolucion,
            String observacionesResolucion,
            LocalDateTime fhRegistro,
            String idUserRegistro,
            LocalDateTime fhAlta,
            String idUserAlta,
            LocalDateTime fhUltMod,
            String idUserUltMod
    ) {}

    private record EstadoEventos(
            long total,
            long confir,
            long plavnc,
            long cierra,
            long pcoinf,
            long pcocnf,
            long pcoobs,
            long aperaz
    ) {}

    private record EstadoSnapshot(
            boolean presente,
            CodigoBandeja codBandeja,
            AccionPendiente accionPendiente,
            BloqueActual bloqueActual
    ) {}

    private record EstadoObservable(
            EstadoActa acta,
            EstadoFallo fallo,
            EstadoApelacion apelacion,
            EstadoEventos eventos,
            EstadoSnapshot snapshot
    ) {}

    // -------------------------------------------------------------------------
    // Factories de captura por valor
    // -------------------------------------------------------------------------

    private EstadoActa toEstadoActa(FalActa acta) {
        return new EstadoActa(acta.getId(), acta.getVersionRow(), acta.getResultadoFinal(),
                acta.getSituacionAdministrativa(), acta.getBloqueActual(), acta.estaCerrada());
    }

    private EstadoFallo toEstadoFallo(FalActaFallo f) {
        return new EstadoFallo(
                f.getId(), f.getVersionRow(), f.getActaId(), f.getDocumentoId(),
                f.getTipoFallo(), f.getResultadoFallo(), f.getMontoCondena(),
                f.getValorizacionId(), f.getFundamentos(),
                f.getFhDictado(), f.getIdUserDictado(), f.getEstadoFallo(),
                f.getFhFirma(), f.getFhNotificacion(), f.getFhVtoApelacion(),
                f.isSiApelable(), f.isSiFirme(), f.getFhFirmeza(), f.getOrigenFirmeza(),
                f.isSiVigente(), f.getFalloReemplazadoId(), f.getFhAlta(), f.getIdUserAlta());
    }

    private EstadoApelacion toEstadoApelacion(FalActaApelacion a) {
        return new EstadoApelacion(
                a.getId(), a.getVersionRow(), a.getActaId(), a.getFalloId(),
                a.getEstadoApelacion(), a.getResultadoResolucion(),
                a.getFhResolucion(), a.getIdUserResolucion(), a.getDocumentoResolucionId(),
                a.getFundamentosResolucion(), a.getObservacionesResolucion(),
                a.getFhRegistro(), a.getIdUserRegistro(),
                a.getFhAlta(), a.getIdUserAlta(),
                a.getFhUltMod(), a.getIdUserUltMod());
    }

    private EstadoEventos toEstadoEventos(List<FalActaEvento> evts) {
        return new EstadoEventos(
                evts.size(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOCNF).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOOBS).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.APERAZ).count());
    }

    private EstadoSnapshot toEstadoSnapshot(FalActaSnapshot snap) {
        if (snap == null) return new EstadoSnapshot(false, null, null, null);
        return new EstadoSnapshot(true, snap.getCodBandeja(), snap.getAccionPendiente(),
                snap.getBloqueActual());
    }

    /**
     * Captura el estado observable completo por valor.
     * Si no existe fallo activo, EstadoFallo = null.
     * Si no existe apelacion para el fallo activo, EstadoApelacion = null.
     */
    private EstadoObservable capturarEstado(Long actaId) {
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElse(null);
        FalActaApelacion ape = fallo != null
                ? apelacionRepo.buscarPorFallo(fallo.getId()).orElse(null)
                : null;
        List<FalActaEvento> evts = eventoRepo.buscarPorActa(actaId);
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElse(null);
        return new EstadoObservable(
                toEstadoActa(acta),
                fallo != null ? toEstadoFallo(fallo) : null,
                ape != null ? toEstadoApelacion(ape) : null,
                toEstadoEventos(evts),
                toEstadoSnapshot(snap));
    }

    /** Captura la apelacion por ID para tests donde la apelacion es historica/inconsistente. */
    private EstadoApelacion capturarApelacionPorId(Long apelacionId) {
        FalActaApelacion a = apelacionRepo.findById(apelacionId).orElse(null);
        return a != null ? toEstadoApelacion(a) : null;
    }

    /** Captura la apelacion asociada al fallo indicado. */
    private EstadoApelacion capturarApelacionPorFallo(Long falloId) {
        FalActaApelacion a = apelacionRepo.buscarPorFallo(falloId).orElse(null);
        return a != null ? toEstadoApelacion(a) : null;
    }

    /**
     * Verifica que el estado observable completo no cambio respecto al capturado antes.
     * Usa isEqualTo sobre el record completo (comparacion por valor en todos los campos).
     */
    private void assertEstadoSinCambios(EstadoObservable antes, Long actaId) {
        assertThat(capturarEstado(actaId)).isEqualTo(antes);
    }

    // =========================================================================
    // 14.1 Camino feliz: RESUELTA + RECHAZADA
    // =========================================================================

    @Nested
    @DisplayName("14.1 Camino feliz: RESUELTA + RECHAZADA")
    class HP1_ResueRechazada {

        @Test
        @DisplayName("HP-1: actor con espacios se normaliza; CountingClock = 1; ComandoResultado exacto")
        void hp1_actor_normalizado_reloj_resultado() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-001");
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();

            testClock.invocaciones.set(0);
            ComandoResultado resultado = firmezaService.declararFirmePorApelacionRechazada(
                    cmdConObs(actaId, "Obs HP1", "  juez-001  "));

            assertThat(testClock.invocaciones()).as("CountingClock = 1").isEqualTo(1);
            assertThat(resultado.idActa()).isEqualTo(actaId);
            assertThat(resultado.idEntidadAfectada()).isEqualTo(String.valueOf(falloId));
            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.CONFIR.codigo());
            assertThat(resultado.descripcion())
                    .as("mensaje exacto del ComandoResultado")
                    .isEqualTo("Condena firme declarada por apelacion rechazada.")
                    .doesNotContain("Slice 3E").doesNotContain("Slice 3D");
        }

        @Test
        @DisplayName("HP-2: fallo final: cambios exactos; campos inmutables idénticos (por valor)")
        void hp2_fallo_final_completo() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-002");

            // Captura por valor ANTES del comando
            EstadoFallo antes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            // Captura por valor DESPUÉS del comando (diferente instancia)
            EstadoFallo despues = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());
            LocalDateTime instante = FaltasClockTestSupport.FIXED.now();

            // Campos que cambian exactamente
            assertThat(despues.estadoFallo()).as("estadoFallo -> FIRME").isEqualTo(EstadoFalloActa.FIRME);
            assertThat(despues.siFirme()).as("siFirme -> true").isTrue();
            assertThat(despues.fhFirmeza()).as("fhFirmeza = instante canonico").isEqualTo(instante);
            assertThat(despues.origenFirmeza())
                    .as("origenFirmeza = APELACION_RECHAZADA")
                    .isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);
            assertThat(despues.versionRow())
                    .as("versionRow += 1").isEqualTo(antes.versionRow() + 1);

            // Campos que permanecen identicos
            assertThat(despues.id()).as("id preservado").isEqualTo(antes.id());
            assertThat(despues.actaId()).as("actaId preservado").isEqualTo(antes.actaId());
            assertThat(despues.documentoId()).as("documentoId preservado").isEqualTo(antes.documentoId());
            assertThat(despues.tipoFallo()).as("tipoFallo = CONDENATORIO").isEqualTo(TipoFalloActa.CONDENATORIO);
            assertThat(despues.resultadoFallo()).as("resultadoFallo = CONDENA").isEqualTo(ResultadoFalloActa.CONDENA);
            assertThat(despues.montoCondena())
                    .as("montoCondena preservado").isEqualByComparingTo(antes.montoCondena());
            assertThat(despues.valorizacionId()).as("valorizacionId preservado").isEqualTo(antes.valorizacionId());
            assertThat(despues.fundamentos()).as("fundamentos preservado").isEqualTo(antes.fundamentos());
            assertThat(despues.fhDictado()).as("fhDictado preservado").isEqualTo(antes.fhDictado());
            assertThat(despues.idUserDictado()).as("idUserDictado preservado").isEqualTo(antes.idUserDictado());
            assertThat(despues.fhFirma()).as("fhFirma preservado (not null)").isNotNull().isEqualTo(antes.fhFirma());
            assertThat(despues.fhNotificacion())
                    .as("fhNotificacion preservado (not null)").isNotNull().isEqualTo(antes.fhNotificacion());
            assertThat(despues.fhVtoApelacion()).as("fhVtoApelacion preservado").isEqualTo(antes.fhVtoApelacion());
            assertThat(despues.siApelable()).as("siApelable preservado").isEqualTo(antes.siApelable());
            assertThat(despues.siVigente()).as("siVigente preservado").isEqualTo(antes.siVigente());
            assertThat(despues.falloReemplazadoId()).as("falloReemplazadoId preservado").isEqualTo(antes.falloReemplazadoId());
            assertThat(despues.fhAlta()).as("fhAlta preservado").isEqualTo(antes.fhAlta());
            assertThat(despues.idUserAlta()).as("idUserAlta preservado").isEqualTo(antes.idUserAlta());
        }

        @Test
        @DisplayName("HP-3: acta final: CONDENA_FIRME, ACTIVA, no cerrada; bloqueActual intacto; versionRow+1")
        void hp3_acta_final_correcta() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-003");

            // Capturar valores relevantes del acta ANTES del comando
            FalActa actaAntes = actaRepo.buscarPorId(actaId).orElseThrow();
            BloqueActual bloqueAntes = actaAntes.getBloqueActual();
            int verAntes = actaAntes.getVersionRow();

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.estaCerrada()).as("estaCerrada = false").isFalse();
            assertThat(acta.getBloqueActual()).as("bloqueActual no cambio").isEqualTo(bloqueAntes);
            assertThat(acta.getVersionRow()).as("versionRow += 1").isEqualTo(verAntes + 1);
        }

        @Test
        @DisplayName("HP-4: apelacion completamente preservada (por valor, isEqualTo)")
        void hp4_apelacion_completamente_preservada() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-004");
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();

            // Captura por valor ANTES
            EstadoApelacion antes = capturarApelacionPorFallo(falloId);
            assertThat(antes).isNotNull();

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            // Captura por valor DESPUÉS: debe ser idéntica
            assertThat(capturarApelacionPorFallo(falloId)).as("apelacion intacta").isEqualTo(antes);
        }

        @Test
        @DisplayName("HP-5: CMD-FALLO-006 agrega exactamente un CONFIR; cero eventos nuevos de otros tipos")
        void hp5_exactamente_un_confir_sin_plavnc() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-005");
            // Capturar contadores ANTES (el fixture ya pudo registrar APERAZ via resolverRechazada)
            EstadoEventos antes = toEstadoEventos(eventoRepo.buscarPorActa(actaId));

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            EstadoEventos despues = toEstadoEventos(eventoRepo.buscarPorActa(actaId));

            assertThat(despues.total())
                    .as("solo CONFIR fue agregado").isEqualTo(antes.total() + 1);
            assertThat(despues.confir()).as("exactamente un CONFIR nuevo").isEqualTo(antes.confir() + 1);
            assertThat(despues.plavnc()).as("cero PLAVNC nuevos").isEqualTo(antes.plavnc());
            assertThat(despues.cierra()).as("cero CIERRA nuevos").isEqualTo(antes.cierra());
            assertThat(despues.pcoinf()).as("cero PCOINF nuevos").isEqualTo(antes.pcoinf());
            assertThat(despues.pcocnf()).as("cero PCOCNF nuevos").isEqualTo(antes.pcocnf());
            assertThat(despues.pcoobs()).as("cero PCOOBS nuevos").isEqualTo(antes.pcoobs());
            assertThat(despues.aperaz()).as("cero APERAZ nuevos por CMD-FALLO-006").isEqualTo(antes.aperaz());
        }

        @Test
        @DisplayName("HP-6: CONFIR actor, origen, instante e identidad exactos")
        void hp6_confir_actor_origen_instante() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-006");

            firmezaService.declararFirmePorApelacionRechazada(
                    cmdConObs(actaId, null, "  juez-confir  "));

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            FalActaEvento confir = eventos.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR)
                    .findFirst().orElseThrow();

            LocalDateTime instante = FaltasClockTestSupport.FIXED.now();
            assertThat(confir.fhEvt()).isEqualTo(instante);
            assertThat(confir.idUserEvt()).isEqualTo("juez-confir");
            assertThat(confir.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);
            assertThat(confir.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
            assertThat(confir.actaId()).isEqualTo(actaId);
        }

        @Test
        @DisplayName("HP-7: observaciones incorporadas exactamente una vez en CONFIR; sin espacio sobrante si null")
        void hp7_observaciones_exactamente_una_vez() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-007");
            Long actaId2 = crearActaConApelacionResueltaRechazada("HP1-007b");

            firmezaService.declararFirmePorApelacionRechazada(
                    cmdConObs(actaId, "obs-exacta", "juez-001"));

            FalActaEvento confir = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).findFirst().orElseThrow();
            long ocurrencias = countOccurrences(confir.descripcionLegible(), "obs-exacta");
            assertThat(ocurrencias).as("observacion aparece exactamente una vez").isEqualTo(1);

            firmezaService.declararFirmePorApelacionRechazada(
                    cmdConObs(actaId2, null, "juez-001"));

            FalActaEvento confir2 = eventoRepo.buscarPorActa(actaId2).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).findFirst().orElseThrow();
            assertThat(confir2.descripcionLegible()).doesNotEndWith(" ");
        }

        @Test
        @DisplayName("HP-8: snapshot exacto: PENDIENTE_PAGO_CONDENA / GESTIONAR_PAGO_CONDENA")
        void hp8_snapshot_exacto() {
            Long actaId = crearActaConApelacionResueltaRechazada("HP1-008");

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }
    }

    // =========================================================================
    // 14.2 Camino feliz: RECHAZADA (estado legacy)
    // =========================================================================

    @Nested
    @DisplayName("14.2 Camino feliz: estado legacy RECHAZADA")
    class HP2_LegacyRechazada {

        @Test
        @DisplayName("HP-L1: apelacion RECHAZADA (legacy) habilita firmeza; no la modifica (por valor, isEqualTo)")
        void hpl1_apelacion_rechazada_legacy() {
            Long actaId = crearActaConApelacionLegacyRechazada("HP2-001");
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();

            EstadoApelacion apeAntes = capturarApelacionPorFallo(falloId);
            assertThat(apeAntes).isNotNull();
            assertThat(apeAntes.estadoApelacion()).isEqualTo(EstadoApelacionActa.RECHAZADA);

            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));

            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(fallo.isSiFirme()).isTrue();
            assertThat(fallo.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);

            // Apelacion completamente intacta (por valor)
            assertThat(capturarApelacionPorFallo(falloId)).as("apelacion legacy intacta").isEqualTo(apeAntes);
        }
    }

    // =========================================================================
    // 14.3 Validacion estructural pre-reloj
    // =========================================================================

    @Nested
    @DisplayName("14.3 Validacion estructural pre-reloj")
    class V_Estructural {

        @Test
        @DisplayName("V-1: cmd null -> IllegalArgumentException; CountingClock = 0; acta sentinel intacta")
        void v1_cmd_null() {
            Long actaId = crearActaConApelacionResueltaRechazada("V1-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-2: actaId null -> PrecondicionVioladaException; CountingClock = 0; acta intacta")
        void v2_actaId_null() {
            Long actaId = crearActaConApelacionResueltaRechazada("V2-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(null, null, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actaId");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-3: actor null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v3_actor_null() {
            Long actaId = crearActaConApelacionResueltaRechazada("V3-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-4: actor blank -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v4_actor_blank() {
            Long actaId = crearActaConApelacionResueltaRechazada("V4-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null, "   ")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-5: actor.trim > 36 -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v5_actor_muy_largo() {
            Long actaId = crearActaConApelacionResueltaRechazada("V5-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(
                            actaId, null, "a".repeat(37))))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }
    }

    // =========================================================================
    // 14.4 Precondiciones de acta y fallo
    // =========================================================================

    @Nested
    @DisplayName("14.4 Precondiciones de acta y fallo")
    class P_ActaFallo {

        @Test
        @DisplayName("P-1: acta inexistente -> ActaNoEncontradaException; CountingClock = 0; sentinel intacto")
        void p1_acta_inexistente() {
            // Acta sentinel para demostrar que el estado global no se altera
            Long sentinelId = crearActaConApelacionResueltaRechazada("P1-sentinel");
            EstadoObservable sentinelAntes = capturarEstado(sentinelId);

            // El ID inexistente no puede tener eventos, fallo, snapshot ni apelacion
            final Long idInexistente = 999999L;

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    cmd(idInexistente, "juez-001")))
                    .isInstanceOf(ActaNoEncontradaException.class);
            assertThat(testClock.invocaciones()).isZero();

            // Sentinel completamente intacto
            assertEstadoSinCambios(sentinelAntes, sentinelId);
            // ID inexistente: sin eventos, sin fallo, sin snapshot, sin apelacion
            assertThat(eventoRepo.buscarPorActa(idInexistente)).as("cero eventos para ID inexistente").isEmpty();
            assertThat(falloRepo.buscarActivo(idInexistente)).as("sin fallo para ID inexistente").isEmpty();
            assertThat(snapshotRepo.buscarPorActa(idInexistente)).as("sin snapshot para ID inexistente").isEmpty();
        }

        @Test
        @DisplayName("P-2: acta CERRADA -> PrecondicionVioladaException; cero efectos completo")
        void p2_acta_cerrada() {
            Long actaId = crearActaConApelacionResueltaRechazada("P2-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-3: acta ANULADA -> PrecondicionVioladaException; cero efectos completo")
        void p3_acta_anulada() {
            Long actaId = crearActaConApelacionResueltaRechazada("P3-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-4: acta ARCHIVADA -> PrecondicionVioladaException; cero efectos completo")
        void p4_acta_archivada() {
            Long actaId = crearActaConApelacionResueltaRechazada("P4-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("archivada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-5: acta PARALIZADA -> PrecondicionVioladaException; cero efectos completo")
        void p5_acta_paralizada() {
            Long actaId = crearActaConApelacionResueltaRechazada("P5-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-6: sin fallo activo -> PrecondicionVioladaException; cero efectos (fallo=null en estado)")
        void p6_sin_fallo_activo() {
            Long actaId = crearActaPreFallo("P6-001");
            EstadoObservable antes = capturarEstado(actaId);
            assertThat(antes.fallo()).as("fallo ausente antes del comando").isNull();

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fallo activo");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-7: fallo ABSOLUTORIO -> PrecondicionVioladaException; cero efectos completo")
        void p7_fallo_absolutorio() {
            Long actaId = crearActaPreFallo("P7-001");
            falloService.dictarAbsolutorio(
                    new ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand(
                            actaId, "Fundamentos abs", null));
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("condenatorio");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-8: resultadoFallo distinto de CONDENA -> PrecondicionVioladaException; cero efectos completo")
        void p8_resultado_fallo_no_condena() {
            Long actaId = crearActaConFalloCondenatorioNotificado("P8-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setResultadoFallo(ResultadoFalloActa.ABSUELVE);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-9: estadoFallo distinto de NOTIFICADO -> PrecondicionVioladaException; cero efectos completo")
        void p9_estado_fallo_no_notificado() {
            Long actaId = crearActaPreFallo("P9-001");
            falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                    actaId, new BigDecimal("1000"), "Fund", null));
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("NOTIFICADO");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-10: fhFirma null -> PrecondicionVioladaException; cero efectos completo")
        void p10_fhFirma_null() {
            Long actaId = crearActaConFalloCondenatorioNotificado("P10-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setFhFirma(null);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhFirma");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-11: fhNotificacion null -> PrecondicionVioladaException; cero efectos completo")
        void p11_fhNotificacion_null() {
            Long actaId = crearActaConFalloCondenatorioNotificado("P11-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setFhNotificacion(null);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhNotificacion");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-12: siFirme ya declarado -> PrecondicionVioladaException; estado completo intacto")
        void p12_siFirme_ya_declarado() {
            // Los tres marcadores (siFirme, fhFirmeza, origenFirmeza) nacen juntos via declararFirmeza.
            // Restauramos estadoFallo a NOTIFICADO para que el servicio llegue al check de siFirme.
            Long actaId = crearActaConFalloCondenatorioNotificado("P12-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.declararFirmeza(FaltasClockTestSupport.FIXED.now(), OrigenFirmezaCondena.APELACION_RECHAZADA);
            fallo.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
            falloRepo.guardar(fallo);

            EstadoObservable antes = capturarEstado(actaId);
            assertThat(antes.fallo()).isNotNull();
            assertThat(antes.fallo().siFirme()).as("siFirme=true antes").isTrue();
            assertThat(antes.fallo().fhFirmeza()).as("fhFirmeza presente").isNotNull();
            assertThat(antes.fallo().origenFirmeza()).as("origenFirmeza presente").isNotNull();

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firme");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-13: acta.resultadoFinal = CONDENA_FIRME -> PrecondicionVioladaException; cero efectos completo")
        void p13_condena_firme_ya() {
            Long actaId = crearActaConApelacionResueltaRechazada("P13-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firme");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }
    }

    // =========================================================================
    // 14.5 Asociacion de apelacion
    // =========================================================================

    @Nested
    @DisplayName("14.5 Asociacion de apelacion por fallo.id")
    class A_Asociacion {

        @Test
        @DisplayName("A-1: sin apelacion asociada al fallo activo -> PrecondicionVioladaException; cero efectos completo")
        void a1_sin_apelacion_fallo_activo() {
            Long actaId = crearActaConFalloCondenatorioNotificado("A1-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("A-2: solo apelacion historica de otro fallo -> no habilita; cero efectos; apelacion historica intacta")
        void a2_apelacion_historica_otro_fallo() {
            Long actaId = crearActaConFalloCondenatorioNotificado("A2-001");
            Long otroFalloId = 99999L;
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            @SuppressWarnings("deprecation")
            FalActaApelacion apeOtroFallo = new FalActaApelacion(
                    apelacionRepo.nextId(), actaId, otroFalloId,
                    EstadoApelacionActa.RECHAZADA,
                    ahora, "Infractor", "Fundamentos otro fallo", null, false,
                    ahora, "test-user");
            apelacionRepo.guardar(apeOtroFallo);
            Long apelacionId = apeOtroFallo.getId();

            // Estado observable: apelacion=null (busca por falloActivo, no por otroFalloId)
            EstadoObservable antes = capturarEstado(actaId);
            assertThat(antes.apelacion()).as("apelacion historica fuera del fallo activo").isNull();
            // Captura explícita de la apelacion histórica por ID
            EstadoApelacion apeHistoricaAntes = capturarApelacionPorId(apelacionId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
            // Apelacion historica intacta
            assertThat(capturarApelacionPorId(apelacionId))
                    .as("apelacion historica intacta").isEqualTo(apeHistoricaAntes);
        }

        @Test
        @DisplayName("A-3: apelacion con actaId inconsistente -> PrecondicionVioladaException; cero efectos; apelacion intacta")
        void a3_apelacion_actaId_inconsistente() {
            Long actaId = crearActaConFalloCondenatorioNotificado("A3-001");
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
            Long actaIdFalsa = 88888L;
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            @SuppressWarnings("deprecation")
            FalActaApelacion apeConActaIdMal = new FalActaApelacion(
                    apelacionRepo.nextId(), actaIdFalsa, falloId,
                    EstadoApelacionActa.RECHAZADA,
                    ahora, "Infractor", "Fundamentos", null, false,
                    ahora, "test-user");
            apelacionRepo.guardar(apeConActaIdMal);
            Long apelacionId = apeConActaIdMal.getId();

            // capturarEstado encuentra la apelacion (buscarPorFallo(falloId) la encuentra)
            EstadoObservable antes = capturarEstado(actaId);
            assertThat(antes.apelacion()).as("apelacion encontrada por falloId").isNotNull();
            EstadoApelacion apeAntes = capturarApelacionPorId(apelacionId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("acta");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
            // Apelacion con actaId inconsistente intacta
            assertThat(capturarApelacionPorId(apelacionId))
                    .as("apelacion inconsistente intacta").isEqualTo(apeAntes);
        }

        @Test
        @DisplayName("A-4: buscarPorFallo gobierna; buscarUltima no se usa para decidir; cero efectos; apelacion historica intacta")
        void a4_buscarPorFallo_gobierna() {
            Long actaId = crearActaConFalloCondenatorioNotificado("A4-001");
            Long falloHistoricoId = 77777L;
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            @SuppressWarnings("deprecation")
            FalActaApelacion apeHistorica = new FalActaApelacion(
                    apelacionRepo.nextId(), actaId, falloHistoricoId,
                    EstadoApelacionActa.RECHAZADA,
                    ahora, "Infractor", "Fundamentos historica", null, false,
                    ahora, "test-user");
            apelacionRepo.guardar(apeHistorica);
            Long apelacionId = apeHistorica.getId();

            // buscarUltima encontraria apeHistorica; buscarPorFallo(falloActivo.id) no la encuentra
            EstadoObservable antes = capturarEstado(actaId);
            assertThat(antes.apelacion()).as("apelacion historica fuera del fallo activo").isNull();
            EstadoApelacion apeHistoricaAntes = capturarApelacionPorId(apelacionId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
            // Apelacion historica intacta
            assertThat(capturarApelacionPorId(apelacionId))
                    .as("apelacion historica intacta").isEqualTo(apeHistoricaAntes);
        }
    }

    // =========================================================================
    // 14.6 Estados/resultados invalidos de la apelacion
    // =========================================================================

    @Nested
    @DisplayName("14.6 Estados/resultados invalidos de la apelacion")
    class E_EstadosInvalidos {

        @SuppressWarnings("deprecation")
        private Long crearActaConApelacionEstado(String docNum, EstadoApelacionActa estado,
                ResultadoResolucionApelacion resultado) {
            Long actaId = crearActaConFalloCondenatorioNotificado(docNum);
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            FalActaApelacion ape = new FalActaApelacion(
                    apelacionRepo.nextId(), actaId, falloId,
                    estado, ahora, "Infractor", "Fundamentos", null, false,
                    ahora, "test-user");
            if (resultado != null) {
                ape.setResultadoResolucion(resultado);
            }
            apelacionRepo.guardar(ape);
            return actaId;
        }

        private void assertRechazaConCeroEfectos(Long actaId) {
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001")))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
            testClock.invocaciones.set(0);
        }

        @Test
        @DisplayName("E-1: PRESENTADA -> PrecondicionVioladaException; cero efectos")
        void e1_presentada() {
            Long actaId = crearActaConApelacionEstado("E1-001", EstadoApelacionActa.PRESENTADA, null);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-2: EN_ANALISIS -> PrecondicionVioladaException; cero efectos")
        void e2_en_analisis() {
            Long actaId = crearActaConApelacionEstado("E2-001", EstadoApelacionActa.EN_ANALISIS, null);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-3: ACEPTADA_ABSUELVE -> PrecondicionVioladaException; cero efectos")
        void e3_aceptada_absuelve() {
            Long actaId = crearActaConApelacionEstado("E3-001", EstadoApelacionActa.ACEPTADA_ABSUELVE, null);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-4: SIN_EFECTO -> PrecondicionVioladaException; cero efectos")
        void e4_sin_efecto() {
            Long actaId = crearActaConApelacionEstado("E4-001", EstadoApelacionActa.SIN_EFECTO, null);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-5: RESUELTA + ACEPTADA_ABSUELVE -> PrecondicionVioladaException; cero efectos")
        void e5_resuelta_aceptada_absuelve() {
            Long actaId = crearActaConApelacionEstado("E5-001", EstadoApelacionActa.RESUELTA,
                    ResultadoResolucionApelacion.ACEPTADA_ABSUELVE);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-6: RESUELTA + MODIFICA_CONDENA -> PrecondicionVioladaException; cero efectos")
        void e6_resuelta_modifica_condena() {
            Long actaId = crearActaConApelacionEstado("E6-001", EstadoApelacionActa.RESUELTA,
                    ResultadoResolucionApelacion.MODIFICA_CONDENA);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-7: RESUELTA + NULIDAD -> PrecondicionVioladaException; cero efectos")
        void e7_resuelta_nulidad() {
            Long actaId = crearActaConApelacionEstado("E7-001", EstadoApelacionActa.RESUELTA,
                    ResultadoResolucionApelacion.NULIDAD);
            assertRechazaConCeroEfectos(actaId);
        }

        @Test
        @DisplayName("E-8: RESUELTA + resultado null -> PrecondicionVioladaException; cero efectos")
        void e8_resuelta_resultado_null() {
            Long actaId = crearActaConApelacionEstado("E8-001", EstadoApelacionActa.RESUELTA, null);
            assertRechazaConCeroEfectos(actaId);
        }
    }

    // =========================================================================
    // 14.7 Repeticion secuencial
    // =========================================================================

    @Nested
    @DisplayName("14.7 Repeticion secuencial")
    class DS_Secuencial {

        @Test
        @DisplayName("DS-1: primer exito; capturar estado; segundo rechazado; estado identico al del exito")
        void ds1_repeticion_secuencial() {
            Long actaId = crearActaConApelacionResueltaRechazada("DS1-001");

            testClock.invocaciones.set(0);
            firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-001"));
            assertThat(testClock.invocaciones()).isEqualTo(1);

            // Capturar estado observable completo despues del exito
            EstadoObservable despuesExito = capturarEstado(actaId);
            assertThat(despuesExito.eventos().confir()).as("un CONFIR tras exito").isEqualTo(1);
            assertThat(despuesExito.eventos().plavnc()).as("cero PLAVNC tras exito").isZero();
            assertThat(despuesExito.snapshot().codBandeja())
                    .isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(despuesExito.snapshot().accionPendiente())
                    .isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);

            // Segundo intento -> rechazo
            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-002")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firme");

            // CountingClock no avanzo en el segundo intento
            assertThat(testClock.invocaciones()).as("CountingClock total = 1").isEqualTo(1);

            // Estado observable completo identico al capturado tras el exito
            assertThat(capturarEstado(actaId))
                    .as("estado identico al del exito tras segundo rechazo")
                    .isEqualTo(despuesExito);
        }
    }

    // =========================================================================
    // 14.8 Concurrencia InMemory
    // =========================================================================

    @Nested
    @DisplayName("14.8 Concurrencia InMemory: exactamente un ganador")
    class CC_Concurrencia {

        @Test
        @DisplayName("CC-1: dos CMD-FALLO-006 concurrentes; exitos=1; rechazos=1; errores=0; CountingClock=1")
        void cc1_un_ganador() throws Exception {
            Long actaId = crearActaConApelacionResueltaRechazada("CC1-001");
            Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();

            int falloVerAntes = falloRepo.buscarActivo(actaId).orElseThrow().getVersionRow();
            int actaVerAntes = actaRepo.buscarPorId(actaId).orElseThrow().getVersionRow();
            // Captura de la apelacion por valor antes de los hilos
            EstadoApelacion apelacionAntes = capturarApelacionPorFallo(falloId);
            assertThat(apelacionAntes).isNotNull();

            testClock.invocaciones.set(0);

            CyclicBarrier barrera = new CyclicBarrier(2);
            List<Exception> erroresInesperados = new java.util.concurrent.CopyOnWriteArrayList<>();
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger rechazosPrecondicion = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> f1 = executor.submit(() -> {
                    try {
                        barrera.await();
                        firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-A"));
                        exitos.incrementAndGet();
                    } catch (PrecondicionVioladaException e) {
                        rechazosPrecondicion.incrementAndGet();
                    } catch (Exception e) {
                        erroresInesperados.add(e);
                    }
                });
                Future<?> f2 = executor.submit(() -> {
                    try {
                        barrera.await();
                        firmezaService.declararFirmePorApelacionRechazada(cmd(actaId, "juez-B"));
                        exitos.incrementAndGet();
                    } catch (PrecondicionVioladaException e) {
                        rechazosPrecondicion.incrementAndGet();
                    } catch (Exception e) {
                        erroresInesperados.add(e);
                    }
                });
                f1.get();
                f2.get();
            } finally {
                executor.shutdown();
            }

            assertThat(erroresInesperados).as("sin errores inesperados").isEmpty();
            assertThat(exitos.get()).as("exactamente un exito").isEqualTo(1);
            assertThat(rechazosPrecondicion.get()).as("exactamente un rechazo PrecondicionViolada").isEqualTo(1);
            assertThat(testClock.invocaciones()).as("CountingClock = 1").isEqualTo(1);

            // Fallo actualizado exactamente una vez
            FalActaFallo falloDespues = falloRepo.buscarActivo(actaId).orElseThrow();
            assertThat(falloDespues.getVersionRow()).as("fallo.versionRow += 1").isEqualTo(falloVerAntes + 1);
            assertThat(falloDespues.isSiFirme()).isTrue();
            assertThat(falloDespues.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);

            // Acta actualizada exactamente una vez
            assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getVersionRow())
                    .as("acta.versionRow += 1").isEqualTo(actaVerAntes + 1);

            // Eventos exactos
            EstadoEventos evts = toEstadoEventos(eventoRepo.buscarPorActa(actaId));
            assertThat(evts.confir()).as("exactamente un CONFIR").isEqualTo(1);
            assertThat(evts.plavnc()).as("cero PLAVNC").isZero();
            assertThat(evts.cierra()).as("cero CIERRA").isZero();
            assertThat(evts.pcoinf()).as("cero PCOINF").isZero();
            assertThat(evts.pcocnf()).as("cero PCOCNF").isZero();
            assertThat(evts.pcoobs()).as("cero PCOOBS").isZero();

            // Snapshot exacto
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);

            // Apelacion completamente intacta (por valor, todos los campos)
            assertThat(capturarApelacionPorFallo(falloId))
                    .as("apelacion identica al valor previo").isEqualTo(apelacionAntes);
        }
    }

    // =========================================================================
    // Util
    // =========================================================================

    private static long countOccurrences(String text, String search) {
        if (text == null || search == null || search.isEmpty()) return 0;
        long count = 0;
        int idx = 0;
        while ((idx = text.indexOf(search, idx)) != -1) {
            count++;
            idx += search.length();
        }
        return count;
    }
}
