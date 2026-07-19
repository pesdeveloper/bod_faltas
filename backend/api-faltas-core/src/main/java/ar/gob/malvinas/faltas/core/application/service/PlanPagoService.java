package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenEvento;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.exception.PlanPagoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PlanPagoService {

    private final PlanPagoRefRepository planRepo;
    private final FormaPagoRepository formaRepo;
    private final ObligacionPagoRepository obligacionRepo;
    private final ActaEventoRepository eventoRepo;
    private final EconomiaProyeccionRecalculador recalculador;
    private final FaltasClock clock;

    public PlanPagoService(PlanPagoRefRepository planRepo, FormaPagoRepository formaRepo,
            ObligacionPagoRepository obligacionRepo, ActaEventoRepository eventoRepo,
            EconomiaProyeccionRecalculador recalculador, FaltasClock clock) {
        this.planRepo = planRepo;
        this.formaRepo = formaRepo;
        this.obligacionRepo = obligacionRepo;
        this.eventoRepo = eventoRepo;
        this.recalculador = recalculador;
        this.clock = clock;
    }

    public FalActaPlanPagoRef generarPlan(Long formaPagoId, Long obligacionPagoId, short idTdocPlan, long idDocPlan,
            short cantidadCuotas, BigDecimal importeTotalPlan, BigDecimal importeCuotaRegular) {
        FalActaFormaPago forma = formaRepo.findById(formaPagoId)
                .orElseThrow(() -> new PrecondicionVioladaException("Forma de pago no encontrada: " + formaPagoId));
        if (!forma.esPlan())
            throw new PrecondicionVioladaException("La forma debe ser PLAN_PAGO o REFINANCIACION");
        if (planRepo.findVigenteByObligacionPagoId(obligacionPagoId).isPresent()
                && forma.getTipoFormaPago() == TipoFormaPago.PLAN_PAGO)
            throw new PrecondicionVioladaException("Ya existe un plan vigente. Usar refinanciarPlan.");

        Long nuevoId = planRepo.nextId();
        FalActaPlanPagoRef nuevo = new FalActaPlanPagoRef(
                nuevoId, formaPagoId, obligacionPagoId, idTdocPlan, idDocPlan, cantidadCuotas, importeTotalPlan);
        nuevo.setFhGeneracionPlan(clock.now());
        if (importeCuotaRegular != null) nuevo.setImporteCuotaRegular(importeCuotaRegular);
        FalActaPlanPagoRef guardado = planRepo.save(nuevo);

        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId).orElseThrow();
        obligacion.setEstadoObligacion(EstadoObligacionPago.CON_FORMA_PAGO_VIGENTE);
        obligacionRepo.save(obligacion);

        String actor = ActorContextHolder.subOr("SISTEMA");
        emitirEvento(obligacion.getActaId(), TipoEventoActa.PLNGEN, actor, "Plan de pago generado");
        recalculador.recalcular(obligacion.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return guardado;
    }

    public FalActaPlanPagoRef refinanciar(Long obligacionPagoId, Long nuevaFormaPagoId, short idTdocPlanNuevo,
            long idDocPlanNuevo, short cantidadCuotasNuevo, BigDecimal importeTotalNuevo,
            BigDecimal importeCuotaRegularNuevo) {
        FalActaPlanPagoRef anteriorVigente = planRepo.findVigenteByObligacionPagoId(obligacionPagoId)
                .orElseThrow(() -> new PlanPagoNoEncontradoException("No hay plan vigente para obligacionPagoId=" + obligacionPagoId));
        if (!anteriorVigente.estaActivo() && !anteriorVigente.estaAnulado())
            throw new PrecondicionVioladaException("El plan debe estar ACTIVO o ANULADO para refinanciar");

        var ahora = clock.now();
        anteriorVigente.setFhRefinanciacion(ahora);
        Long nuevoId = planRepo.nextId();
        FalActaPlanPagoRef nuevo = new FalActaPlanPagoRef(
                nuevoId, nuevaFormaPagoId, obligacionPagoId, idTdocPlanNuevo, idDocPlanNuevo,
                cantidadCuotasNuevo, importeTotalNuevo);
        nuevo.setFhGeneracionPlan(ahora);
        if (importeCuotaRegularNuevo != null) nuevo.setImporteCuotaRegular(importeCuotaRegularNuevo);
        nuevo.setPlanRefinanciadoId(anteriorVigente.getId());
        FalActaPlanPagoRef guardado = planRepo.refinanciarAtomico(nuevo, anteriorVigente);

        FalActaObligacionPago obligacion = obligacionRepo.findById(obligacionPagoId).orElseThrow();
        obligacion.setEstadoObligacion(EstadoObligacionPago.CON_FORMA_PAGO_VIGENTE);
        obligacion.setFormaPagoVigenteId(nuevaFormaPagoId);
        obligacionRepo.save(obligacion);

        String actor = ActorContextHolder.subOr("SISTEMA");
        emitirEvento(obligacion.getActaId(), TipoEventoActa.PLNREF, actor, "Plan refinanciado");
        recalculador.recalcular(obligacion.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return guardado;
    }

    public FalActaPlanPagoRef anular(Long planId, String idUser) {
        FalActaPlanPagoRef plan = planRepo.findById(planId).orElseThrow(() -> new PlanPagoNoEncontradoException(planId));
        plan.setEstadoPlan(EstadoPlanPago.ANULADO);
        plan.setSiVigente(false);
        plan.setFhCancelacion(clock.now());
        FalActaPlanPagoRef saved = planRepo.save(plan);
        FalActaObligacionPago obl = obligacionRepo.findById(plan.getObligacionPagoId()).orElseThrow();
        String actor = ActorContextHolder.subOr(idUser);
        emitirEvento(obl.getActaId(), TipoEventoActa.PLNANU, actor, "Plan de pago anulado");
        recalculador.recalcular(obl.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
        return saved;
    }

    public FalActaPlanPagoRef actualizarEstado(Long planId, EstadoPlanPago nuevoEstado) {
        if (nuevoEstado == EstadoPlanPago.FINALIZADO_POR_PAGO) {
            throw new PrecondicionVioladaException(
                    "FINALIZADO_POR_PAGO es una transicion atomica automatica (siVigente=false, fhFinalizacionPago) " +
                    "y no puede establecerse mediante el metodo generico actualizarEstado.");
        }
        FalActaPlanPagoRef plan = planRepo.findById(planId).orElseThrow(() -> new PlanPagoNoEncontradoException(planId));
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
        return planRepo.findById(id).orElseThrow(() -> new PlanPagoNoEncontradoException(id));
    }

    private void emitirEvento(Long actaId, TipoEventoActa tipo, String actor, String desc) {
        eventoRepo.registrar(FalActaEvento.builder()
                .actaId(actaId).tipoEvt(tipo).origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(clock.now()).idUserEvt(actor).descripcionLegible(desc).build());
    }
}
