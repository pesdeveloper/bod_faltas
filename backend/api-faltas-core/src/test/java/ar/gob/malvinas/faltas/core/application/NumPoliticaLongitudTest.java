package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de frontera para NumPolitica.prefijo (VARCHAR(10)) y formatoVisible (VARCHAR(60)).
 *
 * Longitudes canonicas cerradas en HUMAN_DECISION_CLOSED CORRECCION-07:
 *   - prefijo VARCHAR(10): max 10 chars (antes era VARCHAR(8) en el DDL — incorrecto)
 *   - formato_visible VARCHAR(60): max 60 chars (antes era VARCHAR(64) en el DDL — incorrecto)
 */
@DisplayName("NumPolitica: longitudes canonicas (F6 CORRECCION-07)")
class NumPoliticaLongitudTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 20, 12, 0);
    private static final LocalDate HOY = LocalDate.of(2026, 7, 20);

    private NumPolitica politica(String prefijo, String formatoVisible) {
        return new NumPolitica(
                1L, "POL001", "Descripcion de politica.",
                ClaseNumeracion.ACTA, false,
                prefijo != null, prefijo,
                false, null,
                false, (short) 5,
                formatoVisible,
                true, HOY, null,
                AHORA, "usr-test");
    }

    // =========================================================
    // prefijo VARCHAR(10)
    // =========================================================

    @Nested
    @DisplayName("prefijo VARCHAR(10)")
    class Prefijo {

        @Test
        @DisplayName("prefijo de exactamente 10 caracteres es aceptado")
        void prefijo_10_aceptado() {
            String pref10 = "ABCDEFGHIJ";
            assertThat(pref10).hasSize(10);
            NumPolitica p = politica(pref10, "{NRO}");
            assertThat(p.getPrefijo()).isEqualTo(pref10);
        }

        @Test
        @DisplayName("prefijo de 11 caracteres es rechazado")
        void prefijo_11_rechazado() {
            String pref11 = "ABCDEFGHIJK";
            assertThat(pref11).hasSize(11);
            assertThatThrownBy(() -> politica(pref11, "{NRO}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("prefijo")
                    .hasMessageContaining("10");
        }

        @Test
        @DisplayName("prefijo null es aceptado (nullable en DDL)")
        void prefijo_null_aceptado() {
            NumPolitica p = politica(null, "{NRO}");
            assertThat(p.getPrefijo()).isNull();
        }

        @Test
        @DisplayName("constante MAX_PREFIJO_LENGTH es 10")
        void constante_max_prefijo_es_10() {
            assertThat(NumPolitica.MAX_PREFIJO_LENGTH).isEqualTo(10);
        }
    }

    // =========================================================
    // formato_visible VARCHAR(60)
    // =========================================================

    @Nested
    @DisplayName("formato_visible VARCHAR(60)")
    class FormatoVisible {

        @Test
        @DisplayName("formatoVisible de exactamente 60 caracteres es aceptado")
        void formato_visible_60_aceptado() {
            String fv60 = "{PREF}-{AAAA}-{SERIE}-{NNNNN}123456789012345678901234567890";
            // Ensure exactly 60 chars
            String exacto60 = "A".repeat(60);
            assertThat(exacto60).hasSize(60);
            NumPolitica p = politica(null, exacto60);
            assertThat(p.getFormatoVisible()).isEqualTo(exacto60);
        }

        @Test
        @DisplayName("formatoVisible de 61 caracteres es rechazado")
        void formato_visible_61_rechazado() {
            String fv61 = "A".repeat(61);
            assertThat(fv61).hasSize(61);
            assertThatThrownBy(() -> politica(null, fv61))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("formatoVisible")
                    .hasMessageContaining("60");
        }

        @Test
        @DisplayName("constante MAX_FORMATO_VISIBLE_LENGTH es 60")
        void constante_max_formato_visible_es_60() {
            assertThat(NumPolitica.MAX_FORMATO_VISIBLE_LENGTH).isEqualTo(60);
        }
    }
}
