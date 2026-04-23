package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Un eje de hecho material (medida, rodado, documentación) y su fase explícita
 * frente al expediente documental. El campo {@code bloqueaCierre} se informa
 * solo en bandeja de análisis, cuando aplica al cómputo de cierre; en etapas
 * anteriores la fase y la descripción reflejan ya la condición del caso.
 */
public record HechosMaterialesEjeResponse(
        String clave,
        String etiqueta,
        String fase,
        boolean bloqueaCierre,
        String descripcion) {
}
