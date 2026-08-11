package ar.gob.malvinas.faltas.core.infrastructure.qr;

import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La configuracion del QR es <b>fail-closed</b>: sin una clave valida la aplicacion no
 * arranca ({@code DECISION-QR-INMUTABLE-01}).
 *
 * <p>Son pruebas unitarias deliberadamente: no levantan contexto de Spring y no dependen de
 * ninguna variable del equipo, de modo que verifican la regla y no el ambiente. Las claves
 * son literales sinteticos {@code TEST_ONLY}.
 */
@DisplayName("B1.5B2-R1: configuracion QR fail-closed")
class QrConfigFailClosedTest {

    private static QrProperties propiedades(int payloadVersion, String secret) {
        QrProperties p = new QrProperties();
        p.setPayloadVersionActiva(payloadVersion);
        p.setSecret(secret);
        return p;
    }

    // =====================================================================
    @Nested @DisplayName("Clave de firma")
    class Clave {

        @Test @DisplayName("1. sin clave configurada el arranque falla")
        void sinClaveFalla() {
            assertThatThrownBy(() -> propiedades(0, null).crearProtector())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("faltas.qr.secret");
        }

        @Test @DisplayName("2. clave en blanco falla")
        void claveEnBlancoFalla() {
            assertThatThrownBy(() -> propiedades(0, "").crearProtector())
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> propiedades(0, "     ").crearProtector())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("faltas.qr.secret");
        }

        @Test @DisplayName("3. clave demasiado corta falla y no se registra su valor")
        void claveCortaFalla() {
            String corta = QrTestSupport.SECRETO_DEMASIADO_CORTO_TEST_ONLY;
            assertThatThrownBy(() -> propiedades(0, corta).crearProtector())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("minimo")
                    .satisfies(e -> assertThat(e.getMessage()).doesNotContain(corta));
        }

        @Test @DisplayName("4. clave valida permite construir el protector y emitir")
        void claveValidaConstruye() {
            HmacSha256QrTokenProtector protector =
                    propiedades(0, QrTestSupport.SECRETO_TEST_ONLY).crearProtector();
            String uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
            assertThat(protector.emitir(uuid).token()).hasSize(86);
            assertThat(protector.resolverUuidTecnico(protector.emitir(uuid).token())).isEqualTo(uuid);
        }

        @Test @DisplayName("8. no se genera ninguna clave aleatoria: dos construcciones coinciden")
        void sinClaveAleatoria() {
            String uuid = "3f2504e0-4f89-41d3-9a0c-0305e82c3301";
            String a = propiedades(0, QrTestSupport.SECRETO_TEST_ONLY).crearProtector().emitir(uuid).token();
            String b = propiedades(0, QrTestSupport.SECRETO_TEST_ONLY).crearProtector().emitir(uuid).token();
            assertThat(a)
                    .as("una clave efimera aleatoria haria que los dos tokens difirieran")
                    .isEqualTo(b);
        }

        @Test @DisplayName("las propiedades no exponen el material de clave en toString")
        void toStringSinClave() {
            assertThat(propiedades(0, QrTestSupport.SECRETO_TEST_ONLY).toString())
                    .doesNotContain(QrTestSupport.SECRETO_TEST_ONLY)
                    .contains("secretConfigurado=true");
            assertThat(propiedades(0, null).toString()).contains("secretConfigurado=false");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Version del formato de payload")
    class PayloadVersion {

        @Test @DisplayName("5. una version activa distinta de 0 falla como no implementada")
        void versionDistintaDeCeroFalla() {
            for (int version : new int[] {1, 2, 7, 99}) {
                assertThatThrownBy(() -> propiedades(version, QrTestSupport.SECRETO_TEST_ONLY).crearProtector())
                        .as("payload version %s", version)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("no esta implementada");
            }
        }

        @Test @DisplayName("una version negativa tambien falla")
        void versionNegativaFalla() {
            assertThatThrownBy(() -> propiedades(-1, QrTestSupport.SECRETO_TEST_ONLY).crearProtector())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no esta implementada");
        }

        @Test @DisplayName("la version 0 es la unica soportada y es la que se emite")
        void versionCeroEsLaUnica() {
            assertThat(HmacSha256QrTokenProtector.PAYLOAD_VERSION_SOPORTADA).isZero();
            assertThatCode(() -> propiedades(0, QrTestSupport.SECRETO_TEST_ONLY).crearProtector())
                    .doesNotThrowAnyException();
        }

        @Test @DisplayName("la version se valida antes que la clave: el orden no oculta el error")
        void versionSeValidaAntesQueLaClave() {
            assertThatThrownBy(() -> propiedades(1, null).crearProtector())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no esta implementada");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Arranque del contexto de Spring")
    class ArranqueSpring {

        /** Contexto minimo: solo la configuracion QR, sin la aplicacion completa. */
        private final ApplicationContextRunner contexto = new ApplicationContextRunner()
                .withUserConfiguration(QrConfig.class);

        @Test @DisplayName("sin clave el contexto no arranca")
        void sinClaveElContextoFalla() {
            contexto.withPropertyValues(
                            "faltas.qr.payload-version-activa=0",
                            "faltas.qr.secret=")
                    .run(ctx -> assertThat(ctx)
                            .hasFailed()
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("faltas.qr.secret"));
        }

        @Test @DisplayName("con clave insuficiente el contexto no arranca")
        void claveCortaElContextoFalla() {
            contexto.withPropertyValues(
                            "faltas.qr.payload-version-activa=0",
                            "faltas.qr.secret=" + QrTestSupport.SECRETO_DEMASIADO_CORTO_TEST_ONLY)
                    .run(ctx -> assertThat(ctx)
                            .hasFailed()
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("minimo"));
        }

        @Test @DisplayName("con una version de payload distinta de 0 el contexto no arranca")
        void versionNoImplementadaElContextoFalla() {
            contexto.withPropertyValues(
                            "faltas.qr.payload-version-activa=1",
                            "faltas.qr.secret=" + QrTestSupport.SECRETO_TEST_ONLY)
                    .run(ctx -> assertThat(ctx)
                            .hasFailed()
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("no esta implementada"));
        }

        @Test @DisplayName("con clave valida hay exactamente un bean del puerto")
        void conClaveValidaUnSoloBean() {
            contexto.withPropertyValues(
                            "faltas.qr.payload-version-activa=0",
                            "faltas.qr.secret=" + QrTestSupport.SECRETO_TEST_ONLY)
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed();
                        assertThat(ctx).hasSingleBean(
                                ar.gob.malvinas.faltas.core.application.port.QrTokenProtector.class);
                        assertThat(ctx.getBean(
                                ar.gob.malvinas.faltas.core.application.port.QrTokenProtector.class))
                                .isInstanceOf(HmacSha256QrTokenProtector.class);
                    });
        }
    }

    // =====================================================================
    @Nested @DisplayName("La configuracion base no trae clave funcional")
    class ConfiguracionBase {

        private static final Path MAIN_RESOURCES = Paths.get("src/main/resources");

        private static List<Path> yamlsDeProduccion() throws IOException {
            try (var s = Files.walk(MAIN_RESOURCES)) {
                return s.filter(p -> {
                    String n = p.getFileName().toString();
                    return n.endsWith(".yml") || n.endsWith(".yaml") || n.endsWith(".properties");
                }).toList();
            }
        }

        /**
         * 6. El punto central de la remediacion: un fallback funcional en la configuracion
         * base seria una clave publica conocida, y la etiqueta de "no productivo" no la
         * volveria segura. La propiedad se declara con default vacio.
         */
        @Test @DisplayName("6. ningun recurso de produccion declara una clave QR funcional por defecto")
        void sinClaveFuncionalPorDefecto() throws IOException {
            for (Path p : yamlsDeProduccion()) {
                String contenido = Files.readString(p, StandardCharsets.UTF_8);
                for (String linea : contenido.split("\\R")) {
                    String t = linea.trim();
                    if (t.startsWith("#") || !t.contains("FALTAS_QR_SECRET")) continue;
                    assertThat(t)
                            .as("%s no debe traer una clave QR funcional: %s", p, t)
                            .contains("${FALTAS_QR_SECRET:}");
                }
                assertThat(contenido)
                        .as("%s no debe declarar un keyring ni claves por version", p)
                        .doesNotContain("faltas.qr.claves")
                        .doesNotContain("FALTAS_QR_CLAVE");
            }
        }

        @Test @DisplayName("application.yml declara la version de payload y el secreto vacio")
        void applicationYmlDeclaraLoEsperado() throws IOException {
            String yml = Files.readString(MAIN_RESOURCES.resolve("application.yml"), StandardCharsets.UTF_8);
            assertThat(yml)
                    .contains("payload-version-activa: ${FALTAS_QR_PAYLOAD_VERSION_ACTIVA:0}")
                    .contains("secret: ${FALTAS_QR_SECRET:}");
            assertThat(yml)
                    .as("la semantica de version de clave y de rotacion no debe reaparecer")
                    .doesNotContain("version-activa: ${FALTAS_QR_VERSION_ACTIVA")
                    .doesNotContain("keyring");
        }

        /**
         * 7. La clave sintetica vive solo en recursos de test. Se usa un .properties para que
         * se sume al application.yml de produccion en lugar de reemplazarlo.
         */
        @Test @DisplayName("7. la clave TEST_ONLY vive en los recursos de test y esta marcada como tal")
        void claveDeTestEnRecursosDeTest() throws IOException {
            Path testProps = Paths.get("src/test/resources/application.properties");
            assertThat(testProps).exists();
            String contenido = Files.readString(testProps, StandardCharsets.UTF_8);
            assertThat(contenido)
                    .contains("faltas.qr.secret=")
                    .contains("TEST_ONLY");
        }
    }
}
