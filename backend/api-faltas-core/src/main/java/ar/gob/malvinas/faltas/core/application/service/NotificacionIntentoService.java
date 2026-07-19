package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.model.CalculoPlazoAdministrativo;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
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
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.LoteCorreoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionIntentoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.NotificacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalLoteCorreo;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacionIntento;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.LoteCorreoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionIntentoRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona los intentos de notificacion.
 * Cada intento tiene un correlativo atomico dentro de su notificacion.
 * Los reintentos crean nuevas filas; nunca se sobrescribe un intento anterior.
 *
 * CMD-FALLO-004 (variante portal): registrarPortalPositivo implementa la variante
 * PORTAL_INFRACTOR completa: pieza previa, fallo condenatorio con fhVtoApelacion,
 * fallo absolutorio con cierre/bloqueantes. Emite NOTSUP/PORPOS/CIERRA en el orden
 * normativo; nunca emite NOTPOS.
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
    private final FaltasClock faltasClock;
    private final FalloActaRepository falloActaRepository;
    private final DocumentoRepository documentoRepository;
    private final BloqueantesMaterialesChecker bloqueantesMaterialesChecker;
    private final PlazosAdministrativosService plazosAdministrativosService;

    // Sin monitor propio: se usa ResultadoPositivoInMemoryMonitor.INSTANCE, compartido con la
    // variante ordinaria (NotificacionService), para serializar tambien ordinario vs portal.

    public NotificacionIntentoService(
            NotificacionIntentoRepository intentoRepository,
            NotificacionRepository notificacionRepository,
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            LoteCorreoRepository loteCorreoRepository,
            FaltasClock faltasClock,
            FalloActaRepository falloActaRepository,
            DocumentoRepository documentoRepository,
            BloqueantesMaterialesChecker bloqueantesMaterialesChecker,
            PlazosAdministrativosService plazosAdministrativosService) {
        this.faltasClock = faltasClock;
        this.intentoRepository = intentoRepository;
        this.notificacionRepository = notificacionRepository;
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.loteCorreoRepository = loteCorreoRepository;
        this.falloActaRepository = falloActaRepository;
        this.documentoRepository = documentoRepository;
        this.bloqueantesMaterialesChecker = bloqueantesMaterialesChecker;
        this.plazosAdministrativosService = plazosAdministrativosService;
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

        LocalDateTime ahora = faltasClock.now();
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
                    notificacionId, id, idUser, ahora,
                    "Intento #" + nroIntento + " canal=" + canal + (loteId != null ? " lote=" + loteId : ""));
            snapshotRecalculador.recalcularYGuardar(acta, ahora);
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

        LocalDateTime ahora = faltasClock.now();
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
                    notificacionId, id, idUser, ahora,
                    "Reintento post vencimiento #" + nroIntento + " canal=" + canal);
            snapshotRecalculador.recalcularYGuardar(acta, ahora);
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

        LocalDateTime ahora = faltasClock.now();
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

    /**
     * CMD-FALLO-004 (variante portal): registra resultado notificatorio positivo mediante
     * portal/QR. Cubre pieza previa (avanza acta a ANAL), fallo condenatorio (marcarNotificado
     * + fhVtoApelacion) y fallo absolutorio (marcarNotificado + cierre/bloqueantes).
     *
     * Emite NOTSUP (si habia intentos activos previos), PORPOS y CIERRA (solo absolutorio sin
     * bloqueantes), en ese orden. Nunca emite NOTPOS.
     *
     * Toda la operacion ocurre bajo {@link ResultadoPositivoInMemoryMonitor#INSTANCE}, compartido
     * con la variante ordinaria (NotificacionService), para serializar ordinario/ordinario,
     * portal/portal y ordinario/portal dentro de una instancia JVM. No resuelve multiples nodos:
     * MariaDB requiere transaccion con OCC o bloqueo adecuado sobre cabecera/intentos
     * y restriccion unica (notificacionId, nroIntento).
     *
     * @param notificacionId    identificador de la notificacion
     * @param actaIdQrEsperada  acta resuelta por el token QR; obtenida del resultado de
     *                          registrarAcceso, nunca del caller como autoridad externa
     * @param destinoPortal     identificador del usuario en el portal; requerido, trim, 1..120
     * @param actor             actor autenticado del flujo QR/portal; requerido, trim, 1..36
     * @return el intento portal creado
     */
    public FalNotificacionIntento registrarPortalPositivo(
            Long notificacionId,
            Long actaIdQrEsperada,
            String destinoPortal,
            String actor) {

        // Prevalidaciones estructurales: antes del monitor y antes del reloj
        if (notificacionId == null)
            throw new PrecondicionVioladaException("notificacionId es obligatorio");

        if (actaIdQrEsperada == null)
            throw new PrecondicionVioladaException("actaIdQrEsperada es obligatorio");

        if (destinoPortal == null)
            throw new PrecondicionVioladaException("destinoPortal es obligatorio");
        String destinoNorm = destinoPortal.trim();
        if (destinoNorm.isEmpty())
            throw new PrecondicionVioladaException("destinoPortal no puede estar vacio");
        if (destinoNorm.length() > 120)
            throw new PrecondicionVioladaException("destinoPortal excede 120 caracteres");

        if (actor == null || actor.isBlank())
            throw new PrecondicionVioladaException("actor es obligatorio");
        String actorNorm = actor.trim();
        if (actorNorm.length() > 36)
            throw new PrecondicionVioladaException("actor excede 36 caracteres");

        synchronized (ResultadoPositivoInMemoryMonitor.INSTANCE) {

            // 1. notificacion existente
            FalNotificacion notif = notificacionRepository.buscarPorId(notificacionId)
                    .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));

            // 1.5. invariante de asociacion QR-acta-notificacion
            if (!actaIdQrEsperada.equals(notif.getIdActa()))
                throw new PrecondicionVioladaException(
                        "La notificacion no pertenece al acta resuelta por el token QR.");

            // 2. cabecera sin resultado POSITIVO previo (rechazo seguro de repeticion)
            if (notif.getResultado() == ResultadoNotificacion.POSITIVO)
                throw new PrecondicionVioladaException(
                        "La notificacion " + notificacionId + " ya tiene resultado POSITIVO.");

            // 3. acta existente
            FalActa acta = actaRepository.buscarPorId(notif.getIdActa())
                    .orElseThrow(() -> new ActaNoEncontradaException(notif.getIdActa()));

            // 4. acta no cerrada
            if (acta.estaCerrada())
                throw new PrecondicionVioladaException(
                        "El acta esta cerrada. No se puede registrar resultado positivo por portal.");

            // 5. consultar fallo activo una sola vez
            Optional<FalActaFallo> falloOpt = falloActaRepository.buscarActivo(acta.getId());

            // 6. determinar variante: pieza previa, condenatorio o absolutorio
            VariantePortal variante;
            FalActaFallo fallo = null;

            if (falloOpt.isEmpty()
                    || !notif.getIdDocumento().equals(falloOpt.get().getDocumentoId())) {
                variante = VariantePortal.PIEZA_PREVIA;
            } else {
                FalActaFallo falloActivo = falloOpt.get();

                // validaciones del documento del fallo
                Long falloDocId = falloActivo.getDocumentoId();
                FalDocumento docFallo = documentoRepository.buscarPorId(falloDocId)
                        .orElseThrow(() -> new DocumentoNoEncontradoException(falloDocId));
                if (!acta.getId().equals(docFallo.getIdActa()))
                    throw new PrecondicionVioladaException(
                            "El documento del fallo no pertenece al acta.");
                if (!docFallo.estaFirmado())
                    throw new PrecondicionVioladaException(
                            "El documento del fallo no esta firmado. Estado actual: " + docFallo.getEstadoDocu());

                // validaciones de estado e hitos del fallo
                if (falloActivo.getEstadoFallo() != EstadoFalloActa.PENDIENTE_NOTIFICACION)
                    throw new PrecondicionVioladaException(
                            "El fallo no esta en estado PENDIENTE_NOTIFICACION. Estado actual: "
                                    + falloActivo.getEstadoFallo());
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
                // invariante comun: fhVtoApelacion debe ser null antes del resultado positivo
                if (falloActivo.getFhVtoApelacion() != null)
                    throw new PrecondicionVioladaException(
                            "El fallo ya tiene fhVtoApelacion informado antes de la notificacion positiva: "
                                    + falloActivo.getFhVtoApelacion() + ".");

                // clasificacion explicita: no convertir implicitamente no-condenatorio en absolutorio
                if (falloActivo.esCondenatorio()) {
                    if (falloActivo.getResultadoFallo() != ResultadoFalloActa.CONDENA)
                        throw new PrecondicionVioladaException(
                                "Fallo condenatorio con resultado incoherente. Esperado CONDENA, actual: "
                                        + falloActivo.getResultadoFallo() + ".");
                    variante = VariantePortal.FALLO_CONDENATORIO;
                } else if (falloActivo.esAbsolutorio()) {
                    if (falloActivo.getResultadoFallo() != ResultadoFalloActa.ABSUELVE)
                        throw new PrecondicionVioladaException(
                                "Fallo absolutorio con resultado incoherente. Esperado ABSUELVE, actual: "
                                        + falloActivo.getResultadoFallo() + ".");
                    variante = VariantePortal.FALLO_ABSOLUTORIO;
                } else {
                    throw new PrecondicionVioladaException(
                            "Tipo de fallo no reconocido para notificacion positiva por portal: "
                                    + falloActivo.getTipoFallo() + ".");
                }
                fallo = falloActivo;
            }

            // 7. identificar intentos activos previos (antes del reloj)
            List<FalNotificacionIntento> intentosActivos = intentoRepository
                    .buscarPorNotificacion(notificacionId)
                    .stream()
                    .filter(i -> !i.tieneResultado())
                    .toList();

            // 8. si absolutorio, consultar bloqueantes antes del reloj
            boolean absolutorioSinBloqueantes = false;
            if (variante == VariantePortal.FALLO_ABSOLUTORIO) {
                absolutorioSinBloqueantes =
                        !bloqueantesMaterialesChecker.tieneBloqueantesActivos(acta.getId());
            }

            // 9. capturar ahoraPortal exactamente una vez, despues de todas las validaciones
            LocalDateTime ahoraPortal = faltasClock.now();

            // 10. calcular plazo (solo condenatorio), despues de ahora y antes de mutar
            CalculoPlazoAdministrativo calculo = null;
            if (variante == VariantePortal.FALLO_CONDENATORIO) {
                LocalDate fechaOrigen = ahoraPortal.toLocalDate();
                calculo = plazosAdministrativosService.calcularVencimientoApelacion(fechaOrigen);
                validarContratoCalculoApelacion(calculo, fechaOrigen);
            }

            // ----------------------------------------------------------------
            // Efectos — orden normativo
            // ----------------------------------------------------------------

            // 11. guardar intentos previos superados
            for (FalNotificacionIntento ia : intentosActivos) {
                ia.setResultadoIntento(ResultadoNotificacion.SUPERADA_POR_PORTAL);
                ia.setEstadoIntento(EstadoNotificacion.SIN_EFECTO);
                ia.setFhResultado(ahoraPortal);
                ia.setFhUltMod(ahoraPortal);
                ia.setIdUserUltMod(actorNorm);
                intentoRepository.guardar(ia);
            }

            // 12. crear y guardar intento portal
            short nroIntento = intentoRepository.siguienteNroIntento(notificacionId);
            Long id = intentoRepository.nextId();
            FalNotificacionIntento intentoPortal = new FalNotificacionIntento(
                    id, notificacionId, nroIntento, CanalNotificacion.PORTAL_INFRACTOR,
                    null, destinoNorm, null, null,
                    ahoraPortal, ahoraPortal, actorNorm);
            intentoPortal.setResultadoIntento(ResultadoNotificacion.POSITIVO);
            intentoPortal.setEstadoIntento(EstadoNotificacion.CON_ACUSE_POSITIVO);
            intentoPortal.setFhResultado(ahoraPortal);
            intentoPortal.setFhUltMod(ahoraPortal);
            intentoPortal.setIdUserUltMod(actorNorm);
            intentoRepository.guardar(intentoPortal);

            // 13. actualizar cabecera positiva
            notif.setEstado(EstadoNotificacion.CON_ACUSE_POSITIVO);
            notif.setResultado(ResultadoNotificacion.POSITIVO);
            notif.setFechaResultado(ahoraPortal);
            notif.setFhUltMod(ahoraPortal);
            notif.setIdUserUltMod(actorNorm);
            notificacionRepository.guardar(notif);

            // 14. efectos por variante
            switch (variante) {
                case PIEZA_PREVIA -> {
                    acta.setBloqueActual(BloqueActual.ANAL);
                    actaRepository.guardar(acta);
                }
                case FALLO_CONDENATORIO -> {
                    fallo.marcarNotificado(ahoraPortal);
                    fallo.setFhVtoApelacion(calculo.fechaVencimiento());
                    falloActaRepository.guardar(fallo);
                    acta.setBloqueActual(BloqueActual.ANAL);
                    actaRepository.guardar(acta);
                }
                case FALLO_ABSOLUTORIO -> {
                    fallo.marcarNotificado(ahoraPortal);
                    acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);
                    if (absolutorioSinBloqueantes) {
                        acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
                        acta.setBloqueActual(BloqueActual.CERR);
                    } else {
                        acta.setBloqueActual(BloqueActual.ANAL);
                    }
                    falloActaRepository.guardar(fallo);
                    actaRepository.guardar(acta);
                }
            }

            // 15. eventos en el orden normativo: NOTSUP (si aplica), PORPOS, CIERRA (si aplica)
            if (!intentosActivos.isEmpty()) {
                registrarEventoPortal(acta.getId(), TipoEventoActa.NOTSUP,
                        notif.getIdDocumento(), notif.getId(), actorNorm, ahoraPortal,
                        "Intentos previos superados por portal infractor");
            }
            registrarEventoPortal(acta.getId(), TipoEventoActa.PORPOS,
                    notif.getIdDocumento(), notif.getId(), actorNorm, ahoraPortal,
                    "Notificacion positiva por portal infractor");
            if (variante == VariantePortal.FALLO_ABSOLUTORIO && absolutorioSinBloqueantes) {
                registrarEventoPortal(acta.getId(), TipoEventoActa.CIERRA,
                        null, null, actorNorm, ahoraPortal,
                        "Acta cerrada. Fallo absolutorio notificado via portal sin bloqueantes.");
            }

            // 16. recalcular y guardar snapshot
            snapshotRecalculador.recalcularYGuardar(acta, ahoraPortal);

            return intentoRepository.buscarPorId(id).orElseThrow();
        }
    }

    public List<FalNotificacionIntento> obtenerIntentos(Long notificacionId) {
        notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new NotificacionNoEncontradaException(String.valueOf(notificacionId)));
        return intentoRepository.buscarPorNotificacion(notificacionId);
    }

    private enum VariantePortal { PIEZA_PREVIA, FALLO_CONDENATORIO, FALLO_ABSOLUTORIO }

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

    /**
     * Registra un evento de notificacion con el instante canonico ya capturado.
     * Usado por registrarIntento y registrarReintentoPorVencimiento donde el instante
     * es el propio de cada metodo.
     */
    private void registrarEvento(Long idActa, TipoEventoActa tipo, Long idNotifRel, Long idIntento, String idUser, LocalDateTime ahora, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.SERVICIO_NOTIFICACION)
                .fhEvt(ahora)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUser)
                .actorTipo(ActorTipoEvento.NOTIFICADOR)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

    /**
     * Registra un evento del flujo portal con el instante canonico {@code ahora} ya capturado.
     * Garantiza que NOTSUP, PORPOS y CIERRA compartan exactamente el mismo ahoraPortal.
     */
    private void registrarEventoPortal(Long idActa, TipoEventoActa tipo,
                                       Long idDocuRel, Long idNotifRel,
                                       String actor, LocalDateTime ahora, String descripcion) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.PORTAL_INFRACTOR)
                .fhEvt(ahora)
                .idDocuRel(idDocuRel)
                .idNotifRel(idNotifRel)
                .idUserEvt(actor)
                .actorTipo(ActorTipoEvento.INFRACTOR)
                .descripcionLegible(descripcion)
                .build();
        eventoRepository.registrar(evento);
    }

    /**
     * Valida el contrato interno del resultado del calculo de plazo de apelacion.
     * Una violacion es un error interno del sistema (no del usuario): IllegalStateException.
     * No recalcula el vencimiento ni muta ningun estado.
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
}
