package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion del {@link QrTokenProtector} canonico de las actas.
 *
 * <p>Selecciona {@link HmacSha256QrTokenProtector} con la clave estable y la version de
 * formato de payload que declara {@link QrProperties}. Es el unico bean del puerto: no hay
 * dos candidatos.
 *
 * <p><b>Fail-closed.</b> El arranque falla si {@code faltas.qr.secret} falta, esta en blanco
 * o no alcanza {@link HmacSha256QrTokenProtector#LONGITUD_MINIMA_SECRETO_BYTES} bytes, y
 * tambien si {@code faltas.qr.payload-version-activa} no es la version implementada. No hay
 * clave por defecto en el codigo ni en la configuracion base, y no se genera ninguna clave
 * aleatoria: la clave se inyecta por secreto de ambiente y debe ser estable entre reinicios,
 * porque los QR ya impresos en tickets tienen que seguir verificando
 * ({@code DECISION-QR-INMUTABLE-01}).
 *
 * <p>Los tests usan una clave sintetica {@code TEST_ONLY} declarada en los recursos de test.
 *
 * <p>{@code AesGcmQrTokenProtector} no participa de esta configuracion: es incompatible con
 * el formato canonico y su constructor de clave efimera quedo sin camino productivo.
 */
@Configuration
@EnableConfigurationProperties(QrProperties.class)
public class QrConfig {

    @Bean
    public QrTokenProtector qrTokenProtector(QrProperties propiedades) {
        return propiedades.crearProtector();
    }
}
