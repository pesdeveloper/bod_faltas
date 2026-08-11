package ar.gob.malvinas.faltas.core.support;

import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.FotografiaEvidenciaService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionCalculador;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.infrastructure.imagen.TwelveMonkeysImagenEvidenciaProcesador;
import ar.gob.malvinas.faltas.core.infrastructure.storage.FilesystemEvidenciaAlmacenamientoAdapter;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSustanciasAlimenticiasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoAlcoholemiaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaVehiculoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;

/**
 * Construye {@link ActaService} para los tests que solo necesitan un servicio funcional.
 *
 * <p>Desde WAVE 1A2-B1.5B2 el alta canonica exige dos colaboradores mas: el productor del
 * token QR y el puerto de {@code fal_observacion}. Ambos son obligatorios, porque el acta no
 * puede persistirse sin {@code codigo_qr} ni quedar sin su observacion inicial. Esta fabrica
 * los provee con implementaciones InMemory y claves TEST_ONLY, de modo que los tests que no
 * verifican QR ni observaciones no tengan que conocerlos.
 *
 * <p>Desde Wave 4 PARTE-3A ({@code DECISION-ACTA-DEPS-01}), los cuatro colaboradores del alta
 * integral (domicilios, artículos infringidos, valorización, numeración) y los seis satélites
 * completos (vehículo, tránsito, alcoholemia, contravención, sustancias alimenticias, medida
 * preventiva) son parámetros obligatorios del constructor, no campos opcionales. Esta fabrica
 * los provee con implementaciones InMemory mínimas y deterministas para que los tests que no
 * ejercitan esos bloques no tengan que conocerlos ni instanciarlos uno por uno.
 *
 * <p>Los tests que si verifican QR u observaciones construyen {@link ActaService} de forma
 * explicita para poder inspeccionar esos colaboradores.
 */
public final class ActaServiceTestSupport {

    public static ActaService actaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            FaltasClock faltasClock) {
        return actaService(actaRepository, eventoRepository, snapshotRepository,
                snapshotRecalculador, evidenciaRepository, faltasClock,
                new InMemoryObservacionRepository());
    }

    public static ActaService actaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            FaltasClock faltasClock,
            ObservacionRepository observacionRepository) {
        return new ActaService(actaRepository, eventoRepository, snapshotRepository,
                snapshotRecalculador, evidenciaRepository, faltasClock,
                QrTestSupport.qrActaService(actaRepository, eventoRepository, snapshotRepository,
                        snapshotRecalculador, faltasClock),
                observacionRepository,
                new InMemoryPersonaDomicilioRepository(),
                new InMemoryActaArticuloInfringidoRepository(),
                valorizacionServiceMinima(actaRepository, faltasClock),
                talonarioServiceMinimo(faltasClock),
                new InMemoryActaVehiculoRepository(),
                new InMemoryActaTransitoRepository(),
                new InMemoryActaTransitoAlcoholemiaRepository(),
                new InMemoryActaContravencionRepository(),
                new InMemoryActaSustanciasAlimenticiasRepository(),
                new InMemoryActaMedidaPreventivaRepository(),
                new InMemoryMedidaPreventivaRepository(),
                fotografiaEvidenciaServiceMinima(actaRepository, evidenciaRepository, faltasClock));
    }

    public static ActaService actaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            PersonaRepository personaRepository,
            FaltasClock faltasClock) {
        return actaService(actaRepository, eventoRepository, snapshotRepository,
                snapshotRecalculador, evidenciaRepository, personaRepository, faltasClock,
                new InMemoryObservacionRepository());
    }

    public static ActaService actaService(
            ActaRepository actaRepository,
            ActaEventoRepository eventoRepository,
            ActaSnapshotRepository snapshotRepository,
            SnapshotRecalculador snapshotRecalculador,
            ActaEvidenciaRepository evidenciaRepository,
            PersonaRepository personaRepository,
            FaltasClock faltasClock,
            ObservacionRepository observacionRepository) {
        return new ActaService(actaRepository, eventoRepository, snapshotRepository,
                snapshotRecalculador, evidenciaRepository, personaRepository, faltasClock,
                QrTestSupport.qrActaService(actaRepository, eventoRepository, snapshotRepository,
                        snapshotRecalculador, faltasClock),
                observacionRepository,
                new InMemoryPersonaDomicilioRepository(),
                new InMemoryActaArticuloInfringidoRepository(),
                valorizacionServiceMinima(actaRepository, faltasClock),
                talonarioServiceMinimo(faltasClock),
                new InMemoryActaVehiculoRepository(),
                new InMemoryActaTransitoRepository(),
                new InMemoryActaTransitoAlcoholemiaRepository(),
                new InMemoryActaContravencionRepository(),
                new InMemoryActaSustanciasAlimenticiasRepository(),
                new InMemoryActaMedidaPreventivaRepository(),
                new InMemoryMedidaPreventivaRepository(),
                fotografiaEvidenciaServiceMinima(actaRepository, evidenciaRepository, faltasClock));
    }

    /**
     * {@link FotografiaEvidenciaService} minimo (Wave 4 Parte 3D) con el pipeline de imagen
     * productivo real (sin fakes: valida formato/dimensiones/EXIF de verdad) y un adapter
     * filesystem apuntando a un directorio temporal de la JVM, aislado por invocacion. Los
     * tests que no informan {@code fotografias} en el comando nunca invocan el pipeline ni el
     * filesystem: alcanza con que el bean exista (regimen DECISION-ACTA-DEPS-01).
     */
    public static FotografiaEvidenciaService fotografiaEvidenciaServiceMinima(
            ActaRepository actaRepository, ActaEvidenciaRepository evidenciaRepository, FaltasClock faltasClock) {
        try {
            java.nio.file.Path raiz = java.nio.file.Files.createTempDirectory("faltas-test-evidencias-");
            return new FotografiaEvidenciaService(
                    actaRepository,
                    evidenciaRepository,
                    new TwelveMonkeysImagenEvidenciaProcesador(5_242_880L, 40_000_000L, 1920, 0.85f),
                    new FilesystemEvidenciaAlmacenamientoAdapter(
                            raiz.toString(), raiz.toString(), new org.springframework.mock.env.MockEnvironment()),
                    faltasClock,
                    10);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio temporal de evidencias para tests", e);
        }
    }

    /**
     * {@link ValorizacionService} minimo con repositorios InMemory propios, aislados por
     * invocacion: suficiente para que {@code ActaService.labrar} pueda calcular la valorizacion
     * de los articulos informados sin requerir seed externo (no se ejercita si el comando no
     * informa articulos infringidos).
     */
    public static ValorizacionService valorizacionServiceMinima(
            ActaRepository actaRepository, FaltasClock faltasClock) {
        return new ValorizacionService(
                new InMemoryActaValorizacionRepository(),
                new InMemoryActaValorizacionItemRepository(),
                new InMemoryActaArticuloInfringidoRepository(),
                new InMemoryNormativaRepository(),
                new InMemoryTarifarioUnidadFaltasRepository(),
                new InMemoryValorUnidadFaltasRepository(),
                actaRepository,
                new ValorizacionCalculador(),
                faltasClock);
    }

    /**
     * {@link TalonarioService} minimo con repositorios InMemory propios: preseeda un unico
     * talonario ACTA ELECTRONICO activo, con ambito GLOBAL (aplica a cualquier dependencia),
     * para que {@code ActaService.labrar} resuelva exactamente un talonario compatible
     * ({@code DECISION-NUM-UNICO-ACTIVO-01}, Wave 4 Parte 3B §9.2: "1 activo -&gt; usarlo") en
     * lugar del gap de configuracion "0 activos" que Parte 3A tolera solo como caso residual.
     * Cada invocacion construye su propio {@link ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository}
     * aislado, así que este seed nunca colisiona entre tests.
     *
     * <p>Preseeda dependencia id=1 (activa, sin tipoActa restringido, vigente 2000-01-01 sin
     * fin) e inspector id=1 (activo, habilitado exactamente para esa version de dependencia,
     * misma vigencia), porque {@code LabrarActaCommand} usa idDependencia=1/idInspector=1 como
     * valor por defecto cuando el comando no los informa (patron usado por decenas de tests
     * unitarios preexistentes que no ejercitan dependencia/inspector). Wave 4 PARTE-3A §10
     * exige validar dependencia e inspector antes de numerar; este seed universal evita que esa
     * validacion, nueva en PARTE-3A, rompa tests preexistentes que no son sobre numeracion.
     */
    public static TalonarioService talonarioServiceMinimo(FaltasClock faltasClock) {
        ar.gob.malvinas.faltas.core.repository.DependenciaRepository dependenciaRepository =
                new InMemoryDependenciaRepository();
        ar.gob.malvinas.faltas.core.repository.InspectorRepository inspectorRepository =
                new InMemoryInspectorRepository();
        java.time.LocalDate vigenciaAmplia = java.time.LocalDate.of(2000, 1, 1);
        dependenciaRepository.guardar(new ar.gob.malvinas.faltas.core.domain.model.FalDependencia(
                1L, "DEPENDENCIA_TEST_DEFAULT", faltasClock.now(), "TEST_SUPPORT"));
        dependenciaRepository.guardarVersion(new ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion(
                1L, 1, "DEPENDENCIA_TEST_DEFAULT", null, null, null, vigenciaAmplia));
        inspectorRepository.guardar(new ar.gob.malvinas.faltas.core.domain.model.FalInspector(
                1L, "00000000-0000-0000-0000-000000000001", 1, "INSPECTOR_TEST_DEFAULT",
                faltasClock.now(), "TEST_SUPPORT"));
        inspectorRepository.guardarVersion(new ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion(
                1L, 1, 1, "INSPECTOR_TEST_DEFAULT", 1L, 1, vigenciaAmplia));
        TalonarioService talonarioService = new TalonarioService(
                new InMemoryTalonarioRepository(),
                dependenciaRepository,
                inspectorRepository,
                faltasClock);
        seedTalonarioElectronicoGlobalActivo(talonarioService, faltasClock);
        return talonarioService;
    }

    /**
     * Unico talonario ACTA ELECTRONICO activo con ambito GLOBAL: satisface
     * {@code DECISION-NUM-UNICO-ACTIVO-01} para cualquier dependencia sin colisionar (cada
     * llamada usa un {@code TalonarioService}/repositorio InMemory nuevo y aislado).
     */
    private static void seedTalonarioElectronicoGlobalActivo(
            TalonarioService talonarioService, FaltasClock faltasClock) {
        java.time.LocalDate vigenciaAmplia = java.time.LocalDate.of(2000, 1, 1);
        var politica = talonarioService.crearPolitica(
                new ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand(
                        "TSDEF", "Politica ACTA electronica default de tests",
                        ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                        false, false, null, false, null, false, (short) 6,
                        "{NRO}", true, vigenciaAmplia, null, "TEST_SUPPORT"));
        var talonario = talonarioService.crearTalonario(
                new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand(
                        politica.getId(), "TSDEFTAL", "Talonario electronico default de tests",
                        ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario.ELECTRONICO,
                        ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                        null, null, 1, null,
                        "SEQ_TEST_SUPPORT_DEFAULT", true, false, null, null, "TEST_SUPPORT"));
        talonarioService.crearAmbito(
                new ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand(
                        talonario.getId(), ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion.ACTA,
                        null, null, null, null,
                        ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario.GLOBAL,
                        (short) 1, vigenciaAmplia, null, true, "TEST_SUPPORT"));
    }

    private ActaServiceTestSupport() {}
}
