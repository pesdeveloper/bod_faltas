package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.MovimientoRegistroResult;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PagoIntegracionService {

    private final PagoMovimientoService movimientoService;
    private final ObligacionPagoRepository obligacionRepo;
    private final FormaPagoRepository formaRepo;
    private final PlanPagoRefRepository planRepo;
    private final PagoMovimientoRepository movimientoRepo;
    private final PagoMovimientoReducer reducer;
    private final EconomiaProyeccionRecalculador recalculador;
    private final FaltasClock clock;

    public PagoIntegracionService(
            PagoMovimientoService movimientoService,
            ObligacionPagoRepository obligacionRepo,
            FormaPagoRepository formaRepo,
            PlanPagoRefRepository planRepo,
            PagoMovimientoRepository movimientoRepo,
            PagoMovimientoReducer reducer,
            EconomiaProyeccionRecalculador recalculador,
            FaltasClock clock) {
        this.movimientoService = movimientoService;
        this.obligacionRepo = obligacionRepo;
        this.formaRepo = formaRepo;
        this.planRepo = planRepo;
        this.movimientoRepo = movimientoRepo;
        this.reducer = reducer;
        this.recalculador = recalculador;
        this.clock = clock;
    }

    public RegistroMovimientoOutcome notificarMovimiento(NotificarMovimientoPagoCommand cmd) {
        RegistroMovimientoOutcome outcome = movimientoService.registrar(
                cmd.obligacionPagoId(), cmd.formaPagoId(), cmd.planPagoRefId(),
                cmd.tipoMovimiento(), cmd.origenMovimiento(), cmd.origenConfirmacion(), cmd.evidenciaDocumentoId(),
                cmd.clasificacionPago(), cmd.nroCuota(),
                cmd.importeCapital(), cmd.importeRima(), cmd.importeTotal(),
                cmd.cmteEM(), cmd.prefEM(), cmd.nroEM(),
                cmd.cmtePG(), cmd.prefPG(), cmd.nroPG(),
                cmd.idCierre(), cmd.idOpe(),
                cmd.fhPagoProcesado(), cmd.fhPagoConfirmado(),
                cmd.referenciaExterna(),
                cmd.fhMovimiento(),
                cmd.idUser());

        if (outcome.resultado() == MovimientoRegistroResult.CREATED) {
            recalcularEstados(cmd.obligacionPagoId(), cmd.formaPagoId(), cmd.planPagoRefId());
        }
        return outcome;
    }

    public void recalcularEstados(Long obligacionPagoId, Long formaPagoId, Long planPagoRefId) {
        Optional<FalActaObligacionPago> obligOpt = obligacionRepo.findById(obligacionPagoId);
        if (obligOpt.isEmpty()) return;
        FalActaObligacionPago obligacion = obligOpt.get();
        String actor = ActorContextHolder.subOr("SISTEMA");

        List<FalActaPagoMovimiento> todosMovimientos = movimientoRepo.findByObligacionPagoId(obligacionPagoId);
        EstadoObligacionPago nuevoEstadoOblig = reducer.proyectarEstadoObligacion(obligacion, todosMovimientos);

        if (obligacion.getEstadoObligacion() != nuevoEstadoOblig) {
            obligacion.setEstadoObligacion(nuevoEstadoOblig);
            if (nuevoEstadoOblig == EstadoObligacionPago.CANCELADA_POR_PAGO) {
                obligacion.setFhCancelacion(clock.now());
            } else {
                obligacion.setFhCancelacion(null);
            }
            obligacionRepo.save(obligacion);
        }

        if (formaPagoId != null) {
            formaRepo.findById(formaPagoId).ifPresent(forma -> {
                List<FalActaPagoMovimiento> movForma = movimientoRepo.findByFormaPagoId(formaPagoId);
                EstadoFormaPago nuevoEstadoForma = reducer.proyectarEstadoForma(forma, movForma);
                if (forma.getEstadoFormaPago() != nuevoEstadoForma) {
                    forma.setEstadoFormaPago(nuevoEstadoForma);
                    if (nuevoEstadoForma == EstadoFormaPago.PAGADA && forma.getFhPagoConfirmado() == null) {
                        forma.setFhPagoConfirmado(clock.now());
                    } else if (nuevoEstadoForma != EstadoFormaPago.PAGADA) {
                        forma.setFhPagoConfirmado(null);
                    }
                    formaRepo.save(forma);
                }
            });
        }

        if (planPagoRefId != null) {
            planRepo.findById(planPagoRefId).ifPresent(plan -> {
                List<FalActaPagoMovimiento> movPlan = movimientoRepo.findByPlanPagoRefId(planPagoRefId);
                EstadoPlanPago nuevoEstadoPlan = reducer.proyectarEstadoPlan(plan, movPlan);
                if (plan.getEstadoPlan() != nuevoEstadoPlan) {
                    plan.setEstadoPlan(nuevoEstadoPlan);
                    if (nuevoEstadoPlan == EstadoPlanPago.FINALIZADO_POR_PAGO) {
                        plan.setSiVigente(false);
                        plan.setFhFinalizacionPago(clock.now());
                    }
                    planRepo.save(plan);
                }
            });
        }

        recalculador.recalcular(obligacion.getActaId(), OrigenUltimaActualizacion.TIEMPO_REAL, actor);
    }

    public boolean pagoConfirmadoPorTesoreria(Long obligacionPagoId) {
        List<FalActaPagoMovimiento> movimientos = movimientoRepo.findByObligacionPagoId(obligacionPagoId);
        return reducer.hayPagoConfirmadoActivo(movimientos);
    }
}
