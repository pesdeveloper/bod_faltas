package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.exception.FormaPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class FormaPagoService {

    private final FormaPagoRepository formaPagoRepo;
    private final ObligacionPagoRepository obligacionRepo;
    private final ActaEventoRepository eventoRepo;
    private final EconomiaProyeccionRecalculador recalculador;
    private final FaltasClock clock;

    public FormaPagoService(FormaPagoRepository formaPagoRepo, ObligacionPagoRepository obligacionRepo,
            ActaEventoRepository eventoRepo, EconomiaProyeccionRecalculador recalculador, FaltasClock clock) {
        this.formaPagoRepo = formaPagoRepo;
        this.obligacionRepo = obligacionRepo;
        this.eventoRepo = eventoRepo;
        this.recalculador = recalculador;
        this.clock = clock;
    }

    public FalActaFormaPago generarForma(Long obligacionPagoId, TipoFormaPago tipoFormaPago, BigDecimal montoForma,
            String cmteEM, Short prefEM, Integer nroEM, String cmtePG, Short prefPG, Integer nroPG, String idUser) {
        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Obligacion no encontrada: " + obligacionPagoId));
        if (obligacion.estaDejadaSinEfecto() || obligacion.estaCanceladaPorPago())
            throw new PrecondicionVioladaException("No se puede generar forma de pago para obligacion en estado "
                    + obligacion.getEstadoObligacion());

        Optional<FalActaFormaPago> vigente = formaPagoRepo.findVigenteByObligacionPagoId(obligacionPagoId);
        long nroFormaActual = formaPagoRepo.findByObligacionPagoId(obligacionPagoId).size() + 1;
        String actor = ActorContextHolder.subOr(idUser);
        var ahora = clock.now();
        Long nuevoId = formaPagoRepo.nextId();
        FalActaFormaPago nueva = new FalActaFormaPago(
                nuevoId, obligacionPagoId, (short) nroFormaActual, tipoFormaPago, montoForma, ahora, ahora, actor);
        nueva.setReferenciaEM(cmteEM, prefEM, nroEM);
        nueva.setReferenciaPG(cmtePG, prefPG, nroPG);

        FalActaFormaPago guardada;
        if (vigente.isPresent()) {
            FalActaFormaPago anteriorVigente = vigente.get();
            anteriorVigente.setFhBaja(ahora);
            anteriorVigente.setMotivoBaja(MotivoBajaFormaPago.REFINANCIACION);
            nueva.setFormaReemplazadaId(anteriorVigente.getId());
            guardada = formaPagoRepo.reemplazarVigenteAtomico(nueva, anteriorVigente);
        } else {
            guardada = formaPagoRepo.save(nueva);
        }

        obligacion.setFormaPagoVigenteId(guardada.getId());
        obligacion.setEstadoObligacion(EstadoObligacionPago.CON_FORMA_PAGO_VIGENTE);
        obligacionRepo.save(obligacion);

        TipoEventoActa evento = tipoFormaPago == TipoFormaPago.RECIBO_AL_COBRO ? TipoEventoActa.RCBGEN : null;
        if (evento != null) emitirEvento(obligacion.getActaId(), evento, actor, "Forma de pago generada");
        recalculador.recalcular(obligacion.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return guardada;
    }

    public FalActaFormaPago marcarVigente(Long formaPagoId, java.time.LocalDateTime fhProcesado) {
        FalActaFormaPago forma = formaPagoRepo.findById(formaPagoId)
                .orElseThrow(() -> new FormaPagoNoEncontradaException(formaPagoId));
        forma.setEstadoFormaPago(EstadoFormaPago.VIGENTE);
        forma.setFhPagoProcesado(fhProcesado != null ? fhProcesado : clock.now());
        return formaPagoRepo.save(forma);
    }

    public FalActaFormaPago marcarPagada(Long formaPagoId, java.time.LocalDateTime fhConfirmado) {
        FalActaFormaPago forma = formaPagoRepo.findById(formaPagoId)
                .orElseThrow(() -> new FormaPagoNoEncontradaException(formaPagoId));
        if (forma.getEstadoFormaPago() != EstadoFormaPago.VIGENTE
                && forma.getEstadoFormaPago() != EstadoFormaPago.GENERADA)
            throw new PrecondicionVioladaException("No se puede marcar pagada forma en estado " + forma.getEstadoFormaPago());
        forma.setEstadoFormaPago(EstadoFormaPago.PAGADA);
        forma.setFhPagoConfirmado(fhConfirmado != null ? fhConfirmado : clock.now());
        return formaPagoRepo.save(forma);
    }

    public Optional<FalActaFormaPago> buscarVigenteByObligacion(Long obligacionPagoId) {
        return formaPagoRepo.findVigenteByObligacionPagoId(obligacionPagoId);
    }

    public List<FalActaFormaPago> buscarHistorialByObligacion(Long obligacionPagoId) {
        return formaPagoRepo.findByObligacionPagoId(obligacionPagoId);
    }

    public FalActaFormaPago buscarPorId(Long id) {
        return formaPagoRepo.findById(id).orElseThrow(() -> new FormaPagoNoEncontradaException(id));
    }

    private void emitirEvento(Long actaId, TipoEventoActa tipo, String actor, String desc) {
        eventoRepo.registrar(FalActaEvento.builder()
                .actaId(actaId).tipoEvt(tipo).origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(clock.now()).idUserEvt(actor).descripcionLegible(desc).build());
    }
}
