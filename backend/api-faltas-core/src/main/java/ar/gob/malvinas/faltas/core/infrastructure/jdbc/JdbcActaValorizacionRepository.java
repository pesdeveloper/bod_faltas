package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador JDBC de {@link ActaValorizacionRepository} sobre la cabecera unica
 * {@code fal_acta_valorizacion} (Wave 4 PARTE-2 S7.8).
 */
@Repository
@Profile("jdbc")
public class JdbcActaValorizacionRepository implements ActaValorizacionRepository {

    private static final String SQL_SELECT = """
            SELECT id, id_acta, monto_total_ordinario, monto_total_pago_voluntario, estado_valorizacion,
                   si_confirmada, fh_calculo, fh_confirmacion, id_user_confirmacion, version_row,
                   fh_alta, id_user_alta
            FROM fal_acta_valorizacion
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcActaValorizacionRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() { return provisional.getAndIncrement(); }

    @Override
    public FalActaValorizacion save(FalActaValorizacion v) {
        Optional<FalActaValorizacion> existente = findByActaId(v.getActaId());
        if (existente.isPresent()) {
            FalActaValorizacion actual = existente.get();
            int filasActualizadas = jdbc.sql("""
                    UPDATE fal_acta_valorizacion
                    SET monto_total_ordinario = :montoOrdinario,
                        monto_total_pago_voluntario = :montoPagoVol,
                        estado_valorizacion = :estado,
                        si_confirmada = :siConfirmada,
                        fh_calculo = :fhCalculo,
                        fh_confirmacion = :fhConfirmacion,
                        id_user_confirmacion = :idUserConfirmacion,
                        version_row = version_row + 1
                    WHERE id = :id AND version_row = :versionRow
                    """)
                    .param("montoOrdinario", v.getMontoTotalOrdinario(), Types.DECIMAL)
                    .param("montoPagoVol", v.getMontoTotalPagoVoluntario(), Types.DECIMAL)
                    .param("estado", v.getEstadoValorizacion().codigo(), Types.SMALLINT)
                    .param("siConfirmada", v.isSiConfirmada(), Types.BOOLEAN)
                    .param("fhCalculo", v.getFhCalculo(), Types.TIMESTAMP)
                    .param("fhConfirmacion", v.getFhConfirmacion(), Types.TIMESTAMP)
                    .param("idUserConfirmacion", v.getIdUserConfirmacion(), Types.CHAR)
                    .param("id", actual.getId(), Types.BIGINT)
                    .param("versionRow", v.getVersionRow(), Types.INTEGER)
                    .update();
            if (filasActualizadas == 0) {
                throw new ConcurrenciaConflictoException("FalActaValorizacion", actual.getId(),
                        actual.getVersionRow(), v.getVersionRow());
            }
            return findById(actual.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_acta_valorizacion (
                    id_acta, monto_total_ordinario, monto_total_pago_voluntario, estado_valorizacion,
                    si_confirmada, fh_calculo, fh_alta, id_user_alta
                ) VALUES (
                    :idActa, :montoOrdinario, :montoPagoVol, :estado, :siConfirmada, :fhCalculo, :fhAlta, :idUserAlta
                )
                """)
                .param("idActa", v.getActaId(), Types.BIGINT)
                .param("montoOrdinario", v.getMontoTotalOrdinario(), Types.DECIMAL)
                .param("montoPagoVol", v.getMontoTotalPagoVoluntario(), Types.DECIMAL)
                .param("estado", v.getEstadoValorizacion().codigo(), Types.SMALLINT)
                .param("siConfirmada", v.isSiConfirmada(), Types.BOOLEAN)
                .param("fhCalculo", v.getFhCalculo(), Types.TIMESTAMP)
                .param("fhAlta", v.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", v.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_acta_valorizacion no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public Optional<FalActaValorizacion> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcActaValorizacionRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalActaValorizacion> findByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id_acta = :idActa")
                .param("idActa", actaId, Types.BIGINT)
                .query(JdbcActaValorizacionRepository::mapear)
                .optional();
    }

    private static FalActaValorizacion mapear(ResultSet rs, int fila) throws SQLException {
        FalActaValorizacion v = new FalActaValorizacion(
                rs.getObject("id", Long.class),
                rs.getObject("id_acta", Long.class),
                rs.getObject("monto_total_ordinario", BigDecimal.class),
                rs.getObject("monto_total_pago_voluntario", BigDecimal.class),
                rs.getObject("fh_calculo", LocalDateTime.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        v.setVersionRow(rs.getInt("version_row"));
        EstadoValorizacionActa estado = EstadoValorizacionActa.fromCodigo(rs.getShort("estado_valorizacion"));
        if (estado == EstadoValorizacionActa.CONFIRMADA) {
            v.confirmar(rs.getObject("fh_confirmacion", LocalDateTime.class), rs.getString("id_user_confirmacion"));
        }
        return v;
    }
}
