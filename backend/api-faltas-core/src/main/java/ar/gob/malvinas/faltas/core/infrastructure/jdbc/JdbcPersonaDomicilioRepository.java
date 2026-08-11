package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
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
 * Adaptador JDBC de {@link PersonaDomicilioRepository} sobre {@code fal_persona_domicilio}.
 *
 * <p>Creado en Wave 4 (alta integral JDBC) para permitir persistir, dentro de la misma
 * transacción del alta, los domicilios informados (`DECISION-ACTA-DOMICILIOS-OPCIONALES-01`:
 * domicilio ausente permite el alta; domicilio informado se persiste completo).
 *
 * <p>Estilo heredado de {@code JdbcActaEvidenciaRepository}: {@code JdbcClient}, SQL explícito,
 * parámetros nombrados, RowMapper estático. Sin {@code SELECT *}, sin ORM.
 *
 * <h2>Mapeo contra el DDL real, 29 columnas</h2>
 *
 * <p>{@code fal_persona_domicilio} no tiene {@code version_row} (baja lógica vía
 * {@code si_activo=false}, sin OCC). La PK la asigna MariaDB ({@code AUTO_INCREMENT}); el INSERT
 * nunca informa {@code id}.
 *
 * <p>{@code marcarPrincipal} (desmarcar el principal anterior de la misma persona+tipo) es un
 * método específico de {@code InMemoryPersonaDomicilioRepository} que no forma parte del puerto
 * {@link PersonaDomicilioRepository}; no se replica aquí porque el alta integral no lo necesita
 * (un domicilio recién creado puede marcarse principal directamente en el INSERT sin desmarcar
 * nada, porque es el primero de la transacción).
 */
@Repository
@Profile("jdbc")
public class JdbcPersonaDomicilioRepository implements PersonaDomicilioRepository {

    private static final String COLUMNAS = """
            id, persona_id, acta_origen_id, tipo_domicilio, origen_domicilio, modo_domicilio,
            si_activo, si_notificable, si_principal, id_provincia, unidad_territorial_tipo,
            id_unidad_territorial, id_localidad, id_calle, id_loc_malvinas,
            localidad_malvinas_version_id, id_tca_malvinas, calle_malvinas_version_id,
            calle_txt, altura, si_sin_altura, unidad_funcional, codigo_postal, domicilio_txt,
            si_normalizado_parcial, lat, lon, origen_ubicacion, fh_alta, id_user_alta,
            fh_ult_mod, id_user_ult_mod
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_persona_domicilio (
                persona_id, acta_origen_id, tipo_domicilio, origen_domicilio, modo_domicilio,
                si_activo, si_notificable, si_principal, id_provincia, unidad_territorial_tipo,
                id_unidad_territorial, id_localidad, id_calle, id_loc_malvinas,
                localidad_malvinas_version_id, id_tca_malvinas, calle_malvinas_version_id,
                calle_txt, altura, si_sin_altura, unidad_funcional, codigo_postal, domicilio_txt,
                si_normalizado_parcial, lat, lon, origen_ubicacion, fh_alta, id_user_alta
            ) VALUES (
                :personaId, :actaOrigenId, :tipoDomicilio, :origenDomicilio, :modoDomicilio,
                :siActivo, :siNotificable, :siPrincipal, :idProvincia, :unidadTerritorialTipo,
                :idUnidadTerritorial, :idLocalidad, :idCalle, :idLocMalvinas,
                :localidadMalvinasVersionId, :idTcaMalvinas, :calleMalvinasVersionId,
                :calleTxt, :altura, :siSinAltura, :unidadFuncional, :codigoPostal, :domicilioTxt,
                :siNormalizadoParcial, :lat, :lon, :origenUbicacion, :fhAlta, :idUserAlta
            )
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_persona_domicilio WHERE id = :id
            """;

    private static final String SQL_POR_PERSONA = "SELECT " + COLUMNAS + """
            FROM fal_persona_domicilio WHERE persona_id = :personaId ORDER BY id ASC
            """;

    private static final String SQL_ACTIVOS_POR_PERSONA = "SELECT " + COLUMNAS + """
            FROM fal_persona_domicilio WHERE persona_id = :personaId AND si_activo = TRUE
            ORDER BY id ASC
            """;

    private static final String SQL_NOTIFICABLES_POR_PERSONA = "SELECT " + COLUMNAS + """
            FROM fal_persona_domicilio
            WHERE persona_id = :personaId AND si_activo = TRUE AND si_notificable = TRUE
            ORDER BY id ASC
            """;

    private static final String SQL_PRINCIPAL_ACTIVO = "SELECT " + COLUMNAS + """
            FROM fal_persona_domicilio
            WHERE persona_id = :personaId AND tipo_domicilio = :tipoDomicilio
              AND si_activo = TRUE AND si_principal = TRUE
            """;

    /** Id provisional para construir el agregado antes de persistir; ver nota en {@link #nextId()}. */
    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);

    private final JdbcClient jdbc;

    public JdbcPersonaDomicilioRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    /**
     * Id provisional, no persistente. {@code FalPersonaDomicilio} exige {@code id} no nulo en su
     * constructor; el valor real lo asigna {@code AUTO_INCREMENT} en {@link #guardar}.
     */
    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    @Override
    public FalPersonaDomicilio guardar(FalPersonaDomicilio domicilio) {
        if (domicilio == null) throw new IllegalArgumentException("domicilio es obligatorio");
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql(SQL_INSERT)
                .param("personaId", domicilio.getPersonaId(), Types.BIGINT)
                .param("actaOrigenId", domicilio.getActaOrigenId(), Types.BIGINT)
                .param("tipoDomicilio", domicilio.getTipoDomicilio().codigo(), Types.SMALLINT)
                .param("origenDomicilio", domicilio.getOrigenDomicilio().codigo(), Types.SMALLINT)
                .param("modoDomicilio", domicilio.getModoDomicilio().codigo(), Types.SMALLINT)
                .param("siActivo", domicilio.isSiActivo(), Types.BOOLEAN)
                .param("siNotificable", domicilio.isSiNotificable(), Types.BOOLEAN)
                .param("siPrincipal", domicilio.isSiPrincipal(), Types.BOOLEAN)
                .param("idProvincia", domicilio.getIdProvincia(), Types.SMALLINT)
                .param("unidadTerritorialTipo",
                        domicilio.getUnidadTerritorialTipo() == null ? null : domicilio.getUnidadTerritorialTipo().codigo(),
                        Types.SMALLINT)
                .param("idUnidadTerritorial", domicilio.getIdUnidadTerritorial(), Types.INTEGER)
                .param("idLocalidad", domicilio.getIdLocalidad(), Types.BIGINT)
                .param("idCalle", domicilio.getIdCalle(), Types.BIGINT)
                .param("idLocMalvinas", domicilio.getIdLocMalvinas(), Types.VARCHAR)
                .param("localidadMalvinasVersionId", domicilio.getLocalidadMalvinasVersionId(), Types.BIGINT)
                .param("idTcaMalvinas", domicilio.getIdTcaMalvinas(), Types.VARCHAR)
                .param("calleMalvinasVersionId", domicilio.getCalleMalvinasVersionId(), Types.BIGINT)
                .param("calleTxt", domicilio.getCalleTxt(), Types.VARCHAR)
                .param("altura", domicilio.getAltura(), Types.INTEGER)
                .param("siSinAltura", domicilio.isSiSinAltura(), Types.BOOLEAN)
                .param("unidadFuncional", domicilio.getUnidadFuncional(), Types.VARCHAR)
                .param("codigoPostal", domicilio.getCodigoPostal(), Types.VARCHAR)
                .param("domicilioTxt", domicilio.getDomicilioTxt(), Types.VARCHAR)
                .param("siNormalizadoParcial", domicilio.isSiNormalizadoParcial(), Types.BOOLEAN)
                .param("lat", domicilio.getLat(), Types.DECIMAL)
                .param("lon", domicilio.getLon(), Types.DECIMAL)
                .param("origenUbicacion",
                        domicilio.getOrigenUbicacion() == null ? null : domicilio.getOrigenUbicacion().codigo(),
                        Types.SMALLINT)
                .param("fhAlta", domicilio.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", domicilio.getIdUserAlta(), Types.CHAR)
                .update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_persona_domicilio no devolvio el id generado para persona="
                            + domicilio.getPersonaId());
        }
        return buscarPorId(generado.longValue()).orElseThrow(() -> new IllegalStateException(
                "No se pudo releer el domicilio recien insertado id=" + generado));
    }

    @Override
    public Optional<FalPersonaDomicilio> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_POR_ID).param("id", id, Types.BIGINT)
                .query(JdbcPersonaDomicilioRepository::mapear)
                .optional();
    }

    @Override
    public List<FalPersonaDomicilio> buscarPorPersonaId(Long personaId) {
        if (personaId == null) return List.of();
        return jdbc.sql(SQL_POR_PERSONA).param("personaId", personaId, Types.BIGINT)
                .query(JdbcPersonaDomicilioRepository::mapear).list();
    }

    @Override
    public List<FalPersonaDomicilio> buscarActivosPorPersonaId(Long personaId) {
        if (personaId == null) return List.of();
        return jdbc.sql(SQL_ACTIVOS_POR_PERSONA).param("personaId", personaId, Types.BIGINT)
                .query(JdbcPersonaDomicilioRepository::mapear).list();
    }

    @Override
    public List<FalPersonaDomicilio> buscarNotificablesPorPersonaId(Long personaId) {
        if (personaId == null) return List.of();
        return jdbc.sql(SQL_NOTIFICABLES_POR_PERSONA).param("personaId", personaId, Types.BIGINT)
                .query(JdbcPersonaDomicilioRepository::mapear).list();
    }

    @Override
    public Optional<FalPersonaDomicilio> buscarPrincipalActivo(Long personaId, TipoDomicilio tipoDomicilio) {
        if (personaId == null || tipoDomicilio == null) return Optional.empty();
        return jdbc.sql(SQL_PRINCIPAL_ACTIVO)
                .param("personaId", personaId, Types.BIGINT)
                .param("tipoDomicilio", tipoDomicilio.codigo(), Types.SMALLINT)
                .query(JdbcPersonaDomicilioRepository::mapear)
                .optional();
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static FalPersonaDomicilio mapear(ResultSet rs, int fila) throws SQLException {
        Short unidadTerritorialTipoCodigo = (Short) rs.getObject("unidad_territorial_tipo");
        Short origenUbicacionCodigo = (Short) rs.getObject("origen_ubicacion");
        FalPersonaDomicilio d = new FalPersonaDomicilio(
                rs.getObject("id", Long.class),
                rs.getObject("persona_id", Long.class),
                rs.getObject("acta_origen_id", Long.class),
                TipoDomicilio.fromCodigo(rs.getShort("tipo_domicilio")),
                OrigenDomicilio.fromCodigo(rs.getShort("origen_domicilio")),
                ModoDomicilio.fromCodigo(rs.getShort("modo_domicilio")),
                rs.getBoolean("si_activo"),
                rs.getBoolean("si_notificable"),
                rs.getBoolean("si_principal"),
                (Short) rs.getObject("id_provincia"),
                unidadTerritorialTipoCodigo == null ? null : UnidadTerritorialTipo.fromCodigo(unidadTerritorialTipoCodigo),
                (Integer) rs.getObject("id_unidad_territorial"),
                rs.getObject("id_localidad", Long.class),
                rs.getObject("id_calle", Long.class),
                rs.getString("id_loc_malvinas"),
                rs.getObject("localidad_malvinas_version_id", Long.class),
                rs.getString("id_tca_malvinas"),
                rs.getObject("calle_malvinas_version_id", Long.class),
                rs.getString("calle_txt"),
                (Integer) rs.getObject("altura"),
                rs.getBoolean("si_sin_altura"),
                rs.getString("unidad_funcional"),
                rs.getString("codigo_postal"),
                rs.getString("domicilio_txt"),
                rs.getBoolean("si_normalizado_parcial"),
                rs.getObject("lat", BigDecimal.class),
                rs.getObject("lon", BigDecimal.class),
                origenUbicacionCodigo == null ? null : OrigenUbicacion.fromCodigo(origenUbicacionCodigo),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        LocalDateTime fhUltMod = rs.getObject("fh_ult_mod", LocalDateTime.class);
        if (fhUltMod != null) d.setFhUltMod(fhUltMod);
        String idUserUltMod = rs.getString("id_user_ult_mod");
        if (idUserUltMod != null) d.setIdUserUltMod(idUserUltMod);
        return d;
    }
}
