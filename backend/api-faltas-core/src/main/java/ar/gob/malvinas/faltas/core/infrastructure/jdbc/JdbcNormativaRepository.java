package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
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
 * Adaptador JDBC de {@link NormativaRepository} sobre {@code fal_normativa_faltas},
 * {@code fal_articulo_normativa_faltas} y {@code fal_dependencia_normativa}.
 *
 * Creado en Wave 4 PARTE-2 (cierra el bloqueo documentado en CHECKPOINT.md §3):
 * el dominio Java ahora tiene versionado real ({@code version_norma}/{@code
 * version_articulo} + {@code version_row}) que coincide con el DDL rediseñado.
 * No contiene datos economicos: eso vive en {@code fal_tarifario_unidad_faltas}.
 */
@Repository
@Profile("jdbc")
public class JdbcNormativaRepository implements NormativaRepository {

    private static final String SQL_SELECT_NORMATIVA = """
            SELECT id, codigo_norma, version_norma, nombre_norma, descripcion_norma,
                   si_activa, fh_vig_desde, fh_vig_hasta, version_row, fh_alta, id_user_alta
            FROM fal_normativa_faltas
            """;

    private static final String SQL_SELECT_ARTICULO = """
            SELECT id, id_normativa, codigo_articulo, version_articulo, nombre_articulo,
                   descripcion_articulo, tipo_infraccion, categoria_infraccion, si_admite_medida_preventiva,
                   si_activo, fh_vig_desde, fh_vig_hasta, version_row, fh_alta, id_user_alta
            FROM fal_articulo_normativa_faltas
            """;

    private static final String SQL_SELECT_DEP_NORM = """
            SELECT id_dep, ver_dep, id_normativa, si_activa, fh_alta, id_user_alta
            FROM fal_dependencia_normativa
            """;

    private final JdbcClient jdbc;

    public JdbcNormativaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    // ---- Normativa ----

    @Override
    public FalNormativaFaltas guardarNormativa(FalNormativaFaltas n) {
        Optional<FalNormativaFaltas> porId = n.getId() != null && n.getId() > 0 && existePorIdReal(n.getId())
                ? findNormativaById(n.getId()) : Optional.empty();
        if (porId.isPresent()) {
            jdbc.sql("""
                    UPDATE fal_normativa_faltas
                    SET nombre_norma=:nombre, descripcion_norma=:descripcion, si_activa=:siActiva,
                        fh_vig_hasta=:fhVigHasta, version_row = version_row + 1
                    WHERE id=:id AND version_row=:versionRow
                    """)
                    .param("nombre", n.getNombreNorma())
                    .param("descripcion", n.getDescripcionNorma())
                    .param("siActiva", n.isSiActiva(), Types.BOOLEAN)
                    .param("fhVigHasta", n.getFhVigHasta())
                    .param("id", n.getId(), Types.BIGINT)
                    .param("versionRow", n.getVersionRow(), Types.INTEGER)
                    .update();
            return findNormativaById(n.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_normativa_faltas (
                    codigo_norma, version_norma, nombre_norma, descripcion_norma, si_activa,
                    fh_vig_desde, fh_vig_hasta, fh_alta, id_user_alta
                ) VALUES (
                    :codigoNorma, :versionNorma, :nombreNorma, :descripcionNorma, :siActiva,
                    :fhVigDesde, :fhVigHasta, :fhAlta, :idUserAlta
                )
                """)
                .param("codigoNorma", n.getCodigoNorma())
                .param("versionNorma", n.getVersionNorma(), Types.SMALLINT)
                .param("nombreNorma", n.getNombreNorma())
                .param("descripcionNorma", n.getDescripcionNorma())
                .param("siActiva", n.isSiActiva(), Types.BOOLEAN)
                .param("fhVigDesde", n.getFhVigDesde())
                .param("fhVigHasta", n.getFhVigHasta())
                .param("fhAlta", n.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", n.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_normativa_faltas no devolvio id generado");
        return findNormativaById(generado.longValue()).orElseThrow();
    }

    private boolean existePorIdReal(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_normativa_faltas WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(Integer.class).single() > 0;
    }

    @Override
    public Optional<FalNormativaFaltas> findNormativaById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_NORMATIVA + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearNormativa)
                .optional();
    }

    @Override
    public Optional<FalNormativaFaltas> findNormativaByCodigoYVersion(String codigoNorma, int versionNorma) {
        return jdbc.sql(SQL_SELECT_NORMATIVA + " WHERE codigo_norma = :codigo AND version_norma = :version")
                .param("codigo", codigoNorma)
                .param("version", versionNorma, Types.SMALLINT)
                .query(JdbcNormativaRepository::mapearNormativa)
                .optional();
    }

    @Override
    public boolean existsNormativaByCodigoYVersion(String codigoNorma, int versionNorma) {
        return findNormativaByCodigoYVersion(codigoNorma, versionNorma).isPresent();
    }

    @Override
    public List<FalNormativaFaltas> findAllNormativasActivas() {
        return jdbc.sql(SQL_SELECT_NORMATIVA + " WHERE si_activa = TRUE ORDER BY id")
                .query(JdbcNormativaRepository::mapearNormativa).list();
    }

    // ---- Articulo ----

    @Override
    public FalArticuloNormativaFaltas guardarArticulo(FalArticuloNormativaFaltas a) {
        Optional<FalArticuloNormativaFaltas> porId = a.getId() != null && a.getId() > 0 && existeArticuloPorIdReal(a.getId())
                ? findArticuloById(a.getId()) : Optional.empty();
        if (porId.isPresent()) {
            jdbc.sql("""
                    UPDATE fal_articulo_normativa_faltas
                    SET nombre_articulo=:nombre, descripcion_articulo=:descripcion, si_activo=:siActivo,
                        fh_vig_hasta=:fhVigHasta, version_row = version_row + 1
                    WHERE id=:id AND version_row=:versionRow
                    """)
                    .param("nombre", a.getNombreArticulo())
                    .param("descripcion", a.getDescripcionArticulo())
                    .param("siActivo", a.isSiActivo(), Types.BOOLEAN)
                    .param("fhVigHasta", a.getFhVigHasta())
                    .param("id", a.getId(), Types.BIGINT)
                    .param("versionRow", a.getVersionRow(), Types.INTEGER)
                    .update();
            return findArticuloById(a.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_articulo_normativa_faltas (
                    id_normativa, codigo_articulo, version_articulo, nombre_articulo, descripcion_articulo,
                    tipo_infraccion, categoria_infraccion, si_admite_medida_preventiva,
                    si_activo, fh_vig_desde, fh_vig_hasta, fh_alta, id_user_alta
                ) VALUES (
                    :idNormativa, :codigoArticulo, :versionArticulo, :nombreArticulo, :descripcionArticulo,
                    :tipoInfraccion, :categoriaInfraccion, :siAdmiteMedidaPreventiva,
                    :siActivo, :fhVigDesde, :fhVigHasta, :fhAlta, :idUserAlta
                )
                """)
                .param("idNormativa", a.getNormativaId(), Types.BIGINT)
                .param("codigoArticulo", a.getCodigoArticulo())
                .param("versionArticulo", a.getVersionArticulo(), Types.SMALLINT)
                .param("nombreArticulo", a.getNombreArticulo())
                .param("descripcionArticulo", a.getDescripcionArticulo())
                .param("tipoInfraccion", a.getTipoInfraccion().codigo(), Types.SMALLINT)
                .param("categoriaInfraccion", a.getCategoriaInfraccion() == null ? null : a.getCategoriaInfraccion().codigo(), Types.SMALLINT)
                .param("siAdmiteMedidaPreventiva", a.isSiAdmiteMedidaPreventiva(), Types.BOOLEAN)
                .param("siActivo", a.isSiActivo(), Types.BOOLEAN)
                .param("fhVigDesde", a.getFhVigDesde())
                .param("fhVigHasta", a.getFhVigHasta())
                .param("fhAlta", a.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", a.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_articulo_normativa_faltas no devolvio id generado");
        return findArticuloById(generado.longValue()).orElseThrow();
    }

    private boolean existeArticuloPorIdReal(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_articulo_normativa_faltas WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(Integer.class).single() > 0;
    }

    @Override
    public Optional<FalArticuloNormativaFaltas> findArticuloById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_ARTICULO + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearArticulo)
                .optional();
    }

    @Override
    public Optional<FalArticuloNormativaFaltas> findArticuloByNormativaYCodigo(Long normativaId, String codigoArticulo, int versionArticulo) {
        return jdbc.sql(SQL_SELECT_ARTICULO + " WHERE id_normativa = :idNormativa AND codigo_articulo = :codigo AND version_articulo = :version")
                .param("idNormativa", normativaId, Types.BIGINT)
                .param("codigo", codigoArticulo)
                .param("version", versionArticulo, Types.SMALLINT)
                .query(JdbcNormativaRepository::mapearArticulo)
                .optional();
    }

    @Override
    public boolean existsArticuloByNormativaYCodigo(Long normativaId, String codigoArticulo, int versionArticulo) {
        return findArticuloByNormativaYCodigo(normativaId, codigoArticulo, versionArticulo).isPresent();
    }

    @Override
    public List<FalArticuloNormativaFaltas> findArticulosByNormativa(Long normativaId) {
        return jdbc.sql(SQL_SELECT_ARTICULO + " WHERE id_normativa = :idNormativa ORDER BY id")
                .param("idNormativa", normativaId, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearArticulo).list();
    }

    @Override
    public List<FalArticuloNormativaFaltas> findArticulosActivosByNormativa(Long normativaId) {
        return jdbc.sql(SQL_SELECT_ARTICULO + " WHERE id_normativa = :idNormativa AND si_activo = TRUE ORDER BY id")
                .param("idNormativa", normativaId, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearArticulo).list();
    }

    // ---- Dependencia-Normativa ----

    @Override
    public FalDependenciaNormativa guardarDependenciaNormativa(FalDependenciaNormativa rel) {
        jdbc.sql("""
                INSERT INTO fal_dependencia_normativa (id_dep, ver_dep, id_normativa, si_activa, fh_alta, id_user_alta)
                VALUES (:idDep, :verDep, :idNormativa, :siActiva, :fhAlta, :idUserAlta)
                ON DUPLICATE KEY UPDATE si_activa = VALUES(si_activa)
                """)
                .param("idDep", rel.getIdDep(), Types.BIGINT)
                .param("verDep", rel.getVerDep(), Types.SMALLINT)
                .param("idNormativa", rel.getNormativaId(), Types.BIGINT)
                .param("siActiva", rel.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", rel.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", rel.getIdUserAlta(), Types.CHAR)
                .update();
        return findDependenciaNormativa(rel.getIdDep(), rel.getVerDep(), rel.getNormativaId()).orElseThrow();
    }

    @Override
    public Optional<FalDependenciaNormativa> findDependenciaNormativa(Long idDep, int verDep, Long normativaId) {
        return jdbc.sql(SQL_SELECT_DEP_NORM + " WHERE id_dep=:idDep AND ver_dep=:verDep AND id_normativa=:idNormativa")
                .param("idDep", idDep, Types.BIGINT)
                .param("verDep", verDep, Types.SMALLINT)
                .param("idNormativa", normativaId, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearDepNorm)
                .optional();
    }

    @Override
    public boolean existsDependenciaNormativaActiva(Long idDep, int verDep, Long normativaId) {
        return jdbc.sql(SQL_SELECT_DEP_NORM + " WHERE id_dep=:idDep AND ver_dep=:verDep AND id_normativa=:idNormativa AND si_activa=TRUE")
                .param("idDep", idDep, Types.BIGINT)
                .param("verDep", verDep, Types.SMALLINT)
                .param("idNormativa", normativaId, Types.BIGINT)
                .query(JdbcNormativaRepository::mapearDepNorm)
                .optional().isPresent();
    }

    @Override
    public List<FalDependenciaNormativa> findNormativasByDepVersion(Long idDep, int verDep) {
        return jdbc.sql(SQL_SELECT_DEP_NORM + " WHERE id_dep=:idDep AND ver_dep=:verDep")
                .param("idDep", idDep, Types.BIGINT)
                .param("verDep", verDep, Types.SMALLINT)
                .query(JdbcNormativaRepository::mapearDepNorm).list();
    }

    @Override
    public List<FalDependenciaNormativa> findNormativasActivasByDepVersion(Long idDep, int verDep) {
        return jdbc.sql(SQL_SELECT_DEP_NORM + " WHERE id_dep=:idDep AND ver_dep=:verDep AND si_activa=TRUE")
                .param("idDep", idDep, Types.BIGINT)
                .param("verDep", verDep, Types.SMALLINT)
                .query(JdbcNormativaRepository::mapearDepNorm).list();
    }

    // ---- Mapeo ----

    private static FalNormativaFaltas mapearNormativa(ResultSet rs, int fila) throws SQLException {
        FalNormativaFaltas n = new FalNormativaFaltas(
                rs.getObject("id", Long.class),
                rs.getString("codigo_norma"),
                rs.getInt("version_norma"),
                rs.getString("nombre_norma"),
                rs.getObject("fh_vig_desde", LocalDate.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        n.setDescripcionNorma(rs.getString("descripcion_norma"));
        n.setSiActiva(rs.getBoolean("si_activa"));
        LocalDate fhVigHasta = rs.getObject("fh_vig_hasta", LocalDate.class);
        if (fhVigHasta != null) n.setFhVigHasta(fhVigHasta);
        n.setVersionRow(rs.getInt("version_row"));
        return n;
    }

    private static FalArticuloNormativaFaltas mapearArticulo(ResultSet rs, int fila) throws SQLException {
        FalArticuloNormativaFaltas a = new FalArticuloNormativaFaltas(
                rs.getObject("id", Long.class),
                rs.getObject("id_normativa", Long.class),
                rs.getString("codigo_articulo"),
                rs.getInt("version_articulo"),
                rs.getString("nombre_articulo"),
                ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion.fromCodigo(rs.getShort("tipo_infraccion")),
                rs.getObject("fh_vig_desde", LocalDate.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        a.setDescripcionArticulo(rs.getString("descripcion_articulo"));
        Short categoria = (Short) rs.getObject("categoria_infraccion");
        if (categoria != null) a.setCategoriaInfraccion(ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion.fromCodigo(categoria));
        a.setSiAdmiteMedidaPreventiva(rs.getBoolean("si_admite_medida_preventiva"));
        a.setSiActivo(rs.getBoolean("si_activo"));
        LocalDate fhVigHasta = rs.getObject("fh_vig_hasta", LocalDate.class);
        if (fhVigHasta != null) a.setFhVigHasta(fhVigHasta);
        a.setVersionRow(rs.getInt("version_row"));
        return a;
    }

    private static FalDependenciaNormativa mapearDepNorm(ResultSet rs, int fila) throws SQLException {
        FalDependenciaNormativa rel = new FalDependenciaNormativa(
                rs.getObject("id_dep", Long.class),
                rs.getInt("ver_dep"),
                rs.getObject("id_normativa", Long.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        rel.setSiActiva(rs.getBoolean("si_activa"));
        return rel;
    }
}
