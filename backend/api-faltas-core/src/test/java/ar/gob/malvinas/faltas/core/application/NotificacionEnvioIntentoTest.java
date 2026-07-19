package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Suite focal para CMD-FALLO-002: enviarNotificacion con intento persistido.
 *
 * Cubre: CREAR, REUTILIZAR, EMAIL, canal fisico, PRESENCIAL, PORTAL, referencia externa,
 * concurrencia, cabecera incompatible, reloj canonico.
 *
 * Sin Mockito. Sin sleeps.
 */
@DisplayName("GAP-CONFORMIDAD-NOTENV-INTENTO-001: enviarNotificacion focal")
class NotificacionEnvioIntentoTest {

    private static final String ACTOR = "test-actor";
    private static final String DEP = "DEP-TEST";

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private NotificacionRepository notifRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;
    private InMemoryPersonaDomicilioRepository domicilioRepo;
    private FalloActaRepository falloRepo;

    private NotificacionService notifService;
    private CountingClock countingClock;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        notifRepo = new InMemoryNotificacionRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();
        domicilioRepo = new InMemoryPersonaDomicilioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        countingClock = new CountingClock(FaltasClockTestSupport.FIXED);

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo,
                new InMemoryPagoVoluntarioRepository(), falloRepo,
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED, snapshotRepo);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), countingClock,
                intentoRepo, domicilioRepo,
                ar.gob.malvinas.faltas.core.support.PlazosTestSupport.conCalendarioVacio(countingClock));
    }

    // -------------------------------------------------------------------------
    // Helper: CountingClock
    // -------------------------------------------------------------------------

    static class CountingClock extends FaltasClock {
        int calls = 0;
        private final FaltasClock delegate;

        CountingClock(FaltasClock delegate) {
            super(FaltasClockTestSupport.FIXED_CLOCK);
            this.delegate = delegate;
        }

        @Override
        public LocalDateTime now() {
            calls++;
            return delegate.now();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de fixture
    // -------------------------------------------------------------------------

    private Long crearActa() {
        return crearActaConDomicilio(null);
    }

    private Long crearActaConDomicilio(Long domicilioId) {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, UUID.randomUUID().toString(),
                "TRANSITO", DEP, "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(),
                FaltasClockTestSupport.FIXED.now(),
                "Belgrano 300", "Ref 1", null, null, null,
                "Test Infractor", "12345678", ResultadoFirmaInfractor.FIRMADA);
        acta.setBloqueActual(BloqueActual.ANAL);
        if (domicilioId != null) acta.setIdDomicilioNotifAct(domicilioId);
        actaRepo.guardar(acta);
        return id;
    }

    private Long crearDocFirmado(Long idActa) {
        Long id = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                id, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                FaltasClockTestSupport.FIXED.now(), "Documento Test",
                EstadoDocu.FIRMADO, TipoFirmaReq.NO_REQUIERE,
                null, FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return id;
    }

    private Long crearDocNoFirmado(Long idActa) {
        Long id = docRepo.nextId();
        FalDocumento doc = new FalDocumento(
                id, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                FaltasClockTestSupport.FIXED.now(), "Sin firmar",
                EstadoDocu.PENDIENTE_FIRMA, TipoFirmaReq.NO_REQUIERE,
                null, FaltasClockTestSupport.FIXED.now());
        docRepo.guardar(doc);
        return id;
    }

    private Long crearDomicilio() {
        Long id = domicilioRepo.nextId();
        FalPersonaDomicilio d = new FalPersonaDomicilio(
                id, 1L, null,
                TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.EXTERNO,
                true, true, true,
                (short) 1, UnidadTerritorialTipo.MUNICIPIO, 1001,
                null, null,
                null, null, null, null,
                "Av. Test 100", 100, false, null, "1000",
                "Av. Test 100, Test Ciudad", null, false,
                null, null, null,
                FaltasClockTestSupport.FIXED.now(), "sys");
        domicilioRepo.guardar(d);
        return id;
    }

    private EnviarNotificacionCommand cmdPresencial(Long idActa, Long idDoc) {
        return new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PRESENCIAL,
                null, null, null, ACTOR);
    }

    // =========================================================================
    // 1. Camino feliz CREAR PRESENCIAL
    // =========================================================================

    @Nested
    @DisplayName("1. Camino feliz CREAR PRESENCIAL")
    class CrearPresencial {

        @Test
        @DisplayName("1a. Crea cabecera e intento, actor y timestamp en todo, bloqueActual=NOTI")
        void crear_presencial_feliz() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            countingClock.calls = 0;

            ComandoResultado resultado = notifService.enviarNotificacion(
                    cmdPresencial(idActa, idDoc));

            // reloj exactamente una vez
            assertThat(countingClock.calls).isEqualTo(1);

            // resultado devuelve cabecera.id (no intento.id)
            Long notifId = Long.parseLong(resultado.idEntidadAfectada());
            assertThat(resultado.tipoEvento()).isEqualTo(TipoEventoActa.NOTENV.codigo());
            assertThat(resultado.idActa()).isEqualTo(idActa);

            // exactamente una cabecera
            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);
            FalNotificacion notif = notifs.get(0);
            assertThat(notif.getId()).isEqualTo(notifId);
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(notif.getCanal()).isEqualTo("PRESENCIAL");
            assertThat(notif.getFechaEnvio()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notif.getIdUserAlta()).isEqualTo(ACTOR);
            assertThat(notif.getFhAlta()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notif.getResultado()).isNull();
            assertThat(notif.getIntentos()).isEqualTo(1);
            assertThat(notif.getFhUltMod()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

            // exactamente un intento
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(notifId);
            assertThat(intentos).hasSize(1);
            FalNotificacionIntento intento = intentos.get(0);
            assertThat(intento.getNroIntento()).isEqualTo((short) 1);
            assertThat(intento.getCanalNotif()).isEqualTo(CanalNotificacion.PRESENCIAL);
            assertThat(intento.getDomicilioNotifId()).isNull();
            assertThat(intento.getDestinoDigital()).isNull();
            assertThat(intento.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(intento.getResultadoIntento()).isNull();
            assertThat(intento.getIdUserAlta()).isEqualTo(ACTOR);
            assertThat(intento.getFhIntento()).isEqualTo(FaltasClockTestSupport.FIXED.now());

            // actor y mismo instante en evento NOTENV
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(idActa);
            FalActaEvento notenv = eventos.stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).findFirst().orElseThrow();
            assertThat(notenv.idUserEvt()).isEqualTo(ACTOR);
            assertThat(notenv.fhEvt()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notenv.actorTipo()).isEqualTo(ActorTipoEvento.USUARIO_INTERNO);
            assertThat(notenv.origenEvt()).isEqualTo(OrigenEvento.USUARIO_WEB);
            assertThat(notenv.idNotifRel()).isEqualTo(notifId);

            // mismo instante en cabecera, intento y evento
            assertThat(notif.getFechaEnvio())
                    .isEqualTo(intento.getFhIntento())
                    .isEqualTo(notenv.fhEvt());

            // acta -> NOTI
            FalActa actaPost = actaRepo.buscarPorId(idActa).orElseThrow();
            assertThat(actaPost.getBloqueActual()).isEqualTo(BloqueActual.NOTI);

            // snapshot exacto: codBandeja y accionPendiente
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja())
                    .isEqualTo(CodigoBandeja.EN_NOTIFICACION);
            assertThat(snap.getAccionPendiente())
                    .isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
        }
    }

    // =========================================================================
    // 2. Camino feliz REUTILIZAR
    // =========================================================================

    @Nested
    @DisplayName("2. Camino feliz REUTILIZAR")
    class ReutilizarPendienteEnvio {

        @Test
        @DisplayName("2a. PENDIENTE_ENVIO reutilizado: misma cabecera, un intento nuevo, NOTENV unico")
        void reutilizar_pendiente_envio() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);

            Long idNotifPrev = notifRepo.nextId();
            FalNotificacion notifPrep = FalNotificacion.preparar(
                    idNotifPrev, idActa, idDoc, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "prep-actor");
            notifPrep.setObservaciones("obs previa");
            notifRepo.guardar(notifPrep);

            countingClock.calls = 0;
            notifService.enviarNotificacion(cmdPresencial(idActa, idDoc));

            // reloj exactamente una vez
            assertThat(countingClock.calls).isEqualTo(1);

            // misma cabecera
            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);
            FalNotificacion notif = notifs.get(0);
            assertThat(notif.getId()).isEqualTo(idNotifPrev);
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(notif.getIntentos()).isEqualTo(1);
            assertThat(notif.getResultado()).isNull();
            assertThat(notif.getFhUltMod()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notif.getIdUserUltMod()).isEqualTo(ACTOR);

            // un intento nuevo
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(idNotifPrev);
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getNroIntento()).isEqualTo((short) 1);
            assertThat(intentos.get(0).getFhIntento()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(intentos.get(0).getIdUserAlta()).isEqualTo(ACTOR);

            // un unico NOTENV con actor e instante exactos
            List<FalActaEvento> eventosNotenv = eventoRepo.buscarPorActa(idActa).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV)
                    .toList();
            assertThat(eventosNotenv).hasSize(1);
            FalActaEvento notenv = eventosNotenv.get(0);
            assertThat(notenv.fhEvt()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(notenv.idUserEvt()).isEqualTo(ACTOR);

            // snapshot EN_NOTIFICACION / EVALUAR_NOTIFICACION
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(idActa).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.EVALUAR_NOTIFICACION);
        }

        @Test
        @DisplayName("2b. observaciones null preservan valor previo")
        void reutilizar_observaciones_null_preserva() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);

            Long idNotifPrev = notifRepo.nextId();
            FalNotificacion notifPrep = FalNotificacion.preparar(
                    idNotifPrev, idActa, idDoc, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "prep");
            notifPrep.setObservaciones("VALOR PREVIO");
            notifRepo.guardar(notifPrep);

            notifService.enviarNotificacion(cmdPresencial(idActa, idDoc));

            FalNotificacion notif = notifRepo.buscarPorId(idNotifPrev).orElseThrow();
            assertThat(notif.getObservaciones()).isEqualTo("VALOR PREVIO");
        }
    }

    // =========================================================================
    // 3. EMAIL
    // =========================================================================

    @Nested
    @DisplayName("3. Canal EMAIL")
    class EmailCanal {

        @Test
        @DisplayName("3a. EMAIL: destino trim, domicilioNotifId null")
        void email_destino_trim_domicilio_null() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);

            notifService.enviarNotificacion(new EnviarNotificacionCommand(
                    idActa, idDoc, CanalNotificacion.EMAIL,
                    "  test@test.com  ", null, null, ACTOR));

            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(
                    Long.parseLong(notifRepo.buscarPorActa(idActa).get(0).getId().toString()));
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getDestinoDigital()).isEqualTo("test@test.com");
            assertThat(intentos.get(0).getDomicilioNotifId()).isNull();
        }

        @Test
        @DisplayName("3b. EMAIL sin destino -> rechazo")
        void email_sin_destino_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            countingClock.calls = 0;
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.EMAIL,
                            null, null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(countingClock.calls).isEqualTo(0);
        }

        @Test
        @DisplayName("3c. EMAIL con destino blanco -> rechazo")
        void email_destino_blanco_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.EMAIL,
                            "   ", null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("3d. EMAIL con destino >120 chars -> rechazo")
        void email_destino_muy_largo_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            String largo = "a".repeat(121) + "@test.com";
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.EMAIL,
                            largo, null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 4. Canal fisico
    // =========================================================================

    @Nested
    @DisplayName("4. Canal fisico (CORREO_POSTAL)")
    class CanalFisico {

        @Test
        @DisplayName("4a. domicilioNotifId correcto, destinoDigital null")
        void canal_fisico_domicilio_correcto() {
            Long domicilioId = crearDomicilio();
            Long idActa = crearActaConDomicilio(domicilioId);
            Long idDoc = crearDocFirmado(idActa);

            notifService.enviarNotificacion(new EnviarNotificacionCommand(
                    idActa, idDoc, CanalNotificacion.CORREO_POSTAL,
                    null, null, null, ACTOR));

            FalNotificacion notif = notifRepo.buscarPorActa(idActa).get(0);
            List<FalNotificacionIntento> intentos = intentoRepo.buscarPorNotificacion(notif.getId());
            assertThat(intentos).hasSize(1);
            assertThat(intentos.get(0).getDomicilioNotifId()).isEqualTo(domicilioId);
            assertThat(intentos.get(0).getDestinoDigital()).isNull();
        }

        @Test
        @DisplayName("4b. idDomicilioNotifAct null -> rechazo sin efectos")
        void canal_fisico_sin_domicilio_acta_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            countingClock.calls = 0;
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.CORREO_POSTAL,
                            null, null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(countingClock.calls).isEqualTo(0);
            assertThat(notifRepo.buscarPorActa(idActa)).isEmpty();
            assertThat(intentoRepo.buscarPorNotificacion(1L)).isEmpty();
        }

        @Test
        @DisplayName("4c. domicilio inexistente -> rechazo sin efectos")
        void canal_fisico_domicilio_inexistente_rechaza() {
            Long idActa = crearActaConDomicilio(999L);
            Long idDoc = crearDocFirmado(idActa);
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.CORREO_POSTAL,
                            null, null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(notifRepo.buscarPorActa(idActa)).isEmpty();
        }

        @Test
        @DisplayName("4d. destinoDigital no null con canal fisico -> rechazo")
        void canal_fisico_con_destino_digital_rechaza() {
            Long domicilioId = crearDomicilio();
            Long idActa = crearActaConDomicilio(domicilioId);
            Long idDoc = crearDocFirmado(idActa);
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.CORREO_POSTAL,
                            "correo@test.com", null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 5. PRESENCIAL con destinoDigital -> rechazo
    // =========================================================================

    @Test
    @DisplayName("5. PRESENCIAL con destinoDigital no null -> rechazo")
    void presencial_con_destino_digital_rechaza() {
        Long idActa = crearActa();
        Long idDoc = crearDocFirmado(idActa);
        assertThatThrownBy(() -> notifService.enviarNotificacion(
                new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PRESENCIAL,
                        "destino@test.com", null, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // =========================================================================
    // 6. PORTAL_INFRACTOR -> rechazo
    // =========================================================================

    @Test
    @DisplayName("6. PORTAL_INFRACTOR -> siempre rechazado")
    void portal_infractor_rechaza() {
        Long idActa = crearActa();
        Long idDoc = crearDocFirmado(idActa);
        assertThatThrownBy(() -> notifService.enviarNotificacion(
                new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PORTAL_INFRACTOR,
                        null, null, null, ACTOR)))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // =========================================================================
    // 7. Referencia externa
    // =========================================================================

    @Nested
    @DisplayName("7. Referencia externa")
    class ReferenciaExterna {

        @Test
        @DisplayName("7a. Referencia valida -> trim persistido")
        void referencia_trim_persistida() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);

            notifService.enviarNotificacion(new EnviarNotificacionCommand(
                    idActa, idDoc, CanalNotificacion.PRESENCIAL,
                    null, "  REF-001  ", null, ACTOR));

            Long notifId = notifRepo.buscarPorActa(idActa).get(0).getId();
            FalNotificacionIntento intento = intentoRepo.buscarPorNotificacion(notifId).get(0);
            assertThat(intento.getReferenciaExterna()).isEqualTo("REF-001");
        }

        @Test
        @DisplayName("7b. Referencia blanca -> rechazo")
        void referencia_blanca_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PRESENCIAL,
                            null, "   ", null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("7c. Referencia >80 chars -> rechazo")
        void referencia_muy_larga_rechaza() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            String largo = "X".repeat(81);
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PRESENCIAL,
                            null, largo, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("7d. Referencia duplicada secuencial -> rechazo sin efectos adicionales")
        void referencia_duplicada_secuencial_rechaza() {
            Long idActa = crearActa();
            Long idDoc1 = crearDocFirmado(idActa);
            Long idActa2 = crearActa();
            Long idDoc2 = crearDocFirmado(idActa2);

            notifService.enviarNotificacion(new EnviarNotificacionCommand(
                    idActa, idDoc1, CanalNotificacion.PRESENCIAL, null, "REF-DUP", null, ACTOR));

            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa2, idDoc2, CanalNotificacion.PRESENCIAL,
                            null, "REF-DUP", null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);

            // cero cabeceras para idActa2
            assertThat(notifRepo.buscarPorActa(idActa2)).isEmpty();

            // cero intentos asociados (implicado por cero cabeceras)
            long intentosIdActa2 = 0;
            for (FalNotificacion n : notifRepo.buscarPorActa(idActa2)) {
                intentosIdActa2 += intentoRepo.buscarPorNotificacion(n.getId()).size();
            }
            assertThat(intentosIdActa2).isEqualTo(0);

            // cero NOTENV para idActa2
            long notenvIdActa2 = eventoRepo.buscarPorActa(idActa2).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count();
            assertThat(notenvIdActa2).isEqualTo(0);

            // bloqueActual permanece ANAL
            assertThat(actaRepo.buscarPorId(idActa2).orElseThrow().getBloqueActual())
                    .isEqualTo(BloqueActual.ANAL);

            // cero snapshot para idActa2
            assertThat(snapshotRepo.buscarPorActa(idActa2)).isNotPresent();
        }

        @Test
        @DisplayName("7e. Concurrencia: dos documentos, misma referencia -> exactamente un exito")
        void referencia_concurrente_un_exito() throws InterruptedException {
            Long idActa1 = crearActa();
            Long idDoc1 = crearDocFirmado(idActa1);
            Long idActa2 = crearActa();
            Long idDoc2 = crearDocFirmado(idActa2);

            String refComun = "REF-CONCURRENTE";
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger rechazos = new AtomicInteger(0);

            CountDownLatch listo = new CountDownLatch(2);
            CountDownLatch arranca = new CountDownLatch(1);

            Runnable tarea1 = () -> {
                try {
                    listo.countDown();
                    arranca.await();
                    notifService.enviarNotificacion(new EnviarNotificacionCommand(
                            idActa1, idDoc1, CanalNotificacion.PRESENCIAL,
                            null, refComun, null, ACTOR));
                    exitos.incrementAndGet();
                } catch (PrecondicionVioladaException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            Runnable tarea2 = () -> {
                try {
                    listo.countDown();
                    arranca.await();
                    notifService.enviarNotificacion(new EnviarNotificacionCommand(
                            idActa2, idDoc2, CanalNotificacion.PRESENCIAL,
                            null, refComun, null, ACTOR));
                    exitos.incrementAndGet();
                } catch (PrecondicionVioladaException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            Thread t1 = new Thread(tarea1);
            Thread t2 = new Thread(tarea2);
            t1.start();
            t2.start();
            listo.await();
            arranca.countDown();
            t1.join();
            t2.join();

            // exactamente un exito y un rechazo
            assertThat(exitos.get()).isEqualTo(1);
            assertThat(rechazos.get()).isEqualTo(1);

            // exactamente una cabecera total entre las dos actas
            List<FalNotificacion> notifsActa1 = notifRepo.buscarPorActa(idActa1);
            List<FalNotificacion> notifsActa2 = notifRepo.buscarPorActa(idActa2);
            assertThat((long) notifsActa1.size() + notifsActa2.size()).isEqualTo(1);

            // exactamente un intento total con la referencia
            long intentosConRef = 0;
            for (FalNotificacion n : notifsActa1) {
                for (FalNotificacionIntento i : intentoRepo.buscarPorNotificacion(n.getId())) {
                    if (refComun.equals(i.getReferenciaExterna())) intentosConRef++;
                }
            }
            for (FalNotificacion n : notifsActa2) {
                for (FalNotificacionIntento i : intentoRepo.buscarPorNotificacion(n.getId())) {
                    if (refComun.equals(i.getReferenciaExterna())) intentosConRef++;
                }
            }
            assertThat(intentosConRef).isEqualTo(1);

            // exactamente un NOTENV total
            long totalNotenv = eventoRepo.buscarPorActa(idActa1).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count()
                    + eventoRepo.buscarPorActa(idActa2).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count();
            assertThat(totalNotenv).isEqualTo(1);

            // identificar ganador y perdedor por presencia de cabecera
            Long idActaGanador = notifsActa1.isEmpty() ? idActa2 : idActa1;
            Long idActaPerdedor = notifsActa1.isEmpty() ? idActa1 : idActa2;

            // ganador: bloqueActual=NOTI, snapshot presente, un NOTENV
            assertThat(actaRepo.buscarPorId(idActaGanador).orElseThrow().getBloqueActual())
                    .isEqualTo(BloqueActual.NOTI);
            assertThat(snapshotRepo.buscarPorActa(idActaGanador)).isPresent();
            assertThat(eventoRepo.buscarPorActa(idActaGanador).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count()).isEqualTo(1);

            // perdedor: bloqueActual=ANAL, cero cabeceras, cero intentos, cero NOTENV, snapshot ausente
            assertThat(actaRepo.buscarPorId(idActaPerdedor).orElseThrow().getBloqueActual())
                    .isEqualTo(BloqueActual.ANAL);
            assertThat(notifRepo.buscarPorActa(idActaPerdedor)).isEmpty();
            long intentosPerdedor = 0;
            for (FalNotificacion n : notifRepo.buscarPorActa(idActaPerdedor)) {
                intentosPerdedor += intentoRepo.buscarPorNotificacion(n.getId()).size();
            }
            assertThat(intentosPerdedor).isEqualTo(0);
            assertThat(eventoRepo.buscarPorActa(idActaPerdedor).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count()).isEqualTo(0);
            assertThat(snapshotRepo.buscarPorActa(idActaPerdedor)).isNotPresent();
        }
    }

    // =========================================================================
    // 8. Cabecera incompatible (EN_PROCESO)
    // =========================================================================

    @Test
    @DisplayName("8. Cabecera EN_PROCESO activa -> rechazo sin nuevo intento ni evento")
    void cabecera_en_proceso_rechaza() {
        Long idActa = crearActa();
        Long idDoc = crearDocFirmado(idActa);

        // Envio inicial -> cabecera EN_PROCESO
        notifService.enviarNotificacion(cmdPresencial(idActa, idDoc));

        int intentosAntes = intentoRepo.buscarPorNotificacion(
                notifRepo.buscarPorActa(idActa).get(0).getId()).size();
        long eventosAntes = eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count();

        assertThatThrownBy(() -> notifService.enviarNotificacion(cmdPresencial(idActa, idDoc)))
                .isInstanceOf(PrecondicionVioladaException.class);

        // ningun nuevo intento ni evento
        int intentosDespues = intentoRepo.buscarPorNotificacion(
                notifRepo.buscarPorActa(idActa).get(0).getId()).size();
        long eventosDespues = eventoRepo.buscarPorActa(idActa).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.NOTENV).count();
        assertThat(intentosDespues).isEqualTo(intentosAntes);
        assertThat(eventosDespues).isEqualTo(eventosAntes);
    }

    // =========================================================================
    // 9. Validacion antes del reloj
    // =========================================================================

    @Nested
    @DisplayName("9. Instante canonico: reloj llamado exactamente una vez en exito, cero en fallo")
    class RelojCanonico {

        @Test
        @DisplayName("9a. Comando con canal PORTAL -> rechazado antes del reloj")
        void rechazo_previo_no_llama_reloj() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            countingClock.calls = 0;
            assertThatThrownBy(() -> notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, CanalNotificacion.PORTAL_INFRACTOR,
                            null, null, null, ACTOR)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(countingClock.calls).isEqualTo(0);
        }

        @Test
        @DisplayName("9b. Comando valido -> reloj llamado exactamente una vez")
        void exito_llama_reloj_una_vez() {
            Long idActa = crearActa();
            Long idDoc = crearDocFirmado(idActa);
            countingClock.calls = 0;
            notifService.enviarNotificacion(cmdPresencial(idActa, idDoc));
            assertThat(countingClock.calls).isEqualTo(1);
        }
    }
}
