package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Situacion de identificacion del infractor al momento del labrado del acta.
 *
 * <p>Dimension propia del acta: describe si el infractor estaba presente y en que
 * grado quedo identificado. Es el catalogo cerrado de
 * {@code 10-domain/states-events-catalogs.md} y se persiste como
 * {@code SMALLINT} en {@code fal_acta.situacion_identificacion_infractor}
 * ({@code DECISION_DDL-ACTA-INFR-01}).
 *
 * <p>No se confunde con {@link ResultadoFirmaInfractor}, que califica la firma y no
 * la presencia ni la identificacion. Un infractor puede estar presente, identificarse
 * completamente y negarse a firmar: son dos hechos distintos en dos columnas
 * distintas.
 *
 * <h2>Matriz de coherencia con {@code id_persona_infractor}</h2>
 *
 * <pre>
 * IDENTIFICADO_COMPLETO     informado   persona con tipo_documento y nro_doc informados
 * IDENTIFICADO_PARCIAL      informado   persona con los datos parciales reales
 * PRESENTE_SE_NIEGA         NULL        ninguna
 * PRESENTE_NO_APORTA_DATOS  NULL        ninguna
 * AUSENTE                   NULL        ninguna
 * DATOS_INSUFICIENTES       NULL        ninguna
 * </pre>
 *
 * <p>Hay persona exactamente cuando hay identificacion, completa o parcial. En los
 * otros cuatro casos no existe sujeto identificado y la columna queda NULL: nunca
 * una persona centinela, ficticia, con documento inventado ni una PK reservada.
 *
 * <p>Los seis significados no se fusionan. {@code IDENTIFICADO_PARCIAL} y
 * {@code DATOS_INSUFICIENTES} no son el mismo estado: el primero produce una persona
 * real con los datos disponibles, el segundo no alcanza para constituirla.
 *
 * <p>La situacion no admite valor por defecto: es una calificacion juridica del acto
 * y el alta la informa siempre de forma explicita. Por eso este enum no tiene valor
 * {@code UNKNOWN}, no expone un default y {@link #fromCodigo(short)} rechaza todo
 * codigo fuera de 1..6. La persistencia usa {@link #codigo()} y nunca
 * {@code ordinal()}.
 */
public enum SituacionIdentificacionInfractor {

    /** Infractor presente e identificado con documento canonico en fal_persona. */
    IDENTIFICADO_COMPLETO((short) 1),

    /** Infractor presente que aporto datos parciales verificables sin documento completo. */
    IDENTIFICADO_PARCIAL((short) 2),

    /** Infractor presente que se niega a identificarse. */
    PRESENTE_SE_NIEGA((short) 3),

    /** Infractor presente que no aporta ningun dato utilizable. */
    PRESENTE_NO_APORTA_DATOS((short) 4),

    /** Infractor no presente en el lugar del hecho. */
    AUSENTE((short) 5),

    /** Datos obtenidos por terceros o por observacion, insuficientes para constituir una persona. */
    DATOS_INSUFICIENTES((short) 6);

    private final short codigo;

    SituacionIdentificacionInfractor(short codigo) {
        this.codigo = codigo;
    }

    public short codigo() {
        return codigo;
    }

    /**
     * Resuelve la situacion por su codigo fisico.
     *
     * @throws IllegalArgumentException si el codigo no es uno de los seis del catalogo
     */
    public static SituacionIdentificacionInfractor fromCodigo(short codigo) {
        for (SituacionIdentificacionInfractor s : values()) {
            if (s.codigo == codigo) return s;
        }
        throw new IllegalArgumentException("SituacionIdentificacionInfractor desconocida: " + codigo);
    }

    /**
     * Verdadero cuando la situacion exige una persona infractora informada, es decir
     * para {@code IDENTIFICADO_COMPLETO} e {@code IDENTIFICADO_PARCIAL}.
     */
    public boolean exigePersonaInfractora() {
        return this == IDENTIFICADO_COMPLETO || this == IDENTIFICADO_PARCIAL;
    }
}
