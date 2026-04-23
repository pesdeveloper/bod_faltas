package ar.gob.malvinas.faltas.prototipo.store;

/**
 * Constantes del prototipo compartidas dentro del package {@code store}.
 *
 * <p>Alcance intencionalmente mínimo: sólo se centralizan los literales que
 * hoy aparecen duplicados entre {@link PrototipoStore} y los soportes
 * funcionales extraídos (por ejemplo {@link ArchivoReingresoSupport}). No
 * pretende ser un catálogo general de constantes del dominio: cada área
 * puede mantener como privados los literales que sólo ella usa.
 *
 * <p>Package-private a propósito: estas constantes son detalle interno del
 * prototipo; no forman parte del contrato público del store ni del API.
 */
final class PrototipoConstantes {

    /** Bandeja operativa donde cae el análisis jurídico del acta. */
    static final String BANDEJA_PENDIENTE_ANALISIS = "PENDIENTE_ANALISIS";

    /**
     * Bandeja de actas aún en labrado o enriquecimiento (D1/D2 en el
     * recorrido mock).
     */
    static final String BANDEJA_ACTAS_EN_ENRIQUECIMIENTO = "ACTAS_EN_ENRIQUECIMIENTO";

    /** Bloque de captura en sitio (D1), previo a enriquecimiento. */
    static final String BLOQUE_D1_CAPTURA = "D1_CAPTURA";

    /** Bloque de enriquecimiento (D2). */
    static final String BLOQUE_D2_ENRIQUECIMIENTO = "D2_ENRIQUECIMIENTO";

    /** Bloque de proceso correspondiente al análisis jurídico (D5). */
    static final String BLOQUE_D5 = "D5_ANALISIS";

    /** Estado agregador usado al entrar a análisis. */
    static final String ESTADO_PENDIENTE_REVISION = "PENDIENTE_REVISION";

    /**
     * Bandeja operativa de notificación pendiente de envío. Punto de
     * entrada del circuito de notificación; también es destino al que
     * vuelve el caso desde firma cuando se completan todas las piezas.
     */
    static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";

    /**
     * Bandeja operativa intermedia "en notificación": la notificación ya
     * fue enviada y se espera resultado (entrega, no entrega o vencimiento).
     */
    static final String BANDEJA_EN_NOTIFICACION = "EN_NOTIFICACION";

    /** Bloque de proceso correspondiente a notificación (D4). */
    static final String BLOQUE_D4 = "D4_NOTIFICACION";

    /**
     * Estado agregador de una notificación que todavía no fue despachada al
     * canal (recién creada tras firma, o reinyectada por reintento).
     */
    static final String ESTADO_PENDIENTE_ENVIO = "PENDIENTE_ENVIO";

    /**
     * Macro-bandeja destino de casos derivados a gestión externa post fallo
     * notificado sin novedad en la ventana de espera posterior. Alcance
     * mínimo: representa la salida efectiva del circuito interno; no modela
     * todavía subestados internos ricos de gestión externa ni el retorno.
     */
    static final String BANDEJA_GESTION_EXTERNA = "GESTION_EXTERNA";

    /**
     * Bloque de proceso correspondiente a gestión externa. Se usa como
     * bloque, estado agregador y situación administrativa coherentes para
     * que el caso quede claramente identificado una vez derivado.
     */
    static final String BLOQUE_GESTION_EXTERNA = "GESTION_EXTERNA";

    /**
     * Estado agregador que usa una acta una vez derivada efectivamente a
     * gestión externa, para distinguirla de los otros casos en bloque
     * {@link #BLOQUE_GESTION_EXTERNA} que pudieran modelarse a futuro.
     */
    static final String ESTADO_EN_GESTION_EXTERNA = "EN_GESTION_EXTERNA";

    /**
     * Documento mock de levantamiento de medida preventiva (resolutorio de
     * cierre material; no requiere firma en el slice del prototipo).
     */
    static final String TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA =
            "DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA";

    /** Documento mock de liberación de rodado retenido/secuestrado. */
    static final String TIPO_DOC_LIBERACION_RODADO = "DOC_LIBERACION_RODADO";

    /** Documento mock de restitución / entrega de documentación retenida. */
    static final String TIPO_DOC_RESTITUCION_DOCUMENTACION = "DOC_RESTITUCION_DOCUMENTACION";

    /**
     * Ancla material mínima (expediente): acta/acuse de retención o
     * secuestro de vehículo, coherente con tránsito. Usada para reconocer
     * el origen {@code RODADO_SECUESTRADO} sin precarga aislada.
     */
    static final String TIPO_DOC_ACUSE_RETENCION_VEHICULO = "ACTA_RETENCION";

    /**
     * Ancla material mínima (expediente): constatación de retención de
     * documentación (carnet, cédula, etc.). Reconoce el origen
     * {@code DOCUMENTACION_RETENIDA} sin precarga aislada.
     */
    static final String TIPO_DOC_ACUSE_RETENCION_DOCUMENTAL = "CONSTATACION_RETENCION_DOCUMENTACION";

    /**
     * Ancla en expediente del acto / pieza de medida preventiva (mismo
     * {@code tipoDocumento} que {@code generarMedidaPreventiva} incorpora vía
     * producción de pieza). Reconoce el origen {@code MEDIDA_PREVENTIVA_ACTIVA}
     * con el mismo patrón que secuestro de rodado y retención documental.
     */
    static final String TIPO_ANCLA_MEDIDA_PREVENTIVA = "MEDIDA_PREVENTIVA";

    /** Estado documental mock para piezas resolutorias sin circuito de firma. */
    static final String ESTADO_DOC_EMITIDO = "EMITIDO";

    /** Anexo / constatación de campo aún no formalizada como pieza de firma. */
    static final String ESTADO_DOC_ADJUNTO = "ADJUNTO";

    private PrototipoConstantes() {
    }
}
