package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;

import java.util.List;
import java.util.Optional;

/**
 * Port de acceso a personas del sistema de faltas.
 * No elimina fisicamente personas.
 *
 * <p>Identidad: la PK la asigna la persistencia (DECISION_ID-01). El puerto no expone
 * {@code nextId()} y ningun componente preasigna identificadores.
 *
 * <p>Patron P3: {@code fal_persona} no tiene {@code version_row} y por lo tanto no hay
 * OCC, pero su fila si se edita. Por eso {@code guardar} distingue alta de
 * actualizacion segun la presencia del id, en lugar de ser alta-only.
 *
 * <p>Orden: los tres metodos que devuelven listas prometen orden por {@code id}
 * ascendente. Es contrato del puerto y el futuro adaptador JDBC debe expresarlo con un
 * {@code ORDER BY id ASC} explicito.
 */
public interface PersonaRepository {

    /**
     * Alta o actualizacion, segun la identidad de la entidad recibida.
     *
     * <p>Con {@code id} nulo inserta, la persistencia asigna la PK y se devuelve una
     * copia persistida. Con {@code id} informado actualiza la fila existente. En ninguno
     * de los dos casos se muta el argumento: todo llamador debe continuar con la entidad
     * devuelta.
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException
     *         si el {@code id} esta informado y no existe. Un id elegido por el llamador
     *         nunca se inserta.
     */
    FalPersona guardar(FalPersona persona);

    Optional<FalPersona> buscarPorId(Long id);

    /**
     * Lectura completa sin filtro ni limite. <b>Operacion interna.</b>
     *
     * <p>Ningun endpoint operacional la usa y no existe una variante publica ilimitada del
     * listado ({@code DECISION-LIST-PAGE-SORT-01}). Hoy no tiene ningun consumidor de
     * produccion: solo la ejercitan tests. Queda inventariada para su eliminacion en
     * {@code D-PERSISTENCE-PAGING-INDEX-01}.
     *
     * <p><b>No debe usarse para resolver una pagina.</b> Ordenada por {@code id} ascendente.
     */
    List<FalPersona> listarTodas();

    /**
     * Busca por identidad natural. Recibe la forma canonica que se persiste, no un valor
     * con separadores: la normalizacion es responsabilidad de la capa de aplicacion
     * ({@code DocumentoPersona}). Devuelve lista porque el contrato no promete unicidad.
     * Ordenada por {@code id} ascendente.
     */
    List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc);

    /** Ordenada por {@code id} ascendente. */
    List<FalPersona> buscarPorIdSujBie(Integer idSuj, Integer idBie);

    /**
     * Consulta paginada, filtrada y ordenada en la base
     * ({@code DECISION-LIST-PAGE-SORT-01}).
     *
     * <p><b>No es {@code default} a proposito</b>, por el mismo motivo que en
     * {@code ActaRepository}: obligar a las dos implementaciones a escribirla impide que un
     * adapter recorte en memoria sin que nadie lo note.
     *
     * <p>Contrato:
     *
     * <ul>
     *   <li>{@code filtro} nulo equivale a {@link FiltroPersona#ninguno()};</li>
     *   <li>{@code pagina} nula equivale a {@link PaginaSolicitada#primeraPagina()};</li>
     *   <li>un orden vacio aplica {@link CampoOrdenPersona#ordenPredeterminado()};</li>
     *   <li>todo orden, predeterminado o solicitado, termina con la PK como desempate;</li>
     *   <li>filtro, orden y recorte se ejecutan en el motor: nunca en memoria;</li>
     *   <li>con {@code incluirTotal} en falso el total puede venir nulo y
     *       {@code haySiguiente} se resuelve leyendo una fila mas.</li>
     * </ul>
     *
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si la pagina solicitada viola el contrato de tamano, orden o precedencia
     */
    PaginaResultado<FalPersona> listarPagina(FiltroPersona filtro,
                                             PaginaSolicitada<CampoOrdenPersona> pagina);
}
