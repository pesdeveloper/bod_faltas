package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.ParalizarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReactivarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de paralizacion y reactivacion de actas.
 *
 * paralizar: ACTIVA/GEXT -> PARALIZADA, registra ACTPAR.
 * reactivar: PARALIZADA -> ACTIVA, registra ACTREA.
 *
 * Guardrails:
 *   - No se puede paralizar un acta cerrada o anulada.
 *   - No se puede paralizar una acta ya paralizada.
 *   - Solo se puede reactivar una acta paralizada.
 *
 * Slice 8F-4C: gap cubierto para ACT-020-PARALIZADA.
 */
@Service
public class ActaParalizacionService {

    private final ActaRepository actaRepository;
    private final ActaEventoRepository eventoRepository;
    private final ActaSnapshotRepository snapshotRepository;
    private final SnapshotRecalculador snapshotRecalculador;

    public ActaParalizacionService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador) {
        this.actaRepository = actaRepository;
        this.eventoRepository = eventoRepository;
        this.snapshotRepository = snapshotRepository;
        this.snapshotRecalculador = snapshotRecalculador;
    }

    public ComandoResultado paralizar(ParalizarActaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        if (acta.estaCerrada()) {
            throw new PrecondicionVioladaException("El acta esta cerrada. No se puede paralizar.");
        }
        if (acta.estaParalizada()) {
            throw new PrecondicionVioladaException("El acta ya esta paralizada.");
        }

        acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTPAR,
                cmd.motivoParalizacion() != null
                        ? cmd.motivoParalizacion()
                        : cmd.observaciones());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), acta.getId().toString(),
                TipoEventoActa.ACTPAR.codigo(),
                "Acta paralizada. Situacion: PARALIZADA");
    }

    public ComandoResultado reactivar(ReactivarActaCommand cmd) {
        FalActa acta = actaRepository.buscarPorId(cmd.actaId())
                .orElseThrow(() -> new ActaNoEncontradaException(cmd.actaId()));

        if (!acta.estaParalizada()) {
            throw new PrecondicionVioladaException(
                    "Reactivar requiere que el acta este paralizada. Situacion actual: "
                            + acta.getSituacionAdministrativa());
        }

        acta.setSituacionAdministrativa(SituacionAdministrativaActa.ACTIVA);
        actaRepository.guardar(acta);

        registrarEvento(acta.getId(), TipoEventoActa.ACTREA,
                cmd.motivoReactivacion() != null
                        ? cmd.motivoReactivacion()
                        : cmd.observaciones());

        FalActaSnapshot snap = snapshotRecalculador.recalcular(acta);
        snapshotRepository.guardar(snap);

        return ComandoResultado.de(acta.getId(), acta.getId().toString(),
                TipoEventoActa.ACTREA.codigo(),
                "Acta reactivada. Situacion: ACTIVA");
    }

    private void registrarEvento(Long idActa, TipoEventoActa tipo, String descripcion) {
        int orden = eventoRepository.proximoOrdenLogico(idActa);
        FalActaEvento evento = new FalActaEvento(
                UUID.randomUUID().toString(),
                idActa,
                tipo,
                LocalDateTime.now(),
                orden,
                null, null, null,
                descripcion,
                null);
        eventoRepository.registrar(evento);
    }
}
