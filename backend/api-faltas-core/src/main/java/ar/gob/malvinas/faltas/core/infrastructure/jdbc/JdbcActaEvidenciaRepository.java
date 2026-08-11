package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
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
 * Adaptador JDBC de {@link ActaEvidenciaRepository} sobre {@code fal_acta_evidencia}.
 *
 * <p>Creado en WAVE 1A2-B2. Antes de este slice el puerto solo tenia adaptador InMemory, de
 * modo que las evidencias del alta se escribian en memoria mientras el acta iba a MariaDB: un
 * rollback de la transaccion dejaba las evidencias vivas y apuntando a un acta inexistente.
 * Como la tabla y el contrato del puerto ya existian, el adaptador se implementa aca y no se
 * degrada la promesa de atomicidad de {@code DECISION_OBS-TX-01}.
 *
 * <p>Estilo heredado de {@code JdbcActaEventoRepository}: {@code JdbcClient} sobre el
 * DataSource principal, SQL explicito en constantes, parametros nombrados y RowMapper estatico
 * privado. Sin {@code SELECT *}, sin JPA, sin ORM, sin upsert de motor.
 *
 * <h2>Mapeo contra el DDL real, 18 columnas (Wave 4 Parte 3D: DECISION_DDL-EVID-03)</h2>
 *
 * <pre>
 * id             BIGINT AUTO_INCREMENT  la asigna MariaDB; nunca se informa
 * id_acta        BIGINT NOT NULL        FK fk_evidencia_acta -&gt; fal_acta.id
 * tipo_evid      SMALLINT NOT NULL      &lt;- TipoEvidenciaActa.codigo()
 * storage_key    VARCHAR(255) NOT NULL
 * fecha_registro DATETIME(6) NOT NULL   instante funcional de captura
 * hash_evid      CHAR(64) NULL          SHA-256 hexadecimal; chk_evidencia_hash;
 *                                       exclusiva de FOTO desde Parte 3D
 * orden, descripcion, nombre_original, mime_tipo, tamanio_bytes, ancho_px,
 * alto_px, fecha_captura_exif, bloque_al_incorporar, origen_incorporacion
 *                NULL, exclusivas de tipo_evid=FOTO (DECISION_DDL-EVID-03)
 * fh_alta        DATETIME(6) NOT NULL   instante tecnico de alta
 * id_user_alta   CHAR(36) NOT NULL      actor que incorporo la evidencia
 * </pre>
 *
 * <p>{@code tipo_evid} y {@code origen_incorporacion} viajan por {@code codigo()} y se leen
 * con {@code desdeCodigo(short)}: nunca por {@code ordinal()}, {@code name()} ni
 * {@code toString()} ({@code DECISION_DDL-EVID-01/02} y el mismo criterio para
 * {@code OrigenIncorporacionEvidencia}).
 *
 * <h2>Identidad: la PK la asigna la base</h2>
 *
 * {@code DECISION_ID-01}. El INSERT no informa {@code id} y {@link GeneratedKeyHolder} lo
 * recupera; <b>un id que venga en la evidencia de entrada se ignora</b>, igual que en
 * {@code JdbcActaEventoRepository}. Por eso no existe camino que produzca PK duplicada a
 * traves del puerto.
 *
 * <p>Consecuencia sobre {@link #nextId()}. El puerto lo expone porque
 * {@code FalActaEvidencia} exige {@code id} no nulo en su constructor, de modo que el caso de
 * uso necesita algun valor para construir el agregado antes de persistirlo. En el camino JDBC
 * ese valor es <b>provisional y no persistente</b>: no se consulta a la base, no reserva nada
 * y {@link #guardar(FalActaEvidencia)} lo descarta. La evidencia devuelta lleva el id real que
 * asigno {@code AUTO_INCREMENT}, y todo llamador debe continuar con esa copia.
 *
 * <p>Se devuelve un contador local monotono en lugar de una constante para que dos evidencias
 * construidas y todavia no persistidas no compartan identidad: si el caso de uso las pusiera
 * en un mapa por id antes de guardar, una constante colapsaria las dos en una.
 *
 * <p><b>Transaccionalidad.</b> Este adaptador no abre transacciones ni anota
 * {@code @Transactional}: la frontera vive en {@code ActaService.labrar} y en
 * {@code FotografiaEvidenciaService}. Las fechas y el actor se persisten tal como los entrega
 * el dominio ({@code FaltasClock} es la fuente de tiempo); no se usa {@code NOW()} ni
 * {@code CURRENT_TIMESTAMP}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaEvidenciaRepository implements ActaEvidenciaRepository {

    private static final String COLUMNAS = """
            id, id_acta, tipo_evid, storage_key, fecha_registro, hash_evid,
            orden, descripcion, nombre_original, mime_tipo, tamanio_bytes,
            ancho_px, alto_px, fecha_captura_exif, bloque_al_incorporar,
            origen_incorporacion, fh_alta, id_user_alta
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_acta_evidencia (
                id_acta, tipo_evid, storage_key, fecha_registro, hash_evid,
                orden, descripcion, nombre_original, mime_tipo, tamanio_bytes,
                ancho_px, alto_px, fecha_captura_exif, bloque_al_incorporar,
                origen_incorporacion, fh_alta, id_user_alta
            ) VALUES (
                :idActa, :tipoEvid, :storageKey, :fechaRegistro, :hashEvid,
                :orden, :descripcion, :nombreOriginal, :mimeTipo, :tamanioBytes,
                :anchoPx, :altoPx, :fechaCapturaExif, :bloqueAlIncorporar,
                :origenIncorporacion, :fhAlta, :idUserAlta
            )
            """;

    /**
     * Orden canonico por acta: {@code fh_alta} y despues {@code id}. El desempate por PK evita
     * que dos evidencias incorporadas en el mismo instante —el caso normal, porque el alta
     * comparte un unico instante de {@code FaltasClock}— lleguen en orden distinto en dos
     * lecturas. El prefijo izquierdo de {@code ix_evidencia_acta} sirve el filtro.
     */
    private static final String SQL_POR_ACTA = "SELECT " + COLUMNAS + """
            FROM fal_acta_evidencia
            WHERE id_acta = :idActa
            ORDER BY fh_alta ASC, id ASC
            """;

    private static final String SQL_FOTOS_POR_ACTA = "SELECT " + COLUMNAS + """
            FROM fal_acta_evidencia
            WHERE id_acta = :idActa AND tipo_evid = 4
            ORDER BY orden ASC
            """;

    private static final String SQL_FOTO_POR_ACTA_Y_ID = "SELECT " + COLUMNAS + """
            FROM fal_acta_evidencia
            WHERE id_acta = :idActa AND id = :id AND tipo_evid = 4
            """;

    private static final String SQL_MAX_ORDEN = """
            SELECT COALESCE(MAX(orden), 0) FROM fal_acta_evidencia
            WHERE id_acta = :idActa AND tipo_evid = 4
            """;

    private static final String SQL_EXISTE_HASH = """
            SELECT COUNT(*) FROM fal_acta_evidencia
            WHERE id_acta = :idActa AND tipo_evid = 4 AND hash_evid = :hash
            """;

    /**
     * Contador de ids provisionales. No corresponde a la secuencia de la base y no se
     * persiste: existe solo para que dos agregados construidos antes de guardar no compartan
     * identidad.
     */
    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);

    private final JdbcClient jdbc;

    public JdbcActaEvidenciaRepository(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc es obligatorio");
        }
        this.jdbc = jdbc;
    }

    /**
     * Id provisional para construir el agregado. <b>No es la PK.</b> Ver la nota de identidad
     * en el Javadoc de la clase.
     */
    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    /**
     * Inserta la evidencia y devuelve una copia con el id que asigno {@code AUTO_INCREMENT}.
     *
     * <p>El id de la evidencia de entrada se ignora. Una violacion de la clave ajena, del
     * {@code CHECK} del hash, de un {@code NOT NULL} o de los {@code UNIQUE(id_acta, orden)} /
     * {@code UNIQUE(id_acta, hash_evid)} se propaga como
     * {@code DataIntegrityViolationException} para que la transaccion del caso de uso decida:
     * este adaptador no se traga ninguna excepcion.
     */
    @Override
    public FalActaEvidencia guardar(FalActaEvidencia evidencia) {
        if (evidencia == null) {
            throw new IllegalArgumentException("evidencia es obligatoria");
        }
        KeyHolder claves = new GeneratedKeyHolder();

        Short origenCodigo = evidencia.getOrigenIncorporacion() == null
                ? null : evidencia.getOrigenIncorporacion().codigo();

        jdbc.sql(SQL_INSERT)
                .param("idActa", evidencia.getIdActa(), Types.BIGINT)
                .param("tipoEvid", evidencia.getTipoEvid().codigo(), Types.SMALLINT)
                .param("storageKey", evidencia.getStorageKey(), Types.VARCHAR)
                .param("fechaRegistro", evidencia.getFechaRegistro(), Types.TIMESTAMP)
                .param("hashEvid", evidencia.getHashEvid(), Types.CHAR)
                .param("orden", evidencia.getOrden(), Types.SMALLINT)
                .param("descripcion", evidencia.getDescripcion(), Types.VARCHAR)
                .param("nombreOriginal", evidencia.getNombreOriginal(), Types.VARCHAR)
                .param("mimeTipo", evidencia.getMimeTipo(), Types.VARCHAR)
                .param("tamanioBytes", evidencia.getTamanioBytes(), Types.BIGINT)
                .param("anchoPx", evidencia.getAnchoPx(), Types.INTEGER)
                .param("altoPx", evidencia.getAltoPx(), Types.INTEGER)
                .param("fechaCapturaExif", evidencia.getFechaCapturaExif(), Types.TIMESTAMP)
                .param("bloqueAlIncorporar", evidencia.getBloqueAlIncorporar(), Types.CHAR)
                .param("origenIncorporacion", origenCodigo, Types.SMALLINT)
                .param("fhAlta", evidencia.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", evidencia.getIdUserAlta(), Types.CHAR)
                .update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_acta_evidencia no devolvio el id generado para el acta "
                            + evidencia.getIdActa());
        }
        return conId(evidencia, generado.longValue());
    }

    @Override
    public List<FalActaEvidencia> listarPorActa(Long idActa) {
        if (idActa == null) {
            return List.of();
        }
        return jdbc.sql(SQL_POR_ACTA)
                .param("idActa", idActa, Types.BIGINT)
                .query(JdbcActaEvidenciaRepository::mapear)
                .list();
    }

    @Override
    public List<FalActaEvidencia> listarFotografiasPorActa(Long idActa) {
        if (idActa == null) {
            return List.of();
        }
        return jdbc.sql(SQL_FOTOS_POR_ACTA)
                .param("idActa", idActa, Types.BIGINT)
                .query(JdbcActaEvidenciaRepository::mapear)
                .list();
    }

    @Override
    public Optional<FalActaEvidencia> buscarFotografiaPorActaYId(Long idActa, Long idEvidencia) {
        if (idActa == null || idEvidencia == null) {
            return Optional.empty();
        }
        return jdbc.sql(SQL_FOTO_POR_ACTA_Y_ID)
                .param("idActa", idActa, Types.BIGINT)
                .param("id", idEvidencia, Types.BIGINT)
                .query(JdbcActaEvidenciaRepository::mapear)
                .optional();
    }

    @Override
    public int maxOrdenFotografiasPorActa(Long idActa) {
        if (idActa == null) {
            return 0;
        }
        Integer max = jdbc.sql(SQL_MAX_ORDEN)
                .param("idActa", idActa, Types.BIGINT)
                .query(Integer.class)
                .single();
        return max == null ? 0 : max;
    }

    @Override
    public boolean existeFotografiaConHash(Long idActa, String sha256Hex) {
        if (idActa == null || sha256Hex == null) {
            return false;
        }
        Integer n = jdbc.sql(SQL_EXISTE_HASH)
                .param("idActa", idActa, Types.BIGINT)
                .param("hash", sha256Hex, Types.CHAR)
                .query(Integer.class)
                .single();
        return n != null && n > 0;
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    /**
     * {@code FalActaEvidencia} es inmutable y no expone un {@code conId}, de modo que la copia
     * con el id real se construye con el constructor completo. Reconstruirla por el
     * constructor revalida sus invariantes: una fila que el motor acepto pero que el dominio
     * rechaza se detecta aca en lugar de circular como agregado invalido.
     */
    private static FalActaEvidencia conId(FalActaEvidencia origen, long id) {
        return new FalActaEvidencia(
                id,
                origen.getIdActa(),
                origen.getTipoEvid(),
                origen.getStorageKey(),
                origen.getFechaRegistro(),
                origen.getFhAlta(),
                origen.getIdUserAlta(),
                origen.getHashEvid(),
                origen.getOrden(),
                origen.getDescripcion(),
                origen.getNombreOriginal(),
                origen.getMimeTipo(),
                origen.getTamanioBytes(),
                origen.getAnchoPx(),
                origen.getAltoPx(),
                origen.getFechaCapturaExif(),
                origen.getBloqueAlIncorporar(),
                origen.getOrigenIncorporacion());
    }

    private static FalActaEvidencia mapear(ResultSet rs, int fila) throws SQLException {
        Short origenRaw = rs.getObject("origen_incorporacion", Short.class);
        OrigenIncorporacionEvidencia origen = origenRaw == null
                ? null : OrigenIncorporacionEvidencia.desdeCodigo(origenRaw);
        return new FalActaEvidencia(
                rs.getObject("id", Long.class),
                rs.getObject("id_acta", Long.class),
                TipoEvidenciaActa.desdeCodigo(rs.getShort("tipo_evid")),
                rs.getString("storage_key"),
                rs.getObject("fecha_registro", LocalDateTime.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"),
                rs.getString("hash_evid"),
                rs.getObject("orden", Integer.class),
                rs.getString("descripcion"),
                rs.getString("nombre_original"),
                rs.getString("mime_tipo"),
                rs.getObject("tamanio_bytes", Long.class),
                rs.getObject("ancho_px", Integer.class),
                rs.getObject("alto_px", Integer.class),
                rs.getObject("fecha_captura_exif", LocalDateTime.class),
                rs.getString("bloque_al_incorporar"),
                origen);
    }
}
