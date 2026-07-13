package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.application.service.CalculadorPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.application.service.CalendarioAdministrativoService;
import ar.gob.malvinas.faltas.core.application.service.PlazosAdministrativosService;
import ar.gob.malvinas.faltas.core.infrastructure.config.PlazosAdministrativosProperties;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDiaNoComputableRepository;

/**
 * Soporte de test para construir un {@link PlazosAdministrativosService} determinista.
 *
 * Usa la clase productiva {@link PlazosAdministrativosProperties} (default 30 dias, valor canonico
 * del dominio; no se hardcodea 30 aqui) con un calendario local vacio. Permite que los tests de
 * regresion que cablean {@code NotificacionService} manualmente obtengan un servicio de plazos real
 * sin duplicar la configuracion global.
 */
public final class PlazosTestSupport {

    private PlazosTestSupport() {}

    /** Construye un servicio de plazos con calendario vacio y configuracion global por defecto. */
    public static PlazosAdministrativosService conCalendarioVacio(FaltasClock clock) {
        CalendarioAdministrativoService calendario =
                new CalendarioAdministrativoService(new InMemoryDiaNoComputableRepository(), clock);
        return new PlazosAdministrativosService(
                new PlazosAdministrativosProperties(),
                new CalculadorPlazosAdministrativos(calendario));
    }
}
