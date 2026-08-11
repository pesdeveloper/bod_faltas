package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.CanalAccesoQr;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoAccesoQr;
import ar.gob.malvinas.faltas.core.domain.model.FalActaQrAcceso;
import ar.gob.malvinas.faltas.core.repository.QrAccesoRepository;
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
 * Adaptador JDBC de {@link QrAccesoRepository} sobre {@code fal_acta_qr_acceso}.
 *
 * <p>Creado en {@code POST-B2-TX-CONSUMERS-01}. Antes de este slice el puerto solo tenia
 * adaptador InMemory sin restriccion de perfil, de modo que bajo {@code jdbc} el acceso QR
 * quedaba en memoria (se perdia al reiniciar el proceso) mientras el evento {@code QRACCA} y el
 * snapshot que la misma operacion escribe iban a MariaDB: era una mezcla JDBC/InMemory dentro de
 * una unica operacion logica. Como la tabla y el contrato del puerto ya existian, el adaptador
 * se implementa aca sin DDL.
 *
 * <p>Estilo heredado de {@code JdbcActaEvidenciaRepository}: {@code JdbcClient} sobre el
 * DataSource principal, SQL explicito en constantes, parametros nombrados y RowMapper estatico
 * privado. Sin {@code SELECT *}, sin JPA, sin ORM, sin upsert de motor.
 *
 * <h2>Mapeo contra el DDL real, 7 columnas</h2>
 *
 * <pre>
 * id                   BIGINT AUTO_INCREMENT  la asigna MariaDB; nunca se informa
 * acta_id              BIGINT NOT NULL        FK fk_qr_acceso_acta -&gt; fal_acta.id
 * fh_acceso            DATETIME(6) NOT NULL   instante funcional del acceso
 * ip_origen            VARCHAR(45) NULL       IPv4/IPv6, validado por el dominio
 * user_agent           VARCHAR(255) NULL      sanitizado por el dominio
 * resultado_acceso_qr  SMALLINT NOT NULL      &lt;- ResultadoAccesoQr.codigo()
 * fh_alta              DATETIME(6) NOT NULL   instante tecnico de alta
 * </pre>
 *
 * <p><b>{@code canal_acceso_qr} no existe en el DDL</b> (HUMAN_DECISION_CLOSED,
 * SPEC-MODEL-DDL-CLOSURE-001): mientras el unico canal sea WEB no aporta variabilidad funcional
 * y no se persiste. {@code FalActaQrAcceso.getCanalAcceso()} sigue existiendo en el dominio como
 * campo {@code @Deprecated} de compatibilidad; este adaptador no lo lee ni lo escribe.
 *
 * <h2>Identidad: la PK la asigna la base</h2>
 *
 * El INSERT no informa {@code id} y {@link GeneratedKeyHolder} lo recupera; un id que venga en
 * el acceso de entrada se ignora, igual que en {@code JdbcActaEvidenciaRepository} y
 * {@code JdbcActaEventoRepository}. Por eso {@link #nextId()} devuelve un valor
 * <b>provisional y no persistente</b>: {@code FalActaQrAcceso} exige {@code id} no nulo en su
 * constructor, de modo que el caso de uso necesita algun valor antes de persistir; el que
 * realmente queda es el que asigna {@code AUTO_INCREMENT}.
 *
 * <p><b>Transaccionalidad.</b> Este adaptador no abre transacciones ni anota
 * {@code @Transactional}: la frontera vive en el colaborador transaccional que invoca
 * {@code registrar} junto con el evento {@code QRACCA} y el snapshot
 * ({@code POST-B2-TX-CONSUMERS-01}). Las fechas se persisten tal como las entrega el dominio
 * ({@code FaltasClock} es la fuente de tiempo); no se usa {@code NOW()} ni
 * {@code CURRENT_TIMESTAMP}.
 */
@Repository
@Profile("jdbc")
public class JdbcQrAccesoRepository implements QrAccesoRepository {

    private static final String COLUMNAS = """
            id, acta_id, fh_acceso, ip_origen, user_agent, resultado_acceso_qr, fh_alta
            """;

    /** Las 6 columnas del alta: las 7 fisicas menos la PK, que asigna el motor. */
    private static final String SQL_INSERT = """
            INSERT INTO fal_acta_qr_acceso (
                acta_id, fh_acceso, ip_origen, user_agent, resultado_acceso_qr, fh_alta
            ) VALUES (
                :actaId, :fhAcceso, :ipOrigen, :userAgent, :resultadoAccesoQr, :fhAlta
            )
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_acta_qr_acceso
            WHERE id = :id
            """;

    /** Orden estable: {@code fh_acceso} y despues {@code id}, igual que el oraculo InMemory. */
    private static final String SQL_POR_ACTA = "SELECT " + COLUMNAS + """
            FROM fal_acta_qr_acceso
            WHERE acta_id = :actaId
            ORDER BY fh_acceso ASC, id ASC
            """;

    private static final String SQL_CONTAR_POR_ACTA = """
            SELECT COUNT(*) FROM fal_acta_qr_acceso WHERE acta_id = :actaId
            """;

    /**
     * Contador de ids provisionales. No corresponde a la secuencia de la base y no se persiste:
     * existe solo para que dos agregados construidos antes de guardar no compartan identidad.
     */
    /** Offset imposible de alcanzar por un AUTO_INCREMENT real: ver Javadoc en JdbcTalonarioRepository. */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);

    private final JdbcClient jdbc;

    public JdbcQrAccesoRepository(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc es obligatorio");
        }
        this.jdbc = jdbc;
    }

    /**
     * Id provisional para construir el agregado. <b>No es la PK.</b> Ver la nota de identidad en
     * el Javadoc de la clase.
     */
    @Override
    public Long nextId() {
        return provisional.getAndIncrement();
    }

    /**
     * Inserta el acceso y devuelve una copia con el id que asigno {@code AUTO_INCREMENT}.
     *
     * <p>El id del acceso de entrada se ignora. Una violacion de la clave ajena o de un
     * {@code NOT NULL} se propaga como {@code DataIntegrityViolationException} para que la
     * transaccion del caso de uso decida: este adaptador no se traga ninguna excepcion.
     */
    @Override
    public FalActaQrAcceso registrar(FalActaQrAcceso acceso) {
        if (acceso == null) {
            throw new IllegalArgumentException("acceso es obligatorio");
        }
        KeyHolder claves = new GeneratedKeyHolder();

        jdbc.sql(SQL_INSERT)
                .param("actaId", acceso.getActaId(), Types.BIGINT)
                .param("fhAcceso", acceso.getFhAcceso(), Types.TIMESTAMP)
                .param("ipOrigen", acceso.getIpOrigen(), Types.VARCHAR)
                .param("userAgent", acceso.getUserAgent(), Types.VARCHAR)
                .param("resultadoAccesoQr", acceso.getResultadoAcceso().codigo(), Types.SMALLINT)
                .param("fhAlta", acceso.getFhAlta(), Types.TIMESTAMP)
                .update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_acta_qr_acceso no devolvio el id generado para el acta "
                            + acceso.getActaId());
        }
        return conId(acceso, generado.longValue());
    }

    @Override
    public Optional<FalActaQrAcceso> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbc.sql(SQL_POR_ID)
                .param("id", id, Types.BIGINT)
                .query(JdbcQrAccesoRepository::mapear)
                .optional();
    }

    @Override
    public List<FalActaQrAcceso> listarPorActa(Long actaId) {
        if (actaId == null) {
            return List.of();
        }
        return jdbc.sql(SQL_POR_ACTA)
                .param("actaId", actaId, Types.BIGINT)
                .query(JdbcQrAccesoRepository::mapear)
                .list();
    }

    @Override
    public int contarPorActa(Long actaId) {
        if (actaId == null) {
            return 0;
        }
        return jdbc.sql(SQL_CONTAR_POR_ACTA)
                .param("actaId", actaId, Types.BIGINT)
                .query(Integer.class)
                .single();
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    /**
     * {@code FalActaQrAcceso} es inmutable y no expone un {@code conId}, de modo que la copia
     * con el id real se construye con el constructor. {@code canalAcceso} no se persiste
     * ({@code @Deprecated}); se conserva el del acceso de entrada solo por compatibilidad del
     * agregado en memoria, sin que eso implique una segunda fuente de verdad.
     */
    private static FalActaQrAcceso conId(FalActaQrAcceso origen, long id) {
        return new FalActaQrAcceso(
                id,
                origen.getActaId(),
                origen.getFhAcceso(),
                origen.getCanalAcceso(),
                origen.getIpOrigen(),
                origen.getUserAgent(),
                origen.getResultadoAcceso(),
                origen.getFhAlta());
    }

    /**
     * {@code canalAcceso} no existe en el DDL: se reconstruye como {@code null}-equivalente
     * mediante el unico valor tolerado por el constructor del dominio ({@code PORTAL}), porque
     * el constructor exige un canal no nulo pero el campo es {@code @Deprecated} y no se lee en
     * ningun camino productivo. No se inventa una columna para sostenerlo.
     */
    private static FalActaQrAcceso mapear(ResultSet rs, int fila) throws SQLException {
        return new FalActaQrAcceso(
                rs.getObject("id", Long.class),
                rs.getObject("acta_id", Long.class),
                rs.getObject("fh_acceso", LocalDateTime.class),
                CanalAccesoQr.PORTAL,
                rs.getString("ip_origen"),
                rs.getString("user_agent"),
                ResultadoAccesoQr.fromCodigo(rs.getShort("resultado_acceso_qr")),
                rs.getObject("fh_alta", LocalDateTime.class));
    }
}
