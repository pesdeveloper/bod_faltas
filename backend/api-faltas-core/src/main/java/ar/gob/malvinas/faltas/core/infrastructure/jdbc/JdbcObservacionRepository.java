package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adaptador JDBC de ObservacionRepository sobre fal_observacion.
 *
 * Estilo heredado de JdbcActaEventoRepository (WAVE 0B) y de los catalogos de
 * WAVE 1A1: JdbcClient sobre el DataSource principal, SQL explicito en constantes,
 * parametros nombrados y RowMapper estatico privado. Sin framework interno, sin
 * SELECT *, sin generacion de SQL.
 *
 * Mapeo contra el DDL real, 11 columnas:
 *   id                  BIGINT AUTO_INCREMENT  la asigna MariaDB
 *   entidad_tipo        SMALLINT NOT NULL      <- EntidadTipoObservada.codigo()
 *   entidad_id          BIGINT NOT NULL        propietario polimorfico; sin FK fisica
 *   id_acta_contexto    BIGINT NULL            fk_obs_acta_contexto -> fal_acta(id)
 *   origen_observacion  SMALLINT NOT NULL      <- OrigenObservacion.codigo()
 *   observacion         VARCHAR(512) NOT NULL  chk_obs_texto
 *   si_activa           TINYINT(1) NOT NULL    baja logica
 *   fh_alta             DATETIME(6) NOT NULL   provisto por el dominio
 *   id_user_alta        CHAR(36) NOT NULL
 *   fh_baja             DATETIME(6) NULL       chk_obs_baja_coherente
 *   id_user_baja        CHAR(36) NULL          chk_obs_baja_coherente
 *
 * En ningun caso se usa ordinal(), name() ni toString() del enum.
 *
 * Campo del modelo que el DDL no persiste (HUMAN_DECISION_CLOSED): tipoObs,
 * @Deprecated por redundante con entidadTipo. Un round-trip lo devuelve nulo. Es la
 * decision de modelo vigente, no una perdida accidental.
 *
 * Propietario polimorfico (DECISION_OBS-01): el propietario es el par
 * (entidad_tipo, entidad_id) y no tiene FK fisica. La integridad propietaria la
 * garantiza la aplicacion dentro de la transaccion del caso de uso, no el motor.
 * id_acta_contexto es contexto opcional y nunca se usa como propietario.
 *
 * Append-only (DECISION_OBS-01): la unica escritura sobre una fila existente es la
 * baja logica. No hay UPDATE de texto, ni DELETE, ni REPLACE, ni ON DUPLICATE KEY
 * UPDATE. Los 21 triggers *_ad_observaciones y el ON DELETE CASCADE de
 * fk_obs_acta_contexto que el esquema conserva no forman parte del comportamiento
 * de este adaptador.
 *
 * Tiempo y actor: fh_alta, fh_baja, id_user_alta e id_user_baja se persisten tal
 * como los entrega el dominio (FaltasClock es la fuente de tiempo). No se usa
 * NOW(), CURRENT_TIMESTAMP ni un usuario por defecto. DATETIME(6) conserva
 * microsegundos: un LocalDateTime con precision de nanosegundos se redondea al
 * microsegundo mas cercano en la base.
 *
 * OCC: no aplica. fal_observacion no tiene version_row porque la fila no se edita.
 *
 * Transaccionalidad: este adaptador no abre transacciones. La frontera vive en la
 * capa de aplicacion.
 */
@Repository
@Profile("jdbc")
public class JdbcObservacionRepository implements ObservacionRepository {

    private static final String COLUMNAS = """
            id, entidad_tipo, entidad_id, id_acta_contexto, origen_observacion,
            observacion, si_activa, fh_alta, id_user_alta, fh_baja, id_user_baja
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_observacion (
                entidad_tipo, entidad_id, id_acta_contexto, origen_observacion,
                observacion, si_activa, fh_alta, id_user_alta, fh_baja, id_user_baja
            ) VALUES (
                :entidadTipo, :entidadId, :idActaContexto, :origenObservacion,
                :observacion, :siActiva, :fhAlta, :idUserAlta, :fhBaja, :idUserBaja
            )
            """;

    /**
     * La guarda si_activa = 1 hace la baja idempotente-segura: una segunda baja no
     * reescribe fh_baja ni id_user_baja, y afecta cero filas.
     */
    private static final String SQL_BAJA_LOGICA = """
            UPDATE fal_observacion
               SET si_activa = 0,
                   fh_baja = :fhBaja,
                   id_user_baja = :idUserBaja
             WHERE id = :id
               AND si_activa = 1
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_observacion
            WHERE id = :id
            """;

    /**
     * Orden canonico del historial del propietario. El prefijo izquierdo
     * (entidad_tipo, entidad_id) de ix_obs_entidad sirve el filtro.
     */
    private static final String SQL_POR_ENTIDAD = "SELECT " + COLUMNAS + """
            FROM fal_observacion
            WHERE entidad_tipo = :entidadTipo
              AND entidad_id = :entidadId
            ORDER BY fh_alta ASC, id ASC
            """;

    private static final String SQL_ACTIVAS_POR_ENTIDAD = "SELECT " + COLUMNAS + """
            FROM fal_observacion
            WHERE entidad_tipo = :entidadTipo
              AND entidad_id = :entidadId
              AND si_activa = TRUE
            ORDER BY fh_alta ASC, id ASC
            """;

    /**
     * Historial de varios propietarios en una sola sentencia (DECISION-OBS-BATCH-01). El
     * IN usa la expansion de colecciones de los parametros nombrados de Spring: los ids
     * nunca se concatenan al texto de la consulta y el SQL no se arma por condiciones.
     *
     * <p>La misma lista de once columnas que el resto del adaptador, de modo que no hay
     * una segunda enumeracion que pueda desincronizarse.
     *
     * <p>El ORDER BY agrega entidad_id como primera clave para que las filas lleguen ya
     * contiguas por propietario; dentro de cada propietario rige el orden canonico. El
     * prefijo izquierdo (entidad_tipo, entidad_id) de ix_obs_entidad sirve el filtro y no
     * hace falta indice nuevo.
     */
    private static final String SQL_POR_ENTIDADES = "SELECT " + COLUMNAS + """
            FROM fal_observacion
            WHERE entidad_tipo = :entidadTipo
              AND entidad_id IN (:entidadIds)
            ORDER BY entidad_id ASC, fh_alta ASC, id ASC
            """;

    private static final String SQL_EXISTE = """
            SELECT EXISTS (SELECT 1 FROM fal_observacion WHERE id = :id)
            """;

    private final JdbcClient jdbc;

    public JdbcObservacionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Alta exclusivamente: el INSERT nunca informa la PK y la recupera con
     * GeneratedKeyHolder. Un id informado se rechaza con la misma excepcion
     * funcional y el mismo mensaje que produce el adaptador InMemory, porque
     * ninguna de las dos lecturas posibles es legitima: insertar con la PK del
     * llamador contradice DECISION_ID-01 y actualizar la fila contradice el caracter
     * append-only de DECISION_OBS-01.
     */
    @Override
    public FalObservacion guardar(FalObservacion obs) {
        if (obs.getId() != null) {
            throw new PrecondicionVioladaException(
                    "Una observacion persistida no se edita: para corregirla, darla de baja"
                    + " logicamente e insertar una nueva. id recibido=" + obs.getId());
        }

        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql(SQL_INSERT)
                .param("entidadTipo", codigo(obs.getEntidadTipo()), Types.SMALLINT)
                .param("entidadId", obs.getEntidadId(), Types.BIGINT)
                .param("idActaContexto", obs.getIdActaContexto(), Types.BIGINT)
                .param("origenObservacion", codigo(obs.getOrigenObs()), Types.SMALLINT)
                .param("observacion", obs.getObservacion(), Types.VARCHAR)
                .param("siActiva", obs.isSiActiva())
                .param("fhAlta", obs.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", obs.getIdUserAlta(), Types.CHAR)
                .param("fhBaja", obs.getFhBaja(), Types.TIMESTAMP)
                .param("idUserBaja", obs.getIdUserBaja(), Types.CHAR)
                .update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_observacion no devolvio el id generado para la entidad "
                            + obs.getEntidadTipo() + "/" + obs.getEntidadId());
        }
        return obs.conId(generado.longValue());
    }

    @Override
    public Optional<FalObservacion> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id)
                .query(JdbcObservacionRepository::mapear)
                .optional();
    }

    @Override
    public List<FalObservacion> listarPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return listar(SQL_POR_ENTIDAD, tipo, entidadId);
    }

    @Override
    public List<FalObservacion> listarActivasPorEntidad(EntidadTipoObservada tipo, Long entidadId) {
        return listar(SQL_ACTIVAS_POR_ENTIDAD, tipo, entidadId);
    }

    private List<FalObservacion> listar(String sql, EntidadTipoObservada tipo, Long entidadId) {
        return jdbc.sql(sql)
                .param("entidadTipo", codigo(tipo), Types.SMALLINT)
                .param("entidadId", entidadId, Types.BIGINT)
                .query(JdbcObservacionRepository::mapear)
                .list();
    }

    /**
     * Batch del historial (DECISION-OBS-BATCH-01): una sola sentencia para todos los
     * propietarios pedidos.
     *
     * <p>Con la coleccion vacia <b>no se ejecuta SQL</b>. No es solo una optimizacion:
     * {@code entidad_id IN ()} no es sintaxis valida, de modo que la unica alternativa
     * seria emitir una consulta que el motor rechaza.
     *
     * <p>El agrupado recorre las filas ya ordenadas por la base y preserva ese orden
     * dentro de cada propietario. Un propietario sin filas no recibe entrada en el mapa.
     */
    @Override
    public Map<Long, List<FalObservacion>> listarPorEntidades(
            EntidadTipoObservada tipo, Collection<Long> entidadIds) {
        Set<Long> ids = idsDePropietarios(tipo, entidadIds);
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<FalObservacion>> historiales = new LinkedHashMap<>();
        jdbc.sql(SQL_POR_ENTIDADES)
                .param("entidadTipo", codigo(tipo), Types.SMALLINT)
                .param("entidadIds", ids)
                .query(JdbcObservacionRepository::mapear)
                .list()
                .forEach(obs -> historiales
                        .computeIfAbsent(obs.getEntidadId(), clave -> new ArrayList<>())
                        .add(obs));

        historiales.replaceAll((clave, historial) -> List.copyOf(historial));
        return Map.copyOf(historiales);
    }

    /**
     * Valida los argumentos del batch y deduplica los ids. La deduplicacion es lo que
     * hace que un id repetido en la entrada no produzca filas ni grupos duplicados: el
     * IN recibe cada valor una sola vez.
     *
     * <p>Mismos tipos y mensajes que produce el adaptador InMemory.
     */
    private static Set<Long> idsDePropietarios(EntidadTipoObservada tipo, Collection<Long> entidadIds) {
        if (tipo == null) {
            throw new IllegalArgumentException("tipo es obligatorio");
        }
        if (entidadIds == null) {
            throw new IllegalArgumentException("entidadIds es obligatoria");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : entidadIds) {
            if (id == null) {
                throw new IllegalArgumentException("entidadIds no admite ids nulos");
            }
            ids.add(id);
        }
        return ids;
    }

    /**
     * Cero filas afectadas tiene dos causas y se desambiguan con una lectura por id
     * que solo se paga en el camino de fallo: sin fila, el id no existe; con fila, la
     * observacion ya estaba inactiva. No hay OCC que consultar porque la tabla no
     * tiene version_row.
     *
     * Tras el UPDATE exitoso se relee la fila para devolver la baja vigente tal como
     * quedo en la base, incluidos el texto y los datos de alta que la baja no toca.
     */
    @Override
    public FalObservacion desactivar(Long id, LocalDateTime fhBaja, String idUserBaja) {
        int filas = jdbc.sql(SQL_BAJA_LOGICA)
                .param("id", id, Types.BIGINT)
                .param("fhBaja", fhBaja, Types.TIMESTAMP)
                .param("idUserBaja", idUserBaja, Types.CHAR)
                .update();

        if (filas == 0) {
            throw desempatarBajaSinEfecto(id);
        }
        return buscarPorId(id).orElseThrow(() -> new IllegalStateException(
                "La observacion " + id + " desaparecio entre la baja logica y su relectura"));
    }

    /** Mismos tipos y mensajes que produce el adaptador InMemory. */
    private RuntimeException desempatarBajaSinEfecto(Long id) {
        Boolean existe = jdbc.sql(SQL_EXISTE)
                .param("id", id, Types.BIGINT)
                .query(Boolean.class)
                .single();
        if (!Boolean.TRUE.equals(existe)) {
            return new PrecondicionVioladaException("Observacion no encontrada: " + id);
        }
        return new IllegalStateException("La observacion ya esta inactiva: id=" + id);
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static Short codigo(EntidadTipoObservada tipo) {
        return tipo == null ? null : (short) tipo.codigo();
    }

    private static Short codigo(OrigenObservacion origen) {
        return origen == null ? null : (short) origen.codigo();
    }

    /**
     * Una fila inactiva se reconstruye reproduciendo el comportamiento del dominio
     * (desactivar), no forzando el estado: el modelo no expone un constructor de
     * fila ya dada de baja y su propia validacion queda ejercida.
     *
     * tipoObs se mapea a null porque el DDL no lo persiste.
     */
    private static FalObservacion mapear(ResultSet rs, int fila) throws SQLException {
        FalObservacion obs = new FalObservacion(
                rs.getLong("id"),
                EntidadTipoObservada.fromCodigo(rs.getShort("entidad_tipo")),
                rs.getLong("entidad_id"),
                null,
                rs.getString("observacion"),
                origen(rs.getObject("origen_observacion", Short.class)),
                rs.getObject("id_acta_contexto", Long.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));

        if (!rs.getBoolean("si_activa")) {
            obs.desactivar(
                    rs.getObject("fh_baja", LocalDateTime.class),
                    rs.getString("id_user_baja"));
        }
        return obs;
    }

    private static OrigenObservacion origen(Short codigo) {
        return codigo == null ? null : OrigenObservacion.fromCodigo(codigo);
    }
}
