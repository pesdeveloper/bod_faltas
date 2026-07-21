package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoRedaccion;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OCC para FalDocumentoRedaccion / InMemoryDocumentoRedaccionRepository.
 *
 * Verifica los 10 requisitos funcionales del prompt FULL-R1.2-CORRECCION-06:
 *   1. alta con versionRow = 0
 *   2. actualizacion con version correcta
 *   3. incremento 0 -> 1
 *   4. segunda actualizacion e incremento 1 -> 2
 *   5. rechazo de actualizacion con version antigua
 *   6. ausencia de mutacion cuando hay conflicto
 *   7. repositorio devuelve la version actual
 *   8. reset conserva el contrato OCC
 *   9. paridad DDL: version_row INT NOT NULL DEFAULT 0
 *  10. ausencia de constructores con aridad antigua (no aplicable como test de runtime)
 */
@DisplayName("OCC FalDocumentoRedaccion")
class DocumentoRedaccionOccTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 20, 12, 0);
    private InMemoryDocumentoRedaccionRepository repo;

    @BeforeEach
    void setUp() { repo = new InMemoryDocumentoRedaccionRepository(); }

    private FalDocumentoRedaccion nuevaRedaccion(Long id, Long idDocumento) {
        return new FalDocumentoRedaccion(
                id, idDocumento, 10L,
                (short) 1, null,
                EstadoRedaccionDocumento.BORRADOR, "Contenido inicial.",
                "{}", null, null,
                AHORA, "usr-test",
                AHORA, "usr-test",
                null, null);
    }

    // =========================================================
    // 1. Alta con versionRow = 0
    // =========================================================

    @Nested
    @DisplayName("1. Alta con versionRow = 0")
    class AltaVersionCero {

        @Test
        @DisplayName("nueva redaccion queda con versionRow=0 tras guardar")
        void alta_version_0() {
            FalDocumentoRedaccion r = nuevaRedaccion(1L, 100L);
            FalDocumentoRedaccion guardada = repo.guardar(r);
            assertThat(guardada.getVersionRow()).isEqualTo(0);
        }
    }

    // =========================================================
    // 2. Actualizacion con version correcta
    // =========================================================

    @Nested
    @DisplayName("2. Actualizacion con version correcta")
    class ActualizacionVersionCorrecta {

        @Test
        @DisplayName("guardar con version coincidente no lanza excepcion")
        void actualizacion_version_correcta_no_falla() {
            repo.guardar(nuevaRedaccion(1L, 100L));
            FalDocumentoRedaccion leida = repo.buscarPorId(1L).orElseThrow();
            leida.setContenidoEditable("Contenido editado.");
            repo.guardar(leida);
        }
    }

    // =========================================================
    // 3. Incremento 0 -> 1
    // =========================================================

    @Nested
    @DisplayName("3. Incremento 0 -> 1")
    class IncrementoV0V1 {

        @Test
        @DisplayName("primer update pasa versionRow de 0 a 1")
        void primer_update_pasa_a_v1() {
            repo.guardar(nuevaRedaccion(1L, 100L));
            FalDocumentoRedaccion leida = repo.buscarPorId(1L).orElseThrow();
            assertThat(leida.getVersionRow()).isEqualTo(0);

            FalDocumentoRedaccion actualizada = repo.guardar(leida);
            assertThat(actualizada.getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // 4. Segunda actualizacion: 1 -> 2
    // =========================================================

    @Nested
    @DisplayName("4. Segunda actualizacion 1 -> 2")
    class IncrementoV1V2 {

        @Test
        @DisplayName("segundo update pasa versionRow de 1 a 2")
        void segundo_update_pasa_a_v2() {
            repo.guardar(nuevaRedaccion(1L, 100L));

            FalDocumentoRedaccion v0 = repo.buscarPorId(1L).orElseThrow();
            repo.guardar(v0);

            FalDocumentoRedaccion v1 = repo.buscarPorId(1L).orElseThrow();
            assertThat(v1.getVersionRow()).isEqualTo(1);

            FalDocumentoRedaccion v2 = repo.guardar(v1);
            assertThat(v2.getVersionRow()).isEqualTo(2);
        }
    }

    // =========================================================
    // 5. Rechazo de version antigua
    // =========================================================

    @Nested
    @DisplayName("5. Rechazo de version antigua")
    class RechazoVersionAntigua {

        @Test
        @DisplayName("dos lecturas v0: la primera actualiza a v1, la segunda falla con ConcurrenciaConflictoException")
        void concurrencia_rechaza_version_obsoleta() {
            repo.guardar(nuevaRedaccion(1L, 100L));

            FalDocumentoRedaccion lectorA = repo.buscarPorId(1L).orElseThrow();
            FalDocumentoRedaccion lectorB = repo.buscarPorId(1L).orElseThrow();

            repo.guardar(lectorA);

            assertThatThrownBy(() -> repo.guardar(lectorB))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }
    }

    // =========================================================
    // 6. Ausencia de mutacion cuando hay conflicto
    // =========================================================

    @Nested
    @DisplayName("6. Sin mutacion al conflicto")
    class SinMutacionEnConflicto {

        @Test
        @DisplayName("el contenido del store no se altera cuando el update es rechazado")
        void contenido_no_muta_en_conflicto() {
            repo.guardar(nuevaRedaccion(1L, 100L));

            FalDocumentoRedaccion lectorA = repo.buscarPorId(1L).orElseThrow();
            FalDocumentoRedaccion lectorB = repo.buscarPorId(1L).orElseThrow();

            lectorA.setContenidoEditable("Edicion de A.");
            repo.guardar(lectorA);

            lectorB.setContenidoEditable("Edicion conflictiva de B.");
            try { repo.guardar(lectorB); } catch (ConcurrenciaConflictoException ignored) {}

            FalDocumentoRedaccion postConflicto = repo.buscarPorId(1L).orElseThrow();
            assertThat(postConflicto.getContenidoEditable()).isEqualTo("Edicion de A.");
            assertThat(postConflicto.getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // 7. Repositorio devuelve la version actual
    // =========================================================

    @Nested
    @DisplayName("7. Repositorio devuelve version actual")
    class RepoDevuelveVersionActual {

        @Test
        @DisplayName("buscarPorId devuelve la version almacenada correcta")
        void repo_devuelve_version_actual() {
            repo.guardar(nuevaRedaccion(1L, 100L));

            FalDocumentoRedaccion leida = repo.buscarPorId(1L).orElseThrow();
            repo.guardar(leida);

            FalDocumentoRedaccion post = repo.buscarPorId(1L).orElseThrow();
            assertThat(post.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("buscarPorDocumento devuelve la version almacenada correcta")
        void buscar_por_documento_devuelve_version_actual() {
            repo.guardar(nuevaRedaccion(1L, 100L));

            FalDocumentoRedaccion leida = repo.buscarPorId(1L).orElseThrow();
            repo.guardar(leida);

            FalDocumentoRedaccion desde_lista = repo.buscarPorDocumento(100L).get(0);
            assertThat(desde_lista.getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // 8. Reset conserva el contrato OCC
    // =========================================================

    @Nested
    @DisplayName("8. Reset conserva contrato")
    class ResetConservaContrato {

        @Test
        @DisplayName("despues del reset, una nueva alta comienza con versionRow=0")
        void reset_y_nueva_alta_version_0() {
            repo.guardar(nuevaRedaccion(1L, 100L));
            repo.reset();
            assertThat(repo.buscarPorId(1L)).isEmpty();

            FalDocumentoRedaccion nueva = nuevaRedaccion(2L, 200L);
            repo.guardar(nueva);
            assertThat(repo.buscarPorId(2L).orElseThrow().getVersionRow()).isEqualTo(0);
        }
    }

    // =========================================================
    // 9. Paridad DDL: version_row INT NOT NULL DEFAULT 0
    // =========================================================

    @Nested
    @DisplayName("9. Paridad DDL version_row")
    class ParidadDdl {

        @Test
        @DisplayName("FalDocumentoRedaccion.getVersionRow() inicia en 0 (DDL: version_row INT NOT NULL DEFAULT 0)")
        void version_row_default_0() {
            FalDocumentoRedaccion r = nuevaRedaccion(1L, 100L);
            assertThat(r.getVersionRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("guardar version != 0 en INSERT lanza ConcurrenciaConflictoException")
        void insert_con_version_no_cero_rechazado() {
            FalDocumentoRedaccion r = nuevaRedaccion(1L, 100L);
            r.setVersionRow(1);
            assertThatThrownBy(() -> repo.guardar(r))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("entidad rechazada por INSERT no queda en el store")
        void insert_rechazado_no_queda_en_store() {
            FalDocumentoRedaccion r = nuevaRedaccion(1L, 100L);
            r.setVersionRow(1);
            try { repo.guardar(r); } catch (ConcurrenciaConflictoException ignored) {}
            assertThat(repo.buscarPorId(1L)).isEmpty();
        }
    }
}
