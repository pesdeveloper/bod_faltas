package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.ConfirmarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.InformarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.ObservarPagoCondenaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.ApelacionActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.FirmezaCondenaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.application.service.PagoCondenaService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPagoCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoCondena;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 5: Pago de condena.
 *
 * Flujo: CONDENA_FIRME -> PCOINF -> PCOCNF + CIERRA | PCOOBS
 *
 * PAGCON no existe como evento productivo.
 * Eventos correctos: PCOINF, PCOCNF, PCOOBS.
 */
@DisplayName("Slice 5: Pago de condena (PCOINF/PCOCNF/PCOOBS)")
class PagoCondenaTest {

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

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private ApelacionActaService apelacionService;
    private FirmezaCondenaService firmezaService;
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

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo);

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
                falloRepo, new NoOpBloqueantesMaterialesChecker());
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc);
        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc,
                new NoOpBloqueantesMaterialesChecker());
        firmezaService = new FirmezaCondenaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, firmezaRepo, recalc);
        pagoCondenaService = new PagoCondenaService(
                actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondenaRepo, recalc,
                new NoOpBloqueantesMaterialesChecker());
    }

    // =========================================================================
    // Helpers: construir acta con condena firme
    // =========================================================================

    private Long crearActaConCondenaFirme(String doc) {
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

    private Long informarPago(Long actaId) {
        pagoCondenaService.informar(new InformarPagoCondenaCommand(
                actaId, new BigDecimal("3000.00"), "REF-001", null));
        return actaId;
    }

    // =========================================================================
    // Guardrails de eventos (Tests 1-2)
    // =========================================================================

    @Nested
    @DisplayName("Guardrails de eventos de pago condena")
    class GuardrailEventos {

        @Test
        @DisplayName("Test 1: PCOINF, PCOCNF, PCOOBS existen en TipoEventoActa")
        void eventos_pago_condena_existen() {
            assertThat(TipoEventoActa.deCodigo("PCOINF")).isEqualTo(TipoEventoActa.PCOINF);
            assertThat(TipoEventoActa.deCodigo("PCOCNF")).isEqualTo(TipoEventoActa.PCOCNF);
            assertThat(TipoEventoActa.deCodigo("PCOOBS")).isEqualTo(TipoEventoActa.PCOOBS);
        }

        @Test
        @DisplayName("Test 2: PAGCON no existe (evento prohibido)")
        void pagcon_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGCON"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PAGCON");

            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("PAGCON");
                assertThat(t.name()).isNotEqualTo("PAGCON");
            }
        }
    }

    // =========================================================================
    // Informar pago condena (Tests 3-4)
    // =========================================================================

    @Nested
    @DisplayName("Informar pago condena")
    class InformarPagoCondenaTests {

        @Test
        @DisplayName("Test 3: Informar pago condena registra PCOINF")
        void informar_registra_pcoinf() {
            Long actaId = crearActaConCondenaFirme("50000001");
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-ABC-001", "Pago via banco"));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOINF);
        }

        @Test
        @DisplayName("Test 4: Informar pago condena no cierra el acta")
        void informar_no_cierra() {
            Long actaId = crearActaConCondenaFirme("50000002");
            pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000.00"), "REF-001", null));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // Confirmar pago condena (Tests 5-6-7)
    // =========================================================================

    @Nested
    @DisplayName("Confirmar pago condena")
    class ConfirmarPagoCondenaTests {

        @Test
        @DisplayName("Test 5: Confirmar pago condena registra PCOCNF y luego CIERRA")
        void confirmar_registra_pcocnf_y_cierra() {
            Long actaId = crearActaConCondenaFirme("50000003");
            informarPago(actaId);
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);
            assertThat(tipos).contains(TipoEventoActa.CIERRA);

            int idxPcocnf = tipos.indexOf(TipoEventoActa.PCOCNF);
            int idxCierra = tipos.indexOf(TipoEventoActa.CIERRA);
            assertThat(idxPcocnf).isLessThan(idxCierra);
        }

        @Test
        @DisplayName("Test 6: Confirmar pago condena sin bloqueantes: cierra acta")
        void confirmar_sin_bloqueantes_cierra() {
            Long actaId = crearActaConCondenaFirme("50000004");
            informarPago(actaId);
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Test 7: Confirmar pago condena con bloqueantes activos: emite PCOCNF, no emite CIERRA, acta ACTIVA/ANAL")
        void confirmar_con_bloqueantes_emite_pcocnf_no_cierra() {
            Long actaId = crearActaConCondenaFirme("50000005");
            informarPago(actaId);

            PagoCondenaService servicioConBloqueantes = new PagoCondenaService(
                    actaRepo, eventoRepo, snapshotRepo, falloRepo, pagoCondenaRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo),
                    actaId2 -> true);

            servicioConBloqueantes.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            // PCOCNF fue emitido
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOCNF);

            // CIERRA NO fue emitido
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            // resultadoFinal = CONDENA_FIRME_PAGADA
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

            // El acta queda ACTIVA / ANAL (no cerrada)
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            // El pago queda CONFIRMADO
            Optional<FalPagoCondena> pago = pagoCondenaRepo.buscarPorActa(actaId);
            assertThat(pago).isPresent();
            assertThat(pago.get().getEstadoPagoCondena()).isEqualTo(EstadoPagoCondena.CONFIRMADO);

            // Snapshot: PENDIENTE_ANALISIS / NINGUNA
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }
    }

    // =========================================================================
    // Observar pago condena (Tests 8-9)
    // =========================================================================

    @Nested
    @DisplayName("Observar pago condena")
    class ObservarPagoCondenaTests {

        @Test
        @DisplayName("Test 8: Observar pago condena registra PCOOBS")
        void observar_registra_pcoobs() {
            Long actaId = crearActaConCondenaFirme("50000006");
            informarPago(actaId);
            pagoCondenaService.observar(new ObservarPagoCondenaCommand(
                    actaId, "Referencia invalida", null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.PCOOBS);
        }

        @Test
        @DisplayName("Test 9: Observar pago condena no cierra el acta")
        void observar_no_cierra() {
            Long actaId = crearActaConCondenaFirme("50000007");
            informarPago(actaId);
            pagoCondenaService.observar(new ObservarPagoCondenaCommand(
                    actaId, "Monto incorrecto", null));

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId)
                    .stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // Precondiciones (Tests 10-18)
    // =========================================================================

    @Nested
    @DisplayName("Precondiciones de pago condena")
    class Precondiciones {

        @Test
        @DisplayName("Test 10: No permitir pago condena si resultadoFinal != CONDENA_FIRME")
        void no_pago_condena_sin_condena_firme() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    LocalDate.now(), "Av. Argentina 123", "San Martin 456",
                    null, null, null, "Infractor Test", "50000008",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();

            assertThatThrownBy(() -> pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("1000"), "REF-001", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CONDENA_FIRME");
        }

        @Test
        @DisplayName("Test 11: No permitir pago condena sin fallo condenatorio notificado")
        void no_pago_condena_sin_fallo_notificado() {
            // Simular acta con resultado CONDENA_FIRME pero sin fallo activo (caso borde)
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "DEP-001", "INS-001",
                    LocalDate.now(), "Av. Argentina 123", "San Martin 456",
                    null, null, null, "Infractor Test", "50000009",
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
            Long actaId = actaService.labrar(cmd).idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("1000"), "REF-001", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fallo");
        }

        @Test
        @DisplayName("Test 12: No permitir monto cero o negativo")
        void no_monto_cero_o_negativo() {
            Long actaId = crearActaConCondenaFirme("50000010");

            assertThatThrownBy(() -> pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, BigDecimal.ZERO, "REF-001", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");

            assertThatThrownBy(() -> pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("-100"), "REF-001", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Test 13: No permitir doble confirmacion")
        void no_doble_confirmacion() {
            Long actaId = crearActaConCondenaFirme("50000011");
            informarPago(actaId);
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            // Acta ya cerrada: no se puede operar
            assertThatThrownBy(() -> pagoCondenaService.informar(new InformarPagoCondenaCommand(
                    actaId, new BigDecimal("3000"), "REF-002", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Test 14: No permitir confirmar sin pago informado previo")
        void no_confirmar_sin_pago_informado() {
            Long actaId = crearActaConCondenaFirme("50000012");

            assertThatThrownBy(() -> pagoCondenaService.confirmar(
                    new ConfirmarPagoCondenaCommand(actaId, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("registrado");
        }

        @Test
        @DisplayName("Test 15: No permitir observar pago ya confirmado")
        void no_observar_pago_confirmado() {
            Long actaId = crearActaConCondenaFirme("50000013");
            informarPago(actaId);
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));

            // Acta ya cerrada: no se puede operar
            assertThatThrownBy(() -> pagoCondenaService.observar(new ObservarPagoCondenaCommand(
                    actaId, "Motivo", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Test 16: No permitir PAGCON (evento prohibido)")
        void no_permitir_pagcon() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("PAGCON"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PAGCON");
        }

        @Test
        @DisplayName("Test 17: No reintroducir D3_DOCUMENTAL como bloque")
        void no_d3_documental() {
            assertThatThrownBy(() ->
                    ar.gob.malvinas.faltas.core.domain.enums.BloqueActual.deCodigo("D3_DOCUMENTAL"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Test 18: No reintroducir ACTCER como evento")
        void no_actcer() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("ACTCER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ACTCER");

            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.codigo()).isNotEqualTo("ACTCER");
                assertThat(t.name()).isNotEqualTo("ACTCER");
            }
        }
    }

    // =========================================================================
    // Snapshot routing por estado pago condena
    // =========================================================================

    @Nested
    @DisplayName("Snapshot: routing por estado pago condena")
    class SnapshotRouting {

        @Test
        @DisplayName("CONDENA_FIRME sin pago: snapshot con GESTIONAR_PAGO_CONDENA")
        void condena_firme_sin_pago_snap() {
            Long actaId = crearActaConCondenaFirme("50000020");
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.GESTIONAR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Pago condena INFORMADO: snapshot CONFIRMAR_PAGO_CONDENA")
        void pago_informado_snap() {
            Long actaId = crearActaConCondenaFirme("50000021");
            informarPago(actaId);
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CONFIRMAR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Pago condena OBSERVADO: snapshot CORREGIR_PAGO_CONDENA")
        void pago_observado_snap() {
            Long actaId = crearActaConCondenaFirme("50000022");
            informarPago(actaId);
            pagoCondenaService.observar(new ObservarPagoCondenaCommand(
                    actaId, "Referencia invalida", null));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.CORREGIR_PAGO_CONDENA);
        }

        @Test
        @DisplayName("Pago condena CONFIRMADO y acta cerrada: CERRADAS/NINGUNA")
        void pago_confirmado_snap() {
            Long actaId = crearActaConCondenaFirme("50000023");
            informarPago(actaId);
            pagoCondenaService.confirmar(new ConfirmarPagoCondenaCommand(actaId, null));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }
    }
}





