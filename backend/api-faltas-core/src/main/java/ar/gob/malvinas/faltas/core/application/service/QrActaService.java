package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de generacion y resolucion de accesos via QR.
 *
 * Genera tokens QR para actas y registra accesos validos para auditoria.
 * No almacena el token, el payload ni el hash del token en ninguna tabla.
 *
 * Flujo de resolucion de acceso:
 *  1. Validar formato y tamano del token sin reflejar detalles al cliente.
 *  2. Validar integridad y autenticidad (AES-GCM).
 *  3. Validar version y scope del payload.
 *  4. Resolver uuidTecnico -> acta.
 *  5. Consultar estado actual real del acta.
 *  6. Aplicar reglas de visibilidad.
 *  7. Insertar FalActaQrAcceso con ResultadoAccesoQr.VALIDO.
 *  8. Registrar evento QRACC.
 *  9. Actualizar snapshot.
 * 10. Devolver vista segura (solo datos minimos, sin token, sin PII interna).
 *
 * Politica de privacidad:
 *  - El token NO se incluye en logs, excepciones, eventos ni respuestas.
 *  - La IP y el user-agent no se exponen en el resultado devuelto al portal.
 *  - Los errores de resolucion son deliberadamente indistinguibles.
 */
@Service
public class QrActaService {

    private static final int MAX_TOKEN_LENGTH = 512;

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final QrAccesoRepository qrAccesoRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final QrTokenProtector tokenProtector;
    private final FaltasClock faltasClock;

    public QrActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            QrAccesoRepository qrAccesoRepository,
            SnapshotRecalculador snapshotRecalculador,
            QrTokenProtector tokenProtector,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.qrAccesoRepository = qrAccesoRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.tokenProtector = tokenProtector;
    }

    /**
     * Genera un token QR para el acta indicada.
     * Si el acta ya tiene codigoQr asignado, no lo regenera silenciosamente:
     * lanza PrecondicionVioladaException. La rotacion requiere operacion explicita.
     *
     * @param actaId id del acta
     * @param idUser operador que genera el QR
     * @return codigoQr generado (token opaco)
     */
    public String generarQr(Long actaId, String idUser) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(String.valueOf(actaId)));

        if (acta.getCodigoQr() != null && !acta.getCodigoQr().isBlank()) {
            throw new PrecondicionVioladaException(
                    "El acta " + actaId + " ya tiene un QR emitido. Use rotarQr para regenerar.");
        }

        return emitirNuevoQr(acta, idUser);
    }

    /**
     * Rota (regenera) el QR de un acta que ya tenia uno.
     * Requiere el QR anterior para verificar que se conoce.
     * Registra evento QRGEN.
     *
     * @param actaId id del acta
     * @param idUser operador
     * @return nuevo codigoQr
     */
    public String rotarQr(Long actaId, String idUser) {
        FalActa acta = actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(String.valueOf(actaId)));
        return emitirNuevoQr(acta, idUser);
    }

    private String emitirNuevoQr(FalActa acta, String idUser) {
        String nuevoToken = tokenProtector.generar(acta.getUuidTecnico());
        LocalDateTime ahora = faltasClock.now();

        for (int retry = 0; retry < 10; retry++) {
            try {
                FalActa actaActual = actaRepository.buscarPorId(acta.getId()).orElseThrow();
                actaActual.setCodigoQr(nuevoToken);
                actaActual.setFhUltMod(ahora);
                actaActual.setIdUserUltMod(idUser);
                actaRepository.guardar(actaActual);
                break;
            } catch (ConcurrenciaConflictoException e) {
                if (retry == 9) throw e;
            }
        }

        registrarEvento(acta.getId(), TipoEventoActa.QRGEN, null, idUser, "QR de acceso generado");

        FalActa actaFinal = actaRepository.buscarPorId(acta.getId()).orElseThrow();
        FalActaSnapshot snap = snapshotRecalculador.recalcular(actaFinal);
        snapshotRepository.guardar(snap);

        return nuevoToken;
    }

    /**
     * Registra un acceso valido via QR y devuelve vista segura del acta.
     *
     * Si el token es invalido por cualquier razon (corrupcion, scope, version,
     * acta inexistente), lanza QrTokenInvalidoException con mensaje generico.
     * Nunca registra el acceso si la resolucion falla.
     *
     * @param token      token QR recibido del cliente (no se loguea completo)
     * @param canal      canal por el que se accede
     * @param ipOrigen   IP del solicitante (nullable, max 45 chars)
     * @param userAgent  user-agent sanitizado (nullable, max 255 chars)
     * @param idCorrel   identificador de correlacion para trazabilidad (sin incluir token)
     * @return resultado del acceso registrado
     */
    public AccesoQrResultado registrarAcceso(
            String token,
            CanalAccesoQr canal,
            String ipOrigen,
            String userAgent,
            String idCorrel) {

        validarFormatoToken(token);
        if (canal == null) throw new QrTokenInvalidoException("canal no puede ser null");

        String uuidTecnico = tokenProtector.resolverUuidTecnico(token);

        FalActa acta = actaRepository.buscarPorUuidTecnico(uuidTecnico)
                .orElseThrow(QrTokenInvalidoException::new);

        LocalDateTime ahora = faltasClock.now();

        Long accId = qrAccesoRepository.nextId();
        FalActaQrAcceso acceso = new FalActaQrAcceso(
                accId, acta.getId(), ahora, canal, ipOrigen, userAgent,
                ResultadoAccesoQr.VALIDO, ahora);
        qrAccesoRepository.registrar(acceso);

        registrarEvento(acta.getId(), TipoEventoActa.QRACC, null,
                "PORTAL", "Acceso QR registrado [correl=" + (idCorrel != null ? idCorrel : "?") + "]");

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return new AccesoQrResultado(accId, acta.getId(), ahora, canal, ResultadoAccesoQr.VALIDO);
    }

    /**
     * Registra un acceso QR valido con notificacion por visualizacion de pieza.
     *
     * Si el usuario visualiza una pieza notificable y se cumplen reglas de portal,
     * delega en NotificacionIntentoService para registrar notificacion positiva.
     * El acceso QR es append-only e independiente del resultado notificatorio:
     * queda registrado aunque la notificacion falle por mismatch u otro error.
     *
     * La unica autoridad para actaIdQrEsperada es el resultado de resolver el token
     * protegido; nunca proviene del caller como parametro externo independiente.
     *
     * @param token              token QR
     * @param canal              canal de acceso
     * @param ipOrigen           IP del solicitante (nullable)
     * @param userAgent          user-agent (nullable)
     * @param idCorrel           correlacion
     * @param notificacionId     id de la notificacion a cerrar como positiva (nullable)
     * @param destinoPortal      identificador del usuario en el portal (para el intento)
     * @param idUser             operador del sistema que ejecuta la operacion
     * @param notifService       servicio de notificacion; obligatorio cuando notificacionId
     *                           es no null; null solo si notificacionId es null
     * @return resultado del acceso QR
     * @throws IllegalArgumentException si notificacionId es no null y notifService es null
     */
    public AccesoQrResultado registrarAccesoConNotificacion(
            String token,
            CanalAccesoQr canal,
            String ipOrigen,
            String userAgent,
            String idCorrel,
            Long notificacionId,
            String destinoPortal,
            String idUser,
            NotificacionIntentoService notifService) {

        if (notificacionId != null && notifService == null) {
            throw new IllegalArgumentException(
                    "notifService es obligatorio cuando notificacionId esta informado");
        }

        AccesoQrResultado resultado = registrarAcceso(token, canal, ipOrigen, userAgent, idCorrel);

        if (notificacionId != null) {
            notifService.registrarPortalPositivo(
                    notificacionId, resultado.actaId(), destinoPortal, idUser);
        }

        return resultado;
    }

    /**
     * Lista los accesos registrados para un acta (solo metadatos, sin token ni payload).
     */
    public List<FalActaQrAcceso> listarAccesosPorActa(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(String.valueOf(actaId)));
        return qrAccesoRepository.listarPorActa(actaId);
    }

    private void validarFormatoToken(String token) {
        if (token == null || token.isBlank()) {
            throw new QrTokenInvalidoException();
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new QrTokenInvalidoException();
        }
    }

    private void registrarEvento(Long actaId, TipoEventoActa tipo, Long idDocuRel, String idUser, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(actaId)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.SISTEMA_QR)
                .fhEvt(faltasClock.now())
                .idDocuRel(idDocuRel)
                .idUserEvt(idUser)
                .actorTipo(idUser != null && !"SYS".equals(idUser) ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    /** Vista segura del resultado de un acceso QR. Sin token, sin payload, sin PII. */
    public record AccesoQrResultado(
            Long idAcceso,
            Long actaId,
            LocalDateTime fhAcceso,
            CanalAccesoQr canal,
            ResultadoAccesoQr resultado
    ) {}
}
