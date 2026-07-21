package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.GenerarLoteCorreoCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoLote;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
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
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.LoteCorreoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona los lotes de correo postal.
 * Un lote agrupa N notificaciones para envio postal colectivo.
 * Transiciones: GENERADO -> EMITIDO -> PROCESADO (o ANULADO / CON_ERROR).
 */
@Service
public class LoteCorreoService {

    private final LoteCorreoRepository loteCorreoRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionIntentoRepository intentoRepository;
    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final PersonaDomicilioRepository domicilioRepository;
    private final FaltasClock faltasClock;

    private final Object loteGeneracionMonitor = new Object();

    public LoteCorreoService(
            LoteCorreoRepository loteCorreoRepository,
            NotificacionRepository notificacionRepository,
            NotificacionIntentoRepository intentoRepository,
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            PersonaDomicilioRepository domicilioRepository,
            FaltasClock faltasClock) {
        this.loteCorreoRepository = loteCorreoRepository;
        this.notificacionRepository = notificacionRepository;
        this.intentoRepository = intentoRepository;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.domicilioRepository = domicilioRepository;
        this.faltasClock = faltasClock;
    }

    // =========================================================================
    // CMD-FALLO-003: Generar lote postal desde notificaciones pendientes
    // =========================================================================

    /**
     * Contrato canonico: valida, normaliza, serializa y genera el lote.
     *
     * Orden de ejecucion normativo:
     *  Fuera del monitor:
     *    Validar y normalizar entrada (cmd, actorTecnico, loteCodigo, referenciaExterna, guidLoteExt).
     *  Dentro de synchronized(loteGeneracionMonitor):
     *    1. Precheck de loteCodigo.
     *    2. Consultar PENDIENTE_ENVIO.
     *    3. Ordenar por id ascendente.
     *    4. Recheck ante lista vacia:
 *         loteCodigo existente -> LoteCodigoDuplicadoException
 *         loteCodigo inexistente -> PrecondicionVioladaException
     *    5. Verificar defensivamente que no haya duplicados en lista derivada.
     *    6. Validar cada notificacion, acta y domicilio (sin mutar).
     *    7. Capturar ahora una sola vez.
     *    8. Construir lote candidato con valores normalizados.
     *    9. guardarSiAusentePorCodigo: puerta atomica de unicidad de codigo.
     *   10. Si perdio la carrera: LoteCodigoDuplicadoException sin otros efectos.
     *   11. Solo el ganador aplica efectos (intentos, cabeceras, actas, eventos, snapshots).
     */
    public FalLoteCorreo generarLoteDesdePendientes(GenerarLoteCorreoCommand cmd) {
        if (cmd == null) {
            throw new PrecondicionVioladaException(
                    "El comando de generaci\u00f3n de lote es obligatorio.");
        }
        // Validacion estructural y normalizacion fuera del monitor
        String actorTecnico = normalizarActor(cmd.actorTecnico());
        String loteCodigo   = normalizarLoteCodigo(cmd.loteCodigo());
        String refExt       = normalizarRefExt(cmd.referenciaExterna());
        String guidExt      = normalizarGuid(cmd.guidLoteExt());

        synchronized (loteGeneracionMonitor) {
            // 1. Precheck de loteCodigo
            if (loteCorreoRepository.existeCodigo(loteCodigo))
                throw new LoteCodigoDuplicadoException(loteCodigo);

            // 2-3. Consultar PENDIENTE_ENVIO y ordenar por id ascendente
            List<FalNotificacion> pendientes = notificacionRepository
                    .buscarPorEstado(EstadoNotificacion.PENDIENTE_ENVIO)
                    .stream()
                    .sorted(Comparator.comparingLong(FalNotificacion::getId))
                    .toList();

            // 4. Recheck ante lista vacia
            if (pendientes.isEmpty()) {
                if (loteCorreoRepository.existeCodigo(loteCodigo)) {
                    throw new LoteCodigoDuplicadoException(loteCodigo);
                }
                throw new PrecondicionVioladaException(
                        "No hay notificaciones en estado PENDIENTE_ENVIO para generar el lote.");
            }

            // 5. Verificar defensivamente que no haya duplicados en lista derivada
            verificarSinDuplicados(pendientes);

            // 6. Validar notificaciones, actas y domicilios antes de mutar
            List<NotifConActa> validados = validarDominio(pendientes);

            // 7. Capturar instante canonico una sola vez
            LocalDateTime ahora = faltasClock.now();

            // 8. Construir lote candidato
            Long loteId = loteCorreoRepository.nextId();
            FalLoteCorreo candidato = new FalLoteCorreo(loteId, loteCodigo, ahora, ahora, actorTecnico);
            if (refExt != null)  candidato.setReferenciaExterna(refExt);
            if (guidExt != null) candidato.setGuidLoteExt(guidExt);

            // 9. Puerta atomica de unicidad de codigo
            FalLoteCorreo lote = loteCorreoRepository.guardarSiAusentePorCodigo(candidato);

            // 10. Detectar si perdio la carrera concurrente
            if (!lote.getId().equals(loteId))
                throw new LoteCodigoDuplicadoException(loteCodigo);

            // 11. Efectos: solo el ganador llega aqui
            aplicarEfectos(lote, validados, ahora, actorTecnico);

            return loteCorreoRepository.buscarPorId(lote.getId()).orElseThrow();
        }
    }


    // =========================================================================
    // Operaciones de estado del lote
    // =========================================================================

    public FalLoteCorreo generarLote(String loteCodigo, String referenciaExterna, String guidLoteExt, String idUser) {
        if (loteCorreoRepository.existeCodigo(loteCodigo))
            throw new LoteCodigoDuplicadoException(loteCodigo);

        LocalDateTime ahora = faltasClock.now();
        Long id = loteCorreoRepository.nextId();
        FalLoteCorreo candidato = new FalLoteCorreo(id, loteCodigo, ahora, ahora, idUser);
        if (referenciaExterna != null) candidato.setReferenciaExterna(referenciaExterna);
        if (guidLoteExt != null) candidato.setGuidLoteExt(guidLoteExt);

        FalLoteCorreo lote = loteCorreoRepository.guardarSiAusentePorCodigo(candidato);
        if (!lote.getId().equals(id))
            throw new LoteCodigoDuplicadoException(loteCodigo);

        return loteCorreoRepository.buscarPorId(id).orElseThrow();
    }

    public FalLoteCorreo emitirLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esEmitible())
            throw new PrecondicionVioladaException(
                    "El lote " + loteId + " no puede emitirse en estado " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.EMITIDO);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo procesarLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esProcesable())
            throw new PrecondicionVioladaException(
                    "El lote " + loteId + " no puede procesarse en estado " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.PROCESADO);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo marcarConError(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (lote.getEstadoLote() == EstadoLote.ANULADO || lote.getEstadoLote() == EstadoLote.PROCESADO)
            throw new PrecondicionVioladaException(
                    "El lote " + loteId + " ya esta en estado final: " + lote.getEstadoLote());
        lote.setEstadoLote(EstadoLote.CON_ERROR);
        loteCorreoRepository.guardar(lote);
        return loteCorreoRepository.buscarPorId(loteId).orElseThrow();
    }

    public FalLoteCorreo anularLote(Long loteId, String idUser) {
        FalLoteCorreo lote = loteCorreoRepository.buscarPorId(loteId)
                .orElseThrow(() -> new LoteCorreoNoEncontradoException(loteId));
        if (!lote.esAnulable())
            throw new PrecondicionVioladaException(
                    "El lote " + loteId + " no puede anularse en estado " + lote.getEstadoLote());
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

    // =========================================================================
    // Internos
    // =========================================================================

    /** Registro interno de par notificacion + acta validados. */
    private record NotifConActa(FalNotificacion notif, FalActa acta) {}

    /**
     * Verifica que la lista derivada no contenga IDs duplicados.
     * Defensivo; en la implementacion canonica la lista proviene de un repositorio
     * que no deberia generar duplicados, pero se valida por invariante.
     */
    private void verificarSinDuplicados(List<FalNotificacion> notifs) {
        Set<Long> ids = new HashSet<>();
        for (FalNotificacion n : notifs) {
            if (!ids.add(n.getId()))
                throw new PrecondicionVioladaException(
                        "ID de notificacion duplicado en lista derivada: " + n.getId());
        }
    }

    /**
     * Valida el dominio de cada notificacion antes de cualquier mutacion:
     *   - estado PENDIENTE_ENVIO
     *   - resultado null
     *   - acta asociada existente
     *   - acta no cerrada
     *   - idDomicilioNotifAct no nulo
     *   - FalPersonaDomicilio referenciado existente
     *
     * No reclama correlativos ni IDs durante esta fase.
     */
    private List<NotifConActa> validarDominio(List<FalNotificacion> notifs) {
        List<NotifConActa> resultado = new ArrayList<>();
        for (FalNotificacion notif : notifs) {
            if (notif.getEstado() != EstadoNotificacion.PENDIENTE_ENVIO)
                throw new PrecondicionVioladaException(
                        "Notificacion " + notif.getId() + " no esta en PENDIENTE_ENVIO. Estado: "
                        + notif.getEstado());
            if (notif.getResultado() != null)
                throw new PrecondicionVioladaException(
                        "Notificacion " + notif.getId() + " ya tiene resultado: " + notif.getResultado());

            FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "Acta no encontrada para notificacion " + notif.getId()
                            + ": actaId=" + notif.getIdActa()));
            if (acta.estaCerrada())
                throw new PrecondicionVioladaException(
                        "Acta " + acta.getId() + " esta cerrada; no se puede incluir en el lote.");
            if (acta.getIdDomicilioNotifAct() == null)
                throw new PrecondicionVioladaException(
                        "Acta " + acta.getId() + " no tiene domicilioNotifAct asignado.");
            domicilioRepository.buscarPorId(acta.getIdDomicilioNotifAct())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "Domicilio " + acta.getIdDomicilioNotifAct()
                            + " referenciado por acta " + acta.getId() + " no encontrado."));
            resultado.add(new NotifConActa(notif, acta));
        }
        return resultado;
    }

    /**
     * Aplica todos los efectos del comando despues de ganar la puerta atomica:
     *  1. Por cada notificacion en orden: crear intento, transicionar cabecera.
     *  2. Por cada acta distinta (orden de primera notificacion): mover a NOTI,
     *     registrar LOTGEN, recalcular snapshot.
     *
     * Usa el mismo instante y actorTecnico en todos los hitos.
     */
    private void aplicarEfectos(FalLoteCorreo lote, List<NotifConActa> validados,
                                 LocalDateTime ahora, String actorTecnico) {

        // --- 1. Intentos y cabeceras (en orden de la lista) ---
        for (NotifConActa na : validados) {
            FalNotificacion notif = na.notif();
            FalActa acta = na.acta();

            short nroIntento = intentoRepository.siguienteNroIntento(notif.getId());
            Long intentoId   = intentoRepository.nextId();

            FalNotificacionIntento intento = new FalNotificacionIntento(
                    intentoId,
                    notif.getId(),
                    nroIntento,
                    CanalNotificacion.CORREO_POSTAL,
                    acta.getIdDomicilioNotifAct(),   // domicilioNotifId real
                    null,                            // destinoDigital
                    lote.getId(),
                    null,                            // referenciaExterna del intento: siempre null
                    ahora, ahora, actorTecnico);
            intentoRepository.guardar(intento);

            notif.iniciarEnvio(CanalNotificacion.CORREO_POSTAL.name(), ahora, ahora, actorTecnico);
            notificacionRepository.guardar(notif);
        }

        // --- 2. Actas distintas en orden de primera notificacion asociada ---
        LinkedHashMap<Long, FalActa> actasAfectadas = new LinkedHashMap<>();
        for (NotifConActa na : validados) {
            actasAfectadas.putIfAbsent(na.acta().getId(), na.acta());
        }

        for (FalActa acta : actasAfectadas.values()) {
            acta.setBloqueActual(BloqueActual.NOTI);
            actaRepository.guardar(acta);

            FalActaEvento evento = FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.LOTGEN)
                    .origenEvt(OrigenEvento.LOTE_CORREO)
                    .fhEvt(ahora)
                    .actorTipo(ActorTipoEvento.NOTIFICADOR)
                    .idUserEvt(actorTecnico)
                    .correlacionId(String.valueOf(lote.getId()))
                    .descripcionLegible("Lote generado: " + lote.getLoteCodigo())
                    .build();
            eventoRepository.registrar(evento);

            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);
        }
    }

    // =========================================================================
    // Normalizacion y validacion de entrada del comando canonico
    // =========================================================================

    private String normalizarActor(String raw) {
        if (raw == null || raw.isBlank())
            throw new PrecondicionVioladaException("actorTecnico es obligatorio.");
        String v = raw.trim();
        if (v.length() > 36)
            throw new PrecondicionVioladaException(
                    "actorTecnico excede 36 caracteres: longitud=" + v.length());
        return v;
    }

    private String normalizarLoteCodigo(String raw) {
        if (raw == null || raw.isBlank())
            throw new PrecondicionVioladaException("loteCodigo es obligatorio.");
        String v = raw.trim();
        if (v.length() > 30)
            throw new PrecondicionVioladaException(
                    "loteCodigo excede 30 caracteres: longitud=" + v.length());
        return v;
    }

    private String normalizarRefExt(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty())
            throw new PrecondicionVioladaException(
                    "referenciaExterna no puede ser cadena vacia.");
        if (v.length() > 60)
            throw new PrecondicionVioladaException(
                    "referenciaExterna excede 60 caracteres: longitud=" + v.length());
        return v;
    }

    private String normalizarGuid(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.length() != 36)
            throw new PrecondicionVioladaException(
                    "guidLoteExt debe tener exactamente 36 caracteres (UUID). Longitud: " + v.length());
        UUID uuid;
        try {
            uuid = UUID.fromString(v);
        } catch (IllegalArgumentException e) {
            throw new PrecondicionVioladaException("guidLoteExt no es un UUID valido: " + v);
        }
        if (!uuid.toString().equalsIgnoreCase(v))
            throw new PrecondicionVioladaException(
                    "guidLoteExt no es un UUID canonico: " + v);
        return uuid.toString();
    }
}
