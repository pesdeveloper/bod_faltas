package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adaptador JDBC de {@link ActaSnapshotRepository} sobre {@code fal_acta_snapshot}.
 *
 * <p>Creado en WAVE 1A2-B2. Antes de este slice el puerto solo tenia adaptador InMemory, de
 * modo que el snapshot del alta quedaba en memoria mientras el acta iba a MariaDB: un rollback
 * dejaba una proyeccion operativa viva de un acta que no existia, y esa proyeccion es
 * exactamente lo que alimenta bandejas. Como la tabla y el contrato ya existian, el adaptador
 * se implementa aca en lugar de degradar {@code DECISION_OBS-TX-01}.
 *
 * <p>Estilo heredado de {@code JdbcActaEventoRepository} y {@code JdbcActaRepository}:
 * {@code JdbcClient} sobre el DataSource principal, SQL explicito en constantes, parametros
 * nombrados y RowMapper estatico privado. Sin {@code SELECT *}, sin JPA, sin ORM.
 *
 * <h2>Identidad: la PK es el id del acta</h2>
 *
 * {@code fal_acta_snapshot.id_acta} es {@code BIGINT} <b>sin</b> {@code AUTO_INCREMENT} y es a
 * la vez PK y clave ajena a {@code fal_acta.id}. La proyeccion es uno a uno con el acta, de
 * modo que no hay identidad propia que generar: {@link org.springframework.jdbc.support.GeneratedKeyHolder}
 * no participa y el INSERT informa {@code id_acta} explicitamente.
 *
 * <h2>OCC estricto y por que no hay upsert de motor</h2>
 *
 * {@code DECISION_DDL-SNAP-01}. Mismo contrato que el adaptador InMemory:
 *
 * <pre>
 * fila inexistente + versionRow == 0  -&gt; INSERT; la persistida queda en version_row = 0
 * fila existente   + version coincide -&gt; UPDATE; la persistida queda en versionRow + 1
 * cualquier desajuste                 -&gt; ConcurrenciaConflictoException
 * </pre>
 *
 * <p>Lo que discrimina alta de actualizacion es la <b>existencia de la fila</b> y no el valor de
 * {@code versionRow}: el cero es tanto la version de una proyeccion nueva como la de una fila
 * recien insertada. El {@code INSERT} va primero y el {@code UPDATE} despues, y el orden no es
 * indiferente: el inverso produce un interbloqueo reproducible entre dos altas concurrentes de
 * actas distintas. El analisis completo esta en {@link #guardar(FalActaSnapshot)}.
 *
 * <p>No se usa {@code REPLACE} ni {@code ON DUPLICATE KEY UPDATE}. No es solo que esten
 * prohibidos: un upsert de motor haria indistinguible el alta de la correccion y, sobre todo,
 * <b>anularia el OCC</b>, porque escribiria la fila cualquiera fuera la version almacenada. La
 * deteccion de conflicto es el proposito de la operacion, no un efecto colateral.
 *
 * <p>El desajuste se detecta de forma atomica y sin lectura previa que decida el camino: lo
 * detectan el {@code WHERE} sobre {@code version_row} y, en su defecto, la PK. El adaptador
 * InMemory lee primero y compara despues porque puede hacerlo dentro de un bloque sincronizado;
 * aca la comparacion la hace el motor, que es mas fuerte, no mas debil.
 *
 * <p>Cuando hay conflicto se lee la version almacenada solo para poder informarla en el
 * mensaje. Esa lectura se paga unicamente en el camino de fallo, igual que
 * {@code JdbcActaRepository.desempatarActualizacionSinEfecto}.
 *
 * <h2>Mapeo 30 de 30 columnas</h2>
 *
 * Las 30 columnas fisicas tienen destino en un campo de {@code FalActaSnapshot} y viceversa.
 * Los enums viajan por {@code codigo()} y se leen con su factory canonica: {@code CHAR(4)} para
 * bloque, estado procesal y situacion administrativa; {@code CHAR(6)} para el ultimo evento;
 * {@code VARCHAR} para bandeja y accion pendiente; {@code SMALLINT} para resultado final,
 * estado y tipo de valorizacion; {@code SMALLINT} para el motivo de paralizacion. En ningun
 * caso se usa {@code ordinal()}, {@code name()} ni {@code toString()}.
 *
 * <p>{@code monto_operativo_vigente} es {@code DECIMAL(14,2)} y se persiste tal como lo entrega
 * el dominio: no se redondea ni se reescala aca. {@code DECISION_DDL-SNAP-02} mantiene la
 * economia de pagos fuera de esta tabla, de modo que el monto es valorizacion UX del acta y no
 * un dato economico.
 *
 * <p><b>Transaccionalidad.</b> Este adaptador no abre transacciones. La frontera vive en
 * {@code ActaService.labrar}. {@code ultima_actualizacion} se persiste tal como lo entrega el
 * dominio; no se usa {@code NOW()} ni {@code CURRENT_TIMESTAMP}.
 */
@Repository
@Profile("jdbc")
public class JdbcActaSnapshotRepository implements ActaSnapshotRepository {

    private static final String COLUMNAS = """
            id_acta, version_row, bloque_actual, est_proc_act, sit_adm_act,
            resultado_final, cod_bandeja, sub_bandeja, accion_pendiente,
            tiene_documentos, tiene_docs_pendientes_firma, tiene_docs_listos_para_notificar,
            tiene_notificaciones, notificacion_en_curso, bloqueado_cierre, id_docu_ult,
            bloqueado_notificacion, valorizacion_operativa_id, estado_valorizacion_operativa,
            tipo_valorizacion_operativa, monto_operativo_vigente, si_monto_confirmado,
            licencia_provincia_txt, licencia_unidad_txt, nomenclatura_resumen,
            id_bie_i, id_bie_c, motivo_paralizacion_act, ultimo_evento_tipo,
            ultima_actualizacion
            """;

    /**
     * Alta de la proyeccion. {@code version_row} se informa con el literal 0 en lugar de
     * delegar en el DEFAULT del DDL, para que la version inicial de {@code DECISION_DDL-SNAP-01}
     * sea explicita en el SQL.
     */
    private static final String SQL_INSERT = """
            INSERT INTO fal_acta_snapshot (
                id_acta, version_row, bloque_actual, est_proc_act, sit_adm_act,
                resultado_final, cod_bandeja, sub_bandeja, accion_pendiente,
                tiene_documentos, tiene_docs_pendientes_firma, tiene_docs_listos_para_notificar,
                tiene_notificaciones, notificacion_en_curso, bloqueado_cierre, id_docu_ult,
                bloqueado_notificacion, valorizacion_operativa_id, estado_valorizacion_operativa,
                tipo_valorizacion_operativa, monto_operativo_vigente, si_monto_confirmado,
                licencia_provincia_txt, licencia_unidad_txt, nomenclatura_resumen,
                id_bie_i, id_bie_c, motivo_paralizacion_act, ultimo_evento_tipo,
                ultima_actualizacion
            ) VALUES (
                :idActa, 0, :bloqueActual, :estProcAct, :sitAdmAct,
                :resultadoFinal, :codBandeja, :subBandeja, :accionPendiente,
                :tieneDocumentos, :tieneDocsPendientesFirma, :tieneDocsListosParaNotificar,
                :tieneNotificaciones, :notificacionEnCurso, :bloqueadoCierre, :idDocuUlt,
                :bloqueadoNotificacion, :valorizacionOperativaId, :estadoValorizacionOperativa,
                :tipoValorizacionOperativa, :montoOperativoVigente, :siMontoConfirmado,
                :licenciaProvinciaTxt, :licenciaUnidadTxt, :nomenclaturaResumen,
                :idBieI, :idBieC, :motivoParalizacionAct, :ultimoEventoTipo,
                :ultimaActualizacion
            )
            """;

    /**
     * Actualizacion optimista. El SET lista las 28 columnas mutables completas: no se construye
     * dinamicamente omitiendo nulos, de modo que llevar una columna nullable a NULL se
     * persiste. Es lo que hace que una bandeja o una accion pendiente que dejan de aplicar se
     * limpien de verdad en lugar de quedar residuales.
     */
    private static final String SQL_UPDATE_OCC = """
            UPDATE fal_acta_snapshot
               SET version_row = version_row + 1,
                   bloque_actual = :bloqueActual,
                   est_proc_act = :estProcAct,
                   sit_adm_act = :sitAdmAct,
                   resultado_final = :resultadoFinal,
                   cod_bandeja = :codBandeja,
                   sub_bandeja = :subBandeja,
                   accion_pendiente = :accionPendiente,
                   tiene_documentos = :tieneDocumentos,
                   tiene_docs_pendientes_firma = :tieneDocsPendientesFirma,
                   tiene_docs_listos_para_notificar = :tieneDocsListosParaNotificar,
                   tiene_notificaciones = :tieneNotificaciones,
                   notificacion_en_curso = :notificacionEnCurso,
                   bloqueado_cierre = :bloqueadoCierre,
                   id_docu_ult = :idDocuUlt,
                   bloqueado_notificacion = :bloqueadoNotificacion,
                   valorizacion_operativa_id = :valorizacionOperativaId,
                   estado_valorizacion_operativa = :estadoValorizacionOperativa,
                   tipo_valorizacion_operativa = :tipoValorizacionOperativa,
                   monto_operativo_vigente = :montoOperativoVigente,
                   si_monto_confirmado = :siMontoConfirmado,
                   licencia_provincia_txt = :licenciaProvinciaTxt,
                   licencia_unidad_txt = :licenciaUnidadTxt,
                   nomenclatura_resumen = :nomenclaturaResumen,
                   id_bie_i = :idBieI,
                   id_bie_c = :idBieC,
                   motivo_paralizacion_act = :motivoParalizacionAct,
                   ultimo_evento_tipo = :ultimoEventoTipo,
                   ultima_actualizacion = :ultimaActualizacion
             WHERE id_acta = :idActa
               AND version_row = :versionEsperada
            """;

    private static final String SQL_POR_ACTA = "SELECT " + COLUMNAS + """
            FROM fal_acta_snapshot
            WHERE id_acta = :idActa
            """;

    private static final String SQL_VERSION_POR_ACTA = """
            SELECT version_row FROM fal_acta_snapshot WHERE id_acta = :idActa
            """;

    private final JdbcClient jdbc;

    public JdbcActaSnapshotRepository(JdbcClient jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc es obligatorio");
        }
        this.jdbc = jdbc;
    }

    // =====================================================================
    // ESCRITURA
    // =====================================================================

    /**
     * Alta o actualizacion optimista, con la misma semantica exacta del adaptador InMemory.
     *
     * <p><b>Lo que discrimina alta de actualizacion es la existencia de la fila, no el valor de
     * {@code versionRow}.</b> Es la distincion que importa: {@code version_row = 0} es tanto la
     * version que trae una proyeccion nueva como la que tiene una fila recien insertada, de modo
     * que usar el cero como discriminador convierte la primera actualizacion de un snapshot en un
     * intento de alta duplicada.
     *
     * <pre>
     * versionEntrante == 0 -&gt; INSERT
     *                          exito        -&gt; no existia; la fila queda en version 0
     *                          DuplicateKey -&gt; ya existe; se resuelve como actualizacion
     * UPDATE ... WHERE id_acta = :id AND version_row = :versionEsperada
     *                          1 fila  -&gt; la version efectiva es la entrante mas uno
     *                          0 filas -&gt; la version almacenada no es la esperada: conflicto
     * </pre>
     *
     * <h3>Por que el INSERT va primero</h3>
     *
     * <p>El orden importa y no por rendimiento: <b>el orden inverso produce un interbloqueo
     * reproducible</b>. Con {@code UPDATE} primero, una actualizacion que no encuentra la fila
     * toma un bloqueo de hueco sobre la clave inexistente. Dos altas concurrentes de actas
     * distintas caen en el mismo hueco del indice primario, los dos bloqueos de hueco son
     * compatibles entre si, y despues cada transaccion necesita un bloqueo de intencion de
     * insercion que el hueco de la otra le niega: InnoDB corta una de las dos con
     * {@code Deadlock found when trying to get lock}. El caso aparecio de verdad al ejecutar las
     * dos altas independientes en paralelo.
     *
     * <p>Con {@code INSERT} primero no hay hueco tomado por adelantado. Dos inserciones de claves
     * primarias distintas solo toman bloqueos de intencion de insercion, que no se excluyen entre
     * si, de modo que el alta concurrente de dos actas distintas no puede interbloquearse. La
     * contencion que queda es la de dos escrituras sobre el <b>mismo</b> snapshot, y ahi el
     * desenlace correcto es justamente el conflicto de version que la operacion promete.
     *
     * <p>No se agrega reintento, ni {@code backoff}, ni repeticion silenciosa: la causa era de
     * diseno y se corrige en el diseno.
     *
     * <p>Devuelve el mismo objeto recibido, con {@code versionRow} ya avanzado en la rama de
     * actualizacion. Es la semantica del adaptador InMemory, que tambien muta el argumento en esa
     * rama: cambiarla aca romperia la paridad del oraculo.
     */
    @Override
    public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot es obligatorio");
        }
        int versionEntrante = snapshot.getVersionRow();

        if (versionEntrante == 0) {
            try {
                vincularColumnas(jdbc.sql(SQL_INSERT), snapshot).update();
                return snapshot;
            } catch (DuplicateKeyException filaYaExiste) {
                // La PK es id_acta: el duplicado significa que la proyeccion ya existe. No es un
                // duplicado funcional; es una actualizacion, y la resuelve el UPDATE de abajo.
            }
        }

        int filas = vincularColumnas(jdbc.sql(SQL_UPDATE_OCC), snapshot)
                .param("versionEsperada", versionEntrante, Types.INTEGER)
                .update();
        if (filas == 0) {
            throw conflicto(snapshot.getIdActa(), versionEntrante);
        }
        snapshot.setVersionRow(versionEntrante + 1);
        return snapshot;
    }

    /**
     * Construye el conflicto informando la version realmente almacenada. Si la fila no existe
     * se informa 0, que es la version que un alta habria esperado encontrar libre.
     */
    private ConcurrenciaConflictoException conflicto(Long idActa, int versionEntrante) {
        Optional<Integer> almacenada = jdbc.sql(SQL_VERSION_POR_ACTA)
                .param("idActa", idActa, Types.BIGINT)
                .query(Integer.class)
                .optional();
        return new ConcurrenciaConflictoException(
                "FalActaSnapshot", idActa, almacenada.orElse(0), versionEntrante);
    }

    // =====================================================================
    // LECTURA
    // =====================================================================

    @Override
    public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
        if (idActa == null) {
            return Optional.empty();
        }
        return jdbc.sql(SQL_POR_ACTA)
                .param("idActa", idActa, Types.BIGINT)
                .query(JdbcActaSnapshotRepository::mapear)
                .optional();
    }

    // =====================================================================
    // VINCULACION DE PARAMETROS
    // =====================================================================

    /**
     * Las 28 columnas mutables mas {@code id_acta}, compartidas por el INSERT y por el UPDATE.
     * Tenerlas en un unico lugar evita dos listas de parametros que haya que mantener
     * sincronizadas.
     */
    private static JdbcClient.StatementSpec vincularColumnas(
            JdbcClient.StatementSpec spec, FalActaSnapshot s) {
        return spec
                .param("idActa", s.getIdActa(), Types.BIGINT)
                .param("bloqueActual", codigo(s.getBloqueActual()), Types.CHAR)
                .param("estProcAct", codigo(s.getEstadoProcesal()), Types.CHAR)
                .param("sitAdmAct", codigo(s.getSituacionAdministrativa()), Types.CHAR)
                .param("resultadoFinal", s.getResultadoFinal() == null
                        ? null : Short.valueOf(s.getResultadoFinal().codigo()), Types.SMALLINT)
                .param("codBandeja", codigo(s.getCodBandeja()), Types.VARCHAR)
                .param("subBandeja", s.getSubBandeja(), Types.VARCHAR)
                .param("accionPendiente", codigo(s.getAccionPendiente()), Types.VARCHAR)
                .param("tieneDocumentos", s.isTieneDocumentos())
                .param("tieneDocsPendientesFirma", s.isTieneDocsPendientesFirma())
                .param("tieneDocsListosParaNotificar", s.isTieneDocsListosParaNotificar())
                .param("tieneNotificaciones", s.isTieneNotificaciones())
                .param("notificacionEnCurso", s.isNotificacionEnCurso())
                .param("bloqueadoCierre", s.isBloqueadoCierre())
                .param("idDocuUlt", s.getIdDocuUlt(), Types.BIGINT)
                .param("bloqueadoNotificacion", s.isBloqueadoNotificacion())
                .param("valorizacionOperativaId", s.getValorizacionOperativaId(), Types.BIGINT)
                .param("estadoValorizacionOperativa", s.getEstadoValorizacionOperativa() == null
                        ? null : Short.valueOf(s.getEstadoValorizacionOperativa().codigo()),
                        Types.SMALLINT)
                .param("tipoValorizacionOperativa", s.getTipoValorizacionOperativa() == null
                        ? null : Short.valueOf(s.getTipoValorizacionOperativa().codigo()),
                        Types.SMALLINT)
                .param("montoOperativoVigente", s.getMontoOperativoVigente(), Types.DECIMAL)
                .param("siMontoConfirmado", s.isSiMontoConfirmado())
                .param("licenciaProvinciaTxt", s.getLicenciaProvinciaTxt(), Types.VARCHAR)
                .param("licenciaUnidadTxt", s.getLicenciaUnidadTxt(), Types.VARCHAR)
                .param("nomenclaturaResumen", s.getNomenclaturaResumen(), Types.VARCHAR)
                .param("idBieI", s.getIdBieI(), Types.INTEGER)
                .param("idBieC", s.getIdBieC(), Types.INTEGER)
                .param("motivoParalizacionAct", s.getMotivoParalizacionAct() == null
                        ? null : Integer.valueOf(s.getMotivoParalizacionAct().codigo()),
                        Types.SMALLINT)
                .param("ultimoEventoTipo", s.getUltimoEventoTipo() == null
                        ? null : s.getUltimoEventoTipo().codigo(), Types.CHAR)
                .param("ultimaActualizacion", s.getUltimaActualizacion(), Types.TIMESTAMP);
    }

    private static String codigo(BloqueActual bloque) {
        return bloque == null ? null : bloque.codigo();
    }

    private static String codigo(EstadoProcesalActa estado) {
        return estado == null ? null : estado.codigo();
    }

    private static String codigo(SituacionAdministrativaActa situacion) {
        return situacion == null ? null : situacion.codigo();
    }

    private static String codigo(CodigoBandeja bandeja) {
        return bandeja == null ? null : bandeja.codigo();
    }

    private static String codigo(AccionPendiente accion) {
        return accion == null ? null : accion.codigo();
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static FalActaSnapshot mapear(ResultSet rs, int fila) throws SQLException {
        FalActaSnapshot s = new FalActaSnapshot(rs.getObject("id_acta", Long.class));
        s.setVersionRow(rs.getInt("version_row"));
        s.setBloqueActual(bloque(rs.getString("bloque_actual")));
        s.setEstadoProcesal(estadoProcesal(rs.getString("est_proc_act")));
        s.setSituacionAdministrativa(situacion(rs.getString("sit_adm_act")));
        s.setResultadoFinal(resultadoFinal(rs.getObject("resultado_final", Short.class)));
        s.setCodBandeja(bandeja(rs.getString("cod_bandeja")));
        s.setSubBandeja(rs.getString("sub_bandeja"));
        s.setAccionPendiente(accion(rs.getString("accion_pendiente")));
        s.setTieneDocumentos(rs.getBoolean("tiene_documentos"));
        s.setTieneDocsPendientesFirma(rs.getBoolean("tiene_docs_pendientes_firma"));
        s.setTieneDocsListosParaNotificar(rs.getBoolean("tiene_docs_listos_para_notificar"));
        s.setTieneNotificaciones(rs.getBoolean("tiene_notificaciones"));
        s.setNotificacionEnCurso(rs.getBoolean("notificacion_en_curso"));
        s.setBloqueadoCierre(rs.getBoolean("bloqueado_cierre"));
        s.setIdDocuUlt(rs.getObject("id_docu_ult", Long.class));
        s.setBloqueadoNotificacion(rs.getBoolean("bloqueado_notificacion"));
        s.setValorizacionOperativaId(rs.getObject("valorizacion_operativa_id", Long.class));
        s.setEstadoValorizacionOperativa(
                estadoValorizacion(rs.getObject("estado_valorizacion_operativa", Short.class)));
        s.setTipoValorizacionOperativa(
                tipoValorizacion(rs.getObject("tipo_valorizacion_operativa", Short.class)));
        s.setMontoOperativoVigente(rs.getObject("monto_operativo_vigente", BigDecimal.class));
        s.setSiMontoConfirmado(rs.getBoolean("si_monto_confirmado"));
        s.setLicenciaProvinciaTxt(rs.getString("licencia_provincia_txt"));
        s.setLicenciaUnidadTxt(rs.getString("licencia_unidad_txt"));
        s.setNomenclaturaResumen(rs.getString("nomenclatura_resumen"));
        s.setIdBieI(rs.getObject("id_bie_i", Integer.class));
        s.setIdBieC(rs.getObject("id_bie_c", Integer.class));
        s.setMotivoParalizacionAct(
                motivoParalizacion(rs.getObject("motivo_paralizacion_act", Integer.class)));
        s.setUltimoEventoTipo(tipoEvento(rs.getString("ultimo_evento_tipo")));
        s.setUltimaActualizacion(rs.getObject("ultima_actualizacion", LocalDateTime.class));
        return s;
    }

    private static BloqueActual bloque(String codigo) {
        return codigo == null ? null : BloqueActual.deCodigo(codigo);
    }

    private static EstadoProcesalActa estadoProcesal(String codigo) {
        return codigo == null ? null : EstadoProcesalActa.deCodigo(codigo);
    }

    private static SituacionAdministrativaActa situacion(String codigo) {
        return codigo == null ? null : SituacionAdministrativaActa.deCodigo(codigo);
    }

    private static ResultadoFinalActa resultadoFinal(Short codigo) {
        return codigo == null ? null : ResultadoFinalActa.fromCodigo(codigo);
    }

    private static CodigoBandeja bandeja(String codigo) {
        return codigo == null ? null : CodigoBandeja.desdeCodigo(codigo);
    }

    private static AccionPendiente accion(String codigo) {
        return codigo == null ? null : AccionPendiente.desdeCodigo(codigo);
    }

    private static EstadoValorizacionActa estadoValorizacion(Short codigo) {
        return codigo == null ? null : EstadoValorizacionActa.fromCodigo(codigo);
    }

    private static TipoValorizacionActa tipoValorizacion(Short codigo) {
        return codigo == null ? null : TipoValorizacionActa.fromCodigo(codigo);
    }

    private static MotivoParalizacion motivoParalizacion(Integer codigo) {
        return codigo == null ? null : MotivoParalizacion.fromCodigo(codigo);
    }

    private static TipoEventoActa tipoEvento(String codigo) {
        return codigo == null ? null : TipoEventoActa.deCodigo(codigo);
    }
}
