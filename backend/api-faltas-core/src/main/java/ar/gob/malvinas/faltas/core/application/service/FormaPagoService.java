package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.exception.FormaPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio para FalActaFormaPago.
 * CONTADO: no tiene plan. PLAN_PAGO/REFINANCIACION: exige plan posterior.
 * Una forma vigente por obligacion. El historial se preserva.
 */
@Service
public class FormaPagoService {

    private final FormaPagoRepository formaPagoRepo;
    private final ObligacionPagoRepository obligacionRepo;

    public FormaPagoService(FormaPagoRepository formaPagoRepo, ObligacionPagoRepository obligacionRepo) {
        this.formaPagoRepo = formaPagoRepo;
        this.obligacionRepo = obligacionRepo;
    }

    /**
     * Registra una nueva forma de pago para la obligacion.
     * Si ya existe forma vigente y es una refinanciacion, reemplaza la anterior.
     * CONTADO: unica, no hay plan. PLAN/REFINANCIACION: se espera plan posterior.
     */
    public FalActaFormaPago generarForma(
            Long obligacionPagoId,
            TipoFormaPago tipoFormaPago,
            BigDecimal montoForma,
            String cmteEM, Short prefEM, Integer nroEM,
            String cmtePG, Short prefPG, Integer nroPG,
            String idUser) {
        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Obligacion no encontrada: " + obligacionPagoId));
        if (obligacion.estaAnulada() || obligacion.estaCancelada())
            throw new PrecondicionVioladaException("No se puede generar forma de pago para obligacion en estado "
                    + obligacion.getEstadoObligacion());

        Optional<FalActaFormaPago> vigente = formaPagoRepo.findVigenteByObligacionPagoId(obligacionPagoId);

        long nroFormaActual = formaPagoRepo.findByObligacionPagoId(obligacionPagoId).size() + 1;
        Long nuevoId = formaPagoRepo.nextId();
        LocalDateTime ahora = LocalDateTime.now();
        FalActaFormaPago nueva = new FalActaFormaPago(
                nuevoId, obligacionPagoId, (short) nroFormaActual,
                tipoFormaPago, montoForma, ahora, ahora, idUser);
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
        obligacion.setEstadoObligacion(EstadoObligacionPago.CON_FORMA_PAGO);
        obligacionRepo.save(obligacion);
        return guardada;
    }

    public FalActaFormaPago marcarProcesada(Long formaPagoId, LocalDateTime fhProcesado) {
        FalActaFormaPago forma = formaPagoRepo.findById(formaPagoId)
                .orElseThrow(() -> new FormaPagoNoEncontradaException(formaPagoId));
        forma.setEstadoFormaPago(EstadoFormaPago.PROCESADA);
        forma.setFhPagoProcesado(fhProcesado != null ? fhProcesado : LocalDateTime.now());
        return formaPagoRepo.save(forma);
    }

    public FalActaFormaPago marcarConfirmada(Long formaPagoId, LocalDateTime fhConfirmado) {
        FalActaFormaPago forma = formaPagoRepo.findById(formaPagoId)
                .orElseThrow(() -> new FormaPagoNoEncontradaException(formaPagoId));
        if (forma.getEstadoFormaPago() != EstadoFormaPago.PROCESADA
                && forma.getEstadoFormaPago() != EstadoFormaPago.GENERADA)
            throw new PrecondicionVioladaException(
                    "No se puede confirmar forma en estado " + forma.getEstadoFormaPago());
        forma.setEstadoFormaPago(EstadoFormaPago.CONFIRMADA);
        forma.setFhPagoConfirmado(fhConfirmado != null ? fhConfirmado : LocalDateTime.now());
        return formaPagoRepo.save(forma);
    }

    public Optional<FalActaFormaPago> buscarVigenteByObligacion(Long obligacionPagoId) {
        return formaPagoRepo.findVigenteByObligacionPagoId(obligacionPagoId);
    }

    public List<FalActaFormaPago> buscarHistorialByObligacion(Long obligacionPagoId) {
        return formaPagoRepo.findByObligacionPagoId(obligacionPagoId);
    }

    public FalActaFormaPago buscarPorId(Long id) {
        return formaPagoRepo.findById(id)
                .orElseThrow(() -> new FormaPagoNoEncontradaException(id));
    }
}
