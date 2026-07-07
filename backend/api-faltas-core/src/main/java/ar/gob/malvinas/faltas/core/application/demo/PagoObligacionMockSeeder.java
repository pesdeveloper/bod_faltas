package ar.gob.malvinas.faltas.core.application.demo;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.repository.FormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.ObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.PagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.repository.PlanPagoRefRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Casos demo del nuevo modelo de pagos (Slice 8F-11H).
 *
 * Caso 1 - Acta 101: Pago voluntario contado confirmado
 *   Obligacion pago voluntario -> forma contado -> DEUDA_EMITIDA -> PAGO_PROCESADO -> OBLIGACION_CANCELADA
 *
 * Caso 2 - Acta 102: Plan de pago en mora
 *   Obligacion condena -> forma plan -> PLAN_GENERADO -> EN_PLAN -> PLAN_EN_MORA
 *
 * Caso 3 - Acta 103: Refinanciacion de plan
 *   Obligacion condena -> plan original -> PLAN_GENERADO -> PLAN_REFINANCIADO -> nuevo plan vigente
 *
 * IDs: obligaciones 1001-1003, formas 2001-2004, planes 3001-3003, movimientos 4001+
 */
@Component
public class PagoObligacionMockSeeder {

    private final ObligacionPagoRepository obligacionRepo;
    private final FormaPagoRepository formaRepo;
    private final PlanPagoRefRepository planRepo;
    private final PagoMovimientoRepository movimientoRepo;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 7, 1, 9, 0);

    public PagoObligacionMockSeeder(
            ObligacionPagoRepository obligacionRepo,
            FormaPagoRepository formaRepo,
            PlanPagoRefRepository planRepo,
            PagoMovimientoRepository movimientoRepo) {
        this.obligacionRepo = obligacionRepo;
        this.formaRepo = formaRepo;
        this.planRepo = planRepo;
        this.movimientoRepo = movimientoRepo;
    }

    @PostConstruct
    public void seed() {
        caso1_pagoVoluntarioContadoConfirmado();
        caso2_planDePagoEnMora();
        caso3_refinanciacionDePlan();
    }

    /**
     * Caso 1: Acta 101 - Pago voluntario contado, confirmado por Tesoreria.
     */
    private void caso1_pagoVoluntarioContadoConfirmado() {
        // Obligacion pago voluntario por 1500.00
        FalActaObligacionPago oblig = new FalActaObligacionPago(
                1001L, 101L, 200L, TipoObligacionPago.PAGO_VOLUNTARIO,
                new BigDecimal("1500.00"), BASE, "SYSTEM", BASE, "SYSTEM");
        obligacionRepo.save(oblig);

        // Forma contado
        FalActaFormaPago forma = new FalActaFormaPago(
                2001L, 1001L, (short) 1, TipoFormaPago.CONTADO,
                new BigDecimal("1500.00"), BASE, BASE, "SYSTEM");
        formaRepo.save(forma);

        // Movimientos: deuda -> pago procesado -> obligacion cancelada
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4001L, 1001L, TipoMovimientoPago.DEUDA_EMITIDA, BASE.plusHours(1), BASE.plusHours(1), "SYS")
                .formaPagoId(2001L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4002L, 1001L, TipoMovimientoPago.PAGO_CONTADO_GENERADO, BASE.plusHours(2), BASE.plusHours(2), "SYS")
                .formaPagoId(2001L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4003L, 1001L, TipoMovimientoPago.PAGO_PROCESADO, BASE.plusHours(3), BASE.plusHours(3), "SYS")
                .formaPagoId(2001L).referenciaExterna("EM-2026-0001").build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4004L, 1001L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, BASE.plusHours(4), BASE.plusHours(4), "SYS")
                .formaPagoId(2001L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4005L, 1001L, TipoMovimientoPago.OBLIGACION_CANCELADA, BASE.plusHours(5), BASE.plusHours(5), "SYS")
                .formaPagoId(2001L).build());

        // Proyectar estado final
        oblig = obligacionRepo.findById(1001L).get();
        oblig.cancelar(BASE.plusHours(5));
        obligacionRepo.save(oblig);
    }

    /**
     * Caso 2: Acta 102 - Condena con plan de pago, actualmente en mora.
     */
    private void caso2_planDePagoEnMora() {
        FalActaObligacionPago oblig = new FalActaObligacionPago(
                1002L, 102L, 300L, TipoObligacionPago.CONDENA,
                new BigDecimal("8000.00"), BASE, "SYSTEM", BASE, "SYSTEM");
        obligacionRepo.save(oblig);

        FalActaFormaPago forma = new FalActaFormaPago(
                2002L, 1002L, (short) 1, TipoFormaPago.PLAN_PAGO,
                new BigDecimal("8000.00"), BASE, BASE, "SYSTEM");
        formaRepo.save(forma);

        FalActaPlanPagoRef plan = new FalActaPlanPagoRef(
                3001L, 2002L, 1002L, (short) 1, 80001L, (short) 12,
                new BigDecimal("8000.00"));
        plan.setImporteCuotaRegular(new BigDecimal("666.67"));
        planRepo.save(plan);

        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4010L, 1002L, TipoMovimientoPago.DEUDA_EMITIDA, BASE, BASE, "SYS")
                .formaPagoId(2002L).planPagoRefId(3001L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4011L, 1002L, TipoMovimientoPago.PLAN_GENERADO, BASE.plusDays(1), BASE.plusDays(1), "SYS")
                .formaPagoId(2002L).planPagoRefId(3001L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4012L, 1002L, TipoMovimientoPago.PLAN_EN_MORA, BASE.plusDays(35), BASE.plusDays(35), "SYS")
                .formaPagoId(2002L).planPagoRefId(3001L).build());

        FalActaObligacionPago oblig1002 = obligacionRepo.findById(1002L).get();
        oblig1002.setEstadoObligacion(EstadoObligacionPago.EN_PLAN);
        obligacionRepo.save(oblig1002);
    }

    /**
     * Caso 3: Acta 103 - Plan caido y refinanciado.
     */
    private void caso3_refinanciacionDePlan() {
        FalActaObligacionPago oblig = new FalActaObligacionPago(
                1003L, 103L, 400L, TipoObligacionPago.CONDENA,
                new BigDecimal("5000.00"), BASE, "SYSTEM", BASE, "SYSTEM");
        obligacionRepo.save(oblig);

        // Forma original (plan caido)
        FalActaFormaPago forma1 = new FalActaFormaPago(
                2003L, 1003L, (short) 1, TipoFormaPago.PLAN_PAGO,
                new BigDecimal("5000.00"), BASE, BASE, "SYSTEM");
        formaRepo.save(forma1);

        // Plan original (caido)
        FalActaPlanPagoRef planOriginal = new FalActaPlanPagoRef(
                3002L, 2003L, 1003L, (short) 1, 90001L, (short) 6,
                new BigDecimal("5000.00"));
        planOriginal.setImporteCuotaRegular(new BigDecimal("833.33"));
        planRepo.save(planOriginal);

        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4020L, 1003L, TipoMovimientoPago.PLAN_GENERADO, BASE, BASE, "SYS")
                .formaPagoId(2003L).planPagoRefId(3002L).build());
        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4021L, 1003L, TipoMovimientoPago.PLAN_CAIDO, BASE.plusDays(90), BASE.plusDays(90), "SYS")
                .formaPagoId(2003L).planPagoRefId(3002L).build());

        // Refinanciacion: nuevo plan
        FalActaFormaPago forma2 = new FalActaFormaPago(
                2004L, 1003L, (short) 2, TipoFormaPago.REFINANCIACION,
                new BigDecimal("5000.00"), BASE.plusDays(95), BASE.plusDays(95), "USR_INSPECTOR");
        // Guardamos la nueva forma sin vigente=true (no conflicto con forma1 ya no vigente)
        FalActaFormaPago forma1ActualNV = formaRepo.findById(2003L).get();
        forma1ActualNV.setSiVigente(false);
        formaRepo.save(forma1ActualNV);
        formaRepo.save(forma2);

        FalActaPlanPagoRef planNuevo = new FalActaPlanPagoRef(
                3003L, 2004L, 1003L, (short) 1, 90002L, (short) 10,
                new BigDecimal("5000.00"));
        planNuevo.setImporteCuotaRegular(new BigDecimal("500.00"));
        FalActaPlanPagoRef planAnteriorRec = planRepo.findById(3002L).get();
        planRepo.refinanciarAtomico(planNuevo, planAnteriorRec);

        movimientoRepo.append(new FalActaPagoMovimiento.Builder(
                4022L, 1003L, TipoMovimientoPago.PLAN_REFINANCIADO, BASE.plusDays(95), BASE.plusDays(95), "SYS")
                .formaPagoId(2004L).planPagoRefId(3003L).build());

        FalActaObligacionPago oblig1003 = obligacionRepo.findById(1003L).get();
        oblig1003.setEstadoObligacion(EstadoObligacionPago.REFINANCIADA);
        obligacionRepo.save(oblig1003);
    }
}