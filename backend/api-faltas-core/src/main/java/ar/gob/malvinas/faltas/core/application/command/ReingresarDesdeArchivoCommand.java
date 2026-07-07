package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para reingresar un acta archivada.
 *
 * versionArchivo: version OCC del ciclo activo de archivo.
 * idUserOperacion: usuario que ejecuta la operacion.
 */
public record ReingresarDesdeArchivoCommand(
        Long actaId,
        int versionArchivo,
        String idUserOperacion
) {
    public ReingresarDesdeArchivoCommand(Long actaId) {
        this(actaId, 0, "SYS");
    }
}
