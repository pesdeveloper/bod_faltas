package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Clasifica un error de persistencia como transitorio reconocido de MariaDB (Wave 4 Parte
 * 3E §10, {@code AltaActaRetryPolicy} lo consume para decidir el reintento).
 *
 * <p>Reconoce exclusivamente:
 *
 * <pre>
 * 1213 (ER_LOCK_DEADLOCK)        -&gt; DEADLOCK
 * 1205 (ER_LOCK_WAIT_TIMEOUT)    -&gt; LOCK_WAIT_TIMEOUT
 * SQLSTATE 40001 (serialization failure, equivalente reportado por MariaDB para deadlock)
 *                                -&gt; DEADLOCK
 * </pre>
 *
 * <p>La verificación recorre la cadena de causas de principio a fin buscando un
 * {@link SQLException} real (incluida su cadena encadenada vía {@link SQLException#getNextException()}),
 * y decide únicamente por su código de error nativo o su SQLSTATE. Ningún otro
 * {@code DataAccessException} ni ninguna otra categoría de {@link SQLException} se trata como
 * transitorio: no hay clasificación amplia por tipo (por ejemplo, no se trata cualquier
 * {@code TransientDataAccessException} de Spring como reintentable).
 *
 * <p>Si no hay ningún {@link SQLException} en la cadena de causas (por ejemplo, porque algún
 * traductor no estándar no preservó la causa original), se reconocen como respaldo defensivo
 * únicamente los dos tipos que Spring asigna en su traductor por defecto para MySQL/MariaDB
 * exactamente a estos dos códigos ({@link DeadlockLoserDataAccessException} = 1213,
 * {@link CannotAcquireLockException} = 1205); no es una categoría amplia, son los mismos dos
 * códigos vistos por otra vía.
 */
@Component
public class MariaDbTransientFailureClassifier {

    /** Código nativo MariaDB para deadlock (ER_LOCK_DEADLOCK). */
    static final int ERROR_CODE_DEADLOCK = 1213;

    /** Código nativo MariaDB para lock wait timeout (ER_LOCK_WAIT_TIMEOUT). */
    static final int ERROR_CODE_LOCK_WAIT_TIMEOUT = 1205;

    /** SQLSTATE estándar de "serialization failure"; MariaDB lo reporta para deadlock (1213). */
    static final String SQLSTATE_SERIALIZATION_FAILURE = "40001";

    /**
     * @param error error de persistencia observado (puede ser {@code null})
     * @return la categoría transitoria reconocida, o {@code null} si el error no es
     *         reintentable según esta clasificación
     */
    public CategoriaFalloTransitorioMariaDb clasificar(Throwable error) {
        if (error == null) {
            return null;
        }
        SQLException sqlException = buscarSqlExceptionEnCadena(error);
        if (sqlException != null) {
            return clasificarPorCodigoOSqlState(sqlException);
        }
        return clasificarPorTipoSpringDeRespaldo(error);
    }

    public boolean esTransitorio(Throwable error) {
        return clasificar(error) != null;
    }

    private CategoriaFalloTransitorioMariaDb clasificarPorCodigoOSqlState(SQLException raiz) {
        Set<SQLException> visitadas = new HashSet<>();
        SQLException actual = raiz;
        while (actual != null && visitadas.add(actual)) {
            if (actual.getErrorCode() == ERROR_CODE_DEADLOCK) {
                return CategoriaFalloTransitorioMariaDb.DEADLOCK;
            }
            if (actual.getErrorCode() == ERROR_CODE_LOCK_WAIT_TIMEOUT) {
                return CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT;
            }
            if (SQLSTATE_SERIALIZATION_FAILURE.equals(actual.getSQLState())) {
                return CategoriaFalloTransitorioMariaDb.DEADLOCK;
            }
            actual = actual.getNextException();
        }
        // Se encontro un SQLException real, pero ninguno reconocido: NO es transitorio segun
        // esta clasificacion (evita sobre-clasificar por tipo Spring cuando el codigo real ya
        // esta disponible y no coincide).
        return null;
    }

    private CategoriaFalloTransitorioMariaDb clasificarPorTipoSpringDeRespaldo(Throwable error) {
        if (error instanceof DeadlockLoserDataAccessException) {
            return CategoriaFalloTransitorioMariaDb.DEADLOCK;
        }
        if (error instanceof CannotAcquireLockException) {
            return CategoriaFalloTransitorioMariaDb.LOCK_WAIT_TIMEOUT;
        }
        return null;
    }

    private SQLException buscarSqlExceptionEnCadena(Throwable error) {
        Set<Throwable> visitados = new HashSet<>();
        Throwable actual = error;
        while (actual != null && visitados.add(actual)) {
            if (actual instanceof SQLException sqlException) {
                return sqlException;
            }
            actual = actual.getCause();
        }
        return null;
    }
}
