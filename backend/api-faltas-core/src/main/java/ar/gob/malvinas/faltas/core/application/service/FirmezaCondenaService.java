package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.DeclararCondenaFirmePorApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.command.VencerPlazoApelacionCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmezaCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de firmeza de condena del fallo condenatorio.
 *
 * Slice 3D implementa dos caminos de firmeza:
 *   1. VencerPlazoApelacion: sin apelacion, plazo vencido -> PLAVNC + CONFIR -> CONDENA_FIRME.
 *   2. DeclararFirmePorApelacionRechazada: apelacion RECHAZADA -> CONFIR -> CONDENA_FIRME.
 *
 * Invariantes:
 * - CONDENA_FIRME se asigna solo en este servicio.
 * - No cierra el acta.
 * - No registra CIERRA.
 * - No inicia pago condena.
 * - No genera pago condena ni obligacion de pago.
 * - Pago condena queda para Slice 3E.
 */
@Service
public class FirmezaCondenaService {

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final FirmezaCondenaRepository firmezaCondenaRepository;
    private final SnapshotRecalculador snapshotRecalculador;

        private final FaltasClock faltasClock;

    public FirmezaCondenaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            FirmezaCondenaRepository firmezaCondenaRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        this.faltasClock = faltasClock;
        this.actaRepository = actaRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.firmezaCondenaRepository = firmezaCondenaRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    // -------------------------------------------------------------------------
    // Slice 3D: Vencer plazo de apelacion (condena firme por vencimiento)
    // -------------------------------------------------------------------------

    public ComandoResultado vencerPlazoApelacion(VencerPlazoApelacionCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaOperativa(acta);
        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());
        validarSinCondenaFirme(acta);
        validarSinApelacion(cmd.actaId());

        registrarEvento(cmd.actaId(), TipoEventoActa.PLAVNC, null, null, null,
                "Plazo de apelacion vencido sin presentacion de apelacion. " + nvl(cmd.observaciones()));
        registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, null, null,
                "Condena firme declarada por vencimiento de plazo de apelacion.");

        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        actaRepository.guardar(acta);

        Long idFirmeza = firmezaCondenaRepository.nextId();
        FalActaFirmezaCondena firmeza = new FalActaFirmezaCondena(
                idFirmeza, cmd.actaId(), fallo.getId(), null,
                OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION,
                faltasClock.now(), cmd.observaciones(), true);
        firmezaCondenaRepository.guardar(firmeza);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(idFirmeza),
                TipoEventoActa.CONFIR.codigo(),
                "Condena firme declarada por vencimiento de plazo de apelacion. Pago condena: Slice 3E.");
    }

    // -------------------------------------------------------------------------
    // Slice 3D: Declarar firmeza por apelacion rechazada
    // -------------------------------------------------------------------------

    public ComandoResultado declararFirmePorApelacionRechazada(
            DeclararCondenaFirmePorApelacionRechazadaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaOperativa(acta);
        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());
        validarSinCondenaFirme(acta);

        FalActaApelacion apelacion = apelacionActaRepository.buscarUltima(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe apelacion sobre el acta. Para firmeza por vencimiento de plazo use VencerPlazoApelacionCommand."));

        EstadoApelacionActa estadoAp = apelacion.getEstadoApelacion();
        if (estadoAp == EstadoApelacionActa.PRESENTADA) {
            throw new PrecondicionVioladaException(
                    "La apelacion esta en estado PRESENTADA. Debe resolverse primero (APERAZ) antes de declarar firmeza.");
        }
        boolean apelacionEsRechazo = estadoAp == EstadoApelacionActa.RECHAZADA
                || (estadoAp == EstadoApelacionActa.RESUELTA
                        && apelacion.getResultadoResolucion() == ResultadoResolucionApelacion.RECHAZADA);
        if (!apelacionEsRechazo) {
            throw new PrecondicionVioladaException(
                    "La apelacion no esta RECHAZADA. Estado actual: " + estadoAp
                            + ". Solo la apelacion RECHAZADA habilita este comando.");
        }

        registrarEvento(cmd.actaId(), TipoEventoActa.CONFIR, null, null, null,
                "Condena firme declarada por apelacion rechazada. " + nvl(cmd.observaciones()));

        acta.setResultadoFinal(ResultadoFinalActa.CONDENA_FIRME);
        actaRepository.guardar(acta);

        Long idFirmeza = firmezaCondenaRepository.nextId();
        FalActaFirmezaCondena firmeza = new FalActaFirmezaCondena(
                idFirmeza, cmd.actaId(), fallo.getId(), apelacion.getId(),
                OrigenFirmezaCondena.APELACION_RECHAZADA,
                faltasClock.now(), cmd.observaciones(), true);
        firmezaCondenaRepository.guardar(firmeza);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), String.valueOf(idFirmeza),
                TipoEventoActa.CONFIR.codigo(),
                "Condena firme declarada por apelacion rechazada. Pago condena: Slice 3E.");
    }

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public Optional<FalActaFirmezaCondena> obtenerFirmezaActiva(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return firmezaCondenaRepository.buscarActivaPorActa(actaId);
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

    private void validarSinCondenaFirme(FalActa acta) {
        if (acta.getResultadoFinal() == ResultadoFinalActa.CONDENA_FIRME) {
            throw new PrecondicionVioladaException(
                    "La condena ya fue declarada firme. No se puede repetir la operacion.");
        }
    }

    private void validarSinApelacion(Long actaId) {
        Optional<FalActaApelacion> ultima = apelacionActaRepository.buscarUltima(actaId);
        if (ultima.isEmpty()) return;
        EstadoApelacionActa estado = ultima.get().getEstadoApelacion();
        boolean resueltoRechazado = estado == EstadoApelacionActa.RESUELTA
                && ultima.get().getResultadoResolucion() == ResultadoResolucionApelacion.RECHAZADA;
        if (resueltoRechazado) throw new PrecondicionVioladaException(
                "La apelacion fue rechazada. Para declarar firmeza use DeclararCondenaFirmePorApelacionRechazadaCommand.");
        switch (estado) {
            case PRESENTADA -> throw new PrecondicionVioladaException(
                    "Existe apelacion activa en estado PRESENTADA. No se puede vencer el plazo de apelacion.");
            case RECHAZADA -> throw new PrecondicionVioladaException(
                    "La apelacion fue rechazada. Para declarar firmeza use DeclararCondenaFirmePorApelacionRechazadaCommand.");
            default -> throw new PrecondicionVioladaException(
                    "Existe apelacion sobre el acta (estado: " + estado + "). No aplica vencimiento de plazo.");
        }
    }

    private FalActaFallo obtenerFalloCondenatorioNotificado(Long actaId) {
        FalActaFallo fallo = falloActaRepository.buscarActivo(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe fallo activo sobre el acta. No se puede declarar firmeza."));
        if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO) {
            throw new PrecondicionVioladaException(
                    "Solo se puede declarar firmeza sobre fallo condenatorio. Tipo actual: " + fallo.getTipoFallo());
        }
        if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO) {
            throw new PrecondicionVioladaException(
                    "El fallo condenatorio debe estar NOTIFICADO para declarar firmeza. "
                            + "Estado actual: " + fallo.getEstadoFallo());
        }
        return fallo;
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  Long idDocuRel, Long idNotifRel,
                                  String idUserEvt, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(idActa)
                .tipoEvt(tipo)
                .origenEvt(OrigenEvento.USUARIO_WEB)
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
