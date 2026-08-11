package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenIncorporacionEvidencia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de {@link FalActaEvidencia} para la metadata de fotografias
 * (DECISION_DDL-EVID-03, Wave 4 Parte 3D S18).
 */
@DisplayName("FalActaEvidencia — invariantes de fotografia (DECISION_DDL-EVID-03)")
class FalActaEvidenciaFotografiaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 6, 10, 0, 0);
    private static final String HASH = "a".repeat(64);

    private static FalActaEvidencia fotoValida() {
        return new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                "evid/uuid.jpg", AHORA, AHORA, "actor-test", HASH,
                1, "una descripcion", "foto.jpg", "image/jpeg", 2048L, 800, 600,
                AHORA.minusDays(1), "ENRI", OrigenIncorporacionEvidencia.ALTA_INICIAL);
    }

    @Nested
    @DisplayName("1. FOTO exige las siete columnas obligatorias")
    class CamposObligatoriosFoto {

        @Test
        @DisplayName("FOTO valida se construye sin excepcion")
        void fotoValidaOk() {
            assertThat(fotoValida().esFotografia()).isTrue();
        }

        @Test
        @DisplayName("orden nulo rechaza")
        void ordenNuloRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    null, null, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("orden");
        }

        @Test
        @DisplayName("orden menor a 1 rechaza")
        void ordenCeroRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    0, null, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("hash nulo rechaza para FOTO")
        void hashNuloRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", null,
                    1, null, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hashEvid");
        }

        @Test
        @DisplayName("mimeTipo nulo rechaza")
        void mimeNuloRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, null, 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mimeTipo");
        }

        @Test
        @DisplayName("tamanioBytes nulo o no positivo rechaza")
        void tamanioInvalidoRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, "image/jpeg", 0L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("anchoPx/altoPx nulo o no positivo rechaza")
        void dimensionesInvalidasRechazan() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, "image/jpeg", 10L, 0, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, "image/jpeg", 10L, 10, null, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("origenIncorporacion nulo rechaza")
        void origenNuloRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, "image/jpeg", 10L, 10, 10, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("origenIncorporacion");
        }

        @Test
        @DisplayName("descripcion, nombreOriginal, fechaCapturaExif y bloqueAlIncorporar son opcionales incluso para FOTO")
        void opcionalesPuedenSerNulos() {
            assertThat(fotoValida()).isNotNull();
            FalActaEvidencia sinOpcionales = new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, null, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL);
            assertThat(sinOpcionales.getDescripcion()).isNull();
            assertThat(sinOpcionales.getNombreOriginal()).isNull();
            assertThat(sinOpcionales.getFechaCapturaExif()).isNull();
            assertThat(sinOpcionales.getBloqueAlIncorporar()).isNull();
        }
    }

    @Nested
    @DisplayName("2. No-FOTO exige las diez columnas de fotografia en null")
    class NoFotoSinColumnasDeFotografia {

        @Test
        @DisplayName("constructor legacy de 8 argumentos sigue funcionando para no-FOTO (compatibilidad)")
        void constructorLegacySigueFuncionando() {
            FalActaEvidencia firma = new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR,
                    "k", AHORA, AHORA, "a", null);
            assertThat(firma.esFotografia()).isFalse();
            assertThat(firma.getOrden()).isNull();
        }

        @Test
        @DisplayName("constructor completo con orden informado para no-FOTO rechaza")
        void noFotoConOrdenRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.PDF,
                    "k", AHORA, AHORA, "a", null,
                    1, null, null, null, null, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exclusivas de FOTO");
        }

        @Test
        @DisplayName("constructor completo con origenIncorporacion informado para no-FOTO rechaza")
        void noFotoConOrigenRechaza() {
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.VIDEO,
                    "k", AHORA, AHORA, "a", null,
                    null, null, null, null, null, null, null, null, null,
                    OrigenIncorporacionEvidencia.CARGA_POSTERIOR))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("3. Descripcion: normalizacion y limite")
    class Descripcion {

        @Test
        @DisplayName("espacios multiples se normalizan a uno solo y se recorta")
        void normalizaEspacios() {
            FalActaEvidencia f = new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, "  hola   mundo  ", null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL);
            assertThat(f.getDescripcion()).isEqualTo("hola mundo");
        }

        @Test
        @DisplayName("descripcion en blanco se normaliza a null")
        void blancoEsNull() {
            FalActaEvidencia f = new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, "    ", null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL);
            assertThat(f.getDescripcion()).isNull();
        }

        @Test
        @DisplayName("descripcion de mas de 240 caracteres (tras normalizar) rechaza")
        void masDe240Rechaza() {
            String larga = "a".repeat(241);
            assertThatThrownBy(() -> new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, larga, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("240");
        }

        @Test
        @DisplayName("exactamente 240 caracteres se acepta")
        void exactamente240Acepta() {
            String limite = "a".repeat(240);
            FalActaEvidencia f = new FalActaEvidencia(1L, 100L, TipoEvidenciaActa.FOTO,
                    "k", AHORA, AHORA, "a", HASH,
                    1, limite, null, "image/jpeg", 10L, 10, 10, null, null,
                    OrigenIncorporacionEvidencia.ALTA_INICIAL);
            assertThat(f.getDescripcion()).hasSize(240);
        }
    }

    @Nested
    @DisplayName("4. OrigenIncorporacionEvidencia — catalogo")
    class CatalogoOrigen {

        @Test
        @DisplayName("dos valores, multiplos de 4, round-trip por codigo")
        void catalogoYRoundTrip() {
            assertThat(OrigenIncorporacionEvidencia.values()).hasSize(2);
            for (OrigenIncorporacionEvidencia o : OrigenIncorporacionEvidencia.values()) {
                assertThat(o.codigo() % 4).isEqualTo((short) 0);
                assertThat(OrigenIncorporacionEvidencia.desdeCodigo(o.codigo())).isEqualTo(o);
            }
        }

        @Test
        @DisplayName("codigo desconocido lanza excepcion")
        void codigoDesconocidoLanza() {
            assertThatThrownBy(() -> OrigenIncorporacionEvidencia.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
