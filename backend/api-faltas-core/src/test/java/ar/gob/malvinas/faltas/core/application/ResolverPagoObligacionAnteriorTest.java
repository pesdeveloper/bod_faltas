package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.NotificarMovimientoPagoCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverPagoObligacionAnteriorCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.ObligacionPagoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PagoMovimientoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.ResolucionPagoAnteriorConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Cobertura funcional de ResolverPagoObligacionAnteriorCommand: resolucion
 * administrativa de un pago (PAGO_CONFIRMADO, clasificacion
 * OBLIGACION_ANTERIOR, evento PAGANT) aplicado contra la obligacion vigente
 * de un acta (evento PAGRES), bajo el modelo R1 de movimiento unico: sin
 * fal_acta_pago_resolucion ni obligacion por diferencia. Ver
 * backend/api-faltas-core/docs/spec-as-source/02-estados-bloques-eventos.md
 * y backend/api-faltas-core/docs/spec-as-source/03-comandos-precondiciones-efectos.md.
 */
@DisplayName("ResolverPagoObligacionAnterior - PAGANT/PAGRES (modelo R1, sin tabla de resolucion)")
class ResolverPagoObligacionAnteriorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 7, 1, 10, 0);
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Long ACTA_ID = 100L;
    private static final Long PERSONA_ID = 200L;

    private InMemoryObligacionPagoRepository obligacionRepo;
    private InMemoryPagoMovimientoRepository movimientoRepo;
    private InMemoryEconomiaProyeccionRepository proyeccionRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaRepository actaRepo;
    private PagoEconomicoService economicoService;
    private PagoIntegracionService integracionService;
    private EconomiaProyeccionRecalculador recalculador;
    private ResolverPagoObligacionAnteriorService resolverService;
    private FaltasClock clock;

    @BeforeEach
    void setUp() {
        obligacionRepo = new InMemoryObligacionPagoRepository();
        movimientoRepo = new InMemoryPagoMovimientoRepository();
        proyeccionRepo = new InMemoryEconomiaProyeccionRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        actaRepo = new InMemoryActaRepository();
        clock = new FaltasClock(Clock.fixed(Instant.parse("2026-07-01T13:00:00-03:00"), ZONA));
        montarServicios(clock);

        actaRepo.guardar(crearActa(ACTA_ID));
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    private void montarServicios(FaltasClock clockUsado) {
        var tesoreriaInput = new InMemoryTesoreriaConciliacionInput();
        recalculador = new EconomiaProyeccionRecalculador(obligacionRepo, new InMemoryFormaPagoRepository(),
                new InMemoryPlanPagoRefRepository(), movimientoRepo, proyeccionRepo, actaRepo, clockUsado, tesoreriaInput);
        PagoMovimientoService movimientoService = new PagoMovimientoService(movimientoRepo, obligacionRepo, clockUsado);
        PagoMovimientoReducer reducer = new PagoMovimientoReducer(recalculador);
        integracionService = new PagoIntegracionService(movimientoService, obligacionRepo,
                new InMemoryFormaPagoRepository(), new InMemoryPlanPagoRefRepository(), movimientoRepo, reducer, recalculador, clockUsado);
        economicoService = new PagoEconomicoService(integracionService, movimientoService, recalculador, obligacionRepo,
                movimientoRepo, proyeccionRepo, eventoRepo, clockUsado, tesoreriaInput);
        resolverService = new ResolverPagoObligacionAnteriorService(
                actaRepo, movimientoRepo, obligacionRepo, proyeccionRepo, integracionService, eventoRepo, clockUsado);
    }

    private FalActa crearActa(Long id) {
        return new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 6, 20), T0, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, PERSONA_ID, T0, "SYS");
    }

    /** Obligacion vigente actual (la "alcanzada" por la resolucion). */
    private FalActaObligacionPago crearVigente(Long id, BigDecimal monto) {
        FalActaObligacionPago o = new FalActaObligacionPago(
                id, ACTA_ID, PERSONA_ID, TipoObligacionPago.PAGO_VOLUNTARIO, monto, T0, "USR", T0, "USR");
        return obligacionRepo.save(o);
    }

    /** Obligacion anterior/reemplazada, no vigente: destino original del pago mal aplicado. */
    private FalActaObligacionPago crearAnterior(Long id, BigDecimal monto) {
        FalActaObligacionPago o = new FalActaObligacionPago(
                id, ACTA_ID, PERSONA_ID, TipoObligacionPago.PAGO_VOLUNTARIO, monto, T0, "USR", T0, "USR");
        o.setSiVigente(false);
        return obligacionRepo.save(o);
    }

    /** Contador deterministico para sintetizar cmtePG/prefPG/nroPG unico por llamada (R2-02: recibo obligatorio). */
    private int reciboSeq = 0;

    /**
     * Notifica un PAGO_CONFIRMADO contra una obligacion (potencialmente no
     * vigente). Sintetiza una terna de recibo cmtePG/prefPG/nroPG unica y
     * deterministica: desde R2-02, PagoMovimientoService.registrar exige
     * terna completa para todo PAGO_CONFIRMADO original.
     */
    private FalActaPagoMovimiento notificarPago(Long obligacionId, BigDecimal importe, String ref) {
        reciboSeq++;
        return notificarPago(obligacionId, importe, ref, "RS", (short) 1, reciboSeq);
    }

    private FalActaPagoMovimiento notificarPago(
            Long obligacionId, BigDecimal importe, String ref, String cmtePG, Short prefPG, Integer nroPG) {
        return economicoService.notificarMovimiento(new NotificarMovimientoPagoCommand(
                obligacionId, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, OrigenConfirmacion.INGRESOS, null, ClasificacionPago.NORMAL,
                null, importe, null, importe, null, null, null, cmtePG, prefPG, nroPG,
                null, null, null, null, ref, T0, "USR"));
    }

    // -----------------------------------------------------------------
    // Emision de PAGANT al notificar contra obligacion no vigente
    // -----------------------------------------------------------------

    @Test
    @DisplayName("01. PAGO_CONFIRMADO contra obligacion no vigente se clasifica OBLIGACION_ANTERIOR, emite PAGANT y no toca la proyeccion vigente")
    void notificarContraObligacionAnteriorEmitePagant() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));

        var m = notificarPago(9002L, new BigDecimal("300"), "PAGANT-1");

        assertThat(m.getClasificacionPago()).isEqualTo(ClasificacionPago.OBLIGACION_ANTERIOR);
        List<FalActaEvento> eventos = eventoRepo.buscarPorActa(ACTA_ID);
        assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGANT
                && "PAGANT-1".equals(e.correlacionId()));
        assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.PAGCNF);

        // No debe afectar la proyeccion de la obligacion vigente actual (sin cierre administrativo indebido).
        var p = recalculador.recalcular(ACTA_ID, OrigenUltimaActualizacion.TIEMPO_REAL, "USR");
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo("1000.00");
        assertThat(p.getEstadoObligacion()).isNotEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
    }

    @Test
    @DisplayName("02. importe cero o negativo en PAGANT es rechazado")
    void pagantImporteInvalidoRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        assertThatThrownBy(() -> notificarPago(9002L, BigDecimal.ZERO, "PAGANT-BAD"))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // -----------------------------------------------------------------
    // Resolucion: movimiento unico de aplicacion, saldo derivado
    // -----------------------------------------------------------------

    @Test
    @DisplayName("03. dos pagos parciales acumulativos contra la misma obligacion vigente reducen el saldo en cascada sin crear obligaciones nuevas")
    void dosPagosParcialesAcumulativosReducenSaldoSinNuevaObligacion() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        crearAnterior(9003L, new BigDecimal("200"));

        var pagant1 = notificarPago(9002L, new BigDecimal("300"), "CASC-1");
        var r1 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant1.getId(), null, "USR"));
        assertThat(r1.obligacionAplicadaId()).isEqualTo(9001L);
        assertThat(r1.saldoResultante()).isEqualByComparingTo("700.00");

        var pagant2 = notificarPago(9003L, new BigDecimal("250"), "CASC-2");
        var r2 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant2.getId(), null, "USR"));
        assertThat(r2.obligacionAplicadaId()).isEqualTo(9001L);
        assertThat(r2.saldoResultante()).isEqualByComparingTo("450.00");

        // Sigue existiendo una sola obligacion vigente: la misma de siempre.
        assertThat(obligacionRepo.findVigenteByActaId(ACTA_ID).orElseThrow().getId()).isEqualTo(9001L);
        assertThat(obligacionRepo.findByActaId(ACTA_ID)).hasSize(3);

        // Dos movimientos de aplicacion (uno por cada resolucion) contra la misma obligacion.
        List<FalActaPagoMovimiento> movsVigente = movimientoRepo.findByObligacionPagoId(9001L);
        assertThat(movsVigente).hasSize(2);
        assertThat(movsVigente).allMatch(m -> m.getClasificacionPago() == ClasificacionPago.NORMAL);

        long pagresCount = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES).count();
        assertThat(pagresCount).isEqualTo(2);
    }

    @Test
    @DisplayName("04. pago que completa exactamente el saldo cancela la obligacion vigente por pago")
    void pagoFinalSaldoCeroCancelaObligacion() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("1000"), "TOT-EXACTO");

        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "resolucion total exacta", "USR"));

        assertThat(resultado.importeAplicado()).isEqualByComparingTo("1000.00");
        assertThat(resultado.saldoResultante()).isEqualByComparingTo("0.00");
        assertThat(resultado.importeExcedente()).isEqualByComparingTo("0.00");

        var obl = obligacionRepo.findById(9001L).orElseThrow();
        assertThat(obl.getEstadoObligacion()).isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);

        var p = proyeccionRepo.findByActaId(ACTA_ID).orElseThrow();
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo("0.00");

        List<FalActaEvento> eventos = eventoRepo.buscarPorActa(ACTA_ID);
        assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.PAGRES);
        assertThat(eventos).noneMatch(e -> e.tipoEvt() == TipoEventoActa.OBLREP);
    }

    @Test
    @DisplayName("05. pago superior al saldo cancela la obligacion y deja un excedente informativo, sin obligacion ni devolucion nueva")
    void pagoSuperiorGeneraExcedenteInformativoSinObligacionNueva() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("1500"), "TOT-EXCED");

        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR"));

        assertThat(resultado.importeAplicado()).isEqualByComparingTo("1500.00");
        assertThat(resultado.saldoResultante()).isEqualByComparingTo("0.00");
        assertThat(resultado.importeExcedente()).isEqualByComparingTo("500.00");

        var obl = obligacionRepo.findById(9001L).orElseThrow();
        assertThat(obl.getEstadoObligacion()).isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
        var p = proyeccionRepo.findByActaId(ACTA_ID).orElseThrow();
        assertThat(p.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(p.getImporteExcedente()).isEqualByComparingTo("500.00");

        // No se crea ninguna obligacion adicional (ni por diferencia ni por excedente).
        assertThat(obligacionRepo.findByActaId(ACTA_ID)).hasSize(2);
    }

    @Test
    @DisplayName("06. un movimiento ya derivado (movimientoOrigenId != null) no puede resolverse directamente")
    void movimientoDerivadoNoResoluble() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "DERIV-1");
        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant.getId(), null, "USR"));

        Long movimientoAplicadoId = resultado.movimientoAplicado().getId();
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, movimientoAplicadoId, null, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // -----------------------------------------------------------------
    // Idempotencia y conflicto de reintento
    // -----------------------------------------------------------------

    @Test
    @DisplayName("07. reintento compatible (misma obligacion destino, mismo motivo) devuelve el mismo resultado sin duplicar efectos")
    void reintentoCompatibleDevuelveMismoResultado() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "IDEMP-1");

        var r1 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant.getId(), "motivo-x", "USR"));
        var r2 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant.getId(), "motivo-x", "USR"));

        assertThat(r2.movimientoAplicado().getId()).isEqualTo(r1.movimientoAplicado().getId());
        assertThat(r2.obligacionAplicadaId()).isEqualTo(r1.obligacionAplicadaId());
        assertThat(r2.importeAplicado()).isEqualByComparingTo(r1.importeAplicado());

        // Un solo movimiento de aplicacion, un solo evento PAGRES; el reintento no agrega nada.
        assertThat(movimientoRepo.findByMovimientoOrigenId(pagant.getId())).hasSize(1);
        long pagresCount = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES).count();
        assertThat(pagresCount).isEqualTo(1);
    }

    @Test
    @DisplayName("08. reintento contra una obligacion destino que ya no es la vigente es un conflicto (409)")
    void reintentoIncompatiblePorObligacionDestinoCambiada() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "CONFLICT-OBLIG");
        resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant.getId(), null, "USR"));

        // Cambia la obligacion vigente del acta (deja sin efecto la anterior vigente y crea otra).
        FalActaObligacionPago vigenteVieja = obligacionRepo.findById(9001L).orElseThrow();
        FalActaObligacionPago nuevaVigente = new FalActaObligacionPago(
                9010L, ACTA_ID, PERSONA_ID, TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("300"), T0, "USR", T0, "USR");
        obligacionRepo.crearVigenteAtomico(nuevaVigente, vigenteVieja);

        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR")))
                .isInstanceOf(ResolucionPagoAnteriorConflictoException.class);
    }

    @Test
    @DisplayName("09. reintento con un motivo distinto al original es un conflicto (409)")
    void reintentoIncompatiblePorMotivoDistinto() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "CONFLICT-MOTIVO");
        resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(ACTA_ID, pagant.getId(), "motivo original", "USR"));

        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "motivo distinto", "USR")))
                .isInstanceOf(ResolucionPagoAnteriorConflictoException.class);
    }

    @Test
    @DisplayName("10. concurrencia: multiples hilos resolviendo el mismo PAGANT producen exactamente un movimiento de aplicacion y un evento PAGRES")
    void concurrenciaMismoMovimientoUnaSolaAplicacion() throws InterruptedException {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "CONC-1");

        int hilos = 8;
        Thread[] threads = new Thread[hilos];
        Long[] idsResultado = new Long[hilos];
        for (int i = 0; i < hilos; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
                try {
                    var r = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                            ACTA_ID, pagant.getId(), null, "USR"));
                    idsResultado[idx] = r.movimientoAplicado().getId();
                } finally {
                    ActorContextHolder.clear();
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        Long primerId = idsResultado[0];
        assertThat(idsResultado).allSatisfy(id -> assertThat(id).isEqualTo(primerId));
        assertThat(movimientoRepo.findByMovimientoOrigenId(pagant.getId())).hasSize(1);
        long pagresCount = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES).count();
        assertThat(pagresCount).isEqualTo(1);
    }

    // -----------------------------------------------------------------
    // Clasificacion no falsificable
    // -----------------------------------------------------------------

    @Test
    @DisplayName("11. clasificacionPago=OBLIGACION_ANTERIOR declarada por el actor contra una obligacion vigente es rechazada")
    void clasificacionObligacionAnteriorFalsificadaContraVigenteRechazada() {
        crearVigente(9001L, new BigDecimal("1000"));

        NotificarMovimientoPagoCommand cmd = new NotificarMovimientoPagoCommand(
                9001L, null, null, TipoMovimientoPago.PAGO_CONFIRMADO,
                OrigenMovimiento.INGRESOS, OrigenConfirmacion.INGRESOS, null, ClasificacionPago.OBLIGACION_ANTERIOR,
                null, new BigDecimal("100"), null, new BigDecimal("100"), null, null, null, null, null, null,
                null, null, null, null, "FALSIFICADO-1", T0, "USR");

        assertThatThrownBy(() -> economicoService.notificarMovimiento(cmd))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // -----------------------------------------------------------------
    // Deduplicacion de recibo real (cmtePG/prefPG/nroPG)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("12. un mismo recibo (cmtePG/prefPG/nroPG) con distinta referenciaExterna es rechazado como duplicado")
    void duplicadoDeReciboMismoCmtePgPrefPgNroPgRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        notificarPago(9001L, new BigDecimal("100"), "REF-A", "RE", (short) 1, 555);

        assertThatThrownBy(() -> notificarPago(9001L, new BigDecimal("100"), "REF-B", "RE", (short) 1, 555))
                .isInstanceOf(MovimientoPagoDuplicadoException.class);
    }

    @Test
    @DisplayName("13. el mismo recibo con la misma referenciaExterna es idempotente, no duplicado")
    void mismoReciboMismaReferenciaEsIdempotente() {
        crearVigente(9001L, new BigDecimal("1000"));
        var m1 = notificarPago(9001L, new BigDecimal("100"), "REF-IDEMP", "RE", (short) 1, 777);
        var m2 = notificarPago(9001L, new BigDecimal("100"), "REF-IDEMP", "RE", (short) 1, 777);

        assertThat(m2.getId()).isEqualTo(m1.getId());
        assertThat(movimientoRepo.findByObligacionPagoId(9001L)).hasSize(1);
    }

    // -----------------------------------------------------------------
    // Instante unico de reloj (CMD-ORDER-002)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("14. resolver captura un unico instante de reloj (invocationCount==1): movimiento de aplicacion, obligacion cancelada, evento PAGRES, proyeccion y resultado comparten el mismo hecho")
    void resolverUsaUnicoInstanteParaMovimientoYEvento() {
        CountingClock counting = CountingClock.startingAt("2026-07-01T16:00:00Z");
        montarServicios(counting);
        actaRepo.reset();
        actaRepo.guardar(crearActa(ACTA_ID));

        crearVigente(9001L, new BigDecimal("400"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "RELOJ-1");

        counting.reset();
        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR"));

        assertThat(counting.invocationCount())
                .as("resolver() debe leer FaltasClock.now() exactamente una vez por ejecucion (CMD-ORDER-002)")
                .isEqualTo(1);

        LocalDateTime instanteResolucion = counting.nthInstant(0);
        assertThat(resultado.movimientoAplicado().getFhMovimiento())
                .as("fhMovimiento del movimiento de aplicacion debe usar el unico instante de la resolucion")
                .isEqualTo(instanteResolucion);
        assertThat(resultado.movimientoAplicado().getFhAlta())
                .as("fhAlta del movimiento de aplicacion debe ser el mismo instante")
                .isEqualTo(instanteResolucion);
        assertThat(resultado.fhResolucion())
                .as("fhResolucion del resultado debe ser el mismo instante que el movimiento de aplicacion")
                .isEqualTo(instanteResolucion);

        var oblAplicada = obligacionRepo.findById(9001L).orElseThrow();
        assertThat(oblAplicada.getEstadoObligacion()).isEqualTo(EstadoObligacionPago.CANCELADA_POR_PAGO);
        assertThat(oblAplicada.getFhCancelacion())
                .as("fhCancelacion de la obligacion cancelada debe ser el mismo instante")
                .isEqualTo(instanteResolucion);

        FalActaEvento pagres = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES)
                .findFirst().orElseThrow();
        assertThat(pagres.fhEvt())
                .as("fhEvt del evento PAGRES debe ser el mismo instante que el movimiento de aplicacion (mismo hecho)")
                .isEqualTo(instanteResolucion);

        var proyeccion = proyeccionRepo.findByActaId(ACTA_ID).orElseThrow();
        assertThat(proyeccion.getFhCorteEconomico())
                .as("fhCorteEconomico de la proyeccion debe ser el mismo instante")
                .isEqualTo(instanteResolucion);
        assertThat(proyeccion.getFhUltMod())
                .as("fhUltMod de la proyeccion debe ser el mismo instante")
                .isEqualTo(instanteResolucion);
    }

    // -----------------------------------------------------------------
    // Precondiciones y errores (404/422)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("15. movimiento no clasificado como OBLIGACION_ANTERIOR es rechazado (422)")
    void movimientoNoClasificadoRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        var normal = notificarPago(9001L, new BigDecimal("100"), "NORMAL-1");
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, normal.getId(), null, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("16. movimiento inexistente es rechazado (404)")
    void movimientoInexistenteRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, 9999L, null, "USR")))
                .isInstanceOf(PagoMovimientoNoEncontradoException.class);
    }

    @Test
    @DisplayName("17. acta inexistente es rechazada (404)")
    void actaInexistenteRechazada() {
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                9999L, 1L, null, "USR")))
                .isInstanceOf(ActaNoEncontradaException.class);
    }

    @Test
    @DisplayName("18. sin obligacion vigente en el acta es rechazado (404)")
    void sinObligacionVigenteRechazado() {
        crearAnterior(9001L, new BigDecimal("1000"));
        // Notificar directamente sobre la unica obligacion, que ya es no-vigente.
        var pagant = notificarPago(9001L, new BigDecimal("100"), "SINVIG-1");
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR")))
                .isInstanceOf(ObligacionPagoNoEncontradaException.class);
    }

    @Test
    @DisplayName("19. movimiento de otra acta es rechazado (422)")
    void movimientoDeOtraActaRechazado() {
        Long otraActaId = 101L;
        actaRepo.guardar(crearActa(otraActaId));
        crearVigente(9001L, new BigDecimal("1000"));

        FalActaObligacionPago anteriorOtraActa = new FalActaObligacionPago(
                9050L, otraActaId, PERSONA_ID, TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("300"), T0, "USR", T0, "USR");
        anteriorOtraActa.setSiVigente(false);
        obligacionRepo.save(anteriorOtraActa);
        FalActaObligacionPago vigenteOtraActa = new FalActaObligacionPago(
                9051L, otraActaId, PERSONA_ID, TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("300"), T0, "USR", T0, "USR");
        obligacionRepo.save(vigenteOtraActa);

        var pagant = notificarPago(9050L, new BigDecimal("100"), "OTRA-ACTA-1");

        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    // -----------------------------------------------------------------
    // R2-02: recibo obligatorio, movimiento de aplicacion no copia terna
    // -----------------------------------------------------------------

    @Test
    @DisplayName("20. el movimiento de aplicacion nunca copia cmtePG/prefPG/nroPG del original; el recibo se recupera navegando movimientoOrigenId")
    void movimientoAplicadoNoCopiaTernaDelOriginal() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "RECIBO-1", "RC", (short) 9, 12345);

        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "USR"));

        FalActaPagoMovimiento aplicado = resultado.movimientoAplicado();
        assertThat(aplicado.getCmtePG()).isNull();
        assertThat(aplicado.getPrefPG()).isNull();
        assertThat(aplicado.getNroPG()).isNull();

        FalActaPagoMovimiento original = movimientoRepo.findById(aplicado.getMovimientoOrigenId()).orElseThrow();
        assertThat(original.getCmtePG()).isEqualTo("RC");
        assertThat(original.getPrefPG()).isEqualTo((short) 9);
        assertThat(original.getNroPG()).isEqualTo(12345);
    }

    // -----------------------------------------------------------------
    // R2-03: deduplicacion atomica de recibo real
    // -----------------------------------------------------------------

    @Test
    @DisplayName("21. deduplicacion atomica de recibo: 10 hilos concurrentes con el mismo cmtePG/prefPG/nroPG y referenciaExterna distinta producen exactamente un movimiento, el resto es duplicado")
    void deduplicacionAtomicaDeReciboConcurrente() throws InterruptedException {
        crearVigente(9001L, new BigDecimal("1000"));

        int hilos = 10;
        Thread[] threads = new Thread[hilos];
        Exception[] errores = new Exception[hilos];
        for (int i = 0; i < hilos; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
                try {
                    notificarPago(9001L, new BigDecimal("100"), "CONC-REF-" + idx, "CC", (short) 3, 99001);
                } catch (Exception e) {
                    errores[idx] = e;
                } finally {
                    ActorContextHolder.clear();
                }
            });
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        long exitosos = 0;
        long duplicados = 0;
        for (Exception e : errores) {
            if (e == null) exitosos++;
            else if (e instanceof MovimientoPagoDuplicadoException) duplicados++;
        }
        assertThat(exitosos).isEqualTo(1);
        assertThat(duplicados).isEqualTo(hilos - 1);
        assertThat(movimientoRepo.findByReferenciaPG("CC", (short) 3, 99001)).hasSize(1);
    }

    // -----------------------------------------------------------------
    // R2-04: motivo estructurado, no derivado de FalActaEvento
    // -----------------------------------------------------------------

    @Test
    @DisplayName("22. el motivo de resolucion se persiste en el movimiento de aplicacion; la idempotencia de motivo no depende de PAGRES.descripcionLegible")
    void motivoPersistidoEnMovimientoNoEnEvento() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "MOTIVO-1");

        var resultado = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "motivo estructurado", "USR"));

        assertThat(resultado.movimientoAplicado().getMotivoAplicacionPagoAnterior()).isEqualTo("motivo estructurado");

        // Se elimina todo el historial de eventos (incluido el PAGRES ya emitido) para
        // demostrar que el service nunca consulta eventoRepo para decidir idempotencia
        // de motivo: solo lee el campo estructurado del movimiento de aplicacion.
        eventoRepo.reset();

        var reintento = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "motivo estructurado", "USR"));
        assertThat(reintento.movimientoAplicado().getId()).isEqualTo(resultado.movimientoAplicado().getId());
        assertThat(reintento.motivo()).isEqualTo("motivo estructurado");
        assertThat(movimientoRepo.findByMovimientoOrigenId(pagant.getId())).hasSize(1);
    }

    // -----------------------------------------------------------------
    // R2-05: orden canonico y validaciones directas
    // -----------------------------------------------------------------

    @Test
    @DisplayName("23. actor nulo (sin contexto JWT ni idUser en el comando) es rechazado sin consumir nextId() ni generar efectos")
    void actorNuloRechazadoSinEfectos() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "ACTOR-1");
        long idMaximoAntes = pagant.getId();

        ActorContextHolder.clear();
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, null)))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(movimientoRepo.findByMovimientoOrigenId(pagant.getId())).isEmpty();
        long pagresCount = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES).count();
        assertThat(pagresCount).isEqualTo(0);

        // El intento fallido no debe haber consumido ningun id de movimientoRepo.nextId():
        // el proximo movimiento creado debe continuar la secuencia sin huecos.
        ActorContextHolder.set(new ActorContext("usuario-demo-faltas"));
        var siguiente = notificarPago(9002L, new BigDecimal("50"), "ACTOR-2");
        assertThat(siguiente.getId()).isEqualTo(idMaximoAntes + 1);
    }

    @Test
    @DisplayName("24. actor en blanco (idUser vacio, sin contexto JWT) es rechazado (422)")
    void actorEnBlancoRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "ACTOR-BLANK-1");

        ActorContextHolder.clear();
        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), null, "   ")))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("25. obligacion origen fabricada como vigente (sin pasar por PagoEconomicoService) es rechazada (422)")
    void obligacionOrigenFabricadaComoVigenteRechazada() {
        crearVigente(9001L, new BigDecimal("1000"));
        // Fabrica un movimiento OBLIGACION_ANTERIOR directo contra 9001L, que es vigente,
        // sin pasar por PagoEconomicoService.notificarMovimiento (que ya rechaza esto en
        // origen). Verifica la defensa explicita y directa del resolver ante datos que
        // no respetaron esa invariante por otra via.
        Long movId = movimientoRepo.nextId();
        FalActaPagoMovimiento fabricado = new FalActaPagoMovimiento.Builder(
                movId, 9001L, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, T0, T0, "USR")
                .clasificacionPago(ClasificacionPago.OBLIGACION_ANTERIOR)
                .importes(null, null, new BigDecimal("100"))
                .referenciaExterna("FABRICADO-VIGENTE")
                .build();
        movimientoRepo.append(fabricado);

        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, movId, null, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class);
    }

    @Test
    @DisplayName("26. movimiento con importeTotal cero (fabricado directo) es rechazado (422) sin efectos")
    void importeOriginalInvalidoFabricadoRechazado() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        Long movId = movimientoRepo.nextId();
        FalActaPagoMovimiento fabricado = new FalActaPagoMovimiento.Builder(
                movId, 9002L, TipoMovimientoPago.PAGO_CONFIRMADO, OrigenMovimiento.INGRESOS, T0, T0, "USR")
                .clasificacionPago(ClasificacionPago.OBLIGACION_ANTERIOR)
                .importes(null, null, BigDecimal.ZERO)
                .referenciaExterna("FABRICADO-IMPORTE-CERO")
                .build();
        movimientoRepo.append(fabricado);

        assertThatThrownBy(() -> resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, movId, null, "USR")))
                .isInstanceOf(PrecondicionVioladaException.class);

        assertThat(movimientoRepo.findByMovimientoOrigenId(movId)).isEmpty();
    }

    // -----------------------------------------------------------------
    // R2-06: reintento coherente (datos historicos, no de la solicitud)
    // -----------------------------------------------------------------

    @Test
    @DisplayName("27. reintento compatible por un actor distinto reporta el actor y el motivo historicos de la primera resolucion, no los del reintento")
    void reintentoPorActorDistintoReportaDatosHistoricos() {
        crearVigente(9001L, new BigDecimal("1000"));
        crearAnterior(9002L, new BigDecimal("500"));
        var pagant = notificarPago(9002L, new BigDecimal("400"), "RETRY-ACTOR-1");

        ActorContextHolder.set(new ActorContext("actor-A"));
        var r1 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "motivo original", "USR"));

        ActorContextHolder.set(new ActorContext("actor-B"));
        var r2 = resolverService.resolver(new ResolverPagoObligacionAnteriorCommand(
                ACTA_ID, pagant.getId(), "motivo original", "USR"));

        assertThat(r2.actor())
                .as("el actor reportado debe ser el historico (actor-A), no el del reintento (actor-B)")
                .isEqualTo("actor-A")
                .isEqualTo(r1.actor());
        assertThat(r2.motivo()).isEqualTo(r1.motivo());
        assertThat(r2.fhResolucion()).isEqualTo(r1.fhResolucion());
        assertThat(r2.movimientoAplicado().getId()).isEqualTo(r1.movimientoAplicado().getId());

        assertThat(movimientoRepo.findByMovimientoOrigenId(pagant.getId())).hasSize(1);
        long pagresCount = eventoRepo.buscarPorActa(ACTA_ID).stream()
                .filter(e -> e.tipoEvt() == TipoEventoActa.PAGRES).count();
        assertThat(pagresCount).isEqualTo(1);
    }
}
