package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.repository.VehiculoMarcaRepository;
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
 * Adaptador JDBC de {@link VehiculoMarcaRepository} sobre {@code fal_vehiculo_marca} (catálogo).
 * Creado en Wave 4. Sin drift: 6 columnas físicas mapean 1:1 con {@link FalVehiculoMarca}.
 */
@Repository
@Profile("jdbc")
public class JdbcVehiculoMarcaRepository implements VehiculoMarcaRepository {

    private static final String COLUMNAS = "id, codigo, nombre, si_activo, fh_alta, id_user_alta";
    private final java.util.concurrent.atomic.AtomicLong provisional = new java.util.concurrent.atomic.AtomicLong(1);
    private final JdbcClient jdbc;

    public JdbcVehiculoMarcaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalVehiculoMarca guardar(FalVehiculoMarca marca) {
        if (marca == null) throw new IllegalArgumentException("marca es obligatoria");
        Optional<FalVehiculoMarca> existente = findByCodigo(marca.getCodigo());
        if (existente.isPresent()) {
            jdbc.sql("UPDATE fal_vehiculo_marca SET nombre = :nombre, si_activo = :siActivo WHERE id = :id")
                    .param("nombre", marca.getNombre(), Types.VARCHAR)
                    .param("siActivo", marca.isSiActivo(), Types.BOOLEAN)
                    .param("id", existente.get().getId(), Types.BIGINT)
                    .update();
            return findById(existente.get().getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_vehiculo_marca (codigo, nombre, si_activo, fh_alta, id_user_alta)
                VALUES (:codigo, :nombre, :siActivo, :fhAlta, :idUserAlta)
                """)
                .param("codigo", marca.getCodigo(), Types.VARCHAR)
                .param("nombre", marca.getNombre(), Types.VARCHAR)
                .param("siActivo", marca.isSiActivo(), Types.BOOLEAN)
                .param("fhAlta", marca.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", marca.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_vehiculo_marca no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public Optional<FalVehiculoMarca> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_marca WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcVehiculoMarcaRepository::mapear).optional();
    }

    @Override
    public Optional<FalVehiculoMarca> findByCodigo(String codigo) {
        if (codigo == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_marca WHERE codigo = :codigo")
                .param("codigo", codigo.trim().toUpperCase(), Types.VARCHAR)
                .query(JdbcVehiculoMarcaRepository::mapear).optional();
    }

    @Override
    public Optional<FalVehiculoMarca> findByNombre(String nombre) {
        if (nombre == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_marca WHERE nombre = :nombre")
                .param("nombre", nombre.trim(), Types.VARCHAR)
                .query(JdbcVehiculoMarcaRepository::mapear).optional();
    }

    @Override
    public List<FalVehiculoMarca> findAllActivas() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_marca WHERE si_activo = TRUE ORDER BY id")
                .query(JdbcVehiculoMarcaRepository::mapear).list();
    }

    @Override
    public List<FalVehiculoMarca> findAll() {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_vehiculo_marca ORDER BY id")
                .query(JdbcVehiculoMarcaRepository::mapear).list();
    }

    private static FalVehiculoMarca mapear(ResultSet rs, int fila) throws SQLException {
        FalVehiculoMarca m = new FalVehiculoMarca(
                rs.getObject("id", Long.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getObject("fh_alta", LocalDateTime.class), rs.getString("id_user_alta"));
        m.setSiActivo(rs.getBoolean("si_activo"));
        return m;
    }
}
