package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionAceptaAbsuelveCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.ApelacionActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 3D: firmeza de condena.
 *
 * Dos caminos:
 * 1. Vencer plazo de apelacion (sin apelacion): PLAVNC + CONFIR -> CONDENA_FIRME.
 * 2. Declarar firme por apelacion rechazada: CONFIR (sin PLAVNC) -> CONDENA_FIRME.
 *
 * CONDENA_FIRME no cierra el acta, no registra CIERRA, no inicia pago condena.
 */
@DisplayName("Slice 3D: Firmeza de condena (PLAVNC/CONFIR/CONDENA_FIRME)")
class FirmezaCondenaTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private final ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository intentoRepo = new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository();
    private FalloActaService falloService;
    private ApelacionActaService apelacionService;
    private FirmezaCondenaService firmezaService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();

        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);
        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository(),

                        new ar.gob.malvinas.faltas.core.application.service.TalonarioService(new ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED),

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(),
                new InMemoryNotificacionRepository(), FaltasClockTestSupport.FIXED);
        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, actaId -> false, FaltasClockTestSupport.FIXED, intentoRepo, new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository(),
                ar.gob.malvinas.faltas.core.support.PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, pagoRepo, recalc, FaltasClockTestSupport.FIXED);
        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc,
                actaId -> false, FaltasClockTestSupport.FIXED);
        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                recalc, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long llegarAAnalisis(String doc) {
        LabrarActaCommand cmd = new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 123", "San Martin 456",
                null, null, null, "Infractor Test", doc,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
        Long actaId = actaService.labrar(cmd).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION, null))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "firmante1", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));
        return actaId;
    }

    private Long crearActaConFalloCondenatorioNotificado(String doc) {
        Long actaId = llegarAAnalisis(doc);
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("2500.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotifFallo), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotifFallo)), null, "test-actor"));
        // Retrotraer fhVtoApelacion para satisfacer la validacion temporal del CMD-FALLO-005
        FalActaFallo falloVto = falloRepo.buscarActivo(actaId).orElseThrow();
        falloVto.setFhVtoApelacion(LocalDate.of(2026, 7, 8));
        falloRepo.guardar(falloVto);
        return actaId;
    }

    private Long crearActaConApelacionRechazada(String doc) {
        Long actaId = crearActaConFalloCondenatorioNotificado(doc);
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos apelacion", null));
        apelacionService.resolverRechazada(
                new ResolverApelacionRechazadaCommand(actaId, "Apelacion rechazada", null));
        return actaId;
    }

    // =========================================================================
    // 8.1 Eventos y enums
    // =========================================================================

    @Nested
    @DisplayName("8.1 Eventos y enums de firmeza")
    class EnumsYEventos {

        @Test
        @DisplayName("Test 1: PLAVNC existe y resuelve correctamente")
        void plavnc_existe_y_resuelve() {
            TipoEventoActa plavnc = TipoEventoActa.deCodigo("PLAVNC");
            assertThat(plavnc).isEqualTo(TipoEventoActa.PLAVNC);
            assertThat(plavnc.descripcion()).containsIgnoringCase("plazo");
        }

        @Test
        @DisplayName("Test 2: CONFIR existe y resuelve correctamente")
        void confir_existe_y_resuelve() {
            TipoEventoActa confir = TipoEventoActa.deCodigo("CONFIR");
            assertThat(confir).isEqualTo(TipoEventoActa.CONFIR);
            assertThat(confir.descripcion()).containsIgnoringCase("firme");
        }

        @Test
        @DisplayName("Test 3: ResultadoFinalActa.CONDENA_FIRME existe")
        void condena_firme_existe() {
            assertThatCode(() -> ResultadoFinalActa.valueOf("CONDENA_FIRME")).doesNotThrowAnyException();
            assertThat(ResultadoFinalActa.CONDENA_FIRME).isNotNull();
        }

        @Test
        @DisplayName("Test 4: No existe evento generico FIRMEZA")
        void no_existe_evento_firmeza() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("FIRMEZA"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // 8.2 Vencimiento de plazo sin apelacion
    // =========================================================================

    @Nested
    @DisplayName("8.2 Vencimiento de plazo sin apelacion")
    class VencimientoPlazoCasoFeliz {

        @Test
        @DisplayName("Test 5: Vencer plazo registra PLAVNC+CONFIR, asigna CONDENA_FIRME, no cierra")
        void vencer_plazo_caso_feliz() {
            Long actaId = crearActaConFalloCondenatorioNotificado("10000001");

            firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, "Plazo vencido sin recurso", "test-user"));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PLAVNC);
            assertThat(tipos).contains(TipoEventoActa.CONFIR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(fallo.isSiFirme()).isTrue();
            assertThat(fallo.getFhFirmeza()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(fallo.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
            assertThat(fallo.getFhFirma()).isNotNull();
            assertThat(fallo.getFhNotificacion()).isNotNull();

            List<FalActaEvento> eventosConFh = eventoRepo.buscarPorActa(actaId);
            FalActaEvento evtPlavnc = eventosConFh.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).findFirst().orElseThrow();
            FalActaEvento evtConfir = eventosConFh.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).findFirst().orElseThrow();
            assertThat(evtPlavnc.fhEvt()).isEqualTo(fallo.getFhFirmeza());
            assertThat(evtConfir.fhEvt()).isEqualTo(fallo.getFhFirmeza());

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Test 6: Timeline - PLAVNC aparece antes que CONFIR")
        void timeline_plavnc_antes_confir() {
            Long actaId = crearActaConFalloCondenatorioNotificado("10000002");

            firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user"));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();

            int idxPlavnc = tipos.indexOf(TipoEventoActa.PLAVNC);
            int idxConfir = tipos.indexOf(TipoEventoActa.CONFIR);
            assertThat(idxPlavnc).isGreaterThanOrEqualTo(0);
            assertThat(idxConfir).isGreaterThan(idxPlavnc);
        }
    }

    // =========================================================================
    // 8.3 Firmeza por apelacion rechazada
    // =========================================================================

    @Nested
    @DisplayName("8.3 Firmeza por apelacion rechazada")
    class FirmezaPorApelacionRechazadaCasoFeliz {

        @Test
        @DisplayName("Test 7: Declarar firme por apelacion rechazada: CONFIR, CONDENA_FIRME, sin PLAVNC ni CIERRA")
        void declarar_firme_por_apelacion_rechazada() {
            Long actaId = crearActaConApelacionRechazada("20000001");

            firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, "Apelacion rechazada firme"));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.CONFIR);
            assertThat(tipos).doesNotContain(TipoEventoActa.PLAVNC);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.FIRME);
            assertThat(fallo.isSiFirme()).isTrue();
            assertThat(fallo.getFhFirmeza()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(fallo.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);
            assertThat(fallo.getFhFirma()).isNotNull();
            assertThat(fallo.getFhNotificacion()).isNotNull();

            List<FalActaEvento> eventosConFh = eventoRepo.buscarPorActa(actaId);
            FalActaEvento evtConfirAp = eventosConFh.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).findFirst().orElseThrow();
            assertThat(evtConfirAp.fhEvt()).isEqualTo(fallo.getFhFirmeza());

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Test 8: Timeline - APEPRE, APERAZ, CONFIR presentes; sin PLAVNC")
        void timeline_apepre_aperaz_confir_sin_plavnc() {
            Long actaId = crearActaConApelacionRechazada("20000002");

            firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APEPRE);
            assertThat(tipos).contains(TipoEventoActa.APERAZ);
            assertThat(tipos).contains(TipoEventoActa.CONFIR);
            assertThat(tipos).doesNotContain(TipoEventoActa.PLAVNC);

            int idxAperaz = tipos.indexOf(TipoEventoActa.APERAZ);
            int idxConfir = tipos.indexOf(TipoEventoActa.CONFIR);
            assertThat(idxConfir).isGreaterThan(idxAperaz);
        }
    }

    // =========================================================================
    // 8.4 Casos invalidos
    // =========================================================================

    @Nested
    @DisplayName("8.4 Casos invalidos")
    class CasosInvalidos {

        @Test
        @DisplayName("Test 9: No firmeza sin fallo activo")
        void no_firmeza_sin_fallo() {
            Long actaId = actaService.labrar(new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001", FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    "Dir", "Dir", null, null, null, "Inf", "30000001",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fallo activo");
        }

        @Test
        @DisplayName("Test 10: No firmeza si fallo no es condenatorio")
        void no_firmeza_fallo_absolutorio() {
            Long actaId = llegarAAnalisis("30000002");
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(actaId, "Absolutorias", null));

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("condenatorio");
        }

        @Test
        @DisplayName("Test 11: No firmeza si fallo condenatorio no esta NOTIFICADO")
        void no_firmeza_fallo_no_notificado() {
            Long actaId = llegarAAnalisis("30000003");
            falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                    actaId, new BigDecimal("1000"), "Cargos", null));

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("NOTIFICADO");
        }

        @Test
        @DisplayName("Test 12: No vencer plazo si existe apelacion PRESENTADA")
        void no_vencer_plazo_con_apelacion_presentada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000004");
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos", null));

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PRESENTADA");
        }

        @Test
        @DisplayName("Test 13: No vencer plazo si la apelacion fue RECHAZADA (usar otro comando)")
        void no_vencer_plazo_con_apelacion_rechazada() {
            Long actaId = crearActaConApelacionRechazada("30000005");

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("rechazada");
        }

        @Test
        @DisplayName("Test 14: No declarar firme por apelacion si no hay apelacion")
        void no_declarar_firme_sin_apelacion() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000006");

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion");
        }

        @Test
        @DisplayName("Test 15: No declarar firme si apelacion sigue PRESENTADA")
        void no_declarar_firme_apelacion_presentada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000007");
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos", null));

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PRESENTADA");
        }

        @Test
        @DisplayName("Test 16: No declarar firme si apelacion fue ACEPTADA_ABSUELVE")
        void no_declarar_firme_apelacion_aceptada_absuelve() {
            // Usamos un bloqueantesChecker que siempre bloquea para que la acta no se cierre
            ApelacionActaService apelacionServiceConBloqueantes = new ApelacionActaService(
                    actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED),
                    actaId -> true, FaltasClockTestSupport.FIXED);

            Long actaId = crearActaConFalloCondenatorioNotificado("30000008");
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos", null));
            apelacionServiceConBloqueantes.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Absolucion segunda instancia", null));

            assertThatThrownBy(() -> firmezaService.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("RECHAZADA");
        }

        @Test
        @DisplayName("Test 17: No doble firmeza (vencimiento de plazo dos veces falla)")
        void no_doble_firmeza() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000009");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("firme");
        }

        @Test
        @DisplayName("Test 18: No firmeza si acta cerrada")
        void no_firmeza_acta_cerrada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000010");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("Test 19: No firmeza si acta archivada")
        void no_firmeza_acta_archivada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000011");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("archivada");
        }

        @Test
        @DisplayName("Test 20: No firmeza si acta anulada")
        void no_firmeza_acta_anulada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000012");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulada");
        }

        @Test
        @DisplayName("Test 21: No firmeza si acta paralizada")
        void no_firmeza_acta_paralizada() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000013");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
        }

        @Test
        @DisplayName("Test 22: Firmeza no genera eventos de pago condena")
        void firmeza_no_genera_pago_condena() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000014");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.PCOINF, TipoEventoActa.PCOCNF, TipoEventoActa.PCOOBS);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO);
        }

        @Test
        @DisplayName("Test 23: Firmeza no cierra el acta")
        void firmeza_no_cierra_acta() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000015");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa())
                    .isNotEqualTo(SituacionAdministrativaActa.CERRADA);
        }

        @Test
        @DisplayName("Test 24: Firmeza no registra CIERRA")
        void firmeza_no_registra_cierra() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000016");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // 8.5 Reloj contado: un solo instante por comando exitoso
    // =========================================================================

    static class CountingClock extends FaltasClock {
        private final AtomicInteger llamadas = new AtomicInteger(0);
        private final LocalDateTime instante = FaltasClockTestSupport.FIXED.now();

        @Override
        public LocalDateTime now() {
            llamadas.incrementAndGet();
            return instante;
        }

        public int getLlamadas() { return llamadas.get(); }
        public void reset() { llamadas.set(0); }
    }

    @Nested
    @DisplayName("8.5 Reloj contado: instante unico por comando")
    class RelojContado {

        private CountingClock contando;
        private FirmezaCondenaService firmezaConReloj;

        @BeforeEach
        void setUpReloj() {
            contando = new CountingClock();
            firmezaConReloj = new FirmezaCondenaService(
                    actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo,
                            new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED),
                    contando);
        }

        @Test
        @DisplayName("vencerPlazoApelacion: exactamente un now() en FirmezaCondenaService")
        void vencer_plazo_consume_un_solo_instante() {
            Long actaId = crearActaConFalloCondenatorioNotificado("40000001");
            contando.reset();
            firmezaConReloj.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null, "test-user"));
            assertThat(contando.getLlamadas()).isEqualTo(1);
        }

        @Test
        @DisplayName("declararFirmePorApelacionRechazada: exactamente un now() en FirmezaCondenaService")
        void apelacion_rechazada_consume_un_solo_instante() {
            Long actaId = crearActaConApelacionRechazada("40000002");
            contando.reset();
            firmezaConReloj.declararFirmePorApelacionRechazada(
                    new DeclararCondenaFirmePorApelacionRechazadaCommand(actaId, null));
            assertThat(contando.getLlamadas()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8.6 Invalidez sin efectos parciales
    // =========================================================================

    @Nested
    @DisplayName("8.6 Invalidez sin efectos: fallo permanece NOTIFICADO")
    class InvalidezSinEfectos {

        @Test
        @DisplayName("Comando invalido (apelacion PRESENTADA) no muta fallo ni acta")
        void comando_invalido_no_muta_fallo_ni_acta() {
            Long actaId = crearActaConFalloCondenatorioNotificado("50000099");
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos", null));

            FalActaFallo falloAntes = falloRepo.buscarActivo(actaId).orElseThrow();
            EstadoFalloActa estadoAntes = falloAntes.getEstadoFallo();
            boolean siFirmeAntes = falloAntes.isSiFirme();
            LocalDateTime fhFirmezaAntes = falloAntes.getFhFirmeza();
            ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena origenAntes = falloAntes.getOrigenFirmeza();
            ResultadoFinalActa resultadoAntes = actaRepo.buscarPorId(actaId).orElseThrow().getResultadoFinal();
            long confirAntes = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).count();
            long plavncAntes = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).count();

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null, "test-user")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PRESENTADA");

            FalActaFallo falloDespues = falloRepo.buscarActivo(actaId).orElseThrow();
            assertThat(falloDespues.getEstadoFallo()).isEqualTo(estadoAntes);
            assertThat(falloDespues.isSiFirme()).isEqualTo(siFirmeAntes);
            assertThat(falloDespues.getFhFirmeza()).isEqualTo(fhFirmezaAntes);
            assertThat(falloDespues.getOrigenFirmeza()).isEqualTo(origenAntes);

            FalActa actaDespues = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(actaDespues.getResultadoFinal()).isEqualTo(resultadoAntes);
            assertThat(actaDespues.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.CONDENA_FIRME);

            long confirDespues = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CONFIR).count();
            long plavncDespues = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PLAVNC).count();
            assertThat(confirDespues).isEqualTo(confirAntes);
            assertThat(plavncDespues).isEqualTo(plavncAntes);
        }
    }
}
