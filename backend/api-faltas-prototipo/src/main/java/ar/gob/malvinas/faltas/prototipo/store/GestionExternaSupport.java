package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_EN_GESTION_EXTERNA;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;

/**
 * Soporte funcional del área gestión externa del prototipo. Concentra la
 * derivación efectiva de expedientes desde análisis hacia la macro-bandeja
 * {@code GESTION_EXTERNA}, consumiendo la marca operativa
 * {@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA} que el caso ya trae
 * por haber atravesado fallo + notificación de fallo + ventana de espera
 * posterior cumplida sin novedad.
 *
 * <p>Alcance funcional:
 * <ul>
 *   <li>transición efectiva análisis → gestión externa, parametrizada por
 *       tipo vigente ({@link PrototipoStore#TIPO_GESTION_EXTERNA_APREMIO} o
 *       {@link PrototipoStore#TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ});</li>
 *   <li>retorno efectivo gestión externa → análisis, único destino en este
 *       slice: {@code PENDIENTE_ANALISIS} con marca operativa
 *       {@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA},
 *       preservando el {@code tipoGestionExterna} original como
 *       trazabilidad sintética;</li>
 *   <li>sin integración con sistemas externos;</li>
 *   <li>sin subestados internos ricos dentro de la nueva bandeja;</li>
 *   <li>sin políticas de retorno diferenciadas por tipo todavía.</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}) y mantiene como propio únicamente el mapa
 * de {@code tipoGestionExterna}, que pertenece semánticamente a esta área.
 */
final class GestionExternaSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;

    /**
     * Tipo de gestión externa vigente para la acta dentro de la macro-bandeja
     * {@code GESTION_EXTERNA}. Específico del área: las actas que no fueron
     * derivadas simplemente no aparecen en el mapa.
     */
    private final Map<String, String> tipoGestionExternaPorActa = new LinkedHashMap<>();

    GestionExternaSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
    }

    String getTipoGestionExterna(String actaId) {
        return tipoGestionExternaPorActa.get(actaId);
    }

    /**
     * Helper interno de mocks: asigna el tipo de gestión externa vigente para
     * una acta ya precargada en la macro-bandeja {@code GESTION_EXTERNA} sin
     * ejecutar la transición. Uso restringido a la fábrica de datos demo,
     * para poder representar desde el reset casos ya derivados (por
     * ejemplo, listos para el futuro retorno efectivo).
     */
    void setTipoGestionExterna(String actaId, String tipo) {
        if (actaId == null || tipo == null) {
            return;
        }
        tipoGestionExternaPorActa.put(actaId, tipo);
    }

    void clear() {
        tipoGestionExternaPorActa.clear();
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link PrototipoStore#TIPO_GESTION_EXTERNA_APREMIO}. Delega en el
     * helper compartido; misma precondición y mismo efecto observable que
     * {@link #derivarAJuzgadoDePaz(String)} salvo por el tipo asignado.
     */
    PrototipoStore.DerivarAGestionExternaResultado derivarAApremio(String actaId) {
        return derivar(actaId, PrototipoStore.TIPO_GESTION_EXTERNA_APREMIO);
    }

    /**
     * Demo: derivación efectiva a gestión externa con tipo
     * {@link PrototipoStore#TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ}. Delega en
     * el helper compartido; misma precondición y mismo efecto observable
     * que {@link #derivarAApremio(String)} salvo por el tipo asignado.
     */
    PrototipoStore.DerivarAGestionExternaResultado derivarAJuzgadoDePaz(String actaId) {
        return derivar(actaId, PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ);
    }

    /**
     * Transición compartida por las derivaciones tipadas a gestión externa.
     * Solo aplica si la acta está en PENDIENTE_ANALISIS y trae una marca
     * operativa válida como disparador de derivación efectiva:
     * <ul>
     *   <li>{@link PrototipoStore#ACCION_DERIVAR_GESTION_EXTERNA}: primera
     *       derivación, casos post fallo + notificación de fallo + ventana
     *       de espera posterior cumplida sin novedad;</li>
     *   <li>{@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA}:
     *       re-derivación de un expediente que ya volvió desde gestión
     *       externa y el análisis decide mandarlo otra vez. La posibilidad
     *       de re-derivar no se consume por ida y vuelta: mientras el caso
     *       no entre en una salida terminal (nulidad, cierre, archivo
     *       efectivo), cada retorno habilita una nueva derivación.</li>
     * </ul>
     * Deja el acta con bloque/estado/situación {@code GESTION_EXTERNA},
     * reingreso habilitado para habilitar el retorno efectivo, y limpia la
     * marca operativa previa. Genera un evento
     * {@code DERIVACION_GESTION_EXTERNA} con origen D5_ANALISIS y destino
     * GESTION_EXTERNA, explicitando el tipo asignado y, si corresponde, el
     * tipo previo preservado como trazabilidad de la gestión externa de la
     * que venía el expediente.
     */
    private PrototipoStore.DerivarAGestionExternaResultado derivar(String actaId, String tipo) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND, null, null, null, null);
        }
        String marcaActual = accionPendientePorActa.get(actaId);
        boolean marcaHabilitaDerivacion =
                PrototipoStore.ACCION_DERIVAR_GESTION_EXTERNA.equals(marcaActual)
                        || PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA.equals(marcaActual);
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual()) || !marcaHabilitaDerivacion) {
            return new PrototipoStore.DerivarAGestionExternaResultado(
                    PrototipoStore.DerivarAGestionExternaEstado.CONFLICT, null, null, null, null);
        }

        boolean esRederivacion =
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA.equals(marcaActual);
        String tipoPrevio = tipoGestionExternaPorActa.get(actaId);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_GESTION_EXTERNA,
                ESTADO_EN_GESTION_EXTERNA,
                BLOQUE_GESTION_EXTERNA,
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_GESTION_EXTERNA);
        actas.put(actaId, actualizada);

        String descripcion;
        if (esRederivacion && tipoPrevio != null) {
            descripcion = "Análisis jurídico re-deriva el caso a gestión externa (tipo previo "
                    + tipoPrevio + ", nuevo tipo " + tipo + ").";
        } else if (esRederivacion) {
            descripcion = "Análisis jurídico re-deriva el caso a gestión externa; tipo " + tipo + ".";
        } else {
            descripcion = "Análisis jurídico deriva efectivamente el caso a gestión externa; tipo "
                    + tipo + ".";
        }
        registrarEvento(
                actaId,
                "DERIVACION_GESTION_EXTERNA",
                BLOQUE_D5,
                BLOQUE_GESTION_EXTERNA,
                descripcion);

        accionPendientePorActa.remove(actaId);
        tipoGestionExternaPorActa.put(actaId, tipo);

        return new PrototipoStore.DerivarAGestionExternaResultado(
                PrototipoStore.DerivarAGestionExternaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                tipo);
    }

    /**
     * Demo: retorno efectivo desde la macro-bandeja {@code GESTION_EXTERNA}
     * al circuito operativo. Solo aplica si la acta está en
     * {@code GESTION_EXTERNA} y el modelo declara
     * {@code permiteReingreso = true}. Destino único en este slice:
     * {@code PENDIENTE_ANALISIS} con bloque {@code D5_ANALISIS}, estado
     * {@code PENDIENTE_REVISION} y situación {@code ACTIVA}; el reingreso
     * no se consume: {@code permiteReingreso} se preserva en {@code true}
     * porque la bandera representa la naturaleza reingresable del
     * expediente frente a gestión externa, no un uso único del retorno.
     * La marca operativa pasa a
     * {@link PrototipoStore#ACCION_REVISION_POST_GESTION_EXTERNA}. El
     * {@code tipoGestionExterna} original se preserva intacto para que el
     * caso retorne, pero siga siendo visible de qué tipo de gestión externa
     * venía. Genera un evento
     * {@code ACTA_REINGRESADA_DESDE_GESTION_EXTERNA} con origen
     * {@code GESTION_EXTERNA} y destino {@code D5_ANALISIS}, explicitando
     * el tipo previo como trazabilidad.
     */
    PrototipoStore.ReingresarDesdeGestionExternaResultado reingresarActaDesdeGestionExterna(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                    PrototipoStore.ReingresarDesdeGestionExternaEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_GESTION_EXTERNA.equals(actual.bandejaActual()) || !actual.permiteReingreso()) {
            return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                    PrototipoStore.ReingresarDesdeGestionExternaEstado.CONFLICT, null, null, null, null, null);
        }

        String tipoPrevio = tipoGestionExternaPorActa.get(actaId);

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                true,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        accionPendientePorActa.put(actaId, PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA);

        String descripcion = tipoPrevio == null
                ? "Retorno desde gestión externa; acta vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA + "."
                : "Retorno desde gestión externa (tipo previo " + tipoPrevio
                        + "); acta vuelve a análisis con acción pendiente "
                        + PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA + ".";
        registrarEvento(
                actaId,
                "ACTA_REINGRESADA_DESDE_GESTION_EXTERNA",
                BLOQUE_GESTION_EXTERNA,
                BLOQUE_D5,
                descripcion);

        return new PrototipoStore.ReingresarDesdeGestionExternaResultado(
                PrototipoStore.ReingresarDesdeGestionExternaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.ACCION_REVISION_POST_GESTION_EXTERNA,
                tipoPrevio);
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
