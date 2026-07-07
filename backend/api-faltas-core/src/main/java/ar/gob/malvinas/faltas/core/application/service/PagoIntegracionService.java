package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de integracion con Ingresos/Tesoreria.
 * Recibe notificaciones de movimientos y proyecta estado en el modelo Faltas.
 *
 * POST /api/faltas/pagos/notificar-movimiento (endpoint conceptual).
 * Idempotente por referenciaExterna unica cuando presente.
 * Soporta fuera de orden: recalcula estado desde historial completo.
 * El acta nunca se cierra automaticamente desde aqui; el cierre requiere accion explicita.
 */
@Service
public class PagoIntegracionService {

    private final PagoMovimientoService movimientoService;
    private final ObligacionPagoRepository obligacionRepo;
    private final FormaPagoRepository formaRepo;
    private final PlanPagoRefRepository planRepo;
    private final PagoMovimientoRepository movimientoRepo;
    private final PagoMovimientoReducer reducer;

    public PagoIntegracionService(
            PagoMovimientoService movimientoService,
            ObligacionPagoRepository obligacionRepo,
            FormaPagoRepository formaRepo,
            PlanPagoRefRepository planRepo,
            PagoMovimientoRepository movimientoRepo,
            PagoMovimientoReducer reducer) {
        this.movimientoService = movimientoService;
        this.obligacionRepo = obligacionRepo;
        this.formaRepo = formaRepo;
        this.planRepo = planRepo;
        this.movimientoRepo = movimientoRepo;
        this.reducer = reducer;
    }

    /**
     * Procesa notificacion de movimiento desde Ingresos/Tesoreria.
     * Idempotente: si referenciaExterna ya existe con mismo tipo/obligacion, devuelve existente.
     * Recalcula estado de obligacion, forma y plan desde historial completo.
     */
    public FalActaPagoMovimiento notificarMovimiento(NotificarMovimientoPagoCommand cmd) {
        FalActaPagoMovimiento movimiento = movimientoService.registrar(
                cmd.obligacionPagoId(), cmd.formaPagoId(), cmd.planPagoRefId(),
                cmd.tipoMovimiento(),
                cmd.nroCuota(),
                cmd.importeCapital(), cmd.importeRima(), cmd.importeTotal(),
                cmd.cmteEM(), cmd.prefEM(), cmd.nroEM(),
                cmd.cmtePG(), cmd.prefPG(), cmd.nroPG(),
                cmd.idCierre(), cmd.idOpe(),
                cmd.fhPagoProcesado(), cmd.fhPagoConfirmado(),
                cmd.referenciaExterna(),
                cmd.fhMovimiento(),
                cmd.idUser());

        recalcularEstados(cmd.obligacionPagoId(), cmd.formaPagoId(), cmd.planPagoRefId());
        return movimiento;
    }

    /**
     * Recalcula y persiste estados de obligacion, forma y plan
     * a partir del historial completo de movimientos.
     */
    public void recalcularEstados(Long obligacionPagoId, Long formaPagoId, Long planPagoRefId) {
        Optional<FalActaObligacionPago> obligOpt = obligacionRepo.findById(obligacionPagoId);
        if (obligOpt.isEmpty()) return;
        FalActaObligacionPago obligacion = obligOpt.get();

        List<FalActaPagoMovimiento> todosMovimientos = movimientoRepo.findByObligacionPagoId(obligacionPagoId);
        EstadoObligacionPago nuevoEstadoOblig = reducer.proyectarEstadoObligacion(obligacion, todosMovimientos);

        if (obligacion.getEstadoObligacion() != nuevoEstadoOblig) {
            obligacion.setEstadoObligacion(nuevoEstadoOblig);
            if (nuevoEstadoOblig == EstadoObligacionPago.CANCELADA) {
                obligacion.setFhCancelacion(LocalDateTime.now());
            }
            obligacionRepo.save(obligacion);
        }

        if (formaPagoId != null) {
            formaRepo.findById(formaPagoId).ifPresent(forma -> {
                List<FalActaPagoMovimiento> movForma = movimientoRepo.findByFormaPagoId(formaPagoId);
                EstadoFormaPago nuevoEstadoForma = reducer.proyectarEstadoForma(forma, movForma);
                if (forma.getEstadoFormaPago() != nuevoEstadoForma) {
                    forma.setEstadoFormaPago(nuevoEstadoForma);
                    if (nuevoEstadoForma == EstadoFormaPago.CONFIRMADA && forma.getFhPagoConfirmado() == null) {
                        forma.setFhPagoConfirmado(LocalDateTime.now());
                    }
                    formaRepo.save(forma);
                }
            });
        }

        if (planPagoRefId != null) {
            planRepo.findById(planPagoRefId).ifPresent(plan -> {
                List<FalActaPagoMovimiento> movPlan = movimientoRepo.findByPlanPagoRefId(planPagoRefId);
                var nuevoEstadoPlan = reducer.proyectarEstadoPlan(plan, movPlan);
                if (plan.getEstadoPlan() != nuevoEstadoPlan) {
                    plan.setEstadoPlan(nuevoEstadoPlan);
                    planRepo.save(plan);
                }
            });
        }
    }

    /**
     * Indica si el pago esta confirmado por Tesoreria (puede habilitar cierre si no hay bloqueantes).
     * Procesado solo NO habilita cierre.
     */
    public boolean pagoConfirmadoPorTesoreria(Long obligacionPagoId) {
        List<FalActaPagoMovimiento> movimientos = movimientoRepo.findByObligacionPagoId(obligacionPagoId);
        return reducer.hayPagoConfirmadoActivo(movimientos);
    }
}
