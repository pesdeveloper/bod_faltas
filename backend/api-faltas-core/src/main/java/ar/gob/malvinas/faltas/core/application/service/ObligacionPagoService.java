package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ObligacionPagoService {

    private final ObligacionPagoRepository obligacionRepo;
    private final ActaRepository actaRepo;
    private final ActaValorizacionRepository valorizacionRepo;
    private final FalloActaRepository falloRepo;
    private final ActaEventoRepository eventoRepo;
    private final EconomiaProyeccionRecalculador recalculador;
    private final FaltasClock clock;

    public ObligacionPagoService(
            ObligacionPagoRepository obligacionRepo,
            ActaRepository actaRepo,
            ActaValorizacionRepository valorizacionRepo,
            FalloActaRepository falloRepo,
            ActaEventoRepository eventoRepo,
            EconomiaProyeccionRecalculador recalculador,
            FaltasClock clock) {
        this.obligacionRepo = obligacionRepo;
        this.actaRepo = actaRepo;
        this.valorizacionRepo = valorizacionRepo;
        this.falloRepo = falloRepo;
        this.eventoRepo = eventoRepo;
        this.recalculador = recalculador;
        this.clock = clock;
    }

    public FalActaObligacionPago determinarVoluntaria(Long actaId, Long personaId, Long valorizacionId, String idUser) {
        actaRepo.buscarPorId(actaId)
                .orElseThrow(() -> new PrecondicionVioladaException("Acta no encontrada: " + actaId));
        FalActaValorizacion val = valorizacionRepo.findById(valorizacionId)
                .orElseThrow(() -> new PrecondicionVioladaException("Valorizacion no encontrada: " + valorizacionId));
        if (!val.getActaId().equals(actaId))
            throw new PrecondicionVioladaException("La valorizacion no pertenece al acta " + actaId);
        if (val.getEstadoValorizacion() != EstadoValorizacion.CONFIRMADA)
            throw new PrecondicionVioladaException("La valorizacion debe estar CONFIRMADA para determinar obligacion");
        if (obligacionRepo.findVigenteByActaId(actaId).isPresent())
            throw new PrecondicionVioladaException("Ya existe una obligacion vigente para actaId=" + actaId);

        String actor = ActorContextHolder.subOr(idUser);
        var ahora = clock.now();
        Long nuevoId = obligacionRepo.nextId();
        FalActaObligacionPago nueva = new FalActaObligacionPago(
                nuevoId, actaId, personaId, TipoObligacionPago.PAGO_VOLUNTARIO,
                val.getMontoFinal(), ahora, actor, ahora, actor);
        nueva.setValorizacionId(valorizacionId);
        FalActaObligacionPago guardada = obligacionRepo.crearVigenteAtomico(nueva, null);
        emitirEvento(actaId, TipoEventoActa.OBLDET, actor, "Obligacion de pago voluntario determinada");
        recalculador.recalcular(actaId, OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return guardada;
    }

    public FalActaObligacionPago determinarCondena(Long actaId, Long personaId, Long falloId,
            Long valorizacionId, BigDecimal montoCondena, String idUser) {
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
        String actor = ActorContextHolder.subOr(idUser);
        var ahora = clock.now();
        Long nuevoId = obligacionRepo.nextId();
        FalActaObligacionPago nueva = new FalActaObligacionPago(
                nuevoId, actaId, personaId, TipoObligacionPago.CONDENA, montoCondena, ahora, actor, ahora, actor);
        nueva.setFalloId(falloId);
        if (valorizacionId != null) nueva.setValorizacionId(valorizacionId);

        FalActaObligacionPago guardada;
        if (vigente.isPresent()) {
            FalActaObligacionPago anteriorVigente = vigente.get();
            anteriorVigente.setSiExcluirEscaneo(true);
            anteriorVigente.setEstadoObligacion(EstadoObligacionPago.REEMPLAZADA);
            obligacionRepo.save(anteriorVigente);
            guardada = obligacionRepo.crearVigenteAtomico(nueva, anteriorVigente);
            emitirEvento(actaId, TipoEventoActa.OBLREP, actor, "Obligacion reemplazada por condena");
        } else {
            guardada = obligacionRepo.crearVigenteAtomico(nueva, null);
            emitirEvento(actaId, TipoEventoActa.OBLDET, actor, "Obligacion de condena determinada");
        }
        recalculador.recalcular(actaId, OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return guardada;
    }

    public FalActaObligacionPago cancelar(Long actaId, String idUser) {
        FalActaObligacionPago vigente = obligacionRepo.findVigenteByActaId(actaId)
                .orElseThrow(() -> new ObligacionPagoNoEncontradaException("actaId=" + actaId + " vigente"));
        String actor = ActorContextHolder.subOr(idUser);
        vigente.cancelar(clock.now());
        FalActaObligacionPago saved = obligacionRepo.save(vigente);
        recalculador.recalcular(actaId, OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return saved;
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

    private void emitirEvento(Long actaId, TipoEventoActa tipo, String actor, String desc) {
        eventoRepo.registrar(FalActaEvento.builder()
                .actaId(actaId).tipoEvt(tipo).origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(clock.now()).idUserEvt(actor).descripcionLegible(desc).build());
    }
}