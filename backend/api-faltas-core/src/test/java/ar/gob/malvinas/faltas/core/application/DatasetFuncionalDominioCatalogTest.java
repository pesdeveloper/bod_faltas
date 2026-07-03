package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del catalogo del dataset funcional del dominio in-memory.
 *
 * Valida estructura y completitud del catalogo de actas mock funcionales.
 *
 * Slice 8F-4B.
 */
@DisplayName("8F-4B: DatasetFuncionalDominioCatalog - estructura y completitud")
class DatasetFuncionalDominioCatalogTest {

    @Test
    @DisplayName("1. El catalogo no esta vacio")
    void catalogo_no_esta_vacio() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        assertThat(defs).isNotEmpty();
    }

    @Test
    @DisplayName("2. El catalogo tiene al menos 20 actas mock funcionales")
    void catalogo_tiene_suficientes_actas() {
        List<ActaMockFuncionalDefinicion> defs = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        assertThat(defs).hasSizeGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("3. Todas las actas tienen codigo estable no nulo ni vacio")
    void todas_las_actas_tienen_codigo_estable() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.codigo())
                .as("codigo de la definicion %s", def.codigo())
                .isNotNull()
                .isNotBlank()
        );
    }

    @Test
    @DisplayName("4. No hay codigos duplicados en el catalogo")
    void no_hay_codigos_duplicados() {
        List<String> codigos = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones()
                .stream().map(ActaMockFuncionalDefinicion::codigo).toList();
        Set<String> unicos = Set.copyOf(codigos);
        assertThat(codigos).hasSameSizeAs(unicos);
    }

    @Test
    @DisplayName("5. Todas las actas tienen titulo no vacio")
    void todas_las_actas_tienen_titulo() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.titulo())
                .as("titulo de %s", def.codigo())
                .isNotNull().isNotBlank()
        );
    }

    @Test
    @DisplayName("6. Todas las actas tienen descripcion no vacia")
    void todas_las_actas_tienen_descripcion() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.descripcion())
                .as("descripcion de %s", def.codigo())
                .isNotNull().isNotBlank()
        );
    }

    @Test
    @DisplayName("7. Todas las actas tienen caso de uso principal no vacio")
    void todas_las_actas_tienen_caso_uso_principal() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.casoUsoPrincipal())
                .as("casoUsoPrincipal de %s", def.codigo())
                .isNotNull().isNotBlank()
        );
    }

    @Test
    @DisplayName("8. Todas las actas declaran al menos un caso de uso cubierto")
    void todas_las_actas_tienen_casos_uso_cubiertos() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.casosUsoCubiertos())
                .as("casosUsoCubiertos de %s", def.codigo())
                .isNotNull().isNotEmpty()
        );
    }

    @Test
    @DisplayName("9. Todas las actas tienen bloque esperado no nulo")
    void todas_las_actas_tienen_bloque_esperado() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.bloqueEsperado())
                .as("bloqueEsperado de %s", def.codigo())
                .isNotNull()
        );
    }

    @Test
    @DisplayName("10. Todas las actas tienen situacion administrativa esperada no nula")
    void todas_las_actas_tienen_situacion_esperada() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.situacionEsperada())
                .as("situacionEsperada de %s", def.codigo())
                .isNotNull()
        );
    }

    @Test
    @DisplayName("11. Todas las actas tienen resultado final esperado no nulo")
    void todas_las_actas_tienen_resultado_final_esperado() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.resultadoFinalEsperado())
                .as("resultadoFinalEsperado de %s", def.codigo())
                .isNotNull()
        );
    }

    @Test
    @DisplayName("12. Todas las actas tienen bandeja esperada no nula")
    void todas_las_actas_tienen_bandeja_esperada() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.bandejaEsperada())
                .as("bandejaEsperada de %s", def.codigo())
                .isNotNull()
        );
    }

    @Test
    @DisplayName("13. Todas las actas tienen endpoints o servicios cubiertos declarados")
    void todas_las_actas_tienen_endpoints_o_servicios() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.endpointsServiciosCubiertos())
                .as("endpointsServiciosCubiertos de %s", def.codigo())
                .isNotNull().isNotEmpty()
        );
    }

    @Test
    @DisplayName("14. Todas las actas tienen observaciones declaradas")
    void todas_las_actas_tienen_observaciones() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.observaciones())
                .as("observaciones de %s", def.codigo())
                .isNotNull().isNotEmpty()
        );
    }

    @Test
    @DisplayName("15. Todas las actas tienen documentos esperados no nulos (lista puede estar vacia si justificado)")
    void todas_las_actas_tienen_documentos_esperados_no_nulos() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
            assertThat(def.documentosEsperados())
                .as("documentosEsperados de %s", def.codigo())
                .isNotNull()
        );
    }

    @Test
    @DisplayName("16. buscarPorCodigo encuentra ACT-001-LABRADA")
    void buscar_por_codigo_encuentra_definicion() {
        ActaMockFuncionalDefinicion def = DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-001-LABRADA");
        assertThat(def).isNotNull();
        assertThat(def.codigo()).isEqualTo("ACT-001-LABRADA");
    }

    @Test
    @DisplayName("17. buscarPorCodigo lanza excepcion para codigo inexistente")
    void buscar_por_codigo_inexistente_lanza_excepcion() {
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> DatasetFuncionalDominioCatalog.buscarPorCodigo("CODIGO-QUE-NO-EXISTE")
        );
    }

    @Test
    @DisplayName("18. El dataset incluye ACT-026-NOTIFICACION-NEGATIVA (8F-4B-R1)")
    void dataset_incluye_acta_026() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-026-NOTIFICACION-NEGATIVA")).isNotNull();
    }

    @Test
    @DisplayName("19. El dataset incluye ACT-027-DOC-ADJUNTO-CONVALIDADO (8F-4B-R1)")
    void dataset_incluye_acta_027() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-027-DOC-ADJUNTO-CONVALIDADO")).isNotNull();
    }

    @Test
    @DisplayName("20. El dataset incluye ACT-028-ABSOLUCION-FIRME-CERRADA (8F-4B-R1)")
    void dataset_incluye_acta_028() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-028-ABSOLUCION-FIRME-CERRADA")).isNotNull();
    }

    @Test
    @DisplayName("21. El dataset incluye ACT-029-REINGRESO-PARA-REVISION (8F-4B-R1)")
    void dataset_incluye_acta_029() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-029-REINGRESO-PARA-REVISION")).isNotNull();
    }

    @Test
    @DisplayName("22. El dataset incluye ACT-030-PAGO-CONDENA-OBSERVADO (8F-4B-R1)")
    void dataset_incluye_acta_030() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-030-PAGO-CONDENA-OBSERVADO")).isNotNull();
    }

    @Test
    @DisplayName("23. El dataset incluye ACT-031-PAGO-CONDENA-CON-DESCUENTO (8F-4B-R1)")
    void dataset_incluye_acta_031() {
        assertThat(DatasetFuncionalDominioCatalog.buscarPorCodigo("ACT-031-PAGO-CONDENA-CON-DESCUENTO")).isNotNull();
    }

    @Test
    @DisplayName("24. casosUsoPendientes no contiene los pendientes ya cubiertos por 8F-4B-R1")
    void casos_uso_pendientes_no_contiene_anteriores_pendientes() {
        List<String> pendientes = DatasetFuncionalDominioCatalog.calcularCobertura().casosUsoPendientes();
        assertThat(pendientes).doesNotContain(
            "Acta con notificacion negativa fallida",
            "Acta con adjunto escaneado convalidado",
            "Acta cerrada por ABSOLUCION_FIRME",
            "Acta cerrada por REINGRESO_PARA_REVISION",
            "Acta con pago de condena observado/con descuento"
        );
    }

    @Test
    @DisplayName("25. El dataset tiene al menos 31 actas mock funcionales (8F-4B-R1 completo)")
    void dataset_tiene_al_menos_31_actas() {
        assertThat(DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones())
            .hasSizeGreaterThanOrEqualTo(31);
    }
}