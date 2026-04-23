package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaPiezasRequeridasMock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D4;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_EMITIDO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_ENVIO;

/**
 * Soporte funcional del área piezas/firma del prototipo. Extraído de
 * {@link PrototipoStore} para bajar su tamaño y acoplamiento sin cambiar
 * comportamiento observable: mismos endpoints, mismas transiciones, mismos
 * eventos, mismos nombres de archivo demo.
 *
 * <p>Concentra:
 * <ul>
 *   <li>consulta de piezas requeridas / generadas / pendientes y catálogo
 *       de documentos producidos,</li>
 *   <li>producción de la pieza {@code MEDIDA_PREVENTIVA}
 *       ({@code generarMedidaPreventiva}),</li>
 *   <li>producción de la pieza {@code NOTIFICACION_ACTA}
 *       ({@code generarNotificacionActa}),</li>
 *   <li>firma individual: el último documento en cola, si no es
 *       {@code NULIDAD}, pasa a {@code PENDIENTE_NOTIFICACION}; si es
 *       {@code NULIDAD}, a {@code CERRADAS} (invalidante; sin
 *       notificación) ({@code firmarDocumento}),</li>
 *   <li>eventos {@code MEDIDA_PREVENTIVA_GENERADA},
 *       {@code NOTIFICACION_ACTA_GENERADA}, {@code DOCUMENTO_FIRMADO} y
 *       {@code NULIDAD_FIRMADA} (última firma de nulidad).</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * del prototipo ({@code actas}, {@code eventosPorActa},
 * {@code documentosPorActa}, {@code piezasRequeridasPorActa}) y delega en
 * {@link NotificacionSupport} la materialización inicial del registro de
 * notificación cuando la firma del último documento cierra hacia
 * notificación (tronco no nulidad). Esa es la frontera con notificación.
 */
final class PiezasFirmaSupport {

    private static final String BANDEJA_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String BANDEJA_CERRADAS = "CERRADAS";
    private static final String BLOQUE_CERRADA = "CERRADA";
    private static final String BANDEJA_PENDIENTES_RESOLUCION_REDACCION = "PENDIENTES_RESOLUCION_REDACCION";
    private static final String PIEZA_MEDIDA_PREVENTIVA = "MEDIDA_PREVENTIVA";
    private static final String PIEZA_NOTIFICACION_ACTA = "NOTIFICACION_ACTA";
    private static final String PIEZA_NULIDAD = "NULIDAD";
    private static final String ESTADO_PENDIENTE_FIRMA_PIEZAS = "PENDIENTE_FIRMA_PIEZAS";
    private static final String ESTADO_PENDIENTE_PRODUCCION_PIEZAS = "PENDIENTE_PRODUCCION_PIEZAS";
    private static final String ESTADO_DOC_PENDIENTE_FIRMA = "PENDIENTE_FIRMA";
    private static final String ESTADO_DOC_FIRMADO = "FIRMADO";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaDocumentoMock>> documentosPorActa;
    private final Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa;
    private final NotificacionSupport notificacion;
    private final CerrabilidadSupport cerrabilidad;

    PiezasFirmaSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaDocumentoMock>> documentosPorActa,
            Map<String, ActaPiezasRequeridasMock> piezasRequeridasPorActa,
            NotificacionSupport notificacion,
            CerrabilidadSupport cerrabilidad) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.documentosPorActa = documentosPorActa;
        this.piezasRequeridasPorActa = piezasRequeridasPorActa;
        this.notificacion = notificacion;
        this.cerrabilidad = cerrabilidad;
    }

    // ---------------------------------------------------------------
    // Consultas de piezas y documentos
    // ---------------------------------------------------------------

    List<ActaDocumentoMock> listarDocumentosPorActa(String actaId) {
        List<ActaDocumentoMock> lista = documentosPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return List.of();
        }
        return List.copyOf(lista);
    }

    Optional<ActaPiezasRequeridasMock> findPiezasRequeridas(String actaId) {
        return Optional.ofNullable(piezasRequeridasPorActa.get(actaId));
    }

    List<String> listarPiezasRequeridas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return List.of();
        }
        return List.copyOf(p.piezasRequeridas());
    }

    List<String> listarPiezasGeneradas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasGeneradas() == null || p.piezasGeneradas().isEmpty()) {
            return List.of();
        }
        return List.copyOf(p.piezasGeneradas());
    }

    List<String> listarPiezasPendientes(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return List.of();
        }
        List<String> generadas = p.piezasGeneradas() == null ? List.of() : p.piezasGeneradas();
        return p.piezasRequeridas().stream()
                .filter(req -> !generadas.contains(req))
                .toList();
    }

    boolean todasLasPiezasProducidas(String actaId) {
        ActaPiezasRequeridasMock p = piezasRequeridasPorActa.get(actaId);
        if (p == null || p.piezasRequeridas() == null || p.piezasRequeridas().isEmpty()) {
            return false;
        }
        List<String> generadas = p.piezasGeneradas() == null ? List.of() : p.piezasGeneradas();
        return generadas.containsAll(p.piezasRequeridas());
    }

    // ---------------------------------------------------------------
    // Producción de piezas
    // ---------------------------------------------------------------

    /**
     * Demo: produce la pieza MEDIDA_PREVENTIVA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Si aún quedan otras piezas por producir, la acta
     * permanece en la misma bandeja; si no queda ninguna pieza pendiente,
     * pasa a PENDIENTE_FIRMA conservando el bloque actual.
     */
    PrototipoStore.GenerarMedidaPreventivaResultado generarMedidaPreventiva(String actaId) {
        ProducirPiezaResultado r = producirPieza(
                actaId,
                PIEZA_MEDIDA_PREVENTIVA,
                "medida_preventiva_",
                "MEDIDA_PREVENTIVA_GENERADA",
                "Medida preventiva producida; piezas completas, queda pendiente su firma.",
                "Medida preventiva producida; aún resta producir otras piezas requeridas.");
        return switch (r.estado()) {
            case OK -> {
                cerrabilidad.reconocerOrigenBloqueanteMedidaPreventiva(r.actaId());
                yield new PrototipoStore.GenerarMedidaPreventivaResultado(
                        PrototipoStore.GenerarMedidaPreventivaEstado.OK,
                        r.actaId(), r.bandejaActual(), r.estadoProcesoActual());
            }
            case NOT_FOUND -> new PrototipoStore.GenerarMedidaPreventivaResultado(
                    PrototipoStore.GenerarMedidaPreventivaEstado.NOT_FOUND, null, null, null);
            case CONFLICT -> new PrototipoStore.GenerarMedidaPreventivaResultado(
                    PrototipoStore.GenerarMedidaPreventivaEstado.CONFLICT, null, null, null);
        };
    }

    /**
     * Demo: produce la pieza NOTIFICACION_ACTA (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Si aún quedan otras piezas por producir, la acta
     * permanece en la misma bandeja; si no queda ninguna pieza pendiente,
     * pasa a PENDIENTE_FIRMA conservando el bloque actual y adoptando el
     * estado agregador PENDIENTE_FIRMA_PIEZAS.
     */
    PrototipoStore.GenerarNotificacionActaResultado generarNotificacionActa(String actaId) {
        ProducirPiezaResultado r = producirPieza(
                actaId,
                PIEZA_NOTIFICACION_ACTA,
                "notificacion_acta_",
                "NOTIFICACION_ACTA_GENERADA",
                "Notificación del acta producida; piezas completas, queda pendiente su firma.",
                "Notificación del acta producida; aún resta producir otras piezas requeridas.");
        return switch (r.estado()) {
            case OK -> new PrototipoStore.GenerarNotificacionActaResultado(
                    PrototipoStore.GenerarNotificacionActaEstado.OK,
                    r.actaId(), r.bandejaActual(), r.estadoProcesoActual());
            case NOT_FOUND -> new PrototipoStore.GenerarNotificacionActaResultado(
                    PrototipoStore.GenerarNotificacionActaEstado.NOT_FOUND, null, null, null);
            case CONFLICT -> new PrototipoStore.GenerarNotificacionActaResultado(
                    PrototipoStore.GenerarNotificacionActaEstado.CONFLICT, null, null, null);
        };
    }

    /**
     * Demo: produce la pieza NULIDAD (solo desde
     * PENDIENTES_RESOLUCION_REDACCION y solo si la acta declara esa pieza
     * como requerida). Nulidad se trata como una pieza no-fallo del
     * circuito documental/resolutivo: comparte el mismo agregador que las
     * otras piezas, por lo que si aún quedan otras piezas por producir la
     * acta permanece en la misma bandeja; si no queda ninguna pieza
     * pendiente, pasa a PENDIENTE_FIRMA conservando el bloque actual y
     * adoptando el estado agregador PENDIENTE_FIRMA_PIEZAS. No se modela
     * como bandeja terminal autónoma.
     */
    PrototipoStore.GenerarNulidadResultado generarNulidad(String actaId) {
        ProducirPiezaResultado r = producirPieza(
                actaId,
                PIEZA_NULIDAD,
                "nulidad_",
                "NULIDAD_GENERADA",
                "Nulidad producida; piezas completas, queda pendiente su firma.",
                "Nulidad producida; aún resta producir otras piezas requeridas.");
        return switch (r.estado()) {
            case OK -> new PrototipoStore.GenerarNulidadResultado(
                    PrototipoStore.GenerarNulidadEstado.OK,
                    r.actaId(), r.bandejaActual(), r.estadoProcesoActual());
            case NOT_FOUND -> new PrototipoStore.GenerarNulidadResultado(
                    PrototipoStore.GenerarNulidadEstado.NOT_FOUND, null, null, null);
            case CONFLICT -> new PrototipoStore.GenerarNulidadResultado(
                    PrototipoStore.GenerarNulidadEstado.CONFLICT, null, null, null);
        };
    }

    /**
     * Tronco común de producción de pieza: valida bandeja y requerimientos,
     * materializa el documento PDF demo, mueve a la bandeja correspondiente
     * según queden o no piezas pendientes y registra el evento de
     * generación con el mensaje que corresponde al caso parcial/completo.
     */
    private ProducirPiezaResultado producirPieza(
            String actaId,
            String pieza,
            String prefijoNombreArchivo,
            String tipoEvento,
            String mensajeEventoCompleto,
            String mensajeEventoParcial) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return ProducirPiezaResultado.notFound();
        }
        if (!BANDEJA_PENDIENTES_RESOLUCION_REDACCION.equals(actual.bandejaActual())) {
            return ProducirPiezaResultado.conflict();
        }

        ActaPiezasRequeridasMock piezas = piezasRequeridasPorActa.get(actaId);
        if (piezas == null
                || piezas.piezasRequeridas() == null
                || !piezas.piezasRequeridas().contains(pieza)) {
            return ProducirPiezaResultado.conflict();
        }
        List<String> generadasActuales = piezas.piezasGeneradas() == null
                ? new ArrayList<>()
                : new ArrayList<>(piezas.piezasGeneradas());
        if (generadasActuales.contains(pieza)) {
            return ProducirPiezaResultado.conflict();
        }

        generadasActuales.add(pieza);
        piezasRequeridasPorActa.put(actaId, new ActaPiezasRequeridasMock(
                actaId,
                piezas.piezasRequeridas(),
                generadasActuales));

        String sufijoActa = sufijoActa(actaId);

        if (PIEZA_MEDIDA_PREVENTIVA.equals(pieza)) {
            if (!reutilizarMedidaPreventivaConAnclaTempranaSiCorresponde(
                    actaId, prefijoNombreArchivo, sufijoActa)) {
                agregarDocumentoPendienteFirma(actaId, pieza, prefijoNombreArchivo, sufijoActa);
            }
        } else {
            agregarDocumentoPendienteFirma(actaId, pieza, prefijoNombreArchivo, sufijoActa);
        }

        boolean todasProducidas = piezas.piezasRequeridas().stream()
                .allMatch(generadasActuales::contains);

        String bloqueOrigen = actual.bloqueActual();
        ActaMock actualizada = moverTrasProducirPieza(actual, todasProducidas);
        actas.put(actaId, actualizada);

        registrarEvento(
                actaId,
                tipoEvento,
                bloqueOrigen,
                actualizada.bloqueActual(),
                todasProducidas ? mensajeEventoCompleto : mensajeEventoParcial);

        return ProducirPiezaResultado.ok(
                actualizada.id(),
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    // ---------------------------------------------------------------
    // Firma por documento
    // ---------------------------------------------------------------

    /**
     * Demo: firma individual de un documento puntual de la acta. Los
     * documentos tienen identidad propia y se firman de a uno. La acta sólo
     * abandona la bandeja PENDIENTE_FIRMA cuando todos los documentos
     * firmables pasan a estado FIRMADO. Si aún quedan documentos
     * pendientes, la acta permanece en PENDIENTE_FIRMA con estado
     * {@code PENDIENTE_FIRMA_PIEZAS} y su bloque actual; cuando se firma el
     * último, si su tipo es {@code NULIDAD} pasa a {@code CERRADAS} (salida
     * invalidante, sin notificación); si no, pasa a PENDIENTE_NOTIFICACION
     * con bloque D4 y estado PENDIENTE_ENVIO. En el tronco a notificación, y
     * sólo si la acta aún no tenía notificación, se pide al soporte de
     * notificación la primera notificación en {@code PENDIENTE_ENVIO}.
     */
    PrototipoStore.FirmarDocumentoResultado firmarDocumento(String actaId, String documentoId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.FirmarDocumentoResultado(
                    PrototipoStore.FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        if (!BANDEJA_PENDIENTE_FIRMA.equals(actual.bandejaActual())) {
            return new PrototipoStore.FirmarDocumentoResultado(
                    PrototipoStore.FirmarDocumentoEstado.CONFLICT, null, null, null, null);
        }

        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null || docs.isEmpty()) {
            return new PrototipoStore.FirmarDocumentoResultado(
                    PrototipoStore.FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        int indice = -1;
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).id().equals(documentoId)) {
                indice = i;
                break;
            }
        }
        if (indice < 0) {
            return new PrototipoStore.FirmarDocumentoResultado(
                    PrototipoStore.FirmarDocumentoEstado.NOT_FOUND, null, null, null, null);
        }
        ActaDocumentoMock doc = docs.get(indice);
        if (!ESTADO_DOC_PENDIENTE_FIRMA.equals(doc.estadoDocumento())) {
            return new PrototipoStore.FirmarDocumentoResultado(
                    PrototipoStore.FirmarDocumentoEstado.CONFLICT, null, null, null, null);
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
        ActaMock actualizada;
        String tipoEvento;
        String descripcionEvento;
        if (restanPendientes) {
            actualizada = mantenerEnFirma(actual);
            tipoEvento = "DOCUMENTO_FIRMADO";
            descripcionEvento =
                    "Documento " + documentoId + " firmado; aún quedan documentos pendientes de firma en la acta.";
        } else if (PIEZA_NULIDAD.equals(doc.tipoDocumento())) {
            actualizada = moverACerradaPorNulidadFirmada(actual);
            tipoEvento = "NULIDAD_FIRMADA";
            descripcionEvento =
                    "Documento " + documentoId + " firmado; nulidad concluye el expediente (cerrada, sin notificación).";
        } else {
            actualizada = moverAPendienteNotificacion(actual);
            tipoEvento = "DOCUMENTO_FIRMADO";
            descripcionEvento = "Documento " + documentoId + " firmado; último pendiente, acta pasa a notificación.";
        }
        actas.put(actaId, actualizada);

        registrarEvento(
                actaId,
                tipoEvento,
                bloqueOrigen,
                actualizada.bloqueActual(),
                descripcionEvento);

        if (!restanPendientes
                && !PIEZA_NULIDAD.equals(doc.tipoDocumento())
                && !actual.tieneNotificaciones()) {
            notificacion.asegurarNotificacionInicialPendiente(actaId, actual);
        }

        return new PrototipoStore.FirmarDocumentoResultado(
                PrototipoStore.FirmarDocumentoEstado.OK,
                actualizada.id(),
                documentoId,
                actualizada.bandejaActual(),
                actualizada.estadoProcesoActual());
    }

    // ---------------------------------------------------------------
    // Helpers internos
    // ---------------------------------------------------------------

    private ActaMock moverTrasProducirPieza(ActaMock actual, boolean todasProducidas) {
        String estadoDestino = todasProducidas ? ESTADO_PENDIENTE_FIRMA_PIEZAS : ESTADO_PENDIENTE_PRODUCCION_PIEZAS;
        String bandejaDestino = todasProducidas ? BANDEJA_PENDIENTE_FIRMA : BANDEJA_PENDIENTES_RESOLUCION_REDACCION;
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                actual.bloqueActual(),
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
    }

    private ActaMock mantenerEnFirma(ActaMock actual) {
        return new ActaMock(
                actual.id(),
                actual.numeroActa(),
                actual.dominioReferencia(),
                actual.bloqueActual(),
                ESTADO_PENDIENTE_FIRMA_PIEZAS,
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
                BANDEJA_PENDIENTE_FIRMA);
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

    /**
     * Coherente con cierre definitivo: bloque/estado/situación
     * {@code CERRADA} y sin reingreso. No inicia notificación; la
     * invalidación nace del documento de nulidad firmado.
     */
    private ActaMock moverACerradaPorNulidadFirmada(ActaMock actual) {
        return new ActaMock(
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
    }

    /**
     * {@code true} si ya existía ancla de medida preventiva en
     * {@code EMITIDO} (constatación D1/D2) y se la promueve a
     * {@code PENDIENTE_FIRMA} en lugar de duplicar el tipo en expediente.
     */
    private boolean reutilizarMedidaPreventivaConAnclaTempranaSiCorresponde(
            String actaId, String prefijoNombreArchivo, String sufijoActa) {
        List<ActaDocumentoMock> docs = documentosPorActa.get(actaId);
        if (docs == null) {
            return false;
        }
        String nombre = prefijoNombreArchivo + sufijoActa + ".pdf";
        for (int i = 0; i < docs.size(); i++) {
            ActaDocumentoMock d = docs.get(i);
            if (PIEZA_MEDIDA_PREVENTIVA.equals(d.tipoDocumento())
                    && ESTADO_DOC_EMITIDO.equals(d.estadoDocumento())) {
                docs.set(
                        i,
                        new ActaDocumentoMock(
                                d.id(), actaId, d.tipoDocumento(), ESTADO_DOC_PENDIENTE_FIRMA, nombre));
                return true;
            }
        }
        return false;
    }

    private void agregarDocumentoPendienteFirma(
            String actaId,
            String tipoPieza,
            String prefijoNombreArchivo,
            String sufijoActa) {
        List<ActaDocumentoMock> docs = documentosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        int siguienteDoc = docs.size() + 1;
        String idDoc = "DOC-" + sufijoActa + "-" + String.format("%02d", siguienteDoc);
        docs.add(new ActaDocumentoMock(
                idDoc,
                actaId,
                tipoPieza,
                ESTADO_DOC_PENDIENTE_FIRMA,
                prefijoNombreArchivo + sufijoActa + ".pdf"));
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

    /**
     * Intermediario interno común a los dos endpoints {@code generar*}:
     * expresa el resultado del tronco en términos neutros, sin forzar a
     * usar un enum público específico de una de las dos piezas. Cada
     * entrada pública mapea este resultado a su propio
     * {@code Estado}/{@code Resultado}.
     */
    private record ProducirPiezaResultado(
            Estado estado,
            String actaId,
            String bandejaActual,
            String estadoProcesoActual) {

        enum Estado { OK, NOT_FOUND, CONFLICT }

        static ProducirPiezaResultado ok(String actaId, String bandeja, String estado) {
            return new ProducirPiezaResultado(Estado.OK, actaId, bandeja, estado);
        }

        static ProducirPiezaResultado notFound() {
            return new ProducirPiezaResultado(Estado.NOT_FOUND, null, null, null);
        }

        static ProducirPiezaResultado conflict() {
            return new ProducirPiezaResultado(Estado.CONFLICT, null, null, null);
        }
    }
}
