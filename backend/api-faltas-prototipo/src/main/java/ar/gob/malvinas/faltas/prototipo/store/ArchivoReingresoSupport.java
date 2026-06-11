package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D2_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D4;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_FIRMADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_ENVIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_FALLO_CONDENATORIO;

/**
 * Soporte funcional del área archivo/reingreso del prototipo. Extraído de
 * {@link PrototipoStore} para bajar su tamaño y acoplamiento sin cambiar
 * comportamiento observable: mismos endpoints, mismas transiciones, mismos
 * eventos, mismos motivos de archivo.
 *
 * <p>Concentra:
 * <ul>
 *   <li>archivo directo desde análisis ({@code archivarActaDesdeAnalisis}),</li>
 *   <li>archivo posterior a evaluación de vencimiento ({@code archivarPorVencimiento}),</li>
 *   <li>reingreso explícito desde archivo ({@code reingresarActaDesdeArchivo}),</li>
 *   <li>motivo de archivo vigente y su exposición para DTOs/bandejas,</li>
 *   <li>eventos específicos de archivo/reingreso y marca operativa
 *       {@code REVISION_POST_REINGRESO} al volver a análisis.</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code accionPendientePorActa}) y mantiene como propio únicamente el mapa
 * de {@code motivoArchivo}, que pertenece semánticamente a esta área.
 */
final class ArchivoReingresoSupport {

    private static final String BANDEJA_ARCHIVO = "ARCHIVO";
    private static final String BLOQUE_ARCHIVO = "ARCHIVO";
    private static final String ESTADO_ARCHIVADA_OPERATIVA = "ARCHIVADA_OPERATIVA";
    private static final String SITUACION_ARCHIVO = "ARCHIVO";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa;

    /**
     * Motivo por el cual una acta quedó archivada dentro de la macro-bandeja
     * {@code ARCHIVO}. Específico del área archivo/reingreso: las actas que
     * no están archivadas, o que fueron archivadas antes de modelar esta
     * semántica, simplemente no aparecen en el mapa.
     */
    private final Map<String, String> motivoArchivoPorActa = new LinkedHashMap<>();

    ArchivoReingresoSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, String> accionPendientePorActa,
            Map<String, List<ActaDocumentoMock>> documentosPorActa) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.documentosPorActa = documentosPorActa;
    }

    String getMotivoArchivo(String actaId) {
        return motivoArchivoPorActa.get(actaId);
    }

    void setMotivoArchivo(String actaId, String motivo) {
        if (actaId == null || motivo == null) {
            return;
        }
        motivoArchivoPorActa.put(actaId, motivo);
    }

    void clear() {
        motivoArchivoPorActa.clear();
    }

    /**
     * Demo: archivo directo desde análisis → macro-bandeja ARCHIVO (solo
     * desde PENDIENTE_ANALISIS). Representa una decisión administrativa de
     * archivo tomada directamente en análisis, sin paso previo por evaluación
     * de vencimiento. Deja explícito el motivo
     * {@link PrototipoStore#MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO}, para que
     * el archivo no quede semánticamente mezclado con otros orígenes dentro
     * de la misma bandeja. No se crean bandejas nuevas.
     */
    PrototipoStore.ArchivarActaResultado archivarActaDesdeAnalisis(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ArchivarActaResultado(
                    PrototipoStore.ArchivarActaEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())) {
            return new PrototipoStore.ArchivarActaResultado(
                    PrototipoStore.ArchivarActaEstado.CONFLICT, null, null, null, null);
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

        registrarEvento(
                actaId,
                "ARCHIVADO_DESDE_ANALISIS_DIRECTO",
                BLOQUE_D5,
                BLOQUE_ARCHIVO,
                "Análisis jurídico archiva directamente el acta; pasa a archivo operativo con motivo "
                        + PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO + ".");

        accionPendientePorActa.remove(actaId);
        motivoArchivoPorActa.put(actaId, PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO);

        return new PrototipoStore.ArchivarActaResultado(
                PrototipoStore.ArchivarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.MOTIVO_ARCHIVO_DESDE_ANALISIS_DIRECTO);
    }

    /**
     * Demo: archivo posterior a evaluación de vencimiento → macro-bandeja
     * ARCHIVO. Solo aplica si la acta está en PENDIENTE_ANALISIS y tiene
     * {@code accionPendiente = EVALUAR_NOTIFICACION_VENCIDA}. Representa la
     * decisión humana de no reintentar la notificación vencida y cerrar
     * operativamente el caso por esa vía. Deja motivo explícito
     * {@link PrototipoStore#MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO} para
     * distinguirlo del archivo directo.
     */
    PrototipoStore.ArchivarPorVencimientoResultado archivarPorVencimiento(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ArchivarPorVencimientoResultado(
                    PrototipoStore.ArchivarPorVencimientoEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_ANALISIS.equals(actual.bandejaActual())
                || !PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA.equals(accionPendientePorActa.get(actaId))) {
            return new PrototipoStore.ArchivarPorVencimientoResultado(
                    PrototipoStore.ArchivarPorVencimientoEstado.CONFLICT, null, null, null, null);
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

        registrarEvento(
                actaId,
                "ARCHIVADO_POST_EVALUACION_VENCIMIENTO",
                BLOQUE_D5,
                BLOQUE_ARCHIVO,
                "Decisión posterior al vencimiento: el caso se archiva en lugar de reintentar la notificación; motivo "
                        + PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO + ".");

        accionPendientePorActa.remove(actaId);
        motivoArchivoPorActa.put(actaId, PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO);

        return new PrototipoStore.ArchivarPorVencimientoResultado(
                PrototipoStore.ArchivarPorVencimientoEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.MOTIVO_ARCHIVO_POST_EVALUACION_VENCIMIENTO);
    }

    /**
     * Demo: envía el acta desde enriquecimiento (D2/D1) a la bandeja
     * {@code PENDIENTE_NOTIFICACION} (D4). Solo aplica a actas cuya
     * {@code bandejaActual} sea {@code ACTAS_EN_ENRIQUECIMIENTO} con
     * {@code situacionAdministrativaActual = ACTIVA}. Los bloqueantes
     * materiales pendientes no impiden esta transición.
     */
    PrototipoStore.EnviarActaANotificacionResultado enviarActaANotificacion(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.EnviarActaANotificacionResultado(
                    PrototipoStore.EnviarActaANotificacionEstado.NOT_FOUND, null, null, null);
        }
        if (!BANDEJA_ACTAS_EN_ENRIQUECIMIENTO.equals(actual.bandejaActual())
                || !"ACTIVA".equals(actual.situacionAdministrativaActual())) {
            return new PrototipoStore.EnviarActaANotificacionResultado(
                    PrototipoStore.EnviarActaANotificacionEstado.CONFLICT, null, null, null);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D4,
                ESTADO_PENDIENTE_ENVIO,
                "ACTIVA",
                false,
                actual.permiteReingreso(),
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_NOTIFICACION);
        actas.put(actaId, actualizada);
        accionPendientePorActa.remove(actaId);

        registrarEvento(
                actaId,
                "ACTA_ENVIADA_A_NOTIFICACION",
                BLOQUE_D2_ENRIQUECIMIENTO,
                BLOQUE_D4,
                "Dirección envía el acta a notificación; D2_ENRIQUECIMIENTO -> D4_NOTIFICACION.");

        return new PrototipoStore.EnviarActaANotificacionResultado(
                PrototipoStore.EnviarActaANotificacionEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    /**
     * Demo: anula el acta y la archiva por nulidad desde enriquecimiento.
     * Solo aplica a actas cuya {@code bandejaActual} sea
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con
     * {@code situacionAdministrativaActual = ACTIVA}. El acta queda en
     * ARCHIVO con {@code motivoArchivo = NULIDAD} y
     * {@code permiteReingreso = true}: puede reingresarse si la decisión
     * fue incorrecta.
     */
    PrototipoStore.AnularActaPorNulidadResultado anularActaPorNulidad(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.AnularActaPorNulidadResultado(
                    PrototipoStore.AnularActaPorNulidadEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_ACTAS_EN_ENRIQUECIMIENTO.equals(actual.bandejaActual())
                || !"ACTIVA".equals(actual.situacionAdministrativaActual())) {
            return new PrototipoStore.AnularActaPorNulidadResultado(
                    PrototipoStore.AnularActaPorNulidadEstado.CONFLICT, null, null, null, null);
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
        accionPendientePorActa.remove(actaId);
        motivoArchivoPorActa.put(actaId, PrototipoStore.MOTIVO_ARCHIVO_NULIDAD);

        registrarEvento(
                actaId,
                "ACTA_ANULADA_POR_NULIDAD",
                BLOQUE_D2_ENRIQUECIMIENTO,
                BLOQUE_ARCHIVO,
                "Dirección anula el acta por nulidad; pasa a archivo operativo con motivo "
                        + PrototipoStore.MOTIVO_ARCHIVO_NULIDAD
                        + ". permiteReingreso=true.");

        return new PrototipoStore.AnularActaPorNulidadResultado(
                PrototipoStore.AnularActaPorNulidadEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                PrototipoStore.MOTIVO_ARCHIVO_NULIDAD);
    }

    /**
     * Demo: reingreso explícito desde la macro-bandeja ARCHIVO. Solo aplica
     * si la acta está en ARCHIVO y el modelo declara
     * {@code permiteReingreso = true}. Para actas archivadas por nulidad
     * ({@code motivoArchivo = NULIDAD}), el destino es
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con {@code COMPLETAR_ENRIQUECIMIENTO};
     * en el resto de casos el destino es PENDIENTE_ANALISIS con marca
     * operativa inferida del estado documental/eventos.
     *
     * <p>La acción pendiente post-reingreso (caso no-nulidad) se infiere del
     * estado documental y de los eventos previos al archivo, en lugar de
     * asignarse siempre como {@link PrototipoStore#ACCION_REVISION_POST_REINGRESO}.
     * Esto garantiza que el acta no quede trabada sin acciones operativas
     * cuando el estado material permite continuar el trámite:
     * <ul>
     *   <li>FALLO_CONDENATORIO:FIRMADO + último evento NOTIFICACION_NO_ENTREGADA
     *       → {@link PrototipoStore#ACCION_REINTENTAR_NOTIFICACION}.</li>
     *   <li>FALLO_CONDENATORIO:FIRMADO + último evento NOTIFICACION_VENCIDA
     *       → {@link PrototipoStore#ACCION_EVALUAR_NOTIFICACION_VENCIDA}.</li>
     *   <li>Cualquier otro caso
     *       → {@link PrototipoStore#ACCION_REVISION_POST_REINGRESO} (informativa).</li>
     * </ul>
     */
    PrototipoStore.ReingresarActaResultado reingresarActaDesdeArchivo(String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ReingresarActaResultado(
                    PrototipoStore.ReingresarActaEstado.NOT_FOUND, null, null, null, null, null);
        }
        if (!BANDEJA_ARCHIVO.equals(actual.bandejaActual()) || !actual.permiteReingreso()) {
            return new PrototipoStore.ReingresarActaResultado(
                    PrototipoStore.ReingresarActaEstado.CONFLICT, null, null, null, null, null);
        }

        String motivoArchivoPrevio = motivoArchivoPorActa.get(actaId);

        if (PrototipoStore.MOTIVO_ARCHIVO_NULIDAD.equals(motivoArchivoPrevio)) {
            return reingresarDesdeArchivoNulidad(actaId, actual, motivoArchivoPrevio);
        }

        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D5,
                ESTADO_PENDIENTE_REVISION,
                "ACTIVA",
                false,
                false,
                actual.tieneDocumentos(),
                actual.tieneNotificaciones(),
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
        actas.put(actaId, actualizada);

        String accionInferida = inferirAccionPendientePostReingreso(actaId);
        accionPendientePorActa.put(actaId, accionInferida);

        String descripcion = motivoArchivoPrevio == null
                ? "Reingreso desde archivo; acta vuelve a análisis con acción pendiente "
                        + accionInferida + "."
                : "Reingreso desde archivo (motivo previo " + motivoArchivoPrevio
                        + "); acta vuelve a análisis con acción pendiente "
                        + accionInferida + ".";
        registrarEvento(
                actaId,
                "ACTA_REINGRESADA_DESDE_ARCHIVO",
                BLOQUE_ARCHIVO,
                BLOQUE_D5,
                descripcion);

        return new PrototipoStore.ReingresarActaResultado(
                PrototipoStore.ReingresarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                accionInferida,
                motivoArchivoPrevio);
    }

    /**
     * Reingreso desde archivo por nulidad: devuelve el acta a
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con acción
     * {@code COMPLETAR_ENRIQUECIMIENTO}. El motivoArchivo se limpia del mapa
     * (queda en el log de eventos como trazabilidad). {@code permiteReingreso}
     * se mantiene {@code true}.
     */
    private PrototipoStore.ReingresarActaResultado reingresarDesdeArchivoNulidad(
            String actaId, ActaMock actual, String motivoArchivoPrevio) {
        ActaMock actualizada = new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                BLOQUE_D2_ENRIQUECIMIENTO,
                "EN_CURSO",
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
                BANDEJA_ACTAS_EN_ENRIQUECIMIENTO);
        actas.put(actaId, actualizada);

        String accion = PrototipoStore.ACCION_COMPLETAR_ENRIQUECIMIENTO;
        accionPendientePorActa.put(actaId, accion);
        motivoArchivoPorActa.remove(actaId);

        registrarEvento(
                actaId,
                "ACTA_REINGRESADA_DESDE_ARCHIVO_NULIDAD",
                BLOQUE_ARCHIVO,
                BLOQUE_D2_ENRIQUECIMIENTO,
                "Reingreso desde archivo por nulidad; acta vuelve a enriquecimiento con acción pendiente "
                        + accion + ".");

        return new PrototipoStore.ReingresarActaResultado(
                PrototipoStore.ReingresarActaEstado.OK,
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual(),
                accion,
                motivoArchivoPrevio);
    }

    /**
     * Infiere la acción pendiente operativa real al reingresar un acta desde
     * archivo. Garantiza que el expediente no quede trabado sin acciones
     * cuando el estado documental/eventos permite continuar el trámite.
     *
     * <p>Regla: si hay un fallo condenatorio firmado, el antecedente de
     * notificación determina la acción; si no hay fallo firmado o el estado
     * es otro, se devuelve {@link PrototipoStore#ACCION_REVISION_POST_REINGRESO}
     * como marca informativa no bloqueante.
     */
    private String inferirAccionPendientePostReingreso(String actaId) {
        if (!tieneDocumentoConTipoYEstado(actaId, TIPO_DOC_FALLO_CONDENATORIO, ESTADO_DOC_FIRMADO)) {
            return PrototipoStore.ACCION_REVISION_POST_REINGRESO;
        }
        String ultimoEvento = ultimoEventoNotificacionRelevante(actaId);
        if ("NOTIFICACION_VENCIDA".equals(ultimoEvento)) {
            return PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA;
        }
        if ("NOTIFICACION_NO_ENTREGADA".equals(ultimoEvento)) {
            return PrototipoStore.ACCION_REINTENTAR_NOTIFICACION;
        }
        return PrototipoStore.ACCION_REVISION_POST_REINGRESO;
    }

    private boolean tieneDocumentoConTipoYEstado(String actaId, String tipo, String estado) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null) {
            return false;
        }
        return docs.stream().anyMatch(d -> tipo.equals(d.tipoDocumento()) && estado.equals(d.estadoDocumento()));
    }

    /**
     * Devuelve el tipo del último evento de notificación relevante para
     * inferir el reintento post-reingreso, o {@code null} si no hay eventos
     * de ese tipo en el historial.
     */
    private String ultimoEventoNotificacionRelevante(String actaId) {
        List<ActaEventoMock> eventos = eventosPorActa.get(actaId);
        if (eventos == null || eventos.isEmpty()) {
            return null;
        }
        return eventos.stream()
                .filter(e -> "NOTIFICACION_NO_ENTREGADA".equals(e.tipoEvento())
                        || "NOTIFICACION_VENCIDA".equals(e.tipoEvento()))
                .max(Comparator.comparing(ActaEventoMock::fechaHora))
                .map(ActaEventoMock::tipoEvento)
                .orElse(null);
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
