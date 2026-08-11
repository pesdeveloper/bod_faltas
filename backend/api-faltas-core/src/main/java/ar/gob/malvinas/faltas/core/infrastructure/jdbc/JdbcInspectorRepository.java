package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link InspectorRepository} sobre {@code fal_inspector} y
 * {@code fal_inspector_version} (Wave 4 PARTE-3A §10). Ver Javadoc de
 * {@link JdbcDependenciaRepository} para el motivo de esta adición (gap previo sin adapter
 * JDBC alguno).
 */
@Repository
@Profile("jdbc")
public class JdbcInspectorRepository implements InspectorRepository {

    private static final String SQL_SELECT_INSPECTOR = """
            SELECT id, id_user, legajo_insp, nom_insp, si_activo, fh_alta, id_user_alta
            FROM fal_inspector
            """;

    private static final String SQL_SELECT_VERSION = """
            SELECT id_insp, ver_insp, legajo_insp, nom_insp, id_dep, ver_dep,
                   fh_vig_desde, fh_vig_hasta, si_activo, fh_alta, id_user_alta
            FROM fal_inspector_version
            """;

    private final JdbcClient jdbc;
    private final FaltasClock faltasClock;

    public JdbcInspectorRepository(JdbcClient jdbc, FaltasClock faltasClock) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        if (faltasClock == null) throw new IllegalArgumentException("faltasClock es obligatorio");
        this.jdbc = jdbc;
        this.faltasClock = faltasClock;
    }

    @Override
    public FalInspector guardar(FalInspector inspector) {
        if (inspector.getIdInsp() != null && existePorId(inspector.getIdInsp())) {
            jdbc.sql("""
                    UPDATE fal_inspector SET legajo_insp=:legajo, nom_insp=:nom, si_activo=:siActivo
                    WHERE id=:id
                    """)
                    .param("legajo", inspector.getLegajoInsp(), Types.INTEGER)
                    .param("nom", inspector.getNomInsp())
                    .param("siActivo", inspector.isSiActivo(), Types.BOOLEAN)
                    .param("id", inspector.getIdInsp(), Types.BIGINT)
                    .update();
            return findById(inspector.getIdInsp()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_inspector (id_user, legajo_insp, nom_insp, si_activo, fh_alta, id_user_alta)
                VALUES (:idUser, :legajo, :nom, :siActivo, :fhAlta, :idUserAlta)
                """)
                .param("idUser", inspector.getIdUser())
                .param("legajo", inspector.getLegajoInsp(), Types.INTEGER)
                .param("nom", inspector.getNomInsp())
                .param("siActivo", inspector.isSiActivo(), Types.BOOLEAN)
                .param("fhAlta", inspector.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", inspector.getIdUserAlta(), Types.CHAR)
                .update(claves);
        return findById(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<FalInspector> findById(Long idInsp) {
        if (idInsp == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_INSPECTOR + " WHERE id = :id")
                .param("id", idInsp, Types.BIGINT)
                .query(JdbcInspectorRepository::mapearInspector)
                .optional();
    }

    @Override
    public boolean existsByIdUser(String idUser) {
        if (idUser == null) return false;
        return jdbc.sql("SELECT COUNT(*) FROM fal_inspector WHERE id_user = :idUser")
                .param("idUser", idUser).query(Integer.class).single() > 0;
    }

    @Override
    public List<FalInspector> findAllActivos() {
        return jdbc.sql(SQL_SELECT_INSPECTOR + " WHERE si_activo = TRUE ORDER BY id")
                .query(JdbcInspectorRepository::mapearInspector)
                .list();
    }

    @Override
    public FalInspectorVersion guardarVersion(FalInspectorVersion version) {
        jdbc.sql("""
                INSERT INTO fal_inspector_version
                    (id_insp, ver_insp, legajo_insp, nom_insp, id_dep, ver_dep,
                     fh_vig_desde, fh_vig_hasta, si_activo, fh_alta, id_user_alta)
                VALUES
                    (:idInsp, :verInsp, :legajo, :nom, :idDep, :verDep,
                     :fhVigDesde, :fhVigHasta, :siActivo, :fhAlta, :idUserAlta)
                ON DUPLICATE KEY UPDATE
                    legajo_insp=:legajo, nom_insp=:nom, id_dep=:idDep, ver_dep=:verDep,
                    fh_vig_hasta=:fhVigHasta, si_activo=:siActivo
                """)
                .param("idInsp", version.getIdInsp(), Types.BIGINT)
                .param("verInsp", version.getVerInsp(), Types.SMALLINT)
                .param("legajo", version.getLegajoInsp(), Types.INTEGER)
                .param("nom", version.getNomInsp())
                .param("idDep", version.getIdDep(), Types.BIGINT)
                .param("verDep", version.getVerDep(), Types.SMALLINT)
                .param("fhVigDesde", version.getFhVigDesde())
                .param("fhVigHasta", version.getFhVigHasta())
                .param("siActivo", version.isSiActivo(), Types.BOOLEAN)
                .param("fhAlta", faltasClock.now(), Types.TIMESTAMP)
                .param("idUserAlta", "SYS", Types.CHAR)
                .update();
        return findVersion(version.getIdInsp(), version.getVerInsp()).orElseThrow();
    }

    @Override
    public List<FalInspectorVersion> findVersionesByInsp(Long idInsp) {
        return jdbc.sql(SQL_SELECT_VERSION + " WHERE id_insp = :idInsp ORDER BY ver_insp")
                .param("idInsp", idInsp, Types.BIGINT)
                .query(JdbcInspectorRepository::mapearVersion)
                .list();
    }

    @Override
    public Optional<FalInspectorVersion> findVersionVigente(Long idInsp, LocalDate fecha) {
        return jdbc.sql(SQL_SELECT_VERSION + """
                 WHERE id_insp = :idInsp AND si_activo = TRUE
                   AND fh_vig_desde <= :fecha
                   AND (fh_vig_hasta IS NULL OR fh_vig_hasta > :fecha)
                 ORDER BY ver_insp DESC
                 LIMIT 1
                """)
                .param("idInsp", idInsp, Types.BIGINT)
                .param("fecha", fecha)
                .query(JdbcInspectorRepository::mapearVersion)
                .optional();
    }

    @Override
    public int maxVerInsp(Long idInsp) {
        Integer max = jdbc.sql("SELECT MAX(ver_insp) FROM fal_inspector_version WHERE id_insp = :idInsp")
                .param("idInsp", idInsp, Types.BIGINT)
                .query(Integer.class)
                .optional()
                .orElse(null);
        return max == null ? 0 : max;
    }

    private Optional<FalInspectorVersion> findVersion(Long idInsp, int verInsp) {
        return jdbc.sql(SQL_SELECT_VERSION + " WHERE id_insp = :idInsp AND ver_insp = :verInsp")
                .param("idInsp", idInsp, Types.BIGINT)
                .param("verInsp", verInsp, Types.SMALLINT)
                .query(JdbcInspectorRepository::mapearVersion)
                .optional();
    }

    private boolean existePorId(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_inspector WHERE id = :id")
                .param("id", id, Types.BIGINT).query(Integer.class).single() > 0;
    }

    private static FalInspector mapearInspector(ResultSet rs, int fila) throws SQLException {
        FalInspector inspector = new FalInspector(
                rs.getObject("id", Long.class),
                rs.getString("id_user"),
                rs.getInt("legajo_insp"),
                rs.getString("nom_insp"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        inspector.setSiActivo(rs.getBoolean("si_activo"));
        return inspector;
    }

    private static FalInspectorVersion mapearVersion(ResultSet rs, int fila) throws SQLException {
        FalInspectorVersion version = new FalInspectorVersion(
                rs.getObject("id_insp", Long.class),
                rs.getInt("ver_insp"),
                rs.getInt("legajo_insp"),
                rs.getString("nom_insp"),
                (Long) rs.getObject("id_dep"),
                rs.getInt("ver_dep"),
                rs.getObject("fh_vig_desde", LocalDate.class));
        version.setFhVigHasta(rs.getObject("fh_vig_hasta", LocalDate.class));
        version.setSiActivo(rs.getBoolean("si_activo"));
        return version;
    }
}
