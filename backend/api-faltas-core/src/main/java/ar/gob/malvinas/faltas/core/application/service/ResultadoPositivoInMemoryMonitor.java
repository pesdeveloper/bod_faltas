package ar.gob.malvinas.faltas.core.application.service;

/**
 * Monitor de exclusion mutua compartido entre la variante ordinaria (CMD-FALLO-004 via
 * NotificacionService) y la variante portal (CMD-FALLO-004 via NotificacionIntentoService)
 * del resultado notificatorio positivo, dentro de una instancia JVM.
 *
 * Serializa:
 *   - ordinario vs ordinario sobre la misma FalNotificacion;
 *   - portal vs portal sobre la misma FalNotificacion;
 *   - ordinario vs portal sobre la misma FalNotificacion.
 *
 * El perdedor detecta el resultado ya persistido y rechaza antes de capturar el reloj,
 * sin producir efectos secundarios en intento, cabecera, fallo, acta ni snapshot.
 *
 * Uso exclusivo del contexto InMemory. MariaDB debe reemplazar esta exclusion local
 * por transaccion con OCC o bloqueo de la cabecera de notificacion.
 */
final class ResultadoPositivoInMemoryMonitor {

    static final Object INSTANCE = new Object();

    private ResultadoPositivoInMemoryMonitor() {
    }
}
