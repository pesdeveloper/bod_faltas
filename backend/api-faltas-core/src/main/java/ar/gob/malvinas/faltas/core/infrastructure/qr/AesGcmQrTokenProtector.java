package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Implementacion de QrTokenProtector usando AES/GCM/NoPadding (AEAD).
 *
 * Formato del token:
 *   Base64URL( IV[12] + GCM_Ciphertext + GCM_AuthTag[16] )
 * donde el plaintext es:
 *   {"v":0,"idt":"<uuidTecnico>","nonce":"<32hexchars>","iat":<epochSec>,"scope":"ACTA_QR"}
 *
 * Propiedades de seguridad:
 *  - Confidencialidad: el uuidTecnico y el payload no son visibles sin la clave.
 *  - Autenticidad: la autenticacion GCM garantiza integridad + autenticidad.
 *  - No predecible: nonce de 16 bytes aleatorio + IV GCM de 12 bytes aleatorio por token.
 *  - Sin PII: el payload no contiene nombre, documento, domicilio ni estado del acta.
 *
 * Limite de seguridad (EFIMERO):
 *  Si se usa el constructor sin argumentos, la clave es efimera (random al construir).
 *  Tokens generados con clave efimera no son validos tras reiniciar.
 *  El constructor sin argumentos es SOLO para desarrollo/test.
 *  En produccion, inyectar la clave via application.yml (faltas.qr.secret=base64key).
 */
public class AesGcmQrTokenProtector implements QrTokenProtector {

    private static final String SCOPE = "ACTA_QR";
    private static final int VERSION = 0;
    private static final int MAX_TOKEN_LENGTH = 512;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32; // AES-256

    private final byte[] key;
    private final Clock clock;
    private final SecureRandom rng = new SecureRandom();

    /** Constructor de conveniencia con clave explicita. Usa reloj del sistema.
     * Para production Spring beans usar el constructor inyectable AesGcmQrTokenProtector(byte[], FaltasClock). */
    public AesGcmQrTokenProtector(byte[] key) { this(key, new FaltasClock()); }

    public AesGcmQrTokenProtector(byte[] key, FaltasClock faltasClock) {
        if (key == null || key.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "La clave AES-GCM debe tener exactamente " + KEY_LENGTH_BYTES + " bytes");
        }
        this.key = key.clone();
        this.clock = faltasClock.clock();
    }

    /**
     * Constructor con clave efimera aleatoria.
     * SOLO para uso en desarrollo y tests. No usar en produccion.
     */
    /** Demo/test-only constructor: generates random key and uses system clock. Not for production use. */
    public AesGcmQrTokenProtector() {
        this.key = new byte[KEY_LENGTH_BYTES];
        rng.nextBytes(this.key);
        this.clock = new FaltasClock().clock();
    }

    @Override
    public String generar(String uuidTecnico) {
        if (uuidTecnico == null || uuidTecnico.isBlank()) {
            throw new IllegalArgumentException("uuidTecnico no puede ser nulo/blanco");
        }
        byte[] nonce = new byte[16];
        rng.nextBytes(nonce);
        String nonceHex = HexFormat.of().formatHex(nonce);
        long iat = Instant.now(clock).getEpochSecond();

        String payload = buildPayload(uuidTecnico, nonceHex, iat);
        byte[] cipherBlob = encrypt(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherBlob);
    }

    @Override
    public String resolverUuidTecnico(String token) {
        if (token == null || token.isBlank()) {
            throw new QrTokenInvalidoException("token nulo o vacio");
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            throw new QrTokenInvalidoException("token excede longitud maxima");
        }
        try {
            byte[] cipherBlob = Base64.getUrlDecoder().decode(token);
            byte[] plainBytes = decrypt(cipherBlob);
            String payload = new String(plainBytes, StandardCharsets.UTF_8);
            return extractUuidTecnico(payload);
        } catch (QrTokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new QrTokenInvalidoException("error al descifrar o parsear");
        }
    }

    private byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), paramSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] blob = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, blob, 0, iv.length);
            System.arraycopy(ciphertext, 0, blob, iv.length, ciphertext.length);
            return blob;
        } catch (Exception e) {
            throw new IllegalStateException("Error en cifrado AES-GCM", e);
        }
    }

    private byte[] decrypt(byte[] blob) {
        try {
            if (blob.length < GCM_IV_LENGTH + 16) {
                throw new QrTokenInvalidoException("blob demasiado corto");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(blob, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = new byte[blob.length - GCM_IV_LENGTH];
            System.arraycopy(blob, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec paramSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), paramSpec);
            return cipher.doFinal(ciphertext);
        } catch (QrTokenInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new QrTokenInvalidoException("descifrado fallido");
        }
    }

    private String buildPayload(String uuidTecnico, String nonceHex, long iat) {
        return "{\"v\":" + VERSION
                + ",\"idt\":\"" + uuidTecnico + "\""
                + ",\"nonce\":\"" + nonceHex + "\""
                + ",\"iat\":" + iat
                + ",\"scope\":\"" + SCOPE + "\"}";
    }

    private String extractUuidTecnico(String payload) {
        validateVersion(payload);
        validateScope(payload);
        String idt = extractJsonStringField(payload, "idt");
        if (idt == null || idt.isBlank()) {
            throw new QrTokenInvalidoException("campo idt ausente o vacio");
        }
        return idt;
    }

    private void validateVersion(String payload) {
        String vField = extractJsonNumberField(payload, "v");
        if (!"0".equals(vField)) {
            throw new QrTokenInvalidoException("version desconocida: " + vField);
        }
    }

    private void validateScope(String payload) {
        String scopeField = extractJsonStringField(payload, "scope");
        if (!SCOPE.equals(scopeField)) {
            throw new QrTokenInvalidoException("scope incorrecto");
        }
    }

    private String extractJsonStringField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private String extractJsonNumberField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return end > start ? json.substring(start, end) : null;
    }
}
