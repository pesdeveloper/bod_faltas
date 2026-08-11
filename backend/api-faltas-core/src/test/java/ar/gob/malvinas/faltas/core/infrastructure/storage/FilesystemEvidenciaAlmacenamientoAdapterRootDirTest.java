package ar.gob.malvinas.faltas.core.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Certifica {@link FilesystemEvidenciaAlmacenamientoAdapter#validarRootDirProductivo} de forma
 * aislada (función pura de tres parámetros primitivos, sin Spring). Wave 4 Parte 3E-B §9.
 */
class FilesystemEvidenciaAlmacenamientoAdapterRootDirTest {

    private static final String DEFAULT_DEV = "./faltas-data/evidencias";

    @Test
    @DisplayName("ruta vacia: rechazada en cualquier ambiente")
    void vacia_rechazada() {
        assertThatThrownBy(() ->
                FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo("", "", new String[]{"jdbc", "local"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("ruta classpath: rechazada en cualquier ambiente")
    void classpath_rechazada() {
        assertThatThrownBy(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                "classpath:evidencias", "classpath:evidencias", new String[]{"jdbc", "local"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classpath");
    }

    @Test
    @DisplayName("raiz temporal cruda del sistema operativo: rechazada")
    void raizTemporalCruda_rechazada() {
        String tmp = System.getProperty("java.io.tmpdir");
        assertThatThrownBy(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                tmp, tmp, new String[]{"jdbc", "local"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raiz temporal cruda");
    }

    @Test
    @DisplayName("subdirectorio unico dentro de la raiz temporal (Files.createTempDirectory): permitido")
    void subdirectorioDentroDeTemporal_permitido() {
        String sub = System.getProperty("java.io.tmpdir") + java.io.File.separator + "faltas-it-alguna-cosa-12345";
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                sub, sub, new String[]{"jdbc"})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'local' sin configuracion explicita: permite el default de desarrollo")
    void local_sinConfiguracionExplicita_permitido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                DEFAULT_DEV, null, new String[]{"jdbc", "local"})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'dev' sin configuracion explicita: permite el default de desarrollo")
    void dev_sinConfiguracionExplicita_permitido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                DEFAULT_DEV, null, new String[]{"jdbc", "dev"})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'test' sin configuracion explicita: permite el default (bypass de tests)")
    void test_sinConfiguracionExplicita_permitido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                DEFAULT_DEV, null, new String[]{"test"})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'jdbc' solo (sin local/dev/test), sin configuracion explicita: permitido"
            + " hoy, porque 'jdbc' solo es un combo productivo legitimo del proyecto (prompt"
            + " §13.2 'jdbc arranca'), no un perfil de desarrollo ni uno estricto sin definir")
    void soloJdbc_sinConfiguracionExplicita_permitidoPorAusenciaDePerfilProductivoEstrictoDefinido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                DEFAULT_DEV, null, new String[]{"jdbc"})).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'jdbc' + configuracion explicita: siempre permitido")
    void jdbc_conConfiguracionExplicita_permitido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                "/var/faltas/evidencias", "/var/faltas/evidencias", new String[]{"jdbc"}))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil hipotetico de ambiente distinto de local/dev/test, sin configuracion"
            + " explicita: rechazado (la regla de 'perfil productivo estricto' es real, aunque"
            + " el proyecto no defina ese perfil todavia)")
    void perfilHipoteticoDistinto_sinConfiguracionExplicita_rechazado() {
        assertThatThrownBy(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                DEFAULT_DEV, null, new String[]{"jdbc", "staging"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe configurarse explicitamente");
    }

    @Test
    @DisplayName("perfil hipotetico de ambiente distinto, CON configuracion explicita: permitido")
    void perfilHipoteticoDistinto_conConfiguracionExplicita_permitido() {
        assertThatCode(() -> FilesystemEvidenciaAlmacenamientoAdapter.validarRootDirProductivo(
                "/var/faltas/evidencias", "/var/faltas/evidencias", new String[]{"jdbc", "staging"}))
                .doesNotThrowAnyException();
    }
}
