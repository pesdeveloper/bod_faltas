package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.AnularBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.application.command.CumplirBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarBloqueanteMaterialCommand;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.exception.BloqueanteMaterialNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.BloqueanteMaterialRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gestion minima in-memory de bloqueantes materiales (Slice 7B).
 *
 * registrar: crea bloqueante PENDIENTE/siActivo=true.
 * cumplir:   PENDIENTE -> CUMPLIDO/siActivo=false. Idempotente si ya CUMPLIDO.
 * anular:    PENDIENTE -> ANULADO/siActivo=false.  Idempotente si ya ANULADO.
 *
 * Bloqueante CUMPLIDO o ANULADO no impide CIERRA.
 * Solo bloqueantes con siActivo=true impiden el cierre.
 *
 * Cierre diferido (Slice 7C): al cumplir o anular el ultimo bloqueante activo,
 * si el acta tiene resultado final cerrable y no esta ya cerrada, se emite CIERRA
 * a traves de CierreActaHelper.
 *
 * No se emiten eventos propios del bloqueante: no existe evento de dominio definido
 * para cumplir/anular bloqueante.
 *
 * Slice 9: reemplazar InMemoryBloqueanteMaterialRepository por implementacion JDBC.
 */
@Service
public class BloqueanteMaterialService {

    private final BloqueanteMaterialRepository bloqueanteMaterialRepository;
    private final ActaRepository actaRepository;
    private final CierreActaHelper cierreActaHelper;

    public BloqueanteMaterialService(
            BloqueanteMaterialRepository bloqueanteMaterialRepository,
            ActaRepository actaRepository,
            CierreActaHelper cierreActaHelper) {
        this.bloqueanteMaterialRepository = bloqueanteMaterialRepository;
        this.actaRepository = actaRepository;
        this.cierreActaHelper = cierreActaHelper;
    }

    public FalBloqueanteMaterial registrar(RegistrarBloqueanteMaterialCommand cmd) {
        if (cmd.actaId() == null) {
            throw new PrecondicionVioladaException("El actaId es obligatorio para registrar un bloqueante material.");
        }
        if (cmd.origen() == null) {
            throw new PrecondicionVioladaException("El origen es obligatorio para registrar un bloqueante material.");
        }
        FalBloqueanteMaterial b = new FalBloqueanteMaterial(UUID.randomUUID().toString(), cmd.actaId());
        b.setOrigen(cmd.origen());
        return bloqueanteMaterialRepository.guardar(b);
    }

    /**
     * Cumplir: PENDIENTE -> CUMPLIDO. Idempotente si ya esta CUMPLIDO.
     * No permite cumplir si esta ANULADO.
     * Tras resolver el estado, intenta cierre diferido del acta si corresponde.
     */
    public FalBloqueanteMaterial cumplir(CumplirBloqueanteMaterialCommand cmd) {
        FalBloqueanteMaterial b = bloqueanteMaterialRepository.findById(cmd.bloqueanteId())
                .orElseThrow(() -> new BloqueanteMaterialNoEncontradoException(cmd.bloqueanteId()));
        if (b.getEstado() == EstadoBloqueanteMaterial.CUMPLIDO) {
            return b;
        }
        if (b.getEstado() == EstadoBloqueanteMaterial.ANULADO) {
            throw new PrecondicionVioladaException(
                    "No se puede cumplir un bloqueante material que ya fue anulado. Id: " + cmd.bloqueanteId());
        }
        b.setEstado(EstadoBloqueanteMaterial.CUMPLIDO);
        b.setSiActivo(false);
        b.setFechaCierre(LocalDateTime.now());
        FalBloqueanteMaterial guardado = bloqueanteMaterialRepository.guardar(b);

        intentarCierreDiferido(guardado.getActaId());

        return guardado;
    }

    /**
     * Anular: PENDIENTE -> ANULADO. Idempotente si ya esta ANULADO.
     * No permite anular si esta CUMPLIDO.
     * Tras resolver el estado, intenta cierre diferido del acta si corresponde.
     */
    public FalBloqueanteMaterial anular(AnularBloqueanteMaterialCommand cmd) {
        FalBloqueanteMaterial b = bloqueanteMaterialRepository.findById(cmd.bloqueanteId())
                .orElseThrow(() -> new BloqueanteMaterialNoEncontradoException(cmd.bloqueanteId()));
        if (b.getEstado() == EstadoBloqueanteMaterial.ANULADO) {
            return b;
        }
        if (b.getEstado() == EstadoBloqueanteMaterial.CUMPLIDO) {
            throw new PrecondicionVioladaException(
                    "No se puede anular un bloqueante material que ya fue cumplido. Id: " + cmd.bloqueanteId());
        }
        b.setEstado(EstadoBloqueanteMaterial.ANULADO);
        b.setSiActivo(false);
        b.setFechaCierre(LocalDateTime.now());
        FalBloqueanteMaterial guardado = bloqueanteMaterialRepository.guardar(b);

        intentarCierreDiferido(guardado.getActaId());

        return guardado;
    }

    // -------------------------------------------------------------------------
    // Cierre diferido (Slice 7C)
    // -------------------------------------------------------------------------

    /**
     * Intenta cerrar el acta de forma diferida si se cumplen todas las condiciones:
     *   1. No quedan bloqueantes activos para el acta.
     *   2. El acta existe y no esta ya cerrada/anulada.
     *   3. El resultado final del acta es cerrable.
     *   4. No existe ya un evento CIERRA registrado (guard contra duplicados).
     */
    private void intentarCierreDiferido(Long actaId) {
        if (bloqueanteMaterialRepository.existsActivoByActaId(actaId)) {
            return;
        }

        FalActa acta = actaRepository.buscarPorId(actaId).orElse(null);
        if (acta == null) {
            return;
        }

        if (acta.estaCerrada()) {
            return;
        }

        if (!cierreActaHelper.esResultadoCerrable(acta.getResultadoFinal())) {
            return;
        }

        if (cierreActaHelper.yaTieneCierre(actaId)) {
            return;
        }

        cierreActaHelper.emitirCierre(acta,
                "Acta cerrada por resolucion del ultimo bloqueante material activo. Resultado: "
                        + acta.getResultadoFinal());
    }
}

