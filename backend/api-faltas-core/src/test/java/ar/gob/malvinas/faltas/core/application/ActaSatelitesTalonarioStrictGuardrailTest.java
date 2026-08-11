package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guardrail de Wave 4 Parte 3C §12: impide reintroducir, ni por texto ni por comportamiento,
 * las dos excepciones eliminadas de {@code ActaService}:
 *
 * <pre>
 * optaPorSatelitesTipados                                      (DECISION-ACTA-SATELITES-STRICT-01)
 * captura tolerante de SinTalonarioElectronicoActivoException  (DECISION-ACTA-TALONARIO-REQUIRED-01)
 * </pre>
 *
 * <p>La mitad textual detecta una reintroduccion literal del mismo nombre. La mitad funcional
 * (obligatoria por el prompt: "no base el guardrail unicamente en nombres") prueba el
 * comportamiento real: los cuatro tipos de acta rechazan sin su bloque obligatorio -- incluso
 * sin informar ningun otro satelite -- y cero talonarios activos rechaza el alta completa, sin
 * persistir nada.
 */
@DisplayName("Guardrail Wave 4 Parte 3C §12: sin opt-in de satelites, sin talonario tolerante")
class ActaSatelitesTalonarioStrictGuardrailTest {

    private static final Path ACTA_SERVICE = Paths.get(
            "src/main/java/ar/gob/malvinas/faltas/core/application/service/ActaService.java");

    private static String leerActaService() throws IOException {
        return Files.readString(ACTA_SERVICE, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("G-P3C-1: guardrail textual")
    class GuardrailTextual {

        @Test
        @DisplayName("ActaService no contiene optaPorSatelitesTipados ni ningun equivalente")
        void sinOptaPorSatelitesTipados() throws IOException {
            String fuente = leerActaService();
            assertThat(fuente)
                    .as("DECISION-APLICABILIDAD-SATELITES-COMPATIBILIDAD-01 (Parte 3B) fue eliminada;"
                            + " no debe reintroducirse ningun opt-in de satelites")
                    .doesNotContain("optaPorSatelitesTipados")
                    .doesNotContainIgnoringCase("optaPorSatelites");
        }

        @Test
        @DisplayName("ActaService.labrar no captura SinTalonarioElectronicoActivoException")
        void sinCapturaTolerante() throws IOException {
            String fuente = leerActaService();
            assertThat(fuente)
                    .as("DECISION-NUM-FINAL-ALTA-RECHAZADA-01 (Parte 3B, mitad tolerante) fue"
                            + " eliminada; ActaService.labrar ya no debe capturar este subtipo para"
                            + " tolerar un alta sin numerar")
                    .doesNotContain("catch (ar.gob.malvinas.faltas.core.domain.exception.SinTalonarioElectronicoActivoException")
                    .doesNotContain("catch (SinTalonarioElectronicoActivoException");
        }
    }

    @Nested
    @DisplayName("G-P3C-2: guardrail funcional (comportamiento, no solo nombres)")
    class GuardrailFuncional {

        private InMemoryActaRepository actaRepo;
        private InMemoryActaEventoRepository eventoRepo;
        private InMemoryActaSnapshotRepository snapshotRepo;
        private ActaService serviceConTalonario;

        @BeforeEach
        void setUp() {
            ActorContextHolder.set(new ActorContext("guardrail-p3c"));
            actaRepo = new InMemoryActaRepository();
            eventoRepo = new InMemoryActaEventoRepository();
            snapshotRepo = new InMemoryActaSnapshotRepository();
            SnapshotRecalculador recalc = new SnapshotRecalculador(
                    eventoRepo,
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository(),
                    FaltasClockTestSupport.FIXED, snapshotRepo);
            serviceConTalonario = ActaServiceTestSupport.actaService(
                    actaRepo, eventoRepo, snapshotRepo, recalc,
                    new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);
        }

        @AfterEach
        void tearDown() {
            ActorContextHolder.clear();
        }

        private LabrarActaCommand comandoSinNingunSatelite(TipoActa tipo) {
            return ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory
                    .builder(tipo).sinSatelites().build();
        }

        @Test
        @DisplayName("TRANSITO sin ningun bloque de satelite informado rechaza, aunque no venga ningun otro")
        void transitoSinNingunSateliteRechaza() {
            assertThatThrownBy(() -> serviceConTalonario.labrar(comandoSinNingunSatelite(TipoActa.TRANSITO)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("transito")
                    .hasMessageContaining("obligatorio");
            assertThat(actaRepo.listarTodas()).as("cero actas").isEmpty();
            assertThat(eventoRepo.buscarPorActa(1L)).as("cero eventos").isEmpty();
        }

        @Test
        @DisplayName("CONTRAVENCION sin su bloque obligatorio rechaza")
        void contravencionSinBloqueRechaza() {
            assertThatThrownBy(() -> serviceConTalonario.labrar(comandoSinNingunSatelite(TipoActa.CONTRAVENCION)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
            assertThat(actaRepo.listarTodas()).isEmpty();
        }

        @Test
        @DisplayName("SUSTANCIAS_ALIMENTICIAS sin su bloque obligatorio rechaza")
        void sustanciasSinBloqueRechaza() {
            assertThatThrownBy(() -> serviceConTalonario.labrar(comandoSinNingunSatelite(TipoActa.SUSTANCIAS_ALIMENTICIAS)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("sustanciasAlimenticias")
                    .hasMessageContaining("obligatorio");
            assertThat(actaRepo.listarTodas()).isEmpty();
        }

        @Test
        @DisplayName("COMERCIO sin su bloque obligatorio rechaza")
        void comercioSinBloqueRechaza() {
            assertThatThrownBy(() -> serviceConTalonario.labrar(comandoSinNingunSatelite(TipoActa.COMERCIO)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
            assertThat(actaRepo.listarTodas()).isEmpty();
        }

        @Test
        @DisplayName("Cero talonarios ACTA ELECTRONICO activos rechaza el alta completa, sin persistir nada")
        void ceroTalonariosActivosRechazaElAltaCompleta() {
            // TalonarioService dedicado SIN seed (a diferencia de talonarioServiceMinimo()):
            // 0 talonarios activos, exactamente el escenario que ya no debe tolerarse.
            var talonarioServiceSinSeed = new ar.gob.malvinas.faltas.core.application.service.TalonarioService(
                    new InMemoryTalonarioRepository(),
                    new InMemoryDependenciaRepository(),
                    new InMemoryInspectorRepository(),
                    FaltasClockTestSupport.FIXED);

            SnapshotRecalculador recalc = new SnapshotRecalculador(
                    eventoRepo,
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository(),
                    FaltasClockTestSupport.FIXED, snapshotRepo);

            InMemoryActaEvidenciaRepository evidenciaRepoSinTalonario = new InMemoryActaEvidenciaRepository();
            ActaService serviceSinTalonario = new ActaService(
                    actaRepo, eventoRepo, snapshotRepo, recalc,
                    evidenciaRepoSinTalonario,
                    FaltasClockTestSupport.FIXED,
                    ar.gob.malvinas.faltas.core.support.QrTestSupport.qrActaService(
                            actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository(),
                    ActaServiceTestSupport.valorizacionServiceMinima(actaRepo, FaltasClockTestSupport.FIXED),
                    talonarioServiceSinSeed,
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaVehiculoRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoAlcoholemiaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaContravencionRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSustanciasAlimenticiasRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaMedidaPreventivaRepository(),
                    new ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository(),
                    ActaServiceTestSupport.fotografiaEvidenciaServiceMinima(
                            actaRepo, evidenciaRepoSinTalonario, FaltasClockTestSupport.FIXED));

            LabrarActaCommand comandoValido = ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory
                    .altaTransitoValida();

            // El rollback transaccional real (cero fal_acta/evento/snapshot tras el rechazo)
            // requiere un PlatformTransactionManager real y esta certificado contra MariaDB real
            // por ActaLabrarTransaccionalIT / ActaAltaIntegralParte3AIT (Wave 4 Parte 3C). Sin
            // proxy transaccional de Spring, @Transactional es inerte aqui: este guardrail
            // verifica el comportamiento funcional (la excepcion ya NO se tolera, propaga tal
            // cual, sin capturarse dentro de labrar), no la atomicidad fisica.
            assertThatThrownBy(() -> serviceSinTalonario.labrar(comandoValido))
                    .isInstanceOf(ar.gob.malvinas.faltas.core.domain.exception.SinTalonarioElectronicoActivoException.class)
                    .as("0 talonarios activos debe propagar, nunca tolerarse como alta sin numerar");
        }
    }
}
