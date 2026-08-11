package ar.gob.malvinas.faltas.core.application.port;

/**
 * Token QR recien emitido junto con la version de clave con la que fue firmado.
 *
 * <p>Existe para que {@code codigo_qr} y {@code qr_payload_version} no puedan
 * desacoplarse: el generador devuelve ambos en el mismo valor y el llamador no tiene
 * que parsear el token para averiguar la version que quedo firmada dentro de el
 * ({@code DECISION_DDL-ACTA-QR-01}).
 *
 * <p>Es un value object de aplicacion. No se expone por HTTP y no forma parte de
 * ningun request ni response.
 */
public record QrTokenEmitido(String token, int version) {

    public QrTokenEmitido {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token es obligatorio");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version no puede ser negativa: " + version);
        }
    }
}
