package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Body de la acción mock que registra la presentación de apelación/recurso
 * sobre un fallo condenatorio notificado con plazo de apelación abierto.
 *
 * <p>Canales válidos: {@code PORTAL_INFRACTOR} y
 * {@code PRESENCIAL_DIRECCION}. Este slice sólo registra la presentación;
 * no resuelve ni eleva el recurso ni genera comprobantes.
 */
public record RegistrarApelacionAccionRequest(String canal) {
}
