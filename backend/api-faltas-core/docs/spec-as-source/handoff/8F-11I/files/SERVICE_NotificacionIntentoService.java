package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCorreoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gestiona los intentos de notificacion.
 * Cada intento tiene un correlativo atomico dentro de su notificacion.
 * Los reintentos crean nuevas filas; nunca se sobrescribe un intento anterior.
 */
@Service
public class NotificacionIntentoService {

    private final NotificacionIntentoRepository intentoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final LoteCorreoRepository loteCorreoRepository;

    public NotificacionIntentoService(
            NotificacionIntentoRepository intentoRepository,
            NotificacionRepository notificacionRepository,
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            LoteCorreoRepository loteCorreoRepository) {
        this.intentoRepository = intentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.loteCorreoRepository = loteCorreoRepository;
    }

    public FalNotificacionIntento registrarIntento(
            Long notificacionId,
            CanalNotificacion canal,
            Long domicilioNotifId,
            String destinoDigital,
            Long loteId,
            String referenciaExterna,
            String idUser) {

        FalNotificacion notif = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));

        validarCanalDestino(canal, domicilioNotifId, destinoDigital);

        if (loteId != null) {
            FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                    .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
            if (lote.getEstadoLote() == ar.gob.malvinas.faltas.core.domain.enums.EstadoLote.ANULADO)
                throw new PrecondicionVioladaException("El lote " + loteId + " esta anulado. No se puede asignar a un intento.");
            if (canal != CanalNotificacion.CORREO_POSTAL && canal != CanalNotificacion.NOTIFICADOR_MUNICIPAL)
                throw new PrecondicionVioladaException("Los lotes solo aplican para canales postales/notificador. Canal: " + canal);
        }

        if (referenciaExterna != null && !intentoRepository.claimReferenciaExterna(referenciaExterna)) {
            throw new PrecondicionVioladaException(
                    "Ya existe un intento con referenciaExterna=" + referenciaExterna);
        }

        LocalDateTime ahora = LocalDateTime.now();
        short nroIntento = intentoRepository.siguienteNroIntento(notificacionId);
        Long id = intentoRepository.nextId();

        FalNotificacionIntento intento = new FalNotificacionIntento(
                id, notificacionId, nroIntento, canal,
                domicilioNotifId, destinoDigital, loteId, referenciaExterna,
                ahora, ahora, idUser);
        intentoRepository.guardar(intento);

        // Retry notif update to handle concurrent registrations (optimistic locking)
        for (int retry = 0; retry < 10; retry++) {
            try {
                FalNotificacion notifActual = notificacionRepository.buscarPorId(notificacionId).orElseThrow();
                notifActual.setEstado(EstadoNotificacion.EN_PROCESO);
                notifActual.setFhUltMod(ahora);
                notifActual.setIdUserUltMod(idUser);
                notificacionRepository.guardar(notifActual);
                break;
            } catch (ConcurrenciaConflictoException e) {
                if (retry == 9) throw e;
            }
        }

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa()).orElse(null);
        if (acta != null) {
            TipoEventoActa tipoEvt = nroIntento == 1 ? TipoEventoActa.NOTINT : TipoEventoActa.NOTREI;
            registrarEvento(acta.getId(), tipoEvt,
                    String.valueOf(notificacionId), String.valueOf(id), idUser,
                    "Intento #" + nroIntento + " canal=" + canal + (loteId != null ? " lote=" + loteId : ""));
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        }

        return intentoRepository.buscarPorId(id).orElseThrow();
    }

    public FalNotificacionIntento registrarReintentoPorVencimiento(
            Long notificacionId,
            CanalNotificacion canal,
            Long domicilioNotifId,
            String destinoDigital,
            Long loteId,
            String referenciaExterna,
            String idUser) {

        FalNotificacion notif = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));

        if (notif.getEstado() != EstadoNotificacion.VENCIDA && notif.getResultado() != ResultadoNotificacion.VENCIDO)
            throw new PrecondicionVioladaException("Solo se puede reintentar por vencimiento si la notificacion esta VENCIDA. Estado actual: " + notif.getEstado());

        validarCanalDestino(canal, domicilioNotifId, destinoDigital);

        LocalDateTime ahora = LocalDateTime.now();
        short nroIntento = intentoRepository.siguienteNroIntento(notificacionId);
        Long id = intentoRepository.nextId();

        FalNotificacionIntento intento = new FalNotificacionIntento(
                id, notificacionId, nroIntento, canal,
                domicilioNotifId, destinoDigital, loteId, referenciaExterna,
                ahora, ahora, idUser);
        intentoRepository.guardar(intento);

        notif.setEstado(EstadoNotificacion.EN_PROCESO);
        notif.setResultado(null);
        notif.setFechaResultado(null);
        notif.setFhUltMod(ahora);
        notif.setIdUserUltMod(idUser);
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa()).orElse(null);
        if (acta != null) {
            registrarEvento(acta.getId(), TipoEventoActa.NOTRVE,
                    String.valueOf(notificacionId), String.valueOf(id), idUser,
                    "Reintento post vencimiento #" + nroIntento + " canal=" + canal);
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        }

        return intentoRepository.buscarPorId(id).orElseThrow();
    }

    public FalNotificacionIntento registrarResultadoIntento(
            Long intentoId,
            ResultadoNotificacion resultado,
            String idUser) {

        FalNotificacionIntento intento = intentoRepository.buscarPorId(intentoId)
                .orElseThrow(() -> new NotificacionIntentoNoEncontradoException(intentoId));

        if (intento.tieneResultado())
            throw new PrecondicionVioladaException("El intento " + intentoId + " ya tiene resultado: " + intento.getResultadoIntento());

        if (resultado == null)
            throw new PrecondicionVioladaException("El resultado no puede ser null al registrar resultado de intento");

        LocalDateTime ahora = LocalDateTime.now();
        EstadoNotificacion estadoResultante;
        switch (resultado) {
            case POSITIVO -> estadoResultante = EstadoNotificacion.CON_ACUSE_POSITIVO;
            case NEGATIVO -> estadoResultante = EstadoNotificacion.CON_ACUSE_NEGATIVO;
            case VENCIDO -> estadoResultante = EstadoNotificacion.VENCIDA;
            case SUPERADA_POR_PORTAL -> estadoResultante = EstadoNotificacion.SIN_EFECTO;
            default -> throw new PrecondicionVioladaException("Resultado de intento no reconocido: " + resultado);
        }

        intento.setResultadoIntento(resultado);
        intento.setEstadoIntento(estadoResultante);
        intento.setFhResultado(ahora);
        intento.setFhUltMod(ahora);
        intento.setIdUserUltMod(idUser);
        intentoRepository.guardar(intento);

        return intentoRepository.buscarPorId(intentoId).orElseThrow();
    }

    public FalNotificacionIntento registrarPortalPositivo(
            Long notificacionId,
            String destinoPortal,
            String idUser) {

        FalNotificacion notif = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));

        if (notif.getResultado() == ResultadoNotificacion.POSITIVO)
            throw new PrecondicionVioladaException("La notificacion " + notificacionId + " ya tiene resultado POSITIVO.");

        LocalDateTime ahora = LocalDateTime.now();

        List<FalNotificacionIntento> intentosActivos = intentoRepository.buscarPorNotificacion(notificacionId)
                .stream().filter(i -> !i.tieneResultado()).toList();
        for (FalNotificacionIntento ia : intentosActivos) {
            ia.setResultadoIntento(ResultadoNotificacion.SUPERADA_POR_PORTAL);
            ia.setEstadoIntento(EstadoNotificacion.SIN_EFECTO);
            ia.setFhResultado(ahora);
            ia.setFhUltMod(ahora);
            ia.setIdUserUltMod(idUser);
            intentoRepository.guardar(ia);
        }

        short nroIntento = intentoRepository.siguienteNroIntento(notificacionId);
        Long id = intentoRepository.nextId();

        FalNotificacionIntento intentoPortal = new FalNotificacionIntento(
                id, notificacionId, nroIntento, CanalNotificacion.PORTAL_INFRACTOR,
                null, destinoPortal, null, null,
                ahora, ahora, idUser);
        intentoPortal.setResultadoIntento(ResultadoNotificacion.POSITIVO);
        intentoPortal.setEstadoIntento(EstadoNotificacion.CON_ACUSE_POSITIVO);
        intentoPortal.setFhResultado(ahora);
        intentoRepository.guardar(intentoPortal);

        notif.setEstado(EstadoNotificacion.CON_ACUSE_POSITIVO);
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notif.setFechaResultado(ahora);
        notif.setFhUltMod(ahora);
        notif.setIdUserUltMod(idUser);
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa()).orElse(null);
        if (acta != null) {
            if (!intentosActivos.isEmpty()) {
                registrarEvento(acta.getId(), TipoEventoActa.NOTSUP,
                        String.valueOf(notificacionId), null, idUser,
                        "Intentos previos superados por portal infractor");
            }
            registrarEvento(acta.getId(), TipoEventoActa.PORPOS,
                    String.valueOf(notificacionId), String.valueOf(id), idUser,
                    "Notificacion positiva por visualizacion en portal infractor");
            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        }

        return intentoRepository.buscarPorId(id).orElseThrow();
    }

    public List<FalNotificacionIntento> obtenerIntentos(Long notificacionId) {
        notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));
        return intentoRepository.buscarPorNotificacion(notificacionId);
    }

    private void validarCanalDestino(CanalNotificacion canal, Long domicilioNotifId, String destinoDigital) {
        if (canal.requiereDomicilioFisico() && domicilioNotifId == null)
            throw new PrecondicionVioladaException("El canal " + canal + " requiere domicilioNotifId");
        if (canal.requiereDomicilioFisico() && destinoDigital != null)
            throw new PrecondicionVioladaException("El canal " + canal + " no usa destinoDigital");
        if (canal.esDigital() && (destinoDigital == null || destinoDigital.isBlank()))
            throw new PrecondicionVioladaException("El canal " + canal + " requiere destinoDigital");
        if (canal.esDigital() && domicilioNotifId != null)
            throw new PrecondicionVioladaException("El canal " + canal + " no usa domicilioNotifId");
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, String idNotif, String idIntento, String idUser, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento evento = new FalActaEvento(UUID.randomUUID().toString(), idActa, tipo,
                LocalDateTime.now(), orden, null, idNotif, idUser != null ? idUser : "SYS", descripcion, null);
        eventoRepository.registrar(evento);
    }
}
