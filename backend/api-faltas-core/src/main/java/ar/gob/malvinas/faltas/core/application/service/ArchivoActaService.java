package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ArchivarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReingresarDesdeArchivoCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.ActorTipoEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArchivo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaParalizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.repository.ActaArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaParalizacionRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.MotivoArchivoRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de archivo y reingreso de actas.
 *
 * archivar: captura origen completo, crea FalActaArchivo, registra ACTARCH.
 *   Si el acta estaba PARALIZADA, cierra el ciclo activo de paralizacion dentro de la misma operacion.
 *   No asigna resultado final automaticamente.
 *
 * reingresar: restaura exactamente el estado de origen capturado en el snapshot del ciclo.
 *
 * Matriz de compatibilidad para archivar:
 *   ACTIVA           -> OK
 *   PARALIZADA       -> OK (cierra ciclo de paralizacion)
 *   ARCHIVADA        -> RECHAZADA (ya archivada)
 *   EN_GESTION_EXT   -> RECHAZADA
 *   CERRADA/ANULADA  -> RECHAZADA
 */
@Service
public class ArchivoActaService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final ActaArchivoRepository archivoRepository;
    private final ActaParalizacionRepository paralizacionRepository;
    private final MotivoArchivoRepository motivoArchivoRepository;
    private final ObservacionRepository observacionRepository;
    private final SnapshotRecalculador snapshotRecalculador;
    private final FaltasClock faltasClock;

    public ArchivoActaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            ActaArchivoRepository archivoRepository,
            ActaParalizacionRepository paralizacionRepository,
            MotivoArchivoRepository motivoArchivoRepository,
            ObservacionRepository observacionRepository,
            SnapshotRecalculador snapshotRecalculador,
            FaltasClock faltasClock) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.archivoRepository = archivoRepository;
        this.paralizacionRepository = paralizacionRepository;
        this.motivoArchivoRepository = motivoArchivoRepository;
        this.observacionRepository = observacionRepository;
        this.snapshotRecalculador = snapshotRecalculador;
        this.faltasClock = faltasClock;
    }

    public ComandoResultado archivar(ArchivarActaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        validarPermiteArchivar(acta);
        validarNoExisteArchivoActivo(cmd.actaId());

        FalMotivoArchivo motivo = obtenerMotivoActivo(cmd.idMotivoArchivo());

        if (motivo.isSiRequiereObservacion()
                && (cmd.observacionTexto() == null || cmd.observacionTexto().trim().isEmpty())) {
            throw new PrecondicionVioladaException(
                    "El motivo de archivo requiere observacion. Proporcionar observacionTexto.");
        }

        String usuario = cmd.idUserOperacion() != null ? cmd.idUserOperacion() : "SYS";
        LocalDateTime ahora = faltasClock.now();

        Optional<FalActaSnapshot> snapActual = snapshotRepository.buscarPorActa(cmd.actaId());

        Long archivoId = archivoRepository.nextId();
        FalActaArchivo archivo = new FalActaArchivo(
                archivoId,
                cmd.actaId(),
                motivo.getId(),
                motivo.isSiPermiteReingreso(),
                motivo.isSiNulidad(),
                acta.getEstadoProcesal(),
                acta.getSituacionAdministrativa(),
                acta.getBloqueActual(),
                ahora, usuario, ahora, usuario);

        snapActual.ifPresent(s -> {
            if (s.getCodBandeja() != null) archivo.setCodBandejaOrigen(s.getCodBandeja().name());
            if (s.getSubBandeja() != null) archivo.setSubBandejaOrigen(s.getSubBandeja());
            if (s.getAccionPendiente() != null) archivo.setAccionPendienteOrigen(s.getAccionPendiente().name());
        });

        if (cmd.documentoId() != null) {
            archivo.setDocumentoId(cmd.documentoId());
        }

        Long obsId = null;
        if (cmd.observacionTexto() != null && !cmd.observacionTexto().trim().isEmpty()) {
            Long id = observacionRepository.nextId();
            FalObservacion obs = new FalObservacion(id, EntidadTipoObservada.ARCHIVO,
                    archivoId, null, cmd.observacionTexto().trim(),
                    OrigenObservacion.USUARIO, ahora, usuario);
            observacionRepository.guardar(obs);
            obsId = id;
            archivo.setObservacionId(obsId);
        }

        boolean estabaParalizada = acta.getSituacionAdministrativa() == SituacionAdministrativaActa.PARALIZADA;
        if (estabaParalizada) {
            paralizacionRepository.buscarActivaPorActa(cmd.actaId()).ifPresent(cicloParaliz -> {
                FalActaParalizacion cierre = cicloParaliz.copia();
                cierre.setFhReactivacion(ahora);
                cierre.setIdUserReactivacion(usuario);
                cierre.setSiActiva(false);
                cierre.setFhUltMod(ahora);
                cierre.setIdUserUltMod(usuario);
                paralizacionRepository.cerrarActivaAtomicamente(cmd.actaId(), cierre);
            });
        }

        FalActaArchivo archivoGuardado = archivoRepository.crearActivoAtomicamente(archivo);

        FalActaEvento evento = registrarEvento(acta.getId(), TipoEventoActa.ACTARCH, usuario,
                "Acta archivada. Motivo: " + motivo.getCodMotivoArchivo()
                + (estabaParalizada ? " (paralizacion cerrada)" : "")
                + (obsId != null ? ". Obs: " + obsId : ""));

        FalActaArchivo archivoConEvento = archivoGuardado.copia();
        archivoConEvento.setEventoArchivoId(evento.getId() != null ? String.valueOf(evento.getId()) : null);
        archivoConEvento.setVersionRow(archivoGuardado.getVersionRow());
        archivoRepository.guardar(archivoConEvento);

        acta.setBloqueActual(BloqueActual.ARCH);
        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
        actaRepository.guardar(acta);

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(archivoConEvento.getId()),
                TipoEventoActa.ACTARCH.codigo(),
                "Acta archivada. Motivo: " + motivo.getCodMotivoArchivo());
    }

    public ComandoResultado reingresar(ReingresarDesdeArchivoCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        FalActaArchivo archivoActivo = archivoRepository.buscarActivoPorActa(cmd.actaId())
                .orElseThrow(() -> new PrecondicionVioladaException(
                        "No existe archivo activo para el acta: " + cmd.actaId()));

        if (!archivoActivo.isSiPermiteReingresoSnapshot()) {
            throw new PrecondicionVioladaException(
                    "El ciclo de archivo no permite reingreso (siPermiteReingresoSnapshot=false).");
        }

        if (archivoActivo.getVersionRow() != cmd.versionArchivo()) {
            throw new PrecondicionVioladaException(
                    "Version de archivo incorrecta. Esperada: " + archivoActivo.getVersionRow()
                    + ", recibida: " + cmd.versionArchivo());
        }

        String usuario = cmd.idUserOperacion() != null ? cmd.idUserOperacion() : "SYS";
        LocalDateTime ahora = faltasClock.now();

        FalActaArchivo cierre = archivoActivo.copia();
        cierre.setFhReingreso(ahora);
        cierre.setIdUserReingreso(usuario);
        cierre.setSiActivo(false);
        cierre.setFhUltMod(ahora);
        cierre.setIdUserUltMod(usuario);

        archivoRepository.cerrarActivoAtomicamente(cmd.actaId(), cierre);

        acta.setEstadoProcesal(archivoActivo.getEstProcActOrigen());
        acta.setSituacionAdministrativa(archivoActivo.getSitAdmActOrigen());
        acta.setBloqueActual(archivoActivo.getBloqueOrigen());
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTREI, usuario,
                "Acta reingresada desde archivo. Ciclo: " + archivoActivo.getId()
                + ". Bloque origen: " + archivoActivo.getBloqueOrigen());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), String.valueOf(archivoActivo.getId()),
                TipoEventoActa.ACTREI.codigo(),
                "Acta reingresada. Bloque restaurado: " + archivoActivo.getBloqueOrigen());
    }

    private void validarPermiteArchivar(FalActa acta) {
        switch (acta.getSituacionAdministrativa()) {
            case CERRADA, ANULADA ->
                throw new PrecondicionVioladaException("El acta esta cerrada definitivamente. No se puede archivar.");
            case ARCHIVADA ->
                throw new PrecondicionVioladaException("El acta ya esta archivada.");
            case EN_GESTION_EXTERNA ->
                throw new PrecondicionVioladaException("El acta esta en gestion externa. No se puede archivar desde aqui.");
            default -> { }
        }
    }

    private void validarNoExisteArchivoActivo(Long actaId) {
        if (archivoRepository.buscarActivoPorActa(actaId).isPresent()) {
            throw new PrecondicionVioladaException(
                    "Ya existe un archivo activo para el acta: " + actaId);
        }
    }

    private FalMotivoArchivo obtenerMotivoActivo(Long idMotivo) {
        FalMotivoArchivo motivo = motivoArchivoRepository.buscarPorId(idMotivo)
                .orElseThrow(() -> new PrecondicionVioladaException("Motivo de archivo no encontrado: " + idMotivo));
        if (!motivo.isSiActivo()) {
            throw new PrecondicionVioladaException(
                    "El motivo de archivo esta inactivo y no puede usarse: " + idMotivo);
        }
        return motivo;
    }

    private FalActaEvento registrarEvento(Long actaId, TipoEventoActa tipo, String usuario, String descripcionLegible) {
        FalActaEvento evento = FalActaEvento.builder()
                .actaId(actaId)
                .tipoEvt(tipo)
                .origenEvt(usuario != null ? OrigenEvento.USUARIO_WEB : OrigenEvento.PROCESO_AUTOMATICO)
                .fhEvt(faltasClock.now())
                .idUserEvt(usuario)
                .actorTipo(usuario != null ? ActorTipoEvento.USUARIO_INTERNO : ActorTipoEvento.SISTEMA)
                .descripcionLegible(descripcionLegible)
                .build();
        return eventoRepository.registrar(evento);
    }
}
