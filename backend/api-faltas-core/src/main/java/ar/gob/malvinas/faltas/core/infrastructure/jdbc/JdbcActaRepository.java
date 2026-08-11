package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenCaptura;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUbicacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adaptador JDBC de ActaRepository sobre fal_acta.
 *
 * <p><b>Bean activo bajo el perfil {@code jdbc}.</b> Con ese perfil este adaptador es la
 * unica implementacion del puerto del acta y {@code InMemoryActaRepository} queda restringido
 * a {@code !jdbc}. La activacion se hizo en WAVE 1A2-B2, junto con la frontera transaccional
 * de {@code DECISION_OBS-TX-01} sobre {@code ActaService.labrar}: activarlo antes habria
 * dejado el alta escribiendo en MariaDB sin promesa de rollback conjunto.
 *
 * <p>Estilo heredado de {@code JdbcActaEventoRepository} (WAVE 0B), los catalogos de
 * WAVE 1A1 y {@code JdbcObservacionRepository} (WAVE 1A2-B0): JdbcClient sobre el
 * DataSource principal, SQL explicito en constantes, parametros nombrados y mapeo
 * manual. Sin JPA, sin ORM, sin SELECT *, sin generacion de SQL y sin framework
 * interno de mapeo.
 *
 * <h2>Mapeo 57 de 57 columnas</h2>
 *
 * Las 57 columnas fisicas de fal_acta tienen destino en un campo de FalActa: 15 las
 * recibe el constructor principal y 42 se aplican con setters despues de construir.
 * El constructor asigna valores por defecto a varios campos (bloque, estado, version,
 * origen de captura, banderas booleanas), de modo que el mapeo los sobreescribe
 * siempre con el valor leido del motor.
 *
 * <p>{@code situacion_identificacion_infractor} se persiste por
 * {@code SituacionIdentificacionInfractor.codigo()} y se lee con
 * {@code fromCodigo(short)} ({@code DECISION_DDL-ACTA-INFR-01}): nunca por
 * {@code ordinal()}, y sin COALESCE, default ni CASE que fusione significados. La
 * coherencia con {@code id_persona_infractor} la garantizan el invariante de
 * {@code FalActa} y el CHECK {@code chk_acta_infractor_coherente}; el adaptador no
 * la corrige ni la deduce.
 *
 * <p>Campos de FalActa que fal_acta no persiste, y que por lo tanto vuelven nulos en
 * un round-trip: {@code infractorNombre} e {@code infractorDocumento}. Son campos de
 * compatibilidad con la captura embebida previa; los datos del infractor viven en
 * fal_persona a traves de {@code id_persona_infractor}. Es la decision de modelo
 * vigente, no una perdida accidental, igual que {@code tipoObs} en
 * {@code JdbcObservacionRepository}.
 *
 * <h2>Identidad y OCC</h2>
 *
 * {@code DECISION_ID-01}: el INSERT nunca informa la PK; la recupera
 * {@code GeneratedKeyHolder}. La secuencia no se promete sin huecos.
 *
 * <p>{@code DECISION_OCC-01} y {@code DECISION_OCC-01/B}: el alta deja
 * {@code version_row} en 0 y la actualizacion lo incrementa en SQL. Cero filas
 * afectadas no es un unico caso y se desambigua con una lectura por id que solo se
 * paga en el camino de fallo.
 *
 * <h2>FalActa.observaciones</h2>
 *
 * {@code observaciones} no es una columna de fal_acta. Segun {@code DECISION_OBS-01}
 * es la observacion inicial del labrado y vive en fal_observacion con el vinculo
 * propietario {@code (entidad_tipo = ACTA, entidad_id = fal_acta.id)} e
 * {@code id_acta_contexto} nulo. Nunca se mapea a {@code resumen_hecho}.
 *
 * <p><b>Escritura:</b> este adaptador no inserta, actualiza ni desactiva
 * observaciones. {@code guardar} escribe exclusivamente fal_acta y conserva en la
 * copia devuelta el valor de {@code observaciones} que traia el argumento, porque ese
 * valor sigue formando parte del objeto del caso de uso. El alta conjunta es
 * responsabilidad de {@code ActaService.labrar} ({@code DECISION_OBS-TX-01}).
 *
 * <p><b>Lectura:</b> las tres lecturas del puerto reconstruyen
 * {@code FalActa.observaciones} pidiendo el historial completo del propietario a
 * {@link ObservacionRepository} y tomando su primera fila, ya ordenada por
 * {@code fh_alta ASC, id ASC}. Se usa el historial y no solo las activas: la
 * observacion inicial del labrado sigue siendo la inicial aunque despues se haya dado
 * de baja y corregido con otra fila. Sin filas, el campo queda nulo. Este adaptador
 * no escribe SQL propio sobre fal_observacion ni replica el contrato del puerto de
 * observaciones.
 *
 * <p>La composicion ocurre despues de agotar el ResultSet, no dentro del RowMapper:
 * el mapeo devuelve un constructor pendiente del texto y la consulta a
 * {@link ObservacionRepository} se hace fuera del recorrido. Asi el adaptador no
 * depende de que el driver bufferice el resultado.
 *
 * <p><b>Cardinalidad de las lecturas.</b> {@code listarTodas} resuelve las
 * observaciones de todas las actas leidas con <b>una sola</b> invocacion de
 * {@link ObservacionRepository#listarPorEntidades} ({@code DECISION-OBS-BATCH-01}):
 * una consulta de actas mas una consulta de observaciones, cualquiera sea la cantidad
 * de filas. No llama {@code listarPorEntidad} por acta. Las dos lecturas por clave
 * devuelven una sola acta y conservan la composicion individual, donde el batch no
 * reduciria ninguna consulta.
 *
 * <p>{@code listarPagina} tiene la misma cardinalidad, pero el batch se limita a los ids
 * de <b>la pagina actual</b>: una consulta de pagina mas una de observaciones, mas una de
 * conteo cuando se pide el total. Nunca lee toda {@code fal_acta} para despues recortar.
 *
 * <h2>Consulta paginada</h2>
 *
 * {@code DECISION-LIST-PAGE-SORT-01}. El {@code WHERE} se compone a partir de fragmentos
 * <b>constantes</b> de {@link #FILTROS} y los valores viajan como parametros enlazados; el
 * {@code ORDER BY} se compone a partir de {@link #columnaDeOrden(CampoOrdenActa)}, un
 * {@code switch} exhaustivo sobre la allowlist. En ningun punto se concatena texto recibido
 * del cliente: lo unico que el cliente puede aportar es una constante de
 * {@link CampoOrdenActa} y una de {@link ar.gob.malvinas.faltas.core.application.query.DireccionOrden}.
 *
 * <p><b>Politica de nulos.</b> Se usa el orden nativo de MariaDB: en {@code ASC} los nulos
 * van primero y en {@code DESC} van ultimos. No se envuelve la columna en {@code ISNULL()}
 * ni en {@code COALESCE()} porque una columna envuelta en una funcion no puede usar su
 * indice, y la politica quedaria pagada con un {@code filesort} en todas las consultas para
 * cambiar la posicion de un grupo de filas. La politica es explicita y verificada, no
 * accidental.
 *
 * <h2>Integridad</h2>
 *
 * fal_acta tiene dos UNIQUE ({@code uq_fal_acta_id_tecnico},
 * {@code uq_fal_acta_codigo_qr}), nueve claves ajenas y cuatro CHECK
 * ({@code chk_acta_tipo}, {@code chk_acta_qr_ver},
 * {@code chk_acta_situacion_infractor}, {@code chk_acta_infractor_coherente}).
 * Ninguna se traduce a una
 * excepcion de dominio: el puerto no promete ninguna y no existe excepcion funcional
 * canonica para la duplicacion de {@code id_tecnico} o de {@code codigo_qr}. Crear
 * una seria inventar contrato. Las violaciones llegan como la excepcion de Spring que
 * el motor respalda ({@code DuplicateKeyException} para los UNIQUE,
 * {@code DataIntegrityViolationException} para FK, CHECK y NOT NULL) con el nombre de
 * la constraint en el mensaje.
 *
 * <p>{@code DECISION-ACTA-UNIQUE-01} cierra esa lectura: los dos UNIQUE protegen
 * valores que el sistema genera y que el cliente no puede proponer, de modo que una
 * colision no es una regla de negocio corregible por el usuario ni un conflicto
 * funcional HTTP. No se crea excepcion de dominio, no se agrega mapeo a 409 y no se
 * reintenta con otro UUID. El nombre de la constraint debe permanecer identificable
 * porque es lo unico que distingue cual de las dos se violo. El delta contra el
 * adaptador InMemory, que no verifica unicidad alguna, queda aceptado y documentado
 * como limite del doble de prueba: la unicidad la sostiene el motor y verificarla es
 * responsabilidad de las pruebas de integracion.
 *
 * <h2>Transaccionalidad y reloj</h2>
 *
 * Este adaptador no abre transacciones ni anota {@code @Transactional}: la frontera
 * vive en la capa de aplicacion. Las fechas y los actores se persisten tal como los
 * entrega el dominio ({@code FaltasClock} es la fuente de tiempo); no se usa
 * {@code NOW()}, {@code CURRENT_TIMESTAMP} ni un usuario por defecto.
 */
@Repository
@Profile("jdbc")
public class JdbcActaRepository implements ActaRepository {

    /** Escala fisica de las cuatro coordenadas de fal_acta: DECIMAL(10,7). */
    private static final int ESCALA_COORDENADA = 7;

    /** Escala fisica de precision_captura_m: DECIMAL(8,2). Son metros, no grados. */
    private static final int ESCALA_METROS = 2;

    private static final String COLUMNAS = """
            id, id_tecnico, nro_acta, id_talonario, nro_talonario_usado,
            tipo_acta, origen_captura, id_dispositivo_captura, id_user_captura, fh_captura,
            lat_captura, lon_captura, precision_captura_m, fh_pos_captura, origen_pos_captura,
            fecha_acta, fecha_labrado, id_dep, ver_dep, id_insp, ver_insp,
            situacion_identificacion_infractor,
            id_persona_infractor, id_domicilio_infractor_act, id_domicilio_notif_act,
            resumen_hecho, domicilio_hecho,
            id_loc_infr_malvinas, localidad_infr_malvinas_version_id,
            id_tca_infr_malvinas, calle_infr_malvinas_version_id,
            altura_infr, altura_origen_infr, si_altura_infr_estimada,
            lat_infr, lon_infr, origen_ubicacion_infr, si_ubicacion_infr_manual,
            si_dom_txt_infr, dom_txt_infr, si_eje_urb,
            codigo_qr, qr_payload_version,
            bloque_actual, est_proc_act, sit_adm_act,
            resultado_final, resultado_firma_infractor,
            id_motivo_archivo_actual, permite_reingreso, fh_cierre, fh_archivo,
            version_row, fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            """;

    /**
     * Las 56 columnas del alta: las 57 fisicas menos la PK, que asigna el motor.
     * version_row se informa con el literal 0 en lugar de delegar en el DEFAULT del
     * DDL, para que la version inicial de DECISION_OCC-01 sea explicita en el SQL.
     */
    private static final String SQL_INSERT = """
            INSERT INTO fal_acta (
                id_tecnico, nro_acta, id_talonario, nro_talonario_usado,
                tipo_acta, origen_captura, id_dispositivo_captura, id_user_captura, fh_captura,
                lat_captura, lon_captura, precision_captura_m, fh_pos_captura, origen_pos_captura,
                fecha_acta, fecha_labrado, id_dep, ver_dep, id_insp, ver_insp,
                situacion_identificacion_infractor,
                id_persona_infractor, id_domicilio_infractor_act, id_domicilio_notif_act,
                resumen_hecho, domicilio_hecho,
                id_loc_infr_malvinas, localidad_infr_malvinas_version_id,
                id_tca_infr_malvinas, calle_infr_malvinas_version_id,
                altura_infr, altura_origen_infr, si_altura_infr_estimada,
                lat_infr, lon_infr, origen_ubicacion_infr, si_ubicacion_infr_manual,
                si_dom_txt_infr, dom_txt_infr, si_eje_urb,
                codigo_qr, qr_payload_version,
                bloque_actual, est_proc_act, sit_adm_act,
                resultado_final, resultado_firma_infractor,
                id_motivo_archivo_actual, permite_reingreso, fh_cierre, fh_archivo,
                version_row, fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            ) VALUES (
                :idTecnico, :nroActa, :idTalonario, :nroTalonarioUsado,
                :tipoActa, :origenCaptura, :idDispositivoCaptura, :idUserCaptura, :fhCaptura,
                :latCaptura, :lonCaptura, :precisionCapturaM, :fhPosCaptura, :origenPosCaptura,
                :fechaActa, :fechaLabrado, :idDep, :verDep, :idInsp, :verInsp,
                :situacionIdentificacionInfractor,
                :idPersonaInfractor, :idDomicilioInfractorAct, :idDomicilioNotifAct,
                :resumenHecho, :domicilioHecho,
                :idLocInfrMalvinas, :localidadInfrMalvinasVersionId,
                :idTcaInfrMalvinas, :calleInfrMalvinasVersionId,
                :alturaInfr, :alturaOrigenInfr, :siAlturaInfrEstimada,
                :latInfr, :lonInfr, :origenUbicacionInfr, :siUbicacionInfrManual,
                :siDomTxtInfr, :domTxtInfr, :siEjeUrb,
                :codigoQr, :qrPayloadVersion,
                :bloqueActual, :estProcAct, :sitAdmAct,
                :resultadoFinal, :resultadoFirmaInfractor,
                :idMotivoArchivoActual, :permiteReingreso, :fhCierre, :fhArchivo,
                0, :fhAlta, :idUserAlta, :fhUltMod, :idUserUltMod
            )
            """;

    /**
     * Formula normativa de DECISION_OCC-01/B: el incremento lo hace el motor sobre la
     * columna, nunca un valor calculado en Java, y la guarda de version viaja en el
     * WHERE. El SET lista las 55 columnas mutables completas: no se construye
     * dinamicamente omitiendo nulos, de modo que pasar una columna nullable a NULL se
     * persiste.
     */
    private static final String SQL_UPDATE_OCC = """
            UPDATE fal_acta
               SET id_tecnico = :idTecnico,
                   nro_acta = :nroActa,
                   id_talonario = :idTalonario,
                   nro_talonario_usado = :nroTalonarioUsado,
                   tipo_acta = :tipoActa,
                   origen_captura = :origenCaptura,
                   id_dispositivo_captura = :idDispositivoCaptura,
                   id_user_captura = :idUserCaptura,
                   fh_captura = :fhCaptura,
                   lat_captura = :latCaptura,
                   lon_captura = :lonCaptura,
                   precision_captura_m = :precisionCapturaM,
                   fh_pos_captura = :fhPosCaptura,
                   origen_pos_captura = :origenPosCaptura,
                   fecha_acta = :fechaActa,
                   fecha_labrado = :fechaLabrado,
                   id_dep = :idDep,
                   ver_dep = :verDep,
                   id_insp = :idInsp,
                   ver_insp = :verInsp,
                   situacion_identificacion_infractor = :situacionIdentificacionInfractor,
                   id_persona_infractor = :idPersonaInfractor,
                   id_domicilio_infractor_act = :idDomicilioInfractorAct,
                   id_domicilio_notif_act = :idDomicilioNotifAct,
                   resumen_hecho = :resumenHecho,
                   domicilio_hecho = :domicilioHecho,
                   id_loc_infr_malvinas = :idLocInfrMalvinas,
                   localidad_infr_malvinas_version_id = :localidadInfrMalvinasVersionId,
                   id_tca_infr_malvinas = :idTcaInfrMalvinas,
                   calle_infr_malvinas_version_id = :calleInfrMalvinasVersionId,
                   altura_infr = :alturaInfr,
                   altura_origen_infr = :alturaOrigenInfr,
                   si_altura_infr_estimada = :siAlturaInfrEstimada,
                   lat_infr = :latInfr,
                   lon_infr = :lonInfr,
                   origen_ubicacion_infr = :origenUbicacionInfr,
                   si_ubicacion_infr_manual = :siUbicacionInfrManual,
                   si_dom_txt_infr = :siDomTxtInfr,
                   dom_txt_infr = :domTxtInfr,
                   si_eje_urb = :siEjeUrb,
                   codigo_qr = :codigoQr,
                   qr_payload_version = :qrPayloadVersion,
                   bloque_actual = :bloqueActual,
                   est_proc_act = :estProcAct,
                   sit_adm_act = :sitAdmAct,
                   resultado_final = :resultadoFinal,
                   resultado_firma_infractor = :resultadoFirmaInfractor,
                   id_motivo_archivo_actual = :idMotivoArchivoActual,
                   permite_reingreso = :permiteReingreso,
                   fh_cierre = :fhCierre,
                   fh_archivo = :fhArchivo,
                   fh_alta = :fhAlta,
                   id_user_alta = :idUserAlta,
                   fh_ult_mod = :fhUltMod,
                   id_user_ult_mod = :idUserUltMod,
                   version_row = version_row + 1
             WHERE id = :id
               AND version_row = :versionEsperada
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_acta
            WHERE id = :id
            """;

    /**
     * Igual que {@link #SQL_POR_ID} con {@code FOR UPDATE} (Wave 4 Parte 3D S5.6/S16):
     * bloquea la fila del acta para serializar la asignacion de {@code orden} de fotografias
     * entre operaciones concurrentes de carga posterior sobre la misma acta.
     */
    private static final String SQL_POR_ID_FOR_UPDATE = SQL_POR_ID + " FOR UPDATE";

    /**
     * id_tecnico es clave tecnica de idempotencia y el contrato del puerto la compara
     * exacto, como {@code String.equals}. La collation de la tabla es
     * {@code utf8mb4_uca1400_ai_ci}, insensible a mayusculas y acentos, de modo que la
     * comparacion simple encontraria un UUID que el adaptador InMemory no encuentra.
     * {@code CAST(... AS BINARY)} neutraliza la collation, igual que la decision
     * D-1A1-05 ya ratificada para {@code referencia_externa}, y ademas descarta el
     * relleno de espacios de PAD SPACE.
     *
     * <p>La igualdad simple se conserva junto al CAST porque es la unica de las dos
     * que puede usar {@code uq_fal_acta_id_tecnico}: acota el candidato por indice y el
     * CAST decide la coincidencia exacta. Como la comparacion insensible es un
     * superconjunto de la exacta, el filtro no puede descartar la fila correcta.
     */
    private static final String SQL_POR_UUID_TECNICO = "SELECT " + COLUMNAS + """
            FROM fal_acta
            WHERE id_tecnico = :uuidTecnico
              AND CAST(id_tecnico AS BINARY) = :uuidTecnico
            """;

    /**
     * Igual que {@link #SQL_POR_UUID_TECNICO} con {@code FOR UPDATE} (Wave 4 PARTE-3A §12.5):
     * lectura de bloqueo que ve la fila ya comprometida por la transaccion ganadora de una
     * carrera de idempotencia, incluso si el snapshot no bloqueante de REPEATABLE READ de
     * esta transaccion todavia no la veria.
     */
    private static final String SQL_POR_UUID_TECNICO_FOR_UPDATE = SQL_POR_UUID_TECNICO + " FOR UPDATE";

    /** Orden explicito: no se depende del orden fisico de almacenamiento. */
    private static final String SQL_TODAS = "SELECT " + COLUMNAS + """
            FROM fal_acta
            ORDER BY id ASC
            """;

    private static final String SQL_VERSION_POR_ID = """
            SELECT version_row FROM fal_acta WHERE id = :id
            """;

    // =====================================================================
    // CONSULTA PAGINADA (DECISION-LIST-PAGE-SORT-01)
    // =====================================================================

    /**
     * Columna de desempate. Es la PK, de modo que el orden total resultante es estricto y
     * dos ejecuciones de la misma consulta devuelven la misma secuencia.
     */
    private static final String COLUMNA_DESEMPATE = "id";

    /**
     * Los nueve predicados del filtro tipado, cada uno con su fragmento constante y el
     * nombre del parametro enlazado. Es una lista de constantes: ni el fragmento ni el
     * nombre del parametro pueden originarse en un dato del cliente.
     */
    private static final List<PredicadoFiltro> FILTROS = List.of(
            new PredicadoFiltro("tipo_acta = :fTipoActa", "fTipoActa",
                    f -> f.tipoActa() == null ? null : f.tipoActa().codigo(), Types.SMALLINT),
            new PredicadoFiltro("bloque_actual = :fBloqueActual", "fBloqueActual",
                    f -> f.bloqueActual() == null ? null : f.bloqueActual().codigo(), Types.CHAR),
            new PredicadoFiltro("est_proc_act = :fEstProcAct", "fEstProcAct",
                    f -> f.estadoProcesal() == null ? null : f.estadoProcesal().codigo(), Types.CHAR),
            new PredicadoFiltro("sit_adm_act = :fSitAdmAct", "fSitAdmAct",
                    f -> f.situacionAdministrativa() == null
                            ? null : f.situacionAdministrativa().codigo(), Types.CHAR),
            new PredicadoFiltro("id_dep = :fIdDep", "fIdDep",
                    FiltroActa::idDependencia, Types.BIGINT),
            new PredicadoFiltro("id_insp = :fIdInsp", "fIdInsp",
                    FiltroActa::idInspector, Types.BIGINT),
            new PredicadoFiltro("nro_acta = :fNroActa", "fNroActa",
                    FiltroActa::nroActa, Types.VARCHAR),
            new PredicadoFiltro("fecha_acta >= :fFechaDesde", "fFechaDesde",
                    FiltroActa::fechaActaDesde, Types.DATE),
            new PredicadoFiltro("fecha_acta <= :fFechaHasta", "fFechaHasta",
                    FiltroActa::fechaActaHasta, Types.DATE));

    /**
     * Un predicado del filtro: fragmento SQL constante, nombre del parametro, extractor del
     * valor y tipo JDBC. El valor nulo significa que el predicado no participa.
     */
    private record PredicadoFiltro(String sql, String parametro,
                                   Function<FiltroActa, Object> valor, int tipo) {
    }

    private final JdbcClient jdbc;
    private final ObservacionRepository observaciones;

    /**
     * @param jdbc          acceso al DataSource principal de Faltas
     * @param observaciones puerto canonico de fal_observacion, usado solo para
     *                      componer la observacion inicial en las lecturas
     */
    public JdbcActaRepository(JdbcClient jdbc, ObservacionRepository observaciones) {
        if (jdbc == null) {
            throw new IllegalArgumentException("jdbc es obligatorio");
        }
        if (observaciones == null) {
            throw new IllegalArgumentException("observaciones es obligatorio");
        }
        this.jdbc = jdbc;
        this.observaciones = observaciones;
    }

    // =====================================================================
    // ESCRITURA
    // =====================================================================

    @Override
    public FalActa guardar(FalActa acta) {
        if (acta == null) {
            throw new IllegalArgumentException("acta es obligatoria");
        }
        return acta.getId() == null ? insertar(acta) : actualizar(acta);
    }

    /**
     * Alta: el INSERT no informa la PK y GeneratedKeyHolder la recupera. La copia
     * devuelta conserva el valor de {@code observaciones} del argumento, que no es una
     * columna de fal_acta y que este adaptador no persiste.
     */
    private FalActa insertar(FalActa acta) {
        if (acta.getVersionRow() != 0) {
            throw new PrecondicionVioladaException(
                    "Un acta nueva debe llegar con versionRow=0. Recibido: " + acta.getVersionRow());
        }

        KeyHolder claves = new GeneratedKeyHolder();
        vincularColumnas(jdbc.sql(SQL_INSERT), acta).update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_acta no devolvio el id generado para el acta " + acta.getUuidTecnico());
        }

        FalActa persistida = acta.conId(generado.longValue());
        persistida.setVersionRow(0);
        return persistida;
    }

    /**
     * Actualizacion optimista. Una fila afectada es exito y la version efectiva es la
     * entrante mas uno, sin releer. Cero filas se desambigua por id, y esa lectura
     * solo se paga en el camino de fallo.
     */
    private FalActa actualizar(FalActa acta) {
        Long id = acta.getId();
        int filas = vincularColumnas(jdbc.sql(SQL_UPDATE_OCC), acta)
                .param("id", id, Types.BIGINT)
                .param("versionEsperada", acta.getVersionRow(), Types.INTEGER)
                .update();

        if (filas == 0) {
            throw desempatarActualizacionSinEfecto(id, acta.getVersionRow());
        }

        FalActa persistida = acta.copia();
        persistida.setVersionRow(acta.getVersionRow() + 1);
        return persistida;
    }

    /**
     * DECISION_OCC-01/B: un id inexistente es un 404 funcional y no debe degradarse a
     * conflicto de concurrencia. Mismos tipos y mensajes que produce el adaptador
     * InMemory.
     */
    private RuntimeException desempatarActualizacionSinEfecto(Long id, int versionEntrante) {
        Optional<Integer> almacenada = jdbc.sql(SQL_VERSION_POR_ID)
                .param("id", id, Types.BIGINT)
                .query(Integer.class)
                .optional();

        return almacenada
                .<RuntimeException>map(version ->
                        new ConcurrenciaConflictoException("FalActa", id, version, versionEntrante))
                .orElseGet(() -> new ActaNoEncontradaException(id));
    }

    // =====================================================================
    // LECTURA
    // =====================================================================

    @Override
    public Optional<FalActa> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id, Types.BIGINT)
                .query(JdbcActaRepository::mapear)
                .optional()
                .map(this::componer);
    }

    @Override
    public Optional<FalActa> buscarPorIdConLock(Long id) {
        return jdbc.sql(SQL_POR_ID_FOR_UPDATE)
                .param("id", id, Types.BIGINT)
                .query(JdbcActaRepository::mapear)
                .optional()
                .map(this::componer);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnicoConLock(String uuidTecnico) {
        return jdbc.sql(SQL_POR_UUID_TECNICO_FOR_UPDATE)
                .param("uuidTecnico", uuidTecnico, Types.CHAR)
                .query(JdbcActaRepository::mapear)
                .optional()
                .map(this::componer);
    }

    @Override
    public Optional<FalActa> buscarPorUuidTecnico(String uuidTecnico) {
        return jdbc.sql(SQL_POR_UUID_TECNICO)
                .param("uuidTecnico", uuidTecnico, Types.CHAR)
                .query(JdbcActaRepository::mapear)
                .optional()
                .map(this::componer);
    }

    /**
     * Contrato completo del puerto: cada acta vuelve con {@code observaciones}
     * reconstruida, igual que en las dos lecturas por clave.
     *
     * <p>Dos consultas en total, independientemente de la cantidad de actas: una de
     * fal_acta y una sola operacion batch de observaciones. Sin actas no se consulta
     * fal_observacion, porque no hay propietario por el que preguntar.
     *
     * <p>El orden de salida es el de {@code SQL_TODAS}: la composicion recorre la lista
     * materializada de actas, no las claves del mapa de historiales.
     */
    @Override
    public List<FalActa> listarTodas() {
        List<ActaMapeada> mapeadas = jdbc.sql(SQL_TODAS)
                .query(JdbcActaRepository::mapear)
                .list();
        if (mapeadas.isEmpty()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>(mapeadas.size());
        for (ActaMapeada mapeada : mapeadas) {
            if (mapeada.id() != null) {
                ids.add(mapeada.id());
            }
        }

        Map<Long, List<FalObservacion>> historiales =
                observaciones.listarPorEntidades(EntidadTipoObservada.ACTA, ids);

        return aplicarHistoriales(mapeadas, historiales);
    }

    /**
     * Pagina de actas resuelta enteramente en el motor: {@code WHERE}, {@code ORDER BY},
     * {@code LIMIT} y {@code OFFSET}. No se lee la tabla completa en ningun caso.
     *
     * <p>Cardinalidad, con el total pedido: una consulta de conteo, una de la pagina y una
     * operacion batch de observaciones limitada a los ids de esa pagina. Sin total: dos, y
     * la fila extra de {@code size + 1} decide {@code haySiguiente}.
     *
     * <p>El conteo se ejecuta antes de la pagina y con los mismos parametros enlazados: si
     * el filtro no selecciona ninguna fila, la consulta de la pagina no se emite.
     */
    @Override
    public PaginaResultado<FalActa> listarPagina(FiltroActa filtro,
                                                 PaginaSolicitada<CampoOrdenActa> pagina) {
        FiltroActa f = filtro == null ? FiltroActa.ninguno() : filtro;
        PaginaSolicitada<CampoOrdenActa> p =
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina;

        List<PredicadoFiltro> activos = predicadosActivos(f);
        String where = clausulaWhere(activos);

        Long total = null;
        if (p.incluirTotal()) {
            total = vincularFiltro(jdbc.sql("SELECT COUNT(*) FROM fal_acta" + where), activos, f)
                    .query(Long.class)
                    .single();
            if (total == 0L || p.offset() >= total) {
                return new PaginaResultado<>(List.of(), p.page(), p.size(), total, false);
            }
        }

        // Sin total, la fila extra es lo unico que distingue "ultima pagina" de "hay mas".
        int limite = p.incluirTotal() ? p.size() : p.size() + 1;
        String sql = "SELECT " + COLUMNAS + " FROM fal_acta" + where
                + " ORDER BY " + expresionOrden(p.orden())
                + " LIMIT :paginaLimite OFFSET :paginaDesplazamiento";

        List<ActaMapeada> mapeadas =
                vincularFiltro(jdbc.sql(sql), activos, f)
                        .param("paginaLimite", limite, Types.INTEGER)
                        .param("paginaDesplazamiento", p.offset(), Types.BIGINT)
                        .query(JdbcActaRepository::mapear)
                        .list();

        boolean haySiguiente = !p.incluirTotal() && mapeadas.size() > p.size();
        if (haySiguiente) {
            mapeadas = mapeadas.subList(0, p.size());
        }

        List<FalActa> contenido = componerBatch(mapeadas);
        return p.incluirTotal()
                ? PaginaResultado.conTotal(contenido, p.page(), p.size(), total)
                : PaginaResultado.sinTotal(contenido, p.page(), p.size(), haySiguiente);
    }

    /** Predicados cuyo valor esta informado. Los demas no participan del {@code WHERE}. */
    private static List<PredicadoFiltro> predicadosActivos(FiltroActa filtro) {
        List<PredicadoFiltro> activos = new ArrayList<>(FILTROS.size());
        for (PredicadoFiltro predicado : FILTROS) {
            if (predicado.valor().apply(filtro) != null) {
                activos.add(predicado);
            }
        }
        return activos;
    }

    /** Conjuncion de fragmentos constantes. Sin predicados activos no hay clausula. */
    private static String clausulaWhere(List<PredicadoFiltro> activos) {
        if (activos.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" WHERE ");
        for (int i = 0; i < activos.size(); i++) {
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(activos.get(i).sql());
        }
        return sb.toString();
    }

    /** Enlaza el valor de cada predicado activo. Ningun valor entra en el texto del SQL. */
    private static JdbcClient.StatementSpec vincularFiltro(
            JdbcClient.StatementSpec spec, List<PredicadoFiltro> activos, FiltroActa filtro) {
        JdbcClient.StatementSpec resultado = spec;
        for (PredicadoFiltro predicado : activos) {
            resultado = resultado.param(
                    predicado.parametro(), predicado.valor().apply(filtro), predicado.tipo());
        }
        return resultado;
    }

    /**
     * Expresion del {@code ORDER BY}: los criterios pedidos, o el orden predeterminado de la
     * entidad si no se pidio ninguno, mas la PK como ultimo desempate.
     *
     * <p>La PK toma la direccion del ultimo criterio para que un orden "mas reciente primero"
     * siga siendo descendente en el desempate y no invierta el tramo final de cada empate.
     */
    private static String expresionOrden(List<CriterioOrden<CampoOrdenActa>> criterios) {
        List<CriterioOrden<CampoOrdenActa>> efectivos =
                criterios.isEmpty() ? CampoOrdenActa.ordenPredeterminado() : criterios;
        StringBuilder sb = new StringBuilder();
        for (CriterioOrden<CampoOrdenActa> criterio : efectivos) {
            sb.append(columnaDeOrden(criterio.campo()))
                    .append(' ')
                    .append(criterio.direccion().sql())
                    .append(", ");
        }
        sb.append(COLUMNA_DESEMPATE)
                .append(' ')
                .append(efectivos.get(efectivos.size() - 1).direccion().sql());
        return sb.toString();
    }

    /**
     * Traduce un campo funcional de la allowlist a su columna fisica.
     *
     * <p>Es un {@code switch} exhaustivo sin {@code default}: si alguien agrega una constante
     * a {@link CampoOrdenActa} sin decidir su columna, el modulo no compila. Un mapa con
     * busqueda por clave fallaria en ejecucion, y lo haria produciendo un {@code ORDER BY}
     * con la palabra {@code null}.
     */
    private static String columnaDeOrden(CampoOrdenActa campo) {
        return switch (campo) {
            case NRO_ACTA -> "nro_acta";
            case FECHA_ACTA -> "fecha_acta";
            case FECHA_LABRADO -> "fecha_labrado";
            case FECHA_ALTA -> "fh_alta";
            case TIPO_ACTA -> "tipo_acta";
            case BLOQUE_ACTUAL -> "bloque_actual";
            case ESTADO_PROCESAL -> "est_proc_act";
            case SITUACION_ADMINISTRATIVA -> "sit_adm_act";
            case DEPENDENCIA -> "id_dep";
            case INSPECTOR -> "id_insp";
            case PERSONA_INFRACTOR -> "id_persona_infractor";
        };
    }

    /**
     * Resuelve las observaciones de las actas recibidas con <b>una sola</b> invocacion batch
     * y compone.
     *
     * <p>Con la lista vacia no se consulta {@code fal_observacion}: no hay propietario por el
     * que preguntar. Lo usa {@code listarPagina}, de modo que el batch queda acotado a los ids
     * de la pagina y no a la tabla completa.
     */
    private List<FalActa> componerBatch(List<ActaMapeada> mapeadas) {
        if (mapeadas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(mapeadas.size());
        for (ActaMapeada mapeada : mapeadas) {
            if (mapeada.id() != null) {
                ids.add(mapeada.id());
            }
        }
        return aplicarHistoriales(
                mapeadas, observaciones.listarPorEntidades(EntidadTipoObservada.ACTA, ids));
    }

    /**
     * Aplica los historiales ya leidos, preservando el orden de la lista materializada de
     * actas y no el de las claves del mapa.
     */
    private static List<FalActa> aplicarHistoriales(
            List<ActaMapeada> mapeadas, Map<Long, List<FalObservacion>> historiales) {
        List<FalActa> compuestas = new ArrayList<>(mapeadas.size());
        for (ActaMapeada mapeada : mapeadas) {
            compuestas.add(mapeada.conObservacionInicial().apply(
                    observacionInicialDe(historialDe(historiales, mapeada.id()))));
        }
        return List.copyOf(compuestas);
    }

    /**
     * El puerto batch puede omitir un propietario sin observaciones, de modo que la
     * ausencia se interpreta como historial vacio. Un id nulo no se consulta: no hubo
     * propietario por el que preguntar y los mapas inmutables rechazan la clave nula.
     */
    private static List<FalObservacion> historialDe(
            Map<Long, List<FalObservacion>> historiales, Long idActa) {
        return idActa == null ? List.of() : historiales.getOrDefault(idActa, List.of());
    }

    /**
     * Aplica el constructor pendiente con la observacion inicial del propietario.
     *
     * <p>Se pide el historial completo, no solo las activas: la primera fila por
     * {@code fh_alta ASC, id ASC} es la observacion capturada durante el labrado
     * aunque despues se haya dado de baja y corregido con una fila nueva.
     */
    private FalActa componer(ActaMapeada mapeada) {
        List<FalObservacion> historial =
                observaciones.listarPorEntidad(EntidadTipoObservada.ACTA, mapeada.id());
        return mapeada.conObservacionInicial().apply(observacionInicialDe(historial));
    }

    /** Primera fila del historial ya ordenado, o nulo si el propietario no tiene. */
    private static String observacionInicialDe(List<FalObservacion> historial) {
        return historial.isEmpty() ? null : historial.get(0).getObservacion();
    }

    // =====================================================================
    // VINCULACION DE PARAMETROS
    // =====================================================================

    /**
     * Las 55 columnas mutables, compartidas por el INSERT y por el UPDATE. Tenerlas en
     * un unico lugar evita dos listas de parametros que haya que mantener
     * sincronizadas.
     */
    private static JdbcClient.StatementSpec vincularColumnas(JdbcClient.StatementSpec spec, FalActa a) {
        return spec
                .param("idTecnico", a.getUuidTecnico(), Types.CHAR)
                .param("nroActa", a.getNroActa(), Types.VARCHAR)
                .param("idTalonario", a.getIdTalonario(), Types.BIGINT)
                .param("nroTalonarioUsado", a.getNroTalonarioUsado(), Types.INTEGER)
                .param("tipoActa", codigo(a.getTipoActa()), Types.SMALLINT)
                .param("origenCaptura", codigo(a.getOrigenCaptura()), Types.SMALLINT)
                .param("idDispositivoCaptura", a.getIdDispositivoCaptura(), Types.VARCHAR)
                .param("idUserCaptura", a.getIdUserCaptura(), Types.CHAR)
                .param("fhCaptura", a.getFhCaptura(), Types.TIMESTAMP)
                .param("latCaptura", aDecimal(a.getLatCaptura(), ESCALA_COORDENADA), Types.DECIMAL)
                .param("lonCaptura", aDecimal(a.getLonCaptura(), ESCALA_COORDENADA), Types.DECIMAL)
                .param("precisionCapturaM", aDecimal(a.getPrecisionCapturaM(), ESCALA_METROS), Types.DECIMAL)
                .param("fhPosCaptura", a.getFhPosCaptura(), Types.TIMESTAMP)
                .param("origenPosCaptura", codigo(a.getOrigenPosCaptura()), Types.SMALLINT)
                .param("fechaActa", a.getFechaActa(), Types.DATE)
                .param("fechaLabrado", a.getFechaLabrado(), Types.TIMESTAMP)
                .param("idDep", a.getIdDependencia(), Types.BIGINT)
                .param("verDep", a.getVerDep(), Types.SMALLINT)
                .param("idInsp", a.getIdInspector(), Types.BIGINT)
                .param("verInsp", a.getVerInsp(), Types.SMALLINT)
                .param("situacionIdentificacionInfractor",
                        codigo(a.getSituacionIdentificacionInfractor()), Types.SMALLINT)
                .param("idPersonaInfractor", a.getIdPersonaInfractor(), Types.BIGINT)
                .param("idDomicilioInfractorAct", a.getIdDomicilioInfractorAct(), Types.BIGINT)
                .param("idDomicilioNotifAct", a.getIdDomicilioNotifAct(), Types.BIGINT)
                .param("resumenHecho", a.getResumenHecho(), Types.VARCHAR)
                .param("domicilioHecho", a.getDomicilioHecho(), Types.VARCHAR)
                .param("idLocInfrMalvinas", a.getIdLocInfrMalvinas(), Types.VARCHAR)
                .param("localidadInfrMalvinasVersionId", a.getLocalidadInfrMalvinasVersionId(), Types.BIGINT)
                .param("idTcaInfrMalvinas", a.getIdTcaInfrMalvinas(), Types.VARCHAR)
                .param("calleInfrMalvinasVersionId", a.getCalleInfrMalvinasVersionId(), Types.BIGINT)
                .param("alturaInfr", a.getAlturaInfr(), Types.INTEGER)
                .param("alturaOrigenInfr", a.getAlturaOrigenInfr(), Types.SMALLINT)
                .param("siAlturaInfrEstimada", a.isSiAlturaInfrEstimada())
                .param("latInfr", aDecimal(a.getLatInfr(), ESCALA_COORDENADA), Types.DECIMAL)
                .param("lonInfr", aDecimal(a.getLonInfr(), ESCALA_COORDENADA), Types.DECIMAL)
                .param("origenUbicacionInfr", codigo(a.getOrigenUbicacionInfr()), Types.SMALLINT)
                .param("siUbicacionInfrManual", a.isSiUbicacionInfrManual())
                .param("siDomTxtInfr", a.isSiDomTxtInfr())
                .param("domTxtInfr", a.getDomTxtInfr(), Types.VARCHAR)
                .param("siEjeUrb", a.isSiEjeUrb())
                .param("codigoQr", a.getCodigoQr(), Types.VARCHAR)
                .param("qrPayloadVersion", a.getQrPayloadVersion(), Types.SMALLINT)
                .param("bloqueActual", codigo(a.getBloqueActual()), Types.CHAR)
                .param("estProcAct", codigo(a.getEstadoProcesal()), Types.CHAR)
                .param("sitAdmAct", codigo(a.getSituacionAdministrativa()), Types.CHAR)
                .param("resultadoFinal", codigo(a.getResultadoFinal()), Types.SMALLINT)
                .param("resultadoFirmaInfractor", codigo(a.getResultadoFirmaInfractor()), Types.SMALLINT)
                .param("idMotivoArchivoActual", a.getIdMotivoArchivoActual(), Types.BIGINT)
                .param("permiteReingreso", a.getPermiteReingreso(), Types.BOOLEAN)
                .param("fhCierre", a.getFhCierre(), Types.TIMESTAMP)
                .param("fhArchivo", a.getFhArchivo(), Types.TIMESTAMP)
                .param("fhAlta", a.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", a.getIdUserAlta(), Types.CHAR)
                .param("fhUltMod", a.getFhUltMod(), Types.TIMESTAMP)
                .param("idUserUltMod", a.getIdUserUltMod(), Types.CHAR);
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    /**
     * Fila de fal_acta ya leida, con la construccion de FalActa pendiente del unico
     * dato que no vive en esta tabla: la observacion inicial.
     *
     * <p>El id viaja aparte porque es la clave con la que se pide esa observacion.
     */
    private record ActaMapeada(Long id, Function<String, FalActa> conObservacionInicial) { }

    /**
     * Lee las 57 columnas y devuelve el constructor pendiente. Los enums viajan por su
     * codigo fisico: nunca {@code ordinal()}, {@code name()} ni {@code toString()}.
     *
     * <p>El constructor principal de FalActa asigna valores por defecto a bloque,
     * estado procesal, situacion administrativa, resultado final, version, origen de
     * captura, fh_captura, version del payload QR, version de dependencia y las cuatro
     * banderas booleanas. El mapeo los sobreescribe todos con el valor del motor.
     */
    private static ActaMapeada mapear(ResultSet rs, int fila) throws SQLException {
        Long id = rs.getObject("id", Long.class);
        String idTecnico = rs.getString("id_tecnico");
        String nroActa = rs.getString("nro_acta");
        Long idTalonario = rs.getObject("id_talonario", Long.class);
        Integer nroTalonarioUsado = rs.getObject("nro_talonario_usado", Integer.class);
        TipoActa tipoActa = TipoActa.fromCodigo(rs.getShort("tipo_acta"));
        OrigenCaptura origenCaptura = OrigenCaptura.fromCodigo(rs.getShort("origen_captura"));
        String idDispositivoCaptura = rs.getString("id_dispositivo_captura");
        String idUserCaptura = rs.getString("id_user_captura");
        LocalDateTime fhCaptura = rs.getObject("fh_captura", LocalDateTime.class);
        Double latCaptura = aDouble(rs.getBigDecimal("lat_captura"));
        Double lonCaptura = aDouble(rs.getBigDecimal("lon_captura"));
        Double precisionCapturaM = aDouble(rs.getBigDecimal("precision_captura_m"));
        LocalDateTime fhPosCaptura = rs.getObject("fh_pos_captura", LocalDateTime.class);
        OrigenUbicacion origenPosCaptura = origenUbicacion(rs.getObject("origen_pos_captura", Short.class));
        LocalDate fechaActa = rs.getObject("fecha_acta", LocalDate.class);
        LocalDateTime fechaLabrado = rs.getObject("fecha_labrado", LocalDateTime.class);
        Long idDep = rs.getObject("id_dep", Long.class);
        Integer verDep = rs.getObject("ver_dep", Integer.class);
        Long idInsp = rs.getObject("id_insp", Long.class);
        Integer verInsp = rs.getObject("ver_insp", Integer.class);
        SituacionIdentificacionInfractor situacionIdentificacion =
                SituacionIdentificacionInfractor.fromCodigo(rs.getShort("situacion_identificacion_infractor"));
        Long idPersonaInfractor = rs.getObject("id_persona_infractor", Long.class);
        Long idDomicilioInfractorAct = rs.getObject("id_domicilio_infractor_act", Long.class);
        Long idDomicilioNotifAct = rs.getObject("id_domicilio_notif_act", Long.class);
        String resumenHecho = rs.getString("resumen_hecho");
        String domicilioHecho = rs.getString("domicilio_hecho");
        String idLocInfrMalvinas = rs.getString("id_loc_infr_malvinas");
        Long localidadVersionId = rs.getObject("localidad_infr_malvinas_version_id", Long.class);
        String idTcaInfrMalvinas = rs.getString("id_tca_infr_malvinas");
        Long calleVersionId = rs.getObject("calle_infr_malvinas_version_id", Long.class);
        Integer alturaInfr = rs.getObject("altura_infr", Integer.class);
        Short alturaOrigenInfr = rs.getObject("altura_origen_infr", Short.class);
        boolean siAlturaInfrEstimada = rs.getBoolean("si_altura_infr_estimada");
        Double latInfr = aDouble(rs.getBigDecimal("lat_infr"));
        Double lonInfr = aDouble(rs.getBigDecimal("lon_infr"));
        OrigenUbicacion origenUbicacionInfr = origenUbicacion(rs.getObject("origen_ubicacion_infr", Short.class));
        boolean siUbicacionInfrManual = rs.getBoolean("si_ubicacion_infr_manual");
        boolean siDomTxtInfr = rs.getBoolean("si_dom_txt_infr");
        String domTxtInfr = rs.getString("dom_txt_infr");
        boolean siEjeUrb = rs.getBoolean("si_eje_urb");
        String codigoQr = rs.getString("codigo_qr");
        Integer qrPayloadVersion = rs.getObject("qr_payload_version", Integer.class);
        BloqueActual bloqueActual = BloqueActual.deCodigo(rs.getString("bloque_actual"));
        EstadoProcesalActa estadoProcesal = EstadoProcesalActa.deCodigo(rs.getString("est_proc_act"));
        SituacionAdministrativaActa situacion =
                SituacionAdministrativaActa.deCodigo(rs.getString("sit_adm_act"));
        ResultadoFinalActa resultadoFinal = ResultadoFinalActa.fromCodigo(rs.getShort("resultado_final"));
        ResultadoFirmaInfractor resultadoFirma =
                ResultadoFirmaInfractor.desdeCodigo(rs.getShort("resultado_firma_infractor"));
        Long idMotivoArchivoActual = rs.getObject("id_motivo_archivo_actual", Long.class);
        Boolean permiteReingreso = rs.getObject("permite_reingreso", Boolean.class);
        LocalDateTime fhCierre = rs.getObject("fh_cierre", LocalDateTime.class);
        LocalDateTime fhArchivo = rs.getObject("fh_archivo", LocalDateTime.class);
        int versionRow = rs.getInt("version_row");
        LocalDateTime fhAlta = rs.getObject("fh_alta", LocalDateTime.class);
        String idUserAlta = rs.getString("id_user_alta");
        LocalDateTime fhUltMod = rs.getObject("fh_ult_mod", LocalDateTime.class);
        String idUserUltMod = rs.getString("id_user_ult_mod");

        Function<String, FalActa> constructor = observacionInicial -> {
            FalActa acta = new FalActa(id, idTecnico, tipoActa, idDep, idInsp,
                    fechaActa, fechaLabrado, domicilioHecho, observacionInicial,
                    latInfr, lonInfr, resultadoFirma, situacionIdentificacion,
                    idPersonaInfractor, fhAlta, idUserAlta);

            acta.setNroActa(nroActa);
            acta.setIdTalonario(idTalonario);
            acta.setNroTalonarioUsado(nroTalonarioUsado);
            acta.setOrigenCaptura(origenCaptura);
            acta.setIdDispositivoCaptura(idDispositivoCaptura);
            acta.setIdUserCaptura(idUserCaptura);
            acta.setFhCaptura(fhCaptura);
            acta.setLatCaptura(latCaptura);
            acta.setLonCaptura(lonCaptura);
            acta.setPrecisionCapturaM(precisionCapturaM);
            acta.setFhPosCaptura(fhPosCaptura);
            acta.setOrigenPosCaptura(origenPosCaptura);
            acta.setVerDep(verDep);
            acta.setVerInsp(verInsp);
            acta.setIdDomicilioInfractorAct(idDomicilioInfractorAct);
            acta.setIdDomicilioNotifAct(idDomicilioNotifAct);
            acta.setResumenHecho(resumenHecho);
            acta.setIdLocInfrMalvinas(idLocInfrMalvinas);
            acta.setLocalidadInfrMalvinasVersionId(localidadVersionId);
            acta.setIdTcaInfrMalvinas(idTcaInfrMalvinas);
            acta.setCalleInfrMalvinasVersionId(calleVersionId);
            acta.setAlturaInfr(alturaInfr);
            acta.setAlturaOrigenInfr(alturaOrigenInfr);
            acta.setSiAlturaInfrEstimada(siAlturaInfrEstimada);
            acta.setOrigenUbicacionInfr(origenUbicacionInfr);
            acta.setSiUbicacionInfrManual(siUbicacionInfrManual);
            acta.setSiDomTxtInfr(siDomTxtInfr);
            acta.setDomTxtInfr(domTxtInfr);
            acta.setSiEjeUrb(siEjeUrb);
            // La version se aplica antes del token: una vez informado codigo_qr, el agregado
            // congela ambos campos (DECISION-QR-INMUTABLE-01) y el orden inverso rechazaria
            // una fila cuya version no sea la que trae el constructor por defecto.
            acta.setQrPayloadVersion(qrPayloadVersion);
            acta.setCodigoQr(codigoQr);
            acta.setBloqueActual(bloqueActual);
            acta.setEstadoProcesal(estadoProcesal);
            acta.setSituacionAdministrativa(situacion);
            acta.setResultadoFinal(resultadoFinal);
            acta.setIdMotivoArchivoActual(idMotivoArchivoActual);
            acta.setPermiteReingreso(permiteReingreso);
            acta.setFhCierre(fhCierre);
            acta.setFhArchivo(fhArchivo);
            acta.setVersionRow(versionRow);
            acta.setFhUltMod(fhUltMod);
            acta.setIdUserUltMod(idUserUltMod);
            return acta;
        };

        return new ActaMapeada(id, constructor);
    }

    // =====================================================================
    // CONVERSIONES EXPLICITAS
    // =====================================================================

    /**
     * DECIMAL a Double. La conversion no agrega precision: devuelve exactamente el
     * valor que la columna almaceno con su escala fisica.
     */
    private static Double aDouble(BigDecimal valor) {
        return valor == null ? null : valor.doubleValue();
    }

    /**
     * Double a DECIMAL con la escala de la columna. El redondeo es explicito y no
     * queda a criterio del driver: DECIMAL(10,7) conserva la precision contractual del
     * esquema, que es siete decimales, no la del double de origen.
     */
    private static BigDecimal aDecimal(Double valor, int escala) {
        return valor == null ? null : BigDecimal.valueOf(valor).setScale(escala, RoundingMode.HALF_UP);
    }

    private static OrigenUbicacion origenUbicacion(Short codigo) {
        return codigo == null ? null : OrigenUbicacion.fromCodigo(codigo);
    }

    private static Short codigo(TipoActa valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static Short codigo(OrigenCaptura valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static Short codigo(OrigenUbicacion valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static Short codigo(ResultadoFinalActa valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static Short codigo(ResultadoFirmaInfractor valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static Short codigo(SituacionIdentificacionInfractor valor) {
        return valor == null ? null : Short.valueOf(valor.codigo());
    }

    private static String codigo(BloqueActual valor) {
        return valor == null ? null : valor.codigo();
    }

    private static String codigo(EstadoProcesalActa valor) {
        return valor == null ? null : valor.codigo();
    }

    private static String codigo(SituacionAdministrativaActa valor) {
        return valor == null ? null : valor.codigo();
    }
}
