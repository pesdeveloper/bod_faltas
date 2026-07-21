package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.result.DatasetFuncionalCoberturaResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la matriz de cobertura funcional del dataset in-memory.
 *
 * Valida que el calculo de cobertura sea correcto y deterministico.
 *
 * Slice 8F-4B.
 */
@DisplayName("8F-4B: DatasetFuncionalCobertura - matriz de cobertura")
class DatasetFuncionalCoberturaTest {

    @Test
    @DisplayName("1. Calcula total de actas mock correctamente")
    void calcula_total_actas_mock() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.totalActasMock()).isGreaterThan(0);
        assertThat(cobertura.totalActasMock()).isEqualTo(cobertura.actas().size());
    }

    @Test
    @DisplayName("2. Calcula total de casos de uso cubiertos correctamente")
    void calcula_total_casos_uso_cubiertos() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.totalCasosUsoCubiertos()).isGreaterThan(0);
        assertThat(cobertura.totalCasosUsoCubiertos()).isEqualTo(cobertura.casosUsoCubiertos().size());
    }

    @Test
    @DisplayName("3. Calcula total de documentos esperados correctamente")
    void calcula_total_documentos_esperados() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.totalDocumentosEsperados()).isGreaterThan(0);
    }

    @Test
    @DisplayName("4. Lista de actas no esta vacia")
    void lista_actas_no_vacia() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.actas()).isNotEmpty();
    }

    @Test
    @DisplayName("5. Lista de casos de uso cubiertos no esta vacia")
    void lista_casos_uso_no_vacia() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.casosUsoCubiertos()).isNotEmpty();
    }

    @Test
    @DisplayName("6. Lista de casos pendientes no es nula")
    void lista_casos_pendientes_no_nula() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.casosUsoPendientes()).isNotNull();
    }

    @Test
    @DisplayName("7. La cobertura es incompleta cuando hay pendientes documentados")
    void cobertura_incompleta_cuando_hay_pendientes() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        if (!cobertura.casosUsoPendientes().isEmpty()) {
            assertThat(cobertura.coberturaCompletaSegunDominioActual()).isFalse();
        }
    }

    @Test
    @DisplayName("8. Calculo de cobertura es deterministico (dos llamadas producen mismo resultado)")
    void calculo_es_deterministico() {
        DatasetFuncionalCoberturaResultado c1 = DatasetFuncionalDominioCatalog.calcularCobertura();
        DatasetFuncionalCoberturaResultado c2 = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(c1.totalActasMock()).isEqualTo(c2.totalActasMock());
        assertThat(c1.totalCasosUsoCubiertos()).isEqualTo(c2.totalCasosUsoCubiertos());
        assertThat(c1.totalDocumentosEsperados()).isEqualTo(c2.totalDocumentosEsperados());
    }

    @Test
    @DisplayName("9. Los casos de uso cubiertos estan sin duplicados")
    void casos_uso_cubiertos_sin_duplicados() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        java.util.List<String> cubiertos = cobertura.casosUsoCubiertos();
        java.util.Set<String> sinDuplicados = java.util.Set.copyOf(cubiertos);
        assertThat(cubiertos).hasSameSizeAs(sinDuplicados);
    }

    @Test
    @DisplayName("10. Las advertencias no son nulas")
    void advertencias_no_nulas() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        assertThat(cobertura.advertencias()).isNotNull();
    }

    @Test
    @DisplayName("11. El total de documentos esperados coincide con la suma manual")
    void total_documentos_coincide_con_suma_manual() {
        DatasetFuncionalCoberturaResultado cobertura = DatasetFuncionalDominioCatalog.calcularCobertura();
        int sumaManual = cobertura.actas().stream()
                .mapToInt(a -> a.documentosEsperados().size())
                .sum();
        assertThat(cobertura.totalDocumentosEsperados()).isEqualTo(sumaManual);
    }
}
