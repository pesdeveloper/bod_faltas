package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EconomiaModeloB1R2 - cierre economia InMemory")
class EconomiaModeloB1R2Test {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final BigDecimal MONTO = new BigDecimal("1000.00");
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private InMemoryObligacionPagoRepository obligacionRepo;
    private InMemoryPagoMovimientoRepository movimientoRepo;
    private InMemoryEconomiaProyeccionRepository proyeccionRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaRepository actaRepo;
    private PagoEconomicoService economicoService;
    private PagoMovimientoService movimientoService;
    private PagoIntegracionService integracionService;
    private EconomiaProyeccionRecalculador recalculador;

    @BeforeEach
    void setUp() {
        obligacionRepo = new InMemoryObligacionPagoRepository();
        movimientoRepo = new InMemoryPagoMovimientoRepository();
        proyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        actaRepo = new InMemoryActaRepository();
        FaltasClock clock = new FaltasClock(Clock.fixed(Instant.parse("2026-07-01T13:00:00-03:00"), ZONA));
        var tesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        recalculador = new EconomiaProyeccionRecalculador(obligacionRepo, new InMemoryFormaPagoRepository(),
                new InMemoryPlanPagoRefRepository(), movimientoRepo, proyeccionRepo, actaRepo, clock, tesoreriaInput);
        movimientoService = new PagoMovimientoService(movimientoRepo, obligacionRepo, clock);
        PagoMovimientoReducer reducer = new PagoMovimientoReducer(recalculador);
        integracionService = new PagoIntegracionService(movimientoService, obligacionRepo,
                new InMemoryFormaPagoRepository(), new InMemoryPlanPagoRefRepository(), movimientoRepo, reducer, recalculador, clock);
        economicoService = new PagoEconomicoService(integracionService, movimientoService, recalculador, obligacionRepo,
                movimientoRepo, proyeccionRepo, eventoRepo, clock, tesoreriaInput);
        obligacionRepo.save(new FalActaObligacionPago(1L, 100L, 200L, TipoObligacionPago.PAGO_VOLUNTARIO, MONTO, T0, "USR", T0, "USR"));
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    /** Contador deterministico para sintetizar cmtePG/prefPG/nroPG unico por llamada (R2-02: recibo obligatorio). */
    private int reciboSeq = 0;

    private NotificarMovimientoPagoCommand confirmado(BigDecimal importe, String ref) {
        reciboSeq++;
        return new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, "R2", (short) 1, reciboSeq, null, null, null, null, ref, T0, "USR");
    }

    @Test
    @DisplayName("reverso total: 1000 confirmado + reverso 1000 = aplicado 0")
    void reversoTotalAplicadoCero() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "RT-1"));
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-RT", OrigenMovimiento.TESORERIA, "USR");
        var p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getImporteRevertido()).isEqualByComparingTo(MONTO);
    }

    @Test
    @DisplayName("sin doble descuento: 600 + 600 + reverso del primero = aplicado 600")
    void seiscientosMasSeiscientosReversoPrimero() {
        var m1 = economicoService.notificarMovimiento(confirmado(new BigDecimal("600"), "S-1"));
        economicoService.notificarMovimiento(confirmado(new BigDecimal("600"), "S-2"));
        movimientoService.revertir(m1.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-S1", OrigenMovimiento.TESORERIA, "USR");
        var p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("reverso tecnico repetido con misma referencia es idempotente")
    void reversoRepetidoIdempotente() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "RR-1"));
        var o1 = movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-RR", OrigenMovimiento.TESORERIA, "USR");
        var o2 = movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-RR", OrigenMovimiento.TESORERIA, "USR");
        assertThat(o1.movimiento().getId()).isEqualTo(o2.movimiento().getId());
        long reversos = movimientoRepo.findByObligacionPagoId(1L).stream()
                .filter(x -> x.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO).count();
        assertThat(reversos).isEqualTo(1);
    }

    @Test
    @DisplayName("segundo reverso distinto sobre original ya revertido es rechazado")
    void segundoReversoDistintoRechazado() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "RD-1"));
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-A", OrigenMovimiento.TESORERIA, "USR");
        assertThatThrownBy(() -> movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-B", OrigenMovimiento.TESORERIA, "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("tipo no reversible: DEUDA_EMITIDA no puede revertirse")
    void tipoNoReversible() {
        var m = economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, null, null,
                TipoMovimientoPago.DEUDA_EMITIDA, OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL,
                null, MONTO, null, MONTO, null, null, null, null, null, null, null, null, null, null, "DE-1", T0, "USR"));
        assertThatThrownBy(() -> movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-DE", OrigenMovimiento.TESORERIA, "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("original intacto tras reverso")
    void originalIntactoTrasReverso() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "OI-1"));
        var antes = movimientoRepo.findById(m.getId()).orElseThrow();
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-OI", OrigenMovimiento.TESORERIA, "USR");
        var despues = movimientoRepo.findById(m.getId()).orElseThrow();
        assertThat(despues.payloadEquivalenteA(antes)).isTrue();
    }

    @Test
    @DisplayName("idempotencia con Clock avanzado y sin fhMovimiento informada")
    void idempotenciaClockAvanzado() {
        MutableClock reloj = new MutableClock(Instant.parse("2026-07-01T13:00:00-03:00"));
        FaltasClock clock = new FaltasClock(reloj);
        PagoMovimientoService svc = new PagoMovimientoService(movimientoRepo, obligacionRepo, clock);
        RegistroMovimientoOutcome o1 = svc.registrar(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL, null,
                new BigDecimal("100"), null, new BigDecimal("100"),
                null, null, null, "AD", (short) 1, 1, null, null, null, null, "ADV-1", null, "USR");
        reloj.avanzarHoras(5);
        RegistroMovimientoOutcome o2 = svc.registrar(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL, null,
                new BigDecimal("100"), null, new BigDecimal("100"),
                null, null, null, "AD", (short) 1, 1, null, null, null, null, "ADV-1", null, "USR");
        assertThat(o1.resultado()).isEqualTo(MovimientoRegistroResult.CREATED);
        assertThat(o2.resultado()).isEqualTo(MovimientoRegistroResult.ALREADY_EXISTS);
        long total = movimientoRepo.findByObligacionPagoId(1L).stream()
                .filter(x -> "ADV-1".equals(x.getReferenciaExterna())).count();
        assertThat(total).isEqualTo(1);
    }

    @Test
    @DisplayName("obligacion revive: no queda CANCELADA_POR_PAGO tras reverso total")
    void obligacionReviveTrasReverso() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "OB-1"));
        assertThat(obligacionRepo.findById(1L).orElseThrow().getEstadoObligacion())
                .isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-OB", OrigenMovimiento.TESORERIA, "USR");
        integracionService.recalcularEstados(1L, null, null);
        var obl = obligacionRepo.findById(1L).orElseThrow();
        assertThat(obl.getEstadoObligacion()).isNotEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
        assertThat(obl.getFhCancelacion()).isNull();
    }

    @Test
    @DisplayName("siPagoConfirmado sigue true con dos pagos y reverso de uno")
    void siPagoConfirmadoDosPagosReversoUno() {
        var m1 = economicoService.notificarMovimiento(confirmado(new BigDecimal("400"), "DP-1"));
        economicoService.notificarMovimiento(confirmado(new BigDecimal("400"), "DP-2"));
        movimientoService.revertir(m1.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-DP1", OrigenMovimiento.TESORERIA, "USR");
        var p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.isSiPagoConfirmado()).isTrue();
    }

    @Test
    @DisplayName("conciliacion reclasifica a Tesoreria via input absoluto")
    void conciliacionReclasificaTesoreria() {
        var m = economicoService.notificarMovimiento(confirmado(new BigDecimal("300"), "CT-1"));
        var antes = proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal();
        var p = economicoService.conciliarMovimiento(m.getId(), "CONC-CT");
        assertThat(p.getImporteConfirmadoTesoreria()).isEqualByComparingTo("300.00");
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(antes);
        assertThat(p.getReferenciaUltimaConciliacion()).isEqualTo("CONC-CT");
    }

    static final class MutableClock extends Clock {
        private Instant instante;
        MutableClock(Instant inicial) { this.instante = inicial; }
        void avanzarHoras(int h) { this.instante = this.instante.plusSeconds(h * 3600L); }
        @Override public ZoneId getZone() { return ZONA; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instante; }
    }
}
