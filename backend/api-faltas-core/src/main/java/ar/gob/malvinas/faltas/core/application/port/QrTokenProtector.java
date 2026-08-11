package ar.gob.malvinas.faltas.core.application.port;

import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;

/**
 * Puerto de proteccion de tokens QR.
 *
 * Abstrae la generacion y validacion del token que protege el acceso
 * al expediente via codigo QR.
 *
 * Contrato ({@code DECISION_DDL-ACTA-QR-01}):
 *  - El token es exactamente {@code QR0.<id_tecnico>.<version>.<firma-base64url>}.
 *  - La firma es HMAC-SHA-256 sobre el contenido canonico {@code QR0.<id_tecnico>.<version>},
 *    en Base64 URL-safe sin relleno.
 *  - Es deterministico: el mismo id_tecnico, la misma version y la misma clave producen
 *    exactamente el mismo token. Prohibido nonce, IV, marca temporal, id_acta, relleno
 *    Base64 y cualquier dato variable del entorno.
 *  - La longitud total no supera 128 caracteres, el ancho de {@code fal_acta.codigo_qr}.
 *  - La clave es versionada: la version que el token declara selecciona la clave de
 *    validacion, de modo que rotar la clave no invalida los tokens ya emitidos.
 *  - {@link #resolverUuidTecnico(String)} verifica la firma en tiempo constante y solo
 *    entonces devuelve el uuidTecnico.
 *  - Si el token es invalido por cualquier razon, lanza {@link QrTokenInvalidoException}
 *    con mensaje generico.
 *  - El token no contiene datos personales, id_acta, domicilio ni estado.
 *  - El token no debe loguearse completo; usar correlacionId para trazabilidad.
 *
 * <p>La propiedad que el QR necesita es autenticidad e integridad, no confidencialidad:
 * el id_tecnico es un identificador opaco y puede viajar en claro.
 */
public interface QrTokenProtector {

    /**
     * Emite el token QR del acta identificada por uuidTecnico, firmado con la version
     * activa del keyring.
     *
     * @param uuidTecnico UUID canonico del acta, 36 caracteres en minusculas
     * @return token canonico y version con la que se firmo
     * @throws IllegalArgumentException si uuidTecnico es nulo, blanco o no canonico
     */
    QrTokenEmitido emitir(String uuidTecnico);

    /**
     * Genera un token QR protegido para el acta identificada por uuidTecnico.
     *
     * <p>Conveniencia sobre {@link #emitir(String)} para los llamadores que solo
     * necesitan el token. Cuando el llamador tambien tiene que persistir
     * {@code qr_payload_version}, usar {@link #emitir(String)}: devuelve token y version
     * juntos y evita que se desacoplen.
     *
     * @param uuidTecnico identificador tecnico del acta
     * @return token canonico, max 128 chars
     */
    default String generar(String uuidTecnico) {
        return emitir(uuidTecnico).token();
    }

    /**
     * Valida el token y extrae el uuidTecnico del acta.
     *
     * @param token token recibido del cliente
     * @return uuidTecnico del acta si el token es valido
     * @throws QrTokenInvalidoException si el token es invalido por cualquier razon
     */
    String resolverUuidTecnico(String token);
}
