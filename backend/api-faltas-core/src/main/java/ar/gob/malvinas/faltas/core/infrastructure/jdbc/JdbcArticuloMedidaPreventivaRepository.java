package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.repository.ArticuloMedidaPreventivaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ArticuloMedidaPreventivaRepository} sobre
 * {@code fal_articulo_medida_preventiva} (PK compuesta, sin id sintético). Creado en Wave 4. Sin
 * drift: 5 columnas físicas mapean 1:1 con {@link FalArticuloMedidaPreventiva}.
 */
@Repository
@Profile("jdbc")
public class JdbcArticuloMedidaPreventivaRepository implements ArticuloMedidaPreventivaRepository {

    private static final String COLUMNAS = "id_articulo, id_medida, si_obligatoria, si_activa, fh_alta, id_user_alta";

    private final JdbcClient jdbc;

    public JdbcArticuloMedidaPreventivaRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    /**
     * Igual regla que el adaptador InMemory: no se permite reactivar una relación inactiva vía
     * {@code save}. La reactivación exige una operación explícita (fuera del alcance de Wave 4).
     */
    @Override
    public FalArticuloMedidaPreventiva save(FalArticuloMedidaPreventiva rel) {
        if (rel == null) throw new IllegalArgumentException("rel es obligatoria");
        Optional<FalArticuloMedidaPreventiva> existente = findById(rel.getId());
        if (rel.isSiActiva() && existente.isPresent() && !existente.get().isSiActiva()) {
            throw new PrecondicionVioladaException(
                    "Reactivacion de relacion articulo-medida no permitida via save(). "
                            + "articuloId=" + rel.getArticuloId() + " medidaId=" + rel.getMedidaPreventivaId());
        }
        if (existente.isPresent()) {
            jdbc.sql("UPDATE fal_articulo_medida_preventiva SET si_activa = :siActiva WHERE id_articulo = :idArticulo AND id_medida = :idMedida")
                    .param("siActiva", rel.isSiActiva(), Types.BOOLEAN)
                    .param("idArticulo", rel.getArticuloId(), Types.BIGINT)
                    .param("idMedida", rel.getMedidaPreventivaId(), Types.BIGINT)
                    .update();
        } else {
            jdbc.sql("""
                    INSERT INTO fal_articulo_medida_preventiva (
                        id_articulo, id_medida, si_obligatoria, si_activa, fh_alta, id_user_alta
                    ) VALUES (:idArticulo, :idMedida, :siObligatoria, :siActiva, :fhAlta, :idUserAlta)
                    """)
                    .param("idArticulo", rel.getArticuloId(), Types.BIGINT)
                    .param("idMedida", rel.getMedidaPreventivaId(), Types.BIGINT)
                    .param("siObligatoria", rel.isSiObligatoria(), Types.BOOLEAN)
                    .param("siActiva", rel.isSiActiva(), Types.BOOLEAN)
                    .param("fhAlta", rel.getFhAlta(), Types.TIMESTAMP)
                    .param("idUserAlta", rel.getIdUserAlta(), Types.CHAR)
                    .update();
        }
        return findById(rel.getId()).orElseThrow();
    }

    @Override
    public Optional<FalArticuloMedidaPreventiva> findById(ArticuloMedidaPreventivaId id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_articulo_medida_preventiva WHERE id_articulo = :idArticulo AND id_medida = :idMedida")
                .param("idArticulo", id.articuloId(), Types.BIGINT)
                .param("idMedida", id.medidaPreventivaId(), Types.BIGINT)
                .query(JdbcArticuloMedidaPreventivaRepository::mapear).optional();
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findByArticuloId(Long articuloId) {
        if (articuloId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_articulo_medida_preventiva WHERE id_articulo = :idArticulo")
                .param("idArticulo", articuloId, Types.BIGINT).query(JdbcArticuloMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findActivasByArticuloId(Long articuloId) {
        if (articuloId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_articulo_medida_preventiva WHERE id_articulo = :idArticulo AND si_activa = TRUE")
                .param("idArticulo", articuloId, Types.BIGINT).query(JdbcArticuloMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public List<FalArticuloMedidaPreventiva> findByMedidaPreventivaId(Long medidaPreventivaId) {
        if (medidaPreventivaId == null) return List.of();
        return jdbc.sql("SELECT " + COLUMNAS + " FROM fal_articulo_medida_preventiva WHERE id_medida = :idMedida")
                .param("idMedida", medidaPreventivaId, Types.BIGINT).query(JdbcArticuloMedidaPreventivaRepository::mapear).list();
    }

    @Override
    public boolean existsActiva(ArticuloMedidaPreventivaId id) {
        return findById(id).map(FalArticuloMedidaPreventiva::isSiActiva).orElse(false);
    }

    private static FalArticuloMedidaPreventiva mapear(ResultSet rs, int fila) throws SQLException {
        FalArticuloMedidaPreventiva r = new FalArticuloMedidaPreventiva(
                new ArticuloMedidaPreventivaId(rs.getObject("id_articulo", Long.class), rs.getObject("id_medida", Long.class)),
                rs.getBoolean("si_obligatoria"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        r.setSiActiva(rs.getBoolean("si_activa"));
        return r;
    }
}
