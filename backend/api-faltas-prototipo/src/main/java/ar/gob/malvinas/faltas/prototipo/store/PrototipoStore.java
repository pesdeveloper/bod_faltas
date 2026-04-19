package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
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
            "ARCHIVO",
            "CERRADAS");

    private static final String BANDEJA_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String BANDEJA_PENDIENTE_NOTIFICACION = "PENDIENTE_NOTIFICACION";
    private static final String BANDEJA_EN_NOTIFICACION = "EN_NOTIFICACION";
    private static final String BANDEJA_PENDIENTE_ANALISIS = "PENDIENTE_ANALISIS";
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

    public enum PasarANotificacionEstado {
        OK,
        NOT_FOUND,
        CONFLICT
    }

    /**
     * Resultado interno de la acción demo; en OK los tres strings son no nulos.
     */
    public record PasarANotificacionResultado(
            PasarANotificacionEstado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {
    }

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

    public record BandejaConteo(String codigo, int cantidadActas) {
    }

    private final Map<String, ActaMock> actas = new LinkedHashMap<>();
    private final Map<String, List<ActaEventoMock>> eventosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa = new LinkedHashMap<>();
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa = new LinkedHashMap<>();

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

    public void clearAll() {
        actas.clear();
        eventosPorActa.clear();
        documentosPorActa.clear();
        notificacionesPorActa.clear();
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
        if (codigoBandeja == null) {
            return List.of();
        }
        return actas.values().stream()
                .filter(a -> codigoBandeja.equals(a.bandejaActual()))
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
     * Demo: firma completada → bandeja notificación (solo desde bandeja PENDIENTE_FIRMA).
     */
    public PasarANotificacionResultado pasarActaANotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PasarANotificacionResultado(PasarANotificacionEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_FIRMA.equals(actual.bandejaActual())) {
            return new PasarANotificacionResultado(PasarANotificacionEstado.CONFLICT, null, null, null);
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
                "FIRMA_COMPLETADA",
                "D3_DOCUMENTAL",
                BLOQUE_D4,
                "Acta firmada; pasa a notificación."));

        if (!actual.tieneNotificaciones()) {
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

        return new PasarANotificacionResultado(
                PasarANotificacionEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
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

        return new RegistrarNotificacionPositivaResultado(
                RegistrarNotificacionPositivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
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

        return new ArchivarActaResultado(
                ArchivarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    private static String resumenDestinatarioDemo(ActaMock a) {
        return a.infractorNombre() + " — pendiente constancia de domicilio";
    }

    private static String resumenDestinatarioEntregadaDemo(ActaMock a) {
        return a.infractorNombre() + " — constancia de entrega postal (demo)";
    }
}
