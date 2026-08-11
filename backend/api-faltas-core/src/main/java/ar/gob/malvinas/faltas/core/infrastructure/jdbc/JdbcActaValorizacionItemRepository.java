package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionItemRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador JDBC de {@link ActaValorizacionItemRepository} sobre
 * {@code fal_acta_valorizacion_item} (historial versionado, Wave 4 PARTE-2 S7.5).
 *
 * Los items son inmutables: {@code save} siempre hace {@code INSERT} estricto
 * (nunca {@code ON DUPLICATE KEY UPDATE}), consistente con la regla del prompt
 * de no ocultar duplicados en el alta.
 */
@Repository
@Profile("jdbc")
public class JdbcActaValorizacionItemRepository implements ActaValorizacionItemRepository {

    private static final String SQL_SELECT = """
            SELECT id, id_acta_articulo, version_item, si_actual, id_tarifario_articulo_usado,
                   id_valor_unidad_usado, modo_valorizacion_ordinaria, cantidad_manual_ordinaria,
                   monto_manual_ordinario, modo_valorizacion_pago_voluntario, cantidad_manual_pago_voluntario,
                   monto_manual_pago_voluntario, motivo_cambio, observacion_cambio, fh_alta, id_user_alta
            FROM fal_acta_valorizacion_item
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcActaValorizacionItemRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() { return provisional.getAndIncrement(); }

    @Override
    public FalActaValorizacionItem save(FalActaValorizacionItem item) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_acta_valorizacion_item (
                    id_acta_articulo, version_item, si_actual, id_tarifario_articulo_usado, id_valor_unidad_usado,
                    modo_valorizacion_ordinaria, cantidad_manual_ordinaria, monto_manual_ordinario,
                    modo_valorizacion_pago_voluntario, cantidad_manual_pago_voluntario, monto_manual_pago_voluntario,
                    motivo_cambio, observacion_cambio, fh_alta, id_user_alta
                ) VALUES (
                    :idActaArticulo, :versionItem, :siActual, :idTarifario, :idValorUnidad,
                    :modoOrdinaria, :cantManualOrdinaria, :montoManualOrdinario,
                    :modoPagoVol, :cantManualPagoVol, :montoManualPagoVol,
                    :motivoCambio, :observacionCambio, :fhAlta, :idUserAlta
                )
                """)
                .param("idActaArticulo", item.getActaArticuloId(), Types.BIGINT)
                .param("versionItem", item.getVersionItem(), Types.INTEGER)
                .param("siActual", item.isSiActual(), Types.BOOLEAN)
                .param("idTarifario", item.getTarifarioArticuloUsadoId(), Types.BIGINT)
                .param("idValorUnidad", item.getValorUnidadUsadoId(), Types.BIGINT)
                .param("modoOrdinaria", item.getModoOrdinaria().codigo(), Types.SMALLINT)
                .param("cantManualOrdinaria", item.getCantidadManualOrdinaria(), Types.DECIMAL)
                .param("montoManualOrdinario", item.getMontoManualOrdinario(), Types.DECIMAL)
                .param("modoPagoVol", item.getModoPagoVoluntario() == null ? null : item.getModoPagoVoluntario().codigo(), Types.SMALLINT)
                .param("cantManualPagoVol", item.getCantidadManualPagoVoluntario(), Types.DECIMAL)
                .param("montoManualPagoVol", item.getMontoManualPagoVoluntario(), Types.DECIMAL)
                .param("motivoCambio", item.getMotivoCambio())
                .param("observacionCambio", item.getObservacionCambio())
                .param("fhAlta", item.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", item.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_acta_valorizacion_item no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public void marcarNoActual(Long id) {
        if (id == null) return;
        jdbc.sql("UPDATE fal_acta_valorizacion_item SET si_actual = FALSE WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .update();
    }

    @Override
    public Optional<FalActaValorizacionItem> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id = :id")
                .param("id", id, Types.BIGINT)
                .query(JdbcActaValorizacionItemRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalActaValorizacionItem> findActualByActaArticulo(Long actaArticuloId) {
        if (actaArticuloId == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT + " WHERE id_acta_articulo = :idActaArticulo AND si_actual = TRUE")
                .param("idActaArticulo", actaArticuloId, Types.BIGINT)
                .query(JdbcActaValorizacionItemRepository::mapear)
                .optional();
    }

    @Override
    public List<FalActaValorizacionItem> findAllByActaArticulo(Long actaArticuloId) {
        if (actaArticuloId == null) return List.of();
        return jdbc.sql(SQL_SELECT + " WHERE id_acta_articulo = :idActaArticulo ORDER BY version_item")
                .param("idActaArticulo", actaArticuloId, Types.BIGINT)
                .query(JdbcActaValorizacionItemRepository::mapear).list();
    }

    @Override
    public List<FalActaValorizacionItem> findActualesByActaArticuloIds(List<Long> actaArticuloIds) {
        if (actaArticuloIds == null || actaArticuloIds.isEmpty()) return List.of();
        return jdbc.sql(SQL_SELECT + " WHERE id_acta_articulo IN (:ids) AND si_actual = TRUE")
                .param("ids", actaArticuloIds)
                .query(JdbcActaValorizacionItemRepository::mapear).list();
    }

    private static FalActaValorizacionItem mapear(ResultSet rs, int fila) throws SQLException {
        Short modoPagoVolCodigo = (Short) rs.getObject("modo_valorizacion_pago_voluntario");
        return new FalActaValorizacionItem(
                rs.getObject("id", Long.class),
                rs.getObject("id_acta_articulo", Long.class),
                rs.getInt("version_item"),
                rs.getBoolean("si_actual"),
                rs.getObject("id_tarifario_articulo_usado", Long.class),
                rs.getObject("id_valor_unidad_usado", Long.class),
                ModoValorizacionItem.fromCodigo(rs.getShort("modo_valorizacion_ordinaria")),
                rs.getObject("cantidad_manual_ordinaria", BigDecimal.class),
                rs.getObject("monto_manual_ordinario", BigDecimal.class),
                modoPagoVolCodigo == null ? null : ModoValorizacionItem.fromCodigo(modoPagoVolCodigo),
                rs.getObject("cantidad_manual_pago_voluntario", BigDecimal.class),
                rs.getObject("monto_manual_pago_voluntario", BigDecimal.class),
                rs.getString("motivo_cambio"),
                rs.getString("observacion_cambio"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
    }
}
