package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
            "ARCHIVO",
            "CERRADAS");

    private static final String BANDEJA_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";
    private static final String BANDEJA_EN_NOTIFICACION = "EN_NOTIFICACION";
    private static final String BANDEJA_PENDIENTE_ANALISIS = "PENDIENTE_ANALISIS";
    private static final String BANDEJA_PENDIENTES_RESOLUCION_REDACCION = "PENDIENTES_RESOLUCION_REDACCION";
    private static final String BANDEJA_ARCHIVO = "ARCHIVO";
    private static final String BANDEJA_CERRADAS = "CERRADAS";
    private static final String BLOQUE_ARCHIVO = "ARCHIVO";
    private static final String ESTADO_ARCHIVADA_OPERATIVA = "ARCHIVADA_OPERATIVA";
    private static final String SITUACION_ARCHIVO = "ARCHIVO";
    private static final String BLOQUE_CERRADA = "CERRADA";
    private static final String BLOQUE_D4 = "D4_NOTIFICACION";
    private static final String BLOQUE_D5 = "D5_ANALISIS";
    private static final String ESTADO_PENDIENTE_ENVIO = "PENDIENTE_ENVIO";
    private static final String ESTADO_PENDIENTE_REVISION = "PENDIENTE_REVISION";
    private static final String ESTADO_ENTREGADA = "ENTREGADA";
    private static final String ESTADO_NO_ENTREGADA = "NO_ENTREGADA";
    private static final String ESTADO_VENCIDA = "VENCIDA";
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
    private static final String PIEZA_MEDIDA_PREVENTIVA = "MEDIDA_PREVENTIVA";
    private static final String PIEZA_NOTIFICACION_ACTA = "NOTIFICACION_ACTA";
    private static final String ESTADO_PENDIENTE_FIRMA_PIEZAS = "PENDIENTE_FIRMA_PIEZAS";
    private static final String ESTADO_PENDIENTE_PRODUCCION_PIEZAS = "PENDIENTE_PRODUCCION_PIEZAS";
    private static final String ESTADO_DOC_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String ESTADO_DOC_FIRMADO = "FIRMADO";

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
            String estadoProcesoActual) {
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
    }

    /**
     * Marca operativa vigente de la acta dentro de su bandeja, o {@code null}
     * si la acta no tiene acci��n pendiente marcada.
     */
    public String getAccionPendiente(String actaId) {
        return accionPendientePorActa.get(actaId);
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
     * Si la acta no tiene documentos cargados, lista vacía.
     */
    public List<ActaDocumentoMock> listarDocumentosPorActa(String actaId) {
        List<ActaDocumentoMock> lista = documentosPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return List.copyOf(lista);
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
     */
    public Optional<ActaPiezasRequeridasMock> findPiezasRequeridas(String actaId) {
        return Optional.ofNullable(piezasRequeridasPorActa.get(actaId));
    }

    /**
     * Catálogo de piezas requeridas; lista vacía si la acta no declara piezas.
     */
    public List<String> listarPiezasRequeridas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return List.of();
        }
        return List.copyOf(p.piezasRequeridas());
    }

    /**
     * Piezas ya producidas; lista vacía si la acta no declara piezas o no
     * se produjo ninguna todavía.
     */
    public List<String> listarPiezasGeneradas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasGeneradas() == null || p.piezasGeneradas().isEmpty()) {
            return List.of();
        }
        return List.copyOf(p.piezasGeneradas());
    }

    /**
     * Piezas requeridas que todavía no fueron producidas. Si la acta no
     * declara piezas requeridas, lista vacía.
     */
    public List<String> listarPiezasPendientes(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return List.of();
        }
        List<String> generadas = p.piezasGeneradas() == null ? List.of() : p.piezasGeneradas();
        return p.piezasRequeridas().stream()
                .filter(req -> !generadas.contains(req))
                .toList();
    }

    /**
     * {@code true} si la acta declara piezas requeridas y todas están ya
     * producidas. {@code false} si aún falta alguna, o si la acta no
     * declara piezas (en ese caso la regla de múltiples piezas no aplica).
     */
    public boolean todasLasPiezasProducidas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return false;
        }
        List<String> generadas = p.piezasGeneradas() == null ? List.of() : p.piezasGeneradas();
        return generadas.containsAll(p.piezasRequeridas());
    }

    /**
     * Demo: notificación entregada positivamente → bandeja análisis (solo desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION).
     */
    public RegistrarNotificacionPositivaResultado registrarNotificacionPositiva(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new RegistrarNotificacionPositivaResultado(RegistrarNotificacionPositivaEstado.NOT_FOUND, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new RegistrarNotificacionPositivaResultado(RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
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
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "NOTIFICACION_ENTREGADA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación fehaciente registrada; acta pasa a análisis jurídico."));

        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        if (!notifs.isEmpty()) {
            ActaNotificacionMock primera = notifs.get(0);
            notifs.set(0, new ActaNotificacionMock(
                    primera.id(),
                    primera.actaId(),
                    primera.canal(),
                    ESTADO_ENTREGADA,
                    primera.destinatarioResumen()));
        } else {
            String idNotif = "NOT-" + sufijoActa + "-01";
            notifs.add(new ActaNotificacionMock(
                    idNotif,
                    actaId,
                    "POSTAL",
                    ESTADO_ENTREGADA,
                    resumenDestinatarioEntregadaDemo(actual)));
        }

        accionPendientePorActa.remove(actaId);

        return new RegistrarNotificacionPositivaResultado(
                RegistrarNotificacionPositivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: notificación no entregada → acta retorna a PENDIENTE_ANALISIS con
     * marca operativa {@link #ACCION_REINTENTAR_NOTIFICACION}, para que el
     * caso sea distinguible y filtrable dentro de la bandeja de análisis.
     *
     * <p>Sólo aplica desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION. La
     * notificación existente pasa a {@code NO_ENTREGADA}; si no había
     * notificación cargada (caso demo atípico), se crea una con ese estado.
     * Se registra un evento {@code NOTIFICACION_NO_ENTREGADA} observable por
     * API. No se modelan todavía reintentos ni vencimientos.
     */
    public RegistrarNotificacionNegativaResultado registrarNotificacionNegativa(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new RegistrarNotificacionNegativaResultado(
                    RegistrarNotificacionNegativaEstado.NOT_FOUND, null, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new RegistrarNotificacionNegativaResultado(
                    RegistrarNotificacionNegativaEstado.CONFLICT, null, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
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
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, ACCION_REINTENTAR_NOTIFICACION);

        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        if (!notifs.isEmpty()) {
            ActaNotificacionMock primera = notifs.get(0);
            notifs.set(0, new ActaNotificacionMock(
                    primera.id(),
                    primera.actaId(),
                    primera.canal(),
                    ESTADO_NO_ENTREGADA,
                    primera.destinatarioResumen()));
        } else {
            String idNotif = "NOT-" + sufijoActa + "-01";
            notifs.add(new ActaNotificacionMock(
                    idNotif,
                    actaId,
                    "POSTAL",
                    ESTADO_NO_ENTREGADA,
                    resumenDestinatarioNoEntregadaDemo(actual)));
        }

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "NOTIFICACION_NO_ENTREGADA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación no entregada; acta retorna a análisis con acción pendiente "
                        + ACCION_REINTENTAR_NOTIFICACION + "."));

        return new RegistrarNotificacionNegativaResultado(
                RegistrarNotificacionNegativaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                ACCION_REINTENTAR_NOTIFICACION);
    }

    /**
     * Demo: reintento de notificación desde análisis. Solo aplica si la acta
     * está en {@link #BANDEJA_PENDIENTE_ANALISIS} y tiene
     * {@code accionPendiente = REINTENTAR_NOTIFICACION}. Devuelve el caso al
     * circuito operativo de notificación: acta a
     * {@link #BANDEJA_PENDIENTE_NOTIFICACION} con bloque {@link #BLOQUE_D4}
     * y estado {@link #ESTADO_PENDIENTE_ENVIO}; la notificación existente
     * pasa a {@code PENDIENTE_ENVIO}; se limpia la marca
     * {@code accionPendiente}; se registra evento
     * {@code NOTIFICACION_REINTENTADA}. No se crea una segunda notificación.
     */
    public ReintentarNotificacionResultado reintentarNotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new ReintentarNotificacionResultado(
                    ReintentarNotificacionEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())
                || !ACCION_REINTENTAR_NOTIFICACION.equals(accionPendientePorActa.get(actaId))) {
            return new ReintentarNotificacionResultado(
                    ReintentarNotificacionEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D4,
                ESTADO_PENDIENTE_ENVIO,
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
                BANDEJA_PENDIENTE_NOTIFICACION);
        actas.put(actaId, actualizada);

        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        if (!notifs.isEmpty()) {
            ActaNotificacionMock primera = notifs.get(0);
            notifs.set(0, new ActaNotificacionMock(
                    primera.id(),
                    primera.actaId(),
                    primera.canal(),
                    ESTADO_PENDIENTE_ENVIO,
                    primera.destinatarioResumen()));
        } else {
            String idNotif = "NOT-" + sufijoActa + "-01";
            notifs.add(new ActaNotificacionMock(
                    idNotif,
                    actaId,
                    "POSTAL",
                    ESTADO_PENDIENTE_ENVIO,
                    resumenDestinatarioDemo(actual)));
        }

        accionPendientePorActa.remove(actaId);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "NOTIFICACION_REINTENTADA",
                BLOQUE_D5,
                BLOQUE_D4,
                "Reintento de notificación solicitado desde análisis; acta vuelve a "
                        + BANDEJA_PENDIENTE_NOTIFICACION + "."));

        return new ReintentarNotificacionResultado(
                ReintentarNotificacionEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: notificación vencida → acta retorna a PENDIENTE_ANALISIS con
     * marca operativa {@link #ACCION_EVALUAR_NOTIFICACION_VENCIDA}, para que
     * el caso quede distinguible del reintento por no entrega y pueda
     * filtrarse dentro de la bandeja de análisis para decisión posterior.
     *
     * <p>Sólo aplica desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION. La
     * notificación existente pasa a {@code VENCIDA}; si no había notificación
     * cargada (caso demo atípico), se crea una con ese estado. Se registra un
     * evento {@code NOTIFICACION_VENCIDA} observable por API. No se modelan
     * todavía reintentos automáticos por vencimiento ni la decisión posterior
     * (nuevo intento, archivo, apremio u otra salida).
     */
    public RegistrarNotificacionVencidaResultado registrarNotificacionVencida(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new RegistrarNotificacionVencidaResultado(
                    RegistrarNotificacionVencidaEstado.NOT_FOUND, null, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new RegistrarNotificacionVencidaResultado(
                    RegistrarNotificacionVencidaEstado.CONFLICT, null, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
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
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, ACCION_EVALUAR_NOTIFICACION_VENCIDA);

        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        if (!notifs.isEmpty()) {
            ActaNotificacionMock primera = notifs.get(0);
            notifs.set(0, new ActaNotificacionMock(
                    primera.id(),
                    primera.actaId(),
                    primera.canal(),
                    ESTADO_VENCIDA,
                    primera.destinatarioResumen()));
        } else {
            String idNotif = "NOT-" + sufijoActa + "-01";
            notifs.add(new ActaNotificacionMock(
                    idNotif,
                    actaId,
                    "POSTAL",
                    ESTADO_VENCIDA,
                    resumenDestinatarioVencidaDemo(actual)));
        }

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "NOTIFICACION_VENCIDA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación vencida sin entrega ni rechazo explícito; acta retorna a análisis con acción pendiente "
                        + ACCION_EVALUAR_NOTIFICACION_VENCIDA + "."));

        return new RegistrarNotificacionVencidaResultado(
                RegistrarNotificacionVencidaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                ACCION_EVALUAR_NOTIFICACION_VENCIDA);
    }

    /**
     * Demo: cierre desde análisis → bandeja CERRADAS (solo desde PENDIENTE_ANALISIS).
     */
    public CerrarActaResultado cerrarActaDesdeAnalisis(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new CerrarActaResultado(CerrarActaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new CerrarActaResultado(CerrarActaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_CERRADA,
                BLOQUE_CERRADA,
                BLOQUE_CERRADA,
                true,
                false,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_CERRADAS);
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "CIERRE_ANALISIS",
                BLOQUE_D5,
                BLOQUE_CERRADA,
                "Análisis jurídico concluido; acta cerrada administrativamente."));

        accionPendientePorActa.remove(actaId);

        return new CerrarActaResultado(
                CerrarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: archivo desde análisis → bandeja ARCHIVO (solo desde PENDIENTE_ANALISIS).
     */
    public ArchivarActaResultado archivarActaDesdeAnalisis(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new ArchivarActaResultado(ArchivarActaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new ArchivarActaResultado(ArchivarActaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_ARCHIVO,
                ESTADO_ARCHIVADA_OPERATIVA,
                SITUACION_ARCHIVO,
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_ARCHIVO);
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "ARCHIVADO_DESDE_ANALISIS",
                BLOQUE_D5,
                BLOQUE_ARCHIVO,
                "Análisis jurídico archiva el acta; pasa a archivo operativo con posibilidad de reingreso."));

        accionPendientePorActa.remove(actaId);

        return new ArchivarActaResultado(
                ArchivarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: produce la pieza MEDIDA_PREVENTIVA (solo desde PENDIENTES_RESOLUCION_REDACCION
     * y solo si la acta declara esa pieza como requerida). Si aún quedan otras piezas
     * por producir, la acta permanece en la misma bandeja con estado coherente; si no
     * queda ninguna pieza pendiente, pasa a la bandeja PENDIENTE_FIRMA conservando el
     * bloque D5_ANALISIS (la pieza producida en análisis espera ahora su firma; no se
     * retrocede a un bloque documental previo).
     */
    public GenerarMedidaPreventivaResultado generarMedidaPreventiva(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new GenerarMedidaPreventivaResultado(GenerarMedidaPreventivaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTES_RESOLUCION_REDACCION.equals(actual.bandejaActual())) {
            return new GenerarMedidaPreventivaResultado(GenerarMedidaPreventivaEstado.CONFLICT, null, null, null);
        }

        ActaPiezasRequeridasMock piezas = piezasRequeridasPorActa.get(actaId);
        if (piezas == null
                || piezas.piezasRequeridas() == null
                || !piezas.piezasRequeridas().contains(PIEZA_MEDIDA_PREVENTIVA)) {
            return new GenerarMedidaPreventivaResultado(GenerarMedidaPreventivaEstado.CONFLICT, null, null, null);
        }
        List<String> generadasActuales = piezas.piezasGeneradas() == null
                ? new ArrayList<>()
                : new ArrayList<>(piezas.piezasGeneradas());
        if (generadasActuales.contains(PIEZA_MEDIDA_PREVENTIVA)) {
            return new GenerarMedidaPreventivaResultado(GenerarMedidaPreventivaEstado.CONFLICT, null, null, null);
        }

        generadasActuales.add(PIEZA_MEDIDA_PREVENTIVA);
        piezasRequeridasPorActa.put(actaId, new ActaPiezasRequeridasMock(
                actaId,
                piezas.piezasRequeridas(),
                generadasActuales));

        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;

        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteDoc = docs.size() + 1;
        String idDoc = "DOC-" + sufijoActa + "-" + String.format("%02d", siguienteDoc);
        docs.add(new ActaDocumentoMock(
                idDoc,
                actaId,
                PIEZA_MEDIDA_PREVENTIVA,
                ESTADO_DOC_PENDIENTE_FIRMA,
                "medida_preventiva_" + sufijoActa + ".pdf"));

        List<String> piezasPendientes = piezas.piezasRequeridas().stream()
                .filter(req -> !generadasActuales.contains(req))
                .toList();
        boolean todasProducidas = piezasPendientes.isEmpty();

        String bloqueDestino = actual.bloqueActual();
        String estadoDestino;
        String bandejaDestino;
        if (todasProducidas) {
            estadoDestino = ESTADO_PENDIENTE_FIRMA_PIEZAS;
            bandejaDestino = BANDEJA_PENDIENTE_FIRMA;
        } else {
            estadoDestino = ESTADO_PENDIENTE_PRODUCCION_PIEZAS;
            bandejaDestino = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                bloqueDestino,
                estadoDestino,
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
                bandejaDestino);
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteEvt = eventos.size() + 1;
        String idEvt = "EVT-" + sufijoActa + "-" + String.format("%02d", siguienteEvt);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        String descripcionEvento = todasProducidas
                ? "Medida preventiva producida; piezas completas, queda pendiente su firma."
                : "Medida preventiva producida; aún resta producir otras piezas requeridas.";
        eventos.add(new ActaEventoMock(
                idEvt,
                actaId,
                fechaEvento,
                "MEDIDA_PREVENTIVA_GENERADA",
                actual.bloqueActual(),
                bloqueDestino,
                descripcionEvento));

        return new GenerarMedidaPreventivaResultado(
                GenerarMedidaPreventivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: produce la pieza NOTIFICACION_ACTA (solo desde PENDIENTES_RESOLUCION_REDACCION
     * y solo si la acta declara esa pieza como requerida). Si aún quedan otras piezas
     * por producir, la acta permanece en la misma bandeja con estado coherente; si no
     * queda ninguna pieza pendiente, pasa a la bandeja PENDIENTE_FIRMA conservando el
     * bloque actual del expediente (no se rebobina a un bloque documental previo
     * genérico) y adoptando el estado agregador PENDIENTE_FIRMA_PIEZAS, que no
     * depende de cuál fue la última pieza generada.
     */
    public GenerarNotificacionActaResultado generarNotificacionActa(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new GenerarNotificacionActaResultado(GenerarNotificacionActaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTES_RESOLUCION_REDACCION.equals(actual.bandejaActual())) {
            return new GenerarNotificacionActaResultado(GenerarNotificacionActaEstado.CONFLICT, null, null, null);
        }

        ActaPiezasRequeridasMock piezas = piezasRequeridasPorActa.get(actaId);
        if (piezas == null
                || piezas.piezasRequeridas() == null
                || !piezas.piezasRequeridas().contains(PIEZA_NOTIFICACION_ACTA)) {
            return new GenerarNotificacionActaResultado(GenerarNotificacionActaEstado.CONFLICT, null, null, null);
        }
        List<String> generadasActuales = piezas.piezasGeneradas() == null
                ? new ArrayList<>()
                : new ArrayList<>(piezas.piezasGeneradas());
        if (generadasActuales.contains(PIEZA_NOTIFICACION_ACTA)) {
            return new GenerarNotificacionActaResultado(GenerarNotificacionActaEstado.CONFLICT, null, null, null);
        }

        generadasActuales.add(PIEZA_NOTIFICACION_ACTA);
        piezasRequeridasPorActa.put(actaId, new ActaPiezasRequeridasMock(
                actaId,
                piezas.piezasRequeridas(),
                generadasActuales));

        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;

        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteDoc = docs.size() + 1;
        String idDoc = "DOC-" + sufijoActa + "-" + String.format("%02d", siguienteDoc);
        docs.add(new ActaDocumentoMock(
                idDoc,
                actaId,
                PIEZA_NOTIFICACION_ACTA,
                ESTADO_DOC_PENDIENTE_FIRMA,
                "notificacion_acta_" + sufijoActa + ".pdf"));

        List<String> piezasPendientes = piezas.piezasRequeridas().stream()
                .filter(req -> !generadasActuales.contains(req))
                .toList();
        boolean todasProducidas = piezasPendientes.isEmpty();

        String bloqueDestino = actual.bloqueActual();
        String estadoDestino;
        String bandejaDestino;
        if (todasProducidas) {
            estadoDestino = ESTADO_PENDIENTE_FIRMA_PIEZAS;
            bandejaDestino = BANDEJA_PENDIENTE_FIRMA;
        } else {
            estadoDestino = ESTADO_PENDIENTE_PRODUCCION_PIEZAS;
            bandejaDestino = BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                bloqueDestino,
                estadoDestino,
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
                bandejaDestino);
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteEvt = eventos.size() + 1;
        String idEvt = "EVT-" + sufijoActa + "-" + String.format("%02d", siguienteEvt);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        String descripcionEvento = todasProducidas
                ? "Notificación del acta producida; piezas completas, queda pendiente su firma."
                : "Notificación del acta producida; aún resta producir otras piezas requeridas.";
        eventos.add(new ActaEventoMock(
                idEvt,
                actaId,
                fechaEvento,
                "NOTIFICACION_ACTA_GENERADA",
                actual.bloqueActual(),
                bloqueDestino,
                descripcionEvento));

        return new GenerarNotificacionActaResultado(
                GenerarNotificacionActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: firma individual de un documento puntual de la acta. Los documentos
     * tienen identidad propia y se firman de a uno. La acta sólo abandona la
     * bandeja PENDIENTE_FIRMA cuando todos los documentos firmables pasan a
     * estado FIRMADO. Si aún quedan documentos pendientes, la acta permanece
     * en PENDIENTE_FIRMA con estado {@code PENDIENTE_FIRMA_PIEZAS} y su bloque
     * actual; cuando se firma el último, pasa a PENDIENTE_NOTIFICACION con
     * bloque D4_NOTIFICACION y estado PENDIENTE_ENVIO (no se rebobina bloque).
     */
    public FirmarDocumentoResultado firmarDocumento(String actaId, String documentoId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new FirmarDocumentoResultado(FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_FIRMA.equals(actual.bandejaActual())) {
            return new FirmarDocumentoResultado(FirmarDocumentoEstado.CONFLICT, null, null, null, null);
        }

        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null || docs.isEmpty()) {
            return new FirmarDocumentoResultado(FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        int indice = -1;
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).id().equals(documentoId)) {
                indice = i;
                break;
            }
        }
        if (indice < 0) {
            return new FirmarDocumentoResultado(FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        ActaDocumentoMock doc = docs.get(indice);
        if (!ESTADO_DOC_PENDIENTE_FIRMA.equals(doc.estadoDocumento())) {
            return new FirmarDocumentoResultado(FirmarDocumentoEstado.CONFLICT, null, null, null, null);
        }

        docs.set(indice, new ActaDocumentoMock(
                doc.id(),
                doc.actaId(),
                doc.tipoDocumento(),
                ESTADO_DOC_FIRMADO,
                doc.nombreArchivo()));

        boolean restanPendientes = docs.stream()
                .anyMatch(d -> ESTADO_DOC_PENDIENTE_FIRMA.equals(d.estadoDocumento()));

        String bloqueOrigen = actual.bloqueActual();
        String bloqueDestino;
        String estadoDestino;
        String bandejaDestino;
        boolean tieneNotificacionesDestino;
        if (restanPendientes) {
            bloqueDestino = actual.bloqueActual();
            estadoDestino = ESTADO_PENDIENTE_FIRMA_PIEZAS;
            bandejaDestino = BANDEJA_PENDIENTE_FIRMA;
            tieneNotificacionesDestino = actual.tieneNotificaciones();
        } else {
            bloqueDestino = BLOQUE_D4;
            estadoDestino = ESTADO_PENDIENTE_ENVIO;
            bandejaDestino = BANDEJA_PENDIENTE_NOTIFICACION;
            tieneNotificacionesDestino = true;
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                bloqueDestino,
                estadoDestino,
                actual.situacionAdministrativaActual(),
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                tieneNotificacionesDestino,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                bandejaDestino);
        actas.put(actaId, actualizada);

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguienteEvt = eventos.size() + 1;
        String idEvt = "EVT-" + sufijoActa + "-" + String.format("%02d", siguienteEvt);
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        String descripcionEvento = restanPendientes
                ? "Documento " + documentoId + " firmado; aún quedan documentos pendientes de firma en la acta."
                : "Documento " + documentoId + " firmado; último pendiente, acta pasa a notificación.";
        eventos.add(new ActaEventoMock(
                idEvt,
                actaId,
                fechaEvento,
                "DOCUMENTO_FIRMADO",
                bloqueOrigen,
                bloqueDestino,
                descripcionEvento));

        if (!restanPendientes && !actual.tieneNotificaciones()) {
            List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
            if (notifs.isEmpty()) {
                String idNotif = "NOT-" + sufijoActa + "-01";
                notifs.add(new ActaNotificacionMock(
                        idNotif,
                        actaId,
                        "POSTAL",
                        ESTADO_PENDIENTE_ENVIO,
                        resumenDestinatarioDemo(actual)));
            }
        }

        return new FirmarDocumentoResultado(
                FirmarDocumentoEstado.OK,
                actualizada.id(),
                documentoId,
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    private static String resumenDestinatarioDemo(ActaMock a) {
        return a.infractorNombre() + " — pendiente constancia de domicilio";
    }

    private static String resumenDestinatarioEntregadaDemo(ActaMock a) {
        return a.infractorNombre() + " — constancia de entrega postal (demo)";
    }

    private static String resumenDestinatarioNoEntregadaDemo(ActaMock a) {
        return a.infractorNombre() + " — devuelta por correo: destinatario no habido (demo)";
    }

    private static String resumenDestinatarioVencidaDemo(ActaMock a) {
        return a.infractorNombre() + " — notificación vencida sin acuse (demo)";
    }
}
