package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.time.LocalDateTime;

/**
 * Filtro tipado de Persona ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Mismo criterio que {@link FiltroActa}: predicados especificos de la entidad, ningun
 * lenguaje generico, cada campo nulo significa sin filtro.
 *
 * <h2>Por que estos predicados</h2>
 *
 * {@code tipoDocumento} y {@code nroDoc} son exactamente los dos componentes que ya
 * transporta {@code PersonaRepository.buscarPorTipoDocYNroDoc}; {@code idSuj} e {@code idBie}
 * son los de {@code buscarPorIdSujBie}. Los cuatro reproducen predicados que el puerto ya
 * necesita, de modo que la consulta paginada no inventa capacidad nueva: la generaliza.
 * {@code tipoPersona} y el rango de {@code fhAlta} son el recorte minimo de cualquier
 * listado operativo.
 *
 * <p><b>No se incluye busqueda parcial por nombre.</b> Ninguna consulta existente la
 * necesita, y un {@code LIKE '%texto%'} no puede usar ningun indice de {@code fal_persona}:
 * agregarlo aca seria comprometer un predicado cuyo plan pertenece a la wave de optimizacion
 * fisica registrada en {@code D-PERSISTENCE-PAGING-INDEX-01}.
 *
 * <p><b>{@code nroDoc} se compara por igualdad exacta, no por {@code doc_key}.</b> Es la
 * misma consecuencia declarada de {@code JdbcPersonaRepository.SQL_POR_DOCUMENTO}: el filtro
 * recibe el cuerpo central canonico y no puede reconstruir la clave generada sin inventar el
 * prefijo y el digito verificador.
 *
 * @param tipoPersona    igualdad por codigo canonico del enum
 * @param tipoDocumento  igualdad por codigo canonico del enum
 * @param nroDoc         igualdad exacta sobre el cuerpo canonico
 * @param idSuj          igualdad
 * @param idBie          igualdad
 * @param fhAltaDesde    limite inferior inclusivo
 * @param fhAltaHasta    limite superior inclusivo
 */
public record FiltroPersona(
        TipoPersona tipoPersona,
        TipoDocumentoPersona tipoDocumento,
        String nroDoc,
        Integer idSuj,
        Integer idBie,
        LocalDateTime fhAltaDesde,
        LocalDateTime fhAltaHasta) {

    public FiltroPersona {
        if (fhAltaDesde != null && fhAltaHasta != null && fhAltaDesde.isAfter(fhAltaHasta)) {
            throw new PrecondicionVioladaException(
                    "fhAltaDesde no puede ser posterior a fhAltaHasta.");
        }
        if (nroDoc != null && nroDoc.isBlank()) {
            throw new PrecondicionVioladaException(
                    "nroDoc no puede ser blanco; omitalo para no filtrar.");
        }
    }

    /** Sin ningun predicado. */
    public static FiltroPersona ninguno() {
        return new FiltroPersona(null, null, null, null, null, null, null);
    }

    /** {@code true} si no hay ningun predicado informado. */
    public boolean estaVacio() {
        return tipoPersona == null && tipoDocumento == null && nroDoc == null
                && idSuj == null && idBie == null && fhAltaDesde == null && fhAltaHasta == null;
    }
}
