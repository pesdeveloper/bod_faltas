package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Lanzada cuando hay mas de un contenido activo y vigente para una plantilla,
 * lo que impide resolver una unica version para generar la redaccion.
 *
 * Slice 8F-10-R2.
 */
public class PlantillaContenidoAmbiguaException extends RuntimeException {
    public PlantillaContenidoAmbiguaException(Long plantillaId, int cantidad) {
        super("Configuracion ambigua: " + cantidad + " contenidos activos y vigentes "
                + "para plantillaId=" + plantillaId + ". Debe quedar exactamente uno.");
    }
}
