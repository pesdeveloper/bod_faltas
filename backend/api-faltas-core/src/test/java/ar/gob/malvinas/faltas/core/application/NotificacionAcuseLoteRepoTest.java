package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11I: FalNotificacionAcuse y FalLoteCorreo - entidad y repositorio")
class NotificacionAcuseLoteRepoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    @Nested @DisplayName("FalNotificacionAcuse - entidad")
    class AcuseEntidad {
        private InMemoryNotificacionAcuseRepository repo;

        @BeforeEach void setUp() { repo = new InMemoryNotificacionAcuseRepository(); }

        private FalNotificacionAcuse nuevoAcuse(Long id, Long notifId, Long intentoId, TipoAcuse tipo) {
            return new FalNotificacionAcuse(id, notifId, intentoId, tipo, AHORA, "USR1");
        }

        @Test @DisplayName("estado inicial es PENDIENTE")
        void estadoInicial() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, 5L, TipoAcuse.ACUSE_RECEPCION);
            assertThat(a.getEstadoAcuse()).isEqualTo(EstadoAcuse.PENDIENTE);
            assertThat(a.estaActivo()).isTrue();
            assertThat(a.estaAnulado()).isFalse();
            assertThat(a.estaValidado()).isFalse();
        }

        @Test @DisplayName("notificacionId null lanza excepcion")
        void notifIdNull() {
            assertThatThrownBy(() -> new FalNotificacionAcuse(1L, null, null, TipoAcuse.ACUSE_RECEPCION, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("tipoAcuse null lanza excepcion")
        void tipoNull() {
            assertThatThrownBy(() -> new FalNotificacionAcuse(1L, 10L, null, null, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("storageKey excede 255 chars lanza excepcion")
        void storageKeyMaxLen() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, null, TipoAcuse.ACUSE_OTRO);
            assertThatThrownBy(() -> a.setStorageKey("a".repeat(256)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("intentoId puede ser null (acuse de notificacion sin intento especifico)")
        void intentoIdNull() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, null, TipoAcuse.ACUSE_RECEPCION);
            assertThat(a.getIntentoId()).isNull();
        }

        @Test @DisplayName("6 tipos de acuse")
        void sixTipos() {
            for (TipoAcuse tipo : TipoAcuse.values()) {
                FalNotificacionAcuse a = nuevoAcuse((long) tipo.codigo(), 10L, null, tipo);
                assertThat(a.getTipoAcuse()).isEqualTo(tipo);
            }
        }

        @Test @DisplayName("copia es defensiva")
        void copiaDefensiva() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, null, TipoAcuse.ACUSE_RECEPCION);
            FalNotificacionAcuse c = a.copia();
            c.setEstadoAcuse(EstadoAcuse.VALIDADO);
            assertThat(a.getEstadoAcuse()).isEqualTo(EstadoAcuse.PENDIENTE);
        }

        @Test @DisplayName("guardar y buscarPorId")
        void guardarYBuscar() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, 5L, TipoAcuse.ACUSE_RECEPCION);
            repo.guardar(a);
            assertThat(repo.buscarPorId(1L)).isPresent();
        }

        @Test @DisplayName("buscarPorNotificacion")
        void buscarPorNotif() {
            repo.guardar(nuevoAcuse(1L, 10L, 1L, TipoAcuse.ACUSE_RECEPCION));
            repo.guardar(nuevoAcuse(2L, 10L, 2L, TipoAcuse.ACUSE_RECHAZO));
            repo.guardar(nuevoAcuse(3L, 20L, 3L, TipoAcuse.ACUSE_AUSENTE));
            assertThat(repo.buscarPorNotificacion(10L)).hasSize(2);
            assertThat(repo.buscarPorNotificacion(20L)).hasSize(1);
        }

        @Test @DisplayName("buscarPorIntento")
        void buscarPorIntento() {
            repo.guardar(nuevoAcuse(1L, 10L, 5L, TipoAcuse.ACUSE_RECEPCION));
            repo.guardar(nuevoAcuse(2L, 10L, 5L, TipoAcuse.ACUSE_RECHAZO));
            repo.guardar(nuevoAcuse(3L, 10L, 6L, TipoAcuse.ACUSE_AUSENTE));
            assertThat(repo.buscarPorIntento(5L)).hasSize(2);
            assertThat(repo.buscarPorIntento(6L)).hasSize(1);
        }

        @Test @DisplayName("buscarPorIdempotencia retorna solo activos")
        void buscarPorIdempotencia() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, 5L, TipoAcuse.ACUSE_RECEPCION);
            repo.guardar(a);
            assertThat(repo.buscarPorIdempotencia(10L, 5L, TipoAcuse.ACUSE_RECEPCION)).isPresent();
            assertThat(repo.buscarPorIdempotencia(10L, 5L, TipoAcuse.ACUSE_RECHAZO)).isEmpty();
            assertThat(repo.buscarPorIdempotencia(10L, 6L, TipoAcuse.ACUSE_RECEPCION)).isEmpty();
        }

        @Test @DisplayName("buscarPorIdempotencia ignora anulados")
        void idempotenciaIgnoraAnulados() {
            FalNotificacionAcuse a = nuevoAcuse(1L, 10L, 5L, TipoAcuse.ACUSE_RECEPCION);
            a.setEstadoAcuse(EstadoAcuse.ANULADO);
            repo.guardar(a);
            assertThat(repo.buscarPorIdempotencia(10L, 5L, TipoAcuse.ACUSE_RECEPCION)).isEmpty();
        }

        @Test @DisplayName("nextId se incrementa")
        void nextId() {
            long id1 = repo.nextId();
            long id2 = repo.nextId();
            assertThat(id2).isGreaterThan(id1);
        }
    }

    @Nested @DisplayName("FalLoteCorreo - entidad y repositorio")
    class LoteEntidad {
        private InMemoryLoteCorreoRepository repo;

        @BeforeEach void setUp() { repo = new InMemoryLoteCorreoRepository(); }

        private FalLoteCorreo nuevoLote(Long id, String codigo) {
            return new FalLoteCorreo(id, codigo, AHORA, AHORA, "USR1");
        }

        @Test @DisplayName("estado inicial GENERADO")
        void estadoInicial() {
            FalLoteCorreo l = nuevoLote(1L, "LOT-2026-001");
            assertThat(l.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);
            assertThat(l.esEmitible()).isTrue();
            assertThat(l.esAnulable()).isTrue();
            assertThat(l.esProcesable()).isFalse();
        }

        @Test @DisplayName("codigo null lanza excepcion")
        void codigoNull() {
            assertThatThrownBy(() -> new FalLoteCorreo(1L, null, AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("codigo vacio lanza excepcion")
        void codigoVacio() {
            assertThatThrownBy(() -> new FalLoteCorreo(1L, "  ", AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("codigo excede 30 chars lanza excepcion")
        void codigoMaxLen() {
            assertThatThrownBy(() -> new FalLoteCorreo(1L, "a".repeat(31), AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("guidLoteExt debe tener 36 chars")
        void guidLen36() {
            FalLoteCorreo l = nuevoLote(1L, "LOT-001");
            assertThatThrownBy(() -> l.setGuidLoteExt("too-short"))
                    .isInstanceOf(IllegalArgumentException.class);
            l.setGuidLoteExt("12345678-1234-1234-1234-123456789012");
            assertThat(l.getGuidLoteExt()).hasSize(36);
        }

        @Test @DisplayName("referenciaExterna excede 60 chars lanza excepcion")
        void refExtMaxLen() {
            FalLoteCorreo l = nuevoLote(1L, "LOT-001");
            assertThatThrownBy(() -> l.setReferenciaExterna("a".repeat(61)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("copia es defensiva")
        void copiaDefensiva() {
            FalLoteCorreo l = nuevoLote(1L, "LOT-001");
            FalLoteCorreo c = l.copia();
            c.setEstadoLote(EstadoLote.EMITIDO);
            assertThat(l.getEstadoLote()).isEqualTo(EstadoLote.GENERADO);
        }

        @Test @DisplayName("guardar y buscarPorId")
        void guardarYBuscar() {
            repo.guardar(nuevoLote(1L, "LOT-001"));
            assertThat(repo.buscarPorId(1L)).isPresent();
            assertThat(repo.buscarPorId(999L)).isEmpty();
        }

        @Test @DisplayName("buscarPorCodigo")
        void buscarPorCodigo() {
            repo.guardar(nuevoLote(1L, "LOT-001"));
            assertThat(repo.buscarPorCodigo("LOT-001")).isPresent();
            assertThat(repo.buscarPorCodigo("LOT-XXX")).isEmpty();
            assertThat(repo.buscarPorCodigo(null)).isEmpty();
        }

        @Test @DisplayName("existeCodigo es correcto")
        void existeCodigo() {
            repo.guardar(nuevoLote(1L, "LOT-001"));
            assertThat(repo.existeCodigo("LOT-001")).isTrue();
            assertThat(repo.existeCodigo("LOT-XXX")).isFalse();
        }

        @Test @DisplayName("buscarPorEstado")
        void buscarPorEstado() {
            FalLoteCorreo l1 = nuevoLote(1L, "LOT-001");
            FalLoteCorreo l2 = nuevoLote(2L, "LOT-002");
            l2.setEstadoLote(EstadoLote.EMITIDO);
            repo.guardar(l1);
            repo.guardar(l2);
            assertThat(repo.buscarPorEstado(EstadoLote.GENERADO)).hasSize(1);
            assertThat(repo.buscarPorEstado(EstadoLote.EMITIDO)).hasSize(1);
            assertThat(repo.buscarPorEstado(EstadoLote.PROCESADO)).isEmpty();
        }

        @Test @DisplayName("buscarPorGuid y buscarPorReferenciaExterna")
        void buscarPorGuidYRef() {
            FalLoteCorreo l = nuevoLote(1L, "LOT-001");
            l.setGuidLoteExt("12345678-1234-1234-1234-123456789012");
            l.setReferenciaExterna("REF-CORREO-001");
            repo.guardar(l);
            assertThat(repo.buscarPorGuid("12345678-1234-1234-1234-123456789012")).isPresent();
            assertThat(repo.buscarPorGuid("otro-guid")).isEmpty();
            assertThat(repo.buscarPorReferenciaExterna("REF-CORREO-001")).isPresent();
            assertThat(repo.buscarPorReferenciaExterna("OTRO")).isEmpty();
        }

        @Test @DisplayName("reset limpia todo")
        void reset() {
            repo.guardar(nuevoLote(1L, "LOT-001"));
            repo.reset();
            assertThat(repo.size()).isEqualTo(0);
            assertThat(repo.existeCodigo("LOT-001")).isFalse();
        }

        @Test @DisplayName("cargarSeed reanuda ID counter")
        void cargarSeed() {
            List<FalLoteCorreo> seed = List.of(nuevoLote(10L, "LOT-010"), nuevoLote(20L, "LOT-020"));
            repo.cargarSeed(seed);
            assertThat(repo.size()).isEqualTo(2);
            assertThat(repo.nextId()).isEqualTo(21L);
        }
    }
}
