package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.util.List;

/**
 * Allowlist de campos funcionales ordenables de Acta
 * ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Cada constante nombra un <b>concepto funcional</b>, no una columna. La traduccion a
 * SQL vive en el adapter JDBC, en una constante interna suya, y el comparador equivalente
 * vive en el adapter InMemory. Este enum no contiene texto SQL a proposito: si lo
 * contuviera, la capa de aplicacion pasaria a conocer nombres fisicos y la allowlist
 * dejaria de ser el unico punto donde se decide que se puede ordenar.
 *
 * <h2>Por que estos campos y no otros</h2>
 *
 * Los nueve campos corresponden uno a uno con una columna real de {@code fal_acta} y con un
 * getter real de {@code FalActa}. No se incluyen campos que no existan materialmente.
 *
 * <p><b>{@code titular} no forma parte de la allowlist.</b> {@code fal_acta} no tiene
 * columna de titular: la identidad del infractor vive en {@code fal_persona} a traves de
 * {@code id_persona_infractor}, y {@code FalActa.infractorNombre} es un campo de
 * compatibilidad que la tabla no persiste y que vuelve nulo en un round-trip. Ordenar por
 * titular exige un {@code LEFT JOIN} a {@code fal_persona} y una politica de nulos propia,
 * cuyo plan e indices pertenecen a la wave de optimizacion fisica registrada en
 * {@code D-PERSISTENCE-PAGING-INDEX-01}. Exponerlo ahora prometeria un orden sobre un dato
 * que la entidad no transporta. Se puede ordenar por {@link #PERSONA_INFRACTOR}, que si es
 * una columna de la tabla.
 */
public enum CampoOrdenActa {

    /** {@code nro_acta}: numeracion funcional del acta. Admite nulos. */
    NRO_ACTA("nroActa"),

    /** {@code fecha_acta}: fecha funcional del acta. */
    FECHA_ACTA("fechaActa"),

    /** {@code fecha_labrado}: instante funcional del labrado. */
    FECHA_LABRADO("fechaLabrado"),

    /** {@code fh_alta}: instante tecnico de alta. */
    FECHA_ALTA("fechaAlta"),

    /** {@code tipo_acta}: se ordena por el codigo canonico del enum, nunca por su nombre. */
    TIPO_ACTA("tipoActa"),

    /** {@code bloque_actual}: bloque funcional vigente. */
    BLOQUE_ACTUAL("bloqueActual"),

    /** {@code est_proc_act}: estado procesal vigente. */
    ESTADO_PROCESAL("estadoProcesal"),

    /** {@code sit_adm_act}: situacion administrativa vigente. */
    SITUACION_ADMINISTRATIVA("situacionAdministrativa"),

    /** {@code id_dep}: dependencia que labro el acta. */
    DEPENDENCIA("dependencia"),

    /** {@code id_insp}: inspector que labro el acta. Admite nulos. */
    INSPECTOR("inspector"),

    /** {@code id_persona_infractor}: FK a la persona infractora. Admite nulos. */
    PERSONA_INFRACTOR("personaInfractor");

    private final String nombreFuncional;

    CampoOrdenActa(String nombreFuncional) {
        this.nombreFuncional = nombreFuncional;
    }

    /** Nombre funcional estable con el que un cliente puede pedir este campo. */
    public String nombreFuncional() {
        return nombreFuncional;
    }

    /**
     * Orden predeterminado de Acta: acta mas reciente primero.
     *
     * <p>La PK no figura en esta lista porque no es un campo funcional que el cliente pueda
     * pedir: cada adapter la agrega siempre como ultimo criterio, tanto al orden
     * predeterminado como a cualquier orden solicitado. Ese desempate no es decorativo. Sin
     * el, dos actas con la misma {@code fecha_acta} pueden llegar en distinto orden en dos
     * ejecuciones de la misma consulta, y entonces la pagina 1 puede repetir u omitir filas
     * que ya vinieron en la pagina 0 sobre un conjunto que no cambio.
     */
    public static List<CriterioOrden<CampoOrdenActa>> ordenPredeterminado() {
        return List.of(CriterioOrden.desc(FECHA_ACTA));
    }

    /**
     * Resuelve el campo desde su nombre funcional, sin distinguir mayusculas. Admite
     * tambien el nombre de la constante.
     *
     * <p>Este metodo es la frontera de la allowlist: un nombre desconocido se rechaza aca y
     * nunca llega a un adapter. Un cliente no puede nombrar una columna, un alias, una
     * funcion ni un fragmento de SQL porque ninguno de esos textos resuelve a una constante.
     */
    public static CampoOrdenActa desdeNombreFuncional(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new PrecondicionVioladaException("campo de orden es obligatorio.");
        }
        String buscado = nombre.trim();
        for (CampoOrdenActa campo : values()) {
            if (campo.nombreFuncional.equalsIgnoreCase(buscado)
                    || campo.name().equalsIgnoreCase(buscado)) {
                return campo;
            }
        }
        throw new PrecondicionVioladaException(
                "campo de orden desconocido para Acta: '" + nombre + "'. Campos admitidos: "
                        + nombresFuncionales() + ".");
    }

    /** Nombres funcionales admitidos, para mensajes de rechazo. */
    public static String nombresFuncionales() {
        StringBuilder sb = new StringBuilder();
        for (CampoOrdenActa campo : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(campo.nombreFuncional);
        }
        return sb.toString();
    }
}
