package ar.gob.malvinas.faltas.core.application.service;

/**
 * Input mock absoluto de Tesoreria para reconstruir (rebuild) la clasificacion
 * economica agregada. No es parte de la proyeccion derivada: representa el estado
 * absoluto informado por Tesoreria (que movimientos confirmados quedan clasificados
 * como conciliados por Tesoreria y con que referencia).
 *
 * La proyeccion (FalActaEconomiaProyeccion) no mantiene historia local por movimiento.
 */
public interface TesoreriaConciliacionInput {

    /** true si Tesoreria informo el movimiento como conciliado. */
    boolean estaConciliadoTesoreria(Long movimientoId);

    /** Referencia de conciliacion informada por Tesoreria para el movimiento (o null). */
    String referenciaDe(Long movimientoId);

    /**
     * Registra de forma absoluta e idempotente la conciliacion de un movimiento.
     * Devuelve false si la referencia informada es incompatible con la ya registrada.
     */
    boolean registrar(Long movimientoId, String referencia);

    /**
     * Devuelve el snapshot absoluto actual (copia inmutable).
     * La clave es movimientoId, el valor es la referencia de conciliacion.
     */
    java.util.Map<Long, String> snapshotActual();

    /**
     * Reemplaza atomicamente el estado completo con un nuevo snapshot absoluto.
     * Permite quitar o modificar una conciliacion reemplazando el snapshot.
     * No acumula historial.
     */
    void reemplazarEstadoAbsoluto(java.util.Map<Long, String> nuevoSnapshot);
}
