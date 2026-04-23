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

/**
 * Soporte funcional del área cierre del prototipo. Extraído de
 * {@link PrototipoStore} para cerrar la descompresión por área funcional
 * (archivo/reingreso, notificación, piezas/firma, cierre) sin cambiar
 * comportamiento observable: mismo endpoint, misma transición, mismo
 * evento, misma limpieza de marca operativa.
 *
 * <p>Concentra:
 * <ul>
 *   <li>cierre desde análisis ({@code cerrarActaDesdeAnalisis}),</li>
 *   <li>evento {@code CIERRE_ANALISIS} asociado a la transición,</li>
 *   <li>constantes locales exclusivas del área cierre
 *       ({@code BANDEJA_CERRADAS}, {@code BLOQUE_CERRADA}).</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}). No tiene estado propio: el cierre se
 * materializa íntegramente sobre esas estructuras compartidas.
 */
final class CierreSupport {

    private static final String BANDEJA_CERRADAS = "CERRADAS";
    private static final String BLOQUE_CERRADA = "CERRADA";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final CerrabilidadSupport cerrabilidad;

    CierreSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            CerrabilidadSupport cerrabilidad) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.cerrabilidad = cerrabilidad;
    }

    /**
     * Demo: cierre desde análisis → bandeja CERRADAS (solo desde
     * PENDIENTE_ANALISIS). Representa la conclusión administrativa del
     * análisis jurídico. Deja el acta con bloque/estado/situación
     * {@code CERRADA}, sin reingreso habilitado, y limpia la marca
     * operativa vigente en análisis (si hubiera). Genera un evento
     * {@code CIERRE_ANALISIS} con origen D5_ANALISIS y destino CERRADA.
     */
    PrototipoStore.CerrarActaResultado cerrarActaDesdeAnalisis(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.CerrarActaResultado(
                    PrototipoStore.CerrarActaEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new PrototipoStore.CerrarActaResultado(
                    PrototipoStore.CerrarActaEstado.CONFLICT, null, null, null);
        }
        if (!cerrabilidad.puedeCerrarDesdeAnalisis(actaId)) {
            return new PrototipoStore.CerrarActaResultado(
                    PrototipoStore.CerrarActaEstado.CONFLICT, null, null, null);
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

        registrarEvento(
                actaId,
                "CIERRE_ANALISIS",
                BLOQUE_D5,
                BLOQUE_CERRADA,
                "Análisis jurídico concluido; acta cerrada administrativamente.");

        accionPendientePorActa.remove(actaId);

        return new PrototipoStore.CerrarActaResultado(
                PrototipoStore.CerrarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
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
