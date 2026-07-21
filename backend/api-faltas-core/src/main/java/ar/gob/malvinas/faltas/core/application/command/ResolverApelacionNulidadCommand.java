package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para resolver una apelacion con resultado NULIDAD.
 * Desactiva el fallo anterior y registra el resultado en el acta.
 */
public record ResolverApelacionNulidadCommand(
        Long apelacionId,
        String fundamentosResolucion,
        String idUserResolucion
) {}
