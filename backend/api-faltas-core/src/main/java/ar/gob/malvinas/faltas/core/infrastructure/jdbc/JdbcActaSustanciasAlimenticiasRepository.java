package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;
import ar.gob.malvinas.faltas.core.repository.ActaSustanciasAlimenticiasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ActaSustanciasAlimenticiasRepository} sobre
 * {@code fal_acta_sustancias_alimenticias} (1:1 con {@code fal_acta}, PK = {@code acta_id}).
 * Creado en Wave 4. Sin drift: 8 columnas físicas mapean 1:1 con
 * {@link FalActaSustanciasAlimenticias}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaSustanciasAlimenticiasRepository implements ActaSustanciasAlimenticiasRepository {

    private static final String COLUMNAS = """
            acta_id, rubro_id, id_rub, ambito_ctv, ambito_ctv_txt, descripcion_sustancias,
            fh_alta, id_user_alta
            """;

    private static final String SQL_UPSERT = """
            INSERT INTO fal_acta_sustancias_alimenticias (
                acta_id, rubro_id, id_rub, ambito_ctv, ambito_ctv_txt, descripcion_sustancias,
                fh_alta, id_user_alta
            ) VALUES (
                :actaId, :rubroId, :idRub, :ambitoCtv, :ambitoCtvTxt, :descripcionSustancias,
                :fhAlta, :idUserAlta
            ) ON DUPLICATE KEY UPDATE
                rubro_id = :rubroId, id_rub = :idRub, ambito_ctv = :ambitoCtv,
                ambito_ctv_txt = :ambitoCtvTxt, descripcion_sustancias = :descripcionSustancias
            """;

    private final JdbcClient jdbc;

    public JdbcActaSustanciasAlimenticiasRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    @Override
    public FalActaSustanciasAlimenticias guardar(FalActaSustanciasAlimenticias sustancias) {
        if (sustancias == null) throw new IllegalArgumentException("sustancias es obligatorio");
        jdbc.sql(SQL_UPSERT)
                .param("actaId", sustancias.getActaId(), Types.BIGINT)
                .param("rubroId", sustancias.getRubroId(), Types.BIGINT)
                .param("idRub", sustancias.getIdRub(), Types.INTEGER)
                .param("ambitoCtv", sustancias.getAmbitoCtv() == null ? null : sustancias.getAmbitoCtv().codigo(), Types.SMALLINT)
                .param("ambitoCtvTxt", sustancias.getAmbitoCtvTxt(), Types.VARCHAR)
                .param("descripcionSustancias", sustancias.getDescripcionSustancias(), Types.VARCHAR)
                .param("fhAlta", sustancias.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", sustancias.getIdUserAlta(), Types.CHAR)
                .update();
        return findByActaId(sustancias.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaSustanciasAlimenticias> findByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_sustancias_alimenticias WHERE acta_id = :actaId")
                .param("actaId", actaId, Types.BIGINT)
                .query(JdbcActaSustanciasAlimenticiasRepository::mapear)
                .optional();
    }

    @Override
    public boolean existsByActaId(Long actaId) {
        return findByActaId(actaId).isPresent();
    }

    private static FalActaSustanciasAlimenticias mapear(ResultSet rs, int fila) throws SQLException {
        FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(
                rs.getObject("acta_id", Long.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        Long rubroId = rs.getObject("rubro_id", Long.class);
        Integer idRub = (Integer) rs.getObject("id_rub");
        if (rubroId != null || idRub != null) s.setRubro(rubroId, idRub);
        Short ambito = (Short) rs.getObject("ambito_ctv");
        if (ambito != null) s.setAmbito(AmbitoCtv.fromCodigo(ambito), rs.getString("ambito_ctv_txt"));
        s.setDescripcionSustancias(rs.getString("descripcion_sustancias"));
        return s;
    }
}
