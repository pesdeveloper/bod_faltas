package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.application.port.ConfiguracionPlazosAdministrativos;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Servicio de calculo de plazos administrativos.
 *
 * Combina la configuracion global (cantidad de dias) con el calculador determinista
 * para devolver un resultado completo que incluye el tipo, la fecha de origen,
 * la cantidad aplicada y la fecha de vencimiento.
 */
@Service
public class PlazosAdministrativosService {

    private final ConfiguracionPlazosAdministrativos configuracion;
    private final CalculadorPlazosAdministrativos calculador;

    public PlazosAdministrativosService(
            ConfiguracionPlazosAdministrativos configuracion,
            CalculadorPlazosAdministrativos calculador) {
        this.configuracion = configuracion;
        this.calculador = calculador;
    }

    /**
     * Calcula el vencimiento para el tipo de plazo y fecha de origen indicados.
     * Lee la cantidad global exactamente una vez desde la configuracion.
     */
    public CalculoPlazoAdministrativo calcular(TipoPlazoAdministrativo tipo, LocalDate fechaOrigen) {
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        if (fechaOrigen == null) throw new IllegalArgumentException("fechaOrigen es obligatoria");

        int cantidad = configuracion.cantidadDiasComputables(tipo);
        LocalDate vencimiento = calculador.calcularVencimiento(fechaOrigen, cantidad);
        return new CalculoPlazoAdministrativo(tipo, fechaOrigen, cantidad, vencimiento);
    }

    /**
     * Conveniencia: calcula el vencimiento de apelacion de un fallo condenatorio.
     * Delega al metodo generico con APELACION_FALLO.
     */
    public CalculoPlazoAdministrativo calcularVencimientoApelacion(LocalDate fechaNotificacion) {
        return calcular(TipoPlazoAdministrativo.APELACION_FALLO, fechaNotificacion);
    }
}
