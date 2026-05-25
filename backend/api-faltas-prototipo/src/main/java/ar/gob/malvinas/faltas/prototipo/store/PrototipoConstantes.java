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
     * Macro-bandeja de actas con expediente dado de baja operativa. No aplica
     * dictar resolutorios de cierre mientras el caso no reingresa.
     */
    static final String BANDEJA_ARCHIVO = "ARCHIVO";

    /**
     * Actas conclusas en el trámite. No aplica acciones resolutorias
     * incrementales.
     */
    static final String BANDEJA_CERRADAS = "CERRADAS";

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
     * cierre material; en el tronco por defecto: emitido, sin ingreso al
     * circuito de notificación de acta).
     */
    static final String TIPO_DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA =
            "DOC_LEVANTAMIENTO_MEDIDA_PREVENTIVA";

    /**
     * Forma documental posible del único eje material
     * {@code LEVANTAMIENTO_MEDIDA_PREVENTIVA} (parámetro
     * {@code documentoConCircuitoFirmaNotif} al registrar resolutorio). No
     * constituye un bloqueante de cierre distinto: solo fija
     * {@linkplain #ESTADO_DOC_PENDIENTE_FIRMA} y, tras firma in-situ, trazas
     * lógicas sin notificar el acta.
     */
    static final String TIPO_DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF =
            "DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF";

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

    /**
     * Mismo sentido operativo que en
     * {@code PiezasFirmaSupport} para producción de pieza.
     */
    static final String ESTADO_DOC_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";

    /**
     * Mismo sentido operativo que en
     * {@code PiezasFirmaSupport} luego de firma individual.
     */
    static final String ESTADO_DOC_FIRMADO = "FIRMADO";

    /** Anexo / constatación de campo aún no formalizada como pieza de firma. */
    static final String ESTADO_DOC_ADJUNTO = "ADJUNTO";

    /**
     * Documento mock del fallo absolutorio dictado por la autoridad competente
     * en la etapa de análisis jurídico. Comparte el circuito documental
     * existente: se incorpora en {@link #ESTADO_DOC_PENDIENTE_FIRMA} al
     * dictarse, pasa a {@link #ESTADO_DOC_FIRMADO} al firmarse y luego se
     * notifica vía {@code registrar-notificacion-positiva} reutilizando el
     * endpoint existente.
     */
    static final String TIPO_DOC_FALLO_ABSOLUTORIO = "FALLO_ABSOLUTORIO";

    /**
     * Documento mock del fallo condenatorio dictado por la autoridad
     * competente en la etapa de análisis jurídico. Mismo circuito que el
     * fallo absolutorio: PENDIENTE_FIRMA → FIRMADO → notificación positiva.
     * La notificación positiva del fallo condenatorio abre el plazo de
     * apelación, que se modela en este slice solo como flag operativo (sin
     * cálculo real de días).
     */
    static final String TIPO_DOC_FALLO_CONDENATORIO = "FALLO_CONDENATORIO";

    /**
     * @return {@code true} si {@code tipoDocumento} corresponde a uno de los
     *     dos tipos de fallo dictados en análisis (absolutorio o
     *     condenatorio). Usado por el circuito jurídico para distinguir
     *     notificación de acta de notificación de fallo.
     */
    static boolean esFallo(String tipoDocumento) {
        return TIPO_DOC_FALLO_ABSOLUTORIO.equals(tipoDocumento)
                || TIPO_DOC_FALLO_CONDENATORIO.equals(tipoDocumento);
    }

    /**
     * @return {@code true} salvo cierre, derivación a gestión externa, archivo
     *     o bandeja terminal; dictar resolutorio o registrar cumplimiento
     *     material no se limita a {@link #BANDEJA_PENDIENTE_ANALISIS}.
     */
    static boolean bandejaHabilitaResolucionBloqueoCierre(
            boolean actaCerrada, String bandejaActual) {
        if (actaCerrada) {
            return false;
        }
        if (bandejaActual == null) {
            return false;
        }
        if (BANDEJA_GESTION_EXTERNA.equals(bandejaActual)
                || BANDEJA_ARCHIVO.equals(bandejaActual)
                || BANDEJA_CERRADAS.equals(bandejaActual)) {
            return false;
        }
        return true;
    }

    /**
     * Permite originar / continuar el flujo de pago voluntario en cualquier
     * bandeja interna operable. Excluye únicamente las bandejas
     * terminales o externas ({@link #BANDEJA_ARCHIVO}, {@link
     * #BANDEJA_CERRADAS}, {@link #BANDEJA_GESTION_EXTERNA}) y las actas
     * cerradas.
     *
     * <p>Decisión funcional: el infractor siempre puede pagar mientras el
     * expediente esté en una etapa interna operable; la spec admite
     * originar la solicitud desde labradas o enriquecimiento (ver
     * {@code spec/03-bandejas/01-bandeja-labradas.md} y
     * {@code spec/03-bandejas/02-bandeja-enriquecimiento.md}) y centraliza
     * la evaluación posterior en la bandeja de análisis / presentaciones /
     * pagos ({@code spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md}),
     * pero el origen no se restringe a una sola bandeja interna.
     */
    static boolean bandejaPermitePagoVoluntario(
            boolean actaCerrada, String bandejaActual) {
        if (actaCerrada) {
            return false;
        }
        if (bandejaActual == null) {
            return false;
        }
        if (BANDEJA_GESTION_EXTERNA.equals(bandejaActual)
                || BANDEJA_ARCHIVO.equals(bandejaActual)
                || BANDEJA_CERRADAS.equals(bandejaActual)) {
            return false;
        }
        return true;
    }

    static boolean esResolutorioBloqueoCierreCircuitoFirmaYNotif(String tipoDocumento) {
        return TIPO_DOC_LEVANTAMIENTO_MEDIDA_CIRCUITO_FIRMA_NOTIF.equals(tipoDocumento);
    }

    private PrototipoConstantes() {
    }
}
