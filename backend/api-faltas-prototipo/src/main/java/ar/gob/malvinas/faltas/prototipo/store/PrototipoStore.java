package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PrototipoStore {

    /**
     * Orden fijo para demo: flujo operativo típico, luego archivo y cierre.
     */
    private static final List<String> ORDEN_BANDEJAS_DEMO = List.of(
            "ACTAS_EN_ENRIQUECIMIENTO",
            "PENDIENTE_PREPARACION_DOCUMENTAL",
            "PENDIENTE_FIRMA",
            "PENDIENTE_NOTIFICACION",
            "EN_NOTIFICACION",
            "PENDIENTE_ANALISIS",
            "PENDIENTES_RESOLUCION_REDACCION",
            "GESTION_EXTERNA",
            "ARCHIVO",
            "CERRADAS");

    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvi�� a
     * anǭlisis por notificaci��n fallida y requiere decisi��n posterior. El
     * campo es filtrable por API y sobrevive en el store aparte del record
     * inmutable {@link ActaMock}.
     */
    public static final String ACCION_REINTENTAR_NOTIFICACION = "REINTENTAR_NOTIFICACION";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis porque la notificación quedó vencida sin entrega ni rechazo
     * explícito. Debe quedar distinguible del reintento por no entrega: acá
     * no se asume todavía próximo paso (nuevo intento, archivo, apremio u
     * otra salida), sólo que requiere decisión posterior.
     */
    public static final String ACCION_EVALUAR_NOTIFICACION_VENCIDA = "EVALUAR_NOTIFICACION_VENCIDA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por reingreso desde la macro-bandeja {@code ARCHIVO}. Permite
     * distinguir y filtrar los casos reingresados dentro de la bandeja de
     * análisis, de los que llegaron por el circuito operativo normal o por
     * notificación fallida/vencida. Mantiene coherencia con la política del
     * prototipo de exponer el motivo de retorno como accionPendiente.
     */
    public static final String ACCION_REVISION_POST_REINGRESO = "REVISION_POST_REINGRESO";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso ya
     * atravesó fallo + notificación de fallo y la ventana de espera
     * posterior (regla especial: 5 días, ver
     * {@code spec/02-reglas-transversales/02-reglas-de-notificacion.md}) se
     * cumplió sin novedad que altere el recorrido, por lo que quedó en
     * condición de derivación a gestión externa.
     *
     * <p>En este slice el caso todavía permanece en análisis: sólo se
     * expone la condición de "listo para derivación" como marca filtrable,
     * sin materializar la bandeja {@code GESTION_EXTERNA} ni la derivación
     * efectiva. Distinguible del resto de marcas de análisis porque no
     * proviene de notificación fallida/vencida ni de reingreso desde archivo.
     */
    public static final String ACCION_DERIVAR_GESTION_EXTERNA = "DERIVAR_GESTION_EXTERNA";
    /**
     * Marca operativa dentro de {@code PENDIENTE_ANALISIS}: el caso volvió a
     * análisis por retorno explícito desde la macro-bandeja
     * {@code GESTION_EXTERNA}. Permite distinguir y filtrar dentro de
     * análisis los casos que acaban de reingresar desde gestión externa, de
     * los que vinieron por el circuito operativo normal, por notificación
     * fallida/vencida o por reingreso desde archivo. La trazabilidad del
     * tipo de gestión externa del que provino el caso se preserva aparte
     * vía {@link GestionExternaSupport}, no se sobrescribe con esta marca.
     */
    public static final String ACCION_REVISION_POST_GESTION_EXTERNA = "REVISION_POST_GESTION_EXTERNA";
    /**
     * Motivo de archivo asignado cuando una acta es archivada directamente
     * desde análisis por decisión administrativa (acción genérica de archivo
     * sin paso previo por evaluación de vencimiento). Permite distinguir
     * dentro de la macro-bandeja {@code ARCHIVO} los archivos de origen
     * directo de otros orígenes semánticamente diferentes.
     */
    public static final String MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO = "ARCHIVO_DESDE_ANALISIS_DIRECTO";
    /**
     * Motivo de archivo asignado cuando la decisión posterior a una
     * notificación vencida es archivar el caso (en lugar de reintentar la
     * notificación u otra salida futura no modelada todavía). Distingue este
     * archivo del archivo directo sin pasar por evaluación de vencimiento.
     */
    public static final String MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO = "ARCHIVO_POST_EVALUACION_VENCIMIENTO";
    /**
     * Tipo vigente de gestión externa: apremio. Se asigna al expediente
     * al materializar la derivación efectiva hacia la macro-bandeja
     * {@code GESTION_EXTERNA} vía la acción dedicada, para que la salida
     * del circuito interno no quede muda y se pueda distinguir por API del
     * tipo {@link #TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ}.
     */
    public static final String TIPO_GESTION_EXTERNA_APREMIO = "APREMIO";
    /**
     * Tipo vigente de gestión externa: Juzgado de Paz. Alternativa al tipo
     * {@link #TIPO_GESTION_EXTERNA_APREMIO} dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}: ambos tipos comparten bloque/estado/situación
     * y política de reingreso, y sólo se diferencian por el tipo vigente
     * expuesto por API. El operador elige la salida usando la acción
     * dedicada correspondiente.
     */
    public static final String TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ = "JUZGADO_DE_PAZ";

    public enum RegistrarNotificacionPositivaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionPositivaResultado(
            RegistrarNotificacionPositivaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum RegistrarNotificacionNegativaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionNegativaResultado(
            RegistrarNotificacionNegativaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum ReintentarNotificacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReintentarNotificacionResultado(
            ReintentarNotificacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum RegistrarNotificacionVencidaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record RegistrarNotificacionVencidaResultado(
            RegistrarNotificacionVencidaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente) {
    }

    public enum ReintentarNotificacionVencidaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReintentarNotificacionVencidaResultado(
            ReintentarNotificacionVencidaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum CerrarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record CerrarActaResultado(
            CerrarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum ArchivarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ArchivarActaResultado(
            ArchivarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String motivoArchivo) {
    }

    public enum ArchivarPorVencimientoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ArchivarPorVencimientoResultado(
            ArchivarPorVencimientoEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String motivoArchivo) {
    }

    public enum ReingresarActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarActaResultado(
            ReingresarActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            String motivoArchivoPrevio) {
    }

    public enum DerivarAGestionExternaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record DerivarAGestionExternaResultado(
            DerivarAGestionExternaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String tipoGestionExterna) {
    }

    public enum ReingresarDesdeGestionExternaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record ReingresarDesdeGestionExternaResultado(
            ReingresarDesdeGestionExternaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual,
            String accionPendiente,
            String tipoGestionExternaPrevia) {
    }

    public enum GenerarMedidaPreventivaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarMedidaPreventivaResultado(
            GenerarMedidaPreventivaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum GenerarNotificacionActaEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarNotificacionActaResultado(
            GenerarNotificacionActaEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum GenerarNulidadEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record GenerarNulidadResultado(
            GenerarNulidadEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public enum FirmarDocumentoEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    public record FirmarDocumentoResultado(
            FirmarDocumentoEstado estado,
            String actaId,
            String documentoId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

    public record BandejaConteo(String codigo, int cantidadActas) {
    }

    private final Map<String, ActaMock> actas = new LinkedHashMap<>();
    private final Map<String, List<ActaEventoMock>> eventosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa = new LinkedHashMap<>();
    private final Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa = new LinkedHashMap<>();
    /**
     * Marca operativa actual dentro de la bandeja. Se mantiene en un mapa
     * paralelo para no cambiar el record {@link ActaMock} ni obligar a
     * reescribir todas las transiciones previas. Si una acta no tiene marca,
     * simplemente no aparece en el mapa.
     */
    private final Map<String, String> accionPendientePorActa = new LinkedHashMap<>();

    /**
     * Soporte funcional del área archivo/reingreso: archivo directo desde
     * análisis, archivo post evaluación de vencimiento, reingreso desde
     * archivo, motivoArchivo y eventos asociados. Extraído del store para
     * reducir su tamaño/acoplamiento sin cambiar comportamiento observable.
     * El store mantiene una fachada pública que delega aquí.
     */
    private final ArchivoReingresoSupport archivoReingreso =
            new ArchivoReingresoSupport(actas, eventosPorActa, accionPendientePorActa);

    /**
     * Soporte funcional del área notificación: notificación positiva,
     * negativa, vencida, reintento por no entrega y reintento
     * post-vencimiento, junto con sus estados internos, resumen de
     * destinatario demo y eventos asociados. Extraído del store para bajar
     * su tamaño/acoplamiento sin cambiar comportamiento observable. El
     * store mantiene una fachada pública que delega aquí.
     */
    private final NotificacionSupport notificacion =
            new NotificacionSupport(actas, eventosPorActa, notificacionesPorActa, accionPendientePorActa);

    /**
     * Soporte funcional del área piezas/firma: consulta de piezas,
     * producción de medida preventiva, producción de notificación del acta,
     * firma individual de documentos y transición a
     * {@code PENDIENTE_NOTIFICACION} cuando se cierra la firma. Extraído
     * del store para bajar su tamaño/acoplamiento sin cambiar comportamiento
     * observable. El store mantiene una fachada pública que delega aquí.
     * Frontera con notificación: al cerrarse la firma, delega en
     * {@link NotificacionSupport} la materialización inicial de la
     * notificación (única interacción cruzada entre ambas áreas).
     */
    private final PiezasFirmaSupport piezasFirma =
            new PiezasFirmaSupport(actas, eventosPorActa, documentosPorActa, piezasRequeridasPorActa, notificacion);

    /**
     * Soporte funcional del área cierre: cierre desde análisis, evento
     * {@code CIERRE_ANALISIS} asociado y limpieza de marca operativa
     * pendiente en análisis. Extraído del store para cerrar la
     * descompresión por área funcional sin cambiar comportamiento
     * observable. El store mantiene una fachada pública que delega aquí.
     */
    private final CierreSupport cierre =
            new CierreSupport(actas, eventosPorActa, accionPendientePorActa);

    /**
     * Soporte funcional del área gestión externa: derivación efectiva de
     * casos desde análisis hacia la macro-bandeja {@code GESTION_EXTERNA}
     * post fallo + notificación de fallo + ventana de espera posterior
     * cumplida sin novedad. Mantiene el tipo mínimo de gestión externa
     * ({@link #TIPO_GESTION_EXTERNA_APREMIO}) asociado a cada acta derivada.
     * Extraído del store para preservar la descompresión por área funcional
     * sin cambiar comportamiento observable. El store mantiene una fachada
     * pública que delega aquí.
     */
    private final GestionExternaSupport gestionExterna =
            new GestionExternaSupport(actas, eventosPorActa, accionPendientePorActa);

    public Map<String, ActaMock> getActas() {
        return actas;
    }

    public Map<String, List<ActaEventoMock>> getEventosPorActa() {
        return eventosPorActa;
    }

    public Map<String, List<ActaDocumentoMock>> getDocumentosPorActa() {
        return documentosPorActa;
    }

    public Map<String, List<ActaNotificacionMock>> getNotificacionesPorActa() {
        return notificacionesPorActa;
    }

    public Map<String, ActaPiezasRequeridasMock> getPiezasRequeridasPorActa() {
        return piezasRequeridasPorActa;
    }

    public void clearAll() {
        actas.clear();
        eventosPorActa.clear();
        documentosPorActa.clear();
        notificacionesPorActa.clear();
        piezasRequeridasPorActa.clear();
        accionPendientePorActa.clear();
        archivoReingreso.clear();
        gestionExterna.clear();
    }

    /**
     * Marca operativa vigente de la acta dentro de su bandeja, o {@code null}
     * si la acta no tiene acci��n pendiente marcada.
     */
    public String getAccionPendiente(String actaId) {
        return accionPendientePorActa.get(actaId);
    }

    /**
     * Motivo vigente de archivo para la acta dentro de la macro-bandeja
     * {@code ARCHIVO}, o {@code null} si la acta no está archivada o fue
     * archivada antes de modelar esta semántica. Fachada: el estado y la
     * semántica viven en {@link ArchivoReingresoSupport}.
     */
    public String getMotivoArchivo(String actaId) {
        return archivoReingreso.getMotivoArchivo(actaId);
    }

    /**
     * Tipo de gestión externa vigente para la acta dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}, o {@code null} si la acta no fue derivada.
     * Fachada: el estado y la semántica viven en
     * {@link GestionExternaSupport}.
     */
    public String getTipoGestionExterna(String actaId) {
        return gestionExterna.getTipoGestionExterna(actaId);
    }

    /**
     * Helper interno de mocks: asigna el motivo de archivo vigente para una
     * acta precargada. Uso restringido a la fábrica de datos demo. Fachada:
     * delega en {@link ArchivoReingresoSupport}.
     */
    public void setMotivoArchivo(String actaId, String motivo) {
        archivoReingreso.setMotivoArchivo(actaId, motivo);
    }

    /**
     * Helper interno de mocks: asigna la acción pendiente vigente para una
     * acta precargada, sin ejecutar la transición operativa asociada. Uso
     * restringido a la fábrica de datos demo para poder representar estados
     * intermedios (por ejemplo, un caso que ya quedó en condición de
     * derivación a gestión externa) sin tener que simular todo el recorrido
     * que los supports materializan en runtime.
     */
    public void setAccionPendiente(String actaId, String accion) {
        if (actaId == null) {
            return;
        }
        if (accion == null || accion.isBlank()) {
            accionPendientePorActa.remove(actaId);
            return;
        }
        accionPendientePorActa.put(actaId, accion);
    }

    public List<BandejaConteo> listarBandejasConConteoOrdenadas() {
        Map<String, Integer> conteo = new HashMap<>();
        for (ActaMock acta : actas.values()) {
            String bandeja = acta.bandejaActual();
            conteo.put(bandeja, conteo.getOrDefault(bandeja, 0) + 1);
        }
        if (conteo.isEmpty()) {
            return List.of();
        }
        List<BandejaConteo> resultado = new ArrayList<>();
        for (String codigo : ORDEN_BANDEJAS_DEMO) {
            Integer n = conteo.remove(codigo);
            if (n != null) {
                resultado.add(new BandejaConteo(codigo, n));
            }
        }
        conteo.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> resultado.add(new BandejaConteo(e.getKey(), e.getValue())));
        return resultado;
    }

    public List<ActaMock> listarActasPorBandeja(String codigoBandeja) {
        return listarActasPorBandeja(codigoBandeja, null);
    }

    /**
     * Listado de la bandeja con filtro opcional por acci��n pendiente. Si
     * {@code accionPendiente} es {@code null} o en blanco, no se filtra.
     */
    public List<ActaMock> listarActasPorBandeja(String codigoBandeja, String accionPendiente) {
        if (codigoBandeja == null) {
            return List.of();
        }
        String accionFiltro = (accionPendiente != null && !accionPendiente.isBlank()) ? accionPendiente : null;
        return actas.values().stream()
                .filter(a -> codigoBandeja.equals(a.bandejaActual()))
                .filter(a -> accionFiltro == null || accionFiltro.equals(accionPendientePorActa.get(a.id())))
                .sorted(Comparator.comparing(ActaMock::id))
                .toList();
    }

    public Optional<ActaMock> findActa(String id) {
        return Optional.ofNullable(actas.get(id));
    }

    public boolean existeActa(String id) {
        return actas.containsKey(id);
    }

    /**
     * Historial cronológico. Si la acta no tiene eventos cargados, lista vacía.
     */
    public List<ActaEventoMock> listarEventosActaOrdenados(String actaId) {
        List<ActaEventoMock> lista = eventosPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return lista.stream()
                .sorted(Comparator.comparing(ActaEventoMock::fechaHora))
                .toList();
    }

    /**
     * Si la acta no tiene documentos cargados, lista vacía. Fachada
     * pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public List<ActaDocumentoMock> listarDocumentosPorActa(String actaId) {
        return piezasFirma.listarDocumentosPorActa(actaId);
    }

    /**
     * Si la acta no tiene notificaciones cargadas, lista vacía.
     */
    public List<ActaNotificacionMock> listarNotificacionesPorActa(String actaId) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return List.copyOf(lista);
    }

    /**
     * Si la acta no declara piezas requeridas (caso típico fuera de
     * PENDIENTES_RESOLUCION_REDACCION), devuelve {@link Optional#empty()}.
     * Fachada pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public Optional<ActaPiezasRequeridasMock> findPiezasRequeridas(String actaId) {
        return piezasFirma.findPiezasRequeridas(actaId);
    }

    /**
     * Catálogo de piezas requeridas; lista vacía si la acta no declara
     * piezas. Fachada pública; la lógica vive en {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasRequeridas(String actaId) {
        return piezasFirma.listarPiezasRequeridas(actaId);
    }

    /**
     * Piezas ya producidas; lista vacía si la acta no declara piezas o no
     * se produjo ninguna todavía. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasGeneradas(String actaId) {
        return piezasFirma.listarPiezasGeneradas(actaId);
    }

    /**
     * Piezas requeridas que todavía no fueron producidas. Si la acta no
     * declara piezas requeridas, lista vacía. Fachada pública; la lógica
     * vive en {@link PiezasFirmaSupport}.
     */
    public List<String> listarPiezasPendientes(String actaId) {
        return piezasFirma.listarPiezasPendientes(actaId);
    }

    /**
     * {@code true} si la acta declara piezas requeridas y todas están ya
     * producidas. {@code false} si aún falta alguna, o si la acta no
     * declara piezas. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public boolean todasLasPiezasProducidas(String actaId) {
        return piezasFirma.todasLasPiezasProducidas(actaId);
    }

    /**
     * Demo: notificación entregada positivamente → bandeja análisis (solo
     * desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION). Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionPositivaResultado registrarNotificacionPositiva(String actaId) {
        return notificacion.registrarNotificacionPositiva(actaId);
    }

    /**
     * Demo: notificación no entregada → acta retorna a PENDIENTE_ANALISIS con
     * marca {@link #ACCION_REINTENTAR_NOTIFICACION}. Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionNegativaResultado registrarNotificacionNegativa(String actaId) {
        return notificacion.registrarNotificacionNegativa(actaId);
    }

    /**
     * Demo: reintento de notificación desde análisis. Fachada pública; la
     * lógica vive en {@link NotificacionSupport}.
     */
    public ReintentarNotificacionResultado reintentarNotificacion(String actaId) {
        return notificacion.reintentarNotificacion(actaId);
    }

    /**
     * Demo: notificación vencida → acta retorna a PENDIENTE_ANALISIS con
     * marca {@link #ACCION_EVALUAR_NOTIFICACION_VENCIDA}. Fachada pública;
     * la lógica vive en {@link NotificacionSupport}.
     */
    public RegistrarNotificacionVencidaResultado registrarNotificacionVencida(String actaId) {
        return notificacion.registrarNotificacionVencida(actaId);
    }

    /**
     * Demo: decisión posterior mínima sobre un caso vencido: reintentar la
     * notificación. Fachada pública; la lógica vive en
     * {@link NotificacionSupport}.
     */
    public ReintentarNotificacionVencidaResultado reintentarNotificacionVencida(String actaId) {
        return notificacion.reintentarNotificacionVencida(actaId);
    }

    /**
     * Demo: cierre desde análisis → bandeja CERRADAS (solo desde
     * PENDIENTE_ANALISIS). Fachada pública; la lógica vive en
     * {@link CierreSupport}.
     */
    public CerrarActaResultado cerrarActaDesdeAnalisis(String actaId) {
        return cierre.cerrarActaDesdeAnalisis(actaId);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link #TIPO_GESTION_EXTERNA_APREMIO} → macro-bandeja
     * {@code GESTION_EXTERNA}. Aplica a actas en PENDIENTE_ANALISIS con
     * marca {@link #ACCION_DERIVAR_GESTION_EXTERNA} (derivación inicial) o
     * con marca {@link #ACCION_REVISION_POST_GESTION_EXTERNA}
     * (re-derivación del expediente que ya volvió desde gestión externa).
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public DerivarAGestionExternaResultado derivarAApremio(String actaId) {
        return gestionExterna.derivarAApremio(actaId);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link #TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ} → macro-bandeja
     * {@code GESTION_EXTERNA}. Aplica a actas en PENDIENTE_ANALISIS con
     * marca {@link #ACCION_DERIVAR_GESTION_EXTERNA} (derivación inicial) o
     * con marca {@link #ACCION_REVISION_POST_GESTION_EXTERNA}
     * (re-derivación del expediente que ya volvió desde gestión externa).
     * Fachada pública; la lógica vive en {@link GestionExternaSupport}.
     */
    public DerivarAGestionExternaResultado derivarAJuzgadoDePaz(String actaId) {
        return gestionExterna.derivarAJuzgadoDePaz(actaId);
    }

    /**
     * Demo: retorno efectivo desde la macro-bandeja {@code GESTION_EXTERNA} →
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link #ACCION_REVISION_POST_GESTION_EXTERNA}. Solo aplica a actas en
     * {@code GESTION_EXTERNA} con {@code permiteReingreso = true}. Preserva
     * el {@code tipoGestionExterna} como trazabilidad sintética de la
     * gestión externa de la que provino. Fachada pública; la lógica vive en
     * {@link GestionExternaSupport}.
     */
    public ReingresarDesdeGestionExternaResultado reingresarActaDesdeGestionExterna(String actaId) {
        return gestionExterna.reingresarActaDesdeGestionExterna(actaId);
    }

    /**
     * Helper interno de mocks: asigna el tipo de gestión externa vigente
     * para una acta precargada directamente en {@code GESTION_EXTERNA}, sin
     * ejecutar la transición operativa asociada. Uso restringido a la
     * fábrica de datos demo. Fachada: delega en {@link GestionExternaSupport}.
     */
    public void setTipoGestionExterna(String actaId, String tipo) {
        gestionExterna.setTipoGestionExterna(actaId, tipo);
    }

    /**
     * Demo: archivo directo desde análisis → macro-bandeja ARCHIVO. Fachada
     * pública; la lógica vive en {@link ArchivoReingresoSupport}.
     */
    public ArchivarActaResultado archivarActaDesdeAnalisis(String actaId) {
        return archivoReingreso.archivarActaDesdeAnalisis(actaId);
    }

    /**
     * Demo: archivo posterior a evaluación de vencimiento → macro-bandeja
     * ARCHIVO. Fachada pública; la lógica vive en
     * {@link ArchivoReingresoSupport}.
     */
    public ArchivarPorVencimientoResultado archivarPorVencimiento(String actaId) {
        return archivoReingreso.archivarPorVencimiento(actaId);
    }

    /**
     * Demo: reingreso explícito desde la macro-bandeja ARCHIVO → vuelve a
     * PENDIENTE_ANALISIS con marca {@link #ACCION_REVISION_POST_REINGRESO} y
     * {@code motivoArchivo} previo preservado. Fachada pública; la lógica vive
     * en {@link ArchivoReingresoSupport}.
     */
    public ReingresarActaResultado reingresarActaDesdeArchivo(String actaId) {
        return archivoReingreso.reingresarActaDesdeArchivo(actaId);
    }

    /**
     * Demo: produce la pieza MEDIDA_PREVENTIVA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarMedidaPreventivaResultado generarMedidaPreventiva(String actaId) {
        return piezasFirma.generarMedidaPreventiva(actaId);
    }

    /**
     * Demo: produce la pieza NOTIFICACION_ACTA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarNotificacionActaResultado generarNotificacionActa(String actaId) {
        return piezasFirma.generarNotificacionActa(actaId);
    }

    /**
     * Demo: produce la pieza NULIDAD como pieza no-fallo dentro del
     * circuito documental/resolutivo (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Comparte agregador con las otras piezas: si quedan
     * piezas pendientes, la acta permanece en la misma bandeja; si no,
     * pasa a PENDIENTE_FIRMA. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}.
     */
    public GenerarNulidadResultado generarNulidad(String actaId) {
        return piezasFirma.generarNulidad(actaId);
    }

    /**
     * Demo: firma individual de un documento puntual de la acta. La acta
     * sólo abandona la bandeja PENDIENTE_FIRMA cuando todos los documentos
     * firmables pasan a estado FIRMADO; al firmarse el último, pasa a
     * PENDIENTE_NOTIFICACION. Fachada pública; la lógica vive en
     * {@link PiezasFirmaSupport}, que delega en {@link NotificacionSupport}
     * la materialización inicial de la notificación cuando corresponde.
     */
    public FirmarDocumentoResultado firmarDocumento(String actaId, String documentoId) {
        return piezasFirma.firmarDocumento(actaId, documentoId);
    }
}
