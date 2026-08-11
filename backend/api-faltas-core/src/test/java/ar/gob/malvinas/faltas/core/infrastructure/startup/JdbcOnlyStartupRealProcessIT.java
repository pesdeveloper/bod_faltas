package ar.gob.malvinas.faltas.core.infrastructure.startup;

import ar.gob.malvinas.faltas.core.ApiFaltasCoreApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Certifica en un proceso Spring real (no {@code MockMvc}, no {@link org.springframework.mock.env.MockEnvironment})
 * que el arranque de {@link ApiFaltasCoreApplication} respeta la matriz de perfiles de Wave 4
 * Parte 3E-B §13.2.
 *
 * <p>Los casos negativos que no dependen de una {@code DataSource} (sin perfil, 'local' solo,
 * 'dev' solo, 'jdbc' con {@code app.persistence.mode=memory}) fallan en
 * {@link JdbcOnlyStartupGuardrail} antes de que se intente ninguna conexión, así que no
 * requieren MariaDB real y corren siempre. Los casos positivos ('jdbc', 'jdbc,local',
 * 'jdbc,dev') sí necesitan una {@code DataSource} real y están condicionados a
 * {@code FALTAS_DB_URL}.
 */
@DisplayName("Wave 4 Parte 3E-B §13.2: arranque real de la aplicacion por combinacion de perfiles")
class JdbcOnlyStartupRealProcessIT {

    private static final String QR_SECRET_TEST =
            "IT-ONLY-jdbc-only-startup-real-process-key-0000000000";

    private static SpringApplicationBuilder builder(String... profiles) {
        return new SpringApplicationBuilder(ApiFaltasCoreApplication.class)
                .profiles(profiles)
                .properties(
                        "server.port=0",
                        "faltas.qr.secret=" + QR_SECRET_TEST,
                        "faltas.demo.enabled=false");
    }

    @Test
    @DisplayName("sin ningun perfil activo: el arranque se rechaza por falta de 'jdbc'")
    void sinPerfil_noArranca() {
        assertThatThrownBy(() -> builder().run())
                .hasStackTraceContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'local' solo (sin 'jdbc'): el arranque se rechaza")
    void local_noArranca() {
        assertThatThrownBy(() -> builder("local").run())
                .hasStackTraceContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'dev' solo (sin 'jdbc'): el arranque se rechaza")
    void dev_noArranca() {
        assertThatThrownBy(() -> builder("dev").run())
                .hasStackTraceContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'jdbc' con app.persistence.mode=memory forzado: el arranque se rechaza")
    void jdbcConModoMemoryForzado_noArranca() {
        // Argumento de linea de comandos: unica fuente con precedencia suficiente para pisar
        // "app.persistence.mode: jdbc" de application-jdbc.yml (mas precedencia que
        // SpringApplicationBuilder#properties(...), que usa "defaultProperties", la fuente de
        // MENOR precedencia de todas).
        assertThatThrownBy(() -> builder("jdbc").run("--app.persistence.mode=memory"))
                .hasStackTraceContaining("app.persistence.mode=memory no esta permitido");
    }

    @Test
    @DisplayName("perfil 'jdbc' sin DataSource valida (host inalcanzable): el arranque se rechaza")
    void jdbcSinDataSourceValida_noArranca() {
        // Idem: los argumentos de linea de comandos pisan la variable de entorno real
        // FALTAS_DB_URL heredada de la sesion de shell (las variables de entorno tienen mas
        // precedencia que SpringApplicationBuilder#properties(...), por eso no alcanzaba).
        assertThatThrownBy(() -> builder("jdbc").run(
                        "--FALTAS_DB_URL=jdbc:mariadb://127.0.0.1:1/no-existe",
                        "--FALTAS_DB_USER=no-existe",
                        "--FALTAS_DB_PASSWORD=no-existe"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
    @DisplayName("perfil 'jdbc' con DataSource real: arranca y expone el perfil activo")
    void jdbc_arranca() {
        ConfigurableApplicationContext ctx = builder("jdbc").run();
        try {
            Environment env = ctx.getEnvironment();
            assertThat(env.getActiveProfiles()).contains("jdbc");
            assertThat(env.getProperty("app.persistence.mode")).isEqualTo("jdbc");
            assertThat(ctx.isActive()).isTrue();
        } finally {
            ctx.close();
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
    @DisplayName("perfiles 'jdbc,local' con DataSource real: arranca")
    void jdbcYLocal_arranca() {
        ConfigurableApplicationContext ctx = builder("jdbc", "local").run();
        try {
            assertThat(ctx.getEnvironment().getActiveProfiles()).contains("jdbc", "local");
            assertThat(ctx.isActive()).isTrue();
        } finally {
            ctx.close();
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
    @DisplayName("perfiles 'jdbc,dev' con DataSource real: arranca")
    void jdbcYDev_arranca() {
        ConfigurableApplicationContext ctx = builder("jdbc", "dev").run();
        try {
            assertThat(ctx.getEnvironment().getActiveProfiles()).contains("jdbc", "dev");
            assertThat(ctx.isActive()).isTrue();
        } finally {
            ctx.close();
        }
    }
}
