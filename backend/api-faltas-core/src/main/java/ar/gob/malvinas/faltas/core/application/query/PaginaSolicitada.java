package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Pagina solicitada de una consulta listable ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Transporta las cuatro cosas que el contrato exige: numero de pagina, tamano, lista
 * ordenada de criterios de orden y si hay que contar el total. No es un
 * {@code Pageable} de Spring Data: el proyecto no incluye Spring Data y no se agrega una
 * dependencia para obtener dos campos y una lista.
 *
 * <h2>Ausencia de parametros</h2>
 *
 * La ausencia no significa consulta ilimitada. {@link #primeraPagina()} y
 * {@link #de(int, int)} materializan los valores por defecto:
 *
 * <pre>
 * page  = 0
 * size  = 50
 * orden = vacio  -&gt; el adapter aplica el orden predeterminado de la entidad
 * total = si
 * </pre>
 *
 * <h2>Validaciones</h2>
 *
 * <ul>
 *   <li>{@code page < 0} se rechaza;</li>
 *   <li>{@code size <= 0} se rechaza;</li>
 *   <li>{@code size > 100} se rechaza <b>explicitamente</b>, no se recorta: un cliente que
 *       pidio 500 y recibio 100 sin aviso no puede distinguir esa pagina de la ultima del
 *       conjunto, y paginaria mal a partir de ahi;</li>
 *   <li>un campo repetido se rechaza: dos criterios sobre el mismo campo hacen que el
 *       segundo no pueda influir en el resultado, de modo que la peticion no significa lo
 *       que el cliente cree;</li>
 *   <li>mas de {@link #MAX_CRITERIOS_ORDEN} criterios se rechaza;</li>
 *   <li>un criterio nulo se rechaza.</li>
 * </ul>
 *
 * <p>El campo desconocido y la direccion invalida se rechazan antes de llegar aca, en
 * {@code desdeNombreFuncional} del enum de la entidad y en
 * {@link DireccionOrden#desdeTexto(String)}: con el criterio ya tipado no existe forma de
 * representar un campo que no pertenezca a la allowlist.
 *
 * @param page         pagina base cero
 * @param size         cantidad de elementos por pagina
 * @param orden        criterios en orden de precedencia; vacio delega en el orden
 *                     predeterminado de la entidad
 * @param incluirTotal cuando es falso, el adapter puede omitir el conteo y resolver
 *                     {@code haySiguiente} leyendo {@code size + 1} filas
 * @param <C>          enum de campos ordenables de la entidad
 */
public record PaginaSolicitada<C extends Enum<C>>(
        int page,
        int size,
        List<CriterioOrden<C>> orden,
        boolean incluirTotal) {

    /** Pagina por defecto cuando el cliente no informa nada. */
    public static final int PAGE_DEFAULT = 0;

    /** Tamano por defecto cuando el cliente no informa nada. */
    public static final int SIZE_DEFAULT = 50;

    /** Tamano maximo admitido. Un valor mayor se rechaza, no se recorta. */
    public static final int SIZE_MAX = 100;

    /**
     * Maximo de criterios de orden por consulta.
     *
     * <p>Tres alcanza para cualquier orden funcional conocido y acota el costo del
     * {@code ORDER BY}. El limite es explicito y su violacion se rechaza, en lugar de
     * truncar la lista y ejecutar un orden distinto del pedido.
     */
    public static final int MAX_CRITERIOS_ORDEN = 3;

    public PaginaSolicitada {
        if (page < 0) {
            throw new PrecondicionVioladaException(
                    "page no puede ser negativo. Recibido: " + page);
        }
        if (size <= 0) {
            throw new PrecondicionVioladaException(
                    "size debe ser mayor que cero. Recibido: " + size);
        }
        if (size > SIZE_MAX) {
            throw new PrecondicionVioladaException(
                    "size excede el maximo permitido de " + SIZE_MAX + ". Recibido: " + size);
        }
        if (orden == null) {
            throw new PrecondicionVioladaException("orden es obligatorio; use lista vacia.");
        }
        if (orden.size() > MAX_CRITERIOS_ORDEN) {
            throw new PrecondicionVioladaException(
                    "orden admite hasta " + MAX_CRITERIOS_ORDEN + " criterios. Recibidos: "
                            + orden.size());
        }
        List<CriterioOrden<C>> copia = new ArrayList<>(orden.size());
        EnumSet<C> vistos = null;
        for (CriterioOrden<C> criterio : orden) {
            if (criterio == null) {
                throw new PrecondicionVioladaException("orden no admite criterios nulos.");
            }
            if (vistos == null) {
                vistos = EnumSet.of(criterio.campo());
            } else if (!vistos.add(criterio.campo())) {
                throw new PrecondicionVioladaException(
                        "orden no admite el mismo campo dos veces: " + criterio.campo().name());
            }
            copia.add(criterio);
        }
        orden = List.copyOf(copia);
    }

    /** Primera pagina con los valores por defecto y conteo total. */
    public static <C extends Enum<C>> PaginaSolicitada<C> primeraPagina() {
        return new PaginaSolicitada<>(PAGE_DEFAULT, SIZE_DEFAULT, List.of(), true);
    }

    /** Pagina y tamano explicitos, orden predeterminado de la entidad y conteo total. */
    public static <C extends Enum<C>> PaginaSolicitada<C> de(int page, int size) {
        return new PaginaSolicitada<>(page, size, List.of(), true);
    }

    /** Misma pagina con los criterios de orden indicados. */
    public PaginaSolicitada<C> conOrden(List<CriterioOrden<C>> criterios) {
        return new PaginaSolicitada<>(page, size, criterios, incluirTotal);
    }

    /**
     * Misma pagina en modalidad scroll: sin conteo total.
     *
     * <p>El adapter resuelve {@code haySiguiente} leyendo una fila mas y devolviendo
     * {@code size}. Reutiliza el mismo contrato de filtros y orden que la paginacion
     * tradicional, de modo que no existen dos consultas que puedan divergir.
     */
    public PaginaSolicitada<C> sinTotal() {
        return new PaginaSolicitada<>(page, size, orden, false);
    }

    /** Desplazamiento a aplicar en el motor. */
    public long offset() {
        return (long) page * (long) size;
    }
}
