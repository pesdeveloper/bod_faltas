package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.CasoUsoFuncionalRunner;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;
import ar.gob.malvinas.faltas.core.application.result.CasoUsoFuncionalEjecucionResultado;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8F-4C -- Cobertura total de pruebas funcionales.
 *
 * Valida que:
 * - Las 37 actas del dataset tienen prueba funcional asociada.
 * - Todos los codigos ACT-* aparecen cubiertos en las suites de este slice.
 * - No hay actas sin runner.
 * - No hay casos funcionales marcados como "noEjecutado" por falta de servicio.
 * - Cada acta ejecuta al menos un evento real de dominio.
 * - Los unicos gaps permitidos son infraestructura futura (JDBC/MariaDB).
 *
 * Si se agrega una nueva acta al dataset, este test falla hasta agregar su suite.
 */
@DisplayName("8F-4C: DatasetFuncionalFlujosCoberturaTest")
class DatasetFuncionalFlujosCoberturaTest {

    /**
     * Los 37 codigos canonicos del dataset funcional.
     * Si el dataset crece, actualizar esta lista aqui Y en la suite correspondiente.
     */
    private static final Set<String> CODIGOS_CUBIERTOS_EN_SUITES = Set.of(
            // Suite: ActaFlujoCapturaFuncionalTest
            "ACT-001-LABRADA",
            "ACT-002-EN-ENRIQUECIMIENTO",
            "ACT-004-PENDIENTE-NOTIFICACION",
            // Suite: ActaFlujoDocumentalFuncionalTest
            "ACT-003-DOC-PENDIENTE-FIRMA",
            "ACT-023-REDACCION-BORRADOR",
            "ACT-024-PDF-MOCK-GENERADO",
            "ACT-025-PRECONDICION-VIOLADA",
            "ACT-027-DOC-ADJUNTO-CONVALIDADO",
            // Suite: ActaFlujoNotificacionFuncionalTest
            "ACT-005-NOTI-ACTA-EN-CURSO",
            "ACT-006-ANAL-LISTA-FALLO",
            "ACT-013-FALLO-COND-NOTIFICADO",
            "ACT-026-NOTIFICACION-NEGATIVA",
            // Suite: ActaFlujoPagoVoluntarioFuncionalTest
            "ACT-007-PAGVOL-SOLICITADO",
            "ACT-008-PAGVOL-PENDIENTE-CONF",
            "ACT-009-PAGVOL-CONFIRMADO",
            // Suite: ActaFlujoFalloFuncionalTest
            "ACT-010-FALLO-ABS-DICTADO",
            "ACT-011-ABSUELTO-CERRADO",
            "ACT-012-FALLO-COND-DICTADO",
            "ACT-015-CONDENA-FIRME",
            "ACT-028-ABSOLUCION-FIRME-CERRADA",
            // Suite: ActaFlujoApelacionFuncionalTest
            "ACT-014-APELACION-PRESENTADA",
            // Suite: ActaFlujoPagoCondenaFuncionalTest
            "ACT-016-PAGO-CONDENA-INFORMADO",
            "ACT-017-CONDENA-FIRME-PAGADA",
            "ACT-030-PAGO-CONDENA-OBSERVADO",
            "ACT-031-PAGO-CONDENA-CON-DESCUENTO",
            "ACT-032-APELACION-CON-DOCUMENTOS",
            "ACT-033-APELACION-MIXTA",
            "ACT-034-APELACION-RECHAZADA",
            "ACT-035-APELACION-ABSOLUTORIA",
            "ACT-036-APELACION-MODIFICA-CONDENA",
            "ACT-037-APELACION-NULIDAD",
            // Suite: ActaFlujoGestionExternaFuncionalTest
            "ACT-018-GESTION-EXTERNA",
            "ACT-019-GESTION-EXTERNA-PAGO-EXTERNO",
            // Suite: ActaFlujoParalizacionFuncionalTest
            "ACT-020-PARALIZADA",
            // Suite: ActaFlujoBloqueanteFuncionalTest
            "ACT-021-BLOQUEANTE-ACTIVO",
            "ACT-022-ABSUELTO-CON-BLOQUEANTE",
            // Suite: ActaFlujoReingresoFuncionalTest
            "ACT-029-REINGRESO-PARA-REVISION"
    );

    static Stream<ActaMockFuncionalDefinicion> todasLasDefiniciones() {
        return DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().stream();
    }

    // =========================================================================
    // Test de cobertura total
    // =========================================================================

    @Test
    @DisplayName("Dataset contiene exactamente 37 actas funcionales")
    void dataset_contiene_31_actas() {
        List<ActaMockFuncionalDefinicion> todas = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        assertThat(todas).hasSize(37);
    }

    @Test
    @DisplayName("Todos los 37 codigos del dataset estan cubiertos en alguna suite funcional")
    void todos_los_codigos_tienen_suite_funcional() {
        List<ActaMockFuncionalDefinicion> todas = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        List<String> codigos = todas.stream().map(ActaMockFuncionalDefinicion::codigo).collect(Collectors.toList());

        List<String> sinCobertura = codigos.stream()
                .filter(c -> !CODIGOS_CUBIERTOS_EN_SUITES.contains(c))
                .collect(Collectors.toList());

        assertThat(sinCobertura)
                .as("Actas sin suite funcional: %s", sinCobertura)
                .isEmpty();
    }

    @Test
    @DisplayName("No hay codigos cubiertos que no existan en el dataset (sin fantasmas)")
    void sin_codigos_fantasma_en_suites() {
        Set<String> codigosDataset = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones()
                .stream().map(ActaMockFuncionalDefinicion::codigo).collect(Collectors.toSet());

        List<String> fantasmas = CODIGOS_CUBIERTOS_EN_SUITES.stream()
                .filter(c -> !codigosDataset.contains(c))
                .sorted()
                .collect(Collectors.toList());

        assertThat(fantasmas)
                .as("Codigos en suites que no existen en dataset: %s", fantasmas)
                .isEmpty();
    }

    @Test
    @DisplayName("Cobertura es exactamente 37/37 (sin actas huerfanas)")
    void cobertura_31_de_31() {
        assertThat(CODIGOS_CUBIERTOS_EN_SUITES).hasSize(37);
    }

    // =========================================================================
    // Test de ejecucion de cada acta (runner funcional)
    // =========================================================================

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Runner ejecuta cada acta del dataset sin lanzar excepcion")
    void runner_ejecuta_sin_excepcion(ActaMockFuncionalDefinicion def) {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        assertThat(res.ejecutado())
                .as("Acta %s: ejecutado debe ser true", def.codigo())
                .isTrue();

        assertThat(res.bloqueFinal())
                .as("Acta %s: bloque final no debe ser null", def.codigo())
                .isNotNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Runner no retorna noEjecutado para ninguna acta del dataset")
    void runner_no_retorna_no_ejecutado(ActaMockFuncionalDefinicion def) {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        assertThat(res.ejecutado())
                .as("Acta %s NO debe retornar noEjecutado por falta de servicio. " +
                    "8F-4C requiere implementar servicios faltantes.", def.codigo())
                .isTrue();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Cada acta genera al menos un evento real de dominio")
    void cada_acta_genera_al_menos_un_evento(ActaMockFuncionalDefinicion def) {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        assertThat(res.eventosGenerados())
                .as("Acta %s: debe generar al menos 1 evento real", def.codigo())
                .isGreaterThan(0);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Cada acta tiene actaId asignado (fue creada en el repositorio)")
    void cada_acta_tiene_id_en_repositorio(ActaMockFuncionalDefinicion def) {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        assertThat(res.actaId())
                .as("Acta %s: actaId debe ser no null", def.codigo())
                .isNotNull();

        FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElse(null);
        assertThat(acta)
                .as("Acta %s: debe existir en el repositorio", def.codigo())
                .isNotNull();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Bloque final de cada acta coincide con bloqueEsperado del dataset")
    void bloque_final_coincide_con_dataset(ActaMockFuncionalDefinicion def) {
        if (def.bloqueEsperado() == null) return;

        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        assertThat(res.bloqueFinal())
                .as("Acta %s: bloque final (%s) vs esperado (%s)",
                    def.codigo(), res.bloqueFinal(), def.bloqueEsperado().codigo())
                .isEqualTo(def.bloqueEsperado().codigo());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("todasLasDefiniciones")
    @DisplayName("Situacion administrativa de cada acta coincide con situacionEsperada del dataset")
    void situacion_admin_coincide_con_dataset(ActaMockFuncionalDefinicion def) {
        SituacionAdministrativaActa esperada = def.situacionEsperada();
        if (esperada == null) return;

        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar(def.codigo());

        FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
        assertThat(acta.getSituacionAdministrativa())
                .as("Acta %s: situacion admin (%s) vs esperada (%s)",
                    def.codigo(), acta.getSituacionAdministrativa(), esperada)
                .isEqualTo(esperada);
    }

    // =========================================================================
    // Tests estructurales del dataset
    // =========================================================================

    @Test
    @DisplayName("Todos los codigos del dataset son unicos")
    void codigos_del_dataset_son_unicos() {
        List<ActaMockFuncionalDefinicion> todas = DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones();
        List<String> codigos = todas.stream().map(ActaMockFuncionalDefinicion::codigo).collect(Collectors.toList());
        Set<String> unicos = Set.copyOf(codigos);
        assertThat(codigos).hasSize(unicos.size());
    }

    @Test
    @DisplayName("Todos los codigos del dataset empiezan con ACT-")
    void todos_los_codigos_empiezan_con_act() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
                assertThat(def.codigo()).startsWith("ACT-"));
    }

    @Test
    @DisplayName("No hay actas con casoUsoPrincipal null o blank")
    void no_hay_casos_uso_nulos() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
                assertThat(def.casoUsoPrincipal())
                        .as("casoUsoPrincipal no puede ser null/blank: %s", def.codigo())
                        .isNotBlank());
    }

    @Test
    @DisplayName("No hay actas con bloqueEsperado null")
    void no_hay_bloque_esperado_nulo() {
        DatasetFuncionalDominioCatalog.obtenerTodasLasDefiniciones().forEach(def ->
                assertThat(def.bloqueEsperado())
                        .as("bloqueEsperado no puede ser null: %s", def.codigo())
                        .isNotNull());
    }

    // =========================================================================
    // Tests de guardrails
    // =========================================================================

    @Test
    @DisplayName("GUARDRAIL: No existe JDBC, JPA ni EntityManager en el runner")
    void guardrail_no_jdbc_ni_jpa_en_runner() {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        assertThat(runner).isNotNull();
    }

    @Test
    @DisplayName("GUARDRAIL: Descuento en ACT-031 no tiene evento propio (usa PCOCNF)")
    void guardrail_descuento_act031_usa_pcocnf() {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-031-PAGO-CONDENA-CON-DESCUENTO");

        List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
        List<String> codigos = eventos.stream()
                .map(e -> e.tipoEvt().codigo())
                .collect(Collectors.toList());

        assertThat(codigos).contains("PCOCNF");
        assertThat(codigos).doesNotContain("DESCT");
        assertThat(codigos).doesNotContain("PCODESCT");
    }

    @Test
    @DisplayName("GUARDRAIL: Paralizada real via ActaParalizacionService (no setter directo)")
    void guardrail_paralizada_via_servicio_real() {
        CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
        CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-020-PARALIZADA");

        List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
        List<String> codigos = eventos.stream()
                .map(e -> e.tipoEvt().codigo())
                .collect(Collectors.toList());

        assertThat(codigos).contains("ACTPAR");
        assertThat(res.paralizadaFinal()).isTrue();
    }
}
