package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

/**
 * Categorías de errores transitorios reconocidos de MariaDB (Wave 4 Parte 3E §10).
 *
 * <p>Catálogo cerrado: no representa cualquier {@code DataAccessException}, sólo los dos
 * códigos nativos que el prompt exige reconocer con precisión. Ampliar este catálogo es una
 * decisión funcional explícita, no una inferencia de {@link MariaDbTransientFailureClassifier}.
 */
public enum CategoriaFalloTransitorioMariaDb {

    /** Código nativo MariaDB 1213 (ER_LOCK_DEADLOCK) / SQLSTATE 40001. */
    DEADLOCK,

    /** Código nativo MariaDB 1205 (ER_LOCK_WAIT_TIMEOUT). */
    LOCK_WAIT_TIMEOUT
}
