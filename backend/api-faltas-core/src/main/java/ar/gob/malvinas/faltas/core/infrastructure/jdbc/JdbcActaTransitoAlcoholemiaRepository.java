package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoCualitativoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadMedidaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoAlcoholemiaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;
import ar.gob.malvinas.faltas.core.repository.ActaTransitoAlcoholemiaRepository;
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
 * Adaptador JDBC de {@link ActaTransitoAlcoholemiaRepository} sobre
 * {@code fal_acta_transito_alcoholemia}. Creado en Wave 4. Sin drift: 13 columnas físicas mapean
 * 1:1 con {@link FalActaTransitoAlcoholemia}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaTransitoAlcoholemiaRepository implements ActaTransitoAlcoholemiaRepository {

    private static final String COLUMNAS = """
            id, acta_id, orden_medicion, tipo_prueba, resultado_cualitativo, resultado_numerico,
            unidad_medida, id_alcoholimetro, ver_alcoholimetro, si_resultado_final, fh_medicion,
            fh_alta, id_user_alta
            """;

    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcActaTransitoAlcoholemiaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalActaTransitoAlcoholemia guardar(FalActaTransitoAlcoholemia medicion) {
        if (medicion == null) throw new IllegalArgumentException("medicion es obligatoria");
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO fal_acta_transito_alcoholemia (
                    acta_id, orden_medicion, tipo_prueba, resultado_cualitativo, resultado_numerico,
                    unidad_medida, id_alcoholimetro, ver_alcoholimetro, si_resultado_final,
                    fh_medicion, fh_alta, id_user_alta
                ) VALUES (
                    :actaId, :ordenMedicion, :tipoPrueba, :resultadoCualitativo, :resultadoNumerico,
                    :unidadMedida, :idAlcoholimetro, :verAlcoholimetro, :siResultadoFinal,
                    :fhMedicion, :fhAlta, :idUserAlta
                )
                """)
                .param("actaId", medicion.getActaId(), Types.BIGINT)
                .param("ordenMedicion", medicion.getOrdenMedicion(), Types.SMALLINT)
                .param("tipoPrueba", medicion.getTipoPrueba().codigo(), Types.SMALLINT)
                .param("resultadoCualitativo", medicion.getResultadoCualitativo() == null ? null : medicion.getResultadoCualitativo().codigo(), Types.SMALLINT)
                .param("resultadoNumerico", medicion.getResultadoNumerico(), Types.DECIMAL)
                .param("unidadMedida", medicion.getUnidadMedida() == null ? null : medicion.getUnidadMedida().codigo(), Types.SMALLINT)
                .param("idAlcoholimetro", medicion.getIdAlcoholimetro(), Types.BIGINT)
                .param("verAlcoholimetro", medicion.getVerAlcoholimetro(), Types.SMALLINT)
                .param("siResultadoFinal", medicion.isSiResultadoFinal(), Types.BOOLEAN)
                .param("fhMedicion", medicion.getFhMedicion(), Types.TIMESTAMP)
                .param("fhAlta", medicion.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", medicion.getIdUserAlta(), Types.CHAR)
                .update(claves);
        Number generado = claves.getKey();
        if (generado == null) throw new IllegalStateException("fal_acta_transito_alcoholemia no devolvio id generado");
        return findById(generado.longValue()).orElseThrow();
    }

    @Override
    public Optional<FalActaTransitoAlcoholemia> findById(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_transito_alcoholemia WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcActaTransitoAlcoholemiaRepository::mapear).optional();
    }

    @Override
    public List<FalActaTransitoAlcoholemia> findByActaId(Long actaId) {
        if (actaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_transito_alcoholemia WHERE acta_id = :actaId ORDER BY orden_medicion")
                .param("actaId", actaId, Types.BIGINT).query(JdbcActaTransitoAlcoholemiaRepository::mapear).list();
    }

    @Override
    public Optional<FalActaTransitoAlcoholemia> findResultadoFinalByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_transito_alcoholemia WHERE acta_id = :actaId AND si_resultado_final = TRUE")
                .param("actaId", actaId, Types.BIGINT).query(JdbcActaTransitoAlcoholemiaRepository::mapear).optional();
    }

    @Override
    public boolean existsOrdenByActaId(Long actaId, short orden) {
        if (actaId == null) return false;
        Long total = jdbc.sql("SELECT COUNT(*) FROM fal_acta_transito_alcoholemia WHERE acta_id = :actaId AND orden_medicion = :orden")
                .param("actaId", actaId, Types.BIGINT).param("orden", orden, Types.SMALLINT)
                .query(Long.class).single();
        return total != null && total > 0;
    }

    @Override
    public FalActaTransitoAlcoholemia marcarResultadoFinalAtomicamente(Long actaId, Long medicionId) {
        jdbc.sql("UPDATE fal_acta_transito_alcoholemia SET si_resultado_final = FALSE WHERE acta_id = :actaId AND si_resultado_final = TRUE")
                .param("actaId", actaId, Types.BIGINT)
                .update();
        int actualizadas = jdbc.sql("UPDATE fal_acta_transito_alcoholemia SET si_resultado_final = TRUE WHERE id = :id AND acta_id = :actaId")
                .param("id", medicionId, Types.BIGINT)
                .param("actaId", actaId, Types.BIGINT)
                .update();
        if (actualizadas == 0) throw new ActaTransitoAlcoholemiaNoEncontradaException(medicionId);
        return findById(medicionId).orElseThrow();
    }

    private static FalActaTransitoAlcoholemia mapear(ResultSet rs, int fila) throws SQLException {
        FalActaTransitoAlcoholemia m = new FalActaTransitoAlcoholemia(
                rs.getObject("id", Long.class), rs.getObject("acta_id", Long.class),
                rs.getShort("orden_medicion"), TipoPruebaAlcoholemia.fromCodigo(rs.getShort("tipo_prueba")),
                rs.getObject("fh_alta", LocalDateTime.class), rs.getString("id_user_alta"));
        Short resCual = (Short) rs.getObject("resultado_cualitativo");
        if (resCual != null) m.setResultadoCualitativo(ResultadoCualitativoAlcoholemia.fromCodigo(resCual));
        BigDecimal resNum = rs.getObject("resultado_numerico", BigDecimal.class);
        Short unidad = (Short) rs.getObject("unidad_medida");
        if (resNum != null) m.setResultadoNumerico(resNum, unidad == null ? null : UnidadMedidaAlcoholemia.fromCodigo(unidad));
        Long idAlc = rs.getObject("id_alcoholimetro", Long.class);
        Short verAlc = (Short) rs.getObject("ver_alcoholimetro");
        if (idAlc != null) m.setAlcoholimetro(idAlc, verAlc);
        m.setSiResultadoFinal(rs.getBoolean("si_resultado_final"));
        m.setFhMedicion(rs.getObject("fh_medicion", LocalDateTime.class));
        return m;
    }
}
