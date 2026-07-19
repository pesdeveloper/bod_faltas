package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEconomiaProyeccion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalGestionExterna;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryBloqueanteMaterialRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryEconomiaProyeccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryGestionExternaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de OCC estricto en INSERT: versionRow != 0 en entidad nueva debe lanzar
 * ConcurrenciaConflictoException y la entidad no debe quedar insertada.
 *
 * Cubre los seis repositorios InMemory identificados en R4:
 *   InMemoryActaSnapshotRepository, InMemoryDocumentoRepository,
 *   InMemoryNotificacionRepository, InMemoryGestionExternaRepository,
 *   InMemoryBloqueanteMaterialRepository, InMemoryEconomiaProyeccionRepository.
 */
@DisplayName("OCC estricto en INSERT: versionRow != 0 rechazado por los 6 repositorios InMemory")
class OccInsertVersionRowTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 18, 12, 0);

    // =========================================================
    // 1. InMemoryActaSnapshotRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryActaSnapshotRepository - INSERT con versionRow = 1")
    class ActaSnapshotOccInsert {

        @Test
        @DisplayName("guardar snapshot nuevo con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryActaSnapshotRepository repo = new InMemoryActaSnapshotRepository();

            FalActaSnapshot snap = new FalActaSnapshot(101L);
            snap.setVersionRow(1);

            assertThatThrownBy(() -> repo.guardar(snap))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("entidad con versionRow=1 NO queda insertada tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryActaSnapshotRepository repo = new InMemoryActaSnapshotRepository();

            FalActaSnapshot snap = new FalActaSnapshot(101L);
            snap.setVersionRow(1);

            try { repo.guardar(snap); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.buscarPorActa(101L)).isEmpty();
        }
    }

    // =========================================================
    // 2. InMemoryDocumentoRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryDocumentoRepository - INSERT con versionRow = 1")
    class DocumentoOccInsert {

        @Test
        @DisplayName("guardar documento nuevo con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryDocumentoRepository repo = new InMemoryDocumentoRepository();

            FalDocumento doc = new FalDocumento(1L, 1L, TipoDocu.ACTA_INFRACCION, AHORA, "Desc");
            doc.setVersionRow(1);

            assertThatThrownBy(() -> repo.guardar(doc))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("documento con versionRow=1 NO queda insertado tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryDocumentoRepository repo = new InMemoryDocumentoRepository();

            FalDocumento doc = new FalDocumento(1L, 1L, TipoDocu.ACTA_INFRACCION, AHORA, "Desc");
            doc.setVersionRow(1);

            try { repo.guardar(doc); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.buscarPorId(1L)).isEmpty();
        }
    }

    // =========================================================
    // 3. InMemoryNotificacionRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryNotificacionRepository - INSERT con versionRow = 1")
    class NotificacionOccInsert {

        @Test
        @DisplayName("guardar notificacion nueva con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryNotificacionRepository repo = new InMemoryNotificacionRepository();

            FalNotificacion notif = new FalNotificacion(
                    1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, "POSTAL", AHORA);
            notif.setVersionRow(1);

            assertThatThrownBy(() -> repo.guardar(notif))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("notificacion con versionRow=1 NO queda insertada tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryNotificacionRepository repo = new InMemoryNotificacionRepository();

            FalNotificacion notif = new FalNotificacion(
                    1L, 1L, 1L, TipoDocu.NOTIFICACION_ACTA, "POSTAL", AHORA);
            notif.setVersionRow(1);

            try { repo.guardar(notif); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.buscarPorId(1L)).isEmpty();
        }
    }

    // =========================================================
    // 4. InMemoryGestionExternaRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryGestionExternaRepository - INSERT con versionRow = 1")
    class GestionExternaOccInsert {

        @Test
        @DisplayName("guardar gestion externa nueva con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryGestionExternaRepository repo = new InMemoryGestionExternaRepository();

            FalGestionExterna gestion = new FalGestionExterna(1L, 1L, AHORA, "TEST");
            gestion.setVersionRow(1);

            assertThatThrownBy(() -> repo.guardar(gestion))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("gestion externa con versionRow=1 NO queda insertada tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryGestionExternaRepository repo = new InMemoryGestionExternaRepository();

            FalGestionExterna gestion = new FalGestionExterna(1L, 1L, AHORA, "TEST");
            gestion.setVersionRow(1);

            try { repo.guardar(gestion); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.buscarPorHistorico(1L)).isEmpty();
        }
    }

    // =========================================================
    // 5. InMemoryBloqueanteMaterialRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryBloqueanteMaterialRepository - INSERT con versionRow = 1")
    class BloqueanteMaterialOccInsert {

        @Test
        @DisplayName("guardar bloqueante material nuevo con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryBloqueanteMaterialRepository repo = new InMemoryBloqueanteMaterialRepository();

            FalBloqueanteMaterial bloqueante = new FalBloqueanteMaterial(1L, 1L, AHORA);
            bloqueante.setVersionRow(1);

            assertThatThrownBy(() -> repo.guardar(bloqueante))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("bloqueante material con versionRow=1 NO queda insertado tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryBloqueanteMaterialRepository repo = new InMemoryBloqueanteMaterialRepository();

            FalBloqueanteMaterial bloqueante = new FalBloqueanteMaterial(1L, 1L, AHORA);
            bloqueante.setVersionRow(1);

            try { repo.guardar(bloqueante); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.findById(1L)).isEmpty();
        }
    }

    // =========================================================
    // 6. InMemoryEconomiaProyeccionRepository
    // =========================================================

    @Nested
    @DisplayName("InMemoryEconomiaProyeccionRepository - INSERT con versionRow = 1")
    class EconomiaProyeccionOccInsert {

        @Test
        @DisplayName("guardar proyeccion nueva con versionRow=1 lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_lanza_excepcion() {
            InMemoryEconomiaProyeccionRepository repo = new InMemoryEconomiaProyeccionRepository();

            FalActaEconomiaProyeccion proyeccion = new FalActaEconomiaProyeccion(1L);
            proyeccion.setVersionRow(1);
            proyeccion.setFhUltMod(AHORA);

            assertThatThrownBy(() -> repo.save(proyeccion))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("proyeccion con versionRow=1 NO queda insertada tras el rechazo")
        void insert_rechazado_no_queda_en_store() {
            InMemoryEconomiaProyeccionRepository repo = new InMemoryEconomiaProyeccionRepository();

            FalActaEconomiaProyeccion proyeccion = new FalActaEconomiaProyeccion(1L);
            proyeccion.setVersionRow(1);
            proyeccion.setFhUltMod(AHORA);

            try { repo.save(proyeccion); } catch (ConcurrenciaConflictoException ignored) {}

            assertThat(repo.findByActaId(1L)).isEmpty();
        }
    }
}
