package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de emision y resolucion de accesos via QR.
 *
 * <p>Emite el token QR del acta durante el alta y registra los accesos validos para
 * auditoria. No almacena el token, el payload ni el hash del token en ninguna tabla.
 *
 * <p>El token es el canonico de {@code DECISION_DDL-ACTA-QR-01}:
 * {@code QR0.<id_tecnico>.<version>.<firma-base64url>}, firmado con HMAC-SHA-256 sobre el
 * contenido {@code QR0.<id_tecnico>.<version>}. La propiedad que aporta es
 * <b>autenticidad e integridad</b>, no confidencialidad: el {@code id_tecnico} es un
 * identificador opaco y viaja en claro. El token es deterministico y su longitud no supera
 * los 128 caracteres de {@code fal_acta.codigo_qr}.
 *
 * <p><b>API minima y deliberada</b> ({@code DECISION-QR-INMUTABLE-01}). El QR se emite una
 * sola vez, durante el labrado, y despues es inmutable. Este servicio expone solamente:
 *
 * <pre>
 * emitirTokenPara            emision previa al primer guardar del acta
 * registrarAcceso            resolucion y registro de un acceso valido
 * registrarAccesoConNotificacion
 * listarAccesosPorActa       metadatos de auditoria, sin token
 * </pre>
 *
 * No existen {@code rotarQr} ni {@code generarQr}: no hay caso de uso de rotacion,
 * regeneracion, revocacion ni reemplazo individual del QR de un acta, porque el token viaja
 * impreso en el ticket de infraccion y reemitirlo no tendria contraparte material. La
 * version del token es la del <b>formato del payload</b>, no una version de clave, y la
 * clave de firma es unica y estable.
 *
 * Flujo de resolucion de acceso:
 *  1. Validar formato y tamano del token sin reflejar detalles al cliente.
 *  2. Validar autenticidad e integridad recalculando el HMAC en tiempo constante.
 *  3. Validar prefijo, UUID canonico y version de payload soportada.
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
 *
 * <h2>Frontera transaccional del acceso ({@code POST-B2-TX-CONSUMERS-01})</h2>
 *
 * <p>Los pasos 7 a 9 del flujo anterior (insertar {@code FalActaQrAcceso}, registrar el evento
 * {@code QRACCA} y actualizar el snapshot) ocurren dentro de una unica transaccion real,
 * delegada en {@link QrAccesoTransaccionalService}. Esta clase no anota {@code @Transactional}
 * directamente sobre {@link #registrarAcceso} porque {@link #registrarAccesoConNotificacion}
 * lo invoca desde dentro del mismo bean (self-invocation): una anotacion ahi quedaria
 * neutralizada exactamente para ese llamador. El colaborador transaccional resuelve eso.
 */
@Service
public class QrActaService {

    /** Ancho de {@code fal_acta.codigo_qr} ({@code DECISION_DDL-ACTA-QR-01}). */
    private static final int MAX_TOKEN_LENGTH = 128;

    private final ActaRepository actaRepository;
    private final QrAccesoRepository qrAccesoRepository;
    private final QrAccesoTransaccionalService qrAccesoTransaccionalService;
    private final QrTokenProtector tokenProtector;
    private final FaltasClock faltasClock;

    /**
     * @param qrAccesoTransaccionalService colaborador que ejecuta, dentro de una unica
     *                                     transaccion real, la escritura del acceso QR, el
     *                                     evento {@code QRACCA} y el snapshot
     *                                     ({@code POST-B2-TX-CONSUMERS-01}). Se invoca siempre
     *                                     a traves de esta referencia inyectada (un bean
     *                                     distinto), nunca como llamada interna de este mismo
     *                                     servicio, para que la frontera transaccional no quede
     *                                     neutralizada por self-invocation.
     */
    public QrActaService(
            ActaRepository actaRepository,
            QrAccesoRepository qrAccesoRepository,
            QrAccesoTransaccionalService qrAccesoTransaccionalService,
            QrTokenProtector tokenProtector,
            FaltasClock faltasClock) {
        if (qrAccesoTransaccionalService == null) {
            throw new IllegalArgumentException(
                    "qrAccesoTransaccionalService es obligatorio: es la frontera transaccional"
                            + " real del acceso QR.");
        }
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.qrAccesoRepository = qrAccesoRepository;
        this.qrAccesoTransaccionalService = qrAccesoTransaccionalService;
        this.tokenProtector = tokenProtector;
    }

    /**
     * Emite el token QR canonico del acta identificada por {@code uuidTecnico}, sin
     * necesitar la PK y sin escribir nada.
     *
     * <p>Es <b>la unica</b> operacion de emision del servicio y la usa exclusivamente
     * {@code ActaService.labrar}: el token se deriva del {@code id_tecnico} y nunca del
     * {@code id_acta}, que en ese momento todavia no existe
     * ({@code DECISION_DDL-ACTA-QR-01}). Devuelve token y version de payload juntos para que
     * {@code fal_acta.qr_payload_version} coincida con la version firmada dentro del token y
     * no haga falta parsearlo.
     *
     * <p>No existe ninguna operacion posterior que rote, regenere, revoque o reemplace el QR
     * de un acta ya emitida ({@code DECISION-QR-INMUTABLE-01}): el QR se emite una sola vez,
     * durante el labrado, y el agregado rechaza cualquier cambio despues.
     *
     * @param uuidTecnico UUID canonico del acta
     * @return token canonico y version de payload con la que se firmo
     */
    public QrTokenEmitido emitirTokenPara(String uuidTecnico) {
        return tokenProtector.emitir(uuidTecnico);
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
     *
     * <h2>Frontera transaccional</h2>
     *
     * <p>Implementa {@code POST-B2-TX-CONSUMERS-01}. Este metodo <b>no</b> lleva
     * {@code @Transactional}: la validacion del token y la resolucion del acta son lecturas y
     * computo en memoria que no deben abrir una transaccion de escritura. La escritura real
     * (acceso QR + evento {@code QRACCA} + snapshot) ocurre dentro de
     * {@link QrAccesoTransaccionalService#registrarConfirmado}, invocado aca como llamada a un
     * bean distinto —nunca como llamada interna de este servicio— de modo que la frontera es
     * efectiva sea quien sea el llamador, incluido {@link #registrarAccesoConNotificacion}.
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

        return qrAccesoTransaccionalService.registrarConfirmado(
                acta, canal, ipOrigen, userAgent, idCorrel, ahora);
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
     * <h2>Semantica transaccional ({@code POST-B2-TX-CONSUMERS-01})</h2>
     *
     * <p>El acceso QR, el evento {@code QRACCA} y el snapshot forman <b>una unica transaccion</b>
     * (ver {@link #registrarAcceso}), que <b>debe confirmar completamente antes</b> de que
     * comience cualquier notificacion: la llamada a {@code registrarAcceso} retorna solo despues
     * de que {@link QrAccesoTransaccionalService#registrarConfirmado} hizo commit o revirtio, y
     * la colaboracion con {@code notifService} ocurre siempre <b>despues</b> de ese retorno, en
     * una operacion de nivel superior separada.
     *
     * <p>La notificacion es posterior e independiente: si {@code registrarPortalPositivo} falla
     * (por ejemplo por mismatch QR-acta-notificacion), el acceso QR ya confirmado <b>no se
     * revierte</b>. No se usa una unica transaccion para las dos escrituras ni {@code REQUIRES_NEW}
     * para simular independencia: son dos llamadas de nivel superior reales, cada una con su
     * propia frontera transaccional efectiva.
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

    /** Vista segura del resultado de un acceso QR. Sin token, sin payload, sin PII. */
    public record AccesoQrResultado(
            Long idAcceso,
            Long actaId,
            LocalDateTime fhAcceso,
            CanalAccesoQr canal,
            ResultadoAccesoQr resultado
    ) {}
}
