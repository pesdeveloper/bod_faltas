package ar.gob.malvinas.faltas.core.application.command;

import jakarta.validation.constraints.NotNull;

/**
 * Bloque tipado de artículo normativo imputado en el alta integral, agregado en Wave 4 (alta
 * integral JDBC).
 *
 * <p>{@code normativaId} viaja explícito porque {@code fal_acta_articulo_infringido} no
 * denormaliza esa FK (se deriva de {@code fal_articulo_normativa_faltas.id_normativa} al leer);
 * el alta la recibe del llamador porque es quien conoce contra qué normativa imputa.
 */
public record ArticuloInfringidoItem(
        @NotNull Long normativaId,
        @NotNull Long articuloId
) {}
