package ar.gob.malvinas.faltas.core.infrastructure.time;

import ar.gob.malvinas.faltas.core.application.port.ReintentoEsperaPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador productivo de {@link ReintentoEsperaPort}: espera real con {@link Thread#sleep}.
 */
@Component
public class ThreadSleepReintentoEsperaAdapter implements ReintentoEsperaPort {

    @Override
    public void esperar(long milisegundos) throws InterruptedException {
        Thread.sleep(milisegundos);
    }
}
