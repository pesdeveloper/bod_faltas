package ar.gob.malvinas.faltas.core.repository;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto de persistencia para fal_observacion.
 * Copias defensivas en todas las operaciones de lectura.
 *
 * <p>Identidad (DECISION_ID-01): la PK la asigna la persistencia. El puerto no
 * expone un generador de identificadores y ningun llamador elige un id antes de
 * guardar.
 *
 * <p>Append-only (DECISION_OBS-01): una observacion persistida no se edita. La
 * correccion es baja logica de la fila anterior mas una fila nueva. Por eso
 * {@link #guardar(FalObservacion)} es exclusivamente alta y la unica escritura
 * sobre una fila existente es {@link #desactivar(Long, LocalDateTime, String)}.
 *
 * <p>Orden canonico de las lecturas por propietario: {@code fh_alta ASC, id ASC}.
 * El id desempata instantes DATETIME(6) iguales, de modo que la primera fila es
 * siempre la observacion mas antigua de esa entidad.
 */
public interface ObservacionRepository {

    /**
     * Da de alta una observacion y devuelve la copia persistida con el id que
     * asigno la persistencia. No muta el argumento.
     *
     * @param obs observacion nueva; su id debe ser nulo
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si el id viene informado: con id inexistente seria preasignar la PK
     *         y con id existente seria reescribir una fila append-only
     */
    FalObservacion guardar(FalObservacion obs);

    Optional<FalObservacion> buscarPorId(Long id);

    /** Historial completo del propietario, activas e inactivas, en orden canonico. */
    List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId);

    /** Observaciones vigentes del propietario, en orden canonico. */
    List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId);

    /**
     * Historial completo de varios propietarios del mismo tipo, resuelto en una sola
     * operacion (DECISION-OBS-BATCH-01). Equivale a invocar
     * {@link #listarPorEntidad(EntidadTipoObservada, Long)} una vez por id, sin pagar
     * una consulta por propietario.
     *
     * <p>Cada historial conserva el orden canonico {@code fh_alta ASC, id ASC} e incluye
     * activas e inactivas, de modo que su primera fila sigue siendo la observacion mas
     * antigua del propietario.
     *
     * <p>Un propietario sin observaciones <b>puede estar ausente</b> del mapa: la
     * ausencia es la unica representacion del historial vacio y el llamador la resuelve
     * como lista vacia. No se sintetizan claves que la persistencia no devolvio.
     *
     * <p>Ids repetidos en la entrada no duplican filas ni grupos en la salida. Una
     * coleccion vacia devuelve un mapa vacio sin consultar la persistencia.
     *
     * @param tipo       tipo de propietario, obligatorio; se aplica a todos los ids
     * @param entidadIds ids de los propietarios, obligatorio y sin elementos nulos
     * @return mapa de id de propietario a su historial, sin entradas de listas vacias
     * @throws IllegalArgumentException si {@code tipo} o la coleccion son nulos, o si
     *         algun id de la coleccion es nulo
     */
    Map<Long, List<FalObservacion>> listarPorEntidades(
            EntidadTipoObservada tipo, Collection<Long> entidadIds);

    /**
     * Baja logica coherente: {@code si_activa=false} con {@code fh_baja} e
     * {@code id_user_baja} informados. No reescribe el texto ni los datos de alta.
     *
     * <p>El actor y el instante los provee la capa de aplicacion: el adaptador no
     * usa NOW() ni un usuario ficticio. La coherencia la refuerza ademas el CHECK
     * fisico chk_obs_baja_coherente.
     *
     * @return la observacion ya dada de baja
     * @throws ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException
     *         si el id no corresponde a ninguna observacion
     * @throws IllegalStateException si la observacion ya estaba inactiva
     */
    FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja);
}
