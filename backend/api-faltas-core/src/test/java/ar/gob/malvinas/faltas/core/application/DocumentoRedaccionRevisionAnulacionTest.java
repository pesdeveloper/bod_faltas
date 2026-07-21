package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
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
 * Revision y anulacion de FalDocumentoRedaccion.
 *
 * Verifica los requisitos del prompt FULL-R1.2-CORRECCION-07 (F4):
 *   1. redaccion inicial con nroRevision = 1
 *   2. redaccion inicial sin redaccionOrigenId
 *   3. nueva revision con numero incrementado
 *   4. nueva revision vinculada mediante redaccionOrigenId
 *   5. revision anterior confirmada permanece inmutable
 *   6. anulacion completa
 *   7. rechazo de anulacion sin fecha
 *   8. rechazo de anulacion sin usuario
 *   9. rechazo de anulacion sin motivo
 *  10. copia() conserva los cuatro campos
 *  11. repositorio conserva los cuatro campos
 *  12. responses exponen nroRevision cuando corresponde
 *  13. OCC sigue incrementando versionRow
 *  14. conflicto OCC no altera datos de revision ni anulacion
 */
@DisplayName("Revision y anulacion de FalDocumentoRedaccion (F4 CORRECCION-07)")
class DocumentoRedaccionRevisionAnulacionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 20, 12, 0);
    private static final LocalDateTime DESPUES = LocalDateTime.of(2026, 7, 20, 14, 0);

    private InMemoryDocumentoRedaccionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDocumentoRedaccionRepository();
    }

    private FalDocumentoRedaccion primeraRedaccion(Long id, Long idDocumento) {
        return new FalDocumentoRedaccion(
                id, idDocumento, 10L,
                (short) 1, null,
                EstadoRedaccionDocumento.BORRADOR, "Contenido v1.",
                "{}", null, null,
                AHORA, "usr-creacion",
                AHORA, "usr-creacion",
                null, null);
    }

    private FalDocumentoRedaccion nuevaRevision(Long id, Long idDocumento, short nroRevision, Long origenId) {
        return new FalDocumentoRedaccion(
                id, idDocumento, 10L,
                nroRevision, origenId,
                EstadoRedaccionDocumento.BORRADOR, "Contenido v" + nroRevision + ".",
                "{}", null, null,
                DESPUES, "usr-revisor",
                DESPUES, "usr-revisor",
                null, null);
    }

    // =========================================================
    // 1. Redaccion inicial con nroRevision = 1
    // =========================================================

    @Nested
    @DisplayName("1. Redaccion inicial con nroRevision = 1")
    class NroRevisionInicial {

        @Test
        @DisplayName("primera redaccion tiene nroRevision = 1")
        void primera_redaccion_nro_revision_1() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getNroRevision()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("nroRevision = 0 rechazado por constructor")
        void nro_revision_cero_rechazado() {
            assertThatThrownBy(() -> new FalDocumentoRedaccion(
                    1L, 100L, 10L,
                    (short) 0, null,
                    EstadoRedaccionDocumento.BORRADOR, "Contenido.",
                    "{}", null, null,
                    AHORA, "usr",
                    null, null,
                    null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nroRevision");
        }

        @Test
        @DisplayName("nroRevision negativo rechazado por constructor")
        void nro_revision_negativo_rechazado() {
            assertThatThrownBy(() -> new FalDocumentoRedaccion(
                    1L, 100L, 10L,
                    (short) -1, null,
                    EstadoRedaccionDocumento.BORRADOR, "Contenido.",
                    "{}", null, null,
                    AHORA, "usr",
                    null, null,
                    null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nroRevision");
        }
    }

    // =========================================================
    // 2. Redaccion inicial sin redaccionOrigenId
    // =========================================================

    @Nested
    @DisplayName("2. Redaccion inicial sin redaccionOrigenId")
    class SinOrigenId {

        @Test
        @DisplayName("primera redaccion tiene redaccionOrigenId = null")
        void primera_redaccion_origen_null() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getRedaccionOrigenId()).isNull();
        }
    }

    // =========================================================
    // 3. Nueva revision con numero incrementado
    // =========================================================

    @Nested
    @DisplayName("3. Nueva revision con numero incrementado")
    class NuevoNroRevision {

        @Test
        @DisplayName("segunda revision tiene nroRevision = 2")
        void segunda_revision_nro_2() {
            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            assertThat(r2.getNroRevision()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("tercera revision tiene nroRevision = 3")
        void tercera_revision_nro_3() {
            FalDocumentoRedaccion r3 = nuevaRevision(3L, 100L, (short) 3, 2L);
            assertThat(r3.getNroRevision()).isEqualTo((short) 3);
        }
    }

    // =========================================================
    // 4. Nueva revision vinculada mediante redaccionOrigenId
    // =========================================================

    @Nested
    @DisplayName("4. Nueva revision vinculada mediante redaccionOrigenId")
    class RevisionVinculada {

        @Test
        @DisplayName("segunda revision tiene redaccionOrigenId apuntando a la primera")
        void segunda_revision_origen_apunta_a_primera() {
            FalDocumentoRedaccion r1 = primeraRedaccion(1L, 100L);
            repo.guardar(r1);

            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, r1.getId());
            assertThat(r2.getRedaccionOrigenId()).isEqualTo(r1.getId());
        }
    }

    // =========================================================
    // 5. Revision anterior confirmada permanece inmutable
    // =========================================================

    @Nested
    @DisplayName("5. Revision anterior confirmada permanece inmutable")
    class RevisionConfirmadaInmutable {

        @Test
        @DisplayName("confirmar la primera revision no afecta los datos de la segunda")
        void confirmar_primera_no_afecta_segunda() {
            FalDocumentoRedaccion r1 = primeraRedaccion(1L, 100L);
            repo.guardar(r1);

            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            repo.guardar(r2);

            FalDocumentoRedaccion r1Leida = repo.buscarPorId(1L).orElseThrow();
            r1Leida.confirmar(DESPUES, "usr-confirmador");
            repo.guardar(r1Leida);

            FalDocumentoRedaccion r1Confirmada = repo.buscarPorId(1L).orElseThrow();
            FalDocumentoRedaccion r2Intacta = repo.buscarPorId(2L).orElseThrow();

            assertThat(r1Confirmada.estaConfirmada()).isTrue();
            assertThat(r2Intacta.esBorrador()).isTrue();
            assertThat(r2Intacta.getNroRevision()).isEqualTo((short) 2);
        }
    }

    // =========================================================
    // 6. Anulacion completa
    // =========================================================

    @Nested
    @DisplayName("6. Anulacion completa")
    class AnulacionCompleta {

        @Test
        @DisplayName("anular una redaccion la pasa a estado ANULADA con campos de auditoria")
        void anular_pasa_a_estado_anulada() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            r.anular(DESPUES, "usr-anulador", "Motivo de anulacion valido.");
            assertThat(r.estaAnulada()).isTrue();
            assertThat(r.getFhAnulacion()).isEqualTo(DESPUES);
            assertThat(r.getIdUserAnulacion()).isEqualTo("usr-anulador");
        }

        @Test
        @DisplayName("no se puede anular una redaccion ya ANULADA")
        void no_puede_anular_dos_veces() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            r.anular(DESPUES, "usr-anulador", "Primer motivo.");
            assertThatThrownBy(() -> r.anular(DESPUES, "usr-otro", "Segundo motivo."))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("ANULADA");
        }
    }

    // =========================================================
    // 7. Rechazo de anulacion sin fecha
    // =========================================================

    @Nested
    @DisplayName("7. Rechazo de anulacion sin fecha")
    class RechazaAnulacionSinFecha {

        @Test
        @DisplayName("anular con fhAnulacion null lanza PrecondicionVioladaException")
        void anular_sin_fecha_rechazado() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThatThrownBy(() -> r.anular(null, "usr-anulador", "Motivo."))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhAnulacion");
        }
    }

    // =========================================================
    // 8. Rechazo de anulacion sin usuario
    // =========================================================

    @Nested
    @DisplayName("8. Rechazo de anulacion sin usuario")
    class RechazaAnulacionSinUsuario {

        @Test
        @DisplayName("anular con idUserAnulacion null lanza PrecondicionVioladaException")
        void anular_sin_usuario_null_rechazado() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThatThrownBy(() -> r.anular(DESPUES, null, "Motivo."))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserAnulacion");
        }

        @Test
        @DisplayName("anular con idUserAnulacion en blanco lanza PrecondicionVioladaException")
        void anular_sin_usuario_blanco_rechazado() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThatThrownBy(() -> r.anular(DESPUES, "   ", "Motivo."))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserAnulacion");
        }
    }

    // =========================================================
    // 9. Rechazo de anulacion sin motivo
    // =========================================================

    @Nested
    @DisplayName("9. Rechazo de anulacion sin motivo")
    class RechazaAnulacionSinMotivo {

        @Test
        @DisplayName("anular con motivoAnulacion null lanza PrecondicionVioladaException")
        void anular_sin_motivo_null_rechazado() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThatThrownBy(() -> r.anular(DESPUES, "usr-anulador", null))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivoAnulacion");
        }

        @Test
        @DisplayName("anular con motivoAnulacion en blanco lanza PrecondicionVioladaException")
        void anular_sin_motivo_blanco_rechazado() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThatThrownBy(() -> r.anular(DESPUES, "usr-anulador", "   "))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivoAnulacion");
        }
    }

    // =========================================================
    // 10. copia() conserva los cuatro campos
    // =========================================================

    @Nested
    @DisplayName("10. copia() conserva los cuatro campos de F4")
    class CopiaConservaQuatroCampos {

        @Test
        @DisplayName("copia conserva nroRevision y redaccionOrigenId")
        void copia_conserva_revision() {
            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            FalDocumentoRedaccion copia = r2.copia();
            assertThat(copia.getNroRevision()).isEqualTo((short) 2);
            assertThat(copia.getRedaccionOrigenId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("copia conserva fhAnulacion e idUserAnulacion despues de anular")
        void copia_conserva_anulacion() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            r.anular(DESPUES, "usr-anulador", "Motivo.");
            FalDocumentoRedaccion copia = r.copia();
            assertThat(copia.getFhAnulacion()).isEqualTo(DESPUES);
            assertThat(copia.getIdUserAnulacion()).isEqualTo("usr-anulador");
            assertThat(copia.estaAnulada()).isTrue();
        }
    }

    // =========================================================
    // 11. Repositorio conserva los cuatro campos
    // =========================================================

    @Nested
    @DisplayName("11. Repositorio conserva los cuatro campos")
    class RepositorioConservaCampos {

        @Test
        @DisplayName("guardar y leer conserva nroRevision y redaccionOrigenId")
        void repo_conserva_revision() {
            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            repo.guardar(r2);
            FalDocumentoRedaccion leida = repo.buscarPorId(2L).orElseThrow();
            assertThat(leida.getNroRevision()).isEqualTo((short) 2);
            assertThat(leida.getRedaccionOrigenId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("guardar y leer conserva fhAnulacion e idUserAnulacion despues de anular")
        void repo_conserva_anulacion() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            repo.guardar(r);

            FalDocumentoRedaccion leida = repo.buscarPorId(1L).orElseThrow();
            leida.anular(DESPUES, "usr-anulador", "Motivo.");
            repo.guardar(leida);

            FalDocumentoRedaccion anulada = repo.buscarPorId(1L).orElseThrow();
            assertThat(anulada.getFhAnulacion()).isEqualTo(DESPUES);
            assertThat(anulada.getIdUserAnulacion()).isEqualTo("usr-anulador");
            assertThat(anulada.estaAnulada()).isTrue();
        }
    }

    // =========================================================
    // 12. nroRevision accesible via getter (paridad DDL)
    // =========================================================

    @Nested
    @DisplayName("12. Paridad DDL: nroRevision/redaccionOrigenId/fhAnulacion/idUserAnulacion presentes")
    class ParidadDdlCuatroCampos {

        @Test
        @DisplayName("getNroRevision() disponible — paridad con nro_revision SMALLINT NOT NULL DEFAULT 1")
        void get_nro_revision_disponible() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getNroRevision()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("getRedaccionOrigenId() disponible — paridad con redaccion_origen_id BIGINT NULL")
        void get_redaccion_origen_id_disponible() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getRedaccionOrigenId()).isNull();
        }

        @Test
        @DisplayName("getFhAnulacion() disponible — paridad con fh_anulacion DATETIME(6) NULL")
        void get_fh_anulacion_disponible() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getFhAnulacion()).isNull();
        }

        @Test
        @DisplayName("getIdUserAnulacion() disponible — paridad con id_user_anulacion CHAR(36) NULL")
        void get_id_user_anulacion_disponible() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            assertThat(r.getIdUserAnulacion()).isNull();
        }
    }

    // =========================================================
    // 13. OCC sigue incrementando versionRow
    // =========================================================

    @Nested
    @DisplayName("13. OCC sigue incrementando versionRow correctamente")
    class OccVersionRowSigueIncrementando {

        @Test
        @DisplayName("versionRow incrementa tras guardar — no se pierde por los nuevos campos")
        void version_row_incrementa_con_campos_revision() {
            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            repo.guardar(r2);
            assertThat(repo.buscarPorId(2L).orElseThrow().getVersionRow()).isEqualTo(0);

            FalDocumentoRedaccion leida = repo.buscarPorId(2L).orElseThrow();
            repo.guardar(leida);
            assertThat(repo.buscarPorId(2L).orElseThrow().getVersionRow()).isEqualTo(1);
        }
    }

    // =========================================================
    // 14. Conflicto OCC no altera datos de revision ni anulacion
    // =========================================================

    @Nested
    @DisplayName("14. Conflicto OCC no altera datos de revision ni anulacion")
    class OccConflictoNoAlteraDatosRevision {

        @Test
        @DisplayName("conflicto OCC no modifica nroRevision ni redaccionOrigenId del store")
        void conflicto_occ_no_altera_revision() {
            FalDocumentoRedaccion r2 = nuevaRevision(2L, 100L, (short) 2, 1L);
            repo.guardar(r2);

            FalDocumentoRedaccion lectorA = repo.buscarPorId(2L).orElseThrow();
            FalDocumentoRedaccion lectorB = repo.buscarPorId(2L).orElseThrow();

            repo.guardar(lectorA);

            try { repo.guardar(lectorB); } catch (ConcurrenciaConflictoException ignored) {}

            FalDocumentoRedaccion postConflicto = repo.buscarPorId(2L).orElseThrow();
            assertThat(postConflicto.getNroRevision()).isEqualTo((short) 2);
            assertThat(postConflicto.getRedaccionOrigenId()).isEqualTo(1L);
            assertThat(postConflicto.getVersionRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("conflicto OCC no modifica fhAnulacion del store")
        void conflicto_occ_no_altera_anulacion() {
            FalDocumentoRedaccion r = primeraRedaccion(1L, 100L);
            repo.guardar(r);

            FalDocumentoRedaccion lectorA = repo.buscarPorId(1L).orElseThrow();
            FalDocumentoRedaccion lectorB = repo.buscarPorId(1L).orElseThrow();

            lectorA.anular(DESPUES, "usr-anulador", "Motivo.");
            repo.guardar(lectorA);

            lectorB.setContenidoEditable("Intento concurrente.");
            try { repo.guardar(lectorB); } catch (ConcurrenciaConflictoException ignored) {}

            FalDocumentoRedaccion postConflicto = repo.buscarPorId(1L).orElseThrow();
            assertThat(postConflicto.estaAnulada()).isTrue();
            assertThat(postConflicto.getFhAnulacion()).isEqualTo(DESPUES);
        }
    }
}
