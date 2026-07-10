package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoLote;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCodigoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCorreoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalLoteCorreo;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.LoteCorreoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestiona los lotes de correo postal.
 * Un lote agrupa N notificaciones para envio postal colectivo.
 * Transiciones: GENERADO -> EMITIDO -> PROCESADO (o ANULADO / CON_ERROR).
 */
@Service
public class LoteCorreoService {

    private final Object loteGeneracionMonitor = new Object();

    private final LoteCorreoRepository loteCorreoRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionIntentoRepository intentoRepository;
    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FaltasClock faltasClock;

    public LoteCorreoService(
            LoteCorreoRepository loteCorreoRepository,
            NotificacionRepository notificacionRepository,
            NotificacionIntentoRepository intentoRepository,
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.loteCorreoRepository = loteCorreoRepository;
        this.notificacionRepository = notificacionRepository;
        this.intentoRepository = intentoRepository;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    public FalLoteCorreo generarLote(String loteCodigo, String referenciaExterna, String guidLoteExt, String idUser) {
        synchronized (loteGeneracionMonitor) {
            if (loteCorreoRepository.existeCodigo(loteCodigo))
                throw new LoteCodigoDuplicadoException(loteCodigo);

            LocalDateTime ahora = faltasClock.now();
            Long id = loteCorreoRepository.nextId();
            FalLoteCorreo lote = new FalLoteCorreo(id, loteCodigo, ahora, ahora, idUser);
            if (referenciaExterna != null) lote.setReferenciaExterna(referenciaExterna);
            if (guidLoteExt != null) lote.setGuidLoteExt(guidLoteExt);
            loteCorreoRepository.guardar(lote);
            return loteCorreoRepository.buscarPorId(id).orElseThrow();
        }
    }

    public FalLoteCorreo generarLoteConIntentos(
            String loteCodigo,
            List<Long> notificacionIds,
            String referenciaExterna,
            String guidLoteExt,
            String idUser) {

        if (notificacionIds == null || notificacionIds.isEmpty())
            throw new PrecondicionVioladaException("Debe indicar al menos una notificacion para generar el lote");

        LocalDateTime ahora = faltasClock.now();
        Long loteId = loteCorreoRepository.nextId();
        FalLoteCorreo lote = new FalLoteCorreo(loteId, loteCodigo, ahora, ahora, idUser);
        if (referenciaExterna != null) lote.setReferenciaExterna(referenciaExterna);
        if (guidLoteExt != null) lote.setGuidLoteExt(guidLoteExt);
        synchronized (loteGeneracionMonitor) {
            if (loteCorreoRepository.existeCodigo(loteCodigo))
                throw new LoteCodigoDuplicadoException(loteCodigo);
            loteCorreoRepository.guardar(lote);
        }

        List<Long> actasAfectadas = new ArrayList<>();

        for (Long notifId : notificacionIds) {
            FalNotificacion notif = notificacionRepository.buscarPorId(notifId).orElse(null);
            if (notif == null) continue;

            if (notif.getResultado() == ResultadoNotificacion.POSITIVO) continue;

            short nroIntento = intentoRepository.siguienteNroIntento(notifId);
            Long intentoId = intentoRepository.nextId();
            FalNotificacionIntento intento = new FalNotificacionIntento(
                    intentoId, notifId, nroIntento,
                    ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion.CORREO_POSTAL,
                    null, null, loteId, null,
                    ahora, ahora, idUser);
            intentoRepository.guardar(intento);

            notif.setEstado(EstadoNotificacion.EN_PROCESO);
            notif.setFhUltMod(ahora);
            notif.setIdUserUltMod(idUser);
            notificacionRepository.guardar(notif);

            if (!actasAfectadas.contains(notif.getIdActa())) {
                actasAfectadas.add(notif.getIdActa());
            }
        }

        for (Long actaId : actasAfectadas) {
            FalActa acta = actaRepository.buscarPorId(actaId).orElse(null);
            if (acta != null) {
                registrarEvento(actaId, TipoEventoActa.LOTGEN, loteId, null, idUser,
                        "Lote generado: " + loteCodigo);
                FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
                snapshotRepository.guardar(snap);
            }
        }

        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo emitirLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esEmitible())
            throw new PrecondicionVioladaException("El lote " + loteId + " no puede emitirse en estado " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.EMITIDO);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo procesarLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esProcesable())
            throw new PrecondicionVioladaException("El lote " + loteId + " no puede procesarse en estado " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.PROCESADO);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo marcarConError(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (lote.getEstadoLote() == EstadoLote.ANULADO || lote.getEstadoLote() == EstadoLote.PROCESADO)
            throw new PrecondicionVioladaException("El lote " + loteId + " ya esta en estado final: " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.CON_ERROR);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo anularLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esAnulable())
            throw new PrecondicionVioladaException("El lote " + loteId + " no puede anularse en estado " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.ANULADO);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public List<FalLoteCorreo> buscarPorEstado(EstadoLote estado) {
        return loteCorreoRepository.buscarPorEstado(estado);
    }

    public FalLoteCorreo buscarPorCodigo(String loteCodigo) {
        return loteCorreoRepository.buscarPorCodigo(loteCodigo)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteCodigo));
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, Long idRef, Long idExtra, String idUser, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.LOTE_CORREO)
                .fhEvt(faltasClock.now())
                .actorTipo(ActorTipoEvento.NOTIFICADOR)
                .idUserEvt(idUser)
                .correlacionId(idRef != null ? String.valueOf(idRef) : null)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }
}
