package ar.gob.malvinas.faltas.core.infrastructure;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenCaptura;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.infrastructure.jdbc.JdbcObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * IT de JdbcObservacionRepository sobre fal_observacion (WAVE 1A2-B0).
 *
 * Ejecuta contra el sandbox real sb_faltas_db mediante Failsafe:
 *   mvn -P integration-tests verify
 *
 * Disciplina de datos:
 * - Todo escenario corre dentro de una transaccion que se revierte siempre. El
 *   metodo enEscenario(...) marca rollbackOnly antes de devolver, de modo que ni un
 *   escenario exitoso deja filas.
 * - El acta de contexto y su cadena de padres se construyen en la propia
 *   transaccion respetando el DDL real
 *   (fal_dependencia -> fal_dependencia_version -> fal_persona -> fal_acta).
 * - Todo texto de observacion lleva la marca WAVE1A2_B0_ y ninguna fila del
 *   sandbox se lee ni se modifica.
 * - No hay TRUNCATE ni DELETE, ni se desactivan FK: la limpieza es el rollback.
 *
 * Sobre la FK polimorfica: no existe. entidad_id no tiene padre fisico y MariaDB no
 * lo valida. Varios escenarios usan deliberadamente un entidad_id que no
 * corresponde a ninguna fila para dejarlo explicito: la integridad propietaria la
 * garantiza la aplicacion dentro de la transaccion del caso de uso.
 */
@SpringBootTest
@ActiveProfiles("jdbc")
@EnabledIfEnvironmentVariable(named = "FALTAS_DB_URL", matches = ".+")
@DisplayName("IT WAVE-1A2-B0: JdbcObservacionRepository sobre fal_observacion")
class JdbcObservacionRepositoryIT {

    private static final String MARCA = "WAVE1A2_B0_";

    /** DATETIME(6) guarda microsegundos, no nanosegundos. */
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 3, 14, 5, 6, 789_012_000);

    /** entidad_id sin padre fisico: no hay FK polimorfica que lo valide. */
    private static final long ENTIDAD_SIN_PADRE = 987_654_321L;

    /**
     * DataSource que falla al pedir conexion. Sirve para demostrar que una operacion no
     * toca la persistencia: si la tocara, la excepcion afloraria. No es un mock de un
     * framework: es la implementacion minima del contrato de DataSource.
     */
    private static final DataSource DATASOURCE_QUE_FALLA = new AbstractDataSource() {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("este DataSource no debe usarse: la operacion no debia consultar");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }
    };

    @Autowired
    private ObservacionRepository repositorio;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager txManager;

    // =====================================================================
    // TOPOLOGIA
    // =====================================================================

    @Test
    @DisplayName("1. Con perfil jdbc el puerto lo resuelve el adaptador JDBC")
    void puertoResueltoPorAdaptadorJdbc() {
        assertThat(repositorio).isInstanceOf(JdbcObservacionRepository.class);
    }

    // =====================================================================
    // ALTA Y GENERATED KEYS
    // =====================================================================

    @Test
    @DisplayName("2. guardar con id null inserta y devuelve el id de AUTO_INCREMENT")
    void altaSinIdRecibeIdGenerado() {
        enEscenario(actaId -> {
            FalObservacion persistida = repositorio.guardar(nueva(actaId, "Alta simple", T0));

            assertThat(persistida.getId()).isNotNull().isPositive();
            assertThat(repositorio.buscarPorId(persistida.getId())).isPresent();
            return null;
        });
    }

    @Test
    @DisplayName("3. El id del candidato no se preasigna: dos altas reciben ids distintos")
    void altasSucesivasRecibenIdsDistintos() {
        enEscenario(actaId -> {
            Long primera = repositorio.guardar(nueva(actaId, "Primera", T0)).getId();
            Long segunda = repositorio.guardar(nueva(actaId, "Segunda", T0)).getId();

            assertThat(segunda).isNotNull().isNotEqualTo(primera);
            return null;
        });
    }

    @Test
    @DisplayName("4. guardar no muta el argumento: la candidata conserva id nulo")
    void guardarNoMutaElArgumento() {
        enEscenario(actaId -> {
            FalObservacion candidata = nueva(actaId, "No se muta", T0);

            FalObservacion persistida = repositorio.guardar(candidata);

            assertThat(candidata.getId()).isNull();
            assertThat(persistida.getId()).isNotNull();
            return null;
        });
    }

    /**
     * Ninguna de las dos lecturas posibles de un id informado es legitima: con id
     * inexistente seria preasignar la PK (DECISION_ID-01) y con id existente seria
     * reescribir una fila append-only (DECISION_OBS-01).
     */
    @Test
    @DisplayName("5. guardar con id informado se rechaza y no inserta ni reescribe")
    void guardarConIdInformadoSeRechaza() {
        enEscenario(actaId -> {
            FalObservacion original = repositorio.guardar(nueva(actaId, "Original", T0));
            FalObservacion inexistente = nueva(actaId, "Con PK del llamador", T0).conId(999_999_999L);
            FalObservacion reescritura = new FalObservacion(original.getId(), EntidadTipoObservada.ACTA,
                    actaId, null, MARCA + "Texto reescrito", OrigenObservacion.USUARIO, T0, actor());

            assertThatThrownBy(() -> repositorio.guardar(inexistente))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no se edita");
            assertThatThrownBy(() -> repositorio.guardar(reescritura))
                    .isInstanceOf(PrecondicionVioladaException.class);

            assertThat(repositorio.buscarPorId(999_999_999L)).isEmpty();
            assertThat(repositorio.buscarPorId(original.getId()).orElseThrow().getObservacion())
                    .isEqualTo(MARCA + "Original");
            return null;
        });
    }

    /**
     * Las 11 columnas de la tabla en un unico round-trip. tipoObs no participa: el
     * DDL no lo persiste (HUMAN_DECISION_CLOSED) y vuelve nulo.
     */
    @Test
    @DisplayName("6. Round-trip de las 11 columnas, incluido id_acta_contexto informado")
    void roundTripDeLasOnceColumnas() {
        enEscenario(actaId -> {
            String userAlta = actor();
            FalObservacion original = new FalObservacion(
                    null, EntidadTipoObservada.DOCUMENTO, ENTIDAD_SIN_PADRE, 77,
                    MARCA + "Round trip completo", OrigenObservacion.INTEGRACION,
                    actaId, T0, userAlta);

            Long id = repositorio.guardar(original).getId();
            FalObservacion leida = repositorio.buscarPorId(id).orElseThrow();

            assertThat(leida.getId()).isEqualTo(id);
            assertThat(leida.getEntidadTipo()).isEqualTo(EntidadTipoObservada.DOCUMENTO);
            assertThat(leida.getEntidadId()).isEqualTo(ENTIDAD_SIN_PADRE);
            assertThat(leida.getIdActaContexto()).isEqualTo(actaId);
            assertThat(leida.getOrigenObs()).isEqualTo(OrigenObservacion.INTEGRACION);
            assertThat(leida.getObservacion()).isEqualTo(MARCA + "Round trip completo");
            assertThat(leida.isSiActiva()).isTrue();
            assertThat(leida.getFhAlta()).isEqualTo(T0);
            assertThat(leida.getIdUserAlta()).isEqualTo(userAlta);
            assertThat(leida.getFhBaja()).isNull();
            assertThat(leida.getIdUserBaja()).isNull();
            assertThat(leida.getTipoObs()).as("tipo_obs no se persiste").isNull();
            return null;
        });
    }

    /**
     * Los enums viajan por su codigo fisico, nunca por ordinal(), name() ni
     * toString(). Se comprueba contra la columna cruda.
     */
    @Test
    @DisplayName("7. entidad_tipo y origen_observacion se guardan como el codigo SMALLINT del enum")
    void enumsSeGuardanPorCodigoFisico() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(new FalObservacion(
                    null, EntidadTipoObservada.MOVIMIENTO_PAGO, ENTIDAD_SIN_PADRE, null,
                    MARCA + "Codigos fisicos", OrigenObservacion.SISTEMA, null, T0, actor())).getId();

            short[] crudo = jdbc.sql("""
                            SELECT entidad_tipo, origen_observacion
                            FROM fal_observacion WHERE id = :id
                            """)
                    .param("id", id)
                    .query((rs, n) -> new short[] {
                            rs.getShort("entidad_tipo"), rs.getShort("origen_observacion") })
                    .single();

            assertThat(crudo[0]).isEqualTo((short) EntidadTipoObservada.MOVIMIENTO_PAGO.codigo()).isEqualTo((short) 20);
            assertThat(crudo[1]).isEqualTo((short) OrigenObservacion.SISTEMA.codigo()).isEqualTo((short) 2);
            return null;
        });
    }

    // =====================================================================
    // LECTURAS: PROPIETARIO, CARDINALIDAD Y ORDEN
    // =====================================================================

    @Test
    @DisplayName("8. buscarPorId devuelve Optional vacio para un id inexistente")
    void buscarPorIdInexistente() {
        enEscenario(actaId -> {
            assertThat(repositorio.buscarPorId(-1L)).isEmpty();
            return null;
        });
    }

    /**
     * El propietario es el par (entidad_tipo, entidad_id). Dos entidades distintas
     * con el mismo entidad_id no comparten observaciones.
     */
    @Test
    @DisplayName("9. El listado por propietario aisla (entidad_tipo, entidad_id)")
    void listadoPorPropietarioAisla() {
        enEscenario(actaId -> {
            Long delActa = repositorio.guardar(nueva(actaId, "Del acta", T0)).getId();
            repositorio.guardar(new FalObservacion(null, EntidadTipoObservada.PARALIZACION, actaId,
                    null, MARCA + "De la paralizacion", OrigenObservacion.USUARIO, actaId, T0, actor()));

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(delActa);
            return null;
        });
    }

    @Test
    @DisplayName("10. Cardinalidad 1 a 0..N: sin UNIQUE, un propietario acumula N observaciones")
    void cardinalidadN() {
        enEscenario(actaId -> {
            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .as("el propietario arranca sin observaciones")
                    .isEmpty();

            for (int i = 1; i <= 5; i++) {
                repositorio.guardar(nueva(actaId, "Acumulada " + i, T0.plusMinutes(i)));
            }

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId)).hasSize(5);
            return null;
        });
    }

    @Test
    @DisplayName("11. El historial ordena por fh_alta ASC, no por orden de insercion")
    void ordenPorFhAltaAscendente() {
        enEscenario(actaId -> {
            Long tardia = repositorio.guardar(nueva(actaId, "Tardia", T0.plusHours(2))).getId();
            Long temprana = repositorio.guardar(nueva(actaId, "Temprana", T0)).getId();
            Long media = repositorio.guardar(nueva(actaId, "Media", T0.plusHours(1))).getId();

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(temprana, media, tardia);
            return null;
        });
    }

    /**
     * DATETIME(6) permite instantes identicos. Sin el desempate por id el orden no
     * seria determinista.
     */
    @Test
    @DisplayName("12. Con el mismo fh_alta el id desempata de forma determinista")
    void empatePorId() {
        enEscenario(actaId -> {
            Long primera = repositorio.guardar(nueva(actaId, "Primera del instante", T0)).getId();
            Long segunda = repositorio.guardar(nueva(actaId, "Segunda del instante", T0)).getId();
            Long tercera = repositorio.guardar(nueva(actaId, "Tercera del instante", T0)).getId();

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, segunda, tercera);
            return null;
        });
    }

    @Test
    @DisplayName("13. listarActivasPorEntidad filtra si_activa y conserva el orden canonico")
    void filtroDeActivas() {
        enEscenario(actaId -> {
            Long vigente = repositorio.guardar(nueva(actaId, "Vigente", T0)).getId();
            Long aDarDeBaja = repositorio.guardar(nueva(actaId, "Se da de baja", T0.plusMinutes(1))).getId();
            Long tambienVigente = repositorio.guardar(nueva(actaId, "Tambien vigente", T0.plusMinutes(2))).getId();

            repositorio.desactivar(aDarDeBaja, T0.plusDays(1), actor());

            assertThat(repositorio.listarActivasPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(vigente, tambienVigente);
            return null;
        });
    }

    @Test
    @DisplayName("14. El historial incluye las inactivas: la baja logica no borra la fila")
    void historialIncluyeInactivas() {
        enEscenario(actaId -> {
            Long primera = repositorio.guardar(nueva(actaId, "Primera", T0)).getId();
            Long segunda = repositorio.guardar(nueva(actaId, "Segunda", T0.plusMinutes(1))).getId();

            repositorio.desactivar(primera, T0.plusDays(1), actor());

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(primera, segunda);
            assertThat(repositorio.listarActivasPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(segunda);
            return null;
        });
    }

    // =====================================================================
    // BAJA LOGICA
    // =====================================================================

    @Test
    @DisplayName("15. La baja logica escribe si_activa=0 con fh_baja e id_user_baja")
    void bajaLogicaCoherente() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(nueva(actaId, "Para dar de baja", T0)).getId();
            String userBaja = actor();

            FalObservacion baja = repositorio.desactivar(id, T0.plusDays(1), userBaja);

            assertThat(baja.isSiActiva()).isFalse();
            assertThat(baja.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(baja.getIdUserBaja()).isEqualTo(userBaja);

            FalObservacion leida = repositorio.buscarPorId(id).orElseThrow();
            assertThat(leida.isSiActiva()).isFalse();
            assertThat(leida.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(leida.getIdUserBaja()).isEqualTo(userBaja);
            return null;
        });
    }

    @Test
    @DisplayName("16. La baja no reescribe el texto ni los datos de alta")
    void bajaPreservaTextoYDatosDeAlta() {
        enEscenario(actaId -> {
            String userAlta = actor();
            FalObservacion original = repositorio.guardar(new FalObservacion(
                    null, EntidadTipoObservada.ACTA, actaId, null, MARCA + "Texto intacto",
                    OrigenObservacion.USUARIO, null, T0, userAlta));

            repositorio.desactivar(original.getId(), T0.plusDays(1), actor());
            FalObservacion leida = repositorio.buscarPorId(original.getId()).orElseThrow();

            assertThat(leida.getObservacion()).isEqualTo(MARCA + "Texto intacto");
            assertThat(leida.getEntidadTipo()).isEqualTo(EntidadTipoObservada.ACTA);
            assertThat(leida.getEntidadId()).isEqualTo(actaId);
            assertThat(leida.getOrigenObs()).isEqualTo(OrigenObservacion.USUARIO);
            assertThat(leida.getFhAlta()).isEqualTo(T0);
            assertThat(leida.getIdUserAlta()).isEqualTo(userAlta);
            return null;
        });
    }

    @Test
    @DisplayName("17. Un id inexistente se rechaza con la excepcion funcional")
    void bajaDeIdInexistente() {
        enEscenario(actaId -> {
            assertThatThrownBy(() -> repositorio.desactivar(-4242L, T0, actor()))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessage("Observacion no encontrada: -4242");
            return null;
        });
    }

    /**
     * Cero filas afectadas tiene dos causas y el adaptador las desambigua con una
     * lectura por id que solo se paga en el camino de fallo.
     */
    @Test
    @DisplayName("18. Una segunda baja se distingue del id inexistente y no altera la primera")
    void segundaBajaSeDistingueDeIdInexistente() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(nueva(actaId, "Doble baja", T0)).getId();
            String primerActor = actor();
            repositorio.desactivar(id, T0.plusDays(1), primerActor);

            assertThatThrownBy(() -> repositorio.desactivar(id, T0.plusDays(9), actor()))
                    .isInstanceOf(IllegalStateException.class)
                    .isNotInstanceOf(PrecondicionVioladaException.class)
                    .hasMessage("La observacion ya esta inactiva: id=" + id);

            FalObservacion leida = repositorio.buscarPorId(id).orElseThrow();
            assertThat(leida.getFhBaja()).isEqualTo(T0.plusDays(1));
            assertThat(leida.getIdUserBaja()).isEqualTo(primerActor);
            return null;
        });
    }

    @Test
    @DisplayName("19. La correccion append-only es baja logica mas fila nueva")
    void correccionAppendOnly() {
        enEscenario(actaId -> {
            Long original = repositorio.guardar(nueva(actaId, "Dato erroneo", T0)).getId();

            repositorio.desactivar(original, T0.plusDays(1), actor());
            Long correccion = repositorio.guardar(nueva(actaId, "Dato corregido", T0.plusDays(1))).getId();

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getId)
                    .containsExactly(original, correccion);
            assertThat(repositorio.listarActivasPorEntidad(EntidadTipoObservada.ACTA, actaId))
                    .extracting(FalObservacion::getObservacion)
                    .containsExactly(MARCA + "Dato corregido");
            return null;
        });
    }

    // =====================================================================
    // CONSTRAINTS FISICAS
    // =====================================================================

    /**
     * chk_obs_texto fue retirado por DECISION_DDL-SIN-CHECKS-CODIGOS-01
     * (ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853): el dominio sigue bloqueando el texto
     * en blanco antes de llegar a la base (unica linea de defensa ahora); un INSERT crudo que
     * bypasea el dominio ya no es rechazado por el motor.
     */
    @Test
    @DisplayName("20. chk_obs_texto fue retirado: el dominio sigue bloqueando el texto en blanco")
    void checkTextoEnBlanco() {
        enEscenario(actaId -> {
            assertThatThrownBy(() -> new FalObservacion(null, EntidadTipoObservada.ACTA, actaId,
                    null, "   ", OrigenObservacion.USUARIO, null, T0, actor()))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(catchThrowable(() -> insertarCrudo(actaId, "   ", true, null, null)))
                    .as("sin chk_obs_texto, el INSERT crudo con texto en blanco ya no lanza excepcion")
                    .isNull();
            return null;
        });
    }

    /**
     * El limite superior no lo hace cumplir chk_obs_texto sino el tipo de la columna:
     * un valor de mas de 512 caracteres provoca el error 1406 (SQLSTATE 22001) antes
     * de que el CHECK pueda evaluarse. Como CHAR_LENGTH(TRIM(...)) de un VARCHAR(512)
     * nunca supera 512, la rama superior del BETWEEN es estructuralmente inalcanzable
     * y el CHECK solo defiende el limite inferior (texto en blanco).
     *
     * Consecuencia que este test fija: no toda violacion de integridad de esta tabla
     * llega como DataIntegrityViolationException. Spring clasifica ese SQLSTATE como
     * BadSqlGrammarException.
     */
    @Test
    @DisplayName("21. Mas de 512 caracteres los rechaza el tipo VARCHAR(512), no chk_obs_texto")
    void limiteSuperiorLoImponeElTipoDeColumna() {
        enEscenario(actaId -> {
            String excedido = MARCA + "L".repeat(513);

            assertThatThrownBy(() -> new FalObservacion(null, EntidadTipoObservada.ACTA, actaId,
                    null, excedido, OrigenObservacion.USUARIO, null, T0, actor()))
                    .as("el dominio lo bloquea antes de llegar a la base")
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(catchThrowable(() -> insertarCrudo(actaId, excedido, true, null, null)))
                    .isInstanceOf(DataAccessException.class)
                    .isInstanceOf(BadSqlGrammarException.class)
                    .isNotInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(PrecondicionVioladaException.class);

            assertThat(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId)).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("22. Un texto de exactamente 512 caracteres se acepta: el limite es inclusivo")
    void limite512Inclusivo() {
        enEscenario(actaId -> {
            String limite = "L".repeat(512);
            Long id = repositorio.guardar(new FalObservacion(null, EntidadTipoObservada.ACTA, actaId,
                    null, limite, OrigenObservacion.USUARIO, null, T0, actor())).getId();

            assertThat(repositorio.buscarPorId(id).orElseThrow().getObservacion()).hasSize(512);
            return null;
        });
    }

    /**
     * chk_obs_baja_coherente fue retirado por DECISION_DDL-SIN-CHECKS-CODIGOS-01
     * (ACTA-ALTA-INTEGRAL-JDBC-MASTER-20260804-224853): sus dos ramas ya no son rechazadas por
     * el motor. Ninguna es alcanzable por el adaptador ordinario: el constructor fuerza la
     * combinacion activa y desactivar() exige ambos datos de baja; esa sigue siendo la unica
     * linea de defensa real.
     */
    @Test
    @DisplayName("23. chk_obs_baja_coherente fue retirado: el motor ya no rechaza las combinaciones incoherentes")
    void checkCoherenciaDeBaja() {
        enEscenario(actaId -> {
            assertThat(catchThrowable(() -> insertarCrudo(actaId, MARCA + "Activa con baja", true, T0, actor())))
                    .as("sin chk_obs_baja_coherente, si_activa=1 con datos de baja ya no lanza excepcion")
                    .isNull();

            assertThat(catchThrowable(() -> insertarCrudo(actaId, MARCA + "Inactiva sin baja", false, null, null)))
                    .as("sin chk_obs_baja_coherente, si_activa=0 sin datos de baja ya no lanza excepcion")
                    .isNull();

            return null;
        });
    }

    /**
     * Toda fila que el adaptador produce cumple el CHECK por construccion. Se
     * verifica contra las columnas crudas, no contra el modelo.
     */
    @Test
    @DisplayName("24. El adaptador ordinario produce siempre una combinacion de baja valida")
    void adaptadorSiempreProduceCombinacionValida() {
        enEscenario(actaId -> {
            Long activa = repositorio.guardar(nueva(actaId, "Queda activa", T0)).getId();
            Long inactiva = repositorio.guardar(nueva(actaId, "Queda inactiva", T0.plusMinutes(1))).getId();
            repositorio.desactivar(inactiva, T0.plusDays(1), actor());

            assertThat(combinacionCruda(activa)).isEqualTo("1|NULL|NULL");
            assertThat(combinacionCruda(inactiva)).isEqualTo("0|SET|SET");
            return null;
        });
    }

    @Test
    @DisplayName("25. id_acta_contexto valido se acepta y se recupera")
    void contextoDeActaValido() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(new FalObservacion(
                    null, EntidadTipoObservada.DOCUMENTO, ENTIDAD_SIN_PADRE, null,
                    MARCA + "Con contexto", OrigenObservacion.USUARIO, actaId, T0, actor())).getId();

            assertThat(repositorio.buscarPorId(id).orElseThrow().getIdActaContexto()).isEqualTo(actaId);
            return null;
        });
    }

    @Test
    @DisplayName("26. id_acta_contexto inexistente viola fk_obs_acta_contexto")
    void contextoDeActaInexistente() {
        enEscenario(actaId -> {
            FalObservacion huerfana = new FalObservacion(
                    null, EntidadTipoObservada.DOCUMENTO, ENTIDAD_SIN_PADRE, null,
                    MARCA + "Contexto inexistente", OrigenObservacion.USUARIO,
                    -123_456L, T0, actor());

            assertThatThrownBy(() -> repositorio.guardar(huerfana))
                    .isInstanceOf(DataIntegrityViolationException.class);
            return null;
        });
    }

    /**
     * DECISION_OBS-01: cuando el propietario es el acta, el contexto no se informa.
     * El CASCADE de fk_obs_acta_contexto no alcanza a estas filas y su limpieza es
     * transaccional explicita.
     */
    @Test
    @DisplayName("27. Con ACTA propietaria, id_acta_contexto queda NULL en la columna")
    void actaPropietariaConContextoNulo() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(nueva(actaId, "Acta propietaria", T0)).getId();

            FalObservacion leida = repositorio.buscarPorId(id).orElseThrow();
            assertThat(leida.getEntidadTipo()).isEqualTo(EntidadTipoObservada.ACTA);
            assertThat(leida.getEntidadId()).isEqualTo(actaId);
            assertThat(leida.getIdActaContexto()).isNull();

            Integer conContextoNulo = jdbc.sql("""
                            SELECT COUNT(*) FROM fal_observacion
                            WHERE id = :id AND id_acta_contexto IS NULL
                            """)
                    .param("id", id)
                    .query(Integer.class)
                    .single();
            assertThat(conContextoNulo).as("la columna cruda debe ser NULL").isEqualTo(1);
            return null;
        });
    }

    /**
     * No hay FK sobre entidad_id: MariaDB acepta un propietario que no existe. El
     * test lo deja explicito para que nadie asuma una integridad que el motor no da.
     */
    @Test
    @DisplayName("28. No existe FK polimorfica: entidad_id sin padre se acepta y no hay constraint")
    void sinFkPolimorfica() {
        enEscenario(actaId -> {
            Long id = repositorio.guardar(new FalObservacion(
                    null, EntidadTipoObservada.NOTIFICACION, ENTIDAD_SIN_PADRE, null,
                    MARCA + "Propietario sin padre", OrigenObservacion.USUARIO, null, T0, actor())).getId();

            assertThat(repositorio.buscarPorId(id)).isPresent();

            List<String> fks = jdbc.sql("""
                            SELECT CONSTRAINT_NAME
                            FROM information_schema.KEY_COLUMN_USAGE
                            WHERE TABLE_SCHEMA = DATABASE()
                              AND TABLE_NAME = 'fal_observacion'
                              AND COLUMN_NAME = 'entidad_id'
                              AND REFERENCED_TABLE_NAME IS NOT NULL
                            """)
                    .query(String.class)
                    .list();
            assertThat(fks).isEmpty();
            return null;
        });
    }

    /**
     * origen_observacion es NOT NULL en la base pero el modelo no lo valida. El
     * adaptador no inventa un default: el fallo llega como violacion de integridad.
     */
    @Test
    @DisplayName("29. origen_observacion nulo llega como violacion de integridad, sin default inventado")
    void origenNuloNoRecibeDefault() {
        enEscenario(actaId -> {
            FalObservacion sinOrigen = new FalObservacion(null, EntidadTipoObservada.ACTA, actaId,
                    null, MARCA + "Sin origen", null, null, T0, actor());

            assertThatThrownBy(() -> repositorio.guardar(sinOrigen))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(PrecondicionVioladaException.class);
            return null;
        });
    }

    @Test
    @DisplayName("30. La tabla no tiene version_row ni UNIQUE logico por propietario")
    void sinVersionRowNiUniquePorPropietario() {
        List<String> columnas = jdbc.sql("""
                        SELECT COLUMN_NAME FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fal_observacion'
                        """)
                .query(String.class)
                .list();
        List<String> uniques = jdbc.sql("""
                        SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fal_observacion'
                          AND CONSTRAINT_TYPE = 'UNIQUE'
                        """)
                .query(String.class)
                .list();

        assertThat(columnas).hasSize(11).doesNotContain("version_row");
        assertThat(uniques).as("sin UNIQUE: la cardinalidad es 1 a 0..N").isEmpty();
    }

    // =====================================================================
    // PARIDAD CON EL ADAPTADOR INMEMORY
    // =====================================================================

    /**
     * El oraculo es una instancia InMemory real, no un literal duplicado en el test:
     * si el mensaje cambia en un adaptador, el otro debe cambiar igual.
     */
    @Test
    @DisplayName("31. Paridad: ambos adaptadores rechazan igual un id informado y una baja imposible")
    void paridadDeExcepciones() {
        enEscenario(actaId -> {
            ObservacionRepository enMemoria = new InMemoryObservacionRepository();
            FalObservacion conId = nueva(actaId, "Con id", T0).conId(555_555L);

            Throwable altaJdbc = catchThrowable(() -> repositorio.guardar(conId));
            Throwable altaMemoria = catchThrowable(() -> enMemoria.guardar(conId));
            assertThat(altaJdbc).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(altaMemoria).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(altaJdbc.getMessage()).isEqualTo(altaMemoria.getMessage());

            Throwable bajaJdbc = catchThrowable(() -> repositorio.desactivar(-9L, T0, actor()));
            Throwable bajaMemoria = catchThrowable(() -> enMemoria.desactivar(-9L, T0, actor()));
            assertThat(bajaJdbc).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(bajaMemoria).isInstanceOf(PrecondicionVioladaException.class);
            assertThat(bajaJdbc.getMessage()).isEqualTo(bajaMemoria.getMessage());
            return null;
        });
    }

    @Test
    @DisplayName("32. Paridad: mismo orden canonico, mismo filtro de activas y misma segunda baja")
    void paridadDeOrdenFiltroYSegundaBaja() {
        enEscenario(actaId -> {
            ObservacionRepository enMemoria = new InMemoryObservacionRepository();

            List<FalObservacion> altas = List.of(
                    nueva(actaId, "Tardia", T0.plusHours(2)),
                    nueva(actaId, "Temprana", T0),
                    nueva(actaId, "Empatada A", T0.plusHours(1)),
                    nueva(actaId, "Empatada B", T0.plusHours(1)));

            List<Long> idsJdbc = altas.stream().map(o -> repositorio.guardar(o).getId()).toList();
            List<Long> idsMemoria = altas.stream().map(o -> enMemoria.guardar(o).getId()).toList();

            assertThat(textosDe(repositorio.listarPorEntidad(EntidadTipoObservada.ACTA, actaId)))
                    .containsExactly(MARCA + "Temprana", MARCA + "Empatada A",
                            MARCA + "Empatada B", MARCA + "Tardia")
                    .isEqualTo(textosDe(enMemoria.listarPorEntidad(EntidadTipoObservada.ACTA, actaId)));

            repositorio.desactivar(idsJdbc.get(0), T0.plusDays(1), actor());
            enMemoria.desactivar(idsMemoria.get(0), T0.plusDays(1), actor());

            assertThat(textosDe(repositorio.listarActivasPorEntidad(EntidadTipoObservada.ACTA, actaId)))
                    .isEqualTo(textosDe(enMemoria.listarActivasPorEntidad(EntidadTipoObservada.ACTA, actaId)));

            Throwable deJdbc = catchThrowable(() -> repositorio.desactivar(idsJdbc.get(0), T0.plusDays(2), actor()));
            Throwable deMemoria = catchThrowable(() -> enMemoria.desactivar(idsMemoria.get(0), T0.plusDays(2), actor()));
            assertThat(deJdbc).isInstanceOf(IllegalStateException.class);
            assertThat(deMemoria).isInstanceOf(IllegalStateException.class);
            return null;
        });
    }

    // =====================================================================
    // BATCH DE PROPIETARIOS (DECISION-OBS-BATCH-01)
    // =====================================================================

    @Test
    @DisplayName("35. listarPorEntidades resuelve varios propietarios en una sola sentencia")
    void batchResuelveVariosPropietarios() {
        enEscenario(actaId -> {
            Long otra = insertarPadreMinimo();
            Long tercera = insertarPadreMinimo();
            repositorio.guardar(nueva(actaId, "de la primera", T0));
            repositorio.guardar(nueva(otra, "de la segunda", T0));
            repositorio.guardar(nueva(tercera, "de la tercera", T0));

            Map<Long, List<FalObservacion>> historiales = repositorio.listarPorEntidades(
                    EntidadTipoObservada.ACTA, List.of(actaId, otra, tercera));

            assertThat(historiales).containsOnlyKeys(actaId, otra, tercera);
            assertThat(textosDe(historiales.get(actaId))).containsExactly(MARCA + "de la primera");
            assertThat(textosDe(historiales.get(otra))).containsExactly(MARCA + "de la segunda");
            assertThat(textosDe(historiales.get(tercera))).containsExactly(MARCA + "de la tercera");
            return null;
        });
    }

    /**
     * La ausencia de SQL con la coleccion vacia se demuestra materialmente: el adaptador
     * se construye sobre un DataSource que falla al pedir conexion. Si la operacion
     * intentara consultar, la excepcion afloraria. Con un id la excepcion si aparece, de
     * modo que el test no puede pasar por una falla silenciosa del doble.
     *
     * <p>Ademas {@code entidad_id IN ()} no es sintaxis valida: la unica alternativa a
     * cortar antes seria emitir una sentencia que el motor rechaza.
     */
    @Test
    @DisplayName("36. Una coleccion vacia devuelve un mapa vacio y no ejecuta SQL")
    void coleccionVaciaNoEjecutaSql() {
        ObservacionRepository sinBase = new JdbcObservacionRepository(JdbcClient.create(DATASOURCE_QUE_FALLA));

        assertThat(sinBase.listarPorEntidades(EntidadTipoObservada.ACTA, List.of()))
                .as("con la coleccion vacia no se toca la persistencia")
                .isEmpty();

        assertThatThrownBy(() -> sinBase.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(1L)))
                .as("con al menos un id el adaptador si intenta consultar")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("37. Ids repetidos en la entrada no duplican filas ni grupos")
    void batchConIdsRepetidos() {
        enEscenario(actaId -> {
            repositorio.guardar(nueva(actaId, "unica del batch", T0));

            Map<Long, List<FalObservacion>> historiales = repositorio.listarPorEntidades(
                    EntidadTipoObservada.ACTA, List.of(actaId, actaId, actaId));

            assertThat(historiales).hasSize(1);
            assertThat(historiales.get(actaId)).hasSize(1);
            return null;
        });
    }

    @Test
    @DisplayName("38. El batch excluye las observaciones de otro entidad_tipo")
    void batchExcluyeOtroTipo() {
        enEscenario(actaId -> {
            repositorio.guardar(nueva(actaId, "propia del acta", T0));
            repositorio.guardar(new FalObservacion(null, EntidadTipoObservada.PARALIZACION, actaId,
                    null, MARCA + "de la paralizacion", OrigenObservacion.USUARIO, actaId, T0, actor()));

            assertThat(textosDe(repositorio
                    .listarPorEntidades(EntidadTipoObservada.ACTA, List.of(actaId))
                    .get(actaId)))
                    .containsExactly(MARCA + "propia del acta");
            return null;
        });
    }

    @Test
    @DisplayName("39. El batch excluye las observaciones de ids no solicitados")
    void batchExcluyeIdsNoSolicitados() {
        enEscenario(actaId -> {
            Long otra = insertarPadreMinimo();
            repositorio.guardar(nueva(actaId, "pedida", T0));
            repositorio.guardar(nueva(otra, "no pedida", T0));

            Map<Long, List<FalObservacion>> historiales =
                    repositorio.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(actaId));

            assertThat(historiales).containsOnlyKeys(actaId);
            assertThat(textosDe(historiales.get(actaId))).containsExactly(MARCA + "pedida");
            return null;
        });
    }

    @Test
    @DisplayName("40. El batch incluye activas e inactivas: la inicial dada de baja sigue siendo la primera")
    void batchIncluyeActivasEInactivas() {
        enEscenario(actaId -> {
            Long inicial = repositorio.guardar(nueva(actaId, "inicial del batch", T0)).getId();
            repositorio.guardar(nueva(actaId, "correccion del batch", T0.plusDays(1)));
            repositorio.desactivar(inicial, T0.plusDays(1), actor());

            List<FalObservacion> historial = repositorio
                    .listarPorEntidades(EntidadTipoObservada.ACTA, List.of(actaId))
                    .get(actaId);

            assertThat(textosDe(historial))
                    .containsExactly(MARCA + "inicial del batch", MARCA + "correccion del batch");
            assertThat(historial.get(0).isSiActiva()).isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("41. Cada historial conserva fh_alta ASC, id ASC y el mismo instante desempata por id")
    void batchConservaElOrdenCanonicoPorPropietario() {
        enEscenario(actaId -> {
            Long otra = insertarPadreMinimo();
            repositorio.guardar(nueva(actaId, "A-tardia", T0.plusHours(2)));
            Long tardiaB = repositorio.guardar(nueva(otra, "B-tardia", T0.plusHours(9))).getId();
            repositorio.guardar(nueva(actaId, "A-temprana", T0));
            Long empatadaA = repositorio.guardar(nueva(otra, "B-empatada-1", T0.plusHours(1))).getId();
            Long empatadaB = repositorio.guardar(nueva(otra, "B-empatada-2", T0.plusHours(1))).getId();

            Map<Long, List<FalObservacion>> historiales =
                    repositorio.listarPorEntidades(EntidadTipoObservada.ACTA, List.of(actaId, otra));

            assertThat(textosDe(historiales.get(actaId)))
                    .containsExactly(MARCA + "A-temprana", MARCA + "A-tardia");
            assertThat(historiales.get(otra))
                    .extracting(FalObservacion::getId)
                    .as("las dos filas del mismo fh_alta se ordenan por id, antes de la tardia")
                    .containsExactly(empatadaA, empatadaB, tardiaB);
            assertThat(textosDe(historiales.get(otra)))
                    .containsExactly(MARCA + "B-empatada-1", MARCA + "B-empatada-2", MARCA + "B-tardia");
            return null;
        });
    }

    @Test
    @DisplayName("42. Un propietario sin filas esta ausente del mapa")
    void batchOmiteAlPropietarioSinFilas() {
        enEscenario(actaId -> {
            Long sinObservaciones = insertarPadreMinimo();
            repositorio.guardar(nueva(actaId, "con observacion", T0));

            Map<Long, List<FalObservacion>> historiales = repositorio.listarPorEntidades(
                    EntidadTipoObservada.ACTA, List.of(actaId, sinObservaciones, ENTIDAD_SIN_PADRE));

            assertThat(historiales).containsOnlyKeys(actaId);
            assertThat(historiales.getOrDefault(sinObservaciones, List.of())).isEmpty();
            assertThat(historiales.getOrDefault(ENTIDAD_SIN_PADRE, List.of())).isEmpty();
            return null;
        });
    }

    @Test
    @DisplayName("43. Paridad del batch: JDBC e InMemory devuelven el mismo agrupado y el mismo orden")
    void paridadDelBatch() {
        enEscenario(actaId -> {
            Long otra = insertarPadreMinimo();
            ObservacionRepository enMemoria = new InMemoryObservacionRepository();

            List<FalObservacion> altas = List.of(
                    nueva(actaId, "P-tardia", T0.plusHours(2)),
                    nueva(otra, "Q-unica", T0.plusHours(1)),
                    nueva(actaId, "P-temprana", T0));
            altas.forEach(o -> repositorio.guardar(o));
            altas.forEach(o -> enMemoria.guardar(o));

            List<Long> pedidos = List.of(actaId, otra, ENTIDAD_SIN_PADRE);
            Map<Long, List<FalObservacion>> deJdbc =
                    repositorio.listarPorEntidades(EntidadTipoObservada.ACTA, pedidos);
            Map<Long, List<FalObservacion>> deMemoria =
                    enMemoria.listarPorEntidades(EntidadTipoObservada.ACTA, pedidos);

            assertThat(deJdbc.keySet()).isEqualTo(deMemoria.keySet());
            for (Long id : pedidos) {
                assertThat(textosDe(deJdbc.getOrDefault(id, List.of())))
                        .as("historial del propietario %s", id)
                        .isEqualTo(textosDe(deMemoria.getOrDefault(id, List.of())));
            }
            return null;
        });
    }

    @Test
    @DisplayName("44. Paridad de rechazos: ambos adaptadores rechazan igual tipo nulo, coleccion nula e id nulo")
    void paridadDeRechazosDelBatch() {
        ObservacionRepository enMemoria = new InMemoryObservacionRepository();
        List<Long> conIdNulo = java.util.Arrays.asList(1L, null);

        Throwable tipoJdbc = catchThrowable(() -> repositorio.listarPorEntidades(null, List.of(1L)));
        Throwable tipoMemoria = catchThrowable(() -> enMemoria.listarPorEntidades(null, List.of(1L)));
        assertThat(tipoJdbc).isInstanceOf(IllegalArgumentException.class);
        assertThat(tipoJdbc.getMessage()).isEqualTo(tipoMemoria.getMessage());

        Throwable colJdbc = catchThrowable(() -> repositorio.listarPorEntidades(EntidadTipoObservada.ACTA, null));
        Throwable colMemoria = catchThrowable(() -> enMemoria.listarPorEntidades(EntidadTipoObservada.ACTA, null));
        assertThat(colJdbc).isInstanceOf(IllegalArgumentException.class);
        assertThat(colJdbc.getMessage()).isEqualTo(colMemoria.getMessage());

        Throwable idJdbc = catchThrowable(() -> repositorio.listarPorEntidades(EntidadTipoObservada.ACTA, conIdNulo));
        Throwable idMemoria = catchThrowable(() -> enMemoria.listarPorEntidades(EntidadTipoObservada.ACTA, conIdNulo));
        assertThat(idJdbc).isInstanceOf(IllegalArgumentException.class);
        assertThat(idJdbc.getMessage()).isEqualTo(idMemoria.getMessage());
    }

    /**
     * El filtro del batch se sirve con el prefijo izquierdo (entidad_tipo, entidad_id) de
     * ix_obs_entidad. El indice ya existe y no se modifica: este caso lo verifica para que
     * una baja o un renombre futuro rompa el build.
     */
    @Test
    @DisplayName("45. ix_obs_entidad conserva el prefijo (entidad_tipo, entidad_id) que sirve el batch")
    void indiceQueSirveElBatchSigueIntacto() {
        List<String> columnas = jdbc.sql("""
                        SELECT COLUMN_NAME FROM information_schema.STATISTICS
                        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fal_observacion'
                          AND INDEX_NAME = 'ix_obs_entidad'
                        ORDER BY SEQ_IN_INDEX
                        """)
                .query(String.class)
                .list();

        assertThat(columnas)
                .as("ix_obs_entidad debe empezar por (entidad_tipo, entidad_id)")
                .startsWith("entidad_tipo", "entidad_id");
        assertThat(columnas).containsExactly("entidad_tipo", "entidad_id", "si_activa", "fh_alta");
    }

    // =====================================================================
    // TRANSACCION Y RESIDUOS
    // =====================================================================

    @Test
    @DisplayName("33. Rollback real: la fila es visible dentro de la transaccion y desaparece al revertir")
    void rollbackRealDejaLaBaseSinRastro() {
        long[] ids = new long[2];

        new TransactionTemplate(txManager).execute(status -> {
            Long actaId = insertarPadreMinimo();
            ids[0] = repositorio.guardar(nueva(actaId, "Se revierte", T0)).getId();
            ids[1] = actaId;

            assertThat(repositorio.buscarPorId(ids[0])).isPresent();
            status.setRollbackOnly();
            return null;
        });

        assertThat(contarPorId("fal_observacion", ids[0]))
                .as("la observacion %s no debe sobrevivir al rollback", ids[0])
                .isZero();
        assertThat(contarPorId("fal_acta", ids[1]))
                .as("el acta de contexto %s no debe sobrevivir al rollback", ids[1])
                .isZero();
    }

    @Test
    @DisplayName("34. La suite no deja filas con la marca del slice")
    void sinResiduosDeLaMarcaDelSlice() {
        Integer residuos = jdbc.sql("SELECT COUNT(*) FROM fal_observacion WHERE observacion LIKE :marca")
                .param("marca", MARCA + "%")
                .query(Integer.class)
                .single();

        assertThat(residuos)
                .as("ninguna fila con marca %s debe quedar fuera de una transaccion revertida", MARCA)
                .isZero();
    }

    // =====================================================================
    // SOPORTE
    // =====================================================================

    /**
     * Ejecuta el escenario dentro de una transaccion que siempre se revierte, con un
     * acta ya disponible para usar como propietario o como contexto.
     */
    private <T> T enEscenario(Function<Long, T> escenario) {
        return new TransactionTemplate(txManager).execute(status -> {
            try {
                return escenario.apply(insertarPadreMinimo());
            } finally {
                status.setRollbackOnly();
            }
        });
    }

    /** Observacion nueva cuyo propietario es el acta: contexto NULL (DECISION_OBS-01). */
    private static FalObservacion nueva(Long actaId, String texto, LocalDateTime fhAlta) {
        return new FalObservacion(null, EntidadTipoObservada.ACTA, actaId, null,
                MARCA + texto, OrigenObservacion.USUARIO, null, fhAlta, actor());
    }

    /** Subject de prueba de 36 caracteres, dentro de CHAR(36). */
    private static String actor() {
        return UUID.randomUUID().toString();
    }

    private static List<String> textosDe(List<FalObservacion> observaciones) {
        return observaciones.stream().map(FalObservacion::getObservacion).toList();
    }

    /**
     * INSERT directo para ejercer combinaciones que el modelo de dominio no permite
     * construir. Es el unico camino por el que este IT escribe sin pasar por el
     * adaptador.
     */
    private void insertarCrudo(Long actaId, String texto, boolean siActiva,
                               LocalDateTime fhBaja, String idUserBaja) {
        jdbc.sql("""
                        INSERT INTO fal_observacion (
                            entidad_tipo, entidad_id, id_acta_contexto, origen_observacion,
                            observacion, si_activa, fh_alta, id_user_alta, fh_baja, id_user_baja
                        ) VALUES (
                            :entidadTipo, :entidadId, NULL, :origen,
                            :observacion, :siActiva, :fhAlta, :idUserAlta, :fhBaja, :idUserBaja
                        )
                        """)
                .param("entidadTipo", (short) EntidadTipoObservada.ACTA.codigo())
                .param("entidadId", actaId)
                .param("origen", (short) OrigenObservacion.USUARIO.codigo())
                .param("observacion", texto)
                .param("siActiva", siActiva)
                .param("fhAlta", T0)
                .param("idUserAlta", actor())
                .param("fhBaja", fhBaja)
                .param("idUserBaja", idUserBaja)
                .update();
    }

    /** Combinacion cruda de si_activa/fh_baja/id_user_baja, sin pasar por el modelo. */
    private String combinacionCruda(Long id) {
        return jdbc.sql("""
                        SELECT CONCAT(
                            si_activa, '|',
                            IF(fh_baja IS NULL, 'NULL', 'SET'), '|',
                            IF(id_user_baja IS NULL, 'NULL', 'SET'))
                        FROM fal_observacion WHERE id = :id
                        """)
                .param("id", id)
                .query(String.class)
                .single();
    }

    private int contarPorId(String tabla, long id) {
        Integer total = jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .param("id", id)
                .query(Integer.class)
                .single();
        return total == null ? 0 : total;
    }

    /**
     * Inserta la cadena minima que el DDL exige para poder referenciar un acta:
     * fal_dependencia -> fal_dependencia_version -> fal_persona -> fal_acta.
     *
     * Mismo patron que JdbcActaEventoRepositoryIT. Solo se completan las columnas
     * NOT NULL sin default; el resto queda en NULL. doc_key de fal_persona es STORED
     * GENERATED y por eso no se informa.
     */
    private Long insertarPadreMinimo() {
        LocalDateTime ahora = T0;
        String usuario = actor();

        Long idDep = insertarYObtenerId("""
                INSERT INTO fal_dependencia (nom_dep, fh_alta, id_user_alta)
                VALUES (:nom, :fhAlta, :usuario)
                """, spec -> spec
                .param("nom", MARCA + "DEP")
                .param("fhAlta", ahora)
                .param("usuario", usuario));

        short verDep = 1;
        jdbc.sql("""
                        INSERT INTO fal_dependencia_version
                            (id_dep, ver_dep, tipo_acta, fh_vig_desde, fh_alta, id_user_alta)
                        VALUES (:idDep, :verDep, :tipoActa, :vigDesde, :fhAlta, :usuario)
                        """)
                .param("idDep", idDep)
                .param("verDep", verDep)
                .param("tipoActa", (short) 1)
                .param("vigDesde", LocalDate.of(2026, 1, 1))
                .param("fhAlta", ahora)
                .param("usuario", usuario)
                .update();

        Long idPersona = insertarYObtenerId("""
                INSERT INTO fal_persona
                    (tipo_persona, tipo_documento, nro_doc, apellido, nombres, fh_alta, id_user_alta)
                VALUES (:tipoPersona, :tipoDoc, :nroDoc, :apellido, :nombres, :fhAlta, :usuario)
                """, spec -> spec
                .param("tipoPersona", (short) 1)
                .param("tipoDoc", (short) 1)
                .param("nroDoc", nroDocumentoDePrueba())
                .param("apellido", "WAVE1A2B0")
                .param("nombres", "TEST")
                .param("fhAlta", ahora)
                .param("usuario", usuario));

        return insertarYObtenerId("""
                INSERT INTO fal_acta (
                    id_tecnico, tipo_acta, origen_captura, fh_captura,
                    fecha_acta, fecha_labrado, id_dep, ver_dep,
                    situacion_identificacion_infractor, id_persona_infractor,
                    codigo_qr, bloque_actual, est_proc_act, sit_adm_act,
                    resultado_firma_infractor, fh_alta, id_user_alta
                ) VALUES (
                    :idTecnico, :tipoActa, :origenCaptura, :fhCaptura,
                    :fechaActa, :fechaLabrado, :idDep, :verDep,
                    :situacionIdentificacion, :idPersona,
                    :codigoQr, :bloque, :estProc, :sitAdm,
                    :resultadoFirma, :fhAlta, :usuario
                )
                """, spec -> spec
                .param("idTecnico", UUID.randomUUID().toString())
                .param("tipoActa", (short) 1)
                .param("origenCaptura", OrigenCaptura.WEB_OPERADOR.codigo())
                .param("fhCaptura", ahora)
                .param("fechaActa", ahora.toLocalDate())
                .param("fechaLabrado", ahora)
                .param("idDep", idDep)
                .param("verDep", verDep)
                .param("situacionIdentificacion",
                        SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO.codigo())
                .param("idPersona", idPersona)
                .param("codigoQr", MARCA + UUID.randomUUID())
                .param("bloque", BloqueActual.ENRI.codigo())
                .param("estProc", EstadoProcesalActa.EN_TRAMITE.codigo())
                .param("sitAdm", SituacionAdministrativaActa.ACTIVA.codigo())
                .param("resultadoFirma", ResultadoFirmaInfractor.FIRMADA.codigo())
                .param("fhAlta", ahora)
                .param("usuario", usuario));
    }

    private Long insertarYObtenerId(String sql,
                                    Function<JdbcClient.StatementSpec, JdbcClient.StatementSpec> parametros) {
        KeyHolder claves = new GeneratedKeyHolder();
        parametros.apply(jdbc.sql(sql)).update(claves);
        return Optional.ofNullable(claves.getKey())
                .map(Number::longValue)
                .orElseThrow(() -> new IllegalStateException("La fixture no obtuvo el id generado"));
    }

    /**
     * nro_doc es int unsigned con CHECK entre 1 y 99999999 y participa del UNIQUE
     * (tipo_documento, doc_key). Se usa un valor aleatorio del rango alto para no
     * chocar con datos funcionales; de todos modos la fila se revierte.
     */
    private static int nroDocumentoDePrueba() {
        return 90_000_000 + (int) (Math.random() * 9_000_000);
    }
}
