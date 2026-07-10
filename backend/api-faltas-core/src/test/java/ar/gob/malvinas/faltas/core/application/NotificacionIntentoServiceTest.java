package ar.gob.malvinas.faltas.core.application;

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

@DisplayName("8F-11I: NotificacionIntentoService - circuitos funcionales")
class NotificacionIntentoServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryLoteCorreoRepository loteRepo;
    private NotificacionIntentoService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        loteRepo = new InMemoryLoteCorreoRepository();

        var docRepo = new InMemoryDocumentoRepository();
        var pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        var falloRepo = new InMemoryFalloActaRepository();
        var apelacionRepo = new InMemoryApelacionActaRepository();
        var pagoCondenaRepo = new InMemoryPagoCondenaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED);

        service = new NotificacionIntentoService(
                intentoRepo, notifRepo, actaRepo, eventoRepo, snapshotRepo, recalc, loteRepo, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActa(Long id) {
        FalActa a = new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                java.time.LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
        a.setBloqueActual(BloqueActual.NOTI);
        return actaRepo.guardar(a);
    }

    private FalNotificacion crearNotificacion(Long id, Long actaId) {
        FalNotificacion n = new FalNotificacion(id, actaId, 1L, TipoDocu.NOTIFICACION_ACTA,
                "CORREO", AHORA, AHORA, "SYS");
        return notifRepo.guardar(n);
    }

    @Nested @DisplayName("Registro de intento")
    class RegistrarIntento {

        @Test @DisplayName("primer intento en canal presencial - nroIntento=1")
        void primerIntentoPresencial() {
            crearActa(1L);
            crearNotificacion(10L, 1L);

            FalNotificacionIntento intento = service.registrarIntento(
                    10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");

            assertThat(intento.getNroIntento()).isEqualTo((short)1);
            assertThat(intento.getCanalNotif()).isEqualTo(CanalNotificacion.PRESENCIAL);
            assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(intento.getResultadoIntento()).isNull();
        }

        @Test @DisplayName("canal postal requiere domicilioNotifId")
        void canalPostalRequiereDomicilio() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            assertThatThrownBy(() -> service.registrarIntento(
                    10L, CanalNotificacion.CORREO_POSTAL, null, null, null, null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("domicilioNotifId");
        }

        @Test @DisplayName("canal email requiere destinoDigital")
        void canalEmailRequiereDestino() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            assertThatThrownBy(() -> service.registrarIntento(
                    10L, CanalNotificacion.EMAIL, null, null, null, null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("destinoDigital");
        }

        @Test @DisplayName("canal email - domicilio fisico lanza excepcion")
        void canalEmailConDomicilioFisico() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            assertThatThrownBy(() -> service.registrarIntento(
                    10L, CanalNotificacion.EMAIL, 500L, "email@mail.com", null, null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("lote anulado no puede asignarse")
        void loteAnuladoNoPermitido() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalLoteCorreo lote = new FalLoteCorreo(1L, "LOT-001", AHORA, AHORA, "SYS");
            lote.setEstadoLote(EstadoLote.ANULADO);
            loteRepo.guardar(lote);
            assertThatThrownBy(() -> service.registrarIntento(
                    10L, CanalNotificacion.CORREO_POSTAL, 100L, null, 1L, null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("anulado");
        }

        @Test @DisplayName("referencia externa duplicada lanza excepcion")
        void referenciaExternaDuplicada() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, "REF-001", "USR");
            assertThatThrownBy(() -> service.registrarIntento(
                    10L, CanalNotificacion.PRESENCIAL, null, null, null, "REF-001", "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("notificacion inexistente lanza excepcion")
        void notifInexistente() {
            assertThatThrownBy(() -> service.registrarIntento(
                    999L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR"))
                    .isInstanceOf(NotificacionNoEncontradaException.class);
        }

        @Test @DisplayName("dos intentos sucesivos - correlativos 1 y 2")
        void dosIntentos() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento i1 = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            FalNotificacionIntento i2 = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            assertThat(i1.getNroIntento()).isEqualTo((short)1);
            assertThat(i2.getNroIntento()).isEqualTo((short)2);
        }

        @Test @DisplayName("evento NOTINT registrado en primer intento")
        void eventoNotintPrimerIntento() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(1L);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.NOTINT);
        }

        @Test @DisplayName("evento NOTREI registrado en segundo intento")
        void eventoNotreiSegundoIntento() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(1L);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.NOTREI);
        }
    }

    @Nested @DisplayName("Registro de resultado de intento")
    class RegistrarResultado {

        @Test @DisplayName("resultado POSITIVO - estado CON_ACUSE_POSITIVO")
        void resultadoPositivo() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            FalNotificacionIntento result = service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.POSITIVO, "USR");
            assertThat(result.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(result.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
            assertThat(result.getFhResultado()).isNotNull();
        }

        @Test @DisplayName("resultado NEGATIVO - estado CON_ACUSE_NEGATIVO")
        void resultadoNegativo() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            FalNotificacionIntento result = service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.NEGATIVO, "USR");
            assertThat(result.getResultadoIntento()).isEqualTo(ResultadoNotificacion.NEGATIVO);
            assertThat(result.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_NEGATIVO);
        }

        @Test @DisplayName("resultado VENCIDO - estado VENCIDA")
        void resultadoVencido() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            FalNotificacionIntento result = service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.VENCIDO, "USR");
            assertThat(result.getEstadoIntento()).isEqualTo(EstadoNotificacion.VENCIDA);
        }

        @Test @DisplayName("resultado SUPERADA_POR_PORTAL - estado SIN_EFECTO")
        void resultadoSuperadaPorPortal() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            FalNotificacionIntento result = service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.SUPERADA_POR_PORTAL, "USR");
            assertThat(result.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
        }

        @Test @DisplayName("resultado null lanza excepcion")
        void resultadoNull() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            assertThatThrownBy(() -> service.registrarResultadoIntento(intento.getId(), null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("doble resultado lanza excepcion")
        void dobleResultado() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento intento = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.NEGATIVO, "USR");
            assertThatThrownBy(() -> service.registrarResultadoIntento(intento.getId(), ResultadoNotificacion.POSITIVO, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("intento inexistente lanza excepcion")
        void intentoInexistente() {
            assertThatThrownBy(() -> service.registrarResultadoIntento(999L, ResultadoNotificacion.POSITIVO, "USR"))
                    .isInstanceOf(NotificacionIntentoNoEncontradoException.class);
        }
    }

    @Nested @DisplayName("Reintento post vencimiento")
    class ReintentoPostVencimiento {

        @Test @DisplayName("reintento desde VENCIDA - nuevo intento con correlativo siguiente")
        void reintentoDesdeVencida() {
            crearActa(1L);
            FalNotificacion notif = crearNotificacion(10L, 1L);
            notif.setEstado(EstadoNotificacion.VENCIDA);
            notif.setResultado(ResultadoNotificacion.VENCIDO);
            notifRepo.guardar(notif);

            FalNotificacionIntento i1 = new FalNotificacionIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL, null, null, null, null, AHORA, AHORA, "SYS");
            i1.setResultadoIntento(ResultadoNotificacion.VENCIDO);
            i1.setEstadoIntento(EstadoNotificacion.VENCIDA);
            intentoRepo.guardar(i1);

            FalNotificacionIntento reintento = service.registrarReintentoPorVencimiento(
                    10L, CanalNotificacion.CORREO_POSTAL, 100L, null, null, null, "USR");
            assertThat(reintento.getNroIntento()).isEqualTo((short)2);
            assertThat(reintento.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }

        @Test @DisplayName("reintento desde no-vencida lanza excepcion")
        void reintentoDesdeNoVencida() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            assertThatThrownBy(() -> service.registrarReintentoPorVencimiento(
                    10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("VENCIDA");
        }

        @Test @DisplayName("evento NOTRVE registrado")
        void eventoNotrve() {
            crearActa(1L);
            FalNotificacion notif = crearNotificacion(10L, 1L);
            notif.setEstado(EstadoNotificacion.VENCIDA);
            notif.setResultado(ResultadoNotificacion.VENCIDO);
            notifRepo.guardar(notif);

            service.registrarReintentoPorVencimiento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            assertThat(eventoRepo.buscarPorActa(1L)).anyMatch(e -> e.tipoEvt() == TipoEventoActa.NOTRVE);
        }
    }

    @Nested @DisplayName("Portal como autonotificacion")
    class PortalAutonotificacion {

        @Test @DisplayName("portal positivo - intento PORTAL_INFRACTOR creado con resultado POSITIVO")
        void portalPositivo() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento portalIntento = service.registrarPortalPositivo(10L, "user-portal-001", "USR");
            assertThat(portalIntento.getCanalNotif()).isEqualTo(CanalNotificacion.PORTAL_INFRACTOR);
            assertThat(portalIntento.getResultadoIntento()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(portalIntento.getEstadoIntento()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
        }

        @Test @DisplayName("portal - notificacion cabecera queda CON_ACUSE_POSITIVO")
        void portalActualizaCabecera() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarPortalPositivo(10L, "user-portal-001", "USR");
            FalNotificacion notif = notifRepo.buscarPorId(10L).orElseThrow();
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.CON_ACUSE_POSITIVO);
            assertThat(notif.getResultado()).isEqualTo(ResultadoNotificacion.POSITIVO);
            assertThat(notif.tieneResultadoPositivo()).isTrue();
        }

        @Test @DisplayName("portal supera intentos previos en curso")
        void portalSuperaIntentosPrevios() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            FalNotificacionIntento previo = service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            assertThat(previo.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);

            service.registrarPortalPositivo(10L, "user-portal-001", "USR");

            FalNotificacionIntento previoActualizado = intentoRepo.buscarPorId(previo.getId()).orElseThrow();
            assertThat(previoActualizado.getResultadoIntento()).isEqualTo(ResultadoNotificacion.SUPERADA_POR_PORTAL);
            assertThat(previoActualizado.getEstadoIntento()).isEqualTo(EstadoNotificacion.SIN_EFECTO);
        }

        @Test @DisplayName("portal cuando ya es positiva - lanza excepcion")
        void portalCuandoYaPositiva() {
            crearActa(1L);
            FalNotificacion notif = crearNotificacion(10L, 1L);
            notif.setResultado(ResultadoNotificacion.POSITIVO);
            notifRepo.guardar(notif);
            assertThatThrownBy(() -> service.registrarPortalPositivo(10L, "user", "USR"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("POSITIVO");
        }

        @Test @DisplayName("eventos NOTSUP y PORPOS registrados")
        void eventosPortal() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            service.registrarPortalPositivo(10L, "user-portal", "USR");
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(1L);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.NOTSUP);
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.PORPOS);
        }

        @Test @DisplayName("obtenerIntentos devuelve historial completo")
        void obtenerIntentos() {
            crearActa(1L);
            crearNotificacion(10L, 1L);
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            service.registrarIntento(10L, CanalNotificacion.PRESENCIAL, null, null, null, null, "USR");
            service.registrarPortalPositivo(10L, "user", "USR");
            List<FalNotificacionIntento> intentos = service.obtenerIntentos(10L);
            assertThat(intentos).hasSize(3);
        }
    }
}