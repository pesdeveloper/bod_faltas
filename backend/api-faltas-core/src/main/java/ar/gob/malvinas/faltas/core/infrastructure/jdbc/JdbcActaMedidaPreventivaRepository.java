package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoMedidaAplicada;
import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.ActaMedidaPreventivaRepository;
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
 * Adaptador JDBC de {@link ActaMedidaPreventivaRepository} sobre
 * {@code fal_acta_medida_preventiva}. Creado en Wave 4. Sin drift: 9 columnas físicas mapean 1:1
 * con {@link FalActaMedidaPreventiva}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaMedidaPreventivaRepository implements ActaMedidaPreventivaRepository {

    private static final String COLUMNAS = """
            id, acta_id, acta_articulo_id, medida_preventiva_id, med_prev_txt, estado_medida,
            si_genera_bloqueante, fh_alta, id_user_alta
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcActaMedidaPreventivaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalActaMedidaPreventiva guardar(FalActaMedidaPreventiva medida) {
        if (medida == null) throw new IllegalArgumentException("medida es obligatoria");
        Optional<FalActaMedidaPreventiva> existente = findByPkFisica(medida);
        if (existente.isPresent()) {
            jdbc.sql("""
                    UPDATE fal_acta_medida_preventiva
                    SET med_prev_txt = :medPrevTxt, estado_medida = :estadoMedida
                    WHERE id = :id
                    """)
                    .param("medPrevTxt", medida.getMedPrevTxt(), Types.VARCHAR)
                    .param("estadoMedida", medida.getEstadoMedida().codigo(), Types.SMALLINT)
                    .param("id", existente.get().getId(), Types.BIGINT)
                    .update();
            return findById(existente.get().getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_acta_medida_preventiva (
                    acta_id, acta_articulo_id, medida_preventiva_id, med_prev_txt, estado_medida,
                    si_genera_bloqueante, fh_alta, id_user_alta
                ) VALUES (
                    :actaId, :actaArticuloId, :medidaPreventivaId, :medPrevTxt, :estadoMedida,
                    :siGeneraBloqueante, :fhAlta, :idUserAlta
                )
                """)
                .param("actaId", medida.getActaId(), Types.BIGINT)
                .param("actaArticuloId", medida.getActaArticuloId(), Types.BIGINT)
                .param("medidaPreventivaId", medida.getMedidaPreventivaId(), Types.BIGINT)
                .param("medPrevTxt", medida.getMedPrevTxt(), Types.VARCHAR)
                .param("estadoMedida", medida.getEstadoMedida().codigo(), Types.SMALLINT)
                .param("siGeneraBloqueante", medida.isSiGeneraBloqueante(), Types.BOOLEAN)
                .param("fhAlta", medida.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", medida.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_acta_medida_preventiva no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    private Optional<FalActaMedidaPreventiva> findByPkFisica(FalActaMedidaPreventiva medida) {
        if (medida.getId() == null) return Optional.empty();
        return findById(medida.getId());
    }

    @Override
    public Optional<FalActaMedidaPreventiva> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_medida_preventiva WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcActaMedidaPreventivaRepository::mapear).optional();
    }

    @Override
    public List<FalActaMedidaPreventiva> findByActaId(Long actaId) {
        if (actaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_medida_preventiva WHERE acta_id = :actaId ORDER BY id")
                .param("actaId", actaId, Types.BIGINT).query(JdbcActaMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public List<FalActaMedidaPreventiva> findActivasByActaId(Long actaId) {
        if (actaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_medida_preventiva WHERE acta_id = :actaId AND estado_medida = :estado ORDER BY id")
                .param("actaId", actaId, Types.BIGINT)
                .param("estado", EstadoMedidaAplicada.APLICADA.codigo(), Types.SMALLINT)
                .query(JdbcActaMedidaPreventivaRepository::mapear).list();
    }

    private static FalActaMedidaPreventiva mapear(ResultSet rs, int fila) throws SQLException {
        FalActaMedidaPreventiva m = new FalActaMedidaPreventiva(
                rs.getObject("id", Long.class), rs.getObject("acta_id", Long.class),
                rs.getObject("acta_articulo_id", Long.class), rs.getObject("medida_preventiva_id", Long.class),
                rs.getBoolean("si_genera_bloqueante"),
                rs.getObject("fh_alta", LocalDateTime.class), rs.getString("id_user_alta"));
        m.setMedPrevTxt(rs.getString("med_prev_txt"));
        EstadoMedidaAplicada estado = EstadoMedidaAplicada.fromCodigo(rs.getShort("estado_medida"));
        if (estado != EstadoMedidaAplicada.APLICADA) {
            m.transicionarEstado(estado);
        }
        return m;
    }
}
