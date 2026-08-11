package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Implementacion canonica de {@link QrTokenProtector} segun
 * {@code DECISION_DDL-ACTA-QR-01} y {@code DECISION-QR-INMUTABLE-01}.
 *
 * <p>Formato exacto del token:
 *
 * <pre>
 * QR0.&lt;id_tecnico&gt;.&lt;version&gt;.&lt;firma-base64url&gt;
 * </pre>
 *
 * <p>La firma es {@code HMAC-SHA-256} sobre los bytes UTF-8 del contenido canonico
 * {@code QR0.<id_tecnico>.<version>}, codificada en Base64 URL-safe sin relleno. Para la
 * version 0 el total son 86 caracteres: 4 del prefijo con su punto, 36 del UUID, 1
 * separador, 1 de la version, 1 separador y 43 de la firma.
 *
 * <p><b>La version es la del formato del payload</b>, no una version de clave ni un
 * contador de reemisiones. Hoy existe un unico formato, {@link #PAYLOAD_VERSION_SOPORTADA},
 * y esta clase emite y acepta solamente esa version. Un formato futuro exige una decision
 * normativa nueva; anticipar {@code QR1} esta prohibido.
 *
 * <p><b>Una sola clave estable.</b> No hay keyring, no hay version de clave y no hay
 * rotacion: el QR se imprime en el ticket de infraccion y los tickets historicos deben
 * seguir verificando, de modo que la clave de {@code QR0} no cambia entre reinicios ni
 * despliegues. La clave se inyecta por configuracion externa y no existe valor por defecto:
 * si falta o es mas corta que {@link #LONGITUD_MINIMA_SECRETO_BYTES}, la construccion falla
 * y la aplicacion no arranca.
 *
 * <p>Propiedades:
 *
 * <ul>
 *   <li><b>Autenticidad e integridad</b>, que es lo que el QR necesita. No
 *       confidencialidad: el {@code id_tecnico} es un identificador opaco y viaja en
 *       claro.</li>
 *   <li><b>Determinismo</b>: el mismo {@code id_tecnico} con la misma clave produce
 *       exactamente el mismo token. No hay nonce, IV, marca temporal, reloj, aleatoriedad
 *       ni estado mutable.</li>
 *   <li><b>Comparacion en tiempo constante</b> con {@link MessageDigest#isEqual}.</li>
 *   <li><b>Sin PII</b>: el token no transporta documento, nombre, patente, domicilio,
 *       {@code id_acta} ni estado del acta.</li>
 * </ul>
 *
 * <p>Las excepciones son deliberadamente genericas y nunca reflejan el token, la firma ni
 * el material de clave.
 */
public final class HmacSha256QrTokenProtector implements QrTokenProtector {

    /** Prefijo exacto del formato canonico. */
    static final String PREFIJO = "QR0";

    /**
     * Unica version de formato de payload implementada
     * ({@code DECISION-QR-INMUTABLE-01}).
     */
    public static final int PAYLOAD_VERSION_SOPORTADA = 0;

    /** Salida de HMAC-SHA-256 en bytes; la clave no debe ser mas corta que la firma. */
    public static final int LONGITUD_MINIMA_SECRETO_BYTES = 32;

    /** Ancho de {@code fal_acta.codigo_qr}. */
    static final int MAX_TOKEN_LENGTH = 128;

    static final String ALGORITMO = "HmacSHA256";

    /** Segmentos exactos del token canonico: prefijo, uuid, version, firma. */
    private static final int SEGMENTOS = 4;

    /** HMAC-SHA-256 en Base64URL sin relleno: 32 bytes -> 43 caracteres. */
    private static final int LONGITUD_FIRMA = 43;

    private static final char SEPARADOR = '.';

    /** UUID canonico en minusculas, 36 caracteres. Sin llaves, sin mayusculas. */
    private static final Pattern UUID_CANONICO = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    /** UUID canonico admitiendo mayusculas: solo se acepta al emitir, y se normaliza. */
    private static final Pattern UUID_CANONICO_IGNORANDO_CASO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** Decimal canonico sin signo, sin ceros a la izquierda y sin espacios. */
    private static final Pattern VERSION_CANONICA = Pattern.compile("^(0|[1-9][0-9]{0,4})$");

    /** Alfabeto Base64 URL-safe exacto: rechaza relleno, '+' y '/'. */
    private static final Pattern FIRMA_BASE64URL = Pattern.compile("^[A-Za-z0-9_-]{" + LONGITUD_FIRMA + "}$");

    private final SecretKeySpec clave;

    /**
     * @param secreto material de clave en texto UTF-8, obtenido de configuracion externa
     * @throws IllegalStateException si el secreto es nulo, blanco o mas corto que
     *         {@link #LONGITUD_MINIMA_SECRETO_BYTES}; el mensaje no incluye el valor
     */
    public HmacSha256QrTokenProtector(String secreto) {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave de firma del QR: configure faltas.qr.secret."
                            + " No hay valor por defecto y no se genera ninguna clave aleatoria.");
        }
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_SECRETO_BYTES) {
            throw new IllegalStateException(
                    "La clave de firma del QR tiene " + bytes.length + " bytes y el minimo es "
                            + LONGITUD_MINIMA_SECRETO_BYTES + ". No se registra el valor de la clave.");
        }
        this.clave = new SecretKeySpec(bytes, ALGORITMO);
    }

    // =====================================================================
    // Generacion
    // =====================================================================

    @Override
    public QrTokenEmitido emitir(String uuidTecnico) {
        String uuid = exigirUuidCanonico(uuidTecnico);

        String contenido = contenidoCanonico(uuid, PAYLOAD_VERSION_SOPORTADA);
        String token = contenido + SEPARADOR + firmar(contenido);

        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new IllegalStateException(
                    "El token QR generado tiene " + token.length() + " caracteres y el maximo es "
                            + MAX_TOKEN_LENGTH + ". No se registra el token.");
        }
        return new QrTokenEmitido(token, PAYLOAD_VERSION_SOPORTADA);
    }

    /**
     * Normaliza a minusculas de forma explicita y exige la forma canonica de 36
     * caracteres. Un UUID con llaves, sin guiones, truncado o con caracteres no
     * hexadecimales no es canonico y se rechaza.
     */
    private static String exigirUuidCanonico(String uuidTecnico) {
        if (uuidTecnico == null || uuidTecnico.isBlank()) {
            throw new IllegalArgumentException("uuidTecnico no puede ser nulo/blanco");
        }
        if (uuidTecnico.length() != 36 || !UUID_CANONICO_IGNORANDO_CASO.matcher(uuidTecnico).matches()) {
            throw new IllegalArgumentException(
                    "uuidTecnico debe ser un UUID canonico de 36 caracteres");
        }
        return uuidTecnico.toLowerCase(Locale.ROOT);
    }

    private static String contenidoCanonico(String uuidLowercase, int payloadVersion) {
        return PREFIJO + SEPARADOR + uuidLowercase + SEPARADOR + payloadVersion;
    }

    private String firmar(String contenido) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(clave, contenido));
    }

    private static byte[] hmac(SecretKeySpec clave, String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            return mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            // No se propaga el contenido ni la clave: solo que el algoritmo no esta disponible.
            throw new IllegalStateException("No se pudo calcular HMAC-SHA-256 para el token QR", e);
        }
    }

    // =====================================================================
    // Validacion / resolucion
    // =====================================================================

    @Override
    public String resolverUuidTecnico(String token) {
        if (token == null || token.isBlank()) {
            throw new QrTokenInvalidoException("token nulo o vacio");
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new QrTokenInvalidoException("token excede longitud maxima");
        }

        String[] partes = token.split("\\.", -1);
        if (partes.length != SEGMENTOS) {
            throw new QrTokenInvalidoException("formato no canonico");
        }
        if (!PREFIJO.equals(partes[0])) {
            throw new QrTokenInvalidoException("formato no canonico");
        }

        String uuid = partes[1];
        if (!UUID_CANONICO.matcher(uuid).matches()) {
            throw new QrTokenInvalidoException("formato no canonico");
        }

        String versionTexto = partes[2];
        if (!VERSION_CANONICA.matcher(versionTexto).matches()) {
            throw new QrTokenInvalidoException("formato no canonico");
        }
        int payloadVersion = Integer.parseInt(versionTexto);
        if (payloadVersion != PAYLOAD_VERSION_SOPORTADA) {
            throw new QrTokenInvalidoException("version de payload no soportada");
        }

        String firmaRecibida = partes[3];
        if (!FIRMA_BASE64URL.matcher(firmaRecibida).matches()) {
            throw new QrTokenInvalidoException("formato no canonico");
        }

        // El contenido se reconstruye a partir de los segmentos ya validados, no se
        // recorta del token recibido: cualquier alteracion cambia la firma esperada.
        String firmaEsperada = firmar(contenidoCanonico(uuid, payloadVersion));

        boolean verifica = MessageDigest.isEqual(
                firmaEsperada.getBytes(StandardCharsets.US_ASCII),
                firmaRecibida.getBytes(StandardCharsets.US_ASCII));
        if (!verifica) {
            throw new QrTokenInvalidoException("firma no verifica");
        }
        return uuid;
    }

    /** Sin material de clave ni tokens. */
    @Override
    public String toString() {
        return "HmacSha256QrTokenProtector{payloadVersion=" + PAYLOAD_VERSION_SOPORTADA + "}";
    }
}
