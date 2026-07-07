package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.VincularDependenciaNormativaCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.NormativaService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 8A-4: Normativas in-memory alineadas al modelo MariaDB productivo.
 */
@DisplayName("Slice 8A-4: Normativas in-memory")
class NormativaTest {

    private DependenciaRepository dependenciaRepo;
    private DependenciaService dependenciaService;
    private NormativaRepository normativaRepo;
    private NormativaService normativaService;

    @BeforeEach
    void setUp() {
        dependenciaRepo = new InMemoryDependenciaRepository();
        dependenciaService = new DependenciaService(dependenciaRepo);
        normativaRepo = new InMemoryNormativaRepository();
        normativaService = new NormativaService(normativaRepo, dependenciaRepo);
    }

    private FalDependencia crearDep() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                "DEP01", "Dependencia de Transito", null,
                TipoActa.TRANSITO, LocalDate.now(), "sistema"));
    }

    private FalNormativaFaltas crearNormativa() {
        return normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                "ORCV", 1, "Ordenanza de Control Vehicular",
                "Descripcion de prueba", LocalDate.now(), "sistema"));
    }

    private FalNormativaFaltas crearNormativaConCodigo(String codigoNorma, int versionNorma) {
        return normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                codigoNorma, versionNorma, "Normativa " + codigoNorma + " v" + versionNorma,
                null, LocalDate.now(), "sistema"));
    }

    private FalArticuloNormativaFaltas crearArticulo(Long normativaId, TipoUnidad tipoUnidad) {
        return normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                normativaId, "ART01", 1, "Articulo 1",
                "Descripcion articulo", BigDecimal.valueOf(2.5), tipoUnidad,
                false, null, null, LocalDate.now(), "sistema"));
    }
    @Nested
    @DisplayName("8A4-01: TipoUnidad catalogo productivo cerrado")
    class TipoUnidadGuardrailTest {
        @Test
        @DisplayName("TipoUnidad contiene exactamente SALARIO, UNIDAD_FIJA, MONTO")
        void tipoUnidad_valores_exactos_del_modelo_productivo() {
            Set<String> nombres = Arrays.stream(TipoUnidad.values())
                    .map(Enum::name).collect(Collectors.toSet());
            assertThat(nombres).containsExactlyInAnyOrder("SALARIO", "UNIDAD_FIJA", "MONTO");
            assertThat(TipoUnidad.values()).hasSize(3);
        }
        @Test
        @DisplayName("TipoUnidad.SALARIO existe")
        void tipoUnidad_SALARIO_existe() {
            assertThat(TipoUnidad.valueOf("SALARIO")).isEqualTo(TipoUnidad.SALARIO);
        }
        @Test
        @DisplayName("TipoUnidad.UNIDAD_FIJA existe")
        void tipoUnidad_UNIDAD_FIJA_existe() {
            assertThat(TipoUnidad.valueOf("UNIDAD_FIJA")).isEqualTo(TipoUnidad.UNIDAD_FIJA);
        }
        @Test
        @DisplayName("TipoUnidad.MONTO existe")
        void tipoUnidad_MONTO_existe() {
            assertThat(TipoUnidad.valueOf("MONTO")).isEqualTo(TipoUnidad.MONTO);
        }
    }

    @Nested
    @DisplayName("8A4-02: Crear normativa valida")
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
    @DisplayName("8A4-03: Validaciones obligatorias de normativa")
    class ValidacionesNormativaTest {
        @Test
        @DisplayName("Rechaza normativa sin codigoNorma")
        void rechaza_sin_codigoNorma() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand(null, 1, "Nombre", null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa con codigoNorma en blanco")
        void rechaza_codigoNorma_blank() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("  ", 1, "Nombre", null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa sin nombreNorma")
        void rechaza_sin_nombreNorma() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("COD1", 1, null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza normativa con versionNorma = 0")
        void rechaza_versionNorma_cero() {
            assertThatThrownBy(() -> normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("COD1", 0, "Nombre", null, LocalDate.now(), "sistema")))
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
    @DisplayName("8A4-04: Buscar normativa")
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
    @DisplayName("8A4-05: Crear articulo valido")
    class CrearArticuloValidoTest {
        @Test
        @DisplayName("Crear articulo con tipoUnidad SALARIO")
        void crear_articulo_SALARIO() {
            FalNormativaFaltas n = crearNormativa();
            FalArticuloNormativaFaltas a = crearArticulo(n.getId(), TipoUnidad.SALARIO);
            assertThat(a.getId()).isNotNull().isPositive();
            assertThat(a.getNormativaId()).isEqualTo(n.getId());
            assertThat(a.getCodigoArticulo()).isEqualTo("ART01");
            assertThat(a.getVersionArticulo()).isEqualTo(1);
            assertThat(a.getNombreArticulo()).isEqualTo("Articulo 1");
            assertThat(a.getCantidadUnidades()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
            assertThat(a.getTipoUnidad()).isEqualTo(TipoUnidad.SALARIO);
            assertThat(a.isSiTienePagoVoluntario()).isFalse();
            assertThat(a.isSiActivo()).isTrue();
            assertThat(a.getFhAlta()).isNotNull();
        }
        @Test
        @DisplayName("Crear articulo con tipoUnidad UNIDAD_FIJA")
        void crear_articulo_UNIDAD_FIJA() {
            FalNormativaFaltas n = crearNormativa();
            assertThat(crearArticulo(n.getId(), TipoUnidad.UNIDAD_FIJA).getTipoUnidad())
                    .isEqualTo(TipoUnidad.UNIDAD_FIJA);
        }
        @Test
        @DisplayName("Crear articulo con tipoUnidad MONTO")
        void crear_articulo_MONTO() {
            FalNormativaFaltas n = crearNormativa();
            assertThat(crearArticulo(n.getId(), TipoUnidad.MONTO).getTipoUnidad())
                    .isEqualTo(TipoUnidad.MONTO);
        }
        @Test
        @DisplayName("Crear articulo con pago voluntario diferenciado")
        void crear_articulo_con_pago_voluntario() {
            FalNormativaFaltas n = crearNormativa();
            FalArticuloNormativaFaltas a = normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART02", 1, "Articulo con pago voluntario",
                            null, BigDecimal.valueOf(3.0), TipoUnidad.SALARIO,
                            true, BigDecimal.valueOf(1.5), TipoUnidad.SALARIO,
                            LocalDate.now(), "sistema"));
            assertThat(a.isSiTienePagoVoluntario()).isTrue();
            assertThat(a.getCantidadUnidadesPagoVoluntario())
                    .isEqualByComparingTo(BigDecimal.valueOf(1.5));
            assertThat(a.getTipoUnidadPagoVoluntario()).isEqualTo(TipoUnidad.SALARIO);
        }
        @Test
        @DisplayName("Listar articulos por normativa")
        void listar_articulos_por_normativa() {
            FalNormativaFaltas n = crearNormativa();
            crearArticulo(n.getId(), TipoUnidad.SALARIO);
            normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                    n.getId(), "ART02", 1, "Articulo 2",
                    null, BigDecimal.ONE, TipoUnidad.MONTO,
                    false, null, null, LocalDate.now(), "sistema"));
            assertThat(normativaService.listarArticulosByNormativa(n.getId())).hasSize(2);
        }
    }
    @Nested
    @DisplayName("8A4-06: Validaciones de articulo")
    class ValidacionesArticuloTest {
        @Test
        @DisplayName("Rechaza articulo con normativa inexistente")
        void rechaza_normativa_inexistente() {
            assertThatThrownBy(() -> crearArticulo(9999L, TipoUnidad.SALARIO))
                    .isInstanceOf(NormativaNoEncontradaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin codigoArticulo")
        void rechaza_sin_codigoArticulo() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), null, 1, "Nombre",
                            null, BigDecimal.ONE, TipoUnidad.SALARIO,
                            false, null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin nombreArticulo")
        void rechaza_sin_nombreArticulo() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, null,
                            null, BigDecimal.ONE, TipoUnidad.SALARIO,
                            false, null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin tipoUnidad")
        void rechaza_sin_tipoUnidad() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, "Nombre",
                            null, BigDecimal.ONE, null,
                            false, null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza articulo sin cantidadUnidades")
        void rechaza_sin_cantidadUnidades() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, "Nombre",
                            null, null, TipoUnidad.SALARIO,
                            false, null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza duplicado funcional codigoArticulo+versionArticulo en misma normativa")
        void rechaza_duplicado_articulo() {
            FalNormativaFaltas n = crearNormativa();
            crearArticulo(n.getId(), TipoUnidad.SALARIO);
            assertThatThrownBy(() -> crearArticulo(n.getId(), TipoUnidad.MONTO))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        @DisplayName("Rechaza pago voluntario sin cantidadUnidadesPagoVoluntario cuando siTiene=true")
        void rechaza_pago_voluntario_sin_cantidad() {
            FalNormativaFaltas n = crearNormativa();
            assertThatThrownBy(() -> normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(
                            n.getId(), "ART01", 1, "Nombre",
                            null, BigDecimal.ONE, TipoUnidad.SALARIO,
                            true, null, TipoUnidad.SALARIO, LocalDate.now(), "sistema")))
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
    @DisplayName("8A4-07: Vincular dependencia-normativa")
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
    @DisplayName("8A4-08: Guardrails - conceptos NO implementados en 8A-4")
    class GuardrailScopeTest {

        @Test
        @DisplayName("No existe enum RolFirmaReq en el classpath")
        void no_existe_RolFirmaReq() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.enums.RolFirmaReq"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        @Test
        @DisplayName("No existe enum MecanismoFirmaReq en el classpath")
        void no_existe_MecanismoFirmaReq() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.enums.MecanismoFirmaReq"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        @Test
        @DisplayName("No existe TarifarioRepository en el classpath")
        void no_existe_tarifario_repository() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.repository.TarifarioRepository"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        @Test
        @DisplayName("FalNormativaFaltas usa campos exactos del modelo productivo fal_normativa_faltas")
        void falNormativaFaltas_usa_campos_del_modelo_productivo() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas");
            assertThat(clazz.getDeclaredField("codigoNorma")).isNotNull();
            assertThat(clazz.getDeclaredField("versionNorma")).isNotNull();
            assertThat(clazz.getDeclaredField("nombreNorma")).isNotNull();
            assertThat(clazz.getDeclaredField("descripcionNorma")).isNotNull();
            assertThat(clazz.getDeclaredField("siActiva")).isNotNull();
            assertThat(clazz.getDeclaredField("fhVigDesde")).isNotNull();
            assertThat(clazz.getDeclaredField("fhVigHasta")).isNotNull();
            assertThat(clazz.getDeclaredField("fhAlta")).isNotNull();
            assertThat(clazz.getDeclaredField("idUserAlta")).isNotNull();
        }
        @Test
        @DisplayName("FalArticuloNormativaFaltas usa campos exactos del modelo productivo")
        void falArticuloNormativaFaltas_usa_campos_del_modelo_productivo() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas");
            assertThat(clazz.getDeclaredField("normativaId")).isNotNull();
            assertThat(clazz.getDeclaredField("codigoArticulo")).isNotNull();
            assertThat(clazz.getDeclaredField("versionArticulo")).isNotNull();
            assertThat(clazz.getDeclaredField("nombreArticulo")).isNotNull();
            assertThat(clazz.getDeclaredField("cantidadUnidades")).isNotNull();
            assertThat(clazz.getDeclaredField("tipoUnidad")).isNotNull();
            assertThat(clazz.getDeclaredField("siTienePagoVoluntario")).isNotNull();
            assertThat(clazz.getDeclaredField("cantidadUnidadesPagoVoluntario")).isNotNull();
            assertThat(clazz.getDeclaredField("tipoUnidadPagoVoluntario")).isNotNull();
            assertThat(clazz.getDeclaredField("siActivo")).isNotNull();
            assertThat(clazz.getDeclaredField("fhVigDesde")).isNotNull();
            assertThat(clazz.getDeclaredField("fhVigHasta")).isNotNull();
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