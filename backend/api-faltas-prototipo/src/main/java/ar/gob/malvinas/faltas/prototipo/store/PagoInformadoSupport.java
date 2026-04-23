package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;

/**
 * Soporte funcional mínimo del circuito de "pago informado" + comprobante mock
 * + confirmación/observación mock.
 *
 * <p>Regla clave de modelado del prototipo:
 * <ul>
 *   <li>El hecho informado por el administrado (pago informado, comprobante)
 *       vive en {@link PrototipoStore.PagoInformadoMock}.</li>
 *   <li>La validación interna (pendiente/confirmado/observado) vive en
 *       {@link PrototipoStore.SituacionPagoMock}.</li>
 *   <li>La tarea operativa actual se expresa con
 *       {@link PrototipoStore#ACCION_VERIFICAR_PAGO_INFORMADO} en
 *       {@code accionPendiente}.</li>
 * </ul>
 *
 * <p>No cierra automáticamente: la spec centraliza la evaluación material en
 * la bandeja de análisis y este soporte sólo hace visible el estado de pago.
 */
final class PagoInformadoSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa;
    private final Map<String, PrototipoStore.PagoInformadoMock> pagoInformadoPorActa;

    PagoInformadoSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa,
            Map<String, PrototipoStore.PagoInformadoMock> pagoInformadoPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.situacionPagoPorActa = situacionPagoPorActa;
        this.pagoInformadoPorActa = pagoInformadoPorActa;
    }

    PrototipoStore.RegistrarPagoInformadoResultado registrarPagoInformado(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarPagoInformadoResultado(
                    PrototipoStore.RegistrarPagoInformadoEstado.NOT_FOUND,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.RegistrarPagoInformadoResultado(
                    PrototipoStore.RegistrarPagoInformadoEstado.CONFLICT,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO);
        }

        // Hecho informado: se registra, pero NO implica confirmación.
        pagoInformadoPorActa.put(actaId, new PrototipoStore.PagoInformadoMock(
                LocalDateTime.now(),
                null,
                null));
        setSituacionPago(actaId, PrototipoStore.SituacionPagoMock.PAGO_INFORMADO);

        registrarEvento(
                actaId,
                "PAGO_INFORMADO",
                actual.bloqueActual(),
                actual.bloqueActual(),
                "Pago informado por el administrado (mock). Aún sin comprobante y sin confirmación interna.");

        return new PrototipoStore.RegistrarPagoInformadoResultado(
                PrototipoStore.RegistrarPagoInformadoEstado.OK,
                actaId,
                PrototipoStore.SituacionPagoMock.PAGO_INFORMADO);
    }

    PrototipoStore.AdjuntarComprobantePagoInformadoResultado adjuntarComprobantePagoInformado(
            String actaId,
            String comprobanteNombreArchivo) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.AdjuntarComprobantePagoInformadoResultado(
                    PrototipoStore.AdjuntarComprobantePagoInformadoEstado.NOT_FOUND,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO,
                    null,
                    null);
        }
        if (actual.estaCerrada()) {
            return new PrototipoStore.AdjuntarComprobantePagoInformadoResultado(
                    PrototipoStore.AdjuntarComprobantePagoInformadoEstado.CONFLICT,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO,
                    null,
                    null);
        }
        PrototipoStore.PagoInformadoMock previo = pagoInformadoPorActa.get(actaId);
        if (previo == null) {
            return new PrototipoStore.AdjuntarComprobantePagoInformadoResultado(
                    PrototipoStore.AdjuntarComprobantePagoInformadoEstado.CONFLICT,
                    null,
                    getSituacionPago(actaId),
                    null,
                    null);
        }

        String nombre = (comprobanteNombreArchivo != null && !comprobanteNombreArchivo.isBlank())
                ? comprobanteNombreArchivo.trim()
                : "comprobante_pago_" + actaId.toLowerCase().replace("acta-", "") + ".pdf";
        String comprobanteId = "COMP-" + actaId;
        PrototipoStore.PagoInformadoMock actualizado = new PrototipoStore.PagoInformadoMock(
                previo.fechaInformado(),
                comprobanteId,
                nombre);
        pagoInformadoPorActa.put(actaId, actualizado);

        // Validación interna: se marca pendiente de confirmación, y se asigna la tarea operativa.
        setSituacionPago(actaId, PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION);
        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO);

        registrarEvento(
                actaId,
                "COMPROBANTE_PAGO_ADJUNTADO",
                actual.bloqueActual(),
                BLOQUE_D5,
                "Comprobante de pago adjuntado (mock). Queda pendiente de confirmación interna.");

        return new PrototipoStore.AdjuntarComprobantePagoInformadoResultado(
                PrototipoStore.AdjuntarComprobantePagoInformadoEstado.OK,
                actaId,
                PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION,
                PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO,
                actualizado);
    }

    PrototipoStore.ConfirmarPagoInformadoResultado confirmarPagoInformado(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ConfirmarPagoInformadoResultado(
                    PrototipoStore.ConfirmarPagoInformadoEstado.NOT_FOUND,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO);
        }
        if (getSituacionPago(actaId) != PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return new PrototipoStore.ConfirmarPagoInformadoResultado(
                    PrototipoStore.ConfirmarPagoInformadoEstado.CONFLICT,
                    null,
                    getSituacionPago(actaId));
        }
        setSituacionPago(actaId, PrototipoStore.SituacionPagoMock.CONFIRMADO);
        limpiarAccionVerificarPagoInformado(actaId);

        registrarEvento(
                actaId,
                "PAGO_CONFIRMADO",
                BLOQUE_D5,
                BLOQUE_D5,
                "Pago confirmado internamente (mock). No implica cierre automático.");

        return new PrototipoStore.ConfirmarPagoInformadoResultado(
                PrototipoStore.ConfirmarPagoInformadoEstado.OK,
                actaId,
                PrototipoStore.SituacionPagoMock.CONFIRMADO);
    }

    PrototipoStore.ObservarPagoInformadoResultado observarPagoInformado(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ObservarPagoInformadoResultado(
                    PrototipoStore.ObservarPagoInformadoEstado.NOT_FOUND,
                    null,
                    PrototipoStore.SituacionPagoMock.SIN_PAGO);
        }
        if (getSituacionPago(actaId) != PrototipoStore.SituacionPagoMock.PENDIENTE_CONFIRMACION) {
            return new PrototipoStore.ObservarPagoInformadoResultado(
                    PrototipoStore.ObservarPagoInformadoEstado.CONFLICT,
                    null,
                    getSituacionPago(actaId));
        }
        setSituacionPago(actaId, PrototipoStore.SituacionPagoMock.OBSERVADO);
        limpiarAccionVerificarPagoInformado(actaId);

        registrarEvento(
                actaId,
                "PAGO_OBSERVADO",
                BLOQUE_D5,
                BLOQUE_D5,
                "Pago observado/no confirmado internamente (mock).");

        return new PrototipoStore.ObservarPagoInformadoResultado(
                PrototipoStore.ObservarPagoInformadoEstado.OK,
                actaId,
                PrototipoStore.SituacionPagoMock.OBSERVADO);
    }

    private void setSituacionPago(String actaId, PrototipoStore.SituacionPagoMock situacion) {
        if (actaId == null || situacion == null || situacion == PrototipoStore.SituacionPagoMock.SIN_PAGO) {
            situacionPagoPorActa.remove(actaId);
            return;
        }
        situacionPagoPorActa.put(actaId, situacion);
    }

    private PrototipoStore.SituacionPagoMock getSituacionPago(String actaId) {
        PrototipoStore.SituacionPagoMock v = situacionPagoPorActa.get(actaId);
        return v != null ? v : PrototipoStore.SituacionPagoMock.SIN_PAGO;
    }

    private void limpiarAccionVerificarPagoInformado(String actaId) {
        String actual = accionPendientePorActa.get(actaId);
        if (PrototipoStore.ACCION_VERIFICAR_PAGO_INFORMADO.equals(actual)) {
            accionPendientePorActa.remove(actaId);
        }
    }

    private void registrarEvento(
            String actaId,
            String tipoEvento,
            String bloqueOrigen,
            String bloqueDestino,
            String descripcion) {
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
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }
}

