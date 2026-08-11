package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ActaTransitoRepository} sobre {@code fal_acta_transito} (1:1 con
 * {@code fal_acta}, PK = {@code acta_id}, sin columnas de auditoría). Creado en Wave 4. Sin
 * drift: 8 columnas físicas mapean 1:1 con {@link FalActaTransito}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaTransitoRepository implements ActaTransitoRepository {

    private static final String COLUMNAS = """
            acta_id, nro_licencia, id_prov_lic, unidad_territorial_lic_tipo,
            id_unidad_territorial_lic, si_ret_licencia, si_ret_vehiculo, si_control_alcoholemia
            """;

    private static final String SQL_UPSERT = """
            INSERT INTO fal_acta_transito (
                acta_id, nro_licencia, id_prov_lic, unidad_territorial_lic_tipo,
                id_unidad_territorial_lic, si_ret_licencia, si_ret_vehiculo, si_control_alcoholemia
            ) VALUES (
                :actaId, :nroLicencia, :idProvLic, :unidadTerritorialLicTipo,
                :idUnidadTerritorialLic, :siRetLicencia, :siRetVehiculo, :siControlAlcoholemia
            ) ON DUPLICATE KEY UPDATE
                nro_licencia = :nroLicencia, id_prov_lic = :idProvLic,
                unidad_territorial_lic_tipo = :unidadTerritorialLicTipo,
                id_unidad_territorial_lic = :idUnidadTerritorialLic,
                si_ret_licencia = :siRetLicencia, si_ret_vehiculo = :siRetVehiculo,
                si_control_alcoholemia = :siControlAlcoholemia
            """;

    private final JdbcClient jdbc;

    public JdbcActaTransitoRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public FalActaTransito guardar(FalActaTransito transito) {
        if (transito == null) throw new IllegalArgumentException("transito es obligatorio");
        jdbc.sql(SQL_UPSERT)
                .param("actaId", transito.getActaId(), Types.BIGINT)
                .param("nroLicencia", transito.getNroLicencia(), Types.VARCHAR)
                .param("idProvLic", transito.getIdProvLic(), Types.SMALLINT)
                .param("unidadTerritorialLicTipo",
                        transito.getUnidadTerritorialLicTipo() == null ? null : transito.getUnidadTerritorialLicTipo().codigo(),
                        Types.SMALLINT)
                .param("idUnidadTerritorialLic", transito.getIdUnidadTerritorialLic(), Types.INTEGER)
                .param("siRetLicencia", transito.isSiRetLicencia(), Types.BOOLEAN)
                .param("siRetVehiculo", transito.isSiRetVehiculo(), Types.BOOLEAN)
                .param("siControlAlcoholemia", transito.isSiControlAlcoholemia(), Types.BOOLEAN)
                .update();
        return findByActaId(transito.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaTransito> findByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_transito WHERE acta_id = :actaId")
                .param("actaId", actaId, Types.BIGINT)
                .query(JdbcActaTransitoRepository::mapear)
                .optional();
    }

    @Override
    public boolean existsByActaId(Long actaId) {
        return findByActaId(actaId).isPresent();
    }

    private static FalActaTransito mapear(ResultSet rs, int fila) throws SQLException {
        FalActaTransito t = new FalActaTransito(rs.getObject("acta_id", Long.class));
        t.setNroLicencia(rs.getString("nro_licencia"));
        Short idProvLic = (Short) rs.getObject("id_prov_lic");
        Short tipoCodigo = (Short) rs.getObject("unidad_territorial_lic_tipo");
        Integer idUnidad = (Integer) rs.getObject("id_unidad_territorial_lic");
        if (idProvLic != null || idUnidad != null) {
            t.setLicenciaTerritorio(idProvLic, tipoCodigo == null ? null : UnidadTerritorialTipo.fromCodigo(tipoCodigo), idUnidad);
        }
        t.setSiRetLicencia(rs.getBoolean("si_ret_licencia"));
        t.setSiRetVehiculo(rs.getBoolean("si_ret_vehiculo"));
        t.setSiControlAlcoholemia(rs.getBoolean("si_control_alcoholemia"));
        return t;
    }
}
