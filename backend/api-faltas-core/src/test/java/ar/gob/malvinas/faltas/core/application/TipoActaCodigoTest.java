package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TipoActa - codigos SMALLINT")
class TipoActaCodigoTest {

    @Test
    @DisplayName("cada valor tiene codigo unico y distinto de cero")
    void codigos_unicos() {
        Set<Short> codigos = new HashSet<>();
        for (TipoActa t : TipoActa.values()) {
            assertThat(t.codigo()).isPositive();
            codigos.add(t.codigo());
        }
        assertThat(codigos).hasSize(TipoActa.values().length);
    }

    @Test
    @DisplayName("codigos son exactamente 1 a 4")
    void codigos_exactos() {
        assertThat(TipoActa.TRANSITO.codigo()).isEqualTo((short) 1);
        assertThat(TipoActa.CONTRAVENCION.codigo()).isEqualTo((short) 2);
        assertThat(TipoActa.SUSTANCIAS_ALIMENTICIAS.codigo()).isEqualTo((short) 3);
        assertThat(TipoActa.COMERCIO.codigo()).isEqualTo((short) 4);
    }

    @Test
    @DisplayName("fromCodigo resuelve correctamente cada valor")
    void fromCodigo_valido() {
        assertThat(TipoActa.fromCodigo((short) 1)).isEqualTo(TipoActa.TRANSITO);
        assertThat(TipoActa.fromCodigo((short) 2)).isEqualTo(TipoActa.CONTRAVENCION);
        assertThat(TipoActa.fromCodigo((short) 3)).isEqualTo(TipoActa.SUSTANCIAS_ALIMENTICIAS);
        assertThat(TipoActa.fromCodigo((short) 4)).isEqualTo(TipoActa.COMERCIO);
    }

    @Test
    @DisplayName("fromCodigo rechaza codigo desconocido")
    void fromCodigo_invalido() {
        assertThatThrownBy(() -> TipoActa.fromCodigo((short) 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TipoActa.fromCodigo((short) 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TipoActa.fromCodigo((short) 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("no existe valor sin codigo asignado - todos tienen short > 0")
    void todos_con_codigo_positivo() {
        Arrays.stream(TipoActa.values())
                .forEach(t -> assertThat(t.codigo()).isGreaterThan((short) 0));
    }
}