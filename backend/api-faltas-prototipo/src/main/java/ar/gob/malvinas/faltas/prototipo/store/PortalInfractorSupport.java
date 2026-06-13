package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_ACTAS_EN_ENRIQUECIMIENTO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_EN_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_DOC_FIRMADO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_FALLO_ABSOLUTORIO;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.TIPO_DOC_FALLO_CONDENATORIO;

/**
 * Soporte funcional del área portal infractor del prototipo. Extraído de
 * {@link PrototipoStore} para bajar su tamaño y acoplamiento sin cambiar
 * comportamiento observable: mismas rutas, mismos estados visibles al
 * ciudadano, mismos eventos, misma idempotencia.
 *
 * <p>Concentra:
 * <ul>
 *   <li>predicado de acta en revisión para portal ({@code actaEnRevisionParaPortal}),</li>
 *   <li>búsqueda de notificación pendiente por portal
 *       ({@code findNotificacionPortalPendiente}),</li>
 *   <li>listado de documentos visibles para el infractor
 *       ({@code listarDocumentosVisiblesPortal}),</li>
 *   <li>apertura/visualización de documento notificable
 *       ({@code verDocumentoPortal}),</li>
 *   <li>confirmación de visualización de notificación por portal
 *       ({@code confirmarVisualizacionNotificacionPortal}),</li>
 *   <li>todos los helpers privados de portal: registro de notificación positiva
 *       por visualización, marcado de notificaciones alternativas superadas,
 *       eventos de auditoría, reemplazo de notificación en lista y transición
 *       de acta a análisis por visualización.</li>
 * </ul>
 *
 * <p>No duplica estado: recibe por referencia las estructuras compartidas
 * ({@code actas}, {@code eventosPorActa}, {@code notificacionesPorActa},
 * {@code accionPendientePorActa}) y los supports de área necesarios
 * ({@code piezasFirma}, {@code falloPlazoApelacion}).
 */
final class PortalInfractorSupport {

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final PiezasFirmaSupport piezasFirma;
    private final FalloPlazoApelacionSupport falloPlazoApelacion;

    PortalInfractorSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            Map<String, String> accionPendientePorActa,
            PiezasFirmaSupport piezasFirma,
            FalloPlazoApelacionSupport falloPlazoApelacion) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.piezasFirma = piezasFirma;
        this.falloPlazoApelacion = falloPlazoApelacion;
    }

    Optional<ActaNotificacionMock> findNotificacionPortalPendiente(String actaId) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actaId);
        if (lista == null || lista.isEmpty()) {
            return Optional.empty();
        }
        return lista.stream()
                .filter(this::esNotificacionPortalPendiente)
                .sorted(Comparator.comparingInt((ActaNotificacionMock n) -> prioridadNotificacionPortal(n.tipo()))
                        .thenComparing(ActaNotificacionMock::id))
                .findFirst();
    }

    PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado confirmarVisualizacionNotificacionPortal(
            String actaId) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                    PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.NOT_FOUND,
                    null, null, null, null);
        }
        Optional<ActaNotificacionMock> pendiente = findNotificacionPortalPendiente(actaId);
        if (pendiente.isEmpty()) {
            return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                    PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.SIN_NOTIFICACION_PENDIENTE,
                    actaId,
                    actual.bandejaActual(),
                    actual.estadoProcesoActual(),
                    null);
        }

        ActaNotificacionMock notificacionPortal = pendiente.get();
        if (notificacionPortal.tipo() == TipoNotificacion.ACTA_INFRACCION) {
            if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(actual.bandejaActual())
                    && !BANDEJA_EN_NOTIFICACION.equals(actual.bandejaActual())) {
                return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                        PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.CONFLICT,
                        actaId,
                        actual.bandejaActual(),
                        actual.estadoProcesoActual(),
                        null);
            }
            ActaMock actualizada = moverAAnalisisPorVisualizacionPortal(actual);
            actas.put(actaId, actualizada);
            accionPendientePorActa.remove(actaId);
        } else {
            PrototipoStore.RegistrarNotificacionPositivaResultado juridico =
                    falloPlazoApelacion.registrarNotificacionPositivaDeFalloTipificada(
                            actaId, notificacionPortal.tipo());
            if (juridico.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND) {
                return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                        PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.NOT_FOUND,
                        null, null, null, null);
            }
            if (juridico.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT) {
                return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                        PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.CONFLICT,
                        actaId,
                        actual.bandejaActual(),
                        actual.estadoProcesoActual(),
                        null);
            }
        }

        LocalDateTime fechaResultado = LocalDateTime.now();
        ActaNotificacionMock actualizada = notificacionPortal.conVisualizacionPortal(
                fechaResultado, "Visualización confirmada desde portal infractor");
        reemplazarNotificacion(actualizada);
        ActaMock actaActualizada = actas.get(actaId);
        registrarEventoVisualizacionPortal(actaId, notificacionPortal.tipo());
        marcarNotificacionesAlternativasSuperadasPorPortal(actaId, notificacionPortal.tipo());

        return new PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado(
                PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.OK,
                actaId,
                actaActualizada != null ? actaActualizada.bandejaActual() : actual.bandejaActual(),
                actaActualizada != null ? actaActualizada.estadoProcesoActual() : actual.estadoProcesoActual(),
                actualizada);
    }

    /**
     * Regla de portal: acta ingresada pero todavía no validada como
     * notificable (D1/D2, bandeja {@code ACTAS_EN_ENRIQUECIMIENTO}). En ese
     * estado el portal solo reconoce el número de acta y muestra el mensaje
     * de revisión; no expone detalle, documentos ni acciones operativas.
     */
    boolean actaEnRevisionParaPortal(String actaId) {
        ActaMock a = actas.get(actaId);
        return a != null && BANDEJA_ACTAS_EN_ENRIQUECIMIENTO.equals(a.bandejaActual());
    }

    /**
     * Documentos del expediente formalmente visibles para el infractor:
     * firmados y en etapa de notificación (bandeja
     * {@code PENDIENTE_NOTIFICACION}/{@code EN_NOTIFICACION}) o ya
     * notificados positivamente. Si el acta está en revisión (D1/D2) no se
     * expone ningún documento.
     */
    List<PrototipoStore.DocumentoPortalVista> listarDocumentosVisiblesPortal(String actaId) {
        ActaMock acta = actas.get(actaId);
        if (acta == null || actaEnRevisionParaPortal(actaId)) {
            return List.of();
        }
        List<ActaDocumentoMock> docs = piezasFirma.listarDocumentosPorActa(actaId);
        if (docs.isEmpty()) {
            return List.of();
        }
        boolean enEtapaNotificacion = BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())
                || BANDEJA_EN_NOTIFICACION.equals(acta.bandejaActual());
        List<PrototipoStore.DocumentoPortalVista> visibles = new ArrayList<>();
        for (ActaDocumentoMock d : docs) {
            TipoNotificacion tipo = tipoNotificacionDeDocumentoPortal(d.tipoDocumento());
            if (tipo == null || !ESTADO_DOC_FIRMADO.equals(d.estadoDocumento())) {
                continue;
            }
            boolean notificado = tipoNotificadoPositivamentePortal(actaId, tipo);
            boolean pendiente = !notificado && enEtapaNotificacion;
            if (!notificado && !pendiente) {
                continue;
            }
            visibles.add(new PrototipoStore.DocumentoPortalVista(
                    d.tipoDocumento(),
                    tituloDocumentoPortal(d.tipoDocumento()),
                    d.estadoDocumento(),
                    notificado,
                    pendiente));
        }
        return visibles;
    }

    /**
     * Apertura/visualización de un documento notificable desde el portal.
     * Si el documento estaba pendiente de notificación, registra
     * notificación positiva por canal {@code PORTAL_INFRACTOR} y aplica los
     * efectos jurídicos correspondientes (acta inicial → análisis; fallo →
     * {@code resultadoFinal} y plazo de apelación). Idempotente: si el
     * documento ya estaba notificado devuelve {@code OK_YA_NOTIFICADO} sin
     * efectos.
     */
    PrototipoStore.VerDocumentoPortalResultado verDocumentoPortal(String actaId, String tipoDocumento) {
        ActaMock actual = actas.get(actaId);
        if (actual == null) {
            return new PrototipoStore.VerDocumentoPortalResultado(
                    PrototipoStore.VerDocumentoPortalEstado.NOT_FOUND, null, null);
        }
        PrototipoStore.DocumentoPortalVista vista = listarDocumentosVisiblesPortal(actaId).stream()
                .filter(d -> d.tipoDocumento().equals(tipoDocumento))
                .findFirst()
                .orElse(null);
        if (vista == null) {
            return new PrototipoStore.VerDocumentoPortalResultado(
                    PrototipoStore.VerDocumentoPortalEstado.NOT_FOUND, null, null);
        }
        if (vista.notificado()) {
            return new PrototipoStore.VerDocumentoPortalResultado(
                    PrototipoStore.VerDocumentoPortalEstado.OK_YA_NOTIFICADO, actaId, tipoDocumento);
        }

        TipoNotificacion tipo = tipoNotificacionDeDocumentoPortal(tipoDocumento);
        if (tipo == TipoNotificacion.ACTA_INFRACCION) {
            if (!BANDEJA_PENDIENTE_NOTIFICACION.equals(actual.bandejaActual())
                    && !BANDEJA_EN_NOTIFICACION.equals(actual.bandejaActual())) {
                return new PrototipoStore.VerDocumentoPortalResultado(
                        PrototipoStore.VerDocumentoPortalEstado.CONFLICT, actaId, tipoDocumento);
            }
            actas.put(actaId, moverAAnalisisPorVisualizacionPortal(actual));
            accionPendientePorActa.remove(actaId);
        } else {
            PrototipoStore.RegistrarNotificacionPositivaResultado juridico =
                    falloPlazoApelacion.registrarNotificacionPositivaDeFalloTipificada(actaId, tipo);
            if (juridico.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND) {
                return new PrototipoStore.VerDocumentoPortalResultado(
                        PrototipoStore.VerDocumentoPortalEstado.NOT_FOUND, null, null);
            }
            if (juridico.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT) {
                return new PrototipoStore.VerDocumentoPortalResultado(
                        PrototipoStore.VerDocumentoPortalEstado.CONFLICT, actaId, tipoDocumento);
            }
        }

        registrarNotificacionPositivaPortalPorVisualizacion(actaId, actual, tipo);
        registrarEventoVisualizacionPortal(actaId, tipo);
        marcarNotificacionesAlternativasSuperadasPorPortal(actaId, tipo);
        return new PrototipoStore.VerDocumentoPortalResultado(
                PrototipoStore.VerDocumentoPortalEstado.OK_NOTIFICADO, actaId, tipoDocumento);
    }

    private TipoNotificacion tipoNotificacionDeDocumentoPortal(String tipoDocumento) {
        if (TIPO_DOC_FALLO_CONDENATORIO.equals(tipoDocumento)) {
            return TipoNotificacion.FALLO_CONDENATORIO;
        }
        if (TIPO_DOC_FALLO_ABSOLUTORIO.equals(tipoDocumento)) {
            return TipoNotificacion.FALLO_ABSOLUTORIO;
        }
        // Pieza firmada del acta inicial (tipoDocumento de los mocks demo).
        if ("ACTA_FIRMADA".equals(tipoDocumento)) {
            return TipoNotificacion.ACTA_INFRACCION;
        }
        return null;
    }

    private String tituloDocumentoPortal(String tipoDocumento) {
        if (TIPO_DOC_FALLO_CONDENATORIO.equals(tipoDocumento)) {
            return "Fallo condenatorio";
        }
        if (TIPO_DOC_FALLO_ABSOLUTORIO.equals(tipoDocumento)) {
            return "Fallo absolutorio";
        }
        return "Acta de infracción";
    }

    private boolean tipoNotificadoPositivamentePortal(String actaId, TipoNotificacion tipo) {
        List<ActaNotificacionMock> notifs = notificacionesPorActa.get(actaId);
        boolean notifPositiva = notifs != null && notifs.stream()
                .anyMatch(n -> n.tipo() == tipo && n.resultado() == ResultadoNotificacion.POSITIVA);
        if (notifPositiva) {
            return true;
        }
        if (tipo == TipoNotificacion.ACTA_INFRACCION) {
            return false;
        }
        String evento = tipo == TipoNotificacion.FALLO_ABSOLUTORIO
                ? "FALLO_ABSOLUTORIO_NOTIFICADO"
                : "FALLO_CONDENATORIO_NOTIFICADO";
        List<ActaEventoMock> evs = eventosPorActa.get(actaId);
        return evs != null && evs.stream().anyMatch(e -> evento.equals(e.tipoEvento()));
    }

    /**
     * Registra la notificación positiva derivada de la visualización del
     * documento en el portal. Si existe una notificación de portal pendiente
     * del mismo tipo, se confirma esa (sin duplicar); si no, se agrega una
     * nueva notificación {@code ENTREGADA}/{@code POSITIVA} por canal
     * {@code PORTAL_INFRACTOR}.
     */
    private void registrarNotificacionPositivaPortalPorVisualizacion(
            String actaId, ActaMock acta, TipoNotificacion tipo) {
        List<ActaNotificacionMock> notifs =
                notificacionesPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        LocalDateTime fecha = LocalDateTime.now();
        for (int i = 0; i < notifs.size(); i++) {
            ActaNotificacionMock n = notifs.get(i);
            if (n.tipo() == tipo && esNotificacionPortalPendiente(n)) {
                notifs.set(i, n.conVisualizacionPortal(
                        fecha, "Documento visualizado desde portal infractor"));
                return;
            }
        }
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        String idNotif = "NOT-" + sufijoActa + "-" + String.format("%02d", notifs.size() + 1);
        String descripcion = acta.infractorNombre()
                + " — notificación positiva por visualización en portal (" + tipo.name() + ")";
        notifs.add(new ActaNotificacionMock(
                idNotif,
                actaId,
                CanalNotificacion.PORTAL_INFRACTOR.name(),
                "ENTREGADA",
                descripcion,
                tipo,
                CanalNotificacion.PORTAL_INFRACTOR,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                descripcion,
                "DOCUMENTO_VISUALIZADO_PORTAL",
                null,
                null,
                null,
                null,
                fecha,
                "Documento visualizado desde portal infractor",
                acta.infractorNombre(),
                null,
                null,
                null,
                null));
    }

    /**
     * Regla transversal de notificación: cuando una pieza/documento queda
     * notificada positivamente por canal {@code PORTAL_INFRACTOR}, toda
     * notificación alternativa de la misma pieza que siga pendiente
     * (resultado {@code SIN_RESULTADO}, p. ej. correo postal preparado y
     * nunca enviado) queda formalmente {@code SUPERADA_POR_PORTAL} /
     * {@code SIN_EFECTO}: sale de los circuitos operativos (lotes de
     * correo, acuses, pendientes por resolver) pero conserva trazabilidad.
     * Registra un evento de auditoría por cada notificación superada.
     */
    private void marcarNotificacionesAlternativasSuperadasPorPortal(
            String actaId, TipoNotificacion tipo) {
        List<ActaNotificacionMock> notifs = notificacionesPorActa.get(actaId);
        if (notifs == null || notifs.isEmpty()) {
            return;
        }
        LocalDateTime fecha = LocalDateTime.now();
        for (int i = 0; i < notifs.size(); i++) {
            ActaNotificacionMock n = notifs.get(i);
            if (n.tipo() != tipo || n.resultado() != ResultadoNotificacion.SIN_RESULTADO) {
                continue;
            }
            notifs.set(i, n.conSuperadaPorPortal(fecha));
            registrarEventoNotificacionSuperadaPorPortal(actaId, n, tipo);
        }
    }

    private void registrarEventoNotificacionSuperadaPorPortal(
            String actaId, ActaNotificacionMock superada, TipoNotificacion tipo) {
        ActaMock acta = actas.get(actaId);
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        String bloque = acta != null ? acta.bloqueActual() : "D5_ANALISIS";
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijoActa + "-" + String.format("%02d", eventos.size() + 1),
                actaId,
                LocalDateTime.now(),
                "NOTIFICACION_SUPERADA_POR_PORTAL",
                bloque,
                bloque,
                "Notificación " + superada.id() + " (" + superada.canalTipificado().name()
                        + ") de " + tipo.name()
                        + " queda sin efecto: la pieza ya fue notificada positivamente por portal infractor."));
    }

    private boolean esNotificacionPortalPendiente(ActaNotificacionMock n) {
        return n.canalTipificado() == CanalNotificacion.DOMICILIO_ELECTRONICO
                && n.resultado() == ResultadoNotificacion.SIN_RESULTADO
                && (n.estado() == EstadoNotificacion.PENDIENTE_PREPARACION
                        || n.estado() == EstadoNotificacion.LISTA_PARA_ENVIO
                        || n.estado() == EstadoNotificacion.ENVIADA);
    }

    private int prioridadNotificacionPortal(TipoNotificacion tipo) {
        return switch (tipo) {
            case FALLO_CONDENATORIO -> 1;
            case FALLO_ABSOLUTORIO -> 2;
            case ACTA_INFRACCION -> 3;
        };
    }

    private ActaMock moverAAnalisisPorVisualizacionPortal(ActaMock actual) {
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
                true,
                actual.fechaCreacion(),
                actual.infractorNombre(),
                actual.infractorDocumento(),
                actual.inspectorNombre(),
                actual.resumenHecho(),
                BANDEJA_PENDIENTE_ANALISIS);
    }

    private void reemplazarNotificacion(ActaNotificacionMock actualizada) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actualizada.actaId());
        if (lista == null) {
            return;
        }
        for (int i = 0; i < lista.size(); i++) {
            if (actualizada.id().equals(lista.get(i).id())) {
                lista.set(i, actualizada);
                return;
            }
        }
    }

    private void registrarEventoVisualizacionPortal(String actaId, TipoNotificacion tipo) {
        ActaMock acta = actas.get(actaId);
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new ArrayList<>());
        String sufijoActa = PrototipoStoreUtil.sufijoActa(actaId);
        int siguiente = eventos.size() + 1;
        String bloque = acta != null ? acta.bloqueActual() : "D5_ANALISIS";
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente),
                actaId,
                LocalDateTime.now(),
                "NOTIFICACION_PORTAL_VISUALIZADA",
                bloque,
                bloque,
                "Visualización confirmada desde portal infractor para " + tipo.name() + "."));
    }
}
