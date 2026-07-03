package ar.gob.malvinas.faltas.core.application.result;

/**
 * Resultado generico de un comando de aplicacion.
 * Incluye el identificador del agregado afectado y el tipo de evento registrado.
 */
public record ComandoResultado(
        Long idActa,
        String idEntidadAfectada,
        String tipoEvento,
        String descripcion
) {
    public static ComandoResultado de(Long idActa, String idEntidadAfectada,
                                      String tipoEvento, String descripcion) {
        return new ComandoResultado(idActa, idEntidadAfectada, tipoEvento, descripcion);
    }
}
