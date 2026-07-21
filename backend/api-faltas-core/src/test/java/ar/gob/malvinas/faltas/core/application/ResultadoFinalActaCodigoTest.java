package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResultadoFinalActa - codigos SMALLINT y semantica")
class ResultadoFinalActaCodigoTest {

    @Test
    @DisplayName("codigos son positivos o cero y unicos")
    void codigos_unicos() {
        Set<Short> codigos = new HashSet<>();
        for (ResultadoFinalActa r : ResultadoFinalActa.values()) {
            assertThat(r.codigo()).isGreaterThanOrEqualTo((short) 0);
            codigos.add(r.codigo());
        }
        assertThat(codigos).hasSize(ResultadoFinalActa.values().length);
    }

    @Test
    @DisplayName("codigos exactos de los valores canonicos")
    void codigos_exactos() {
        assertThat(ResultadoFinalActa.SIN_RESULTADO_FINAL.codigo()).isEqualTo((short) 0);
        assertThat(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO.codigo()).isEqualTo((short) 1);
        assertThat(ResultadoFinalActa.ABSUELTO.codigo()).isEqualTo((short) 2);
        assertThat(ResultadoFinalActa.CONDENA_FIRME.codigo()).isEqualTo((short) 3);
        assertThat(ResultadoFinalActa.CONDENA_FIRME_PAGADA.codigo()).isEqualTo((short) 4);
        assertThat(ResultadoFinalActa.ANULADO.codigo()).isEqualTo((short) 8);
        assertThat(ResultadoFinalActa.NULIDAD.codigo()).isEqualTo((short) 9);
    }

    @Test
    @DisplayName("fromCodigo resuelve valores canonicos correctamente")
    void fromCodigo_valido() {
        assertThat(ResultadoFinalActa.fromCodigo((short) 0)).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        assertThat(ResultadoFinalActa.fromCodigo((short) 1)).isEqualTo(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO);
        assertThat(ResultadoFinalActa.fromCodigo((short) 2)).isEqualTo(ResultadoFinalActa.ABSUELTO);
        assertThat(ResultadoFinalActa.fromCodigo((short) 3)).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
        assertThat(ResultadoFinalActa.fromCodigo((short) 4)).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
    }

    @Test
    @DisplayName("fromCodigo rechaza codigo desconocido")
    void fromCodigo_invalido() {
        assertThatThrownBy(() -> ResultadoFinalActa.fromCodigo((short) 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResultadoFinalActa.fromCodigo((short) -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResultadoFinalActa.fromCodigo((short) 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PAGO_VOLUNTARIO_PAGADO reemplaza al alias anterior")
    void pago_voluntario_pagado_existe() {
        Set<String> nombres = new HashSet<>();
        Arrays.stream(ResultadoFinalActa.values()).forEach(r -> nombres.add(r.name()));
        assertThat(nombres).contains("PAGO_VOLUNTARIO_PAGADO");
        assertThat(nombres).doesNotContain("PAGO_VOLUNTARIO_CONFIRMADO", "PAGO_CONFIRMADO", "CONDENADO");
    }
}
