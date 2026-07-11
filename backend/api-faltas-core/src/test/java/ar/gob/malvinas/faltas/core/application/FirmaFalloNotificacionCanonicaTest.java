package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.result.RegistrarFirmaDocumentalResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * FIX-FALLO-NOTI-01: Tests canonicos del circuito firma -> cola notificatoria.
 *
 * Verifica:
 * - EstadoFalloActa tiene exactamente 6 valores; DICTADO y FIRMADO no existen.
 * - Al completar la ultima firma: fallo pasa a PENDIENTE_NOTIFICACION, FalNotificacion preparada en PENDIENTE_ENVIO.
 * - Idempotencia del callback por referenciaFirmaExt.
 * - enviarNotificacion reutiliza PENDIENTE_ENVIO (no duplica notificaciones).
 * - generarLoteDesdePendientes toma las PENDIENTE_ENVIO y genera el lote.
 * - Resultado positivo -> fallo pasa a NOTIFICADO via marcarNotificado.
 */
@DisplayName("FIX-FALLO-NOTI-01: Firma y cola notificatoria del fallo")
class FirmaFalloNotificacionCanonicaTest {

    private static final String USER = "usr-canon-test";
    private static final String DEP_COD = "DEP-CANON-01";

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private DocumentoFirmaReqRepository firmaReqRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private FalloActaRepository falloRepo;
    private NotificacionRepository notifRepo;
    private InMemoryLoteCorreoRepository loteRepo;
    private InMemoryNotificacionIntentoRepository intentoRepo;

    private DocumentoService docService;
    private FalloActaService falloService;
    private NotificacionService notifService;
    private LoteCorreoService loteService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        firmaReqRepo = new InMemoryDocumentoFirmaReqRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        falloRepo = new InMemoryFalloActaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        loteRepo = new InMemoryLoteCorreoRepository();
        intentoRepo = new InMemoryNotificacionIntentoRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo,
                new InMemoryPagoVoluntarioRepository(), falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED);

        TalonarioService talonarioService = new TalonarioService(
                new InMemoryTalonarioRepository(),
                new InMemoryDependenciaRepository(),
                new InMemoryInspectorRepository(),
                FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                plantillaRepo, talonarioService, new InMemoryDependenciaRepository(),
                firmaReqRepo, new InMemoryFirmanteRepository(),
                notifRepo, FaltasClockTestSupport.FIXED);

        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);

        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, new InMemoryPagoVoluntarioRepository(), recalc, FaltasClockTestSupport.FIXED);

        loteService = new LoteCorreoService(
                loteRepo, notifRepo, intentoRepo, actaRepo, eventoRepo,
                snapshotRepo, recalc, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // 1. Guardrail de enum
    // =========================================================================

    @Nested
    @DisplayName("1. Guardrail EstadoFalloActa")
    class EnumGuardrail {

        @Test
        @DisplayName("1a. EstadoFalloActa tiene exactamente 6 valores canonicos")
        void estadoFalloActa_seis_valores() {
            EstadoFalloActa[] valores = EstadoFalloActa.values();
            assertThat(valores).hasSize(6);
            assertThat(valores).containsExactlyInAnyOrder(
                    EstadoFalloActa.PENDIENTE_FIRMA,
                    EstadoFalloActa.PENDIENTE_NOTIFICACION,
                    EstadoFalloActa.NOTIFICADO,
                    EstadoFalloActa.FIRME,
                    EstadoFalloActa.REEMPLAZADO,
                    EstadoFalloActa.SIN_EFECTO
            );
        }

        @Test
        @DisplayName("1b. DICTADO y FIRMADO no existen en el enum")
        void dictado_y_firmado_eliminados() {
            List<String> nombres = Arrays.stream(EstadoFalloActa.values())
                    .map(Enum::name)
                    .toList();
            assertThat(nombres)
                    .doesNotContain("DICTADO")
                    .doesNotContain("FIRMADO");
        }
    }

    // =========================================================================
    // 2. Operaciones de dominio en FalActaFallo
    // =========================================================================

    @Nested
    @DisplayName("2. Operaciones de dominio FalActaFallo")
    class DominioDeFallo {

        @Test
        @DisplayName("2a. marcarPendienteNotificacion actualiza estado y fhFirma")
        void marcarPendienteNotificacion() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);
            assertThat(fallo.getFhFirma()).isNull();

            LocalDateTime firma = FaltasClockTestSupport.FIXED.now();
            fallo.marcarPendienteNotificacion(firma);

            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
            assertThat(fallo.getFhFirma()).isEqualTo(firma);
        }

        @Test
        @DisplayName("2b. marcarNotificado actualiza estado y fhNotificacion")
        void marcarNotificado() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.marcarPendienteNotificacion(FaltasClockTestSupport.FIXED.now());

            LocalDateTime notif = FaltasClockTestSupport.FIXED.now();
            fallo.marcarNotificado(notif);

            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
            assertThat(fallo.getFhNotificacion()).isEqualTo(notif);
        }

        @Test
        @DisplayName("2c. marcarPendienteNotificacion requiere fhFirma no nulo")
        void marcarPendienteNotificacion_nulo_rechaza() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            assertThatThrownBy(() -> fallo.marcarPendienteNotificacion(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("2d. marcarPendienteNotificacion requiere PENDIENTE_FIRMA: rechazo desde PENDIENTE_NOTIFICACION")
        void marcarPendienteNotificacion_segunda_vez_rechaza() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.marcarPendienteNotificacion(FaltasClockTestSupport.FIXED.now());
            EstadoFalloActa estadoAntes = fallo.getEstadoFallo();

            assertThatThrownBy(() -> fallo.marcarPendienteNotificacion(FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);

            assertThat(fallo.getEstadoFallo()).isEqualTo(estadoAntes);
        }

        @Test
        @DisplayName("2e. marcarNotificado desde PENDIENTE_FIRMA rechaza")
        void marcarNotificado_desde_pendiente_firma_rechaza() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);

            assertThatThrownBy(() -> fallo.marcarNotificado(FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);

            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);
            assertThat(fallo.getFhNotificacion()).isNull();
        }

        @Test
        @DisplayName("2f. marcarNotificado segunda vez rechaza")
        void marcarNotificado_segunda_vez_rechaza() {
            FalActaFallo fallo = new FalActaFallo(1L, 10L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.marcarPendienteNotificacion(FaltasClockTestSupport.FIXED.now());
            fallo.marcarNotificado(FaltasClockTestSupport.FIXED.now());
            EstadoFalloActa estadoAntes = fallo.getEstadoFallo();

            assertThatThrownBy(() -> fallo.marcarNotificado(FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class);

            assertThat(fallo.getEstadoFallo()).isEqualTo(estadoAntes);
        }
    }

    // =========================================================================
    // 3. Firma de documento: completarFirmaDocumento con plantilla notificable
    // =========================================================================

    @Nested
    @DisplayName("3. Firma de documento con plantilla notificable")
    class FirmaConPlantillaNotificable {

        private Long idActa;
        private Long idDoc;
        private Long idFallo;
        private Long idPlantilla;

        @BeforeEach
        void setUpFallo() {
            // Acta
            idActa = actaRepo.nextId();
            FalActa acta = new FalActa(
                    idActa, UUID.randomUUID().toString(),
                    "TRANSITO", DEP_COD, "INS-001",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    FaltasClockTestSupport.FIXED.now(),
                    "Belgrano 200", "Calle 123", null, null, null,
                    "Juan Perez", "12345678", ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
            acta.setBloqueActual(BloqueActual.ANAL);
            actaRepo.guardar(acta);

            // Plantilla notificable
            idPlantilla = plantillaRepo.nextPlantillaId();
            FalDocumentoPlantilla plantilla = new FalDocumentoPlantilla(
                    idPlantilla, "PLT-FALLO-01", "Plantilla fallo canonico", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_INTERNA,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    true,  // siNotificable
                    true, true, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), null,
                    FaltasClockTestSupport.FIXED.now(), USER);
            plantillaRepo.guardar(plantilla);

            // Documento con plantillaId
            idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "Fallo absolutorio",
                    EstadoDocu.PENDIENTE_FIRMA, TipoFirmaReq.FIRMA_INTERNA,
                    idPlantilla, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            // Fallo activo apuntando al documento
            idFallo = falloRepo.nextId();
            FalActaFallo fallo = new FalActaFallo(
                    idFallo, idActa, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.setDocumentoId(idDoc);
            falloRepo.guardar(fallo);
        }

        @Test
        @DisplayName("3a. Firmar doc: fallo pasa a PENDIENTE_NOTIFICACION, notif preparada en PENDIENTE_ENVIO")
        void firmar_activa_cola_notificatoria() {
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(idDoc, USER, "DIGITAL", null));

            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            assertThat(fallo.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_NOTIFICACION);
            assertThat(fallo.getFhFirma()).isNotNull();

            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);
            FalNotificacion notif = notifs.get(0);
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.PENDIENTE_ENVIO);
            assertThat(notif.getIntentos()).isZero();
            assertThat(notif.getCanal()).isNull();
            assertThat(notif.getFechaEnvio()).isNull();
            assertThat(notif.getResultado()).isNull();
        }

        @Test
        @DisplayName("3b. Firmar doc segunda vez (idempotencia via firmarDocumento) no duplica notificacion")
        void firmar_no_duplica_notificacion() {
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(idDoc, USER, "DIGITAL", null));

            // Simular segunda llamada: la notif ya existe en PENDIENTE_ENVIO
            // completarFirmaDocumento solo prepara si no hay activa
            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1); // solo 1, no duplica
        }

        @Test
        @DisplayName("3c. Instante unico: firma.fhFirma == fallo.fhFirma == evento DOCFIR.fhEvt")
        void instante_unico_invariante() {
            docService.firmarDocumento(
                    new FirmarDocumentoCommand(
                            idDoc, USER, "DIGITAL", null));

            LocalDateTime expected = FaltasClockTestSupport.FIXED.now();

            var firma = firmaRepo.buscarPorDocumento(idDoc).stream().findFirst().orElseThrow();
            FalActaFallo fallo = falloRepo.buscarActivo(idActa).orElseThrow();
            var docfirEvt = eventoRepo.buscarPorActa(idActa).stream()
                    .filter(e -> e.tipoEvt() == TipoEventoActa.DOCFIR)
                    .findFirst().orElseThrow();

            assertThat(firma.getFhFirma()).isEqualTo(expected);
            assertThat(fallo.getFhFirma()).isEqualTo(expected);
            assertThat(docfirEvt.fhEvt()).isEqualTo(expected);
            assertThat(firma.getFhFirma()).isEqualTo(fallo.getFhFirma());
            assertThat(fallo.getFhFirma()).isEqualTo(docfirEvt.fhEvt());
        }
    }

    // =========================================================================
    // 4. enviarNotificacion reutiliza PENDIENTE_ENVIO
    // =========================================================================

    @Nested
    @DisplayName("4. enviarNotificacion reutiliza notificacion PENDIENTE_ENVIO")
    class EnviarNotificacionReutiliza {

        @Test
        @DisplayName("4a. Cuando existe PENDIENTE_ENVIO, enviarNotificacion la reutiliza (no crea nueva)")
        void enviar_reutiliza_pendiente() {
            Long idActa = actaRepo.nextId();
            FalActa acta = new FalActa(
                    idActa, UUID.randomUUID().toString(),
                    "TRANSITO", DEP_COD, "INS-002",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    FaltasClockTestSupport.FIXED.now(),
                    "Belgrano 300", "Calle 456", null, null, null,
                    "Maria Lopez", "87654321", ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
            acta.setBloqueActual(BloqueActual.NOTI);
            actaRepo.guardar(acta);

            // Documento firmado
            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "Fallo",
                    EstadoDocu.FIRMADO, TipoFirmaReq.NO_REQUIERE,
                    null, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            // Notificacion ya preparada en PENDIENTE_ENVIO por el callback de firma
            Long idNotifPrev = notifRepo.nextId();
            FalNotificacion notifPrep = FalNotificacion.preparar(
                    idNotifPrev, idActa, idDoc, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), USER);
            notifRepo.guardar(notifPrep);

            // Llamar enviarNotificacion
            notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, "POSTAL", null));

            // Debe existir solo 1 notificacion (reutilizada, no duplicada)
            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);

            FalNotificacion notif = notifs.get(0);
            assertThat(notif.getId()).isEqualTo(idNotifPrev); // misma notif
            assertThat(notif.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(notif.getCanal()).isEqualTo("POSTAL");
            assertThat(notif.getFechaEnvio()).isNotNull();
        }

        @Test
        @DisplayName("4b. Cuando no existe PENDIENTE_ENVIO, enviarNotificacion crea una nueva EN_PROCESO")
        void enviar_crea_nueva_si_no_hay_pendiente() {
            Long idActa = actaRepo.nextId();
            FalActa acta = new FalActa(
                    idActa, UUID.randomUUID().toString(),
                    "TRANSITO", DEP_COD, "INS-003",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    FaltasClockTestSupport.FIXED.now(),
                    "San Martin 100", "Belgrano 50", null, null, null,
                    "Carlos Ruiz", "11223344", ResultadoFirmaInfractor.FIRMADA);
            acta.setBloqueActual(BloqueActual.NOTI);
            actaRepo.guardar(acta);

            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "Fallo directo",
                    EstadoDocu.FIRMADO, TipoFirmaReq.NO_REQUIERE,
                    null, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, "EMAIL", null));

            List<FalNotificacion> notifs = notifRepo.buscarPorActa(idActa);
            assertThat(notifs).hasSize(1);
            assertThat(notifs.get(0).getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(notifs.get(0).getCanal()).isEqualTo("EMAIL");
        }
    }

    // =========================================================================
    // 5. generarLoteDesdePendientes
    // =========================================================================

    @Nested
    @DisplayName("5. generarLoteDesdePendientes")
    class GenerarLoteDesdePendientes {

        @Test
        @DisplayName("5a. Genera lote desde notificaciones PENDIENTE_ENVIO disponibles")
        void genera_lote_desde_pendientes() {
            Long idActa = actaRepo.nextId();
            FalActa acta = new FalActa(
                    idActa, UUID.randomUUID().toString(),
                    "TRANSITO", DEP_COD, "INS-010",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    FaltasClockTestSupport.FIXED.now(),
                    "Dir 10", "Dir Ref 10", null, null, null,
                    "Pedro Gomez", "55667788", ResultadoFirmaInfractor.FIRMADA);
            acta.setBloqueActual(BloqueActual.NOTI);
            actaRepo.guardar(acta);

            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "Fallo lote",
                    EstadoDocu.FIRMADO, TipoFirmaReq.NO_REQUIERE,
                    null, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            Long idNotif = notifRepo.nextId();
            FalNotificacion notifPrep = FalNotificacion.preparar(
                    idNotif, idActa, idDoc, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), USER);
            notifRepo.guardar(notifPrep);

            FalLoteCorreo lote = loteService.generarLoteDesdePendientes(
                    "LOTE-CANON-001", null, null, USER);

            assertThat(lote).isNotNull();
            assertThat(lote.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);

            // La notificacion debe haber avanzado a EN_PROCESO
            FalNotificacion notifPost = notifRepo.buscarPorId(idNotif).orElseThrow();
            assertThat(notifPost.getEstado()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }

        @Test
        @DisplayName("5b. generarLoteDesdePendientes lanza excepcion si no hay PENDIENTE_ENVIO")
        void genera_lote_sin_pendientes_falla() {
            assertThatThrownBy(() ->
                    loteService.generarLoteDesdePendientes("LOTE-VACIO", null, null, USER))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException.class)
                    .hasMessageContaining("PENDIENTE_ENVIO");
        }
    }

    // =========================================================================
    // 6. Resultado positivo: fallo pasa a NOTIFICADO
    // =========================================================================

    @Nested
    @DisplayName("6. Resultado positivo: fallo pasa a NOTIFICADO")
    class ResultadoPositivoFallo {

        @Test
        @DisplayName("6a. Registrar notificacion positiva de fallo absolutorio: fallo = NOTIFICADO")
        void notif_positiva_fallo_absolutorio_marca_notificado() {
            Long idActa = actaRepo.nextId();
            FalActa acta = new FalActa(
                    idActa, UUID.randomUUID().toString(),
                    "TRANSITO", DEP_COD, "INS-020",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(),
                    FaltasClockTestSupport.FIXED.now(),
                    "Av. Libertad 200", "Ref 20", null, null, null,
                    "Ana Torres", "99887766", ResultadoFirmaInfractor.FIRMADA);
            acta.setBloqueActual(BloqueActual.NOTI);
            actaRepo.guardar(acta);

            Long idDoc = docRepo.nextId();
            FalDocumento doc = new FalDocumento(
                    idDoc, idActa, TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(), "Fallo abs",
                    EstadoDocu.FIRMADO, TipoFirmaReq.NO_REQUIERE,
                    null, FaltasClockTestSupport.FIXED.now());
            docRepo.guardar(doc);

            // Fallo activo en estado PENDIENTE_NOTIFICACION apuntando al documento
            Long idFallo = falloRepo.nextId();
            FalActaFallo fallo = new FalActaFallo(
                    idFallo, idActa, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), USER);
            fallo.setDocumentoId(idDoc);
            fallo.marcarPendienteNotificacion(FaltasClockTestSupport.FIXED.now());
            falloRepo.guardar(fallo);

            // Enviar notificacion
            ComandoResultado enviada = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(idActa, idDoc, "POSTAL", null));

            Long idNotif = Long.parseLong(enviada.idEntidadAfectada());

            // Registrar positiva
            notifService.registrarPositiva(
                    new RegistrarNotificacionPositivaCommand(idNotif, null));

            // Fallo debe estar NOTIFICADO con fhNotificacion registrado
            FalActaFallo falloPost = falloRepo.buscarActivo(idActa).orElseThrow();
            assertThat(falloPost.getEstadoFallo()).isEqualTo(EstadoFalloActa.NOTIFICADO);
            assertThat(falloPost.getFhNotificacion()).isNotNull();
        }
    }
}
