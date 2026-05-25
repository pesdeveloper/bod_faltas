package ar.gob.malvinas.faltas.prototipo.store;

import ar.gob.malvinas.faltas.prototipo.domain.ActaEventoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_EN_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_ANALISIS;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BANDEJA_PENDIENTE_NOTIFICACION;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D4;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.BLOQUE_D5;
import static ar.gob.malvinas.faltas.prototipo.store.PrototipoConstantes.ESTADO_PENDIENTE_REVISION;

final class NotificadorMunicipalSupport {

    static final String QR_NOTIFICACION_DEMO_PREFIJO = "QR-NOT-";
    static final String QR_NOTIFICACION_DEMO_SUFIJO = "-DEMO";

    private final Map<String, ActaMock> actas;
    private final Map<String, List<ActaEventoMock>> eventosPorActa;
    private final Map<String, List<ActaNotificacionMock>> notificacionesPorActa;
    private final Map<String, String> accionPendientePorActa;
    private final java.util.function.Function<String, PrototipoStore.RegistrarNotificacionPositivaResultado>
            registrarPositiva;

    NotificadorMunicipalSupport(
            Map<String, ActaMock> actas,
            Map<String, List<ActaEventoMock>> eventosPorActa,
            Map<String, List<ActaNotificacionMock>> notificacionesPorActa,
            Map<String, String> accionPendientePorActa,
            java.util.function.Function<String, PrototipoStore.RegistrarNotificacionPositivaResultado>
                    registrarPositiva) {
        this.actas = actas;
        this.eventosPorActa = eventosPorActa;
        this.notificacionesPorActa = notificacionesPorActa;
        this.accionPendientePorActa = accionPendientePorActa;
        this.registrarPositiva = registrarPositiva;
    }

    List<PrototipoStore.NotificacionMunicipalVista> listarPendientes() {
        return notificacionesPorActa.values().stream()
                .flatMap(List::stream)
                .filter(n -> n.canalTipificado() == CanalNotificacion.NOTIFICADOR_MUNICIPAL)
                .filter(n -> n.resultado() == ResultadoNotificacion.SIN_RESULTADO)
                .filter(n -> n.estado() == EstadoNotificacion.PENDIENTE_PREPARACION
                        || n.estado() == EstadoNotificacion.LISTA_PARA_ENVIO
                        || n.estado() == EstadoNotificacion.ENVIADA)
                .sorted(Comparator.comparing(ActaNotificacionMock::actaId).thenComparing(ActaNotificacionMock::id))
                .map(this::vista)
                .toList();
    }

    PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado registrarAcuse(
            String notificacionId,
            ResultadoNotificacion resultado,
            String observacion) {
        Optional<ActaNotificacionMock> encontrada = buscarPorId(notificacionId);
        if (encontrada.isEmpty()) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.notFound();
        }
        ActaNotificacionMock notificacion = encontrada.get();
        if (notificacion.canalTipificado() != CanalNotificacion.NOTIFICADOR_MUNICIPAL) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.wrongChannel(notificacion.actaId());
        }
        if (resultado == null || resultado == ResultadoNotificacion.SIN_RESULTADO) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.badRequest(notificacion.actaId());
        }
        if (notificacion.resultado() != ResultadoNotificacion.SIN_RESULTADO) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.conflict(notificacion.actaId());
        }

        LocalDateTime fechaResultado = LocalDateTime.now();
        if (resultado == ResultadoNotificacion.POSITIVA) {
            return registrarPositiva(notificacion, fechaResultado, observacion);
        }
        return registrarNoPositiva(notificacion, resultado, fechaResultado, observacion);
    }

    private PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado registrarPositiva(
            ActaNotificacionMock notificacion,
            LocalDateTime fechaResultado,
            String observacion) {
        PrototipoStore.RegistrarNotificacionPositivaResultado r = registrarPositiva.apply(notificacion.actaId());
        if (r.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.NOT_FOUND) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.notFound();
        }
        if (r.estado() == PrototipoStore.RegistrarNotificacionPositivaEstado.CONFLICT) {
            return PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado.conflict(notificacion.actaId());
        }
        ActaNotificacionMock actualizada = buscarPorId(notificacion.id()).orElse(notificacion);
        String evento = switch (notificacion.tipo()) {
            case FALLO_ABSOLUTORIO -> "FALLO_ABSOLUTORIO_NOTIFICADO";
            case FALLO_CONDENATORIO -> "FALLO_CONDENATORIO_NOTIFICADO";
            case ACTA_INFRACCION -> "NOTIFICACION_ENTREGADA";
        };
        ActaNotificacionMock conAcuse = actualizada.conResultadoCorreo(
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                "ENTREGADA",
                fechaResultado,
                observacion,
                evento);
        reemplazar(conAcuse);
        return ok(conAcuse);
    }

    private PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado registrarNoPositiva(
            ActaNotificacionMock notificacion,
            ResultadoNotificacion resultado,
            LocalDateTime fechaResultado,
            String observacion) {
        EstadoNotificacion estado = resultado == ResultadoNotificacion.NEGATIVA
                ? EstadoNotificacion.NEGATIVA
                : EstadoNotificacion.VENCIDA;
        String legacy = resultado == ResultadoNotificacion.NEGATIVA ? "NO_ENTREGADA" : "VENCIDA";
        String evento = resultado == ResultadoNotificacion.NEGATIVA ? "NOTIFICACION_NO_ENTREGADA" : "NOTIFICACION_VENCIDA";
        ActaNotificacionMock actualizada = notificacion.conResultadoCorreo(
                estado,
                resultado,
                legacy,
                fechaResultado,
                observacion,
                evento);
        reemplazar(actualizada);

        ActaMock acta = actas.get(notificacion.actaId());
        if (acta != null && (BANDEJA_PENDIENTE_NOTIFICACION.equals(acta.bandejaActual())
                || BANDEJA_EN_NOTIFICACION.equals(acta.bandejaActual()))) {
            actas.put(acta.id(), moverAAnalisis(acta));
        }
        String accion = resultado == ResultadoNotificacion.NEGATIVA
                ? PrototipoStore.ACCION_REINTENTAR_NOTIFICACION
                : PrototipoStore.ACCION_EVALUAR_NOTIFICACION_VENCIDA;
        accionPendientePorActa.put(notificacion.actaId(), accion);
        registrarEvento(
                notificacion.actaId(),
                evento,
                BLOQUE_D4,
                BLOQUE_D5,
                "Acuse de notificador municipal " + resultado.name() + " para "
                        + notificacion.tipo().name() + "; acción pendiente " + accion + ".");
        return ok(actualizada);
    }

    private PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado ok(ActaNotificacionMock notificacion) {
        ActaMock acta = actas.get(notificacion.actaId());
        return new PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado(
                PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.OK,
                acta != null ? acta.id() : notificacion.actaId(),
                acta != null ? acta.numeroActa() : notificacion.actaId(),
                acta != null ? acta.bandejaActual() : null,
                acta != null ? acta.estadoProcesoActual() : null,
                notificacion,
                vista(notificacion));
    }

    private PrototipoStore.NotificacionMunicipalVista vista(ActaNotificacionMock n) {
        ActaMock acta = actas.get(n.actaId());
        return new PrototipoStore.NotificacionMunicipalVista(
                n.id(),
                n.actaId(),
                acta != null ? acta.numeroActa() : n.actaId(),
                n.tipo().name(),
                n.canalTipificado().name(),
                n.estado().name(),
                n.resultado().name(),
                primerNoVacio(n.destinatarioNombre(), n.destinatarioResumen()),
                n.domicilioTexto(),
                n.observacion(),
                qrNotificacionDemo(n.id()),
                n.fechaPreparacion(),
                n.fechaEnvio());
    }

    static String qrNotificacionDemo(String notificacionId) {
        return QR_NOTIFICACION_DEMO_PREFIJO + notificacionId + QR_NOTIFICACION_DEMO_SUFIJO;
    }

    private Optional<ActaNotificacionMock> buscarPorId(String notificacionId) {
        if (notificacionId == null || notificacionId.isBlank()) {
            return Optional.empty();
        }
        return notificacionesPorActa.values().stream()
                .flatMap(List::stream)
                .filter(n -> Objects.equals(notificacionId, n.id()))
                .findFirst();
    }

    private void reemplazar(ActaNotificacionMock actualizada) {
        List<ActaNotificacionMock> lista = notificacionesPorActa.get(actualizada.actaId());
        if (lista == null) {
            return;
        }
        for (int i = 0; i < lista.size(); i++) {
            if (Objects.equals(lista.get(i).id(), actualizada.id())) {
                lista.set(i, actualizada);
                return;
            }
        }
    }

    private ActaMock moverAAnalisis(ActaMock actual) {
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

    private void registrarEvento(
            String actaId,
            String tipoEvento,
            String bloqueOrigen,
            String bloqueDestino,
            String descripcion) {
        List<ActaEventoMock> eventos = eventosPorActa.computeIfAbsent(actaId, k -> new java.util.ArrayList<>());
        String sufijoActa = actaId.startsWith("ACTA-") ? actaId.substring("ACTA-".length()) : actaId;
        int siguiente = eventos.size() + 1;
        eventos.add(new ActaEventoMock(
                "EVT-" + sufijoActa + "-" + String.format("%02d", siguiente),
                actaId,
                LocalDateTime.now(),
                tipoEvento,
                bloqueOrigen,
                bloqueDestino,
                descripcion));
    }

    private static String primerNoVacio(String primero, String segundo) {
        if (primero != null && !primero.isBlank()) {
            return primero;
        }
        return segundo;
    }
}
