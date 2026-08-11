package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.repository.ActaArticuloInfringidoRepository;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador JDBC de {@link ActaArticuloInfringidoRepository} sobre
 * {@code fal_acta_articulo_infringido}. Creado en Wave 4 (alta integral JDBC).
 *
 * <h2>Gap cerrado en Wave 4 PARTE-2: {@code normativaId} sin columna física</h2>
 *
 * <p>{@link FalActaArticuloInfringido#getNormativaId()} no tiene columna propia en
 * {@code fal_acta_articulo_infringido} (el DDL solo guarda {@code id_articulo}, no
 * {@code id_normativa}: es dato derivable, no denormalizado físicamente). Este adaptador lo
 * reconstruye con un JOIN a {@code fal_articulo_normativa_faltas.id_normativa}, columna que sí
 * existe y no tiene drift. No se inventa el valor: se deriva de una relación real ya modelada.
 *
 * <h2>Gap cerrado en Wave 4 PARTE-2: OCC y auditoría de baja real</h2>
 *
 * <p>La columna física {@code cantidad_unidades} se eliminó del DDL (Wave 4 PARTE-2 S7.4): la
 * cantidad efectivamente aplicada, manual u ordinaria, vive en
 * {@code fal_acta_valorizacion_item}. Se agregaron {@code version_row} (OCC real) y
 * {@code fh_baja}/{@code id_user_baja} (auditoría real de baja, sin el fallback previo que
 * aproximaba con {@code fh_alta}/{@code id_user_alta}).
 */
@Repository
@Profile("jdbc")
public class JdbcActaArticuloInfringidoRepository implements ActaArticuloInfringidoRepository {

    private static final String SQL_SELECT_BASE = """
            SELECT aai.id, aai.id_acta, ana.id_normativa AS id_normativa, aai.id_articulo,
                   aai.si_activo, aai.motivo_baja, aai.version_row, aai.fh_baja, aai.id_user_baja,
                   aai.fh_alta, aai.id_user_alta
            FROM fal_acta_articulo_infringido aai
            JOIN fal_articulo_normativa_faltas ana ON ana.id = aai.id_articulo
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_acta_articulo_infringido (
                id_acta, id_articulo, si_activo, motivo_baja, fh_baja, id_user_baja, fh_alta, id_user_alta
            ) VALUES (
                :idActa, :idArticulo, :siActivo, :motivoBaja, :fhBaja, :idUserBaja, :fhAlta, :idUserAlta
            )
            """;

    private static final String SQL_UPDATE = """
            UPDATE fal_acta_articulo_infringido
            SET si_activo = :siActivo, motivo_baja = :motivoBaja, fh_baja = :fhBaja,
                id_user_baja = :idUserBaja, version_row = version_row + 1
            WHERE id = :id AND version_row = :versionRow
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcActaArticuloInfringidoRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalActaArticuloInfringido save(FalActaArticuloInfringido articulo) {
        if (articulo == null) throw new IllegalArgumentException("articulo es obligatorio");
        Optional<FalActaArticuloInfringido> existente = findByPkFisica(articulo);
        if (existente.isPresent()) {
            int filas = jdbc.sql(SQL_UPDATE)
                    .param("siActivo", articulo.isSiActivo(), Types.BOOLEAN)
                    .param("motivoBaja", articulo.getMotivoBaja() == null ? null : articulo.getMotivoBaja().codigo(), Types.SMALLINT)
                    .param("fhBaja", articulo.getFhBaja(), Types.TIMESTAMP)
                    .param("idUserBaja", articulo.getIdUserBaja(), Types.CHAR)
                    .param("id", existente.get().getId(), Types.BIGINT)
                    .param("versionRow", existente.get().getVersionRow(), Types.INTEGER)
                    .update();
            if (filas == 0) {
                throw new ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException(
                        "FalActaArticuloInfringido", existente.get().getId(), existente.get().getVersionRow(), articulo.getVersionRow());
            }
            return findById(existente.get().getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql(SQL_INSERT)
                .param("idActa", articulo.getActaId(), Types.BIGINT)
                .param("idArticulo", articulo.getArticuloId(), Types.BIGINT)
                .param("siActivo", articulo.isSiActivo(), Types.BOOLEAN)
                .param("motivoBaja", articulo.getMotivoBaja() == null ? null : articulo.getMotivoBaja().codigo(), Types.SMALLINT)
                .param("fhBaja", articulo.getFhBaja(), Types.TIMESTAMP)
                .param("idUserBaja", articulo.getIdUserBaja(), Types.CHAR)
                .param("fhAlta", articulo.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", articulo.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_acta_articulo_infringido no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    /**
     * {@code save} no distingue alta de corrección salvo por el id provisional (no persistente).
     * Como el id real lo asigna la base, un artículo "existente" solo puede identificarse por su
     * PK real ya conocida; si el llamador reconstruyó el agregado desde una lectura previa
     * (id real, no provisional), se busca por ese id.
     */
    private Optional<FalActaArticuloInfringido> findByPkFisica(FalActaArticuloInfringido articulo) {
        if (articulo.getId() == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_BASE + " WHERE aai.id = :id")
                .param("id", articulo.getId(), Types.BIGINT)
                .query(JdbcActaArticuloInfringidoRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalActaArticuloInfringido> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_BASE + " WHERE aai.id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcActaArticuloInfringidoRepository::mapear)
                .optional();
    }

    @Override
    public List<FalActaArticuloInfringido> findByActaId(Long actaId) {
        if (actaId == null) return List.of();
        return jdbc.sql(SQL_SELECT_BASE + " WHERE aai.id_acta = :idActa ORDER BY aai.id")
                .param("idActa", actaId, Types.BIGINT)
                .query(JdbcActaArticuloInfringidoRepository::mapear).list();
    }

    @Override
    public List<FalActaArticuloInfringido> findActivosByActaId(Long actaId) {
        if (actaId == null) return List.of();
        return jdbc.sql(SQL_SELECT_BASE + " WHERE aai.id_acta = :idActa AND aai.si_activo = TRUE ORDER BY aai.id")
                .param("idActa", actaId, Types.BIGINT)
                .query(JdbcActaArticuloInfringidoRepository::mapear).list();
    }

    @Override
    public Optional<FalActaArticuloInfringido> findActivoByActaAndArticulo(Long actaId, Long articuloId) {
        if (actaId == null || articuloId == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_BASE + """
                 WHERE aai.id_acta = :idActa AND aai.id_articulo = :idArticulo AND aai.si_activo = TRUE
                """)
                .param("idActa", actaId, Types.BIGINT)
                .param("idArticulo", articuloId, Types.BIGINT)
                .query(JdbcActaArticuloInfringidoRepository::mapear)
                .optional();
    }

    @Override
    public boolean existsActivo(Long actaId, Long articuloId) {
        return findActivoByActaAndArticulo(actaId, articuloId).isPresent();
    }

    private static FalActaArticuloInfringido mapear(ResultSet rs, int fila) throws SQLException {
        FalActaArticuloInfringido a = new FalActaArticuloInfringido(
                rs.getObject("id", Long.class),
                rs.getObject("id_acta", Long.class),
                rs.getObject("id_normativa", Long.class),
                rs.getObject("id_articulo", Long.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        boolean activo = rs.getBoolean("si_activo");
        if (!activo) {
            Short motivo = (Short) rs.getObject("motivo_baja");
            LocalDateTime fhBaja = rs.getObject("fh_baja", LocalDateTime.class);
            String idUserBaja = rs.getString("id_user_baja");
            a.darDeBaja(
                    motivo == null ? MotivoBajaArticuloInfringido.CORRECCION_IMPUTACION : MotivoBajaArticuloInfringido.fromCodigo(motivo),
                    fhBaja != null ? fhBaja : rs.getObject("fh_alta", LocalDateTime.class),
                    idUserBaja != null ? idUserBaja : rs.getString("id_user_alta"));
        }
        a.setVersionRow(rs.getInt("version_row"));
        return a;
    }
}
