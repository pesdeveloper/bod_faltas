package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PlanPagoNoEncontradoException;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EconomiaModeloB1R2R1 - ajuste final minimo")
class EconomiaModeloB1R2R1Test {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final BigDecimal MONTO = new BigDecimal("1000.00");
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private InMemoryObligacionPagoRepository obligacionRepo;
    private InMemoryFormaPagoRepository formaRepo;
    private InMemoryPlanPagoRefRepository planRepo;
    private InMemoryPagoMovimientoRepository movimientoRepo;
    private InMemoryEconomiaProyeccionRepository proyeccionRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaRepository actaRepo;
    private InMemoryTesoreriaConciliacionInput tesoreriaInput;
    private EconomiaProyeccionRecalculador recalculador;
    private PagoMovimientoService movimientoService;
    private PagoIntegracionService integracionService;
    private PagoEconomicoService economicoService;
    private PlanPagoService planPagoService;

    @BeforeEach
    void setUp() {
        obligacionRepo = new InMemoryObligacionPagoRepository();
        formaRepo = new InMemoryFormaPagoRepository();
        planRepo = new InMemoryPlanPagoRefRepository();
        movimientoRepo = new InMemoryPagoMovimientoRepository();
        proyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        actaRepo = new InMemoryActaRepository();
        tesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        FaltasClock clock = new FaltasClock(Clock.fixed(Instant.parse("2026-07-01T13:00:00-03:00"), ZONA));
        recalculador = new EconomiaProyeccionRecalculador(obligacionRepo, formaRepo, planRepo, movimientoRepo, proyeccionRepo, actaRepo, clock, tesoreriaInput);
        movimientoService = new PagoMovimientoService(movimientoRepo, obligacionRepo, clock);
        PagoMovimientoReducer reducer = new PagoMovimientoReducer(recalculador);
        integracionService = new PagoIntegracionService(movimientoService, obligacionRepo, formaRepo, planRepo, movimientoRepo, reducer, recalculador, clock);
        economicoService = new PagoEconomicoService(integracionService, movimientoService, recalculador, obligacionRepo, movimientoRepo, proyeccionRepo, eventoRepo, clock, tesoreriaInput);
        planPagoService = new PlanPagoService(planRepo, formaRepo, obligacionRepo, eventoRepo, recalculador, clock);
        obligacionRepo.save(new FalActaObligacionPago(1L, 100L, 200L, TipoObligacionPago.PAGO_VOLUNTARIO, MONTO, T0, "USR", T0, "USR"));
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    private NotificarMovimientoPagoCommand confirmado(BigDecimal importe, String ref) {
        return new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, null, null, null, null, null, null, null, ref, T0, "USR");
    }

    // ---- Punto 1: Reverso atomico ----

    @Test
    @DisplayName("reversoAtomico_genera1Movimiento1Evento")
    void reversoAtomico_genera1Movimiento1Evento() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "CNF-AT"));
        int eventoAntes = eventoRepo.buscarPorActa(100L).size();

        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-AT", OrigenMovimiento.TESORERIA, "USR");

        long reversos = movimientoRepo.findByObligacionPagoId(1L).stream()
                .filter(x -> x.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO).count();
        assertThat(reversos).isEqualTo(1);
        long eventosPagrev = eventoRepo.buscarPorActa(100L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGREV).count();
        assertThat(eventosPagrev).isEqualTo(1);
        assertThat(eventoRepo.buscarPorActa(100L).size()).isEqualTo(eventoAntes + 1);
    }

    @Test
    @DisplayName("reversoAtomico_reintentoIdempotente")
    void reversoAtomico_reintentoIdempotente() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "CNF-IDEM"));
        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-IDEM", OrigenMovimiento.TESORERIA, "USR");
        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-IDEM", OrigenMovimiento.TESORERIA, "USR");

        long reversos = movimientoRepo.findByObligacionPagoId(1L).stream()
                .filter(x -> x.getTipoMovimiento() == TipoMovimientoPago.PAGO_REVERTIDO).count();
        assertThat(reversos).isEqualTo(1);
        long pagrevEvents = eventoRepo.buscarPorActa(100L).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGREV).count();
        assertThat(pagrevEvents).isEqualTo(1);
    }

    @Test
    @DisplayName("obligacionReviveAutomaticamente_sinLlamadaManual")
    void obligacionReviveAutomaticamente_sinLlamadaManual() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "CNF-OBL"));
        assertThat(obligacionRepo.findById(1L).orElseThrow().getEstadoObligacion())
                .isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);

        // revertirMovimiento recalcula automaticamente sin llamada manual
        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-OBL", OrigenMovimiento.TESORERIA, "USR");

        var obl = obligacionRepo.findById(1L).orElseThrow();
        assertThat(obl.getEstadoObligacion()).isNotEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
        assertThat(obl.getFhCancelacion()).isNull();
    }

    @Test
    @DisplayName("formaReviveAutomaticamente_sinLlamadaManual")
    void formaReviveAutomaticamente_sinLlamadaManual() {
        FalActaFormaPago forma = new FalActaFormaPago(10L, 1L, (short)1, TipoFormaPago.RECIBO_AL_COBRO, MONTO, T0, T0, "USR");
        formaRepo.save(forma);
        obligacionRepo.findById(1L).ifPresent(o -> { o.setFormaPagoVigenteId(10L); obligacionRepo.save(o); });

        var m = economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, 10L, null,
                TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL,
                null, MONTO, null, MONTO, null, null, null, null, null, null, null, null, null, null, "CNF-FRM", T0, "USR"));
        assertThat(formaRepo.findById(10L).orElseThrow().getEstadoFormaPago()).isEqualTo(EstadoFormaPago.PAGADA);

        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-FRM", OrigenMovimiento.TESORERIA, "USR");

        assertThat(formaRepo.findById(10L).orElseThrow().getEstadoFormaPago()).isNotEqualTo(EstadoFormaPago.PAGADA);
    }

    @Test
    @DisplayName("notificarMovimientoPagoRevertidoSinVinculo_rechazado")
    void notificarMovimientoPagoRevertidoSinVinculo_rechazado() {
        assertThatThrownBy(() -> economicoService.notificarMovimiento(
                new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_REVERTIDO,
                        OrigenMovimiento.TESORERIA, null, null, ClasificacionPago.NORMAL,
                        null, MONTO, null, MONTO, null, null, null, null, null, null, null, null, null, null, "REV-BYPASS", T0, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("PAGO_REVERTIDO");
    }

    // ---- Punto 2: siPagoConfirmado vigente ----

    @Test
    @DisplayName("siPagoConfirmado_false_tras_reverso_total")
    void siPagoConfirmado_false_tras_reverso_total() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "SC-1"));
        economicoService.revertirMovimiento(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-SC", OrigenMovimiento.TESORERIA, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo(MONTO);
        assertThat(p.isSiPagoConfirmado()).isFalse();
        assertThat(p.getEstadoConciliacionActual()).isEqualTo(EstadoConciliacionActual.NO_APLICA);
    }

    @Test
    @DisplayName("siPagoConfirmado_true_con_dos_pagos_reverso_uno")
    void siPagoConfirmado_true_con_dos_pagos_reverso_uno() {
        var m1 = economicoService.notificarMovimiento(confirmado(new BigDecimal("400"), "SC2-1"));
        economicoService.notificarMovimiento(confirmado(new BigDecimal("400"), "SC2-2"));
        economicoService.revertirMovimiento(m1.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-SC2", OrigenMovimiento.TESORERIA, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo("400.00");
        assertThat(p.isSiPagoConfirmado()).isTrue();
    }

    // ---- Punto 3: Idempotencia y secuencia de IDs ----

    @Test
    @DisplayName("reverso_mismaRef_mismoMotivo_ALREADY_EXISTS")
    void reverso_mismaRef_mismoMotivo_ALREADY_EXISTS() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "ID-1"));
        var o1 = movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-ID", OrigenMovimiento.TESORERIA, "USR");
        var o2 = movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-ID", OrigenMovimiento.TESORERIA, "USR");
        assertThat(o1.resultado()).isEqualTo(MovimientoRegistroResult.CREATED);
        assertThat(o2.resultado()).isEqualTo(MovimientoRegistroResult.ALREADY_EXISTS);
        assertThat(o1.movimiento().getId()).isEqualTo(o2.movimiento().getId());
    }

    @Test
    @DisplayName("reverso_mismaRef_motivoDiferente_CONFLICT")
    void reverso_mismaRef_motivoDiferente_CONFLICT() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "CF-1"));
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV-CF", OrigenMovimiento.TESORERIA, "USR");
        assertThatThrownBy(() -> movimientoService.revertir(m.getId(), MotivoAnulacionPago.ERROR_OPERATIVO, "REV-CF", OrigenMovimiento.TESORERIA, "USR"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("reintento_normal_noConsumeNuevoID")
    void reintento_normal_noConsumeNuevoID() {
        var c = confirmado(new BigDecimal("100"), "NID-1");
        var o1 = integracionService.notificarMovimiento(c);
        long idAntes = o1.movimiento().getId();
        var o2 = integracionService.notificarMovimiento(c);
        assertThat(o2.resultado()).isEqualTo(MovimientoRegistroResult.ALREADY_EXISTS);
        assertThat(o2.movimiento().getId()).isEqualTo(idAntes);
        // El siguiente nuevo movimiento debe tener ID = idAntes + 1 (no +2)
        var o3 = integracionService.notificarMovimiento(confirmado(new BigDecimal("200"), "NID-2"));
        assertThat(o3.movimiento().getId()).isEqualTo(idAntes + 1);
    }

    @Test
    @DisplayName("reverso_sinReferenciaExterna_rechazado")
    void reverso_sinReferenciaExterna_rechazado() {
        var m = economicoService.notificarMovimiento(confirmado(MONTO, "NR-1"));
        assertThatThrownBy(() -> movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, null, OrigenMovimiento.TESORERIA, "USR"))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("referenciaExterna");
    }

    // ---- Punto 4: Plan bypass ----

    @Test
    @DisplayName("actualizarEstadoPlan_FINALIZADO_POR_PAGO_rechazado")
    void actualizarEstadoPlan_FINALIZADO_POR_PAGO_rechazado() {
        FalActaFormaPago forma = new FalActaFormaPago(20L, 1L, (short)1, TipoFormaPago.PLAN_PAGO, MONTO, T0, T0, "USR");
        formaRepo.save(forma);
        obligacionRepo.findById(1L).ifPresent(o -> { o.setFormaPagoVigenteId(20L); obligacionRepo.save(o); });
        FalActaPlanPagoRef plan = planPagoService.generarPlan(20L, 1L, (short)1, 9999L, (short)6, MONTO, null);
        assertThatThrownBy(() -> planPagoService.actualizarEstado(plan.getId(), EstadoPlanPago.FINALIZADO_POR_PAGO))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("FINALIZADO_POR_PAGO");
    }

    @Test
    @DisplayName("planFinalizadoPorPago_transitAtomicaGarantiza3efectos")
    void planFinalizadoPorPago_transitAtomicaGarantiza3efectos() {
        FalActaFormaPago forma = new FalActaFormaPago(21L, 1L, (short)1, TipoFormaPago.PLAN_PAGO, MONTO, T0, T0, "USR");
        formaRepo.save(forma);
        obligacionRepo.findById(1L).ifPresent(o -> { o.setFormaPagoVigenteId(21L); obligacionRepo.save(o); });
        planPagoService.generarPlan(21L, 1L, (short)1, 8888L, (short)3, MONTO, null);

        // Pago que cubre el total del plan
        economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(1L, 21L, planRepo.findVigenteByObligacionPagoId(1L).orElseThrow().getId(),
                TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, null, null, ClasificacionPago.NORMAL,
                null, MONTO, null, MONTO, null, null, null, null, null, null, null, null, null, null, "PL-CNF", T0, "USR"));

        var planFinalizado = planRepo.findVigenteByObligacionPagoId(1L);
        assertThat(planFinalizado).isEmpty();
        var planId = planRepo.findByObligacionPagoId(1L).get(0).getId();
        var planGuardado = planRepo.findById(planId).orElseThrow();
        assertThat(planGuardado.getEstadoPlan()).isEqualTo(EstadoPlanPago.FINALIZADO_POR_PAGO);
        assertThat(planGuardado.isSiVigente()).isFalse();
        assertThat(planGuardado.getFhFinalizacionPago()).isNotNull();
    }

    // ---- Punto 5: Conciliacion input absoluto ----

    @Test
    @DisplayName("conciliacion_rebuildDesdeSnapshotAbsoluto")
    void conciliacion_rebuildDesdeSnapshotAbsoluto() {
        var m = economicoService.notificarMovimiento(confirmado(new BigDecimal("300"), "RBS-1"));

        // Reemplazar el snapshot absoluto directamente
        Map<Long, String> snapshotExterno = new HashMap<>();
        snapshotExterno.put(m.getId(), "CONC-EXT-1");
        tesoreriaInput.reemplazarEstadoAbsoluto(snapshotExterno);

        var p = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.getImporteConfirmadoTesoreria()).isEqualByComparingTo("300.00");
        assertThat(p.getImporteConfirmadoEvidenciaPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("conciliacion_reemplazoSnapshotEliminaAnterior")
    void conciliacion_reemplazoSnapshotEliminaAnterior() {
        var m1 = economicoService.notificarMovimiento(confirmado(new BigDecimal("200"), "REM-1"));
        var m2 = economicoService.notificarMovimiento(confirmado(new BigDecimal("100"), "REM-2"));

        // Conciliar ambos
        economicoService.conciliarMovimiento(m1.getId(), "CONC-M1");
        economicoService.conciliarMovimiento(m2.getId(), "CONC-M2");

        var p1 = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p1.getImporteConfirmadoTesoreria()).isEqualByComparingTo("300.00");

        // Reemplazar snapshot absoluto quitando m1 (solo m2 queda conciliado)
        Map<Long, String> snapshotReducido = new HashMap<>();
        snapshotReducido.put(m2.getId(), "CONC-M2");
        tesoreriaInput.reemplazarEstadoAbsoluto(snapshotReducido);

        var p2 = recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        // m1 ya no esta conciliado -> va a evidenciaPendiente
        assertThat(p2.getImporteConfirmadoTesoreria()).isEqualByComparingTo("100.00");
        assertThat(p2.getImporteConfirmadoEvidenciaPendiente()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("conciliacion_reintentoIdenticoNoMutaFechaEstado")
    void conciliacion_reintentoIdenticoNoMutaFechaEstado() {
        java.time.Instant[] now = { java.time.Instant.parse("2026-07-01T13:00:00Z") };
        java.time.Clock advanceable = new java.time.Clock() {
            public java.time.ZoneId getZone() { return ZONA; }
            public java.time.Clock withZone(java.time.ZoneId z) { return this; }
            public java.time.Instant instant() { return now[0]; }
        };
        FaltasClock advClock = new FaltasClock(advanceable);
        var localObligacionRepo = new InMemoryObligacionPagoRepository();
        localObligacionRepo.save(new FalActaObligacionPago(1L, 100L, 200L,
                TipoObligacionPago.PAGO_VOLUNTARIO, MONTO, T0, "USR", T0, "USR"));
        var localFormaRepo = new InMemoryFormaPagoRepository();
        var localPlanRepo = new InMemoryPlanPagoRefRepository();
        var localMovimientoRepo = new InMemoryPagoMovimientoRepository();
        var localProyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        var localEventoRepo = new InMemoryActaEventoRepository();
        var localActaRepo = new InMemoryActaRepository();
        var localTesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        var localRecalculador = new EconomiaProyeccionRecalculador(localObligacionRepo, localFormaRepo,
                localPlanRepo, localMovimientoRepo, localProyeccionRepo, localActaRepo, advClock, localTesoreriaInput);
        var localMovimientoService = new PagoMovimientoService(localMovimientoRepo, localObligacionRepo, advClock);
        var localReducer = new PagoMovimientoReducer(localRecalculador);
        var localIntegracionService = new PagoIntegracionService(localMovimientoService, localObligacionRepo,
                localFormaRepo, localPlanRepo, localMovimientoRepo, localReducer, localRecalculador, advClock);
        var localEconomicoService = new PagoEconomicoService(localIntegracionService, localMovimientoService,
                localRecalculador, localObligacionRepo, localMovimientoRepo, localProyeccionRepo,
                localEventoRepo, advClock, localTesoreriaInput);
        var cmd = new ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand(
                1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS,
                null, null, ClasificacionPago.NORMAL, null, new BigDecimal("150"),
                null, new BigDecimal("150"), null, null, null, null, null, null, null, null, null, null, "RI-1", T0, "USR");
        var m = localEconomicoService.notificarMovimiento(cmd);
        var p1 = localEconomicoService.conciliarMovimiento(m.getId(), "CONC-RI");
        LocalDateTime fh1 = p1.getFhUltimaConciliacion();
        int eventosT1 = localEventoRepo.buscarPorActa(100L).size();
        now[0] = now[0].plusSeconds(3600);
        var p2 = localEconomicoService.conciliarMovimiento(m.getId(), "CONC-RI");
        LocalDateTime fh2 = p2.getFhUltimaConciliacion();
        assertThat(fh2).isEqualTo(fh1);
        assertThat(p2.getImporteConfirmadoTesoreria()).isEqualByComparingTo("150.00");
        assertThat(localTesoreriaInput.referenciaDe(m.getId())).isEqualTo("CONC-RI");
        assertThat(localEventoRepo.buscarPorActa(100L)).hasSize(eventosT1);
        assertThat(localMovimientoRepo.findById(m.getId()).orElseThrow().payloadEquivalenteA(m)).isTrue();
    }

    @Test
    @DisplayName("conciliacion_referenciaIncompatible_CONFLICT")
    void conciliacion_referenciaIncompatible_CONFLICT() {
        var m = economicoService.notificarMovimiento(confirmado(new BigDecimal("100"), "IC-1"));
        economicoService.conciliarMovimiento(m.getId(), "CONC-A");
        assertThatThrownBy(() -> economicoService.conciliarMovimiento(m.getId(), "CONC-B"))
                .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException.class);
    }

    @Test
    @DisplayName("conciliacion_movimientoYEventosIntactos")
    void conciliacion_movimientoYEventosIntactos() {
        var m = economicoService.notificarMovimiento(confirmado(new BigDecimal("200"), "MI-1"));
        var mAntes = movimientoRepo.findById(m.getId()).orElseThrow();
        int eventosAntes = eventoRepo.buscarPorActa(100L).size();
        economicoService.conciliarMovimiento(m.getId(), "CONC-MI");
        var mDespues = movimientoRepo.findById(m.getId()).orElseThrow();
        assertThat(mDespues.payloadEquivalenteA(mAntes)).isTrue();
        assertThat(eventoRepo.buscarPorActa(100L)).hasSize(eventosAntes);
    }
}