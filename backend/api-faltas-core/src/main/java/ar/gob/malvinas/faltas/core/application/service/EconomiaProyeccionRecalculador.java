package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoConciliacionActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoPlanCaidoCalculado;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenUltimaActualizacion;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.EconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EconomiaProyeccionRecalculador {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final short UMBRAL_MORA_CONSEC = 2;

    private final ObligacionPagoRepository obligacionRepo;
    private final FormaPagoRepository formaRepo;
    private final PlanPagoRefRepository planRepo;
    private final PagoMovimientoRepository movimientoRepo;
    private final EconomiaProyeccionRepository proyeccionRepo;
    private final ActaRepository actaRepo;
    private final FaltasClock clock;
    private final TesoreriaConciliacionInput tesoreriaInput;

    public EconomiaProyeccionRecalculador(
            ObligacionPagoRepository obligacionRepo,
            FormaPagoRepository formaRepo,
            PlanPagoRefRepository planRepo,
            PagoMovimientoRepository movimientoRepo,
            EconomiaProyeccionRepository proyeccionRepo,
            ActaRepository actaRepo,
            FaltasClock clock,
            TesoreriaConciliacionInput tesoreriaInput) {
        this.obligacionRepo = obligacionRepo;
        this.formaRepo = formaRepo;
        this.planRepo = planRepo;
        this.movimientoRepo = movimientoRepo;
        this.proyeccionRepo = proyeccionRepo;
        this.actaRepo = actaRepo;
        this.clock = clock;
        this.tesoreriaInput = tesoreriaInput;
    }

    public FalActaEconomiaProyeccion recalcular(Long actaId, OrigenUltimaActualizacion origen, String actorId) {
        LocalDateTime corte = clock.now();
        Optional<FalActaObligacionPago> oblOpt = obligacionRepo.findVigenteByActaId(actaId);
        if (oblOpt.isEmpty()) {
            proyeccionRepo.deleteByActaId(actaId);
            return null;
        }
        FalActaObligacionPago obligacion = oblOpt.get();
        FalActaEconomiaProyeccion p = proyeccionRepo.findByActaId(actaId).orElse(new FalActaEconomiaProyeccion(actaId));

        p.setObligacionVigenteId(obligacion.getId());
        p.setTipoObligacion(obligacion.getTipoObligacion());
        p.setEstadoObligacion(obligacion.getEstadoObligacion());
        p.setMontoObligacionVigente(obligacion.getMontoOriginal());

        if (obligacion.getFormaPagoVigenteId() != null) {
            formaRepo.findById(obligacion.getFormaPagoVigenteId()).ifPresent(forma -> {
                p.setFormaPagoVigenteId(forma.getId());
                p.setTipoFormaPago(forma.getTipoFormaPago());
                p.setEstadoFormaPago(forma.getEstadoFormaPago());
            });
        } else {
            p.setFormaPagoVigenteId(null);
            p.setTipoFormaPago(null);
            p.setEstadoFormaPago(null);
        }

        planRepo.findVigenteByObligacionPagoId(obligacion.getId()).ifPresentOrElse(plan -> {
            p.setPlanPagoVigenteId(plan.getId());
            p.setEstadoPlan(plan.getEstadoPlan());
            p.setCantidadCuotas(plan.getCantidadCuotas());
            p.setImporteCuotaRegular(plan.getImporteCuotaRegular());
            p.setImporteVencidoPlan(calcularImporteVencidoPlan(p));
            calcularPlanCaido(p, corte);
        }, () -> {
            p.setPlanPagoVigenteId(null);
            p.setEstadoPlan(null);
            p.setCantidadCuotas(null);
            p.setImporteCuotaRegular(null);
            p.setImporteVencidoPlan(null);
            p.setSiPlanCaidoCalculado(false);
            p.setFhDesdePlanCaidoCalculado(null);
            p.setMotivoPlanCaidoCalculado(null);
        });

        List<FalActaPagoMovimiento> movimientos = ordenar(movimientoRepo.findByObligacionPagoId(obligacion.getId()));
        Map<Long, FalActaPagoMovimiento> porId = movimientos.stream()
                .collect(Collectors.toMap(FalActaPagoMovimiento::getId, Function.identity(), (a, b) -> a));

        BigDecimal procesado = ZERO;
        BigDecimal evidenciaPend = ZERO;
        BigDecimal tesoreria = ZERO;
        BigDecimal observado = ZERO;
        BigDecimal revertido = ZERO;
        BigDecimal totalConfirmado = ZERO;
        boolean hayProcesado = false;
        Long ultimoMovId = null;

        for (FalActaPagoMovimiento m : movimientos) {
            ultimoMovId = m.getId();
            BigDecimal imp = importeMovimiento(m);
            switch (m.getTipoMovimiento()) {
                case PAGO_PROCESADO -> {
                    procesado = procesado.add(imp);
                    hayProcesado = true;
                }
                case PAGO_CONFIRMADO -> {
                    totalConfirmado = totalConfirmado.add(imp);
                    if (clasificaTesoreria(m)) {
                        tesoreria = tesoreria.add(imp);
                    } else {
                        evidenciaPend = evidenciaPend.add(imp);
                    }
                }
                case PAGO_REVERTIDO -> {
                    revertido = revertido.add(imp);
                    FalActaPagoMovimiento orig = m.getMovimientoOrigenId() == null
                            ? null : porId.get(m.getMovimientoOrigenId());
                    if (orig != null && orig.getTipoMovimiento() == TipoMovimientoPago.PAGO_CONFIRMADO) {
                        if (clasificaTesoreria(orig)) {
                            tesoreria = tesoreria.subtract(imp);
                        } else {
                            evidenciaPend = evidenciaPend.subtract(imp);
                        }
                    } else {
                        evidenciaPend = evidenciaPend.subtract(imp);
                    }
                }
                case DEUDA_EMITIDA, EMISION_ANULADA -> { }
            }
        }

        boolean hayConfirmado = hayPagoConfirmadoActivo(movimientos);
        BigDecimal monto = obligacion.getMontoOriginal() != null ? obligacion.getMontoOriginal() : ZERO;
        BigDecimal aplicadoNeto = totalConfirmado.subtract(revertido).max(ZERO);
        BigDecimal aplicado = aplicadoNeto.min(monto);
        BigDecimal saldo = monto.subtract(aplicado).max(ZERO);

        tesoreria = tesoreria.max(ZERO);
        evidenciaPend = evidenciaPend.max(ZERO);

        p.setImportePagoProcesado(procesado);
        p.setImporteConfirmadoEvidenciaPendiente(evidenciaPend);
        p.setImporteConfirmadoTesoreria(tesoreria);
        p.setImporteObservadoTesoreria(observado);
        p.setImporteRevertido(revertido);
        p.setImporteAplicadoTotal(aplicado);
        p.setSaldoPendiente(saldo);
        p.setSiPagoProcesado(hayProcesado);
        p.setSiPagoConfirmado(hayConfirmado);
        p.setUltimoMovimientoIdProyectado(ultimoMovId);
        p.setEstadoConciliacionActual(calcularEstadoConciliacion(observado, evidenciaPend, tesoreria, hayConfirmado));
        p.setSiConciliacionPendiente(evidenciaPend.compareTo(ZERO) > 0);
        p.setSiReaperturaRequerida(actaCerrada(actaId) && revertido.compareTo(ZERO) > 0 && saldo.compareTo(ZERO) > 0);
        p.setFhCorteEconomico(corte);
        p.setFhUltMod(corte);
        p.setIdUserUltMod(actorId);
        p.setOrigenUltimaActualizacion(origen);
        if (origen == OrigenUltimaActualizacion.SINCRONIZACION_NOCTURNA) {
            p.setFhUltimaSincronizacion(corte);
        }

        return proyeccionRepo.save(p);
    }

    private boolean clasificaTesoreria(FalActaPagoMovimiento m) {
        if (tesoreriaInput.estaConciliadoTesoreria(m.getId())) {
            return true;
        }
        OrigenConfirmacion oc = m.getOrigenConfirmacion();
        if (oc == null) {
            return false;
        }
        return oc == OrigenConfirmacion.TESORERIA
                || oc == OrigenConfirmacion.CAJA
                || oc == OrigenConfirmacion.INGRESOS;
    }

    private boolean actaCerrada(Long actaId) {
        return actaRepo.buscarPorId(actaId)
                .map(a -> a.getBloqueActual() == BloqueActual.CERR
                        || a.getSituacionAdministrativa() == SituacionAdministrativaActa.CERRADA)
                .orElse(false);
    }

    public EstadoObligacionPago proyectarEstadoObligacion(FalActaObligacionPago obligacion, List<FalActaPagoMovimiento> movimientos) {
        EstadoObligacionPago estado = obligacion.getEstadoObligacion() != null
                ? obligacion.getEstadoObligacion() : EstadoObligacionPago.DETERMINADA;
        boolean hayConfirmado = hayPagoConfirmadoActivo(movimientos);
        BigDecimal monto = obligacion.getMontoOriginal() != null ? obligacion.getMontoOriginal() : ZERO;
        BigDecimal aplicado = calcularAplicadoNeto(ordenar(movimientos), monto);
        if (aplicado.compareTo(monto) >= 0 && monto.compareTo(ZERO) > 0) {
            return EstadoObligacionPago.CANCELADA_POR_PAGO;
        }
        if (estado == EstadoObligacionPago.CANCELADA_POR_PAGO) {
            return obligacion.getFormaPagoVigenteId() != null
                    ? EstadoObligacionPago.CON_FORMA_PAGO_VIGENTE
                    : EstadoObligacionPago.PENDIENTE_FORMA_PAGO;
        }
        boolean deudaEmitida = ordenar(movimientos).stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.DEUDA_EMITIDA);
        if (deudaEmitida && estado == EstadoObligacionPago.DETERMINADA) {
            return EstadoObligacionPago.PENDIENTE_FORMA_PAGO;
        }
        return estado;
    }

    public EstadoFormaPago proyectarEstadoForma(FalActaFormaPago forma, List<FalActaPagoMovimiento> movimientosForma) {
        EstadoFormaPago estado = forma.getEstadoFormaPago() != null ? forma.getEstadoFormaPago() : EstadoFormaPago.GENERADA;
        List<FalActaPagoMovimiento> movs = ordenar(movimientosForma);
        boolean confirmado = movs.stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.PAGO_CONFIRMADO && !estaRevertido(m, movs));
        boolean procesado = movs.stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.PAGO_PROCESADO && !estaRevertido(m, movs));
        if (confirmado) return EstadoFormaPago.PAGADA;
        if (procesado) return EstadoFormaPago.VIGENTE;
        if (estado == EstadoFormaPago.REEMPLAZADA || estado == EstadoFormaPago.ANULADA) return estado;
        if (estado == EstadoFormaPago.PAGADA) return EstadoFormaPago.VIGENTE;
        return estado;
    }

    public EstadoPlanPago proyectarEstadoPlan(FalActaPlanPagoRef plan, List<FalActaPagoMovimiento> movimientosPlan) {
        if (plan.getEstadoPlan() == EstadoPlanPago.REFINANCIADO || plan.getEstadoPlan() == EstadoPlanPago.ANULADO) {
            return plan.getEstadoPlan();
        }
        List<FalActaPagoMovimiento> movs = ordenar(movimientosPlan);
        boolean pagado = movs.stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.PAGO_CONFIRMADO && !estaRevertido(m, movs));
        if (pagado && plan.getImporteTotalPlan() != null) {
            BigDecimal aplicado = calcularAplicadoNeto(movs, plan.getImporteTotalPlan());
            if (aplicado.compareTo(plan.getImporteTotalPlan()) >= 0) return EstadoPlanPago.FINALIZADO_POR_PAGO;
        }
        return plan.getEstadoPlan() != null ? plan.getEstadoPlan() : EstadoPlanPago.ACTIVO;
    }

    public boolean hayPagoProcesadoActivo(List<FalActaPagoMovimiento> movimientos) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientos);
        return ordenados.stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.PAGO_PROCESADO && !estaRevertido(m, ordenados));
    }

    public boolean hayPagoConfirmadoActivo(List<FalActaPagoMovimiento> movimientos) {
        List<FalActaPagoMovimiento> ordenados = ordenar(movimientos);
        return ordenados.stream().anyMatch(m -> m.getTipoMovimiento() == TipoMovimientoPago.PAGO_CONFIRMADO && !estaRevertido(m, ordenados));
    }

    private BigDecimal calcularAplicadoNeto(List<FalActaPagoMovimiento> movimientos, BigDecimal tope) {
        BigDecimal confirmado = ZERO;
        BigDecimal revertido = ZERO;
        for (FalActaPagoMovimiento m : movimientos) {
            if (m.getTipoMovimiento() == TipoMovimientoPago.PAGO_CONFIRMADO) {
                confirmado = confirmado.add(importeMovimiento(m));
            } else if (m.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO) {
                revertido = revertido.add(importeMovimiento(m));
            }
        }
        BigDecimal aplicado = confirmado.subtract(revertido).max(ZERO);
        return aplicado.min(tope != null ? tope : aplicado);
    }

    private void calcularPlanCaido(FalActaEconomiaProyeccion p, LocalDateTime corte) {
        boolean caido = false;
        MotivoPlanCaidoCalculado motivo = null;
        Short moraConsec = p.getCantidadCuotasMoraConsec();
        Short mora = p.getCantidadCuotasEnMora();
        Short dias = p.getDiasMoraMax();
        if (moraConsec != null && moraConsec >= UMBRAL_MORA_CONSEC) {
            caido = true; motivo = MotivoPlanCaidoCalculado.MORA_CONSECUTIVA;
        } else if (mora != null && mora > 0) {
            caido = true; motivo = MotivoPlanCaidoCalculado.CUOTAS_EN_MORA;
        } else if (dias != null && dias > 30) {
            caido = true; motivo = MotivoPlanCaidoCalculado.ANTIGUEDAD_MORA;
        }
        if (caido && !p.isSiPlanCaidoCalculado()) p.setFhDesdePlanCaidoCalculado(corte);
        if (!caido) p.setFhDesdePlanCaidoCalculado(null);
        p.setSiPlanCaidoCalculado(caido);
        p.setMotivoPlanCaidoCalculado(motivo);
    }

    private BigDecimal calcularImporteVencidoPlan(FalActaEconomiaProyeccion p) {
        if (p.getImporteCuotaRegular() == null || p.getCantidadCuotasVencidas() == null) return null;
        return p.getImporteCuotaRegular().multiply(BigDecimal.valueOf(p.getCantidadCuotasVencidas()));
    }

    private EstadoConciliacionActual calcularEstadoConciliacion(BigDecimal observado, BigDecimal evidencia, BigDecimal tesoreria, boolean hayConfirmado) {
        if (!hayConfirmado) return EstadoConciliacionActual.NO_APLICA;
        if (observado.compareTo(ZERO) > 0) return EstadoConciliacionActual.OBSERVADO_TESORERIA;
        if (evidencia.compareTo(ZERO) > 0) return EstadoConciliacionActual.PENDIENTE_TESORERIA;
        if (tesoreria.compareTo(ZERO) > 0) return EstadoConciliacionActual.CONCILIADO_TESORERIA;
        return EstadoConciliacionActual.NO_APLICA;
    }

    private boolean estaRevertido(FalActaPagoMovimiento m, List<FalActaPagoMovimiento> todos) {
        return todos.stream().anyMatch(r -> r.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO
                && m.getId().equals(r.getMovimientoOrigenId()));
    }

    private BigDecimal importeMovimiento(FalActaPagoMovimiento m) {
        if (m.getImporteTotal() != null) return m.getImporteTotal();
        return ZERO;
    }

    private List<FalActaPagoMovimiento> ordenar(List<FalActaPagoMovimiento> movimientos) {
        return movimientos.stream()
                .sorted(Comparator.comparing(FalActaPagoMovimiento::getFhMovimiento)
                        .thenComparingLong(FalActaPagoMovimiento::getId))
                .toList();
    }
}
