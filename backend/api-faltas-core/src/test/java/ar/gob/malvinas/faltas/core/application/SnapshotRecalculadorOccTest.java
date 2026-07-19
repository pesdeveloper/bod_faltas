package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests focales de SnapshotRecalculador: fail-fast sin repositorio, retry OCC real
 * e instante estable durante el retry.
 */
@DisplayName("SnapshotRecalculador OCC: fail-fast, retry real e instante estable")
class SnapshotRecalculadorOccTest {

    private static final LocalDateTime AHORA_FIJO = LocalDateTime.of(2026, 7, 18, 15, 30);

    private FalActa crearActaEnMemoria() {
        InMemoryActaRepository actaRepo = new InMemoryActaRepository();
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(),
                FaltasClockTestSupport.FIXED.now(),
                "Calle Test", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    private SnapshotRecalculador recalculadorSinSnapshotRepo() {
        return new SnapshotRecalculador(
                new InMemoryActaEventoRepository(),
                new InMemoryDocumentoRepository(),
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED);
    }

    private SnapshotRecalculador recalculadorConSnapshotRepo(ActaSnapshotRepository snapshotRepo) {
        return new SnapshotRecalculador(
                new InMemoryActaEventoRepository(),
                new InMemoryDocumentoRepository(),
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED,
                snapshotRepo);
    }

    // =========================================================
    // 3.1 Fail-fast sin repositorio
    // =========================================================

    @Nested
    @DisplayName("3.1 Fail-fast sin ActaSnapshotRepository")
    class FailFastSinRepositorio {

        @Test
        @DisplayName("recalcularYGuardar sin ActaSnapshotRepository lanza IllegalStateException")
        void sin_snapshot_repo_lanza_illegal_state() {
            SnapshotRecalculador recalc = recalculadorSinSnapshotRepo();
            FalActa acta = crearActaEnMemoria();

            assertThatThrownBy(() -> recalc.recalcularYGuardar(acta))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("mensaje de IllegalStateException contiene 'requiere ActaSnapshotRepository'")
        void mensaje_contiene_requiere_snapshot_repository() {
            SnapshotRecalculador recalc = recalculadorSinSnapshotRepo();
            FalActa acta = crearActaEnMemoria();

            assertThatThrownBy(() -> recalc.recalcularYGuardar(acta))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requiere ActaSnapshotRepository");
        }
    }

    // =========================================================
    // 3.2 Retry OCC real
    // =========================================================

    @Nested
    @DisplayName("3.2 Retry OCC real")
    class RetryOccReal {

        @Test
        @DisplayName("primer guardar lanza ConcurrenciaConflictoException; segundo guarda correctamente")
        void primer_fallo_segundo_exito() {
            InMemoryActaSnapshotRepository realRepo = new InMemoryActaSnapshotRepository();
            AtomicInteger guardarCount = new AtomicInteger(0);

            ActaSnapshotRepository fakeRepo = new ActaSnapshotRepository() {
                @Override
                public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
                    if (guardarCount.incrementAndGet() == 1) {
                        throw new ConcurrenciaConflictoException(
                                "FalActaSnapshot", snapshot.getIdActa(), 0, 0);
                    }
                    return realRepo.guardar(snapshot);
                }

                @Override
                public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
                    return realRepo.buscarPorActa(idActa);
                }
            };

            SnapshotRecalculador recalc = recalculadorConSnapshotRepo(fakeRepo);
            FalActa acta = crearActaEnMemoria();

            FalActaSnapshot resultado = recalc.recalcularYGuardar(acta);

            assertThat(resultado).isNotNull();
        }

        @Test
        @DisplayName("se intento guardar exactamente 2 veces")
        void se_intento_guardar_dos_veces() {
            InMemoryActaSnapshotRepository realRepo = new InMemoryActaSnapshotRepository();
            AtomicInteger guardarCount = new AtomicInteger(0);

            ActaSnapshotRepository fakeRepo = new ActaSnapshotRepository() {
                @Override
                public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
                    if (guardarCount.incrementAndGet() == 1) {
                        throw new ConcurrenciaConflictoException(
                                "FalActaSnapshot", snapshot.getIdActa(), 0, 0);
                    }
                    return realRepo.guardar(snapshot);
                }

                @Override
                public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
                    return realRepo.buscarPorActa(idActa);
                }
            };

            SnapshotRecalculador recalc = recalculadorConSnapshotRepo(fakeRepo);
            FalActa acta = crearActaEnMemoria();

            recalc.recalcularYGuardar(acta);

            assertThat(guardarCount.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("el snapshot queda persistido en el repositorio real tras el retry")
        void snapshot_queda_persistido_tras_retry() {
            InMemoryActaSnapshotRepository realRepo = new InMemoryActaSnapshotRepository();
            AtomicInteger guardarCount = new AtomicInteger(0);

            ActaSnapshotRepository fakeRepo = new ActaSnapshotRepository() {
                @Override
                public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
                    if (guardarCount.incrementAndGet() == 1) {
                        throw new ConcurrenciaConflictoException(
                                "FalActaSnapshot", snapshot.getIdActa(), 0, 0);
                    }
                    return realRepo.guardar(snapshot);
                }

                @Override
                public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
                    return realRepo.buscarPorActa(idActa);
                }
            };

            SnapshotRecalculador recalc = recalculadorConSnapshotRepo(fakeRepo);
            FalActa acta = crearActaEnMemoria();

            recalc.recalcularYGuardar(acta);

            assertThat(realRepo.buscarPorActa(acta.getId())).isPresent();
        }
    }

    // =========================================================
    // 3.3 Instante estable durante retry
    // =========================================================

    @Nested
    @DisplayName("3.3 Instante estable durante retry")
    class InstanteEstableDuranteRetry {

        @Test
        @DisplayName("ultimaActualizacion del snapshot persistido es exactamente ahoraFijo tras conflicto forzado")
        void ultima_actualizacion_es_ahora_fijo_tras_conflicto() {
            InMemoryActaSnapshotRepository realRepo = new InMemoryActaSnapshotRepository();
            AtomicInteger guardarCount = new AtomicInteger(0);

            ActaSnapshotRepository fakeRepo = new ActaSnapshotRepository() {
                @Override
                public FalActaSnapshot guardar(FalActaSnapshot snapshot) {
                    if (guardarCount.incrementAndGet() == 1) {
                        throw new ConcurrenciaConflictoException(
                                "FalActaSnapshot", snapshot.getIdActa(), 0, 0);
                    }
                    return realRepo.guardar(snapshot);
                }

                @Override
                public Optional<FalActaSnapshot> buscarPorActa(Long idActa) {
                    return realRepo.buscarPorActa(idActa);
                }
            };

            SnapshotRecalculador recalc = recalculadorConSnapshotRepo(fakeRepo);
            FalActa acta = crearActaEnMemoria();

            recalc.recalcularYGuardar(acta, AHORA_FIJO);

            FalActaSnapshot persistido = realRepo.buscarPorActa(acta.getId()).orElseThrow();
            assertThat(persistido.getUltimaActualizacion()).isEqualTo(AHORA_FIJO);
        }
    }
}
