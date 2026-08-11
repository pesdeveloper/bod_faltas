package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.MedidaPreventivaRepository;
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
 * Adaptador JDBC de {@link MedidaPreventivaRepository} sobre {@code fal_medida_preventiva}
 * (catálogo, versionado real vía {@code version_medida}). Creado en Wave 4. Sin drift: 12
 * columnas físicas mapean 1:1 con {@link FalMedidaPreventiva}.
 */
@Repository
@Profile("jdbc")
public class JdbcMedidaPreventivaRepository implements MedidaPreventivaRepository {

    private static final String COLUMNAS = """
            id, codigo, version_medida, descripcion, descripcion_detalle, id_dep, ver_dep,
            si_activa, si_puede_bloquear_cierre, tipo_bloqueante_default, fh_alta, id_user_alta
            """;

    private final java.util.concurrent.atomic.AtomicLong provisional = new java.util.concurrent.atomic.AtomicLong(1);
    private final JdbcClient jdbc;

    public JdbcMedidaPreventivaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalMedidaPreventiva save(FalMedidaPreventiva medida) {
        if (medida == null) throw new IllegalArgumentException("medida es obligatoria");
        Optional<FalMedidaPreventiva> existente = findByCodigoAndVersion(medida.getCodigo(), medida.getVersionMedida());
        if (existente.isPresent()) {
            jdbc.sql("""
                    UPDATE fal_medida_preventiva SET
                        descripcion_detalle = :descripcionDetalle, id_dep = :idDep, ver_dep = :verDep,
                        si_activa = :siActiva, si_puede_bloquear_cierre = :siPuedeBloquearCierre,
                        tipo_bloqueante_default = :tipoBloqueanteDefault
                    WHERE id = :id
                    """)
                    .param("descripcionDetalle", medida.getDescripcionDetalle(), Types.VARCHAR)
                    .param("idDep", medida.getIdDep(), Types.BIGINT)
                    .param("verDep", medida.getVerDep(), Types.SMALLINT)
                    .param("siActiva", medida.isSiActiva(), Types.BOOLEAN)
                    .param("siPuedeBloquearCierre", medida.isSiPuedeBloquearCierre(), Types.BOOLEAN)
                    .param("tipoBloqueanteDefault", medida.getTipoBloqueanteDefault() == null ? null : medida.getTipoBloqueanteDefault().codigo(), Types.SMALLINT)
                    .param("id", existente.get().getId(), Types.BIGINT)
                    .update();
            return findById(existente.get().getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_medida_preventiva (
                    codigo, version_medida, descripcion, descripcion_detalle, id_dep, ver_dep,
                    si_activa, si_puede_bloquear_cierre, tipo_bloqueante_default, fh_alta, id_user_alta
                ) VALUES (
                    :codigo, :versionMedida, :descripcion, :descripcionDetalle, :idDep, :verDep,
                    :siActiva, :siPuedeBloquearCierre, :tipoBloqueanteDefault, :fhAlta, :idUserAlta
                )
                """)
                .param("codigo", medida.getCodigo(), Types.VARCHAR)
                .param("versionMedida", medida.getVersionMedida(), Types.SMALLINT)
                .param("descripcion", medida.getDescripcion(), Types.VARCHAR)
                .param("descripcionDetalle", medida.getDescripcionDetalle(), Types.VARCHAR)
                .param("idDep", medida.getIdDep(), Types.BIGINT)
                .param("verDep", medida.getVerDep(), Types.SMALLINT)
                .param("siActiva", medida.isSiActiva(), Types.BOOLEAN)
                .param("siPuedeBloquearCierre", medida.isSiPuedeBloquearCierre(), Types.BOOLEAN)
                .param("tipoBloqueanteDefault", medida.getTipoBloqueanteDefault() == null ? null : medida.getTipoBloqueanteDefault().codigo(), Types.SMALLINT)
                .param("fhAlta", medida.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", medida.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_medida_preventiva no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public Optional<FalMedidaPreventiva> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcMedidaPreventivaRepository::mapear).optional();
    }

    @Override
    public Optional<FalMedidaPreventiva> findByCodigoAndVersion(String codigo, short version) {
        if (codigo == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE codigo = :codigo AND version_medida = :version")
                .param("codigo", codigo.trim().toUpperCase(), Types.VARCHAR)
                .param("version", version, Types.SMALLINT)
                .query(JdbcMedidaPreventivaRepository::mapear).optional();
    }

    @Override
    public Optional<FalMedidaPreventiva> findActivaByCodigo(String codigo) {
        if (codigo == null) return Optional.empty();
        List<FalMedidaPreventiva> activas = jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE codigo = :codigo AND si_activa = TRUE")
                .param("codigo", codigo.trim().toUpperCase(), Types.VARCHAR)
                .query(JdbcMedidaPreventivaRepository::mapear).list();
        if (activas.size() > 1) {
            throw new IllegalStateException("Invariante rota: " + activas.size() + " versiones activas para codigo=" + codigo);
        }
        return activas.isEmpty() ? Optional.empty() : Optional.of(activas.get(0));
    }

    @Override
    public List<FalMedidaPreventiva> findVersionesByCodigo(String codigo) {
        if (codigo == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE codigo = :codigo ORDER BY version_medida")
                .param("codigo", codigo.trim().toUpperCase(), Types.VARCHAR)
                .query(JdbcMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public List<FalMedidaPreventiva> findActivas() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE si_activa = TRUE ORDER BY id")
                .query(JdbcMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public List<FalMedidaPreventiva> findActivasParaDependencia(Long idDep, Short verDep) {
        if (idDep == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_medida_preventiva WHERE si_activa = TRUE AND id_dep = :idDep AND ver_dep = :verDep")
                .param("idDep", idDep, Types.BIGINT).param("verDep", verDep, Types.SMALLINT)
                .query(JdbcMedidaPreventivaRepository::mapear).list();
    }

    /**
     * Atómico dentro de la transacción del llamador (sin {@code @Transactional} propio: la
     * frontera transaccional es del caso de uso, igual que el resto de los adaptadores JDBC del
     * alta). Desactiva la versión activa anterior e inserta la nueva en el mismo contexto.
     */
    @Override
    public FalMedidaPreventiva crearNuevaVersionAtomico(FalMedidaPreventiva nuevaVersion) {
        List<FalMedidaPreventiva> versiones = findVersionesByCodigo(nuevaVersion.getCodigo());
        if (versiones.isEmpty()) {
            throw new PrecondicionVioladaException(
                    "No existe medida con codigo=" + nuevaVersion.getCodigo() + ". Usar save (primera version).");
        }
        boolean yaExiste = versiones.stream().anyMatch(m -> m.getVersionMedida() == nuevaVersion.getVersionMedida());
        if (yaExiste) {
            throw new PrecondicionVioladaException(
                    "Ya existe version=" + nuevaVersion.getVersionMedida() + " para codigo=" + nuevaVersion.getCodigo());
        }
        jdbc.sql("UPDATE fal_medida_preventiva SET si_activa = FALSE WHERE codigo = :codigo AND si_activa = TRUE")
                .param("codigo", nuevaVersion.getCodigo(), Types.VARCHAR)
                .update();
        return save(nuevaVersion);
    }

    private static FalMedidaPreventiva mapear(ResultSet rs, int fila) throws SQLException {
        FalMedidaPreventiva m = new FalMedidaPreventiva(
                rs.getObject("id", Long.class), rs.getString("codigo"), rs.getShort("version_medida"),
                rs.getString("descripcion"), rs.getObject("fh_alta", LocalDateTime.class), rs.getString("id_user_alta"));
        m.setDescripcionDetalle(rs.getString("descripcion_detalle"));
        Long idDep = rs.getObject("id_dep", Long.class);
        Short verDep = (Short) rs.getObject("ver_dep");
        if (idDep != null) m.setDependencia(idDep, verDep);
        m.setSiActiva(rs.getBoolean("si_activa"));
        m.setSiPuedeBloquearCierre(rs.getBoolean("si_puede_bloquear_cierre"));
        Short tipoBloq = (Short) rs.getObject("tipo_bloqueante_default");
        if (tipoBloq != null) m.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.fromCodigo(tipoBloq));
        return m;
    }
}
