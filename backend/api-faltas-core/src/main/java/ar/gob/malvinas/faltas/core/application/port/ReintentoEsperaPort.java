package ar.gob.malvinas.faltas.core.application.port;

/**
 * Puerto de espera entre reintentos (Wave 4 Parte 3E §10, §17).
 *
 * <p>Abstrae {@code Thread.sleep} para que la política de reintento del alta sea testeable
 * sin sleeps reales: los tests unitarios inyectan una implementación instantánea que registra
 * las esperas solicitadas en vez de bloquear el hilo.
 */
public interface ReintentoEsperaPort {

    /**
     * Espera la cantidad de milisegundos indicada antes de que el orquestador de reintento
     * ejecute el intento siguiente.
     *
     * @param milisegundos duración de la espera, siempre mayor a cero
     * @throws InterruptedException si el hilo es interrumpido durante la espera; el llamador
     *         debe restaurar {@link Thread#interrupt()} y abortar el reintento, nunca ignorarla
     */
    void esperar(long milisegundos) throws InterruptedException;
}
