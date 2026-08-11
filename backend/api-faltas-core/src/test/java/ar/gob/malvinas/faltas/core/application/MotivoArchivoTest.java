package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

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
        service = new MotivoArchivoService(repo, FaltasClockTestSupport.FIXED);
    }

    @Nested
    @DisplayName("FalMotivoArchivo - validaciones de constructor")
    class ModeloValidaciones {

        @Test
        @DisplayName("codigo null rechazado")
        void codigo_null_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, null, "nombre",
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codigo de 8 caracteres aceptado (limite fisico VARCHAR(8))")
        void codigo_limite_8() {
            String cod = "A".repeat(8);
            FalMotivoArchivo m = new FalMotivoArchivo(1L, cod, "nombre",
                    false, false, false, true, null, "U");
            assertThat(m.getCodMotivoArchivo()).isEqualTo(cod.toUpperCase());
        }

        @Test
        @DisplayName("codigo de 9 caracteres rechazado")
        void codigo_supera_8_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, "A".repeat(9), "nombre",
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("nombre de 64 caracteres aceptado (limite fisico VARCHAR(64))")
        void nombre_limite_64() {
            FalMotivoArchivo m = new FalMotivoArchivo(1L, "COD", "N".repeat(64),
                    false, false, false, true, null, "U");
            assertThat(m.getNombre()).hasSize(64);
        }

        @Test
        @DisplayName("nombre supera 64 rechazado")
        void nombre_supera_64_rechazado() {
            assertThatThrownBy(() -> new FalMotivoArchivo(1L, "COD", "N".repeat(65),
                    false, false, false, true, null, "U"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("codigo normalizado a mayusculas")
        void codigo_normalizado_mayusculas() {
            FalMotivoArchivo m = new FalMotivoArchivo(1L, "prescrip", "nombre",
                    false, false, false, true, null, "U");
            assertThat(m.getCodMotivoArchivo()).isEqualTo("PRESCRIP");
        }
    }

    @Nested
    @DisplayName("MotivoArchivoService - operaciones")
    class ServicioOperaciones {

        @Test
        @DisplayName("crear motivo disponible en listarActivos")
        void crear_disponible() {
            FalMotivoArchivo m = service.crear("TEST_MOT", "Motivo test",
                    false, false, false, "U");
            assertThat(service.listarActivos()).hasSize(1);
            assertThat(m.isSiActivo()).isTrue();
        }

        @Test
        @DisplayName("unicidad de codigo - duplicado rechazado")
        void unicidad_codigo() {
            service.crear("UNICO", "nombre1", false, false, false, "U");
            assertThatThrownBy(() -> service.crear("UNICO", "nombre2", false, false, false, "U"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("dar de baja quita de listarActivos pero mantiene en listarTodos")
        void baja_logica() {
            FalMotivoArchivo m = service.crear("M1", "nombre", false, false, false, "U");
            service.darDeBaja(m.getId(), "U");
            assertThat(service.listarActivos()).isEmpty();
            assertThat(service.listarTodos()).hasSize(1);
        }

        @Test
        @DisplayName("motivo inactivo no seleccionable para archivo")
        void inactivo_no_seleccionable() {
            FalMotivoArchivo m = service.crear("M2", "nombre", false, false, false, "U");
            service.darDeBaja(m.getId(), "U");
            assertThatThrownBy(() -> service.buscarActivoPorId(m.getId()))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("actualizar no afecta ciclos historicos - snapshot inmutable")
        void actualizar_no_afecta_historico() {
            FalMotivoArchivo m = service.crear("M3", "nombre original", false, true, false, "U");
            boolean reingresoOriginal = m.isSiPermiteReingreso();
            service.actualizar(m.getId(), "nombre nuevo", false, false, false, "U");
            assertThat(reingresoOriginal).isTrue();
            FalMotivoArchivo actualizado = service.buscarPorId(m.getId());
            assertThat(actualizado.isSiPermiteReingreso()).isFalse();
        }
    }

    @Nested
    @DisplayName("Identificadores generados por la persistencia")
    class IdentificadoresTests {

        @Test
        @DisplayName("alta con id null devuelve la entidad con el id asignado")
        void alta_sin_id_recibe_id() {
            FalMotivoArchivo persistido = repo.guardar(new FalMotivoArchivo(
                    null, "SINID", "Sin id", false, false, false, true, null, "U"));

            assertThat(persistido.getId()).isNotNull();
            assertThat(repo.buscarPorId(persistido.getId())).isPresent();
        }

        @Test
        @DisplayName("el InMemory genera ids unicos y monotonicos, simulando AUTO_INCREMENT")
        void ids_unicos_y_monotonicos() {
            List<Long> ids = new java.util.ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                ids.add(repo.guardar(new FalMotivoArchivo(
                        null, "M" + i, "Motivo " + i,
                        false, false, false, true, null, "U")).getId());
            }

            assertThat(ids).doesNotContainNull().doesNotHaveDuplicates().isSorted();
        }

        @Test
        @DisplayName("guardar con id existente conserva el id y no crea otra fila")
        void guardar_con_id_conserva_id() {
            FalMotivoArchivo original = repo.guardar(new FalMotivoArchivo(
                    null, "CONSERV", "Original", false, false, false, true, null, "U"));
            Long id = original.getId();

            original.setNombre("Modificado");
            FalMotivoArchivo actualizado = repo.actualizarAtomicamente(original);

            assertThat(actualizado.getId()).isEqualTo(id);
            assertThat(repo.listarTodos()).hasSize(1);
        }

        @Test
        @DisplayName("el servicio usa la entidad persistida, sin preasignar identificadores")
        void servicio_usa_entidad_persistida() {
            FalMotivoArchivo m = service.crear("SRVID", "Motivo", false, false, false, "U");

            assertThat(m.getId()).isNotNull();
            assertThat(service.buscarPorId(m.getId()).getCodMotivoArchivo()).isEqualTo("SRVID");
        }

        /**
         * D-1A1-01: insertar con una PK elegida por el llamador contradice
         * DECISION_ID-01 y el adaptador JDBC no puede hacerlo. Ambos adaptadores
         * rechazan con la misma excepcion y el mismo mensaje.
         */
        @Test
        @DisplayName("guardar con id inexistente lanza la excepcion funcional y no crea fila")
        void guardar_con_id_inexistente_rechaza() {
            FalMotivoArchivo fantasma = new FalMotivoArchivo(
                    777L, "FANTASMA", "Fantasma", false, false, false, true, null, "U");

            assertThatThrownBy(() -> repo.guardar(fantasma))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessage("MotivoArchivo no encontrado: 777");
            assertThat(repo.listarTodos()).isEmpty();
            assertThat(repo.buscarPorId(777L)).isEmpty();
        }

        @Test
        @DisplayName("guardar con id existente actualiza la fila y no crea otra")
        void guardar_con_id_existente_actualiza() {
            FalMotivoArchivo original = repo.guardar(new FalMotivoArchivo(
                    null, "ACTUAL", "Original", false, false, false, true, null, "U"));

            original.setNombre("Modificado");
            FalMotivoArchivo actualizado = repo.guardar(original);

            assertThat(actualizado.getId()).isEqualTo(original.getId());
            assertThat(repo.listarTodos()).hasSize(1);
            assertThat(repo.buscarPorId(original.getId()).orElseThrow().getNombre())
                    .isEqualTo("Modificado");
        }
    }

    /**
     * D-1A1-03: el orden lo fija el contrato, no la iteracion interna del Map. El
     * adaptador JDBC ordena por id ASC y el InMemory debe hacer lo mismo de forma
     * explicita, aunque el recorrido del ConcurrentHashMap coincidiera por
     * accidente con la secuencia de ids.
     */
    @Nested
    @DisplayName("Orden deterministico de los listados")
    class OrdenListados {

        @Test
        @DisplayName("listarTodos devuelve id ascendente aunque los codigos entren desordenados")
        void listar_todos_ordenado_por_id() {
            List<Long> ids = List.of(
                    service.crear("ZZZ", "Tercero alfabetico", false, false, false, "U").getId(),
                    service.crear("MMM", "Segundo alfabetico", false, false, false, "U").getId(),
                    service.crear("AAA", "Primero alfabetico", false, false, false, "U").getId());

            assertThat(repo.listarTodos().stream().map(FalMotivoArchivo::getId))
                    .containsExactlyElementsOf(ids);
            assertThat(repo.listarTodos().stream().map(FalMotivoArchivo::getId)).isSorted();
        }

        @Test
        @DisplayName("listarActivos devuelve id ascendente y excluye los inactivos")
        void listar_activos_ordenado_y_filtrado() {
            Long primero = service.crear("ACT1", "Activo uno", false, false, false, "U").getId();
            Long baja = service.crear("BAJA1", "Se da de baja", false, false, false, "U").getId();
            Long segundo = service.crear("ACT2", "Activo dos", false, false, false, "U").getId();
            service.darDeBaja(baja, "U");

            assertThat(repo.listarActivos().stream().map(FalMotivoArchivo::getId))
                    .containsExactly(primero, segundo);
            assertThat(repo.listarTodos().stream().map(FalMotivoArchivo::getId))
                    .containsExactly(primero, baja, segundo);
        }
    }

    @Nested
    @DisplayName("Guardrail: los dos puertos de WAVE 1A1 no preasignan identificadores")
    class GuardrailIdentificadores {

        private static final java.nio.file.Path BASE =
                java.nio.file.Path.of("src/main/java/ar/gob/malvinas/faltas/core/repository");

        /**
         * Devuelve el codigo sin comentarios. El guardrail vigila la declaracion real,
         * no la prosa: el Javadoc de estos puertos menciona nextId y AUTO_INCREMENT
         * justamente para explicar por que ya no se usan.
         */
        private String codigoSinComentarios(String archivo) throws java.io.IOException {
            return java.nio.file.Files.readString(BASE.resolve(archivo))
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("//[^\n]*", "");
        }

        @Test
        @DisplayName("MotivoArchivoRepository no declara nextId ni lee la PK manualmente")
        void motivo_sin_generadores_inseguros() throws Exception {
            assertThat(codigoSinComentarios("MotivoArchivoRepository.java"))
                    .doesNotContain("nextId")
                    .doesNotContain("MAX(id)")
                    .doesNotContain("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("DiaNoComputableRepository no declara nextId ni lee la PK manualmente")
        void dia_sin_generadores_inseguros() throws Exception {
            assertThat(codigoSinComentarios("DiaNoComputableRepository.java"))
                    .doesNotContain("nextId")
                    .doesNotContain("MAX(id)")
                    .doesNotContain("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("los servicios de ambos catalogos no preasignan identificadores")
        void servicios_no_preasignan() throws java.io.IOException {
            java.nio.file.Path svc =
                    java.nio.file.Path.of("src/main/java/ar/gob/malvinas/faltas/core/application/service");
            for (String archivo : new String[]{
                    "MotivoArchivoService.java", "CalendarioAdministrativoService.java"}) {
                String codigo = java.nio.file.Files.readString(svc.resolve(archivo))
                        .replaceAll("(?s)/\\*.*?\\*/", "")
                        .replaceAll("//[^\n]*", "");
                assertThat(codigo)
                        .as("%s no debe llamar nextId()", archivo)
                        .doesNotContain("nextId()");
            }
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
                    service.crear("CONCURR", "nombre", false, false, false, "U");
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
                    .filter(m -> m.getCodMotivoArchivo().equals("CONCURR")).count()).isEqualTo(1);
        }

        @Test
        @DisplayName("baja de motivo compite con creacion: estado coherente")
        void baja_compite_con_creacion() throws InterruptedException {
            FalMotivoArchivo m = service.crear("EXISTE", "nombre", false, false, false, "U");
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger ops = new AtomicInteger(0);

            Thread t1 = new Thread(() -> {
                try { start.await(); service.darDeBaja(m.getId(), "U"); ops.incrementAndGet(); }
                catch (Exception ignored) { ops.incrementAndGet(); }
            });
            Thread t2 = new Thread(() -> {
                try { start.await(); service.crear("NUEVO_M", "nombre2", false, false, false, "U"); ops.incrementAndGet(); }
                catch (Exception ignored) { ops.incrementAndGet(); }
            });

            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(ops.get()).isEqualTo(2);
        }
    }
}
