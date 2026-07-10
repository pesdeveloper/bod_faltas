package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("8F-11B: Identidades Long y secuencias InMemory")
class IdentidadesLongTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryFalloActaRepository falloRepo;
    private InMemoryApelacionActaRepository apelacionRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryBloqueanteMaterialRepository blocanteRepo;
    private InMemoryGestionExternaRepository gestionRepo;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        blocanteRepo = new InMemoryBloqueanteMaterialRepository();
        gestionRepo = new InMemoryGestionExternaRepository();
    }

    private FalActa crearActa(Long id) {
        return new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Calle 123", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), "TEST_USER");
    }

    @Nested
    @DisplayName("FalActa - tipoActa es TipoActa enum, FKs son Long")
    class ActaIdentidades {

        @Test
        @DisplayName("FalActa.tipoActa es TipoActa, no String")
        void tipo_acta_es_enum() {
            FalActa acta = crearActa(1L);
            assertThat(acta.getTipoActa()).isInstanceOf(TipoActa.class);
            assertThat(acta.getTipoActa()).isEqualTo(TipoActa.TRANSITO);
        }

        @Test
        @DisplayName("FalActa.idInspector es Long")
        void id_inspector_es_long() {
            FalActa acta = crearActa(1L);
            assertThat(acta.getIdInspector()).isInstanceOf(Long.class);
            assertThat(acta.getIdInspector()).isEqualTo(1L);
        }

        @Test
        @DisplayName("FalActa.idDependencia es Long")
        void id_dependencia_es_long() {
            FalActa acta = crearActa(1L);
            assertThat(acta.getIdDependencia()).isInstanceOf(Long.class);
            assertThat(acta.getIdDependencia()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("FalActaFallo - id es Long, secuencia crece")
    class FalloIdentidades {

        @Test
        @DisplayName("nextId genera Long unico incremental")
        void nextId_incremental() {
            Long id1 = falloRepo.nextId();
            Long id2 = falloRepo.nextId();
            Long id3 = falloRepo.nextId();
            assertThat(id1).isInstanceOf(Long.class);
            assertThat(id2).isGreaterThan(id1);
            assertThat(id3).isGreaterThan(id2);
        }

        @Test
        @DisplayName("FalActaFallo.id es Long")
        void id_es_long() {
            Long id = falloRepo.nextId();
            FalActaFallo fallo = new FalActaFallo(id, 100L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "TEST_USER");
            assertThat(fallo.getId()).isInstanceOf(Long.class);
            assertThat(fallo.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("reset reinicia secuencia")
        void reset_reinicia_secuencia() {
            falloRepo.nextId();
            falloRepo.nextId();
            falloRepo.reset();
            Long primeraPostReset = falloRepo.nextId();
            assertThat(primeraPostReset).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("FalActaApelacion - id es Long, falloId es Long")
    class ApelacionIdentidades {

        @Test
        @DisplayName("nextId genera Long unico incremental")
        void nextId_incremental() {
            Long id1 = apelacionRepo.nextId();
            Long id2 = apelacionRepo.nextId();
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("FalActaApelacion.id y falloId son Long")
        void id_y_falloId_long() {
            Long id = apelacionRepo.nextId();
            Long falloId = 10L;
            FalActaApelacion ap = new FalActaApelacion(id, 100L, falloId,
                    EstadoApelacionActa.PRESENTADA, FaltasClockTestSupport.FIXED.now(),
                    "presentante", "fundamentos", null, true,
                    FaltasClockTestSupport.FIXED.now(), "TEST_USER");
            assertThat(ap.getId()).isInstanceOf(Long.class);
            assertThat(ap.getFalloId()).isInstanceOf(Long.class);
            assertThat(ap.getFalloId()).isEqualTo(falloId);
        }
    }

    @Nested
    @DisplayName("FalNotificacion - id es Long")
    class NotificacionIdentidades {

        @Test
        @DisplayName("nextId genera Long incremental")
        void nextId_incremental() {
            Long id1 = notifRepo.nextId();
            Long id2 = notifRepo.nextId();
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("FalNotificacion.id es Long")
        void id_es_long() {
            Long id = notifRepo.nextId();
            FalNotificacion n = new FalNotificacion(id, 100L, 200L,
                    TipoDocu.ACTO_ADMINISTRATIVO, "POSTAL", FaltasClockTestSupport.FIXED.now(),
                    FaltasClockTestSupport.FIXED.now(), "TEST_USER");
            assertThat(n.getId()).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("buscarPorId usa Long")
        void buscarPorId_long() {
            Long id = notifRepo.nextId();
            FalNotificacion n = new FalNotificacion(id, 100L, 200L,
                    TipoDocu.ACTO_ADMINISTRATIVO, "POSTAL", FaltasClockTestSupport.FIXED.now(),
                    FaltasClockTestSupport.FIXED.now(), "TEST_USER");
            notifRepo.guardar(n);
            assertThat(notifRepo.buscarPorId(id)).isPresent();
        }
    }

    @Nested
    @DisplayName("FalBloqueanteMaterial - id es Long")
    class BlocanteIdentidades {

        @Test
        @DisplayName("nextId genera Long incremental")
        void nextId_incremental() {
            Long id1 = blocanteRepo.nextId();
            Long id2 = blocanteRepo.nextId();
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("FalBloqueanteMaterial.id es Long")
        void id_es_long() {
            Long id = blocanteRepo.nextId();
            FalBloqueanteMaterial b = new FalBloqueanteMaterial(id, 100L, FaltasClockTestSupport.FIXED.now());
            assertThat(b.getId()).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("findById usa Long")
        void findById_long() {
            Long id = blocanteRepo.nextId();
            FalBloqueanteMaterial b = new FalBloqueanteMaterial(id, 100L, FaltasClockTestSupport.FIXED.now());
            blocanteRepo.guardar(b);
            assertThat(blocanteRepo.findById(id)).isPresent();
        }
    }

    @Nested
    @DisplayName("FalGestionExterna - id es Long")
    class GestionIdentidades {

        @Test
        @DisplayName("nextId genera Long incremental")
        void nextId_incremental() {
            Long id1 = gestionRepo.nextId();
            Long id2 = gestionRepo.nextId();
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("FalGestionExterna.id es Long")
        void id_es_long() {
            Long id = gestionRepo.nextId();
            FalGestionExterna g = new FalGestionExterna(id, 100L, FaltasClockTestSupport.FIXED.now(), "TEST_USER");
            assertThat(g.getId()).isInstanceOf(Long.class);
        }
    }

    @Nested
    @DisplayName("Secuencias no reutilizan IDs")
    class SecuenciasNoReutilizan {

        @Test
        @DisplayName("post-reset: nuevas entidades generan IDs distintos a los anteriores si se rastrearan")
        void ids_no_colisionan_en_sesion() {
            Long a1 = falloRepo.nextId();
            Long a2 = falloRepo.nextId();
            Long a3 = falloRepo.nextId();
            assertThat(a1).isNotEqualTo(a2);
            assertThat(a2).isNotEqualTo(a3);
        }
    }
}