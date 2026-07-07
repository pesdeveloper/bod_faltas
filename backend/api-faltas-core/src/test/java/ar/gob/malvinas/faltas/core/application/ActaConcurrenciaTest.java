package ar.gob.malvinas.faltas.core.application;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de concurrencia para FalActa (OCC/versionRow) y FalActaEvento (append-only).
 * Slice 8F-11L.
 */
@DisplayName("ActaConcurrencia - OCC y append-only de eventos")
class ActaConcurrenciaTest {

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
                LocalDate.now(), LocalDateTime.now(),
                "Calle Test", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                LocalDateTime.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    @Nested
    @DisplayName("FalActa - Control de concurrencia optimista (versionRow)")
    class OccFalActa {

        @Test
        @DisplayName("Primera escritura pasa, segunda con version stale lanza ConcurrenciaConflictoException")
        void segunda_escritura_con_version_stale_falla() {
            FalActa acta = crearActa();
            assertThat(acta.getVersionRow()).isEqualTo(0);

            // Primer thread: lee y modifica
            FalActa copia1 = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            copia1.setBloqueActual(BloqueActual.ENRI);
            actaRepo.guardar(copia1); // version pasa a 1

            // Segundo thread: tiene version stale (0)
            FalActa copia2 = crearActa();
            copia2 = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            // Ahora version en repo es 1; si intentaramos guardar con version 0 falla
            // Simular stale: usar una copia con version 0
            final FalActa actaStale = new FalActa(acta.getId(), acta.getUuidTecnico(),
                    TipoActa.TRANSITO, 1L, 1L,
                    LocalDate.now(), LocalDateTime.now(),
                    "Stale", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                    LocalDateTime.now(), "TEST");
            // versionRow de actaStale es 0, pero repo tiene version 1
            assertThatThrownBy(() -> actaRepo.guardar(actaStale))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("Actualizaciones seriales incrementan versionRow correctamente")
        void actualizaciones_seriales_incrementan_version() {
            FalActa acta = crearActa();

            for (int i = 1; i <= 5; i++) {
                FalActa actual = actaRepo.buscarPorId(acta.getId()).orElseThrow();
                actual.setBloqueActual(BloqueActual.ANAL);
                FalActa guardada = actaRepo.guardar(actual);
                assertThat(guardada.getVersionRow()).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("Concurrencia: solo una escritura simultanea gana, otras fallan con OCC")
        void concurrencia_una_escritura_gana() throws InterruptedException {
            FalActa acta = crearActa();
            int hilos = 5;
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger fallos = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(hilos);
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            for (int i = 0; i < hilos; i++) {
                exec.submit(() -> {
                    try {
                        // Todos leen la misma version stale (0)
                        FalActa stale = new FalActa(acta.getId(), acta.getUuidTecnico(),
                                TipoActa.TRANSITO, 1L, 1L,
                                LocalDate.now(), LocalDateTime.now(),
                                "Intento concurrente", null, null, null,
                                ResultadoFirmaInfractor.FIRMADA, null,
                                LocalDateTime.now(), "TEST");
                        actaRepo.guardar(stale);
                        exitos.incrementAndGet();
                    } catch (ConcurrenciaConflictoException e) {
                        fallos.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            exec.shutdown();
            assertThat(exitos.get()).isEqualTo(1);
            assertThat(fallos.get()).isEqualTo(hilos - 1);
        }
    }

    @Nested
    @DisplayName("FalActaEvento - Append-only e inmutabilidad")
    class AppendOnlyEvento {

        @Test
        @DisplayName("Eventos registrados son inmutables: conId() retorna nueva instancia")
        void eventos_inmutables() {
            FalActa acta = crearActa();
            FalActaEvento original = FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .fhEvt(LocalDateTime.now())
                    .build();
            assertThat(original.getId()).isNull();

            FalActaEvento conId = original.conId(99L);
            assertThat(conId.getId()).isEqualTo(99L);
            assertThat(original.getId()).isNull(); // original no modificado
        }

        @Test
        @DisplayName("Registro concurrente de eventos no pierde ningun evento")
        void registro_concurrente_no_pierde_eventos() throws InterruptedException {
            FalActa acta = crearActa();
            int nEventos = 20;
            CountDownLatch latch = new CountDownLatch(nEventos);
            ExecutorService exec = Executors.newFixedThreadPool(10);

            for (int i = 0; i < nEventos; i++) {
                final int idx = i;
                exec.submit(() -> {
                    try {
                        eventoRepo.registrar(FalActaEvento.builder()
                                .actaId(acta.getId())
                                .tipoEvt(TipoEventoActa.DOCGEN)
                                .fhEvt(LocalDateTime.now())
                                .descripcionLegible("Evento concurrente " + idx)
                                .build());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            exec.shutdown();

            List<FalActaEvento> todos = eventoRepo.buscarPorActa(acta.getId());
            assertThat(todos).hasSize(nEventos);

            // IDs deben ser todos distintos
            long idsDistintos = todos.stream().map(FalActaEvento::getId).distinct().count();
            assertThat(idsDistintos).isEqualTo(nEventos);
        }

        @Test
        @DisplayName("buscarPorId retorna el evento correcto")
        void buscar_por_id() {
            FalActa acta = crearActa();
            FalActaEvento guardado = eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .fhEvt(LocalDateTime.now())
                    .descripcionLegible("Acta labrada test")
                    .build());

            assertThat(guardado.getId()).isNotNull();
            var encontrado = eventoRepo.buscarPorId(guardado.getId());
            assertThat(encontrado).isPresent();
            assertThat(encontrado.get().tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(encontrado.get().descripcionLegible()).isEqualTo("Acta labrada test");
        }
    }
}