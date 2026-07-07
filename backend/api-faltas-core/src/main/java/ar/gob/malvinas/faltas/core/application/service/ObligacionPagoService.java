package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de dominio para FalActaObligacionPago.
 *
 * Pago voluntario: nace de valorizacion confirmada, falloId NULL.
 * Condena: exige falloId y valorizacion coherente.
 * Una sola obligacion vigente por acta.
 * Sustitution de voluntario por condena es atomica.
 */
@Service
public class ObligacionPagoService {

    private final ObligacionPagoRepository obligacionRepo;
    private final ActaRepository actaRepo;
    private final ActaValorizacionRepository valorizacionRepo;
    private final FalloActaRepository falloRepo;

    public ObligacionPagoService(
            ObligacionPagoRepository obligacionRepo,
            ActaRepository actaRepo,
            ActaValorizacionRepository valorizacionRepo,
            FalloActaRepository falloRepo) {
        this.obligacionRepo = obligacionRepo;
        this.actaRepo = actaRepo;
        this.valorizacionRepo = valorizacionRepo;
        this.falloRepo = falloRepo;
    }

    /**
     * Determina obligacion de pago voluntario para un acta.
     * Requiere valorizacion confirmada de tipo PAGO_VOLUNTARIO o compatible.
     * No puede coexistir con otra obligacion vigente.
     */
    public FalActaObligacionPago determinarVoluntaria(
            Long actaId,
            Long personaId,
            Long valorizacionId,
            String idUser) {
        actaRepo.buscarPorId(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("Acta no encontrada: " + actaId));
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new PrecondicionVioladaException("Valorizacion no encontrada: " + valorizacionId));
        if (!val.getActaId().equals(actaId))
            throw new PrecondicionVioladaException("La valorizacion no pertenece al acta " + actaId);
        if (val.getEstadoValorizacion() != EstadoValorizacion.CONFIRMADA)
            throw new PrecondicionVioladaException("La valorizacion debe estar CONFIRMADA para determinar obligacion");

        Optional<FalActaObligacionPago> vigente = obligacionRepo.findVigenteByActaId(actaId);
        if (vigente.isPresent())
            throw new PrecondicionVioladaException(
                    "Ya existe una obligacion vigente para actaId=" + actaId + ". Usar sustituir o anular primero.");

        LocalDateTime ahora = LocalDateTime.now();
        Long nuevoId = obligacionRepo.nextId();
        FalActaObligacionPago nueva = new FalActaObligacionPago(
                nuevoId, actaId, personaId,
                TipoObligacionPago.PAGO_VOLUNTARIO,
                val.getMontoFinal(), ahora, idUser, ahora, idUser);
        nueva.setValorizacionId(valorizacionId);
        return obligacionRepo.crearVigenteAtomico(nueva, null);
    }

    /**
     * Determina obligacion de condena para un acta.
     * Requiere fallo condenatorio/firme de la misma acta.
     * No puede coexistir con otra obligacion vigente (usa crearVigenteAtomico internamente).
     */
    public FalActaObligacionPago determinarCondena(
            Long actaId,
            Long personaId,
            Long falloId,
            Long valorizacionId,
            BigDecimal montoCondena,
            String idUser) {
        actaRepo.buscarPorId(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("Acta no encontrada: " + actaId));
        FalActaFallo fallo = falloRepo.buscarActivo(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("Fallo activo no encontrado para actaId=" + actaId));
        if (!fallo.getId().equals(falloId))
            throw new PrecondicionVioladaException("falloId no coincide con el fallo activo del acta");
        if (fallo.getResultadoFallo() == null
                || fallo.getResultadoFallo() == ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa.ABSUELVE)
            throw new PrecondicionVioladaException("El fallo no es condenatorio");

        if (valorizacionId != null) {
            FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                    .orElseThrow(() -> new PrecondicionVioladaException("Valorizacion no encontrada: " + valorizacionId));
            if (!val.getActaId().equals(actaId))
                throw new PrecondicionVioladaException("La valorizacion no pertenece al acta " + actaId);
        }

        if (montoCondena == null || montoCondena.compareTo(BigDecimal.ZERO) < 0)
            throw new PrecondicionVioladaException("montoCondena no puede ser nulo ni negativo");

        Optional<FalActaObligacionPago> vigente = obligacionRepo.findVigenteByActaId(actaId);

        LocalDateTime ahora = LocalDateTime.now();
        Long nuevoId = obligacionRepo.nextId();
        FalActaObligacionPago nueva = new FalActaObligacionPago(
                nuevoId, actaId, personaId,
                TipoObligacionPago.CONDENA,
                montoCondena, ahora, idUser, ahora, idUser);
        nueva.setFalloId(falloId);
        if (valorizacionId != null) nueva.setValorizacionId(valorizacionId);

        if (vigente.isPresent()) {
            FalActaObligacionPago anteriorVigente = vigente.get();
            anteriorVigente.setSiExcluirEscaneo(true);
            obligacionRepo.save(anteriorVigente);
            return obligacionRepo.crearVigenteAtomico(nueva, anteriorVigente);
        }
        return obligacionRepo.crearVigenteAtomico(nueva, null);
    }

    /**
     * Cancela la obligacion vigente (pago total confirmado).
     */
    public FalActaObligacionPago cancelar(Long actaId, String idUser) {
        FalActaObligacionPago vigente = obligacionRepo.findVigenteByActaId(actaId)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException("actaId=" + actaId + " vigente"));
        vigente.cancelar(LocalDateTime.now());
        return obligacionRepo.save(vigente);
    }

    public FalActaObligacionPago actualizarEstado(Long id, EstadoObligacionPago nuevoEstado) {
        FalActaObligacionPago o = obligacionRepo.findById(id)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(id));
        o.setEstadoObligacion(nuevoEstado);
        return obligacionRepo.save(o);
    }

    public Optional<FalActaObligacionPago> buscarVigenteByActaId(Long actaId) {
        return obligacionRepo.findVigenteByActaId(actaId);
    }

    public FalActaObligacionPago buscarPorId(Long id) {
        return obligacionRepo.findById(id)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException(id));
    }
}
