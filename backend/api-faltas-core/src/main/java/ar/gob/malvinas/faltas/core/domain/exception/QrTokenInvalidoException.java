package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Excepcion lanzada cuando un token QR no puede ser resuelto.
 *
 * El mensaje es deliberadamente generico para no revelar informacion sobre
 * el motivo exacto del fallo (token malformado, firma invalida, scope incorrecto,
 * version desconocida, acta inexistente, etc.).
 *
 * No incluir el token ni datos del payload en el mensaje ni en los campos.
 */
public class QrTokenInvalidoException extends RuntimeException {

    public QrTokenInvalidoException() {
        super("Token QR invalido o no resolvible");
    }

    public QrTokenInvalidoException(String contextoTecnico) {
        super("Token QR invalido o no resolvible: " + contextoTecnico);
    }
}
