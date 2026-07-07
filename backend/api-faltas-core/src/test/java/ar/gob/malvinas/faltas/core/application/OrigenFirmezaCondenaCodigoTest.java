package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrigenFirmezaCondena - codigos SMALLINT")
class OrigenFirmezaCondenaCodigoTest {

    @Test
    @DisplayName("exactamente 3 valores")
    void exactamente_tres_valores() {
        assertThat(OrigenFirmezaCondena.values()).hasSize(3);
    }

    @Test
    @DisplayName("codigos son exactamente 1, 2, 3")
    void codigos_exactos() {
        assertThat(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION.codigo()).isEqualTo((short) 1);
        assertThat(OrigenFirmezaCondena.APELACION_RECHAZADA.codigo()).isEqualTo((short) 2);
        assertThat(OrigenFirmezaCondena.CONSENTIMIENTO_EXPRESO.codigo()).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("codigos unicos")
    void codigos_unicos() {
        Set<Short> codigos = new HashSet<>();
        for (OrigenFirmezaCondena o : OrigenFirmezaCondena.values()) {
            codigos.add(o.codigo());
        }
        assertThat(codigos).hasSize(3);
    }

    @Test
    @DisplayName("fromCodigo resuelve cada valor")
    void fromCodigo_valido() {
        assertThat(OrigenFirmezaCondena.fromCodigo((short) 1))
                .isEqualTo(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
        assertThat(OrigenFirmezaCondena.fromCodigo((short) 2))
                .isEqualTo(OrigenFirmezaCondena.APELACION_RECHAZADA);
        assertThat(OrigenFirmezaCondena.fromCodigo((short) 3))
                .isEqualTo(OrigenFirmezaCondena.CONSENTIMIENTO_EXPRESO);
    }

    @Test
    @DisplayName("fromCodigo rechaza codigo desconocido")
    void fromCodigo_invalido() {
        assertThatThrownBy(() -> OrigenFirmezaCondena.fromCodigo((short) 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrigenFirmezaCondena.fromCodigo((short) 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CONSENTIMIENTO_EXPRESO existe con codigo 3")
    void consentimiento_expreso_presente() {
        assertThat(OrigenFirmezaCondena.CONSENTIMIENTO_EXPRESO).isNotNull();
        assertThat(OrigenFirmezaCondena.CONSENTIMIENTO_EXPRESO.codigo()).isEqualTo((short) 3);
    }
}