package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de proceso del circuito notificatorio del expediente.
 *
 * Enviar, registrar positiva, registrar negativa, registrar vencida.
 * Cada resultado actualiza el estado de la notificacion, registra el evento
 * real y recalcula el snapshot del acta.
 *
 * Regla: no se puede notificar si el documento no esta firmado.
 *
 * Comportamiento en registrarPositiva segun tipo de documento notificado:
 * - documento de fallo absolutorio (detectado por FalActaFallo.tipoFallo):
 *     asigna ABSUELTO; si no hay bloqueantes, cierra el acta.
 * - documento de fallo condenatorio (detectado por FalActaFallo.tipoFallo):
 *     deja en ANAL para prox. slice (apelacion/pago condena).
 * - cualquier otro documento: avanza a bloque ANAL.
 *
 * La distincion absolutorio/condenatorio ya no se basa en TipoDocu (ambos son
 * ACTO_ADMINISTRATIVO desde 8C-1), sino en FalActaFallo.tipoFallo comparando
 * el documentoId del fallo con el idDocumento de la notificacion.
 */
@Service
public class NotificacionService {

    private final ActaRepository actaRepository;
    private final DocumentoRepository documentoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FalloActaRepository falloActaRepository;
    private final BloqueantesMaterialesChecker bloqueantesMaterialesChecker;
    private final FaltasClock faltasClock;

    public NotificacionService(
            ActaRepository actaRepository,
            DocumentoRepository documentoRepository,
            NotificacionRepository notificacionRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FalloActaRepository falloActaRepository,
            BloqueantesMaterialesChecker bloqueantesMaterialesChecker,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.falloActaRepository = falloActaRepository;
        this.bloqueantesMaterialesChecker = bloqueantesMaterialesChecker;
    }

    // -------------------------------------------------------------------------
    // EnviarNotificacion
    // -------------------------------------------------------------------------

    public ComandoResultado enviarNotificacion(EnviarNotificacionCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede enviar notificacion.");
        }

        FalDocumento doc = documentoRepository.buscarPorId(cmd.idDocumento())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.idDocumento()));

        if (!doc.estaFirmado()) {
            throw new PrecondicionVioladaException(
                    "No se puede notificar un documento que no esta firmado. Estado actual: "
                            + doc.getEstadoDocu());
        }
        if (!acta.getId().equals(doc.getIdActa())) {
            throw new PrecondicionVioladaException(
                    "El documento no pertenece al acta indicada.");
        }

        LocalDateTime ahora = faltasClock.now();
        Optional<FalNotificacion> activaOpt = notificacionRepository.buscarActivaPorDocumento(doc.getId());

        FalNotificacion notif;
        Long idNotif;
        if (activaOpt.isEmpty()) {
            idNotif = notificacionRepository.nextId();
            notif = new FalNotificacion(idNotif, acta.getId(), doc.getId(), doc.getTipoDocu(),
                    cmd.canal(), ahora);
        } else {
            FalNotificacion activa = activaOpt.get();
            if (activa.getEstado() == EstadoNotificacion.PENDIENTE_ENVIO) {
                notif = activa;
                idNotif = notif.getId();
                notif.iniciarEnvio(cmd.canal(), ahora, ahora, "SISTEMA");
            } else {
                throw new PrecondicionVioladaException(
                        "Ya existe una notificacion activa en estado " + activa.getEstado()
                        + " para el documento " + doc.getId()
                        + ". No se puede crear una segunda notificacion activa.");
            }
        }
        if (cmd.observaciones() != null) notif.setObservaciones(cmd.observaciones());
        notificacionRepository.guardar(notif);

        acta.setBloqueActual(BloqueActual.NOTI);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.NOTENV, doc.getId(), idNotif,
                null, "Notificacion enviada. Canal: " + cmd.canal());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(idNotif),
                TipoEventoActa.NOTENV.codigo(),
                "Notificacion enviada. Canal: " + cmd.canal());
    }

    // -------------------------------------------------------------------------
    // RegistrarNotificacionPositiva
    // -------------------------------------------------------------------------

    public ComandoResultado registrarPositiva(RegistrarNotificacionPositivaCommand cmd) {
        FalNotificacion notif = notificacionRepository.buscarPorId(cmd.idNotificacion())
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null) {
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());
        }

        LocalDateTime ahoraPositiva = faltasClock.now();
        notif.setEstado(EstadoNotificacion.CON_ACUSE_POSITIVO);
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notif.setFechaResultado(ahoraPositiva);
        if (cmd.observaciones() != null) notif.setObservaciones(cmd.observaciones());
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

        // Detectar si la notificacion corresponde al documento de fallo activo.
        // La distincion absolutorio/condenatorio vive en FalActaFallo.tipoFallo,
        // no en TipoDocu (ambos fallos son ACTO_ADMINISTRATIVO desde 8C-1).
        Optional<FalActaFallo> falloOpt = falloActaRepository.buscarActivo(acta.getId());
        if (falloOpt.isPresent()
                && notif.getIdDocumento().equals(falloOpt.get().getDocumentoId())) {
            FalActaFallo fallo = falloOpt.get();
            if (fallo.esAbsolutorio()) {
                return registrarPositivaFalloAbsolutorio(acta, notif, ahoraPositiva, cmd.observaciones());
            } else {
                return registrarPositivaFalloCondenatorio(acta, notif, ahoraPositiva, cmd.observaciones());
            }
        }

        return registrarPositivaPiezaPrevia(acta, notif, cmd.observaciones());
    }

    // -------------------------------------------------------------------------
    // RegistrarNotificacionNegativa
    // -------------------------------------------------------------------------

    public ComandoResultado registrarNegativa(RegistrarNotificacionNegativaCommand cmd) {
        FalNotificacion notif = notificacionRepository.buscarPorId(cmd.idNotificacion())
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null) {
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());
        }

        notif.setEstado(EstadoNotificacion.CON_ACUSE_NEGATIVO);
        notif.setResultado(ResultadoNotificacion.NEGATIVO);
        notif.setFechaResultado(faltasClock.now());
        if (cmd.observaciones() != null) notif.setObservaciones(cmd.observaciones());
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.NOTNEG, notif.getIdDocumento(),
                notif.getId(), null,
                "Notificacion negativa. Bloque retorna a ANAL. " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTNEG.codigo(),
                "Notificacion registrada como negativa. Snapshot actualizado para evaluacion.");
    }

    // -------------------------------------------------------------------------
    // RegistrarNotificacionVencida
    // -------------------------------------------------------------------------

    public ComandoResultado registrarVencida(RegistrarNotificacionVencidaCommand cmd) {
        FalNotificacion notif = notificacionRepository.buscarPorId(cmd.idNotificacion())
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null) {
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());
        }

        notif.setEstado(EstadoNotificacion.VENCIDA);
        notif.setResultado(ResultadoNotificacion.VENCIDO);
        notif.setFechaResultado(faltasClock.now());
        if (cmd.observaciones() != null) notif.setObservaciones(cmd.observaciones());
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

        registrarEvento(acta.getId(), TipoEventoActa.NOTVNC, notif.getIdDocumento(),
                notif.getId(), null,
                "Notificacion vencida. " + nvl(cmd.observaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTVNC.codigo(),
                "Notificacion registrada como vencida. Snapshot actualizado para evaluacion.");
    }

    // -------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------

    public List<FalNotificacion> obtenerNotificaciones(Long idActa) {
        actaRepository.buscarPorId(idActa)
                .orElseThrow(() -> new ActaNoEncontradaException(idActa));
        return notificacionRepository.buscarPorActa(idActa);
    }

    // -------------------------------------------------------------------------
    // Internos: manejo especifico por tipo de pieza notificada
    // -------------------------------------------------------------------------

    private ComandoResultado registrarPositivaFalloAbsolutorio(
            FalActa acta, FalNotificacion notif, LocalDateTime ahora, String observaciones) {

        actualizarFalloNotificado(acta.getId(), ahora);

        acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);

        boolean sinBloqueantes = !bloqueantesMaterialesChecker.tieneBloqueantesActivos(acta.getId());
        if (sinBloqueantes) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
        } else {
            acta.setBloqueActual(BloqueActual.ANAL);
        }
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), null,
                "Notificacion positiva de fallo absolutorio. " + nvl(observaciones));

        if (sinBloqueantes) {
            registrarEvento(acta.getId(), TipoEventoActa.CIERRA, null, null, null,
                    "Acta cerrada. Fallo absolutorio notificado sin bloqueantes.");

            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);

            return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                    TipoEventoActa.NOTPOS.codigo(),
                    "Notificacion positiva de fallo absolutorio. Acta cerrada. Resultado: ABSUELTO.");
        }

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTPOS.codigo(),
                "Notificacion positiva de fallo absolutorio. Resultado: ABSUELTO. "
                        + "No se cierra por bloqueantes materiales activos.");
    }

    private ComandoResultado registrarPositivaFalloCondenatorio(
            FalActa acta, FalNotificacion notif, LocalDateTime ahora, String observaciones) {

        actualizarFalloNotificado(acta.getId(), ahora);

        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), null,
                "Notificacion positiva de fallo condenatorio. " + nvl(observaciones));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTPOS.codigo(),
                "Notificacion positiva de fallo condenatorio. "
                        + "Pendiente proximo slice: apelacion / firmeza / pago condena.");
    }

    private ComandoResultado registrarPositivaPiezaPrevia(
            FalActa acta, FalNotificacion notif, String observaciones) {

        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), null,
                "Notificacion positiva. " + nvl(observaciones));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTPOS.codigo(),
                "Notificacion registrada como positiva. Bloque avanzado a ANAL.");
    }

    private void actualizarFalloNotificado(Long actaId, LocalDateTime ahora) {
        Optional<FalActaFallo> falloOpt = falloActaRepository.buscarActivo(actaId);
        falloOpt.ifPresent(fallo -> {
            fallo.marcarNotificado(ahora);
            falloActaRepository.guardar(fallo);
        });
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(idUserEvt != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(faltasClock.now())
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}
