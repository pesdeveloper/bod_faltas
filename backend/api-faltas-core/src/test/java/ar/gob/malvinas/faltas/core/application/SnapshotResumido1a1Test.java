package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wave 4 PARTE-3A §13: {@code fal_acta_snapshot} es una proyección derivada 1:1 con el acta,
 * resumida, regenerable y nunca fuente de verdad. Este test demuestra el diseño ya vigente
 * (auditado en {@code PART-3A-SNAPSHOT-REPORT.md}): no requiere cambios de DDL ni de dominio,
 * solo evidencia material de que la propiedad se cumple.
 */
@DisplayName("Wave 4 PARTE-3A: snapshot resumido 1:1, nunca fuente de verdad")
class SnapshotResumido1a1Test {

    private static final Path DDL = Path.of("../../database/ddl/create-bod-faltas-domain.sql");
    private static final String ACTOR = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext(ACTOR));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    @Nested
    @DisplayName("Estructura: sin tablas hijas, sin coleccion por articulo")
    class Estructura {

        @Test
        @DisplayName("1. fal_acta_snapshot no tiene tablas hijas en la DDL canonica")
        void sinTablasHijasEnLaDdl() throws Exception {
            String ddl = Files.readString(DDL, StandardCharsets.UTF_8);
            Pattern creaTabla = Pattern.compile("(?im)^CREATE TABLE (fal_acta_snapshot\\S*)");
            Matcher m = creaTabla.matcher(ddl);
            List<String> tablas = new java.util.ArrayList<>();
            while (m.find()) tablas.add(m.group(1));
            assertThat(tablas).as("una sola tabla, sin variantes de detalle/hijas")
                    .containsExactly("fal_acta_snapshot");
        }

        @Test
        @DisplayName("2. fal_acta_snapshot tiene PRIMARY KEY(id_acta): unicidad 1:1 forzada por el motor")
        void primaryKeyEsIdActa() throws Exception {
            String ddl = Files.readString(DDL, StandardCharsets.UTF_8);
            int inicio = ddl.indexOf("CREATE TABLE fal_acta_snapshot");
            int finAproximado = ddl.indexOf("\n\n-- ----------------------------------------------------------------------------", inicio);
            String bloque = ddl.substring(inicio, finAproximado > 0 ? finAproximado : Math.min(ddl.length(), inicio + 4000));
            assertThat(bloque).contains("PRIMARY KEY (id_acta)");
        }

        @Test
        @DisplayName("3. FalActaSnapshot no declara ninguna coleccion (List/Set/Map): no hay detalle por articulo")
        void sinColeccionesEnElModelo() {
            for (Field f : FalActaSnapshot.class.getDeclaredFields()) {
                assertThat(java.util.Collection.class.isAssignableFrom(f.getType()))
                        .as("campo '%s' no debe ser una coleccion", f.getName()).isFalse();
                assertThat(java.util.Map.class.isAssignableFrom(f.getType()))
                        .as("campo '%s' no debe ser un mapa", f.getName()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Comportamiento: una fila por acta, resumida, regenerable")
    class Comportamiento {

        private InMemoryActaRepository actaRepo;
        private InMemoryActaSnapshotRepository snapshotRepo;
        private InMemoryActaEventoRepository eventoRepo;
        private SnapshotRecalculador recalculador;
        private ActaService service;

        @BeforeEach
        void setUpServicio() {
            actaRepo = new InMemoryActaRepository();
            eventoRepo = new InMemoryActaEventoRepository();
            snapshotRepo = new InMemoryActaSnapshotRepository();
            recalculador = new SnapshotRecalculador(
                    eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                    new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                    new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                    FaltasClockTestSupport.FIXED, snapshotRepo);
            service = ActaServiceTestSupport.actaService(
                    actaRepo, eventoRepo, snapshotRepo, recalculador,
                    new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);
        }

        private LabrarActaCommand comando(String idTecnico) {
            return new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, LocalDate.of(2026, 8, 6),
                    "dom", null, null, null, null, null, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, SituacionIdentificacionInfractor.AUSENTE,
                    null, idTecnico, null,
                    // Multiples articulos informados: el snapshot no debe crecer con ellos.
                    List.of(new ArticuloInfringidoItem(1L, 1L), new ArticuloInfringidoItem(1L, 2L),
                            new ArticuloInfringidoItem(1L, 3L)),
                    null, ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(),
                    null, null, null);
        }

        @Test
        @DisplayName("4. Alta con varios articulos informados crea EXACTAMENTE una fila de snapshot")
        void altaConVariosArticulosCreaUnaSolaFilaDeSnapshot() {
            ComandoResultado res = service.labrar(comando(null));

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(res.idActa()).orElseThrow();
            assertThat(snap.getIdActa()).isEqualTo(res.idActa());
            // No hay forma de "listar snapshots por articulo": el puerto solo expone
            // buscarPorActa(idActa) -> Optional (cardinalidad 0..1), nunca una lista.
        }

        @Test
        @DisplayName("5. Reintento idempotente por id_tecnico no crea una segunda fila de snapshot")
        void reintentoIdempotenteNoDuplicaSnapshot() {
            String idTecnico = UUID.randomUUID().toString();
            ComandoResultado r1 = service.labrar(comando(idTecnico));
            ComandoResultado r2 = service.labrar(comando(idTecnico));

            assertThat(r2.idActa()).isEqualTo(r1.idActa());
            assertThat(snapshotRepo.buscarPorActa(r1.idActa())).isPresent();
        }

        @Test
        @DisplayName("6. El snapshot se puede regenerar desde las fuentes autoritativas sin perder la identidad de la fila")
        void seRegeneraDesdeLasFuentesAutoritativas() {
            ComandoResultado res = service.labrar(comando(null));
            FalActaSnapshot antes = snapshotRepo.buscarPorActa(res.idActa()).orElseThrow();

            FalActa acta = actaRepo.buscarPorId(res.idActa()).orElseThrow();
            FalActaSnapshot regenerado = recalculador.recalcularYGuardar(acta, FaltasClockTestSupport.FIXED.now());

            assertThat(regenerado.getIdActa()).isEqualTo(antes.getIdActa());
            assertThat(regenerado.getVersionRow())
                    .as("OCC: la regeneracion actualiza la MISMA fila (version+1), no crea una nueva")
                    .isEqualTo(antes.getVersionRow() + 1);
            assertThat(snapshotRepo.buscarPorActa(res.idActa())).isPresent();
        }

        @Test
        @DisplayName("7. El snapshot no participa de la resolucion de identidad ni numeracion del acta: solo se lee al final")
        void elSnapshotNuncaSeConsultaParaDecidir() throws Exception {
            // Guardrail estructural: ActaService no debe leer el snapshot antes de escribirlo
            // (snapshotRepository.buscarPorActa/obtenerSnapshot no se invoca dentro de labrar()).
            String codigo = Files.readString(
                    Path.of("src/main/java/ar/gob/malvinas/faltas/core/application/service/ActaService.java"),
                    StandardCharsets.UTF_8);
            int inicioLabrar = codigo.indexOf("public ComandoResultado labrar(LabrarActaCommand cmd)");
            int finLabrar = codigo.indexOf("\n    }", inicioLabrar);
            String cuerpoLabrar = codigo.substring(inicioLabrar, finLabrar);
            assertThat(cuerpoLabrar)
                    .as("labrar() nunca lee el snapshot para decidir: solo lo recalcula y guarda al final")
                    .doesNotContain("snapshotRepository.buscarPorActa")
                    .doesNotContain("obtenerSnapshot");
            assertThat(cuerpoLabrar).contains("snapshotRecalculador.recalcularYGuardar");
        }
    }
}
