package ar.gob.malvinas.faltas.prototipo.domain;

/**
 * Bloques productivos del circuito de un acta de faltas.
 *
 * <p>El código DB ({@link #codigo()}) es un {@code CHAR(4)} que identifica el
 * bloque en persistencia y en la API. Es el único valor que debe almacenarse
 * o exponerse como {@code bloque_actual}.
 *
 * <h3>Equivalencias legacy/prototipo</h3>
 * Los valores heredados del prototipo ({@code D1_CAPTURA}, {@code D2_ENRIQUECIMIENTO},
 * {@code D4_NOTIFICACION}, {@code D5_ANALISIS}) <strong>no son productivos</strong>.
 * Solo se admiten como entrada para compatibilidad a través de
 * {@link #fromLegacyOrProductive(String)}.
 *
 * <h3>D3_DOCUMENTAL eliminado</h3>
 * {@code D3_DOCUMENTAL} no es un bloque productivo. La etapa documental se
 * representa por {@code cod_bandeja = PENDIENTE_PREPARACION_DOCUMENTAL},
 * sub-bandejas documentales, {@code fal_documento} y estados documentales/firma.
 * {@link #fromLegacyOrProductive(String)} rechaza explícitamente cualquier
 * variante de D3.
 */
public enum BloqueActa {

    CAPT("CAPT", "CAPTURA",         "Captura/labrado inicial"),
    ENRI("ENRI", "ENRIQUECIMIENTO", "Enriquecimiento/completitud del acta"),
    NOTI("NOTI", "NOTIFICACION",    "Notificación del acta, fallo u otra pieza"),
    ANAL("ANAL", "ANALISIS",        "Análisis, resolución, fallo, pagos y apelación"),
    GEXT("GEXT", "GESTION_EXTERNA", "Gestión externa: apremio / juzgado de paz"),
    ARCH("ARCH", "ARCHIVO",         "Archivo administrativo/procesal"),
    CERR("CERR", "CERRADA",         "Cierre definitivo del circuito");

    private final String codigo;
    private final String nombre;
    private final String descripcion;

    BloqueActa(String codigo, String nombre, String descripcion) {
        this.codigo      = codigo;
        this.nombre      = nombre;
        this.descripcion = descripcion;
    }

    /** Código de 4 caracteres persistible como {@code CHAR(4)} en DB. */
    public String codigo() {
        return codigo;
    }

    /** Nombre semántico largo del bloque. */
    public String nombre() {
        return nombre;
    }

    /** Descripción legible del bloque. */
    public String descripcion() {
        return descripcion;
    }

    // -------------------------------------------------------------------------
    // Mapper: acepta valores productivos y legacy/prototipo
    // -------------------------------------------------------------------------

    /**
     * Resuelve un {@code BloqueActa} desde un código productivo ({@code CAPT},
     * {@code ENRI}, …) o desde un valor legacy del prototipo
     * ({@code D1_CAPTURA}, {@code D2_ENRIQUECIMIENTO}, …).
     *
     * <p>Conversiones legacy aceptadas:
     * <ul>
     *   <li>{@code D1_CAPTURA} → {@code CAPT}</li>
     *   <li>{@code D2_ENRIQUECIMIENTO} → {@code ENRI}</li>
     *   <li>{@code D4_NOTIFICACION} → {@code NOTI}</li>
     *   <li>{@code D5_ANALISIS} → {@code ANAL}</li>
     *   <li>{@code GESTION_EXTERNA} → {@code GEXT}</li>
     *   <li>{@code ARCHIVO} → {@code ARCH}</li>
     *   <li>{@code CERRADA} → {@code CERR}</li>
     * </ul>
     *
     * <p>{@code D3_DOCUMENTAL}, {@code D3} y cualquier variante documental se
     * rechazan con una excepción explícita: la etapa documental no es un bloque.
     *
     * @param value código productivo o legacy; {@code null}/blank lanza excepción
     * @return el {@code BloqueActa} correspondiente
     * @throws IllegalArgumentException si el valor es nulo, vacío o no reconocido
     * @throws UnsupportedOperationException si el valor es una variante de D3/DOCUMENTAL
     */
    public static BloqueActa fromLegacyOrProductive(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("bloque_actual no puede ser nulo ni vacío");
        }
        return switch (value.trim()) {
            // Valores productivos
            case "CAPT" -> CAPT;
            case "ENRI" -> ENRI;
            case "NOTI" -> NOTI;
            case "ANAL" -> ANAL;
            case "GEXT" -> GEXT;
            case "ARCH" -> ARCH;
            case "CERR" -> CERR;

            // Legacy/prototipo — solo para compatibilidad/migración
            case "D1_CAPTURA"         -> CAPT;
            case "D2_ENRIQUECIMIENTO" -> ENRI;
            case "D4_NOTIFICACION"    -> NOTI;
            case "D5_ANALISIS"        -> ANAL;
            case "GESTION_EXTERNA"    -> GEXT;
            case "ARCHIVO"            -> ARCH;
            case "CERRADA"            -> CERR;

            // D3 rechazado explícitamente
            case "D3_DOCUMENTAL", "D3_PREPARACION_DOCUMENTAL", "D3", "DOCUMENTAL" ->
                throw new UnsupportedOperationException(
                    "D3_DOCUMENTAL no es un bloque productivo. "
                    + "La etapa documental se representa por bandeja/sub-bandeja/documentos/firma.");

            default -> throw new IllegalArgumentException(
                "Valor de bloque_actual no reconocido: '" + value + "'");
        };
    }
}
