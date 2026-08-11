package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDate;

/**
 * Filtro tipado de Acta ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Es un contrato especifico de la entidad y <b>no</b> un lenguaje generico
 * campo-operador-valor. La diferencia es material: con campos tipados, el conjunto de
 * predicados posibles queda fijado en tiempo de compilacion y el adapter puede escribir su
 * {@code WHERE} en una constante; con un lenguaje generico habria que construir SQL a partir
 * de texto del cliente, que es exactamente lo que la decision prohibe.
 *
 * <p>Cada campo nulo significa <b>sin filtro</b>. No existe un valor especial para "buscar
 * los nulos": ese seria otro predicado y tendria que declararse como campo propio.
 *
 * <h2>Por que estos predicados</h2>
 *
 * Los ocho corresponden a dimensiones que el sistema ya usa: las tres de estado
 * ({@code bloqueActual}, {@code estadoProcesal}, {@code situacionAdministrativa}) son las que
 * gobiernan el routing de bandejas de {@code 30-projections/snapshot-bandejas-acciones.md};
 * {@code dependencia} e {@code inspector} son las dos dimensiones organizativas del labrado;
 * {@code tipoActa} y {@code nroActa} identifican el acta; el rango de {@code fechaActa} es el
 * recorte temporal de cualquier bandeja. No se inventan predicados que ninguna consulta
 * existente necesite.
 *
 * @param tipoActa                 igualdad por codigo canonico del enum
 * @param bloqueActual             igualdad por codigo canonico del enum
 * @param estadoProcesal           igualdad por codigo canonico del enum
 * @param situacionAdministrativa  igualdad por codigo canonico del enum
 * @param idDependencia            igualdad
 * @param idInspector              igualdad
 * @param nroActa                  igualdad exacta; no es una busqueda parcial
 * @param fechaActaDesde           limite inferior inclusivo
 * @param fechaActaHasta           limite superior inclusivo
 */
public record FiltroActa(
        TipoActa tipoActa,
        BloqueActual bloqueActual,
        EstadoProcesalActa estadoProcesal,
        SituacionAdministrativaActa situacionAdministrativa,
        Long idDependencia,
        Long idInspector,
        String nroActa,
        LocalDate fechaActaDesde,
        LocalDate fechaActaHasta) {

    public FiltroActa {
        if (fechaActaDesde != null && fechaActaHasta != null
                && fechaActaDesde.isAfter(fechaActaHasta)) {
            throw new PrecondicionVioladaException(
                    "fechaActaDesde no puede ser posterior a fechaActaHasta.");
        }
        if (nroActa != null && nroActa.isBlank()) {
            throw new PrecondicionVioladaException(
                    "nroActa no puede ser blanco; omitalo para no filtrar.");
        }
    }

    /** Sin ningun predicado. */
    public static FiltroActa ninguno() {
        return new FiltroActa(null, null, null, null, null, null, null, null, null);
    }

    /** {@code true} si no hay ningun predicado informado. */
    public boolean estaVacio() {
        return tipoActa == null && bloqueActual == null && estadoProcesal == null
                && situacionAdministrativa == null && idDependencia == null && idInspector == null
                && nroActa == null && fechaActaDesde == null && fechaActaHasta == null;
    }
}
