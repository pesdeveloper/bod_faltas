package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptador JDBC de {@link TalonarioRepository} sobre {@code num_politica},
 * {@code num_talonario}, {@code num_talonario_ambito}, {@code num_talonario_inspector}
 * y {@code num_talonario_movimiento} (Wave 4 PARTE-2 S8).
 *
 * <h2>Numeracion electronica sin SEQUENCE (S8.3)</h2>
 * {@link #nextNumeroTalonario} implementa la seccion critica breve del alta: bloquea
 * unicamente la fila del talonario con {@code SELECT ... FOR UPDATE}, calcula el
 * siguiente correlativo, lo valida contra {@code nro_hasta} y actualiza
 * {@code ultimo_nro_emitido} en la misma transaccion del llamador (no hay
 * {@code @Transactional} propio: la frontera es de {@code ActaService.labrar}). Un
 * rollback posterior revierte el {@code UPDATE} junto con el resto: el numero queda
 * libre y reutilizable.
 */
@Repository
@Profile("jdbc")
public class JdbcTalonarioRepository implements TalonarioRepository {

    private static final String SQL_SELECT_POLITICA = """
            SELECT id, codigo, apodo, descripcion, clase_numeracion, si_reinicio_anual,
                   si_incluye_prefijo, prefijo, si_incluye_anio, formato_anio, si_incluye_serie,
                   longitud_nro, formato_visible, si_activa, fh_vig_desde, fh_vig_hasta,
                   fh_alta, id_user_alta
            FROM num_politica
            """;

    private static final String SQL_SELECT_TALONARIO = """
            SELECT id, version_row, politica_id, codigo, descripcion, tipo_talonario, clase_talonario,
                   anio, serie, nro_desde, nro_hasta, nombre_secuencia, ultimo_nro_emitido,
                   si_activo, si_bloqueado, cod_desbloqueo, fh_alta, id_user_alta
            FROM num_talonario
            """;

    private static final String SQL_SELECT_AMBITO = """
            SELECT id, talonario_id, clase_talonario, tipo_docu, tipo_acta, id_dep, ver_dep,
                   alcance, prioridad, fh_desde, fh_hasta, si_activo, fh_alta, id_user_alta
            FROM num_talonario_ambito
            """;

    private static final String SQL_SELECT_ASIGNACION = """
            SELECT id, id_talonario, id_insp, ver_insp, fh_entrega, id_user_entrega,
                   fh_devolucion, id_user_devolucion, estado_asignacion, si_activa, talonario_id_activo
            FROM num_talonario_inspector
            """;

    private static final String SQL_SELECT_MOVIMIENTO = """
            SELECT id, id_talonario, nro_talonario, estado_numero, motivo_anulacion,
                   acta_id, documento_id, id_dep, ver_dep, id_insp, ver_insp,
                   fh_movimiento, id_user_movimiento
            FROM num_talonario_movimiento
            """;

    /**
     * Contador puramente local y efimero para los {@code nextXxxId()} del puerto. No es un id
     * real: {@code guardarPolitica}/{@code guardarTalonario}/{@code guardarAmbito} lo descartan
     * siempre e insertan con {@code GeneratedKeyHolder} (AUTO_INCREMENT real). Arranca en un
     * offset negativo enorme, imposible de alcanzar por un AUTO_INCREMENT real (siempre
     * positivo), para que el chequeo defensivo {@code existePorId} de esos tres metodos nunca
     * confunda este contador efimero con la PK real de una fila ya persistida por una sesion
     * de pruebas anterior (bug detectado en Wave 4 PARTE-3A: sin este offset, un contador que
     * arranca en 1 coincide con ids reales acumulados y dispara una actualizacion espuria que
     * anula {@code apodo}/otros campos NOT NULL de una fila ajena).
     */
    private final AtomicLong provisional = new AtomicLong(Long.MIN_VALUE + 1);
    private final JdbcClient jdbc;

    public JdbcTalonarioRepository(JdbcClient jdbc) {
        if (jdbc == null) throw new IllegalArgumentException("jdbc es obligatorio");
        this.jdbc = jdbc;
    }

    private long provisionalId() { return provisional.getAndIncrement(); }

    @Override public long nextPoliticaId() { return provisionalId(); }
    @Override public long nextTalonarioId() { return provisionalId(); }
    @Override public long nextAmbitoId() { return provisionalId(); }
    @Override public long nextAsignacionInspectorId() { return provisionalId(); }
    @Override public long nextMovimientoId() { return provisionalId(); }

    // ---- Numeracion electronica: seccion critica ----

    @Override
    public int nextNumeroTalonario(Long idTalonario, int nroDesde, Integer nroHasta) {
        Integer ultimo = jdbc.sql("SELECT ultimo_nro_emitido FROM num_talonario WHERE id = :id FOR UPDATE")
                .param("id", idTalonario, Types.BIGINT)
                .query(Integer.class)
                .optional()
                .orElseThrow(() -> new PrecondicionVioladaException("Talonario no encontrado: " + idTalonario));
        int siguiente = Math.max(ultimo, nroDesde - 1) + 1;
        if (nroHasta != null && siguiente > nroHasta) {
            throw new PrecondicionVioladaException(
                    "El talonario " + idTalonario + " ha agotado su rango de numeracion (nroHasta=" + nroHasta + ").");
        }
        int filas = jdbc.sql("UPDATE num_talonario SET ultimo_nro_emitido = :siguiente WHERE id = :id")
                .param("siguiente", siguiente, Types.INTEGER)
                .param("id", idTalonario, Types.BIGINT)
                .update();
        if (filas == 0) throw new IllegalStateException("No se pudo actualizar el contador de num_talonario id=" + idTalonario);
        return siguiente;
    }

    // ---- Politica ----

    @Override
    public NumPolitica guardarPolitica(NumPolitica p) {
        if (p.getId() != null && existePorId("num_politica", p.getId())) {
            // Correccion administrativa minima (activar/desactivar, cerrar vigencia). fh_ult_mod/
            // id_user_ult_mod quedan a cargo de un caso de uso de edicion dedicado si se necesita
            // en una wave futura: aqui no se usa reloj directo (FaltasClockUsageGuardTest).
            jdbc.sql("""
                    UPDATE num_politica SET apodo=:apodo, si_activa=:siActiva, fh_vig_hasta=:fhVigHasta
                    WHERE id=:id
                    """)
                    .param("apodo", p.getApodo())
                    .param("siActiva", p.isSiActiva(), Types.BOOLEAN)
                    .param("fhVigHasta", p.getFhVigHasta())
                    .param("id", p.getId(), Types.BIGINT)
                    .update();
            return buscarPoliticaPorId(p.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO num_politica (
                    codigo, apodo, descripcion, clase_numeracion, si_reinicio_anual, si_incluye_prefijo, prefijo,
                    si_incluye_anio, formato_anio, si_incluye_serie, longitud_nro, formato_visible, si_activa,
                    fh_vig_desde, fh_vig_hasta, fh_alta, id_user_alta
                ) VALUES (
                    :codigo, :apodo, :descripcion, :clase, :siReinicioAnual, :siIncluyePrefijo, :prefijo,
                    :siIncluyeAnio, :formatoAnio, :siIncluyeSerie, :longitudNro, :formatoVisible, :siActiva,
                    :fhVigDesde, :fhVigHasta, :fhAlta, :idUserAlta
                )
                """)
                .param("codigo", p.getCodigo())
                .param("apodo", p.getApodo() != null ? p.getApodo() : p.getCodigo())
                .param("descripcion", p.getDescripcion())
                .param("clase", p.getClaseNumeracion().codigo(), Types.SMALLINT)
                .param("siReinicioAnual", p.isSiReinicioAnual(), Types.BOOLEAN)
                .param("siIncluyePrefijo", p.isSiIncluyePrefijo(), Types.BOOLEAN)
                .param("prefijo", p.getPrefijo())
                .param("siIncluyeAnio", p.isSiIncluyeAnio(), Types.BOOLEAN)
                .param("formatoAnio", p.getFormatoAnio(), Types.SMALLINT)
                .param("siIncluyeSerie", p.isSiIncluyeSerie(), Types.BOOLEAN)
                .param("longitudNro", p.getLongitudNro(), Types.SMALLINT)
                .param("formatoVisible", p.getFormatoVisible())
                .param("siActiva", p.isSiActiva(), Types.BOOLEAN)
                .param("fhVigDesde", p.getFhVigDesde())
                .param("fhVigHasta", p.getFhVigHasta())
                .param("fhAlta", p.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", p.getIdUserAlta(), Types.CHAR)
                .update(claves);
        return buscarPoliticaPorId(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<NumPolitica> buscarPoliticaPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_POLITICA + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcTalonarioRepository::mapearPolitica).optional();
    }

    @Override
    public Optional<NumPolitica> buscarPoliticaPorCodigo(String codigo) {
        return jdbc.sql(SQL_SELECT_POLITICA + " WHERE codigo = :codigo")
                .param("codigo", codigo).query(JdbcTalonarioRepository::mapearPolitica).optional();
    }

    @Override
    public List<NumPolitica> listarPoliticasActivas() {
        return jdbc.sql(SQL_SELECT_POLITICA + " WHERE si_activa = TRUE ORDER BY id")
                .query(JdbcTalonarioRepository::mapearPolitica).list();
    }

    @Override
    public boolean existePoliticaCodigo(String codigo) {
        return buscarPoliticaPorCodigo(codigo).isPresent();
    }

    // ---- Talonario ----

    @Override
    public NumTalonario guardarTalonario(NumTalonario t) {
        if (t.getId() != null && existePorId("num_talonario", t.getId())) {
            int filas = jdbc.sql("""
                    UPDATE num_talonario SET si_activo=:siActivo, si_bloqueado=:siBloqueado,
                        cod_desbloqueo=:codDesbloqueo, version_row = version_row + 1
                    WHERE id=:id AND version_row=:versionRow
                    """)
                    .param("siActivo", t.isSiActivo(), Types.BOOLEAN)
                    .param("siBloqueado", t.isSiBloqueado(), Types.BOOLEAN)
                    .param("codDesbloqueo", t.getCodDesbloqueo())
                    .param("id", t.getId(), Types.BIGINT)
                    .param("versionRow", t.getVersionRow(), Types.INTEGER)
                    .update();
            if (filas == 0) {
                throw new ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException(
                        "NumTalonario", t.getId(), -1, t.getVersionRow());
            }
            return buscarTalonarioPorId(t.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO num_talonario (
                    politica_id, codigo, descripcion, tipo_talonario, clase_talonario, anio, serie,
                    nro_desde, nro_hasta, nombre_secuencia, ultimo_nro_emitido, si_activo, si_bloqueado,
                    cod_desbloqueo, fh_alta, id_user_alta
                ) VALUES (
                    :politicaId, :codigo, :descripcion, :tipoTalonario, :claseTalonario, :anio, :serie,
                    :nroDesde, :nroHasta, :nombreSecuencia, 0, :siActivo, :siBloqueado,
                    :codDesbloqueo, :fhAlta, :idUserAlta
                )
                """)
                .param("politicaId", t.getPoliticaId(), Types.BIGINT)
                .param("codigo", t.getCodigo())
                .param("descripcion", t.getDescripcion())
                .param("tipoTalonario", tipoTalonarioCodigo(t.getTipoTalonario()), Types.SMALLINT)
                .param("claseTalonario", t.getClaseTalonario().codigo(), Types.SMALLINT)
                .param("anio", t.getAnio(), Types.SMALLINT)
                .param("serie", t.getSerie())
                .param("nroDesde", t.getNroDesde(), Types.INTEGER)
                .param("nroHasta", t.getNroHasta(), Types.INTEGER)
                .param("nombreSecuencia", t.getNombreSecuencia())
                .param("siActivo", t.isSiActivo(), Types.BOOLEAN)
                .param("siBloqueado", t.isSiBloqueado(), Types.BOOLEAN)
                .param("codDesbloqueo", t.getCodDesbloqueo())
                .param("fhAlta", t.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", t.getIdUserAlta(), Types.CHAR)
                .update(claves);
        return buscarTalonarioPorId(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<NumTalonario> buscarTalonarioPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_TALONARIO + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcTalonarioRepository::mapearTalonario).optional();
    }

    @Override
    public Optional<NumTalonario> buscarTalonarioPorCodigo(String codigo) {
        return jdbc.sql(SQL_SELECT_TALONARIO + " WHERE codigo = :codigo")
                .param("codigo", codigo).query(JdbcTalonarioRepository::mapearTalonario).optional();
    }

    @Override
    public Optional<NumTalonario> buscarTalonarioPorNombreSecuencia(String nombreSecuencia) {
        return jdbc.sql(SQL_SELECT_TALONARIO + " WHERE nombre_secuencia = :n")
                .param("n", nombreSecuencia).query(JdbcTalonarioRepository::mapearTalonario).optional();
    }

    @Override
    public List<NumTalonario> listarTalonariosActivos() {
        return jdbc.sql(SQL_SELECT_TALONARIO + " WHERE si_activo = TRUE ORDER BY id")
                .query(JdbcTalonarioRepository::mapearTalonario).list();
    }

    @Override
    public boolean existeTalonarioCodigo(String codigo) {
        return buscarTalonarioPorCodigo(codigo).isPresent();
    }

    @Override
    public boolean existeNombreSecuencia(String nombreSecuencia) {
        return buscarTalonarioPorNombreSecuencia(nombreSecuencia).isPresent();
    }

    // ---- Ambito ----

    @Override
    public NumTalonarioAmbito guardarAmbito(NumTalonarioAmbito a) {
        if (a.getId() != null && existePorId("num_talonario_ambito", a.getId())) {
            jdbc.sql("UPDATE num_talonario_ambito SET si_activo=:siActivo, fh_hasta=:fhHasta WHERE id=:id")
                    .param("siActivo", a.isSiActivo(), Types.BOOLEAN)
                    .param("fhHasta", a.getFhHasta())
                    .param("id", a.getId(), Types.BIGINT)
                    .update();
            return buscarAmbitoPorId(a.getId()).orElseThrow();
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO num_talonario_ambito (
                    talonario_id, clase_talonario, tipo_docu, tipo_acta, id_dep, ver_dep,
                    alcance, prioridad, fh_desde, fh_hasta, si_activo, fh_alta, id_user_alta
                ) VALUES (
                    :talonarioId, :claseTalonario, :tipoDocu, :tipoActa, :idDep, :verDep,
                    :alcance, :prioridad, :fhDesde, :fhHasta, :siActivo, :fhAlta, :idUserAlta
                )
                """)
                .param("talonarioId", a.getTalonarioId(), Types.BIGINT)
                .param("claseTalonario", a.getClaseTalonario().codigo(), Types.SMALLINT)
                .param("tipoDocu", a.getTipoDocu(), Types.SMALLINT)
                .param("tipoActa", a.getTipoActa(), Types.SMALLINT)
                .param("idDep", a.getIdDep(), Types.BIGINT)
                .param("verDep", a.getVerDep(), Types.SMALLINT)
                .param("alcance", a.getAlcance().codigo(), Types.SMALLINT)
                .param("prioridad", a.getPrioridad(), Types.SMALLINT)
                .param("fhDesde", a.getFhDesde())
                .param("fhHasta", a.getFhHasta())
                .param("siActivo", a.isSiActivo(), Types.BOOLEAN)
                .param("fhAlta", a.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", a.getIdUserAlta(), Types.CHAR)
                .update(claves);
        return buscarAmbitoPorId(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<NumTalonarioAmbito> buscarAmbitoPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_AMBITO + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcTalonarioRepository::mapearAmbito).optional();
    }

    @Override
    public List<NumTalonarioAmbito> listarAmbitosPorTalonario(Long talonarioId) {
        return jdbc.sql(SQL_SELECT_AMBITO + " WHERE talonario_id = :id ORDER BY id")
                .param("id", talonarioId, Types.BIGINT).query(JdbcTalonarioRepository::mapearAmbito).list();
    }

    @Override
    public List<NumTalonarioAmbito> listarAmbitosActivos() {
        return jdbc.sql(SQL_SELECT_AMBITO + " WHERE si_activo = TRUE ORDER BY id")
                .query(JdbcTalonarioRepository::mapearAmbito).list();
    }

    @Override
    public void bloquearParaExclusividadElectronicaActa(AlcanceTalonario alcance, Long idDep) {
        if (alcance == AlcanceTalonario.GLOBAL) {
            jdbc.sql("SELECT id FROM fal_dependencia FOR UPDATE").query(Long.class).list();
        } else if (alcance == AlcanceTalonario.DEPENDENCIA && idDep != null) {
            jdbc.sql("SELECT id FROM fal_dependencia WHERE id = :id FOR UPDATE")
                    .param("id", idDep, Types.BIGINT)
                    .query(Long.class)
                    .list();
        }
    }

    // ---- Asignacion inspector (circuito manual) ----

    @Override
    public NumTalonarioInspector guardarAsignacionInspector(NumTalonarioInspector a) {
        if (a.getId() != null && existePorId("num_talonario_inspector", a.getId())) {
            return actualizarAsignacionInspector(a);
        }
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO num_talonario_inspector (
                    id_talonario, id_insp, ver_insp, fh_entrega, id_user_entrega,
                    fh_devolucion, id_user_devolucion, estado_asignacion, si_activa, fh_alta, id_user_alta
                ) VALUES (
                    :idTalonario, :idInsp, :verInsp, :fhEntrega, :idUserEntrega,
                    :fhDevolucion, :idUserDevolucion, :estado, :siActiva, :fhAlta, :idUserAlta
                )
                """)
                .param("idTalonario", a.getIdTalonario(), Types.BIGINT)
                .param("idInsp", a.getIdInsp(), Types.BIGINT)
                .param("verInsp", a.getVerInsp(), Types.SMALLINT)
                .param("fhEntrega", a.getFhEntrega(), Types.TIMESTAMP)
                .param("idUserEntrega", a.getIdUserEntrega(), Types.CHAR)
                .param("fhDevolucion", a.getFhDevolucion(), Types.TIMESTAMP)
                .param("idUserDevolucion", a.getIdUserDevolucion(), Types.CHAR)
                .param("estado", a.getEstadoAsignacion().codigo(), Types.SMALLINT)
                .param("siActiva", a.isSiActiva(), Types.BOOLEAN)
                .param("fhAlta", a.getFhEntrega(), Types.TIMESTAMP)
                .param("idUserAlta", a.getIdUserEntrega(), Types.CHAR)
                .update(claves);
        return buscarAsignacionInspectorPorId(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public NumTalonarioInspector actualizarAsignacionInspector(NumTalonarioInspector a) {
        jdbc.sql("""
                UPDATE num_talonario_inspector
                SET fh_devolucion=:fhDevolucion, id_user_devolucion=:idUserDevolucion,
                    estado_asignacion=:estado, si_activa=:siActiva
                WHERE id=:id
                """)
                .param("fhDevolucion", a.getFhDevolucion(), Types.TIMESTAMP)
                .param("idUserDevolucion", a.getIdUserDevolucion(), Types.CHAR)
                .param("estado", a.getEstadoAsignacion().codigo(), Types.SMALLINT)
                .param("siActiva", a.isSiActiva(), Types.BOOLEAN)
                .param("id", a.getId(), Types.BIGINT)
                .update();
        return buscarAsignacionInspectorPorId(a.getId()).orElseThrow();
    }

    @Override
    public Optional<NumTalonarioInspector> buscarAsignacionInspectorPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_ASIGNACION + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcTalonarioRepository::mapearAsignacion).optional();
    }

    @Override
    public Optional<NumTalonarioInspector> buscarAsignacionActivaPorTalonario(Long idTalonario) {
        return jdbc.sql(SQL_SELECT_ASIGNACION + " WHERE id_talonario = :id AND si_activa = TRUE")
                .param("id", idTalonario, Types.BIGINT).query(JdbcTalonarioRepository::mapearAsignacion).optional();
    }

    @Override
    public List<NumTalonarioInspector> listarAsignacionesActivas() {
        return jdbc.sql(SQL_SELECT_ASIGNACION + " WHERE si_activa = TRUE ORDER BY id")
                .query(JdbcTalonarioRepository::mapearAsignacion).list();
    }

    @Override
    public List<NumTalonarioInspector> listarAsignacionesPorInspector(Long idInsp) {
        return jdbc.sql(SQL_SELECT_ASIGNACION + " WHERE id_insp = :id ORDER BY id")
                .param("id", idInsp, Types.BIGINT).query(JdbcTalonarioRepository::mapearAsignacion).list();
    }

    @Override
    public List<NumTalonarioInspector> listarAsignacionesPorTalonario(Long idTalonario) {
        return jdbc.sql(SQL_SELECT_ASIGNACION + " WHERE id_talonario = :id ORDER BY id")
                .param("id", idTalonario, Types.BIGINT).query(JdbcTalonarioRepository::mapearAsignacion).list();
    }

    // ---- Movimiento (append-only; circuito manual) ----

    @Override
    public NumTalonarioMovimiento guardarMovimiento(NumTalonarioMovimiento m) {
        KeyHolder claves = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO num_talonario_movimiento (
                    id_talonario, nro_talonario, estado_numero, motivo_anulacion,
                    acta_id, documento_id, id_dep, ver_dep, id_insp, ver_insp,
                    fh_movimiento, id_user_movimiento
                ) VALUES (
                    :idTalonario, :nroTalonario, :estado, :motivoAnulacion,
                    :actaId, :documentoId, :idDep, :verDep, :idInsp, :verInsp,
                    :fhMovimiento, :idUserMovimiento
                )
                """)
                .param("idTalonario", m.getIdTalonario(), Types.BIGINT)
                .param("nroTalonario", m.getNroTalonario(), Types.INTEGER)
                .param("estado", m.getEstadoNumero().codigo(), Types.SMALLINT)
                .param("motivoAnulacion", m.getMotivoAnulacion() == null ? null : m.getMotivoAnulacion().codigo(), Types.SMALLINT)
                .param("actaId", m.getActaId(), Types.BIGINT)
                .param("documentoId", m.getDocumentoId(), Types.BIGINT)
                .param("idDep", m.getIdDep(), Types.BIGINT)
                .param("verDep", m.getVerDep(), Types.SMALLINT)
                .param("idInsp", m.getIdInsp(), Types.BIGINT)
                .param("verInsp", m.getVerInsp(), Types.SMALLINT)
                .param("fhMovimiento", m.getFhMovimiento(), Types.TIMESTAMP)
                .param("idUserMovimiento", m.getIdUserMovimiento(), Types.CHAR)
                .update(claves);
        return buscarMovimientoPorId(claves.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<NumTalonarioMovimiento> buscarMovimientoPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.sql(SQL_SELECT_MOVIMIENTO + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(JdbcTalonarioRepository::mapearMovimiento).optional();
    }

    @Override
    public Optional<NumTalonarioMovimiento> buscarMovimientoPorTalonarioYNro(Long idTalonario, int nroTalonario) {
        return jdbc.sql(SQL_SELECT_MOVIMIENTO + " WHERE id_talonario = :id AND nro_talonario = :nro")
                .param("id", idTalonario, Types.BIGINT).param("nro", nroTalonario, Types.INTEGER)
                .query(JdbcTalonarioRepository::mapearMovimiento).optional();
    }

    @Override
    public List<NumTalonarioMovimiento> listarMovimientosPorTalonario(Long idTalonario) {
        return jdbc.sql(SQL_SELECT_MOVIMIENTO + " WHERE id_talonario = :id ORDER BY id")
                .param("id", idTalonario, Types.BIGINT).query(JdbcTalonarioRepository::mapearMovimiento).list();
    }

    @Override
    public boolean existeMovimientoTalonarioNumero(Long idTalonario, int nroTalonario) {
        return buscarMovimientoPorTalonarioYNro(idTalonario, nroTalonario).isPresent();
    }

    @Override
    public List<Integer> buscarNumerosFaltantesEnRango(Long idTalonario, int nroDesde, int nroHasta) {
        List<Integer> usados = jdbc.sql("SELECT nro_talonario FROM num_talonario_movimiento WHERE id_talonario = :id")
                .param("id", idTalonario, Types.BIGINT).query(Integer.class).list();
        List<Integer> faltantes = new ArrayList<>();
        for (int n = nroDesde; n <= nroHasta; n++) {
            if (!usados.contains(n)) faltantes.add(n);
        }
        return faltantes;
    }

    // ---- Utilidades ----

    private boolean existePorId(String tabla, Long id) {
        return jdbc.sql("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .param("id", id, Types.BIGINT).query(Integer.class).single() > 0;
    }

    private static short tipoTalonarioCodigo(TipoTalonario t) {
        return t == TipoTalonario.ELECTRONICO ? (short) 1 : (short) 2;
    }

    private static TipoTalonario tipoTalonarioDesdeCodigo(short codigo) {
        return codigo == 1 ? TipoTalonario.ELECTRONICO : TipoTalonario.MANUAL_FISICO;
    }

    private static NumPolitica mapearPolitica(ResultSet rs, int fila) throws SQLException {
        NumPolitica p = new NumPolitica(
                rs.getObject("id", Long.class),
                rs.getString("codigo"),
                rs.getString("descripcion"),
                ClaseNumeracion.desdeCodigo(rs.getShort("clase_numeracion")),
                rs.getBoolean("si_reinicio_anual"),
                rs.getBoolean("si_incluye_prefijo"),
                rs.getString("prefijo"),
                rs.getBoolean("si_incluye_anio"),
                (Short) rs.getObject("formato_anio"),
                rs.getBoolean("si_incluye_serie"),
                (Short) rs.getObject("longitud_nro"),
                rs.getString("formato_visible"),
                rs.getBoolean("si_activa"),
                rs.getObject("fh_vig_desde", LocalDate.class),
                rs.getObject("fh_vig_hasta", LocalDate.class),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        p.setApodo(rs.getString("apodo"));
        return p;
    }

    private static NumTalonario mapearTalonario(ResultSet rs, int fila) throws SQLException {
        NumTalonario t = new NumTalonario(
                rs.getObject("id", Long.class),
                rs.getInt("version_row"),
                rs.getObject("politica_id", Long.class),
                rs.getString("codigo"),
                rs.getString("descripcion"),
                tipoTalonarioDesdeCodigo(rs.getShort("tipo_talonario")),
                ClaseNumeracion.desdeCodigo(rs.getShort("clase_talonario")),
                (Short) rs.getObject("anio"),
                rs.getString("serie"),
                rs.getInt("nro_desde"),
                (Integer) rs.getObject("nro_hasta"),
                rs.getString("nombre_secuencia"),
                rs.getBoolean("si_activo"),
                rs.getBoolean("si_bloqueado"),
                rs.getString("cod_desbloqueo"),
                null,
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
        return t;
    }

    private static NumTalonarioAmbito mapearAmbito(ResultSet rs, int fila) throws SQLException {
        return new NumTalonarioAmbito(
                rs.getObject("id", Long.class),
                rs.getObject("talonario_id", Long.class),
                ClaseNumeracion.desdeCodigo(rs.getShort("clase_talonario")),
                (Short) rs.getObject("tipo_docu"),
                (Short) rs.getObject("tipo_acta"),
                rs.getObject("id_dep", Long.class),
                (Short) rs.getObject("ver_dep"),
                AlcanceTalonario.desdeCodigo(rs.getShort("alcance")),
                rs.getShort("prioridad"),
                rs.getObject("fh_desde", LocalDate.class),
                rs.getObject("fh_hasta", LocalDate.class),
                rs.getBoolean("si_activo"),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));
    }

    private static NumTalonarioInspector mapearAsignacion(ResultSet rs, int fila) throws SQLException {
        return new NumTalonarioInspector(
                rs.getObject("id", Long.class),
                rs.getObject("id_talonario", Long.class),
                rs.getObject("id_insp", Long.class),
                rs.getShort("ver_insp"),
                rs.getObject("fh_entrega", LocalDateTime.class),
                rs.getString("id_user_entrega"),
                rs.getObject("fh_devolucion", LocalDateTime.class),
                rs.getString("id_user_devolucion"),
                EstadoAsignacionTalonario.desdeCodigo(rs.getShort("estado_asignacion")),
                rs.getBoolean("si_activa"),
                (Long) rs.getObject("talonario_id_activo"));
    }

    private static NumTalonarioMovimiento mapearMovimiento(ResultSet rs, int fila) throws SQLException {
        Short motivo = (Short) rs.getObject("motivo_anulacion");
        return new NumTalonarioMovimiento(
                rs.getObject("id", Long.class),
                rs.getObject("id_talonario", Long.class),
                rs.getInt("nro_talonario"),
                EstadoNumeroTalonario.desdeCodigo(rs.getShort("estado_numero")),
                motivo == null ? null : MotivoAnulacionTalonario.desdeCodigo(motivo),
                null,
                rs.getObject("acta_id", Long.class),
                rs.getObject("documento_id", Long.class),
                rs.getObject("id_dep", Long.class),
                (Short) rs.getObject("ver_dep"),
                rs.getObject("id_insp", Long.class),
                (Short) rs.getObject("ver_insp"),
                rs.getObject("fh_movimiento", LocalDateTime.class),
                rs.getString("id_user_movimiento"));
    }
}
