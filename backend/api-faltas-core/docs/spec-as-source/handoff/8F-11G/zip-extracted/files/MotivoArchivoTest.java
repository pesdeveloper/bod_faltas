package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.MotivoArchivoService;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalMotivoArchivo;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMotivoArchivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11G: MotivoArchivoTest")
class MotivoArchivoTest {

    private InMemoryMotivoArchivoRepository repo;
    private MotivoArchivoService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryMotivoArchivoRepository();
        service = new MotivoArchivoService(repo);
    }

    @Nested
    @DisplayName("FalMotivoArchivo - validaciones de constructor")
    class ModeloValidaciones {

        @Test
        @DisplayName("codigo null rechazado")
        void codigo_null_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, null, "nombre", null,
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codigo de 32 caracteres aceptado")
        void codigo_limite_32() {
            String cod = "A".repeat(32);
            FalMotivoArchivo m = new FalMotivoArchivo(1L, cod, "nombre", null,
                    false, false, false, true, null, "U");
            assertThat(m.getCodMotivoArchivo()).isEqualTo(cod.toUpperCase());
        }

        @Test
        @DisplayName("codigo de 33 caracteres rechazado")
        void codigo_supera_32_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, "A".repeat(33), "nombre", null,
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nombre de 80 caracteres aceptado")
        void nombre_limite_80() {
            FalMotivoArchivo m = new FalMotivoArchivo(1L, "COD", "N".repeat(80), null,
                    false, false, false, true, null, "U");
            assertThat(m.getNombre()).hasSize(80);
        }

        @Test
        @DisplayName("nombre supera 80 rechazado")
        void nombre_supera_80_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, "COD", "N".repeat(81), null,
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("descripcion de 255 caracteres aceptada")
        void descripcion_limite_255() {
            FalMotivoArchivo m = new FalMotivoArchivo(1L, "COD", "nombre", "D".repeat(255),
                    false, false, false, true, null, "U");
            assertThat(m.getDescripcion()).hasSize(255);
        }

        @Test
        @DisplayName("descripcion supera 255 rechazada")
        void descripcion_supera_255_rechazada() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, "COD", "nombre", "D".repeat(256),
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codigo normalizado a mayusculas")
        void codigo_normalizado_mayusculas() {
            FalMotivoArchivo m = new FalMotivoArchivo(1L, "prescripcion", "nombre", null,
                    false, false, false, true, null, "U");
            assertThat(m.getCodMotivoArchivo()).isEqualTo("PRESCRIPCION");
        }
    }

    @Nested
    @DisplayName("MotivoArchivoService - operaciones")
    class ServicioOperaciones {

        @Test
        @DisplayName("crear motivo disponible en listarActivos")
        void crear_disponible() {
            FalMotivoArchivo m = service.crear("TEST_MOT", "Motivo test", null,
                    false, false, false, "U");
            assertThat(service.listarActivos()).hasSize(1);
            assertThat(m.isSiActivo()).isTrue();
        }

        @Test
        @DisplayName("unicidad de codigo - duplicado rechazado")
        void unicidad_codigo() {
            service.crear("UNICO", "nombre1", null, false, false, false, "U");
            assertThatThrownBy(() -> service.crear("UNICO", "nombre2", null, false, false, false, "U"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("dar de baja quita de listarActivos pero mantiene en listarTodos")
        void baja_logica() {
            FalMotivoArchivo m = service.crear("M1", "nombre", null, false, false, false, "U");
            service.darDeBaja(m.getId(), "U");
            assertThat(service.listarActivos()).isEmpty();
            assertThat(service.listarTodos()).hasSize(1);
        }

        @Test
        @DisplayName("motivo inactivo no seleccionable para archivo")
        void inactivo_no_seleccionable() {
            FalMotivoArchivo m = service.crear("M2", "nombre", null, false, false, false, "U");
            service.darDeBaja(m.getId(), "U");
            assertThatThrownBy(() -> service.buscarActivoPorId(m.getId()))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("actualizar no afecta ciclos historicos - snapshot inmutable")
        void actualizar_no_afecta_historico() {
            FalMotivoArchivo m = service.crear("M3", "nombre original", "desc", false, true, false, "U");
            boolean reingresoOriginal = m.isSiPermiteReingreso();
            service.actualizar(m.getId(), "nombre nuevo", "desc nueva", false, false, false, "U");
            assertThat(reingresoOriginal).isTrue();
            FalMotivoArchivo actualizado = service.buscarPorId(m.getId());
            assertThat(actualizado.isSiPermiteReingreso()).isFalse();
        }
    }

    @Nested
    @DisplayName("Concurrencia - unicidad de codigo")
    class ConcurrenciaTests {

        @Test
        @DisplayName("dos creaciones del mismo codigo: solo una gana")
        void dos_creaciones_misma_codigo() throws InterruptedException {
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);

            Runnable tarea = () -> {
                try {
                    start.await();
                    service.crear("CONCURRENTE", "nombre", null, false, false, false, "U");
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    errores.incrementAndGet();
                }
            };

            Thread t1 = new Thread(tarea);
            Thread t2 = new Thread(tarea);
            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(exitos.get() + errores.get()).isEqualTo(2);
            assertThat(service.listarTodos().stream()
                    .filter(m -> m.getCodMotivoArchivo().equals("CONCURRENTE")).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("baja de motivo compite con creacion: estado coherente")
        void baja_compite_con_creacion() throws InterruptedException {
            FalMotivoArchivo m = service.crear("EXISTENTE", "nombre", null, false, false, false, "U");
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger ops = new AtomicInteger(0);

            Thread t1 = new Thread(() -> {
                try { start.await(); service.darDeBaja(m.getId(), "U"); ops.incrementAndGet(); }
                catch (Exception ignored) { ops.incrementAndGet(); }
            });
            Thread t2 = new Thread(() -> {
                try { start.await(); service.crear("NUEVO_M", "nombre2", null, false, false, false, "U"); ops.incrementAndGet(); }
                catch (Exception ignored) { ops.incrementAndGet(); }
            });

            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(ops.get()).isEqualTo(2);
        }
    }
}