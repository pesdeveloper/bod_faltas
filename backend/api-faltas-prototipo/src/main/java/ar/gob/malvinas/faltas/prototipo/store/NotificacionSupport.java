package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_EN_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D4;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_ENVIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;

/**
 * Soporte funcional del área notificación del prototipo. Extraído de
 * {@link PrototipoStore} para bajar su tamaño y acoplamiento sin cambiar
 * comportamiento observable: mismos endpoints, mismas transiciones, mismos
 * eventos, mismas marcas operativas.
 *
 * <p>Concentra:
 * <ul>
 *   <li>notificación positiva ({@code registrarNotificacionPositiva}),</li>
 *   <li>notificación negativa ({@code registrarNotificacionNegativa}),</li>
 *   <li>notificación vencida ({@code registrarNotificacionVencida}),</li>
 *   <li>reintento por no entrega ({@code reintentarNotificacion}),</li>
 *   <li>reintento post-vencimiento ({@code reintentarNotificacionVencida}),</li>
 *   <li>estados internos de la notificación (entregada, no entregada,
 *       vencida) y resumen del destinatario demo,</li>
 *   <li>eventos específicos del área y marcas operativas asociadas
 *       ({@code REINTENTAR_NOTIFICACION}, {@code EVALUAR_NOTIFICACION_VENCIDA}).</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code notificacionesPorActa}, {@code accionPendientePorActa}). Las
 * marcas operativas {@code ACCION_REINTENTAR_NOTIFICACION} y
 * {@code ACCION_EVALUAR_NOTIFICACION_VENCIDA} siguen expuestas como
 * constantes públicas de {@link PrototipoStore} para no cambiar su
 * superficie pública ni romper los consumidores ya escritos.
 */
final class NotificacionSupport {

    private static final String ESTADO_ENTREGADA = "ENTREGADA";
    private static final String ESTADO_NO_ENTREGADA = "NO_ENTREGADA";
    private static final String ESTADO_VENCIDA = "VENCIDA";
    private static final String CANAL_POSTAL = "POSTAL";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final Map<String, String> accionPendientePorActa;

    NotificacionSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            Map<String, String> accionPendientePorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
    }

    /**
     * Demo: notificación entregada positivamente → bandeja análisis (solo
     * desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION).
     */
    PrototipoStore.RegistrarNotificacionPositivaResultado registrarNotificacionPositiva(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                    PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = moverAAnalisis(actual, true);
        actas.put(actaId, actualizada);

        actualizarOCrearNotificacion(
                actaId,
                actual,
                ESTADO_ENTREGADA,
                NotificacionSupport::resumenDestinatarioEntregadaDemo);

        accionPendientePorActa.remove(actaId);

        registrarEvento(
                actaId,
                "NOTIFICACION_ENTREGADA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación fehaciente registrada; acta pasa a análisis jurídico.");

        return new PrototipoStore.RegistrarNotificacionPositivaResultado(
                PrototipoStore.RegistrarNotificacionPositivaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: notificación no entregada → acta retorna a PENDIENTE_ANALISIS con
     * marca operativa {@link PrototipoStore#ACCION_REINTENTAR_NOTIFICACION},
     * para que el caso sea distinguible y filtrable dentro de la bandeja de
     * análisis. Sólo aplica desde PENDIENTE_NOTIFICACION o EN_NOTIFICACION.
     * La notificación existente pasa a {@code NO_ENTREGADA}; si no había
     * notificación cargada (caso demo atípico), se crea una con ese estado.
     */
    PrototipoStore.RegistrarNotificacionNegativaResultado registrarNotificacionNegativa(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarNotificacionNegativaResultado(
                    PrototipoStore.RegistrarNotificacionNegativaEstado.NOT_FOUND, null, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new PrototipoStore.RegistrarNotificacionNegativaResultado(
                    PrototipoStore.RegistrarNotificacionNegativaEstado.CONFLICT, null, null, null, null);
        }

        ActaMock actualizada = moverAAnalisis(actual, true);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REINTENTAR_NOTIFICACION);

        actualizarOCrearNotificacion(
                actaId,
                actual,
                ESTADO_NO_ENTREGADA,
                NotificacionSupport::resumenDestinatarioNoEntregadaDemo);

        registrarEvento(
                actaId,
                "NOTIFICACION_NO_ENTREGADA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación no entregada; acta retorna a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REINTENTAR_NOTIFICACION + ".");

        return new PrototipoStore.RegistrarNotificacionNegativaResultado(
                PrototipoStore.RegistrarNotificacionNegativaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REINTENTAR_NOTIFICACION);
    }

    /**
     * Demo: reintento de notificación desde análisis. Solo aplica si la acta
     * está en PENDIENTE_ANALISIS y tiene
     * {@code accionPendiente = REINTENTAR_NOTIFICACION}. Devuelve el caso al
     * circuito operativo reutilizando la notificación existente.
     */
    PrototipoStore.ReintentarNotificacionResultado reintentarNotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReintentarNotificacionResultado(
                    PrototipoStore.ReintentarNotificacionEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())
                || !PrototipoStore.ACCION_REINTENTAR_NOTIFICACION.equals(accionPendientePorActa.get(actaId))) {
            return new PrototipoStore.ReintentarNotificacionResultado(
                    PrototipoStore.ReintentarNotificacionEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = moverAPendienteNotificacion(actual);
        actas.put(actaId, actualizada);

        actualizarOCrearNotificacion(
                actaId,
                actual,
                ESTADO_PENDIENTE_ENVIO,
                NotificacionSupport::resumenDestinatarioDemo);

        accionPendientePorActa.remove(actaId);

        registrarEvento(
                actaId,
                "NOTIFICACION_REINTENTADA",
                BLOQUE_D5,
                BLOQUE_D4,
                "Reintento de notificación solicitado desde análisis; acta vuelve a "
                        + BANDEJA_PENDIENTE_NOTIFICACION + ".");

        return new PrototipoStore.ReintentarNotificacionResultado(
                PrototipoStore.ReintentarNotificacionEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: notificación vencida → acta retorna a PENDIENTE_ANALISIS con
     * marca operativa {@link PrototipoStore#ACCION_EVALUAR_NOTIFICACION_VENCIDA}.
     */
    PrototipoStore.RegistrarNotificacionVencidaResultado registrarNotificacionVencida(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarNotificacionVencidaResultado(
                    PrototipoStore.RegistrarNotificacionVencidaEstado.NOT_FOUND, null, null, null, null);
        }
        String bandeja = actual.bandejaActual();
        if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(bandeja) && !BANDEJA_EN_NOTIFICACION.equals(bandeja)) {
            return new PrototipoStore.RegistrarNotificacionVencidaResultado(
                    PrototipoStore.RegistrarNotificacionVencidaEstado.CONFLICT, null, null, null, null);
        }

        ActaMock actualizada = moverAAnalisis(actual, true);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA);

        actualizarOCrearNotificacion(
                actaId,
                actual,
                ESTADO_VENCIDA,
                NotificacionSupport::resumenDestinatarioVencidaDemo);

        registrarEvento(
                actaId,
                "NOTIFICACION_VENCIDA",
                BLOQUE_D4,
                BLOQUE_D5,
                "Notificación vencida sin entrega ni rechazo explícito; acta retorna a análisis con acción pendiente "
                        + PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA + ".");

        return new PrototipoStore.RegistrarNotificacionVencidaResultado(
                PrototipoStore.RegistrarNotificacionVencidaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA);
    }

    /**
     * Demo: decisión posterior mínima sobre un caso que volvió a análisis por
     * vencimiento de notificación. Solo aplica si la acta está en
     * PENDIENTE_ANALISIS y tiene
     * {@code accionPendiente = EVALUAR_NOTIFICACION_VENCIDA}. Reutiliza la
     * notificación existente (no se crea una segunda) y registra evento
     * {@code NOTIFICACION_REINTENTADA_POST_VENCIMIENTO} para distinguirlo del
     * reintento por no entrega.
     */
    PrototipoStore.ReintentarNotificacionVencidaResultado reintentarNotificacionVencida(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReintentarNotificacionVencidaResultado(
                    PrototipoStore.ReintentarNotificacionVencidaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())
                || !PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA.equals(accionPendientePorActa.get(actaId))) {
            return new PrototipoStore.ReintentarNotificacionVencidaResultado(
                    PrototipoStore.ReintentarNotificacionVencidaEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = moverAPendienteNotificacion(actual);
        actas.put(actaId, actualizada);

        actualizarOCrearNotificacion(
                actaId,
                actual,
                ESTADO_PENDIENTE_ENVIO,
                NotificacionSupport::resumenDestinatarioDemo);

        accionPendientePorActa.remove(actaId);

        registrarEvento(
                actaId,
                "NOTIFICACION_REINTENTADA_POST_VENCIMIENTO",
                BLOQUE_D5,
                BLOQUE_D4,
                "Decisión posterior al vencimiento: se reintenta la notificación; acta vuelve a "
                        + BANDEJA_PENDIENTE_NOTIFICACION + ".");

        return new PrototipoStore.ReintentarNotificacionVencidaResultado(
                PrototipoStore.ReintentarNotificacionVencidaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Materializa la notificación inicial demo de la acta si todavía no
     * existe. Pensado para ser invocado desde el área firma cuando se
     * completa el último documento pendiente y el acta pasa a
     * PENDIENTE_NOTIFICACION sin notificación previa. No registra evento
     * propio (el evento lo registra firma): este método es sólo el alta
     * mínima del registro {@link ActaNotificacionMock} en estado
     * {@code PENDIENTE_ENVIO} con el resumen de destinatario demo.
     */
    void asegurarNotificacionInicialPendiente(String actaId, ActaMock acta) {
        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        if (!notifs.isEmpty()) {
            return;
        }
        String sufijoActa = sufijoActa(actaId);
        String idNotif = "NOT-" + sufijoActa + "-01";
        notifs.add(new ActaNotificacionMock(
                idNotif,
                actaId,
                CANAL_POSTAL,
                ESTADO_PENDIENTE_ENVIO,
                resumenDestinatarioDemo(acta)));
    }

    private ActaMock moverAAnalisis(ActaMock actual, boolean tieneNotificaciones) {
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
                tieneNotificaciones,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
    }

    private ActaMock moverAPendienteNotificacion(ActaMock actual) {
        return new ActaMock(
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
    }

    private void actualizarOCrearNotificacion(
            String actaId,
            ActaMock acta,
            String nuevoEstado,
            java.util.function.Function<ActaMock, String> resumenSiNueva) {
        List<ActaNotificacionMock> notifs = notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        if (!notifs.isEmpty()) {
            ActaNotificacionMock primera = notifs.get(0);
            notifs.set(0, new ActaNotificacionMock(
                    primera.id(),
                    primera.actaId(),
                    primera.canal(),
                    nuevoEstado,
                    primera.destinatarioResumen()));
            return;
        }
        String sufijoActa = sufijoActa(actaId);
        String idNotif = "NOT-" + sufijoActa + "-01";
        notifs.add(new ActaNotificacionMock(
                idNotif,
                actaId,
                CANAL_POSTAL,
                nuevoEstado,
                resumenSiNueva.apply(acta)));
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
        LocalDateTime fechaEvento = eventos.stream()
                .map(ActaEventoMock::fechaHora)
                .max(Comparator.naturalOrder())
                .map(t -> t.plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }

    private static String sufijoActa(String actaId) {
        return actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
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
