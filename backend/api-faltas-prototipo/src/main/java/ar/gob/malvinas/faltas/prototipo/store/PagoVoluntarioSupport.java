package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.bandejaPermitePagoVoluntario;

/**
 * Soporte funcional del área presentaciones / pagos. Alcance del slice:
 * registrar la solicitud de pago voluntario originada por el infractor en
 * cualquier etapa interna operable del expediente.
 *
 * <p>Respaldo de dominio:
 * <ul>
 *   <li>{@code spec/03-bandejas/01-bandeja-labradas.md} admite "iniciar
 *       solicitud de pago voluntario" como acción inicial temprana;</li>
 *   <li>{@code spec/03-bandejas/02-bandeja-enriquecimiento.md} contempla
 *       como salida típica hacia "análisis / presentaciones / pagos" si
 *       surge una actuación que exige tratamiento material;</li>
 *   <li>{@code spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md}
 *       es la bandeja principal candidata para agrupar expedientes que
 *       solicitaron o registraron pago voluntario.</li>
 * </ul>
 *
 * <p>Decisión funcional (único circuito verdadero): el infractor siempre
 * puede pagar mientras el expediente esté en una bandeja interna
 * operable. La precondición se delega en
 * {@link PrototipoConstantes#bandejaPermitePagoVoluntario}, que excluye
 * {@code ARCHIVO}, {@code CERRADAS} y {@code GESTION_EXTERNA}. Tras
 * registrar la solicitud, el acta queda en {@code PENDIENTE_ANALISIS}
 * con marca operativa
 * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}, alineada con la
 * spec que centraliza la evaluación en análisis. No se modela cierre
 * automático por pago: la resolución posterior queda en análisis y puede
 * cerrarse con la acción genérica ya existente ({@link CierreSupport}).
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}).
 */
final class PagoVoluntarioSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa;

    PagoVoluntarioSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            Map<String, PrototipoStore.SituacionPagoMock> situacionPagoPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.situacionPagoPorActa = situacionPagoPorActa;
    }

    /**
     * Demo: solicitud de pago voluntario originada por el infractor en
     * cualquier bandeja interna operable (helper
     * {@link PrototipoConstantes#bandejaPermitePagoVoluntario}). Lleva al
     * acta a {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}, dentro del
     * bloque {@code D5_ANALISIS} y estado {@code PENDIENTE_REVISION}.
     * Genera evento {@code PAGO_VOLUNTARIO_SOLICITADO} con bloque origen
     * el bloque actual del acta y destino {@code D5_ANALISIS}.
     *
     * <p>Devuelve {@code CONFLICT} si el acta está cerrada, en bandeja
     * terminal/externa ({@code ARCHIVO}, {@code CERRADAS},
     * {@code GESTION_EXTERNA}) o si la situación de pago no es
     * {@code SIN_PAGO}: estos casos quedan también fuera de la lista
     * expuesta por {@link PrototipoStore#getAccionesPagoVoluntarioDisponibles}
     * para mantener simetría entre lista y handler. No cierra el
     * expediente: el cierre depende del análisis material posterior
     * (ver spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md).
     */
    PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado registrarSolicitudPagoVoluntario(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null);
        }
        if (!bandejaPermitePagoVoluntario(actual.estaCerrada(), actual.bandejaActual())) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null);
        }
        PrototipoStore.SituacionPagoMock situacionActual = situacionPagoPorActa.getOrDefault(
                actaId, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        if (situacionActual != PrototipoStore.SituacionPagoMock.SIN_PAGO) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null);
        }

        String bloqueOrigen = actual.bloqueActual();
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
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO);
        // Hecho informado: existe solicitud. No implica pago informado ni confirmación.
        situacionPagoPorActa.put(actaId, PrototipoStore.SituacionPagoMock.SOLICITADO);

        registrarEvento(
                actaId,
                "PAGO_VOLUNTARIO_SOLICITADO",
                bloqueOrigen,
                BLOQUE_D5,
                "Solicitud de pago voluntario registrada por el infractor; "
                        + "expediente pasa a análisis para evaluación.");

        return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_EVALUAR_PAGO_VOLUNTARIO);
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
