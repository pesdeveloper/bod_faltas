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
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests canonicos de CMD-FALLO-005: Declarar firmeza por vencimiento del plazo de apelacion.
 *
 * Sin Mockito. Repositorios InMemory reales. CountingClock fijo para firmezaService.
 *
 * Slice GAP-CONFORMIDAD-FIRMEZA-VENCIMIENTO-001 R1/R2.
 */
@DisplayName("GAP-CMD-FALLO-005: Firmeza por vencimiento del plazo de apelacion (canonical)")
class FirmezaVencimientoCanonicaTest {

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
        LabrarActaCommand cmd = new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 100", "San Martin 200",
                null, null, null, "Infractor Test", docNum,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
        Long actaId = actaService.labrar(cmd).idActa();
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
        return actaId;
    }

    private Long crearActaConFalloCondenatorioNotificado(String docNum) {
        Long actaId = crearActaPreFallo(docNum);
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("2500.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, CanalNotificacion.PRESENCIAL,
                        null, null, null, "test-user")).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(
                Long.parseLong(idNotifFallo),
                IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotifFallo)),
                null, "test-actor"));
        // Retrotraer fhVtoApelacion: fechaActual (2026-07-09) > fhVtoApelacion (2026-07-08)
        FalActaFallo falloVto = falloRepo.buscarActivo(actaId).orElseThrow();
        falloVto.setFhVtoApelacion(LocalDate.of(2026, 7, 8));
        falloRepo.guardar(falloVto);
        return actaId;
    }

    private VencerPlazoApelacionCommand cmd(Long actaId, String actor) {
        return new VencerPlazoApelacionCommand(actaId, null, actor);
    }

    private VencerPlazoApelacionCommand cmdConObs(Long actaId, String observaciones, String actor) {
        return new VencerPlazoApelacionCommand(actaId, observaciones, actor);
    }

    // -------------------------------------------------------------------------
    // Estado observable para cero-efectos
    // -------------------------------------------------------------------------

    /**
     * Captura escalar completa del estado observable del acta, fallo activo,
     * contadores de eventos y snapshot. No almacena referencias a objetos mutables.
     */
    private record EstadoActaFallo(
            // Acta
            ResultadoFinalActa actaResultadoFinal,
            SituacionAdministrativaActa actaSituacion,
            BloqueActual actaBloque,
            boolean actaCerrada,
            int actaVersionRow,
            // Fallo activo (null cuando no existe fallo activo)
            Long falloId,
            int falloVersionRow,
            EstadoFalloActa falloEstado,
            TipoFalloActa falloTipo,
            ResultadoFalloActa falloResultado,
            boolean falloSiFirme,
            LocalDateTime falloFhFirmeza,
            OrigenFirmezaCondena falloOrigenFirmeza,
            LocalDateTime falloFhFirma,
            LocalDateTime falloFhNotificacion,
            LocalDate falloFhVtoApelacion,
            BigDecimal falloMonto,
            boolean falloSiVigente,
            // Contadores de eventos
            long eventosTotales,
            long eventosPlavnc,
            long eventosConfir,
            long eventosCierra,
            long eventosPcoinf,
            long eventosPcocnf,
            long eventosPcoobs,
            // Snapshot
            boolean snapshotPresente,
            CodigoBandeja snapshotBandeja,
            AccionPendiente snapshotAccion
    ) {}

    private EstadoActaFallo capturarEstado(Long actaId) {
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElse(null);
        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElse(null);
        return new EstadoActaFallo(
                acta.getResultadoFinal(),
                acta.getSituacionAdministrativa(),
                acta.getBloqueActual(),
                acta.estaCerrada(),
                acta.getVersionRow(),
                fallo != null ? fallo.getId() : null,
                fallo != null ? fallo.getVersionRow() : -1,
                fallo != null ? fallo.getEstadoFallo() : null,
                fallo != null ? fallo.getTipoFallo() : null,
                fallo != null ? fallo.getResultadoFallo() : null,
                fallo != null && fallo.isSiFirme(),
                fallo != null ? fallo.getFhFirmeza() : null,
                fallo != null ? fallo.getOrigenFirmeza() : null,
                fallo != null ? fallo.getFhFirma() : null,
                fallo != null ? fallo.getFhNotificacion() : null,
                fallo != null ? fallo.getFhVtoApelacion() : null,
                fallo != null ? fallo.getMontoCondena() : null,
                fallo != null && fallo.isSiVigente(),
                eventoRepo.buscarPorActa(actaId).size(),
                contarEventos(actaId, TipoEventoActa.PLAVNC),
                contarEventos(actaId, TipoEventoActa.CONFIR),
                contarEventos(actaId, TipoEventoActa.CIERRA),
                contarEventos(actaId, TipoEventoActa.PCOINF),
                contarEventos(actaId, TipoEventoActa.PCOCNF),
                contarEventos(actaId, TipoEventoActa.PCOOBS),
                snap != null,
                snap != null ? snap.getCodBandeja() : null,
                snap != null ? snap.getAccionPendiente() : null
        );
    }

    private void assertEstadoSinCambios(EstadoActaFallo antes, Long actaId) {
        EstadoActaFallo despues = capturarEstado(actaId);

        // Acta
        assertThat(despues.actaResultadoFinal()).isEqualTo(antes.actaResultadoFinal());
        assertThat(despues.actaSituacion()).isEqualTo(antes.actaSituacion());
        assertThat(despues.actaBloque()).isEqualTo(antes.actaBloque());
        assertThat(despues.actaCerrada()).isEqualTo(antes.actaCerrada());
        assertThat(despues.actaVersionRow()).isEqualTo(antes.actaVersionRow());

        // Fallo activo (solo si existia antes)
        if (antes.falloId() != null) {
            assertThat(despues.falloId()).isEqualTo(antes.falloId());
            assertThat(despues.falloVersionRow()).isEqualTo(antes.falloVersionRow());
            assertThat(despues.falloEstado()).isEqualTo(antes.falloEstado());
            assertThat(despues.falloTipo()).isEqualTo(antes.falloTipo());
            assertThat(despues.falloResultado()).isEqualTo(antes.falloResultado());
            assertThat(despues.falloSiFirme()).isEqualTo(antes.falloSiFirme());
            assertThat(despues.falloFhFirmeza()).isEqualTo(antes.falloFhFirmeza());
            assertThat(despues.falloOrigenFirmeza()).isEqualTo(antes.falloOrigenFirmeza());
            assertThat(despues.falloFhFirma()).isEqualTo(antes.falloFhFirma());
            assertThat(despues.falloFhNotificacion()).isEqualTo(antes.falloFhNotificacion());
            assertThat(despues.falloFhVtoApelacion()).isEqualTo(antes.falloFhVtoApelacion());
            if (antes.falloMonto() != null) {
                assertThat(despues.falloMonto()).isEqualByComparingTo(antes.falloMonto());
            }
            assertThat(despues.falloSiVigente()).isEqualTo(antes.falloSiVigente());
        }

        // Eventos
        assertThat(despues.eventosTotales()).isEqualTo(antes.eventosTotales());
        assertThat(despues.eventosPlavnc()).isEqualTo(antes.eventosPlavnc());
        assertThat(despues.eventosConfir()).isEqualTo(antes.eventosConfir());
        assertThat(despues.eventosCierra()).isEqualTo(antes.eventosCierra());
        assertThat(despues.eventosPcoinf()).isEqualTo(antes.eventosPcoinf());
        assertThat(despues.eventosPcocnf()).isEqualTo(antes.eventosPcocnf());
        assertThat(despues.eventosPcoobs()).isEqualTo(antes.eventosPcoobs());

        // Snapshot
        assertThat(despues.snapshotPresente()).isEqualTo(antes.snapshotPresente());
        assertThat(despues.snapshotBandeja()).isEqualTo(antes.snapshotBandeja());
        assertThat(despues.snapshotAccion()).isEqualTo(antes.snapshotAccion());
    }

    // -------------------------------------------------------------------------
    // 6.1 Camino feliz completo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("HP-1: actor con espacios se normaliza; ComandoResultado exacto; CountingClock = 1")
    void camino_feliz_actor_trim_resultado_comando() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H001");
        FalActaFallo falloAntes = falloRepo.buscarActivo(actaId).orElseThrow();
        Long falloId = falloAntes.getId();

        ComandoResultado res = firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(actaId, null, "  operador-trim  "));

        assertThat(testClock.invocaciones()).isEqualTo(1);
        assertThat(res.idActa()).isEqualTo(actaId);
        assertThat(res.idEntidadAfectada()).isEqualTo(String.valueOf(falloId));
        assertThat(res.tipoEvento()).isEqualTo(TipoEventoActa.CONFIR.codigo());
        assertThat(res.descripcion()).isNotBlank();

        List<FalActaEvento> nuevos = eventoRepo.buscarPorActa(actaId).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC || e.tipoEvt() == TipoEventoActa.CONFIR)
                .toList();
        assertThat(nuevos).allMatch(e -> "operador-trim".equals(e.idUserEvt()));
    }

    @Test
    @DisplayName("HP-2: campos del fallo preservados y actualizados tras firmeza")
    void camino_feliz_fallo_campos_preservados() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H002");
        FalActaFallo antes = falloRepo.buscarActivo(actaId).orElseThrow();
        Long idAntes = antes.getId();
        Long actaIdAntes = antes.getActaId();
        Long documentoIdAntes = antes.getDocumentoId();
        Long valorizacionIdAntes = antes.getValorizacionId();
        TipoFalloActa tipoFalloAntes = antes.getTipoFallo();
        ResultadoFalloActa resultadoAntes = antes.getResultadoFallo();
        BigDecimal montoAntes = antes.getMontoCondena();
        String fundamentosAntes = antes.getFundamentos();
        LocalDateTime fhDictadoAntes = antes.getFhDictado();
        String idUserDictadoAntes = antes.getIdUserDictado();
        boolean siApelableAntes = antes.isSiApelable();
        Long falloReemplazadoIdAntes = antes.getFalloReemplazadoId();
        LocalDateTime fhAltaAntes = antes.getFhAlta();
        String idUserAltaAntes = antes.getIdUserAlta();
        LocalDateTime fhFirmaAntes = antes.getFhFirma();
        LocalDateTime fhNotifAntes = antes.getFhNotificacion();
        LocalDate fhVtoAntes = antes.getFhVtoApelacion();
        boolean siVigenteAntes = antes.isSiVigente();

        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        FalActaFallo despues = falloRepo.buscarActivo(actaId).orElseThrow();

        // Campos que deben cambiar
        assertThat(despues.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
        assertThat(despues.isSiFirme()).isTrue();
        assertThat(despues.getFhFirmeza()).isEqualTo(FaltasClockTestSupport.FIXED.now());
        assertThat(despues.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);

        // Campos que deben preservarse sin cambio
        assertThat(despues.getId()).isEqualTo(idAntes);
        assertThat(despues.getActaId()).isEqualTo(actaIdAntes);
        assertThat(despues.getDocumentoId()).isEqualTo(documentoIdAntes);
        assertThat(despues.getValorizacionId()).isEqualTo(valorizacionIdAntes);
        assertThat(despues.getTipoFallo()).isEqualTo(tipoFalloAntes);
        assertThat(despues.getTipoFallo()).isEqualTo(TipoFalloActa.CONDENATORIO);
        assertThat(despues.getResultadoFallo()).isEqualTo(resultadoAntes);
        assertThat(despues.getResultadoFallo()).isEqualTo(ResultadoFalloActa.CONDENA);
        assertThat(despues.getMontoCondena()).isEqualByComparingTo(montoAntes);
        assertThat(despues.getFundamentos()).isEqualTo(fundamentosAntes);
        assertThat(despues.getFhDictado()).isEqualTo(fhDictadoAntes);
        assertThat(despues.getIdUserDictado()).isEqualTo(idUserDictadoAntes);
        assertThat(despues.isSiApelable()).isEqualTo(siApelableAntes);
        assertThat(despues.getFalloReemplazadoId()).isEqualTo(falloReemplazadoIdAntes);
        assertThat(despues.getFhAlta()).isEqualTo(fhAltaAntes);
        assertThat(despues.getIdUserAlta()).isEqualTo(idUserAltaAntes);
        assertThat(despues.getFhFirma()).isEqualTo(fhFirmaAntes);
        assertThat(despues.getFhNotificacion()).isEqualTo(fhNotifAntes);
        assertThat(despues.getFhVtoApelacion()).isEqualTo(fhVtoAntes);
        assertThat(despues.isSiVigente()).isEqualTo(siVigenteAntes);
    }

    @Test
    @DisplayName("HP-3: acta.resultadoFinal = CONDENA_FIRME; situacionAdministrativa = ACTIVA; no cerrada")
    void camino_feliz_acta_condena_firme() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H003");

        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        assertThat(acta.estaCerrada()).isFalse();
    }

    @Test
    @DisplayName("HP-4: eventos PLAVNC+CONFIR: count=1 c/u, orden, fhEvt, actor, origen, actorTipo, observaciones exactamente una vez")
    void camino_feliz_eventos_detalle() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H004");
        int eventosAntes = eventoRepo.buscarPorActa(actaId).size();
        String observaciones = "obs-canonica-test";

        firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(actaId, observaciones, "actor-det"));

        List<FalActaEvento> todos = eventoRepo.buscarPorActa(actaId);
        List<FalActaEvento> nuevos = todos.subList(eventosAntes, todos.size());

        long cntPlavnc = nuevos.stream().filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).count();
        long cntConfir = nuevos.stream().filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).count();
        assertThat(cntPlavnc).isEqualTo(1);
        assertThat(cntConfir).isEqualTo(1);

        List<TipoEventoActa> tiposNuevos = nuevos.stream().map(FalActaEvento::tipoEvt).toList();
        int idxPlavnc = tiposNuevos.indexOf(TipoEventoActa.PLAVNC);
        int idxConfir = tiposNuevos.indexOf(TipoEventoActa.CONFIR);
        assertThat(idxPlavnc).isGreaterThanOrEqualTo(0);
        assertThat(idxConfir).isGreaterThan(idxPlavnc);

        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalActaEvento plavnc = nuevos.stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).findFirst().orElseThrow();
        FalActaEvento confir = nuevos.stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).findFirst().orElseThrow();

        assertThat(plavnc.fhEvt()).isEqualTo(ahora);
        assertThat(confir.fhEvt()).isEqualTo(ahora);
        assertThat(plavnc.idUserEvt()).isEqualTo("actor-det");
        assertThat(confir.idUserEvt()).isEqualTo("actor-det");
        assertThat(plavnc.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
        assertThat(confir.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
        assertThat(plavnc.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);
        assertThat(confir.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);

        // observaciones en PLAVNC exactamente una vez; CONFIR no las contiene
        assertThat(plavnc.descripcionLegible()).containsOnlyOnce(observaciones);
        assertThat(confir.descripcionLegible()).doesNotContain(observaciones);
    }

    @Test
    @DisplayName("HP-5: cero eventos PCOINF, PCOCNF, PCOOBS y CIERRA tras firmeza")
    void camino_feliz_eventos_pago_cero() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H005");

        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                .stream().map(FalActaEvento::tipoEvt).toList();
        assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        assertThat(tipos).doesNotContain(TipoEventoActa.PCOINF);
        assertThat(tipos).doesNotContain(TipoEventoActa.PCOCNF);
        assertThat(tipos).doesNotContain(TipoEventoActa.PCOOBS);
    }

    @Test
    @DisplayName("HP-6: snapshot codBandeja = PENDIENTE_PAGO_CONDENA; accionPendiente = GESTIONAR_PAGO_CONDENA")
    void camino_feliz_snapshot() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H006");

        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
    }

    // -------------------------------------------------------------------------
    // 6.2 Frontera temporal exacta
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FT-1: fechaActual < fhVtoApelacion -> rechazo; CountingClock = 1; cero efectos")
    void frontera_futuro_rechaza() {
        Long actaId = crearActaConFalloCondenatorioNotificado("F001");
        // Fijar vencimiento en el futuro: 2026-07-10 > 2026-07-09
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setFhVtoApelacion(LocalDate.of(2026, 7, 10));
        falloRepo.guardar(fallo);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("no ha vencido");

        assertThat(testClock.invocaciones()).isEqualTo(1);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("FT-2: fechaActual = fhVtoApelacion -> rechazo explicito (frontera exacta); CountingClock = 1; cero efectos")
    void frontera_igual_rechaza() {
        Long actaId = crearActaConFalloCondenatorioNotificado("F002");
        // Fijar vencimiento igual al dia actual del reloj: 2026-07-09
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setFhVtoApelacion(LocalDate.of(2026, 7, 9));
        falloRepo.guardar(fallo);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("no ha vencido");

        assertThat(testClock.invocaciones()).isEqualTo(1);
        assertEstadoSinCambios(antes, actaId);
        // snapshot no fue cambiado a PENDIENTE_PAGO_CONDENA por este comando rechazado
        assertThat(snapshotRepo.buscarPorActa(actaId).map(FalActaSnapshot::getCodBandeja).orElse(null))
                .isNotEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
    }

    @Test
    @DisplayName("FT-3: fechaActual > fhVtoApelacion -> exito (plazo efectivamente vencido)")
    void frontera_pasado_exitoso() {
        Long actaId = crearActaConFalloCondenatorioNotificado("F003");
        // fhVtoApelacion = 2026-07-08, fechaActual = 2026-07-09 -> despues -> exito

        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getResultadoFinal())
                .isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        assertThat(testClock.invocaciones()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 6.3 Validacion estructural antes del reloj (CountingClock = 0)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("V1: cmd null -> IllegalArgumentException; CountingClock = 0")
    void validacion_cmd_null() {
        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cmd");
        assertThat(testClock.invocaciones()).isEqualTo(0);
    }

    @Test
    @DisplayName("V2: actaId null -> PrecondicionVioladaException; CountingClock = 0")
    void validacion_actaId_null() {
        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(null, null, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("actaId");
        assertThat(testClock.invocaciones()).isEqualTo(0);
    }

    @Test
    @DisplayName("V3: actor null -> rechazo estructural antes de tocar el acta; CountingClock = 0")
    void validacion_actor_null() {
        // Sembrar acta valida; el rechazo por actor ocurre antes de cualquier acceso al repo
        Long actaId = crearActaConFalloCondenatorioNotificado("V003");
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(actaId, null, null)))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("actor");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("V4: actor blank -> rechazo estructural antes de tocar el acta; CountingClock = 0")
    void validacion_actor_blank() {
        Long actaId = crearActaConFalloCondenatorioNotificado("V004");
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(actaId, null, "   ")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("actor");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("V5: actor.trim().length() = 37 -> rechazo estructural antes de tocar el acta; CountingClock = 0")
    void validacion_actor_largo_37() {
        Long actaId = crearActaConFalloCondenatorioNotificado("V005");
        EstadoActaFallo antes = capturarEstado(actaId);
        String actorLargo = "a".repeat(37);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                new VencerPlazoApelacionCommand(actaId, null, actorLargo)))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("36");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    // -------------------------------------------------------------------------
    // 6.4 Precondiciones de acta y fallo (CountingClock = 0 en todos)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("P1: acta inexistente -> ActaNoEncontradaException; CountingClock = 0; ningun evento/snapshot nuevo")
    void precond_acta_inexistente() {
        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(9999L, "actor")))
                .isInstanceOf(ActaNoEncontradaException.class);
        assertThat(testClock.invocaciones()).isEqualTo(0);
        // No hay acta 9999L; ningun evento ni snapshot fue creado para ese ID
        assertThat(eventoRepo.buscarPorActa(9999L)).isEmpty();
        assertThat(snapshotRepo.buscarPorActa(9999L)).isEmpty();
        assertThat(falloRepo.buscarActivo(9999L)).isEmpty();
    }

    @Test
    @DisplayName("P2: acta CERRADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_acta_cerrada() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P002");
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
        actaRepo.guardar(acta);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("cerrada");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P3: acta ANULADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_acta_anulada() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P003");
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
        actaRepo.guardar(acta);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("anulada");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P4: acta ARCHIVADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_acta_archivada() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P004");
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
        actaRepo.guardar(acta);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("archivada");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P5: acta PARALIZADA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_acta_paralizada() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P005");
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
        actaRepo.guardar(acta);

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("paralizada");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P6: sin fallo activo -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_sin_fallo_activo() {
        Long actaId = crearActaPreFallo("P006");
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("fallo activo");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P7: fallo ABSOLUTORIO -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_fallo_absolutorio() {
        Long actaId = crearActaPreFallo("P007");
        falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(actaId, "Absolutorias", null));
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("condenatorio");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P8: resultadoFallo != CONDENA -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_resultadoFallo_no_condena() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P008");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setResultadoFallo(ResultadoFalloActa.ABSUELVE);
        falloRepo.guardar(fallo);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("CONDENA");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P9: estadoFallo != NOTIFICADO -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_estadoFallo_no_notificado() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P009");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setEstadoFallo(EstadoFalloActa.PENDIENTE_FIRMA);
        falloRepo.guardar(fallo);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("NOTIFICADO");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P10: fhFirma null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_fhFirma_null() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P010");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setFhFirma(null);
        falloRepo.guardar(fallo);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("fhFirma");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P11: fhNotificacion null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_fhNotificacion_null() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P011");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setFhNotificacion(null);
        falloRepo.guardar(fallo);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("fhNotificacion");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P12: siFirme=true / fhFirmeza not null / origenFirmeza not null -> rechazo; tres marcadores intactos; CountingClock = 0")
    void precond_siFirme_ya_declarado() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P012");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        // Los tres marcadores nacen juntos por la operacion de dominio;
        // las ramas individuales son defensivas frente a persistencia futura inconsistente.
        fallo.declararFirmeza(FaltasClockTestSupport.FIXED.now(), OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
        // Restaurar estadoFallo a NOTIFICADO para que el servicio llegue a verificar siFirme
        fallo.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        falloRepo.guardar(fallo);
        // acta.resultadoFinal permanece sin cambio (no CONDENA_FIRME)

        // Verificar estado previo explicitamente
        FalActaFallo falloAntesFirmeza = falloRepo.buscarActivo(actaId).orElseThrow();
        LocalDateTime fhFirmezaCapturada = falloAntesFirmeza.getFhFirmeza();
        OrigenFirmezaCondena origenCapturado = falloAntesFirmeza.getOrigenFirmeza();
        int versionRowFalloAntes = falloAntesFirmeza.getVersionRow();
        assertThat(falloAntesFirmeza.isSiFirme()).isTrue();
        assertThat(fhFirmezaCapturada).isNotNull();
        assertThat(origenCapturado).isNotNull();

        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("firme");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);

        // Verificar que los tres marcadores son exactamente iguales despues del rechazo
        FalActaFallo falloDespues = falloRepo.buscarActivo(actaId).orElseThrow();
        assertThat(falloDespues.isSiFirme()).isTrue();
        assertThat(falloDespues.getFhFirmeza()).isEqualTo(fhFirmezaCapturada);
        assertThat(falloDespues.getOrigenFirmeza()).isEqualTo(origenCapturado);
        assertThat(falloDespues.getVersionRow()).isEqualTo(versionRowFalloAntes);
    }

    @Test
    @DisplayName("P13: acta.resultadoFinal = CONDENA_FIRME -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_resultadoFinal_condena_firme() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P013");
        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        actaRepo.guardar(acta);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("firme");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("P14: fhVtoApelacion null -> PrecondicionVioladaException; CountingClock = 0; cero efectos")
    void precond_fhVtoApelacion_null() {
        Long actaId = crearActaConFalloCondenatorioNotificado("P014");
        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        fallo.setFhVtoApelacion(null);
        falloRepo.guardar(fallo);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("fhVtoApelacion");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    // -------------------------------------------------------------------------
    // 6.5 Apelaciones asociadas al fallo activo (CountingClock = 0 en todos)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A1: apelacion PRESENTADA bloquea CMD-FALLO-005; CountingClock = 0; cero efectos")
    void apelacion_presentada_bloquea() {
        Long actaId = crearActaConFalloCondenatorioNotificado("A001");
        Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        insertarApelacion(actaId, falloId, EstadoApelacionActa.PRESENTADA, null);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("PRESENTADA");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("A2: apelacion RECHAZADA (estado directo) -> bloquea y orienta a CMD-FALLO-006; CountingClock = 0; cero efectos")
    void apelacion_rechazada_bloquea_y_orienta_cmd006() {
        Long actaId = crearActaConFalloCondenatorioNotificado("A002");
        Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        insertarApelacion(actaId, falloId, EstadoApelacionActa.RECHAZADA, null);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("rechazada")
                .hasMessageContaining("CMD-FALLO-006");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("A3: apelacion RESUELTA + resultado RECHAZADA -> orienta a CMD-FALLO-006; CountingClock = 0; cero efectos")
    void apelacion_resuelta_rechazada_bloquea() {
        Long actaId = crearActaConFalloCondenatorioNotificado("A003");
        Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        FalActaApelacion ap = crearApelacionPresent(actaId, falloId);
        ap.resolver(ResultadoResolucionApelacion.RECHAZADA, FaltasClockTestSupport.FIXED.now(), "user", null);
        apelacionRepo.guardar(ap);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("rechazada")
                .hasMessageContaining("CMD-FALLO-006");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    @Test
    @DisplayName("A4: apelacion RESUELTA + resultado distinto de RECHAZADA -> bloquea; CountingClock = 0; cero efectos")
    void apelacion_resuelta_otro_resultado_bloquea() {
        Long actaId = crearActaConFalloCondenatorioNotificado("A004");
        Long falloId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        FalActaApelacion ap = crearApelacionPresent(actaId, falloId);
        ap.resolver(ResultadoResolucionApelacion.ACEPTADA_ABSUELVE, FaltasClockTestSupport.FIXED.now(), "user", null);
        apelacionRepo.guardar(ap);
        EstadoActaFallo antes = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("apelacion");

        assertThat(testClock.invocaciones()).isEqualTo(0);
        assertEstadoSinCambios(antes, actaId);
    }

    // -------------------------------------------------------------------------
    // 6.6 Apelacion historica de otro fallo no bloquea CMD-FALLO-005
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("H6-1: apelacion.falloId != falloActivo.id -> no bloquea; firmeza completa; clock = 1")
    void apelacion_historica_otro_fallo_ignorada() {
        Long actaId = crearActaConFalloCondenatorioNotificado("H601");
        Long falloActivoId = falloRepo.buscarActivo(actaId).orElseThrow().getId();
        // Apelacion cuyo falloId es diferente al fallo activo
        Long otroFalloId = falloActivoId + 9000L;
        insertarApelacion(actaId, otroFalloId, EstadoApelacionActa.RECHAZADA, null);

        // buscarPorFallo(falloActivoId) no la encuentra -> no bloquea
        firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));

        assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getResultadoFinal())
                .isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        assertThat(testClock.invocaciones()).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.PLAVNC)).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.CONFIR)).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 6.7 Duplicado secuencial
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DS-1: primer cmd exitoso; segundo cmd falla; CountingClock total = 1; cero efectos sobre la segunda ejecucion")
    void duplicado_secuencial() {
        Long actaId = crearActaConFalloCondenatorioNotificado("D001");

        ComandoResultado primero = firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test"));
        assertThat(primero).isNotNull();
        assertThat(testClock.invocaciones()).isEqualTo(1);

        // Capturar estado tras el primer exito (con PLAVNC, CONFIR, CONDENA_FIRME)
        EstadoActaFallo estadoTrasExito = capturarEstado(actaId);

        assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-test")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("firme");

        // El reloj no se invoco en la segunda ejecucion
        assertThat(testClock.invocaciones()).isEqualTo(1);
        // El estado no cambio tras el segundo intento rechazado
        assertEstadoSinCambios(estadoTrasExito, actaId);
        // Verificaciones explicitas de invariantes
        assertThat(contarEventos(actaId, TipoEventoActa.PLAVNC)).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.CONFIR)).isEqualTo(1);

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
    }

    // -------------------------------------------------------------------------
    // 6.8 Concurrencia
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CC-1: dos hilos, una misma instancia; exactamente 1 exito; CountingClock total = 1; exactamente una actualizacion de fallo y de acta")
    void concurrencia_un_ganador() throws Exception {
        Long actaId = crearActaConFalloCondenatorioNotificado("C001");

        // Capturar versionRow antes de la ejecucion concurrente
        int falloVersionRowAntes = falloRepo.buscarActivo(actaId).orElseThrow().getVersionRow();
        int actaVersionRowAntes = actaRepo.buscarPorId(actaId).orElseThrow().getVersionRow();

        int HILOS = 2;
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(HILOS);
        List<Future<Boolean>> futuros = new ArrayList<>();

        for (int i = 0; i < HILOS; i++) {
            futuros.add(exec.submit(() -> {
                try {
                    latch.await();
                    firmezaService.vencerPlazoApelacion(cmd(actaId, "actor-cc"));
                    return true;
                } catch (PrecondicionVioladaException e) {
                    return false;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        latch.countDown();
        exec.shutdown();

        List<Boolean> resultados = new ArrayList<>();
        try {
            for (Future<Boolean> f : futuros) {
                resultados.add(f.get());
            }
        } finally {
            exec.shutdownNow();
        }

        long ganadores = resultados.stream().filter(Boolean::booleanValue).count();
        long perdedores = resultados.stream().filter(b -> !b).count();

        assertThat(ganadores).isEqualTo(1);
        assertThat(perdedores).isEqualTo(1);
        assertThat(resultados).hasSize(2);

        assertThat(testClock.invocaciones()).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.PLAVNC)).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.CONFIR)).isEqualTo(1);
        assertThat(contarEventos(actaId, TipoEventoActa.CIERRA)).isEqualTo(0);

        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);

        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        assertThat(fallo.isSiFirme()).isTrue();
        assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);

        // Exactamente una actualizacion de fallo y una de acta por el ganador
        assertThat(fallo.getVersionRow()).isEqualTo(falloVersionRowAntes + 1);
        assertThat(acta.getVersionRow()).isEqualTo(actaVersionRowAntes + 1);

        FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
        assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
        assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
    }

    // -------------------------------------------------------------------------
    // Utilidades privadas
    // -------------------------------------------------------------------------

    private long contarEventos(Long actaId, TipoEventoActa tipo) {
        return eventoRepo.buscarPorActa(actaId).stream()
                .filter(e -> e.tipoEvt() == tipo).count();
    }

    @SuppressWarnings("deprecation")
    private void insertarApelacion(Long actaId, Long falloId,
                                   EstadoApelacionActa estado,
                                   ResultadoResolucionApelacion resultado) {
        FalActaApelacion ap = new FalActaApelacion(
                apelacionRepo.nextId(), actaId, falloId, estado,
                FaltasClockTestSupport.FIXED.now(), "user", "Fundamentos", null, true,
                FaltasClockTestSupport.FIXED.now(), "user");
        if (resultado != null) {
            ap.resolver(resultado, FaltasClockTestSupport.FIXED.now(), "user", null);
        }
        apelacionRepo.guardar(ap);
    }

    @SuppressWarnings("deprecation")
    private FalActaApelacion crearApelacionPresent(Long actaId, Long falloId) {
        FalActaApelacion ap = new FalActaApelacion(
                apelacionRepo.nextId(), actaId, falloId,
                EstadoApelacionActa.PRESENTADA,
                FaltasClockTestSupport.FIXED.now(), "user", "Fundamentos", null, true,
                FaltasClockTestSupport.FIXED.now(), "user");
        apelacionRepo.guardar(ap);
        return apelacionRepo.findById(ap.getId()).orElseThrow();
    }
}
