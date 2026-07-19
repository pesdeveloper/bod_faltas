package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11I: FalNotificacionIntento - entidad, repositorio y correlativo atomico")
class NotificacionIntentoRepoTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);
    private InMemoryNotificacionIntentoRepository repo;

    @BeforeEach
    void setUp() { repo = new InMemoryNotificacionIntentoRepository(); }

    private FalNotificacionIntento nuevoIntento(Long id, Long notifId, short nroIntento, CanalNotificacion canal) {
        return new FalNotificacionIntento(id, notifId, nroIntento, canal, null, null, null, null, AHORA, AHORA, "USR1");
    }

    @Nested @DisplayName("Entidad - validaciones")
    class EntidadValidaciones {

        @Test @DisplayName("id null lanza excepcion")
        void idNull() {
            assertThatThrownBy(() -> new FalNotificacionIntento(null, 1L, (short)1, CanalNotificacion.PRESENCIAL, null, null, null, null, AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("notificacionId null lanza excepcion")
        void notificacionIdNull() {
            assertThatThrownBy(() -> new FalNotificacionIntento(1L, null, (short)1, CanalNotificacion.PRESENCIAL, null, null, null, null, AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("nroIntento < 1 lanza excepcion")
        void nroIntentoMenorA1() {
            assertThatThrownBy(() -> new FalNotificacionIntento(1L, 1L, (short)0, CanalNotificacion.PRESENCIAL, null, null, null, null, AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("destinoDigital excede 120 chars lanza excepcion")
        void destinoDigitalMaxLen() {
            String largo = "a".repeat(121);
            assertThatThrownBy(() -> new FalNotificacionIntento(1L, 1L, (short)1, CanalNotificacion.EMAIL, null, largo, null, null, AHORA, AHORA, "USR"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("referenciaExterna excede 80 chars lanza excepcion")
        void referenciaExternaMaxLen() {
            assertThatThrownBy(() -> {
                FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
                i.setReferenciaExterna("a".repeat(81));
            }).isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("estado inicial es EN_PROCESO, resultado es null")
        void estadoInicial() {
            FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            assertThat(i.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
            assertThat(i.getResultadoIntento()).isNull();
            assertThat(i.tieneResultado()).isFalse();
        }

        @Test @DisplayName("canal postal con domicilioNotifId se crea correctamente")
        void canalPostalConDomicilio() {
            FalNotificacionIntento i = new FalNotificacionIntento(1L, 10L, (short)1, CanalNotificacion.CORREO_POSTAL,
                    500L, null, null, null, AHORA, AHORA, "USR");
            assertThat(i.getDomicilioNotifId()).isEqualTo(500L);
            assertThat(i.getDestinoDigital()).isNull();
        }

        @Test @DisplayName("canal email con destinoDigital trimmed")
        void canalEmailDestinoTrimmed() {
            FalNotificacionIntento i = new FalNotificacionIntento(1L, 10L, (short)1, CanalNotificacion.EMAIL,
                    null, "  user@mail.com  ", null, null, AHORA, AHORA, "USR");
            assertThat(i.getDestinoDigital()).isEqualTo("user@mail.com");
        }

        @Test @DisplayName("copia es defensiva")
        void copiaDefensiva() {
            FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            FalNotificacionIntento c = i.copia();
            c.setEstadoIntento(EstadoNotificacion.CON_ACUSE_POSITIVO);
            assertThat(i.getEstadoIntento()).isEqualTo(EstadoNotificacion.EN_PROCESO);
        }

        @Test @DisplayName("esPositivo/esNegativo/esVencido/esSuperadoPorPortal")
        void resultadoBooleans() {
            FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            i.setResultadoIntento(ResultadoNotificacion.POSITIVO);
            assertThat(i.esPositivo()).isTrue();
            assertThat(i.esNegativo()).isFalse();

            i.setResultadoIntento(ResultadoNotificacion.SUPERADA_POR_PORTAL);
            assertThat(i.esSuperadoPorPortal()).isTrue();
        }
    }

    @Nested @DisplayName("Repositorio - operaciones basicas")
    class RepoBasico {

        @Test @DisplayName("guardar y buscarPorId retorna copia")
        void guardarYBuscar() {
            FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            repo.guardar(i);
            FalNotificacionIntento found = repo.buscarPorId(1L).orElseThrow();
            assertThat(found.getId()).isEqualTo(1L);
            assertThat(found.getNotificacionId()).isEqualTo(10L);
        }

        @Test @DisplayName("buscarPorId inexistente retorna empty")
        void buscarInexistente() {
            assertThat(repo.buscarPorId(999L)).isEmpty();
        }

        @Test @DisplayName("buscarPorNotificacion retorna todos ordenados por nroIntento")
        void buscarPorNotificacion() {
            repo.guardar(nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL));
            repo.guardar(nuevoIntento(2L, 10L, (short)2, CanalNotificacion.CORREO_POSTAL));
            repo.guardar(nuevoIntento(3L, 99L, (short)1, CanalNotificacion.EMAIL));

            List<FalNotificacionIntento> result = repo.buscarPorNotificacion(10L);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNroIntento()).isEqualTo((short)1);
            assertThat(result.get(1).getNroIntento()).isEqualTo((short)2);
        }

        @Test @DisplayName("buscarPorNroIntento especifico")
        void buscarPorNroIntento() {
            repo.guardar(nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL));
            repo.guardar(nuevoIntento(2L, 10L, (short)2, CanalNotificacion.CORREO_POSTAL));

            assertThat(repo.buscarPorNroIntento(10L, (short)2)).isPresent()
                    .hasValueSatisfying(i -> assertThat(i.getCanalNotif()).isEqualTo(CanalNotificacion.CORREO_POSTAL));
            assertThat(repo.buscarPorNroIntento(10L, (short)99)).isEmpty();
        }

        @Test @DisplayName("buscarPorReferenciaExterna")
        void buscarPorRefExt() {
            FalNotificacionIntento i = nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL);
            i.setReferenciaExterna("REF-001");
            repo.guardar(i);

            assertThat(repo.buscarPorReferenciaExterna("REF-001")).isPresent();
            assertThat(repo.buscarPorReferenciaExterna("REF-OTRO")).isEmpty();
            assertThat(repo.buscarPorReferenciaExterna(null)).isEmpty();
        }

        @Test @DisplayName("reset limpia todo incluyendo correlativo")
        void reset() {
            repo.guardar(nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL));
            repo.reset();
            assertThat(repo.size()).isEqualTo(0);
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)1);
        }
    }

    @Nested @DisplayName("Correlativo atomico")
    class CorrelativoAtomico {

        @Test @DisplayName("primer nroIntento es 1")
        void primerNro() {
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)1);
        }

        @Test @DisplayName("incrementa por notificacion")
        void incrementaPorNotif() {
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)1);
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)2);
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)3);
        }

        @Test @DisplayName("independiente por notificacion")
        void independientePorNotif() {
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)1);
            assertThat(repo.siguienteNroIntento(20L)).isEqualTo((short)1);
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)2);
            assertThat(repo.siguienteNroIntento(20L)).isEqualTo((short)2);
        }

        @Test @DisplayName("correlativo simultaneo - sin duplicados")
        void correlativoSimultaneo() throws InterruptedException {
            int hilos = 10;
            CountDownLatch latch = new CountDownLatch(hilos);
            AtomicInteger errores = new AtomicInteger(0);
            java.util.Set<Short> nros = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

            for (int i = 0; i < hilos; i++) {
                new Thread(() -> {
                    try {
                        short nro = repo.siguienteNroIntento(100L);
                        nros.add(nro);
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
            latch.await(5, TimeUnit.SECONDS);
            assertThat(errores.get()).isEqualTo(0);
            assertThat(nros).hasSize(hilos);
        }

        @Test @DisplayName("cargarSeed reanuda correlativo correctamente")
        void cargarSeedReanuda() {
            List<FalNotificacionIntento> seed = List.of(
                    nuevoIntento(1L, 10L, (short)1, CanalNotificacion.PRESENCIAL),
                    nuevoIntento(2L, 10L, (short)2, CanalNotificacion.CORREO_POSTAL),
                    nuevoIntento(3L, 20L, (short)1, CanalNotificacion.EMAIL)
            );
            repo.cargarSeed(seed);
            assertThat(repo.size()).isEqualTo(3);
            assertThat(repo.siguienteNroIntento(10L)).isEqualTo((short)3);
            assertThat(repo.siguienteNroIntento(20L)).isEqualTo((short)2);
        }
    }
}
