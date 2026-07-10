package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoAcuse;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoAcuse;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.AcuseDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionAcuseNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionAcuse;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionAcuseRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona los acuses de notificacion.
 * El acuse documenta/valida; no reemplaza el resultado operativo del intento.
 * La validacion de acuse produce efectos sobre el intento y la cabecera.
 *
 * Matriz de efectos (validacion):
 * ACUSE_RECEPCION -> POSITIVO / ENTREGADA
 * ACUSE_RECHAZO -> NEGATIVO / NEGATIVA
 * ACUSE_DOMICILIO_INEXISTENTE -> NEGATIVO / NEGATIVA
 * ACUSE_PERSONA_DESCONOCIDA -> NEGATIVO / NEGATIVA
 * ACUSE_AUSENTE -> segun circuito (no produce efecto automatico)
 * ACUSE_OTRO -> resultado explicito, no se infiere
 */
@Service
public class NotificacionAcuseService {

    private final NotificacionAcuseRepository acuseRepository;
    private final NotificacionIntentoRepository intentoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FaltasClock faltasClock;

    public NotificacionAcuseService(
            NotificacionAcuseRepository acuseRepository,
            NotificacionIntentoRepository intentoRepository,
            NotificacionRepository notificacionRepository,
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.acuseRepository = acuseRepository;
        this.intentoRepository = intentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    public FalNotificacionAcuse registrarAcuse(
            Long notificacionId,
            Long intentoId,
            TipoAcuse tipoAcuse,
            String storageKey,
            LocalDateTime fhAcuse,
            String idUser) {

        FalNotificacion notif = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));

        if (intentoId != null) {
            FalNotificacionIntento intento = intentoRepository.buscarPorId(intentoId)
                    .orElseThrow(() -> new NotificacionIntentoNoEncontradoException(intentoId));
            if (!intento.getNotificacionId().equals(notificacionId))
                throw new PrecondicionVioladaException("El intento " + intentoId + " no pertenece a la notificacion " + notificacionId);
        }

        LocalDateTime ahora = faltasClock.now();
        FalNotificacionAcuse acuse;
        // Atomic check+save for idempotency under concurrency
        synchronized (acuseRepository) {
            acuseRepository.buscarPorIdempotencia(notificacionId, intentoId, tipoAcuse)
                    .ifPresent(existing -> {
                        throw new AcuseDuplicadoException(notificacionId, intentoId, tipoAcuse.name());
                    });
            Long id = acuseRepository.nextId();
            acuse = new FalNotificacionAcuse(id, notificacionId, intentoId, tipoAcuse, ahora, idUser);
            acuse.setStorageKey(storageKey);
            acuse.setFhAcuse(fhAcuse);
            acuse.setEstadoAcuse(EstadoAcuse.RECIBIDO);
            acuseRepository.guardar(acuse);
        }

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa()).orElse(null);
        if (acta != null) {
            registrarEvento(acta.getId(), TipoEventoActa.ACUGEN,
                    notificacionId, acuse.getId(), idUser,
                    "Acuse registrado tipo=" + tipoAcuse + (intentoId != null ? " intentoId=" + intentoId : ""));
        }

        return acuseRepository.buscarPorId(acuse.getId()).orElseThrow();
    }

    public FalNotificacionAcuse validarAcuse(Long acuseId, String idUser) {
        FalNotificacionAcuse acuse = acuseRepository.buscarPorId(acuseId)
                .orElseThrow(() -> new NotificacionAcuseNoEncontradoException(acuseId));

        if (acuse.estaAnulado())
            throw new PrecondicionVioladaException("El acuse " + acuseId + " esta anulado");
        if (acuse.estaValidado())
            throw new PrecondicionVioladaException("El acuse " + acuseId + " ya esta validado");

        acuse.setEstadoAcuse(EstadoAcuse.VALIDADO);
        acuseRepository.guardar(acuse);

        aplicarEfectosDeValidacion(acuse, idUser);

        FalNotificacion notif = notificacionRepository.buscarPorId(acuse.getNotificacionId()).orElse(null);
        if (notif != null) {
            FalActa acta = actaRepository.buscarPorId(notif.getIdActa()).orElse(null);
            if (acta != null) {
                registrarEvento(acta.getId(), TipoEventoActa.ACUVAL,
                        acuse.getNotificacionId(), acuseId, idUser,
                        "Acuse validado tipo=" + acuse.getTipoAcuse());
                FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
                snapshotRepository.guardar(snap);
            }
        }

        return acuseRepository.buscarPorId(acuseId).orElseThrow();
    }

    public FalNotificacionAcuse observarAcuse(Long acuseId, String idUser) {
        FalNotificacionAcuse acuse = acuseRepository.buscarPorId(acuseId)
                .orElseThrow(() -> new NotificacionAcuseNoEncontradoException(acuseId));
        if (acuse.estaAnulado())
            throw new PrecondicionVioladaException("El acuse " + acuseId + " esta anulado");
        acuse.setEstadoAcuse(EstadoAcuse.OBSERVADO);
        acuseRepository.guardar(acuse);
        return acuseRepository.buscarPorId(acuseId).orElseThrow();
    }

    public FalNotificacionAcuse anularAcuse(Long acuseId, String idUser) {
        FalNotificacionAcuse acuse = acuseRepository.buscarPorId(acuseId)
                .orElseThrow(() -> new NotificacionAcuseNoEncontradoException(acuseId));
        if (acuse.estaAnulado())
            throw new PrecondicionVioladaException("El acuse " + acuseId + " ya esta anulado");
        acuse.setEstadoAcuse(EstadoAcuse.ANULADO);
        acuseRepository.guardar(acuse);
        return acuseRepository.buscarPorId(acuseId).orElseThrow();
    }

    public List<FalNotificacionAcuse> obtenerAcusesPorNotificacion(Long notificacionId) {
        return acuseRepository.buscarPorNotificacion(notificacionId);
    }

    public List<FalNotificacionAcuse> obtenerAcusesPorIntento(Long intentoId) {
        return acuseRepository.buscarPorIntento(intentoId);
    }

    private void aplicarEfectosDeValidacion(FalNotificacionAcuse acuse, String idUser) {
        TipoAcuse tipo = acuse.getTipoAcuse();
        if (tipo == TipoAcuse.ACUSE_AUSENTE || tipo == TipoAcuse.ACUSE_OTRO) return;

        ResultadoNotificacion resultado;
        EstadoNotificacion estadoIntento;
        EstadoNotificacion estadoNotif;

        if (tipo.implicaResultadoPositivo()) {
            resultado = ResultadoNotificacion.POSITIVO;
            estadoIntento = EstadoNotificacion.CON_ACUSE_POSITIVO;
            estadoNotif = EstadoNotificacion.CON_ACUSE_POSITIVO;
        } else if (tipo.implicaResultadoNegativo()) {
            resultado = ResultadoNotificacion.NEGATIVO;
            estadoIntento = EstadoNotificacion.CON_ACUSE_NEGATIVO;
            estadoNotif = EstadoNotificacion.CON_ACUSE_NEGATIVO;
        } else {
            return;
        }

        LocalDateTime ahora = faltasClock.now();

        if (acuse.getIntentoId() != null) {
            intentoRepository.buscarPorId(acuse.getIntentoId()).ifPresent(intento -> {
                if (!intento.tieneResultado()) {
                    intento.setResultadoIntento(resultado);
                    intento.setEstadoIntento(estadoIntento);
                    intento.setFhResultado(ahora);
                    intento.setFhUltMod(ahora);
                    intento.setIdUserUltMod(idUser);
                    intentoRepository.guardar(intento);
                }
            });
        }

        notificacionRepository.buscarPorId(acuse.getNotificacionId()).ifPresent(notif -> {
            boolean positiva = resultado == ResultadoNotificacion.POSITIVO;
            boolean yaPositiva = notif.getResultado() == ResultadoNotificacion.POSITIVO;
            if (positiva || !yaPositiva) {
                notif.setEstado(estadoNotif);
                notif.setResultado(resultado);
                notif.setFechaResultado(ahora);
                notif.setFhUltMod(ahora);
                notif.setIdUserUltMod(idUser);
                notificacionRepository.guardar(notif);
            }
        });
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, Long idNotifRel, Long idAcuse, String idUser, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.SERVICIO_NOTIFICACION)
                .fhEvt(faltasClock.now())
                .idNotifRel(idNotifRel)
                .idUserEvt(idUser)
                .actorTipo(ActorTipoEvento.NOTIFICADOR)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }
}
