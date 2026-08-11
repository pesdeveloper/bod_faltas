package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
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

/**
 * Adaptador JDBC de MotivoArchivoRepository sobre fal_motivo_archivo.
 *
 * Estilo heredado de JdbcActaEventoRepository (WAVE 0B): JdbcClient sobre el
 * DataSource principal, SQL explicito en constantes, parametros nombrados y
 * RowMapper estatico privado. Sin framework interno ni generacion de SQL.
 *
 * Mapeo contra el DDL real:
 *   id                       BIGINT AUTO_INCREMENT  la asigna MariaDB
 *   cod_motivo_archivo       VARCHAR(8) UNIQUE      uq_fal_motivo_arch_cod
 *   nombre                   VARCHAR(64)
 *   si_nulidad               TINYINT(1)
 *   si_permite_reingreso     TINYINT(1)
 *   si_requiere_observacion  TINYINT(1)
 *   si_activo                TINYINT(1)             baja logica
 *   fh_alta                  DATETIME(6) NOT NULL   provisto por el dominio
 *   id_user_alta             CHAR(36) NOT NULL
 *   fh_ult_mod               DATETIME(6) NULL
 *   id_user_ult_mod          CHAR(36) NULL
 *
 * Identificadores (DECISION_ID-01): el INSERT nunca informa la PK y la recupera
 * con GeneratedKeyHolder. La actualizacion es UPDATE por PK; nunca REPLACE, que
 * borraria la fila, cambiaria el id y perderia la auditoria original.
 *
 * Tiempo: fh_alta y fh_ult_mod se persisten tal como los entrega el dominio
 * (FaltasClock es la fuente de tiempo). No se usa NOW() ni CURRENT_TIMESTAMP.
 *
 * Transaccionalidad: el adaptador no abre transacciones. La frontera vive en la
 * capa de aplicacion.
 */
@Repository
@Profile("jdbc")
public class JdbcMotivoArchivoRepository implements MotivoArchivoRepository {

    /**
     * Unica constraint cuya violacion se traduce a excepcion funcional. Cualquier
     * otro fallo de integridad conserva su error tecnico.
     */
    private static final String UQ_CODIGO = "uq_fal_motivo_arch_cod";

    private static final String COLUMNAS = """
            id, cod_motivo_archivo, nombre,
            si_nulidad, si_permite_reingreso, si_requiere_observacion, si_activo,
            fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            """;

    private static final String SQL_INSERT = """
            INSERT INTO fal_motivo_archivo (
                cod_motivo_archivo, nombre,
                si_nulidad, si_permite_reingreso, si_requiere_observacion, si_activo,
                fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            ) VALUES (
                :cod, :nombre,
                :siNulidad, :siPermiteReingreso, :siRequiereObservacion, :siActivo,
                :fhAlta, :idUserAlta, :fhUltMod, :idUserUltMod
            )
            """;

    private static final String SQL_UPDATE = """
            UPDATE fal_motivo_archivo SET
                cod_motivo_archivo = :cod,
                nombre = :nombre,
                si_nulidad = :siNulidad,
                si_permite_reingreso = :siPermiteReingreso,
                si_requiere_observacion = :siRequiereObservacion,
                si_activo = :siActivo,
                fh_alta = :fhAlta,
                id_user_alta = :idUserAlta,
                fh_ult_mod = :fhUltMod,
                id_user_ult_mod = :idUserUltMod
            WHERE id = :id
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_motivo_archivo
            WHERE id = :id
            """;

    private static final String SQL_POR_CODIGO = "SELECT " + COLUMNAS + """
            FROM fal_motivo_archivo
            WHERE cod_motivo_archivo = :cod
            """;

    private static final String SQL_ACTIVOS = "SELECT " + COLUMNAS + """
            FROM fal_motivo_archivo
            WHERE si_activo = TRUE
            ORDER BY id
            """;

    private static final String SQL_TODOS = "SELECT " + COLUMNAS + """
            FROM fal_motivo_archivo
            ORDER BY id
            """;

    private final JdbcClient jdbc;

    public JdbcMotivoArchivoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Con id nulo inserta y devuelve la entidad con el id que asigno la base; con
     * id presente actualiza esa fila por PK.
     *
     * A diferencia del adaptador InMemory, un id presente que no corresponde a
     * ninguna fila no se inserta: crear una fila con PK elegida por el llamador
     * seria preasignar identificadores. Se rechaza con la misma excepcion
     * funcional que usa actualizarAtomicamente para un id inexistente.
     */
    @Override
    public FalMotivoArchivo guardar(FalMotivoArchivo motivo) {
        return motivo.getId() == null ? insertar(motivo) : actualizarPorId(motivo);
    }

    @Override
    public Optional<FalMotivoArchivo> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id)
                .query(JdbcMotivoArchivoRepository::mapear)
                .optional();
    }

    /** Misma normalizacion que el adaptador InMemory: trim y mayusculas. */
    @Override
    public Optional<FalMotivoArchivo> buscarPorCodigo(String codMotivoArchivo) {
        if (codMotivoArchivo == null) {
            return Optional.empty();
        }
        return jdbc.sql(SQL_POR_CODIGO)
                .param("cod", codMotivoArchivo.trim().toUpperCase())
                .query(JdbcMotivoArchivoRepository::mapear)
                .optional();
    }

    @Override
    public List<FalMotivoArchivo> listarActivos() {
        return jdbc.sql(SQL_ACTIVOS).query(JdbcMotivoArchivoRepository::mapear).list();
    }

    @Override
    public List<FalMotivoArchivo> listarTodos() {
        return jdbc.sql(SQL_TODOS).query(JdbcMotivoArchivoRepository::mapear).list();
    }

    @Override
    public FalMotivoArchivo actualizarAtomicamente(FalMotivoArchivo motivo) {
        return actualizarPorId(motivo);
    }

    // =====================================================================
    // ESCRITURA
    // =====================================================================

    private FalMotivoArchivo insertar(FalMotivoArchivo motivo) {
        KeyHolder claves = new GeneratedKeyHolder();
        try {
            parametros(jdbc.sql(SQL_INSERT), motivo).update(claves);
        } catch (DuplicateKeyException e) {
            throw traducirCodigoDuplicado(e, motivo);
        }

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_motivo_archivo no devolvio el id generado para el codigo "
                            + motivo.getCodMotivoArchivo());
        }
        FalMotivoArchivo persistido = motivo.copia();
        persistido.setId(generado.longValue());
        return persistido;
    }

    /**
     * El UPDATE es la operacion atomica: la unicidad del codigo la refuerza el
     * UNIQUE fisico, no una lectura previa.
     *
     * MariaDB informa filas coincidentes, no filas modificadas, de modo que un
     * UPDATE con los mismos valores sobre una fila existente devuelve 1. Cero
     * filas significa que el id no existe.
     */
    private FalMotivoArchivo actualizarPorId(FalMotivoArchivo motivo) {
        int filas;
        try {
            filas = parametros(jdbc.sql(SQL_UPDATE), motivo)
                    .param("id", motivo.getId())
                    .update();
        } catch (DuplicateKeyException e) {
            throw traducirCodigoDuplicado(e, motivo);
        }
        if (filas == 0) {
            throw new PrecondicionVioladaException("MotivoArchivo no encontrado: " + motivo.getId());
        }
        return motivo.copia();
    }

    private static JdbcClient.StatementSpec parametros(JdbcClient.StatementSpec spec,
                                                       FalMotivoArchivo motivo) {
        return spec
                .param("cod", motivo.getCodMotivoArchivo(), Types.VARCHAR)
                .param("nombre", motivo.getNombre(), Types.VARCHAR)
                .param("siNulidad", motivo.isSiNulidad())
                .param("siPermiteReingreso", motivo.isSiPermiteReingreso())
                .param("siRequiereObservacion", motivo.isSiRequiereObservacion())
                .param("siActivo", motivo.isSiActivo())
                .param("fhAlta", motivo.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", motivo.getIdUserAlta(), Types.CHAR)
                .param("fhUltMod", motivo.getFhUltMod(), Types.TIMESTAMP)
                .param("idUserUltMod", motivo.getIdUserUltMod(), Types.CHAR);
    }

    /**
     * Traduce exclusivamente la violacion de uq_fal_motivo_arch_cod a la misma
     * excepcion funcional que produce el adaptador InMemory. Cualquier otra clave
     * duplicada se propaga sin enmascarar.
     */
    private static RuntimeException traducirCodigoDuplicado(DuplicateKeyException e,
                                                            FalMotivoArchivo motivo) {
        String causa = e.getMostSpecificCause().getMessage();
        if (causa != null && causa.contains(UQ_CODIGO)) {
            return new PrecondicionVioladaException(
                    "Ya existe un motivo con codigo: " + motivo.getCodMotivoArchivo());
        }
        return e;
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static FalMotivoArchivo mapear(ResultSet rs, int fila) throws SQLException {
        FalMotivoArchivo motivo = new FalMotivoArchivo(
                rs.getLong("id"),
                rs.getString("cod_motivo_archivo"),
                rs.getString("nombre"),
                rs.getBoolean("si_nulidad"),
                rs.getBoolean("si_permite_reingreso"),
                rs.getBoolean("si_requiere_observacion"),
                rs.getBoolean("si_activo"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        motivo.setFhUltMod(rs.getObject("fh_ult_mod", LocalDateTime.class));
        motivo.setIdUserUltMod(rs.getString("id_user_ult_mod"));
        return motivo;
    }
}
