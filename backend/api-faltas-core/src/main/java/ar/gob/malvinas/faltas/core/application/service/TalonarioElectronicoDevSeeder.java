package ar.gob.malvinas.faltas.core.application.service;

import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.SinTalonarioElectronicoActivoException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.infrastructure.time.FaltasClock;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder real, idempotente y limitado a perfiles {@code local}/{@code dev} (Wave 4 Parte 3C
 * §11). Asegura -- sin duplicar -- una dependencia de desarrollo, su version vigente, un
 * inspector de desarrollo con su version/habilitacion, una politica de numeracion ACTA, y
 * exactamente un talonario ACTA ELECTRONICO activo con ambito aplicable a esa dependencia.
 *
 * <p>Existe porque, desde {@code DECISION-ACTA-TALONARIO-REQUIRED-01} (Wave 4 Parte 3C §7), el
 * alta integral rechaza completa sin exactamente un talonario ACTA ELECTRONICO activo: un
 * ambiente local/dev que arranca con {@code sb_faltas_db} vacia no puede labrar ninguna acta de
 * demostracion sin este seed minimo.
 *
 * <h2>Guardas de seguridad</h2>
 *
 * <p>No usa ningun repositorio InMemory: depende exclusivamente de los puertos de dominio
 * ({@link DependenciaRepository}, {@link InspectorRepository}, {@link TalonarioService}), cuya
 * implementacion activa la decide el perfil de Spring. {@link #asegurar()} verifica en tiempo de
 * ejecucion que {@code app.persistence.mode=jdbc} (la misma property que ya distingue el perfil
 * JDBC del perfil por defecto en memoria, sin introducir una segunda estrategia paralela) y
 * rechaza ejecutar si no es asi: nunca crea datos de desarrollo en un repositorio InMemory
 * efimero, y nunca corre por accidente en un contexto de test que ya tiene su propio soporte
 * ({@code ActaServiceTestSupport}/{@code TalonarioElectronicoITSeedSupport}).
 *
 * <p>No almacena secretos: la identidad tecnica del inspector de desarrollo es un UUID fijo,
 * publico y sin significado sensible, documentado en {@link #ID_USER_INSPECTOR_DEV}.
 *
 * <p>Idempotente: dos ejecuciones sucesivas dejan el mismo estado logico. Si detecta una
 * configuracion de talonario ambigua preexistente (2+ activos, o el talonario existente
 * agotado/bloqueado), no elige ni borra nada: falla con {@link PrecondicionVioladaException} y
 * un mensaje que identifica exactamente la inconsistencia.
 *
 * <h2>Endurecimiento estructural (Wave 4 Parte 3E §15)</h2>
 *
 * <p>Hasta Parte 3E, el unico gate era la property {@code faltas.seed.local-dev.enabled}
 * (deshabilitada por defecto) mas la verificacion en tiempo de ejecucion de
 * {@code app.persistence.mode=jdbc} de {@link #asegurar()}: el bean existia en cualquier
 * perfil de Spring, incluido el productivo. {@code @Profile({"local", "dev"})} agrega un
 * segundo gate estructural, independiente de cualquier property: bajo cualquier otro perfil
 * (incluido {@code jdbc} sin {@code local}/{@code dev}), este bean no se registra en el
 * contexto y no puede habilitarse solo con la property.
 */
@Service
@Profile({"local", "dev"})
public class TalonarioElectronicoDevSeeder {

    private static final Logger log = LoggerFactory.getLogger(TalonarioElectronicoDevSeeder.class);

    /** Fijo, publico, sin significado sensible: identidad tecnica del inspector de desarrollo. */
    public static final String ID_USER_INSPECTOR_DEV = "00000000-0000-0000-0000-0000000dev01";

    /** {@code fal_dependencia.cod_dep} es {@code VARCHAR(8)}: maximo 8 caracteres. */
    public static final String COD_DEP_DEV = "DEVFAL01";
    public static final String NOM_DEP_DEV = "Dependencia de desarrollo (seeder local/dev)";
    /** {@code fal_inspector.nom_insp} es {@code VARCHAR(36)}: maximo 36 caracteres. */
    public static final String NOM_INSP_DEV = "Inspector desarrollo (seeder)";
    public static final String COD_POLITICA_DEV = "DEVACTA1";
    public static final String COD_TALONARIO_DEV = "DEVTALAC";
    public static final String NOMBRE_SECUENCIA_DEV = "SEQ_DEV_TALONARIO_ACTA_ELECTRONICO";
    private static final LocalDate VIGENCIA_DESDE = LocalDate.of(2000, 1, 1);

    /** Estado de cada paso del seed, para el log y la respuesta verificable (prompt §11.1). */
    public enum Estado { CREATED, ALREADY_PRESENT }

    public record PasoSeed(String entidad, Estado estado, Long id) {}

    public record ResultadoSeederLocalDev(
            List<PasoSeed> pasos, Long idDependencia, Long idInspector, Long idTalonario) {}

    private final DependenciaRepository dependenciaRepository;
    private final InspectorRepository inspectorRepository;
    private final TalonarioService talonarioService;
    private final FaltasClock faltasClock;
    private final String persistenceMode;

    public TalonarioElectronicoDevSeeder(
            DependenciaRepository dependenciaRepository,
            InspectorRepository inspectorRepository,
            TalonarioService talonarioService,
            FaltasClock faltasClock,
            @Value("${app.persistence.mode:memory}") String persistenceMode) {
        this.dependenciaRepository = dependenciaRepository;
        this.inspectorRepository = inspectorRepository;
        this.talonarioService = talonarioService;
        this.faltasClock = faltasClock;
        this.persistenceMode = persistenceMode;
    }

    @Transactional
    public ResultadoSeederLocalDev asegurar() {
        if (!"jdbc".equals(persistenceMode)) {
            throw new IllegalStateException(
                    "TalonarioElectronicoDevSeeder requiere app.persistence.mode=jdbc (perfil jdbc"
                            + " con MariaDB real). Modo actual: '" + persistenceMode + "'. No se"
                            + " ejecuta contra repositorios InMemory.");
        }

        List<PasoSeed> pasos = new ArrayList<>();
        LocalDateTime ahora = faltasClock.now();
        LocalDate hoy = ahora.toLocalDate();

        Long idDependencia = asegurarDependencia(pasos, ahora);
        Long idInspector = asegurarInspector(pasos, ahora, idDependencia);
        Long idTalonario = asegurarTalonarioElectronico(pasos, idDependencia, hoy);

        for (PasoSeed paso : pasos) {
            log.info("SEED_LOCAL_DEV {} entidad={} id={}", paso.estado(), paso.entidad(), paso.id());
        }
        return new ResultadoSeederLocalDev(pasos, idDependencia, idInspector, idTalonario);
    }

    private Long asegurarDependencia(List<PasoSeed> pasos, LocalDateTime ahora) {
        FalDependencia existente = dependenciaRepository.findByCodDep(COD_DEP_DEV).orElse(null);
        if (existente != null) {
            if (dependenciaRepository.findVersionVigente(existente.getIdDep(), VIGENCIA_DESDE).isEmpty()) {
                throw new PrecondicionVioladaException(
                        "INVALID_CONFIGURATION: la dependencia de desarrollo '" + COD_DEP_DEV
                                + "' (id=" + existente.getIdDep() + ") existe pero no tiene una"
                                + " version vigente. El seeder no la corrige automaticamente.");
            }
            pasos.add(new PasoSeed("fal_dependencia", Estado.ALREADY_PRESENT, existente.getIdDep()));
            return existente.getIdDep();
        }
        FalDependencia nueva = new FalDependencia(null, NOM_DEP_DEV, ahora, "SEEDER_LOCAL_DEV");
        nueva.setCodDep(COD_DEP_DEV);
        FalDependencia guardada = dependenciaRepository.guardar(nueva);
        dependenciaRepository.guardarVersion(new FalDependenciaVersion(
                guardada.getIdDep(), 1, NOM_DEP_DEV, null, null,
                ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO, VIGENCIA_DESDE));
        pasos.add(new PasoSeed("fal_dependencia", Estado.CREATED, guardada.getIdDep()));
        return guardada.getIdDep();
    }

    private Long asegurarInspector(List<PasoSeed> pasos, LocalDateTime ahora, Long idDependencia) {
        if (inspectorRepository.existsByIdUser(ID_USER_INSPECTOR_DEV)) {
            FalInspector existente = inspectorRepository.findAllActivos().stream()
                    .filter(i -> ID_USER_INSPECTOR_DEV.equals(i.getIdUser()))
                    .findFirst()
                    .orElseThrow(() -> new PrecondicionVioladaException(
                            "INVALID_CONFIGURATION: existe un inspector con idUser=" + ID_USER_INSPECTOR_DEV
                                    + " pero no esta activo. El seeder no lo reactiva automaticamente."));
            if (inspectorRepository.findVersionVigente(existente.getIdInsp(), VIGENCIA_DESDE).isEmpty()) {
                throw new PrecondicionVioladaException(
                        "INVALID_CONFIGURATION: el inspector de desarrollo (id=" + existente.getIdInsp()
                                + ") existe pero no tiene una version/habilitacion vigente.");
            }
            pasos.add(new PasoSeed("fal_inspector", Estado.ALREADY_PRESENT, existente.getIdInsp()));
            return existente.getIdInsp();
        }
        FalInspector nuevo = new FalInspector(null, ID_USER_INSPECTOR_DEV, 1, NOM_INSP_DEV, ahora, "SEEDER_LOCAL_DEV");
        FalInspector guardado = inspectorRepository.guardar(nuevo);
        inspectorRepository.guardarVersion(new FalInspectorVersion(
                guardado.getIdInsp(), 1, 1, NOM_INSP_DEV, idDependencia, 1, VIGENCIA_DESDE));
        pasos.add(new PasoSeed("fal_inspector", Estado.CREATED, guardado.getIdInsp()));
        return guardado.getIdInsp();
    }

    private Long asegurarTalonarioElectronico(List<PasoSeed> pasos, Long idDependencia, LocalDate hoy) {
        try {
            TalonarioService.TalonarioResuelto resuelto =
                    talonarioService.resolverTalonarioActaElectronico(idDependencia, null, hoy);
            NumTalonario talonario = resuelto.talonario();
            pasos.add(new PasoSeed("num_talonario(ELECTRONICO,ACTA)", Estado.ALREADY_PRESENT, talonario.getId()));
            return talonario.getId();
        } catch (SinTalonarioElectronicoActivoException ningunoActivo) {
            var politica = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    COD_POLITICA_DEV, "Politica ACTA electronica de desarrollo", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, (short) 6,
                    "{NRO}", true, VIGENCIA_DESDE, null, "SEEDER_LOCAL_DEV"));
            var talonario = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    politica.getId(), COD_TALONARIO_DEV, "Talonario electronico de desarrollo",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, NOMBRE_SECUENCIA_DEV, true, false, null, null, "SEEDER_LOCAL_DEV"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talonario.getId(), ClaseNumeracion.ACTA, null, null,
                    idDependencia, (short) 1, AlcanceTalonario.DEPENDENCIA,
                    (short) 1, VIGENCIA_DESDE, null, true, "SEEDER_LOCAL_DEV"));
            pasos.add(new PasoSeed("num_talonario(ELECTRONICO,ACTA)", Estado.CREATED, talonario.getId()));
            return talonario.getId();
        } catch (PrecondicionVioladaException inconsistente) {
            throw new PrecondicionVioladaException(
                    "INVALID_CONFIGURATION: configuracion de talonario ACTA ELECTRONICO ambigua para"
                            + " la dependencia de desarrollo (id=" + idDependencia + "). El seeder no"
                            + " elige ni borra nada. Detalle original: " + inconsistente.getMessage());
        }
    }
}
