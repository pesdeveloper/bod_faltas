package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.ValorUnidadFaltasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador JDBC de {@link ValorUnidadFaltasRepository} sobre
 * {@code fal_valor_unidad_faltas} (nueva tabla, Wave 4 PARTE-2 S7.2).
 */
@Repository
@Profile("jdbc")
public class JdbcValorUnidadFaltasRepository implements ValorUnidadFaltasRepository {

    private static final String SQL_SELECT = """
            SELECT id, tipo_unidad, valor_unidad, fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta
            FROM fal_valor_unidad_faltas
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcValorUnidadFaltasRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() { return provisional.getAndIncrement(); }

    @Override
    public FalValorUnidadFaltas save(FalValorUnidadFaltas v) {
        if (v.getId() != null && existePorId(v.getId())) {
            jdbc.sql("""
                    UPDATE fal_valor_unidad_faltas
                    SET si_activa = :siActiva, fh_vig_hasta = :fhVigHasta
                    WHERE id = :id
                    """)
                    .param("siActiva", v.isSiActiva(), Types.BOOLEAN)
                    .param("fhVigHasta", v.getFhVigHasta())
                    .param("id", v.getId(), Types.BIGINT)
                    .update();
            return findById(v.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_valor_unidad_faltas (
                    tipo_unidad, valor_unidad, fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta
                ) VALUES (
                    :tipoUnidad, :valorUnidad, :fhVigDesde, :fhVigHasta, :siActiva, :fhAlta, :idUserAlta
                )
                """)
                .param("tipoUnidad", v.getTipoUnidad().codigo(), Types.SMALLINT)
                .param("valorUnidad", v.getValorUnidad(), Types.DECIMAL)
                .param("fhVigDesde", v.getFhVigDesde())
                .param("fhVigHasta", v.getFhVigHasta())
                .param("siActiva", v.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", v.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", v.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_valor_unidad_faltas no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    private boolean existePorId(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_valor_unidad_faltas WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(Integer.class).single() > 0;
    }

    @Override
    public Optional<FalValorUnidadFaltas> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcValorUnidadFaltasRepository::mapear)
                .optional();
    }

    @Override
    public List<FalValorUnidadFaltas> findAllByTipo(TipoUnidadFaltas tipo) {
        return jdbc.sql(SQL_SELECT + " WHERE tipo_unidad = :tipo ORDER BY fh_vig_desde")
                .param("tipo", tipo.codigo(), Types.SMALLINT)
                .query(JdbcValorUnidadFaltasRepository::mapear).list();
    }

    @Override
    public Optional<FalValorUnidadFaltas> findActivoByTipo(TipoUnidadFaltas tipo) {
        return jdbc.sql(SQL_SELECT + " WHERE tipo_unidad = :tipo AND si_activa = TRUE")
                .param("tipo", tipo.codigo(), Types.SMALLINT)
                .query(JdbcValorUnidadFaltasRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalValorUnidadFaltas> findVigenteByTipo(TipoUnidadFaltas tipo, LocalDate fecha) {
        return jdbc.sql(SQL_SELECT + """
                 WHERE tipo_unidad = :tipo AND fh_vig_desde <= :fecha
                   AND (fh_vig_hasta IS NULL OR fh_vig_hasta > :fecha)
                 ORDER BY fh_vig_desde DESC LIMIT 1
                """)
                .param("tipo", tipo.codigo(), Types.SMALLINT)
                .param("fecha", fecha)
                .query(JdbcValorUnidadFaltasRepository::mapear)
                .optional();
    }

    private static FalValorUnidadFaltas mapear(ResultSet rs, int fila) throws SQLException {
        FalValorUnidadFaltas v = new FalValorUnidadFaltas(
                rs.getObject("id", Long.class),
                TipoUnidadFaltas.fromCodigo(rs.getShort("tipo_unidad")),
                rs.getObject("valor_unidad", BigDecimal.class),
                rs.getObject("fh_vig_desde", LocalDate.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        LocalDate fhVigHasta = rs.getObject("fh_vig_hasta", LocalDate.class);
        if (fhVigHasta != null) v.setFhVigHasta(fhVigHasta);
        v.setSiActiva(rs.getBoolean("si_activa"));
        return v;
    }
}
