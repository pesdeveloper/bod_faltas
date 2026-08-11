package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.port.QrTokenProtector;
import ar.gob.malvinas.faltas.core.domain.exception.QrTokenInvalidoException;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Matriz de verificacion de {@code DECISION_DDL-ACTA-QR-01} sobre el protector canonico.
 *
 * <p>Todas las claves son TEST_ONLY: literales sinteticos fijos definidos en
 * {@link QrTestSupport}. No provienen del entorno local y no son secretos.
 */
@DisplayName("B1.5B2: HmacSha256QrTokenProtector — token QR canonico")
class HmacSha256QrTokenProtectorTest {

    private static final String UUID_A = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
    private static final String UUID_B = "b1c2d3e4-f5a6-4b7c-8d9e-0f1a2b3c4d5e";

    private final QrTokenProtector v0 = QrTestSupport.protector();

    // =====================================================================
    @Nested @DisplayName("Generacion")
    class Generacion {

        @Test @DisplayName("1. mismo UUID + version + clave produce exactamente el mismo token")
        void deterministico() {
            assertThat(v0.generar(UUID_A)).isEqualTo(v0.generar(UUID_A));
            assertThat(v0.emitir(UUID_A)).isEqualTo(v0.emitir(UUID_A));
        }

        @Test @DisplayName("2. UUID distinto produce token distinto")
        void uuidDistintoTokenDistinto() {
            assertThat(v0.generar(UUID_A)).isNotEqualTo(v0.generar(UUID_B));
        }

        @Test @DisplayName("3. clave distinta produce firma distinta")
        void claveDistintaFirmaDistinta() {
            String a = v0.generar(UUID_A);
            String b = QrTestSupport.protectorAlternativo().generar(UUID_A);
            assertThat(a).isNotEqualTo(b);
            assertThat(contenido(a))
                    .as("solo cambia la firma: el contenido canonico es el mismo")
                    .isEqualTo(contenido(b));
        }

        /**
         * Contraparte de la matriz original, ahora bajo {@code DECISION-QR-INMUTABLE-01}: la
         * version es la del formato del payload y existe un unico formato, de modo que la
         * emision no puede producir otra version. La verificacion de que una version distinta
         * cambia el contenido firmado se hace del lado de la validacion, que es donde importa.
         */
        @Test @DisplayName("4. la emision usa siempre la unica version de payload implementada")
        void emisionUsaLaUnicaVersionImplementada() {
            QrTokenEmitido emitido = v0.emitir(UUID_A);
            assertThat(emitido.version()).isZero();
            assertThat(emitido.token().split("\\.")[2]).isEqualTo("0");

            // Una version distinta en el token cambia el contenido firmado: la firma del
            // token de version 0 no puede verificar sobre un contenido de version 1.
            String falsificado = "QR0." + UUID_A + ".1." + firma(emitido.token());
            assertThatThrownBy(() -> v0.resolverUuidTecnico(falsificado))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("5. formato exacto QR0.uuid.version.firma")
        void formatoExacto() {
            String token = v0.generar(UUID_A);
            String[] partes = token.split("\\.");
            assertThat(partes).hasSize(4);
            assertThat(partes[0]).isEqualTo("QR0");
            assertThat(partes[1]).isEqualTo(UUID_A);
            assertThat(partes[2]).isEqualTo("0");
            assertThat(partes[3]).matches("^[A-Za-z0-9_-]{43}$");
        }

        @Test @DisplayName("6. el UUID del token es el canonico en minusculas")
        void uuidLowercaseCanonico() {
            String enMayusculas = UUID_A.toUpperCase(java.util.Locale.ROOT);
            QrTokenEmitido emitido = v0.emitir(enMayusculas);
            assertThat(emitido.token().split("\\.")[1])
                    .as("la generacion normaliza a minusculas de forma explicita")
                    .isEqualTo(UUID_A);
            assertThat(emitido.token())
                    .as("el token de un UUID en mayusculas es el mismo que el del canonico")
                    .isEqualTo(v0.generar(UUID_A));
        }

        @Test @DisplayName("7. la version 0 produce 86 caracteres, dentro de VARCHAR(128)")
        void longitudDentroDe128() {
            assertThat(v0.generar(UUID_A)).hasSize(86);
            assertThat(v0.generar(UUID_A).length()).isLessThanOrEqualTo(128);
        }

        @Test @DisplayName("8. la firma es Base64URL sin padding de 43 caracteres")
        void firmaBase64UrlSinPadding() {
            String firma = v0.generar(UUID_A).split("\\.")[3];
            assertThat(firma).hasSize(43)
                    .doesNotContain("=")
                    .doesNotContain("+")
                    .doesNotContain("/");
        }

        @Test @DisplayName("21. el token no contiene id_acta ni PII")
        void sinIdActaNiPii() {
            String token = v0.generar(UUID_A);
            assertThat(token)
                    .doesNotContain("id_acta")
                    .doesNotContain("idActa")
                    .doesNotContain("nombre")
                    .doesNotContain("documento")
                    .doesNotContain("domicilio")
                    .doesNotContain("patente");
            assertThat(token.split("\\."))
                    .as("el token solo transporta prefijo, identidad tecnica, version y firma")
                    .hasSize(4);
        }

        @Test @DisplayName("22. la generacion no depende del reloj: dos emisiones separadas coinciden")
        void noDependeDelReloj() throws InterruptedException {
            String primero = v0.generar(UUID_A);
            Thread.sleep(5);
            assertThat(v0.generar(UUID_A)).isEqualTo(primero);
        }

        @Test @DisplayName("uuidTecnico nulo, blanco o no canonico se rechaza al generar")
        void uuidInvalidoAlGenerar() {
            assertThatThrownBy(() -> v0.emitir(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> v0.emitir("   ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> v0.emitir("no-es-uuid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("canonico");
            assertThatThrownBy(() -> v0.emitir(UUID_A.replace("-", "")))
                    .as("un UUID sin guiones no es canonico")
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> v0.emitir("{" + UUID_A + "}"))
                    .as("un UUID con llaves no es canonico")
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =====================================================================
    @Nested @DisplayName("Validacion y resolucion")
    class Validacion {

        @Test @DisplayName("9. un token valido resuelve el UUID")
        void tokenValidoResuelve() {
            assertThat(v0.resolverUuidTecnico(v0.generar(UUID_A))).isEqualTo(UUID_A);
        }

        @Test @DisplayName("10. UUID alterado se rechaza")
        void uuidAlteradoSeRechaza() {
            String token = v0.generar(UUID_A);
            String alterado = token.replace(UUID_A, UUID_B);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(alterado))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("11. version alterada se rechaza como version de payload no soportada")
        void versionAlteradaSeRechaza() {
            String conVersionCambiada = "QR0." + UUID_A + ".1." + firma(v0.generar(UUID_A));
            assertThatThrownBy(() -> v0.resolverUuidTecnico(conVersionCambiada))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageContaining("version de payload no soportada");
        }

        @Test @DisplayName("12. firma alterada se rechaza")
        void firmaAlteradaSeRechaza() {
            String token = v0.generar(UUID_A);
            String f = firma(token);
            char c = f.charAt(10);
            String alterada = f.substring(0, 10) + (c == 'X' ? 'Y' : 'X') + f.substring(11);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(contenido(token) + "." + alterada))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("13. firma truncada se rechaza")
        void firmaTruncadaSeRechaza() {
            String token = v0.generar(UUID_A);
            String truncada = firma(token).substring(0, 42);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(contenido(token) + "." + truncada))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageContaining("formato no canonico");
        }

        @Test @DisplayName("14. relleno '=' se rechaza")
        void paddingSeRechaza() {
            String token = v0.generar(UUID_A);
            String conPadding = firma(token).substring(0, 42) + "=";
            assertThatThrownBy(() -> v0.resolverUuidTecnico(contenido(token) + "." + conPadding))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("15. Base64 standard con '+' o '/' se rechaza")
        void base64StandardSeRechaza() {
            String token = v0.generar(UUID_A);
            String conMas = "+" + firma(token).substring(1);
            String conBarra = "/" + firma(token).substring(1);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(contenido(token) + "." + conMas))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(contenido(token) + "." + conBarra))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("16. prefijo distinto de QR0 se rechaza")
        void prefijoDistintoSeRechaza() {
            String token = v0.generar(UUID_A);
            assertThatThrownBy(() -> v0.resolverUuidTecnico("QR1" + token.substring(3)))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThatThrownBy(() -> v0.resolverUuidTecnico("qr0" + token.substring(3)))
                    .as("el prefijo es exacto y sensible al caso")
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("17. cantidad de segmentos distinta de 4 se rechaza")
        void segmentosIncorrectosSeRechazan() {
            assertThatThrownBy(() -> v0.resolverUuidTecnico("QR0." + UUID_A + ".0"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(v0.generar(UUID_A) + ".extra"))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThatThrownBy(() -> v0.resolverUuidTecnico("QR0.." + UUID_A + ".0"))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        /**
         * Existe un unico formato de payload. Cualquier otra version se rechaza antes de
         * intentar verificar la firma, y el mensaje no puede sugerir que exista un QR1.
         */
        @Test @DisplayName("18. una version de payload distinta de 0 se rechaza sin verificar la firma")
        void versionNoSoportadaSeRechaza() {
            for (String version : new String[] {"1", "2", "99", "12345"}) {
                String candidato = "QR0." + UUID_A + "." + version + "." + firma(v0.generar(UUID_A));
                assertThatThrownBy(() -> v0.resolverUuidTecnico(candidato))
                        .as("version de payload %s", version)
                        .isInstanceOf(QrTokenInvalidoException.class)
                        .hasMessageContaining("version de payload no soportada");
            }
        }

        @Test @DisplayName("18b. version no canonica se rechaza")
        void versionNoCanonicaSeRechaza() {
            String token = v0.generar(UUID_A);
            for (String version : new String[] {"00", "01", "+0", "-1", "0x0", " 0", ""}) {
                String candidato = "QR0." + UUID_A + "." + version + "." + firma(token);
                assertThatThrownBy(() -> v0.resolverUuidTecnico(candidato))
                        .as("version no canonica: '%s'", version)
                        .isInstanceOf(QrTokenInvalidoException.class);
            }
        }

        @Test @DisplayName("19. UUID no canonico se rechaza, incluido el canonico en mayusculas")
        void uuidNoCanonicoSeRechaza() {
            String token = v0.generar(UUID_A);
            String enMayusculas = "QR0." + UUID_A.toUpperCase(java.util.Locale.ROOT)
                    + ".0." + firma(token);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(enMayusculas))
                    .as("la validacion no normaliza: el contenido firmado es exacto")
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageContaining("formato no canonico");
        }

        @Test @DisplayName("20. token de mas de 128 caracteres se rechaza por longitud")
        void tokenMayorA128SeRechaza() {
            String largo = "QR0." + UUID_A + ".0." + "a".repeat(100);
            assertThat(largo.length()).isGreaterThan(128);
            assertThatThrownBy(() -> v0.resolverUuidTecnico(largo))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .hasMessageContaining("excede longitud maxima");
        }

        @Test @DisplayName("token nulo o blanco se rechaza")
        void nuloOBlancoSeRechaza() {
            assertThatThrownBy(() -> v0.resolverUuidTecnico(null))
                    .isInstanceOf(QrTokenInvalidoException.class);
            assertThatThrownBy(() -> v0.resolverUuidTecnico("   "))
                    .isInstanceOf(QrTokenInvalidoException.class);
        }

        @Test @DisplayName("24. ni el token ni la clave se filtran en las excepciones")
        void sinFiltracionEnExcepciones() {
            String token = v0.generar(UUID_A);
            String manipulado = contenido(token) + "." + ("A" + firma(token).substring(1));
            assertThatThrownBy(() -> v0.resolverUuidTecnico(manipulado))
                    .isInstanceOf(QrTokenInvalidoException.class)
                    .satisfies(e -> {
                        assertThat(e.getMessage()).doesNotContain(manipulado);
                        assertThat(e.getMessage()).doesNotContain(firma(token));
                        assertThat(e.getMessage()).doesNotContain(QrTestSupport.SECRETO_TEST_ONLY);
                    });
        }

        @Test @DisplayName("toString del protector no expone material de clave")
        void toStringSinClaves() {
            assertThat(v0.toString())
                    .doesNotContain(QrTestSupport.SECRETO_TEST_ONLY)
                    .contains("payloadVersion=0");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Clave unica, estable y fail-closed")
    class Clave {

        @Test @DisplayName("sin clave configurada el protector no se construye")
        void sinClaveFalla() {
            assertThatThrownBy(() -> new HmacSha256QrTokenProtector(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("faltas.qr.secret");
        }

        @Test @DisplayName("una clave en blanco cuenta como ausente")
        void claveEnBlancoEsAusente() {
            assertThatThrownBy(() -> new HmacSha256QrTokenProtector("   "))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("faltas.qr.secret");
            assertThatThrownBy(() -> new HmacSha256QrTokenProtector(""))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("una clave mas corta que la salida HMAC no se acepta")
        void claveCortaFalla() {
            assertThatThrownBy(() -> new HmacSha256QrTokenProtector("corta"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("minimo");
        }

        @Test @DisplayName("una clave corta no aparece en el mensaje de error")
        void claveCortaNoSeFiltra() {
            String claveCorta = "clave-corta-identificable";
            assertThatThrownBy(() -> new HmacSha256QrTokenProtector(claveCorta))
                    .isInstanceOf(IllegalStateException.class)
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(claveCorta));
        }

        @Test @DisplayName("una clave de exactamente el minimo tecnico se acepta")
        void claveDelMinimoExactoSeAcepta() {
            String justa = "x".repeat(HmacSha256QrTokenProtector.LONGITUD_MINIMA_SECRETO_BYTES);
            QrTokenProtector prot = new HmacSha256QrTokenProtector(justa);
            assertThat(prot.resolverUuidTecnico(prot.generar(UUID_A))).isEqualTo(UUID_A);
        }

        /**
         * La clave debe ser estable: los QR ya impresos en tickets tienen que seguir
         * verificando. Dos protectores construidos por separado con la misma clave producen
         * y validan exactamente el mismo token, que es lo que hace que un reinicio no
         * invalide los historicos.
         */
        @Test @DisplayName("la misma clave en dos instancias produce y valida el mismo token")
        void claveEstableEntreInstancias() {
            QrTokenProtector uno = new HmacSha256QrTokenProtector(QrTestSupport.SECRETO_TEST_ONLY);
            QrTokenProtector otro = new HmacSha256QrTokenProtector(QrTestSupport.SECRETO_TEST_ONLY);

            String token = uno.generar(UUID_A);
            assertThat(otro.generar(UUID_A)).isEqualTo(token);
            assertThat(otro.resolverUuidTecnico(token)).isEqualTo(UUID_A);
        }

        @Test @DisplayName("no hay version de clave: solo la version del formato de payload")
        void noHayVersionDeClave() {
            assertThat(HmacSha256QrTokenProtector.PAYLOAD_VERSION_SOPORTADA).isZero();
            assertThat(v0.emitir(UUID_A).version())
                    .as("la version emitida es la del formato del payload")
                    .isEqualTo(HmacSha256QrTokenProtector.PAYLOAD_VERSION_SOPORTADA);
        }
    }

    // =====================================================================
    @Nested @DisplayName("Escaneo del codigo fuente del protector")
    class EscaneoFuente {

        private static final Path FUENTE = Paths.get(
                "src/main/java/ar/gob/malvinas/faltas/core/infrastructure/qr/HmacSha256QrTokenProtector.java");

        private String codigoEjecutable() throws Exception {
            StringBuilder sb = new StringBuilder();
            for (String linea : Files.readAllLines(FUENTE, StandardCharsets.UTF_8)) {
                String t = linea.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
                sb.append(linea).append('\n');
            }
            return sb.toString();
        }

        @Test @DisplayName("23. la comparacion de la firma usa MessageDigest.isEqual")
        void comparacionConstante() throws Exception {
            String codigo = codigoEjecutable();
            assertThat(codigo)
                    .as("la comparacion de la firma debe ser en tiempo constante")
                    .contains("MessageDigest.isEqual");
            assertThat(codigo)
                    .as("prohibido comparar la firma con equals o Arrays.equals")
                    .doesNotContain("firmaRecibida.equals")
                    .doesNotContain("Arrays.equals");
        }

        @Test @DisplayName("no usa AES, GCM ni cifrado reversible")
        void sinAesNiGcm() throws Exception {
            String codigo = codigoEjecutable();
            assertThat(codigo)
                    .doesNotContain("AES")
                    .doesNotContain("GCM")
                    .doesNotContain("Cipher");
        }

        @Test @DisplayName("no usa reloj, aleatoriedad ni UUID.randomUUID")
        void sinRelojNiAleatoriedad() throws Exception {
            String codigo = codigoEjecutable();
            assertThat(codigo)
                    .doesNotContain("SecureRandom")
                    .doesNotContain("Random")
                    .doesNotContain("Instant")
                    .doesNotContain("Clock")
                    .doesNotContain("FaltasClock")
                    .doesNotContain("currentTimeMillis")
                    .doesNotContain("UUID.randomUUID");
        }

        @Test @DisplayName("usa Base64 URL-safe sin relleno y el limite de 128")
        void base64UrlYLimite() throws Exception {
            String codigo = codigoEjecutable();
            assertThat(codigo).contains("Base64.getUrlEncoder().withoutPadding()");
            assertThat(codigo).contains("MAX_TOKEN_LENGTH = 128");
            assertThat(codigo)
                    .as("prohibido Base64 MIME o standard")
                    .doesNotContain("getMimeEncoder")
                    .doesNotContain("getEncoder()");
        }

        @Test @DisplayName("el contenido firmado no incluye id_acta")
        void contenidoFirmadoSinIdActa() throws Exception {
            assertThat(codigoEjecutable())
                    .doesNotContain("idActa")
                    .doesNotContain("id_acta");
        }
    }

    private static String contenido(String token) {
        return token.substring(0, token.lastIndexOf('.'));
    }

    private static String firma(String token) {
        return token.substring(token.lastIndexOf('.') + 1);
    }
}
