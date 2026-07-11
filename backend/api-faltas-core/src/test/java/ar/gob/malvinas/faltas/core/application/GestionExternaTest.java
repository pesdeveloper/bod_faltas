package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DerivarGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReingresarDesdeGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarPagoExternoGestionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.GestionExternaService;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ModoReingresoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.GestionExternaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmezaCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryGestionExternaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de Slice 6A+6B: Gestion externa - derivacion (EXTDER) y reingreso (EXTRET).
 *
 * Flujo habilitado (6A): CONDENA_FIRME -> EXTDER -> bloqueActual=GEXT / situacion=EN_GESTION_EXTERNA.
 * Flujo habilitado (6B): GEXT -> EXTRET -> ANAL / ACTIVA. Modos: REINGRESO_PARA_REVISION, REINGRESO_SIN_PAGO.
 * No implementa pago apremio (PAGAPR) ni cierre externo (reservados slices futuros).
 * DRVEXT sigue prohibido. Modos reservados (bloqueados): REINGRESO_PARA_CIERRE, REINGRESO_CON_PAGO.
 * Desde 6D-2: REINGRESO_PARA_NUEVO_FALLO y REINGRESO_CON_DICTAMEN habilitados con resultado explicito.
 */
@DisplayName("Slice 6A+6B+6C: Gestion externa - derivacion (EXTDER), reingreso (EXTRET) y pago externo (PAGAPR)")
class GestionExternaTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoVolRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;
    private FirmezaCondenaRepository firmezaRepo;
    private PagoCondenaRepository pagoCondenaRepo;
    private GestionExternaRepository gestionExternaRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private FirmezaCondenaService firmezaService;
    private GestionExternaService gestionExternaService;
    private PagoCondenaService pagoCondenaService;

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
        firmezaRepo = new InMemoryFirmezaCondenaRepository();
        pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        gestionExternaRepo = new InMemoryGestionExternaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED);

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
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, FaltasClockTestSupport.FIXED);
        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, firmezaRepo, recalc, FaltasClockTestSupport.FIXED);
        gestionExternaService = new GestionExternaService(
                actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);
        pagoCondenaService = new PagoCondenaService(
                actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondenaRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long crearActaConCondenaFirme(String docNum) {
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
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotif, null));

        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("5000.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotifFallo, null));

        firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));
        return actaId;
    }

    private void derivar(Long actaId) {
        gestionExternaService.derivar(new DerivarGestionExternaCommand(
                actaId, TipoGestionExterna.APREMIO,
                "Sin pago voluntario ni pago condena. Deriva a apremio.", null));
    }

    // =========================================================================
    // 9.1 Eventos / guardrails
    // =========================================================================

    @Nested
    @DisplayName("9.1 Eventos: guardrails EXTDER/EXTRET/PAGAPR/DRVEXT")
    class EventosGuardrails {

        @Test
        @DisplayName("Test 1: EXTDER existe y resuelve correctamente")
        void extder_existe() {
            TipoEventoActa extder = TipoEventoActa.deCodigo("EXTDER");
            assertThat(extder).isEqualTo(TipoEventoActa.EXTDER);
            assertThat(extder.codigo()).isEqualTo("EXTDER");
        }

        @Test
        @DisplayName("Test 2: EXTRET existe y resuelve, pero no se emite en derivacion")
        void extret_existe_pero_no_se_emite() {
            TipoEventoActa extret = TipoEventoActa.deCodigo("EXTRET");
            assertThat(extret).isEqualTo(TipoEventoActa.EXTRET);

            Long actaId = crearActaConCondenaFirme("60000001");
            derivar(actaId);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
        }

        @Test
        @DisplayName("Test 3: PAGAPR existe y resuelve, pero no se emite en derivacion")
        void pagapr_existe_pero_no_se_emite() {
            TipoEventoActa pagapr = TipoEventoActa.deCodigo("PAGAPR");
            assertThat(pagapr).isEqualTo(TipoEventoActa.PAGAPR);

            Long actaId = crearActaConCondenaFirme("60000002");
            derivar(actaId);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
        }

        @Test
        @DisplayName("Test 4: DRVEXT sigue rechazado como evento productivo")
        void drvext_sigue_rechazado() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("DRVEXT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DRVEXT");

            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("DRVEXT");
                assertThat(t.name()).isNotEqualTo("DRVEXT");
            }
        }
    }

    // =========================================================================
    // 9.2 Derivacion feliz
    // =========================================================================

    @Nested
    @DisplayName("9.2 Derivacion feliz: CONDENA_FIRME sin pago confirmado")
    class DerivacionFeliz {

        @Test
        @DisplayName("Test 5: Derivar acta con CONDENA_FIRME crea gestion externa activa y muta estado")
        void derivar_condena_firme_sin_pago() {
            Long actaId = crearActaConCondenaFirme("60000010");

            gestionExternaService.derivar(new DerivarGestionExternaCommand(
                    actaId, TipoGestionExterna.APREMIO,
                    "Sin pago condena. Deriva a apremio.", "Observaciones adicionales"));

            FalGestionExterna gestion = gestionExternaRepo.buscarActiva(actaId).orElseThrow();
            assertThat(gestion.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.DERIVADA);
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.SIN_RESULTADO);
            assertThat(gestion.getModoReingresoGestionExterna()).isNull();
            assertThat(gestion.isSiActiva()).isTrue();
            assertThat(gestion.getTipoGestionExterna()).isEqualTo(TipoGestionExterna.APREMIO);
            assertThat(gestion.getMotivoDerivacion()).isNotBlank();
            assertThat(gestion.getFechaDerivacion()).isNotNull();

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.GEXT);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.EN_GESTION_EXTERNA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.EXTDER);
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.GESTION_EXTERNA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Test 6: Timeline - EXTDER aparece despues de los eventos previos de condena firme")
        void timeline_extder_al_final() {
            Long actaId = crearActaConCondenaFirme("60000011");
            derivar(actaId);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).isNotEmpty();

            TipoEventoActa ultimoEvento = eventos.get(eventos.size() - 1).tipoEvt();
            assertThat(ultimoEvento).isEqualTo(TipoEventoActa.EXTDER);

            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            int idxConfir = tipos.indexOf(TipoEventoActa.CONFIR);
            int idxExtder = tipos.lastIndexOf(TipoEventoActa.EXTDER);
            assertThat(idxExtder).isGreaterThan(idxConfir);
        }
    }

    // =========================================================================
    // 9.3 Origen determinista
    // =========================================================================

    @Nested
    @DisplayName("9.3 Origen determinista: guardar origen para futuro reingreso")
    class OrigenDeterminista {

        @Test
        @DisplayName("Test 7: Al derivar se guarda bloque, situacion, bandeja y accion origen")
        void derivar_guarda_origen_completo() {
            Long actaId = crearActaConCondenaFirme("60000020");
            derivar(actaId);

            FalGestionExterna gestion = gestionExternaRepo.buscarActiva(actaId).orElseThrow();

            assertThat(gestion.getBloqueOrigen()).isNotNull();
            assertThat(gestion.getSituacionAdministrativaOrigen()).isNotNull();
            assertThat(gestion.getCodigoBandejaOrigen()).isNotNull();
            assertThat(gestion.getAccionPendienteOrigen()).isNotNull();
        }

        @Test
        @DisplayName("Test 8: El origen guardado es consistente con el estado previo a la derivacion")
        void origen_es_el_estado_previo_a_derivacion() {
            Long actaId = crearActaConCondenaFirme("60000021");

            FalActaSnapshot snapAntes = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            CodigoBandeja bandejaAntes = snapAntes.getCodBandeja();
            AccionPendiente accionAntes = snapAntes.getAccionPendiente();
            FalActa actaAntes = actaRepo.buscarPorId(actaId).orElseThrow();
            BloqueActual bloqueAntes = actaAntes.getBloqueActual();
            SituacionAdministrativaActa situacionAntes = actaAntes.getSituacionAdministrativa();

            derivar(actaId);

            FalGestionExterna gestion = gestionExternaRepo.buscarActiva(actaId).orElseThrow();
            assertThat(gestion.getBloqueOrigen()).isEqualTo(bloqueAntes);
            assertThat(gestion.getSituacionAdministrativaOrigen()).isEqualTo(situacionAntes);
            assertThat(gestion.getCodigoBandejaOrigen()).isEqualTo(bandejaAntes);
            assertThat(gestion.getAccionPendienteOrigen()).isEqualTo(accionAntes);
        }
    }

    // =========================================================================
    // 9.4 Casos invalidos
    // =========================================================================

    @Nested
    @DisplayName("9.4 Casos invalidos: precondiciones rechazadas")
    class CasosInvalidos {

        @Test
        @DisplayName("Test 9: No permitir derivar si no hay acta")
        void no_derivar_sin_acta() {
            assertThatThrownBy(() -> gestionExternaService.derivar(new DerivarGestionExternaCommand(
                    999999L, TipoGestionExterna.APREMIO, "Motivo", null)))
                    .isInstanceOf(ActaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Test 10: No permitir derivar si acta esta cerrada")
        void no_derivar_acta_cerrada() {
            Long actaId = crearActaConCondenaFirme("60000030");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("Test 11: No permitir derivar si acta esta archivada")
        void no_derivar_acta_archivada() {
            Long actaId = crearActaConCondenaFirme("60000031");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("archivada");
        }

        @Test
        @DisplayName("Test 12: No permitir derivar si acta esta anulada")
        void no_derivar_acta_anulada() {
            Long actaId = crearActaConCondenaFirme("60000032");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulada");
        }

        @Test
        @DisplayName("Test 13: No permitir derivar si acta esta paralizada")
        void no_derivar_acta_paralizada() {
            Long actaId = crearActaConCondenaFirme("60000033");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
        }

        @Test
        @DisplayName("Test 14: No permitir derivar si ya esta en GEXT")
        void no_derivar_ya_en_gext() {
            Long actaId = crearActaConCondenaFirme("60000034");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            acta.setBloqueActual(BloqueActual.GEXT);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("gestion externa");
        }

        @Test
        @DisplayName("Test 15: No permitir derivar si ya existe gestion externa activa")
        void no_derivar_si_ya_existe_gestion_activa() {
            Long actaId = crearActaConCondenaFirme("60000035");
            derivar(actaId);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            acta.setBloqueActual(BloqueActual.ANAL);
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("gestion externa activa");
        }

        @Test
        @DisplayName("Test 16: No permitir derivar si resultado final es ABSUELTO")
        void no_derivar_absuelto() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. X 1", "Calle Y 2",
                    null, null, null, "Test", "60000036",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ABSUELTO");
        }

        @Test
        @DisplayName("Test 17: No permitir derivar si resultado final es PAGO_VOLUNTARIO_PAGADO")
        void no_derivar_PAGO_VOLUNTARIO_PAGADO() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. X 1", "Calle Y 2",
                    null, null, null, "Test", "60000037",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PAGO_VOLUNTARIO_PAGADO");
        }

        @Test
        @DisplayName("Test 18: No permitir derivar si resultado final es CONDENA_FIRME_PAGADA")
        void no_derivar_condena_firme_pagada() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. X 1", "Calle Y 2",
                    null, null, null, "Test", "60000038",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA_FIRME_PAGADA");
        }

        @Test
        @DisplayName("Test 19: No permitir derivar si pago condena esta CONFIRMADO")
        void no_derivar_si_pago_condena_confirmado() {
            Long actaId = crearActaConCondenaFirme("60000039");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> derivar(actaId))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Test 20: No permitir derivar sin tipo de gestion externa")
        void no_derivar_sin_tipo_gestion() {
            Long actaId = crearActaConCondenaFirme("60000040");

            assertThatThrownBy(() -> gestionExternaService.derivar(new DerivarGestionExternaCommand(
                    actaId, null, "Motivo valido", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("tipo de gestion");
        }

        @Test
        @DisplayName("Test 21: No permitir derivar sin motivo de derivacion")
        void no_derivar_sin_motivo() {
            Long actaId = crearActaConCondenaFirme("60000041");

            assertThatThrownBy(() -> gestionExternaService.derivar(new DerivarGestionExternaCommand(
                    actaId, TipoGestionExterna.APREMIO, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivo");

            assertThatThrownBy(() -> gestionExternaService.derivar(new DerivarGestionExternaCommand(
                    actaId, TipoGestionExterna.APREMIO, "   ", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivo");
        }

        @Test
        @DisplayName("Test 22: La derivacion no cierra el acta")
        void derivacion_no_cierra_el_acta() {
            Long actaId = crearActaConCondenaFirme("60000050");
            derivar(actaId);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isNotEqualTo(BloqueActual.CERR);
        }

        @Test
        @DisplayName("Test 23: La derivacion no registra CIERRA")
        void derivacion_no_registra_cierra() {
            Long actaId = crearActaConCondenaFirme("60000051");
            derivar(actaId);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("Test 24: La derivacion no registra EXTRET")
        void derivacion_no_registra_extret() {
            Long actaId = crearActaConCondenaFirme("60000052");
            derivar(actaId);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
        }

        @Test
        @DisplayName("Test 25: La derivacion no registra PAGAPR")
        void derivacion_no_registra_pagapr() {
            Long actaId = crearActaConCondenaFirme("60000053");
            derivar(actaId);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
        }
    }

    // =========================================================================
    // Slice 6B: Reingreso desde gestion externa (EXTRET)
    // =========================================================================

    // -------------------------------------------------------------------------
    // Helpers Slice 6B
    // -------------------------------------------------------------------------

    private Long derivarActa(Long actaId) {
        gestionExternaService.derivar(new DerivarGestionExternaCommand(
                actaId, TipoGestionExterna.APREMIO, "Motivo derivacion test", null));
        return actaId;
    }

    private ComandoResultado reingresar(Long actaId, ModoReingresoGestionExterna modo) {
        return gestionExternaService.reingresar(new ReingresarDesdeGestionExternaCommand(
                actaId, modo, "Motivo reingreso test", null, null, null));
    }

    // -------------------------------------------------------------------------
    // Casos felices
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Reingreso feliz")
    class ReingresoFeliz {

        @Test
        @DisplayName("Test 6B-01: Reingreso feliz a analisis")
        void reingreso_feliz_a_analisis() {
            Long actaId = crearActaConCondenaFirme("60001001");
            derivarActa(actaId);

            ComandoResultado resultado = reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            assertThat(resultado.idActa()).isEqualTo(actaId);
            assertThat(resultado.tipoEvento()).isEqualTo("EXTRET");
        }

        @Test
        @DisplayName("Test 6B-02: Reingreso feliz a pago condena")
        void reingreso_feliz_a_pago_condena() {
            Long actaId = crearActaConCondenaFirme("60001002");
            derivarActa(actaId);

            ComandoResultado resultado = reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO);

            assertThat(resultado.idActa()).isEqualTo(actaId);
            assertThat(resultado.tipoEvento()).isEqualTo("EXTRET");
        }

        @Test
        @DisplayName("Test 6B-03: Reingreso acepta resultadoGestionExterna informado (par coherente)")
        void reingreso_acepta_resultado_gestion_externa() {
            Long actaId = crearActaConCondenaFirme("60001003");
            derivarActa(actaId);

            gestionExternaService.reingresar(new ReingresarDesdeGestionExternaCommand(
                    actaId,
                    ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                    "Reingresa sin cambios sustantivos",
                    ResultadoGestionExterna.SIN_CAMBIOS,
                    "Observacion opcional", null));

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId)
                    .orElseThrow();
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.SIN_CAMBIOS);
            assertThat(gestion.getObservacionesReingreso()).isEqualTo("Observacion opcional");
        }
    }

    // -------------------------------------------------------------------------
    // Eventos registrados
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Eventos registrados en reingreso")
    class EventosReingreso {

        @Test
        @DisplayName("Test 6B-04: Registra EXTRET al reingresar")
        void registra_extret() {
            Long actaId = crearActaConCondenaFirme("60001010");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.EXTRET);
        }

        @Test
        @DisplayName("Test 6B-05: No registra PAGAPR en Slice 6B")
        void no_registra_pagapr() {
            Long actaId = crearActaConCondenaFirme("60001011");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
        }

        @Test
        @DisplayName("Test 6B-06: No registra CIERRA al reingresar")
        void no_registra_cierra() {
            Long actaId = crearActaConCondenaFirme("60001012");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("Test 6B-07: No registra EXTDER al reingresar")
        void no_registra_extder_adicional() {
            Long actaId = crearActaConCondenaFirme("60001013");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            long countExtder = eventoRepo.buscarPorActa(actaId)
                    .stream().filter(e -> e.tipoEvt() == TipoEventoActa.EXTDER).count();
            assertThat(countExtder).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // Estado de la gestion externa tras reingreso
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Estado de gestion externa tras reingreso")
    class EstadoGestionExternaTrasReingreso {

        @Test
        @DisplayName("Test 6B-08: Cierra gestion externa activa (siActiva = false)")
        void deja_si_activa_false() {
            Long actaId = crearActaConCondenaFirme("60001020");
            derivarActa(actaId);

            assertThat(gestionExternaRepo.existeActiva(actaId)).isTrue();
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);
            assertThat(gestionExternaRepo.existeActiva(actaId)).isFalse();
        }

        @Test
        @DisplayName("Test 6B-09: Deja estadoGestionExterna = REINGRESADA")
        void deja_estado_reingresada() {
            Long actaId = crearActaConCondenaFirme("60001021");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.REINGRESADA);
        }

        @Test
        @DisplayName("Test 6B-10: Persiste modoReingresoGestionExterna en la gestion externa")
        void persiste_modo_reingreso() {
            Long actaId = crearActaConCondenaFirme("60001022");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getModoReingresoGestionExterna())
                    .isEqualTo(ModoReingresoGestionExterna.REINGRESO_SIN_PAGO);
        }

        @Test
        @DisplayName("Test 6B-11: Persiste motivoReingreso en la gestion externa")
        void persiste_motivo_reingreso() {
            Long actaId = crearActaConCondenaFirme("60001023");
            derivarActa(actaId);

            gestionExternaService.reingresar(new ReingresarDesdeGestionExternaCommand(
                    actaId,
                    ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                    "Motivo especifico de reingreso",
                    null, null, null));

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getMotivoReingreso()).isEqualTo("Motivo especifico de reingreso");
        }

        @Test
        @DisplayName("Test 6B-12: Persiste fechaReingreso en la gestion externa")
        void persiste_fecha_reingreso() {
            Long actaId = crearActaConCondenaFirme("60001024");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getFechaReingreso()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Mutacion del acta tras reingreso
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Mutacion del acta tras reingreso")
    class MutacionActaReingreso {

        @Test
        @DisplayName("Test 6B-13: Deja situacionAdministrativa = ACTIVA")
        void deja_situacion_activa() {
            Long actaId = crearActaConCondenaFirme("60001030");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("Test 6B-14: Deja bloqueActual = ANAL")
        void deja_bloque_anal() {
            Long actaId = crearActaConCondenaFirme("60001031");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("Test 6B-15: Recalcula snapshot - REINGRESO_PARA_REVISION con CONDENA_FIRME")
        void recalcula_snapshot_a_analisis() {
            Long actaId = crearActaConCondenaFirme("60001032");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            // REINGRESO_PARA_REVISION: vuelve a PENDIENTE_ANALISIS para nueva evaluacion.
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
        }

        @Test
        @DisplayName("Test 6B-16: Recalcula snapshot - REINGRESO_SIN_PAGO")
        void recalcula_snapshot_a_pago_condena() {
            Long actaId = crearActaConCondenaFirme("60001033");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }
    }

    // -------------------------------------------------------------------------
    // Precondiciones de reingreso
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Precondiciones de reingreso")
    class PrecondicionesReingreso {

        @Test
        @DisplayName("Test 6B-17: Rechaza sin gestion externa activa")
        void rechaza_sin_gestion_activa() {
            Long actaId = crearActaConCondenaFirme("60001040");
            // Poner el acta en GEXT manualmente sin gestión activa
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setBloqueActual(BloqueActual.GEXT);
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("No existe gestion externa activa");
        }

        @Test
        @DisplayName("Test 6B-18: Rechaza acta no en bloque GEXT")
        void rechaza_acta_no_gext() {
            Long actaId = crearActaConCondenaFirme("60001041");
            derivarActa(actaId);
            // Forzar bloque a ANAL manteniendo situacion EN_GESTION_EXTERNA
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setBloqueActual(BloqueActual.ANAL);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("GEXT");
        }

        @Test
        @DisplayName("Test 6B-19: Rechaza acta no en situacion EN_GESTION_EXTERNA")
        void rechaza_acta_no_en_gestion_externa() {
            Long actaId = crearActaConCondenaFirme("60001042");
            derivarActa(actaId);
            // Forzar situacion a ACTIVA manteniendo bloque GEXT
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("EN_GESTION_EXTERNA");
        }

        @Test
        @DisplayName("Test 6B-20: Rechaza modo nulo")
        void rechaza_modo_nulo() {
            Long actaId = crearActaConCondenaFirme("60001043");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, null, "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("modo de reingreso");
        }

        @Test
        @DisplayName("Test 6B-21: Rechaza motivo nulo")
        void rechaza_motivo_nulo() {
            Long actaId = crearActaConCondenaFirme("60001044");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, null, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivo de reingreso");
        }

        @Test
        @DisplayName("Test 6B-22: Rechaza motivo vacio")
        void rechaza_motivo_vacio() {
            Long actaId = crearActaConCondenaFirme("60001045");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, "   ", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivo de reingreso");
        }

        @Test
        @DisplayName("Test 6B-23: Rechaza REINGRESO_PARA_CIERRE (reservado para slice futuro)")
        void rechaza_reingresar_a_cierre() {
            Long actaId = crearActaConCondenaFirme("60001046");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_CIERRE");
        }

        @Test
        @DisplayName("Test 6B-24: REINGRESO_PARA_NUEVO_FALLO sin resultado explicito falla (requiere ABSUELVE)")
        void rechaza_reingresar_a_archivo() {
            Long actaId = crearActaConCondenaFirme("60001047");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_NUEVO_FALLO");
        }

        @Test
        @DisplayName("Test 6B-25: Rechaza reingreso doble (segunda llamada falla)")
        void rechaza_reingreso_doble() {
            Long actaId = crearActaConCondenaFirme("60001048");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            // Segundo reingreso debe fallar (acta ya no esta en EN_GESTION_EXTERNA)
            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Test 6B-26: Rechaza REINGRESO_SIN_PAGO si resultadoFinal no es CONDENA_FIRME")
        void rechaza_pago_condena_sin_condena_firme() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. X 1", "Calle Y 2",
                    null, null, null, "Test", "60001049",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setBloqueActual(BloqueActual.GEXT);
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            // No tiene CONDENA_FIRME
            actaRepo.guardar(acta);

            // Crear gestión externa artificial
            FalGestionExterna gestionManual = new FalGestionExterna(9999L, actaId, FaltasClockTestSupport.FIXED.now(), "SYS");
            gestionManual.setTipoGestionExterna(ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna.APREMIO);
            gestionManual.setEstadoGestionExterna(EstadoGestionExterna.DERIVADA);
            gestionManual.setSiActiva(true);
            gestionExternaRepo.guardar(gestionManual);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA_FIRME");
        }
    }

    // -------------------------------------------------------------------------
    // Guardrails Slice 6B
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Slice 6B: Guardrails y restricciones")
    class GuardrailsSlice6B {

        @Test
        @DisplayName("Test 6B-27: PAGAPR existe en enum pero no se emite en Slice 6B")
        void pagapr_existe_pero_no_emitido() {
            Long actaId = crearActaConCondenaFirme("60001050");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
            // PAGAPR existe en el enum (guardrail: no se elimina)
            assertThat(TipoEventoActa.PAGAPR).isNotNull();
        }

        @Test
        @DisplayName("Test 6B-28: EXTRET existe y es el evento correcto de reingreso")
        void extret_es_evento_reingreso() {
            assertThat(TipoEventoActa.EXTRET.codigo()).isEqualTo("EXTRET");
            assertThat(TipoEventoActa.EXTRET.descripcion()).isNotBlank();
        }

        @Test
        @DisplayName("Test 6B-29: DRVEXT no existe como evento productivo")
        void drvext_no_existe() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> TipoEventoActa.deCodigo("DRVEXT"));
        }

        @Test
        @DisplayName("Test 6B-30: InMemoryGestionExternaRepository no usa JDBC")
        void repositorio_usa_in_memory() {
            // El repositorio es in-memory: no importa ninguna clase JDBC
            assertThat(gestionExternaRepo.getClass().getSimpleName())
                    .contains("InMemory");
        }

        @Test
        @DisplayName("Test 6B-31: Ciclo completo derivar -> reingresar preserva trazabilidad")
        void ciclo_completo_preserva_trazabilidad() {
            Long actaId = crearActaConCondenaFirme("60001051");
            derivarActa(actaId);
            reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();

            // Ciclo externo completo trazado
            assertThat(tipos).contains(TipoEventoActa.EXTDER);
            assertThat(tipos).contains(TipoEventoActa.EXTRET);

            // Trazabilidad en la gestion externa
            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getFechaDerivacion()).isNotNull();
            assertThat(gestion.getFechaReingreso()).isNotNull();
            assertThat(gestion.getBloqueOrigen()).isNotNull();
        }
    }
    // =========================================================================
    // Slice 6C: Pago externo de gestion externa (PAGAPR)
    // =========================================================================

    private RegistrarPagoExternoGestionCommand cmdPagoExterno(Long actaId, String obs) {
        return new RegistrarPagoExternoGestionCommand(actaId, obs);
    }

    @Nested
    @DisplayName("EventosPagoExterno")
    class EventosPagoExterno {

        @Test
        @DisplayName("6C-01: PAGAPR se registra al ejecutar el comando")
        void pagaprSeRegistra() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-001");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PAGAPR);
        }

        @Test
        @DisplayName("6C-02: Sin bloqueantes, CIERRA se registra despues de PAGAPR")
        void sinBloqueantes_CierraDespesDePagapr() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-002");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            int idxPagapr = tipos.indexOf(TipoEventoActa.PAGAPR);
            int idxCierra = tipos.indexOf(TipoEventoActa.CIERRA);
            assertThat(idxPagapr).isGreaterThan(-1);
            assertThat(idxCierra).isGreaterThan(idxPagapr);
        }

        @Test
        @DisplayName("6C-03: Con bloqueantes, solo PAGAPR, no CIERRA")
        void conBloqueantesNoCierra() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-003");
            derivar(actaId);
            GestionExternaService servicioConBloqueantes = new GestionExternaService(
                    actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED),
                    id -> true, FaltasClockTestSupport.FIXED);
            servicioConBloqueantes.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PAGAPR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("6C-04: EXTRET no se emite al registrar pago externo")
        void extretNoSeEmite() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-004");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
        }

        @Test
        @DisplayName("6C-05: Observaciones quedan en la descripcion del evento PAGAPR")
        void observacionesEnDescripcionEvento() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-005");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(
                    cmdPagoExterno(actaId, "pago recibido por ventanilla"));
            FalActaEvento evPagapr = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.PAGAPR)
                    .findFirst().orElseThrow();
            assertThat(evPagapr.descripcionLegible()).contains("pago recibido por ventanilla");
        }
    }

    @Nested
    @DisplayName("CamposCierreGestionExterna")
    class CamposCierreGestionExterna {

        @Test
        @DisplayName("6C-06: siActiva = false tras pago externo")
        void siActivaFalse() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-006");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.isSiActiva()).isFalse();
        }

        @Test
        @DisplayName("6C-07: estadoGestionExterna = CERRADA_EXTERNA tras pago externo")
        void estadoCerradaExterna() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-007");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.CERRADA_EXTERNA);
        }

        @Test
        @DisplayName("6C-08: resultadoGestionExterna = PAGO_REGISTRADO tras pago externo")
        void resultadoPagoExternoInformado() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-008");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.getResultadoGestionExterna())
                    .isEqualTo(ResultadoGestionExterna.PAGO_REGISTRADO);
        }

        @Test
        @DisplayName("6C-09: fechaCierreGestionExterna no es nula tras pago externo")
        void fechaCierreNoNula() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-009");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.getFechaCierreGestionExterna()).isNotNull();
        }

        @Test
        @DisplayName("6C-10: fechaReingreso, motivoReingreso y observacionesReingreso no se tocan por PAGAPR")
        void camposReingresoNoTocados() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-010");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.getFechaReingreso()).isNull();
            assertThat(g.getMotivoReingreso()).isNull();
            assertThat(g.getObservacionesReingreso()).isNull();
        }
    }

    @Nested
    @DisplayName("MutacionActaPagoExterno")
    class MutacionActaPagoExterno {

        @Test
        @DisplayName("6C-11: resultadoFinal = CONDENA_FIRME_PAGADA siempre")
        void resultadoFinalCondenaFirmePagada() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-011");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
        }

        @Test
        @DisplayName("6C-12: Sin bloqueantes: situacionAdministrativa = CERRADA")
        void sinBloqueantesActaCerrada() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-012");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.CERRADA);
        }

        @Test
        @DisplayName("6C-13: Sin bloqueantes: bloqueActual = CERR")
        void sinBloqueantesBloqueCerr() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-013");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }

        @Test
        @DisplayName("6C-14: Con bloqueantes: situacionAdministrativa = ACTIVA")
        void conBloqueantesActaActiva() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-014");
            derivar(actaId);
            GestionExternaService svc = new GestionExternaService(
                    actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED),
                    id -> true, FaltasClockTestSupport.FIXED);
            svc.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("6C-15: Con bloqueantes: bloqueActual = ANAL")
        void conBloqueantesActaAnal() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-015");
            derivar(actaId);
            GestionExternaService svc = new GestionExternaService(
                    actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED),
                    id -> true, FaltasClockTestSupport.FIXED);
            svc.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("6C-16: Snapshot sin bloqueantes: CERRADAS / NINGUNA")
        void snapshotSinBloqueantes() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-016");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("6C-17: Snapshot con bloqueantes: PENDIENTE_ANALISIS / NINGUNA")
        void snapshotConBloqueantes() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-017");
            derivar(actaId);
            GestionExternaService svc = new GestionExternaService(
                    actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED),
                    id -> true, FaltasClockTestSupport.FIXED);
            svc.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }
    }

    @Nested
    @DisplayName("InteraccionFalPagoCondena")
    class InteraccionFalPagoCondena {

        @Test
        @DisplayName("6C-18: PAGAPR rechaza si existe FalPagoCondena CONFIRMADO")
        void rechazaSiPagoCondenaConfirmado() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-018");
            // informar y confirmar pago condena interno primero (no hay bloqueantes, cierra el acta)
            // Para este test necesitamos que exista un pago condena confirmado sin que el acta este cerrada.
            // Esto es una situacion imposible en el flujo normal (PCOCNF cierra).
            // Validamos la precondicion directamente: insertar pago condena confirmado a mano.
            pagoCondenaRepo.guardar(buildPagoCondenaConfirmado(actaId));
            // Ahora derivar (aunque acta no este en CONDENA_FIRME, vamos a probar la precondicion 7)
            // La derivacion tambien valida que no haya pago confirmado, asi que probamos la precondicion
            // del pago externo directamente.
            // Simular acta en GEXT manualmente
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setBloqueActual(BloqueActual.GEXT);
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            actaRepo.guardar(acta);
            ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna g =
                    new ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna(
                            9998L, actaId, FaltasClockTestSupport.FIXED.now(), "SYS");
            g.setTipoGestionExterna(ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna.APREMIO);
            g.setFechaDerivacion(FaltasClockTestSupport.FIXED.now());
            g.setMotivoDerivacion("test");
            gestionExternaRepo.guardar(g);

            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("pago de condena interno ya fue confirmado");
        }

        @Test
        @DisplayName("6C-19: PAGAPR procede si existe FalPagoCondena INFORMADO")
        void procedeSiPagoCondenaInformado() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-019");
            pagoCondenaRepo.guardar(buildPagoCondenaInformado(actaId));
            derivar(actaId);
            // Debe proceder sin excepcion
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
        }

        @Test
        @DisplayName("6C-20: Tras PAGAPR, FalPagoCondena INFORMADO sigue INFORMADO (no se modifica)")
        void pagoCondenaInformadoNoModificado() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-020");
            pagoCondenaRepo.guardar(buildPagoCondenaInformado(actaId));
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalPagoCondena pago = pagoCondenaRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);
        }

        @Test
        @DisplayName("6C-21: Despues de PAGAPR, InformarPagoCondena lanza PrecondicionVioladaException (resultadoFinal ya no es CONDENA_FIRME)")
        void despuesDePagaprConfirmarCondenaFalla() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-021");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            // resultadoFinal ya es CONDENA_FIRME_PAGADA, no CONDENA_FIRME
            // informar requiere CONDENA_FIRME, por lo que falla primero en informar
            assertThatThrownBy(() ->
                    pagoCondenaService.informar(new InformarPagoCondenaCommand(
                            actaId, new BigDecimal("5000.00"), "REF-TEST", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6C-22: No hay doble CIERRA: acta cerrada por PAGAPR rechaza operaciones adicionales")
        void noDoubleCierra() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-022");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            // Intentar derivar de nuevo falla porque el acta esta cerrada
            assertThatThrownBy(() ->
                    gestionExternaService.derivar(new DerivarGestionExternaCommand(
                            actaId, TipoGestionExterna.APREMIO, "reintento", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }
    }

    @Nested
    @DisplayName("PrecondicionesPagoExterno")
    class PrecondicionesPagoExterno {

        @Test
        @DisplayName("6C-23: Rechaza si acta no esta en EN_GESTION_EXTERNA")
        void rechazaActaNoEnGestionExterna() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-023");
            // Acta en ANAL, no derivada aun
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6C-24: Rechaza si bloqueActual != GEXT")
        void rechazaBloqueNoGext() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-024");
            // Forzar situacion EN_GESTION_EXTERNA pero bloque ANAL
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            acta.setBloqueActual(BloqueActual.ANAL);
            actaRepo.guardar(acta);
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("GEXT");
        }

        @Test
        @DisplayName("6C-25: Rechaza si no existe gestion externa activa")
        void rechazaSinGestionActiva() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-025");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            acta.setBloqueActual(BloqueActual.GEXT);
            actaRepo.guardar(acta);
            // Sin gestion externa activa en repo
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("gestion externa activa");
        }

        @Test
        @DisplayName("6C-26: Rechaza si resultadoFinal != CONDENA_FIRME")
        void rechazaResultadoNoCondenaFirme() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-026");
            derivar(actaId);
            // Forzar resultado a SIN_RESULTADO_FINAL
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            actaRepo.guardar(acta);
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA_FIRME");
        }

        @Test
        @DisplayName("6C-27: Rechaza segundo PAGAPR por inexistencia de gestion externa activa")
        void rechazaSegundoPagapr() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-027");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            // Segundo intento falla porque el acta esta cerrada
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6C-28: Rechaza acta cerrada")
        void rechazaActaCerrada() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-028");
            derivar(actaId);
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("6C-29: Rechaza acta anulada")
        void rechazaActaAnulada() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-029");
            derivar(actaId);
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulada");
        }

        @Test
        @DisplayName("6C-30: Rechaza acta archivada")
        void rechazaActaArchivada() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-030");
            derivar(actaId);
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("archivada");
        }
    }

    @Nested
    @DisplayName("GuardrailsSlice6C")
    class GuardrailsSlice6C {

        @Test
        @DisplayName("6C-31: PAGAPR existe en TipoEventoActa con codigo PAGAPR")
        void pagaprExisteEnEnum() {
            TipoEventoActa ev = TipoEventoActa.deCodigo("PAGAPR");
            assertThat(ev).isEqualTo(TipoEventoActa.PAGAPR);
            assertThat(ev.codigo()).isEqualTo("PAGAPR");
        }

        @Test
        @DisplayName("6C-32: CERRADA_EXTERNA existe en EstadoGestionExterna")
        void cerradaExternaExiste() {
            assertThat(EstadoGestionExterna.CERRADA_EXTERNA).isNotNull();
        }

        @Test
        @DisplayName("6C-33: PAGO_REGISTRADO existe en ResultadoGestionExterna")
        void pagoExternoInformadoExiste() {
            assertThat(ResultadoGestionExterna.PAGO_REGISTRADO).isNotNull();
        }

        @Test
        @DisplayName("6C-34: FalGestionExterna no tiene campo observacionesCierreGestionExterna")
        void noHayCampoObservacionesCierre() {
            // Verifica que no se creo la columna prohibida; se valida inspeccionando los metodos
            boolean tieneObs = java.util.Arrays.stream(FalGestionExterna.class.getMethods())
                    .anyMatch(m -> m.getName().contains("ObservacionesCierre"));
            assertThat(tieneObs).isFalse();
        }

        @Test
        @DisplayName("6C-35: FalGestionExterna no tiene campo motivoCierreGestionExterna")
        void noHayCampoMotivoCierre() {
            boolean tieneMotivo = java.util.Arrays.stream(FalGestionExterna.class.getMethods())
                    .anyMatch(m -> m.getName().contains("MotivoCierre"));
            assertThat(tieneMotivo).isFalse();
        }

        @Test
        @DisplayName("6C-36: EXTDER no se re-emite durante PAGAPR (exactamente un EXTDER por ciclo completo)")
        void extderNoSeReemiteDurantePagapr() {
            Long actaId = crearActaConCondenaFirme("PAG-EXT-036");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            long countExtder = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.EXTDER)
                    .count();
            assertThat(countExtder)
                    .as("EXTDER debe aparecer exactamente una vez (al derivar), no durante PAGAPR")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("6C-37: CONDENA_FIRME + PAGAPR + intento ConfirmarPagoCondena: falla, sin PCOCNF ni CIERRA extra")
        void pagoCondenaInformado_luegoPagapr_confirmarFalla_sinPcocnfNiCierraExtra() {
            // Arrange: acta con condena firme + pago condena INFORMADO preexistente
            Long actaId = crearActaConCondenaFirme("PAG-EXT-037");
            pagoCondenaRepo.guardar(buildPagoCondenaInformado(actaId));

            // Derive a gestion externa (emite EXTDER)
            derivar(actaId);

            // PAGAPR exitoso (sin bloqueantes): cierra gestion, emite PAGAPR + CIERRA
            // resultadoFinal pasa a CONDENA_FIRME_PAGADA
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));

            // Estado intermedio validado: acta cerrada, condena firme pagada
            FalActa actaPost = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(actaPost.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(actaPost.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);

            // Snapshot de eventos antes del intento de confirmar
            List<TipoEventoActa> tiposAntes = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tiposAntes).contains(TipoEventoActa.PAGAPR);
            assertThat(tiposAntes).contains(TipoEventoActa.CIERRA);
            long cierrasAntes = tiposAntes.stream().filter(t -> t == TipoEventoActa.CIERRA).count();

            // Accion: intentar ConfirmarPagoCondena → debe lanzar PrecondicionVioladaException
            // porque validarCondenaFirme falla: resultadoFinal es CONDENA_FIRME_PAGADA, no CONDENA_FIRME
            // El acta ya esta CERRADA por PAGAPR (sin bloqueantes).
            // cargarActaOperativa() la rechaza antes de llegar a validarCondenaFirme.
            // El mensaje correcto es "cerrada", no "CONDENA_FIRME" (esa seria la razon si el acta estuviera activa).
            assertThatThrownBy(() ->
                    pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");

            // Verificar invariantes post-intento fallido
            List<TipoEventoActa> tiposPost = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();

            // No se emitio PCOCNF
            assertThat(tiposPost).doesNotContain(TipoEventoActa.PCOCNF);

            // No se emitio un CIERRA adicional
            long cierrasPost = tiposPost.stream().filter(t -> t == TipoEventoActa.CIERRA).count();
            assertThat(cierrasPost)
                    .as("No debe emitirse un CIERRA adicional tras el intento fallido de confirmar pago condena")
                    .isEqualTo(cierrasAntes);

            // El pago condena interno sigue INFORMADO (no se modifico)
            FalPagoCondena pago = pagoCondenaRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(pago.getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.INFORMADO);
        }
    }

    // =========================================================================
    // Helpers para tests de FalPagoCondena (Slice 6C)
    // =========================================================================

    private ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena buildPagoCondenaInformado(Long actaId) {
        ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena p =
                new ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena(
                        java.util.UUID.randomUUID().toString(), actaId);
        p.setEstadoPagoCondena(ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena.INFORMADO);
        p.setMonto(new BigDecimal("5000.00"));
        p.setReferenciaPago("REF-EXT-TEST");
        return p;
    }

    private ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena buildPagoCondenaConfirmado(Long actaId) {
        ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena p =
                new ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena(
                        java.util.UUID.randomUUID().toString(), actaId);
        p.setEstadoPagoCondena(ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena.CONFIRMADO);
        p.setMonto(new BigDecimal("5000.00"));
        p.setReferenciaPago("REF-EXT-TEST");
        return p;
    }

    // =========================================================================
    // Guardrails Slice 6D-0: catalogo productivo gestion externa
    // =========================================================================

    @Nested
    @DisplayName("GuardrailsCatalogos6D0")
    class GuardrailsCatalogos6D0 {

        @Test
        @DisplayName("6D0-01: ResultadoGestionExterna contiene exactamente los valores productivos")
        void resultadoGestionExternaValoresProductivos() {
            java.util.Set<String> esperados = java.util.Set.of(
                    "SIN_RESULTADO", "SIN_CAMBIOS", "PAGO_REGISTRADO",
                    "SIN_PAGO", "ABSUELVE", "CONFIRMA_CONDENA", "MODIFICA_MONTO");
            java.util.Set<String> actuales = java.util.Arrays.stream(ResultadoGestionExterna.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(actuales).containsExactlyInAnyOrderElementsOf(esperados);
        }

        @Test
        @DisplayName("6D0-02: ModoReingresoGestionExterna contiene exactamente los valores productivos")
        void modoReingresoGestionExternaValoresProductivos() {
            java.util.Set<String> esperados = java.util.Set.of(
                    "REINGRESO_CON_PAGO", "REINGRESO_SIN_PAGO", "REINGRESO_CON_DICTAMEN",
                    "REINGRESO_PARA_NUEVO_FALLO", "REINGRESO_PARA_CIERRE", "REINGRESO_PARA_REVISION");
            java.util.Set<String> actuales = java.util.Arrays.stream(ModoReingresoGestionExterna.values())
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(actuales).containsExactlyInAnyOrderElementsOf(esperados);
        }

        @Test
        @DisplayName("6D0-03: PAGAPR asigna resultadoGestionExterna = PAGO_REGISTRADO")
        void pagaprUsaPagoRegistrado() {
            Long actaId = crearActaConCondenaFirme("6D0-003");
            derivar(actaId);
            gestionExternaService.registrarPagoExternoGestion(cmdPagoExterno(actaId, null));
            FalGestionExterna g = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(g.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.PAGO_REGISTRADO);
        }

        @Test
        @DisplayName("6D0-04: PAGO_EXTERNO_INFORMADO no existe en ResultadoGestionExterna")
        void pagoExternoInformadoNoExiste() {
            boolean existe = java.util.Arrays.stream(ResultadoGestionExterna.values())
                    .anyMatch(v -> v.name().equals("PAGO_EXTERNO_INFORMADO"));
            assertThat(existe).isFalse();
        }

        @Test
        @DisplayName("6D0-05: REINGRESAR_A_ANALISIS no existe en ModoReingresoGestionExterna")
        void reingresoAAnalisisNoExiste() {
            boolean existe = java.util.Arrays.stream(ModoReingresoGestionExterna.values())
                    .anyMatch(v -> v.name().equals("REINGRESAR_A_ANALISIS"));
            assertThat(existe).isFalse();
        }

        @Test
        @DisplayName("6D0-06: REINGRESAR_A_PAGO_CONDENA no existe en ModoReingresoGestionExterna")
        void reingresoAPagoCondenaNoExiste() {
            boolean existe = java.util.Arrays.stream(ModoReingresoGestionExterna.values())
                    .anyMatch(v -> v.name().equals("REINGRESAR_A_PAGO_CONDENA"));
            assertThat(existe).isFalse();
        }

        @Test
        @DisplayName("6D0-07: REINGRESO_PARA_CIERRE existe en enum pero sigue bloqueado para reingreso")
        void reingresParaCierreExistePeroBloqueado() {
            assertThat(ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE).isNotNull();
            Long actaId = crearActaConCondenaFirme("6D0-007");
            derivar(actaId);
            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_CIERRE");
        }

        @Test
        @DisplayName("6D0-08: REINGRESO_PARA_NUEVO_FALLO sin resultado explicito falla (Slice 6D-2: requiere ABSUELVE)")
        void reingresoParaNuevoFalloExistePeroBloqueado() {
            assertThat(ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO).isNotNull();
            Long actaId = crearActaConCondenaFirme("6D0-008");
            derivar(actaId);
            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_NUEVO_FALLO");
        }

        @Test
        @DisplayName("6D0-09: modoReingresoGestionExterna es null tras EXTDER (no NO_APLICA)")
        void modoReingresoNullTrasDerivar() {
            Long actaId = crearActaConCondenaFirme("6D0-009");
            derivar(actaId);
            FalGestionExterna g = gestionExternaRepo.buscarActiva(actaId).orElseThrow();
            assertThat(g.getModoReingresoGestionExterna()).isNull();
        }
    }

    // =========================================================================
    // Slice 6D-1: Reingreso sin pago (SIN_PAGO) y sin cambios (SIN_CAMBIOS)
    // =========================================================================

    @Nested
    @DisplayName("Slice 6D-1: Reingreso SIN_PAGO y SIN_CAMBIOS con validacion de pares")
    class Slice6D1 {

        private void reingresar6D1(Long actaId, ModoReingresoGestionExterna modo, ResultadoGestionExterna resultado) {
            gestionExternaService.reingresar(new ReingresarDesdeGestionExternaCommand(
                    actaId, modo, "Motivo 6D1 test", resultado, null, null));
        }

        @Test
        @DisplayName("6D1-01: SIN_PAGO + REINGRESO_SIN_PAGO reingresa por EXTRET")
        void sinPago_reingresaPorExtret() {
            Long actaId = crearActaConCondenaFirme("6D1-001");
            derivarActa(actaId);

            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_PAGO);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.EXTRET);
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
            assertThat(tipos).doesNotContain(TipoEventoActa.PCOCNF);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.isSiActiva()).isFalse();
            assertThat(gestion.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.REINGRESADA);
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.SIN_PAGO);
            assertThat(gestion.getModoReingresoGestionExterna()).isEqualTo(ModoReingresoGestionExterna.REINGRESO_SIN_PAGO);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("6D1-02: SIN_CAMBIOS + REINGRESO_PARA_REVISION reingresa por EXTRET")
        void sinCambios_reingresaPorExtret() {
            Long actaId = crearActaConCondenaFirme("6D1-002");
            derivarActa(actaId);

            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_CAMBIOS);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.EXTRET);
            assertThat(tipos).doesNotContain(TipoEventoActa.PAGAPR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
            assertThat(tipos).doesNotContain(TipoEventoActa.PCOCNF);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.isSiActiva()).isFalse();
            assertThat(gestion.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.REINGRESADA);
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.SIN_CAMBIOS);
            assertThat(gestion.getModoReingresoGestionExterna()).isEqualTo(ModoReingresoGestionExterna.REINGRESO_PARA_REVISION);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("6D1-03: SIN_PAGO no toca campos PAGAPR (fechaCierre null, no CERRADA_EXTERNA, no CONDENA_FIRME_PAGADA)")
        void sinPago_noTocaCamposPagapr() {
            Long actaId = crearActaConCondenaFirme("6D1-003");
            derivarActa(actaId);

            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_PAGO);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getFechaCierreGestionExterna()).isNull();
            assertThat(gestion.getEstadoGestionExterna()).isNotEqualTo(EstadoGestionExterna.CERRADA_EXTERNA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
        }

        @Test
        @DisplayName("6D1-04: SIN_CAMBIOS no toca campos PAGAPR (fechaCierre null, no CERRADA_EXTERNA, no CONDENA_FIRME_PAGADA)")
        void sinCambios_noTocaCamposPagapr() {
            Long actaId = crearActaConCondenaFirme("6D1-004");
            derivarActa(actaId);

            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_CAMBIOS);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getFechaCierreGestionExterna()).isNull();
            assertThat(gestion.getEstadoGestionExterna()).isNotEqualTo(EstadoGestionExterna.CERRADA_EXTERNA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
        }

        @Test
        @DisplayName("6D1-05: Despues de SIN_PAGO no se puede PAGAPR en mismo ciclo")
        void despuesDeSinPago_noPuedeHacersePagapr() {
            Long actaId = crearActaConCondenaFirme("6D1-005");
            derivarActa(actaId);
            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_PAGO);

            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(
                            new RegistrarPagoExternoGestionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6D1-06: Despues de SIN_CAMBIOS no se puede PAGAPR en mismo ciclo")
        void despuesDeSinCambios_noPuedeHacersePagapr() {
            Long actaId = crearActaConCondenaFirme("6D1-006");
            derivarActa(actaId);
            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_CAMBIOS);

            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(
                            new RegistrarPagoExternoGestionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6D1-07: SIN_PAGO permite seguir circuito interno de pago condena")
        void sinPago_permiteCirculoInternoPagoCondena() {
            Long actaId = crearActaConCondenaFirme("6D1-007");
            derivarActa(actaId);
            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_PAGO);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new java.math.BigDecimal("5000.00"), "REF-6D1-07", null));

            FalActa actaDespues = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(actaDespues.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        }

        @Test
        @DisplayName("6D1-08: SIN_CAMBIOS con REINGRESO_PARA_REVISION deja snapshot en PENDIENTE_ANALISIS")
        void sinCambios_dejaSnapshotCoherenteEnAnalisis() {
            Long actaId = crearActaConCondenaFirme("6D1-008");
            derivarActa(actaId);
            reingresar6D1(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_CAMBIOS);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
        }

        @Test
        @DisplayName("6D1-09: Rechaza SIN_PAGO si acta no esta en GEXT (no derivada)")
        void rechaza_sinPago_actaNoEnGext() {
            Long actaId = crearActaConCondenaFirme("6D1-009");

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_PAGO))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6D1-10: Rechaza SIN_CAMBIOS si no hay gestion externa activa")
        void rechaza_sinCambios_sinGestionActiva() {
            Long actaId = crearActaConCondenaFirme("6D1-010");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setBloqueActual(BloqueActual.GEXT);
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_CAMBIOS))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("No existe gestion externa activa");
        }

        @Test
        @DisplayName("6D1-11: Rechaza par incoherente SIN_PAGO + REINGRESO_PARA_REVISION")
        void rechaza_parIncoherente_sinPago_revision() {
            Long actaId = crearActaConCondenaFirme("6D1-011");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION, ResultadoGestionExterna.SIN_PAGO))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("SIN_PAGO");
        }

        @Test
        @DisplayName("6D1-12: Rechaza par incoherente SIN_CAMBIOS + REINGRESO_SIN_PAGO")
        void rechaza_parIncoherente_sinCambios_sinPago() {
            Long actaId = crearActaConCondenaFirme("6D1-012");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.SIN_CAMBIOS))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("SIN_CAMBIOS");
        }

        @Test
        @DisplayName("6D1-13: Rechaza resultado reservado ABSUELVE (requiere REINGRESO_CON_DICTAMEN, slice futuro)")
        void rechaza_resultadoReservado_absuelve() {
            Long actaId = crearActaConCondenaFirme("6D1-013");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_SIN_PAGO, ResultadoGestionExterna.ABSUELVE))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ABSUELVE");
        }

        @Test
        @DisplayName("6D1-14: Rechaza modo reservado REINGRESO_PARA_NUEVO_FALLO")
        void rechaza_modoReservado_nuevoFallo() {
            Long actaId = crearActaConCondenaFirme("6D1-014");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresar6D1(
                    actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO, ResultadoGestionExterna.SIN_CAMBIOS))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_NUEVO_FALLO");
        }

        @Test
        @DisplayName("6D1-15: Guardrail - EXTRET es el unico evento de reingreso; prohibidos intactos; modos reservados bloqueados")
        void guardrail_eventoYModosFinalIntegridad() {
            assertThat(TipoEventoActa.EXTRET.codigo()).isEqualTo("EXTRET");

            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGCON"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("ACTCER"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("APELAC"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("DRVEXT"))
                    .isInstanceOf(IllegalArgumentException.class);

            Long actaId = crearActaConCondenaFirme("6D1-015");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_CIERRE");

            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_NUEVO_FALLO");

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
        }
    }

    // =========================================================================
    // Slice 6D-2: Reingreso con dictamen externo
    // =========================================================================

    @Nested
    @DisplayName("Slice 6D-2: Reingreso con dictamen externo (ABSUELVE, CONFIRMA_CONDENA, MODIFICA_MONTO)")
    class Slice6D2 {

        private static final java.math.BigDecimal MONTO_VALIDO = new java.math.BigDecimal("8000.00");

        private void reingresarConDictamen(Long actaId,
                ModoReingresoGestionExterna modo,
                ResultadoGestionExterna resultado,
                java.math.BigDecimal monto) {
            gestionExternaService.reingresar(new ReingresarDesdeGestionExternaCommand(
                    actaId, modo, "Motivo dictamen externo", resultado, null, monto));
        }

        @Test
        @DisplayName("6D2-01: ABSUELVE + REINGRESO_PARA_NUEVO_FALLO reingresa por EXTRET para nuevo fallo")
        void absuelve_reingresaPorExtretParaNuevoFallo() {
            Long actaId = crearActaConCondenaFirme("6D2-001");
            derivarActa(actaId);

            reingresarConDictamen(actaId,
                    ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                    ResultadoGestionExterna.ABSUELVE, null);

            // Verifica EXTRET emitido
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.EXTRET);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.PAGAPR);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.CIERRA);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.ABSUELVE);
            assertThat(gestion.getModoReingresoGestionExterna()).isEqualTo(ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO);
            assertThat(gestion.isSiActiva()).isFalse();
            assertThat(gestion.getEstadoGestionExterna()).isEqualTo(EstadoGestionExterna.REINGRESADA);

            // Acta vuelve a ANAL / ACTIVA; resultadoFinal NO cambia automaticamente
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        }

        @Test
        @DisplayName("6D2-02: CONFIRMA_CONDENA + REINGRESO_CON_DICTAMEN reingresa por EXTRET con dictamen")
        void confirmacionCondena_reingresaPorExtretConDictamen() {
            Long actaId = crearActaConCondenaFirme("6D2-002");
            derivarActa(actaId);

            reingresarConDictamen(actaId,
                    ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.CONFIRMA_CONDENA, null);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.EXTRET);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.CIERRA);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.PAGAPR);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.CONFIRMA_CONDENA);
            assertThat(gestion.getModoReingresoGestionExterna()).isEqualTo(ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN);
            assertThat(gestion.isSiActiva()).isFalse();

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("6D2-03: MODIFICA_MONTO + REINGRESO_CON_DICTAMEN registra monto y vuelve a analisis")
        void modificaMonto_reingresaConMontoYVuelveAAnalisis() {
            Long actaId = crearActaConCondenaFirme("6D2-003");
            derivarActa(actaId);

            reingresarConDictamen(actaId,
                    ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.MODIFICA_MONTO, MONTO_VALIDO);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.EXTRET);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.CIERRA);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.PAGAPR);

            FalGestionExterna gestion = gestionExternaRepo.buscarPorHistorico(actaId).orElseThrow();
            assertThat(gestion.getResultadoGestionExterna()).isEqualTo(ResultadoGestionExterna.MODIFICA_MONTO);
            assertThat(gestion.getModoReingresoGestionExterna()).isEqualTo(ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN);
            assertThat(gestion.getMontoResultado()).isEqualByComparingTo(MONTO_VALIDO);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("6D2-04: MODIFICA_MONTO rechaza monto null")
        void modificaMonto_rechazaMontoNull() {
            Long actaId = crearActaConCondenaFirme("6D2-004");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId, ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.MODIFICA_MONTO, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("montoResultado");
        }

        @Test
        @DisplayName("6D2-05: MODIFICA_MONTO rechaza monto cero o negativo")
        void modificaMonto_rechazaMontoCeroONegativo() {
            Long actaId = crearActaConCondenaFirme("6D2-005a");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId, ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.MODIFICA_MONTO, java.math.BigDecimal.ZERO))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mayor a cero");

            Long actaId2 = crearActaConCondenaFirme("6D2-005b");
            derivarActa(actaId2);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId2, ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.MODIFICA_MONTO, new java.math.BigDecimal("-100.00")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("mayor a cero");
        }

        @Test
        @DisplayName("6D2-06: ABSUELVE rechaza modo REINGRESO_CON_DICTAMEN")
        void absuelve_rechazaModoReingresoDictamen() {
            Long actaId = crearActaConCondenaFirme("6D2-006");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId, ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.ABSUELVE, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ABSUELVE");
        }

        @Test
        @DisplayName("6D2-07: CONFIRMA_CONDENA rechaza modo REINGRESO_PARA_NUEVO_FALLO")
        void confirmaCondena_rechazaModoNuevoFallo() {
            Long actaId = crearActaConCondenaFirme("6D2-007");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                    ResultadoGestionExterna.CONFIRMA_CONDENA, null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONFIRMA_CONDENA");
        }

        @Test
        @DisplayName("6D2-08: MODIFICA_MONTO rechaza modo REINGRESO_PARA_NUEVO_FALLO")
        void modificaMonto_rechazaModoNuevoFallo() {
            Long actaId = crearActaConCondenaFirme("6D2-008");
            derivarActa(actaId);

            assertThatThrownBy(() -> reingresarConDictamen(
                    actaId, ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                    ResultadoGestionExterna.MODIFICA_MONTO, MONTO_VALIDO))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("MODIFICA_MONTO");
        }

        @Test
        @DisplayName("6D2-09: REINGRESO_PARA_CIERRE sigue bloqueado")
        void reingresoCierre_sigueBloqueado() {
            Long actaId = crearActaConCondenaFirme("6D2-009");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_CIERRE");
        }

        @Test
        @DisplayName("6D2-10: Post dictamen ABSUELVE no permite PAGAPR en mismo ciclo")
        void postAbsuelve_noPermitePagapr() {
            Long actaId = crearActaConCondenaFirme("6D2-010");
            derivarActa(actaId);
            reingresarConDictamen(actaId,
                    ModoReingresoGestionExterna.REINGRESO_PARA_NUEVO_FALLO,
                    ResultadoGestionExterna.ABSUELVE, null);

            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(
                            new RegistrarPagoExternoGestionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6D2-11: Post MODIFICA_MONTO no pasa directo a pago ni cierre")
        void postModificaMonto_noPasaAPagoNiCierre() {
            Long actaId = crearActaConCondenaFirme("6D2-011");
            derivarActa(actaId);
            reingresarConDictamen(actaId,
                    ModoReingresoGestionExterna.REINGRESO_CON_DICTAMEN,
                    ResultadoGestionExterna.MODIFICA_MONTO, MONTO_VALIDO);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.CIERRA);
            assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.PAGAPR);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);

            // PAGAPR tampoco esta disponible (gestion externa ya no esta activa)
            assertThatThrownBy(() ->
                    gestionExternaService.registrarPagoExternoGestion(
                            new RegistrarPagoExternoGestionCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("6D2-12: PAGO_REGISTRADO sigue siendo exclusivo de PAGAPR")
        void pagoRegistrado_sigueExclusivoDePagapr() {
            Long actaId = crearActaConCondenaFirme("6D2-012");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                            "Motivo", ResultadoGestionExterna.PAGO_REGISTRADO, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PAGO_REGISTRADO");
        }

        @Test
        @DisplayName("6D2-13: SIN_RESULTADO no es aceptado como resultado de reingreso")
        void sinResultado_noEsAceptadoComoResultado() {
            Long actaId = crearActaConCondenaFirme("6D2-013");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                            "Motivo", ResultadoGestionExterna.SIN_RESULTADO, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("SIN_RESULTADO");
        }

        @Test
        @DisplayName("6D2-14: Guardrail integral - no nuevos eventos, no prohibidos, REINGRESO_PARA_CIERRE bloqueado")
        void guardrail_integral() {
            // No se crean eventos de dominio nuevos
            assertThat(TipoEventoActa.EXTRET.codigo()).isEqualTo("EXTRET");

            // Eventos prohibidos siguen sin existir
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGCON"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("ACTCER"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("APELAC"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("DRVEXT"))
                    .isInstanceOf(IllegalArgumentException.class);

            // REINGRESO_PARA_CIERRE sigue bloqueado
            Long actaId = crearActaConCondenaFirme("6D2-014");
            derivarActa(actaId);
            assertThatThrownBy(() -> reingresar(actaId, ModoReingresoGestionExterna.REINGRESO_PARA_CIERRE))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_PARA_CIERRE");

            // PAGO_REGISTRADO sigue exclusivo de PAGAPR
            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                            "Motivo", ResultadoGestionExterna.PAGO_REGISTRADO, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PAGO_REGISTRADO");

            // SIN_RESULTADO no es resultado valido de reingreso
            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_PARA_REVISION,
                            "Motivo", ResultadoGestionExterna.SIN_RESULTADO, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("SIN_RESULTADO");

            // No aparecen D3_DOCUMENTAL, PAGCON, ACTCER, APELAC, DRVEXT como dominios validos
            assertThat(java.util.Arrays.stream(ar.gob.malvinas.faltas.core.domain.enums.BloqueActual.values())
                    .map(Enum::name).toList())
                    .doesNotContain("D3_DOCUMENTAL", "PAGCON", "ACTCER", "APELAC", "DRVEXT");
        }

        @Test
        @DisplayName("6D2-15: REINGRESO_CON_PAGO sigue bloqueado fuera de PAGAPR")
        void reingreso_con_pago_bloqueado() {
            Long actaId = crearActaConCondenaFirme("6D2-015");
            derivarActa(actaId);

            assertThatThrownBy(() -> gestionExternaService.reingresar(
                    new ReingresarDesdeGestionExternaCommand(
                            actaId, ModoReingresoGestionExterna.REINGRESO_CON_PAGO,
                            "Motivo valido", null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("REINGRESO_CON_PAGO");
        }
    }}
