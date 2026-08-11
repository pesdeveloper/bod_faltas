package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link MariaDbTransientFailureClassifier} (Wave 4 Parte 3E §17).
 *
 * <p>Cubre los dos codigos reconocidos (1213 deadlock, 1205 lock wait timeout), su SQLSTATE
 * equivalente, cadenas de causa anidadas, y una lista explicita de errores NO reintentables
 * para verificar que la clasificacion no se amplia por tipo.
 */
class MariaDbTransientFailureClassifierTest {

    private final MariaDbTransientFailureClassifier classifier = new MariaDbTransientFailureClassifier();

    @Test
    void nullNoEsTransitorio() {
        assertThat(classifier.clasificar(null)).isNull();
        assertThat(classifier.esTransitorio(null)).isFalse();
    }

    @Test
    void reconoceCodigo1213ComoDeadlock() {
        SQLException sqlException = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        RuntimeException error = new UncategorizedSQLException("update", "UPDATE ...", sqlException);

        assertThat(classifier.clasificar(error)).isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
        assertThat(classifier.esTransitorio(error)).isTrue();
    }

    @Test
    void reconoceCodigo1205ComoLockWaitTimeout() {
        SQLException sqlException = new SQLException("Lock wait timeout exceeded", "HY000", 1205);
        RuntimeException error = new UncategorizedSQLException("update", "UPDATE ...", sqlException);

        assertThat(classifier.clasificar(error)).isEqualTo(CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT);
    }

    @Test
    void reconoceSqlState40001SinCodigoDeErrorExplicito() {
        // errorCode = 0 a proposito: solo el SQLSTATE identifica la serializacion fallida.
        SQLException sqlException = new SQLException("Serialization failure", "40001", 0);
        RuntimeException error = new UncategorizedSQLException("update", "UPDATE ...", sqlException);

        assertThat(classifier.clasificar(error)).isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
    }

    @Test
    void recorreCadenaDeCausasAnidadaHastaEncontrarElSqlException() {
        SQLException sqlException = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        RuntimeException envoltorioIntermedio = new IllegalStateException("fallo de repositorio", sqlException);
        RuntimeException envoltorioExterno = new RuntimeException("fallo de servicio", envoltorioIntermedio);

        assertThat(classifier.clasificar(envoltorioExterno)).isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
    }

    @Test
    void recorreLaCadenaEncadenadaDeSqlExceptionGetNextException() {
        SQLException primeraSinCodigoReconocido = new SQLException("advertencia previa", "01000", 9999);
        SQLException segundaConDeadlock = new SQLException("Deadlock found when trying to get lock", "40001", 1213);
        primeraSinCodigoReconocido.setNextException(segundaConDeadlock);

        assertThat(classifier.clasificar(primeraSinCodigoReconocido))
                .isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
    }

    @Test
    void respaldoPorTipoSpringReconoceDeadlockLoserSinSqlExceptionEnLaCadena() {
        RuntimeException error = new DeadlockLoserDataAccessException("deadlock", null);
        assertThat(classifier.clasificar(error)).isEqualTo(CategoriaFalloTransitorioMariaDb.DEADLOCK);
    }

    @Test
    void respaldoPorTipoSpringReconoceCannotAcquireLockSinSqlExceptionEnLaCadena() {
        RuntimeException error = new CannotAcquireLockException("lock wait timeout", null);
        assertThat(classifier.clasificar(error)).isEqualTo(CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT);
    }

    @Test
    void duplicateKeyNoEsTransitorio() {
        SQLException sqlException = new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062);
        RuntimeException error = new DuplicateKeyException("uq_fal_acta_id_tecnico", sqlException);

        assertThat(classifier.clasificar(error)).isNull();
        assertThat(classifier.esTransitorio(error)).isFalse();
    }

    @Test
    void errorDeSintaxisSqlNoEsTransitorio() {
        SQLException sqlException = new SQLException("You have an error in your SQL syntax", "42000", 1064);
        RuntimeException error = new BadSqlGrammarException("query", "SELECT ...", sqlException);

        assertThat(classifier.clasificar(error)).isNull();
    }

    @Test
    void constraintDeIntegridadGenericaNoEsTransitoria() {
        SQLException sqlException = new SQLException("Cannot add or update a child row", "23000", 1452);
        RuntimeException error = new DataIntegrityViolationException("fk violada", sqlException);

        assertThat(classifier.clasificar(error)).isNull();
    }

    @Test
    void excepcionFuncionalSinCausaSqlNoEsTransitoria() {
        RuntimeException error = new IllegalArgumentException("dato invalido");
        assertThat(classifier.clasificar(error)).isNull();
    }

    @Test
    void sqlExceptionConCodigoDesconocidoNoSeClasificaComoTransitoriaPorAproximacion() {
        // Codigo de error real de MariaDB no reconocido por esta clasificacion (por ejemplo,
        // 1042 "unable to connect"): no debe tratarse como transitorio por aproximacion.
        SQLException sqlException = new SQLException("Unable to connect to database", "08S01", 1042);
        RuntimeException error = new UncategorizedSQLException("connect", null, sqlException);

        assertThat(classifier.clasificar(error)).isNull();
    }

    @Test
    void cadenaDeCausasMuyProfundaTerminaSinBloquearse() {
        Throwable profunda = new IllegalArgumentException("raiz sin SQLException");
        for (int i = 0; i < 500; i++) {
            profunda = new RuntimeException("nivel " + i, profunda);
        }
        assertThat(classifier.clasificar(profunda)).isNull();
    }
}
