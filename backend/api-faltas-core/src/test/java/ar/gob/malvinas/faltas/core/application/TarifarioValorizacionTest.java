package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.TarifarioService;
import ar.gob.malvinas.faltas.core.application.service.ValorUnidadFaltasService;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionCalculador;
import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.exception.TarifarioNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del rediseño de tarifario/valor-unidad/calculador (PROMPT-NOCTURNO-WAVE-04-PARTE-2 S7.2/S7.3/S7.9).
 *
 * El tarifario es POR ARTICULO (no por tipo de unidad global). El valor de unidad
 * (SALARIO/UNIDAD_FIJA) es una tabla nueva independiente. El calculador es un
 * componente puro reusable, sin persistencia.
 */
@DisplayName("Tarifario por articulo + valor de unidad + calculador (Wave 4 PARTE-2)")
class TarifarioValorizacionTest {

    private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
    private InMemoryValorUnidadFaltasRepository valorUnidadRepo;
    private TarifarioService tarifarioService;
    private ValorUnidadFaltasService valorUnidadService;
    private ValorizacionCalculador calculador;

    private static final LocalDate HOY = LocalDate.of(2026, 6, 1);

    @BeforeEach
    void setUp() {
        tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
        valorUnidadRepo = new InMemoryValorUnidadFaltasRepository();
        tarifarioService = new TarifarioService(tarifarioRepo, FaltasClockTestSupport.FIXED);
        valorUnidadService = new ValorUnidadFaltasService(valorUnidadRepo, FaltasClockTestSupport.FIXED);
        calculador = new ValorizacionCalculador();
    }

    @Nested
    @DisplayName("Tarifario por articulo")
    class TarifarioPorArticulo {

        @Test
        @DisplayName("Crea formula SALARIO ordinaria simple")
        void crea_formula_salario() {
            FalTarifarioUnidadFaltas t = tarifarioService.crearParaArticulo(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2.0000"), null,
                    false, null, null, HOY.minusMonths(1), "sistema");
            assertThat(t.getId()).isNotNull();
            assertThat(t.getArticuloId()).isEqualTo(1L);
            assertThat(t.getTipoUnidad()).isEqualTo(TipoUnidadFaltas.SALARIO);
            assertThat(t.getCantidadUnidadesOrdinaria()).isEqualByComparingTo("2.0000");
            assertThat(t.getMontoFijoOrdinario()).isNull();
            assertThat(t.isSiAdmitePagoVoluntario()).isFalse();
            assertThat(t.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Crea formula MONTO con monto fijo y pago voluntario")
        void crea_formula_monto_con_pago_voluntario() {
            FalTarifarioUnidadFaltas t = tarifarioService.crearParaArticulo(
                    2L, TipoUnidadFaltas.MONTO, null, new BigDecimal("50000.00"),
                    true, null, new BigDecimal("25000.00"), HOY.minusMonths(1), "sistema");
            assertThat(t.getMontoFijoOrdinario()).isEqualByComparingTo("50000.00");
            assertThat(t.getMontoFijoPagoVoluntario()).isEqualByComparingTo("25000.00");
            assertThat(t.isSiAdmitePagoVoluntario()).isTrue();
        }

        @Test
        @DisplayName("Rechaza SALARIO con montoFijoOrdinario informado")
        void rechaza_salario_con_monto_fijo() {
            assertThatThrownBy(() -> new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), new BigDecimal("100"),
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rechaza MONTO sin montoFijoOrdinario")
        void rechaza_monto_sin_monto_fijo() {
            assertThatThrownBy(() -> new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.MONTO, null, null,
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rechaza siAdmitePagoVoluntario=true sin contraparte de pago voluntario")
        void rechaza_pago_voluntario_sin_contraparte() {
            assertThatThrownBy(() -> new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    true, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rechaza crear segunda formula activa para el mismo articulo")
        void rechaza_segunda_formula_activa() {
            tarifarioService.crearParaArticulo(1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY.minusMonths(2), "sistema");
            assertThatThrownBy(() -> tarifarioService.crearParaArticulo(
                    1L, TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("3"), null,
                    false, null, null, HOY.minusMonths(1), "sistema"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("cambiarFormula cierra la anterior y activa la nueva")
        void cambiar_formula_cierra_anterior() {
            FalTarifarioUnidadFaltas t1 = tarifarioService.crearParaArticulo(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY.minusYears(1), "sistema");
            FalTarifarioUnidadFaltas t2 = tarifarioService.cambiarFormula(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("3"), null,
                    false, null, null, HOY, "sistema");

            FalTarifarioUnidadFaltas anteriorRecargada = tarifarioService.obtener(t1.getId());
            assertThat(anteriorRecargada.isSiActiva()).isFalse();
            assertThat(anteriorRecargada.getFhVigHasta()).isEqualTo(HOY);
            assertThat(tarifarioService.resolverActiva(1L).map(FalTarifarioUnidadFaltas::getId)).contains(t2.getId());
        }

        @Test
        @DisplayName("resolverVigente devuelve la formula historica correcta segun fecha")
        void resolver_vigente_historico() {
            FalTarifarioUnidadFaltas t1 = tarifarioService.crearParaArticulo(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY.minusYears(2), "sistema");
            tarifarioService.cambiarFormula(1L, TipoUnidadFaltas.SALARIO, new BigDecimal("3"), null,
                    false, null, null, HOY.minusYears(1), "sistema");

            assertThat(tarifarioService.resolverVigente(1L, HOY.minusYears(2).plusDays(1)).map(FalTarifarioUnidadFaltas::getId))
                    .contains(t1.getId());
        }

        @Test
        @DisplayName("Buscar tarifario inexistente lanza TarifarioNoEncontradoException")
        void buscar_inexistente() {
            assertThatThrownBy(() -> tarifarioService.obtener(9999L))
                    .isInstanceOf(TarifarioNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Valor de unidad (SALARIO/UNIDAD_FIJA)")
    class ValorUnidad {

        @Test
        @DisplayName("Crea valor activo para SALARIO")
        void crea_valor_salario() {
            FalValorUnidadFaltas v = valorUnidadService.crear(TipoUnidadFaltas.SALARIO,
                    new BigDecimal("120000.0000"), HOY.minusMonths(6), "sistema");
            assertThat(v.isSiActiva()).isTrue();
            assertThat(valorUnidadService.resolverActivo(TipoUnidadFaltas.SALARIO).map(FalValorUnidadFaltas::getId))
                    .contains(v.getId());
        }

        @Test
        @DisplayName("Rechaza crear valor de unidad para MONTO")
        void rechaza_monto() {
            assertThatThrownBy(() -> valorUnidadService.crear(TipoUnidadFaltas.MONTO,
                    new BigDecimal("1"), HOY, "sistema"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("actualizarValor cierra el activo anterior y crea uno nuevo")
        void actualizar_valor() {
            FalValorUnidadFaltas v1 = valorUnidadService.crear(TipoUnidadFaltas.UNIDAD_FIJA,
                    new BigDecimal("8000"), HOY.minusYears(1), "sistema");
            FalValorUnidadFaltas v2 = valorUnidadService.actualizarValor(TipoUnidadFaltas.UNIDAD_FIJA,
                    new BigDecimal("9000"), HOY, "sistema");
            assertThat(valorUnidadService.resolverActivo(TipoUnidadFaltas.UNIDAD_FIJA).map(FalValorUnidadFaltas::getId))
                    .contains(v2.getId());
            assertThat(valorUnidadRepo.findById(v1.getId()).orElseThrow().isSiActiva()).isFalse();
        }

        @Test
        @DisplayName("Rechaza segundo valor activo simultaneo para el mismo tipo")
        void rechaza_segundo_activo() {
            valorUnidadService.crear(TipoUnidadFaltas.SALARIO, new BigDecimal("1"), HOY.minusYears(1), "sistema");
            assertThatThrownBy(() -> valorUnidadService.crear(TipoUnidadFaltas.SALARIO,
                    new BigDecimal("2"), HOY, "sistema"))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Calculador reusable (sin persistencia)")
    class Calculador {

        @Test
        @DisplayName("Calcula ordinaria automatica SALARIO: cantidad x valor unidad")
        void calcula_ordinaria_salario() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            FalValorUnidadFaltas v = new FalValorUnidadFaltas(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("120000"), HOY, FaltasClockTestSupport.FIXED.now(), "sistema");

            ValorizacionCalculador.ResultadoDimension r = calculador.calcularOrdinarioAutomatico(t, v);
            assertThat(r.monto()).isEqualByComparingTo("240000.00");
        }

        @Test
        @DisplayName("Calcula ordinaria automatica MONTO: usa el monto fijo directamente")
        void calcula_ordinaria_monto() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 3L, TipoUnidadFaltas.MONTO, null, new BigDecimal("50000.00"),
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            ValorizacionCalculador.ResultadoDimension r = calculador.calcularOrdinarioAutomatico(t, null);
            assertThat(r.monto()).isEqualByComparingTo("50000.00");
        }

        @Test
        @DisplayName("Calcula pago voluntario automatico cuando el articulo lo admite")
        void calcula_pago_voluntario() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    true, new BigDecimal("1"), null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            FalValorUnidadFaltas v = new FalValorUnidadFaltas(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("120000"), HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            assertThat(calculador.calcularPagoVoluntarioAutomatico(t, v).monto()).isEqualByComparingTo("120000.00");
        }

        @Test
        @DisplayName("Falla al pedir pago voluntario si el articulo no lo admite")
        void falla_pago_voluntario_no_admitido() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            assertThatThrownBy(() -> calculador.calcularPagoVoluntarioAutomatico(t, null))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Falla el automatico SALARIO/UNIDAD_FIJA sin valor de unidad vigente")
        void falla_sin_valor_unidad() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            assertThatThrownBy(() -> calculador.calcularOrdinarioAutomatico(t, null))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("Calcula MANUAL_MONTO con el monto informado")
        void calcula_manual_monto() {
            assertThat(calculador.calcularManualMonto(new BigDecimal("777")).monto()).isEqualByComparingTo("777.00");
        }

        @Test
        @DisplayName("Calcula MANUAL_UNIDADES con cantidad informada x valor de unidad")
        void calcula_manual_unidades() {
            FalValorUnidadFaltas v = new FalValorUnidadFaltas(
                    1L, TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("8000"), HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            assertThat(calculador.calcularManualUnidades(new BigDecimal("3"), v).monto()).isEqualByComparingTo("24000.00");
        }

        @Test
        @DisplayName("calcularOrdinaria despacha por modo correctamente")
        void calcular_ordinaria_dispatch() {
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    1L, 1L, TipoUnidadFaltas.SALARIO, new BigDecimal("2"), null,
                    false, null, null, HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            FalValorUnidadFaltas v = new FalValorUnidadFaltas(
                    1L, TipoUnidadFaltas.SALARIO, new BigDecimal("100"), HOY, FaltasClockTestSupport.FIXED.now(), "sistema");
            assertThat(calculador.calcularOrdinaria(ModoValorizacionItem.AUTOMATICA, t, v, null, null).monto())
                    .isEqualByComparingTo("200.00");
            assertThat(calculador.calcularOrdinaria(ModoValorizacionItem.MANUAL_MONTO, t, v, null, new BigDecimal("555")).monto())
                    .isEqualByComparingTo("555.00");
        }
    }
}
