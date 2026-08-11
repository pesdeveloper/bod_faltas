package ar.gob.malvinas.faltas.core.application.query;

import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;

import java.util.List;

/**
 * Allowlist de campos funcionales ordenables de Persona
 * ({@code DECISION-LIST-PAGE-SORT-01}).
 *
 * <p>Mismo criterio que {@link CampoOrdenActa}: cada constante nombra un concepto
 * funcional, la traduccion a SQL vive en el adapter JDBC y el comparador equivalente en el
 * adapter InMemory. Este enum no contiene texto SQL.
 *
 * <p>Los siete campos corresponden uno a uno con una columna real de {@code fal_persona} y
 * con un getter real de {@code FalPersona}. {@code doc_key} <b>no</b> forma parte de la
 * allowlist: es una columna {@code STORED GENERATED} que {@code FalPersona} no expone
 * ({@code DECISION_DDL-PERS-01}), de modo que ordenar por ella devolveria un orden que el
 * modelo no puede explicar.
 */
public enum CampoOrdenPersona {

    /** {@code nombre_mostrar}: nombre de presentacion. Admite nulos. */
    NOMBRE_MOSTRAR("nombreMostrar"),

    /** {@code apellido}: apellido de la persona fisica. Admite nulos. */
    APELLIDO("apellido"),

    /** {@code razon_social}: razon social de la persona juridica. Admite nulos. */
    RAZON_SOCIAL("razonSocial"),

    /** {@code tipo_documento}: se ordena por el codigo canonico del enum. Admite nulos. */
    TIPO_DOCUMENTO("tipoDocumento"),

    /** {@code nro_doc}: cuerpo canonico del documento. Admite nulos. */
    NUMERO_DOCUMENTO("numeroDocumento"),

    /** {@code tipo_persona}: se ordena por el codigo canonico del enum. */
    TIPO_PERSONA("tipoPersona"),

    /** {@code fh_alta}: instante tecnico de alta. */
    FECHA_ALTA("fechaAlta");

    private final String nombreFuncional;

    CampoOrdenPersona(String nombreFuncional) {
        this.nombreFuncional = nombreFuncional;
    }

    /** Nombre funcional estable con el que un cliente puede pedir este campo. */
    public String nombreFuncional() {
        return nombreFuncional;
    }

    /**
     * Orden predeterminado de Persona: alta mas reciente primero.
     *
     * <p>Se elige {@code fh_alta} y no {@code nombre_mostrar} porque {@code fh_alta} es
     * {@code NOT NULL} y {@code nombre_mostrar} no: un orden predeterminado sobre una
     * columna nullable pondria a todas las personas sin nombre de presentacion en un extremo
     * de la primera pagina de cualquier consulta que no pida orden.
     *
     * <p>La PK no figura en esta lista: cada adapter la agrega siempre como ultimo criterio
     * de desempate.
     */
    public static List<CriterioOrden<CampoOrdenPersona>> ordenPredeterminado() {
        return List.of(CriterioOrden.desc(FECHA_ALTA));
    }

    /**
     * Resuelve el campo desde su nombre funcional, sin distinguir mayusculas. Admite
     * tambien el nombre de la constante. Un nombre desconocido se rechaza aca y nunca llega
     * a un adapter.
     */
    public static CampoOrdenPersona desdeNombreFuncional(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new PrecondicionVioladaException("campo de orden es obligatorio.");
        }
        String buscado = nombre.trim();
        for (CampoOrdenPersona campo : values()) {
            if (campo.nombreFuncional.equalsIgnoreCase(buscado)
                    || campo.name().equalsIgnoreCase(buscado)) {
                return campo;
            }
        }
        throw new PrecondicionVioladaException(
                "campo de orden desconocido para Persona: '" + nombre + "'. Campos admitidos: "
                        + nombresFuncionales() + ".");
    }

    /** Nombres funcionales admitidos, para mensajes de rechazo. */
    public static String nombresFuncionales() {
        StringBuilder sb = new StringBuilder();
        for (CampoOrdenPersona campo : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(campo.nombreFuncional);
        }
        return sb.toString();
    }
}
