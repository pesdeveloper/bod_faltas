package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.EconomiaProyeccionRecalculador;
import ar.gob.malvinas.faltas.core.application.service.PagoMovimientoReducer;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagoMovimientoReducer - reduccion de estados B1")
class PagoMovimientoReducerTest {

    private PagoMovimientoReducer reducer;
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 7, 1, 11, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 7, 1, 12, 0);

    @BeforeEach
    void setUp() {
        var recalculador = new EconomiaProyeccionRecalculador(
                new InMemoryObligacionPagoRepository(),
                new InMemoryFormaPagoRepository(),
                new InMemoryPlanPagoRefRepository(),
                new InMemoryPagoMovimientoRepository(),
                new InMemoryEconomiaProyeccionRepository(),
                new InMemoryActaRepository(),
                new FaltasClock(),
                new InMemoryTesoreriaConciliacionInput());
        reducer = new PagoMovimientoReducer(recalculador);
    }

    private FalActaObligacionPago obligacion(Long id) {
        return new FalActaObligacionPago(id, 1L, 100L,
                TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("1000.00"), T0, "SYS", T0, "SYS");
    }

    private FalActaFormaPago forma(Long id) {
        return new FalActaFormaPago(id, 1L, (short) 1, TipoFormaPago.RECIBO_AL_COBRO, new BigDecimal("1000.00"), T0, T0, "SYS");
    }

    private FalActaPlanPagoRef plan(Long id) {
        return new FalActaPlanPagoRef(id, 1L, 1L, (short) 1, id * 100, (short) 6, new BigDecimal("1000.00"));
    }

    private FalActaPagoMovimiento mov(Long id, Long obligId, TipoMovimientoPago tipo, LocalDateTime fh) {
        return new FalActaPagoMovimiento.Builder(id, obligId, tipo, OrigenMovimiento.INGRESOS, fh, fh, "SYS").build();
    }

    @Test
    void sinMovimientos_proyectaDeterminada() {
        assertThat(reducer.proyectarEstadoObligacion(obligacion(1L), List.of()))
                .isEqualTo(EstadoObligacionPago.DETERMINADA);
    }

    @Test
    void pagoConfirmado_formaPagada() {
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1),
                mov(2L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO, T2));
        assertThat(reducer.proyectarEstadoForma(forma(1L), movs)).isEqualTo(EstadoFormaPago.PAGADA);
    }

    @Test
    void pagoProcesado_formaVigente() {
        List<FalActaPagoMovimiento> movs = List.of(mov(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, T1));
        assertThat(reducer.proyectarEstadoForma(forma(1L), movs)).isEqualTo(EstadoFormaPago.VIGENTE);
    }

    @Test
    void hayPagoConfirmadoActivo_true() {
        List<FalActaPagoMovimiento> movs = List.of(
                mov(1L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO, T1));
        assertThat(reducer.hayPagoConfirmadoActivo(movs)).isTrue();
    }

    @Test
    void hayPagoConfirmadoActivo_falseTrasReverso() {
        FalActaPagoMovimiento conf = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, T1, T1, "SYS").importes(new BigDecimal("100"), null, new BigDecimal("100")).build();
        FalActaPagoMovimiento rev = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_REVERTIDO,
                OrigenMovimiento.TESORERIA, T2, T2, "SYS").movimientoOrigenId(1L)
                .importes(new BigDecimal("100"), null, new BigDecimal("100")).build();
        assertThat(reducer.hayPagoConfirmadoActivo(List.of(conf, rev))).isFalse();
    }
}
