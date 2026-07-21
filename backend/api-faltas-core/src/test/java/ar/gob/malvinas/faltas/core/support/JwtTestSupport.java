package ar.gob.malvinas.faltas.core.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Utilidades de test para crear JWT firmados reales con la clave ficticia de desarrollo.
 *
 * El secreto debe coincidir con la propiedad
 * faltas.security.jwt.dev-secret en application.yml (por defecto TEST_SECRET).
 *
 * No usar en produccion.
 */
public final class JwtTestSupport {

    public static final String TEST_SECRET = "faltas-dev-test-secret-not-for-prod-use";
    public static final String ISSUER = "https://tokenserver.dev.local";
    public static final String AUDIENCE = "api-faltas-core";

    private JwtTestSupport() {}

    /** Crea un token firmado valido con el sub dado, expiracion en 1 hora. */
    public static String token(String sub) {
        return buildToken(sub, ISSUER, AUDIENCE, expiresIn(3600));
    }

    /** Crea un token firmado valido sin campo sub. */
    public static String tokenNoSub() {
        return buildToken(null, ISSUER, AUDIENCE, expiresIn(3600));
    }

    /** Crea un token firmado valido con sub vacio. */
    public static String tokenSubVacio() {
        return buildToken("", ISSUER, AUDIENCE, expiresIn(3600));
    }

    /** Crea un token ya vencido. */
    public static String tokenVencido(String sub) {
        return buildToken(sub, ISSUER, AUDIENCE, expiresIn(-10));
    }

    /** Crea un token con issuer incorrecto. */
    public static String tokenIssuerIncorrecto(String sub) {
        return buildToken(sub, "https://otro-issuer.example.com", AUDIENCE, expiresIn(3600));
    }

    /** Crea un token con audience incorrecta. */
    public static String tokenAudienceIncorrecta(String sub) {
        return buildToken(sub, ISSUER, "otra-audience", expiresIn(3600));
    }

    /** Crea un token con firma incorrecta (firmado con clave distinta). */
    public static String tokenFirmaIncorrecta(String sub) {
        try {
            byte[] wrongKey = "wrong-key-different-from-test-secret-0000".getBytes(StandardCharsets.UTF_8);
            MACSigner signer = new MACSigner(wrongKey);
            JWTClaimsSet claims = claimsBuilder(sub, ISSUER, AUDIENCE, expiresIn(3600)).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error creando token con firma incorrecta", e);
        }
    }

    /** Crea un token alg=none (no firmado). */
    public static String tokenAlgNone(String sub) {
        try {
            com.nimbusds.jwt.PlainJWT plain = new com.nimbusds.jwt.PlainJWT(
                    claimsBuilder(sub, ISSUER, AUDIENCE, expiresIn(3600)).build());
            return plain.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error creando token alg=none", e);
        }
    }

    /** Construye un JwtDecoder de test con el TEST_SECRET y los validadores canonicos. */
    public static JwtDecoder buildTestDecoder() {
        SecretKeySpec key = new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                jwt -> {
                    List<String> aud = jwt.getAudience();
                    if (aud != null && aud.contains(AUDIENCE)) {
                        return OAuth2TokenValidatorResult.success();
                    }
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Invalid audience", null));
                }));
        return decoder;
    }

    // -----------------------------------------------------------------------
    // Helpers internos
    // -----------------------------------------------------------------------

    private static String buildToken(String sub, String issuer, String audience, Date expiry) {
        try {
            MACSigner signer = new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = claimsBuilder(sub, issuer, audience, expiry).build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error creando JWT de test", e);
        }
    }

    private static JWTClaimsSet.Builder claimsBuilder(
            String sub, String issuer, String audience, Date expiry) {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .expirationTime(expiry)
                .issueTime(new Date());
        if (sub != null) b.subject(sub);
        return b;
    }

    private static Date expiresIn(long seconds) {
        return Date.from(Instant.now().plusSeconds(seconds));
    }
}
