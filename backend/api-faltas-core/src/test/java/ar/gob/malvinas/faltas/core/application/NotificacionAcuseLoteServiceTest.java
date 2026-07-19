package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.GenerarLoteCorreoCommand;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11I: NotificacionAcuseService y LoteCorreoService - circuitos")
class NotificacionAcuseLoteServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryNotificacionAcuseRepository acuseRepo;
    private InMemoryLoteCorreoRepository loteRepo;
    private NotificacionAcuseService acuseService;
    private InMemoryPersonaDomicilioRepository domicilioRepo;
    private LoteCorreoService loteService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        acuseRepo = new InMemoryNotificacionAcuseRepository();
        loteRepo = new InMemoryLoteCorreoRepository();
        domicilioRepo = new InMemoryPersonaDomicilioRepository();

        var docRepo = new InMemoryDocumentoRepository();
        var pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        var falloRepo = new InMemoryFalloActaRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

        acuseService = new NotificacionAcuseService(
                acuseRepo, intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED);

        loteService = new LoteCorreoService(
                loteRepo, notifRepo, intentoRepo, actaRepo, eventoRepo, snapshotRepo, recalc, domicilioRepo, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActa(Long id) {
        Long domId = domicilioRepo.nextId();
        domicilioRepo.guardar(new FalPersonaDomicilio(
                domId, 1L, null,
                TipoDomicilio.CONSTITUIDO, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                true, true, true,
                null, null, null, null, null,
                null, null, null, null,
                "Av. Libertad 100", 100, false,
                null, null, "Av. Libertad 100, Malvinas",
                null, false, null, null, null,
                AHORA, "SYS"));
        FalActa a = new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                java.time.LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
        a.setBloqueActual(BloqueActual.NOTI);
        a.setIdDomicilioNotifAct(domId);
        return actaRepo.guardar(a);
    }

    private FalNotificacion crearNotificacion(Long id, Long actaId) {
        return notifRepo.guardar(new FalNotificacion(id, actaId, id, TipoDocu.NOTIFICACION_ACTA, "CORREO", AHORA, AHORA, "SYS"));
    }

    private FalNotificacion crearNotificacionPendiente(Long id, Long actaId) {
        return notifRepo.guardar(FalNotificacion.preparar(id, actaId, id, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS"));
    }

    private FalNotificacionIntento crearIntento(Long id, Long notifId, short nro, CanalNotificacion canal) {
        FalNotificacionIntento i = new FalNotificacionIntento(id, notifId, nro, canal, null, null, null, null, AHORA, AHORA, "SYS");
        return intentoRepo.guardar(i);
    }

    @Nested @DisplayName("NotificacionAcuseService")
    class AcuseServiceTests {

        @Test @DisplayName("registrar acuse RECIBIDO - estado inicial RECIBIDO")
        void registrarAcuseRecibido() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.PRESENCIAL);

            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            assertThat(acuse.getTipoAcuse()).isEqualTo(TipoAcuse.ACUSE_RECEPCION);
            assertThat(acuse.getEstadoAcuse()).isEqualTo(EstadoAcuse.RECIBIDO);
            assertThat(acuse.getNotificacionId()).isEqualTo(10L);
            assertThat(acuse.getIntentoId()).isEqualTo(100L);
        }

        @Test @DisplayName("registrar acuse sin intentoId (canal externo)")
        void acuseSinIntento() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_OTRO, null, null, "USR");
            assertThat(acuse.getIntentoId()).isNull();
        }

        @Test @DisplayName("acuse duplicado activo lanza excepcion")
        void acuseDuplicado() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            assertThatThrownBy(() -> acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR"))
                    .isInstanceOf(AcuseDuplicadoException.class);
        }

        @Test @DisplayName("acuse con intento de otra notificacion lanza excepcion")
        void acuseIntentoOtraNotif() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearNotificacion(20L, 1L);
            crearIntento(100L, 20L, (short)1, CanalNotificacion.PRESENCIAL);
            assertThatThrownBy(() -> acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("validar ACUSE_RECEPCION - resultado POSITIVO en intento y cabecera")
        void validarRecepcionPositivo() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");

            FalNotificacionAcuse validado = acuseRepo.buscarPorId(acuse.getId()).orElseThrow();
            assertThat(validado.getEstadoAcuse()).isEqualTo(EstadoAcuse.VALIDADO);

            FalNotificacionIntento intento = intentoRepo.buscarPorId(100L).orElseThrow();
            assertThat(intento.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);

            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        }

        @Test @DisplayName("validar ACUSE_RECHAZO - resultado NEGATIVO en intento y cabecera")
        void validarRechazoNegativo() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.CORREO_POSTAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECHAZO, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.NEGATIVO);
        }

        @Test @DisplayName("validar ACUSE_DOMICILIO_INEXISTENTE - resultado NEGATIVO")
        void validarDomicilioInexistente() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.CORREO_POSTAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_DOMICILIO_INEXISTENTE, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.NEGATIVO);
        }

        @Test @DisplayName("validar ACUSE_PERSONA_DESCONOCIDA - resultado NEGATIVO")
        void validarPersonaDesconocida() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.CORREO_POSTAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_PERSONA_DESCONOCIDA, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.NEGATIVO);
        }

        @Test @DisplayName("validar ACUSE_AUSENTE - no produce efecto automatico")
        void validarAusente() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.CORREO_POSTAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_AUSENTE, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isNull();
        }

        @Test @DisplayName("validar ACUSE_OTRO - no produce efecto automatico")
        void validarOtro() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_OTRO, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isNull();
        }

        @Test @DisplayName("positiva no se degrada por negativa posterior")
        void positivaNoSeDegrada() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            crearIntento(100L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            crearIntento(101L, 10L, (short)2, CanalNotificacion.CORREO_POSTAL);
            FalNotificacionAcuse a1 = acuseService.registrarAcuse(10L, 100L, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            acuseService.validarAcuse(a1.getId(), "USR");
            FalNotificacionAcuse a2 = acuseService.registrarAcuse(10L, 101L, TipoAcuse.ACUSE_RECHAZO, null, AHORA, "USR");
            acuseService.validarAcuse(a2.getId(), "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
        }

        @Test @DisplayName("observar acuse")
        void observar() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_OTRO, null, AHORA, "USR");
            acuseService.observarAcuse(acuse.getId(), "USR");
            assertThat(acuseRepo.buscarPorId(acuse.getId()).orElseThrow().getEstadoAcuse()).isEqualTo(EstadoAcuse.OBSERVADO);
        }

        @Test @DisplayName("anular acuse - conserva evidencia")
        void anular() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_OTRO, null, AHORA, "USR");
            acuseService.anularAcuse(acuse.getId(), "USR");
            FalNotificacionAcuse anulado = acuseRepo.buscarPorId(acuse.getId()).orElseThrow();
            assertThat(anulado.estaAnulado()).isTrue();
            assertThat(anulado.getNotificacionId()).isEqualTo(10L);
        }

        @Test @DisplayName("doble anulacion lanza excepcion")
        void dobleAnulacion() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_OTRO, null, AHORA, "USR");
            acuseService.anularAcuse(acuse.getId(), "USR");
            assertThatThrownBy(() -> acuseService.anularAcuse(acuse.getId(), "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("validar anulado lanza excepcion")
        void validarAnulado() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_OTRO, null, AHORA, "USR");
            acuseService.anularAcuse(acuse.getId(), "USR");
            assertThatThrownBy(() -> acuseService.validarAcuse(acuse.getId(), "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("evento ACUGEN registrado al crear acuse")
        void eventoAcugen() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            assertThat(eventoRepo.buscarPorActa(1L)).anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACUGEN);
        }

        @Test @DisplayName("evento ACUVAL registrado al validar")
        void eventoAcuval() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            acuseService.validarAcuse(acuse.getId(), "USR");
            assertThat(eventoRepo.buscarPorActa(1L)).anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACUVAL);
        }

        @Test @DisplayName("idempotencia - segundo registro mismo tipo activo lanza excepcion")
        void idempotencia() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR");
            assertThatThrownBy(() -> acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, null, AHORA, "USR"))
                    .isInstanceOf(AcuseDuplicadoException.class);
        }

        @Test @DisplayName("storageKey persiste en acuse")
        void storageKey() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionAcuse acuse = acuseService.registrarAcuse(10L, null, TipoAcuse.ACUSE_RECEPCION, "storage/acuse-001.pdf", AHORA, "USR");
            assertThat(acuse.getStorageKey()).isEqualTo("storage/acuse-001.pdf");
        }
    }

    @Nested @DisplayName("LoteCorreoService")
    class LoteServiceTests {

        @Test @DisplayName("generar lote - estado GENERADO")
        void generarLote() {
            FalLoteCorreo lote = loteService.generarLote("LOT-2026-001", null, null, "USR");
            assertThat(lote.getLoteCodigo()).isEqualTo("LOT-2026-001");
            assertThat(lote.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);
        }

        @Test @DisplayName("codigo duplicado lanza excepcion")
        void codigoDuplicado() {
            loteService.generarLote("LOT-001", null, null, "USR");
            assertThatThrownBy(() -> loteService.generarLote("LOT-001", null, null, "USR"))
                    .isInstanceOf(LoteCodigoDuplicadoException.class);
        }

        @Test @DisplayName("emitir lote GENERADO -> EMITIDO")
        void emitir() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            FalLoteCorreo emitido = loteService.emitirLote(lote.getId(), "USR");
            assertThat(emitido.getEstadoLote()).isEqualTo(EstadoLote.EMITIDO);
        }

        @Test @DisplayName("no se puede emitir un lote ya emitido")
        void noEmitirDosVeces() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            loteService.emitirLote(lote.getId(), "USR");
            assertThatThrownBy(() -> loteService.emitirLote(lote.getId(), "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("procesar lote EMITIDO -> PROCESADO")
        void procesar() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            loteService.emitirLote(lote.getId(), "USR");
            FalLoteCorreo procesado = loteService.procesarLote(lote.getId(), "USR");
            assertThat(procesado.getEstadoLote()).isEqualTo(EstadoLote.PROCESADO);
        }

        @Test @DisplayName("no se puede procesar un lote generado (sin emitir)")
        void noProcesarSinEmitir() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            assertThatThrownBy(() -> loteService.procesarLote(lote.getId(), "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("anular lote GENERADO -> ANULADO")
        void anularDesdeGenerado() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            FalLoteCorreo anulado = loteService.anularLote(lote.getId(), "USR");
            assertThat(anulado.getEstadoLote()).isEqualTo(EstadoLote.ANULADO);
        }

        @Test @DisplayName("anular lote EMITIDO -> ANULADO")
        void anularDesdeEmitido() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            loteService.emitirLote(lote.getId(), "USR");
            FalLoteCorreo anulado = loteService.anularLote(lote.getId(), "USR");
            assertThat(anulado.getEstadoLote()).isEqualTo(EstadoLote.ANULADO);
        }

        @Test @DisplayName("no se puede anular un lote procesado")
        void noAnularProcesado() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            loteService.emitirLote(lote.getId(), "USR");
            loteService.procesarLote(lote.getId(), "USR");
            assertThatThrownBy(() -> loteService.anularLote(lote.getId(), "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("marcarConError desde GENERADO")
        void conError() {
            FalLoteCorreo lote = loteService.generarLote("LOT-001", null, null, "USR");
            FalLoteCorreo conError = loteService.marcarConError(lote.getId(), "USR");
            assertThat(conError.getEstadoLote()).isEqualTo(EstadoLote.CON_ERROR);
        }

        @Test @DisplayName("generarLoteDesdePendientes - crea intento CORREO_POSTAL por notificacion")
        void generarLoteDesdePendientes_creaIntento() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            crearNotificacionPendiente(20L, 1L);

            FalLoteCorreo lote = loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-001", null, null, "USR"));

            assertThat(lote.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);
            assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
            assertThat(intentoRepo.buscarPorNotificacion(20L)).hasSize(1);
            assertThat(intentoRepo.buscarPorNotificacion(10L).get(0).getCanalNotif()).isEqualTo(CanalNotificacion.CORREO_POSTAL);
        }

        @Test @DisplayName("sin notificaciones PENDIENTE_ENVIO lanza excepcion")
        void generarLoteVacio() {
            assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-001", null, null, "USR")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_ENVIO");
        }

        @Test @DisplayName("notificacion ya positiva rechazada al incluir en lote")
        void notifPositivaNoSeIncluye() {
            crearActa(1L);
            FalNotificacion notif = FalNotificacion.preparar(10L, 1L, 10L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            notif.setResultado(ResultadoNotificacion.POSITIVO);
            notifRepo.guardar(notif);

            assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-001", null, null, "SYS")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ya tiene resultado");
        }

        @Test @DisplayName("buscarPorEstado")
        void buscarPorEstado() {
            loteService.generarLote("LOT-001", null, null, "USR");
            loteService.generarLote("LOT-002", null, null, "USR");
            assertThat(loteService.buscarPorEstado(EstadoLote.GENERADO)).hasSize(2);
            assertThat(loteService.buscarPorEstado(EstadoLote.EMITIDO)).isEmpty();
        }

        @Test @DisplayName("buscarPorCodigo")
        void buscarPorCodigo() {
            loteService.generarLote("LOT-ALFA", null, null, "USR");
            assertThat(loteService.buscarPorCodigo("LOT-ALFA")).isNotNull();
            assertThatThrownBy(() -> loteService.buscarPorCodigo("LOT-BETA"))
                    .isInstanceOf(LoteCorreoNoEncontradoException.class);
        }

        @Test @DisplayName("lote inexistente lanza excepcion al emitir")
        void loteInexistente() {
            assertThatThrownBy(() -> loteService.emitirLote(999L, "USR"))
                    .isInstanceOf(LoteCorreoNoEncontradoException.class);
        }

        @Test @DisplayName("command null lanza PrecondicionVioladaException sin efectos")
        void generarLote_idInexistente() {
            assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(null))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(loteRepo.buscarPorEstado(EstadoLote.GENERADO)).isEmpty();
        }

        @Test @DisplayName("guidLoteExt en mayusculas se persiste canonicamente en minusculas")
        void generarLote_idsDuplicados() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            FalLoteCorreo lote = loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-DUP", null,
                            "550E8400-E29B-41D4-A716-446655440000", "SYS"));
            assertThat(lote.getGuidLoteExt()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }

        @Test @DisplayName("notificacion EN_PROCESO rechazada al incluir en lote")
        void generarLote_notifEnProceso_rechazada() {
            crearActa(1L);
            FalNotificacion n = notifRepo.guardar(FalNotificacion.preparar(10L, 1L, 10L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS"));
            n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS");
            notifRepo.guardar(n);
            assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-ENP", null, null, "SYS")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("validacion global previa: segunda notif invalida -> primera permanece PENDIENTE_ENVIO")
        void generarLote_sinMutaciones_cuando_falla() {
            Long domId = domicilioRepo.nextId();
            domicilioRepo.guardar(new FalPersonaDomicilio(
                    domId, 1L, null,
                    TipoDomicilio.CONSTITUIDO, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                    true, true, true,
                    null, null, null, null, null,
                    null, null, null, null,
                    "Av. Libertad 100", 100, false,
                    null, null, "Av. Libertad 100, Malvinas",
                    null, false, null, null, null,
                    AHORA, "SYS"));
            FalActa actaValida = new FalActa(1L, "UUID-1", TipoActa.TRANSITO, 1L, 1L,
                    java.time.LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                    null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
            actaValida.setBloqueActual(BloqueActual.ANAL);
            actaValida.setIdDomicilioNotifAct(domId);
            actaRepo.guardar(actaValida);
            crearNotificacionPendiente(10L, 1L);
            notifRepo.guardar(FalNotificacion.preparar(20L, 999L, 20L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS"));

            assertThatThrownBy(() -> loteService.generarLoteDesdePendientes(
                    new GenerarLoteCorreoCommand("LOT-FAIL", null, null, "SYS")))
                    .isInstanceOf(PrecondicionVioladaException.class);

            assertThatThrownBy(() -> loteService.buscarPorCodigo("LOT-FAIL")).isInstanceOf(Exception.class);
            assertThat(intentoRepo.buscarPorNotificacion(10L)).isEmpty();
            assertThat(notifRepo.buscarPorId(10L).orElseThrow().getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
            assertThat(eventoRepo.buscarPorActa(1L)).isEmpty();
            assertThat(actaRepo.buscarPorId(1L).orElseThrow().getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test @DisplayName("canal final es CORREO_POSTAL al generar lote")
        void generarLote_canal_correoPostal() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-CANAL", null, null, "SYS"));
            assertThat(notifRepo.buscarPorId(10L).orElseThrow().getCanal()).isEqualTo("CORREO_POSTAL");
        }

        @Test @DisplayName("fechaEnvio no es nula al generar lote")
        void generarLote_fechaEnvio_noNula() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-FECHA", null, null, "SYS"));
            assertThat(notifRepo.buscarPorId(10L).orElseThrow().getFechaEnvio()).isNotNull();
        }

        @Test @DisplayName("intentos == 1 despues de generar lote")
        void generarLote_intentos_uno() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-INT", null, null, "SYS"));
            assertThat(notifRepo.buscarPorId(10L).orElseThrow().getIntentos()).isEqualTo(1);
        }

        @Test @DisplayName("exactamente un intento en intentoRepo por notificacion")
        void generarLote_exactamente_un_intento() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-UNO", null, null, "SYS"));
            assertThat(intentoRepo.buscarPorNotificacion(10L)).hasSize(1);
        }

        @Test @DisplayName("evento LOTGEN emitido al generar lote")
        void generarLote_evento_LOTGEN() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-EVT", null, null, "SYS"));
            assertThat(eventoRepo.buscarPorActa(1L)).anyMatch(e -> e.tipoEvt() == TipoEventoActa.LOTGEN);
        }

        @Test @DisplayName("snapshot recalculado al generar lote")
        void generarLote_snapshot_recalculado() {
            crearActa(1L);
            crearNotificacionPendiente(10L, 1L);
            loteService.generarLoteDesdePendientes(new GenerarLoteCorreoCommand("LOT-SNAP", null, null, "SYS"));
            assertThat(snapshotRepo.buscarPorActa(1L)).isPresent();
        }
    }
    @Nested @DisplayName("FalNotificacion.iniciarEnvio")
    class IniciarEnvioTests {

        @Test @DisplayName("PENDIENTE_ENVIO -> EN_PROCESO")
        void iniciarEnvio_transicion_ok() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
            n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS");
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }

        @Test @DisplayName("canal obligatorio")
        void iniciarEnvio_canal_obligatorio() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            assertThatThrownBy(() -> n.iniciarEnvio(null, AHORA, AHORA, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> n.iniciarEnvio("", AHORA, AHORA, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
        }

        @Test @DisplayName("fecha obligatoria")
        void iniciarEnvio_fecha_obligatoria() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            assertThatThrownBy(() -> n.iniciarEnvio("CORREO_POSTAL", null, AHORA, "SYS"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
        }

        @Test @DisplayName("intentos incrementa de 0 a 1")
        void iniciarEnvio_intentos_incrementa() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            assertThat(n.getIntentos()).isEqualTo(0);
            n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS");
            assertThat(n.getIntentos()).isEqualTo(1);
        }

        @Test @DisplayName("segunda llamada desde EN_PROCESO lanza excepcion")
        void iniciarEnvio_segunda_vez_rechaza() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS");
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(n.getIntentos()).isEqualTo(1);

            assertThatThrownBy(() -> n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS"))
                    .isInstanceOf(PrecondicionVioladaException.class);

            // Contador permanece en 1, estado no muto
            assertThat(n.getIntentos()).isEqualTo(1);
            assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }

        @Test @DisplayName("estado y datos no mutan ante rechazo")
        void iniciarEnvio_no_muta_ante_rechazo() {
            FalNotificacion n = FalNotificacion.preparar(1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA, "SYS");
            n.iniciarEnvio("CORREO_POSTAL", AHORA, AHORA, "SYS");
            String canalAntes = n.getCanal();
            int intentosAntes = n.getIntentos();

            try { n.iniciarEnvio("OTRO_CANAL", AHORA, AHORA, "SYS"); } catch (Exception ignored) {}

            assertThat(n.getCanal()).isEqualTo(canalAntes);
            assertThat(n.getIntentos()).isEqualTo(intentosAntes);
        }
    }
}
