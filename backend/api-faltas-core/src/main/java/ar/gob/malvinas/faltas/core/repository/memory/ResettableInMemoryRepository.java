package ar.gob.malvinas.faltas.core.repository.memory;

/**
 * Contrato de reset para repositorios in-memory.
 *
 * Permite al DevInMemoryResetService limpiar y recuperar el estado inicial
 * de todos los repositorios in-memory sin reiniciar la aplicacion.
 *
 * Solo para uso en dev/test/demo.
 * No debe implementarse en repositorios JDBC/MariaDB productivos.
 *
 * Slice 8F-5.
 */
public interface ResettableInMemoryRepository {

    /**
     * Limpia el estado interno del repositorio y resetea los contadores de secuencia.
     * Debe ser idempotente.
     */
    void reset();

    /** Nombre legible del repositorio, usado en el reporte de reset. */
    String nombre();

    /** Cantidad total de elementos actualmente almacenados. */
    int size();
}
