package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.TarifarioUnidadFaltasRepository;
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
 * Adaptador JDBC de {@link TarifarioUnidadFaltasRepository} sobre
 * {@code fal_tarifario_unidad_faltas} (formula por articulo, Wave 4 PARTE-2 S7.3).
 */
@Repository
@Profile("jdbc")
public class JdbcTarifarioUnidadFaltasRepository implements TarifarioUnidadFaltasRepository {

    private static final String SQL_SELECT = """
            SELECT id, id_articulo, tipo_unidad, cantidad_unidades_ordinaria, cantidad_unidades_pago_voluntario,
                   monto_fijo_ordinario, monto_fijo_pago_voluntario, si_admite_pago_voluntario,
                   fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta
            FROM fal_tarifario_unidad_faltas
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcTarifarioUnidadFaltasRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() { return provisional.getAndIncrement(); }

    @Override
    public FalTarifarioUnidadFaltas save(FalTarifarioUnidadFaltas t) {
        if (t.getId() != null && existePorId(t.getId())) {
            jdbc.sql("""
                    UPDATE fal_tarifario_unidad_faltas
                    SET si_activa = :siActiva, fh_vig_hasta = :fhVigHasta
                    WHERE id = :id
                    """)
                    .param("siActiva", t.isSiActiva(), Types.BOOLEAN)
                    .param("fhVigHasta", t.getFhVigHasta())
                    .param("id", t.getId(), Types.BIGINT)
                    .update();
            return findById(t.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_tarifario_unidad_faltas (
                    id_articulo, tipo_unidad, cantidad_unidades_ordinaria, cantidad_unidades_pago_voluntario,
                    monto_fijo_ordinario, monto_fijo_pago_voluntario, si_admite_pago_voluntario,
                    fh_vig_desde, fh_vig_hasta, si_activa, fh_alta, id_user_alta
                ) VALUES (
                    :idArticulo, :tipoUnidad, :cantOrdinaria, :cantPagoVol,
                    :montoOrdinario, :montoPagoVol, :siAdmitePagoVol,
                    :fhVigDesde, :fhVigHasta, :siActiva, :fhAlta, :idUserAlta
                )
                """)
                .param("idArticulo", t.getArticuloId(), Types.BIGINT)
                .param("tipoUnidad", t.getTipoUnidad().codigo(), Types.SMALLINT)
                .param("cantOrdinaria", t.getCantidadUnidadesOrdinaria(), Types.DECIMAL)
                .param("cantPagoVol", t.getCantidadUnidadesPagoVoluntario(), Types.DECIMAL)
                .param("montoOrdinario", t.getMontoFijoOrdinario(), Types.DECIMAL)
                .param("montoPagoVol", t.getMontoFijoPagoVoluntario(), Types.DECIMAL)
                .param("siAdmitePagoVol", t.isSiAdmitePagoVoluntario(), Types.BOOLEAN)
                .param("fhVigDesde", t.getFhVigDesde())
                .param("fhVigHasta", t.getFhVigHasta())
                .param("siActiva", t.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", t.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", t.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_tarifario_unidad_faltas no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    private boolean existePorId(Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM fal_tarifario_unidad_faltas WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(Integer.class).single() > 0;
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcTarifarioUnidadFaltasRepository::mapear)
                .optional();
    }

    @Override
    public List<FalTarifarioUnidadFaltas> findAllByArticulo(Long articuloId) {
        return jdbc.sql(SQL_SELECT + " WHERE id_articulo = :idArticulo ORDER BY fh_vig_desde")
                .param("idArticulo", articuloId, Types.BIGINT)
                .query(JdbcTarifarioUnidadFaltasRepository::mapear).list();
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findActivaByArticulo(Long articuloId) {
        return jdbc.sql(SQL_SELECT + " WHERE id_articulo = :idArticulo AND si_activa = TRUE")
                .param("idArticulo", articuloId, Types.BIGINT)
                .query(JdbcTarifarioUnidadFaltasRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalTarifarioUnidadFaltas> findVigenteByArticulo(Long articuloId, LocalDate fecha) {
        return jdbc.sql(SQL_SELECT + """
                 WHERE id_articulo = :idArticulo AND fh_vig_desde <= :fecha
                   AND (fh_vig_hasta IS NULL OR fh_vig_hasta > :fecha)
                 ORDER BY fh_vig_desde DESC LIMIT 1
                """)
                .param("idArticulo", articuloId, Types.BIGINT)
                .param("fecha", fecha)
                .query(JdbcTarifarioUnidadFaltasRepository::mapear)
                .optional();
    }

    private static FalTarifarioUnidadFaltas mapear(ResultSet rs, int fila) throws SQLException {
        TipoUnidadFaltas tipoUnidad = TipoUnidadFaltas.fromCodigo(rs.getShort("tipo_unidad"));
        FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                rs.getObject("id", Long.class),
                rs.getObject("id_articulo", Long.class),
                tipoUnidad,
                rs.getObject("cantidad_unidades_ordinaria", BigDecimal.class),
                rs.getObject("monto_fijo_ordinario", BigDecimal.class),
                rs.getBoolean("si_admite_pago_voluntario"),
                rs.getObject("cantidad_unidades_pago_voluntario", BigDecimal.class),
                rs.getObject("monto_fijo_pago_voluntario", BigDecimal.class),
                rs.getObject("fh_vig_desde", LocalDate.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        LocalDate fhVigHasta = rs.getObject("fh_vig_hasta", LocalDate.class);
        if (fhVigHasta != null) t.setFhVigHasta(fhVigHasta);
        t.setSiActiva(rs.getBoolean("si_activa"));
        return t;
    }
}
