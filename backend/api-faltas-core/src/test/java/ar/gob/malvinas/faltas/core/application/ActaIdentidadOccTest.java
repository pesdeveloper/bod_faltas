package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;

import ar.gob.malvinas.faltas.core.application.demo.GraphDemoActaFactory;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WAVE 1A2-A1: identidad y OCC del acta.
 *
 * DECISION_ID-01 aplicada a Acta: la PK la asigna la persistencia.
 * DECISION_OCC-01: alta con id nulo y version inicial 0; guardar devuelve copia.
 * DECISION_OCC-01/B: cero filas afectadas se desambigua en 404 y 409.
 */
@DisplayName("WAVE 1A2-A1: identidad y OCC de ActaRepository")
class ActaIdentidadOccTest {

    private InMemoryActaRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryActaRepository();
    }

    private static FalActa nuevaActaSinId() {
        return new FalActa(
                null, "uuid-" + UUID.randomUUID(), TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Calle Test 100", "Observaciones", -34.5, -58.5,
                ResultadoFirmaInfractor.FIRMADA, SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, 7L,
                FaltasClockTestSupport.FIXED.now(), "TEST");
    }

    // =====================================================================
    // 10.1 FalActa
    // =====================================================================

    @Nested
    @DisplayName("FalActa - contrato de identidad")
    class ContratoFalActa {

        @Test
        @DisplayName("acta nueva admite id nulo y arranca en versionRow 0")
        void nueva_admite_id_nulo() {
            FalActa acta = nuevaActaSinId();
            assertThat(acta.getId()).isNull();
            assertThat(acta.getVersionRow()).isZero();
        }

        @Test
        @DisplayName("conId conserva todos los campos de la entidad original")
        void conId_conserva_campos() {
            FalActa acta = nuevaActaSinId();
            acta.setBloqueActual(BloqueActual.ANAL);
            acta.setNroActa("ACT-2026-00042");
            acta.setCodigoQr("QR0.token");
            acta.setResumenHecho("resumen del hecho");
            acta.setVersionRow(3);

            FalActa conId = acta.conId(77L);

            assertThat(conId.getId()).isEqualTo(77L);
            assertThat(conId.getUuidTecnico()).isEqualTo(acta.getUuidTecnico());
            assertThat(conId.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(conId.getNroActa()).isEqualTo("ACT-2026-00042");
            assertThat(conId.getCodigoQr()).isEqualTo("QR0.token");
            assertThat(conId.getResumenHecho()).isEqualTo("resumen del hecho");
            assertThat(conId.getVersionRow()).isEqualTo(3);
            assertThat(conId.getTipoActa()).isEqualTo(acta.getTipoActa());
            assertThat(conId.getIdPersonaInfractor()).isEqualTo(acta.getIdPersonaInfractor());
            assertThat(conId.getResultadoFirmaInfractor()).isEqualTo(acta.getResultadoFirmaInfractor());
            assertThat(conId.getFhAlta()).isEqualTo(acta.getFhAlta());
            assertThat(conId.getIdUserAlta()).isEqualTo(acta.getIdUserAlta());
        }

        @Test
        @DisplayName("conId devuelve una copia y no muta la instancia original")
        void conId_no_muta_original() {
            FalActa acta = nuevaActaSinId();

            FalActa conId = acta.conId(5L);

            assertThat(acta.getId()).isNull();
            assertThat(conId).isNotSameAs(acta);
        }

        @Test
        @DisplayName("el id persistido debe ser positivo")
        void conId_rechaza_id_invalido() {
            FalActa acta = nuevaActaSinId();

            assertThatThrownBy(() -> acta.conId(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> acta.conId(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positivo");
            assertThatThrownBy(() -> acta.conId(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positivo");
        }
    }

    // =====================================================================
    // Identidad tecnica del acta demo (H-1A2-A1-01)
    // =====================================================================

    @Nested
    @DisplayName("GraphDemoActaFactory - identidad tecnica del acta demo")
    class IdentidadTecnicaDemo {

        @Test
        @DisplayName("crearActaDemo() devuelve un acta nueva: id nulo y versionRow 0")
        void demo_sin_id_ni_version() {
            FalActa demo = GraphDemoActaFactory.crearActaDemo();

            assertThat(demo.getId()).isNull();
            assertThat(demo.getVersionRow()).isZero();
        }

        @Test
        @DisplayName("el id tecnico demo entra en fal_acta.id_tecnico CHAR(36)")
        void demo_id_tecnico_de_36_caracteres() {
            String idTecnico = GraphDemoActaFactory.crearActaDemo().getUuidTecnico();

            assertThat(idTecnico).isNotNull();
            assertThat(idTecnico.length()).isEqualTo(36);
        }

        @Test
        @DisplayName("el id tecnico demo es un UUID canonico")
        void demo_id_tecnico_es_uuid_canonico() {
            String idTecnico = GraphDemoActaFactory.crearActaDemo().getUuidTecnico();

            assertThat(UUID.fromString(idTecnico).toString()).isEqualTo(idTecnico);
        }

        @Test
        @DisplayName("dos ejecuciones demo no colisionan contra uq_fal_acta_id_tecnico")
        void demo_id_tecnico_unico_por_invocacion() {
            String primero = GraphDemoActaFactory.crearActaDemo().getUuidTecnico();
            String segundo = GraphDemoActaFactory.crearActaDemo().getUuidTecnico();

            assertThat(primero).isNotEqualTo(segundo);
        }

        @Test
        @DisplayName("el id tecnico demo queda fijado antes de que exista la PK")
        void demo_id_tecnico_no_deriva_de_la_pk() {
            FalActa nueva = GraphDemoActaFactory.crearActaDemo();
            String antesDePersistir = nueva.getUuidTecnico();

            FalActa persistida = repo.guardar(nueva);

            assertThat(nueva.getId()).isNull();
            assertThat(persistida.getId()).isNotNull();
            assertThat(persistida.getUuidTecnico()).isEqualTo(antesDePersistir);
        }

        @Test
        @DisplayName("la sobrecarga con Long sigue sirviendo a los tests que no persisten")
        void sobrecarga_con_id_conserva_comportamiento() {
            FalActa demo = GraphDemoActaFactory.crearActaDemo(1L);

            assertThat(demo.getId()).isEqualTo(1L);
            assertThat(demo.getUuidTecnico()).isEqualTo("uuid-demo-1");
            assertThat(demo.getNroActa()).isEqualTo(GraphDemoActaFactory.NRO_ACTA_DEMO);
        }
    }

    // =====================================================================
    // 10.2 InMemoryActaRepository - alta
    // =====================================================================

    @Nested
    @DisplayName("InMemoryActaRepository - alta")
    class AltaInMemory {

        @Test
        @DisplayName("alta con id nulo recibe un id de la persistencia y version 0")
        void alta_asigna_id() {
            FalActa nueva = nuevaActaSinId();

            FalActa persistida = repo.guardar(nueva);

            assertThat(persistida.getId()).isNotNull().isPositive();
            assertThat(persistida.getVersionRow()).isZero();
        }

        @Test
        @DisplayName("dos altas reciben identificadores distintos")
        void dos_altas_ids_distintos() {
            FalActa a = repo.guardar(nuevaActaSinId());
            FalActa b = repo.guardar(nuevaActaSinId());

            assertThat(a.getId()).isNotEqualTo(b.getId());
        }

        @Test
        @DisplayName("el argumento no se muta: conserva id nulo tras el alta")
        void argumento_conserva_id_nulo() {
            FalActa nueva = nuevaActaSinId();

            FalActa persistida = repo.guardar(nueva);

            assertThat(nueva.getId()).isNull();
            assertThat(persistida).isNotSameAs(nueva);
        }

        @Test
        @DisplayName("buscarPorId devuelve la entidad persistida")
        void buscar_devuelve_la_persistida() {
            FalActa persistida = repo.guardar(nuevaActaSinId());

            FalActa leida = repo.buscarPorId(persistida.getId()).orElseThrow();

            assertThat(leida.getId()).isEqualTo(persistida.getId());
            assertThat(leida.getUuidTecnico()).isEqualTo(persistida.getUuidTecnico());
            assertThat(leida.getVersionRow()).isZero();
        }

        @Test
        @DisplayName("un alta con versionRow distinto de 0 es rechazada")
        void alta_con_version_invalida_rechazada() {
            FalActa nueva = nuevaActaSinId();
            nueva.setVersionRow(4);

            assertThatThrownBy(() -> repo.guardar(nueva))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("versionRow=0");
            assertThat(repo.size()).isZero();
        }
    }

    // =====================================================================
    // 10.3 InMemoryActaRepository - actualizacion OCC
    // =====================================================================

    @Nested
    @DisplayName("InMemoryActaRepository - actualizacion OCC")
    class ActualizacionOcc {

        @Test
        @DisplayName("version vigente: actualiza, devuelve copia y sube la version exactamente uno")
        void version_vigente_actualiza() {
            FalActa persistida = repo.guardar(nuevaActaSinId());
            FalActa leida = repo.buscarPorId(persistida.getId()).orElseThrow();
            leida.setBloqueActual(BloqueActual.ENRI);

            FalActa actualizada = repo.guardar(leida);

            assertThat(actualizada.getVersionRow()).isEqualTo(1);
            assertThat(actualizada).isNotSameAs(leida);
            assertThat(leida.getVersionRow()).isZero();
            assertThat(repo.buscarPorId(persistida.getId()).orElseThrow().getBloqueActual())
                    .isEqualTo(BloqueActual.ENRI);
        }

        @Test
        @DisplayName("id inexistente lanza ActaNoEncontradaException y no inserta fila")
        void id_inexistente_es_404() {
            FalActa fantasma = nuevaActaSinId().conId(9_999L);

            assertThatThrownBy(() -> repo.guardar(fantasma))
                    .isInstanceOf(ActaNoEncontradaException.class)
                    .hasMessage("Acta no encontrada: 9999");
            assertThat(repo.size()).isZero();
            assertThat(repo.buscarPorId(9_999L)).isEmpty();
        }

        @Test
        @DisplayName("version obsoleta lanza ConcurrenciaConflictoException y no modifica la fila")
        void version_obsoleta_es_409() {
            FalActa persistida = repo.guardar(nuevaActaSinId());
            FalActa lectorA = repo.buscarPorId(persistida.getId()).orElseThrow();
            FalActa lectorB = repo.buscarPorId(persistida.getId()).orElseThrow();

            lectorA.setBloqueActual(BloqueActual.ANAL);
            repo.guardar(lectorA);

            lectorB.setBloqueActual(BloqueActual.NOTI);
            assertThatThrownBy(() -> repo.guardar(lectorB))
                    .isInstanceOf(ConcurrenciaConflictoException.class);

            FalActa post = repo.buscarPorId(persistida.getId()).orElseThrow();
            assertThat(post.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(post.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("un id inexistente no se degrada a conflicto de concurrencia")
        void no_encontrado_no_se_degrada_a_conflicto() {
            FalActa fantasma = nuevaActaSinId().conId(4_242L);

            assertThatThrownBy(() -> repo.guardar(fantasma))
                    .isNotInstanceOf(ConcurrenciaConflictoException.class);
        }
    }

    // =====================================================================
    // 10.4 Concurrencia OCC
    // =====================================================================

    @Nested
    @DisplayName("InMemoryActaRepository - concurrencia OCC")
    class ConcurrenciaOcc {

        @Test
        @DisplayName("cuatro hilos con la misma version: un unico ganador y version final +1")
        void un_unico_ganador() throws Exception {
            FalActa persistida = repo.guardar(nuevaActaSinId());
            int hilos = 4;

            CyclicBarrier barrera = new CyclicBarrier(hilos);
            CountDownLatch fin = new CountDownLatch(hilos);
            AtomicInteger exitos = new AtomicInteger();
            AtomicInteger conflictos = new AtomicInteger();
            List<Throwable> inesperadas = new ArrayList<>();
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            try {
                for (int i = 0; i < hilos; i++) {
                    FalActa candidato = repo.buscarPorId(persistida.getId()).orElseThrow();
                    exec.submit(() -> {
                        try {
                            barrera.await(5, TimeUnit.SECONDS);
                            repo.guardar(candidato);
                            exitos.incrementAndGet();
                        } catch (ConcurrenciaConflictoException e) {
                            conflictos.incrementAndGet();
                        } catch (Throwable t) {
                            synchronized (inesperadas) { inesperadas.add(t); }
                        } finally {
                            fin.countDown();
                        }
                    });
                }
                assertThat(fin.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                exec.shutdownNow();
            }

            assertThat(inesperadas).isEmpty();
            assertThat(exitos.get()).isEqualTo(1);
            assertThat(conflictos.get()).isEqualTo(hilos - 1);
            assertThat(repo.buscarPorId(persistida.getId()).orElseThrow().getVersionRow())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("altas concurrentes reciben identificadores unicos")
        void altas_concurrentes_ids_unicos() throws Exception {
            int hilos = 8;
            CyclicBarrier barrera = new CyclicBarrier(hilos);
            CountDownLatch fin = new CountDownLatch(hilos);
            List<Long> ids = java.util.Collections.synchronizedList(new ArrayList<>());
            ExecutorService exec = Executors.newFixedThreadPool(hilos);

            try {
                for (int i = 0; i < hilos; i++) {
                    exec.submit(() -> {
                        try {
                            barrera.await(5, TimeUnit.SECONDS);
                            ids.add(repo.guardar(nuevaActaSinId()).getId());
                        } catch (Exception ignored) {
                            // el conteo final detecta cualquier alta perdida
                        } finally {
                            fin.countDown();
                        }
                    });
                }
                assertThat(fin.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                exec.shutdownNow();
            }

            assertThat(ids).hasSize(hilos).doesNotHaveDuplicates();
            assertThat(repo.size()).isEqualTo(hilos);
        }
    }

    // =====================================================================
    // 9. Guardrail: no reintroducir la identidad preasignada del acta
    // =====================================================================

    @Nested
    @DisplayName("Guardrail: ActaRepository no preasigna identificadores")
    class GuardrailIdentidadActa {

        private static final Path MAIN = Path.of("src/main/java/ar/gob/malvinas/faltas/core");
        private static final Path TEST = Path.of("src/test/java/ar/gob/malvinas/faltas/core");

        /**
         * Devuelve el codigo sin comentarios: el guardrail vigila la declaracion real,
         * no la prosa. El Javadoc de estos archivos menciona nextId justamente para
         * explicar por que ya no se usa.
         */
        private String codigoSinComentarios(Path archivo) throws IOException {
            return Files.readString(archivo)
                    .replaceAll("(?s)/\\*.*?\\*/", "")
                    .replaceAll("//[^\n]*", "");
        }

        private List<Path> javaBajo(Path raiz) throws IOException {
            try (Stream<Path> s = Files.walk(raiz)) {
                return s.filter(p -> p.toString().endsWith(".java")).toList();
            }
        }

        @Test
        @DisplayName("ActaRepository no declara nextId ni lee la PK manualmente")
        void puerto_sin_generador() throws IOException {
            String codigo = codigoSinComentarios(MAIN.resolve("repository/ActaRepository.java"));
            assertThat(codigo)
                    .doesNotContain("nextId")
                    .doesNotContain("MAX(id)")
                    .doesNotContain("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("InMemoryActaRepository no expone un generador de identificadores")
        void adaptador_no_expone_generador() throws IOException {
            String codigo = codigoSinComentarios(MAIN.resolve("repository/memory/InMemoryActaRepository.java"));
            assertThat(codigo)
                    .doesNotContain("nextId")
                    .doesNotContain("AUTO_INCREMENT");
        }

        @Test
        @DisplayName("ningun archivo de produccion llama nextId() sobre el puerto del acta")
        void produccion_sin_callsites() throws IOException {
            assertThat(archivosConNextIdDeActa(MAIN)).isEmpty();
        }

        @Test
        @DisplayName("ningun test llama nextId() sobre el puerto del acta")
        void tests_sin_callsites() throws IOException {
            assertThat(archivosConNextIdDeActa(TEST)).isEmpty();
        }

        private List<String> archivosConNextIdDeActa(Path raiz) throws IOException {
            // El guardrail vigila especificamente la identidad de FalActa (fal_acta.id,
            // AUTO_INCREMENT real): variables como "actaRepository"/"actaRepo". No debe
            // atrapar los puertos de los seis satelites del alta integral (Wave 4 PARTE-3A),
            // que legitimamente usan nextId() para sus propias PK client-side (mismo patron que
            // articuloInfringidoRepository/evidenciaRepository/personaDomicilioRepository) y
            // cuyo nombre de interfaz tambien empieza con "Acta" (ActaVehiculoRepository,
            // ActaTransitoAlcoholemiaRepository, ActaMedidaPreventivaRepository, etc.): el "\\b"
            // final exige que "Repo"/"Repository" siga inmediatamente a "acta", sin palabras
            // intermedias, para no confundir el puerto del acta con sus satelites.
            Pattern callsite = Pattern.compile("(?i)\\bacta(?:Repo|Repository)\\b\\.nextId\\(\\)");
            List<String> encontrados = new ArrayList<>();
            for (Path archivo : javaBajo(raiz)) {
                Matcher m = callsite.matcher(codigoSinComentarios(archivo));
                if (m.find()) {
                    encontrados.add(archivo.getFileName().toString());
                }
            }
            return encontrados;
        }

        @Test
        @DisplayName("produccion crea el acta demo sin id preasignado")
        void demo_sin_id_preasignado() throws IOException {
            String codigo = codigoSinComentarios(
                    MAIN.resolve("application/service/DocumentoGraphDemoService.java"));
            assertThat(codigo).contains("GraphDemoActaFactory.crearActaDemo()");
        }

        @Test
        @DisplayName("un id presente pero inexistente no se inserta")
        void no_inserta_id_del_llamador() {
            FalActa fantasma = nuevaActaSinId().conId(31_337L);

            assertThatThrownBy(() -> repo.guardar(fantasma))
                    .isInstanceOf(ActaNoEncontradaException.class);
            assertThat(repo.listarTodas()).isEmpty();
        }
    }
}
