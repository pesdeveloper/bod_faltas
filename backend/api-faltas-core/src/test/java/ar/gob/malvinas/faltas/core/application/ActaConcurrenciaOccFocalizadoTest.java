package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests focalizados OCC InMemory - CAS atomico por idActa.
 * Slice: CIERRE-OCC-INMEMORY-PRE-R11.
 * Complementa ActaConcurrenciaTest (no lo modifica).
 *
 * Contrato verificado:
 *   - dos threads, version igual: uno gana, uno pierde.
 *   - cuatro threads, version igual: uno gana, tres pierden.
 *   - versionRow incrementa exactamente una vez.
 *   - evento registrado exactamente una vez (evento repo thread-safe).
 *   - el perdedor recibe ConcurrenciaConflictoException.
 *   - actas distintas no quedan serializadas globalmente.
 *   - sin lock global que bloquee ids diferentes.
 *
 * Paridad MariaDB futura:
 *   UPDATE fal_acta SET ..., version_row = version_row + 1
 *   WHERE id_acta = ? AND version_row = ?
 *   -> filas=0 => ConcurrenciaConflictoException
 */
@DisplayName("OCC InMemory focalizado (CAS atomico por idActa)")
class ActaConcurrenciaOccFocalizadoTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
    }

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Calle Test", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    private FalActa staleConVersion(FalActa acta, int version) {
        FalActa stale = new FalActa(acta.getId(), acta.getUuidTecnico(),
                TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Intento concurrente", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), "TEST");
        stale.setVersionRow(version);
        return stale;
    }

    // =========================================================
    // 1. Dos threads, misma version: uno gana, uno pierde
    // =========================================================

    @Nested
    @DisplayName("Dos threads - CAS: uno gana, uno pierde")
    class DosThreads {

        @Test
        @DisplayName("dos threads con version 0: exito=1, fallo=1")
        void dos_threads_uno_gana_uno_pierde() throws Exception {
            FalActa acta = crearActa();
            int hilos = 2;
            CyclicBarrier barrier = new CyclicBarrier(hilos);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger fallos = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(hilos);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                final FalActa stale = staleConVersion(acta, 0);
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        actaRepo.guardar(stale);
                        exitos.incrementAndGet();
                    } catch (ConcurrenciaConflictoException e) {
                        fallos.incrementAndGet();
                    } catch (BrokenBarrierException | InterruptedException | java.util.concurrent.TimeoutException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(fallos.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("el perdedor recibe ConcurrenciaConflictoException - no RuntimeException generica")
        void perdedor_recibe_excepcion_occ() throws Exception {
            FalActa acta = crearActa();
            int hilos = 2;
            CyclicBarrier barrier = new CyclicBarrier(hilos);
            List<Throwable> excepciones = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(hilos);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                final FalActa stale = staleConVersion(acta, 0);
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        actaRepo.guardar(stale);
                    } catch (ConcurrenciaConflictoException e) {
                        synchronized (excepciones) { excepciones.add(e); }
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            assertThat(excepciones).hasSize(1);
            assertThat(excepciones.get(0)).isInstanceOf(ConcurrenciaConflictoException.class);
        }
    }

    // =========================================================
    // 2. Cuatro threads, misma version: uno gana, tres pierden
    // =========================================================

    @Nested
    @DisplayName("Cuatro threads - CAS: uno gana, tres pierden")
    class CuatroThreads {

        @Test
        @DisplayName("cuatro threads con version 0: exito=1, fallo=3")
        void cuatro_threads_uno_gana_tres_pierden() throws Exception {
            FalActa acta = crearActa();
            int hilos = 4;
            CyclicBarrier barrier = new CyclicBarrier(hilos);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger fallos = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(hilos);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                final FalActa stale = staleConVersion(acta, 0);
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        actaRepo.guardar(stale);
                        exitos.incrementAndGet();
                    } catch (ConcurrenciaConflictoException e) {
                        fallos.incrementAndGet();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(fallos.get()).isEqualTo(3);
        }
    }

    // =========================================================
    // 3. versionRow incrementado exactamente una vez
    // =========================================================

    @Nested
    @DisplayName("versionRow - incremento exactamente una vez")
    class VersionRowExacto {

        @Test
        @DisplayName("tras 5 escrituras concurrentes con version 0, versionRow en store es 1")
        void version_row_incrementada_exactamente_una_vez() throws Exception {
            FalActa acta = crearActa();
            int hilos = 5;
            CyclicBarrier barrier = new CyclicBarrier(hilos);
            CountDownLatch latch = new CountDownLatch(hilos);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                final FalActa stale = staleConVersion(acta, 0);
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        actaRepo.guardar(stale);
                    } catch (ConcurrenciaConflictoException | BrokenBarrierException
                             | InterruptedException | java.util.concurrent.TimeoutException ignored) {
                        // esperado para los perdedores
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            FalActa enStore = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(enStore.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("el perdedor no deja cambios parciales: store refleja solo al ganador")
        void perdedor_no_deja_cambios_parciales() throws Exception {
            FalActa acta = crearActa();
            int hilos = 5;
            CyclicBarrier barrier = new CyclicBarrier(hilos);
            CountDownLatch latch = new CountDownLatch(hilos);
            AtomicReference<String> ganadorDomicilio = new AtomicReference<>(null);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                final String domicilio = "Intento-" + i;
                final FalActa stale = new FalActa(acta.getId(), acta.getUuidTecnico(),
                        TipoActa.TRANSITO, 1L, 1L,
                        FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                        domicilio, null, null, null,
                        ResultadoFirmaInfractor.FIRMADA, null,
                        FaltasClockTestSupport.FIXED.now(), "TEST");
                stale.setVersionRow(0);
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        actaRepo.guardar(stale);
                        ganadorDomicilio.compareAndSet(null, domicilio);
                    } catch (ConcurrenciaConflictoException | BrokenBarrierException
                             | InterruptedException | java.util.concurrent.TimeoutException ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            FalActa enStore = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(enStore.getDomicilioHecho()).isEqualTo(ganadorDomicilio.get());
            assertThat(enStore.getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // 4. Evento registrado exactamente una vez
    // =========================================================

    @Nested
    @DisplayName("Evento - registro exactamente una vez bajo OCC")
    class EventoExactoUnaVez {

        @Test
        @DisplayName("20 threads intentan registrar evento: se registran exactamente 20 (no duplicados)")
        void registro_concurrente_sin_duplicados_ni_perdidas() throws Exception {
            FalActa acta = crearActa();
            int n = 20;
            CountDownLatch latch = new CountDownLatch(n);
            CyclicBarrier barrier = new CyclicBarrier(n);
            ExecutorService exec = Executors.newFixedThreadPool(n);

            for (int i = 0; i < n; i++) {
                final int idx = i;
                exec.submit(() -> {
                    try {
                        barrier.await(5, TimeUnit.SECONDS);
                        eventoRepo.registrar(FalActaEvento.builder()
                                .actaId(acta.getId())
                                .tipoEvt(TipoEventoActa.DOCGEN)
                                .fhEvt(FaltasClockTestSupport.FIXED.now())
                                .descripcionLegible("Evento OCC " + idx)
                                .build());
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            List<FalActaEvento> todos = eventoRepo.buscarPorActa(acta.getId());
            assertThat(todos).hasSize(n);

            long idsDistintos = todos.stream().map(FalActaEvento::getId).distinct().count();
            assertThat(idsDistintos).isEqualTo(n);
        }
    }

    // =========================================================
    // 5. Actas distintas: no se serializan globalmente
    // =========================================================

    @Nested
    @DisplayName("Actas distintas - sin lock global")
    class ActasDistintasParalelas {

        @Test
        @DisplayName("dos threads escriben sobre actas distintas: ambos tienen exito en paralelo")
        void actas_distintas_no_se_serializan_globalmente() throws Exception {
            FalActa actaA = crearActa();
            FalActa actaB = crearActa();
            assertThat(actaA.getId()).isNotEqualTo(actaB.getId());

            CyclicBarrier barrier = new CyclicBarrier(2);
            AtomicInteger exitos = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(2);
            ExecutorService exec = Executors.newFixedThreadPool(2);

            FalActa staleA = staleConVersion(actaA, 0);
            FalActa staleB = staleConVersion(actaB, 0);

            exec.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    actaRepo.guardar(staleA);
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });

            exec.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                    actaRepo.guardar(staleB);
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            assertThat(exitos.get()).isEqualTo(2);

            FalActa aEnStore = actaRepo.buscarPorId(actaA.getId()).orElseThrow();
            FalActa bEnStore = actaRepo.buscarPorId(actaB.getId()).orElseThrow();
            assertThat(aEnStore.getVersionRow()).isEqualTo(1);
            assertThat(bEnStore.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("ocho threads escriben sobre cuatro actas distintas: exito=8, sin interferencia")
        void ocho_threads_cuatro_actas_distintas_sin_interferencia() throws Exception {
            int nActas = 4;
            int threadsPorActa = 2;
            int totalHilos = nActas * threadsPorActa;

            List<FalActa> actas = new ArrayList<>();
            for (int i = 0; i < nActas; i++) {
                actas.add(crearActa());
            }

            CyclicBarrier barrier = new CyclicBarrier(totalHilos);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger fallos = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(totalHilos);
            ExecutorService exec = Executors.newFixedThreadPool(totalHilos);

            for (int actaIdx = 0; actaIdx < nActas; actaIdx++) {
                final FalActa acta = actas.get(actaIdx);
                for (int t = 0; t < threadsPorActa; t++) {
                    final FalActa stale = staleConVersion(acta, 0);
                    exec.submit(() -> {
                        try {
                            barrier.await(5, TimeUnit.SECONDS);
                            actaRepo.guardar(stale);
                            exitos.incrementAndGet();
                        } catch (ConcurrenciaConflictoException e) {
                            fallos.incrementAndGet();
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            latch.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            // Por cada acta: 1 gana, 1 pierde
            assertThat(exitos.get()).isEqualTo(nActas);
            assertThat(fallos.get()).isEqualTo(nActas);

            for (FalActa acta : actas) {
                FalActa enStore = actaRepo.buscarPorId(acta.getId()).orElseThrow();
                assertThat(enStore.getVersionRow()).isEqualTo(1);
            }
        }
    }
}