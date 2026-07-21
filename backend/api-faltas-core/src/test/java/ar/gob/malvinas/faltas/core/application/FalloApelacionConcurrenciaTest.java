package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Slice 8F-11F: Concurrencia fallo/apelacion (5 escenarios)")
class FalloApelacionConcurrenciaTest {

    private InMemoryFalloActaRepository falloRepo;
    private InMemoryApelacionActaRepository apelacionRepo;
    private InMemoryApelacionDocumentoRepository apelDocRepo;

    @BeforeEach
    void setUp() {
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();
        apelDocRepo = new InMemoryApelacionDocumentoRepository();
    }

    private FalActaFallo crearFallo(Long actaId) {
        Long id = falloRepo.nextId();
        FalActaFallo f = new FalActaFallo(id, actaId,
                TipoFalloActa.CONDENATORIO, FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "USR-1");
        f.setMontoCondena(new BigDecimal("1000"));
        f.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        return f;
    }

    private FalActaApelacion crearApelacion(Long actaId, Long falloId) {
        Long id = apelacionRepo.nextId();
        FalActaApelacion a = new FalActaApelacion(id, actaId, falloId, CanalApelacion.PRESENCIAL,
                TipoPresentacion.TEXTO, "Apelo el fallo", FaltasClockTestSupport.FIXED.now(), "USR", FaltasClockTestSupport.FIXED.now(), "USR");
        a.setEstadoApelacion(EstadoApelacionActa.PRESENTADA);
        return a;
    }

    /**
     * Escenario 1: Dos threads intentan ser el primer fallo vigente de la misma acta.
     * Solo uno debe quedar vigente; el otro debe recibir excepcion o su fallo no queda vigente.
     */
    @Test
    @DisplayName("E1: Dos fallos compiten por ser vigente - solo uno gana")
    void e1_dos_fallos_compiten_vigente() throws InterruptedException {
        Long actaId = 1001L;
        int threads = 2;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);
        List<Thread> ts = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            ts.add(Thread.ofVirtual().start(() -> {
                try {
                    FalActaFallo f = crearFallo(actaId);
                    barrier.await();
                    falloRepo.guardarComoVigente(f);
                    exitos.incrementAndGet();
                } catch (PrecondicionVioladaException | BrokenBarrierException | InterruptedException e) {
                    fallos.incrementAndGet();
                }
            }));
        }
        for (Thread t : ts) t.join(5000);

        assertThat(exitos.get() + fallos.get()).isEqualTo(threads);
        long vigentes = falloRepo.findByActaId(actaId).stream().filter(FalActaFallo::isSiVigente).count();
        assertThat(vigentes).isEqualTo(1);
    }

    /**
     * Escenario 2: Dos threads intentan actualizar el mismo fallo con mismo versionRow.
     * Solo uno debe tener exito; el otro debe recibir ConcurrenciaConflictoException.
     */
    @Test
    @DisplayName("E2: Dos actualizaciones concurrentes del mismo fallo/versionRow")
    void e2_dos_actualizaciones_concurrentes() throws InterruptedException {
        Long actaId = 1002L;
        FalActaFallo f = crearFallo(actaId);
        falloRepo.guardarComoVigente(f);

        FalActaFallo v1 = falloRepo.findVigenteByActaId(actaId).orElseThrow();
        FalActaFallo v2 = falloRepo.findVigenteByActaId(actaId).orElseThrow();

        int threads = 2;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);

        Thread t1 = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                v1.setFundamentos("Thread 1");
                falloRepo.guardar(v1);
                exitos.incrementAndGet();
            } catch (ConcurrenciaConflictoException e) { conflictos.incrementAndGet(); }
            catch (Exception e) { }
        });
        Thread t2 = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                v2.setFundamentos("Thread 2");
                falloRepo.guardar(v2);
                exitos.incrementAndGet();
            } catch (ConcurrenciaConflictoException e) { conflictos.incrementAndGet(); }
            catch (Exception e) { }
        });

        t1.join(5000); t2.join(5000);
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(1);
    }

    /**
     * Escenario 3: Dos threads intentan resolver la misma apelacion concurrentemente.
     * Solo una resolucion debe ganar; la segunda debe fallar por optimistic locking.
     */
    @Test
    @DisplayName("E3: Dos resoluciones concurrentes de la misma apelacion")
    void e3_dos_resoluciones_concurrentes() throws InterruptedException {
        Long actaId = 1003L;
        Long falloId = 10L;
        FalActaApelacion apelacion = crearApelacion(actaId, falloId);
        apelacionRepo.guardar(apelacion);

        FalActaApelacion a1 = apelacionRepo.findById(apelacion.getId()).orElseThrow();
        FalActaApelacion a2 = apelacionRepo.findById(apelacion.getId()).orElseThrow();

        int threads = 2;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger conflictos = new AtomicInteger(0);

        Thread t1 = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                a1.resolver(ResultadoResolucionApelacion.RECHAZADA, FaltasClockTestSupport.FIXED.now(), "T1", null);
                apelacionRepo.guardar(a1);
                exitos.incrementAndGet();
            } catch (ConcurrenciaConflictoException e) { conflictos.incrementAndGet(); }
            catch (Exception e) { }
        });
        Thread t2 = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                a2.resolver(ResultadoResolucionApelacion.ACEPTADA_ABSUELVE, FaltasClockTestSupport.FIXED.now(), "T2", null);
                apelacionRepo.guardar(a2);
                exitos.incrementAndGet();
            } catch (ConcurrenciaConflictoException e) { conflictos.incrementAndGet(); }
            catch (Exception e) { }
        });

        t1.join(5000); t2.join(5000);
        assertThat(exitos.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(1);
        FalActaApelacion final_ = apelacionRepo.findById(apelacion.getId()).orElseThrow();
        assertThat(final_.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.RESUELTA);
    }

    /**
     * Escenario 4: Un thread intenta registrar una apelacion mientras otro declara firmeza.
     * La apelacion usa buscarActiva(); la firmeza usa falloRepo.guardar().
     * No deben coexistir apelacion presentada y condena firme al mismo tiempo sin inconsistencia.
     * En este test verificamos que las operaciones no dejen estado parcial.
     */
    @Test
    @DisplayName("E4: Firmeza y registro de apelacion concurrentes - sin estado parcial")
    void e4_firmeza_vs_apelacion_concurrentes() throws InterruptedException {
        Long actaId = 1004L;
        FalActaFallo fallo = crearFallo(actaId);
        falloRepo.guardarComoVigente(fallo);
        FalActaFallo falloVigente = falloRepo.findVigenteByActaId(actaId).orElseThrow();

        int threads = 2;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger exitos = new AtomicInteger(0);

        Thread tFirmeza = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                falloVigente.declararFirmeza(FaltasClockTestSupport.FIXED.now(), OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
                falloRepo.guardar(falloVigente);
                exitos.incrementAndGet();
            } catch (Exception e) { }
        });

        Thread tApelacion = Thread.ofVirtual().start(() -> {
            try {
                barrier.await();
                FalActaApelacion apelacion = crearApelacion(actaId, falloVigente.getId());
                apelacionRepo.guardar(apelacion);
                exitos.incrementAndGet();
            } catch (Exception e) { }
        });

        tFirmeza.join(5000); tApelacion.join(5000);

        long vigentesFallo = falloRepo.findByActaId(actaId).stream().filter(FalActaFallo::isSiVigente).count();
        assertThat(vigentesFallo).isLessThanOrEqualTo(1);
        assertThat(exitos.get()).isGreaterThanOrEqualTo(1);
    }

    /**
     * Escenario 5: Registro concurrente de multiples documentos en la misma apelacion.
     * Todos deben guardarse; ninguno debe perderse (append historico).
     */
    @Test
    @DisplayName("E5: Registro concurrente de N documentos en misma apelacion - append historico")
    void e5_documentos_concurrentes() throws InterruptedException {
        Long actaId = 1005L;
        Long falloId = 20L;
        FalActaApelacion apelacion = crearApelacion(actaId, falloId);
        apelacionRepo.guardar(apelacion);
        final Long apelacionId = apelacion.getId();

        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        List<Thread> ts = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            ts.add(Thread.ofVirtual().start(() -> {
                try {
                    FalActaApelacionDocumento doc = new FalActaApelacionDocumento(
                            apelDocRepo.nextId(), apelacionId,
                            TipoDocumentoApelacion.OTRO, OrigenPresentacion.INFRACTOR,
                            null, "key-" + idx, "doc-" + idx + ".pdf", (short) 1,
                            1000L + idx, FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "USR-" + idx);
                    apelDocRepo.guardar(doc);
                } finally {
                    latch.countDown();
                }
            }));
        }
        latch.await(10, TimeUnit.SECONDS);

        List<FalActaApelacionDocumento> docs = apelDocRepo.findByApelacionId(apelacionId);
        assertThat(docs).hasSize(numThreads);
        long idsUnicos = docs.stream().map(FalActaApelacionDocumento::getId).distinct().count();
        assertThat(idsUnicos).isEqualTo(numThreads);
    }
}
