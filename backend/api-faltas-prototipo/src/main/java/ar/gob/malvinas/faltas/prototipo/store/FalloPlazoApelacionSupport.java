package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D4;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_FIRMADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_PENDIENTE_FIRMA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_FALLO_ABSOLUTORIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_FALLO_CONDENATORIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.esFallo;

/**
 * Circuito jurídico mínimo de fallo y plazo de apelación. Concentra:
 *
 * <ul>
 *   <li>dictado de fallo absolutorio y condenatorio desde
 *       {@code PENDIENTE_ANALISIS} (genera documento mock
 *       {@code FALLO_ABSOLUTORIO} / {@code FALLO_CONDENATORIO} en
 *       {@code PENDIENTE_FIRMA} y mueve el acta a la bandeja
 *       {@code PENDIENTE_FIRMA}; no cambia {@code resultadoFinal});</li>
 *   <li>detección de "fallo pendiente de notificación" para que el endpoint
 *       genérico {@code registrar-notificacion-positiva} se interprete como
 *       notificación de fallo cuando corresponda;</li>
 *   <li>materialización de la notificación positiva del fallo: vuelve el
 *       caso a {@code PENDIENTE_ANALISIS} con
 *       {@code estadoProceso PENDIENTE_REVISION}, fija
 *       {@code resultadoFinal} ({@code ABSUELTO} para absolutorio o
 *       {@code CONDENADO} para condenatorio), y, en el caso condenatorio,
 *       abre el plazo de apelación;</li>
 *   <li>registro mock del vencimiento del plazo de apelación (sin cálculo
 *       real de días): si vence sin apelación, el resultado pasa a
 *       {@link PrototipoStore.ResultadoFinalCierreMock#CONDENA_FIRME} y el
 *       portal del infractor deja de habilitar la presentación de
 *       apelación.</li>
 * </ul>
 *
 * <p>No materializa comprobantes (EM/RC/Cmte/Pref/Nro) ni domicilio
 * electrónico: alcance estricto del slice. La presentación de apelación
 * se registra con la acción {@code registrar-apelacion}; la resolución mock
 * del recurso presentado vive en {@code resolver-apelacion}.
 */
final class FalloPlazoApelacionSupport {

    private static final String BANDEJA_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";
    private static final String SITUACION_ADMIN_ACTIVA = "ACTIVA";
    private static final String ESTADO_PROCESO_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String ESTADO_NOTIFICACION_ENTREGADA = "ENTREGADA";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final CerrabilidadSupport cerrabilidad;

    /**
     * Marca operativa demo de "plazo de apelación abierto" por acta. Mock
     * puro: el slice no calcula días; el vencimiento se materializa con la
     * acción dedicada {@code registrar-vencimiento-plazo-apelacion}.
     */
    private final Map<String, Boolean> plazoApelacionAbiertoPorActa = new HashMap<>();

    /**
     * Marca operativa demo de apelación/recurso ya presentado por acta.
     * Mock puro: no modela elevación del recurso.
     */
    private final Map<String, Boolean> apelacionPresentadaPorActa = new HashMap<>();

    /**
     * Marca operativa demo de apelación/recurso ya resuelto por acta.
     * Mock puro: no modela instancia externa ni pago post condena firme.
     */
    private final Map<String, Boolean> apelacionResueltaPorActa = new HashMap<>();

    /**
     * Resultado mock de la resolución ({@code RECHAZADA} o
     * {@code ACEPTADA_ABSUELVE}) conservado por acta.
     */
    private final Map<String, String> resultadoResolucionApelacionPorActa = new HashMap<>();

    /** Canal de presentación conservado por acta ({@code PORTAL_INFRACTOR} o
     * {@code PRESENCIAL_DIRECCION}). */
    private final Map<String, String> canalApelacionPorActa = new HashMap<>();

    /** Monto de condena fijado al dictar fallo condenatorio (distinto de pago voluntario). */
    private final Map<String, java.math.BigDecimal> montoCondenaPorActa;

    /** Situacion de pago de condena compartida con {@link PagoCondenaSupport}. */
    private final Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa;

    /** Situacion de pago voluntario compartida con {@link PagoInformadoSupport}. */
    private final Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa;

    /** Tipo de pago compartido con {@link PagoCondenaSupport}. */
    private final Map<String, PrototipoStore.TipoPago> tipoPagoPorActa;

    FalloPlazoApelacionSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaDocumentoMock>> documentosPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            Map<String, String> accionPendientePorActa,
            CerrabilidadSupport cerrabilidad,
            Map<String, java.math.BigDecimal> montoCondenaPorActa,
            Map<String, PrototipoStore.SituacionPagoCondena> situacionPagoCondenaPorActa,
            Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa,
            Map<String, PrototipoStore.TipoPago> tipoPagoPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.documentosPorActa = documentosPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.cerrabilidad = cerrabilidad;
        this.montoCondenaPorActa = montoCondenaPorActa;
        this.situacionPagoCondenaPorActa = situacionPagoCondenaPorActa;
        this.situacionPagoPorActa = situacionPagoPorActa;
        this.tipoPagoPorActa = tipoPagoPorActa;
    }

    void clear() {
        plazoApelacionAbiertoPorActa.clear();
        apelacionPresentadaPorActa.clear();
        apelacionResueltaPorActa.clear();
        resultadoResolucionApelacionPorActa.clear();
        canalApelacionPorActa.clear();
    }

    boolean plazoApelacionAbierto(String actaId) {
        return Boolean.TRUE.equals(plazoApelacionAbiertoPorActa.get(actaId));
    }

    boolean apelacionPresentada(String actaId) {
        return Boolean.TRUE.equals(apelacionPresentadaPorActa.get(actaId));
    }

    boolean apelacionResuelta(String actaId) {
        return Boolean.TRUE.equals(apelacionResueltaPorActa.get(actaId));
    }

    // ---------------------------------------------------------------
    // Dictado de fallo
    // ---------------------------------------------------------------

    PrototipoStore.DictarFalloResultado dictarFalloAbsolutorio(String actaId) {
        return dictarFallo(
                actaId,
                TIPO_DOC_FALLO_ABSOLUTORIO,
                "fallo_absolutorio_",
                "FALLO_ABSOLUTORIO_DICTADO",
                "Fallo absolutorio dictado por autoridad competente; documento mock generado pendiente de firma.");
    }

    PrototipoStore.DictarFalloResultado dictarFalloCondenatorio(String actaId, java.math.BigDecimal montoCondena) {
        PrototipoStore.DictarFalloResultado resultado =
                dictarFallo(
                        actaId,
                        TIPO_DOC_FALLO_CONDENATORIO,
                        "fallo_condenatorio_",
                        "FALLO_CONDENATORIO_DICTADO",
                        "Fallo condenatorio dictado por autoridad competente; documento mock generado pendiente de firma.");
        if (resultado.estado() == PrototipoStore.DictarFalloEstado.OK) {
            montoCondenaPorActa.put(actaId, montoCondena);
        }
        return resultado;
    }

    private PrototipoStore.DictarFalloResultado dictarFallo(
            String actaId,
            String tipoDocumentoFallo,
            String prefijoArchivo,
            String tipoEvento,
            String descripcionEvento) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.DictarFalloResultado(
                    PrototipoStore.DictarFalloEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (actual.estaCerrada()) {
            return conflict();
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return conflict();
        }
        if (!SITUACION_ADMIN_ACTIVA.equals(actual.situacionAdministrativaActual())) {
            return conflict();
        }
        if (cerrabilidad.getResultadoFinal(actaId)
                != PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            return conflict();
        }
        if (expedienteIncluyeFallo(actaId)) {
            return conflict();
        }

        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = docs.size() + 1;
        String sufijoActa = sufijoActa(actaId);
        String idDoc = "DOC-" + sufijoActa + "-" + String.format("%02d", siguiente);
        String nombreArchivo = prefijoArchivo + sufijoActa.toLowerCase() + ".pdf";
        docs.add(new ActaDocumentoMock(idDoc, actaId, tipoDocumentoFallo, ESTADO_DOC_PENDIENTE_FIRMA, nombreArchivo));

        String bloqueOrigen = actual.bloqueActual();
        ActaMock actualizada = moverAPendienteFirma(actual);
        actas.put(actaId, actualizada);
        accionPendientePorActa.remove(actaId);

        registrarEvento(
                actaId,
                tipoEvento,
                bloqueOrigen,
                actualizada.bloqueActual(),
                descripcionEvento);

        return new PrototipoStore.DictarFalloResultado(
                PrototipoStore.DictarFalloEstado.OK,
                actaId,
                idDoc,
                tipoDocumentoFallo,
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    private PrototipoStore.DictarFalloResultado conflict() {
        return new PrototipoStore.DictarFalloResultado(
                PrototipoStore.DictarFalloEstado.CONFLICT, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // Hooks invocados por la fachada del store
    // ---------------------------------------------------------------

    /**
     * @return {@code true} si la acta tiene específicamente un documento
     *     {@code FALLO_CONDENATORIO} firmado y aún sin notificación positiva
     *     registrada. No requiere bandeja particular; aplica mientras el fallo
     *     esté firmado y no exista evento {@code FALLO_CONDENATORIO_NOTIFICADO}.
     */
    boolean hayFalloCondenatorioPendienteDeNotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null || actual.estaCerrada()) {
            return false;
        }
        return TIPO_DOC_FALLO_CONDENATORIO.equals(tipoFalloFirmadoSinNotificar(actaId));
    }

    /**
     * @return {@code true} si la acta tiene un documento fallo firmado y la
     *     bandeja actual habilita aún recibir su notificación (es decir,
     *     {@code PENDIENTE_NOTIFICACION} o {@code EN_NOTIFICACION}) y no se
     *     registró todavía el evento de notificación del fallo.
     */
    boolean hayFalloPendienteDeNotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null || actual.estaCerrada()) {
            return false;
        }
        String tipoFalloFirmado = tipoFalloFirmadoSinNotificar(actaId);
        return tipoFalloFirmado != null;
    }

    /**
     * Materializa la notificación positiva del fallo cuando
     * {@link #hayFalloPendienteDeNotificacion} es {@code true}. Reutiliza la
     * semántica del endpoint genérico {@code registrar-notificacion-positiva}:
     * el acta vuelve a {@code PENDIENTE_ANALISIS} con
     * {@code estadoProcesoActual = PENDIENTE_REVISION}, se agrega una nueva
     * notificación {@code ENTREGADA} para el fallo (sin pisar la del acta),
     * y se ajusta el {@code resultadoFinal} y el plazo de apelación según el
     * tipo de fallo.
     */
    PrototipoStore.RegistrarNotificacionPositivaResultado registrarNotificacionPositivaDeFallo(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !"EN_NOTIFICACION".equals(bandeja)) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }
        String tipoFallo = tipoFalloFirmadoSinNotificar(actaId);
        if (tipoFallo == null) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = moverAAnalisisPostFallo(actual);
        actas.put(actaId, actualizada);
        accionPendientePorActa.remove(actaId);

        String tipoEvento;
        String descripcionEvento;
        TipoNotificacion tipoNotificacion;
        if (TIPO_DOC_FALLO_ABSOLUTORIO.equals(tipoFallo)) {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
            plazoApelacionAbiertoPorActa.remove(actaId);
            tipoEvento = "FALLO_ABSOLUTORIO_NOTIFICADO";
            tipoNotificacion = TipoNotificacion.FALLO_ABSOLUTORIO;
            descripcionEvento =
                    "Fallo absolutorio notificado fehacientemente; resultadoFinal ABSUELTO; el acta queda cerrable.";
        } else {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENADO);
            plazoApelacionAbiertoPorActa.put(actaId, Boolean.TRUE);
            tipoEvento = "FALLO_CONDENATORIO_NOTIFICADO";
            tipoNotificacion = TipoNotificacion.FALLO_CONDENATORIO;
            descripcionEvento =
                    "Fallo condenatorio notificado fehacientemente; resultadoFinal CONDENADO; plazo de apelación abierto.";
        }

        agregarOActualizarNotificacionEntregadaDeFallo(actaId, actual, tipoNotificacion, tipoEvento);

        registrarEvento(actaId, tipoEvento, BLOQUE_D4, BLOQUE_D5, descripcionEvento);

        return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                PrototipoStore.RegistrarNotificacionPositivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    PrototipoStore.RegistrarNotificacionPositivaResultado registrarNotificacionPositivaDeFalloTipificada(
            String actaId, TipoNotificacion tipoNotificacion) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !"EN_NOTIFICACION".equals(bandeja)) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }
        if (tipoNotificacion != TipoNotificacion.FALLO_ABSOLUTORIO
                && tipoNotificacion != TipoNotificacion.FALLO_CONDENATORIO) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = moverAAnalisisPostFallo(actual);
        actas.put(actaId, actualizada);
        accionPendientePorActa.remove(actaId);

        String tipoEvento;
        String descripcionEvento;
        if (tipoNotificacion == TipoNotificacion.FALLO_ABSOLUTORIO) {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
            plazoApelacionAbiertoPorActa.remove(actaId);
            tipoEvento = "FALLO_ABSOLUTORIO_NOTIFICADO";
            descripcionEvento =
                    "Fallo absolutorio notificado fehacientemente; resultadoFinal ABSUELTO; el acta queda cerrable.";
        } else {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENADO);
            plazoApelacionAbiertoPorActa.put(actaId, Boolean.TRUE);
            tipoEvento = "FALLO_CONDENATORIO_NOTIFICADO";
            descripcionEvento =
                    "Fallo condenatorio notificado fehacientemente; resultadoFinal CONDENADO; plazo de apelación abierto.";
        }

        registrarEvento(actaId, tipoEvento, BLOQUE_D4, BLOQUE_D5, descripcionEvento);

        return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                PrototipoStore.RegistrarNotificacionPositivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    // ---------------------------------------------------------------
    // Vencimiento del plazo de apelación
    // ---------------------------------------------------------------

    PrototipoStore.RegistrarVencimientoPlazoApelacionResultado registrarVencimientoPlazoApelacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                    PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.NOT_FOUND, null, null, null, null);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                    PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT, null, null, null, null);
        }
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                    PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT, null, null, null, null);
        }
        if (!plazoApelacionAbierto(actaId)) {
            return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                    PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT, null, null, null, null);
        }
        if (apelacionPresentada(actaId)) {
            return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                    PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.CONFLICT, null, null, null, null);
        }

        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        plazoApelacionAbiertoPorActa.remove(actaId);

        String bloque = actual.bloqueActual();
        registrarEvento(
                actaId,
                "PLAZO_APELACION_VENCIDO",
                bloque,
                bloque,
                "Plazo de apelación vencido sin apelación presentada (mock); resultadoFinal CONDENA_FIRME.");

        return new PrototipoStore.RegistrarVencimientoPlazoApelacionResultado(
                PrototipoStore.RegistrarVencimientoPlazoApelacionEstado.OK,
                actaId,
                actual.bandejaActual(),
                actual.estadoProcesoActual(),
                PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME.name());
    }

    // ---------------------------------------------------------------
    // Presentación de apelación
    // ---------------------------------------------------------------

    PrototipoStore.RegistrarApelacionResultado registrarApelacion(
            String actaId, PrototipoStore.CanalPresentacionApelacionMock canal) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarApelacionResultado(
                    PrototipoStore.RegistrarApelacionEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (actual.estaCerrada()) {
            return conflictApelacion();
        }
        String bandeja = actual.bandejaActual();
        if ("CERRADAS".equals(bandeja) || "ARCHIVO".equals(bandeja) || "GESTION_EXTERNA".equals(bandeja)) {
            return conflictApelacion();
        }
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return conflictApelacion();
        }
        if (!plazoApelacionAbierto(actaId)) {
            return conflictApelacion();
        }
        if (apelacionPresentada(actaId)) {
            return conflictApelacion();
        }

        apelacionPresentadaPorActa.put(actaId, Boolean.TRUE);
        canalApelacionPorActa.put(actaId, canal.name());
        plazoApelacionAbiertoPorActa.remove(actaId);

        String bloque = actual.bloqueActual();
        registrarEvento(
                actaId,
                "APELACION_PRESENTADA",
                bloque,
                bloque,
                "Apelación/recurso presentado por canal "
                        + canal.name()
                        + " (mock); plazo de apelación cerrado; resultadoFinal permanece CONDENADO.");

        return new PrototipoStore.RegistrarApelacionResultado(
                PrototipoStore.RegistrarApelacionEstado.OK,
                actaId,
                actual.bandejaActual(),
                actual.estadoProcesoActual(),
                PrototipoStore.ResultadoFinalCierreMock.CONDENADO.name(),
                canal.name());
    }

    private PrototipoStore.RegistrarApelacionResultado conflictApelacion() {
        return new PrototipoStore.RegistrarApelacionResultado(
                PrototipoStore.RegistrarApelacionEstado.CONFLICT, null, null, null, null, null);
    }

    // ---------------------------------------------------------------
    // Resolución de apelación presentada
    // ---------------------------------------------------------------

    PrototipoStore.ResolverApelacionResultado resolverApelacion(
            String actaId, PrototipoStore.ResultadoResolucionApelacionMock resultado) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ResolverApelacionResultado(
                    PrototipoStore.ResolverApelacionEstado.NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
        if (actual.estaCerrada()) {
            return conflictResolverApelacion();
        }
        String bandeja = actual.bandejaActual();
        if ("CERRADAS".equals(bandeja) || "ARCHIVO".equals(bandeja) || "GESTION_EXTERNA".equals(bandeja)) {
            return conflictResolverApelacion();
        }
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return conflictResolverApelacion();
        }
        if (!apelacionPresentada(actaId)) {
            return conflictResolverApelacion();
        }
        if (plazoApelacionAbierto(actaId)) {
            return conflictResolverApelacion();
        }
        if (apelacionResuelta(actaId)) {
            return conflictResolverApelacion();
        }

        apelacionResueltaPorActa.put(actaId, Boolean.TRUE);
        resultadoResolucionApelacionPorActa.put(actaId, resultado.name());

        String bloque = actual.bloqueActual();
        PrototipoStore.ResultadoFinalCierreMock nuevoResultadoFinal;
        String tipoEvento;
        String descripcionEvento;
        if (resultado == PrototipoStore.ResultadoResolucionApelacionMock.RECHAZADA) {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
            nuevoResultadoFinal = PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME;
            tipoEvento = "APELACION_RECHAZADA";
            descripcionEvento =
                    "Apelación rechazada (mock); condena confirmada; resultadoFinal CONDENA_FIRME.";
        } else {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
            nuevoResultadoFinal = PrototipoStore.ResultadoFinalCierreMock.ABSUELTO;
            tipoEvento = "APELACION_ACEPTADA_ABSUELVE";
            descripcionEvento =
                    "Apelación acogida — absolución (mock); resultadoFinal ABSUELTO; acta cerrable si no hay otros bloqueantes.";
        }

        registrarEvento(actaId, tipoEvento, bloque, bloque, descripcionEvento);

        return new PrototipoStore.ResolverApelacionResultado(
                PrototipoStore.ResolverApelacionEstado.OK,
                actaId,
                actual.bandejaActual(),
                actual.estadoProcesoActual(),
                nuevoResultadoFinal.name(),
                resultado.name());
    }

    private PrototipoStore.ResolverApelacionResultado conflictResolverApelacion() {
        return new PrototipoStore.ResolverApelacionResultado(
                PrototipoStore.ResolverApelacionEstado.CONFLICT, null, null, null, null, null);
    }

    /**
     * Helper interno de mocks: precarga apelación presentada (y opcionalmente
     * resuelta) sin recorrer firma, notificación ni registro por API.
     */
    void precargarApelacionDemo(
            String actaId,
            PrototipoStore.CanalPresentacionApelacionMock canal,
            boolean resuelta,
            PrototipoStore.ResultadoResolucionApelacionMock resultadoResolucion) {
        if (actaId == null) {
            return;
        }
        plazoApelacionAbiertoPorActa.remove(actaId);
        apelacionPresentadaPorActa.put(actaId, Boolean.TRUE);
        if (canal != null) {
            canalApelacionPorActa.put(actaId, canal.name());
        }
        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENADO);
        if (!resuelta || resultadoResolucion == null) {
            return;
        }
        apelacionResueltaPorActa.put(actaId, Boolean.TRUE);
        resultadoResolucionApelacionPorActa.put(actaId, resultadoResolucion.name());
        if (resultadoResolucion == PrototipoStore.ResultadoResolucionApelacionMock.ACEPTADA_ABSUELVE) {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.ABSUELTO);
        } else {
            cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        }
    }

    // ---------------------------------------------------------------
    // Consentimiento de condena desde portal infractor
    // ---------------------------------------------------------------

    /**
     * El infractor consiente la condena desde el portal, renunciando a apelar.
     * Precondiciones: {@code resultadoFinal=CONDENADO}, sin apelacion presentada,
     * {@code situacionPagoCondena=NO_APLICA}, {@code montoCondena > 0}, acta no
     * cerrada/archivada/gestion-externa.
     * Efecto: {@code resultadoFinal=CONDENA_FIRME}, plazo apelacion cerrado,
     * {@code situacionPagoCondena=PENDIENTE}, evento {@code CONDENA_CONSENTIDA}.
     */
    PrototipoStore.ConsentirCondenaResultado consentirCondena(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ConsentirCondenaResultado(
                    PrototipoStore.ConsentirCondenaEstado.NOT_FOUND, null, null, null);
        }
        if (actual.estaCerrada()) {
            return conflictConsentir();
        }
        String b = actual.bandejaActual();
        if ("CERRADAS".equals(b) || "ARCHIVO".equals(b) || "GESTION_EXTERNA".equals(b)) {
            return conflictConsentir();
        }
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return conflictConsentir();
        }
        if (apelacionPresentada(actaId)) {
            return conflictConsentir();
        }
        PrototipoStore.SituacionPagoCondena situacion = situacionPagoCondenaPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoCondena.NO_APLICA);
        if (situacion != PrototipoStore.SituacionPagoCondena.NO_APLICA) {
            return conflictConsentir();
        }
        java.math.BigDecimal monto = montoCondenaPorActa.get(actaId);
        if (monto == null || monto.signum() <= 0) {
            return conflictConsentir();
        }

        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        plazoApelacionAbiertoPorActa.remove(actaId);
        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.PENDIENTE);

        String bloque = actual.bloqueActual();
        registrarEvento(
                actaId,
                "CONDENA_CONSENTIDA",
                bloque,
                bloque,
                "Condena consentida por el infractor desde PORTAL_INFRACTOR; resultadoFinal CONDENA_FIRME; situacionPagoCondena PENDIENTE.");

        return new PrototipoStore.ConsentirCondenaResultado(
                PrototipoStore.ConsentirCondenaEstado.OK,
                actaId,
                PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME,
                PrototipoStore.SituacionPagoCondena.PENDIENTE);
    }

    private PrototipoStore.ConsentirCondenaResultado conflictConsentir() {
        return new PrototipoStore.ConsentirCondenaResultado(
                PrototipoStore.ConsentirCondenaEstado.CONFLICT, null, null, null);
    }

    // ---------------------------------------------------------------
    // Consentimiento + registro de pago presencial (acción Dirección)
    // ---------------------------------------------------------------

    /**
     * Dirección registra en un único acto el consentimiento presencial de la
     * condena y el pago correspondiente.
     *
     * <p>Admite dos caminos:
     * <ul>
     *   <li><b>Fallo ya notificado:</b> {@code resultadoFinal=CONDENADO}, sin
     *       apelación presentada. Comportamiento original.</li>
     *   <li><b>Fallo firmado no notificado:</b> {@code resultadoFinal=SIN_RESULTADO_FINAL},
     *       fallo condenatorio firmado pendiente de notificación. La acción
     *       registra primero una notificación positiva presencial
     *       ({@code FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL}) y luego
     *       procede igual que el camino anterior. La DDJJ de domicilio
     *       electrónico es opcional en este caso (no bloqueante).</li>
     * </ul>
     *
     * <p>Precondiciones comunes: {@code situacionPagoCondena=NO_APLICA},
     * {@code situacionPago=SIN_PAGO}, {@code montoCondena > 0}, acta no
     * cerrada ni en bandeja terminal, sin apelación pendiente.
     *
     * <p>Efecto: {@code resultadoFinal=CONDENA_FIRME},
     * {@code situacionPagoCondena=INFORMADO},
     * {@code situacionPago=PENDIENTE_CONFIRMACION}, {@code tipoPago=CONDENA},
     * eventos {@code CONDENA_CONSENTIDA_PRESENCIAL} +
     * {@code PAGO_CONDENA_REGISTRADO_PRESENCIAL} (precedidos por
     * {@code FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL} en el camino B).
     */
    PrototipoStore.ConsentirCondenaYRegistrarPagoResultado consentirCondenaYRegistrarPago(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ConsentirCondenaYRegistrarPagoResultado(
                    PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.NOT_FOUND, null, null, null);
        }
        if (actual.estaCerrada()) {
            return conflictConsentirYPagar();
        }
        String b = actual.bandejaActual();
        if ("CERRADAS".equals(b) || "ARCHIVO".equals(b) || "GESTION_EXTERNA".equals(b)) {
            return conflictConsentirYPagar();
        }

        PrototipoStore.ResultadoFinalCierreMock rf = cerrabilidad.getResultadoFinal(actaId);
        boolean conFalloPendienteNotificacion = false;
        if (rf == PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL) {
            // Camino B: fallo condenatorio firmado pero aún no notificado
            if (!TIPO_DOC_FALLO_CONDENATORIO.equals(tipoFalloFirmadoSinNotificar(actaId))) {
                return conflictConsentirYPagar();
            }
            conFalloPendienteNotificacion = true;
        } else if (rf != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return conflictConsentirYPagar();
        }

        if (apelacionPresentada(actaId)) {
            return conflictConsentirYPagar();
        }
        PrototipoStore.SituacionPagoCondena situacionCondena = situacionPagoCondenaPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoCondena.NO_APLICA);
        if (situacionCondena != PrototipoStore.SituacionPagoCondena.NO_APLICA) {
            return conflictConsentirYPagar();
        }
        PrototipoStore.SituacionPagoMock situacionPago = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionPago != PrototipoStore.SituacionPagoMock.SIN_PAGO) {
            return conflictConsentirYPagar();
        }
        java.math.BigDecimal monto = montoCondenaPorActa.get(actaId);
        if (monto == null || monto.signum() <= 0) {
            return conflictConsentirYPagar();
        }

        // Camino B: registrar notificación presencial del fallo antes del consentimiento
        if (conFalloPendienteNotificacion) {
            ActaMock actualizada = moverAAnalisisPostFallo(actual);
            actas.put(actaId, actualizada);
            accionPendientePorActa.remove(actaId);
            registrarEvento(
                    actaId,
                    "FALLO_CONDENATORIO_NOTIFICADO_PRESENCIAL",
                    BLOQUE_D4,
                    BLOQUE_D5,
                    "Fallo condenatorio notificado presencialmente ante Direccion de Faltas"
                            + " en acto de consentimiento y pago de condena.");
            actual = actualizada;
        }

        cerrabilidad.setResultadoFinalDemo(actaId, PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME);
        plazoApelacionAbiertoPorActa.remove(actaId);
        situacionPagoCondenaPorActa.put(actaId, PrototipoStore.SituacionPagoCondena.INFORMADO);
        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION);
        tipoPagoPorActa.put(actaId, PrototipoStore.TipoPago.CONDENA);

        String bloque = actual.bloqueActual();
        registrarEvento(
                actaId,
                "CONDENA_CONSENTIDA_PRESENCIAL",
                bloque,
                bloque,
                "Condena consentida presencialmente por el infractor ante Direccion de Faltas;"
                        + " resultadoFinal CONDENA_FIRME.");
        registrarEvento(
                actaId,
                "PAGO_CONDENA_REGISTRADO_PRESENCIAL",
                bloque,
                bloque,
                "Pago de condena registrado/informado presencialmente por Direccion de Faltas;"
                        + " situacionPagoCondena INFORMADO; pendiente confirmacion de acreditacion.");

        return new PrototipoStore.ConsentirCondenaYRegistrarPagoResultado(
                PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.OK,
                actaId,
                PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME,
                PrototipoStore.SituacionPagoCondena.INFORMADO);
    }

    private PrototipoStore.ConsentirCondenaYRegistrarPagoResultado conflictConsentirYPagar() {
        return new PrototipoStore.ConsentirCondenaYRegistrarPagoResultado(
                PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.CONFLICT, null, null, null);
    }

    // ---------------------------------------------------------------
    // Regla de puedePresentarApelacion para portal infractor
    // ---------------------------------------------------------------

    /**
     * Regla de portal: el infractor puede presentar apelación sólo si el
     * fallo condenatorio fue notificado, el plazo está abierto, no hay
     * apelación presentada, y el acta no está en bandeja terminal/externa
     * ni cerrada.
     */
    boolean puedePresentarApelacion(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null || acta.estaCerrada()) {
            return false;
        }
        String b = acta.bandejaActual();
        if ("CERRADAS".equals(b) || "ARCHIVO".equals(b) || "GESTION_EXTERNA".equals(b)) {
            return false;
        }
        if (cerrabilidad.getResultadoFinal(actaId) != PrototipoStore.ResultadoFinalCierreMock.CONDENADO) {
            return false;
        }
        if (apelacionPresentada(actaId)) {
            return false;
        }
        return plazoApelacionAbierto(actaId);
    }

    // ---------------------------------------------------------------
    // Helpers internos
    // ---------------------------------------------------------------

    private boolean expedienteIncluyeFallo(String actaId) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null || docs.isEmpty()) {
            return false;
        }
        return docs.stream().anyMatch(d -> esFallo(d.tipoDocumento()));
    }

    /**
     * @return tipo del fallo (constante {@code FALLO_ABSOLUTORIO} o
     *     {@code FALLO_CONDENATORIO}) si existe un documento de fallo
     *     {@code FIRMADO} en expediente sin evento de notificación de fallo
     *     posterior; {@code null} en otro caso.
     */
    private String tipoFalloFirmadoSinNotificar(String actaId) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null || docs.isEmpty()) {
            return null;
        }
        String tipoFalloFirmado = null;
        for (ActaDocumentoMock d : docs) {
            if (esFallo(d.tipoDocumento()) && ESTADO_DOC_FIRMADO.equals(d.estadoDocumento())) {
                tipoFalloFirmado = d.tipoDocumento();
            }
        }
        if (tipoFalloFirmado == null) {
            return null;
        }
        if (yaSeRegistroNotificacionDeFallo(actaId)) {
            return null;
        }
        return tipoFalloFirmado;
    }

    private boolean yaSeRegistroNotificacionDeFallo(String actaId) {
        List<ActaEventoMock> evs = eventosPorActa.get(actaId);
        if (evs == null || evs.isEmpty()) {
            return false;
        }
        return evs.stream()
                .map(ActaEventoMock::tipoEvento)
                .anyMatch(
                        t ->
                                "FALLO_ABSOLUTORIO_NOTIFICADO".equals(t)
                                        || "FALLO_CONDENATORIO_NOTIFICADO".equals(t));
    }

    private ActaMock moverAPendienteFirma(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                actual.bloqueActual(),
                ESTADO_PROCESO_PENDIENTE_FIRMA,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                true,
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_FIRMA);
    }

    private ActaMock moverAAnalisisPostFallo(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                true,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
    }

    private void agregarOActualizarNotificacionEntregadaDeFallo(
            String actaId, ActaMock actual, TipoNotificacion tipoNotificacion, String tipoEvento) {
        List<ActaNotificacionMock> notifs =
                notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String descripcion = descripcionNotificacionFalloEntregada(actual, tipoNotificacion);
        LocalDateTime fecha = fechaSiguienteEvento(actaId);
        for (int i = 0; i < notifs.size(); i++) {
            ActaNotificacionMock n = notifs.get(i);
            if (n.tipo() == tipoNotificacion && n.resultado() == ResultadoNotificacion.SIN_RESULTADO) {
                notifs.set(i, n.conResultado(
                        EstadoNotificacion.ENTREGADA,
                        ResultadoNotificacion.POSITIVA,
                        ESTADO_NOTIFICACION_ENTREGADA,
                        descripcion,
                        fecha,
                        tipoEvento));
                return;
            }
        }
        String sufijoActa = sufijoActa(actaId);
        int siguiente = notifs.size() + 1;
        String idNotif = "NOT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        notifs.add(new ActaNotificacionMock(
                idNotif,
                actaId,
                "POSTAL",
                ESTADO_NOTIFICACION_ENTREGADA,
                descripcion,
                tipoNotificacion,
                CanalNotificacion.CORREO_POSTAL,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                descripcion,
                tipoEvento,
                null,
                null,
                null,
                null,
                fecha,
                null,
                actual.infractorNombre(),
                null,
                "pendiente constancia de domicilio",
                null,
                null));
    }

    private static String descripcionNotificacionFalloEntregada(ActaMock actual, TipoNotificacion tipoNotificacion) {
        String etiqueta =
                tipoNotificacion == TipoNotificacion.FALLO_ABSOLUTORIO
                        ? "constancia de entrega postal — notificación de fallo absolutorio (demo)"
                        : "constancia de entrega postal — notificación de fallo condenatorio (demo)";
        return actual.infractorNombre() + " — " + etiqueta;
    }

    private void registrarEvento(
            String actaId,
            String tipoEvento,
            String bloqueOrigen,
            String bloqueDestino,
            String descripcion) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = sufijoActa(actaId);
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = fechaSiguienteEvento(actaId);
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }

    private LocalDateTime fechaSiguienteEvento(String actaId) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        return eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
    }

    private static String sufijoActa(String actaId) {
        return actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
    }
}
