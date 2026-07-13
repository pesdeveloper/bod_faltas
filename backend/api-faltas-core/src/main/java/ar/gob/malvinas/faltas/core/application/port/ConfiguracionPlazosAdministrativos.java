package ar.gob.malvinas.faltas.core.application.port;

import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;

/**
 * Puerto de configuracion global de plazos administrativos.
 *
 * El servicio de plazos depende de esta interfaz; no accede directamente a application.yml.
 * La separacion permite variar la fuente de configuracion sin modificar el servicio.
 */
public interface ConfiguracionPlazosAdministrativos {

    /**
     * Devuelve la cantidad de dias computables configurada para el tipo de plazo indicado.
     * No debe devolver cero ni un valor negativo.
     *
     * @throws IllegalArgumentException si el tipo es desconocido o no tiene configuracion.
     */
    int cantidadDiasComputables(TipoPlazoAdministrativo tipo);
}
