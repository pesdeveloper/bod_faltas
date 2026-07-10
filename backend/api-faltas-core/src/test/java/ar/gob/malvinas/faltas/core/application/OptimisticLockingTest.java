package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("8F-11B: versionRow y control de concurrencia optimista")
class OptimisticLockingTest {

    // =========================================================
    // FalActa - aggregate raiz
    // =========================================================

    @Nested
    @DisplayName("FalActa - versionRow OCC")
    class ActaOCC {

        private InMemoryActaRepository repo;

        @BeforeEach
        void setUp() { repo = new InMemoryActaRepository(); }

        private FalActa crearActa() {
            Long id = repo.nextId();
            FalActa acta = new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Calle", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                    FaltasClockTestSupport.FIXED.now(), "TEST");
            return repo.guardar(acta);
        }

        @Test
        @DisplayName("creacion queda en version 0")
        void creacion_version_0() {
            FalActa guardada = crearActa();
            assertThat(guardada.getVersionRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("update valido pasa a version 1")
        void update_pasa_a_v1() {
            FalActa guardada = crearActa();
            FalActa leida = repo.buscarPorId(guardada.getId()).orElseThrow();
            leida.setBloqueActual(BloqueActual.ANAL);
            FalActa actualizada = repo.guardar(leida);
            assertThat(actualizada.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("segundo update valido pasa a version 2")
        void segundo_update_pasa_a_v2() {
            FalActa v0 = crearActa();
            FalActa v1 = repo.guardar(repo.buscarPorId(v0.getId()).orElseThrow());
            FalActa leida = repo.buscarPorId(v0.getId()).orElseThrow();
            assertThat(leida.getVersionRow()).isEqualTo(1);
            FalActa v2 = repo.guardar(leida);
            assertThat(v2.getVersionRow()).isEqualTo(2);
        }

        @Test
        @DisplayName("dos lecturas de v0: la primera guarda v1, la segunda falla por version obsoleta")
        void concurrencia_falla_version_obsoleta() {
            FalActa original = crearActa();
            // Dos lecturas independientes (copias independientes)
            FalActa lectorA = repo.buscarPorId(original.getId()).orElseThrow();
            FalActa lectorB = repo.buscarPorId(original.getId()).orElseThrow();
            assertThat(lectorA.getVersionRow()).isEqualTo(0);
            assertThat(lectorB.getVersionRow()).isEqualTo(0);

            // A guarda primero: OK, version pasa a 1
            lectorA.setBloqueActual(BloqueActual.ANAL);
            repo.guardar(lectorA);

            // B intenta guardar con version 0 pero store tiene version 1: FALLA
            lectorB.setBloqueActual(BloqueActual.NOTI);
            assertThatThrownBy(() -> repo.guardar(lectorB))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("estado de la primera actualizacion se conserva tras el fallo de la segunda")
        void estado_primera_conservado() {
            FalActa original = crearActa();
            FalActa lectorA = repo.buscarPorId(original.getId()).orElseThrow();
            FalActa lectorB = repo.buscarPorId(original.getId()).orElseThrow();

            lectorA.setBloqueActual(BloqueActual.ANAL);
            repo.guardar(lectorA);

            lectorB.setBloqueActual(BloqueActual.NOTI);
            try { repo.guardar(lectorB); } catch (ConcurrenciaConflictoException ignored) {}

            FalActa final_ = repo.buscarPorId(original.getId()).orElseThrow();
            assertThat(final_.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }

        @Test
        @DisplayName("mutacion sin save no altera el store")
        void mutacion_sin_save_no_altera_store() {
            FalActa guardada = crearActa();
            FalActa leida = repo.buscarPorId(guardada.getId()).orElseThrow();
            leida.setBloqueActual(BloqueActual.ANAL);
            // Sin guardar: el store debe tener el bloque original (CAPT)
            FalActa enStore = repo.buscarPorId(guardada.getId()).orElseThrow();
            assertThat(enStore.getBloqueActual()).isEqualTo(BloqueActual.CAPT);
        }

        @Test
        @DisplayName("operacion fallida no incrementa version")
        void fallo_no_incrementa_version() {
            FalActa original = crearActa();
            FalActa lectorA = repo.buscarPorId(original.getId()).orElseThrow();
            FalActa lectorB = repo.buscarPorId(original.getId()).orElseThrow();

            repo.guardar(lectorA); // store v1

            // intento fallido con lectorB (v0)
            try { repo.guardar(lectorB); } catch (ConcurrenciaConflictoException ignored) {}

            // store sigue en v1 (no fue a v2 por el intento fallido)
            FalActa postFallo = repo.buscarPorId(original.getId()).orElseThrow();
            assertThat(postFallo.getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // FalActaFallo - aggregate secundario
    // =========================================================

    @Nested
    @DisplayName("FalActaFallo - versionRow OCC")
    class FalloOCC {

        private InMemoryFalloActaRepository repo;
        private long actaIdFijo = 100L;

        @BeforeEach
        void setUp() { repo = new InMemoryFalloActaRepository(); }

        private FalActaFallo crearFallo() {
            Long id = repo.nextId();
            FalActaFallo fallo = new FalActaFallo(id, actaIdFijo, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "TEST");
            return repo.guardar(fallo);
        }

        @Test
        @DisplayName("creacion queda en version 0")
        void creacion_version_0() {
            FalActaFallo guardado = crearFallo();
            assertThat(guardado.getVersionRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("update valido pasa a version 1")
        void update_v1() {
            FalActaFallo guardado = crearFallo();
            FalActaFallo leido = repo.buscarActivo(actaIdFijo).orElseThrow();
            leido.setEstadoFallo(EstadoFalloActa.FIRMADO);
            FalActaFallo actualizado = repo.guardar(leido);
            assertThat(actualizado.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("dos lecturas v0: primera guarda v1, segunda falla")
        void concurrencia_fallo() {
            crearFallo();
            FalActaFallo a = repo.buscarActivo(actaIdFijo).orElseThrow();
            FalActaFallo b = repo.buscarActivo(actaIdFijo).orElseThrow();

            a.setEstadoFallo(EstadoFalloActa.FIRMADO);
            repo.guardar(a);

            b.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
            assertThatThrownBy(() -> repo.guardar(b))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("mutacion sin save no altera store")
        void mutacion_sin_save_no_altera() {
            crearFallo();
            FalActaFallo leido = repo.buscarActivo(actaIdFijo).orElseThrow();
            leido.setEstadoFallo(EstadoFalloActa.FIRMADO);

            FalActaFallo enStore = repo.buscarActivo(actaIdFijo).orElseThrow();
            assertThat(enStore.getEstadoFallo()).isEqualTo(EstadoFalloActa.PENDIENTE_FIRMA);
        }
    }
}