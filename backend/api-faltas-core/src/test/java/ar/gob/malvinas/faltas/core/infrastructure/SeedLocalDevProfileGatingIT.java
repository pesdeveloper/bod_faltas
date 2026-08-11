package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.service.TalonarioElectronicoDevSeeder;
import ar.gob.malvinas.faltas.core.web.SeedLocalDevController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IT de endurecimiento estructural del seeder local/dev (Wave 4 Parte 3E §15).
 *
 * <p>Certifica que {@code @Profile({"local", "dev"})} en {@link TalonarioElectronicoDevSeeder} y
 * {@link SeedLocalDevController} es un segundo gate <b>estructural</b>, independiente de
 * {@code faltas.seed.local-dev.enabled}: ningún valor de esa property, por sí solo, puede
 * habilitar el seeder fuera de los perfiles {@code local}/{@code dev}.
 *
 * <p>Cada {@link Nested} usa una combinación distinta de {@code @ActiveProfiles}, así que
 * Spring Test arranca un contexto por combinación (cacheado por firma de configuración). Los
 * tres requieren {@code jdbc} activo (y por lo tanto {@code FALTAS_DB_URL}) porque el resto del
 * contexto productivo (JdbcConfig, JdbcClient, etc.) lo exige para arrancar.
 */
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT Wave 4 Parte 3E §15: gate estructural @Profile del seeder local/dev")
class SeedLocalDevProfileGatingIT {

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest
    @ActiveProfiles("jdbc")
    @TestPropertySource(properties = "faltas.seed.local-dev.enabled=true")
    @DisplayName("perfil jdbc SIN local/dev, property=true: seeder y controller AUSENTES")
    class SoloJdbcConPropertyTrue {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("TalonarioElectronicoDevSeeder no esta en el contexto")
        void seederAusente() {
            assertThatThrownBy(() -> context.getBean(TalonarioElectronicoDevSeeder.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }

        @Test
        @DisplayName("SeedLocalDevController no esta en el contexto (ni con la property en true)")
        void controllerAusente() {
            assertThatThrownBy(() -> context.getBean(SeedLocalDevController.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest
    @ActiveProfiles("jdbc")
    @DisplayName("perfil jdbc SIN local/dev, property=false (default): tambien ausentes")
    class SoloJdbcConPropertyFalsePorDefecto {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("ambos beans ausentes: la property por si sola nunca alcanza fuera de local/dev")
        void ambosAusentes() {
            assertThat(context.getBeanNamesForType(TalonarioElectronicoDevSeeder.class)).isEmpty();
            assertThat(context.getBeanNamesForType(SeedLocalDevController.class)).isEmpty();
        }
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest
    @ActiveProfiles({"jdbc", "local"})
    @DisplayName("perfil local + jdbc: disponibles")
    class JdbcYLocal {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("ambos beans presentes bajo el perfil local")
        void ambosPresentes() {
            assertThat(context.getBean(TalonarioElectronicoDevSeeder.class)).isNotNull();
            assertThat(context.getBean(SeedLocalDevController.class)).isNotNull();
        }
    }

    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @SpringBootTest
    @ActiveProfiles({"jdbc", "dev"})
    @DisplayName("perfil dev + jdbc: disponibles")
    class JdbcYDev {

        @Autowired
        private ApplicationContext context;

        @Test
        @DisplayName("ambos beans presentes bajo el perfil dev")
        void ambosPresentes() {
            assertThat(context.getBean(TalonarioElectronicoDevSeeder.class)).isNotNull();
            assertThat(context.getBean(SeedLocalDevController.class)).isNotNull();
        }
    }
}
