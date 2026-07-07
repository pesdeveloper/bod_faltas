package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.application.service.PagoMovimientoReducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagoMovimientoReducer - reduccion de estados")
class PagoMovimientoReducerTest {

    private PagoMovimientoReducer reducer;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 7, 1, 11, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 7, 1, 12, 0);
    private static final LocalDateTime T4 = LocalDateTime.of(2026, 7, 1, 13, 0);

    @BeforeEach
    void setUp() { reducer = new PagoMovimientoReducer(); }

    private FalActaObligacionPago obligacion(Long id) {
        return new FalActaObligacionPago(id, 1L, 100L,
                TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("1000.00"), T0, "SYS", T0, "SYS");
    }

    private FalActaFormaPago forma(Long id, short nro, TipoFormaPago tipo) {
        return new FalActaFormaPago(id, 1L, nro, tipo, new BigDecimal("1000.00"), T0, T0, "SYS");
    }

    private FalActaPlanPagoRef plan(Long id, Long formaId, Long oblId) {
        return new FalActaPlanPagoRef(id, formaId, oblId, (short) 1, (long) id * 100, (short) 6,
                new BigDecimal("1000.00"));
    }

    private FalActaPagoMovimiento mov(Long id, Long obligId, TipoMovimientoPago tipo, LocalDateTime fh) {
        return new FalActaPagoMovimiento.Builder(id, obligId, tipo, fh, fh, "SYS").build();
    }

    @Test
    @DisplayName("Sin movimientos: proyecta DETERMINADA")
    void sinMovimientos_proyectaDeterminada() {
        FalActaObligacionPago obl = obligacion(1L);
        EstadoObligacionPago estado = reducer.proyectarEstadoObligacion(obl, List.of());
        assertThat(estado).isEqualTo(EstadoObligacionPago.DETERMINADA);
    }

    @Test
    @DisplayName("DEUDA_EMITIDA -> proyecta DEUDA_EMITIDA en obligacion")
    void deudaEmitida_proyectaDeudaEmitida() {
        FalActaObligacionPago obl = obligacion(1L);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T1));
        assertThat(reducer.proyectarEstadoObligacion(obl, movs))
                .isEqualTo(EstadoObligacionPago.DEUDA_EMITIDA);
    }

    @Test
    @DisplayName("PAGO_CONTADO_GENERADO -> CON_FORMA_PAGO en obligacion")
    void pagoContadoGenerado_proyectaConFormaPago() {
        FalActaObligacionPago obl = obligacion(1L);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONTADO_GENERADO, T1));
        assertThat(reducer.proyectarEstadoObligacion(obl, movs))
                .isEqualTo(EstadoObligacionPago.CON_FORMA_PAGO);
    }

    @Test
    @DisplayName("PAGO_PROCESADO -> forma PROCESADA; obligacion sin cambio adicional")
    void pagoProcesado_formaProcessada() {
        FalActaFormaPago forma = forma(1L, (short) 1, TipoFormaPago.CONTADO);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1));
        assertThat(reducer.proyectarEstadoForma(forma, movs))
                .isEqualTo(EstadoFormaPago.PROCESADA);
    }

    @Test
    @DisplayName("PAGO_CONFIRMADO_TESORERIA -> forma CONFIRMADA")
    void pagoConfirmado_formaConfirmada() {
        FalActaFormaPago forma = forma(1L, (short) 1, TipoFormaPago.CONTADO);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(3L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, T2));
        assertThat(reducer.proyectarEstadoForma(forma, movs))
                .isEqualTo(EstadoFormaPago.CONFIRMADA);
    }

    @Test
    @DisplayName("OBLIGACION_CANCELADA -> obligacion CANCELADA")
    void obligacionCancelada_estadoCancelada() {
        FalActaObligacionPago obl = obligacion(1L);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONTADO_GENERADO, T1),
                mov(3L, 1L, TipoMovimientoPago.OBLIGACION_CANCELADA, T2));
        assertThat(reducer.proyectarEstadoObligacion(obl, movs))
                .isEqualTo(EstadoObligacionPago.CANCELADA);
    }

    @Test
    @DisplayName("PLAN_GENERADO -> obligacion EN_PLAN; PLAN_EN_MORA -> plan EN_MORA")
    void plan_estadosBasicos() {
        FalActaObligacionPago obl = obligacion(1L);
        FalActaPlanPagoRef planRef = plan(1L, 1L, 1L);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PLAN_GENERADO, T1),
                mov(3L, 1L, TipoMovimientoPago.PLAN_EN_MORA, T2));
        assertThat(reducer.proyectarEstadoObligacion(obl, movs))
                .isEqualTo(EstadoObligacionPago.EN_PLAN);
        assertThat(reducer.proyectarEstadoPlan(planRef, movs))
                .isEqualTo(EstadoPlanPago.EN_MORA);
    }

    @Test
    @DisplayName("Movimientos fuera de orden: resultado determinista por fhMovimiento")
    void fueraDeOrden_determinista() {
        FalActaObligacionPago obl = obligacion(1L);
        FalActaFormaPago forma = forma(1L, (short) 1, TipoFormaPago.CONTADO);
        // Insertados fuera de orden: T2, T0, T1
        List<FalActaPagoMovimiento> movs = List.of(
                mov(3L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, T2),
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1));
        // Resultado es el mismo que si estuvieran ordenados
        assertThat(reducer.proyectarEstadoForma(forma, movs))
                .isEqualTo(EstadoFormaPago.CONFIRMADA);
    }

    @Test
    @DisplayName("PAGO_PROCESADO_ANULADO despues de PAGO_PROCESADO: forma vuelve a GENERADA")
    void pagoAnulado_formaVuelveAGenerada() {
        FalActaFormaPago forma = forma(1L, (short) 1, TipoFormaPago.CONTADO);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(3L, 1L, TipoMovimientoPago.PAGO_PROCESADO_ANULADO, T2));
        assertThat(reducer.proyectarEstadoForma(forma, movs))
                .isEqualTo(EstadoFormaPago.GENERADA);
    }

    @Test
    @DisplayName("CONTRACARGO_REGISTRADO despues de OBLIGACION_CANCELADA: regresa a CON_FORMA_PAGO")
    void contracargo_despuesDeCancelada_regresaAConFormaPago() {
        FalActaObligacionPago obl = obligacion(1L);
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, T0),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONTADO_GENERADO, T1),
                mov(3L, 1L, TipoMovimientoPago.OBLIGACION_CANCELADA, T2),
                mov(4L, 1L, TipoMovimientoPago.CONTRACARGO_REGISTRADO, T3));
        assertThat(reducer.proyectarEstadoObligacion(obl, movs))
                .isEqualTo(EstadoObligacionPago.CON_FORMA_PAGO);
    }

    @Test
    @DisplayName("hayPagoProcesadoActivo: true/false segun movimientos")
    void hayPagoProcesadoActivo_logica() {
        List<FalActaPagoMovimiento> sin = List.of();
        assertThat(reducer.hayPagoProcesadoActivo(sin)).isFalse();

        List<FalActaPagoMovimiento> conPago = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1));
        assertThat(reducer.hayPagoProcesadoActivo(conPago)).isTrue();

        List<FalActaPagoMovimiento> pagadoYAnulado = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO_ANULADO, T2));
        assertThat(reducer.hayPagoProcesadoActivo(pagadoYAnulado)).isFalse();
    }

    @Test
    @DisplayName("hayPagoConfirmadoActivo: true/false segun movimientos")
    void hayPagoConfirmadoActivo_logica() {
        List<FalActaPagoMovimiento> confirmado = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, T2));
        assertThat(reducer.hayPagoConfirmadoActivo(confirmado)).isTrue();

        List<FalActaPagoMovimiento> conContracargo = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, T2),
                mov(3L, 1L, TipoMovimientoPago.CONTRACARGO_REGISTRADO, T3));
        assertThat(reducer.hayPagoConfirmadoActivo(conContracargo)).isFalse();
    }
}