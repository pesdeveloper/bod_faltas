package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.DerivarGestionExternaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarPagoExternoGestionCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.GestionExternaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
import ar.gob.malvinas.faltas.core.application.service.RepositoryBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoGestionExterna;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
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
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryBloqueanteMaterialRepository;
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
import ar.gob.malvinas.faltas.core.application.command.AnularBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.application.command.CumplirBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.application.service.BloqueanteMaterialService;
import ar.gob.malvinas.faltas.core.application.service.CierreActaHelper;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

/**
 * Tests del Slice 7A: Motor real de bloqueantes materiales.
 *
 * Usa RepositoryBloqueantesMaterialesChecker + InMemoryBloqueanteMaterialRepository
 * para verificar que la presencia de bloqueantes activos impide el cierre del acta.
 *
 * Caminos cubiertos: PCOCNF (pago condena) y PAGAPR (pago externo gestion externa).
 * Los tests sin bloqueantes verifican que CIERRA se emite cuando el repo esta vacio.
 * Los tests con bloqueantes verifican que CIERRA NO se emite cuando hay un bloqueante activo.
 * El test de bloqueante inactivo verifica que siActivo=false no impide el cierre.
 */
@DisplayName("Slice 7A: Motor real de bloqueantes materiales")
class BloqueantesMaterialesTest {

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
    private BloqueanteMaterialRepository bloqueanteMaterialRepo;

    private RepositoryBloqueantesMaterialesChecker bloqueantesChecker;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private FirmezaCondenaService firmezaService;
    private GestionExternaService gestionExternaService;
    private PagoCondenaService pagoCondenaService;
    private BloqueanteMaterialService bloqueanteMaterialService;

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
        bloqueanteMaterialRepo = new InMemoryBloqueanteMaterialRepository();

        bloqueantesChecker = new RepositoryBloqueantesMaterialesChecker(bloqueanteMaterialRepo);

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
                        FaltasClockTestSupport.FIXED);
        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, FaltasClockTestSupport.FIXED);
        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, firmezaRepo, recalc, FaltasClockTestSupport.FIXED);
        gestionExternaService = new GestionExternaService(
                actaRepo, eventoRepo, snapshotRepo, pagoCondenaRepo, gestionExternaRepo, recalc,
                bloqueantesChecker, FaltasClockTestSupport.FIXED);
        CierreActaHelper cierreActaHelper = new CierreActaHelper(actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED);
        bloqueanteMaterialService = new BloqueanteMaterialService(bloqueanteMaterialRepo, actaRepo, cierreActaHelper, FaltasClockTestSupport.FIXED);
        pagoCondenaService = new PagoCondenaService(
                actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondenaRepo, recalc,
                bloqueantesChecker, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotif, null));

        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("3000.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, "CORREO", null))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(idNotifFallo, null));
        firmezaService.vencerPlazoApelacion(new VencerPlazoApelacionCommand(actaId, null));
        return actaId;
    }

    private Long crearActaEnGestionExternaConCondenaFirme(String docNum) {
        Long actaId = crearActaConCondenaFirme(docNum);
        gestionExternaService.derivar(new DerivarGestionExternaCommand(
                actaId, TipoGestionExterna.APREMIO, "Derivacion para cobro", null));
        return actaId;
    }

    private void registrarBloqueante(Long actaId) {
        FalBloqueanteMaterial b = new FalBloqueanteMaterial(bloqueanteMaterialRepo.nextId(), actaId, FaltasClockTestSupport.FIXED.now());
        b.setOrigen(OrigenBloqueanteMaterial.RODADO);
        b.setDescripcion("Rodado retenido en deposito");
        bloqueanteMaterialRepo.guardar(b);
    }

    private void registrarBloqueanteCumplido(Long actaId) {
        FalBloqueanteMaterial b = new FalBloqueanteMaterial(bloqueanteMaterialRepo.nextId(), actaId, FaltasClockTestSupport.FIXED.now());
        b.setOrigen(OrigenBloqueanteMaterial.RODADO);
        b.setEstado(EstadoBloqueanteMaterial.CUMPLIDO);
        b.setSiActivo(false);
        b.setDescripcion("Rodado devuelto al titular");
        bloqueanteMaterialRepo.guardar(b);
    }

    // =========================================================================
    // Tests Slice 7A
    // =========================================================================

    @Nested
    @DisplayName("PagoCondena: confirmar con motor real de bloqueantes")
    class PagoCondenaConBloqueantesReales {

        @Test
        @DisplayName("Test 7A-1: Pago condena confirmado sin bloqueantes: emite PCOCNF y CIERRA, acta CERRADA")
        void pago_condena_sin_bloqueantes_cierra() {
            Long actaId = crearActaConCondenaFirme("7A00001");
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7A-001", null));

            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);
            assertThat(tipos.indexOf(TipoEventoActa.PCOCNF)).isLessThan(tipos.indexOf(TipoEventoActa.CIERRA));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Test 7A-2: Pago condena confirmado con bloqueante activo: emite PCOCNF, no emite CIERRA, acta ACTIVA/ANAL")
        void pago_condena_con_bloqueante_activo_no_cierra() {
            Long actaId = crearActaConCondenaFirme("7A00002");
            registrarBloqueante(actaId);

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7A-002", null));

            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Test 7A-5: Bloqueante inactivo (siActivo=false) no impide cierre por PCOCNF")
        void bloqueante_inactivo_no_impide_cierre_pcocnf() {
            Long actaId = crearActaConCondenaFirme("7A00005");
            registrarBloqueanteCumplido(actaId);

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7A-005", null));

            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }
    }

    @Nested
    @DisplayName("PagoExterno (PAGAPR): con motor real de bloqueantes")
    class PagoExternoConBloqueantesReales {

        @Test
        @DisplayName("Test 7A-3: Pago externo PAGAPR sin bloqueantes: emite PAGAPR y CIERRA, acta CERRADA")
        void pagapr_sin_bloqueantes_cierra() {
            Long actaId = crearActaEnGestionExternaConCondenaFirme("7A00003");

            gestionExternaService.registrarPagoExternoGestion(
                    new RegistrarPagoExternoGestionCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PAGAPR);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);
            assertThat(tipos.indexOf(TipoEventoActa.PAGAPR)).isLessThan(tipos.indexOf(TipoEventoActa.CIERRA));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Test 7A-4: Pago externo PAGAPR con bloqueante activo: emite PAGAPR, no emite CIERRA, acta ACTIVA/ANAL")
        void pagapr_con_bloqueante_activo_no_cierra() {
            Long actaId = crearActaEnGestionExternaConCondenaFirme("7A00004");
            registrarBloqueante(actaId);

            gestionExternaService.registrarPagoExternoGestion(
                    new RegistrarPagoExternoGestionCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PAGAPR);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }
    }

    @Nested
    @DisplayName("Guardrails Slice 7A")
    class GuardrailsSlice7A {

        @Test
        @DisplayName("Test 7A-6: No se agregan eventos prohibidos en ningun camino del Slice 7A")
        void sin_eventos_prohibidos() {
            Long actaIdPcocnf = crearActaConCondenaFirme("7A00006a");
            registrarBloqueante(actaIdPcocnf);
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaIdPcocnf, new BigDecimal("3000.00"), "REF-7A-006", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaIdPcocnf, null));

            Long actaIdPagapr = crearActaEnGestionExternaConCondenaFirme("7A00006b");
            registrarBloqueante(actaIdPagapr);
            gestionExternaService.registrarPagoExternoGestion(
                    new RegistrarPagoExternoGestionCommand(actaIdPagapr, null));

            List<String> codigos1 = eventoRepo.buscarPorActa(actaIdPcocnf)
                    .stream().map(e -> e.tipoEvt().codigo()).toList();
            List<String> codigos2 = eventoRepo.buscarPorActa(actaIdPagapr)
                    .stream().map(e -> e.tipoEvt().codigo()).toList();

            for (List<String> codigos : List.of(codigos1, codigos2)) {
                assertThat(codigos).doesNotContain("PAGCON");
                assertThat(codigos).doesNotContain("ACTCER");
                assertThat(codigos).doesNotContain("APELAC");
                assertThat(codigos).doesNotContain("DRVEXT");
            }
        }
    }

    // =========================================================================
    // Tests Slice 7B: gestion minima de bloqueantes materiales
    // =========================================================================

    @Nested
    @DisplayName("Slice 7B: Registrar bloqueante material")
    class RegistrarBloqueante {

        @Test
        @DisplayName("Test 7B-01: Registrar bloqueante → PENDIENTE, siActivo=true, fechaAlta not null, fechaCierre null, existsActivo=true")
        void registrar_bloqueante_estado_inicial() {
            Long actaId = 7001L;
            RegistrarBloqueanteMaterialCommand cmd = new RegistrarBloqueanteMaterialCommand(
                    actaId, OrigenBloqueanteMaterial.RODADO);

            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(cmd);

            assertThat(b.getEstado()).isEqualTo(EstadoBloqueanteMaterial.PENDIENTE);
            assertThat(b.isSiActivo()).isTrue();
            assertThat(b.getFechaAlta()).isNotNull();
            assertThat(b.getFechaCierre()).isNull();
            assertThat(b.getOrigen()).isEqualTo(OrigenBloqueanteMaterial.RODADO);
            assertThat(bloqueanteMaterialRepo.existsActivoByActaId(actaId)).isTrue();
        }

        @Test
        @DisplayName("Test 7B-02: Registrar sin actaId lanza PrecondicionVioladaException")
        void registrar_sin_actaId_falla() {
            assertThatThrownBy(() -> bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(null, OrigenBloqueanteMaterial.RODADO)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Test 7B-03: Registrar sin origen lanza PrecondicionVioladaException")
        void registrar_sin_origen_falla() {
            assertThatThrownBy(() -> bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(7003L, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Slice 7B: Cumplir bloqueante material")
    class CumplirBloqueante {

        @Test
        @DisplayName("Test 7B-04: Cumplir bloqueante → CUMPLIDO, siActivo=false, fechaCierre not null, existsActivo=false")
        void cumplir_bloqueante_estado_cumplido() {
            Long actaId = 7004L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));

            FalBloqueanteMaterial resultado = bloqueanteMaterialService.cumplir(
                    new CumplirBloqueanteMaterialCommand(b.getId()));

            assertThat(resultado.getEstado()).isEqualTo(EstadoBloqueanteMaterial.CUMPLIDO);
            assertThat(resultado.isSiActivo()).isFalse();
            assertThat(resultado.getFechaCierre()).isNotNull();
            assertThat(bloqueanteMaterialRepo.existsActivoByActaId(actaId)).isFalse();
        }

        @Test
        @DisplayName("Test 7B-05: Cumplir bloqueante ya CUMPLIDO es idempotente")
        void cumplir_bloqueante_ya_cumplido_idempotente() {
            Long actaId = 7005L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b.getId()));

            FalBloqueanteMaterial resultado = bloqueanteMaterialService.cumplir(
                    new CumplirBloqueanteMaterialCommand(b.getId()));

            assertThat(resultado.getEstado()).isEqualTo(EstadoBloqueanteMaterial.CUMPLIDO);
            assertThat(resultado.isSiActivo()).isFalse();
        }

        @Test
        @DisplayName("Test 7B-06: No se puede cumplir un bloqueante ANULADO")
        void cumplir_bloqueante_anulado_falla() {
            Long actaId = 7006L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(b.getId()));

            assertThatThrownBy(() -> bloqueanteMaterialService.cumplir(
                    new CumplirBloqueanteMaterialCommand(b.getId())))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Slice 7B: Anular bloqueante material")
    class AnularBloqueante {

        @Test
        @DisplayName("Test 7B-07: Anular bloqueante → ANULADO, siActivo=false, fechaCierre not null, existsActivo=false")
        void anular_bloqueante_estado_anulado() {
            Long actaId = 7007L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.DOCUMENTACION_RETENIDA));

            FalBloqueanteMaterial resultado = bloqueanteMaterialService.anular(
                    new AnularBloqueanteMaterialCommand(b.getId()));

            assertThat(resultado.getEstado()).isEqualTo(EstadoBloqueanteMaterial.ANULADO);
            assertThat(resultado.isSiActivo()).isFalse();
            assertThat(resultado.getFechaCierre()).isNotNull();
            assertThat(bloqueanteMaterialRepo.existsActivoByActaId(actaId)).isFalse();
        }

        @Test
        @DisplayName("Test 7B-08: Anular bloqueante ya ANULADO es idempotente")
        void anular_bloqueante_ya_anulado_idempotente() {
            Long actaId = 7008L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.OTRO));
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(b.getId()));

            FalBloqueanteMaterial resultado = bloqueanteMaterialService.anular(
                    new AnularBloqueanteMaterialCommand(b.getId()));

            assertThat(resultado.getEstado()).isEqualTo(EstadoBloqueanteMaterial.ANULADO);
            assertThat(resultado.isSiActivo()).isFalse();
        }

        @Test
        @DisplayName("Test 7B-09: No se puede anular un bloqueante CUMPLIDO")
        void anular_bloqueante_cumplido_falla() {
            Long actaId = 7009L;
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b.getId()));

            assertThatThrownBy(() -> bloqueanteMaterialService.anular(
                    new AnularBloqueanteMaterialCommand(b.getId())))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Slice 7B: Bloqueante cumplido o anulado no impide cierre")
    class CierreConBloqueantesResueltos {

        @Test
        @DisplayName("Test 7B-10: Bloqueante cumplido no impide cierre por PCOCNF")
        void bloqueante_cumplido_no_impide_cierre_pcocnf() {
            Long actaId = crearActaConCondenaFirme("7B00010");

            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b.getId()));

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new java.math.BigDecimal("2000.00"), "REF-7B-010", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }

        @Test
        @DisplayName("Test 7B-11: Bloqueante anulado no impide cierre por PAGAPR")
        void bloqueante_anulado_no_impide_cierre_pagapr() {
            Long actaId = crearActaEnGestionExternaConCondenaFirme("7B00011");

            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.DOCUMENTACION_RETENIDA));
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(b.getId()));

            gestionExternaService.registrarPagoExternoGestion(
                    new RegistrarPagoExternoGestionCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PAGAPR);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }

        @Test
        @DisplayName("Test 7B-12: Bloqueante activo si impide cierre por PCOCNF (refuerzo 7A-2)")
        void bloqueante_activo_impide_cierre_pcocnf() {
            Long actaId = crearActaConCondenaFirme("7B00012");

            bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new java.math.BigDecimal("2000.00"), "REF-7B-012", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }
    }

    @Nested
    @DisplayName("Slice 7B: Guardrails")
    class GuardrailsSlice7B {

        @Test
        @DisplayName("Test 7B-13: No se agregan eventos/bloques/estados centrales nuevos en el ciclo de bloqueantes")
        void sin_nuevos_eventos_bloques_estados_centrales() {
            Long actaId = crearActaConCondenaFirme("7B00013");

            FalBloqueanteMaterial b1 = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b1.getId()));

            FalBloqueanteMaterial b2 = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.OTRO));
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(b2.getId()));

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new java.math.BigDecimal("1500.00"), "REF-7B-013", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<String> codigos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(e -> e.tipoEvt().codigo()).toList();

            assertThat(codigos).doesNotContain("PAGCON");
            assertThat(codigos).doesNotContain("ACTCER");
            assertThat(codigos).doesNotContain("APELAC");
            assertThat(codigos).doesNotContain("DRVEXT");
            assertThat(codigos).doesNotContain("D3_DOCUMENTAL");

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }
    }
    // =========================================================================
    // Tests Slice 7C: Cierre diferido por resolucion del ultimo bloqueante activo
    // =========================================================================

    @Nested
    @DisplayName("Slice 7C: Cierre diferido al resolver el ultimo bloqueante activo")
    class CierreDiferido {

        /**
         * Helper: crea acta con CONDENA_FIRME_PAGADA + ACTIVA/ANAL + 1 bloqueante activo.
         * El cierre fue impedido por el bloqueante activo al momento de PCOCNF.
         * Retorna par [actaId, bloqueanteId].
         */
        private Object[] crearActaCondenaFirmePagadaConBloqueante(String docNum) {
            Long actaId = crearActaConCondenaFirme(docNum);
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7C-" + docNum, null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));
            return new Object[]{actaId, b.getId()};
        }

        @Test
        @DisplayName("Test 7C-01: Cumplir ultimo bloqueante activo con CONDENA_FIRME_PAGADA emite CIERRA y cierra el acta")
        void cumplir_ultimo_bloqueante_condena_firme_pagada_cierra() {
            Object[] par = crearActaCondenaFirmePagadaConBloqueante("7C00001");
            Long actaId = (Long) par[0];
            Long bloqueanteId = (Long) par[1];

            // Estado previo: ACTIVA/ANAL, sin CIERRA
            FalActa actaAntes = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(actaAntes.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(actaAntes.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(eventoRepo.buscarPorActa(actaId).stream().map(FalActaEvento::tipoEvt).toList())
                    .doesNotContain(TipoEventoActa.CIERRA);

            // Cumplir ultimo bloqueante -> cierre diferido
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(bloqueanteId));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
        }

        @Test
        @DisplayName("Test 7C-02: Anular ultimo bloqueante activo con CONDENA_FIRME_PAGADA emite CIERRA y cierra el acta")
        void anular_ultimo_bloqueante_condena_firme_pagada_cierra() {
            Object[] par = crearActaCondenaFirmePagadaConBloqueante("7C00002");
            Long actaId = (Long) par[0];
            Long bloqueanteId = (Long) par[1];

            // Estado previo: ACTIVA/ANAL, sin CIERRA
            assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.ACTIVA);

            // Anular ultimo bloqueante -> cierre diferido
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(bloqueanteId));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }

        @Test
        @DisplayName("Test 7C-03: Con dos bloqueantes activos, cumplir uno no emite CIERRA ni cierra el acta")
        void cumplir_uno_de_dos_bloqueantes_no_cierra() {
            Long actaId = crearActaConCondenaFirme("7C00003");
            FalBloqueanteMaterial b1 = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            FalBloqueanteMaterial b2 = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA));

            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7C-003", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            // Cumplir solo b1; b2 sigue activo
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b1.getId()));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("Test 7C-04: Acta con resultado no cerrable (CONDENA_FIRME): cumplir bloqueante no emite CIERRA")
        void acta_sin_resultado_cerrable_no_cierra() {
            // crearActaConCondenaFirme deja el acta con CONDENA_FIRME (no CONDENA_FIRME_PAGADA)
            Long actaId = crearActaConCondenaFirme("7C00004");
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.DOCUMENTACION_RETENIDA));

            // No se confirma pago: resultadoFinal sigue siendo CONDENA_FIRME (no cerrable)
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b.getId()));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        }

        @Test
        @DisplayName("Test 7C-05: Acta ya cerrada: cumplir bloqueante no emite CIERRA duplicado")
        void acta_ya_cerrada_no_emite_cierra_duplicado() {
            // Crear acta y cerrarla limpiamente (sin bloqueantes activos al confirmar pago)
            Long actaId = crearActaConCondenaFirme("7C00005");
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-7C-005", null));
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            // Verificar que ya esta cerrada con CIERRA
            assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.CERRADA);
            long cierrasAntes = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count();
            assertThat(cierrasAntes).isEqualTo(1L);

            // Registrar y cumplir bloqueante sobre acta ya cerrada
            FalBloqueanteMaterial b = bloqueanteMaterialService.registrar(
                    new RegistrarBloqueanteMaterialCommand(actaId, OrigenBloqueanteMaterial.RODADO));
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(b.getId()));

            long cierrasDespues = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count();
            assertThat(cierrasDespues).isEqualTo(1L);
        }

        @Test
        @DisplayName("Test 7C-06: Cumplir bloqueante idempotente: segunda operacion no emite CIERRA duplicado")
        void cumplir_idempotente_no_duplica_cierra() {
            Object[] par = crearActaCondenaFirmePagadaConBloqueante("7C00006");
            Long actaId = (Long) par[0];
            Long bloqueanteId = (Long) par[1];

            // Primera vez: cumplir -> CIERRA diferido
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(bloqueanteId));
            assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.CERRADA);

            // Segunda vez: idempotente (ya CUMPLIDO, retorno temprano sin intentarCierreDiferido)
            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(bloqueanteId));

            long cierras = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count();
            assertThat(cierras).isEqualTo(1L);
        }

        @Test
        @DisplayName("Test 7C-07: Anular bloqueante idempotente: segunda operacion no emite CIERRA duplicado")
        void anular_idempotente_no_duplica_cierra() {
            Object[] par = crearActaCondenaFirmePagadaConBloqueante("7C00007");
            Long actaId = (Long) par[0];
            Long bloqueanteId = (Long) par[1];

            // Primera vez: anular -> CIERRA diferido
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(bloqueanteId));
            assertThat(actaRepo.buscarPorId(actaId).orElseThrow().getSituacionAdministrativa())
                    .isEqualTo(SituacionAdministrativaActa.CERRADA);

            // Segunda vez: idempotente (ya ANULADO, retorno temprano sin intentarCierreDiferido)
            bloqueanteMaterialService.anular(new AnularBloqueanteMaterialCommand(bloqueanteId));

            long cierras = eventoRepo.buscarPorActa(actaId).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.CIERRA).count();
            assertThat(cierras).isEqualTo(1L);
        }

        @Test
        @DisplayName("Test 7C-08: Guardrail - no se agregan eventos/bloques/estados centrales nuevos")
        void sin_nuevos_eventos_bloques_estados_centrales_7c() {
            Object[] par = crearActaCondenaFirmePagadaConBloqueante("7C00008");
            Long actaId = (Long) par[0];
            Long bloqueanteId = (Long) par[1];

            bloqueanteMaterialService.cumplir(new CumplirBloqueanteMaterialCommand(bloqueanteId));

            List<String> codigos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(e -> e.tipoEvt().codigo()).toList();

            assertThat(codigos).doesNotContain("PAGCON");
            assertThat(codigos).doesNotContain("ACTCER");
            assertThat(codigos).doesNotContain("APELAC");
            assertThat(codigos).doesNotContain("DRVEXT");
            assertThat(codigos).doesNotContain("D3_DOCUMENTAL");

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
        }
    }
}


