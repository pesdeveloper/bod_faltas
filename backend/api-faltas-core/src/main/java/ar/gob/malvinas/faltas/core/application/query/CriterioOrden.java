package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

/**
 * Un criterio de orden: campo funcional mas direccion
 * ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>El parametro de tipo {@code C} es el enum de campos ordenables de la entidad, no un
 * {@code String} ni un {@code Map}. Esa eleccion es la que hace imposible que el cliente
 * aporte un nombre de columna, un alias, una funcion o un fragmento de SQL: para construir
 * un criterio hay que nombrar una constante del enum de la entidad, y la traduccion a SQL
 * la hace el adapter contra su propia allowlist.
 *
 * @param campo      campo funcional de la allowlist de la entidad
 * @param direccion  ASC o DESC
 * @param <C>        enum de campos ordenables de la entidad
 */
public record CriterioOrden<C extends Enum<C>>(C campo, DireccionOrden direccion) {

    public CriterioOrden {
        if (campo == null) {
            throw new PrecondicionVioladaException("campo de orden es obligatorio.");
        }
        if (direccion == null) {
            throw new PrecondicionVioladaException("direccion de orden es obligatoria.");
        }
    }

    /** Criterio ascendente. */
    public static <C extends Enum<C>> CriterioOrden<C> asc(C campo) {
        return new CriterioOrden<>(campo, DireccionOrden.ASC);
    }

    /** Criterio descendente. */
    public static <C extends Enum<C>> CriterioOrden<C> desc(C campo) {
        return new CriterioOrden<>(campo, DireccionOrden.DESC);
    }
}
