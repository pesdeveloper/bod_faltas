package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Request para resolver una apelacion: rechazo (APERAZ) o aceptacion absuelve (APEABS).
 */
public record ResolverApelacionRequest(
        String fundamentosResolucion,
        String observaciones
) {}
