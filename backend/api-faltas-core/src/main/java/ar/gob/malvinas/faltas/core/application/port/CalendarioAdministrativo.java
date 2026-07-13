package ar.gob.malvinas.faltas.core.application.port;

import java.time.LocalDate;

/**
 * Puerto de consulta del calendario administrativo local.
 *
 * El calculador depende de esta interfaz, no de un repositorio ni de una clase concreta.
 * La separacion permite que una futura sincronizacion con un proveedor externo actualice
 * el repositorio local sin modificar el calculador.
 */
public interface CalendarioAdministrativo {

    /**
     * Devuelve true si la fecha es un dia computable administrativamente.
     *
     * Reglas fijas (no persisten en el repositorio):
     *   - Domingo: nunca computable.
     *   - 1 de enero: nunca computable.
     *   - 1 de mayo: nunca computable.
     *
     * Excepciones locales activas (persisten en el repositorio):
     *   - cualquier fecha marcada activamente como no computable.
     *
     * El sabado es computable salvo excepcion activa.
     */
    boolean esDiaComputable(LocalDate fecha);
}
