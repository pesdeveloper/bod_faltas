package ar.gob.malvinas.faltas.core.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Configuracion del JwtDecoder para validacion criptografica local (desarrollo/test).
 *
 * Algoritmo: HS256. Secreto obtenido de configuracion (no hardcodeado).
 * Validaciones: firma, algoritmo, exp, nbf, issuer, audience.
 * Sub: validado en JwtActorFilter (no nulo, no vacio).
 *
 * Para staging/prod: reemplazar el bean por NimbusJwtDecoder.withJwkSetUri(...)
 * o configurar spring.security.oauth2.resourceserver.jwt.issuer-uri
 * sin modificar JwtActorFilter ni los controllers.
 *
 * FIX-FALLO-NOTI-01-R2: JWT local real.
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${faltas.security.jwt.issuer}")
    private String issuer;

    @Value("${faltas.security.jwt.audience}")
    private String audience;

    @Value("${faltas.security.jwt.dev-secret}")
    private String devSecret;

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(
                devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.contains(this.audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token",
                            "El token no contiene la audience esperada: " + this.audience, null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }
}
