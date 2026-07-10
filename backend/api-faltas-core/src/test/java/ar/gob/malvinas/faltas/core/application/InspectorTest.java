package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarInspectorCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.InspectorService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.InspectorNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.FalInspectorVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 8A-2: Inspectores in-memory alineados al modelo MariaDB productivo.
 *
 * Verifica:
 * - Creacion de inspectores con version inicial verInsp=1.
 * - Validaciones obligatorias: idUser, legajoInsp (INT > 0), nomInsp, idDep, verDep.
 * - Unicidad de idUser.
 * - Dependencia versionada debe existir y estar activa.
 * - Versionado incrementa verInsp y conserva historial.
 * - Guardrail: inspector no tiene tipoActa propio; ambito via idDep/verDep.
 * - No se creo FalAgente, TipoInspector, TipoAgente ni EstadoInspector.
 */
@DisplayName("Slice 8A-2: Inspectores in-memory")
class InspectorTest {

    private DependenciaRepository dependenciaRepo;
    private DependenciaService dependenciaService;
    private InspectorRepository inspectorRepo;
    private InspectorService inspectorService;

    @BeforeEach
    void setUp() {
        dependenciaRepo = new InMemoryDependenciaRepository();
        dependenciaService = new DependenciaService(dependenciaRepo, FaltasClockTestSupport.FIXED);
        inspectorRepo = new InMemoryInspectorRepository();
        inspectorService = new InspectorService(inspectorRepo, dependenciaRepo, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FalDependencia crearDependenciaTransito() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                null, "Oficina de Transito Central", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalInspector crearInspectorConDep(String idUser, FalDependencia dep) {
        return inspectorService.crear(new CrearInspectorCommand(
                idUser, 1001, "Juan Inspector",
                dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    // =========================================================================
    // 8A2-01: Crear inspector valido con dependencia versionada existente
    // =========================================================================

    @Nested
    @DisplayName("8A2-01: Crear inspector valido con dependencia existente")
    class CrearInspectorValidoTest {

        @Test
        @DisplayName("Genera idInsp, idUser correcto, siActivo=true, verInsp=1")
        void crear_genera_idInsp_y_version_inicial() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-001", dep);

            assertThat(insp.getIdInsp()).isNotNull();
            assertThat(insp.getIdUser()).isEqualTo("USER-001");
            assertThat(insp.getLegajoInsp()).isEqualTo(1001);
            assertThat(insp.getNomInsp()).isEqualTo("Juan Inspector");
            assertThat(insp.isSiActivo()).isTrue();

            List<FalInspectorVersion> versiones = inspectorRepo.findVersionesByInsp(insp.getIdInsp());
            assertThat(versiones).hasSize(1);
            assertThat(versiones.get(0).getVerInsp()).isEqualTo(1);
            assertThat(versiones.get(0).getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(versiones.get(0).getVerDep()).isEqualTo(1);
            assertThat(versiones.get(0).isSiActivo()).isTrue();
        }
    }

    // =========================================================================
    // 8A2-02: Rechazar inspector sin idUser
    // =========================================================================

    @Nested
    @DisplayName("8A2-02: Rechazar inspector sin idUser")
    class SinIdUserTest {

        @Test
        @DisplayName("idUser nulo lanza PrecondicionVioladaException")
        void sin_idUser_nulo_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    null, 1001, "Juan Inspector",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUser");
        }

        @Test
        @DisplayName("idUser en blanco lanza PrecondicionVioladaException")
        void sin_idUser_blanco_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "  ", 1001, "Juan Inspector",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUser");
        }
    }

    // =========================================================================
    // 8A2-03: Rechazar inspector con idUser duplicado
    // =========================================================================

    @Nested
    @DisplayName("8A2-03: Rechazar idUser duplicado")
    class IdUserDuplicadoTest {

        @Test
        @DisplayName("Segundo inspector con mismo idUser lanza PrecondicionVioladaException")
        void idUser_duplicado_falla() {
            FalDependencia dep = crearDependenciaTransito();
            crearInspectorConDep("USER-DUP", dep);

            assertThatThrownBy(() -> crearInspectorConDep("USER-DUP", dep))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("USER-DUP");
        }
    }

    // =========================================================================
    // 8A2-04: Rechazar inspector sin legajo
    // =========================================================================

    @Nested
    @DisplayName("8A2-04: Rechazar inspector sin legajoInsp")
    class SinLegajoTest {

        @Test
        @DisplayName("legajoInsp nulo lanza PrecondicionVioladaException")
        void sin_legajo_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-003", null, "Juan Inspector",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("legajoInsp");
        }

        @Test
        @DisplayName("legajoInsp cero lanza PrecondicionVioladaException")
        void legajoInsp_cero_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-003B", 0, "Juan Inspector",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("legajoInsp");
        }
    }

    // =========================================================================
    // 8A2-05: Rechazar inspector sin nombre
    // =========================================================================

    @Nested
    @DisplayName("8A2-05: Rechazar inspector sin nomInsp")
    class SinNombreTest {

        @Test
        @DisplayName("nomInsp nulo lanza PrecondicionVioladaException")
        void sin_nombre_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-004", 1004, null,
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nomInsp");
        }
    }

    // =========================================================================
    // 8A2-06: Rechazar inspector sin dependencia
    // =========================================================================

    @Nested
    @DisplayName("8A2-06: Rechazar inspector sin idDep o verDep")
    class SinDependenciaTest {

        @Test
        @DisplayName("idDep nulo lanza PrecondicionVioladaException")
        void sin_idDep_falla() {
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-005", 1005, "Inspector Cinco",
                    null, 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idDep");
        }

        @Test
        @DisplayName("verDep nulo lanza PrecondicionVioladaException")
        void sin_verDep_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-005B", 1005, "Inspector CincoB",
                    dep.getIdDep(), null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verDep");
        }
    }

    // =========================================================================
    // 8A2-07: Rechazar inspector con dependencia inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A2-07: Rechazar inspector con dependencia inexistente")
    class DependenciaInexistenteTest {

        @Test
        @DisplayName("idDep inexistente lanza PrecondicionVioladaException")
        void dep_inexistente_falla() {
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-007", 1007, "Inspector Siete",
                    9999L, 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("9999");
        }
    }

    // =========================================================================
    // 8A2-08: Rechazar inspector con version de dependencia inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A2-08: Rechazar inspector con version de dependencia inexistente")
    class VersionDepInexistenteTest {

        @Test
        @DisplayName("verDep inexistente lanza PrecondicionVioladaException")
        void verDep_inexistente_falla() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-008", 1008, "Inspector Ocho",
                    dep.getIdDep(), 99, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 8A2-09: Rechazar inspector con version de dependencia inactiva
    // =========================================================================

    @Nested
    @DisplayName("8A2-09: Rechazar inspector con version de dependencia inactiva")
    class VersionDepInactivaTest {

        @Test
        @DisplayName("Version de dependencia inactiva lanza PrecondicionVioladaException")
        void verDep_inactiva_falla() {
            FalDependencia dep = crearDependenciaTransito();
            List<ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion> versiones =
                    dependenciaRepo.findVersionesByDep(dep.getIdDep());
            versiones.get(0).setSiActiva(false);

            assertThatThrownBy(() -> inspectorService.crear(new CrearInspectorCommand(
                    "USER-009", 1009, "Inspector Nueve",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 8A2-10: Versionar incrementa verInsp
    // =========================================================================

    @Nested
    @DisplayName("8A2-10: Versionar incrementa verInsp")
    class VersionarIncrementaTest {

        @Test
        @DisplayName("Nueva version tiene verInsp = 2 despues de versionar desde verInsp 1")
        void versionar_incrementa_verInsp() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-010", dep);

            FalInspectorVersion v2 = inspectorService.versionar(new VersionarInspectorCommand(
                    insp.getIdInsp(), 1010, "Juan Inspector Actualizado",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(1), "admin"));

            assertThat(v2.getVerInsp()).isEqualTo(2);
        }
    }

    // =========================================================================
    // 8A2-11: Versionar cierra version anterior
    // =========================================================================

    @Nested
    @DisplayName("8A2-11: Versionar cierra version anterior")
    class VersionarCierraAnteriorTest {

        @Test
        @DisplayName("Version anterior queda siActivo=false con fhVigHasta asignada")
        void versionar_cierra_version_anterior() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-011", dep);
            LocalDate fhVigDesdeV2 = FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(1);

            inspectorService.versionar(new VersionarInspectorCommand(
                    insp.getIdInsp(), 1011, "Juan v2",
                    dep.getIdDep(), 1, fhVigDesdeV2, "admin"));

            List<FalInspectorVersion> versiones = inspectorRepo.findVersionesByInsp(insp.getIdInsp());
            assertThat(versiones).hasSize(2);

            FalInspectorVersion v1 = versiones.stream()
                    .filter(v -> v.getVerInsp() == 1).findFirst().orElseThrow();
            FalInspectorVersion v2 = versiones.stream()
                    .filter(v -> v.getVerInsp() == 2).findFirst().orElseThrow();

            assertThat(v1.isSiActivo()).isFalse();
            assertThat(v1.getFhVigHasta()).isEqualTo(fhVigDesdeV2);
            assertThat(v2.isSiActivo()).isTrue();
            assertThat(v2.getFhVigHasta()).isNull();
        }
    }

    // =========================================================================
    // 8A2-12: Versionar congela nueva dependencia idDep/verDep
    // =========================================================================

    @Nested
    @DisplayName("8A2-12: Versionar congela nueva dependencia idDep/verDep")
    class VersionarCongelaDependenciaTest {

        @Test
        @DisplayName("Nueva version congela idDep y verDep de la segunda dependencia")
        void versionar_congela_nueva_dependencia() {
            FalDependencia dep1 = crearDependenciaTransito();
            FalDependencia dep2 = dependenciaService.crear(new CrearDependenciaCommand(
                    null, "Oficina de Comercio", null,
                    TipoActa.COMERCIO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));

            FalInspector insp = crearInspectorConDep("USER-012", dep1);

            FalInspectorVersion v2 = inspectorService.versionar(new VersionarInspectorCommand(
                    insp.getIdInsp(), 1012, "Juan v2",
                    dep2.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(1), "admin"));

            assertThat(v2.getIdDep()).isEqualTo(dep2.getIdDep());
            assertThat(v2.getVerDep()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8A2-13: Obtener inspector por id
    // =========================================================================

    @Nested
    @DisplayName("8A2-13: Obtener inspector por id")
    class ObtenerPorIdTest {

        @Test
        @DisplayName("Inspector creado es obtenible por su idInsp")
        void obtener_por_id_retorna_inspector() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-013", dep);

            FalInspector obtenido = inspectorService.obtener(insp.getIdInsp());
            assertThat(obtenido.getIdInsp()).isEqualTo(insp.getIdInsp());
            assertThat(obtenido.getIdUser()).isEqualTo("USER-013");
        }
    }

    // =========================================================================
    // 8A2-14: Listar inspectores activos
    // =========================================================================

    @Nested
    @DisplayName("8A2-14: Listar inspectores activos")
    class ListarActivosTest {

        @Test
        @DisplayName("Inspector creado aparece en la lista de activos")
        void inspector_creado_aparece_en_activos() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-014", dep);

            List<FalInspector> activos = inspectorService.listarActivos();
            assertThat(activos).anyMatch(i -> i.getIdInsp().equals(insp.getIdInsp()));
        }
    }

    // =========================================================================
    // 8A2-15: Inspector no encontrado lanza error controlado
    // =========================================================================

    @Nested
    @DisplayName("8A2-15: Inspector no encontrado lanza error controlado")
    class InspectorNoEncontradoTest {

        @Test
        @DisplayName("obtener con idInsp inexistente lanza InspectorNoEncontradoException")
        void obtener_inexistente_lanza_excepcion() {
            assertThatThrownBy(() -> inspectorService.obtener(9999L))
                    .isInstanceOf(InspectorNoEncontradoException.class)
                    .hasMessageContaining("9999");
        }

        @Test
        @DisplayName("versionar con idInsp inexistente lanza InspectorNoEncontradoException")
        void versionar_inexistente_lanza_excepcion() {
            FalDependencia dep = crearDependenciaTransito();
            assertThatThrownBy(() -> inspectorService.versionar(new VersionarInspectorCommand(
                    9999L, 9999, "Nombre X",
                    dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "admin")))
                    .isInstanceOf(InspectorNoEncontradoException.class)
                    .hasMessageContaining("9999");
        }
    }

    // =========================================================================
    // 8A2-16: Guardrail: inspector no tiene tipoActa propio
    // =========================================================================

    @Nested
    @DisplayName("8A2-16: Guardrail: inspector no tiene tipoActa propio")
    class SinTipoActaPropioTest {

        @Test
        @DisplayName("FalInspector no expone getter getTipoActa")
        void inspector_no_tiene_getTipoActa() {
            boolean tieneMetodo = false;
            for (var m : FalInspector.class.getMethods()) {
                if (m.getName().equals("getTipoActa")) {
                    tieneMetodo = true;
                    break;
                }
            }
            assertThat(tieneMetodo).as("FalInspector no debe tener getTipoActa()").isFalse();
        }

        @Test
        @DisplayName("FalInspectorVersion no expone getter getTipoActa")
        void inspector_version_no_tiene_getTipoActa() {
            boolean tieneMetodo = false;
            for (var m : FalInspectorVersion.class.getMethods()) {
                if (m.getName().equals("getTipoActa")) {
                    tieneMetodo = true;
                    break;
                }
            }
            assertThat(tieneMetodo).as("FalInspectorVersion no debe tener getTipoActa()").isFalse();
        }

        @Test
        @DisplayName("FalInspector no tiene campo con nombre tipoActa")
        void inspector_no_tiene_campo_tipoActa() {
            boolean tieneField = false;
            for (Field f : FalInspector.class.getDeclaredFields()) {
                if (f.getName().toLowerCase().contains("tipoac")) {
                    tieneField = true;
                    break;
                }
            }
            assertThat(tieneField).as("FalInspector no debe tener campo tipoActa").isFalse();
        }
    }

    // =========================================================================
    // 8A2-17: El ambito del inspector depende de idDep/verDep
    // =========================================================================

    @Nested
    @DisplayName("8A2-17: Ambito del inspector via idDep/verDep")
    class AmbitoViaDepVersionTest {

        @Test
        @DisplayName("Version del inspector expone idDep y verDep congelados de la dependencia")
        void version_expone_idDep_y_verDep() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-017", dep);

            List<FalInspectorVersion> versiones = inspectorRepo.findVersionesByInsp(insp.getIdInsp());
            assertThat(versiones).hasSize(1);
            FalInspectorVersion v = versiones.get(0);
            assertThat(v.getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(v.getVerDep()).isEqualTo(1);
        }

        @Test
        @DisplayName("El tipoActa del inspector se obtiene consultando la dependencia versionada")
        void tipoActa_se_obtiene_desde_dependencia() {
            FalDependencia dep = crearDependenciaTransito();
            FalInspector insp = crearInspectorConDep("USER-017B", dep);

            FalInspectorVersion vInsp = inspectorRepo
                    .findVersionesByInsp(insp.getIdInsp()).get(0);

            TipoActa tipoActaDerived = dependenciaRepo
                    .findVersionesByDep(vInsp.getIdDep())
                    .stream()
                    .filter(vd -> vd.getVerDep() == vInsp.getVerDep())
                    .map(vd -> vd.getTipoActa())
                    .findFirst()
                    .orElseThrow();

            assertThat(tipoActaDerived).isEqualTo(TipoActa.TRANSITO);
        }
    }
}
