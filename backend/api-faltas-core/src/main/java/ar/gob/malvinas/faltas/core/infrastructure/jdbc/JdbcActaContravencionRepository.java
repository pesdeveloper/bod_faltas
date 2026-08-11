package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.repository.ActaContravencionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ActaContravencionRepository} sobre {@code fal_acta_contravencion}
 * (1:1 con {@code fal_acta}). Creado en Wave 4 (alta integral JDBC). Sin drift contra el DDL
 * real: las 20 columnas físicas mapean 1:1 con los campos de {@link FalActaContravencion}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaContravencionRepository implements ActaContravencionRepository {

    private static final String COLUMNAS = """
            acta_id, id_suj_i, id_bie_i, id_suj_c, id_bie_c, circ, secc, frac, mza, parc,
            ufun, ucomp, origen_nomencl, si_nomenclatura_manual, motivo_nomenclatura_manual,
            rubro_id, id_rub, ambito_ctv, ambito_ctv_txt, fh_alta, id_user_alta
            """;

    private static final String SQL_UPSERT = """
            INSERT INTO fal_acta_contravencion (
                acta_id, id_suj_i, id_bie_i, id_suj_c, id_bie_c, circ, secc, frac, mza, parc,
                ufun, ucomp, origen_nomencl, si_nomenclatura_manual, motivo_nomenclatura_manual,
                rubro_id, id_rub, ambito_ctv, ambito_ctv_txt, fh_alta, id_user_alta
            ) VALUES (
                :actaId, :idSujI, :idBieI, :idSujC, :idBieC, :circ, :secc, :frac, :mza, :parc,
                :ufun, :ucomp, :origenNomencl, :siNomenclaturaManual, :motivoNomenclaturaManual,
                :rubroId, :idRub, :ambitoCtv, :ambitoCtvTxt, :fhAlta, :idUserAlta
            ) ON DUPLICATE KEY UPDATE
                id_suj_i = :idSujI, id_bie_i = :idBieI, id_suj_c = :idSujC, id_bie_c = :idBieC,
                circ = :circ, secc = :secc, frac = :frac, mza = :mza, parc = :parc, ufun = :ufun,
                ucomp = :ucomp, ambito_ctv = :ambitoCtv, ambito_ctv_txt = :ambitoCtvTxt,
                rubro_id = :rubroId, id_rub = :idRub
            """;

    private final JdbcClient jdbc;

    public JdbcActaContravencionRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    /**
     * {@code fal_acta_contravencion} tiene PK = {@code acta_id} (relación 1:1, sin AUTO_INCREMENT
     * propio): a diferencia de los satélites con PK sintética, {@code ON DUPLICATE KEY UPDATE} es
     * la estrategia canónica y no una excepción a la convención del proyecto (decisión ya vigente
     * para relaciones 1:1 por PK=FK, distinta de un upsert de negocio sobre PK sintética).
     */
    @Override
    public FalActaContravencion guardar(FalActaContravencion contravencion) {
        if (contravencion == null) throw new IllegalArgumentException("contravencion es obligatoria");
        jdbc.sql(SQL_UPSERT)
                .param("actaId", contravencion.getActaId(), Types.BIGINT)
                .param("idSujI", contravencion.getIdSujI(), Types.TINYINT)
                .param("idBieI", contravencion.getIdBieI(), Types.INTEGER)
                .param("idSujC", contravencion.getIdSujC(), Types.TINYINT)
                .param("idBieC", contravencion.getIdBieC(), Types.INTEGER)
                .param("circ", contravencion.getCirc(), Types.SMALLINT)
                .param("secc", contravencion.getSecc(), Types.VARCHAR)
                .param("frac", contravencion.getFrac(), Types.VARCHAR)
                .param("mza", contravencion.getMza(), Types.VARCHAR)
                .param("parc", contravencion.getParc(), Types.VARCHAR)
                .param("ufun", contravencion.getUfun(), Types.VARCHAR)
                .param("ucomp", contravencion.getUcomp(), Types.VARCHAR)
                .param("origenNomencl", contravencion.getOrigenNomencl().codigo(), Types.SMALLINT)
                .param("siNomenclaturaManual", contravencion.isSiNomenclaturaManual(), Types.BOOLEAN)
                .param("motivoNomenclaturaManual",
                        contravencion.getMotivoNomenclaturaManual() == null ? null : contravencion.getMotivoNomenclaturaManual().codigo(),
                        Types.SMALLINT)
                .param("rubroId", contravencion.getRubroId(), Types.BIGINT)
                .param("idRub", contravencion.getIdRub(), Types.INTEGER)
                .param("ambitoCtv", contravencion.getAmbitoCtv() == null ? null : contravencion.getAmbitoCtv().codigo(), Types.SMALLINT)
                .param("ambitoCtvTxt", contravencion.getAmbito_ctv_txt(), Types.VARCHAR)
                .param("fhAlta", contravencion.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", contravencion.getIdUserAlta(), Types.CHAR)
                .update();
        return findByActaId(contravencion.getActaId()).orElseThrow();
    }

    @Override
    public Optional<FalActaContravencion> findByActaId(Long actaId) {
        if (actaId == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_acta_contravencion WHERE acta_id = :actaId")
                .param("actaId", actaId, Types.BIGINT)
                .query(JdbcActaContravencionRepository::mapear)
                .optional();
    }

    @Override
    public boolean existsByActaId(Long actaId) {
        return findByActaId(actaId).isPresent();
    }

    private static FalActaContravencion mapear(ResultSet rs, int fila) throws SQLException {
        FalActaContravencion c = new FalActaContravencion(
                rs.getObject("acta_id", Long.class),
                OrigenNomenclatura.fromCodigo(rs.getShort("origen_nomencl")),
                rs.getBoolean("si_nomenclatura_manual"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        Integer idSujI = (Integer) rs.getObject("id_suj_i");
        Integer idBieI = (Integer) rs.getObject("id_bie_i");
        if (idSujI != null || idBieI != null) c.setSujetoInmueble(idSujI, idBieI);
        Integer idSujC = (Integer) rs.getObject("id_suj_c");
        Integer idBieC = (Integer) rs.getObject("id_bie_c");
        if (idSujC != null || idBieC != null) c.setSujetoComercio(idSujC, idBieC);
        c.setCirc((Short) rs.getObject("circ"));
        c.setSecc(rs.getString("secc"));
        c.setFrac(rs.getString("frac"));
        c.setMza(rs.getString("mza"));
        c.setParc(rs.getString("parc"));
        c.setUfun(rs.getString("ufun"));
        c.setUcomp(rs.getString("ucomp"));
        Short motivo = (Short) rs.getObject("motivo_nomenclatura_manual");
        if (motivo != null) c.setMotivoNomenclaturaManual(MotivoNomenclaturaManual.fromCodigo(motivo));
        Long rubroId = rs.getObject("rubro_id", Long.class);
        Integer idRub = (Integer) rs.getObject("id_rub");
        if (rubroId != null || idRub != null) c.setRubro(rubroId, idRub);
        Short ambito = (Short) rs.getObject("ambito_ctv");
        if (ambito != null) c.setAmbito(AmbitoCtv.fromCodigo(ambito), rs.getString("ambito_ctv_txt"));
        return c;
    }
}
