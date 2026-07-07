package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.exception.PlanPagoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio para FalActaPlanPagoRef.
 * Un plan vigente por obligacion.
 * Refinanciacion es atomica: plan anterior REFINANCIADO, nuevo ACTIVO.
 * Caches son auxiliares; la verdad financiera es Ingresos.
 */
@Service
public class PlanPagoService {

    private final PlanPagoRefRepository planRepo;
    private final FormaPagoRepository formaRepo;
    private final ObligacionPagoRepository obligacionRepo;

    public PlanPagoService(
            PlanPagoRefRepository planRepo,
            FormaPagoRepository formaRepo,
            ObligacionPagoRepository obligacionRepo) {
        this.planRepo = planRepo;
        this.formaRepo = formaRepo;
        this.obligacionRepo = obligacionRepo;
    }

    /**
     * Genera un nuevo plan de pago para la forma indicada.
     * La forma debe ser PLAN_PAGO o REFINANCIACION. Par (idTdocPlan,idDocPlan) unico.
     */
    public FalActaPlanPagoRef generarPlan(
            Long formaPagoId,
            Long obligacionPagoId,
            short idTdocPlan,
            long idDocPlan,
            short cantidadCuotas,
            BigDecimal importeTotalPlan,
            BigDecimal importeCuotaRegular) {
        FalActaFormaPago forma = formaRepo.findById(formaPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Forma de pago no encontrada: " + formaPagoId));
        if (!forma.esPlan())
            throw new PrecondicionVioladaException(
                    "La forma de pago debe ser PLAN_PAGO o REFINANCIACION para generar plan. Tipo: "
                            + forma.getTipoFormaPago());

        Optional<FalActaPlanPagoRef> vigente = planRepo.findVigenteByObligacionPagoId(obligacionPagoId);
        if (vigente.isPresent() && forma.getTipoFormaPago() == TipoFormaPago.PLAN_PAGO)
            throw new PrecondicionVioladaException(
                    "Ya existe un plan vigente. Usar refinanciarPlan para reemplazarlo.");

        Long nuevoId = planRepo.nextId();
        FalActaPlanPagoRef nuevo = new FalActaPlanPagoRef(
                nuevoId, formaPagoId, obligacionPagoId,
                idTdocPlan, idDocPlan, cantidadCuotas, importeTotalPlan);
        nuevo.setFhGeneracionPlan(LocalDateTime.now());
        if (importeCuotaRegular != null) nuevo.setImporteCuotaRegular(importeCuotaRegular);

        FalActaPlanPagoRef guardado = planRepo.save(nuevo);

        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Obligacion no encontrada: " + obligacionPagoId));
        obligacion.setEstadoObligacion(EstadoObligacionPago.EN_PLAN);
        obligacionRepo.save(obligacion);

        return guardado;
    }

    /**
     * Operacion atomica de refinanciacion:
     * 1. plan anterior pasa a REFINANCIADO, siVigente=false
     * 2. nueva forma REFINANCIACION generada (via FormaPagoService)
     * 3. nuevo plan activo creado
     * Devuelve el nuevo plan.
     */
    public FalActaPlanPagoRef refinanciar(
            Long obligacionPagoId,
            Long nuevaFormaPagoId,
            short idTdocPlanNuevo,
            long idDocPlanNuevo,
            short cantidadCuotasNuevo,
            BigDecimal importeTotalNuevo,
            BigDecimal importeCuotaRegularNuevo) {
        FalActaPlanPagoRef anteriorVigente = planRepo.findVigenteByObligacionPagoId(obligacionPagoId)
                .orElseThrow(() -> new PlanPagoNoEncontradoException(
                        "No hay plan vigente para obligacionPagoId=" + obligacionPagoId));
        if (!anteriorVigente.estaActivo() && !anteriorVigente.estaCaido())
            throw new PrecondicionVioladaException(
                    "El plan debe estar ACTIVO o CAIDO para refinanciar. Estado: " + anteriorVigente.getEstadoPlan());

        LocalDateTime ahora = LocalDateTime.now();
        anteriorVigente.setFhRefinanciacion(ahora);

        Long nuevoId = planRepo.nextId();
        FalActaPlanPagoRef nuevo = new FalActaPlanPagoRef(
                nuevoId, nuevaFormaPagoId, obligacionPagoId,
                idTdocPlanNuevo, idDocPlanNuevo, cantidadCuotasNuevo, importeTotalNuevo);
        nuevo.setFhGeneracionPlan(ahora);
        if (importeCuotaRegularNuevo != null) nuevo.setImporteCuotaRegular(importeCuotaRegularNuevo);
        nuevo.setPlanRefinanciadoId(anteriorVigente.getId());

        FalActaPlanPagoRef guardado = planRepo.refinanciarAtomico(nuevo, anteriorVigente);

        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Obligacion no encontrada: " + obligacionPagoId));
        obligacion.setEstadoObligacion(EstadoObligacionPago.REFINANCIADA);
        obligacion.setFormaPagoVigenteId(nuevaFormaPagoId);
        obligacionRepo.save(obligacion);

        return guardado;
    }

    public FalActaPlanPagoRef actualizarEstado(Long planId, EstadoPlanPago nuevoEstado) {
        FalActaPlanPagoRef plan = planRepo.findById(planId)
                .orElseThrow(() -> new PlanPagoNoEncontradoException(planId));
        plan.setEstadoPlan(nuevoEstado);
        return planRepo.save(plan);
    }

    public Optional<FalActaPlanPagoRef> buscarVigenteByObligacion(Long obligacionPagoId) {
        return planRepo.findVigenteByObligacionPagoId(obligacionPagoId);
    }

    public List<FalActaPlanPagoRef> buscarHistorialByObligacion(Long obligacionPagoId) {
        return planRepo.findByObligacionPagoId(obligacionPagoId);
    }

    public FalActaPlanPagoRef buscarPorId(Long id) {
        return planRepo.findById(id)
                .orElseThrow(() -> new PlanPagoNoEncontradoException(id));
    }
}
