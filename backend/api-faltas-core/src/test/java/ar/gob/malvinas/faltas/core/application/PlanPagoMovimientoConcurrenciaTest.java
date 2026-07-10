package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoPlanPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPlanPagoRef;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObligacionPagoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoMovimientoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPlanPagoRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlanPagoRef e invariantes de concurrencia")
class PlanPagoMovimientoConcurrenciaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    /**
     * Constructor de FalActaPlanPagoRef:
     * (Long id, Long formaPagoId, Long obligacionPagoId,
     *  short idTdocPlan, long idDocPlan, short cantidadCuotas, BigDecimal importeTotalPlan)
     */
    private FalActaPlanPagoRef nuevoPlan(Long id, Long formaPagoId, Long oblId, short idTdoc, long idDoc) {
        return new FalActaPlanPagoRef(id, formaPagoId, oblId, idTdoc, idDoc, (short) 12, new BigDecimal("1000.00"));
    }

    @Nested
    @DisplayName("FalActaPlanPagoRef - validaciones")
    class PlanPagoRefTest {
        private InMemoryPlanPagoRefRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryPlanPagoRefRepository(); }

        @Test
        void estadoInicialActivo() {
            FalActaPlanPagoRef p = nuevoPlan(1L, 1L, 1L, (short) 1, 1001L);
            assertThat(p.getEstadoPlan()).isEqualTo(EstadoPlanPago.ACTIVO);
            assertThat(p.isSiVigente()).isTrue();
        }

        @Test
        void cantCuotasMinimo1() {
            assertThatThrownBy(() ->
                    new FalActaPlanPagoRef(1L, 1L, 1L, (short) 1, 1001L, (short) 0, new BigDecimal("1000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void importeNegativo_lanzaExcepcion() {
            assertThatThrownBy(() ->
                    new FalActaPlanPagoRef(1L, 1L, 1L, (short) 1, 1001L, (short) 6, new BigDecimal("-0.01")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void estadoPlan_todosLosCodigos() {
            assertThat(EstadoPlanPago.ACTIVO.codigo()).isEqualTo((short) 1);
            assertThat(EstadoPlanPago.FINALIZADO_POR_PAGO.codigo()).isEqualTo((short) 2);
            assertThat(EstadoPlanPago.ANULADO.codigo()).isEqualTo((short) 3);
            assertThat(EstadoPlanPago.REFINANCIADO.codigo()).isEqualTo((short) 4);
        }

        @Test
        void repo_parDuplicado_lanzaExcepcion() {
            // El par (idTdocPlan, idDocPlan) es la clave unica
            FalActaPlanPagoRef p1 = nuevoPlan(1L, 1L, 1L, (short) 1, 9001L);
            repo.save(p1);
            // Mismo par (idTdoc=1, idDoc=9001) -> excepcion
            FalActaPlanPagoRef p2 = nuevoPlan(2L, 2L, 2L, (short) 1, 9001L);
            assertThatThrownBy(() -> repo.save(p2))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        void repo_dosViigentesParaMismaObligacion_lanzaExcepcion() {
            FalActaPlanPagoRef p1 = nuevoPlan(1L, 1L, 1L, (short) 1, 1001L);
            repo.save(p1);
            // Mismo oblId, diferente par, pero intenta ser vigente
            FalActaPlanPagoRef p2 = nuevoPlan(2L, 2L, 1L, (short) 1, 1002L);
            assertThatThrownBy(() -> repo.save(p2))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("vigente");
        }

        @Test
        void refinanciarAtomico_marcaAnteriorRefinanciado_nuevoVigente() {
            FalActaPlanPagoRef planAnterior = nuevoPlan(1L, 1L, 1L, (short) 1, 1001L);
            repo.save(planAnterior);

            FalActaPlanPagoRef planNuevo = nuevoPlan(2L, 2L, 1L, (short) 1, 2001L);
            FalActaPlanPagoRef anteriorRecuperado = repo.findById(1L).get();
            repo.refinanciarAtomico(planNuevo, anteriorRecuperado);

            // El nuevo plan es el vigente
            assertThat(repo.findVigenteByObligacionPagoId(1L)).isPresent()
                    .hasValueSatisfying(v -> assertThat(v.getIdDocPlan()).isEqualTo(2001L));
            // El anterior ya no es vigente y tiene estado REFINANCIADO
            assertThat(repo.findById(1L)).isPresent()
                    .hasValueSatisfying(v -> {
                        assertThat(v.isSiVigente()).isFalse();
                        assertThat(v.getEstadoPlan()).isEqualTo(EstadoPlanPago.REFINANCIADO);
                    });
        }
    }

    @Nested
    @DisplayName("OCC concurrente - ObligacionPago")
    class OccConcurrente {
        @Test
        @DisplayName("10 hilos intentan actualizar la misma entidad; exactamente 1 gana")
        void occConcurrente_soloUnGana() throws InterruptedException {
            InMemoryObligacionPagoRepository repo = new InMemoryObligacionPagoRepository();
            FalActaObligacionPago base = new FalActaObligacionPago(1L, 1L, 100L,
                    TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("500.00"), AHORA, "USR1", AHORA, "USR1");
            repo.save(base);

            int threads = 10;
            ExecutorService ex = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger conflictos = new AtomicInteger(0);

            for (int i = 0; i < threads; i++) {
                ex.submit(() -> {
                    try {
                        start.await();
                        FalActaObligacionPago snapshot = repo.findById(1L).get();
                        snapshot.cancelar(FaltasClockTestSupport.FIXED.now());
                        repo.save(snapshot);
                        exitos.incrementAndGet();
                    } catch (ConcurrenciaConflictoException e) {
                        conflictos.incrementAndGet();
                    } catch (IllegalStateException e) {
                        conflictos.incrementAndGet();
                    } catch (Exception e) {
                        conflictos.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
            ex.shutdown();

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(conflictos.get()).isEqualTo(threads - 1);
        }
    }

    @Nested
    @DisplayName("Idempotencia PagoMovimiento - fuera de orden")
    class IdempotenciaFueraDeOrden {
        private InMemoryPagoMovimientoRepository repoMov;

        @BeforeEach
        void setUp() { repoMov = new InMemoryPagoMovimientoRepository(); }

        @Test
        @DisplayName("misma referenciaExterna y tipo: devuelve el original sin excepcion")
        void idempotencia_mismaTipoRef_devuelveOriginal() {
            FalActaPagoMovimiento m1 = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1")
                    .referenciaExterna("EXT-42").build();
            repoMov.append(m1);
            FalActaPagoMovimiento m1dup = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1")
                    .referenciaExterna("EXT-42").build();
            FalActaPagoMovimiento result = repoMov.append(m1dup).movimiento();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(repoMov.findByObligacionPagoId(1L)).hasSize(1);
        }

        @Test
        @DisplayName("referenciaExterna con tipo diferente: lanza conflicto")
        void idempotencia_diferenteTipo_lanzaConflicto() {
            FalActaPagoMovimiento m1 = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1")
                    .referenciaExterna("EXT-42").build();
            repoMov.append(m1);
            FalActaPagoMovimiento mConflict = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1")
                    .referenciaExterna("EXT-42").build();
            var outcome = repoMov.append(mConflict);
            assertThat(outcome.resultado().name()).isEqualTo("CONFLICT");
        }

        @Test
        @DisplayName("multiples movimientos distintos para misma obligacion")
        void multiplesTipos_mismaObligacion() {
            FalActaPagoMovimiento m1 = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            FalActaPagoMovimiento m2 = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            FalActaPagoMovimiento m3 = new FalActaPagoMovimiento.Builder(3L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            repoMov.append(m1);
            repoMov.append(m2);
            repoMov.append(m3);
            assertThat(repoMov.findByObligacionPagoId(1L)).hasSize(3);
        }

        @Test
        @DisplayName("movimientos para multiples obligaciones: findByObligacionPagoId filtra correctamente")
        void multipleObligaciones_filtranCorrectamente() {
            FalActaPagoMovimiento m1 = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            FalActaPagoMovimiento m2 = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            FalActaPagoMovimiento m3 = new FalActaPagoMovimiento.Builder(3L, 2L, TipoMovimientoPago.DEUDA_EMITIDA, ar.gob.malvinas.faltas.core.domain.enums.OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1").build();
            repoMov.append(m1);
            repoMov.append(m2);
            repoMov.append(m3);
            assertThat(repoMov.findByObligacionPagoId(1L)).hasSize(2);
            assertThat(repoMov.findByObligacionPagoId(2L)).hasSize(1);
            assertThat(repoMov.findByObligacionPagoId(99L)).isEmpty();
        }
    }
}