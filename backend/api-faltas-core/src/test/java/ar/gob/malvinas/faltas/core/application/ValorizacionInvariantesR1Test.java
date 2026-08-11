package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.ValorizacionCalculador;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes del rediseño de valorizacion (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7):
 * cabecera unica por acta, historial versionado por articulo, un solo item
 * actual por articulo, MONTO sin valor de unidad, pago voluntario opcional.
 */
@DisplayName("Invariantes de valorizacion: cabecera unica + historial por articulo (Wave 4 PARTE-2)")
class ValorizacionInvariantesR1Test {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 1);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaArticuloInfringidoRepository articuloInfringidoRepo;
    private InMemoryNormativaRepository normativaRepo;
    private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
    private InMemoryValorUnidadFaltasRepository valorUnidadRepo;
    private InMemoryActaValorizacionRepository valorizacionRepo;
    private InMemoryActaValorizacionItemRepository itemRepo;
    private ValorizacionService valorizacionService;

    private Long articuloSalarioId;
    private Long articuloMontoId;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        articuloInfringidoRepo = new InMemoryActaArticuloInfringidoRepository();
        normativaRepo = new InMemoryNormativaRepository();
        tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
        valorUnidadRepo = new InMemoryValorUnidadFaltasRepository();
        valorizacionRepo = new InMemoryActaValorizacionRepository();
        itemRepo = new InMemoryActaValorizacionItemRepository();

        valorizacionService = new ValorizacionService(
                valorizacionRepo, itemRepo, articuloInfringidoRepo, normativaRepo,
                tarifarioRepo, valorUnidadRepo, actaRepo, new ValorizacionCalculador(),
                FaltasClockTestSupport.FIXED);

        FalNormativaFaltas normativa = new FalNormativaFaltas(
                1L, "ORCV", 1, "Ordenanza", HOY.minusYears(2), FaltasClockTestSupport.FIXED.now(), "sistema");
        normativaRepo.guardarNormativa(normativa);

        FalArticuloNormativaFaltas artSalario = new FalArticuloNormativaFaltas(
                1L, 1L, "ART-SAL", 1, "Articulo salario", TipoInfraccion.TRANSITO,
                HOY.minusYears(2), FaltasClockTestSupport.FIXED.now(), "sistema");
        normativaRepo.guardarArticulo(artSalario);
        articuloSalarioId = artSalario.getId();

        FalArticuloNormativaFaltas artMonto = new FalArticuloNormativaFaltas(
                2L, 1L, "ART-MON", 1, "Articulo monto", TipoInfraccion.CONTRAVENCION,
                HOY.minusYears(2), FaltasClockTestSupport.FIXED.now(), "sistema");
        normativaRepo.guardarArticulo(artMonto);
        articuloMontoId = artMonto.getId();

        FalTarifarioUnidadFaltas tarSalario = new FalTarifarioUnidadFaltas(
                1L, articuloSalarioId, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                true, new BigDecimal("1"), null, HOY.minusYears(1), FaltasClockTestSupport.FIXED.now(), "sistema");
        tarifarioRepo.save(tarSalario);

        FalTarifarioUnidadFaltas tarMonto = new FalTarifarioUnidadFaltas(
                2L, articuloMontoId, TipoUnidadFaltas.MONTO, null, new BigDecimal("50000.00"),
                false, null, null, HOY.minusYears(1), FaltasClockTestSupport.FIXED.now(), "sistema");
        tarifarioRepo.save(tarMonto);

        FalValorUnidadFaltas valorSalario = new FalValorUnidadFaltas(
                1L, TipoUnidadFaltas.SALARIO, new BigDecimal("120000"), HOY.minusYears(1), FaltasClockTestSupport.FIXED.now(), "sistema");
        valorUnidadRepo.save(valorSalario);
    }

    private FalActa crearActa() {
        FalActa acta = new FalActa(
                null, UUID.randomUUID().toString(), TipoActa.TRANSITO, 1L, 1L,
                HOY, FaltasClockTestSupport.FIXED.now(), "Belgrano 200", null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, SituacionIdentificacionInfractor.DATOS_INSUFICIENTES,
                null, FaltasClockTestSupport.FIXED.now(), "sistema");
        return actaRepo.guardar(acta);
    }

    private FalActaArticuloInfringido imputar(Long actaId, Long articuloId) {
        Long id = articuloInfringidoRepo.nextId();
        FalActaArticuloInfringido imp = new FalActaArticuloInfringido(
                id, actaId, 1L, articuloId, FaltasClockTestSupport.FIXED.now(), "sistema");
        return articuloInfringidoRepo.save(imp);
    }

    @Nested
    @DisplayName("Calculo inicial automatico")
    class CalculoInicial {

        @Test
        @DisplayName("Sin articulos activos no calcula nada")
        void sin_articulos_no_calcula() {
            FalActa acta = crearActa();
            assertThat(valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now()))
                    .isEmpty();
        }

        @Test
        @DisplayName("Un articulo SALARIO calcula ordinaria y pago voluntario, cabecera con ambos totales")
        void un_articulo_salario_con_pago_voluntario() {
            FalActa acta = crearActa();
            FalActaArticuloInfringido imp = imputar(acta.getId(), articuloSalarioId);

            FalActaValorizacion header = valorizacionService
                    .calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now())
                    .orElseThrow();

            assertThat(header.getMontoTotalOrdinario()).isEqualByComparingTo("240000.00");
            assertThat(header.getMontoTotalPagoVoluntario()).isEqualByComparingTo("120000.00");
            assertThat(header.isSiConfirmada()).isFalse();

            FalActaValorizacionItem item = itemRepo.findActualByActaArticulo(imp.getId()).orElseThrow();
            assertThat(item.getVersionItem()).isEqualTo(1);
            assertThat(item.isSiActual()).isTrue();
            assertThat(item.getModoOrdinaria()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
            assertThat(item.getModoPagoVoluntario()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
        }

        @Test
        @DisplayName("Un articulo MONTO no requiere valor de unidad y no admite pago voluntario")
        void articulo_monto_sin_valor_unidad() {
            FalActa acta = crearActa();
            imputar(acta.getId(), articuloMontoId);

            FalActaValorizacion header = valorizacionService
                    .calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now())
                    .orElseThrow();

            assertThat(header.getMontoTotalOrdinario()).isEqualByComparingTo("50000.00");
            assertThat(header.getMontoTotalPagoVoluntario()).isNull();
        }

        @Test
        @DisplayName("Dos articulos suman sus montos ordinarios en una unica cabecera")
        void dos_articulos_suman_en_una_cabecera() {
            FalActa acta = crearActa();
            imputar(acta.getId(), articuloSalarioId);
            imputar(acta.getId(), articuloMontoId);

            FalActaValorizacion header = valorizacionService
                    .calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now())
                    .orElseThrow();

            assertThat(header.getMontoTotalOrdinario()).isEqualByComparingTo("290000.00");
            assertThat(valorizacionRepo.findByActaId(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("Falla si no hay tarifario activo para el articulo")
        void falla_sin_tarifario() {
            FalActa acta = crearActa();
            FalArticuloNormativaFaltas sinTarifario = new FalArticuloNormativaFaltas(
                    3L, 1L, "ART-SIN-TAR", 1, "Sin tarifario", TipoInfraccion.TRANSITO,
                    HOY.minusYears(1), FaltasClockTestSupport.FIXED.now(), "sistema");
            normativaRepo.guardarArticulo(sinTarifario);
            imputar(acta.getId(), sinTarifario.getId());

            assertThatThrownBy(() -> valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("No recalcula silenciosamente un articulo que ya tiene item actual")
        void no_recalcula_silenciosamente() {
            FalActa acta = crearActa();
            imputar(acta.getId(), articuloSalarioId);
            valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());

            Optional<FalActaValorizacion> segunda = valorizacionService
                    .calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());
            // El acta ya tiene cabecera y el articulo ya tiene item: la segunda llamada
            // no debe duplicar el total (recorre activos, pero ninguno queda sin item).
            assertThat(segunda).isPresent();
            assertThat(segunda.orElseThrow().getMontoTotalOrdinario()).isEqualByComparingTo("240000.00");
        }
    }

    @Nested
    @DisplayName("Un solo item actual por articulo (UK)")
    class UnicidadItemActual {

        @Test
        @DisplayName("manualizarItem crea nueva version y marca la anterior no-actual")
        void manualizar_crea_nueva_version() {
            FalActa acta = crearActa();
            FalActaArticuloInfringido imp = imputar(acta.getId(), articuloSalarioId);
            valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());

            FalActaValorizacionItem v1 = itemRepo.findActualByActaArticulo(imp.getId()).orElseThrow();

            FalActaValorizacionItem v2 = valorizacionService.manualizarItem(
                    imp.getId(), ModoValorizacionItem.MANUAL_MONTO, null, new BigDecimal("999999"),
                    null, null, null, "correccion manual", null,
                    "sistema", FaltasClockTestSupport.FIXED.now());

            assertThat(v2.getVersionItem()).isEqualTo(2);
            assertThat(v2.isSiActual()).isTrue();
            assertThat(itemRepo.findById(v1.getId()).orElseThrow().isSiActual()).isFalse();
            assertThat(itemRepo.findAllByActaArticulo(imp.getId())).hasSize(2);
            assertThat(itemRepo.findActualByActaArticulo(imp.getId()).orElseThrow().getId()).isEqualTo(v2.getId());
        }

        @Test
        @DisplayName("manualizarItem exige motivoCambio")
        void manualizar_exige_motivo() {
            FalActa acta = crearActa();
            FalActaArticuloInfringido imp = imputar(acta.getId(), articuloSalarioId);
            valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());

            assertThatThrownBy(() -> valorizacionService.manualizarItem(
                    imp.getId(), ModoValorizacionItem.MANUAL_MONTO, null, new BigDecimal("1"),
                    null, null, null, null, null, "sistema", FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("El item historico no se sobrescribe: conserva su contenido original")
        void item_historico_inmutable() {
            FalActa acta = crearActa();
            FalActaArticuloInfringido imp = imputar(acta.getId(), articuloSalarioId);
            valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());
            FalActaValorizacionItem v1 = itemRepo.findActualByActaArticulo(imp.getId()).orElseThrow();

            valorizacionService.manualizarItem(
                    imp.getId(), ModoValorizacionItem.MANUAL_MONTO, null, new BigDecimal("1"),
                    null, null, null, "motivo", null, "sistema", FaltasClockTestSupport.FIXED.now());

            FalActaValorizacionItem v1Recargado = itemRepo.findById(v1.getId()).orElseThrow();
            assertThat(v1Recargado.getModoOrdinaria()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
        }
    }

    @Nested
    @DisplayName("Cabecera unica por acta con OCC")
    class CabeceraUnicaConOcc {

        @Test
        @DisplayName("Solo existe una cabecera por acta")
        void una_sola_cabecera() {
            FalActa acta = crearActa();
            imputar(acta.getId(), articuloSalarioId);
            valorizacionService.calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now());
            assertThat(valorizacionRepo.findByActaId(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("recalcular esta bloqueado cuando la cabecera ya esta confirmada")
        void recalcular_bloqueado_si_confirmada() {
            FalActa acta = crearActa();
            imputar(acta.getId(), articuloSalarioId);
            FalActaValorizacion header = valorizacionService
                    .calcularInicial(acta.getId(), "sistema", FaltasClockTestSupport.FIXED.now())
                    .orElseThrow();
            header.confirmar(FaltasClockTestSupport.FIXED.now(), "sistema");

            assertThatThrownBy(() -> header.recalcular(BigDecimal.TEN, null, FaltasClockTestSupport.FIXED.now()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
