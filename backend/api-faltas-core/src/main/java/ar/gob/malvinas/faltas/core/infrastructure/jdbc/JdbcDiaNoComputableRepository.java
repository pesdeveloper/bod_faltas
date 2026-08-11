package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDiaNoComputable;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDiaNoComputable;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableRepository;
import ar.gob.malvinas.faltas.core.repository.DiaNoComputableSaveResult;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC de DiaNoComputableRepository sobre fal_dia_no_computable.
 *
 * Estilo heredado de JdbcActaEventoRepository (WAVE 0B): JdbcClient sobre el
 * DataSource principal, SQL explicito en constantes, parametros nombrados y
 * RowMapper estatico privado.
 *
 * Mapeo contra el DDL real:
 *   id                  BIGINT AUTO_INCREMENT  la asigna MariaDB
 *   fecha               DATE NOT NULL
 *   tipo                SMALLINT NOT NULL      TipoDiaNoComputable.codigo()
 *   origen              SMALLINT NOT NULL      OrigenDiaNoComputable.codigo()
 *   descripcion         VARCHAR(64) NOT NULL
 *   referencia_externa  VARCHAR(200) NULL      obligatoria para origen 2
 *   si_activo           TINYINT(1) NOT NULL    baja logica
 *   fh_alta             DATETIME(6) NOT NULL   provisto por el dominio
 *   id_user_alta        CHAR(36) NOT NULL
 *   fh_baja             DATETIME(6) NULL
 *   id_user_baja        CHAR(36) NULL
 *   fecha_activa        DATE STORED GENERATED  no se escribe nunca
 *
 * Los enums se persisten por codigo(), jamas por ordinal(), name() ni toString()
 * (DECISION_DDL-ENUM-01).
 *
 * La regla "exactamente un registro activo por fecha" la sostiene el UNIQUE
 * fisico uq_fal_dia_nc_fecha_activa sobre la columna generada fecha_activa, que
 * vale NULL cuando si_activo es falso. No hay lock en la JVM: la atomicidad es
 * del motor.
 *
 * Transaccionalidad: el adaptador no abre transacciones. La frontera vive en la
 * capa de aplicacion.
 */
@Repository
@Profile("jdbc")
public class JdbcDiaNoComputableRepository implements DiaNoComputableRepository {

    /**
     * Unica constraint cuya violacion significa "la fecha ya tenia un registro
     * activo". Cualquier otra clave duplicada conserva su error tecnico.
     */
    private static final String UQ_FECHA_ACTIVA = "uq_fal_dia_nc_fecha_activa";

    private static final String COLUMNAS = """
            id, fecha, tipo, origen, descripcion, referencia_externa,
            si_activo, fh_alta, id_user_alta, fh_baja, id_user_baja
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_dia_no_computable (
                fecha, tipo, origen, descripcion, referencia_externa,
                si_activo, fh_alta, id_user_alta, fh_baja, id_user_baja
            ) VALUES (
                :fecha, :tipo, :origen, :descripcion, :referenciaExterna,
                :siActivo, :fhAlta, :idUserAlta, :fhBaja, :idUserBaja
            )
            """;

    private static final String SQL_UPDATE = """
            UPDATE fal_dia_no_computable SET
                fecha = :fecha,
                tipo = :tipo,
                origen = :origen,
                descripcion = :descripcion,
                referencia_externa = :referenciaExterna,
                si_activo = :siActivo,
                fh_alta = :fhAlta,
                id_user_alta = :idUserAlta,
                fh_baja = :fhBaja,
                id_user_baja = :idUserBaja
            WHERE id = :id
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_dia_no_computable
            WHERE id = :id
            """;

    /** Se consulta por la columna generada: es la misma expresion que sostiene el UNIQUE. */
    private static final String SQL_ACTIVO_POR_FECHA = "SELECT " + COLUMNAS + """
            FROM fal_dia_no_computable
            WHERE fecha_activa = :fecha
            """;

    /**
     * Relectura tras perder la carrera. Es una lectura bloqueante porque debe ver
     * la fila que el ganador acaba de confirmar: bajo REPEATABLE READ una lectura
     * consistente podria fijar un snapshot que todavia no la incluye.
     */
    private static final String SQL_ACTIVO_POR_FECHA_CONFIRMADO =
            SQL_ACTIVO_POR_FECHA + " LOCK IN SHARE MODE";

    /**
     * La referencia externa es un identificador tecnico, no texto de busqueda: se
     * compara de forma exacta (D-1A1-05). Sin el CAST, la collation de la tabla
     * (utf8mb4_uca1400_ai_ci) haria la comparacion insensible a mayusculas y a
     * acentos, divergiendo del String.equals del adaptador InMemory.
     *
     * CAST(... AS BINARY) en vez de COLLATE utf8mb4_bin porque esa collation es
     * PAD SPACE y seguiria igualando valores que difieren en espacios finales.
     * No hay indice sobre referencia_externa, asi que el CAST no descarta ninguno.
     */
    private static final String SQL_POR_ORIGEN_Y_REFERENCIA = "SELECT " + COLUMNAS + """
            FROM fal_dia_no_computable
            WHERE origen = :origen
              AND CAST(referencia_externa AS BINARY) = :referenciaExterna
            ORDER BY id
            LIMIT 1
            """;

    private static final String SQL_ACTIVOS_ORDENADOS = "SELECT " + COLUMNAS + """
            FROM fal_dia_no_computable
            WHERE si_activo = TRUE
            ORDER BY fecha, id
            """;

    private final JdbcClient jdbc;

    public JdbcDiaNoComputableRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Con id nulo inserta y devuelve la entidad con el id que asigno la base; con
     * id presente actualiza esa fila por PK.
     *
     * A diferencia del adaptador InMemory, un id presente que no corresponde a
     * ninguna fila no se inserta: crear una fila con PK elegida por el llamador
     * seria preasignar identificadores.
     *
     * Reactivar una fecha que ya tiene otro registro activo viola
     * uq_fal_dia_nc_fecha_activa. Ese caso no se traduce: guardar no expresa
     * preexistencia y el error de integridad debe llegar al llamador tal cual.
     */
    @Override
    public FalDiaNoComputable guardar(FalDiaNoComputable dia) {
        if (dia.getId() == null) {
            return dia.conId(insertar(dia));
        }
        int filas = parametros(jdbc.sql(SQL_UPDATE), dia)
                .param("id", dia.getId())
                .update();
        if (filas == 0) {
            throw new PrecondicionVioladaException("Dia no computable no encontrado: id=" + dia.getId());
        }
        return dia.copia();
    }

    /**
     * Intenta insertar y deja que el UNIQUE decida quien gana.
     *
     * El id del candidato se ignora si viene informado: la PK la asigna siempre
     * MariaDB. Si el INSERT es rechazado por uq_fal_dia_nc_fecha_activa, se relee
     * la fila activa de esa fecha y se devuelve como entidad canonica.
     *
     * En InnoDB un duplicate key revierte la sentencia, no la transaccion, de modo
     * que la relectura se hace en la misma transaccion del llamador.
     *
     * El identificador consumido por un INSERT que pierde la carrera no se
     * reutiliza: la secuencia AUTO_INCREMENT no es gapless y el contrato no lo
     * exige. Lo que se garantiza es unicidad, identidad asignada y un resultado
     * explicito.
     */
    @Override
    public DiaNoComputableSaveResult guardarActivoSiAusentePorFecha(FalDiaNoComputable candidato) {
        try {
            return new DiaNoComputableSaveResult(candidato.conId(insertar(candidato)), false);
        } catch (DuplicateKeyException e) {
            String causa = e.getMostSpecificCause().getMessage();
            if (causa == null || !causa.contains(UQ_FECHA_ACTIVA)) {
                throw e;
            }
            return jdbc.sql(SQL_ACTIVO_POR_FECHA_CONFIRMADO)
                    .param("fecha", candidato.getFecha())
                    .query(JdbcDiaNoComputableRepository::mapear)
                    .optional()
                    .map(existente -> new DiaNoComputableSaveResult(existente, true))
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<FalDiaNoComputable> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id)
                .query(JdbcDiaNoComputableRepository::mapear)
                .optional();
    }

    @Override
    public Optional<FalDiaNoComputable> buscarActivoPorFecha(LocalDate fecha) {
        return jdbc.sql(SQL_ACTIVO_POR_FECHA)
                .param("fecha", fecha)
                .query(JdbcDiaNoComputableRepository::mapear)
                .optional();
    }

    /** Mismas guardas que el adaptador InMemory: sin origen o sin referencia util no hay busqueda. */
    @Override
    public Optional<FalDiaNoComputable> buscarPorOrigenYReferenciaExterna(
            OrigenDiaNoComputable origen,
            String referenciaExterna) {
        if (origen == null || referenciaExterna == null || referenciaExterna.isBlank()) {
            return Optional.empty();
        }
        return jdbc.sql(SQL_POR_ORIGEN_Y_REFERENCIA)
                .param("origen", origen.codigo())
                .param("referenciaExterna", referenciaExterna.trim(), Types.VARCHAR)
                .query(JdbcDiaNoComputableRepository::mapear)
                .optional();
    }

    @Override
    public List<FalDiaNoComputable> listarActivosOrdenados() {
        return jdbc.sql(SQL_ACTIVOS_ORDENADOS)
                .query(JdbcDiaNoComputableRepository::mapear)
                .list();
    }

    // =====================================================================
    // ESCRITURA
    // =====================================================================

    private long insertar(FalDiaNoComputable dia) {
        KeyHolder claves = new GeneratedKeyHolder();
        parametros(jdbc.sql(SQL_INSERT), dia).update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_dia_no_computable no devolvio el id generado para la fecha " + dia.getFecha());
        }
        return generado.longValue();
    }

    private static JdbcClient.StatementSpec parametros(JdbcClient.StatementSpec spec,
                                                       FalDiaNoComputable dia) {
        return spec
                .param("fecha", dia.getFecha())
                .param("tipo", dia.getTipo().codigo())
                .param("origen", dia.getOrigen().codigo())
                .param("descripcion", dia.getDescripcion(), Types.VARCHAR)
                .param("referenciaExterna", dia.getReferenciaExterna(), Types.VARCHAR)
                .param("siActivo", dia.isSiActivo())
                .param("fhAlta", dia.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", dia.getIdUserAlta(), Types.CHAR)
                .param("fhBaja", dia.getFhBaja(), Types.TIMESTAMP)
                .param("idUserBaja", dia.getIdUserBaja(), Types.CHAR);
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    /**
     * FalDiaNoComputable nace activa y solo puede desactivarse por comportamiento,
     * asi que una fila inactiva se reconstruye reproduciendo la baja con sus datos
     * de auditoria reales.
     */
    private static FalDiaNoComputable mapear(ResultSet rs, int fila) throws SQLException {
        FalDiaNoComputable dia = new FalDiaNoComputable(
                rs.getLong("id"),
                rs.getObject("fecha", LocalDate.class),
                TipoDiaNoComputable.desdeCodigo(rs.getShort("tipo")),
                rs.getString("descripcion"),
                OrigenDiaNoComputable.desdeCodigo(rs.getShort("origen")),
                rs.getString("referencia_externa"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));

        if (!rs.getBoolean("si_activo")) {
            LocalDateTime fhBaja = rs.getObject("fh_baja", LocalDateTime.class);
            String idUserBaja = rs.getString("id_user_baja");
            if (fhBaja == null || idUserBaja == null) {
                throw new IllegalStateException(
                        "fal_dia_no_computable id=" + rs.getLong("id")
                                + " esta inactiva sin fh_baja o id_user_baja");
            }
            dia.desactivar(fhBaja, idUserBaja);
        }
        return dia;
    }
}
