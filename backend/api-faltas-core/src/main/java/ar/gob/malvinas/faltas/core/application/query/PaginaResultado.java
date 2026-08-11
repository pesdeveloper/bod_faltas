package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.util.List;

/**
 * Una pagina devuelta por una consulta listable ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>{@code total} es opcional a proposito: en modalidad scroll el adapter omite el conteo
 * y resuelve {@link #haySiguiente()} leyendo una fila mas. Un total sintetizado como
 * {@code contenido.size()} cuando no se conto seria un numero falso indistinguible de un
 * total real, de modo que la ausencia se representa con {@code null} y no con un valor
 * inventado.
 *
 * @param contenido    elementos de la pagina, en el orden que impuso el motor
 * @param page         pagina base cero efectivamente devuelta
 * @param size         tamano solicitado
 * @param total        cantidad total de elementos que cumplen el filtro, o {@code null} si
 *                     no se conto
 * @param haySiguiente si existe al menos una pagina mas
 * @param <T>          tipo del elemento
 */
public record PaginaResultado<T>(
        List<T> contenido,
        int page,
        int size,
        Long total,
        boolean haySiguiente) {

    public PaginaResultado {
        if (contenido == null) {
            throw new PrecondicionVioladaException("contenido es obligatorio; use lista vacia.");
        }
        if (page < 0) {
            throw new PrecondicionVioladaException("page no puede ser negativo.");
        }
        if (size <= 0) {
            throw new PrecondicionVioladaException("size debe ser mayor que cero.");
        }
        if (total != null && total < 0) {
            throw new PrecondicionVioladaException("total no puede ser negativo.");
        }
        contenido = List.copyOf(contenido);
    }

    /** Pagina con conteo total conocido. */
    public static <T> PaginaResultado<T> conTotal(List<T> contenido, int page, int size, long total) {
        long consumidos = (long) page * (long) size + contenido.size();
        return new PaginaResultado<>(contenido, page, size, total, consumidos < total);
    }

    /** Pagina sin conteo total: {@code haySiguiente} lo resolvio la fila extra. */
    public static <T> PaginaResultado<T> sinTotal(
            List<T> contenido, int page, int size, boolean haySiguiente) {
        return new PaginaResultado<>(contenido, page, size, null, haySiguiente);
    }

    /** Pagina vacia con total cero. */
    public static <T> PaginaResultado<T> vacia(int page, int size) {
        return new PaginaResultado<>(List.of(), page, size, 0L, false);
    }

    /** {@code true} cuando el conteo total se calculo. */
    public boolean tieneTotal() {
        return total != null;
    }
}
