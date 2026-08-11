package ar.gob.malvinas.faltas.core.infrastructure.jdbc;

import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.CriterioOrden;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.domain.enums.SujBieEstado;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PersonaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.DocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adaptador JDBC candidato de {@link PersonaRepository} sobre {@code fal_persona}.
 *
 * <p>Estilo heredado de {@code JdbcActaRepository} y {@code JdbcObservacionRepository}:
 * {@code JdbcClient} sobre el DataSource principal, SQL explicito en constantes,
 * parametros nombrados y RowMapper estatico privado. Sin {@code SELECT *}, sin
 * generacion de SQL, sin JPA ni ORM.
 *
 * <p><b>Bean activo bajo el perfil {@code jdbc}.</b> Activado en WAVE 1A2-B2 junto con
 * {@code JdbcActaRepository} y la frontera transaccional de {@code DECISION_OBS-TX-01}: los
 * dos participan del alta del acta, de modo que activar uno sin el otro habria dejado la
 * persona resuelta en memoria y el acta en MariaDB dentro de la misma operacion.
 * {@code InMemoryPersonaRepository} queda restringido a {@code !jdbc}.
 *
 * <p>Mapeo contra el DDL real, 21 columnas de las cuales 20 son insertables:
 * <pre>
 * id                   BIGINT AUTO_INCREMENT   la asigna MariaDB; nunca se informa
 * tipo_persona         SMALLINT NOT NULL       &lt;- TipoPersona.codigo()
 * tipo_documento       SMALLINT NULL           &lt;- TipoDocumentoPersona.codigo()
 * prefijo_cuit_cuil    TINYINT UNSIGNED NULL   solo CUIT/CUIL, 10..99
 * nro_doc              VARCHAR(12) NULL        forma canonica, ya normalizada en Java
 * digito_verificador   TINYINT UNSIGNED NULL   solo CUIT/CUIL, 0..9
 * doc_key              VARCHAR(12) STORED      la genera el motor; NO se informa
 * apellido             VARCHAR(24) NULL
 * nombres              VARCHAR(36) NULL
 * razon_social         VARCHAR(64) NULL
 * nombre_mostrar       VARCHAR(64) NULL
 * email_principal      VARCHAR(160) NULL
 * telefono_principal   VARCHAR(20) NULL
 * id_suj               TINYINT UNSIGNED NULL
 * id_bie               MEDIUMINT UNSIGNED NULL
 * suj_bie_estado       SMALLINT NULL           &lt;- SujBieEstado.codigo()
 * fh_suj_bie_creacion  DATETIME(6) NULL
 * fh_alta              DATETIME(6) NOT NULL    provisto por el dominio
 * id_user_alta         CHAR(36) NOT NULL       provisto por el dominio
 * fh_ult_mod           DATETIME(6) NULL
 * id_user_ult_mod      CHAR(36) NULL
 * </pre>
 *
 * <p>En ningun caso se usa {@code ordinal()}, {@code name()} ni {@code toString()} de un
 * enum: los tres enums persistibles viajan por {@code codigo()}
 * ({@code DECISION_DDL-PERS-01} seccion 9).
 *
 * <p><b>Documento.</b> Los cuatro componentes se escriben tal como los entrega
 * {@link DocumentoPersona}, que es la unica autoridad de normalizacion del dominio. El
 * adaptador no normaliza, no valida formato y no reconstruye componentes: si el valor
 * llega mal formado, lo rechaza el {@code CHECK} fisico, no una regla duplicada aqui.
 * {@code doc_key} es {@code STORED GENERATED} y por eso queda fuera del INSERT y del
 * UPDATE; se lee unicamente para validacion y no forma parte del modelo, porque
 * {@code FalPersona} no lo expone.
 *
 * <p><b>Patron P3.</b> {@code guardar} distingue alta de actualizacion por la presencia
 * del id. Con id nulo inserta sin informar la PK y la recupera con
 * {@link GeneratedKeyHolder}; con id informado actualiza la fila existente y un id
 * inexistente produce {@link PersonaNoEncontradaException} en lugar de insertarse. No
 * hay {@code REPLACE} ni {@code ON DUPLICATE KEY UPDATE}: un upsert de motor haria
 * indistinguibles el alta y la correccion.
 *
 * <p><b>OCC.</b> No aplica: {@code fal_persona} no tiene {@code version_row}
 * ({@code DECISION_DDL-PERS-01}). El adaptador no la agrega ni la simula.
 *
 * <p><b>Tiempo y actor.</b> {@code fh_alta}, {@code fh_ult_mod}, {@code id_user_alta} e
 * {@code id_user_ult_mod} se persisten tal como los entrega el dominio
 * ({@code FaltasClock} es la fuente de tiempo). No se usa {@code NOW()},
 * {@code CURRENT_TIMESTAMP} ni un usuario por defecto. {@code DATETIME(6)} conserva
 * microsegundos: un {@code LocalDateTime} con precision de nanosegundos se redondea al
 * microsegundo mas cercano en la base.
 *
 * <p><b>Transaccionalidad.</b> Este adaptador no abre transacciones. La frontera vive en
 * la capa de aplicacion.
 *
 * <h2>Consulta paginada</h2>
 *
 * {@code DECISION-LIST-PAGE-SORT-01}. El {@code WHERE} se compone con los fragmentos
 * constantes de {@link #FILTROS} y valores enlazados; el {@code ORDER BY} sale de
 * {@link #columnaDeOrden(CampoOrdenPersona)}, un {@code switch} exhaustivo sobre la
 * allowlist. Misma politica de nulos que {@code JdbcActaRepository}: el orden nativo de
 * MariaDB, sin envolver columnas en funciones que impidan usar sus indices.
 *
 * <p>Este adaptador <b>no compone observaciones</b> en ninguna de sus lecturas y por lo tanto
 * no tiene N+1 que resolver: {@code fal_persona} no participa del vinculo propietario de
 * {@code fal_observacion} en el codigo vigente.
 */
@Repository
@Profile("jdbc")
public class JdbcPersonaRepository implements PersonaRepository {

    /**
     * Las 20 columnas del modelo. {@code doc_key} se lee aparte cuando hace falta como
     * evidencia: no pertenece a {@link FalPersona} y agregarlo al dominio solo porque
     * existe fisicamente seria invertir la direccion de la decision.
     */
    private static final String COLUMNAS = """
            id, tipo_persona, tipo_documento, prefijo_cuit_cuil, nro_doc,
            digito_verificador, apellido, nombres, razon_social, nombre_mostrar,
            email_principal, telefono_principal, id_suj, id_bie, suj_bie_estado,
            fh_suj_bie_creacion, fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            """;

    /**
     * La PK se omite deliberadamente: la asigna el {@code AUTO_INCREMENT}
     * ({@code DECISION_ID-01}). {@code doc_key} se omite porque es
     * {@code STORED GENERATED} y MariaDB rechaza informarla.
     */
    private static final String SQL_INSERT = """
            INSERT INTO fal_persona (
                tipo_persona, tipo_documento, prefijo_cuit_cuil, nro_doc,
                digito_verificador, apellido, nombres, razon_social, nombre_mostrar,
                email_principal, telefono_principal, id_suj, id_bie, suj_bie_estado,
                fh_suj_bie_creacion, fh_alta, id_user_alta, fh_ult_mod, id_user_ult_mod
            ) VALUES (
                :tipoPersona, :tipoDocumento, :prefijoCuitCuil, :nroDoc,
                :digitoVerificador, :apellido, :nombres, :razonSocial, :nombreMostrar,
                :emailPrincipal, :telefonoPrincipal, :idSuj, :idBie, :sujBieEstado,
                :fhSujBieCreacion, :fhAlta, :idUserAlta, :fhUltMod, :idUserUltMod
            )
            """;

    /**
     * El SET lista las 19 columnas mutables completas: no se construye dinamicamente
     * omitiendo nulos, de modo que llevar una columna nullable a NULL se persiste. Es lo
     * que hace que quitar el documento, o pasar de CUIT a pasaporte, limpie realmente el
     * prefijo y el digito en lugar de dejarlos residuales.
     *
     * <p>{@code fh_alta} e {@code id_user_alta} participan del SET porque el modelo los
     * declara finales y toda actualizacion los reenvia con el mismo valor leido: no se
     * excluyen del UPDATE para no crear una segunda lista de columnas que mantener.
     */
    private static final String SQL_UPDATE = """
            UPDATE fal_persona
               SET tipo_persona = :tipoPersona,
                   tipo_documento = :tipoDocumento,
                   prefijo_cuit_cuil = :prefijoCuitCuil,
                   nro_doc = :nroDoc,
                   digito_verificador = :digitoVerificador,
                   apellido = :apellido,
                   nombres = :nombres,
                   razon_social = :razonSocial,
                   nombre_mostrar = :nombreMostrar,
                   email_principal = :emailPrincipal,
                   telefono_principal = :telefonoPrincipal,
                   id_suj = :idSuj,
                   id_bie = :idBie,
                   suj_bie_estado = :sujBieEstado,
                   fh_suj_bie_creacion = :fhSujBieCreacion,
                   fh_alta = :fhAlta,
                   id_user_alta = :idUserAlta,
                   fh_ult_mod = :fhUltMod,
                   id_user_ult_mod = :idUserUltMod
             WHERE id = :id
            """;

    private static final String SQL_POR_ID = "SELECT " + COLUMNAS + """
            FROM fal_persona
            WHERE id = :id
            """;

    private static final String SQL_TODAS = "SELECT " + COLUMNAS + """
            FROM fal_persona
            ORDER BY id ASC
            """;

    /**
     * Busqueda por identidad natural. El filtro compara {@code tipo_documento} y
     * {@code nro_doc}, que son exactamente los dos componentes que el puerto transporta.
     *
     * <p>Deliberadamente <b>no</b> filtra por {@code doc_key}. Para CUIT y CUIL,
     * {@code doc_key} son los once digitos que el motor compone con el prefijo y el
     * digito verificador, y el puerto recibe unicamente el cuerpo central canonico: no
     * hay forma de reconstruir la clave desde su firma sin inventar los dos componentes
     * faltantes. Filtrar por {@code doc_key} exigiria una concatenacion incompatible con
     * {@link DocumentoPersona}, que es justamente lo que la decision prohibe.
     *
     * <p>Comparar {@code nro_doc} tambien es lo que hace el oraculo de paridad
     * {@code InMemoryPersonaRepository}, que filtra por {@code getNroDoc()}. Consecuencia
     * declarada y compartida por los dos adaptadores: un CUIT y un CUIL con el mismo
     * cuerpo central se distinguen por el tipo, pero dos CUIT que solo difieren en el
     * prefijo o en el digito comparten cuerpo y aparecen juntos. Por eso el contrato del
     * puerto devuelve lista y no promete unicidad.
     *
     * <p>Nota de plan: el prefijo izquierdo {@code tipo_documento} del UNIQUE
     * {@code uq_fal_persona_tipo_doc_key} sirve el primer predicado; {@code nro_doc} no
     * esta indexado y se resuelve por filtro. Con el volumen actual es irrelevante y
     * agregar un indice es una decision fisica que este slice no toma.
     */
    private static final String SQL_POR_DOCUMENTO = "SELECT " + COLUMNAS + """
            FROM fal_persona
            WHERE tipo_documento = :tipoDocumento
              AND nro_doc = :nroDoc
            ORDER BY id ASC
            """;

    /**
     * Mismo predicado que el adaptador InMemory: exige los dos valores informados y los
     * compara por igualdad. Un {@code idSuj} o un {@code idBie} nulo no busca filas con
     * NULL, devuelve vacio; es la semantica vigente y este adaptador no la reinterpreta.
     */
    private static final String SQL_POR_ID_SUJ_BIE = "SELECT " + COLUMNAS + """
            FROM fal_persona
            WHERE id_suj = :idSuj
              AND id_bie = :idBie
            ORDER BY id ASC
            """;

    // =====================================================================
    // CONSULTA PAGINADA (DECISION-LIST-PAGE-SORT-01)
    // =====================================================================

    /** Columna de desempate: la PK, para que el orden total sea estricto. */
    private static final String COLUMNA_DESEMPATE = "id";

    /**
     * Los siete predicados del filtro tipado. Cada fragmento es una constante y cada valor
     * viaja enlazado: no hay forma de que un dato del cliente entre en el texto del SQL.
     */
    private static final List<PredicadoFiltro> FILTROS = List.of(
            new PredicadoFiltro("tipo_persona = :fTipoPersona", "fTipoPersona",
                    f -> codigo(f.tipoPersona()), Types.SMALLINT),
            new PredicadoFiltro("tipo_documento = :fTipoDocumento", "fTipoDocumento",
                    f -> codigo(f.tipoDocumento()), Types.SMALLINT),
            new PredicadoFiltro("nro_doc = :fNroDoc", "fNroDoc",
                    FiltroPersona::nroDoc, Types.VARCHAR),
            new PredicadoFiltro("id_suj = :fIdSuj", "fIdSuj",
                    FiltroPersona::idSuj, Types.TINYINT),
            new PredicadoFiltro("id_bie = :fIdBie", "fIdBie",
                    FiltroPersona::idBie, Types.INTEGER),
            new PredicadoFiltro("fh_alta >= :fFhAltaDesde", "fFhAltaDesde",
                    FiltroPersona::fhAltaDesde, Types.TIMESTAMP),
            new PredicadoFiltro("fh_alta <= :fFhAltaHasta", "fFhAltaHasta",
                    FiltroPersona::fhAltaHasta, Types.TIMESTAMP));

    /** Un predicado: fragmento constante, parametro, extractor del valor y tipo JDBC. */
    private record PredicadoFiltro(String sql, String parametro,
                                   Function<FiltroPersona, Object> valor, int tipo) {
    }

    private final JdbcClient jdbc;

    public JdbcPersonaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // =====================================================================
    // ESCRITURA
    // =====================================================================

    @Override
    public FalPersona guardar(FalPersona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("persona es obligatoria");
        }
        return persona.getId() == null ? insertar(persona) : actualizar(persona);
    }

    /**
     * Alta. El INSERT nunca informa la PK y la recupera del motor. El argumento no se
     * muta: el llamador debe continuar con la copia devuelta.
     */
    private FalPersona insertar(FalPersona persona) {
        KeyHolder claves = new GeneratedKeyHolder();
        vincularColumnas(jdbc.sql(SQL_INSERT), persona).update(claves);

        Number generado = claves.getKey();
        if (generado == null) {
            throw new IllegalStateException(
                    "fal_persona no devolvio el id generado para la persona nueva");
        }
        return persona.conId(generado.longValue());
    }

    /**
     * Actualizacion. Cero filas afectadas solo puede significar que el id no existe: no
     * hay guarda de version en el WHERE porque la tabla no tiene OCC. Un id elegido por
     * el llamador nunca se inserta.
     *
     * <p>La excepcion y su mensaje son los mismos que produce el adaptador InMemory.
     */
    private FalPersona actualizar(FalPersona persona) {
        int filas = vincularColumnas(jdbc.sql(SQL_UPDATE), persona)
                .param("id", persona.getId(), Types.BIGINT)
                .update();

        if (filas == 0) {
            throw new PersonaNoEncontradaException(persona.getId());
        }
        return persona.copia();
    }

    /**
     * Las 19 columnas mutables, compartidas por el INSERT y por el UPDATE. Tenerlas en un
     * unico lugar evita dos listas de parametros que haya que mantener sincronizadas.
     */
    private static JdbcClient.StatementSpec vincularColumnas(JdbcClient.StatementSpec spec,
                                                             FalPersona p) {
        DocumentoPersona documento = p.getDocumento();
        return spec
                .param("tipoPersona", codigo(p.getTipoPersona()), Types.SMALLINT)
                .param("tipoDocumento", codigo(documento.tipoDoc()), Types.SMALLINT)
                .param("prefijoCuitCuil", documento.prefijoCuitCuil(), Types.TINYINT)
                .param("nroDoc", documento.nroDoc(), Types.VARCHAR)
                .param("digitoVerificador", documento.digitoVerificador(), Types.TINYINT)
                .param("apellido", p.getApellido(), Types.VARCHAR)
                .param("nombres", p.getNombres(), Types.VARCHAR)
                .param("razonSocial", p.getRazonSocial(), Types.VARCHAR)
                .param("nombreMostrar", p.getNombreMostrar(), Types.VARCHAR)
                .param("emailPrincipal", p.getEmailPrincipal(), Types.VARCHAR)
                .param("telefonoPrincipal", p.getTelefonoPrincipal(), Types.VARCHAR)
                .param("idSuj", p.getIdSuj(), Types.TINYINT)
                .param("idBie", p.getIdBie(), Types.INTEGER)
                .param("sujBieEstado", codigo(p.getSujBieEstado()), Types.SMALLINT)
                .param("fhSujBieCreacion", p.getFhSujBieCreacion(), Types.TIMESTAMP)
                .param("fhAlta", p.getFhAlta(), Types.TIMESTAMP)
                .param("idUserAlta", p.getIdUserAlta(), Types.CHAR)
                .param("fhUltMod", p.getFhUltMod(), Types.TIMESTAMP)
                .param("idUserUltMod", p.getIdUserUltMod(), Types.CHAR);
    }

    // =====================================================================
    // LECTURA
    // =====================================================================

    @Override
    public Optional<FalPersona> buscarPorId(Long id) {
        return jdbc.sql(SQL_POR_ID)
                .param("id", id, Types.BIGINT)
                .query(JdbcPersonaRepository::mapear)
                .optional();
    }

    @Override
    public List<FalPersona> listarTodas() {
        return jdbc.sql(SQL_TODAS)
                .query(JdbcPersonaRepository::mapear)
                .list();
    }

    /**
     * Recibe la forma canonica que se persiste, no un valor con separadores: la
     * normalizacion es responsabilidad de la capa de aplicacion, que la resuelve con
     * {@link DocumentoPersona}. Un par incompleto no puede coincidir con ninguna fila y
     * devuelve lista vacia, igual que el adaptador InMemory.
     */
    @Override
    public List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc) {
        if (tipoDoc == null || nroDoc == null) {
            return List.of();
        }
        return jdbc.sql(SQL_POR_DOCUMENTO)
                .param("tipoDocumento", codigo(tipoDoc), Types.SMALLINT)
                .param("nroDoc", nroDoc, Types.VARCHAR)
                .query(JdbcPersonaRepository::mapear)
                .list();
    }

    @Override
    public List<FalPersona> buscarPorIdSujBie(Integer idSuj, Integer idBie) {
        if (idSuj == null || idBie == null) {
            return List.of();
        }
        return jdbc.sql(SQL_POR_ID_SUJ_BIE)
                .param("idSuj", idSuj, Types.TINYINT)
                .param("idBie", idBie, Types.INTEGER)
                .query(JdbcPersonaRepository::mapear)
                .list();
    }

    /**
     * Pagina de personas resuelta enteramente en el motor: {@code WHERE}, {@code ORDER BY},
     * {@code LIMIT} y {@code OFFSET}.
     *
     * <p>Cardinalidad: una consulta de conteo cuando se pide el total y una de la pagina. No
     * hay composicion de colecciones hijas, de modo que no hay consulta batch.
     */
    @Override
    public PaginaResultado<FalPersona> listarPagina(FiltroPersona filtro,
                                                    PaginaSolicitada<CampoOrdenPersona> pagina) {
        FiltroPersona f = filtro == null ? FiltroPersona.ninguno() : filtro;
        PaginaSolicitada<CampoOrdenPersona> p =
                pagina == null ? PaginaSolicitada.primeraPagina() : pagina;

        List<PredicadoFiltro> activos = predicadosActivos(f);
        String where = clausulaWhere(activos);

        Long total = null;
        if (p.incluirTotal()) {
            total = vincularFiltro(jdbc.sql("SELECT COUNT(*) FROM fal_persona" + where), activos, f)
                    .query(Long.class)
                    .single();
            if (total == 0L || p.offset() >= total) {
                return new PaginaResultado<>(List.of(), p.page(), p.size(), total, false);
            }
        }

        int limite = p.incluirTotal() ? p.size() : p.size() + 1;
        String sql = "SELECT " + COLUMNAS + " FROM fal_persona" + where
                + " ORDER BY " + expresionOrden(p.orden())
                + " LIMIT :paginaLimite OFFSET :paginaDesplazamiento";

        List<FalPersona> filas =
                vincularFiltro(jdbc.sql(sql), activos, f)
                        .param("paginaLimite", limite, Types.INTEGER)
                        .param("paginaDesplazamiento", p.offset(), Types.BIGINT)
                        .query(JdbcPersonaRepository::mapear)
                        .list();

        boolean haySiguiente = !p.incluirTotal() && filas.size() > p.size();
        if (haySiguiente) {
            filas = filas.subList(0, p.size());
        }

        return p.incluirTotal()
                ? PaginaResultado.conTotal(filas, p.page(), p.size(), total)
                : PaginaResultado.sinTotal(filas, p.page(), p.size(), haySiguiente);
    }

    /** Predicados cuyo valor esta informado. Los demas no participan del {@code WHERE}. */
    private static List<PredicadoFiltro> predicadosActivos(FiltroPersona filtro) {
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

    /** Enlaza el valor de cada predicado activo. */
    private static JdbcClient.StatementSpec vincularFiltro(
            JdbcClient.StatementSpec spec, List<PredicadoFiltro> activos, FiltroPersona filtro) {
        JdbcClient.StatementSpec resultado = spec;
        for (PredicadoFiltro predicado : activos) {
            resultado = resultado.param(
                    predicado.parametro(), predicado.valor().apply(filtro), predicado.tipo());
        }
        return resultado;
    }

    /**
     * Criterios pedidos, o el orden predeterminado de la entidad, mas la PK como ultimo
     * desempate con la direccion del ultimo criterio.
     */
    private static String expresionOrden(List<CriterioOrden<CampoOrdenPersona>> criterios) {
        List<CriterioOrden<CampoOrdenPersona>> efectivos =
                criterios.isEmpty() ? CampoOrdenPersona.ordenPredeterminado() : criterios;
        StringBuilder sb = new StringBuilder();
        for (CriterioOrden<CampoOrdenPersona> criterio : efectivos) {
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
     * Traduce un campo funcional de la allowlist a su columna fisica. {@code switch}
     * exhaustivo sin {@code default}: agregar una constante sin decidir su columna rompe la
     * compilacion en lugar de producir un {@code ORDER BY} con la palabra {@code null}.
     */
    private static String columnaDeOrden(CampoOrdenPersona campo) {
        return switch (campo) {
            case NOMBRE_MOSTRAR -> "nombre_mostrar";
            case APELLIDO -> "apellido";
            case RAZON_SOCIAL -> "razon_social";
            case TIPO_DOCUMENTO -> "tipo_documento";
            case NUMERO_DOCUMENTO -> "nro_doc";
            case TIPO_PERSONA -> "tipo_persona";
            case FECHA_ALTA -> "fh_alta";
        };
    }

    // =====================================================================
    // MAPEO
    // =====================================================================

    private static Short codigo(TipoPersona tipo) {
        return tipo == null ? null : tipo.codigo();
    }

    private static Short codigo(TipoDocumentoPersona tipo) {
        return tipo == null ? null : tipo.codigo();
    }

    private static Short codigo(SujBieEstado estado) {
        return estado == null ? null : estado.codigo();
    }

    /**
     * El documento se reconstruye por el constructor de {@link DocumentoPersona}, que
     * revalida sus invariantes: una fila que el motor acepto pero que el dominio
     * considera incoherente falla al leerse en lugar de propagarse silenciosamente.
     */
    private static FalPersona mapear(ResultSet rs, int fila) throws SQLException {
        FalPersona persona = new FalPersona(
                rs.getObject("id", Long.class),
                TipoPersona.fromCodigo(rs.getShort("tipo_persona")),
                rs.getObject("fh_alta", LocalDateTime.class),
                rs.getString("id_user_alta"));

        persona.setDocumento(documentoDe(rs));
        persona.setApellido(rs.getString("apellido"));
        persona.setNombres(rs.getString("nombres"));
        persona.setRazonSocial(rs.getString("razon_social"));
        persona.setNombreMostrar(rs.getString("nombre_mostrar"));
        persona.setEmailPrincipal(rs.getString("email_principal"));
        persona.setTelefonoPrincipal(rs.getString("telefono_principal"));
        persona.setSujBieEstado(sujBieEstado(rs.getObject("suj_bie_estado", Short.class)));
        persona.setIdSuj(rs.getObject("id_suj", Integer.class));
        persona.setIdBie(rs.getObject("id_bie", Integer.class));
        persona.setFhSujBieCreacion(rs.getObject("fh_suj_bie_creacion", LocalDateTime.class));
        persona.setFhUltMod(rs.getObject("fh_ult_mod", LocalDateTime.class));
        persona.setIdUserUltMod(rs.getString("id_user_ult_mod"));
        return persona;
    }

    private static DocumentoPersona documentoDe(ResultSet rs) throws SQLException {
        Short tipoDocumento = rs.getObject("tipo_documento", Short.class);
        if (tipoDocumento == null) {
            return DocumentoPersona.ausente();
        }
        return new DocumentoPersona(
                TipoDocumentoPersona.fromCodigo(tipoDocumento),
                rs.getString("nro_doc"),
                rs.getObject("prefijo_cuit_cuil", Integer.class),
                rs.getObject("digito_verificador", Integer.class));
    }

    private static SujBieEstado sujBieEstado(Short codigo) {
        return codigo == null ? null : SujBieEstado.fromCodigo(codigo);
    }
}
