package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de firmeza de condena del fallo condenatorio.
 *
 * Dos caminos de firmeza:
 *   CMD-FALLO-005: VencerPlazoApelacion — sin apelacion, plazo vencido -> PLAVNC + CONFIR -> CONDENA_FIRME.
 *   CMD-FALLO-006: DeclararFirmePorApelacionRechazada — apelacion RECHAZADA -> CONFIR -> CONDENA_FIRME.
 *
 * El monitor firmezaMonitor garantiza exclusion mutua InMemory entre ambos caminos.
 * MariaDB requiere transaccion, OCC o bloqueo para garantias multinodo.
 *
 * Invariantes:
 * - La firmeza vive exclusivamente en FalActaFallo (no existe tabla paralela).
 * - CONDENA_FIRME se asigna solo en este servicio.
 * - No cierra el acta ni registra CIERRA.
 * - No inicia ni informa pago condena.
 */
@Service
public class FirmezaCondenaService {

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FaltasClock faltasClock;
    private final Object firmezaMonitor = new Object();

    public FirmezaCondenaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    // -------------------------------------------------------------------------
    // CMD-FALLO-005: Vencer plazo de apelacion (condena firme por vencimiento)
    // -------------------------------------------------------------------------

    public ComandoResultado vencerPlazoApelacion(VencerPlazoApelacionCommand cmd) {
        if (cmd == null) throw new IllegalArgumentException("cmd requerido");
        if (cmd.actaId() == null) throw new PrecondicionVioladaException("actaId requerido");
        if (cmd.actor() == null) throw new PrecondicionVioladaException("actor requerido");
        String actor = cmd.actor().trim();
        if (actor.isEmpty()) throw new PrecondicionVioladaException("actor no puede ser vacio");
        if (actor.length() > 36) throw new PrecondicionVioladaException("actor max 36 caracteres");

        synchronized (firmezaMonitor) {
            // 1. Cargar acta
            FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                    .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

            // 2. Validar acta operativa
            validarActaOperativa(acta);

            // 3. Cargar fallo activo (una sola vez)
            FalActaFallo fallo = falloActaRepository.buscarActivo(cmd.actaId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe fallo activo sobre el acta. No se puede declarar firmeza."));

            // 4. CONDENATORIO
            if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO)
                throw new PrecondicionVioladaException(
                        "Solo se puede declarar firmeza sobre fallo condenatorio. Tipo actual: " + fallo.getTipoFallo());

            // 5. resultadoFallo = CONDENA
            if (fallo.getResultadoFallo() != ResultadoFalloActa.CONDENA)
                throw new PrecondicionVioladaException(
                        "El fallo debe tener resultado CONDENA. Resultado actual: " + fallo.getResultadoFallo());

            // 6. estadoFallo = NOTIFICADO
            if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO)
                throw new PrecondicionVioladaException(
                        "El fallo condenatorio debe estar NOTIFICADO para declarar firmeza. Estado actual: "
                                + fallo.getEstadoFallo());

            // 7. fhFirma != null
            if (fallo.getFhFirma() == null)
                throw new PrecondicionVioladaException(
                        "fhFirma ausente en el fallo. El fallo debe haber sido firmado.");

            // 8. fhNotificacion != null
            if (fallo.getFhNotificacion() == null)
                throw new PrecondicionVioladaException(
                        "fhNotificacion ausente en el fallo. El fallo debe haber sido notificado.");

            // 9. siFirme = false
            if (fallo.isSiFirme())
                throw new PrecondicionVioladaException(
                        "La condena ya fue declarada firme. No se puede repetir la operacion.");

            // 10. fhFirmeza = null
            if (fallo.getFhFirmeza() != null)
                throw new PrecondicionVioladaException(
                        "fhFirmeza ya registrado en el fallo. La firmeza ya fue declarada.");

            // 11. origenFirmeza = null
            if (fallo.getOrigenFirmeza() != null)
                throw new PrecondicionVioladaException(
                        "origenFirmeza ya registrado en el fallo. La firmeza ya fue declarada.");

            // 12. resultadoFinal != CONDENA_FIRME
            if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME)
                throw new PrecondicionVioladaException(
                        "La condena ya fue declarada firme. No se puede repetir la operacion.");

            // 13-14. Consultar apelacion exclusivamente por fallo.id
            Optional<FalActaApelacion> apelacionOpt = apelacionActaRepository.buscarPorFallo(fallo.getId());
            if (apelacionOpt.isPresent()) {
                FalActaApelacion apelacion = apelacionOpt.get();
                EstadoApelacionActa estadoAp = apelacion.getEstadoApelacion();
                boolean esRechazo = estadoAp == EstadoApelacionActa.RECHAZADA
                        || (estadoAp == EstadoApelacionActa.RESUELTA
                                && apelacion.getResultadoResolucion() == ResultadoResolucionApelacion.RECHAZADA);
                if (estadoAp == EstadoApelacionActa.PRESENTADA) {
                    throw new PrecondicionVioladaException(
                            "Existe apelacion activa en estado PRESENTADA asociada al fallo activo. "
                                    + "No se puede vencer el plazo de apelacion.");
                }
                if (esRechazo) {
                    throw new PrecondicionVioladaException(
                            "La apelacion fue rechazada. Para declarar firmeza use CMD-FALLO-006 "
                                    + "(DeclararCondenaFirmePorApelacionRechazadaCommand).");
                }
                throw new PrecondicionVioladaException(
                        "Existe apelacion asociada al fallo activo (estado: " + estadoAp
                                + "). No aplica vencimiento de plazo.");
            }

            // 15. fhVtoApelacion != null
            if (fallo.getFhVtoApelacion() == null)
                throw new PrecondicionVioladaException(
                        "fhVtoApelacion no calculado (null). El fallo debe haber sido notificado con calculo de plazo.");

            // 16. Capturar ahora exactamente una vez
            LocalDateTime ahora = faltasClock.now();

            // 17. Validar vencimiento
            LocalDate fechaActual = ahora.toLocalDate();
            LocalDate fechaVencimiento = fallo.getFhVtoApelacion();
            if (!fechaActual.isAfter(fechaVencimiento))
                throw new PrecondicionVioladaException(
                        "El plazo de apelacion no ha vencido. Fecha actual: " + fechaActual
                                + ", ultimo dia apelable: " + fechaVencimiento
                                + ". El plazo vence a partir del dia siguiente al ultimo dia habilitado.");

            // Efectos
            fallo.declararFirmeza(ahora, OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
            falloActaRepository.guardar(fallo);

            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaRepository.guardar(acta);

            String descPlavnc = "Plazo de apelacion vencido sin presentacion de apelacion.";
            if (cmd.observaciones() != null) {
                descPlavnc += " " + cmd.observaciones();
            }
            registrarEvento(cmd.actaId(), TipoEventoActa.PLAVNC, null, actor, descPlavnc, ahora);
            registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, actor,
                    "Condena firme declarada por vencimiento de plazo de apelacion.", ahora);

            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);

            return ComandoResultado.de(cmd.actaId(), String.valueOf(fallo.getId()),
                    TipoEventoActa.CONFIR.codigo(),
                    "Condena firme declarada por vencimiento de plazo de apelacion.");
        }
    }

    // -------------------------------------------------------------------------
    // CMD-FALLO-006: Declarar firmeza por apelacion rechazada
    // -------------------------------------------------------------------------

    public ComandoResultado declararFirmePorApelacionRechazada(
            DeclararCondenaFirmePorApelacionRechazadaCommand cmd) {
        if (cmd == null) throw new IllegalArgumentException("cmd requerido");
        if (cmd.actaId() == null) throw new PrecondicionVioladaException("actaId requerido");
        if (cmd.actor() == null) throw new PrecondicionVioladaException("actor requerido");
        String actor = cmd.actor().trim();
        if (actor.isEmpty()) throw new PrecondicionVioladaException("actor no puede ser vacio");
        if (actor.length() > 36) throw new PrecondicionVioladaException("actor max 36 caracteres");

        synchronized (firmezaMonitor) {
            // 1. Cargar acta
            FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                    .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

            // 2. Validar acta operativa
            validarActaOperativa(acta);

            // 3. Cargar fallo activo una sola vez
            FalActaFallo fallo = falloActaRepository.buscarActivo(cmd.actaId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe fallo activo sobre el acta. No se puede declarar firmeza."));

            // 4. CONDENATORIO
            if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO)
                throw new PrecondicionVioladaException(
                        "Solo se puede declarar firmeza sobre fallo condenatorio. Tipo actual: " + fallo.getTipoFallo());

            // 5. resultadoFallo = CONDENA
            if (fallo.getResultadoFallo() != ResultadoFalloActa.CONDENA)
                throw new PrecondicionVioladaException(
                        "El fallo debe tener resultado CONDENA. Resultado actual: " + fallo.getResultadoFallo());

            // 6. estadoFallo = NOTIFICADO
            if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO)
                throw new PrecondicionVioladaException(
                        "El fallo condenatorio debe estar NOTIFICADO para declarar firmeza. Estado actual: "
                                + fallo.getEstadoFallo());

            // 7. fhFirma != null
            if (fallo.getFhFirma() == null)
                throw new PrecondicionVioladaException(
                        "fhFirma ausente en el fallo. El fallo debe haber sido firmado.");

            // 8. fhNotificacion != null
            if (fallo.getFhNotificacion() == null)
                throw new PrecondicionVioladaException(
                        "fhNotificacion ausente en el fallo. El fallo debe haber sido notificado.");

            // 9. siFirme = false
            if (fallo.isSiFirme())
                throw new PrecondicionVioladaException(
                        "La condena ya fue declarada firme. No se puede repetir la operacion.");

            // 10. fhFirmeza = null
            if (fallo.getFhFirmeza() != null)
                throw new PrecondicionVioladaException(
                        "fhFirmeza ya registrado en el fallo. La firmeza ya fue declarada.");

            // 11. origenFirmeza = null
            if (fallo.getOrigenFirmeza() != null)
                throw new PrecondicionVioladaException(
                        "origenFirmeza ya registrado en el fallo. La firmeza ya fue declarada.");

            // 12. resultadoFinal != CONDENA_FIRME
            if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME)
                throw new PrecondicionVioladaException(
                        "La condena ya fue declarada firme. No se puede repetir la operacion.");

            // 13. Cargar apelacion exclusivamente por fallo.id
            FalActaApelacion apelacion = apelacionActaRepository.buscarPorFallo(fallo.getId())
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "No existe apelacion asociada al fallo activo. Para firmeza por vencimiento de plazo use VencerPlazoApelacionCommand."));

            // 14. apelacion.actaId = acta.id
            if (!acta.getId().equals(apelacion.getActaId()))
                throw new PrecondicionVioladaException(
                        "La apelacion no pertenece al acta indicada.");

            // 15. apelacion.falloId = fallo.id
            if (!fallo.getId().equals(apelacion.getFalloId()))
                throw new PrecondicionVioladaException(
                        "La apelacion no pertenece al fallo activo.");

            // 16. Validar resultado rechazado
            EstadoApelacionActa estadoAp = apelacion.getEstadoApelacion();
            boolean apelacionEsRechazo = estadoAp == EstadoApelacionActa.RECHAZADA
                    || (estadoAp == EstadoApelacionActa.RESUELTA
                            && apelacion.getResultadoResolucion() == ResultadoResolucionApelacion.RECHAZADA);
            if (!apelacionEsRechazo) {
                throw new PrecondicionVioladaException(
                        "La apelacion no esta en estado RECHAZADA. Estado actual: " + estadoAp
                                + (estadoAp == EstadoApelacionActa.RESUELTA
                                        ? ", resultado: " + apelacion.getResultadoResolucion()
                                        : "")
                                + ". Solo RECHAZADA o RESUELTA con resultado RECHAZADA habilita este comando.");
            }

            // 17. Capturar ahora exactamente una vez
            LocalDateTime ahora = faltasClock.now();

            fallo.declararFirmeza(ahora, OrigenFirmezaCondena.APELACION_RECHAZADA);
            falloActaRepository.guardar(fallo);

            acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
            actaRepository.guardar(acta);

            String descConfir = "Condena firme declarada por apelacion rechazada.";
            if (cmd.observaciones() != null && !cmd.observaciones().isBlank()) {
                descConfir += " " + cmd.observaciones().trim();
            }
            registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, actor, descConfir, ahora);

            FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
            snapshotRepository.guardar(snap);

            return ComandoResultado.de(cmd.actaId(), String.valueOf(fallo.getId()),
                    TipoEventoActa.CONFIR.codigo(),
                    "Condena firme declarada por apelacion rechazada.");
        }
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private void validarActaOperativa(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA)
            throw new PrecondicionVioladaException("El acta esta cerrada.");
        if (sit == SituacionAdministrativaActa.ANULADA)
            throw new PrecondicionVioladaException("El acta esta anulada.");
        if (sit == SituacionAdministrativaActa.ARCHIVADA)
            throw new PrecondicionVioladaException("El acta esta archivada.");
        if (sit == SituacionAdministrativaActa.PARALIZADA)
            throw new PrecondicionVioladaException("El acta esta paralizada.");
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idNotifRel, String idUserEvt,
                                  String descripcionLegible, LocalDateTime fhEvt) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(fhEvt)
                .idNotifRel(idNotifRel)
                .idUserEvt(idUserEvt)
                .actorTipo(idUserEvt != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        eventoRepository.registrar(evento);
    }

}
