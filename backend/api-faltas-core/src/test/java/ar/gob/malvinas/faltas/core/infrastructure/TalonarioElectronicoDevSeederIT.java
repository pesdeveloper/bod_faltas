package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.application.service.TalonarioElectronicoDevSeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT de Wave 4 Parte 3C §11.2: ejecuta {@link TalonarioElectronicoDevSeeder} dos veces contra
 * MariaDB real y demuestra que la primera crea (o completa) y la segunda no duplica nada, con
 * exactamente un talonario ACTA ELECTRONICO activo aplicable al final.
 *
 * <p>Limpia al final los datos que este IT creo, identificados por los codigos fijos y
 * documentados del seeder ({@code DEV-FALTAS-01}, {@code DEVACTA1}, {@code DEVTALAC}). No borra
 * nada si esos datos ya existian antes de esta corrida (entorno local/dev real ya seedeado):
 * en ese caso, ambas ejecuciones del test resuelven "ALREADY_PRESENT" y no se toca nada.
 */
@SpringBootTest
@ActiveProfiles({"jdbc", "dev"})
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT Wave 4 Parte 3C §11.2 / Parte 3E §15: seeder local/dev idempotente")
class TalonarioElectronicoDevSeederIT {

    @Autowired
    private TalonarioElectronicoDevSeeder seeder;

    @Autowired
    private JdbcClient jdbc;

    private boolean creadoPorEsteTest;

    @AfterEach
    void limpiarSiLoCreoEsteTest() {
        if (!creadoPorEsteTest) {
            return;
        }
        Long idTalonario = jdbc.sql("SELECT id FROM num_talonario WHERE codigo = :cod")
                .param("cod", TalonarioElectronicoDevSeeder.COD_TALONARIO_DEV)
                .query(Long.class).optional().orElse(null);
        if (idTalonario != null) {
            jdbc.sql("DELETE FROM num_talonario_ambito WHERE talonario_id = :id").param("id", idTalonario).update();
            jdbc.sql("DELETE FROM num_talonario WHERE id = :id").param("id", idTalonario).update();
        }
        jdbc.sql("DELETE FROM num_politica WHERE codigo = :cod")
                .param("cod", TalonarioElectronicoDevSeeder.COD_POLITICA_DEV).update();
        jdbc.sql("DELETE FROM fal_inspector_version WHERE id_insp IN (SELECT id FROM fal_inspector WHERE id_user = :u)")
                .param("u", TalonarioElectronicoDevSeeder.ID_USER_INSPECTOR_DEV).update();
        jdbc.sql("DELETE FROM fal_inspector WHERE id_user = :u")
                .param("u", TalonarioElectronicoDevSeeder.ID_USER_INSPECTOR_DEV).update();
        Long idDep = jdbc.sql("SELECT id FROM fal_dependencia WHERE cod_dep = :cod")
                .param("cod", TalonarioElectronicoDevSeeder.COD_DEP_DEV)
                .query(Long.class).optional().orElse(null);
        if (idDep != null) {
            jdbc.sql("DELETE FROM fal_dependencia_version WHERE id_dep = :id").param("id", idDep).update();
            jdbc.sql("DELETE FROM fal_dependencia WHERE id = :id").param("id", idDep).update();
        }
    }

    @Test
    @DisplayName("Primera ejecucion crea o completa; segunda no duplica; estado final: exactamente un talonario activo")
    void dosEjecucionesSucesivasDejanElMismoEstadoLogico() {
        boolean yaExistiaAntes = jdbc.sql("SELECT COUNT(*) FROM fal_dependencia WHERE cod_dep = :cod")
                .param("cod", TalonarioElectronicoDevSeeder.COD_DEP_DEV)
                .query(Long.class).single() > 0;
        creadoPorEsteTest = !yaExistiaAntes;

        TalonarioElectronicoDevSeeder.ResultadoSeederLocalDev primera = seeder.asegurar();
        assertThat(primera.idDependencia()).isNotNull();
        assertThat(primera.idInspector()).isNotNull();
        assertThat(primera.idTalonario()).isNotNull();

        TalonarioElectronicoDevSeeder.ResultadoSeederLocalDev segunda = seeder.asegurar();
        assertThat(segunda.idDependencia()).as("misma dependencia en la 2da corrida").isEqualTo(primera.idDependencia());
        assertThat(segunda.idInspector()).as("mismo inspector en la 2da corrida").isEqualTo(primera.idInspector());
        assertThat(segunda.idTalonario()).as("mismo talonario en la 2da corrida").isEqualTo(primera.idTalonario());

        List<TalonarioElectronicoDevSeeder.Estado> estadosSegunda = segunda.pasos().stream()
                .map(TalonarioElectronicoDevSeeder.PasoSeed::estado)
                .toList();
        assertThat(estadosSegunda)
                .as("la 2da corrida no crea nada nuevo: los tres pasos son ALREADY_PRESENT")
                .containsOnly(TalonarioElectronicoDevSeeder.Estado.ALREADY_PRESENT);

        assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_dependencia WHERE cod_dep = :cod")
                .param("cod", TalonarioElectronicoDevSeeder.COD_DEP_DEV)
                .query(Long.class).single())
                .as("una sola fila de dependencia de desarrollo, no duplicada").isEqualTo(1L);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM fal_inspector WHERE id_user = :u")
                .param("u", TalonarioElectronicoDevSeeder.ID_USER_INSPECTOR_DEV)
                .query(Long.class).single())
                .as("un solo inspector de desarrollo, no duplicado").isEqualTo(1L);

        Long activosFinal = jdbc.sql("""
                SELECT COUNT(*) FROM num_talonario_ambito a JOIN num_talonario t ON a.talonario_id = t.id
                 WHERE a.id_dep = :dep AND a.si_activo = TRUE AND t.si_activo = TRUE
                   AND t.clase_talonario = 1 AND t.tipo_talonario = 1
                """).param("dep", primera.idDependencia()).query(Long.class).single();
        assertThat(activosFinal)
                .as("resultado final verificable por consulta: exactamente un talonario activo aplicable")
                .isEqualTo(1L);
    }
}
