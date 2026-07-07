package ar.gob.malvinas.faltas.core.application.port;

import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;

/**
 * Puerto de proteccion de tokens QR.
 *
 * Abstrae la generacion y validacion del token que protege el acceso
 * al expediente via codigo QR.
 *
 * Contrato:
 *  - generar() produce un token opaco, unico, no predecible.
 *  - resolverUuidTecnico() valida integridad, version y scope; retorna uuidTecnico del acta.
 *  - Si el token es invalido por cualquier razon, lanza QrTokenInvalidoException.
 *  - El token no contiene datos personales, numero de acta en claro, domicilio ni estado.
 *  - El token no debe loguearse completo; usar correlacionId para trazabilidad.
 *
 * Limite de seguridad:
 *  La implementacion de produccion usa AES-GCM-256 con clave configurada via propiedad.
 *  La implementacion de desarrollo/test usa clave efimera aleatoria y NO debe usarse
 *  en produccion. La clave efimera no sobrevive reinicios; los tokens quedan invalidos.
 */
public interface QrTokenProtector {

    /**
     * Genera un token QR protegido para el acta identificada por uuidTecnico.
     *
     * @param uuidTecnico identificador tecnico del acta (no en claro en el token final)
     * @return token opaco, protegido, max 512 chars
     */
    String generar(String uuidTecnico);

    /**
     * Valida el token y extrae el uuidTecnico del acta.
     *
     * @param token token recibido del cliente
     * @return uuidTecnico del acta si el token es valido
     * @throws QrTokenInvalidoException si el token es invalido por cualquier razon
     */
    String resolverUuidTecnico(String token);
}
