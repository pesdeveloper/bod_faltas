package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC de ActaEventoRepository sobre fal_acta_evento.
 *
 * Primer adaptador Jdbc* de dominio (WAVE 0B, repositorio piloto). Estilo tomado
 * del precedente real del proyecto (GeoRepository / GeoCatalogMariaDbRepository):
 * JdbcClient sobre el DataSource principal, SQL explicito y parametros nombrados.
 *
 * Diferencia deliberada con ese precedente: los repositorios Geo devuelven listas
 * vacias ante cualquier excepcion porque alimentan un visor de catalogos. Aca no
 * se traga ninguna excepcion: es persistencia de dominio y un fallo de integridad
 * debe propagarse como DataAccessException para que la transaccion del caso de uso
 * decida.
 *
 * Append-only: la unica escritura es INSERT. No hay UPDATE, DELETE ni upsert,
 * porque el contrato del puerto no los expone.
 *
 * Mapeo de tipos contra el DDL real de fal_acta_evento:
 *   tipo_evt          CHAR(6)     <- TipoEventoActa.codigo()          (String; el DDL
 *                                    documenta explicitamente CHAR(6), no SMALLINT)
 *   origen_evt        SMALLINT    <- OrigenEvento.codigo()            (short)
 *   actor_tipo        SMALLINT    <- ActorTipoEvento.codigo()         (short, nullable)
 *   bloque_func       CHAR(4)     <- BloqueActual.codigo()            (nullable)
 *   est_proc_ant/nvo  CHAR(4)     <- EstadoProcesalActa.codigo()      (nullable)
 *   sit_adm_ant/nva   CHAR(4)     <- SituacionAdministrativaActa.codigo() (nullable)
 *   fh_evt            DATETIME(6) <- LocalDateTime provisto por el dominio
 *
 * En ningun caso se usa ordinal(), name() ni toString() del enum.
 *
 * Tiempo: fh_evt se persiste tal como lo entrega el dominio (FaltasClock sigue
 * siendo la fuente de tiempo). No se usa NOW() ni CURRENT_TIMESTAMP. DATETIME(6)
 * conserva microsegundos: un LocalDateTime con precision de nanosegundos se
 * redondea al microsegundo mas cercano en la base.
 *
 * Campos del modelo que el DDL no persiste (HUMAN_DECISION_CLOSED): actorRef y
 * descripcionLegible, ambos @Deprecated. Un round-trip por este adaptador los
 * devuelve en null. Es la decision de modelo vigente, no una perdida accidental.
 *
 * Transaccionalidad: este adaptador no abre transacciones. La frontera
 * transaccional vive en la capa de aplicacion (1 transaccion = 1 comando).
 */
@Repository
@Profile("jdbc")
public class JdbcActaEventoRepository implements ActaEventoRepository {

    private static final String COLUMNAS = """
            id, acta_id, tipo_evt, origen_evt, fh_evt,
            bloque_func, est_proc_ant, est_proc_nvo, sit_adm_ant, sit_adm_nva,
            actor_tipo, actor_id, id_docu_rel, id_notif_rel, id_pres_rel,
            id_user_evt, si_evt_cierre, si_evt_ext, si_permite_reing, correlacion_id
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_acta_evento (
                acta_id, tipo_evt, origen_evt, fh_evt,
                bloque_func, est_proc_ant, est_proc_nvo, sit_adm_ant, sit_adm_nva,
                actor_tipo, actor_id, id_docu_rel, id_notif_rel, id_pres_rel,
                id_user_evt, si_evt_cierre, si_evt_ext, si_permite_reing, correlacion_id
            ) VALUES (
                :actaId, :tipoEvt, :origenEvt, :fhEvt,
                :bloqueFunc, :estProcAnt, :estProcNvo, :sitAdmAnt, :sitAdmNva,
                :actorTipo, :actorId, :idDocuRel, :idNotifRel, :idPresRel,
                :idUserEvt, :siEvtCierre, :siEvtExt, :siPermiteReing, :correlacionId
            )
            """;

    /** Orden canonico del timeline: fh_evt + id. Cubierto por ix_evento_acta_timeline. */
    private static final String SQL_POR_ACTA = "SELECT " + COLUMNAS + """
            FROM fal_acta_evento
            WHERE acta_id = :actaId
            ORDER BY fh_evt, id
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_acta_evento
            WHERE id = :id
            """;

    private static final String SQL_EXISTE_CORRELACION = """
            SELECT EXISTS (
                SELECT 1 FROM fal_acta_evento
                WHERE acta_id = :actaId AND correlacion_id = :correlacionId
            )
            """;

    private final JdbcClient jdbc;

    public JdbcActaEventoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserta el evento y devuelve una copia con el id que asigno AUTO_INCREMENT.
     *
     * El id lo genera siempre la base, igual que el InMemory lo genera siempre con
     * su secuencia: un id que venga en el evento de entrada se ignora. Por eso no
     * existe camino que produzca PK duplicada a traves del puerto.
     */
    @Override
    public FalActaEvento registrar(FalActaEvento evento) {
        KeyHolder claves = new GeneratedKeyHolder();

        jdbc.sql(SQL_INSERT)
                .param("actaId", evento.actaId())
                .param("tipoEvt", evento.tipoEvt().codigo())
                .param("origenEvt", evento.origenEvt().codigo())
                .param("fhEvt", evento.fhEvt())
                .param("bloqueFunc", evento.bloqueFunc() == null ? null : evento.bloqueFunc().codigo(), Types.CHAR)
                .param("estProcAnt", evento.estProcAnt() == null ? null : evento.estProcAnt().codigo(), Types.CHAR)
                .param("estProcNvo", evento.estProcNvo() == null ? null : evento.estProcNvo().codigo(), Types.CHAR)
                .param("sitAdmAnt", evento.sitAdmAnt() == null ? null : evento.sitAdmAnt().codigo(), Types.CHAR)
                .param("sitAdmNva", evento.sitAdmNva() == null ? null : evento.sitAdmNva().codigo(), Types.CHAR)
                .param("actorTipo", evento.actorTipo() == null ? null : evento.actorTipo().codigo(), Types.SMALLINT)
                .param("actorId", evento.actorId(), Types.CHAR)
                .param("idDocuRel", evento.idDocuRel(), Types.BIGINT)
                .param("idNotifRel", evento.idNotifRel(), Types.BIGINT)
                .param("idPresRel", evento.idPresRel(), Types.BIGINT)
                .param("idUserEvt", evento.idUserEvt(), Types.CHAR)
                .param("siEvtCierre", evento.siEvtCierre())
                .param("siEvtExt", evento.siEvtExt())
                .param("siPermiteReing", evento.siPermiteReing())
                .param("correlacionId", evento.correlacionId(), Types.CHAR)
                .update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_acta_evento no devolvio el id generado para el acta " + evento.actaId());
        }
        return evento.conId(generado.longValue());
    }

    @Override
    public List<FalActaEvento> buscarPorActa(Long idActa) {
        return jdbc.sql(SQL_POR_ACTA)
                .param("actaId", idActa)
                .query(JdbcActaEventoRepository::mapear)
                .list();
    }

    @Override
    public Optional<FalActaEvento> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id)
                .query(JdbcActaEventoRepository::mapear)
                .optional();
    }

    /**
     * Misma guarda que el adaptador InMemory: un correlacionId nulo o en blanco no
     * identifica nada, de modo que no puede existir. Se resuelve sin ir a la base.
     */
    @Override
    public boolean existeCorrelacion(Long idActa, String correlacionId) {
        if (correlacionId == null || correlacionId.isBlank()) {
            return false;
        }
        Boolean existe = jdbc.sql(SQL_EXISTE_CORRELACION)
                .param("actaId", idActa)
                .param("correlacionId", correlacionId)
                .query(Boolean.class)
                .single();
        return Boolean.TRUE.equals(existe);
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static FalActaEvento mapear(ResultSet rs, int fila) throws SQLException {
        return FalActaEvento.builder()
                .id(rs.getLong("id"))
                .actaId(rs.getLong("acta_id"))
                .tipoEvt(TipoEventoActa.deCodigo(rs.getString("tipo_evt")))
                .origenEvt(OrigenEvento.fromCodigo(rs.getShort("origen_evt")))
                .fhEvt(rs.getObject("fh_evt", LocalDateTime.class))
                .bloqueFunc(bloque(rs.getString("bloque_func")))
                .estProcAnt(estadoProcesal(rs.getString("est_proc_ant")))
                .estProcNvo(estadoProcesal(rs.getString("est_proc_nvo")))
                .sitAdmAnt(situacion(rs.getString("sit_adm_ant")))
                .sitAdmNva(situacion(rs.getString("sit_adm_nva")))
                .actorTipo(actorTipo(rs.getObject("actor_tipo", Short.class)))
                .actorId(rs.getString("actor_id"))
                .idDocuRel(rs.getObject("id_docu_rel", Long.class))
                .idNotifRel(rs.getObject("id_notif_rel", Long.class))
                .idPresRel(rs.getObject("id_pres_rel", Long.class))
                .idUserEvt(rs.getString("id_user_evt"))
                .siEvtCierre(rs.getBoolean("si_evt_cierre"))
                .siEvtExt(rs.getBoolean("si_evt_ext"))
                .siPermiteReing(rs.getBoolean("si_permite_reing"))
                .correlacionId(rs.getString("correlacion_id"))
                .build();
    }

    private static BloqueActual bloque(String codigo) {
        return codigo == null ? null : BloqueActual.deCodigo(codigo);
    }

    private static EstadoProcesalActa estadoProcesal(String codigo) {
        return codigo == null ? null : EstadoProcesalActa.deCodigo(codigo);
    }

    private static SituacionAdministrativaActa situacion(String codigo) {
        return codigo == null ? null : SituacionAdministrativaActa.deCodigo(codigo);
    }

    private static ActorTipoEvento actorTipo(Short codigo) {
        return codigo == null ? null : ActorTipoEvento.fromCodigo(codigo);
    }
}
