package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
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
 * Adaptador JDBC de {@link DependenciaRepository} sobre {@code fal_dependencia} y
 * {@code fal_dependencia_version} (Wave 4 PARTE-3A §10).
 *
 * <p>Antes de esta sesión, {@code DependenciaRepository} y {@code InspectorRepository}
 * carecían de adapter JDBC (el puerto existía desde Slice 8A pero nunca se migró): bajo el
 * perfil {@code jdbc}, {@code InMemoryDependenciaRepository} quedaba activo como único bean
 * y no reflejaba las filas reales de MariaDB. Este adapter cierra ese gap, requisito previo
 * para poder validar dependencia/inspector antes de numerar (prompt §10).
 */
@Repository
@Profile("jdbc")
public class JdbcDependenciaRepository implements DependenciaRepository {

    private static final String SQL_SELECT_DEPENDENCIA = """
            SELECT id, cod_dep, nom_dep, id_dep_padre, si_activa, fh_alta, id_user_alta
            FROM fal_dependencia
            """;

    private static final String SQL_SELECT_VERSION = """
            SELECT id_dep, ver_dep, nom_dep, id_dep_padre, ver_dep_padre, tipo_acta,
                   fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta
            FROM fal_dependencia_version
            """;

    private final JdbcClient jdbc;
    private final FaltasClock faltasClock;

    public JdbcDependenciaRepository(JdbcClient jdbc, FaltasClock faltasClock) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        if (faltasClock == null) throw new IllegalArgumentException("faltasClock es obligatorio");
        this.jdbc = jdbc;
        this.faltasClock = faltasClock;
    }

    @Override
    public FalDependencia guardar(FalDependencia dependencia) {
        if (dependencia.getIdDep() != null && existePorId(dependencia.getIdDep())) {
            jdbc.sql("""
                    UPDATE fal_dependencia SET cod_dep=:codDep, nom_dep=:nomDep,
                        id_dep_padre=:idDepPadre, si_activa=:siActiva
                    WHERE id=:id
                    """)
                    .param("codDep", dependencia.getCodDep())
                    .param("nomDep", dependencia.getNomDep())
                    .param("idDepPadre", dependencia.getIdDepPadre(), Types.BIGINT)
                    .param("siActiva", dependencia.isSiActiva(), Types.BOOLEAN)
                    .param("id", dependencia.getIdDep(), Types.BIGINT)
                    .update();
            return findById(dependencia.getIdDep()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_dependencia (cod_dep, nom_dep, id_dep_padre, si_activa, fh_alta, id_user_alta)
                VALUES (:codDep, :nomDep, :idDepPadre, :siActiva, :fhAlta, :idUserAlta)
                """)
                .param("codDep", dependencia.getCodDep())
                .param("nomDep", dependencia.getNomDep())
                .param("idDepPadre", dependencia.getIdDepPadre(), Types.BIGINT)
                .param("siActiva", dependencia.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", dependencia.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", dependencia.getIdUserAlta(), Types.CHAR)
                .update(claves);
        return findById(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<FalDependencia> findById(Long idDep) {
        if (idDep == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_DEPENDENCIA + " WHERE id = :id")
                .param("id", idDep, Types.BIGINT)
                .query(JdbcDependenciaRepository::mapearDependencia)
                .optional();
    }

    @Override
    public List<FalDependencia> findAllActivas() {
        return jdbc.sql(SQL_SELECT_DEPENDENCIA + " WHERE si_activa = TRUE ORDER BY id")
                .query(JdbcDependenciaRepository::mapearDependencia)
                .list();
    }

    @Override
    public boolean existsByCodDep(String codDep) {
        return findByCodDep(codDep).isPresent();
    }

    @Override
    public Optional<FalDependencia> findByCodDep(String codDep) {
        if (codDep == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_DEPENDENCIA + " WHERE cod_dep = :codDep")
                .param("codDep", codDep)
                .query(JdbcDependenciaRepository::mapearDependencia)
                .optional();
    }

    @Override
    public FalDependenciaVersion guardarVersion(FalDependenciaVersion version) {
        jdbc.sql("""
                INSERT INTO fal_dependencia_version
                    (id_dep, ver_dep, nom_dep, id_dep_padre, ver_dep_padre, tipo_acta,
                     fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta)
                VALUES
                    (:idDep, :verDep, :nomDep, :idDepPadre, :verDepPadre, :tipoActa,
                     :fhVigDesde, :fhVigHasta, :siActiva, :fhAlta, :idUserAlta)
                ON DUPLICATE KEY UPDATE
                    nom_dep=:nomDep, id_dep_padre=:idDepPadre, ver_dep_padre=:verDepPadre,
                    tipo_acta=:tipoActa, fh_vig_hasta=:fhVigHasta, si_activa=:siActiva
                """)
                .param("idDep", version.getIdDep(), Types.BIGINT)
                .param("verDep", version.getVerDep(), Types.SMALLINT)
                .param("nomDep", version.getNomDep())
                .param("idDepPadre", version.getIdDepPadre(), Types.BIGINT)
                .param("verDepPadre", version.getVerDepPadre(), Types.SMALLINT)
                .param("tipoActa", version.getTipoActa() == null ? null : version.getTipoActa().codigo(), Types.SMALLINT)
                .param("fhVigDesde", version.getFhVigDesde())
                .param("fhVigHasta", version.getFhVigHasta())
                .param("siActiva", version.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", faltasClock.now(), Types.TIMESTAMP)
                .param("idUserAlta", "SYS", Types.CHAR)
                .update();
        return findVersion(version.getIdDep(), version.getVerDep()).orElseThrow();
    }

    @Override
    public List<FalDependenciaVersion> findVersionesByDep(Long idDep) {
        return jdbc.sql(SQL_SELECT_VERSION + " WHERE id_dep = :idDep ORDER BY ver_dep")
                .param("idDep", idDep, Types.BIGINT)
                .query(JdbcDependenciaRepository::mapearVersion)
                .list();
    }

    @Override
    public Optional<FalDependenciaVersion> findVersionVigente(Long idDep, LocalDate fecha) {
        return jdbc.sql(SQL_SELECT_VERSION + """
                 WHERE id_dep = :idDep AND si_activa = TRUE
                   AND fh_vig_desde <= :fecha
                   AND (fh_vig_hasta IS NULL OR fh_vig_hasta > :fecha)
                 ORDER BY ver_dep DESC
                 LIMIT 1
                """)
                .param("idDep", idDep, Types.BIGINT)
                .param("fecha", fecha)
                .query(JdbcDependenciaRepository::mapearVersion)
                .optional();
    }

    @Override
    public int maxVerDep(Long idDep) {
        Integer max = jdbc.sql("SELECT MAX(ver_dep) FROM fal_dependencia_version WHERE id_dep = :idDep")
                .param("idDep", idDep, Types.BIGINT)
                .query(Integer.class)
                .optional()
                .orElse(null);
        return max == null ? 0 : max;
    }

    private Optional<FalDependenciaVersion> findVersion(Long idDep, int verDep) {
        return jdbc.sql(SQL_SELECT_VERSION + " WHERE id_dep = :idDep AND ver_dep = :verDep")
                .param("idDep", idDep, Types.BIGINT)
                .param("verDep", verDep, Types.SMALLINT)
                .query(JdbcDependenciaRepository::mapearVersion)
                .optional();
    }

    private boolean existePorId(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_dependencia WHERE id = :id")
                .param("id", id, Types.BIGINT).query(Integer.class).single() > 0;
    }

    private static FalDependencia mapearDependencia(ResultSet rs, int fila) throws SQLException {
        FalDependencia dep = new FalDependencia(
                rs.getObject("id", Long.class),
                rs.getString("nom_dep"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        dep.setCodDep(rs.getString("cod_dep"));
        dep.setIdDepPadre((Long) rs.getObject("id_dep_padre"));
        dep.setSiActiva(rs.getBoolean("si_activa"));
        return dep;
    }

    private static FalDependenciaVersion mapearVersion(ResultSet rs, int fila) throws SQLException {
        Short tipoActaCodigo = (Short) rs.getObject("tipo_acta");
        FalDependenciaVersion version = new FalDependenciaVersion(
                rs.getObject("id_dep", Long.class),
                rs.getInt("ver_dep"),
                rs.getString("nom_dep"),
                (Long) rs.getObject("id_dep_padre"),
                (Integer) rs.getObject("ver_dep_padre"),
                tipoActaCodigo == null ? null : TipoActa.fromCodigo(tipoActaCodigo),
                rs.getObject("fh_vig_desde", LocalDate.class));
        version.setFhVigHasta(rs.getObject("fh_vig_hasta", LocalDate.class));
        version.setSiActiva(rs.getBoolean("si_activa"));
        return version;
    }
}
