package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ParalizarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReactivarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoParalizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaParalizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaParalizacionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de paralizacion y reactivacion de actas con ciclos FalActaParalizacion.
 *
 * Slice 8F-11G: reemplaza ActaParalizacionService con soporte de entidad propia,
 * motivos enum, observaciones vinculadas y optimistic locking de ciclos.
 *
 * Matriz de compatibilidad:
 *   ACTIVA       -> Paralizar: OK | Reactivar: X | Archivar: (via ArchivoActaService)
 *   PARALIZADA   -> Paralizar: X  | Reactivar: OK
 *   ARCHIVADA    -> Paralizar: X  | Reactivar: X
 *   EN_GEST_EXT  -> Paralizar: X  | Reactivar: X
 *   CERRADA/ANUL -> Paralizar: X  | Reactivar: X
 */
@Service
public class ParalizacionActaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final ActaParalizacionRepository paralizacionRepository;
    private final ObservacionRepository observacionRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    public ParalizacionActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            ActaParalizacionRepository paralizacionRepository,
            ObservacionRepository observacionRepository,
            SnapshotRecalculador snapshotRecalculador) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.paralizacionRepository = paralizacionRepository;
        this.observacionRepository = observacionRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    // -------------------------------------------------------------------------
    // Paralizar
    // -------------------------------------------------------------------------

    public ComandoResultado paralizar(ParalizarActaCommand cmd) {
        if (cmd.motivoParalizacion() == null) {
            throw new PrecondicionVioladaException("motivoParalizacion es obligatorio");
        }
        if (cmd.motivoParalizacion() == MotivoParalizacion.OTRO
                && (cmd.observacionTexto() == null || cmd.observacionTexto().trim().isEmpty())) {
            throw new PrecondicionVioladaException(
                    "Motivo OTRO requiere observacionTexto no vacio");
        }

        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarPermiteParalizacion(acta);
        validarNoExisteCicloActivo(cmd.actaId());

        String usuario = cmd.idUserOperacion() != null ? cmd.idUserOperacion() : "SYS";
        LocalDateTime ahora = LocalDateTime.now();

        Long cicloId = paralizacionRepository.nextId();
        FalActaParalizacion ciclo = new FalActaParalizacion(
                cicloId, cmd.actaId(), cmd.motivoParalizacion(),
                ahora, usuario, ahora, usuario);

        FalActaParalizacion cicloGuardado = paralizacionRepository.crearActivaAtomicamente(ciclo);

        Long obsId = null;
        if (cmd.observacionTexto() != null && !cmd.observacionTexto().trim().isEmpty()) {
            Long id = observacionRepository.nextId();
            FalObservacion obs = new FalObservacion(id, EntidadTipoObservada.PARALIZACION,
                    cicloGuardado.getId(), null, cmd.observacionTexto().trim(),
                    OrigenObservacion.USUARIO, ahora, usuario);
            observacionRepository.guardar(obs);
            obsId = id;
        }

        acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
        actaRepository.guardar(acta);

        String descripcion = "Acta paralizada. Motivo: " + cmd.motivoParalizacion()
                + (obsId != null ? ". Observacion id: " + obsId : "");
        FalActaEvento evento = registrarEvento(acta.getId(), TipoEventoActa.ACTPAR, usuario, descripcion);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(cicloGuardado.getId()),
                TipoEventoActa.ACTPAR.codigo(),
                descripcion);
    }

    // -------------------------------------------------------------------------
    // Reactivar
    // -------------------------------------------------------------------------

    public ComandoResultado reactivar(ReactivarActaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        if (!acta.estaParalizada()) {
            throw new PrecondicionVioladaException(
                    "Reactivar requiere PARALIZADA. Situacion actual: " + acta.getSituacionAdministrativa());
        }

        FalActaParalizacion cicloActivo = paralizacionRepository.buscarActivaPorActa(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe ciclo activo de paralizacion para acta: " + cmd.actaId()));

        String usuario = cmd.idUserOperacion() != null ? cmd.idUserOperacion() : "SYS";
        LocalDateTime ahora = LocalDateTime.now();

        FalActaParalizacion cierre = cicloActivo.copia();
        cierre.setFhReactivacion(ahora);
        cierre.setIdUserReactivacion(usuario);
        cierre.setSiActiva(false);
        cierre.setFhUltMod(ahora);
        cierre.setIdUserUltMod(usuario);

        paralizacionRepository.cerrarActivaAtomicamente(cmd.actaId(), cierre);

        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTREA, usuario,
                "Acta reactivada. Ciclo cerrado: " + cicloActivo.getId());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(cicloActivo.getId()),
                TipoEventoActa.ACTREA.codigo(),
                "Acta reactivada. Situacion: ACTIVA");
    }

    // -------------------------------------------------------------------------
    // Validaciones
    // -------------------------------------------------------------------------

    private void validarPermiteParalizacion(FalActa acta) {
        switch (acta.getSituacionAdministrativa()) {
            case CERRADA, ANULADA ->
                throw new PrecondicionVioladaException("El acta esta cerrada definitivamente. No se puede paralizar.");
            case PARALIZADA ->
                throw new PrecondicionVioladaException("El acta ya esta paralizada.");
            case ARCHIVADA ->
                throw new PrecondicionVioladaException("El acta esta archivada. No se puede paralizar.");
            case EN_GESTION_EXTERNA ->
                throw new PrecondicionVioladaException("El acta esta en gestion externa. No se puede paralizar.");
            default -> { /* ACTIVA: permitido */ }
        }
    }

    private void validarNoExisteCicloActivo(Long actaId) {
        Optional<FalActaParalizacion> activo = paralizacionRepository.buscarActivaPorActa(actaId);
        if (activo.isPresent()) {
            throw new PrecondicionVioladaException(
                    "Ya existe un ciclo activo de paralizacion para el acta: " + actaId);
        }
    }

    // -------------------------------------------------------------------------
    // Interno
    // -------------------------------------------------------------------------

    private FalActaEvento registrarEvento(Long actaId, TipoEventoActa tipo, String usuario, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(actaId);
        FalActaEvento evento = new FalActaEvento(
                UUID.randomUUID().toString(), actaId, tipo,
                LocalDateTime.now(), orden,
                null, null, usuario, descripcion, null);
        eventoRepository.registrar(evento);
        return evento;
    }
}
