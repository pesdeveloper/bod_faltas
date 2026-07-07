package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.ActaArticuloInfringidoService;
import ar.gob.malvinas.faltas.core.application.service.MedidaPreventivaService;
import ar.gob.malvinas.faltas.core.application.service.NormativaService;
import ar.gob.malvinas.faltas.core.application.service.TarifarioService;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.domain.enums.CriterioTarifario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoManualizacionValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.MedidaPreventivaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.TarifarioNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.ValorizacionNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 8F-11D: Tarifario, Medidas Preventivas, Artículos Imputados y Valorización.
 */
@DisplayName("Slice 8F-11D: Tarifario, Medidas, Imputación y Valorización")
class TarifarioValorizacionTest {

    // =========================================================
    // Enums
    // =========================================================

    @Nested
    @DisplayName("8F11D-01: TipoUnidadFaltas")
    class TipoUnidadFaltasTest {
        @Test @DisplayName("valores exactos")
        void valores_exactos() {
            Set<String> nombres = Arrays.stream(TipoUnidadFaltas.values()).map(Enum::name).collect(Collectors.toSet());
            assertThat(nombres).containsExactlyInAnyOrder("SALARIO", "UNIDAD_FIJA", "MONTO");
        }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> codigos = Arrays.stream(TipoUnidadFaltas.values()).map(TipoUnidadFaltas::codigo).collect(Collectors.toSet());
            assertThat(codigos).hasSize(TipoUnidadFaltas.values().length);
        }
        @Test @DisplayName("fromCodigo SALARIO=1")
        void fromCodigo_salario() { assertThat(TipoUnidadFaltas.fromCodigo((short) 1)).isEqualTo(TipoUnidadFaltas.SALARIO); }
        @Test @DisplayName("fromCodigo UNIDAD_FIJA=2")
        void fromCodigo_unidad_fija() { assertThat(TipoUnidadFaltas.fromCodigo((short) 2)).isEqualTo(TipoUnidadFaltas.UNIDAD_FIJA); }
        @Test @DisplayName("fromCodigo MONTO=3")
        void fromCodigo_monto() { assertThat(TipoUnidadFaltas.fromCodigo((short) 3)).isEqualTo(TipoUnidadFaltas.MONTO); }
        @Test @DisplayName("fromCodigo desconocido lanza excepcion")
        void fromCodigo_desconocido() {
            assertThatThrownBy(() -> TipoUnidadFaltas.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("8F11D-02: EstadoValorizacion")
    class EstadoValorizacionTest {
        @Test @DisplayName("4 valores")
        void tamanio() { assertThat(EstadoValorizacion.values()).hasSize(4); }
        @Test @DisplayName("PRELIMINAR=1")
        void preliminar() { assertThat(EstadoValorizacion.fromCodigo((short) 1)).isEqualTo(EstadoValorizacion.PRELIMINAR); }
        @Test @DisplayName("CONFIRMADA=2")
        void confirmada() { assertThat(EstadoValorizacion.fromCodigo((short) 2)).isEqualTo(EstadoValorizacion.CONFIRMADA); }
        @Test @DisplayName("REEMPLAZADA=3")
        void reemplazada() { assertThat(EstadoValorizacion.fromCodigo((short) 3)).isEqualTo(EstadoValorizacion.REEMPLAZADA); }
        @Test @DisplayName("ANULADA=4")
        void anulada() { assertThat(EstadoValorizacion.fromCodigo((short) 4)).isEqualTo(EstadoValorizacion.ANULADA); }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(EstadoValorizacion.values()).map(EstadoValorizacion::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(EstadoValorizacion.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> EstadoValorizacion.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11D-03: TipoValorizacionActa")
    class TipoValorizacionActaTest {
        @Test @DisplayName("7 valores")
        void tamanio() { assertThat(TipoValorizacionActa.values()).hasSize(7); }
        @Test @DisplayName("INFRACCION_BASE=1")
        void infraccion_base() { assertThat(TipoValorizacionActa.fromCodigo((short) 1)).isEqualTo(TipoValorizacionActa.INFRACCION_BASE); }
        @Test @DisplayName("PAGO_VOLUNTARIO=2")
        void pago_voluntario() { assertThat(TipoValorizacionActa.fromCodigo((short) 2)).isEqualTo(TipoValorizacionActa.PAGO_VOLUNTARIO); }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(TipoValorizacionActa.values()).map(TipoValorizacionActa::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(TipoValorizacionActa.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> TipoValorizacionActa.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11D-04: CriterioTarifario")
    class CriterioTarifarioTest {
        @Test @DisplayName("3 valores")
        void tamanio() { assertThat(CriterioTarifario.values()).hasSize(3); }
        @Test @DisplayName("ULTIMO_VIGENTE=1, MANTIENE_ANTERIOR=2, MANUAL=3")
        void valores() {
            assertThat(CriterioTarifario.fromCodigo((short) 1)).isEqualTo(CriterioTarifario.ULTIMO_VIGENTE);
            assertThat(CriterioTarifario.fromCodigo((short) 2)).isEqualTo(CriterioTarifario.MANTIENE_ANTERIOR);
            assertThat(CriterioTarifario.fromCodigo((short) 3)).isEqualTo(CriterioTarifario.MANUAL);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> CriterioTarifario.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11D-05: OrigenValorizacion")
    class OrigenValorizacionTest {
        @Test @DisplayName("5 valores")
        void tamanio() { assertThat(OrigenValorizacion.values()).hasSize(5); }
        @Test @DisplayName("SISTEMA=1")
        void sistema() { assertThat(OrigenValorizacion.fromCodigo((short) 1)).isEqualTo(OrigenValorizacion.SISTEMA); }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(OrigenValorizacion.values()).map(OrigenValorizacion::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(OrigenValorizacion.values().length);
        }
    }

    @Nested
    @DisplayName("8F11D-06: TipoValorizacionItem")
    class TipoValorizacionItemTest {
        @Test @DisplayName("4 valores")
        void tamanio() { assertThat(TipoValorizacionItem.values()).hasSize(4); }
        @Test @DisplayName("AUTOMATICA=1, PAGO_VOLUNTARIO=2, MANUAL=3, FALLO=4")
        void valores() {
            assertThat(TipoValorizacionItem.fromCodigo((short) 1)).isEqualTo(TipoValorizacionItem.AUTOMATICA);
            assertThat(TipoValorizacionItem.fromCodigo((short) 4)).isEqualTo(TipoValorizacionItem.FALLO);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> TipoValorizacionItem.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11D-07: MotivoBajaArticuloInfringido")
    class MotivoBajaTest {
        @Test @DisplayName("4 valores")
        void tamanio() { assertThat(MotivoBajaArticuloInfringido.values()).hasSize(4); }
        @Test @DisplayName("CORRECCION_IMPUTACION=1")
        void correccion() { assertThat(MotivoBajaArticuloInfringido.fromCodigo((short) 1)).isEqualTo(MotivoBajaArticuloInfringido.CORRECCION_IMPUTACION); }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(MotivoBajaArticuloInfringido.values()).map(MotivoBajaArticuloInfringido::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(MotivoBajaArticuloInfringido.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> MotivoBajaArticuloInfringido.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11D-08: MotivoManualizacionValorizacion")
    class MotivoManualizacionTest {
        @Test @DisplayName("5 valores")
        void tamanio() { assertThat(MotivoManualizacionValorizacion.values()).hasSize(5); }
        @Test @DisplayName("OTRO_FUNDADO=5")
        void otro_fundado() { assertThat(MotivoManualizacionValorizacion.fromCodigo((short) 5)).isEqualTo(MotivoManualizacionValorizacion.OTRO_FUNDADO); }
        @Test @DisplayName("codigos únicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(MotivoManualizacionValorizacion.values()).map(MotivoManualizacionValorizacion::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(MotivoManualizacionValorizacion.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> MotivoManualizacionValorizacion.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    // =========================================================
    // Tarifario
    // =========================================================

    @Nested
    @DisplayName("8F11D-10: Tarifario")
    class TarifarioTests {

        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
        private TarifarioService tarifarioService;

        @BeforeEach
        void setUp() {
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
            tarifarioService = new TarifarioService(tarifarioRepo);
        }

        @Test @DisplayName("alta válida crea tarifario con siActiva=true")
        void alta_valida() {
            FalTarifarioUnidadFaltas t = tarifarioService.crear(
                    TipoUnidadFaltas.SALARIO, new BigDecimal("100000"), LocalDate.now(), null, "u1");
            assertThat(t.getId()).isNotNull();
            assertThat(t.isSiActiva()).isTrue();
            assertThat(t.getValorUnidad()).isEqualByComparingTo(new BigDecimal("100000.00"));
        }

        @Test @DisplayName("valorUnidad cero lanza excepcion")
        void valorUnidad_cero() {
            assertThatThrownBy(() -> tarifarioService.crear(
                    TipoUnidadFaltas.SALARIO, BigDecimal.ZERO, LocalDate.now(), null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("valorUnidad negativo lanza excepcion")
        void valorUnidad_negativo() {
            assertThatThrownBy(() -> tarifarioService.crear(
                    TipoUnidadFaltas.SALARIO, new BigDecimal("-1"), LocalDate.now(), null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("fhVigHasta anterior a fhVigDesde lanza excepcion")
        void fechas_invalidas() {
            assertThatThrownBy(() -> tarifarioService.crear(
                    TipoUnidadFaltas.SALARIO, new BigDecimal("100000"),
                    LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1), "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("superposicion activa lanza excepcion")
        void superposicion() {
            tarifarioService.crear(TipoUnidadFaltas.SALARIO, new BigDecimal("100000"),
                    LocalDate.of(2023, 1, 1), null, "u1");
            assertThatThrownBy(() -> tarifarioService.crear(
                    TipoUnidadFaltas.SALARIO, new BigDecimal("120000"),
                    LocalDate.of(2024, 1, 1), null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("findUltimoVigente resuelve tarifario por fecha")
        void findUltimoVigente() {
            FalTarifarioUnidadFaltas viejo = new FalTarifarioUnidadFaltas(1L,
                    TipoUnidadFaltas.SALARIO, new BigDecimal("80000"),
                    LocalDate.of(2022, 1, 1), LocalDateTime.now(), "u1");
            viejo.setFhVigHasta(LocalDate.of(2022, 12, 31));
            viejo.setSiActiva(false);
            tarifarioRepo.save(viejo);

            FalTarifarioUnidadFaltas nuevo = new FalTarifarioUnidadFaltas(2L,
                    TipoUnidadFaltas.SALARIO, new BigDecimal("120000"),
                    LocalDate.of(2023, 1, 1), LocalDateTime.now(), "u1");
            tarifarioRepo.save(nuevo);

            Optional<FalTarifarioUnidadFaltas> res = tarifarioService.resolverUltimoVigente(
                    TipoUnidadFaltas.SALARIO, LocalDate.of(2024, 1, 1));
            assertThat(res).isPresent();
            assertThat(res.get().getValorUnidad()).isEqualByComparingTo(new BigDecimal("120000.00"));
        }

        @Test @DisplayName("findUltimoVigente no devuelve inactivos")
        void findUltimoVigente_sin_inactivos() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(1L,
                    TipoUnidadFaltas.SALARIO, new BigDecimal("100000"),
                    LocalDate.of(2022, 1, 1), LocalDateTime.now(), "u1");
            t.setFhVigHasta(LocalDate.of(2022, 12, 31));
            t.setSiActiva(false);
            tarifarioRepo.save(t);

            Optional<FalTarifarioUnidadFaltas> res = tarifarioService.resolverUltimoVigente(
                    TipoUnidadFaltas.SALARIO, LocalDate.of(2024, 1, 1));
            assertThat(res).isEmpty();
        }

        @Test @DisplayName("aislamiento: save devuelve copia")
        void aislamiento() {
            FalTarifarioUnidadFaltas t = tarifarioService.crear(
                    TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("5000"), LocalDate.now(), null, "u1");
            FalTarifarioUnidadFaltas leido = tarifarioRepo.findById(t.getId()).orElseThrow();
            leido.setSiActiva(false);
            FalTarifarioUnidadFaltas relectura = tarifarioRepo.findById(t.getId()).orElseThrow();
            assertThat(relectura.isSiActiva()).isTrue();
        }

        @Test @DisplayName("secuencia ID incremental")
        void secuencia_id() {
            FalTarifarioUnidadFaltas t1 = tarifarioService.crear(TipoUnidadFaltas.SALARIO, new BigDecimal("100000"), LocalDate.of(2022,1,1), LocalDate.of(2022,12,31), "u1");
            FalTarifarioUnidadFaltas t2 = tarifarioService.crear(TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("5000"), LocalDate.of(2022,1,1), null, "u1");
            assertThat(t2.getId()).isGreaterThan(t1.getId());
        }

        @Test @DisplayName("obtener id inexistente lanza TarifarioNoEncontradoException")
        void obtener_inexistente() {
            assertThatThrownBy(() -> tarifarioService.obtener(999L)).isInstanceOf(TarifarioNoEncontradoException.class);
        }

        @Test @DisplayName("historico cerrado no aparece como vigente actual")
        void historico_no_vigente() {
            tarifarioService.crear(TipoUnidadFaltas.SALARIO, new BigDecimal("80000"),
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "u1");
            Optional<FalTarifarioUnidadFaltas> res = tarifarioService.resolverUltimoVigente(
                    TipoUnidadFaltas.SALARIO, LocalDate.of(2024, 1, 1));
            assertThat(res).isEmpty();
        }
    }

    // =========================================================
    // Medida Preventiva
    // =========================================================

    @Nested
    @DisplayName("8F11D-20: Medida Preventiva")
    class MedidaPreventivaTests {

        private InMemoryMedidaPreventivaRepository medidaRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private MedidaPreventivaService medidaService;

        @BeforeEach
        void setUp() {
            medidaRepo = new InMemoryMedidaPreventivaRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            medidaService = new MedidaPreventivaService(medidaRepo, articuloMedidaRepo);
        }

        @Test @DisplayName("crear primera versión con version=1")
        void crear_primera_version() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion(
                    "RETLIC", "Retención de licencia", null, null, null, true,
                    OrigenBloqueanteMaterial.DOCUMENTACION_RETENIDA, "u1");
            assertThat(m.getVersionMedida()).isEqualTo((short) 1);
            assertThat(m.isSiActiva()).isTrue();
            assertThat(m.getCodigo()).isEqualTo("RETLIC");
        }

        @Test @DisplayName("codigo normalizado a mayúsculas")
        void codigo_mayusculas() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion(
                    "retlic", "Retención", null, null, null, false, null, "u1");
            assertThat(m.getCodigo()).isEqualTo("RETLIC");
        }

        @Test @DisplayName("codigo max 12 caracteres lanza excepcion")
        void codigo_max_12() {
            assertThatThrownBy(() -> medidaService.crearPrimeraVersion(
                    "ABCDE12345XYZ", "desc", null, null, null, false, null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("no puede crear segunda primera version para mismo codigo")
        void no_duplicar_primera_version() {
            medidaService.crearPrimeraVersion("RETLIC", "Desc", null, null, null, false, null, "u1");
            assertThatThrownBy(() -> medidaService.crearPrimeraVersion("RETLIC", "Desc2", null, null, null, false, null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("crear nueva versión incrementa version y desactiva anterior")
        void nueva_version() {
            medidaService.crearPrimeraVersion("CLAUSURA", "Versión 1", null, null, null, false, null, "u1");
            FalMedidaPreventiva v2 = medidaService.crearNuevaVersion("CLAUSURA", "Versión 2", null, null, null, false, null, "u1");
            assertThat(v2.getVersionMedida()).isEqualTo((short) 2);
            assertThat(v2.isSiActiva()).isTrue();
            assertThat(medidaRepo.findActivaByCodigo("CLAUSURA")).isPresent().get().extracting(FalMedidaPreventiva::getVersionMedida).isEqualTo((short) 2);
        }

        @Test @DisplayName("solo una activa por codigo")
        void una_activa_por_codigo() {
            medidaService.crearPrimeraVersion("SECVEH", "V1", null, null, null, false, null, "u1");
            medidaService.crearNuevaVersion("SECVEH", "V2", null, null, null, false, null, "u1");
            long activas = medidaRepo.findVersionesByCodigo("SECVEH").stream().filter(FalMedidaPreventiva::isSiActiva).count();
            assertThat(activas).isEqualTo(1L);
        }

        @Test @DisplayName("tipoBloqueanteDefault requiere siPuedeBloquearCierre=true")
        void bloqueante_default_requiere_puede_bloquear() {
            FalMedidaPreventiva m = new FalMedidaPreventiva(1L, "TEST", (short) 1, "Test", LocalDateTime.now(), "u1");
            m.setSiPuedeBloquearCierre(false);
            assertThatThrownBy(() -> m.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.RODADO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("si no puede bloquear, tipoBloqueanteDefault es null")
        void sin_bloqueante_null() {
            FalMedidaPreventiva m = new FalMedidaPreventiva(1L, "TEST", (short) 1, "Test", LocalDateTime.now(), "u1");
            m.setSiPuedeBloquearCierre(true);
            m.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.RODADO);
            m.setSiPuedeBloquearCierre(false);
            assertThat(m.getTipoBloqueanteDefault()).isNull();
        }

        @Test @DisplayName("baja lógica desactiva la medida")
        void baja_logica() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("RETROD", "V1", null, null, null, false, null, "u1");
            medidaService.desactivar(m.getId(), "u1");
            assertThat(medidaRepo.findActivaByCodigo("RETROD")).isEmpty();
        }

        @Test @DisplayName("obtener id inexistente lanza excepcion")
        void obtener_inexistente() {
            assertThatThrownBy(() -> medidaService.obtenerPorId(999L)).isInstanceOf(MedidaPreventivaNoEncontradaException.class);
        }

        @Test @DisplayName("listarActivas solo incluye activas")
        void listar_activas() {
            medidaService.crearPrimeraVersion("AA", "A", null, null, null, false, null, "u1");
            FalMedidaPreventiva b = medidaService.crearPrimeraVersion("BB", "B", null, null, null, false, null, "u1");
            medidaService.desactivar(b.getId(), "u1");
            assertThat(medidaService.listarActivas()).hasSize(1).extracting(FalMedidaPreventiva::getCodigo).containsExactly("AA");
        }
    }

    // =========================================================
    // Artículo-Medida Preventiva
    // =========================================================

    @Nested
    @DisplayName("8F11D-30: Artículo-Medida Preventiva")
    class ArticuloMedidaTests {

        private InMemoryMedidaPreventivaRepository medidaRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private MedidaPreventivaService medidaService;

        @BeforeEach
        void setUp() {
            medidaRepo = new InMemoryMedidaPreventivaRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            medidaService = new MedidaPreventivaService(medidaRepo, articuloMedidaRepo);
        }

        @Test @DisplayName("vincular con PK compuesta")
        void vincular_pk_compuesta() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("RETLIC", "V1", null, null, null, false, null, "u1");
            FalArticuloMedidaPreventiva rel = medidaService.vincularArticulo(1L, m.getId(), true, "u1");
            assertThat(rel.getId()).isEqualTo(new ArticuloMedidaPreventivaId(1L, m.getId()));
            assertThat(rel.isSiObligatoria()).isTrue();
            assertThat(rel.isSiActiva()).isTrue();
        }

        @Test @DisplayName("no puede vincular medida inactiva")
        void no_vincular_medida_inactiva() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("CLAUSURA", "V1", null, null, null, false, null, "u1");
            medidaService.desactivar(m.getId(), "u1");
            assertThatThrownBy(() -> medidaService.vincularArticulo(1L, m.getId(), false, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("no puede duplicar relación activa")
        void no_duplicar() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("RETROD", "V1", null, null, null, false, null, "u1");
            medidaService.vincularArticulo(1L, m.getId(), false, "u1");
            assertThatThrownBy(() -> medidaService.vincularArticulo(1L, m.getId(), false, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("baja lógica de relación")
        void baja_logica_relacion() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("SECVEH", "V1", null, null, null, false, null, "u1");
            medidaService.vincularArticulo(1L, m.getId(), true, "u1");
            medidaService.desvincularArticulo(1L, m.getId(), "u1");
            assertThat(articuloMedidaRepo.existsActiva(new ArticuloMedidaPreventivaId(1L, m.getId()))).isFalse();
        }

        @Test @DisplayName("findActivasByArticuloId solo activas")
        void find_activas_por_articulo() {
            FalMedidaPreventiva m1 = medidaService.crearPrimeraVersion("M1", "M1", null, null, null, false, null, "u1");
            FalMedidaPreventiva m2 = medidaService.crearPrimeraVersion("M2", "M2", null, null, null, false, null, "u1");
            medidaService.vincularArticulo(1L, m1.getId(), true, "u1");
            medidaService.vincularArticulo(1L, m2.getId(), false, "u1");
            medidaService.desvincularArticulo(1L, m2.getId(), "u1");
            List<FalArticuloMedidaPreventiva> activas = articuloMedidaRepo.findActivasByArticuloId(1L);
            assertThat(activas).hasSize(1);
            assertThat(activas.get(0).getMedidaPreventivaId()).isEqualTo(m1.getId());
        }
    }

    // =========================================================
    // Artículo Imputado
    // =========================================================

    @Nested
    @DisplayName("8F11D-40: Artículo Imputado al Acta")
    class ArticuloImputadoTests {

        private InMemoryActaRepository actaRepo;
        private InMemoryNormativaRepository normativaRepo;
        private InMemoryActaArticuloInfringidoRepository articuloImputadoRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private NormativaService normativaService;
        private ActaArticuloInfringidoService svc;
        private FalActa acta;
        private FalNormativaFaltas normativa;
        private FalArticuloNormativaFaltas articulo;

        @BeforeEach
        void setUp() {
            actaRepo = new InMemoryActaRepository();
            normativaRepo = new InMemoryNormativaRepository();
            articuloImputadoRepo = new InMemoryActaArticuloInfringidoRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            normativaService = new NormativaService(normativaRepo, new InMemoryDependenciaRepository());
            svc = new ActaArticuloInfringidoService(articuloImputadoRepo, actaRepo, normativaRepo, articuloMedidaRepo);

            acta = actaRepo.guardar(new FalActa(actaRepo.nextId(), "uuid-1", TipoActa.TRANSITO, 1L, 1L,
                    LocalDate.now(), LocalDateTime.now(), "Calle 123", null, null, null,
                    ResultadoFirmaInfractor.FIRMADA, null, LocalDateTime.now(), "u1"));

            normativa = normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                    "ORD01", 1, "Ordenanza 01", null, LocalDate.now(), "u1"));

            articulo = normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                    normativa.getId(), "ART01", 1, "Artículo 1", null,
                    new BigDecimal("2"), ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad.SALARIO,
                    false, null, null, LocalDate.now(), "u1"));
        }

        @Test @DisplayName("imputar artículo válido")
        void imputar_valido() {
            FalActaArticuloInfringido imp = svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            assertThat(imp.getId()).isNotNull();
            assertThat(imp.isSiActivo()).isTrue();
            assertThat(imp.getMotivoBaja()).isNull();
        }

        @Test @DisplayName("no duplicar imputación activa")
        void no_duplicar_activa() {
            svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            assertThatThrownBy(() -> svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("artículo no pertenece a la normativa lanza excepcion")
        void articulo_no_pertenece_normativa() {
            FalNormativaFaltas otra = normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                    "ORD02", 1, "Otra", null, LocalDate.now(), "u1"));
            assertThatThrownBy(() -> svc.imputar(acta.getId(), otra.getId(), articulo.getId(), "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("baja lógica con motivo y fecha")
        void baja_logica() {
            FalActaArticuloInfringido imp = svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            FalActaArticuloInfringido baja = svc.darDeBaja(imp.getId(), MotivoBajaArticuloInfringido.ERROR_CARGA, "u1");
            assertThat(baja.isSiActivo()).isFalse();
            assertThat(baja.getMotivoBaja()).isEqualTo(MotivoBajaArticuloInfringido.ERROR_CARGA);
            assertThat(baja.getFhBaja()).isNotNull();
            assertThat(baja.getIdUserBaja()).isEqualTo("u1");
        }

        @Test @DisplayName("doble baja lanza excepcion")
        void doble_baja() {
            FalActaArticuloInfringido imp = svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            svc.darDeBaja(imp.getId(), MotivoBajaArticuloInfringido.ERROR_CARGA, "u1");
            assertThatThrownBy(() -> svc.darDeBaja(imp.getId(), MotivoBajaArticuloInfringido.ERROR_CARGA, "u1"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("corrección preserva historia y crea nueva fila")
        void correccion_nueva_fila() {
            FalArticuloNormativaFaltas art2 = normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                    normativa.getId(), "ART02", 1, "Artículo 2", null,
                    new BigDecimal("1"), ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad.UNIDAD_FIJA,
                    false, null, null, LocalDate.now(), "u1"));

            FalActaArticuloInfringido imp1 = svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            FalActaArticuloInfringido imp2 = svc.corregir(imp1.getId(), normativa.getId(), art2.getId(), "u1");

            assertThat(imp2.getId()).isNotEqualTo(imp1.getId());
            assertThat(imp2.isSiActivo()).isTrue();
            assertThat(articuloImputadoRepo.findById(imp1.getId()).orElseThrow().isSiActivo()).isFalse();
        }

        @Test @DisplayName("listarActivosPorActa solo devuelve activos")
        void listar_activos() {
            FalActaArticuloInfringido imp = svc.imputar(acta.getId(), normativa.getId(), articulo.getId(), "u1");
            svc.darDeBaja(imp.getId(), MotivoBajaArticuloInfringido.ANULACION_IMPUTACION, "u1");
            assertThat(svc.listarActivosPorActa(acta.getId())).isEmpty();
            assertThat(svc.listarTodosPorActa(acta.getId())).hasSize(1);
        }

        @Test @DisplayName("la imputación no contiene montos")
        void sin_montos() throws Exception {
            Class<?> c = FalActaArticuloInfringido.class;
            assertThatThrownBy(() -> c.getDeclaredField("monto")).isInstanceOf(NoSuchFieldException.class);
            assertThatThrownBy(() -> c.getDeclaredField("valorUnidad")).isInstanceOf(NoSuchFieldException.class);
            assertThatThrownBy(() -> c.getDeclaredField("montoBase")).isInstanceOf(NoSuchFieldException.class);
        }
    }

    // =========================================================
    // Valorización
    // =========================================================

    @Nested
    @DisplayName("8F11D-50: Valorización de Acta")
    class ValorizacionTests {

        private InMemoryActaRepository actaRepo;
        private InMemoryNormativaRepository normativaRepo;
        private InMemoryActaArticuloInfringidoRepository articuloImputadoRepo;
        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
        private InMemoryActaValorizacionRepository valorizacionRepo;
        private InMemoryActaValorizacionItemRepository itemRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private NormativaService normativaService;
        private ActaArticuloInfringidoService articuloService;
        private ValorizacionService valorizacionService;
        private FalActa acta;
        private FalNormativaFaltas normativa;
        private FalArticuloNormativaFaltas artSalario;

        @BeforeEach
        void setUp() {
            actaRepo = new InMemoryActaRepository();
            normativaRepo = new InMemoryNormativaRepository();
            articuloImputadoRepo = new InMemoryActaArticuloInfringidoRepository();
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
            valorizacionRepo = new InMemoryActaValorizacionRepository();
            itemRepo = new InMemoryActaValorizacionItemRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            normativaService = new NormativaService(normativaRepo, new InMemoryDependenciaRepository());
            articuloService = new ActaArticuloInfringidoService(articuloImputadoRepo, actaRepo, normativaRepo, articuloMedidaRepo);
            valorizacionService = new ValorizacionService(valorizacionRepo, itemRepo, articuloImputadoRepo, normativaRepo, tarifarioRepo, actaRepo);

            acta = actaRepo.guardar(new FalActa(actaRepo.nextId(), "uuid-1", TipoActa.TRANSITO, 1L, 1L,
                    LocalDate.now(), LocalDateTime.now(), "Calle 123", null, null, null,
                    ResultadoFirmaInfractor.FIRMADA, null, LocalDateTime.now(), "u1"));

            normativa = normativaService.crearNormativa(new CrearNormativaFaltasCommand(
                    "ORD01", 1, "Ordenanza 01", null, LocalDate.now(), "u1"));

            artSalario = normativaService.crearArticulo(new CrearArticuloNormativaFaltasCommand(
                    normativa.getId(), "ART01", 1, "Artículo 1", null,
                    new BigDecimal("3"), ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad.SALARIO,
                    false, null, null, LocalDate.now(), "u1"));

            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    tarifarioRepo.nextId(), TipoUnidadFaltas.SALARIO, new BigDecimal("100000"),
                    LocalDate.of(2020, 1, 1), LocalDateTime.now(), "sistema");
            tarifarioRepo.save(t);
        }

        @Test @DisplayName("calcular base preliminar con un artículo")
        void calcular_base_preliminar() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThat(val.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.PRELIMINAR);
            assertThat(val.isSiVigente()).isFalse();
            assertThat(val.getMontoFinal()).isEqualByComparingTo(new BigDecimal("300000.00"));
        }

        @Test @DisplayName("preliminar no desplaza confirmada vigente")
        void preliminar_no_desplaza_confirmada() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel1.getId(), "u1");

            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThat(prel2.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.PRELIMINAR);
            assertThat(prel2.isSiVigente()).isFalse();

            Optional<FalActaValorizacion> vigente = valorizacionService.consultarVigente(
                    acta.getId(), TipoValorizacionActa.INFRACCION_BASE);
            assertThat(vigente).isPresent();
            assertThat(vigente.get().getId()).isEqualTo(prel1.getId());
        }

        @Test @DisplayName("confirmar marca como CONFIRMADA y siVigente=true")
        void confirmar() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion confirmada = valorizacionService.confirmar(val.getId(), "u1");
            assertThat(confirmada.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.CONFIRMADA);
            assertThat(confirmada.isSiVigente()).isTrue();
            assertThat(confirmada.getFhConfirmacion()).isNotNull();
        }

        @Test @DisplayName("reemplazo: la anterior pasa a REEMPLAZADA y no vigente")
        void reemplazo() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion v1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion c1 = valorizacionService.confirmar(v1.getId(), "u1");

            FalActaValorizacion v2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(v2.getId(), "u1");

            FalActaValorizacion historica = valorizacionRepo.findById(c1.getId()).orElseThrow();
            assertThat(historica.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.REEMPLAZADA);
            assertThat(historica.isSiVigente()).isFalse();
        }

        @Test @DisplayName("una sola vigente por acta+tipo")
        void una_vigente_por_acta_tipo() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion v1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(v1.getId(), "u1");

            FalActaValorizacion v2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(v2.getId(), "u1");

            long vigentes = valorizacionRepo.findByActaIdAndTipo(acta.getId(), TipoValorizacionActa.INFRACCION_BASE)
                    .stream().filter(FalActaValorizacion::isSiVigente).count();
            assertThat(vigentes).isEqualTo(1L);
        }

        @Test @DisplayName("anulación pone estado ANULADA y siVigente=false")
        void anulacion() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(val.getId(), "u1");
            valorizacionService.anular(val.getId(), "u1");
            FalActaValorizacion anulada = valorizacionRepo.findById(val.getId()).orElseThrow();
            assertThat(anulada.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.ANULADA);
            assertThat(anulada.isSiVigente()).isFalse();
        }

        @Test @DisplayName("confirmar ya confirmada lanza excepcion")
        void confirmar_ya_confirmada() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(val.getId(), "u1");
            assertThatThrownBy(() -> valorizacionService.confirmar(val.getId(), "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("sin artículos activos, no puede calcular base")
        void sin_articulos() {
            assertThatThrownBy(() -> valorizacionService.calcularBasePreliminar(acta.getId(), "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("historial preserva todas las valorizaciones")
        void historial() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion v1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(v1.getId(), "u1");
            FalActaValorizacion v2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(v2.getId(), "u1");
            assertThat(valorizacionService.listarHistorial(acta.getId())).hasSize(2);
        }

        @Test @DisplayName("optimistic locking: versión incorrecta lanza ConcurrenciaConflictoException")
        void optimistic_locking() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion leida1 = valorizacionRepo.findById(val.getId()).orElseThrow();
            FalActaValorizacion leida2 = valorizacionRepo.findById(val.getId()).orElseThrow();
            leida1.setMontoFinal(new BigDecimal("200000"));
            valorizacionRepo.save(leida1);
            leida2.setMontoFinal(new BigDecimal("300000"));
            assertThatThrownBy(() -> valorizacionRepo.save(leida2)).isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test @DisplayName("escala montoFinal max 2 decimales")
        void escala_montoFinal() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThat(val.getMontoFinal().scale()).isLessThanOrEqualTo(2);
        }

        @Test @DisplayName("ítem manual requiere motivo")
        void item_manual_requiere_motivo() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThatThrownBy(() -> valorizacionService.agregarItemManual(
                    val.getId(), null, new BigDecimal("5000"), null, null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("OTRO_FUNDADO requiere documentoId")
        void otro_fundado_requiere_doc() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThatThrownBy(() -> valorizacionService.agregarItemManual(
                    val.getId(), null, new BigDecimal("5000"),
                    MotivoManualizacionValorizacion.OTRO_FUNDADO, null, "u1"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test @DisplayName("ítem manual agrega al historial de ítems")
        void item_manual_ok() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacionItem item = valorizacionService.agregarItemManual(
                    val.getId(), null, new BigDecimal("5000"),
                    MotivoManualizacionValorizacion.CRITERIO_AUTORIDAD, null, "u1");
            assertThat(item.isSiManual()).isTrue();
            assertThat(item.getMotivoManual()).isEqualTo(MotivoManualizacionValorizacion.CRITERIO_AUTORIDAD);
            assertThat(valorizacionService.listarItems(val.getId())).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test @DisplayName("seleccionarOperativa prefiere condena sobre infraccion base")
        void seleccionar_operativa_condena() {
            FalActaValorizacion condena = new FalActaValorizacion(
                    valorizacionRepo.nextId(), acta.getId(),
                    TipoValorizacionActa.CONDENA, OrigenValorizacion.JUEZ_ADMINISTRATIVO,
                    CriterioTarifario.MANUAL, new BigDecimal("500000"),
                    LocalDateTime.now(), "u1", LocalDateTime.now(), "u1");
            condena.setEstadoValorizacion(EstadoValorizacion.CONFIRMADA);
            condena.setSiVigente(true);
            valorizacionRepo.save(condena);

            Optional<FalActaValorizacion> op = valorizacionService.seleccionarOperativa(acta.getId());
            assertThat(op).isPresent();
            assertThat(op.get().getTipoValorizacionActa()).isEqualTo(TipoValorizacionActa.CONDENA);
        }

        @Test @DisplayName("FalActaValorizacionItem no tiene versionRow")
        void item_sin_versionRow() throws Exception {
            Class<?> c = FalActaValorizacionItem.class;
            assertThatThrownBy(() -> c.getDeclaredField("versionRow")).isInstanceOf(NoSuchFieldException.class);
        }

        @Test @DisplayName("versionRow empieza en 0 en alta")
        void versionRow_cero_en_alta() {
            articuloService.imputar(acta.getId(), normativa.getId(), artSalario.getId(), "u1");
            FalActaValorizacion val = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            assertThat(val.getVersionRow()).isEqualTo(0);
        }

        @Test @DisplayName("cantidades escala max 4 decimales")
        void cantidades_escala_4() {
            FalActaValorizacionItem item = new FalActaValorizacionItem(
                    1L, 1L, 1L, TipoValorizacionItem.AUTOMATICA, new BigDecimal("1000"), false);
            item.setCantidadUnidadesBase(new BigDecimal("2.123456"));
            assertThat(item.getCantidadUnidadesBase().scale()).isLessThanOrEqualTo(4);
        }

        @Test @DisplayName("ValorizacionNoEncontrada lanza excepcion")
        void valoracion_no_encontrada() {
            assertThatThrownBy(() -> valorizacionService.confirmar(999L, "u1"))
                    .isInstanceOf(ValorizacionNoEncontradaException.class);
        }
    }

    // =========================================================
    // FalActaSnapshot con campos de valorización
    // =========================================================

    @Nested
    @DisplayName("8F11D-60: Snapshot con campos de valorización")
    class SnapshotValorizacionTest {

        @Test @DisplayName("snapshot tiene campos valorizacionOperativaId y montoOperativoVigente")
        void snapshot_tiene_campos_valorizacion() throws Exception {
            Class<?> c = ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot.class;
            assertThat(c.getDeclaredField("valorizacionOperativaId")).isNotNull();
            assertThat(c.getDeclaredField("estadoValorizacionOperativa")).isNotNull();
            assertThat(c.getDeclaredField("tipoValorizacionOperativa")).isNotNull();
            assertThat(c.getDeclaredField("montoOperativoVigente")).isNotNull();
            assertThat(c.getDeclaredField("siMontoConfirmado")).isNotNull();
        }

        @Test @DisplayName("setter de montoOperativoVigente funciona correctamente")
        void setter_monto_operativo() {
            ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot snapshot =
                    new ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot(1L);
            snapshot.setMontoOperativoVigente(new BigDecimal("250000.00"));
            snapshot.setEstadoValorizacionOperativa(EstadoValorizacion.CONFIRMADA);
            snapshot.setTipoValorizacionOperativa(TipoValorizacionActa.INFRACCION_BASE);
            snapshot.setSiMontoConfirmado(true);
            assertThat(snapshot.getMontoOperativoVigente()).isEqualByComparingTo("250000.00");
            assertThat(snapshot.isSiMontoConfirmado()).isTrue();
        }
    }

    // =========================================================
    // Guardrails 8F-11D
    // =========================================================

    @Nested
    @DisplayName("8F11D-70: Guardrails estructurales")
    class GuardrailsTest {

        @Test @DisplayName("FalActaArticuloInfringido NO tiene campo versionRow")
        void infringido_sin_versionRow() throws Exception {
            assertThatThrownBy(() -> FalActaArticuloInfringido.class.getDeclaredField("versionRow"))
                    .isInstanceOf(NoSuchFieldException.class);
        }

        @Test @DisplayName("FalActaValorizacion tiene versionRow")
        void valorizacion_con_versionRow() throws Exception {
            assertThat(FalActaValorizacion.class.getDeclaredField("versionRow")).isNotNull();
        }

        @Test @DisplayName("FalTarifarioUnidadFaltas no tiene versionRow")
        void tarifario_sin_versionRow() throws Exception {
            assertThatThrownBy(() -> FalTarifarioUnidadFaltas.class.getDeclaredField("versionRow"))
                    .isInstanceOf(NoSuchFieldException.class);
        }

        @Test @DisplayName("ArticuloMedidaPreventivaId es record con campos articuloId y medidaPreventivaId")
        void articulo_medida_id_es_record() throws Exception {
            Class<?> c = ArticuloMedidaPreventivaId.class;
            assertThat(c.isRecord()).isTrue();
            assertThat(c.getDeclaredField("articuloId")).isNotNull();
            assertThat(c.getDeclaredField("medidaPreventivaId")).isNotNull();
        }

        @Test @DisplayName("FalActaValorizacion no tiene campo double ni float")
        void sin_double_float() throws Exception {
            for (java.lang.reflect.Field f : FalActaValorizacion.class.getDeclaredFields()) {
                assertThat(f.getType()).isNotIn(double.class, float.class, Double.class, Float.class);
            }
        }

        @Test @DisplayName("FalActaValorizacionItem no tiene campo double ni float")
        void item_sin_double_float() throws Exception {
            for (java.lang.reflect.Field f : FalActaValorizacionItem.class.getDeclaredFields()) {
                assertThat(f.getType()).isNotIn(double.class, float.class, Double.class, Float.class);
            }
        }
    }
}
