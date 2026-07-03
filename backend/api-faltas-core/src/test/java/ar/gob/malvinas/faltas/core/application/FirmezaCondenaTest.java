package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
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
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmezaCondenaRepository;
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
    private FirmezaCondenaRepository firmezaRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
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
        firmezaRepo = new InMemoryFirmezaCondenaRepository();

        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, new InMemoryActaEvidenciaRepository());
        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository(),

                        new ar.gob.malvinas.faltas.core.application.service.TalonarioService(new ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository()),

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository());
        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, actaId -> false);
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, pagoRepo, recalc);
        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc,
                actaId -> false);
        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                firmezaRepo, recalc);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long llegarAAnalisis(String doc) {
        LabrarActaCommand cmd = new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                LocalDate.now(), "Av. Argentina 123", "San Martin 456",
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
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotif, null));
        return actaId;
    }

    private Long crearActaConFalloCondenatorioNotificado(String doc) {
        Long actaId = llegarAAnalisis(doc);
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("2500.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotifFallo, null));
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
                    new VencerPlazoApelacionCommand(actaId, "Plazo vencido sin recurso"));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PLAVNC);
            assertThat(tipos).contains(TipoEventoActa.CONFIR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            Optional<FalActaFirmezaCondena> firmeza = firmezaRepo.buscarActivaPorActa(actaId);
            assertThat(firmeza).isPresent();
            assertThat(firmeza.get().getOrigenFirmeza())
                    .isEqualTo(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
            assertThat(firmeza.get().getApelacionId()).isNull();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Test 6: Timeline - PLAVNC aparece antes que CONFIR")
        void timeline_plavnc_antes_confir() {
            Long actaId = crearActaConFalloCondenatorioNotificado("10000002");

            firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null));

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

            Optional<FalActaFirmezaCondena> firmeza = firmezaRepo.buscarActivaPorActa(actaId);
            assertThat(firmeza).isPresent();
            assertThat(firmeza.get().getOrigenFirmeza())
                    .isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);
            assertThat(firmeza.get().getApelacionId()).isNotNull();

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
                    "TRANSITO", "DEP-001", "INS-001", LocalDate.now(),
                    "Dir", "Dir", null, null, null, "Inf", "30000001",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PRESENTADA");
        }

        @Test
        @DisplayName("Test 13: No vencer plazo si la apelacion fue RECHAZADA (usar otro comando)")
        void no_vencer_plazo_con_apelacion_rechazada() {
            Long actaId = crearActaConApelacionRechazada("30000005");

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, new InMemoryPagoCondenaRepository()),
                    actaId -> true);

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
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));

            assertThatThrownBy(() -> firmezaService.vencerPlazoApelacion(
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
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
                    new VencerPlazoApelacionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
        }

        @Test
        @DisplayName("Test 22: Firmeza no genera eventos de pago condena")
        void firmeza_no_genera_pago_condena() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000014");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));

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
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa())
                    .isNotEqualTo(SituacionAdministrativaActa.CERRADA);
        }

        @Test
        @DisplayName("Test 24: Firmeza no registra CIERRA")
        void firmeza_no_registra_cierra() {
            Long actaId = crearActaConFalloCondenatorioNotificado("30000016");
            firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }
}








