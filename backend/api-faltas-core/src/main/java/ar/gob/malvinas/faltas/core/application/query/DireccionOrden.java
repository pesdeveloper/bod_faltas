package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

/**
 * Direccion de un criterio de orden ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Es un enum y no un texto libre a proposito: el fragmento que llega al
 * {@code ORDER BY} sale de {@link #sql()}, que es una constante interna del enum, de modo
 * que ningun valor aportado por el cliente puede convertirse en SQL. La conversion desde
 * texto vive en {@link #desdeTexto(String)} y rechaza cualquier valor que no sea uno de
 * los dos aceptados.
 */
public enum DireccionOrden {

    ASC("ASC"),
    DESC("DESC");

    private final String sql;

    DireccionOrden(String sql) {
        this.sql = sql;
    }

    /** Fragmento SQL constante. Nunca deriva de una cadena recibida. */
    public String sql() {
        return sql;
    }

    /**
     * Resuelve la direccion desde su nombre funcional, sin distinguir mayusculas.
     *
     * <p>Un valor desconocido se rechaza: no se degrada a {@code ASC} por defecto. Un
     * orden que el cliente creyo descendente y que el sistema resuelve ascendente en
     * silencio devuelve una pagina distinta de la pedida, y esa divergencia es
     * indistinguible de un dato faltante.
     */
    public static DireccionOrden desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new PrecondicionVioladaException("direccion de orden es obligatoria.");
        }
        String normalizado = texto.trim().toUpperCase(java.util.Locale.ROOT);
        for (DireccionOrden d : values()) {
            if (d.name().equals(normalizado)) {
                return d;
            }
        }
        throw new PrecondicionVioladaException(
                "direccion de orden invalida: '" + texto + "'. Valores admitidos: ASC, DESC.");
    }
}
