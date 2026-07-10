package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.ConciliacionIncompatibleException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EconomiaModeloB1R1 - correcciones R1")
class EconomiaModeloB1R1Test {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final BigDecimal MONTO = new BigDecimal("1000.00");

    private InMemoryObligacionPagoRepository obligacionRepo;
    private InMemoryPagoMovimientoRepository movimientoRepo;
    private InMemoryEconomiaProyeccionRepository proyeccionRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaRepository actaRepo;
    private PagoEconomicoService economicoService;
    private PagoMovimientoService movimientoService;
    private EconomiaProyeccionRecalculador recalculador;

    @BeforeEach
    void setUp() {
        obligacionRepo = new InMemoryObligacionPagoRepository();
        movimientoRepo = new InMemoryPagoMovimientoRepository();
        proyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        actaRepo = new InMemoryActaRepository();
        FaltasClock clock = new FaltasClock(Clock.fixed(Instant.parse("2026-07-01T13:00:00-03:00"), ZoneId.of("America/Argentina/Buenos_Aires")));
        var tesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        recalculador = new EconomiaProyeccionRecalculador(obligacionRepo, new InMemoryFormaPagoRepository(), new InMemoryPlanPagoRefRepository(), movimientoRepo, proyeccionRepo, actaRepo, clock, tesoreriaInput);
        movimientoService = new PagoMovimientoService(movimientoRepo, obligacionRepo, clock);
        PagoMovimientoReducer reducer = new PagoMovimientoReducer(recalculador);
        PagoIntegracionService integracion = new PagoIntegracionService(movimientoService, obligacionRepo, new InMemoryFormaPagoRepository(), new InMemoryPlanPagoRefRepository(), movimientoRepo, reducer, recalculador, clock);
        economicoService = new PagoEconomicoService(integracion, movimientoService, recalculador, obligacionRepo, movimientoRepo, proyeccionRepo, eventoRepo, clock, tesoreriaInput);
        obligacionRepo.save(new FalActaObligacionPago(1L, 100L, 200L, TipoObligacionPago.PAGO_VOLUNTARIO, MONTO, T0, "USR", T0, "USR"));
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    private NotificarMovimientoPagoCommand cmdConfirmado(BigDecimal importe, String ref, OrigenConfirmacion oc) {
        return new NotificarMovimientoPagoCommand(1L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, oc, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, null, null, null, null, null, null, null, ref, T0, "USR");
    }

    @Test
    void movimiento_inmutable_tras_conciliar() {
        var mov = economicoService.notificarMovimiento(cmdConfirmado(new BigDecimal("300"), "C-1", null));
        var antes = movimientoRepo.findById(mov.getId()).orElseThrow();
        int eventos = eventoRepo.buscarPorActa(100L).size();
        economicoService.conciliarMovimiento(mov.getId(), "CONC-1");
        var despues = movimientoRepo.findById(mov.getId()).orElseThrow();
        assertThat(despues.payloadEquivalenteA(antes)).isTrue();
        assertThat(eventoRepo.buscarPorActa(100L)).hasSize(eventos);
    }

    @Test
    void conciliacion_reclasifica_sin_cambiar_aplicado() {
        var mov = economicoService.notificarMovimiento(cmdConfirmado(new BigDecimal("300"), "C-2", null));
        var aplicadoAntes = proyeccionRepo.findByActaId(100L).orElseThrow().getImporteAplicadoTotal();
        economicoService.conciliarMovimiento(mov.getId(), "CONC-2");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(aplicadoAntes);
        assertThat(p.getImporteConfirmadoTesoreria()).isEqualByComparingTo("300.00");
        assertThat(p.getFhUltimaConciliacion()).isNotNull();
    }

    @Test
    void reverso_sin_doble_descuento() {
        economicoService.notificarMovimiento(cmdConfirmado(MONTO, "R-full", null));
        var m = movimientoRepo.findByOrigenAndReferenciaExterna(OrigenMovimiento.INGRESOS, "R-full").orElseThrow();
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV", OrigenMovimiento.TESORERIA, "USR");
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        var p = proyeccionRepo.findByActaId(100L).orElseThrow();
        assertThat(p.getImporteAplicadoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getImporteRevertido()).isEqualByComparingTo(MONTO);
    }

    @Test
    void idempotencia_un_evento() {
        var c = cmdConfirmado(new BigDecimal("100"), "IDEM-R1", null);
        economicoService.notificarMovimiento(c);
        economicoService.notificarMovimiento(c);
        assertThat(movimientoRepo.findByObligacionPagoId(1L)).hasSize(1);
        assertThat(eventoRepo.buscarPorActa(100L).stream().filter(e -> e.tipoEvt() == TipoEventoActa.PAGCNF)).hasSize(1);
    }

    @Test
    void conflicto_payload_distinto() {
        economicoService.notificarMovimiento(cmdConfirmado(new BigDecimal("100"), "X", null));
        assertThatThrownBy(() -> economicoService.notificarMovimiento(cmdConfirmado(new BigDecimal("200"), "X", null)))
                .isInstanceOf(MovimientoPagoDuplicadoException.class);
    }

    @Test
    void reapertura_solo_acta_cerrada() {
        economicoService.notificarMovimiento(cmdConfirmado(MONTO, "REAP", null));
        var m = movimientoRepo.findByOrigenAndReferenciaExterna(OrigenMovimiento.INGRESOS, "REAP").orElseThrow();
        movimientoService.revertir(m.getId(), MotivoAnulacionPago.CONTRACARGO, "REV", OrigenMovimiento.TESORERIA, "USR");
        recalculador.recalcular(100L, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(proyeccionRepo.findByActaId(100L).orElseThrow().isSiReaperturaRequerida()).isFalse();
    }

    @Test
    void conciliacion_idempotente_incompatible() {
        var mov = economicoService.notificarMovimiento(cmdConfirmado(new BigDecimal("50"), "CI", null));
        economicoService.conciliarMovimiento(mov.getId(), "REF-A");
        economicoService.conciliarMovimiento(mov.getId(), "REF-A");
        assertThatThrownBy(() -> economicoService.conciliarMovimiento(mov.getId(), "REF-B"))
                .isInstanceOf(ConciliacionIncompatibleException.class);
    }
}
