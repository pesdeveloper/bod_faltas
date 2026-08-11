package ar.gob.malvinas.faltas.core.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardrails de {@code DECISION-QR-INMUTABLE-01} sobre <b>codigo ejecutable</b>.
 *
 * <p>Los escaneos excluyen comentarios y Javadoc a proposito: la documentacion sí nombra lo
 * prohibido, justamente para declararlo prohibido, de modo que buscar sobre el texto completo
 * produciria falsos positivos. Son sensibles al caso cuando el patron es un identificador o
 * una sigla, porque una busqueda insensible confunde {@code AES} con {@code aEs} dentro de un
 * identificador como {@code firmaEsperada}.
 */
@DisplayName("B1.5B2-R1: guardrails del API QR minima")
class QrApiMinimaGuardrailTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Path RECURSOS = Paths.get("src/main/resources");

    private static final Path QR_SERVICE = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/application/service/QrActaService.java");
    private static final Path ACTA_SERVICE = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/application/service/ActaService.java");
    private static final Path PROTECTOR = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/qr/HmacSha256QrTokenProtector.java");
    private static final Path QR_CONFIG = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/qr/QrConfig.java");
    private static final Path JDBC_ACTA = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcActaRepository.java");
    private static final Path JDBC_PERSONA = MAIN.resolve(
            "ar/gob/malvinas/faltas/core/infrastructure/jdbc/JdbcPersonaRepository.java");

    /** Lineas de codigo, sin comentarios de linea ni bloques de Javadoc. */
    private static String ejecutable(Path fuente) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String linea : Files.readAllLines(fuente, StandardCharsets.UTF_8)) {
            String t = linea.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            sb.append(linea).append('\n');
        }
        return sb.toString();
    }

    // =====================================================================
    @Nested @DisplayName("Sin rotacion ni regeneracion")
    class SinRotacion {

        @Test @DisplayName("rotarQr esta ausente de todo el codigo ejecutable de produccion")
        void rotarQrAusenteDeProduccion() throws IOException {
            assertThat(ocurrenciasEnProduccion("rotarQr"))
                    .as("no existe caso de uso de rotacion del QR de un acta")
                    .isEmpty();
        }

        @Test @DisplayName("generarQr y las demas puertas de regeneracion estan ausentes")
        void puertasDeRegeneracionAusentes() throws IOException {
            for (String prohibido : new String[] {
                    "generarQr", "regenerarQr", "revocarQr", "reemitirQr", "emitirNuevoQr"}) {
                assertThat(ocurrenciasEnProduccion(prohibido))
                        .as("puerta de regeneracion prohibida: %s", prohibido)
                        .isEmpty();
            }
        }

        @Test @DisplayName("QrActaService conserva solo la emision previa al alta")
        void servicioConservaSoloLaEmision() throws IOException {
            String codigo = ejecutable(QR_SERVICE);
            assertThat(codigo).contains("public QrTokenEmitido emitirTokenPara(");
            assertThat(codigo)
                    .doesNotContain("rotarQr")
                    .doesNotContain("generarQr")
                    .doesNotContain("TipoEventoActa.QRGENA");
        }

        @Test @DisplayName("ninguna clase de produccion emite QRGENA")
        void nadieEmiteQrgena() throws IOException {
            // El enum del catalogo y el mapa de transiciones lo nombran; lo que no debe existir
            // es una emision de evento, que en este codigo se escribe como TipoEventoActa.QRGENA
            // pasado a registrarEvento.
            List<String> emisores = new ArrayList<>();
            for (Path p : fuentesDeProduccion()) {
                String codigo = ejecutable(p);
                if (codigo.contains("registrarEvento") && codigo.contains("TipoEventoActa.QRGENA")) {
                    emisores.add(p.toString());
                }
            }
            assertThat(emisores)
                    .as("QRGENA significa generacion inicial y hoy no lo emite nadie")
                    .isEmpty();
        }
    }

    // =====================================================================
    @Nested @DisplayName("Sin criptografia prohibida")
    class SinCriptografiaProhibida {

        @Test @DisplayName("el protector no usa AES, GCM, Cipher, SecureRandom ni nonce")
        void protectorSinCriptografiaProhibida() throws IOException {
            String codigo = ejecutable(PROTECTOR);
            for (String prohibido : new String[] {
                    "AES", "GCM", "Cipher", "SecureRandom", "Random", "nonce", "Nonce",
                    "timestamp", "Instant", "Clock", "currentTimeMillis", "UUID.randomUUID"}) {
                assertThat(codigo)
                        .as("prohibido en el protector: %s", prohibido)
                        .doesNotContain(prohibido);
            }
        }

        @Test @DisplayName("el protector conserva HMAC, Base64URL sin relleno y comparacion constante")
        void protectorConservaLoCanonico() throws IOException {
            String codigo = ejecutable(PROTECTOR);
            assertThat(codigo)
                    .contains("MessageDigest.isEqual")
                    .contains("Base64.getUrlEncoder().withoutPadding()")
                    .contains("MAX_TOKEN_LENGTH = 128")
                    .contains("PAYLOAD_VERSION_SOPORTADA = 0");
        }

        @Test @DisplayName("la configuracion QR no instancia el protector AES retirado")
        void configuracionSinAes() throws IOException {
            assertThat(ejecutable(QR_CONFIG)).doesNotContain("AesGcmQrTokenProtector");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Sin clave funcional por defecto")
    class SinClaveFallback {

        @Test @DisplayName("ningun recurso de produccion trae una clave QR utilizable")
        void sinClaveFallbackEnRecursos() throws IOException {
            List<String> hallazgos = new ArrayList<>();
            try (var s = Files.walk(RECURSOS)) {
                for (Path p : s.filter(Files::isRegularFile).toList()) {
                    for (String linea : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        String t = linea.trim();
                        if (t.startsWith("#")) continue;
                        if (t.contains("FALTAS_QR_SECRET") && !t.contains("${FALTAS_QR_SECRET:}")) {
                            hallazgos.add(p + " -> " + t);
                        }
                        if (t.contains("FALTAS_QR_CLAVE")) {
                            hallazgos.add(p + " -> keyring legacy: " + t);
                        }
                    }
                }
            }
            assertThat(hallazgos)
                    .as("una clave funcional en la configuracion base seria una clave publica conocida")
                    .isEmpty();
        }

        @Test @DisplayName("ninguna clase de produccion define una clave QR literal por defecto")
        void sinClaveLiteralEnCodigo() throws IOException {
            List<String> hallazgos = new ArrayList<>();
            for (Path p : fuentesDeProduccion()) {
                // CasoUsoFuncionalRunner (el composition root manual que declaraba su propia
                // clave sintetica de demo) se retiro de src/main en Wave 4 Parte 3E-B; ya no
                // puede aparecer en fuentesDeProduccion() y no necesita excepcion.
                String codigo = ejecutable(p);
                if (codigo.contains("faltas-qr-dev-key") || codigo.contains("not-for-prod-use")) {
                    hallazgos.add(p.toString());
                }
            }
            assertThat(hallazgos).isEmpty();
        }
    }

    // =====================================================================
    @Nested @DisplayName("Fronteras preservadas")
    class Fronteras {

        /**
         * Invertido en WAVE 1A2-B2, el slice que implementa {@code DECISION_OBS-TX-01}. La
         * frontera del alta existe y es {@code labrar}; lo que este grupo protege desde B2 es
         * que la demarcacion no se haya movido a la clase ni al camino del QR.
         */
        @Test @DisplayName("@Transactional demarca labrar y no alcanza al camino del QR")
        void fronteraTransaccionalSoloEnLabrar() throws IOException {
            assertThat(ejecutable(ACTA_SERVICE))
                    .as("DECISION_OBS-TX-01 implementada sobre el metodo publico del alta")
                    .contains("@Transactional");
            assertThat(ejecutable(QR_SERVICE))
                    .as("la emision del QR no abre ni participa de una transaccion propia")
                    .doesNotContain("@Transactional");
        }

        @Test @DisplayName("los dos adaptadores JDBC activados son beans solo bajo el perfil jdbc")
        void adaptadoresJdbcActivadosSoloEnPerfilJdbc() throws IOException {
            for (Path p : new Path[] {JDBC_ACTA, JDBC_PERSONA}) {
                String codigo = ejecutable(p);
                assertThat(codigo)
                        .as("%s debe ser bean exclusivamente bajo jdbc", p.getFileName())
                        .contains("@Repository")
                        .contains("@Profile(\"jdbc\")")
                        .doesNotContain("@Primary");
            }
        }

        @Test @DisplayName("el request HTTP no acepta codigo_qr ni id_user_alta")
        void requestSinCamposDeConfianza() throws IOException {
            String request = ejecutable(MAIN.resolve(
                    "ar/gob/malvinas/faltas/core/web/dto/LabrarActaRequest.java"));
            assertThat(request)
                    .doesNotContain("codigoQr")
                    .doesNotContain("qrPayloadVersion")
                    .doesNotContain("idUserAlta");
        }
    }

    // =====================================================================

    private static List<Path> fuentesDeProduccion() throws IOException {
        try (var s = Files.walk(MAIN)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    /** Archivos de produccion cuyo codigo ejecutable contiene el identificador. */
    private static List<String> ocurrenciasEnProduccion(String identificador) throws IOException {
        List<String> encontrados = new ArrayList<>();
        for (Path p : fuentesDeProduccion()) {
            if (ejecutable(p).contains(identificador)) {
                encontrados.add(p.toString());
            }
        }
        return encontrados;
    }
}
