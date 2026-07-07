package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para registrar una apelacion sobre el fallo condenatorio notificado.
 *
 * Precondiciones: fallo condenatorio activo y NOTIFICADO, sin apelacion previa activa,
 * acta no cerrada/anulada/archivada/paralizada.
 */
public record RegistrarApelacionCommand(
        Long actaId,
        String presentante,
        String fundamentos,
        String observaciones
) {
    public static RegistrarApelacionCommand legacy(Long actaId, String presentante, String fundamentos, String observaciones) {
        return new RegistrarApelacionCommand(actaId, presentante, fundamentos, observaciones);
    }
}
