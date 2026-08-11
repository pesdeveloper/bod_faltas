package ar.gob.malvinas.faltas.core.infrastructure.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.List;

/**
 * Guardrail de arranque de Wave 4 Parte 3E-B (DECISION-JDBC-ONLY-01).
 *
 * <p>El perfil {@code jdbc} es obligatorio para ejecutar la aplicación productiva de Faltas: sin
 * él, este {@link EnvironmentPostProcessor} rechaza el arranque con un mensaje explícito, antes
 * de que Spring intente construir ningún bean dependiente de persistencia. No existe un modo
 * productivo en memoria.
 *
 * <p>Único bypass permitido: el perfil {@code test}, exclusivo de {@code src/test} (definido en
 * {@code application-test.yml}, que no se empaqueta con la aplicación). Ese perfil no es una
 * opción desplegable: sólo lo activan explícitamente los {@code @SpringBootTest} que necesitan un
 * contexto rápido con fakes en memoria, vía {@code @ActiveProfiles("test")}.
 *
 * <p>Se ejecuta como {@link EnvironmentPostProcessor} (registrado en
 * {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}) para
 * fallar lo antes posible en el ciclo de vida de {@code SpringApplication}: antes de que se
 * registre ninguna definición de bean, evitando que un fallo de persistencia se manifieste más
 * adelante como un {@code NullPointerException} o un error de inyección de dependencias difícil
 * de diagnosticar.
 */
public class JdbcOnlyStartupGuardrail implements EnvironmentPostProcessor, Ordered {

    static final String PERFIL_JDBC = "jdbc";
    static final String PERFIL_TEST_BYPASS = "test";
    static final String PROPERTY_MODO_PERSISTENCIA = "app.persistence.mode";
    static final String MODO_MEMORY = "memory";

    /**
     * Se ejecuta último entre los {@link EnvironmentPostProcessor}, a propósito: los perfiles
     * "adicionales" (los que llegan por {@code SpringApplicationBuilder#profiles(String...)} o
     * por {@code @ActiveProfiles} en tests) recién se fusionan con
     * {@code environment.getActiveProfiles()} durante el procesamiento estándar de Spring Boot
     * (posterior a {@code ConfigDataEnvironmentPostProcessor}). Si este guardrail corriera con
     * {@code HIGHEST_PRECEDENCE} vería siempre una lista de perfiles vacía, incluso cuando el
     * perfil solicitado sí está presente.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> perfilesActivos = Arrays.asList(environment.getActiveProfiles());
        boolean jdbcActivo = perfilesActivos.contains(PERFIL_JDBC);
        boolean bypassDeTest = perfilesActivos.contains(PERFIL_TEST_BYPASS);
        String modoPersistencia = environment.getProperty(PROPERTY_MODO_PERSISTENCIA);

        if (!jdbcActivo && !bypassDeTest) {
            throw new IllegalStateException(mensajeSinPerfilJdbc(perfilesActivos));
        }

        if (jdbcActivo && MODO_MEMORY.equalsIgnoreCase(modoPersistencia)) {
            throw new IllegalStateException(mensajeModoMemoryConJdbc(perfilesActivos));
        }
    }

    static String mensajeSinPerfilJdbc(List<String> perfilesActivos) {
        return "Arranque rechazado (Wave 4 Parte 3E-B, DECISION-JDBC-ONLY-01): falta el perfil "
                + "'jdbc'. La aplicacion productiva de Faltas requiere el perfil 'jdbc' activo, "
                + "solo o combinado con un perfil de ambiente ('local', 'dev' u otro), y una "
                + "DataSource real configurada mediante FALTAS_DB_URL/FALTAS_DB_USER/"
                + "FALTAS_DB_PASSWORD. No existe un modo productivo en memoria. "
                + "Perfiles activos recibidos: " + perfilesActivos + ". "
                + "Ejecucion esperada: --spring.profiles.active=jdbc"
                + " (o jdbc,local / jdbc,dev / jdbc,<ambiente>).";
    }

    static String mensajeModoMemoryConJdbc(List<String> perfilesActivos) {
        return "Arranque rechazado (Wave 4 Parte 3E-B, DECISION-JDBC-ONLY-01): "
                + PROPERTY_MODO_PERSISTENCIA + "=" + MODO_MEMORY + " no esta permitido con el "
                + "perfil 'jdbc' activo. El modo memoria productivo fue retirado. "
                + "Perfiles activos recibidos: " + perfilesActivos + ".";
    }
}
