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

/**
 * Soporte funcional del área presentaciones / pagos. Alcance mínimo del
 * slice: registrar la solicitud de pago voluntario temprano, es decir, una
 * solicitud que se origina antes de la etapa formal de análisis.
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
 * <p>Circuito implementado (único verdadero): acta en
 * {@code ACTAS_EN_ENRIQUECIMIENTO} + acción
 * {@code registrar-solicitud-pago-voluntario} → acta pasa a
 * {@code PENDIENTE_ANALISIS} con marca operativa
 * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}. No se modela
 * todavía cierre por pago cumplido: la resolución posterior queda en
 * análisis y puede cerrarse con la acción genérica ya existente
 * ({@link CierreSupport}). Esto es deliberado: la spec habilita el
 * origen temprano de la solicitud pero exige evaluación en análisis
 * antes del cierre.
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}).
 */
final class PagoVoluntarioSupport {

    private static final String BANDEJA_ACTAS_EN_ENRIQUECIMIENTO = "ACTAS_EN_ENRIQUECIMIENTO";
    private static final String BLOQUE_D2_ENRIQUECIMIENTO = "D2_ENRIQUECIMIENTO";

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
     * Demo: solicitud de pago voluntario originada en etapa temprana del
     * expediente (solo desde {@code ACTAS_EN_ENRIQUECIMIENTO}). Lleva al
     * acta a {@code PENDIENTE_ANALISIS} con marca operativa
     * {@link PrototipoStore#ACCION_EVALUAR_PAGO_VOLUNTARIO}, dentro del
     * bloque {@code D5_ANALISIS} y estado {@code PENDIENTE_REVISION}.
     * Genera evento {@code PAGO_VOLUNTARIO_SOLICITADO} con origen
     * {@code D2_ENRIQUECIMIENTO} y destino {@code D5_ANALISIS}. No cierra
     * el expediente: el cierre depende del análisis material posterior
     * (ver spec/03-bandejas/03-bandeja-analisis-presentaciones-pagos.md).
     */
    PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado registrarSolicitudPagoVoluntario(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND,
                    null, null, null, null);
        }
        if (!BANDEJA_ACTAS_EN_ENRIQUECIMIENTO.equals(actual.bandejaActual())) {
            return new PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado(
                    PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT,
                    null, null, null, null);
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
                BLOQUE_D2_ENRIQUECIMIENTO,
                BLOQUE_D5,
                "Solicitud de pago voluntario registrada en etapa temprana; "
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
