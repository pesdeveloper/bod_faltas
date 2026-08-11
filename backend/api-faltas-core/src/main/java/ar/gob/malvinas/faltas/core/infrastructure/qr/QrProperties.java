package ar.gob.malvinas.faltas.core.infrastructure.qr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del QR del acta ({@code DECISION_DDL-ACTA-QR-01},
 * {@code DECISION-QR-INMUTABLE-01}).
 *
 * <pre>
 * faltas.qr.payload-version-activa  version del formato de payload que se emite; hoy 0
 * faltas.qr.secret                  clave de firma HMAC, estable y externa
 * </pre>
 *
 * <p><b>Fail-closed.</b> No hay clave por defecto y no se genera ninguna clave aleatoria:
 * si {@code secret} falta, esta en blanco o no alcanza el minimo tecnico, el arranque falla.
 * Un valor por defecto funcional en la configuracion base seria una clave publica conocida,
 * de modo que un ambiente mal configurado emitiria tokens verificables por cualquiera; la
 * etiqueta de "no productivo" no volveria segura una clave incluida en el repositorio.
 *
 * <p><b>La version es del payload, no de la clave.</b> No hay keyring y no hay rotacion.
 * Mientras exista un unico formato, una version activa distinta de
 * {@link HmacSha256QrTokenProtector#PAYLOAD_VERSION_SOPORTADA} es un error de configuracion
 * y falla como no implementada, en lugar de degradar a un formato desconocido.
 *
 * <p>No expone el material de clave en {@code toString()}.
 */
@ConfigurationProperties(prefix = "faltas.qr")
public class QrProperties {

    /** Version del formato de payload que se emite. Debe ser la soportada. */
    private int payloadVersionActiva;

    /** Clave de firma HMAC-SHA-256, en texto UTF-8. Sin valor por defecto. */
    private String secret;

    public int getPayloadVersionActiva() {
        return payloadVersionActiva;
    }

    public void setPayloadVersionActiva(int payloadVersionActiva) {
        this.payloadVersionActiva = payloadVersionActiva;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Construye el protector validando la configuracion completa. Es el unico punto donde
     * la aplicacion decide que puede emitir QR.
     *
     * @throws IllegalStateException si la version activa no es la implementada o si la clave
     *         falta o es insuficiente; el mensaje nunca incluye la clave
     */
    public HmacSha256QrTokenProtector crearProtector() {
        exigirPayloadVersionSoportada();
        return new HmacSha256QrTokenProtector(secret);
    }

    private void exigirPayloadVersionSoportada() {
        if (payloadVersionActiva != HmacSha256QrTokenProtector.PAYLOAD_VERSION_SOPORTADA) {
            throw new IllegalStateException(
                    "faltas.qr.payload-version-activa=" + payloadVersionActiva
                            + " no esta implementada. La unica version de formato de payload"
                            + " existente es " + HmacSha256QrTokenProtector.PAYLOAD_VERSION_SOPORTADA
                            + "; una version nueva exige una decision normativa"
                            + " (DECISION-QR-INMUTABLE-01).");
        }
    }

    /** Sin material de clave: solo si esta configurada y con que version de payload. */
    @Override
    public String toString() {
        return "QrProperties{payloadVersionActiva=" + payloadVersionActiva
                + ", secretConfigurado=" + (secret != null && !secret.isBlank()) + "}";
    }
}
