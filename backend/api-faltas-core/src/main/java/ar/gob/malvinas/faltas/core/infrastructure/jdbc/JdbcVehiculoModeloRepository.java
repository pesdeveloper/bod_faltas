package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.repository.VehiculoModeloRepository;
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
 * Adaptador JDBC de {@link VehiculoModeloRepository} sobre {@code fal_vehiculo_modelo}
 * (catálogo). Creado en Wave 4. Sin drift: 7 columnas físicas mapean 1:1 con
 * {@link FalVehiculoModelo}.
 */
@Repository
@Profile("jdbc")
public class JdbcVehiculoModeloRepository implements VehiculoModeloRepository {

    private static final String COLUMNAS = "id, marca_vehiculo_id, codigo, nombre, si_activo, fh_alta, id_user_alta";
    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcVehiculoModeloRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalVehiculoModelo guardar(FalVehiculoModelo modelo) {
        if (modelo == null) throw new IllegalArgumentException("modelo es obligatorio");
        Optional<FalVehiculoModelo> existente = findByMarcaAndCodigo(modelo.getMarcaVehiculoId(), modelo.getCodigo());
        if (existente.isPresent()) {
            jdbc.sql("UPDATE fal_vehiculo_modelo SET nombre = :nombre, si_activo = :siActivo WHERE id = :id")
                    .param("nombre", modelo.getNombre(), Types.VARCHAR)
                    .param("siActivo", modelo.isSiActivo(), Types.BOOLEAN)
                    .param("id", existente.get().getId(), Types.BIGINT)
                    .update();
            return findById(existente.get().getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_vehiculo_modelo (marca_vehiculo_id, codigo, nombre, si_activo, fh_alta, id_user_alta)
                VALUES (:marcaVehiculoId, :codigo, :nombre, :siActivo, :fhAlta, :idUserAlta)
                """)
                .param("marcaVehiculoId", modelo.getMarcaVehiculoId(), Types.BIGINT)
                .param("codigo", modelo.getCodigo(), Types.VARCHAR)
                .param("nombre", modelo.getNombre(), Types.VARCHAR)
                .param("siActivo", modelo.isSiActivo(), Types.BOOLEAN)
                .param("fhAlta", modelo.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", modelo.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_vehiculo_modelo no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public Optional<FalVehiculoModelo> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_modelo WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcVehiculoModeloRepository::mapear).optional();
    }

    @Override
    public Optional<FalVehiculoModelo> findByMarcaAndCodigo(Long marcaId, String codigo) {
        if (marcaId == null || codigo == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_modelo WHERE marca_vehiculo_id = :marcaId AND codigo = :codigo")
                .param("marcaId", marcaId, Types.BIGINT)
                .param("codigo", codigo.trim().toUpperCase(), Types.VARCHAR)
                .query(JdbcVehiculoModeloRepository::mapear).optional();
    }

    @Override
    public Optional<FalVehiculoModelo> findByMarcaAndNombre(Long marcaId, String nombre) {
        if (marcaId == null || nombre == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_modelo WHERE marca_vehiculo_id = :marcaId AND nombre = :nombre")
                .param("marcaId", marcaId, Types.BIGINT)
                .param("nombre", nombre.trim(), Types.VARCHAR)
                .query(JdbcVehiculoModeloRepository::mapear).optional();
    }

    @Override
    public List<FalVehiculoModelo> findActivasByMarca(Long marcaId) {
        if (marcaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_modelo WHERE marca_vehiculo_id = :marcaId AND si_activo = TRUE ORDER BY id")
                .param("marcaId", marcaId, Types.BIGINT).query(JdbcVehiculoModeloRepository::mapear).list();
    }

    @Override
    public List<FalVehiculoModelo> findByMarca(Long marcaId) {
        if (marcaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_modelo WHERE marca_vehiculo_id = :marcaId ORDER BY id")
                .param("marcaId", marcaId, Types.BIGINT).query(JdbcVehiculoModeloRepository::mapear).list();
    }

    private static FalVehiculoModelo mapear(ResultSet rs, int fila) throws SQLException {
        FalVehiculoModelo m = new FalVehiculoModelo(
                rs.getObject("id", Long.class), rs.getObject("marca_vehiculo_id", Long.class),
                rs.getString("codigo"), rs.getString("nombre"),
                rs.getObject("fh_alta", LocalDateTime.class), rs.getString("id_user_alta"));
        m.setSiActivo(rs.getBoolean("si_activo"));
        return m;
    }
}
