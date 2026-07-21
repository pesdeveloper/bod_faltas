package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.service.ActaContravencionService;
import ar.gob.malvinas.faltas.core.application.service.ActaMedidaPreventivaAplicadaService;
import ar.gob.malvinas.faltas.core.application.service.ActaSustanciasAlimenticiasService;
import ar.gob.malvinas.faltas.core.application.service.ActaTransitoService;
import ar.gob.malvinas.faltas.core.application.service.ActaVehiculoService;
import ar.gob.malvinas.faltas.core.application.service.RubroVersionService;
import ar.gob.malvinas.faltas.core.application.service.VehiculoMarcaService;
import ar.gob.malvinas.faltas.core.application.service.VehiculoModeloService;
import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoMedidaAplicada;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoCualitativoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadMedidaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.exception.ActaTransitoNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.ActaVehiculoNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoMarcaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.VehiculoModeloNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaContravencion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSustanciasAlimenticias;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransito;
import ar.gob.malvinas.faltas.core.domain.model.FalActaTransitoAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.model.FalActaVehiculo;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalRubroVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoMarca;
import ar.gob.malvinas.faltas.core.domain.model.FalVehiculoModelo;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSustanciasAlimenticiasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoAlcoholemiaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaVehiculoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryBloqueanteMaterialRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryRubroVersionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryVehiculoMarcaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryVehiculoModeloRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Suite de tests para el Slice 8F-11E: Satelites del Acta y Catalogos Vehiculo/Rubro.
 *
 * Cubre: 9 enums nuevos, 9 entidades satelite, repos InMemory, servicios de aplicacion,
 * restricciones de unicidad, invariantes de dominio, concurrencia y proyeccion de snapshot.
 */
@DisplayName("Slice 8F-11E: Satelites del Acta y Catalogos Vehiculo/Rubro")
class SatelitesCatalogosTest {

    // =========================================================
    // ENUMS
    // =========================================================

    @Nested
    @DisplayName("8F11E-01: TipoPruebaAlcoholemia")
    class TipoPruebaAlcoholemiaTest {
        @Test @DisplayName("2 valores exactos")
        void valores() { assertThat(TipoPruebaAlcoholemia.values()).hasSize(2); }
        @Test @DisplayName("ALOMETRO=1")
        void alometro() { assertThat(TipoPruebaAlcoholemia.fromCodigo((short) 1)).isEqualTo(TipoPruebaAlcoholemia.ALOMETRO); }
        @Test @DisplayName("ALCOHOLIMETRO=2")
        void alcoholimetro() { assertThat(TipoPruebaAlcoholemia.fromCodigo((short) 2)).isEqualTo(TipoPruebaAlcoholemia.ALCOHOLIMETRO); }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(TipoPruebaAlcoholemia.values()).map(TipoPruebaAlcoholemia::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(TipoPruebaAlcoholemia.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> TipoPruebaAlcoholemia.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
        @Test @DisplayName("no usa ordinal")
        void no_ordinal() {
            assertThat(TipoPruebaAlcoholemia.ALOMETRO.codigo()).isEqualTo((short) 1);
            assertThat(TipoPruebaAlcoholemia.ALOMETRO.ordinal()).isEqualTo(0);
            assertThat(TipoPruebaAlcoholemia.ALCOHOLIMETRO.ordinal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("8F11E-02: ResultadoCualitativoAlcoholemia")
    class ResultadoCualitativoAlcoholemiaTest {
        @Test @DisplayName("4 valores")
        void tamanio() { assertThat(ResultadoCualitativoAlcoholemia.values()).hasSize(4); }
        @Test @DisplayName("NEGATIVO=1, POSITIVO=2, INVALIDO=3, NO_REALIZADO=4")
        void valores() {
            assertThat(ResultadoCualitativoAlcoholemia.fromCodigo((short) 1)).isEqualTo(ResultadoCualitativoAlcoholemia.NEGATIVO);
            assertThat(ResultadoCualitativoAlcoholemia.fromCodigo((short) 2)).isEqualTo(ResultadoCualitativoAlcoholemia.POSITIVO);
            assertThat(ResultadoCualitativoAlcoholemia.fromCodigo((short) 3)).isEqualTo(ResultadoCualitativoAlcoholemia.INVALIDO);
            assertThat(ResultadoCualitativoAlcoholemia.fromCodigo((short) 4)).isEqualTo(ResultadoCualitativoAlcoholemia.NO_REALIZADO);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(ResultadoCualitativoAlcoholemia.values()).map(ResultadoCualitativoAlcoholemia::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(ResultadoCualitativoAlcoholemia.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> ResultadoCualitativoAlcoholemia.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-03: UnidadMedidaAlcoholemia")
    class UnidadMedidaAlcoholemiaTest {
        @Test @DisplayName("2 valores")
        void tamanio() { assertThat(UnidadMedidaAlcoholemia.values()).hasSize(2); }
        @Test @DisplayName("G_L=1, MG_L_AIRE=2")
        void valores() {
            assertThat(UnidadMedidaAlcoholemia.fromCodigo((short) 1)).isEqualTo(UnidadMedidaAlcoholemia.G_L);
            assertThat(UnidadMedidaAlcoholemia.fromCodigo((short) 2)).isEqualTo(UnidadMedidaAlcoholemia.MG_L_AIRE);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(UnidadMedidaAlcoholemia.values()).map(UnidadMedidaAlcoholemia::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(UnidadMedidaAlcoholemia.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> UnidadMedidaAlcoholemia.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-04: TipoVehiculo")
    class TipoVehiculoTest {
        @Test @DisplayName("10 valores")
        void tamanio() { assertThat(TipoVehiculo.values()).hasSize(10); }
        @Test @DisplayName("AUTO=1, MOTO=2, OTRO=10")
        void valores() {
            assertThat(TipoVehiculo.fromCodigo((short) 1)).isEqualTo(TipoVehiculo.AUTO);
            assertThat(TipoVehiculo.fromCodigo((short) 2)).isEqualTo(TipoVehiculo.MOTO);
            assertThat(TipoVehiculo.fromCodigo((short) 10)).isEqualTo(TipoVehiculo.OTRO);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(TipoVehiculo.values()).map(TipoVehiculo::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(TipoVehiculo.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> TipoVehiculo.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-05: EstadoGeneralVehiculo")
    class EstadoGeneralVehiculoTest {
        @Test @DisplayName("5 valores")
        void tamanio() { assertThat(EstadoGeneralVehiculo.values()).hasSize(5); }
        @Test @DisplayName("BUENO=1, REGULAR=2, MALO=3, SIN_VERIFICAR=4, NO_APLICA=5")
        void valores() {
            assertThat(EstadoGeneralVehiculo.fromCodigo((short) 1)).isEqualTo(EstadoGeneralVehiculo.BUENO);
            assertThat(EstadoGeneralVehiculo.fromCodigo((short) 5)).isEqualTo(EstadoGeneralVehiculo.NO_APLICA);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(EstadoGeneralVehiculo.values()).map(EstadoGeneralVehiculo::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(EstadoGeneralVehiculo.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> EstadoGeneralVehiculo.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-06: OrigenNomenclatura")
    class OrigenNomenclaturaTest {
        @Test @DisplayName("6 valores")
        void tamanio() { assertThat(OrigenNomenclatura.values()).hasSize(6); }
        @Test @DisplayName("CATASTRO=1, MANUAL_EXCEPCIONAL=6")
        void valores() {
            assertThat(OrigenNomenclatura.fromCodigo((short) 1)).isEqualTo(OrigenNomenclatura.CATASTRO);
            assertThat(OrigenNomenclatura.fromCodigo((short) 6)).isEqualTo(OrigenNomenclatura.MANUAL_EXCEPCIONAL);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(OrigenNomenclatura.values()).map(OrigenNomenclatura::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(OrigenNomenclatura.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> OrigenNomenclatura.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-07: MotivoNomenclaturaManual")
    class MotivoNomenclaturaManualTest {
        @Test @DisplayName("6 valores")
        void tamanio() { assertThat(MotivoNomenclaturaManual.values()).hasSize(6); }
        @Test @DisplayName("SIN_DATOS_CATASTRO=1, OTRO=6")
        void valores() {
            assertThat(MotivoNomenclaturaManual.fromCodigo((short) 1)).isEqualTo(MotivoNomenclaturaManual.SIN_DATOS_CATASTRO);
            assertThat(MotivoNomenclaturaManual.fromCodigo((short) 6)).isEqualTo(MotivoNomenclaturaManual.OTRO);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(MotivoNomenclaturaManual.values()).map(MotivoNomenclaturaManual::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(MotivoNomenclaturaManual.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> MotivoNomenclaturaManual.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-08: AmbitoCtv")
    class AmbitoCtvTest {
        @Test @DisplayName("6 valores")
        void tamanio() { assertThat(AmbitoCtv.values()).hasSize(6); }
        @Test @DisplayName("BALDIO=1, OTRO=6")
        void valores() {
            assertThat(AmbitoCtv.fromCodigo((short) 1)).isEqualTo(AmbitoCtv.BALDIO);
            assertThat(AmbitoCtv.fromCodigo((short) 6)).isEqualTo(AmbitoCtv.OTRO);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(AmbitoCtv.values()).map(AmbitoCtv::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(AmbitoCtv.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> AmbitoCtv.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    @Nested
    @DisplayName("8F11E-09: EstadoMedidaAplicada")
    class EstadoMedidaAplicadaTest {
        @Test @DisplayName("4 valores")
        void tamanio() { assertThat(EstadoMedidaAplicada.values()).hasSize(4); }
        @Test @DisplayName("APLICADA=1, LEVANTADA=2, ANULADA=3, CUMPLIDA=4")
        void valores() {
            assertThat(EstadoMedidaAplicada.fromCodigo((short) 1)).isEqualTo(EstadoMedidaAplicada.APLICADA);
            assertThat(EstadoMedidaAplicada.fromCodigo((short) 2)).isEqualTo(EstadoMedidaAplicada.LEVANTADA);
            assertThat(EstadoMedidaAplicada.fromCodigo((short) 3)).isEqualTo(EstadoMedidaAplicada.ANULADA);
            assertThat(EstadoMedidaAplicada.fromCodigo((short) 4)).isEqualTo(EstadoMedidaAplicada.CUMPLIDA);
        }
        @Test @DisplayName("codigos unicos")
        void codigos_unicos() {
            Set<Short> cs = Arrays.stream(EstadoMedidaAplicada.values()).map(EstadoMedidaAplicada::codigo).collect(Collectors.toSet());
            assertThat(cs).hasSize(EstadoMedidaAplicada.values().length);
        }
        @Test @DisplayName("desconocido lanza excepcion")
        void desconocido() { assertThatThrownBy(() -> EstadoMedidaAplicada.fromCodigo((short) 99)).isInstanceOf(IllegalArgumentException.class); }
    }

    // =========================================================
    // CATALOGO: MARCAS DE VEHICULO
    // =========================================================

    @Nested
    @DisplayName("8F11E-10: fal_vehiculo_marca")
    class VehiculoMarcaTests {

        private InMemoryVehiculoMarcaRepository repo;
        private VehiculoMarcaService servicio;

        @BeforeEach void setUp() {
            repo = new InMemoryVehiculoMarcaRepository();
            servicio = new VehiculoMarcaService(repo, FaltasClockTestSupport.FIXED);
        }

        @Test @DisplayName("alta valida retorna marca con siActivo=true")
        void alta_valida() {
            FalVehiculoMarca m = servicio.altaMarca("FORD", "Ford", "u1");
            assertThat(m.getId()).isNotNull();
            assertThat(m.getCodigo()).isEqualTo("FORD");
            assertThat(m.getNombre()).isEqualTo("Ford");
            assertThat(m.isSiActivo()).isTrue();
            assertThat(m.getFhAlta()).isNotNull();
        }

        @Test @DisplayName("codigo max 12 caracteres en entidad")
        void codigo_max_12() {
            assertThatThrownBy(() -> new FalVehiculoMarca(1L, "A".repeat(13), "Marca", FaltasClockTestSupport.FIXED.now(), "u1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("nombre max 24 caracteres en entidad")
        void nombre_max_24() {
            assertThatThrownBy(() -> new FalVehiculoMarca(1L, "COD", "A".repeat(25), FaltasClockTestSupport.FIXED.now(), "u1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("codigo duplicado lanza excepcion")
        void codigo_duplicado() {
            servicio.altaMarca("FORD", "Ford", "u1");
            assertThatThrownBy(() -> servicio.altaMarca("FORD", "Ford Otro", "u2"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("nombre duplicado lanza excepcion")
        void nombre_duplicado() {
            servicio.altaMarca("FORD", "Ford", "u1");
            assertThatThrownBy(() -> servicio.altaMarca("FORD2", "Ford", "u2"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("baja logica: siActivo pasa a false")
        void baja_logica() {
            FalVehiculoMarca m = servicio.altaMarca("FORD", "Ford", "u1");
            servicio.desactivar(m.getId(), "u2");
            assertThat(servicio.findById(m.getId()).isSiActivo()).isFalse();
        }

        @Test @DisplayName("findById inexistente lanza VehiculoMarcaNoEncontradaException")
        void findById_inexistente() {
            assertThatThrownBy(() -> servicio.findById(999L))
                    .isInstanceOf(VehiculoMarcaNoEncontradaException.class);
        }

        @Test @DisplayName("copia de entidad es independiente")
        void copia_independiente() {
            FalVehiculoMarca original = new FalVehiculoMarca(1L, "FORD", "Ford", FaltasClockTestSupport.FIXED.now(), "u1");
            FalVehiculoMarca copia = original.copia();
            copia.setSiActivo(false);
            assertThat(original.isSiActivo()).isTrue();
        }

        @Test @DisplayName("secuencia de IDs es incremental")
        void secuencia_ids() {
            FalVehiculoMarca m1 = servicio.altaMarca("A", "Alpha", "u1");
            FalVehiculoMarca m2 = servicio.altaMarca("B", "Beta", "u1");
            assertThat(m2.getId()).isGreaterThan(m1.getId());
        }

        @Test @DisplayName("concurrencia: alta duplicada solo crea una marca")
        void concurrencia_alta_duplicada() throws Exception {
            int threads = 5;
            CyclicBarrier barrier = new CyclicBarrier(threads);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                ts.add(new Thread(() -> {
                    try {
                        barrier.await();
                        servicio.altaMarca("CONCUR", "Concurrente", "u");
                        exitos.incrementAndGet();
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    }
                }));
            }
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join(5000);
            assertThat(exitos.get()).isEqualTo(1);
            assertThat(errores.get()).isEqualTo(threads - 1);
            assertThat(repo.findByCodigo("CONCUR")).isPresent();
        }
    }

    // =========================================================
    // CATALOGO: MODELOS DE VEHICULO
    // =========================================================

    @Nested
    @DisplayName("8F11E-20: fal_vehiculo_modelo")
    class VehiculoModeloTests {

        private InMemoryVehiculoMarcaRepository marcaRepo;
        private InMemoryVehiculoModeloRepository modeloRepo;
        private VehiculoMarcaService marcaService;
        private VehiculoModeloService modeloService;
        private FalVehiculoMarca ford;

        @BeforeEach void setUp() {
            marcaRepo = new InMemoryVehiculoMarcaRepository();
            modeloRepo = new InMemoryVehiculoModeloRepository();
            marcaService = new VehiculoMarcaService(marcaRepo, FaltasClockTestSupport.FIXED);
            modeloService = new VehiculoModeloService(modeloRepo, marcaRepo, FaltasClockTestSupport.FIXED);
            ford = marcaService.altaMarca("FORD", "Ford", "u1");
        }

        @Test @DisplayName("alta valida retorna modelo con siActivo=true")
        void alta_valida() {
            FalVehiculoModelo m = modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");
            assertThat(m.getId()).isNotNull();
            assertThat(m.getMarcaVehiculoId()).isEqualTo(ford.getId());
            assertThat(m.getCodigo()).isEqualTo("FOCUS");
            assertThat(m.isSiActivo()).isTrue();
        }

        @Test @DisplayName("codigo duplicado en misma marca lanza excepcion")
        void codigo_duplicado_misma_marca() {
            modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");
            assertThatThrownBy(() -> modeloService.altaModelo(ford.getId(), "FOCUS", "Focus 2", "u2"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("nombre duplicado en misma marca lanza excepcion")
        void nombre_duplicado_misma_marca() {
            modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");
            assertThatThrownBy(() -> modeloService.altaModelo(ford.getId(), "FOCUS2", "Focus", "u2"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("baja logica: siActivo pasa a false")
        void baja_logica() {
            FalVehiculoModelo m = modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");
            modeloService.desactivar(m.getId(), "u2");
            assertThat(modeloService.findById(m.getId()).isSiActivo()).isFalse();
        }

        @Test @DisplayName("findById inexistente lanza VehiculoModeloNoEncontradoException")
        void findById_inexistente() {
            assertThatThrownBy(() -> modeloService.findById(999L))
                    .isInstanceOf(VehiculoModeloNoEncontradoException.class);
        }

        @Test @DisplayName("marca inexistente lanza excepcion al crear modelo")
        void marca_inexistente() {
            assertThatThrownBy(() -> modeloService.altaModelo(999L, "FOCUS", "Focus", "u1"))
                    .isInstanceOf(VehiculoMarcaNoEncontradaException.class);
        }

        @Test @DisplayName("findActivasByMarca no incluye modelos inactivos")
        void findActivas_excluye_inactivos() {
            FalVehiculoModelo m1 = modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");
            FalVehiculoModelo m2 = modeloService.altaModelo(ford.getId(), "FIESTA", "Fiesta", "u1");
            modeloService.desactivar(m2.getId(), "u2");
            List<FalVehiculoModelo> activos = modeloRepo.findActivasByMarca(ford.getId());
            assertThat(activos).hasSize(1);
            assertThat(activos.get(0).getCodigo()).isEqualTo("FOCUS");
        }

        @Test @DisplayName("modelo inactivo sigue siendo legible por ID")
        void historico_inactivo_legible() {
            FalVehiculoModelo m = modeloService.altaModelo(ford.getId(), "LAGUNA", "Laguna", "u1");
            modeloService.desactivar(m.getId(), "u2");
            assertThat(modeloService.findById(m.getId()).getCodigo()).isEqualTo("LAGUNA");
        }
    }

    // =========================================================
    // RUBRO VERSIONADO
    // =========================================================

    @Nested
    @DisplayName("8F11E-30: fal_rubro_version")
    class RubroVersionTests {

        private InMemoryRubroVersionRepository repo;
        private RubroVersionService servicio;

        @BeforeEach void setUp() {
            repo = new InMemoryRubroVersionRepository(FaltasClockTestSupport.FIXED);
            servicio = new RubroVersionService(repo, FaltasClockTestSupport.FIXED);
        }

        @Test @DisplayName("alta inicial crea version actual siVersionActual=true")
        void alta_inicial() {
            FalRubroVersion r = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            assertThat(r.getRubroId()).isNotNull();
            assertThat(r.getIdRub()).isEqualTo(101);
            assertThat(r.getNombre()).isEqualTo("Kiosco");
            assertThat(r.isSiVersionActual()).isTrue();
            assertThat(r.getValidTo()).isNull();
            assertThat(r.isSiActivo()).isTrue();
        }

        @Test @DisplayName("sincronizar con mismo hash no crea nueva version")
        void unchanged_no_crea_version() {
            FalRubroVersion v1 = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            FalRubroVersion v2 = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            assertThat(v2.getRubroId()).isEqualTo(v1.getRubroId());
            assertThat(repo.findByIdRub(101)).hasSize(1);
        }

        @Test @DisplayName("cambio de nombre crea nueva version y cierra la anterior")
        void cambio_crea_version() {
            FalRubroVersion v1 = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            FalRubroVersion v2 = servicio.sincronizar(101, "Kiosco Nuevo", (short) 0, "u1");
            assertThat(v2.getRubroId()).isNotEqualTo(v1.getRubroId());
            assertThat(v2.isSiVersionActual()).isTrue();
            // Version anterior debe estar cerrada
            FalRubroVersion anterior = repo.findByRubroId(v1.getRubroId()).orElseThrow();
            assertThat(anterior.isSiVersionActual()).isFalse();
            assertThat(anterior.getValidTo()).isNotNull();
        }

        @Test @DisplayName("sidesabilitado != 0 => siActivo=false")
        void deshabilitado_siActivo_false() {
            FalRubroVersion r = servicio.sincronizar(999, "Actividad no habilitada", (short) 1, "u1");
            assertThat(r.isSiActivo()).isFalse();
        }

        @Test @DisplayName("findActualByIdRub devuelve solo version vigente")
        void findActualByIdRub() {
            servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            servicio.sincronizar(101, "Kiosco Nuevo", (short) 0, "u1");
            assertThat(servicio.findActualByIdRub(101)).isPresent();
            assertThat(servicio.findActualByIdRub(101).get().getNombre()).isEqualTo("Kiosco Nuevo");
        }

        @Test @DisplayName("findAllActualesActivas solo devuelve actuales y activos")
        void findAllActualesActivas() {
            servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            servicio.sincronizar(102, "Panaderia", (short) 0, "u1");
            servicio.sincronizar(999, "Inactivo", (short) 1, "u1");
            List<FalRubroVersion> activos = servicio.findAllActualesActivas();
            assertThat(activos).hasSize(2);
        }

        @Test @DisplayName("una sola version actual por IdRub")
        void una_sola_version_actual() {
            servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            servicio.sincronizar(101, "Kiosco v2", (short) 0, "u1");
            servicio.sincronizar(101, "Kiosco v3", (short) 0, "u1");
            long actuales = repo.findByIdRub(101).stream()
                    .filter(FalRubroVersion::isSiVersionActual).count();
            assertThat(actuales).isEqualTo(1);
        }

        @Test @DisplayName("validarCoherenciaRubroIdExterno: coherencia OK")
        void coherencia_ok() {
            FalRubroVersion v = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            servicio.validarCoherenciaRubroIdExterno(v.getRubroId(), 101);
        }

        @Test @DisplayName("validarCoherenciaRubroIdExterno: incoherente lanza excepcion")
        void coherencia_incoherente() {
            FalRubroVersion v = servicio.sincronizar(101, "Kiosco", (short) 0, "u1");
            assertThatThrownBy(() -> servicio.validarCoherenciaRubroIdExterno(v.getRubroId(), 999))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("concurrencia: solo una version actual por IdRub bajo carga")
        void concurrencia_una_version_actual() throws Exception {
            int threads = 8;
            CyclicBarrier barrier = new CyclicBarrier(threads);
            AtomicInteger exitos = new AtomicInteger(0);
            List<Thread> ts = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                ts.add(new Thread(() -> {
                    try {
                        barrier.await();
                        servicio.sincronizar(200, "Rubro " + idx, (short) 0, "u");
                        exitos.incrementAndGet();
                    } catch (Exception ignored) {}
                }));
            }
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join(5000);
            long actuales = repo.findByIdRub(200).stream()
                    .filter(FalRubroVersion::isSiVersionActual).count();
            assertThat(actuales).isEqualTo(1);
        }
    }

    // =========================================================
    // TRANSITO (satelite 1:1)
    // =========================================================

    @Nested
    @DisplayName("8F11E-40: fal_acta_transito")
    class ActaTransitoTests {

        private InMemoryActaTransitoRepository transitoRepo;
        private InMemoryActaTransitoAlcoholemiaRepository alcoholemiaRepo;
        private ActaTransitoService servicio;

        @BeforeEach void setUp() {
            transitoRepo = new InMemoryActaTransitoRepository();
            alcoholemiaRepo = new InMemoryActaTransitoAlcoholemiaRepository();
            servicio = new ActaTransitoService(transitoRepo, alcoholemiaRepo, FaltasClockTestSupport.FIXED);
        }

        @Test @DisplayName("registro valido: 1:1 por actaId")
        void registro_valido() {
            FalActaTransito t = servicio.registrarTransito(1L);
            assertThat(t.getActaId()).isEqualTo(1L);
        }

        @Test @DisplayName("segundo registro para mismo actaId lanza excepcion")
        void segundo_registro_rechazado() {
            servicio.registrarTransito(1L);
            assertThatThrownBy(() -> servicio.registrarTransito(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("findByActaId inexistente lanza ActaTransitoNoEncontradaException")
        void findByActaId_inexistente() {
            assertThatThrownBy(() -> servicio.findByActaId(999L))
                    .isInstanceOf(ActaTransitoNoEncontradaException.class);
        }

        @Test @DisplayName("si_ret_licencia y si_ret_vehiculo son false por defecto")
        void flags_false_por_defecto() {
            FalActaTransito t = servicio.registrarTransito(1L);
            assertThat(t.isSiRetLicencia()).isFalse();
            assertThat(t.isSiRetVehiculo()).isFalse();
            assertThat(t.isSiControlAlcoholemia()).isFalse();
        }

        @Test @DisplayName("copia es independiente del original")
        void copia_independiente() {
            FalActaTransito t = new FalActaTransito(1L);
            t.setNroLicencia("ABC123");
            FalActaTransito copia = t.copia();
            copia.setNroLicencia("MODIFICADA");
            assertThat(t.getNroLicencia()).isEqualTo("ABC123");
        }
    }

    // =========================================================
    // ALCOHOLEMIA
    // =========================================================

    @Nested
    @DisplayName("8F11E-41: fal_acta_transito_alcoholemia")
    class ActaTransitoAlcoholemiaTests {

        private InMemoryActaTransitoRepository transitoRepo;
        private InMemoryActaTransitoAlcoholemiaRepository alcoholemiaRepo;
        private ActaTransitoService servicio;

        @BeforeEach void setUp() {
            transitoRepo = new InMemoryActaTransitoRepository();
            alcoholemiaRepo = new InMemoryActaTransitoAlcoholemiaRepository();
            servicio = new ActaTransitoService(transitoRepo, alcoholemiaRepo, FaltasClockTestSupport.FIXED);
            servicio.registrarTransito(1L);
        }

        @Test @DisplayName("medicion valida se agrega con siResultadoFinal=false")
        void medicion_valida() {
            FalActaTransitoAlcoholemia m = servicio.agregarMedicion(1L, (short) 1,
                    TipoPruebaAlcoholemia.ALCOHOLIMETRO, ResultadoCualitativoAlcoholemia.NEGATIVO,
                    null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThat(m.getId()).isNotNull();
            assertThat(m.isSiResultadoFinal()).isFalse();
        }

        @Test @DisplayName("orden_medicion positivo unico por acta")
        void orden_unico_por_acta() {
            servicio.agregarMedicion(1L, (short) 1, TipoPruebaAlcoholemia.ALOMETRO, null,
                    null, null, null, null, null, "u1");
            assertThatThrownBy(() -> servicio.agregarMedicion(1L, (short) 1, TipoPruebaAlcoholemia.ALOMETRO, null,
                    null, null, null, null, null, "u1"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("resultado numerico sin unidad lanza excepcion en entidad")
        void numerico_sin_unidad() {
            FalActaTransitoAlcoholemia m = new FalActaTransitoAlcoholemia(1L, 1L, (short) 1,
                    TipoPruebaAlcoholemia.ALCOHOLIMETRO, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> m.setResultadoNumerico(new BigDecimal("0.25"), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("resultado numerico escala maxima 2")
        void escala_maxima_2() {
            FalActaTransitoAlcoholemia m = new FalActaTransitoAlcoholemia(1L, 1L, (short) 1,
                    TipoPruebaAlcoholemia.ALCOHOLIMETRO, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> m.setResultadoNumerico(new BigDecimal("0.123"), UnidadMedidaAlcoholemia.G_L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("una sola medicion final por acta")
        void una_sola_final() {
            FalActaTransitoAlcoholemia m1 = servicio.agregarMedicion(1L, (short) 1,
                    TipoPruebaAlcoholemia.ALCOHOLIMETRO, null, null, null, null, null, null, "u1");
            FalActaTransitoAlcoholemia m2 = servicio.agregarMedicion(1L, (short) 2,
                    TipoPruebaAlcoholemia.ALOMETRO, null, null, null, null, null, null, "u1");
            servicio.marcarResultadoFinal(1L, m1.getId());
            assertThat(alcoholemiaRepo.findResultadoFinalByActaId(1L)).isPresent();
            servicio.marcarResultadoFinal(1L, m2.getId());
            // Solo la segunda debe ser final
            assertThat(alcoholemiaRepo.findResultadoFinalByActaId(1L).get().getId()).isEqualTo(m2.getId());
            long finales = alcoholemiaRepo.findByActaId(1L).stream()
                    .filter(FalActaTransitoAlcoholemia::isSiResultadoFinal).count();
            assertThat(finales).isEqualTo(1);
        }

        @Test @DisplayName("concurrencia: una sola medicion final bajo carga")
        void concurrencia_resultado_final() throws Exception {
            List<Long> ids = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                FalActaTransitoAlcoholemia m = servicio.agregarMedicion(1L, (short) i,
                        TipoPruebaAlcoholemia.ALCOHOLIMETRO, null, null, null, null, null, null, "u1");
                ids.add(m.getId());
            }
            int threads = ids.size();
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Thread> ts = new ArrayList<>();
            for (Long mid : ids) {
                ts.add(new Thread(() -> {
                    try {
                        barrier.await();
                        alcoholemiaRepo.marcarResultadoFinalAtomicamente(1L, mid);
                    } catch (Exception ignored) {}
                }));
            }
            ts.forEach(Thread::start);
            for (Thread t : ts) t.join(5000);
            long finales = alcoholemiaRepo.findByActaId(1L).stream()
                    .filter(FalActaTransitoAlcoholemia::isSiResultadoFinal).count();
            assertThat(finales).isEqualTo(1);
        }
    }

    // =========================================================
    // VEHICULO (satelite 1:1)
    // =========================================================

    @Nested
    @DisplayName("8F11E-50: fal_acta_vehiculo")
    class ActaVehiculoTests {

        private InMemoryActaVehiculoRepository vehiculoRepo;
        private InMemoryVehiculoMarcaRepository marcaRepo;
        private InMemoryVehiculoModeloRepository modeloRepo;
        private VehiculoMarcaService marcaService;
        private VehiculoModeloService modeloService;
        private ActaVehiculoService servicio;

        @BeforeEach void setUp() {
            vehiculoRepo = new InMemoryActaVehiculoRepository();
            marcaRepo = new InMemoryVehiculoMarcaRepository();
            modeloRepo = new InMemoryVehiculoModeloRepository();
            marcaService = new VehiculoMarcaService(marcaRepo, FaltasClockTestSupport.FIXED);
            modeloService = new VehiculoModeloService(modeloRepo, marcaRepo, FaltasClockTestSupport.FIXED);
            servicio = new ActaVehiculoService(vehiculoRepo, marcaRepo, modeloRepo);
        }

        @Test @DisplayName("vehiculo textual sin marca normalizada se acepta")
        void fallback_textual() {
            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            v.setTipoVehiculo(TipoVehiculo.AUTO);
            v.setMarca(null, "Marca Desconocida");
            v.setModelo(null, "Modelo Desconocido");
            FalActaVehiculo guardado = servicio.registrarVehiculo(1L, v);
            assertThat(guardado.getMarcaVehiculoId()).isNull();
            assertThat(guardado.getMarcaVehiculoTxt()).isEqualTo("Marca Desconocida");
        }

        @Test @DisplayName("modelo de otra marca rechazado")
        void modelo_otra_marca_rechazado() {
            FalVehiculoMarca ford = marcaService.altaMarca("FORD", "Ford", "u1");
            FalVehiculoMarca toyota = marcaService.altaMarca("TOYOTA", "Toyota", "u1");
            FalVehiculoModelo corolla = modeloService.altaModelo(toyota.getId(), "COROLLA", "Corolla", "u1");

            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            v.setMarca(ford.getId(), "Ford");
            v.setModelo(corolla.getId(), "Corolla");
            assertThatThrownBy(() -> servicio.registrarVehiculo(1L, v))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("modelo y marca correctos aceptados")
        void marca_modelo_correctos() {
            FalVehiculoMarca ford = marcaService.altaMarca("FORD", "Ford", "u1");
            FalVehiculoModelo focus = modeloService.altaModelo(ford.getId(), "FOCUS", "Focus", "u1");

            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            v.setMarca(ford.getId(), "Ford");
            v.setModelo(focus.getId(), "Focus");
            FalActaVehiculo guardado = servicio.registrarVehiculo(1L, v);
            assertThat(guardado.getMarcaVehiculoId()).isEqualTo(ford.getId());
            assertThat(guardado.getModeloVehiculoId()).isEqualTo(focus.getId());
        }

        @Test @DisplayName("segundo vehiculo para mismo actaId rechazado")
        void segundo_vehiculo_rechazado() {
            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            servicio.registrarVehiculo(1L, v);
            FalActaVehiculo v2 = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> servicio.registrarVehiculo(1L, v2))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("findByActaId inexistente lanza ActaVehiculoNoEncontradoException")
        void findByActaId_inexistente() {
            assertThatThrownBy(() -> servicio.findByActaId(999L))
                    .isInstanceOf(ActaVehiculoNoEncontradoException.class);
        }

        @Test @DisplayName("dominio max 10 caracteres en entidad")
        void dominio_max_10() {
            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> v.setDominioVehiculo("ABC1234567X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("marca inactiva puede ser referenciada historicamente")
        void marca_inactiva_referencia_historica() {
            FalVehiculoMarca renault = marcaService.altaMarca("RENAULT", "Renault", "u1");
            FalVehiculoModelo laguna = modeloService.altaModelo(renault.getId(), "LAGUNA", "Laguna", "u1");
            marcaService.desactivar(renault.getId(), "u2");

            FalActaVehiculo v = new FalActaVehiculo(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            v.setMarca(renault.getId(), "Renault");
            v.setModelo(laguna.getId(), "Laguna");
            FalActaVehiculo guardado = servicio.registrarVehiculo(1L, v);
            assertThat(guardado.getMarcaVehiculoId()).isEqualTo(renault.getId());
        }
    }

    // =========================================================
    // CONTRAVENCION
    // =========================================================

    @Nested
    @DisplayName("8F11E-60: fal_acta_contravencion")
    class ActaContravencionTests {

        private InMemoryActaContravencionRepository repo;
        private InMemoryRubroVersionRepository rubroRepo;
        private ActaContravencionService servicio;
        private RubroVersionService rubroService;

        @BeforeEach void setUp() {
            repo = new InMemoryActaContravencionRepository();
            rubroRepo = new InMemoryRubroVersionRepository(FaltasClockTestSupport.FIXED);
            servicio = new ActaContravencionService(repo, rubroRepo);
            rubroService = new RubroVersionService(rubroRepo, FaltasClockTestSupport.FIXED);
        }

        @Test @DisplayName("registro valido con nomenclatura controlada")
        void registro_valido_controlado() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setSujetoInmueble(1, 100);
            FalActaContravencion guardado = servicio.registrar(1L, ctv);
            assertThat(guardado.getActaId()).isEqualTo(1L);
            assertThat(guardado.getIdBieI()).isEqualTo(100);
        }

        @Test @DisplayName("manual excepcional sin idBieI: si_nomenclatura_manual=true")
        void manual_excepcional() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.MANUAL_EXCEPCIONAL, true, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setMotivoNomenclaturaManual(MotivoNomenclaturaManual.SIN_DATOS_CATASTRO);
            assertThat(ctv.isSiNomenclaturaManual()).isTrue();
            assertThat(ctv.getOrigenNomencl()).isEqualTo(OrigenNomenclatura.MANUAL_EXCEPCIONAL);
        }

        @Test @DisplayName("si_nomenclatura_manual=true y origen != MANUAL_EXCEPCIONAL lanza excepcion")
        void manual_true_con_origen_incorrecto() {
            assertThatThrownBy(() -> new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, true, FaltasClockTestSupport.FIXED.now(), "u1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("pares Id_Suj_c e Id_Bie_c deben informarse juntos")
        void pares_suj_bie_c_juntos() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> ctv.setSujetoComercio(2, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("ambito OTRO exige texto")
        void ambito_otro_exige_texto() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> ctv.setAmbito(AmbitoCtv.OTRO, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("ambito no OTRO exige texto null")
        void ambito_no_otro_texto_null() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> ctv.setAmbito(AmbitoCtv.COMERCIO, "No deberia"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("ambito_ctv_txt max 80 caracteres")
        void ambito_ctv_txt_max_80() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> ctv.setAmbito(AmbitoCtv.OTRO, "A".repeat(81)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("secc como CHAR(2) - longitud max 2")
        void secc_max_2() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> ctv.setSecc("ABC"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("rubro coherente: rubroId e idRub coinciden")
        void rubro_coherente() {
            FalRubroVersion rub = rubroService.sincronizar(101, "Kiosco", (short) 0, "u1");
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setRubro(rub.getRubroId(), rub.getIdRub());
            FalActaContravencion guardado = servicio.registrar(1L, ctv);
            assertThat(guardado.getRubroId()).isEqualTo(rub.getRubroId());
        }

        @Test @DisplayName("rubro incoherente: rubroId no corresponde a idRub lanza excepcion")
        void rubro_incoherente() {
            FalRubroVersion rub = rubroService.sincronizar(101, "Kiosco", (short) 0, "u1");
            FalActaContravencion ctv = new FalActaContravencion(
                    2L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setRubro(rub.getRubroId(), 999);
            assertThatThrownBy(() -> servicio.registrar(2L, ctv))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("segundo registro para mismo actaId rechazado")
        void segundo_registro_rechazado() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            servicio.registrar(1L, ctv);
            FalActaContravencion ctv2 = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> servicio.registrar(1L, ctv2))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("generarNomenclaturaResumen produce texto no vacio con datos cargados")
        void nomenclatura_resumen_con_datos() {
            FalActaContravencion ctv = new FalActaContravencion(
                    1L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setSujetoInmueble(1, 100);
            ctv.setCirc((short) 1);
            ctv.setSecc("01");
            assertThat(ctv.generarNomenclaturaResumen()).isNotBlank();
        }
    }

    // =========================================================
    // SUSTANCIAS ALIMENTICIAS
    // =========================================================

    @Nested
    @DisplayName("8F11E-70: fal_acta_sustancias_alimenticias")
    class ActaSustanciasTests {

        private InMemoryActaSustanciasAlimenticiasRepository repo;
        private InMemoryRubroVersionRepository rubroRepo;
        private ActaSustanciasAlimenticiasService servicio;
        private RubroVersionService rubroService;

        @BeforeEach void setUp() {
            repo = new InMemoryActaSustanciasAlimenticiasRepository();
            rubroRepo = new InMemoryRubroVersionRepository(FaltasClockTestSupport.FIXED);
            servicio = new ActaSustanciasAlimenticiasService(repo, rubroRepo);
            rubroService = new RubroVersionService(rubroRepo, FaltasClockTestSupport.FIXED);
        }

        @Test @DisplayName("registro valido con rubro y ambito")
        void registro_valido() {
            FalRubroVersion rub = rubroService.sincronizar(301, "Restaurante", (short) 0, "u1");
            FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            s.setRubro(rub.getRubroId(), rub.getIdRub());
            s.setAmbito(AmbitoCtv.COMERCIO, null);
            s.setDescripcionSustancias("Alimentos en mal estado");
            FalActaSustanciasAlimenticias guardado = servicio.registrar(1L, s);
            assertThat(guardado.getActaId()).isEqualTo(1L);
            assertThat(guardado.getRubroId()).isEqualTo(rub.getRubroId());
        }

        @Test @DisplayName("rubro coherente: rubroId e idRub deben coincidir")
        void rubro_coherente() {
            FalRubroVersion rub = rubroService.sincronizar(301, "Restaurante", (short) 0, "u1");
            FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(2L, FaltasClockTestSupport.FIXED.now(), "u1");
            s.setRubro(rub.getRubroId(), 999);
            assertThatThrownBy(() -> servicio.registrar(2L, s))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("ambito OTRO exige texto")
        void ambito_otro_exige_texto() {
            FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> s.setAmbito(AmbitoCtv.OTRO, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("descripcion larga se acepta")
        void descripcion_larga() {
            FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            String desc = "Descripcion sanitaria larga con detalles tecnicos sobre los alimentos inspeccionados. ".repeat(10);
            s.setDescripcionSustancias(desc);
            assertThat(s.getDescripcionSustancias()).isEqualTo(desc);
        }

        @Test @DisplayName("segundo registro para mismo actaId rechazado")
        void segundo_registro_rechazado() {
            FalActaSustanciasAlimenticias s1 = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            servicio.registrar(1L, s1);
            FalActaSustanciasAlimenticias s2 = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThatThrownBy(() -> servicio.registrar(1L, s2))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("historico de rubro versión antigua sigue siendo valido")
        void historico_rubro_valido() {
            FalRubroVersion v1 = rubroService.sincronizar(301, "Restaurante", (short) 0, "u1");
            rubroService.sincronizar(301, "Restaurante Nuevo", (short) 0, "u1");
            // v1 es historica pero sigue siendo valida para referencia
            FalActaSustanciasAlimenticias s = new FalActaSustanciasAlimenticias(1L, FaltasClockTestSupport.FIXED.now(), "u1");
            s.setRubro(v1.getRubroId(), v1.getIdRub());
            FalActaSustanciasAlimenticias guardado = servicio.registrar(1L, s);
            assertThat(guardado.getRubroId()).isEqualTo(v1.getRubroId());
        }
    }

    // =========================================================
    // MEDIDA PREVENTIVA APLICADA
    // =========================================================

    @Nested
    @DisplayName("8F11E-80: fal_acta_medida_preventiva")
    class ActaMedidaPreventivaTests {

        private InMemoryActaMedidaPreventivaRepository medidaRepo;
        private InMemoryActaArticuloInfringidoRepository articuloRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private InMemoryMedidaPreventivaRepository catalogoRepo;
        private InMemoryBloqueanteMaterialRepository bloqueanteMaterialRepo;
        private ActaMedidaPreventivaAplicadaService servicio;

        @BeforeEach void setUp() {
            medidaRepo = new InMemoryActaMedidaPreventivaRepository();
            articuloRepo = new InMemoryActaArticuloInfringidoRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            catalogoRepo = new InMemoryMedidaPreventivaRepository();
            bloqueanteMaterialRepo = new InMemoryBloqueanteMaterialRepository();
            servicio = new ActaMedidaPreventivaAplicadaService(
                    medidaRepo, articuloRepo, articuloMedidaRepo, catalogoRepo, bloqueanteMaterialRepo, FaltasClockTestSupport.FIXED);

            // Setup catalogo: medida preventiva
            FalMedidaPreventiva mp = new FalMedidaPreventiva(10L, "MP001", (short) 1,
                    "Decomiso provisional", FaltasClockTestSupport.FIXED.now(), "u1");
            mp.setSiPuedeBloquearCierre(true);
            mp.setTipoBloqueanteDefault(OrigenBloqueanteMaterial.MEDIDA_PREVENTIVA);
            catalogoRepo.save(mp);

            // Setup: articulo del acta (actaId=1, articuloId=20, actaArticuloId=100)
            FalActaArticuloInfringido art = new FalActaArticuloInfringido(
                    100L, 1L, 5L, 20L, FaltasClockTestSupport.FIXED.now(), "u1");
            articuloRepo.save(art);

            // Setup: relacion articulo-medida valida
            ArticuloMedidaPreventivaId rel = new ArticuloMedidaPreventivaId(20L, 10L);
            FalArticuloMedidaPreventiva amp = new FalArticuloMedidaPreventiva(
                    rel, false, FaltasClockTestSupport.FIXED.now(), "u1");
            articuloMedidaRepo.save(amp);
        }

        @Test @DisplayName("aplicar medida valida crea medida con estado APLICADA")
        void aplicar_medida_valida() {
            FalActaMedidaPreventiva m = servicio.aplicarMedida(1L, 100L, 10L, false, "Decomiso", "u1");
            assertThat(m.getId()).isNotNull();
            assertThat(m.getEstadoMedida()).isEqualTo(EstadoMedidaAplicada.APLICADA);
        }

        @Test @DisplayName("articulo de otro acta rechazado")
        void articulo_otro_acta_rechazado() {
            assertThatThrownBy(() -> servicio.aplicarMedida(999L, 100L, 10L, false, null, "u1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("medida no aplicable al articulo rechazada")
        void medida_no_aplicable_rechazada() {
            assertThatThrownBy(() -> servicio.aplicarMedida(1L, 100L, 999L, false, null, "u1"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("si genera bloqueante, se crea FalBloqueanteMaterial")
        void crea_bloqueante() {
            servicio.aplicarMedida(1L, 100L, 10L, true, "Decomiso", "u1");
            assertThat(bloqueanteMaterialRepo.findByActaId(1L)).hasSize(1);
        }

        @Test @DisplayName("sin bloqueante, no crea FalBloqueanteMaterial")
        void sin_bloqueante_no_crea() {
            servicio.aplicarMedida(1L, 100L, 10L, false, "Decomiso", "u1");
            assertThat(bloqueanteMaterialRepo.findByActaId(1L)).isEmpty();
        }

        @Test @DisplayName("transicion LEVANTADA resuelve bloqueante activo")
        void levantar_resuelve_bloqueante() {
            FalActaMedidaPreventiva m = servicio.aplicarMedida(1L, 100L, 10L, true, "Decomiso", "u1");
            assertThat(bloqueanteMaterialRepo.findByActaId(1L)).hasSize(1);
            assertThat(bloqueanteMaterialRepo.findByActaId(1L).get(0).isSiActivo()).isTrue();
            servicio.transicionarEstado(m.getId(), EstadoMedidaAplicada.LEVANTADA, "u1");
            assertThat(bloqueanteMaterialRepo.findByActaId(1L).get(0).isSiActivo()).isFalse();
        }

        @Test @DisplayName("transicion CUMPLIDA resuelve bloqueante")
        void cumplida_resuelve_bloqueante() {
            FalActaMedidaPreventiva m = servicio.aplicarMedida(1L, 100L, 10L, true, "Decomiso", "u1");
            servicio.transicionarEstado(m.getId(), EstadoMedidaAplicada.CUMPLIDA, "u1");
            assertThat(bloqueanteMaterialRepo.findByActaId(1L).get(0).isSiActivo()).isFalse();
        }

        @Test @DisplayName("historial: medidas previas no se borran")
        void historial_conservado() {
            FalActaMedidaPreventiva m = servicio.aplicarMedida(1L, 100L, 10L, false, "Decomiso", "u1");
            servicio.transicionarEstado(m.getId(), EstadoMedidaAplicada.ANULADA, "u1");
            assertThat(servicio.findByActaId(1L)).hasSize(1);
            assertThat(servicio.findByActaId(1L).get(0).getEstadoMedida()).isEqualTo(EstadoMedidaAplicada.ANULADA);
        }

        @Test @DisplayName("articulo inactivo rechazado")
        void articulo_inactivo_rechazado() {
            FalActaArticuloInfringido articuloRepo2 = articuloRepo.findById(100L).orElseThrow();
            articuloRepo2.darDeBaja(ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido.CORRECCION_IMPUTACION,
                    FaltasClockTestSupport.FIXED.now(), "u1");
            articuloRepo.save(articuloRepo2);
            assertThatThrownBy(() -> servicio.aplicarMedida(1L, 100L, 10L, false, null, "u1"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // =========================================================
    // SNAPSHOT: PROYECCION DE SATELITES
    // =========================================================

    @Nested
    @DisplayName("8F11E-90: Snapshot - proyeccion satelites")
    class SnapshotProyeccionTests {

        private SnapshotRecalculador recalculador;
        private InMemoryActaTransitoRepository transitoRepo;
        private InMemoryActaContravencionRepository contravencionRepo;
        private InMemoryActaSnapshotRepository snapshotRepo;

        private FalActa crearActa(Long id, TipoActa tipo) {
            return new FalActa(id, "uuid-" + id, tipo, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle Falsa 123",
                    null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                    FaltasClockTestSupport.FIXED.now(), "u1");
        }

        @BeforeEach void setUp() throws Exception {
            snapshotRepo = new InMemoryActaSnapshotRepository();
            transitoRepo = new InMemoryActaTransitoRepository();
            contravencionRepo = new InMemoryActaContravencionRepository();

            recalculador = new SnapshotRecalculador(
                    new InMemoryActaEventoRepository(),
                    new InMemoryDocumentoRepository(),
                    new InMemoryNotificacionRepository(),
                    new InMemoryPagoVoluntarioRepository(),
                    new InMemoryFalloActaRepository(),
                    new InMemoryApelacionActaRepository(),
                    new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);

            // Inyectar repos satelite via reflexion (son @Autowired required=false)
            Field fTransito = SnapshotRecalculador.class.getDeclaredField("actaTransitoRepository");
            fTransito.setAccessible(true);
            fTransito.set(recalculador, transitoRepo);

            Field fContravencion = SnapshotRecalculador.class.getDeclaredField("actaContravencionRepository");
            fContravencion.setAccessible(true);
            fContravencion.set(recalculador, contravencionRepo);
        }

        @Test @DisplayName("snapshot de acta transito proyecta licenciaProvinciaTxt")
        void proyecta_licencia_provincia() {
            FalActa acta = crearActa(1L, TipoActa.TRANSITO);
            FalActaTransito t = new FalActaTransito(1L);
            t.setLicenciaTerritorio((short) 2, ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo.MUNICIPIO, 60515);
            transitoRepo.guardar(t);

            FalActaSnapshot snap = recalculador.recalcular(acta);
            assertThat(snap.getLicenciaProvinciaTxt()).isNotNull();
        }

        @Test @DisplayName("snapshot sin satelite transito: licenciaProvinciaTxt null")
        void sin_transito_null() {
            FalActa acta = crearActa(2L, TipoActa.TRANSITO);
            FalActaSnapshot snap = recalculador.recalcular(acta);
            assertThat(snap.getLicenciaProvinciaTxt()).isNull();
        }

        @Test @DisplayName("snapshot de acta contravencion proyecta idBieI e idBieC")
        void proyecta_idBie() {
            FalActa acta = crearActa(3L, TipoActa.CONTRAVENCION);
            FalActaContravencion ctv = new FalActaContravencion(
                    3L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setSujetoInmueble(1, 500);
            ctv.setSujetoComercio(2, 600);
            contravencionRepo.guardar(ctv);

            FalActaSnapshot snap = recalculador.recalcular(acta);
            assertThat(snap.getIdBieI()).isEqualTo(500);
            assertThat(snap.getIdBieC()).isEqualTo(600);
        }

        @Test @DisplayName("snapshot de acta contravencion con nomenclatura proyecta nomenclaturaResumen")
        void proyecta_nomenclatura_resumen() {
            FalActa acta = crearActa(4L, TipoActa.CONTRAVENCION);
            FalActaContravencion ctv = new FalActaContravencion(
                    4L, OrigenNomenclatura.CATASTRO, false, FaltasClockTestSupport.FIXED.now(), "u1");
            ctv.setSujetoInmueble(1, 700);
            ctv.setCirc((short) 1);
            ctv.setSecc("02");
            contravencionRepo.guardar(ctv);

            FalActaSnapshot snap = recalculador.recalcular(acta);
            assertThat(snap.getNomenclaturaResumen()).isNotBlank();
        }

        @Test @DisplayName("snapshot sin satelite contravencion: idBieI null")
        void sin_contravencion_null() {
            FalActa acta = crearActa(5L, TipoActa.CONTRAVENCION);
            FalActaSnapshot snap = recalculador.recalcular(acta);
            assertThat(snap.getIdBieI()).isNull();
            assertThat(snap.getIdBieC()).isNull();
            assertThat(snap.getNomenclaturaResumen()).isNull();
        }
    }
}
