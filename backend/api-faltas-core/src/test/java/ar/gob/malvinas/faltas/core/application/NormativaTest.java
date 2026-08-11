package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.VincularDependenciaNormativaCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.NormativaService;
import ar.gob.malvinas.faltas.core.domain.enums.CategoriaInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;
import ar.gob.malvinas.faltas.core.domain.exception.ArticuloNormativaNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.DependenciaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.NormativaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.NormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de normativa/articulo versionados (rediseño PROMPT-NOCTURNO-WAVE-04-PARTE-2 S6/S7.3).
 *
 * El articulo ya NO contiene datos economicos (cantidadUnidades/tipoUnidad/pagoVoluntario):
 * esos campos viven exclusivamente en {@code fal_tarifario_unidad_faltas} (por articulo).
 * En su lugar el articulo declara {@code tipoInfraccion}, {@code categoriaInfraccion} y
 * {@code siAdmiteMedidaPreventiva}.
 */
@DisplayName("Normativa y articulo versionados (Wave 4 PARTE-2)")
class NormativaTest {

    private DependenciaRepository dependenciaRepo;
    private DependenciaService dependenciaService;
    private NormativaRepository normativaRepo;
    private NormativaService normativaService;

    @BeforeEach
    void setUp() {
        dependenciaRepo = new InMemoryDependenciaRepository();
        dependenciaService = new DependenciaService(dependenciaRepo, FaltasClockTestSupport.FIXED);
        normativaRepo = new InMemoryNormativaRepository();
        normativaService = new NormativaService(normativaRepo, dependenciaRepo, FaltasClockTestSupport.FIXED);
    }

    private FalDependencia crearDep() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                "DEP01", "Dependencia de Transito", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalNormativaFaltas crearNormativa() {
        return normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                "ORCV", 1, "Ordenanza de Control Vehicular",
                "Descripcion de prueba", FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalNormativaFaltas crearNormativaConCodigo(String codigoNorma, int versionNorma) {
        return normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                codigoNorma, versionNorma, "Normativa " + codigoNorma + " v" + versionNorma,
                null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalArticuloNormativaFaltas crearArticulo(Long normativaId, TipoInfraccion tipoInfraccion) {
        return normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                normativaId, "ART01", 1, "Articulo 1", "Descripcion articulo",
                tipoInfraccion, null, false,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    @Nested
    @DisplayName("Crear normativa valida")
    class CrearNormativaValidaTest {
        @Test
        @DisplayName("Crear normativa valida asigna id y campos del modelo productivo")
        void crear_normativa_valida() {
            FalNormativaFaltas n = crearNormativa();
            assertThat(n.getId()).isNotNull().isPositive();
            assertThat(n.getCodigoNorma()).isEqualTo("ORCV");
            assertThat(n.getVersionNorma()).isEqualTo(1);
            assertThat(n.getNombreNorma()).isEqualTo("Ordenanza de Control Vehicular");
            assertThat(n.getDescripcionNorma()).isEqualTo("Descripcion de prueba");
            assertThat(n.isSiActiva()).isTrue();
            assertThat(n.getFhVigDesde()).isNotNull();
            assertThat(n.getFhVigHasta()).isNull();
            assertThat(n.getVersionRow()).isZero();
            assertThat(n.getFhAlta()).isNotNull();
            assertThat(n.getIdUserAlta()).isEqualTo("sistema");
        }
        @Test
        @DisplayName("Normativa sin descripcionNorma es valida (campo opcional)")
        void crear_normativa_sin_descripcion_es_valida() {
            FalNormativaFaltas n = crearNormativaConCodigo("LEY01", 1);
            assertThat(n.getDescripcionNorma()).isNull();
            assertThat(n.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validaciones obligatorias de normativa")
    class ValidacionesNormativaTest {
        @Test
        @DisplayName("Rechaza normativa sin codigoNorma")
        void rechaza_sin_codigoNorma() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand(null, 1, "Nombre", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa con codigoNorma en blanco")
        void rechaza_codigoNorma_blank() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("  ", 1, "Nombre", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa sin nombreNorma")
        void rechaza_sin_nombreNorma() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("COD1", 1, null, null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa con versionNorma = 0")
        void rechaza_versionNorma_cero() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("COD1", 0, "Nombre", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza duplicado funcional codigoNorma+versionNorma")
        void rechaza_duplicado_funcional() {
            crearNormativa();
            assertThatThrownBy(() -> crearNormativa())
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Buscar normativa")
    class BuscarNormativaTest {
        @Test
        @DisplayName("Buscar normativa por id retorna la normativa creada")
        void buscar_por_id_retorna_normativa() {
            FalNormativaFaltas creada = crearNormativa();
            FalNormativaFaltas encontrada = normativaService.obtenerNormativa(creada.getId());
            assertThat(encontrada.getId()).isEqualTo(creada.getId());
            assertThat(encontrada.getCodigoNorma()).isEqualTo("ORCV");
        }
        @Test
        @DisplayName("Buscar normativa inexistente lanza NormativaNoEncontradaException")
        void buscar_inexistente_lanza_excepcion() {
            assertThatThrownBy(() -> normativaService.obtenerNormativa(9999L))
                    .isInstanceOf(NormativaNoEncontradaException.class);
        }
        @Test
        @DisplayName("Listar normativas activas devuelve solo las activas")
        void listar_normativas_activas() {
            crearNormativaConCodigo("N01", 1);
            crearNormativaConCodigo("N02", 1);
            FalNormativaFaltas inactiva = crearNormativaConCodigo("N03", 1);
            inactiva.setSiActiva(false);
            normativaRepo.guardarNormativa(inactiva);
            List<FalNormativaFaltas> activas = normativaService.listarNormativasActivas();
            assertThat(activas).hasSize(2);
            assertThat(activas).noneMatch(n -> "N03".equals(n.getCodigoNorma()));
        }
    }

    @Nested
    @DisplayName("Crear articulo valido (sin datos economicos)")
    class CrearArticuloValidoTest {
        @Test
        @DisplayName("Crear articulo con tipoInfraccion TRANSITO")
        void crear_articulo_transito() {
            FalNormativaFaltas n = crearNormativa();
            FalArticuloNormativaFaltas a = crearArticulo(n.getId(), TipoInfraccion.TRANSITO);
            assertThat(a.getId()).isNotNull().isPositive();
            assertThat(a.getNormativaId()).isEqualTo(n.getId());
            assertThat(a.getCodigoArticulo()).isEqualTo("ART01");
            assertThat(a.getVersionArticulo()).isEqualTo(1);
            assertThat(a.getNombreArticulo()).isEqualTo("Articulo 1");
            assertThat(a.getTipoInfraccion()).isEqualTo(TipoInfraccion.TRANSITO);
            assertThat(a.getCategoriaInfraccion()).isNull();
            assertThat(a.isSiAdmiteMedidaPreventiva()).isFalse();
            assertThat(a.isSiActivo()).isTrue();
            assertThat(a.getVersionRow()).isZero();
            assertThat(a.getFhAlta()).isNotNull();
        }
        @Test
        @DisplayName("Crear articulo con categoriaInfraccion y medida preventiva habilitada")
        void crear_articulo_con_categoria_y_medida() {
            FalNormativaFaltas n = crearNormativa();
            FalArticuloNormativaFaltas a = normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART02", 1, "Articulo con categoria",
                            null, TipoInfraccion.CONTRAVENCION, CategoriaInfraccion.GRAVE, true,
                            FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
            assertThat(a.getCategoriaInfraccion()).isEqualTo(CategoriaInfraccion.GRAVE);
            assertThat(a.isSiAdmiteMedidaPreventiva()).isTrue();
        }
        @Test
        @DisplayName("Listar articulos por normativa")
        void listar_articulos_por_normativa() {
            FalNormativaFaltas n = crearNormativa();
            crearArticulo(n.getId(), TipoInfraccion.TRANSITO);
            normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                    n.getId(), "ART02", 1, "Articulo 2", null,
                    TipoInfraccion.COMERCIO, null, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
            assertThat(normativaService.listarArticulosByNormativa(n.getId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Validaciones de articulo")
    class ValidacionesArticuloTest {
        @Test
        @DisplayName("Rechaza articulo con normativa inexistente")
        void rechaza_normativa_inexistente() {
            assertThatThrownBy(() -> crearArticulo(9999L, TipoInfraccion.TRANSITO))
                    .isInstanceOf(NormativaNoEncontradaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin codigoArticulo")
        void rechaza_sin_codigoArticulo() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), null, 1, "Nombre", null,
                            TipoInfraccion.TRANSITO, null, false,
                            FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin nombreArticulo")
        void rechaza_sin_nombreArticulo() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, null, null,
                            TipoInfraccion.TRANSITO, null, false,
                            FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin tipoInfraccion")
        void rechaza_sin_tipoInfraccion() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, "Nombre", null,
                            null, null, false,
                            FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza duplicado funcional codigoArticulo+versionArticulo en misma normativa")
        void rechaza_duplicado_articulo() {
            FalNormativaFaltas n = crearNormativa();
            crearArticulo(n.getId(), TipoInfraccion.TRANSITO);
            assertThatThrownBy(() -> crearArticulo(n.getId(), TipoInfraccion.COMERCIO))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo inexistente por id")
        void rechaza_articulo_inexistente_por_id() {
            assertThatThrownBy(() -> normativaService.obtenerArticulo(9999L))
                    .isInstanceOf(ArticuloNormativaNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Vincular dependencia-normativa")
    class VincularDependenciaNormativaTest {
        @Test
        @DisplayName("Vincular dependencia versionada con normativa existente")
        void vincular_dep_normativa_valido() {
            FalDependencia dep = crearDep();
            FalNormativaFaltas n = crearNormativa();
            FalDependenciaNormativa rel = normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n.getId(), "sistema"));
            assertThat(rel.getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(rel.getVerDep()).isEqualTo(1);
            assertThat(rel.getNormativaId()).isEqualTo(n.getId());
            assertThat(rel.isSiActiva()).isTrue();
            assertThat(rel.getFhAlta()).isNotNull();
        }
        @Test
        @DisplayName("Rechaza vinculo con dependencia inexistente")
        void rechaza_dep_inexistente() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(9999L, 1, n.getId(), "sistema")))
                    .isInstanceOf(DependenciaNoEncontradaException.class);
        }
        @Test
        @DisplayName("Rechaza vinculo con version de dependencia inexistente")
        void rechaza_version_dep_inexistente() {
            FalDependencia dep = crearDep();
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 99, n.getId(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza vinculo con normativa inexistente")
        void rechaza_normativa_inexistente() {
            FalDependencia dep = crearDep();
            assertThatThrownBy(() -> normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, 9999L, "sistema")))
                    .isInstanceOf(NormativaNoEncontradaException.class);
        }
        @Test
        @DisplayName("Rechaza duplicado activo dependencia+version+normativa")
        void rechaza_duplicado_activo() {
            FalDependencia dep = crearDep();
            FalNormativaFaltas n = crearNormativa();
            normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n.getId(), "sistema"));
            assertThatThrownBy(() -> normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n.getId(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Listar normativas activas por dependencia versionada")
        void listar_normativas_por_dep_version() {
            FalDependencia dep = crearDep();
            FalNormativaFaltas n1 = crearNormativaConCodigo("N01", 1);
            FalNormativaFaltas n2 = crearNormativaConCodigo("N02", 1);
            normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n1.getId(), "sistema"));
            normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n2.getId(), "sistema"));
            assertThat(normativaService.listarNormativasByDepVersion(dep.getIdDep(), 1)).hasSize(2);
        }
        @Test
        @DisplayName("Rechaza vinculo con normativa inactiva")
        void rechaza_normativa_inactiva() {
            FalDependencia dep = crearDep();
            FalNormativaFaltas n = crearNormativa();
            n.setSiActiva(false);
            normativaRepo.guardarNormativa(n);
            assertThatThrownBy(() -> normativaService.vincularDependenciaNormativa(
                    new VincularDependenciaNormativaCommand(dep.getIdDep(), 1, n.getId(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Guardrails de campos del modelo productivo")
    class GuardrailScopeTest {
        @Test
        @DisplayName("No existe TarifarioRepository (nombre generico) en el classpath")
        void no_existe_tarifario_repository_generico() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.repository.TarifarioRepository"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        @Test
        @DisplayName("FalNormativaFaltas tiene versionNorma (funcional) y versionRow (OCC) separados")
        void falNormativaFaltas_tiene_ambas_versiones() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas");
            assertThat(clazz.getDeclaredField("versionNorma")).isNotNull();
            assertThat(clazz.getDeclaredField("versionRow")).isNotNull();
        }
        @Test
        @DisplayName("FalArticuloNormativaFaltas NO tiene campos economicos (viven en el tarifario)")
        void falArticuloNormativaFaltas_sin_campos_economicos() {
            Class<?> clazz = FalArticuloNormativaFaltas.class;
            List<String> nombresCampos = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .map(java.lang.reflect.Field::getName).toList();
            assertThat(nombresCampos).doesNotContain(
                    "cantidadUnidades", "tipoUnidad", "siTienePagoVoluntario",
                    "cantidadUnidadesPagoVoluntario", "tipoUnidadPagoVoluntario");
            assertThat(nombresCampos).contains("tipoInfraccion", "categoriaInfraccion", "siAdmiteMedidaPreventiva", "versionRow");
        }
        @Test
        @DisplayName("FalDependenciaNormativa usa campos exactos del modelo productivo")
        void falDependenciaNormativa_usa_campos_del_modelo_productivo() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalDependenciaNormativa");
            assertThat(clazz.getDeclaredField("idDep")).isNotNull();
            assertThat(clazz.getDeclaredField("verDep")).isNotNull();
            assertThat(clazz.getDeclaredField("normativaId")).isNotNull();
            assertThat(clazz.getDeclaredField("siActiva")).isNotNull();
            assertThat(clazz.getDeclaredField("fhAlta")).isNotNull();
            assertThat(clazz.getDeclaredField("idUserAlta")).isNotNull();
        }
    }
}
