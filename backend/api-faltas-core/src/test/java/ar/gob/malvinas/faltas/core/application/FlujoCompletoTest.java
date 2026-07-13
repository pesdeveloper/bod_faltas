package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
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
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Flujo completo del expediente de faltas")
class FlujoCompletoTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoVoluntarioRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;


    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private final ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository intentoRepo = new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository();

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoVoluntarioRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();


        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVoluntarioRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED);
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
    }

    @Nested
    @DisplayName("1. Labrar acta")
    class LabrarActaTests {

        @Test
        @DisplayName("Crea FalActa con bloque CAPT y evento ACTLAB")
        void labrar_crea_acta_bloque_capt() {
            ComandoResultado resultado = actaService.labrar(cmdLabrar());

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.ACTLAB.codigo());

            FalActa acta = actaService.obtenerActa(resultado.idActa());
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CAPT);
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getInfractorDocumento()).isEqualTo("12345678");

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(acta.getId());
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
        }

        @Test
        @DisplayName("Snapshot inicial con bandeja ACTAS_EN_ENRIQUECIMIENTO y accion COMPLETAR_CAPTURA")
        void labrar_snapshot_inicial_correcto() {
            ComandoResultado resultado = actaService.labrar(cmdLabrar());

            FalActaSnapshot snap = actaService.obtenerSnapshot(resultado.idActa());
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.COMPLETAR_CAPTURA);
            assertThat(snap.getBloqueActual()).isEqualTo(BloqueActual.CAPT);
            assertThat(snap.isTieneDocumentos()).isFalse();
        }
    }

    @Nested
    @DisplayName("2. Completar captura")
    class CompletarCapturaTests {

        @Test
        @DisplayName("Mueve a bloque ENRI y registra evento ACTCAP")
        void completarCaptura_mueve_a_enri() {
            Long id = actaService.labrar(cmdLabrar()).idActa();

            ComandoResultado resultado = actaService.completarCaptura(
                    new CompletarCapturaCommand(id, "captura ok"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.ACTCAP.codigo());

            FalActa acta = actaService.obtenerActa(id);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ENRI);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(id);
            assertThat(eventos).hasSize(2);
            assertThat(eventos.get(1).tipoEvt()).isEqualTo(TipoEventoActa.ACTCAP);

            FalActaSnapshot snap = actaService.obtenerSnapshot(id);
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO);
            assertThat(snap.getBloqueActual()).isEqualTo(BloqueActual.ENRI);
        }
    }

    @Nested
    @DisplayName("3. Generar documento")
    class GenerarDocumentoTests {

        @Test
        @DisplayName("Crea FalDocumento PENDIENTE_FIRMA y registra evento DOCGEN")
        void generar_documento_correcto() {
            Long idActa = labrarYCompletarCaptura();
            actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));

            ComandoResultado resultado = docService.generarDocumento(
                    new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Acta principal"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.DOCGEN.codigo());

            List<FalDocumento> docs = docRepo.buscarPorActa(idActa);
            assertThat(docs).hasSize(1);
            assertThat(docs.get(0).getEstadoDocu()).isEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(docs.get(0).getTipoDocu()).isEqualTo(TipoDocu.ACTA_INFRACCION);

            FalActaSnapshot snap = actaService.obtenerSnapshot(idActa);
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.FIRMAR_DOCUMENTO);
            assertThat(snap.isTieneDocumentos()).isTrue();
            assertThat(snap.isTieneDocsPendientesFirma()).isTrue();
        }
    }

    @Nested
    @DisplayName("4. Firmar documento")
    class FirmarDocumentoTests {

        @Test
        @DisplayName("Actualiza estado a FIRMADO, crea FirmaDocumento y registra DOCFIR")
        void firmar_documento_correcto() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = docService.generarDocumento(
                    new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Desc"))
                    .idEntidadAfectada();

            ComandoResultado resultado = docService.firmarDocumento(
                    new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.DOCFIR.codigo());

            FalDocumento doc = docRepo.buscarPorActa(idActa).get(0);
            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.FIRMADO);

            FalActaSnapshot snap = actaService.obtenerSnapshot(idActa);
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_NOTIFICACION);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.ENVIAR_NOTIFICACION);
        }
    }

    @Nested
    @DisplayName("5. Enviar notificacion")
    class EnviarNotificacionTests {

        @Test
        @DisplayName("Crea FalNotificacion, mueve acta a NOTI y registra NOTENV")
        void enviar_notificacion_correcto() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = generarYFirmarDoc(idActa);

            ComandoResultado resultado = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTENV.codigo());

            FalActa acta = actaService.obtenerActa(idActa);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.NOTI);

            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);
            assertThat(notifs.get(0).getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }
    }

    @Nested
    @DisplayName("6. Registrar resultado de notificacion")
    class RegistrarResultadoNotificacionTests {

        @Test
        @DisplayName("Positiva: mueve a ANAL y registra NOTPOS")
        void registrar_notificacion_positiva_avanza_bloque_a_anal() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = generarYFirmarDoc(idActa);
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarPositiva(
                    new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTPOS.codigo());

            FalActa acta = actaService.obtenerActa(idActa);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            FalActaSnapshot snap = actaService.obtenerSnapshot(idActa);
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DICTAR_FALLO);
        }

        @Test
        @DisplayName("Negativa: registra NOTNEG y no cambia bloque")
        void registrar_notificacion_negativa() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = generarYFirmarDoc(idActa);
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarNegativa(
                    new RegistrarNotificacionNegativaCommand(idNotif, null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTNEG.codigo());
        }

        @Test
        @DisplayName("Vencida: registra NOTVNC")
        void registrar_notificacion_vencida() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = generarYFirmarDoc(idActa);
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                    .idEntidadAfectada();

            ComandoResultado resultado = notifService.registrarVencida(
                    new RegistrarNotificacionVencidaCommand(idNotif, null));

            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTVNC.codigo());
        }
    }

    @Nested
    @DisplayName("7. No labrar si acta no existe")
    class ErroresTests {

        @Test
        @DisplayName("Lanza ActaNoEncontradaException si id no existe")
        void lanza_no_encontrada_si_id_inexistente() {
            assertThatThrownBy(() -> actaService.obtenerActa(999999L))
                    .isInstanceOf(ActaNoEncontradaException.class);
        }

        @Test
        @DisplayName("No permite completar captura si no esta en CAPT")
        void no_completar_captura_fuera_de_capt() {
            Long idActa = labrarYCompletarCaptura();
            assertThatThrownBy(() ->
                    actaService.completarCaptura(new CompletarCapturaCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("CAPT");
        }

        @Test
        @DisplayName("No permite enriquecer si bloque es CAPT")
        void no_enriquecer_en_capt() {
            Long idActa = actaService.labrar(cmdLabrar()).idActa();

            assertThatThrownBy(() ->
                    actaService.enriquecer(new EnriquecerActaCommand(idActa, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ENRI");
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza D3_DOCUMENTAL")
        void bloque_d3_documental_rechazado() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("D3"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> BloqueActual.deCodigo("D3_DOCUMENTAL"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("BloqueActual.deCodigo rechaza D1_CAPTURA (alias legacy prohibido)")
        void bloque_d1_captura_rechazado_como_codigo_productivo() {
            assertThatThrownBy(() -> BloqueActual.deCodigo("D1_CAPTURA"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("No registrar doble resultado en la misma notificacion")
        void no_doble_resultado_notificacion() {
            Long idActa = labrarYCompletarCaptura();
            String idDoc = generarYFirmarDoc(idActa);
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                    .idEntidadAfectada();

            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThatThrownBy(() ->
                    notifService.registrarNegativa(
                            new RegistrarNotificacionNegativaCommand(idNotif, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("resultado registrado");
        }

        @Test
        @DisplayName("TipoEventoActa no incluye PASE_BANDEJA")
        void tipo_evento_no_incluye_pase_bandeja() {
            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.name()).doesNotContain("PASE_BANDEJA");
                assertThat(t.codigo()).doesNotContain("PASE");
            }
        }

        @Test
        @DisplayName("BloqueActual no incluye D3_DOCUMENTAL ni D1 ni D2 ni D4 ni D5")
        void bloque_actual_no_incluye_valores_historicos() {
            for (BloqueActual b : BloqueActual.values()) {
                assertThat(b.codigo()).doesNotContain("D1");
                assertThat(b.codigo()).doesNotContain("D2");
                assertThat(b.codigo()).doesNotContain("D3");
                assertThat(b.codigo()).doesNotContain("D4");
                assertThat(b.codigo()).doesNotContain("D5");
                assertThat(b.name()).doesNotContain("DOCUMENTAL");
            }
        }
    }

    @Nested
    @DisplayName("10. Timeline del expediente")
    class TimelineTests {

        @Test
        @DisplayName("Timeline contiene eventos en orden logico con tipos correctos")
        void timeline_flujo_completo_orden_correcto() {
            Long idActa = labrarYCompletarCaptura();
            actaService.enriquecer(new EnriquecerActaCommand(idActa, "enriquecido"));
            String idDoc = generarYFirmarDoc(idActa);
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, Long.parseLong(idDoc), CanalNotificacion.EMAIL, "test@malvinas.gob.ar", null, null, "test-user"))
                    .idEntidadAfectada();
            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            List<FalActaEvento> timeline = eventoRepo.buscarPorActa(idActa);
            assertThat(timeline).hasSize(7);
            assertThat(timeline.get(0).tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(timeline.get(1).tipoEvt()).isEqualTo(TipoEventoActa.ACTCAP);
            assertThat(timeline.get(2).tipoEvt()).isEqualTo(TipoEventoActa.ACTENR);
            assertThat(timeline.get(3).tipoEvt()).isEqualTo(TipoEventoActa.DOCGEN);
            assertThat(timeline.get(4).tipoEvt()).isEqualTo(TipoEventoActa.DOCFIR);
            assertThat(timeline.get(5).tipoEvt()).isEqualTo(TipoEventoActa.NOTENV);
            assertThat(timeline.get(6).tipoEvt()).isEqualTo(TipoEventoActa.NOTPOS);
        }
    }

    @Nested
    @DisplayName("11. Separacion semantica de enums")
    class SeparacionEnumsTests {

        @Test
        @DisplayName("EstadoProcesalActa no contiene valores de situacion administrativa")
        void estadoProcesal_no_contiene_situaciones_administrativas() {
            Set<String> nombres = Arrays.stream(EstadoProcesalActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("ACTIVA", "VIGENTE", "PARALIZADA", "ARCHIVADA", "ANULADA");
        }

        @Test
        @DisplayName("EstadoProcesalActa contiene los valores procesales esperados")
        void estadoProcesal_contiene_valores_procesales() {
            Set<String> nombres = Arrays.stream(EstadoProcesalActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).contains("EN_TRAMITE", "CONCLUIDO", "PRESCRIPTO");
        }

        @Test
        @DisplayName("SituacionAdministrativaActa no contiene VIGENTE (reemplazado por ACTIVA)")
        void situacionAdministrativa_no_contiene_vigente() {
            Set<String> nombres = Arrays.stream(SituacionAdministrativaActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("VIGENTE");
        }

        @Test
        @DisplayName("SituacionAdministrativaActa contiene los valores administrativos esperados")
        void situacionAdministrativa_contiene_valores_administrativos() {
            Set<String> nombres = Arrays.stream(SituacionAdministrativaActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).contains(
                    "ACTIVA", "PARALIZADA", "EN_GESTION_EXTERNA", "ARCHIVADA", "CERRADA", "ANULADA");
        }

        @Test
        @DisplayName("ResultadoFinalActa contiene SIN_RESULTADO_FINAL como primer valor")
        void resultadoFinal_contiene_sin_resultado_final() {
            assertThat(ResultadoFinalActa.SIN_RESULTADO_FINAL).isNotNull();
            assertThat(ResultadoFinalActa.values()[0]).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        }

        @Test
        @DisplayName("ResultadoFinalActa contiene ABSUELTO")
        void resultadoFinal_contiene_absuelto() {
            assertThat(ResultadoFinalActa.ABSUELTO).isNotNull();
        }

        @Test
        @DisplayName("Snapshot inicial tiene estadoProcesal EN_TRAMITE y situacion ACTIVA")
        void snapshot_inicial_estado_procesal_en_tramite_y_situacion_activa() {
            ComandoResultado resultado = actaService.labrar(cmdLabrar());
            FalActaSnapshot snap = actaService.obtenerSnapshot(resultado.idActa());
            assertThat(snap.getEstadoProcesal()).isEqualTo(EstadoProcesalActa.EN_TRAMITE);
            assertThat(snap.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("FalActa inicial no esta cerrada ni paralizada (via situacionAdministrativa)")
        void acta_inicial_no_cerrada_ni_paralizada() {
            ComandoResultado resultado = actaService.labrar(cmdLabrar());
            FalActa acta = actaService.obtenerActa(resultado.idActa());
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.estaParalizada()).isFalse();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("FalActa inicial tiene resultadoFinal SIN_RESULTADO_FINAL, no null")
        void acta_inicial_resultado_final_sin_resultado() {
            ComandoResultado resultado = actaService.labrar(cmdLabrar());
            FalActa acta = actaService.obtenerActa(resultado.idActa());
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            assertThat(acta.getResultadoFinal()).isNotNull();
        }

        @Test
        @DisplayName("TipoEventoActa incluye ACTCAP y ACTENR como eventos productivos")
        void tipo_evento_incluye_actcap_y_actenr_como_eventos_productivos() {
            assertThat(TipoEventoActa.ACTCAP.codigo()).isEqualTo("ACTCAP");
            assertThat(TipoEventoActa.ACTENR.codigo()).isEqualTo("ACTENR");
            assertThat(TipoEventoActa.deCodigo("ACTCAP")).isEqualTo(TipoEventoActa.ACTCAP);
            assertThat(TipoEventoActa.deCodigo("ACTENR")).isEqualTo(TipoEventoActa.ACTENR);
        }

        @Test
        @DisplayName("TipoEventoActa incluye FALABS y FALCON con codigos correctos")
        void tipo_evento_incluye_falabs_y_falcon() {
            assertThat(TipoEventoActa.FALABS.codigo()).isEqualTo("FALABS");
            assertThat(TipoEventoActa.FALCON.codigo()).isEqualTo("FALCON");
            assertThat(TipoEventoActa.deCodigo("FALABS")).isEqualTo(TipoEventoActa.FALABS);
            assertThat(TipoEventoActa.deCodigo("FALCON")).isEqualTo(TipoEventoActa.FALCON);
        }
    }

    private LabrarActaCommand cmdLabrar() {
        return new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 123", "San Martin 456",
                null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null
        );
    }

    private Long labrarYCompletarCaptura() {
        Long idActa = actaService.labrar(cmdLabrar()).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(idActa, null));
        return idActa;
    }

    private String generarYFirmarDoc(Long idActa) {
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(idActa, TipoDocu.ACTA_INFRACCION, "Desc"))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "Inspector", "DIGITAL", null));
        return idDoc;
    }
}
