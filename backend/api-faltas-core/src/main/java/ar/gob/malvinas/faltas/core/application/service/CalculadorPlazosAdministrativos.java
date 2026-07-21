package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.port.CalendarioAdministrativo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Calculador reutilizable de vencimientos administrativos.
 *
 * Dado una fecha de origen y una cantidad de dias computables, avanza desde
 * el dia siguiente contando unicamente dias computables segun el calendario local.
 *
 * Garantias:
 *  - No consulta el reloj (funcion pura respecto del tiempo actual).
 *  - No usa TemporalAdjusters que asuman semana lunes-viernes.
 *  - No hardcodea la cantidad de dias.
 *  - El sabado es computable salvo excepcion activa.
 */
@Service
public class CalculadorPlazosAdministrativos {

    private final CalendarioAdministrativo calendario;

    public CalculadorPlazosAdministrativos(CalendarioAdministrativo calendario) {
        this.calendario = calendario;
    }

    /**
     * Calcula el ultimo dia computable luego de transcurrir {@code cantidadDiasComputables}
     * dias computables desde {@code fechaOrigen} (exclusive).
     *
     * @param fechaOrigen            fecha de notificacion (no se cuenta).
     * @param cantidadDiasComputables cantidad de dias computables a contar (1..3650).
     * @return el ultimo dia computable del plazo.
     */
    public LocalDate calcularVencimiento(LocalDate fechaOrigen, int cantidadDiasComputables) {
        if (fechaOrigen == null) throw new IllegalArgumentException("fechaOrigen es obligatoria");
        if (cantidadDiasComputables < 1 || cantidadDiasComputables > 3650)
            throw new IllegalArgumentException(
                    "cantidadDiasComputables debe estar entre 1 y 3650, fue: " + cantidadDiasComputables);

        LocalDate fecha = fechaOrigen.plusDays(1);
        int diasContados = 0;

        while (diasContados < cantidadDiasComputables) {
            if (calendario.esDiaComputable(fecha)) {
                diasContados++;
            }
            if (diasContados < cantidadDiasComputables) {
                fecha = fecha.plusDays(1);
            }
        }

        return fecha;
    }
}
