package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
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
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Motor de proceso del circuito notificatorio del expediente.
 *
 * Enviar, registrar positiva, registrar negativa, registrar vencida.
 *
 * CMD-FALLO-002: enviarNotificacion usa CanalNotificacion canonico, obtiene el actor
 * exclusivamente del JWT sub (via EnviarNotificacionCommand.actor), persiste exactamente
 * un FalNotificacionIntento por llamada exitosa y usa un unico instante canonico para
 * cabecera, intento y evento NOTENV.
 */
@Service
public class NotificacionService {

    private final ActaRepository actaRepository;
    private final DocumentoRepository documentoRepository;
    private final NotificacionRepository notificacionRepository;
    private final NotificacionIntentoRepository notificacionIntentoRepository;
    private final PersonaDomicilioRepository personaDomicilioRepository;
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
            FaltasClock faltasClock,
            NotificacionIntentoRepository notificacionIntentoRepository,
            PersonaDomicilioRepository personaDomicilioRepository) {
        this.actaRepository = actaRepository;
        this.documentoRepository = documentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.falloActaRepository = falloActaRepository;
        this.bloqueantesMaterialesChecker = bloqueantesMaterialesChecker;
        this.faltasClock = faltasClock;
        this.notificacionIntentoRepository = notificacionIntentoRepository;
        this.personaDomicilioRepository = personaDomicilioRepository;
    }

    // -------------------------------------------------------------------------
    // CMD-FALLO-002 EnviarNotificacion
    // -------------------------------------------------------------------------

    public ComandoResultado enviarNotificacion(EnviarNotificacionCommand cmd) {
        // 1. cmd no nulo
        if (cmd == null) throw new IllegalArgumentException("cmd no puede ser null");

        // 2. actor no nulo, no blanco, maximo 36
        String actor = cmd.actor();
        if (actor == null || actor.isBlank())
            throw new PrecondicionVioladaException("actor es obligatorio");
        if (actor.length() > 36)
            throw new PrecondicionVioladaException("actor excede 36 caracteres");

        // 3. acta existente
        FalActa acta = actaRepository.buscarPorId(cmd.idActa())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.idActa()));

        // 4. acta no cerrada
        if (acta.estaCerrada())
            throw new PrecondicionVioladaException(
                    "El acta esta cerrada. No se puede enviar notificacion.");

        // 5. documento existente
        FalDocumento doc = documentoRepository.buscarPorId(cmd.idDocumento())
                .orElseThrow(() -> new DocumentoNoEncontradoException(cmd.idDocumento()));

        // 6. documento firmado
        if (!doc.estaFirmado())
            throw new PrecondicionVioladaException(
                    "No se puede notificar un documento que no esta firmado. Estado actual: "
                            + doc.getEstadoDocu());

        // 7. documento pertenece al acta
        if (!acta.getId().equals(doc.getIdActa()))
            throw new PrecondicionVioladaException(
                    "El documento no pertenece al acta indicada.");

        // 8. canal no nulo
        CanalNotificacion canal = cmd.canal();
        if (canal == null)
            throw new PrecondicionVioladaException("canal es obligatorio");

        // 9. validar y normalizar destino segun canal
        if (canal == CanalNotificacion.PORTAL_INFRACTOR)
            throw new PrecondicionVioladaException(
                    "PORTAL_INFRACTOR se procesa exclusivamente mediante su comando especifico.");

        Long domicilioNotifId = null;
        String destinoDigitalNorm = null;

        if (canal.requiereDomicilioFisico()) {
            if (cmd.destinoDigital() != null)
                throw new PrecondicionVioladaException(
                        "destinoDigital debe ser null para canal " + canal + ".");
            Long idDomicilio = acta.getIdDomicilioNotifAct();
            if (idDomicilio == null)
                throw new PrecondicionVioladaException(
                        "El acta no tiene domicilio de notificacion asignado (idDomicilioNotifAct null).");
            personaDomicilioRepository.buscarPorId(idDomicilio)
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "Domicilio de notificacion " + idDomicilio + " no encontrado."));
            domicilioNotifId = idDomicilio;
        } else if (canal.esDigital()) {
            String dest = cmd.destinoDigital();
            if (dest == null)
                throw new PrecondicionVioladaException(
                        "destinoDigital es obligatorio para canal EMAIL.");
            String destTrim = dest.trim();
            if (destTrim.isEmpty())
                throw new PrecondicionVioladaException(
                        "destinoDigital no puede estar vacio para canal EMAIL.");
            if (destTrim.length() > 120)
                throw new PrecondicionVioladaException(
                        "destinoDigital excede 120 caracteres.");
            destinoDigitalNorm = destTrim;
        } else if (canal == CanalNotificacion.PRESENCIAL) {
            if (cmd.destinoDigital() != null)
                throw new PrecondicionVioladaException(
                        "destinoDigital debe ser null para canal PRESENCIAL.");
        }

        // 10. validar y normalizar referenciaExterna (precheck antes de ahora)
        String refExtNorm = null;
        if (cmd.referenciaExterna() != null) {
            String refTrim = cmd.referenciaExterna().trim();
            if (refTrim.isEmpty())
                throw new PrecondicionVioladaException(
                        "referenciaExterna no puede estar vacia.");
            if (refTrim.length() > 80)
                throw new PrecondicionVioladaException(
                        "referenciaExterna excede 80 caracteres.");
            if (notificacionIntentoRepository.buscarPorReferenciaExterna(refTrim).isPresent())
                throw new PrecondicionVioladaException(
                        "referenciaExterna ya existe: " + refTrim);
            refExtNorm = refTrim;
        }

        // 11. consultar cabecera activa exactamente una vez
        Optional<FalNotificacion> activaOpt =
                notificacionRepository.buscarActivaPorDocumento(doc.getId());

        // 12. seleccionar rama: sin cabecera -> CREAR; PENDIENTE_ENVIO -> REUTILIZAR; otro -> rechazar
        boolean crear;
        if (activaOpt.isEmpty()) {
            crear = true;
        } else {
            FalNotificacion activa = activaOpt.get();
            if (activa.getEstado() == EstadoNotificacion.PENDIENTE_ENVIO) {
                crear = false;
            } else {
                throw new PrecondicionVioladaException(
                        "Ya existe una notificacion activa en estado " + activa.getEstado()
                                + " para el documento " + doc.getId()
                                + ". No se puede crear una segunda notificacion activa.");
            }
        }

        // 13. capturar ahora exactamente una vez
        LocalDateTime ahora = faltasClock.now();

        // Reclamar referencia externa atomicamente (despues de ahora, antes de IDs y mutaciones)
        if (refExtNorm != null) {
            boolean claimed = notificacionIntentoRepository.claimReferenciaExterna(refExtNorm);
            if (!claimed)
                throw new PrecondicionVioladaException(
                        "referenciaExterna ya tomada concurrentemente: " + refExtNorm);
        }

        final String refExtFinal = refExtNorm;
        final Long domicilioFinal = domicilioNotifId;
        final String destinoFinal = destinoDigitalNorm;

        FalNotificacion notif;

        if (crear) {
            // Rama CREAR
            Long notificacionId = notificacionRepository.nextId();
            Long intentoId = notificacionIntentoRepository.nextId();
            short nroIntento = notificacionIntentoRepository.siguienteNroIntento(notificacionId);
            if (nroIntento != 1)
                throw new IllegalStateException(
                        "Integridad comprometida: nroIntento esperado 1, obtenido: " + nroIntento);

            FalNotificacion cabecera = new FalNotificacion(
                    notificacionId, acta.getId(), doc.getId(), doc.getTipoDocu(),
                    canal.name(), ahora, ahora, actor);
            cabecera.setFhUltMod(ahora);
            cabecera.setIdUserUltMod(actor);
            if (cmd.observaciones() != null) cabecera.setObservaciones(cmd.observaciones());

            FalNotificacionIntento intento = new FalNotificacionIntento(
                    intentoId, notificacionId, nroIntento, canal,
                    domicilioFinal, destinoFinal, null, refExtFinal,
                    ahora, ahora, actor);

            notificacionRepository.guardar(cabecera);
            notificacionIntentoRepository.guardar(intento);
            notif = cabecera;
        } else {
            // Rama REUTILIZAR
            FalNotificacion cabecera = activaOpt.get();
            Long intentoId = notificacionIntentoRepository.nextId();
            short nroIntento = notificacionIntentoRepository.siguienteNroIntento(cabecera.getId());

            FalNotificacionIntento intento = new FalNotificacionIntento(
                    intentoId, cabecera.getId(), nroIntento, canal,
                    domicilioFinal, destinoFinal, null, refExtFinal,
                    ahora, ahora, actor);

            cabecera.iniciarEnvio(canal.name(), ahora, ahora, actor);
            if (cmd.observaciones() != null) cabecera.setObservaciones(cmd.observaciones());

            notificacionRepository.guardar(cabecera);
            notificacionIntentoRepository.guardar(intento);
            notif = cabecera;
        }

        acta.setBloqueActual(BloqueActual.NOTI);
        actaRepository.guardar(acta);

        // Emitir exactamente un NOTENV con el instante canonico y el actor real
        registrarEventoNotenv(acta.getId(), doc.getId(), notif.getId(), actor, ahora,
                "Notificacion enviada. Canal: " + canal.name());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTENV.codigo(),
                "Notificacion enviada. Canal: " + canal.name());
    }

    // -------------------------------------------------------------------------
    // RegistrarNotificacionPositiva
    // -------------------------------------------------------------------------

    public ComandoResultado registrarPositiva(RegistrarNotificacionPositivaCommand cmd) {
        FalNotificacion notif = notificacionRepository.buscarPorId(cmd.idNotificacion())
                .orElseThrow(() -> new NotificacionNoEncontradaException(
                        String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null)
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());

        LocalDateTime ahoraPositiva = faltasClock.now();
        notif.setEstado(EstadoNotificacion.CON_ACUSE_POSITIVO);
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notif.setFechaResultado(ahoraPositiva);
        if (cmd.observaciones() != null) notif.setObservaciones(cmd.observaciones());
        notificacionRepository.guardar(notif);

        FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

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
                .orElseThrow(() -> new NotificacionNoEncontradaException(
                        String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null)
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());

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
                .orElseThrow(() -> new NotificacionNoEncontradaException(
                        String.valueOf(cmd.idNotificacion())));

        if (notif.getResultado() != null)
            throw new PrecondicionVioladaException(
                    "La notificacion ya tiene resultado registrado: " + notif.getResultado());

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

    /**
     * Registra el evento NOTENV usando el instante canonico ya capturado y el actor real del JWT.
     * Garantiza que cabecera, intento y evento compartan exactamente el mismo instante.
     */
    private void registrarEventoNotenv(Long idActa, Long idDocuRel, Long idNotifRel,
                                        String actor, LocalDateTime ahora, String descripcion) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(TipoEventoActa.NOTENV)
                .origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(ahora)
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(actor)
                .actorTipo(ActorTipoEvento.USUARIO_INTERNO)
                .descripcionLegible(descripcion)
                .build();
        eventoRepository.registrar(evento);
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

    private static String nvl(String s) { return s != null ? s : ""; }
}
