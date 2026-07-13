package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests canonicos de CMD-FALLO-007: Informar pago de condena.
 *
 * Sin Mockito. Repositorios InMemory reales. CountingClock para pagoCondenaService.
 *
 * Cubre: camino feliz (pago creado/actualizado, evento PCOINF, snapshot), validacion
 * estructural pre-reloj, precondiciones, re-informe secuencial, concurrencia InMemory,
 * y null guard de recalcular(acta, ahora).
 *
 * CountingClock garantiza que faltasClock.now() se invoca exactamente una vez por
 * llamada exitosa a informar (instante canonico ahora).
 * El overload recalcular(acta, ahora) no consulta el reloj internamente.
 */
@DisplayName("GAP-CMD-FALLO-007: Informar pago de condena (canonical)")
class PagoCondenaInformarCanonicaTest {

    // -------------------------------------------------------------------------
    // CountingClock
    // -------------------------------------------------------------------------

    static class CountingClock extends FaltasClock {
        final AtomicInteger invocaciones = new AtomicInteger(0);

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
    private FirmezaCondenaService firmezaService;
    private PagoCondenaService pagoCondenaService;

    /** Disponible como campo para el test de null guard. */
    private SnapshotRecalculador snapshotRecalc;

    private CountingClock testClock;

    // -------------------------------------------------------------------------
    // setUp
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
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

        snapshotRecalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo,
                pagoCondRepo, FaltasClockTestSupport.FIXED);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, snapshotRecalc,
                new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, snapshotRecalc, falloRepo,
                new InMemoryDocumentoPlantillaRepository(),
                new TalonarioService(new InMemoryTalonarioRepository(),
                        new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(),
                        FaltasClockTestSupport.FIXED),
                new InMemoryDependenciaRepository(),
                new InMemoryDocumentoFirmaReqRepository(),
                new InMemoryFirmanteRepository(),
                notifRepo, FaltasClockTestSupport.FIXED);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, snapshotRecalc,
                falloRepo, actaId -> false, FaltasClockTestSupport.FIXED, intentoRepo,
                new InMemoryPersonaDomicilioRepository(),
                PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo,
                snapshotRecalc, FaltasClockTestSupport.FIXED);

        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                snapshotRecalc, FaltasClockTestSupport.FIXED);

        testClock = new CountingClock();

        pagoCondenaService = new PagoCondenaService(
                actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondRepo, snapshotRecalc,
                new NoOpBloqueantesMaterialesChecker(), testClock);
    }

    // -------------------------------------------------------------------------
    // Helpers de fixture
    // -------------------------------------------------------------------------

    private Long crearActaConCondenaFirme(String docNum) {
        Long actaId = actaService.labrar(new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 100", "San Martin 200",
                null, null, null, "Infractor Test", docNum,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION, null))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "firmante1", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), CanalNotificacion.PRESENCIAL,
                        null, null, null, "test-user")).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotif),
                IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)),
                null, "test-actor"));

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

        var falloVto = falloRepo.buscarActivo(actaId).orElseThrow();
        falloVto.setFhVtoApelacion(LocalDate.of(2026, 7, 8));
        falloRepo.guardar(falloVto);
        firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));
        return actaId;
    }

    private InformarPagoCondenaCommand cmd(Long actaId) {
        return new InformarPagoCondenaCommand(actaId, new BigDecimal("3000.00"), "REF-HP-001", null, "test-actor");
    }

    private InformarPagoCondenaCommand cmdConObs(Long actaId, BigDecimal monto, String ref,
                                                  String obs, String actor) {
        return new InformarPagoCondenaCommand(actaId, monto, ref, obs, actor);
    }

    /**
     * Crea un FalActaFallo CONDENATORIO persistido en estado FIRME pero sin los marcadores de firmeza
     * (siFirme=false, fhFirmeza=null, origenFirmeza=null). Simula datos migrados inconsistentes.
     * Usa exclusivamente API publica existente; no usa reflection ni Unsafe.
     * El nuevo fallo es el activo; el anterior queda siVigente=false.
     */
    private FalActaFallo reemplazarPorFalloFirmeSinMarcadores(Long actaId, long offset) {
        FalActaFallo existente = falloRepo.buscarActivo(actaId).orElseThrow();
        existente.setSiVigente(false);
        falloRepo.guardar(existente);

        LocalDateTime now = FaltasClockTestSupport.FIXED.now();
        FalActaFallo nuevoFallo = new FalActaFallo(
                existente.getId() + offset, actaId,
                TipoFalloActa.CONDENATORIO, now, now, "test-fixture");
        nuevoFallo.setResultadoFallo(ResultadoFalloActa.CONDENA);
        nuevoFallo.setEstadoFallo(EstadoFalloActa.FIRME);
        nuevoFallo.setFhFirma(now);
        nuevoFallo.setFhNotificacion(now);
        nuevoFallo.setMontoCondena(existente.getMontoCondena());
        nuevoFallo.setFundamentos(existente.getFundamentos());
        falloRepo.guardar(nuevoFallo);
        return nuevoFallo;
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
            EstadoProcesalActa estadoProcesal,
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

    private record EstadoPago(
            String id,
            Long actaId,
            EstadoPagoCondena estado,
            BigDecimal monto,
            String referenciaPago,
            String observaciones,
            LocalDateTime fechaInforme,
            String motivoObservacion,
            LocalDateTime fechaObservacion,
            LocalDateTime fechaConfirmacion
    ) {}

    private record EstadoEventos(
            long total,
            long pcoinf,
            long pcocnf,
            long pcoobs,
            long confir,
            long cierra,
            long plavnc
    ) {}

    private record EstadoSnapshot(
            boolean presente,
            CodigoBandeja codBandeja,
            AccionPendiente accionPendiente,
            ResultadoFinalActa resultadoFinal,
            LocalDateTime ultimaActualizacion,
            BloqueActual bloqueActual
    ) {}

    private record EstadoObservable(
            EstadoActa acta,
            EstadoFallo fallo,
            EstadoPago pago,
            EstadoEventos eventos,
            EstadoSnapshot snapshot
    ) {}

    // -------------------------------------------------------------------------
    // Factories de captura por valor
    // -------------------------------------------------------------------------

    private EstadoActa toEstadoActa(FalActa acta) {
        return new EstadoActa(acta.getId(), acta.getVersionRow(),
                acta.getResultadoFinal(), acta.getSituacionAdministrativa(),
                acta.getBloqueActual(), acta.getEstadoProcesal(), acta.estaCerrada());
    }

    private EstadoFallo toEstadoFallo(FalActaFallo f) {
        if (f == null) return null;
        return new EstadoFallo(f.getId(), f.getVersionRow(), f.getActaId(), f.getDocumentoId(),
                f.getTipoFallo(), f.getResultadoFallo(), f.getMontoCondena(), f.getValorizacionId(),
                f.getFundamentos(), f.getFhDictado(), f.getIdUserDictado(), f.getEstadoFallo(),
                f.getFhFirma(), f.getFhNotificacion(), f.getFhVtoApelacion(), f.isSiApelable(),
                f.isSiFirme(), f.getFhFirmeza(), f.getOrigenFirmeza(), f.isSiVigente(),
                f.getFalloReemplazadoId(), f.getFhAlta(), f.getIdUserAlta());
    }

    private EstadoPago toEstadoPago(FalPagoCondena p) {
        if (p == null) return null;
        return new EstadoPago(p.getId(), p.getActaId(), p.getEstadoPagoCondena(),
                p.getMonto(), p.getReferenciaPago(), p.getObservaciones(), p.getFechaInforme(),
                p.getMotivoObservacion(), p.getFechaObservacion(), p.getFechaConfirmacion());
    }

    private EstadoEventos toEstadoEventos(List<FalActaEvento> evts) {
        return new EstadoEventos(
                evts.size(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOCNF).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PCOOBS).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count(),
                evts.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).count());
    }

    private EstadoSnapshot toEstadoSnapshot(FalActaSnapshot snap) {
        if (snap == null) return new EstadoSnapshot(false, null, null, null, null, null);
        return new EstadoSnapshot(true, snap.getCodBandeja(), snap.getAccionPendiente(),
                snap.getResultadoFinal(), snap.getUltimaActualizacion(), snap.getBloqueActual());
    }

    private EstadoObservable capturarEstado(Long actaId) {
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElse(null);
        FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElse(null);
        List<FalActaEvento> evts = eventoRepo.buscarPorActa(actaId);
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElse(null);
        return new EstadoObservable(toEstadoActa(acta), toEstadoFallo(fallo), toEstadoPago(pago),
                toEstadoEventos(evts), toEstadoSnapshot(snap));
    }

    private void assertEstadoSinCambios(EstadoObservable antes, Long actaId) {
        assertThat(capturarEstado(actaId)).isEqualTo(antes);
    }

    // =========================================================================
    // HP: Camino feliz
    // =========================================================================

    @Nested
    @DisplayName("HP: Camino feliz")
    class HP_CaminoFeliz {

        @Test
        @DisplayName("HP-1: actor con espacios se normaliza; CountingClock = 1; ComandoResultado exacto; acta y fallo intactos")
        void hp1_actor_normalizado_reloj_resultado() {
            Long actaId = crearActaConCondenaFirme("HP1-001");
            EstadoActa actaAntes = toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow());
            EstadoFallo falloAntes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());

            testClock.invocaciones.set(0);
            ComandoResultado resultado = pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-HP-001", null, "  test-actor  "));

            assertThat(testClock.invocaciones()).as("CountingClock = 1").isEqualTo(1);
            assertThat(resultado.idActa()).isEqualTo(actaId);
            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.PCOINF.codigo());
            assertThat(resultado.descripcion()).isEqualTo("Pago de condena informado. Pendiente de confirmacion.");

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(resultado.idEntidadAfectada()).isEqualTo(pago.getId());

            assertThat(toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow()))
                    .as("acta intacta").isEqualTo(actaAntes);
            assertThat(toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow()))
                    .as("fallo intacto").isEqualTo(falloAntes);
        }

        @Test
        @DisplayName("HP-2: FalPagoCondena fields exactos: id no null, monto, referencia, estado INFORMADO, fechaInforme=ahora, fechaConfirmacion/motivoObservacion/fechaObservacion null")
        void hp2_pago_fields_exactos() {
            Long actaId = crearActaConCondenaFirme("HP1-002");
            LocalDateTime instante = FaltasClockTestSupport.FIXED.now();

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("5000.50"), "REF-HP-002", "Obs-HP2", "juez-001"));

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getId()).as("id no null ni blank").isNotNull().isNotBlank();
            assertThat(pago.getActaId()).isEqualTo(actaId);
            assertThat(pago.getMonto()).isEqualByComparingTo(new BigDecimal("5000.50"));
            assertThat(pago.getReferenciaPago()).isEqualTo("REF-HP-002");
            assertThat(pago.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);
            assertThat(pago.getFechaInforme()).as("fechaInforme = instante canonico").isEqualTo(instante);
            assertThat(pago.getObservaciones()).isEqualTo("Obs-HP2");
            assertThat(pago.getFechaConfirmacion()).as("fechaConfirmacion null en pago nuevo").isNull();
            assertThat(pago.getMotivoObservacion()).as("motivoObservacion null en pago nuevo").isNull();
            assertThat(pago.getFechaObservacion()).as("fechaObservacion null en pago nuevo").isNull();
        }

        @Test
        @DisplayName("HP-3: evento PCOINF atributos exactos; descripcion con referencia, monto, obs una vez, sin 'null'")
        void hp3_pcoinf_atributos_exactos() {
            Long actaId = crearActaConCondenaFirme("HP1-003");
            LocalDateTime instante = FaltasClockTestSupport.FIXED.now();

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-HP-003", "obs-hp3", "  juez-confir  "));

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            FalActaEvento pcoinf = eventos.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF)
                    .findFirst().orElseThrow();

            assertThat(pcoinf.fhEvt()).as("fhEvt = instante canonico").isEqualTo(instante);
            assertThat(pcoinf.idUserEvt()).as("actor normalizado").isEqualTo("juez-confir");
            assertThat(pcoinf.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);
            assertThat(pcoinf.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
            assertThat(pcoinf.actaId()).isEqualTo(actaId);

            String desc = pcoinf.descripcionLegible();
            assertThat(desc).as("descripcion contiene referencia normalizada").contains("REF-HP-003");
            assertThat(desc).as("descripcion contiene monto").contains("3000.00");
            assertThat(desc).as("descripcion contiene obs exactamente una vez")
                    .satisfies(d -> assertThat(countOccurrences(d, "obs-hp3")).isEqualTo(1));
            assertThat(desc).as("descripcion no contiene 'null'").doesNotContain("null");

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("HP-4: exactamente un PCOINF agregado; cero CIERRA, PCOCNF, PCOOBS, CONFIR, PLAVNC nuevos")
        void hp4_exactamente_un_pcoinf() {
            Long actaId = crearActaConCondenaFirme("HP1-004");
            EstadoEventos antes = toEstadoEventos(eventoRepo.buscarPorActa(actaId));

            pagoCondenaService.informar(cmd(actaId));

            EstadoEventos despues = toEstadoEventos(eventoRepo.buscarPorActa(actaId));
            assertThat(despues.total()).as("solo PCOINF agregado").isEqualTo(antes.total() + 1);
            assertThat(despues.pcoinf()).as("exactamente un PCOINF nuevo").isEqualTo(antes.pcoinf() + 1);
            assertThat(despues.cierra()).as("cero CIERRA").isEqualTo(antes.cierra());
            assertThat(despues.pcocnf()).as("cero PCOCNF").isEqualTo(antes.pcocnf());
            assertThat(despues.pcoobs()).as("cero PCOOBS").isEqualTo(antes.pcoobs());
            assertThat(despues.confir()).as("cero CONFIR").isEqualTo(antes.confir());
            assertThat(despues.plavnc()).as("cero PLAVNC").isEqualTo(antes.plavnc());
        }

        @Test
        @DisplayName("HP-5: snapshot exacto: bandeja, accion, resultadoFinal=CONDENA_FIRME, ultimaActualizacion=ahora, bloqueActual=acta.bloqueActual")
        void hp5_snapshot_exacto() {
            Long actaId = crearActaConCondenaFirme("HP1-005");
            BloqueActual bloqueEsperado = actaRepo.buscarPorId(actaId).orElseThrow().getBloqueActual();
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

            pagoCondenaService.informar(cmd(actaId));

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
            assertThat(snap.getResultadoFinal()).as("resultadoFinal = CONDENA_FIRME").isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(snap.getUltimaActualizacion()).as("ultimaActualizacion = ahora").isEqualTo(ahora);
            assertThat(snap.getBloqueActual()).as("bloqueActual = acta.bloqueActual").isEqualTo(bloqueEsperado);
        }

        @Test
        @DisplayName("HP-6: observaciones null -> descripcion sin texto sobrante; observaciones presentes -> incluidas una vez")
        void hp6_observaciones_en_descripcion() {
            Long actaId1 = crearActaConCondenaFirme("HP1-006a");
            Long actaId2 = crearActaConCondenaFirme("HP1-006b");

            pagoCondenaService.informar(
                    cmdConObs(actaId1, new BigDecimal("3000.00"), "REF-006a", null, "juez-001"));
            FalActaEvento e1 = eventoRepo.buscarPorActa(actaId1).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).findFirst().orElseThrow();
            assertThat(e1.descripcionLegible()).doesNotEndWith(" ");

            pagoCondenaService.informar(
                    cmdConObs(actaId2, new BigDecimal("3000.00"), "REF-006b", "obs-exacta", "juez-001"));
            FalActaEvento e2 = eventoRepo.buscarPorActa(actaId2).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).findFirst().orElseThrow();
            long ocurrencias = countOccurrences(e2.descripcionLegible(), "obs-exacta");
            assertThat(ocurrencias).as("observacion aparece exactamente una vez").isEqualTo(1);
        }

        @Test
        @DisplayName("HP-7: pago sin observaciones previas; observaciones=null no sobreescribe nada")
        void hp7_observaciones_null_no_cambia_pago() {
            Long actaId = crearActaConCondenaFirme("HP1-007");

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-007", null, "juez-001"));

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getObservaciones()).as("observaciones null -> campo no modificado").isNull();
        }
    }

    // =========================================================================
    // V: Validacion estructural pre-reloj
    // =========================================================================

    @Nested
    @DisplayName("V: Validacion estructural pre-reloj")
    class V_Estructural {

        @Test
        @DisplayName("V-1: cmd null -> IllegalArgumentException; CountingClock = 0")
        void v1_cmd_null() {
            Long actaId = crearActaConCondenaFirme("V1-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-2: actaId null -> PrecondicionVioladaException; CountingClock = 0; acta sentinel intacta")
        void v2_actaId_null() {
            Long actaId = crearActaConCondenaFirme("V2-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(null, new BigDecimal("3000"), "REF", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actaId");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-3: actor null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v3_actor_null() {
            Long actaId = crearActaConCondenaFirme("V3-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("3000"), "REF", null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-4: actor blank -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v4_actor_blank() {
            Long actaId = crearActaConCondenaFirme("V4-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("3000"), "REF", null, "   ")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-5: actor.trim > 36 -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v5_actor_muy_largo() {
            Long actaId = crearActaConCondenaFirme("V5-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("3000"), "REF", null, "a".repeat(37))))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-6: monto null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v6_monto_null() {
            Long actaId = crearActaConCondenaFirme("V6-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, null, "REF", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-7: monto cero -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v7_monto_cero() {
            Long actaId = crearActaConCondenaFirme("V7-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, BigDecimal.ZERO, "REF", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-8: monto negativo -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v8_monto_negativo() {
            Long actaId = crearActaConCondenaFirme("V8-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("-1"), "REF", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-9: referenciaPago null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v9_referencia_null() {
            Long actaId = crearActaConCondenaFirme("V9-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("100"), null, null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("referencia");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("V-10: referenciaPago blank -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void v10_referencia_blank() {
            Long actaId = crearActaConCondenaFirme("V10-001");
            EstadoObservable antes = capturarEstado(actaId);

            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(actaId, new BigDecimal("100"), "   ", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("referencia");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }
    }

    // =========================================================================
    // P: Precondiciones de acta y pago
    // =========================================================================

    @Nested
    @DisplayName("P: Precondiciones de acta y pago")
    class P_Precondiciones {

        @Test
        @DisplayName("P-1: acta inexistente -> ActaNoEncontradaException; CountingClock = 0; sentinel intacto")
        void p1_acta_inexistente() {
            Long sentinelId = crearActaConCondenaFirme("P1-sentinel");
            EstadoObservable sentinelAntes = capturarEstado(sentinelId);
            final Long idInexistente = 999999L;

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(
                    new InformarPagoCondenaCommand(idInexistente, new BigDecimal("1000"), "REF", null, "actor")))
                    .isInstanceOf(ActaNoEncontradaException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(sentinelAntes, sentinelId);
            assertThat(eventoRepo.buscarPorActa(idInexistente)).isEmpty();
            assertThat(pagoCondRepo.buscarPorActa(idInexistente)).isEmpty();
            assertThat(falloRepo.buscarActivo(idInexistente)).isEmpty();
            assertThat(snapshotRepo.buscarPorActa(idInexistente)).isEmpty();
        }

        @Test
        @DisplayName("P-2: acta CERRADA -> PrecondicionVioladaException (cerrada); CountingClock = 0; cero efectos")
        void p2_acta_cerrada() {
            Long actaId = crearActaConCondenaFirme("P2-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-NO-1: acta ANULADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void pno1_acta_anulada() {
            Long actaId = crearActaConCondenaFirme("PNO1-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-NO-2: acta ARCHIVADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void pno2_acta_archivada() {
            Long actaId = crearActaConCondenaFirme("PNO2-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("archivada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-NO-3: acta PARALIZADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void pno3_acta_paralizada() {
            Long actaId = crearActaConCondenaFirme("PNO3-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-3: resultadoFinal != CONDENA_FIRME -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
        void p3_resultado_no_condena_firme() {
            Long actaId = crearActaConCondenaFirme("P3-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            actaRepo.guardar(acta);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA_FIRME");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("P-4: pago ya CONFIRMADO -> PrecondicionVioladaException; CountingClock = 0 extra; estado completo identico")
        void p4_pago_ya_confirmado() {
            Long actaId = crearActaConCondenaFirme("P4-001");
            pagoCondenaService.informar(cmd(actaId));
            pagoCondenaService.confirmar(
                    new ConfirmarPagoCondenaCommand(actaId, null));

            FalActa actaDespues = actaRepo.buscarPorId(actaId).orElseThrow();
            actaDespues.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaDespues.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            actaRepo.guardar(actaDespues);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000"), "REF-OTRO", null, "actor")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("confirmado");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }
    }

    // =========================================================================
    // FI: Fallo invalido
    // =========================================================================

    @Nested
    @DisplayName("FI: Fallo invalido")
    class FI_FalloInvalido {

        @Test
        @DisplayName("FI-1: sin fallo activo -> PrecondicionVioladaException; CountingClock = 0; estado sin cambios")
        void fi1_sin_fallo_activo() {
            Long actaId = crearActaConCondenaFirme("FI1-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setSiVigente(false);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fallo activo");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-2: tipoFallo ABSOLUTORIO (fallo activo reemplazado) -> PrecondicionVioladaException; CountingClock = 0")
        void fi2_tipo_absolutorio() {
            Long actaId = crearActaConCondenaFirme("FI2-001");
            FalActaFallo existente = falloRepo.buscarActivo(actaId).orElseThrow();
            existente.setSiVigente(false);
            falloRepo.guardar(existente);
            LocalDateTime now = FaltasClockTestSupport.FIXED.now();
            FalActaFallo falloAbs = new FalActaFallo(existente.getId() + 9001L, actaId,
                    TipoFalloActa.ABSOLUTORIO, now, now, "test");
            falloRepo.guardar(falloAbs);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("condenatorio");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-3: resultadoFallo distinto de CONDENA -> PrecondicionVioladaException; CountingClock = 0")
        void fi3_resultado_fallo_no_condena() {
            Long actaId = crearActaConCondenaFirme("FI3-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setResultadoFallo(ResultadoFalloActa.ABSUELVE);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-4: estadoFallo distinto de FIRME (NOTIFICADO) -> PrecondicionVioladaException; CountingClock = 0")
        void fi4_estado_fallo_no_firme() {
            Long actaId = crearActaConCondenaFirme("FI4-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRME");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-5: estado FIRME persistido con marcadores de firmeza inconsistentes (siFirme=false) -> PrecondicionVioladaException; mensaje 'firmeza inconsistente'; CountingClock = 0")
        void fi5_si_firme_false_inconsistente() {
            Long actaId = crearActaConCondenaFirme("FI5-001");
            FalActaFallo falloInconsistente = reemplazarPorFalloFirmeSinMarcadores(actaId, 9005L);

            assertThat(falloInconsistente.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(falloInconsistente.isSiFirme()).isFalse();

            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firmeza inconsistente");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-6: fhFirma null (FIRME inconsistente) -> PrecondicionVioladaException; CountingClock = 0")
        void fi6_fh_firma_null() {
            Long actaId = crearActaConCondenaFirme("FI6-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setFhFirma(null);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-7: fhNotificacion null (FIRME inconsistente) -> PrecondicionVioladaException; CountingClock = 0")
        void fi7_fh_notificacion_null() {
            Long actaId = crearActaConCondenaFirme("FI7-001");
            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            fallo.setFhNotificacion(null);
            falloRepo.guardar(fallo);
            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-8: estado FIRME persistido con marcadores de firmeza inconsistentes (fhFirmeza=null) -> PrecondicionVioladaException; mensaje 'firmeza inconsistente'; CountingClock = 0")
        void fi8_fh_firmeza_null_inconsistente() {
            Long actaId = crearActaConCondenaFirme("FI8-001");
            FalActaFallo falloInconsistente = reemplazarPorFalloFirmeSinMarcadores(actaId, 9008L);

            assertThat(falloInconsistente.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(falloInconsistente.getFhFirmeza()).isNull();

            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firmeza inconsistente");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }

        @Test
        @DisplayName("FI-9: estado FIRME persistido con marcadores de firmeza inconsistentes (origenFirmeza=null) -> PrecondicionVioladaException; mensaje 'firmeza inconsistente'; CountingClock = 0")
        void fi9_origen_firmeza_null_inconsistente() {
            Long actaId = crearActaConCondenaFirme("FI9-001");
            FalActaFallo falloInconsistente = reemplazarPorFalloFirmeSinMarcadores(actaId, 9009L);

            assertThat(falloInconsistente.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(falloInconsistente.getOrigenFirmeza()).isNull();

            EstadoObservable antes = capturarEstado(actaId);

            testClock.invocaciones.set(0);
            assertThatThrownBy(() -> pagoCondenaService.informar(cmd(actaId)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firmeza inconsistente");
            assertThat(testClock.invocaciones()).isZero();
            assertEstadoSinCambios(antes, actaId);
        }
    }

    // =========================================================================
    // PEN: Pago en estado PENDIENTE
    // =========================================================================

    @Nested
    @DisplayName("PEN: Pago previo en PENDIENTE")
    class PEN_Pendiente {

        @Test
        @DisplayName("PEN-1: pago PENDIENTE -> mismo ID, PENDIENTE->INFORMADO, campos actualizados, un PCOINF, acta/fallo intactos, snapshot exacto")
        void pen1_pago_pendiente_transicion() {
            Long actaId = crearActaConCondenaFirme("PEN1-001");
            EstadoActa actaAntes = toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow());
            EstadoFallo falloAntes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());
            EstadoEventos eventosAntes = toEstadoEventos(eventoRepo.buscarPorActa(actaId));

            FalPagoCondena pagoPendiente = new FalPagoCondena(UUID.randomUUID().toString(), actaId);
            pagoCondRepo.guardar(pagoPendiente);
            String pagoId = pagoPendiente.getId();
            assertThat(pagoCondRepo.buscarPorActa(actaId).orElseThrow().getEstadoPagoCondena())
                    .isEqualTo(EstadoPagoCondena.PENDIENTE);

            testClock.invocaciones.set(0);
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("2500.00"), "REF-PEN1", null, "operador-1"));

            assertThat(testClock.invocaciones()).isEqualTo(1);

            FalPagoCondena pagoFinal = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pagoFinal.getId()).as("mismo pago ID").isEqualTo(pagoId);
            assertThat(pagoFinal.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);
            assertThat(pagoFinal.getMonto()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(pagoFinal.getReferenciaPago()).isEqualTo("REF-PEN1");
            assertThat(pagoFinal.getFechaInforme()).isEqualTo(ahora);

            EstadoEventos eventosDespues = toEstadoEventos(eventoRepo.buscarPorActa(actaId));
            assertThat(eventosDespues.pcoinf()).isEqualTo(eventosAntes.pcoinf() + 1);
            assertThat(eventosDespues.total()).isEqualTo(eventosAntes.total() + 1);

            assertThat(toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow())).isEqualTo(actaAntes);
            assertThat(toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow())).isEqualTo(falloAntes);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
            assertThat(snap.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(snap.getUltimaActualizacion()).isEqualTo(ahora);
        }
    }

    // =========================================================================
    // DS: Re-informe secuencial
    // =========================================================================

    @Nested
    @DisplayName("DS: Re-informe secuencial")
    class DS_ReInformar {

        @Test
        @DisplayName("DS-1: re-informe sobre INFORMADO con observaciones nuevas; mismo ID, datos actualizados, CountingClock = 2, acta/fallo intactos")
        void ds1_reinforme_sobre_informado_con_obs() {
            Long actaId = crearActaConCondenaFirme("DS1-001");
            EstadoActa actaAntes = toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow());
            EstadoFallo falloAntes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());

            testClock.invocaciones.set(0);
            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-DS1-ORIG", null, "actor-1"));
            assertThat(testClock.invocaciones()).isEqualTo(1);

            String pagoIdOriginal = pagoCondRepo.buscarPorActa(actaId).orElseThrow().getId();

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("4000.00"), "REF-DS1-NUEVO", "Corregido", "actor-2"));
            assertThat(testClock.invocaciones()).isEqualTo(2);

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getId()).as("mismo pago reutilizado").isEqualTo(pagoIdOriginal);
            assertThat(pago.getMonto()).isEqualByComparingTo(new BigDecimal("4000.00"));
            assertThat(pago.getReferenciaPago()).isEqualTo("REF-DS1-NUEVO");
            assertThat(pago.getObservaciones()).isEqualTo("Corregido");
            assertThat(pago.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);

            long pcoinfs = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).count();
            assertThat(pcoinfs).as("dos PCOINF en total").isEqualTo(2);

            assertThat(toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow())).isEqualTo(actaAntes);
            assertThat(toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow())).isEqualTo(falloAntes);
        }

        @Test
        @DisplayName("DS-2: re-informe sobre INFORMADO con observaciones null preserva las anteriores; mismo ID, monto/ref actualizados")
        void ds2_reinforme_informado_obs_null_preserva_anterior() {
            Long actaId = crearActaConCondenaFirme("DS2-001");

            testClock.invocaciones.set(0);
            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-DS2-ORIG", "obs-primera", "actor-1"));
            String pagoIdOriginal = pagoCondRepo.buscarPorActa(actaId).orElseThrow().getId();
            assertThat(pagoCondRepo.buscarPorActa(actaId).orElseThrow().getObservaciones())
                    .isEqualTo("obs-primera");

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("5000.00"), "REF-DS2-NUEVO", null, "actor-2"));

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getId()).as("mismo ID").isEqualTo(pagoIdOriginal);
            assertThat(pago.getMonto()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(pago.getReferenciaPago()).isEqualTo("REF-DS2-NUEVO");
            assertThat(pago.getObservaciones()).as("obs anteriores preservadas cuando reinformar con obs=null")
                    .isEqualTo("obs-primera");
        }

        @Test
        @DisplayName("DS-3: re-informe sobre OBSERVADO; estado->INFORMADO, motivoObservacion/fechaObservacion preservados, monto/ref/fechaInforme actualizados, un PCOINF nuevo")
        void ds3_reinforme_sobre_observado() {
            Long actaId = crearActaConCondenaFirme("DS3-001");
            testClock.invocaciones.set(0);
            pagoCondenaService.informar(cmd(actaId));
            pagoCondenaService.observar(new ObservarPagoCondenaCommand(actaId, "Comprobante invalido", null));

            FalPagoCondena pagoObservado = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pagoObservado.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.OBSERVADO);
            String pagoId = pagoObservado.getId();
            String motivoPreservado = pagoObservado.getMotivoObservacion();
            LocalDateTime fechaObsPreservada = pagoObservado.getFechaObservacion();
            assertThat(motivoPreservado).isEqualTo("Comprobante invalido");
            assertThat(fechaObsPreservada).isNotNull();

            long pcoinfsAntes = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).count();
            EstadoActa actaAntes = toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow());
            EstadoFallo falloAntes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

            pagoCondenaService.informar(
                    cmdConObs(actaId, new BigDecimal("3000.00"), "REF-DS3-CORREGIDO", "Obs-nueva", "actor"));

            FalPagoCondena pago = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getId()).as("mismo ID").isEqualTo(pagoId);
            assertThat(pago.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);
            assertThat(pago.getMotivoObservacion()).as("motivoObservacion preservado exactamente").isEqualTo(motivoPreservado);
            assertThat(pago.getFechaObservacion()).as("fechaObservacion preservada exactamente").isEqualTo(fechaObsPreservada);
            assertThat(pago.getMonto()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(pago.getReferenciaPago()).isEqualTo("REF-DS3-CORREGIDO");
            assertThat(pago.getFechaInforme()).isEqualTo(ahora);
            assertThat(pago.getFechaConfirmacion()).as("fechaConfirmacion preservada (null)").isNull();

            long pcoinfsNuevos = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF).count();
            assertThat(pcoinfsNuevos).isEqualTo(pcoinfsAntes + 1);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
            assertThat(snap.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(snap.getUltimaActualizacion()).isEqualTo(ahora);

            assertThat(toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow())).isEqualTo(actaAntes);
            assertThat(toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow())).isEqualTo(falloAntes);
        }
    }

    // =========================================================================
    // CC: Concurrencia InMemory
    // =========================================================================

    @Nested
    @DisplayName("CC: Concurrencia InMemory: dos ejecuciones serializadas")
    class CC_Concurrencia {

        @Test
        @DisplayName("CC-1: dos informar concurrentes con datos distintos; ambos exitosos; CountingClock = 2; dos PCOINF; pago final coherente")
        void cc1_dos_informar_concurrentes() throws Exception {
            Long actaId = crearActaConCondenaFirme("CC1-001");
            EstadoActa actaAntes = toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow());
            EstadoFallo falloAntes = toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow());
            EstadoEventos eventosAntes = toEstadoEventos(eventoRepo.buscarPorActa(actaId));

            testClock.invocaciones.set(0);
            CyclicBarrier barrera = new CyclicBarrier(2);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            Future<ComandoResultado> futA;
            Future<ComandoResultado> futB;
            try {
                futA = executor.submit(() -> {
                    barrera.await();
                    return pagoCondenaService.informar(cmdConObs(actaId,
                            new BigDecimal("3001.00"), "REF-CC-A", "OBS-A", "actor-A"));
                });
                futB = executor.submit(() -> {
                    barrera.await();
                    return pagoCondenaService.informar(cmdConObs(actaId,
                            new BigDecimal("4002.00"), "REF-CC-B", "OBS-B", "actor-B"));
                });
            } finally {
                executor.shutdown();
            }

            ComandoResultado resA = futA.get();
            ComandoResultado resB = futB.get();

            assertThat(testClock.invocaciones()).as("CountingClock = 2").isEqualTo(2);
            assertThat(resA.tipoEvento()).isEqualTo(TipoEventoActa.PCOINF.codigo());
            assertThat(resB.tipoEvento()).isEqualTo(TipoEventoActa.PCOINF.codigo());
            assertThat(resA.idActa()).as("resA.idActa = actaId").isEqualTo(actaId);
            assertThat(resB.idActa()).as("resB.idActa = actaId").isEqualTo(actaId);
            assertThat(resA.descripcion()).as("resA descripcion exacta")
                    .isEqualTo("Pago de condena informado. Pendiente de confirmacion.");
            assertThat(resB.descripcion()).as("resB descripcion exacta")
                    .isEqualTo("Pago de condena informado. Pendiente de confirmacion.");
            assertThat(resA.idEntidadAfectada()).as("misma entidad de pago")
                    .isEqualTo(resB.idEntidadAfectada());

            FalPagoCondena pagoFinal = pagoCondRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pagoFinal.getId()).isEqualTo(resA.idEntidadAfectada());
            assertThat(pagoFinal.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);

            boolean pagoEsA = pagoFinal.getReferenciaPago().equals("REF-CC-A");
            boolean pagoEsB = pagoFinal.getReferenciaPago().equals("REF-CC-B");
            assertThat(pagoEsA || pagoEsB).as("pago final es exactamente A o exactamente B").isTrue();
            if (pagoEsA) {
                assertThat(pagoFinal.getMonto()).isEqualByComparingTo("3001.00");
                assertThat(pagoFinal.getObservaciones()).isEqualTo("OBS-A");
            } else {
                assertThat(pagoFinal.getMonto()).isEqualByComparingTo("4002.00");
                assertThat(pagoFinal.getObservaciones()).isEqualTo("OBS-B");
            }

            List<FalActaEvento> eventosFinales = eventoRepo.buscarPorActa(actaId);
            EstadoEventos eventosDespues = toEstadoEventos(eventosFinales);
            assertThat(eventosDespues.total()).as("solo se agregaron los dos PCOINF")
                    .isEqualTo(eventosAntes.total() + 2);
            assertThat(eventosDespues.pcoinf()).as("dos PCOINF nuevos").isEqualTo(eventosAntes.pcoinf() + 2);
            assertThat(eventosDespues.pcocnf()).isEqualTo(eventosAntes.pcocnf());
            assertThat(eventosDespues.pcoobs()).isEqualTo(eventosAntes.pcoobs());
            assertThat(eventosDespues.cierra()).isEqualTo(eventosAntes.cierra());
            assertThat(eventosDespues.confir()).isEqualTo(eventosAntes.confir());
            assertThat(eventosDespues.plavnc()).isEqualTo(eventosAntes.plavnc());

            List<FalActaEvento> nuevoPcoinfs = eventosFinales.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PCOINF)
                    .toList();
            boolean hayEventoA = nuevoPcoinfs.stream()
                    .anyMatch(e -> e.descripcionLegible() != null
                            && e.descripcionLegible().contains("REF-CC-A")
                            && "actor-A".equals(e.idUserEvt()));
            boolean hayEventoB = nuevoPcoinfs.stream()
                    .anyMatch(e -> e.descripcionLegible() != null
                            && e.descripcionLegible().contains("REF-CC-B")
                            && "actor-B".equals(e.idUserEvt()));
            assertThat(hayEventoA).as("existe evento con REF-CC-A y actor-A").isTrue();
            assertThat(hayEventoB).as("existe evento con REF-CC-B y actor-B").isTrue();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
            assertThat(snap.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(snap.getBloqueActual()).as("bloqueActual = acta.bloqueActual preservado")
                    .isEqualTo(actaAntes.bloqueActual());
            assertThat(snap.getUltimaActualizacion()).as("ultimaActualizacion = instante fijo del clock")
                    .isEqualTo(FaltasClockTestSupport.FIXED.now());

            assertThat(toEstadoActa(actaRepo.buscarPorId(actaId).orElseThrow()))
                    .as("acta completa intacta").isEqualTo(actaAntes);
            assertThat(toEstadoFallo(falloRepo.buscarActivo(actaId).orElseThrow()))
                    .as("fallo completo intacto").isEqualTo(falloAntes);
        }
    }

    // =========================================================================
    // SN: SnapshotRecalculador null guard
    // =========================================================================

    @Nested
    @DisplayName("SN: SnapshotRecalculador null guard")
    class SN_NullGuard {

        @Test
        @DisplayName("SN-1: recalcular(acta, null) -> NullPointerException con mensaje 'ahora'; no se persiste snapshot")
        void sn1_recalcular_ahora_null() {
            Long actaId = crearActaConCondenaFirme("SN1-001");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            boolean snapPresenteAntes = snapshotRepo.buscarPorActa(actaId).isPresent();

            assertThatThrownBy(() -> snapshotRecalc.recalcular(acta, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ahora");

            assertThat(snapshotRepo.buscarPorActa(actaId).isPresent())
                    .as("presencia de snapshot no cambia por invocacion que lanza").isEqualTo(snapPresenteAntes);
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
