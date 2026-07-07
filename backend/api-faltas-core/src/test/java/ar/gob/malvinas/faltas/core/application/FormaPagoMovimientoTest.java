package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFormaPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoMovimientoPago;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoMovimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FormaPago y PagoMovimiento - invariantes")
class FormaPagoMovimientoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    @Nested
    @DisplayName("FalActaFormaPago - validaciones")
    class FormaPagoValidacionTest {
        private InMemoryFormaPagoRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryFormaPagoRepository(); }

        private FalActaFormaPago nuevaForma(Long id, short nroForma, TipoFormaPago tipo) {
            return new FalActaFormaPago(id, 1L, nroForma, tipo, new BigDecimal("100.00"), AHORA, AHORA, "USR1");
        }

        @Test
        void estadoInicialGenerada() {
            FalActaFormaPago f = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            assertThat(f.getEstadoFormaPago()).isEqualTo(EstadoFormaPago.GENERADA);
        }
        @Test
        void nroFormaPositivo() {
            assertThatThrownBy(() -> nuevaForma(1L, (short) 0, TipoFormaPago.CONTADO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> nuevaForma(1L, (short) -1, TipoFormaPago.CONTADO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        void montoNegativo_lanzaExcepcion() {
            assertThatThrownBy(() ->
                    new FalActaFormaPago(1L, 1L, (short) 1, TipoFormaPago.CONTADO, new BigDecimal("-0.01"), AHORA, AHORA, "USR1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        void referenciaEM_tripleCompleto_oTodoNull() {
            FalActaFormaPago f = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            f.setReferenciaEM("AB", (short) 1, 1000);
            assertThat(f.getCmteEM()).isEqualTo("AB");
            assertThatThrownBy(() -> f.setReferenciaEM("AB", null, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Triple EM");
            f.setReferenciaEM(null, null, null);
            assertThat(f.getCmteEM()).isNull();
        }
        @Test
        void referenciaPG_tripleCompleto_oTodoNull() {
            FalActaFormaPago f = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            f.setReferenciaPG("PG", (short) 2, 2000);
            assertThat(f.getCmtePG()).isEqualTo("PG");
            assertThatThrownBy(() -> f.setReferenciaPG("PG", (short) 2, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        void cmteEM_exactamente2caracteres() {
            FalActaFormaPago f = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            assertThatThrownBy(() -> f.setReferenciaEM("ABC", (short) 1, 1))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2");
        }
        @Test
        void tipoFormaPago_codigos() {
            assertThat(TipoFormaPago.CONTADO.codigo()).isEqualTo((short) 1);
            assertThat(TipoFormaPago.PLAN_PAGO.codigo()).isEqualTo((short) 2);
            assertThat(TipoFormaPago.REFINANCIACION.codigo()).isEqualTo((short) 3);
        }
        @Test
        void estadoFormaPago_todosLosCodigos() {
            assertThat(EstadoFormaPago.GENERADA.codigo()).isEqualTo((short) 1);
            assertThat(EstadoFormaPago.CONFIRMADA.codigo()).isEqualTo((short) 3);
        }
        @Test
        void repo_nroDuplicado_lanzaExcepcion() {
            FalActaFormaPago f1 = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            repo.save(f1);
            FalActaFormaPago f2 = nuevaForma(2L, (short) 1, TipoFormaPago.CONTADO);
            assertThatThrownBy(() -> repo.save(f2))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
        @Test
        void esPlan_contado_noEsPlan() {
            FalActaFormaPago contado = nuevaForma(1L, (short) 1, TipoFormaPago.CONTADO);
            FalActaFormaPago plan = nuevaForma(2L, (short) 2, TipoFormaPago.PLAN_PAGO);
            assertThat(contado.esPlan()).isFalse();
            assertThat(plan.esPlan()).isTrue();
        }
    }

    @Nested
    @DisplayName("FalActaPagoMovimiento - invariantes append-only")
    class MovimientoTest {
        private InMemoryPagoMovimientoRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryPagoMovimientoRepository(); }

        private FalActaPagoMovimiento nuevoMovimiento(Long id, Long obligId, TipoMovimientoPago tipo) {
            return new FalActaPagoMovimiento.Builder(id, obligId, tipo, AHORA, AHORA, "USR1").build();
        }

        @Test
        void tipoMovimientoPago_27tipos() {
            assertThat(TipoMovimientoPago.values()).hasSize(27);
            assertThat(TipoMovimientoPago.DEUDA_EMITIDA.codigo()).isEqualTo((short) 1);
            assertThat(TipoMovimientoPago.OTRO.codigo()).isEqualTo((short) 27);
        }
        @Test
        void motivoAnulacion_6motivos() {
            assertThat(MotivoAnulacionPago.values()).hasSize(6);
            assertThat(MotivoAnulacionPago.CONTRACARGO.codigo()).isEqualTo((short) 1);
            assertThat(MotivoAnulacionPago.OTRO.codigo()).isEqualTo((short) 6);
        }
        @Test
        void append_exitoso() {
            FalActaPagoMovimiento m = nuevoMovimiento(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA);
            FalActaPagoMovimiento saved = repo.append(m);
            assertThat(saved.getId()).isEqualTo(1L);
        }
        @Test
        void appendDuplicado_idDuplicado_lanzaExcepcion() {
            FalActaPagoMovimiento m = nuevoMovimiento(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA);
            repo.append(m);
            FalActaPagoMovimiento m2 = nuevoMovimiento(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO);
            assertThatThrownBy(() -> repo.append(m2))
                    .isInstanceOf(MovimientoPagoDuplicadoException.class);
        }
        @Test
        void referenciaExterna_idempotencia_mismoTipo() {
            FalActaPagoMovimiento m = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, AHORA, AHORA, "USR1")
                    .referenciaExterna("REF-001").build();
            repo.append(m);
            FalActaPagoMovimiento m2 = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_PROCESADO, AHORA, AHORA, "USR1")
                    .referenciaExterna("REF-001").build();
            FalActaPagoMovimiento result = repo.append(m2);
            assertThat(result.getId()).isEqualTo(1L);
        }
        @Test
        void referenciaExterna_conflicto_diferenteTipo() {
            FalActaPagoMovimiento m = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, AHORA, AHORA, "USR1")
                    .referenciaExterna("REF-001").build();
            repo.append(m);
            FalActaPagoMovimiento m2 = new FalActaPagoMovimiento.Builder(2L, 1L, TipoMovimientoPago.PAGO_CONFIRMADO_TESORERIA, AHORA, AHORA, "USR1")
                    .referenciaExterna("REF-001").build();
            assertThatThrownBy(() -> repo.append(m2))
                    .isInstanceOf(MovimientoPagoDuplicadoException.class);
        }
        @Test
        void importes_suma_valida() {
            FalActaPagoMovimiento m = new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, AHORA, AHORA, "USR1")
                    .importes(new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"))
                    .build();
            assertThat(m.getImporteTotal()).isEqualByComparingTo(new BigDecimal("120.00"));
        }
        @Test
        void importes_suma_invalida() {
            assertThatThrownBy(() ->
                new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_PROCESADO, AHORA, AHORA, "USR1")
                        .importes(new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("115.00"))
                        .build())
                .isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        void referenciaExterna_maxLong() {
            assertThatThrownBy(() ->
                new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.OTRO, AHORA, AHORA, "USR1")
                        .referenciaExterna("X".repeat(81)).build())
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("80");
        }
    }
}