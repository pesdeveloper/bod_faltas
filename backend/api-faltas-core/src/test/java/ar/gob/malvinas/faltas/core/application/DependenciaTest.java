package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaVersion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 8A-1: Dependencias in-memory alineadas al modelo MariaDB productivo.
 *
 * Verifica:
 * - Creacion de dependencias con version inicial verDep=1.
 * - Consulta como activa.
 * - Validaciones obligatorias (nomDep, tipoActa).
 * - Unicidad de codDep.
 * - Relacion padre-hijo con congelamiento de verDepPadre.
 * - Versionado incrementa verDep y conserva historial.
 * - Guardrail del catalogo productivo: ALCOHOLEMIA no existe, strings libres prohibidos.
 */
@DisplayName("Slice 8A-1: Dependencias in-memory")
class DependenciaTest {

    private DependenciaRepository dependenciaRepo;
    private DependenciaService dependenciaService;

    @BeforeEach
    void setUp() {
        dependenciaRepo = new InMemoryDependenciaRepository();
        dependenciaService = new DependenciaService(dependenciaRepo, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FalDependencia crearDependenciaTransito() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                null, "Oficina de Transito Central", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalDependencia crearDependenciaConCod(String codDep, String nomDep, TipoActa tipo) {
        return dependenciaService.crear(new CrearDependenciaCommand(
                codDep, nomDep, null, tipo, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    // =========================================================================
    // 8A1-01: Crear dependencia valida genera idDep y verDep = 1
    // =========================================================================

    @Nested
    @DisplayName("8A1-01: Crear dependencia valida")
    class CrearDependenciaValidaTest {

        @Test
        @DisplayName("Genera idDep asignado y version inicial verDep = 1")
        void crear_genera_idDep_y_verDep_1() {
            FalDependencia dep = crearDependenciaTransito();

            assertThat(dep.getIdDep()).isNotNull();
            assertThat(dep.getNomDep()).isEqualTo("Oficina de Transito Central");
            assertThat(dep.isSiActiva()).isTrue();

            List<FalDependenciaVersion> versiones = dependenciaRepo.findVersionesByDep(dep.getIdDep());
            assertThat(versiones).hasSize(1);
            assertThat(versiones.get(0).getVerDep()).isEqualTo(1);
            assertThat(versiones.get(0).getTipoActa()).isEqualTo(TipoActa.TRANSITO);
            assertThat(versiones.get(0).isSiActiva()).isTrue();
        }
    }

    // =========================================================================
    // 8A1-02: Crear con tipoActa TRANSITO queda consultable como activa
    // =========================================================================

    @Nested
    @DisplayName("8A1-02: Dependencia con TRANSITO queda activa y consultable")
    class DependenciaActivaConsultableTest {

        @Test
        @DisplayName("Aparece en listarActivas y en obtener por id")
        void dependencia_transito_activa_y_consultable() {
            FalDependencia dep = crearDependenciaTransito();

            List<FalDependencia> activas = dependenciaService.listarActivas();
            assertThat(activas).anyMatch(d -> d.getIdDep().equals(dep.getIdDep()));

            FalDependencia obtenida = dependenciaService.obtener(dep.getIdDep());
            assertThat(obtenida.getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(obtenida.isSiActiva()).isTrue();
        }
    }

    // =========================================================================
    // 8A1-03: Crear sin nombre falla
    // =========================================================================

    @Nested
    @DisplayName("8A1-03: Crear sin nomDep falla")
    class CrearSinNombreTest {

        @Test
        @DisplayName("nomDep nulo lanza PrecondicionVioladaException")
        void crear_sin_nomDep_nulo_falla() {
            assertThatThrownBy(() -> dependenciaService.crear(new CrearDependenciaCommand(
                    null, null, null, TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nomDep");
        }

        @Test
        @DisplayName("nomDep en blanco lanza PrecondicionVioladaException")
        void crear_con_nomDep_blanco_falla() {
            assertThatThrownBy(() -> dependenciaService.crear(new CrearDependenciaCommand(
                    null, "  ", null, TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nomDep");
        }
    }

    // =========================================================================
    // 8A1-04: Crear sin tipoActa falla
    // =========================================================================

    @Nested
    @DisplayName("8A1-04: Crear sin tipoActa falla")
    class CrearSinTipoActaTest {

        @Test
        @DisplayName("tipoActa nulo lanza PrecondicionVioladaException")
        void crear_sin_tipoActa_falla() {
            assertThatThrownBy(() -> dependenciaService.crear(new CrearDependenciaCommand(
                    null, "Oficina Comercio", null, null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("tipoActa");
        }
    }

    // =========================================================================
    // 8A1-05: Crear con codDep duplicado falla
    // =========================================================================

    @Nested
    @DisplayName("8A1-05: codDep duplicado falla")
    class CodDepDuplicadoTest {

        @Test
        @DisplayName("Segundo codDep identico lanza PrecondicionVioladaException")
        void codDep_duplicado_falla() {
            crearDependenciaConCod("DEP-001", "Transito Norte", TipoActa.TRANSITO);

            assertThatThrownBy(() -> crearDependenciaConCod("DEP-001", "Transito Sur", TipoActa.TRANSITO))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("DEP-001");
        }

        @Test
        @DisplayName("codDep nulo no dispara validacion de unicidad")
        void codDep_nulo_no_falla() {
            crearDependenciaConCod(null, "Dependencia A", TipoActa.COMERCIO);
            crearDependenciaConCod(null, "Dependencia B", TipoActa.COMERCIO);
            // sin excepcion: dos dependencias sin codigo conviven
        }
    }

    // =========================================================================
    // 8A1-06: Crear dependencia hija congela idDepPadre y verDepPadre vigente
    // =========================================================================

    @Nested
    @DisplayName("8A1-06: Dependencia hija congela padre y verDepPadre")
    class DependenciaHijaCongelaPadreTest {

        @Test
        @DisplayName("Version hija contiene idDepPadre y verDepPadre congelados")
        void hija_congela_padre_y_verDepPadre() {
            FalDependencia padre = crearDependenciaTransito();

            FalDependencia hija = dependenciaService.crear(new CrearDependenciaCommand(
                    null, "Transito Zona Sur", padre.getIdDep(),
                    TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));

            List<FalDependenciaVersion> versionesHija = dependenciaRepo.findVersionesByDep(hija.getIdDep());
            assertThat(versionesHija).hasSize(1);
            FalDependenciaVersion v = versionesHija.get(0);
            assertThat(v.getIdDepPadre()).isEqualTo(padre.getIdDep());
            assertThat(v.getVerDepPadre()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8A1-07: Crear hija con padre inexistente falla
    // =========================================================================

    @Nested
    @DisplayName("8A1-07: Crear hija con padre inexistente falla")
    class HijaPadreInexistenteTest {

        @Test
        @DisplayName("idDepPadre inexistente lanza PrecondicionVioladaException")
        void padre_inexistente_falla() {
            assertThatThrownBy(() -> dependenciaService.crear(new CrearDependenciaCommand(
                    null, "Hija sin padre", 9999L,
                    TipoActa.CONTRAVENCION, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("9999");
        }
    }

    // =========================================================================
    // 8A1-08: Versionar crea verDep = anterior + 1
    // =========================================================================

    @Nested
    @DisplayName("8A1-08: Versionar incrementa verDep")
    class VersionarIncrementaVerDepTest {

        @Test
        @DisplayName("Nueva version tiene verDep = 2 despues de versionar desde verDep 1")
        void versionar_crea_verDep_2() {
            FalDependencia dep = crearDependenciaTransito();

            FalDependenciaVersion v2 = dependenciaService.versionar(new VersionarDependenciaCommand(
                    dep.getIdDep(), "Transito Central (actualizada)", null,
                    TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(1), "admin"));

            assertThat(v2.getVerDep()).isEqualTo(2);
        }
    }

    // =========================================================================
    // 8A1-09: Versionar conserva historial y nueva version queda vigente
    // =========================================================================

    @Nested
    @DisplayName("8A1-09: Versionar conserva historial")
    class VersionarConservaHistorialTest {

        @Test
        @DisplayName("Historial tiene 2 versiones; la nueva esta activa, la anterior inactiva")
        void versionar_conserva_historial_y_deja_nueva_vigente() {
            FalDependencia dep = crearDependenciaTransito();
            LocalDate fhVigDesdeV2 = FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(1);

            dependenciaService.versionar(new VersionarDependenciaCommand(
                    dep.getIdDep(), "Nombre v2", null,
                    TipoActa.COMERCIO, fhVigDesdeV2, "admin"));

            List<FalDependenciaVersion> versiones = dependenciaRepo.findVersionesByDep(dep.getIdDep());
            assertThat(versiones).hasSize(2);

            FalDependenciaVersion v1 = versiones.stream()
                    .filter(v -> v.getVerDep() == 1).findFirst().orElseThrow();
            FalDependenciaVersion v2 = versiones.stream()
                    .filter(v -> v.getVerDep() == 2).findFirst().orElseThrow();

            assertThat(v1.isSiActiva()).isFalse();
            assertThat(v1.getFhVigHasta()).isEqualTo(fhVigDesdeV2);
            assertThat(v2.isSiActiva()).isTrue();
            assertThat(v2.getTipoActa()).isEqualTo(TipoActa.COMERCIO);
        }
    }

    // =========================================================================
    // 8A1-10: ALCOHOLEMIA no es tipoActa
    // =========================================================================

    @Nested
    @DisplayName("8A1-10: Guardrail ALCOHOLEMIA no es tipo de acta")
    class AlcoholemiaNoEsTipoActaTest {

        @Test
        @DisplayName("El enum TipoActa no contiene ALCOHOLEMIA")
        void alcoholemia_no_existe_en_TipoActa() {
            Set<String> nombres = Arrays.stream(TipoActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).doesNotContain("ALCOHOLEMIA");
        }
    }

    // =========================================================================
    // 8A1-11: Solo valores del catalogo productivo son aceptados
    // =========================================================================

    @Nested
    @DisplayName("8A1-11: Catalogo TipoActa solo contiene valores productivos")
    class CatalogoProductivoTest {

        @Test
        @DisplayName("TipoActa tiene exactamente los 4 valores productivos")
        void tipoActa_tiene_valores_productivos() {
            Set<String> nombres = Arrays.stream(TipoActa.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());
            assertThat(nombres).containsExactlyInAnyOrder(
                    "TRANSITO", "CONTRAVENCION", "SUSTANCIAS_ALIMENTICIAS", "COMERCIO");
        }

        @Test
        @DisplayName("Los 4 valores del catalogo se pueden usar para crear dependencias")
        void todos_los_valores_productivos_son_validos() {
            for (TipoActa tipo : TipoActa.values()) {
                FalDependencia dep = dependenciaService.crear(new CrearDependenciaCommand(
                        null, "Dep para " + tipo.name(), null,
                        tipo, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
                assertThat(dep).isNotNull();
            }
        }
    }

    // =========================================================================
    // 8A1-12: Versionar dependencia inexistente falla
    // =========================================================================

    @Nested
    @DisplayName("8A1-12: Versionar dependencia inexistente falla")
    class VersionarInexistenteTest {

        @Test
        @DisplayName("Versionar dependencia con idDep inexistente lanza DependenciaNoEncontradaException")
        void versionar_dependencia_inexistente_falla() {
            assertThatThrownBy(() -> dependenciaService.versionar(new VersionarDependenciaCommand(
                    9999L, "Nombre", null, TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "admin")))
                    .isInstanceOf(DependenciaNoEncontradaException.class)
                    .hasMessageContaining("9999");
        }
    }
}
