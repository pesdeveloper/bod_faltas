package ar.gob.malvinas.faltas.core.infrastructure.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Certifica {@link JdbcOnlyStartupGuardrail} de forma aislada, sin levantar contexto de Spring:
 * la lógica de decisión (perfiles activos + {@code app.persistence.mode}) es una función pura de
 * {@code Environment}, verificable con {@link MockEnvironment} en milisegundos.
 *
 * <p>Los escenarios que requieren un arranque real (arranca con MariaDB real bajo {@code jdbc})
 * están en {@code JdbcOnlyStartupRealProcessIT}, porque exigen un {@code DataSource} funcional
 * que este test no puede ni debe simular.
 *
 * Wave 4 Parte 3E-B §13.2.
 */
class JdbcOnlyStartupGuardrailTest {

    private final JdbcOnlyStartupGuardrail guardrail = new JdbcOnlyStartupGuardrail();

    @Test
    @DisplayName("sin ningun perfil activo: rechaza el arranque")
    void sinPerfil_rechaza() {
        MockEnvironment env = new MockEnvironment();
        assertThatThrownBy(() -> guardrail.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'local' sin 'jdbc': rechaza el arranque")
    void soloLocal_rechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThatThrownBy(() -> guardrail.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'dev' sin 'jdbc': rechaza el arranque")
    void soloDev_rechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        assertThatThrownBy(() -> guardrail.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("falta el perfil 'jdbc'");
    }

    @Test
    @DisplayName("perfil 'jdbc' solo: no rechaza (a nivel de este guardrail)")
    void soloJdbc_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("jdbc");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfiles 'jdbc,local': no rechaza")
    void jdbcYLocal_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("jdbc", "local");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfiles 'jdbc,dev': no rechaza")
    void jdbcYDev_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("jdbc", "dev");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'jdbc' con app.persistence.mode=memory: rechaza el arranque")
    void jdbcConModoMemory_rechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("jdbc");
        env.setProperty("app.persistence.mode", "memory");
        assertThatThrownBy(() -> guardrail.postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.persistence.mode=memory no esta permitido");
    }

    @Test
    @DisplayName("perfil 'test' solo (bypass exclusivo de tests): no rechaza")
    void soloTest_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'test' con app.persistence.mode=memory: no rechaza (uso esperado del bypass)")
    void testConModoMemory_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        env.setProperty("app.persistence.mode", "memory");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("perfil 'local' + 'test' sin 'jdbc': el bypass de test sigue habilitando el arranque")
    void localMasTestSinJdbc_noRechaza() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local", "test");
        assertThatCode(() -> guardrail.postProcessEnvironment(env, null)).doesNotThrowAnyException();
    }
}
