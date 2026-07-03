package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ARCHIVO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_CERRADAS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PARALIZADAS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;

/**
 * Soporte funcional del área paralización/reactivación del prototipo. Extraído de
 * {@link PrototipoStore} para bajar su tamaño y acoplamiento sin cambiar
 * comportamiento observable: mismos endpoints, mismas transiciones, mismos
 * eventos, mismo manejo de observación y mismas marcas operativas.
 *
 * <p>Concentra:
 * <ul>
 *   <li>paralización administrativa transversal desde cualquier bandeja
 *       interna operativa activa ({@code paralizarActa}),</li>
 *   <li>reactivación explícita desde la macro-bandeja {@code PARALIZADAS}
 *       ({@code reactivarActa}),</li>
 *   <li>predicado de elegibilidad ({@code puedeParalizarActa}),</li>
 *   <li>eventos específicos de paralización/reactivación y registro de
 *       observación de paralización.</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}, {@code observacionParalizacionPorActa})
 * y opera directamente sobre ellas.
 */
final class ParalizacionReactivacionSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, String> observacionParalizacionPorActa;

    ParalizacionReactivacionSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            Map<String, String> observacionParalizacionPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.observacionParalizacionPorActa = observacionParalizacionPorActa;
    }

    boolean puedeParalizarActa(String actaId) {
        return puedeParalizarActa(actas.get(actaId));
    }

    /**
     * Demo: paralización administrativa transversal desde cualquier bandeja
     * interna operativa activa. Conserva expediente, pagos, montos y resultado
     * de fondo; sólo cambia la proyección operativa agregadora y deja el motivo
     * como acción pendiente trazable.
     */
    PrototipoStore.ParalizarActaResultado paralizarActa(
            String actaId, PrototipoStore.MotivoParalizacionActa motivo, String observacion) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ParalizarActaResultado(
                    PrototipoStore.ParalizarActaEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!puedeParalizarActa(actual)) {
            return new PrototipoStore.ParalizarActaResultado(
                    PrototipoStore.ParalizarActaEstado.CONFLICT, null, null, null, null, null);
        }

        PrototipoStore.MotivoParalizacionActa motivoVigente =
                motivo != null ? motivo : PrototipoStore.MotivoParalizacionActa.ESPERA_DOCUMENTAL;
        String accionPendiente = accionPendienteParalizacion(motivoVigente);
        String bloqueAnterior = actual.bloqueActual();

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                actual.bloqueActual(),
                "PARALIZADA",
                "PARALIZADA",
                actual.estaCerrada(),
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PARALIZADAS);
        actas.put(actaId, actualizada);
        accionPendientePorActa.put(actaId, accionPendiente);
        String observacionNormalizada = (observacion == null || observacion.isBlank())
                ? null
                : observacion.trim();
        if (observacionNormalizada != null) {
            observacionParalizacionPorActa.put(actaId, observacionNormalizada);
        } else {
            observacionParalizacionPorActa.remove(actaId);
        }

        registrarEventoParalizacion(actaId, bloqueAnterior, accionPendiente, observacion);

        return new PrototipoStore.ParalizarActaResultado(
                PrototipoStore.ParalizarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                actualizada.situacionAdministrativaActual(),
                accionPendiente);
    }

    /**
     * Demo: reactivación explícita desde la macro-bandeja PARALIZADAS →
     * vuelve a PENDIENTE_ANALISIS con bloque {@code ANAL}, estado
     * PENDIENTE_REVISION, situación ACTIVA y marca operativa
     * {@link PrototipoStore#ACCION_REVISION_POST_REACTIVACION}. La información
     * histórica de la paralización queda en el log de eventos; no bloquea
     * acciones luego de reactivar. Solo aplica a actas cuya bandejaActual sea
     * {@code PARALIZADAS}.
     */
    PrototipoStore.ReactivarActaResultado reactivarActa(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReactivarActaResultado(
                    PrototipoStore.ReactivarActaEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_PARALIZADAS.equals(actual.bandejaActual())) {
            return new PrototipoStore.ReactivarActaResultado(
                    PrototipoStore.ReactivarActaEstado.CONFLICT, null, null, null, null);
        }

        String accionParalizacionPrevia = accionPendientePorActa.get(actaId);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
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

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_REACTIVACION);

        String descripcion = accionParalizacionPrevia == null
                ? "Acta reactivada desde PARALIZADAS; vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_REACTIVACION + "."
                : "Acta reactivada desde PARALIZADAS (motivo previo " + accionParalizacionPrevia
                        + "); vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_REACTIVACION + ".";

        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .max(Comparator.comparing(ActaEventoMock::fechaHora))
                .map(e -> e.fechaHora().plusMinutes(1))
                .orElse(LocalDateTime.now());
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "ACTA_REACTIVADA_DESDE_PARALIZADAS",
                BANDEJA_PARALIZADAS,
                BLOQUE_D5,
                descripcion));

        return new PrototipoStore.ReactivarActaResultado(
                PrototipoStore.ReactivarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_REACTIVACION);
    }

    private boolean puedeParalizarActa(ActaMock acta) {
        if (acta == null || acta.estaCerrada()) {
            return false;
        }
        if (!"ACTIVA".equals(acta.situacionAdministrativaActual())) {
            return false;
        }
        String bandeja = acta.bandejaActual();
        return !BANDEJA_CERRADAS.equals(bandeja)
                && !BANDEJA_ARCHIVO.equals(bandeja)
                && !BANDEJA_GESTION_EXTERNA.equals(bandeja)
                && !BANDEJA_PARALIZADAS.equals(bandeja);
    }

    private String accionPendienteParalizacion(PrototipoStore.MotivoParalizacionActa motivo) {
        return switch (motivo) {
            case ESPERA_DOCUMENTAL -> PrototipoStore.ACCION_PARALIZACION_ESPERA_DOCUMENTAL;
            case ESPERA_INFORME_EXTERNO -> PrototipoStore.ACCION_PARALIZACION_ESPERA_INFORME_EXTERNO;
            case ESPERA_OTRA_DEPENDENCIA -> PrototipoStore.ACCION_PARALIZACION_ESPERA_OTRA_DEPENDENCIA;
            case ESPERA_RESOLUCION_RELACIONADA -> PrototipoStore.ACCION_PARALIZACION_ESPERA_RESOLUCION_RELACIONADA;
            case OTRO -> PrototipoStore.ACCION_PARALIZACION_OTRO;
        };
    }

    private void registrarEventoParalizacion(
            String actaId, String bloqueAnterior, String accionPendiente, String observacion) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        int siguiente = eventos.size() + 1;
        String idEvento = "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente);
        LocalDateTime fechaEvento = eventos.stream()
                .max(Comparator.comparing(ActaEventoMock::fechaHora))
                .map(e -> e.fechaHora().plusMinutes(1))
                .orElse(LocalDateTime.now());
        String detalleObservacion = observacion == null || observacion.isBlank()
                ? ""
                : " Observacion: " + observacion.trim();
        eventos.add(new ActaEventoMock(
                idEvento,
                actaId,
                fechaEvento,
                "PARALIZACION",
                bloqueAnterior,
                BANDEJA_PARALIZADAS,
                "Acta paralizada; accionPendiente " + accionPendiente + "." + detalleObservacion));
    }
}
