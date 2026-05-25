package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Body de la acción mock que resuelve una apelación/recurso ya presentado.
 *
 * <p>Resultados válidos: {@code RECHAZADA} y {@code ACEPTADA_ABSUELVE}.
 * Este slice sólo resuelve el recurso; no eleva, no deriva a gestión
 * externa ni genera comprobantes.
 */
public record ResolverApelacionAccionRequest(String resultado) {
}
