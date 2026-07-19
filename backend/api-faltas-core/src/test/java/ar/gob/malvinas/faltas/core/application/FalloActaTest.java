package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
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
 * Tests del Slice 3A: fallo absolutorio y condenatorio.
 *
 * Circuito: dictar fallo -> generar doc -> firmar doc -> notificar -> resultado.
 * Dictar fallo NO cierra. Firmar NO cierra.
 * Absolutorio notificado sin bloqueantes -> cierra con ABSUELTO.
 * Condenatorio notificado -> pendiente prox slice (apelacion / pago condena).
 */
@DisplayName("Slice 3A: Fallo absolutorio y condenatorio")
class FalloActaTest {

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

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
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
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

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
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED, intentoRepo, new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository(),
                    ar.gob.malvinas.faltas.core.support.PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, pagoRepo, recalc, FaltasClockTestSupport.FIXED);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // =========================================================================
    // Casos felices
    // =========================================================================

    @Nested
    @DisplayName("Casos felices: Absolutorio")
    class AbsolutorioCasosFelices {

        @Test
        @DisplayName("1a. Dictar absolutorio: crea FalActaFallo, doc FALLO_ABSOLUTORIO, FALABS+DOCGEN, no cierra")
        void dictar_absolutorio_flujo_inicial() {
            Long idActa = llegarAAnalisis();

            ComandoResultado resultado = falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Absolutorio por falta de evidencia", null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.FALABS.codigo());
            assertThat(resultado.idActa()).isEqualTo(idActa);

            Optional<FalActaFallo> falloOpt = falloRepo.buscarActivo(idActa);
            assertThat(falloOpt).isPresent();
            FalActaFallo fallo = falloOpt.get();
            assertThat(fallo.getTipoFallo()).isEqualTo(TipoFalloActa.ABSOLUTORIO);
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);
            assertThat(fallo.isSiActivo()).isTrue();

            List<FalDocumento> docs = docRepo.buscarPorActa(idActa);
            FalDocumento docFallo = docs.stream()
                    .filter(d -> d.getTipoDocu() == TipoDocu.ACTO_ADMINISTRATIVO)
                    .findFirst().orElseThrow();
            assertThat(docFallo.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(fallo.getDocumentoId()).isEqualTo(docFallo.getId());

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.FALABS, TipoEventoActa.DOCGEN);

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.FIRMAR_DOCUMENTO);
        }

        @Test
        @DisplayName("3. Firma de doc de fallo absolutorio: DOCFIR, fallo FIRMADO, snapshot PENDIENTE_NOTIFICACION")
        void firmar_documento_fallo_absolutorio() {
            Long idActa = llegarAAnalisis();
            String idFallo = falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null)).idEntidadAfectada();

            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            Long idDocFallo = fallo.getDocumentoId();

            ComandoResultado resultado = docService.firmarDocumento(
                    new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.DOCFIR.codigo());

            FalActaFallo falloActualizado = falloRepo.buscarActivo(idActa).orElseThrow();
            assertThat(falloActualizado.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_NOTIFICACION);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.ENVIAR_NOTIFICACION);

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
        }

        @Test
        @DisplayName("4. Notif positiva absolutorio sin bloqueantes: ABSUELTO, CIERRA, snapshot CERRADAS")
        void notif_positiva_absolutorio_sin_bloqueantes_cierra() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            Long idDocFallo = fallo.getDocumentoId();

            docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));

            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarPositiva(
                    new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);

            FalActaFallo falloAct = falloRepo.buscarActivo(idActa).orElseThrow();
            assertThat(falloAct.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.FALABS, TipoEventoActa.DOCGEN,
                    TipoEventoActa.DOCFIR, TipoEventoActa.NOTENV,
                    TipoEventoActa.NOTPOS, TipoEventoActa.CIERRA);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("4b. Timeline completo absolutorio: ACTLAB..FALABS..CIERRA en orden")
        void timeline_absolutorio_completo() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            Long idDocFallo = fallo.getDocumentoId();
            docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();
            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(idActa).stream()
                    .map(FalActaEvento::tipoEvt).toList();

            assertThat(tipos).containsSubsequence(
                    TipoEventoActa.ACTLAB,
                    TipoEventoActa.ACTCAP,
                    TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCGEN,
                    TipoEventoActa.DOCFIR,
                    TipoEventoActa.NOTENV,
                    TipoEventoActa.NOTPOS,
                    TipoEventoActa.FALABS,
                    TipoEventoActa.DOCGEN,
                    TipoEventoActa.DOCFIR,
                    TipoEventoActa.NOTENV,
                    TipoEventoActa.NOTPOS,
                    TipoEventoActa.CIERRA);
        }
    }

    @Nested
    @DisplayName("Casos felices: Condenatorio")
    class CondenatoriosCasosFelices {

        @Test
        @DisplayName("2. Dictar condenatorio: FalActaFallo, montoCondena, doc FALLO_CONDENATORIO, FALCON+DOCGEN, no cierra")
        void dictar_condenatorio_flujo_inicial() {
            Long idActa = llegarAAnalisis();

            ComandoResultado resultado = falloService.dictarCondenatorio(
                    new DictarFalloCondenatorioCommand(
                            idActa, new BigDecimal("5000.00"), "Condenatorio por infraccion grave", null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.FALCON.codigo());

            Optional<FalActaFallo> falloOpt = falloRepo.buscarActivo(idActa);
            assertThat(falloOpt).isPresent();
            FalActaFallo fallo = falloOpt.get();
            assertThat(fallo.getTipoFallo()).isEqualTo(TipoFalloActa.CONDENATORIO);
            assertThat(fallo.getMontoCondena()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);

            List<FalDocumento> docs = docRepo.buscarPorActa(idActa);
            FalDocumento docFallo = docs.stream()
                    .filter(d -> d.getTipoDocu() == TipoDocu.ACTO_ADMINISTRATIVO)
                    .findFirst().orElseThrow();
            assertThat(docFallo.getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(idActa).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.FALCON, TipoEventoActa.DOCGEN);

            assertThat(actaRepo.buscarPorId(idActa).orElseThrow().estaCerrada()).isFalse();

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);
        }

        @Test
        @DisplayName("5. Notif positiva condenatorio: fallo NOTIFICADO, no cierra, no CONDENA_FIRME, no CIERRA")
        void notif_positiva_condenatorio_no_cierra() {
            Long idActa = llegarAAnalisis();
            falloService.dictarCondenatorio(
                    new DictarFalloCondenatorioCommand(
                            idActa, new BigDecimal("3000.00"), "Cond", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            Long idDocFallo = fallo.getDocumentoId();

            docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarPositiva(
                    new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);

            FalActaFallo falloAct = falloRepo.buscarActivo(idActa).orElseThrow();
            assertThat(falloAct.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(idActa).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTES_FALLO);
        }

        @Test
        @DisplayName("6. Notificacion negativa de fallo: NOTNEG, no consolida resultado, no cierra")
        void notif_negativa_fallo() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", "DIGITAL", null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, fallo.getDocumentoId(), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarNegativa(
                    new RegistrarNotificacionNegativaCommand(idNotif, null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTNEG.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        }

        @Test
        @DisplayName("7. Notificacion vencida de fallo: NOTVNC, no consolida resultado, no cierra")
        void notif_vencida_fallo() {
            Long idActa = llegarAAnalisis();
            falloService.dictarCondenatorio(
                    new DictarFalloCondenatorioCommand(
                            idActa, new BigDecimal("2000.00"), "Cond", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", "DIGITAL", null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, fallo.getDocumentoId(), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarVencida(
                    new RegistrarNotificacionVencidaCommand(idNotif, null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTVNC.codigo());

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        }
    }

    // =========================================================================
    // Casos invalidos
    // =========================================================================

    @Nested
    @DisplayName("Casos invalidos")
    class CasosInvalidos {

        @Test
        @DisplayName("No dictar fallo si acta no esta en ANAL")
        void no_dictar_fallo_fuera_de_anal() {
            Long idActa = labrarYCompletarCaptura();

            assertThatThrownBy(() ->
                    falloService.dictarAbsolutorio(
                            new DictarFalloAbsolutorioCommand(idActa, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ANAL");
        }

        @Test
        @DisplayName("No dictar fallo si acta ya esta cerrada")
        void no_dictar_fallo_si_cerrada() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", null, null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, fallo.getDocumentoId(), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();
            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThat(actaRepo.buscarPorId(idActa).orElseThrow().estaCerrada()).isTrue();

            assertThatThrownBy(() ->
                    falloService.dictarAbsolutorio(
                            new DictarFalloAbsolutorioCommand(idActa, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("No dictar segundo fallo activo en la misma acta")
        void no_dictar_fallo_duplicado() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));

            assertThatThrownBy(() ->
                    falloService.dictarAbsolutorio(
                            new DictarFalloAbsolutorioCommand(idActa, "Abs2", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fallo activo");
        }

        @Test
        @DisplayName("No dictar condenatorio con monto cero")
        void no_condenatorio_monto_cero() {
            Long idActa = llegarAAnalisis();

            assertThatThrownBy(() ->
                    falloService.dictarCondenatorio(
                            new DictarFalloCondenatorioCommand(
                                    idActa, BigDecimal.ZERO, "Cond", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("No dictar condenatorio con monto negativo")
        void no_condenatorio_monto_negativo() {
            Long idActa = llegarAAnalisis();

            assertThatThrownBy(() ->
                    falloService.dictarCondenatorio(
                            new DictarFalloCondenatorioCommand(
                                    idActa, new BigDecimal("-100"), "Cond", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("monto");
        }

        @Test
        @DisplayName("Absolutorio con bloqueantes: asigna ABSUELTO pero no cierra")
        void absolutorio_con_bloqueantes_no_cierra() {
            FalloActaService falloConBloqueantes = new FalloActaService(
                    actaRepo, eventoRepo, snapshotRepo, docRepo,
                    falloRepo, pagoRepo,
                    new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo), FaltasClockTestSupport.FIXED);
            NotificacionService notifConBloqueantes = new NotificacionService(
                    actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo,
                    new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo),
                    falloRepo,
                    actaId -> true, FaltasClockTestSupport.FIXED,
                    intentoRepo, new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository(),
                    ar.gob.malvinas.faltas.core.support.PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));

            Long idActa = llegarAAnalisis();
            falloConBloqueantes.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", null, null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, fallo.getDocumentoId(), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();

            notifConBloqueantes.registrarPositiva(
                    new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.estaCerrada()).isFalse();

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(idActa).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("Condenatorio notificado no genera CONDENA_FIRME ni cierra")
        void condenatorio_notificado_no_genera_condena_firme() {
            Long idActa = llegarAAnalisis();
            falloService.dictarCondenatorio(
                    new DictarFalloCondenatorioCommand(
                            idActa, new BigDecimal("5000"), "Cond", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", null, null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, fallo.getDocumentoId(), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();
            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO);
        }

        @Test
        @DisplayName("Firma de doc de fallo NO cierra el acta")
        void firmar_fallo_no_cierra() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();

            docService.firmarDocumento(
                    new FirmarDocumentoCommand(fallo.getDocumentoId(), "Juez", null, null));

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
        }

        @Test
        @DisplayName("Dictado de fallo NO cierra el acta")
        void dictar_fallo_no_cierra() {
            Long idActa = llegarAAnalisis();
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(idActa, "Abs", null));

            FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LabrarActaCommand cmdLabrar() {
        return new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 123", "San Martin 456",
                null, null, null, "Maria Lopez", "87654321",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null
        );
    }

    private Long labrarYCompletarCaptura() {
        Long idActa = actaService.labrar(cmdLabrar()).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        return idActa;
    }

    /**
     * Lleva el acta hasta bloque ANAL listo para dictar fallo:
     * labrar -> captura -> enriquecer -> generar doc inicial -> firmar -> notificar -> positiva.
     */
    private Long llegarAAnalisis() {
        Long idActa = labrarYCompletarCaptura();
        actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));

        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Acta principal"))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));

        String idNotif = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

        FalActa acta = actaRepo.buscarPorId(idActa).orElseThrow();
        assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        return idActa;
    }
}
