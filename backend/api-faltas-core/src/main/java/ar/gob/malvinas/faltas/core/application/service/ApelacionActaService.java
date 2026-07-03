package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionAceptaAbsuelveCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.port.BloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
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
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de apelacion del fallo condenatorio.
 *
 * Slice 3B: registrarApelacion (APEPRE).
 * Slice 3C: resolverRechazada (APERAZ) y resolverAceptaAbsuelve (APEABS).
 * No implementa firmeza ni pago condena: Slice futuro.
 *
 * Invariantes:
 * - Apelacion solo sobre fallo CONDENATORIO NOTIFICADO.
 * - No existe APELAC como evento.
 * - Registrar apelacion no cierra el acta.
 * - Rechazar apelacion no genera CONFIR ni CONDENA_FIRME.
 * - Aceptar apelacion que absuelve cierra solo si no hay bloqueantes activos.
 */
@Service
public class ApelacionActaService {

    private final ActaRepository actaRepository;
    private final FalloActaRepository falloActaRepository;
    private final ApelacionActaRepository apelacionActaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final BloqueantesMaterialesChecker bloqueantesChecker;

    public ApelacionActaService(
            ActaRepository actaRepository,
            FalloActaRepository falloActaRepository,
            ApelacionActaRepository apelacionActaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            BloqueantesMaterialesChecker bloqueantesChecker) {
        this.actaRepository = actaRepository;
        this.falloActaRepository = falloActaRepository;
        this.apelacionActaRepository = apelacionActaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.bloqueantesChecker = bloqueantesChecker;
    }

    // -------------------------------------------------------------------------
    // Slice 3B: Registrar apelacion
    // -------------------------------------------------------------------------

    public ComandoResultado registrarApelacion(RegistrarApelacionCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaAbierta(acta);

        FalActaFallo fallo = obtenerFalloCondenatorioNotificado(cmd.actaId());

        if (apelacionActaRepository.buscarActiva(cmd.actaId()).isPresent()) {
            throw new PrecondicionVioladaException(
                    "Ya existe una apelacion activa para esta acta. No se puede registrar otra.");
        }

        String idApelacion = UUID.randomUUID().toString();
        FalActaApelacion apelacion = new FalActaApelacion(
                idApelacion,
                cmd.actaId(),
                fallo.getId(),
                EstadoApelacionActa.PRESENTADA,
                LocalDateTime.now(),
                cmd.presentante(),
                cmd.fundamentos(),
                cmd.observaciones(),
                true
        );
        apelacionActaRepository.guardar(apelacion);

        registrarEvento(cmd.actaId(), TipoEventoActa.APEPRE, null, null, null,
                "Apelacion presentada por: " + nvl(cmd.presentante())
                        + ". " + nvl(cmd.fundamentos()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), idApelacion,
                TipoEventoActa.APEPRE.codigo(),
                "Apelacion registrada. Pendiente resolucion.");
    }

    // -------------------------------------------------------------------------
    // Slice 3C: Rechazar apelacion (APERAZ)
    // -------------------------------------------------------------------------

    public ComandoResultado resolverRechazada(ResolverApelacionRechazadaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaAbierta(acta);
        obtenerFalloCondenatorioNotificado(cmd.actaId());

        FalActaApelacion apelacion = apelacionActaRepository.buscarActiva(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe apelacion activa para esta acta."));

        if (apelacion.getEstadoApelacion() != EstadoApelacionActa.PRESENTADA) {
            throw new PrecondicionVioladaException(
                    "La apelacion no esta en estado PRESENTADA. Estado actual: "
                            + apelacion.getEstadoApelacion());
        }

        apelacion.setEstadoApelacion(EstadoApelacionActa.RECHAZADA);
        apelacion.setFechaResolucion(LocalDateTime.now());
        apelacion.setFundamentosResolucion(cmd.fundamentosResolucion());
        apelacion.setObservacionesResolucion(cmd.observaciones());
        apelacion.setSiActiva(false);
        apelacionActaRepository.guardar(apelacion);

        registrarEvento(cmd.actaId(), TipoEventoActa.APERAZ, null, null, null,
                "Apelacion rechazada. Fundamentos: " + nvl(cmd.fundamentosResolucion()));

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(cmd.actaId(), apelacion.getId(),
                TipoEventoActa.APERAZ.codigo(),
                "Apelacion rechazada. Pendiente declaracion de firmeza de condena (Slice futuro).");
    }

    // -------------------------------------------------------------------------
    // Slice 3C: Aceptar apelacion que absuelve (APEABS)
    // -------------------------------------------------------------------------

    public ComandoResultado resolverAceptaAbsuelve(ResolverApelacionAceptaAbsuelveCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarActaAbierta(acta);
        obtenerFalloCondenatorioNotificado(cmd.actaId());

        FalActaApelacion apelacion = apelacionActaRepository.buscarActiva(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe apelacion activa para esta acta."));

        if (apelacion.getEstadoApelacion() != EstadoApelacionActa.PRESENTADA) {
            throw new PrecondicionVioladaException(
                    "La apelacion no esta en estado PRESENTADA. Estado actual: "
                            + apelacion.getEstadoApelacion());
        }

        apelacion.setEstadoApelacion(EstadoApelacionActa.ACEPTADA_ABSUELVE);
        apelacion.setFechaResolucion(LocalDateTime.now());
        apelacion.setFundamentosResolucion(cmd.fundamentosResolucion());
        apelacion.setObservacionesResolucion(cmd.observaciones());
        apelacion.setSiActiva(false);
        apelacionActaRepository.guardar(apelacion);

        acta.setResultadoFinal(ResultadoFinalActa.ABSUELTO);
        registrarEvento(cmd.actaId(), TipoEventoActa.APEABS, null, null, null,
                "Apelacion aceptada - absolucion en segunda instancia. Fundamentos: "
                        + nvl(cmd.fundamentosResolucion()));

        boolean cerrar = !bloqueantesChecker.tieneBloqueantesActivos(cmd.actaId());
        if (cerrar) {
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            acta.setBloqueActual(BloqueActual.CERR);
            acta.setEstadoProcesal(EstadoProcesalActa.CONCLUIDO);
            actaRepository.guardar(acta);
            registrarEvento(cmd.actaId(), TipoEventoActa.CIERRA, null, null, null,
                    "Acta cerrada por absolucion en apelacion. Sin bloqueantes activos.");
        } else {
            actaRepository.guardar(acta);
        }

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        String mensaje = cerrar
                ? "Apelacion aceptada. Infractor absuelto. Acta cerrada."
                : "Apelacion aceptada. Infractor absuelto. Cierre pendiente por bloqueantes materiales activos.";
        return ComandoResultado.de(cmd.actaId(), apelacion.getId(),
                TipoEventoActa.APEABS.codigo(), mensaje);
    }

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    public Optional<FalActaApelacion> obtenerApelacionActiva(Long actaId) {
        actaRepository.buscarPorId(actaId)
                .orElseThrow(() -> new ActaNoEncontradaException(actaId));
        return apelacionActaRepository.buscarActiva(actaId);
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private void validarActaAbierta(FalActa acta) {
        SituacionAdministrativaActa sit = acta.getSituacionAdministrativa();
        if (sit == SituacionAdministrativaActa.CERRADA) {
            throw new PrecondicionVioladaException("El acta esta cerrada.");
        }
        if (sit == SituacionAdministrativaActa.ANULADA) {
            throw new PrecondicionVioladaException("El acta esta anulada.");
        }
        if (sit == SituacionAdministrativaActa.ARCHIVADA) {
            throw new PrecondicionVioladaException("El acta esta archivada.");
        }
        if (sit == SituacionAdministrativaActa.PARALIZADA) {
            throw new PrecondicionVioladaException("El acta esta paralizada.");
        }
    }

    /**
     * Verifica y retorna el fallo condenatorio notificado.
     * Lanza PrecondicionVioladaException si no cumple.
     */
    private FalActaFallo obtenerFalloCondenatorioNotificado(Long actaId) {
        FalActaFallo fallo = falloActaRepository.buscarActivo(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe fallo activo sobre el acta. No se puede operar."));
        if (fallo.getTipoFallo() != TipoFalloActa.CONDENATORIO) {
            throw new PrecondicionVioladaException(
                    "Solo se puede operar sobre fallo condenatorio. Tipo actual: " + fallo.getTipoFallo());
        }
        if (fallo.getEstadoFallo() != EstadoFalloActa.NOTIFICADO) {
            throw new PrecondicionVioladaException(
                    "El fallo condenatorio debe estar NOTIFICADO. "
                            + "Estado actual: " + fallo.getEstadoFallo());
        }
        return fallo;
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo,
                                  String idDocumento, String idNotificacion,
                                  String idOperador, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento evento = new FalActaEvento(
                UUID.randomUUID().toString(),
                idActa,
                tipo,
                LocalDateTime.now(),
                orden,
                idDocumento,
                idNotificacion,
                idOperador,
                descripcion,
                null
        );
        eventoRepository.registrar(evento);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }
}

