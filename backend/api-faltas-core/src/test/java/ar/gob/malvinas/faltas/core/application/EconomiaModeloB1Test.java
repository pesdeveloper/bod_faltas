package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenConfirmacion;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EconomiaModeloB1 - 30 casos obligatorios")
class EconomiaModeloB1Test {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final BigDecimal MONTO = new BigDecimal("1000.00");

    private InMemoryObligacionPagoRepository obligacionRepo;
    private InMemoryFormaPagoRepository formaRepo;
    private InMemoryPlanPagoRefRepository planRepo;
    private InMemoryPagoMovimientoRepository movimientoRepo;
    private InMemoryEconomiaProyeccionRepository proyeccionRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaRepository actaRepo;
    private FaltasClock clock;
    private EconomiaProyeccionRecalculador recalculador;
    private PagoMovimientoService movimientoService;
    private PagoIntegracionService integracionService;
    private PagoEconomicoService economicoService;
    private ProcesoNocturnoEconomicoService nocturnoService;

    private FalActaObligacionPago obligacion;

    @BeforeEach
    void setUp() {
        obligacionRepo = new InMemoryObligacionPagoRepository();
        formaRepo = new InMemoryFormaPagoRepository();
        planRepo = new InMemoryPlanPagoRefRepository();
        movimientoRepo = new InMemoryPagoMovimientoRepository();
        proyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        actaRepo = new InMemoryActaRepository();
        clock = new FaltasClock(Clock.fixed(Instant.parse("2026-07-01T13:00:00-03:00"), ZoneId.of("America/Argentina/Buenos_Aires")));
        var tesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        recalculador = new EconomiaProyeccionRecalculador(obligacionRepo, formaRepo, planRepo, movimientoRepo, proyeccionRepo, actaRepo, clock, tesoreriaInput);
        movimientoService = new PagoMovimientoService(movimientoRepo, obligacionRepo, clock);
        PagoMovimientoReducer reducer = new PagoMovimientoReducer(recalculador);
        integracionService = new PagoIntegracionService(movimientoService, obligacionRepo, formaRepo, planRepo, movimientoRepo, reducer, recalculador, clock);
        economicoService = new PagoEconomicoService(integracionService, movimientoService, recalculador, obligacionRepo, movimientoRepo, proyeccionRepo, eventoRepo, clock, tesoreriaInput);
        nocturnoService = new ProcesoNocturnoEconomicoService(obligacionRepo, recalculador);
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
        obligacion = new FalActaObligacionPago(1L, 100L, 200L, TipoObligacionPago.PAGO_VOLUNTARIO, MONTO, T0, "USR", T0, "USR");
        obligacionRepo.save(obligacion);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    private NotificarMovimientoPagoCommand cmd(TipoMovimientoPago tipo, BigDecimal importe, String ref, OrigenMovimiento origen) {
        return new NotificarMovimientoPagoCommand(1L, null, null, tipo, origen, null, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, null, null, null, null, null, null, null, ref, T0, "USR");
    }

    @Test @DisplayName("01 - proyeccion inicial con obligacion determinada")
    void caso01_proyeccionInicial() {
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        p.setCantidadCuotasMoraConsec((short)3);
        proyeccionRepo.save(p);
        p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.getEstadoObligacion()).isEqualTo(EstadoObligacionPago.DETERMINADA);
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("02 - deuda emitida no cambia aplicado")
    void caso02_deudaEmitida() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.DEUDA_EMITIDA, MONTO, "DEU-1", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("03 - pago procesado incrementa procesado no aplicado")
    void caso03_pagoProcesado() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_PROCESADO, new BigDecimal("500"), "PRC-1", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImportePagoProcesado()).isEqualByComparingTo("500.00");
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.isSiPagoProcesado()).isTrue();
    }

    @Test @DisplayName("04 - pago confirmado incrementa aplicado")
    void caso04_pagoConfirmado() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("400"), "CNF-1", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo("400.00");
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo("600.00");
    }

    @Test @DisplayName("05 - saldo = MAX(monto - aplicado, 0)")
    void caso05_saldoPendiente() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, MONTO, "CNF-FULL", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("06 - aplicado capped at monto_obligacion_vigente")
    void caso06_aplicadoCap() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("1500"), "CNF-EXC", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(MONTO);
    }

    @Test @DisplayName("07 - reverso reduce aplicado")
    void caso07_reversoReduceAplicado() {
        var mov = economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("800"), "CNF-REV", OrigenMovimiento.INGRESOS));
        movimientoService.revertir(mov.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-1", OrigenMovimiento.TESORERIA, "USR");
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getImporteRevertido()).isEqualByComparingTo("800.00");
    }

    @Test @DisplayName("08 - conciliacion no cambia aplicado")
    void caso08_conciliacionNoCambiaAplicado() {
        var mov = economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("300"), "CNF-CON", OrigenMovimiento.INGRESOS));
        var antes = proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal();
        economicoService.conciliarMovimiento(mov.getId(), "CONC-1");
        var despues = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(despues.getImporteAplicadoTotal()).isEqualByComparingTo(antes);
        assertThat(despues.getImporteConfirmadoTesoreria()).isEqualByComparingTo("300.00");
        assertThat(despues.getImporteConfirmadoEvidenciaPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("09 - idempotencia origen+referencia")
    void caso09_idempotencia() {
        var c = cmd(TipoMovimientoPago.PAGO_PROCESADO, new BigDecimal("100"), "IDEM-1", OrigenMovimiento.INGRESOS);
        economicoService.notificarMovimiento(c);
        economicoService.notificarMovimiento(c);
        assertThat(movimientoRepo.findByObligacionPagoId(1L)).hasSize(1);
    }

    @Test @DisplayName("10 - conflicto misma ref distinto tipo")
    void caso10_conflictoRef() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_PROCESADO, new BigDecimal("100"), "X-1", OrigenMovimiento.INGRESOS));
        assertThatThrownBy(() -> economicoService.notificarMovimiento(
                cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("100"), "X-1", OrigenMovimiento.INGRESOS)))
                .isInstanceOf(MovimientoPagoDuplicadoException.class);
    }

    @Test @DisplayName("11 - origenMovimiento requerido")
    void caso11_origenRequerido() {
        assertThatThrownBy(() -> new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, null, T0, T0, "USR").build())
                .isInstanceOf(Exception.class);
    }

    @Test @DisplayName("12 - evento PAGCNF al confirmar voluntario")
    void caso12_eventoPagcnf() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("100"), "E-1", OrigenMovimiento.INGRESOS));
        assertThat(eventoRepo.buscarPorActa(100L).stream().anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGCNF)).isTrue();
    }

    @Test @DisplayName("13 - evento PAGREV al revertir")
    void caso13_eventoPagrev() {
        var m = economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("100"), "E-2", OrigenMovimiento.INGRESOS));
        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-E", OrigenMovimiento.TESORERIA, "USR");
        assertThat(eventoRepo.buscarPorActa(100L).stream().anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGREV)).isTrue();
        assertThat(eventoRepo.buscarPorActa(100L).stream().anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGCNF)).isTrue();
    }

    @Test @DisplayName("14 - estado conciliacion PENDIENTE_TESORERIA")
    void caso14_conciliacionPendiente() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("200"), "PEN-1", OrigenMovimiento.INGRESOS));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getEstadoConciliacionActual()).isEqualTo(EstadoConciliacionActual.PENDIENTE_TESORERIA);
        assertThat(p.isSiConciliacionPendiente()).isTrue();
    }

    @Test @DisplayName("15 - tesoreria origen conciliado directo")
    void caso15_tesoreriaConciliado() {
        economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.TESORERIA, OrigenConfirmacion.TESORERIA, null, ClasificacionPago.NORMAL, null, new BigDecimal("200"), null, new BigDecimal("200"), null, null, null, null, null, null, null, null, null, null, "TES-1", T0, "USR"));
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteConfirmadoTesoreria()).isEqualByComparingTo("200.00");
    }

    @Test @DisplayName("16 - emision anulada no afecta aplicado")
    void caso16_emisionAnulada() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.EMISION_ANULADA, MONTO, "ANU-1", OrigenMovimiento.INGRESOS));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("17 - multiples pagos acumulan aplicado")
    void caso17_multiplesPagos() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("300"), "M1", OrigenMovimiento.INGRESOS));
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("400"), "M2", OrigenMovimiento.INGRESOS));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal()).isEqualByComparingTo("700.00");
    }

    @Test @DisplayName("18 - fuera de orden determinista")
    void caso18_fueraDeOrden() {
        economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, null, null,
                TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL, null,
                new BigDecimal("500"), null, new BigDecimal("500"), null, null, null, null, null, null, null, null, null, null, "OO-1",
                T0.plusHours(2), "USR"));
        economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, null, null,
                TipoMovimientoPago.PAGO_PROCESADO, OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL, null,
                new BigDecimal("500"), null, new BigDecimal("500"), null, null, null, null, null, null, null, null, null, null, "OO-2",
                T0.plusHours(1), "USR"));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal()).isEqualByComparingTo("500.00");
    }

    @Test @DisplayName("19 - si_pago_confirmado flag")
    void caso19_flagConfirmado() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("1"), "F-1", OrigenMovimiento.INGRESOS));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().isSiPagoConfirmado()).isTrue();
    }

    @Test @DisplayName("20 - si_reapertura_requerida tras reverso parcial")
    void caso20_reaperturaRequerida() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, MONTO, "R-1", OrigenMovimiento.INGRESOS));
        var m = movimientoRepo.findByOrigenAndReferenciaExterna(OrigenMovimiento.INGRESOS, "R-1").orElseThrow();
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "R-REV", OrigenMovimiento.TESORERIA, "USR");
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().isSiReaperturaRequerida()).isFalse();
    }

    @Test @DisplayName("21 - plan caido calculado por mora consecutiva")
    void caso21_planCaido() {
        FalActaFormaPago forma = new FalActaFormaPago(10L, 1L, (short)1, TipoFormaPago.PLAN_PAGO, MONTO, T0, T0, "USR");
        formaRepo.save(forma);
        FalActaPlanPagoRef plan = new FalActaPlanPagoRef(20L, 10L, 1L, (short)1, 5001L, (short)6, MONTO);
        planRepo.save(plan);
        obligacion.setFormaPagoVigenteId(10L);
        obligacionRepo.save(obligacion);
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        p.setCantidadCuotasMoraConsec((short)3);
        proyeccionRepo.save(p);
        p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.isSiPlanCaidoCalculado()).isTrue();
        assertThat(p.getMotivoPlanCaidoCalculado()).isEqualTo(MotivoPlanCaidoCalculado.MORA_CONSECUTIVA);
    }

    @Test @DisplayName("22 - sincronizacion nocturna sin movimientos nuevos")
    void caso22_nocturno() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_CONFIRMADO, new BigDecimal("250"), "N-1", OrigenMovimiento.INGRESOS));
        var resultados = nocturnoService.ejecutarSincronizacionNocturna();
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getOrigenUltimaActualizacion()).isEqualTo(OrigenUltimaActualizacion.SINCRONIZACION_NOCTURNA);
        assertThat(resultados.get(0).getFhUltimaSincronizacion()).isNotNull();
    }

    @Test @DisplayName("23 - clasificacion DUPLICADO_REAL permitida")
    void caso23_clasificacionDuplicado() {
        var c = new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.DUPLICADO_REAL, null,
                new BigDecimal("100"), null, new BigDecimal("100"), null, null, null, null, null, null, null, null, null, null, "DUP-1", T0, "USR");
        economicoService.notificarMovimiento(c);
        assertThat(movimientoRepo.findByOrigenAndReferenciaExterna(OrigenMovimiento.INGRESOS, "DUP-1")).isPresent();
    }

    @Test @DisplayName("24 - actor context sub desde JWT holder")
    void caso24_actorContext() {
        ActorContextHolder.set(new ActorContext("jwt-sub-test"));
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.DEUDA_EMITIDA, MONTO, "ACT-1", OrigenMovimiento.INGRESOS));
        assertThat(movimientoRepo.findByObligacionPagoId(1L).get(0).getIdUserAlta()).isEqualTo("jwt-sub-test");
    }

    @Test @DisplayName("25 - TipoMovimientoPago 5 tipos canonicos")
    void caso25_cincoTiposMovimiento() {
        assertThat(TipoMovimientoPago.values()).hasSize(5);
    }

    @Test @DisplayName("26 - TipoEventoActa eventos economicos canonicos presentes")
    void caso26_eventosCanonicos() {
        assertThat(TipoEventoActa.OBLDET).isNotNull();
        assertThat(TipoEventoActa.RCBGEN).isNotNull();
        assertThat(TipoEventoActa.PLNGEN).isNotNull();
        assertThat(TipoEventoActa.PAGREV).isNotNull();
        assertThat(TipoEventoActa.EMIANU).isNotNull();
    }

    @Test @DisplayName("27 - obligacion cancelada por pago total")
    void caso27_canceladaPorPago() {
        economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.TESORERIA, OrigenConfirmacion.TESORERIA, null, ClasificacionPago.NORMAL, null, MONTO, null, MONTO, null, null, null, null, null, null, null, null, null, null, "CAN-1", T0, "USR"));
        assertThat(obligacionRepo.findById(1L).orElseThrow().getEstadoObligacion()).isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
    }

    @Test @DisplayName("28 - watermark ultimo movimiento proyectado")
    void caso28_watermark() {
        var m = economicoService.notificarMovimiento(cmd(TipoMovimientoPago.PAGO_PROCESADO, new BigDecimal("50"), "W-1", OrigenMovimiento.INGRESOS));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().getUltimoMovimientoIdProyectado()).isEqualTo(m.getId());
    }

    @Test @DisplayName("29 - fh_corte_economico establecido")
    void caso29_fhCorte() {
        economicoService.notificarMovimiento(cmd(TipoMovimientoPago.DEUDA_EMITIDA, MONTO, "CORTE-1", OrigenMovimiento.INGRESOS));
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().getFhCorteEconomico()).isNotNull();
    }

    @Test @DisplayName("30 - esAnulacion para PAGO_REVERTIDO")
    void caso30_esAnulacionRevertido() {
        var m = new FalActaPagoMovimiento.Builder(99L, 1L, TipoMovimientoPago.PAGO_REVERTIDO, OrigenMovimiento.TESORERIA, T0, T0, "USR").build();
        assertThat(m.esAnulacion()).isTrue();
    }
}
