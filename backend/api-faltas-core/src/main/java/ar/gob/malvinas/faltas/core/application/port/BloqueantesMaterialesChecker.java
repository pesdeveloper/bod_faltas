package ar.gob.malvinas.faltas.core.application.port;

/**
 * Puerto de verificacion de bloqueantes materiales activos del expediente.
 *
 * DEUDA TECNICA (Slice 2B): El motor real de bloqueantes no esta implementado.
 * La implementacion por defecto (NoOpBloqueantesMaterialesChecker) devuelve siempre false.
 * Cuando exista el motor real, se reemplaza la implementacion sin tocar los servicios.
 *
 * Se declara como @FunctionalInterface para permitir lambdas en tests.
 */
@FunctionalInterface
public interface BloqueantesMaterialesChecker {
    /**
     * @param actaId identificador del acta a verificar
     * @return true si existen bloqueantes materiales activos que impiden el cierre
     */
    boolean tieneBloqueantesActivos(Long actaId);
}
