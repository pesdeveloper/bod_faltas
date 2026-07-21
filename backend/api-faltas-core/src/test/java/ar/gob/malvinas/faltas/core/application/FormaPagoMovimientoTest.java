package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.MovimientoPagoDuplicadoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFormaPago;
import ar.gob.malvinas.faltas.core.domain.model.FalActaPagoMovimiento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFormaPagoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoMovimientoRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FormaPago y PagoMovimiento - invariantes B1")
class FormaPagoMovimientoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    @Nested
    @DisplayName("FalActaFormaPago")
    class FormaPagoValidacionTest {
        private InMemoryFormaPagoRepository repo;
        @BeforeEach void setUp() { repo = new InMemoryFormaPagoRepository(); }

        private FalActaFormaPago nuevaForma(Long id, short nro, TipoFormaPago tipo) {
            return new FalActaFormaPago(id, 1L, nro, tipo, new BigDecimal("100.00"), AHORA, AHORA, "USR1");
        }

        @Test void estadoInicialGenerada() {
            assertThat(nuevaForma(1L, (short)1, TipoFormaPago.RECIBO_AL_COBRO).getEstadoFormaPago()).isEqualTo(EstadoFormaPago.GENERADA);
        }
        @Test void tipoFormaPago_codigos() {
            assertThat(TipoFormaPago.RECIBO_AL_COBRO.codigo()).isEqualTo((short)1);
            assertThat(EstadoFormaPago.PAGADA.codigo()).isEqualTo((short)4);
        }
    }

    @Nested
    @DisplayName("FalActaPagoMovimiento")
    class MovimientoTest {
        private InMemoryPagoMovimientoRepository repo;
        @BeforeEach void setUp() { repo = new InMemoryPagoMovimientoRepository(); }

        private FalActaPagoMovimiento mov(Long id, TipoMovimientoPago tipo, String ref) {
            return new FalActaPagoMovimiento.Builder(id, 1L, tipo, OrigenMovimiento.INGRESOS, AHORA, AHORA, "USR1")
                    .referenciaExterna(ref).importes(new BigDecimal("100"), null, new BigDecimal("100")).build();
        }

        @Test void cincoTiposMovimiento() { assertThat(TipoMovimientoPago.values()).hasSize(5); }

        @Test void idempotencia_origenYReferencia() {
            repo.append(mov(1L, TipoMovimientoPago.PAGO_PROCESADO, "REF-001"));
            var dup = repo.append(mov(2L, TipoMovimientoPago.PAGO_PROCESADO, "REF-001"));
            assertThat(dup.movimiento().getId()).isEqualTo(1L);
        }

        @Test void conflicto_distintoTipo() {
            repo.append(mov(1L, TipoMovimientoPago.PAGO_PROCESADO, "REF-001"));
            var outcome = repo.append(mov(2L, TipoMovimientoPago.PAGO_CONFIRMADO, "REF-001"));
            assertThat(outcome.resultado().name()).isEqualTo("CONFLICT");
        }

        @Test void origenMovimiento_requerido() {
            assertThatThrownBy(() -> new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.DEUDA_EMITIDA, null, AHORA, AHORA, "USR1").build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test void pagoRevertido_esAnulacion() {
            assertThat(new FalActaPagoMovimiento.Builder(1L, 1L, TipoMovimientoPago.PAGO_REVERTIDO, OrigenMovimiento.TESORERIA, AHORA, AHORA, "USR1").build().esAnulacion()).isTrue();
        }
    }
}
