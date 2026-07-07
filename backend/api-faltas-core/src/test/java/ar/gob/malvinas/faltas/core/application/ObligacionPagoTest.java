package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.enums.TipoObligacionPago;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaObligacionPago;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObligacionPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FalActaObligacionPago - invariantes y OCC")
class ObligacionPagoTest {

    private InMemoryObligacionPagoRepository repo;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    @BeforeEach
    void setUp() {
        repo = new InMemoryObligacionPagoRepository();
    }

    private FalActaObligacionPago nueva(Long id, Long actaId, TipoObligacionPago tipo, BigDecimal monto) {
        return new FalActaObligacionPago(id, actaId, 100L, tipo, monto, AHORA, "USR1", AHORA, "USR1");
    }

    @Nested
    @DisplayName("Constructor y validaciones")
    class ConstructorTest {
        @Test
        void idNulo_lanzaExcepcion() {
            assertThatThrownBy(() -> nueva(null, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("id");
        }
        @Test
        void montoNegativo_lanzaExcepcion() {
            assertThatThrownBy(() -> nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("-1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negativo");
        }
        @Test
        void montoEscala2() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, new BigDecimal("10.567"));
            assertThat(o.getMontoOriginal()).isEqualByComparingTo(new BigDecimal("10.57"));
        }
        @Test
        void estadoInicialDeterminada() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            assertThat(o.getEstadoObligacion()).isEqualTo(EstadoObligacionPago.DETERMINADA);
            assertThat(o.isSiVigente()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tipos y helpers")
    class TiposTest {
        @Test
        void tipoObligacionPago_TipoObligacionPago() {
            assertThat(TipoObligacionPago.PAGO_VOLUNTARIO.codigo()).isEqualTo((short) 1);
            assertThat(TipoObligacionPago.CONDENA.codigo()).isEqualTo((short) 2);
            assertThat(TipoObligacionPago.fromCodigo((short) 1)).isEqualTo(TipoObligacionPago.PAGO_VOLUNTARIO);
            assertThat(TipoObligacionPago.fromCodigo((short) 2)).isEqualTo(TipoObligacionPago.CONDENA);
        }
        @Test
        void estadoObligacionPago_todosLosCodigos() {
            assertThat(EstadoObligacionPago.DETERMINADA.codigo()).isEqualTo((short) 1);
            assertThat(EstadoObligacionPago.CANCELADA.codigo()).isEqualTo((short) 6);
            assertThat(EstadoObligacionPago.ANULADA.codigo()).isEqualTo((short) 7);
        }
        @Test
        void esVoluntaria_esCondena() {
            FalActaObligacionPago vol = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            FalActaObligacionPago cond = nueva(2L, 2L, TipoObligacionPago.CONDENA, BigDecimal.TEN);
            assertThat(vol.esVoluntaria()).isTrue();
            assertThat(vol.esCondena()).isFalse();
            assertThat(cond.esCondena()).isTrue();
            assertThat(cond.esVoluntaria()).isFalse();
        }
    }

    @Nested
    @DisplayName("Cancelacion y anulacion")
    class CancelacionTest {
        @Test
        void cancelar_transicion_correcta() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            o.cancelar(AHORA);
            assertThat(o.estaCancelada()).isTrue();
            assertThat(o.getFhCancelacion()).isEqualTo(AHORA);
        }
        @Test
        void cancelarDosVeces_lanzaExcepcion() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            o.cancelar(AHORA);
            assertThatThrownBy(() -> o.cancelar(AHORA))
                    .isInstanceOf(IllegalStateException.class);
        }
        @Test
        void cancelarSinFecha_lanzaExcepcion() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            assertThatThrownBy(() -> o.cancelar(null)).isInstanceOf(IllegalArgumentException.class);
        }
        @Test
        void anular_desactivaVigente() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            o.anular();
            assertThat(o.estaAnulada()).isTrue();
            assertThat(o.isSiVigente()).isFalse();
        }
    }

    @Nested
    @DisplayName("Repository - unicidad y OCC")
    class RepositoryTest {
        @Test
        void guardaPrimerObligacion() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            FalActaObligacionPago saved = repo.save(o);
            assertThat(saved.getId()).isEqualTo(1L);
            assertThat(saved.getVersionRow()).isEqualTo(0);
        }
        @Test
        void guardarSegundaVigenteParaMismoActa_lanzaExcepcion() {
            FalActaObligacionPago o1 = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            repo.save(o1);
            FalActaObligacionPago o2 = nueva(2L, 1L, TipoObligacionPago.CONDENA, BigDecimal.TEN);
            assertThatThrownBy(() -> repo.save(o2))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("vigente");
        }
        @Test
        void occVersion_conflicto() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            repo.save(o);
            FalActaObligacionPago v0 = repo.findById(1L).get();
            FalActaObligacionPago v0b = repo.findById(1L).get();
            v0.setEstadoObligacion(EstadoObligacionPago.DEUDA_EMITIDA);
            repo.save(v0);
            v0b.setEstadoObligacion(EstadoObligacionPago.CANCELADA);
            assertThatThrownBy(() -> repo.save(v0b))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }
        @Test
        void crearVigenteAtomico_reemplazaAnterior() {
            FalActaObligacionPago antigua = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            repo.save(antigua);
            FalActaObligacionPago anteriorVigente = repo.findVigenteByActaId(1L).get();
            FalActaObligacionPago nueva = nueva(2L, 1L, TipoObligacionPago.CONDENA, new BigDecimal("200"));
            repo.crearVigenteAtomico(nueva, anteriorVigente);
            assertThat(repo.findVigenteByActaId(1L)).isPresent()
                    .hasValueSatisfying(v -> assertThat(v.getTipoObligacion()).isEqualTo(TipoObligacionPago.CONDENA));
            assertThat(repo.findById(1L)).isPresent()
                    .hasValueSatisfying(v -> assertThat(v.isSiVigente()).isFalse());
        }
        @Test
        void findByActaId_devuelveHistorial() {
            FalActaObligacionPago o = nueva(1L, 1L, TipoObligacionPago.PAGO_VOLUNTARIO, BigDecimal.TEN);
            repo.save(o);
            assertThat(repo.findByActaId(1L)).hasSize(1);
            assertThat(repo.findByActaId(99L)).isEmpty();
        }
    }
}