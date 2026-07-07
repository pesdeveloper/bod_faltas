package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/**
 * Configuracion del QrTokenProtector.
 *
 * En produccion: configurar faltas.qr.secret con una clave AES-256 codificada en Base64.
 * Ejemplo: faltas.qr.secret=base64-de-32-bytes-aleatorios
 *
 * En desarrollo/test: si la propiedad no esta configurada (o es "EPHEMERO"),
 * se usa una clave efimera aleatoria. Los tokens QR no sobreviven reinicios.
 *
 * ADVERTENCIA: la clave efimera NO debe usarse en produccion.
 */
@Configuration
public class QrConfig {

    @Value("${faltas.qr.secret:}")
    private String secretBase64;

    @Bean
    public QrTokenProtector qrTokenProtector() {
        if (secretBase64 != null && !secretBase64.isBlank() && !"EPHEMERO".equals(secretBase64)) {
            byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
            return new AesGcmQrTokenProtector(keyBytes);
        }
        return new AesGcmQrTokenProtector();
    }
}
