package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;
import ar.gob.malvinas.faltas.core.repository.ActaVehiculoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ActaVehiculoRepository} sobre {@code fal_acta_vehiculo} (1:1 con
 * {@code fal_acta}, PK = {@code acta_id}). Creado en Wave 4. Sin drift: 12 columnas físicas
 * mapean 1:1 con {@link FalActaVehiculo}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaVehiculoRepository implements ActaVehiculoRepository {

    private static final String COLUMNAS = """
            acta_id, dominio_vehiculo, tipo_vehiculo, marca_vehiculo_id, marca_vehiculo_txt,
            modelo_vehiculo_id, modelo_vehiculo_txt, anio_vehiculo, color_vehiculo,
            estado_general_vehiculo, fh_alta, id_user_alta
            """;

    private static final String SQL_UPSERT = """
            INSERT INTO fal_acta_vehiculo (
                acta_id, dominio_vehiculo, tipo_vehiculo, marca_vehiculo_id, marca_vehiculo_txt,
                modelo_vehiculo_id, modelo_vehiculo_txt, anio_vehiculo, color_vehiculo,
                estado_general_vehiculo, fh_alta, id_user_alta
            ) VALUES (
                :actaId, :dominioVehiculo, :tipoVehiculo, :marcaVehiculoId, :marcaVehiculoTxt,
                :modeloVehiculoId, :modeloVehiculoTxt, :anioVehiculo, :colorVehiculo,
                :estadoGeneralVehiculo, :fhAlta, :idUserAlta
            ) ON DUPLICATE KEY UPDATE
                dominio_vehiculo = :dominioVehiculo, tipo_vehiculo = :tipoVehiculo,
                marca_vehiculo_id = :marcaVehiculoId, marca_vehiculo_txt = :marcaVehiculoTxt,
                modelo_vehiculo_id = :modeloVehiculoId, modelo_vehiculo_txt = :modeloVehiculoTxt,
                anio_vehiculo = :anioVehiculo, color_vehiculo = :colorVehiculo,
                estado_general_vehiculo = :estadoGeneralVehiculo
            """;

    private final JdbcClient jdbc;

    public JdbcActaVehiculoRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public FalActaVehiculo guardar(FalActaVehiculo vehiculo) {
        if (vehiculo == null) throw new IllegalArgumentException("vehiculo es obligatorio");
        jdbc.sql(SQL_UPSERT)
                .param("actaId", vehiculo.getActaId(), Types.BIGINT)
                .param("dominioVehiculo", vehiculo.getDominioVehiculo(), Types.VARCHAR)
                .param("tipoVehiculo", vehiculo.getTipoVehiculo() == null ? null : vehiculo.getTipoVehiculo().codigo(), Types.SMALLINT)
                .param("marcaVehiculoId", vehiculo.getMarcaVehiculoId(), Types.BIGINT)
                .param("marcaVehiculoTxt", vehiculo.getMarcaVehiculoTxt(), Types.VARCHAR)
                .param("modeloVehiculoId", vehiculo.getModeloVehiculoId(), Types.BIGINT)
                .param("modeloVehiculoTxt", vehiculo.getModeloVehiculoTxt(), Types.VARCHAR)
                .param("anioVehiculo", vehiculo.getAnioVehiculo(), Types.SMALLINT)
                .param("colorVehiculo", vehiculo.getColorVehiculo(), Types.VARCHAR)
                .param("estadoGeneralVehiculo", vehiculo.getEstadoGeneralVehiculo() == null ? null : vehiculo.getEstadoGeneralVehiculo().codigo(), Types.SMALLINT)
                .param("fhAlta", vehiculo.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", vehiculo.getIdUserAlta(), Types.CHAR)
                .update();
        return findByActaId(vehiculo.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaVehiculo> findByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_vehiculo WHERE acta_id = :actaId")
                .param("actaId", actaId, Types.BIGINT)
                .query(JdbcActaVehiculoRepository::mapear)
                .optional();
    }

    @Override
    public boolean existsByActaId(Long actaId) {
        return findByActaId(actaId).isPresent();
    }

    private static FalActaVehiculo mapear(ResultSet rs, int fila) throws SQLException {
        FalActaVehiculo v = new FalActaVehiculo(
                rs.getObject("acta_id", Long.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        v.setDominioVehiculo(rs.getString("dominio_vehiculo"));
        Short tipo = (Short) rs.getObject("tipo_vehiculo");
        if (tipo != null) v.setTipoVehiculo(TipoVehiculo.fromCodigo(tipo));
        v.setMarca(rs.getObject("marca_vehiculo_id", Long.class), rs.getString("marca_vehiculo_txt"));
        v.setModelo(rs.getObject("modelo_vehiculo_id", Long.class), rs.getString("modelo_vehiculo_txt"));
        v.setAnioVehiculo((Short) rs.getObject("anio_vehiculo"));
        v.setColorVehiculo(rs.getString("color_vehiculo"));
        Short estado = (Short) rs.getObject("estado_general_vehiculo");
        if (estado != null) v.setEstadoGeneralVehiculo(EstadoGeneralVehiculo.fromCodigo(estado));
        return v;
    }
}
