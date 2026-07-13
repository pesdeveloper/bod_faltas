package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionNegativaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionVencidaCommand;
import ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoNotificacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
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

import java.time.LocalDate;
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
    private final PlazosAdministrativosService plazosAdministrativosService;

    /**
     * Monitor de exclusion mutua de la variante ordinaria de resultado positivo (CMD-FALLO-004).
     * Serializa lectura, validaciones, calculo de plazo, persistencia, eventos y snapshot dentro
     * de una instancia JVM. No afecta enviar, negativa ni vencida.
     * MariaDB debe reemplazar esta exclusion local por transaccion/OCC.
     */
    private final Object resultadoPositivoMonitor = new Object();

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
            PersonaDomicilioRepository personaDomicilioRepository,
            PlazosAdministrativosService plazosAdministrativosService) {
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
        this.plazosAdministrativosService = plazosAdministrativosService;
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

    /**
     * CMD-FALLO-004 (variante ordinaria). Registra resultado notificatorio positivo sobre un
     * intento concreto. Cubre pieza previa, fallo condenatorio (calcula fhVtoApelacion) y fallo
     * absolutorio (aplica cierre/bloqueantes).
     *
     * Toda lectura de dominio, validacion, calculo de plazo, persistencia, eventos y snapshot
     * ocurren bajo {@code resultadoPositivoMonitor}. La validacion estructural del comando puede
     * ocurrir antes del monitor.
     */
    public ComandoResultado registrarPositiva(RegistrarNotificacionPositivaCommand cmd) {
        // Validacion estructural previa al monitor (puede ocurrir fuera del reloj y del dominio)
        if (cmd == null) throw new IllegalArgumentException("cmd no puede ser null");
        String actor = normalizarActor(cmd.actor());
        if (cmd.idNotificacion() == null)
            throw new PrecondicionVioladaException("idNotificacion es obligatorio");
        if (cmd.intentoId() == null)
            throw new PrecondicionVioladaException("intentoId es obligatorio");

        synchronized (resultadoPositivoMonitor) {
            // 1. cargar FalNotificacion
            FalNotificacion notif = notificacionRepository.buscarPorId(cmd.idNotificacion())
                    .orElseThrow(() -> new NotificacionNoEncontradaException(
                            String.valueOf(cmd.idNotificacion())));

            // 2. cargar FalNotificacionIntento
            FalNotificacionIntento intento = notificacionIntentoRepository.buscarPorId(cmd.intentoId())
                    .orElseThrow(() -> new NotificacionIntentoNoEncontradoException(cmd.intentoId()));

            // 3. intento pertenece a la notificacion
            if (!notif.getId().equals(intento.getNotificacionId()))
                throw new PrecondicionVioladaException(
                        "El intento " + intento.getId() + " no pertenece a la notificacion " + notif.getId() + ".");

            // 4. intento en EN_PROCESO
            if (intento.getEstadoIntento() != EstadoNotificacion.EN_PROCESO)
                throw new PrecondicionVioladaException(
                        "El intento no esta en estado EN_PROCESO. Estado actual: " + intento.getEstadoIntento());

            // 5. intento sin resultado previo
            if (intento.getResultadoIntento() != null)
                throw new PrecondicionVioladaException(
                        "El intento ya tiene resultado registrado: " + intento.getResultadoIntento());

            // 6. cabecera en EN_PROCESO
            if (notif.getEstado() != EstadoNotificacion.EN_PROCESO)
                throw new PrecondicionVioladaException(
                        "La notificacion no esta en estado EN_PROCESO. Estado actual: " + notif.getEstado());

            // 7. cabecera sin resultado previo
            if (notif.getResultado() != null)
                throw new PrecondicionVioladaException(
                        "La notificacion ya tiene resultado registrado: " + notif.getResultado());

            // 8. cargar acta
            FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                    .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

            // 9. acta no cerrada
            if (acta.estaCerrada())
                throw new PrecondicionVioladaException("El acta esta cerrada. No se puede registrar resultado positivo.");

            // 10. consultar fallo activo una sola vez
            Optional<FalActaFallo> falloOpt = falloActaRepository.buscarActivo(acta.getId());

            // 11. determinar variante
            VariantePositiva variante;
            FalActaFallo fallo = null;
            if (falloOpt.isEmpty()
                    || !notif.getIdDocumento().equals(falloOpt.get().getDocumentoId())) {
                variante = VariantePositiva.PIEZA_PREVIA;
            } else {
                FalActaFallo falloActivo = falloOpt.get();
                // 12. validaciones de la variante de fallo
                if (falloActivo.getDocumentoId() == null)
                    throw new PrecondicionVioladaException("El fallo activo no tiene documento asociado.");
                final Long falloDocId = falloActivo.getDocumentoId();
                FalDocumento docFallo = documentoRepository.buscarPorId(falloDocId)
                        .orElseThrow(() -> new DocumentoNoEncontradoException(falloDocId));
                if (!acta.getId().equals(docFallo.getIdActa()))
                    throw new PrecondicionVioladaException("El documento del fallo no pertenece al acta.");
                if (!docFallo.estaFirmado())
                    throw new PrecondicionVioladaException(
                            "El documento del fallo no esta firmado. Estado actual: " + docFallo.getEstadoDocu());
                if (falloActivo.getEstadoFallo() != EstadoFalloActa.PENDIENTE_NOTIFICACION)
                    throw new PrecondicionVioladaException(
                            "El fallo no esta en estado PENDIENTE_NOTIFICACION. Estado actual: " + falloActivo.getEstadoFallo());
                if (falloActivo.getFhFirma() == null)
                    throw new PrecondicionVioladaException("El fallo no tiene fhFirma registrada.");
                if (falloActivo.getFhNotificacion() != null)
                    throw new PrecondicionVioladaException("El fallo ya tiene fhNotificacion registrada.");
                if (falloActivo.isSiFirme())
                    throw new PrecondicionVioladaException("El fallo ya es firme.");
                if (falloActivo.getFhFirmeza() != null)
                    throw new PrecondicionVioladaException("El fallo ya tiene fhFirmeza registrada.");
                if (falloActivo.getOrigenFirmeza() != null)
                    throw new PrecondicionVioladaException("El fallo ya tiene origenFirmeza registrado.");
                // Invariante comun de ambas variantes: el vencimiento de apelacion no puede venir
                // precargado antes del resultado positivo. Para condenatorio se calcula recien despues
                // del positivo; para absolutorio permanece null toda su vida. Un valor previo oculta un
                // estado inconsistente y se rechaza antes del reloj, la clasificacion y toda mutacion.
                if (falloActivo.getFhVtoApelacion() != null)
                    throw new PrecondicionVioladaException(
                            "El fallo ya tiene fhVtoApelacion informado antes de la notificacion positiva: "
                                    + falloActivo.getFhVtoApelacion() + ".");
                // 13. clasificacion explicita con invariante obligatoria de resultadoFallo.
                // No usar el ternario esCondenatorio() ? CONDENATORIO : ABSOLUTORIO: convertiria
                // implicitamente cualquier tipo no condenatorio en absolutorio.
                if (falloActivo.esCondenatorio()) {
                    if (falloActivo.getResultadoFallo() != ResultadoFalloActa.CONDENA)
                        throw new PrecondicionVioladaException(
                                "Fallo condenatorio con resultado incoherente. Esperado CONDENA, actual: "
                                        + falloActivo.getResultadoFallo() + ".");
                    variante = VariantePositiva.FALLO_CONDENATORIO;
                } else if (falloActivo.esAbsolutorio()) {
                    if (falloActivo.getResultadoFallo() != ResultadoFalloActa.ABSUELVE)
                        throw new PrecondicionVioladaException(
                                "Fallo absolutorio con resultado incoherente. Esperado ABSUELVE, actual: "
                                        + falloActivo.getResultadoFallo() + ".");
                    variante = VariantePositiva.FALLO_ABSOLUTORIO;
                } else {
                    throw new PrecondicionVioladaException(
                            "Tipo de fallo no reconocido para notificacion positiva: "
                                    + falloActivo.getTipoFallo() + ".");
                }
                fallo = falloActivo;
            }

            // 15. si absolutorio, consultar bloqueantes antes del reloj
            boolean absolutorioSinBloqueantes = false;
            if (variante == VariantePositiva.FALLO_ABSOLUTORIO) {
                absolutorioSinBloqueantes =
                        !bloqueantesMaterialesChecker.tieneBloqueantesActivos(acta.getId());
            }

            // 16. capturar ahoraPositiva exactamente una vez
            LocalDateTime ahoraPositiva = faltasClock.now();

            // 14. calculo de plazo (solo condenatorio), despues de ahora y antes de mutar.
            // Un unico llamado a calcularVencimientoApelacion; el contrato interno se valida antes
            // de mutar. Si el calculo lanza o incumple el contrato, no hay mutaciones.
            CalculoPlazoAdministrativo calculo = null;
            if (variante == VariantePositiva.FALLO_CONDENATORIO) {
                LocalDate fechaOrigen = ahoraPositiva.toLocalDate();
                calculo = plazosAdministrativosService.calcularVencimientoApelacion(fechaOrigen);
                validarContratoCalculoApelacion(calculo, fechaOrigen);
            }

            // Efectos comunes: intento y cabecera
            aplicarResultadoPositivoIntento(intento, ahoraPositiva, actor);
            aplicarResultadoPositivoCabecera(notif, ahoraPositiva, actor, cmd.observaciones());

            return switch (variante) {
                case PIEZA_PREVIA -> aplicarPiezaPrevia(acta, notif, intento, ahoraPositiva, actor);
                case FALLO_CONDENATORIO -> aplicarFalloCondenatorio(
                        acta, notif, intento, fallo, calculo, ahoraPositiva, actor);
                case FALLO_ABSOLUTORIO -> aplicarFalloAbsolutorio(
                        acta, notif, intento, fallo, ahoraPositiva, actor, absolutorioSinBloqueantes);
            };
        }
    }

    private enum VariantePositiva { PIEZA_PREVIA, FALLO_CONDENATORIO, FALLO_ABSOLUTORIO }

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

    /** Aplica el resultado positivo sobre el intento concreto. */
    private void aplicarResultadoPositivoIntento(
            FalNotificacionIntento intento, LocalDateTime ahoraPositiva, String actor) {
        intento.setResultadoIntento(ResultadoNotificacion.POSITIVO);
        intento.setEstadoIntento(EstadoNotificacion.CON_ACUSE_POSITIVO);
        intento.setFhResultado(ahoraPositiva);
        intento.setFhUltMod(ahoraPositiva);
        intento.setIdUserUltMod(actor);
    }

    /**
     * Aplica el resultado positivo sobre la cabecera. Si el comando informa observaciones,
     * reemplaza; si es null, preserva el valor existente.
     */
    private void aplicarResultadoPositivoCabecera(
            FalNotificacion notif, LocalDateTime ahoraPositiva, String actor, String observaciones) {
        notif.setEstado(EstadoNotificacion.CON_ACUSE_POSITIVO);
        notif.setResultado(ResultadoNotificacion.POSITIVO);
        notif.setFechaResultado(ahoraPositiva);
        notif.setFhUltMod(ahoraPositiva);
        notif.setIdUserUltMod(actor);
        if (observaciones != null) notif.setObservaciones(observaciones);
    }

    private ComandoResultado aplicarPiezaPrevia(
            FalActa acta, FalNotificacion notif, FalNotificacionIntento intento,
            LocalDateTime ahoraPositiva, String actor) {

        // Persistencia: intento, cabecera, acta
        notificacionIntentoRepository.guardar(intento);
        notificacionRepository.guardar(notif);
        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        // Evento NOTPOS con el instante y actor autenticado
        registrarEventoPositivo(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), actor, ahoraPositiva,
                "Notificacion positiva sobre intento " + intento.getId() + ". "
                        + descripcionObservaciones(notif.getObservaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);
        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTPOS.codigo(),
                "Notificacion registrada como positiva. Bloque avanzado a ANAL.");
    }

    private ComandoResultado aplicarFalloCondenatorio(
            FalActa acta, FalNotificacion notif, FalNotificacionIntento intento,
            FalActaFallo fallo, CalculoPlazoAdministrativo calculo,
            LocalDateTime ahoraPositiva, String actor) {

        fallo.marcarNotificado(ahoraPositiva);
        fallo.setFhVtoApelacion(calculo.fechaVencimiento());

        // Persistencia: intento, cabecera, fallo, acta
        notificacionIntentoRepository.guardar(intento);
        notificacionRepository.guardar(notif);
        falloActaRepository.guardar(fallo);
        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepository.guardar(acta);

        registrarEventoPositivo(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), actor, ahoraPositiva,
                "Notificacion positiva de fallo condenatorio sobre intento " + intento.getId()
                        + ". Vencimiento apelacion: " + calculo.fechaVencimiento() + ". "
                        + descripcionObservaciones(notif.getObservaciones()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);
        return ComandoResultado.de(acta.getId(), String.valueOf(notif.getId()),
                TipoEventoActa.NOTPOS.codigo(),
                "Notificacion positiva de fallo condenatorio. Fallo NOTIFICADO. "
                        + "Vencimiento de apelacion: " + calculo.fechaVencimiento() + ".");
    }

    private ComandoResultado aplicarFalloAbsolutorio(
            FalActa acta, FalNotificacion notif, FalNotificacionIntento intento,
            FalActaFallo fallo, LocalDateTime ahoraPositiva, String actor, boolean sinBloqueantes) {

        fallo.marcarNotificado(ahoraPositiva);

        acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);
        if (sinBloqueantes) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
        } else {
            acta.setBloqueActual(BloqueActual.ANAL);
        }

        // Persistencia: intento, cabecera, fallo, acta
        notificacionIntentoRepository.guardar(intento);
        notificacionRepository.guardar(notif);
        falloActaRepository.guardar(fallo);
        actaRepository.guardar(acta);

        registrarEventoPositivo(acta.getId(), TipoEventoActa.NOTPOS, notif.getIdDocumento(),
                notif.getId(), actor, ahoraPositiva,
                "Notificacion positiva de fallo absolutorio sobre intento " + intento.getId() + ". "
                        + descripcionObservaciones(notif.getObservaciones()));

        if (sinBloqueantes) {
            registrarEventoPositivo(acta.getId(), TipoEventoActa.CIERRA, null, null, actor, ahoraPositiva,
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

    private void registrarEventoPositivo(Long idActa, TipoEventoActa tipo,
                                         Long idDocuRel, Long idNotifRel,
                                         String actor, LocalDateTime ahoraPositiva, String descripcion) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(ahoraPositiva)
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(actor)
                .actorTipo(ActorTipoEvento.USUARIO_INTERNO)
                .descripcionLegible(descripcion)
                .build();
        eventoRepository.registrar(evento);
    }

    /**
     * PARTE B: valida el contrato interno del resultado del calculo de plazo de apelacion antes de
     * mutar intento/cabecera/fallo/acta. Una violacion es un error interno del sistema (no del
     * usuario): {@link IllegalStateException}. No recalcula el vencimiento.
     */
    private static void validarContratoCalculoApelacion(
            CalculoPlazoAdministrativo calculo, LocalDate fechaOrigen) {
        if (calculo == null)
            throw new IllegalStateException(
                    "Contrato interno violado: el calculo de plazo de apelacion es null.");
        if (calculo.tipo() != TipoPlazoAdministrativo.APELACION_FALLO)
            throw new IllegalStateException(
                    "Contrato interno violado: tipo de plazo esperado APELACION_FALLO, obtenido: "
                            + calculo.tipo() + ".");
        if (!fechaOrigen.equals(calculo.fechaOrigen()))
            throw new IllegalStateException(
                    "Contrato interno violado: fechaOrigen esperada " + fechaOrigen
                            + ", obtenida: " + calculo.fechaOrigen() + ".");
        if (calculo.fechaVencimiento() == null)
            throw new IllegalStateException(
                    "Contrato interno violado: fechaVencimiento del plazo de apelacion es null.");
    }

    private static String normalizarActor(String actor) {
        if (actor == null || actor.isBlank())
            throw new PrecondicionVioladaException("actor es obligatorio");
        String norm = actor.trim();
        if (norm.length() > 36)
            throw new PrecondicionVioladaException("actor excede 36 caracteres");
        return norm;
    }

    private static String descripcionObservaciones(String observaciones) {
        return observaciones != null ? observaciones : "";
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
