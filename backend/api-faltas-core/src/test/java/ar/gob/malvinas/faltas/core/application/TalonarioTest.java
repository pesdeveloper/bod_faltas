package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AsignarTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioAmbitoCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroActaCommand;
import ar.gob.malvinas.faltas.core.application.result.NumeroActaEmitidoResponse;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.InspectorService;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumPolitica;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioAmbito;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.InspectorRepository;
import ar.gob.malvinas.faltas.core.repository.TalonarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de los Slices 8B-2, 8B-3 y 8B-4: Politicas, talonarios, ambitos, asignaciones inspector y numeracion in-memory.
 *
 * Verifica:
 * - Catalogos productivos: ClaseNumeracion, TipoTalonario, AlcanceTalonario, EstadoAsignacionTalonario.
 * - Creacion y validaciones de NumPolitica.
 * - Creacion y validaciones de NumTalonario.
 * - Creacion y validaciones de NumTalonarioAmbito.
 * - Asignacion y devolucion de NumTalonarioInspector.
 * - Guardrail: no existe NumTalonarioMovimiento.
 * - Guardrail: no se emite numero, no se toca FalActa.
 */
@DisplayName("Slice 8B-2/8B-3/8B-4: Talonarios, numeracion, asignaciones inspector y emision in-memory")
class TalonarioTest {

    private TalonarioRepository talonarioRepo;
    private DependenciaRepository dependenciaRepo;
    private InspectorRepository inspectorRepo;
    private TalonarioService talonarioService;
    private DependenciaService dependenciaService;
    private InspectorService inspectorService;

    @BeforeEach
    void setUp() {
        talonarioRepo = new InMemoryTalonarioRepository();
        dependenciaRepo = new InMemoryDependenciaRepository();
        inspectorRepo = new InMemoryInspectorRepository();
        talonarioService = new TalonarioService(talonarioRepo, dependenciaRepo, inspectorRepo, FaltasClockTestSupport.FIXED);
        dependenciaService = new DependenciaService(dependenciaRepo, FaltasClockTestSupport.FIXED);
        inspectorService = new InspectorService(inspectorRepo, dependenciaRepo, FaltasClockTestSupport.FIXED);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private NumPolitica crearPoliticaActaSimple() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-ACTA-01", "Politica actas transito", ClaseNumeracion.ACTA,
                false, false, null, false, null, false, null,
                "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
    }

    private NumPolitica crearPoliticaActaConReinicioAnual() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-ACTA-ANUAL", "Politica actas con reinicio anual", ClaseNumeracion.ACTA,
                true, false, null, true, (short) 4, false, null,
                "{ANIO}-{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
    }

    private NumPolitica crearPoliticaDocSimple() {
        return talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                "POL-DOC-01", "Politica documentos", ClaseNumeracion.DOCUMENTO,
                false, false, null, false, null, false, null,
                "DOC-{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
    }

    private NumTalonario crearTalonarioElectronicoActa(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-ELEC-01", "Talonario electronico actas",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                null, null, 1, null, "seq_fal_tal_acta_01",
                true, false, null, null, "sistema"));
    }

    private NumTalonario crearTalonarioManualActa(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-MAN-01", "Talonario manual actas",
                TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                null, null, 1, 100, "seq_fal_tal_man_01",
                true, false, null, null, "sistema"));
    }

    private NumTalonario crearTalonarioManualActaExtra(Long politicaId) {
        return talonarioService.crearTalonario(new CrearTalonarioCommand(
                politicaId, "TAL-MAN-02", "Talonario manual actas extra",
                TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                null, null, 101, 200, "seq_fal_tal_man_02",
                true, false, null, null, "sistema"));
    }

    private FalDependencia crearDependenciaTransito() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                "DEP-T-01", "Transito Central", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalInspector crearInspectorConVersion() {
        FalDependencia dep = crearDependenciaTransito();
        return inspectorService.crear(new CrearInspectorCommand(
                "user-insp-01", 12345, "Juan Perez",
                dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }

    private FalInspector crearInspectorConVersionExtra() {
        FalDependencia dep = dependenciaService.crear(new CrearDependenciaCommand(
                "DEP-T-02", "Transito Norte", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
        return inspectorService.crear(new CrearInspectorCommand(
                "user-insp-02", 67890, "Maria Lopez",
                dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "sistema"));
    }
    private NumTalonarioAmbito crearAmbitoGlobal(Long talonarioId, ClaseNumeracion clase) {
        return talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                talonarioId, clase, null, null,
                null, null, AlcanceTalonario.GLOBAL, (short) 10,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
    }


    // =========================================================================
    // 8B2-01/02/03: Enums productivos
    // =========================================================================

    @Nested
    @DisplayName("8B2-01-03: Catalogos productivos")
    class CatalogosProductivos {

        @Test
        @DisplayName("ClaseNumeracion tiene exactamente ACTA=1 y DOCUMENTO=2")
        void claseNumeracion_valores_productivos() {
            assertThat(ClaseNumeracion.ACTA.codigo()).isEqualTo((short) 1);
            assertThat(ClaseNumeracion.DOCUMENTO.codigo()).isEqualTo((short) 2);
            assertThat(ClaseNumeracion.values()).hasSize(2);
            assertThat(ClaseNumeracion.desdeCodigo((short) 1)).isEqualTo(ClaseNumeracion.ACTA);
            assertThat(ClaseNumeracion.desdeCodigo((short) 2)).isEqualTo(ClaseNumeracion.DOCUMENTO);
        }

        @Test
        @DisplayName("TipoTalonario tiene exactamente ELECTRONICO=1 y MANUAL_FISICO=2")
        void tipoTalonario_valores_productivos() {
            assertThat(TipoTalonario.ELECTRONICO.codigo()).isEqualTo((short) 1);
            assertThat(TipoTalonario.MANUAL_FISICO.codigo()).isEqualTo((short) 2);
            assertThat(TipoTalonario.values()).hasSize(2);
            assertThat(TipoTalonario.desdeCodigo((short) 1)).isEqualTo(TipoTalonario.ELECTRONICO);
            assertThat(TipoTalonario.desdeCodigo((short) 2)).isEqualTo(TipoTalonario.MANUAL_FISICO);
        }

        @Test
        @DisplayName("AlcanceTalonario tiene exactamente GLOBAL=1, DEPENDENCIA=2, TRANSVERSAL_DOCUMENTO=3")
        void alcanceTalonario_valores_productivos() {
            assertThat(AlcanceTalonario.GLOBAL.codigo()).isEqualTo((short) 1);
            assertThat(AlcanceTalonario.DEPENDENCIA.codigo()).isEqualTo((short) 2);
            assertThat(AlcanceTalonario.TRANSVERSAL_DOCUMENTO.codigo()).isEqualTo((short) 3);
            assertThat(AlcanceTalonario.values()).hasSize(3);
            assertThat(AlcanceTalonario.desdeCodigo((short) 1)).isEqualTo(AlcanceTalonario.GLOBAL);
            assertThat(AlcanceTalonario.desdeCodigo((short) 2)).isEqualTo(AlcanceTalonario.DEPENDENCIA);
            assertThat(AlcanceTalonario.desdeCodigo((short) 3)).isEqualTo(AlcanceTalonario.TRANSVERSAL_DOCUMENTO);
        }
    }

    // =========================================================================
    // 8B2-04/16: Politicas
    // =========================================================================

    @Nested
    @DisplayName("8B2-04-16: Politicas de numeracion")
    class PoliticasTest {

        @Test
        @DisplayName("Crear politica valida ACTA")
        void crear_politica_acta_valida() {
            NumPolitica p = crearPoliticaActaSimple();
            assertThat(p.getId()).isNotNull();
            assertThat(p.getCodigo()).isEqualTo("POL-ACTA-01");
            assertThat(p.getClaseNumeracion()).isEqualTo(ClaseNumeracion.ACTA);
            assertThat(p.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Crear politica valida DOCUMENTO")
        void crear_politica_documento_valida() {
            NumPolitica p = crearPoliticaDocSimple();
            assertThat(p.getClaseNumeracion()).isEqualTo(ClaseNumeracion.DOCUMENTO);
            assertThat(p.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Rechazar politica sin codigo")
        void rechazar_politica_sin_codigo() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            null, "desc", ClaseNumeracion.ACTA,
                            false, false, null, false, null, false, null,
                            "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("codigo");
        }

        @Test
        @DisplayName("Rechazar politica con codigo duplicado")
        void rechazar_politica_codigo_duplicado() {
            crearPoliticaActaSimple();
            assertThatThrownBy(() -> crearPoliticaActaSimple())
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("POL-ACTA-01");
        }

        @Test
        @DisplayName("Rechazar politica sin descripcion")
        void rechazar_politica_sin_descripcion() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            "COD-X", null, ClaseNumeracion.ACTA,
                            false, false, null, false, null, false, null,
                            "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("descripcion");
        }

        @Test
        @DisplayName("Rechazar politica sin claseNumeracion")
        void rechazar_politica_sin_claseNumeracion() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            "COD-X", "desc", null,
                            false, false, null, false, null, false, null,
                            "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("claseNumeracion");
        }

        @Test
        @DisplayName("Rechazar politica sin formatoVisible")
        void rechazar_politica_sin_formatoVisible() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            "COD-X", "desc", ClaseNumeracion.ACTA,
                            false, false, null, false, null, false, null,
                            null, true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("formatoVisible");
        }

        @Test
        @DisplayName("Rechazar politica con siIncluyePrefijo=true y prefijo nulo")
        void rechazar_politica_prefijo_nulo_cuando_siIncluyePrefijo() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            "COD-X", "desc", ClaseNumeracion.ACTA,
                            false, true, null, false, null, false, null,
                            "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("prefijo");
        }

        @Test
        @DisplayName("Rechazar politica sin fhVigDesde")
        void rechazar_politica_sin_fhVigDesde() {
            assertThatThrownBy(() -> talonarioService.crearPolitica(
                    new CrearPoliticaNumeracionCommand(
                            "COD-X", "desc", ClaseNumeracion.ACTA,
                            false, false, null, false, null, false, null,
                            "{NRO}", true, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhVigDesde");
        }

        @Test
        @DisplayName("Listar politicas activas")
        void listar_politicas_activas() {
            crearPoliticaActaSimple();
            assertThat(talonarioService.listarPoliticasActivas()).hasSize(1);
        }
    }

    // =========================================================================
    // 8B2-17/31: Talonarios
    // =========================================================================

    @Nested
    @DisplayName("8B2-17-31: Talonarios")
    class TalonariosTest {

        @Test
        @DisplayName("Crear talonario electronico valido")
        void crear_talonario_electronico_valido() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThat(t.getId()).isNotNull();
            assertThat(t.getTipoTalonario()).isEqualTo(TipoTalonario.ELECTRONICO);
            assertThat(t.isSiActivo()).isTrue();
        }

        @Test
        @DisplayName("Crear talonario manual valido")
        void crear_talonario_manual_valido() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            assertThat(t.getTipoTalonario()).isEqualTo(TipoTalonario.MANUAL_FISICO);
            assertThat(t.getNroHasta()).isEqualTo(100);
        }

        @Test
        @DisplayName("Rechazar talonario con codigo duplicado")
        void rechazar_talonario_codigo_duplicado() {
            NumPolitica p = crearPoliticaActaSimple();
            crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> crearTalonarioElectronicoActa(p.getId()))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("TAL-ELEC-01");
        }

        @Test
        @DisplayName("Rechazar talonario sin politica")
        void rechazar_talonario_sin_politica() {
            assertThatThrownBy(() -> talonarioService.crearTalonario(new CrearTalonarioCommand(
                    999L, "COD", "desc", TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_x",
                    true, false, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Rechazar talonario con claseTalonario distinta a la de politica")
        void rechazar_clase_distinta_a_politica() {
            NumPolitica p = crearPoliticaActaSimple();
            assertThatThrownBy(() -> talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "COD-DOC", "desc",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, null, "seq_x2",
                    true, false, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("coincidir");
        }

        @Test
        @DisplayName("Rechazar nroDesde menor o igual a cero")
        void rechazar_nroDesde_cero() {
            NumPolitica p = crearPoliticaActaSimple();
            assertThatThrownBy(() -> talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "COD-X", "desc",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 0, null, "seq_x3",
                    true, false, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroDesde");
        }

        @Test
        @DisplayName("Rechazar nroHasta menor que nroDesde")
        void rechazar_nroHasta_menor_que_nroDesde() {
            NumPolitica p = crearPoliticaActaSimple();
            assertThatThrownBy(() -> talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "COD-X", "desc",
                    TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                    null, null, 50, 10, "seq_x4",
                    true, false, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroHasta");
        }

        @Test
        @DisplayName("Rechazar nombreSecuencia duplicado")
        void rechazar_nombreSecuencia_duplicado() {
            NumPolitica p = crearPoliticaActaSimple();
            crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "COD-OTRO", "desc2",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_fal_tal_acta_01",
                    true, false, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("seq_fal_tal_acta_01");
        }

        @Test
        @DisplayName("Listar talonarios activos")
        void listar_talonarios_activos() {
            NumPolitica p = crearPoliticaActaSimple();
            crearTalonarioElectronicoActa(p.getId());
            crearTalonarioManualActa(p.getId());
            assertThat(talonarioService.listarTalonariosActivos()).hasSize(2);
        }
    }

    // =========================================================================
    // 8B2-32/46: Ambitos
    // =========================================================================

    @Nested
    @DisplayName("8B2-32-46: Ambitos de talonario")
    class AmbitosTest {

        @Test
        @DisplayName("Crear ambito GLOBAL valido")
        void crear_ambito_global_valido() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            NumTalonarioAmbito a = talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            assertThat(a.getId()).isNotNull();
            assertThat(a.getAlcance()).isEqualTo(AlcanceTalonario.GLOBAL);
        }

        @Test
        @DisplayName("Crear ambito DEPENDENCIA valido con dependencia existente")
        void crear_ambito_dependencia_valido() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            FalDependencia dep = crearDependenciaTransito();
            NumTalonarioAmbito a = talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    dep.getIdDep(), (short) 1, AlcanceTalonario.DEPENDENCIA, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            assertThat(a.getAlcance()).isEqualTo(AlcanceTalonario.DEPENDENCIA);
        }

        @Test
        @DisplayName("Rechazar talonario inexistente en ambito")
        void rechazar_talonario_inexistente_en_ambito() {
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    999L, ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 0,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Rechazar claseTalonario distinta en ambito")
        void rechazar_clase_distinta_en_ambito() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.DOCUMENTO, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("coincidir");
        }

        @Test
        @DisplayName("Rechazar idDep sin verDep")
        void rechazar_idDep_sin_verDep() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    1L, null, AlcanceTalonario.DEPENDENCIA, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verDep");
        }

        @Test
        @DisplayName("Rechazar verDep sin idDep")
        void rechazar_verDep_sin_idDep() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, (short) 1, AlcanceTalonario.DEPENDENCIA, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idDep");
        }

        @Test
        @DisplayName("Rechazar dependencia inexistente en ambito DEPENDENCIA")
        void rechazar_dependencia_inexistente() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    999L, (short) 1, AlcanceTalonario.DEPENDENCIA, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("Rechazar alcance GLOBAL con idDep informado")
        void rechazar_global_con_idDep() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    1L, (short) 1, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("GLOBAL");
        }

        @Test
        @DisplayName("Rechazar alcance DEPENDENCIA sin idDep/verDep")
        void rechazar_dependencia_sin_idDep() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.DEPENDENCIA, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("DEPENDENCIA");
        }

        @Test
        @DisplayName("Rechazar alcance TRANSVERSAL_DOCUMENTO sin tipoDocu")
        void rechazar_transversal_sin_tipoDocu() {
            NumPolitica p = crearPoliticaDocSimple();
            NumTalonario t = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "TAL-DOC-X", "doc",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.DOCUMENTO,
                    null, null, 1, null, "seq_doc_x",
                    true, false, null, null, "sistema"));
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.DOCUMENTO, null, null,
                    null, null, AlcanceTalonario.TRANSVERSAL_DOCUMENTO, (short) 3,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("tipoDocu");
        }

        @Test
        @DisplayName("Rechazar prioridad negativa")
        void rechazar_prioridad_negativa() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) -1,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("prioridad");
        }

        @Test
        @DisplayName("Rechazar fhDesde null")
        void rechazar_fhDesde_null() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            assertThatThrownBy(() -> talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 0,
                    null, null, true, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhDesde");
        }

        @Test
        @DisplayName("Listar ambitos por talonario devuelve los del talonario")
        void listar_ambitos_por_talonario() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            List<NumTalonarioAmbito> ambitos = talonarioService.listarAmbitosPorTalonario(t.getId());
            assertThat(ambitos).hasSize(1);
        }

        @Test
        @DisplayName("No se emite numero al crear ambito")
        void no_se_emite_numero() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    t.getId(), ClaseNumeracion.ACTA, null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            assertThat(t.getNroDesde()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8B3-01/03: EstadoAsignacionTalonario
    // =========================================================================

    @Nested
    @DisplayName("8B3-01-03: Catalogo EstadoAsignacionTalonario")
    class EstadoAsignacionTalonarioTest {

        @Test
        @DisplayName("8B3-01: EstadoAsignacionTalonario tiene exactamente ENTREGADO=1, DEVUELTO=2, CERRADO=3, OBSERVADO=4")
        void estadoAsignacion_valores_productivos() {
            assertThat(EstadoAsignacionTalonario.ENTREGADO.codigo()).isEqualTo((short) 1);
            assertThat(EstadoAsignacionTalonario.DEVUELTO.codigo()).isEqualTo((short) 2);
            assertThat(EstadoAsignacionTalonario.CERRADO.codigo()).isEqualTo((short) 3);
            assertThat(EstadoAsignacionTalonario.OBSERVADO.codigo()).isEqualTo((short) 4);
            assertThat(EstadoAsignacionTalonario.values()).hasSize(4);
        }

        @Test
        @DisplayName("8B3-02: desdeCodigo devuelve valor correcto para cada codigo")
        void estadoAsignacion_desdeCodigo_correcto() {
            assertThat(EstadoAsignacionTalonario.desdeCodigo((short) 1))
                    .isEqualTo(EstadoAsignacionTalonario.ENTREGADO);
            assertThat(EstadoAsignacionTalonario.desdeCodigo((short) 2))
                    .isEqualTo(EstadoAsignacionTalonario.DEVUELTO);
            assertThat(EstadoAsignacionTalonario.desdeCodigo((short) 3))
                    .isEqualTo(EstadoAsignacionTalonario.CERRADO);
            assertThat(EstadoAsignacionTalonario.desdeCodigo((short) 4))
                    .isEqualTo(EstadoAsignacionTalonario.OBSERVADO);
        }

        @Test
        @DisplayName("8B3-03: desdeCodigo rechaza codigo invalido")
        void estadoAsignacion_desdeCodigo_invalido() {
            assertThatThrownBy(() -> EstadoAsignacionTalonario.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // 8B3-04/26: Asignacion inspector
    // =========================================================================

    @Nested
    @DisplayName("8B3-04-26: Asignacion de talonario manual fisico a inspector")
    class AsignacionInspectorTest {

        @Test
        @DisplayName("8B3-04: Asignar talonario MANUAL_FISICO activo a inspector existente")
        void asignar_talonario_manual_a_inspector() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThat(a.getId()).isNotNull();
            assertThat(a.getIdTalonario()).isEqualTo(t.getId());
            assertThat(a.getIdInsp()).isEqualTo(insp.getIdInsp());
        }

        @Test
        @DisplayName("8B3-05: La asignacion queda en estado ENTREGADO")
        void asignacion_estado_entregado() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThat(a.getEstadoAsignacion()).isEqualTo(EstadoAsignacionTalonario.ENTREGADO);
        }

        @Test
        @DisplayName("8B3-06: La asignacion queda siActiva = true")
        void asignacion_siActiva_true() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThat(a.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("8B3-07: talonarioIdActivo = idTalonario cuando siActiva = true")
        void asignacion_talonarioIdActivo_igual_idTalonario() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThat(a.getTalonarioIdActivo()).isEqualTo(t.getId());
        }

        @Test
        @DisplayName("8B3-08: fhEntrega se conserva en la asignacion")
        void asignacion_fhEntrega_conservada() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            LocalDateTime fhEntrega = LocalDateTime.of(2026, 6, 30, 9, 0, 0);
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            fhEntrega, "sistema"));
            assertThat(a.getFhEntrega()).isEqualTo(fhEntrega);
        }

        @Test
        @DisplayName("8B3-09: idUserEntrega se conserva en la asignacion")
        void asignacion_idUserEntrega_conservado() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "user-entrega-01"));
            assertThat(a.getIdUserEntrega()).isEqualTo("user-entrega-01");
        }

        @Test
        @DisplayName("8B3-10: Rechazar asignacion con talonario inexistente")
        void rechazar_talonario_inexistente() {
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            999L, insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("8B3-11: Rechazar asignacion con talonario inactivo")
        void rechazar_talonario_inactivo() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "TAL-MAN-INACT", "Inactivo",
                    TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                    null, null, 1, 100, "seq_man_inact",
                    false, false, null, null, "sistema"));
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("activo");
        }

        @Test
        @DisplayName("8B3-12: Rechazar asignacion con talonario bloqueado")
        void rechazar_talonario_bloqueado() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    p.getId(), "TAL-MAN-BLOQ", "Bloqueado",
                    TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                    null, null, 1, 100, "seq_man_bloq",
                    true, true, "ABC123", null, "sistema"));
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("bloqueado");
        }

        @Test
        @DisplayName("8B3-13: Rechazar asignacion con talonario ELECTRONICO")
        void rechazar_talonario_electronico() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioElectronicoActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("MANUAL_FISICO");
        }

        @Test
        @DisplayName("8B3-14: Permitir solo MANUAL_FISICO → verificar que ELECTRONICO falla")
        void solo_manual_fisico_permitido() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario elec = crearTalonarioElectronicoActa(p.getId());
            NumTalonario manual = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            elec.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            manual.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThat(a.getId()).isNotNull();
        }

        @Test
        @DisplayName("8B3-15: Rechazar inspector inexistente")
        void rechazar_inspector_inexistente() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), 9999L, (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("9999");
        }

        @Test
        @DisplayName("8B3-16: Rechazar version de inspector inexistente")
        void rechazar_version_inspector_inexistente() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 99,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("8B3-17: Rechazar idInsp nulo")
        void rechazar_idInsp_nulo() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), null, (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idInsp");
        }

        @Test
        @DisplayName("8B3-18: Rechazar idTalonario nulo")
        void rechazar_idTalonario_nulo() {
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            null, insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idTalonario");
        }

        @Test
        @DisplayName("8B3-19: Rechazar fhEntrega null")
        void rechazar_fhEntrega_null() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhEntrega");
        }

        @Test
        @DisplayName("8B3-20: Rechazar idUserEntrega nulo o vacio")
        void rechazar_idUserEntrega_nulo() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserEntrega");
        }

        @Test
        @DisplayName("8B3-21: Rechazar doble asignacion activa del mismo talonario")
        void rechazar_doble_asignacion_activa_mismo_talonario() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp1 = crearInspectorConVersion();
            FalInspector insp2 = crearInspectorConVersionExtra();
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp1.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp2.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("8B3-22: Rechazar asignar talonario ya asignado activamente a otro inspector")
        void rechazar_asignar_talonario_ya_asignado_a_otro() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp1 = crearInspectorConVersion();
            FalInspector insp2 = crearInspectorConVersionExtra();
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp1.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThatThrownBy(() -> talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp2.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B3-23: Listar asignaciones activas")
        void listar_asignaciones_activas() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t1 = crearTalonarioManualActa(p.getId());
            NumTalonario t2 = crearTalonarioManualActaExtra(p.getId());
            FalInspector insp1 = crearInspectorConVersion();
            FalInspector insp2 = crearInspectorConVersionExtra();
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t1.getId(), insp1.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t2.getId(), insp2.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            List<NumTalonarioInspector> activas = talonarioService.listarAsignacionesActivas();
            assertThat(activas).hasSize(2);
            assertThat(activas).allMatch(NumTalonarioInspector::isSiActiva);
        }

        @Test
        @DisplayName("8B3-24: Listar asignaciones por inspector")
        void listar_asignaciones_por_inspector() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t1 = crearTalonarioManualActa(p.getId());
            NumTalonario t2 = crearTalonarioManualActaExtra(p.getId());
            FalInspector insp1 = crearInspectorConVersion();
            FalInspector insp2 = crearInspectorConVersionExtra();
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t1.getId(), insp1.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t2.getId(), insp2.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            List<NumTalonarioInspector> porInsp1 = talonarioService.listarAsignacionesPorInspector(insp1.getIdInsp());
            assertThat(porInsp1).hasSize(1);
            assertThat(porInsp1.get(0).getIdInsp()).isEqualTo(insp1.getIdInsp());
        }

        @Test
        @DisplayName("8B3-25: Listar asignaciones por talonario")
        void listar_asignaciones_por_talonario() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            List<NumTalonarioInspector> porTalonario = talonarioService.listarAsignacionesPorTalonario(t.getId());
            assertThat(porTalonario).hasSize(1);
            assertThat(porTalonario.get(0).getIdTalonario()).isEqualTo(t.getId());
        }
    }

    // =========================================================================
    // 8B3-27/36: Devolucion
    // =========================================================================

    @Nested
    @DisplayName("8B3-27-36: Devolucion de talonario manual fisico")
    class DevolucionTest {

        @Test
        @DisplayName("8B3-27: Devolver asignacion activa")
        void devolver_asignacion_activa() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "user-dev-01"));
            assertThat(dev.getId()).isEqualTo(a.getId());
        }

        @Test
        @DisplayName("8B3-28: Al devolver, estadoAsignacion = DEVUELTO")
        void devolucion_estado_devuelto() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "user-dev-01"));
            assertThat(dev.getEstadoAsignacion()).isEqualTo(EstadoAsignacionTalonario.DEVUELTO);
        }

        @Test
        @DisplayName("8B3-29: Al devolver, siActiva = false")
        void devolucion_siActiva_false() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "user-dev-01"));
            assertThat(dev.isSiActiva()).isFalse();
        }

        @Test
        @DisplayName("8B3-30: Al devolver, talonarioIdActivo = null")
        void devolucion_talonarioIdActivo_null() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "user-dev-01"));
            assertThat(dev.getTalonarioIdActivo()).isNull();
        }

        @Test
        @DisplayName("8B3-31: Al devolver, fhDevolucion queda registrada")
        void devolucion_fhDevolucion_registrada() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            LocalDateTime fhDev = LocalDateTime.of(2026, 6, 30, 17, 0, 0);
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(a.getId(), fhDev, "user-dev-01"));
            assertThat(dev.getFhDevolucion()).isEqualTo(fhDev);
        }

        @Test
        @DisplayName("8B3-32: Al devolver, idUserDevolucion queda registrado")
        void devolucion_idUserDevolucion_registrado() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            NumTalonarioInspector dev = talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "user-devolucion-xyz"));
            assertThat(dev.getIdUserDevolucion()).isEqualTo("user-devolucion-xyz");
        }

        @Test
        @DisplayName("8B3-33: Rechazar devolucion de asignacion inexistente")
        void rechazar_devolucion_asignacion_inexistente() {
            assertThatThrownBy(() -> talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            9999L, FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("9999");
        }

        @Test
        @DisplayName("8B3-34: Rechazar devolucion de asignacion ya inactiva/devuelta")
        void rechazar_devolucion_ya_inactiva() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThatThrownBy(() -> talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(
                            a.getId(), FaltasClockTestSupport.FIXED.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("8B3-35: Rechazar devolucion sin fhDevolucion")
        void rechazar_devolucion_sin_fhDevolucion() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThatThrownBy(() -> talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(a.getId(), null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhDevolucion");
        }

        @Test
        @DisplayName("8B3-36: Rechazar devolucion sin idUserDevolucion")
        void rechazar_devolucion_sin_idUserDevolucion() {
            NumPolitica p = crearPoliticaActaSimple();
            NumTalonario t = crearTalonarioManualActa(p.getId());
            FalInspector insp = crearInspectorConVersion();
            NumTalonarioInspector a = talonarioService.asignarTalonarioInspector(
                    new AsignarTalonarioInspectorCommand(
                            t.getId(), insp.getIdInsp(), (short) 1,
                            FaltasClockTestSupport.FIXED.now(), "sistema"));
            assertThatThrownBy(() -> talonarioService.devolverTalonarioInspector(
                    new DevolverTalonarioInspectorCommand(a.getId(), FaltasClockTestSupport.FIXED.now(), null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUserDevolucion");
        }
    }

    // =========================================================================
    // 8B2-47/52 + 8B3-37/44: Guardrail fuera de scope
    // =========================================================================

    @Nested
    @DisplayName("8B2-47-52 / 8B3-37-44: Guardrail: fuera de scope")
    class GuardrailFueraDeScope {

        @Test
        @DisplayName("No existe clase NumSerieTalonario: serie es campo de NumTalonario")
        void no_existe_NumSerieTalonario() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.model.NumSerieTalonario"))
                    .isInstanceOf(ClassNotFoundException.class);
        }

        @Test
        @DisplayName("TalonarioService no tiene metodo rendir ni rendicion")
        void talonarioService_no_tiene_rendir() {
            boolean tieneRendir = java.util.Arrays.stream(TalonarioService.class.getMethods())
                    .anyMatch(m -> m.getName().toLowerCase().contains("rendir")
                            || m.getName().toLowerCase().contains("rendicion"));
            assertThat(tieneRendir)
                    .as("TalonarioService no debe tener metodo de rendicion en 8B-3").isFalse();
        }

        @Test
        @DisplayName("TalonarioService no tiene metodo anular numero")
        void talonarioService_no_tiene_anularNumero() {
            boolean tieneAnular = java.util.Arrays.stream(TalonarioService.class.getMethods())
                    .anyMatch(m -> m.getName().toLowerCase().contains("anulatnumero")
                            || m.getName().toLowerCase().contains("anularnumero"));
            assertThat(tieneAnular)
                    .as("TalonarioService tiene anularNumeroTalonario desde 8B-6").isTrue();
        }
    }

    // =========================================================================
    // 8B4-01/02/03: Enums productivos 8B-4
    // =========================================================================

    @Nested
    @DisplayName("8B4-01-03: EstadoNumeroTalonario y MotivoAnulacionTalonario")
    class EnumsNumeracionTalonario {

        @Test
        @DisplayName("8B4-01: EstadoNumeroTalonario tiene USADO=1, ANULADO=2, DEVUELTO_SIN_USAR=3, RENDIDO=4, JUSTIFICADO=5")
        void estadoNumeroTalonario_valores_productivos() {
            assertThat(EstadoNumeroTalonario.USADO.codigo()).isEqualTo((short) 1);
            assertThat(EstadoNumeroTalonario.ANULADO.codigo()).isEqualTo((short) 2);
            assertThat(EstadoNumeroTalonario.DEVUELTO_SIN_USAR.codigo()).isEqualTo((short) 3);
            assertThat(EstadoNumeroTalonario.RENDIDO.codigo()).isEqualTo((short) 4);
            assertThat(EstadoNumeroTalonario.JUSTIFICADO.codigo()).isEqualTo((short) 5);
            assertThat(EstadoNumeroTalonario.values()).hasSize(5);
            assertThat(EstadoNumeroTalonario.desdeCodigo((short) 1)).isEqualTo(EstadoNumeroTalonario.USADO);
            assertThat(EstadoNumeroTalonario.desdeCodigo((short) 2)).isEqualTo(EstadoNumeroTalonario.ANULADO);
            assertThat(EstadoNumeroTalonario.desdeCodigo((short) 3)).isEqualTo(EstadoNumeroTalonario.DEVUELTO_SIN_USAR);
            assertThat(EstadoNumeroTalonario.desdeCodigo((short) 4)).isEqualTo(EstadoNumeroTalonario.RENDIDO);
            assertThat(EstadoNumeroTalonario.desdeCodigo((short) 5)).isEqualTo(EstadoNumeroTalonario.JUSTIFICADO);
        }

        @Test
        @DisplayName("8B4-02: MotivoAnulacionTalonario tiene ERROR_LABRADO=1, ROTURA_FORMULARIO=2, DUPLICADO=3, EXTRAVIO=4, OTRO=5")
        void motivoAnulacionTalonario_valores_productivos() {
            assertThat(MotivoAnulacionTalonario.ERROR_LABRADO.codigo()).isEqualTo((short) 1);
            assertThat(MotivoAnulacionTalonario.ROTURA_FORMULARIO.codigo()).isEqualTo((short) 2);
            assertThat(MotivoAnulacionTalonario.DUPLICADO.codigo()).isEqualTo((short) 3);
            assertThat(MotivoAnulacionTalonario.EXTRAVIO.codigo()).isEqualTo((short) 4);
            assertThat(MotivoAnulacionTalonario.OTRO.codigo()).isEqualTo((short) 5);
            assertThat(MotivoAnulacionTalonario.values()).hasSize(5);
            assertThat(MotivoAnulacionTalonario.desdeCodigo((short) 1)).isEqualTo(MotivoAnulacionTalonario.ERROR_LABRADO);
            assertThat(MotivoAnulacionTalonario.desdeCodigo((short) 2)).isEqualTo(MotivoAnulacionTalonario.ROTURA_FORMULARIO);
            assertThat(MotivoAnulacionTalonario.desdeCodigo((short) 3)).isEqualTo(MotivoAnulacionTalonario.DUPLICADO);
            assertThat(MotivoAnulacionTalonario.desdeCodigo((short) 4)).isEqualTo(MotivoAnulacionTalonario.EXTRAVIO);
            assertThat(MotivoAnulacionTalonario.desdeCodigo((short) 5)).isEqualTo(MotivoAnulacionTalonario.OTRO);
        }

        @Test
        @DisplayName("8B4-03: desdeCodigo rechaza codigos invalidos")
        void desdeCodigo_rechaza_codigo_invalido() {
            assertThatThrownBy(() -> EstadoNumeroTalonario.desdeCodigo((short) 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
            assertThatThrownBy(() -> MotivoAnulacionTalonario.desdeCodigo((short) 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0");
        }
    }

    // =========================================================================
    // 8B4-04/11: NumTalonarioMovimiento creacion y validaciones
    // =========================================================================

    @Nested
    @DisplayName("8B4-04-11: NumTalonarioMovimiento creacion y validaciones")
    class MovimientoNumeracion {

        @Test
        @DisplayName("8B4-04: Crear movimiento USADO valido")
        void crear_movimiento_usado_valido() {
            NumTalonarioMovimiento m = new NumTalonarioMovimiento(
                    1L, 10L, 5, EstadoNumeroTalonario.USADO,
                    null, null, null, null,
                    100L, (short) 1, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01");
            assertThat(m.getId()).isEqualTo(1L);
            assertThat(m.getIdTalonario()).isEqualTo(10L);
            assertThat(m.getNroTalonario()).isEqualTo(5);
            assertThat(m.getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.USADO);
            assertThat(m.getMotivoAnulacion()).isNull();
            assertThat(m.getActaId()).isEqualTo(null);
            assertThat(m.getDocumentoId()).isNull();
            assertThat(m.getIdDep()).isEqualTo(100L);
            assertThat(m.getVerDep()).isEqualTo((short) 1);
            assertThat(m.getIdInsp()).isNull();
            assertThat(m.getVerInsp()).isNull();
            assertThat(m.getIdUserMovimiento()).isEqualTo("user-01");
        }

        @Test
        @DisplayName("8B4-05: Listar movimientos por talonario")
        void listar_movimientos_por_talonario() {
            NumPolitica pol = crearPoliticaActaSimple();
            NumTalonario tal = crearTalonarioElectronicoActa(pol.getId());
            crearAmbitoGlobal(tal.getId(), pol.getClaseNumeracion());

            EmitirNumeroActaCommand cmd = new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null,
                    null, FaltasClockTestSupport.FIXED.now(), "user-01");
            talonarioService.emitirNumeroActa(cmd);
            talonarioService.emitirNumeroActa(cmd);

            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(tal.getId());
            assertThat(movs).hasSize(2);
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.USADO);
            assertThat(movs.get(1).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.USADO);
        }

        @Test
        @DisplayName("8B4-06: Rechazar duplicado idTalonario + nroTalonario en repository")
        void rechazar_duplicado_talonario_numero() {
            NumTalonarioMovimiento m1 = new NumTalonarioMovimiento(
                    1L, 10L, 5, EstadoNumeroTalonario.USADO,
                    null, null, null, null, 100L, (short) 1, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01");
            talonarioRepo.guardarMovimiento(m1);

            assertThat(talonarioRepo.existeMovimientoTalonarioNumero(10L, 5)).isTrue();
            assertThat(talonarioRepo.existeMovimientoTalonarioNumero(10L, 6)).isFalse();
        }
    }

    // =========================================================================
    // 8B4-12/29: Emision de numero de acta
    // =========================================================================

    @Nested
    @DisplayName("8B4-12-29: Emision de numero de acta in-memory")
    class EmisionNumeroActa {

        private NumPolitica politicaSimple;
        private NumTalonario talonarioActa;

        @BeforeEach
        void setUpEmision() {
            politicaSimple = crearPoliticaActaSimple();
            talonarioActa = crearTalonarioElectronicoActa(politicaSimple.getId());
            crearAmbitoGlobal(talonarioActa.getId(), politicaSimple.getClaseNumeracion());
        }

        @Test
        @DisplayName("8B4-12: Primer numero emitido usa nroDesde (=1)")
        void primer_numero_usa_nroDesde() {
            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r.nroTalonarioUsado()).isEqualTo(1);
        }

        @Test
        @DisplayName("8B4-13: Segundo numero incrementa correlativo")
        void segundo_numero_incrementa() {
            talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "user-01"));
            NumeroActaEmitidoResponse r2 = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r2.nroTalonarioUsado()).isEqualTo(2);
        }

        @Test
        @DisplayName("8B4-14: Si nroHasta se supera, rechazar")
        void rechazar_si_nroHasta_superado() {
            NumPolitica pol = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-H", "Politica con limite", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario talLimitado = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-LIM", "Talonario limitado",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, 2, "seq_limitado",
                    true, false, null, null, "sistema"));
            // priority 5 (beats setUp talonario at priority 10) to avoid ambiguity
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talLimitado.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1"));
            talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1"));
            assertThatThrownBy(() -> talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroHasta");
        }

        @Test
        @DisplayName("8B4-15: Rechazar si no hay ambito compatible")
        void rechazar_si_sin_ambito_compatible() {
            // Fresh service - no setUp talonario to interfere
            InMemoryTalonarioRepository freshRepo = new InMemoryTalonarioRepository();
            TalonarioService freshSvc = new TalonarioService(freshRepo, new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
            NumPolitica pol = freshSvc.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-SA", "Politica sin ambito", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            freshSvc.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-SIN-AMB", "Sin ambito",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_sin_amb",
                    true, false, null, null, "sistema"));

            assertThatThrownBy(() -> freshSvc.emitirNumeroActa(new EmitirNumeroActaCommand(
                    888L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("No hay talonario ACTA compatible");
        }

        @Test
        @DisplayName("8B4-16: Rechazar si talonario esta inactivo")
        void rechazar_si_talonario_inactivo() {
            InMemoryTalonarioRepository freshRepo = new InMemoryTalonarioRepository();
            TalonarioService freshSvc = new TalonarioService(freshRepo, new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
            NumPolitica pol = freshSvc.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-IN", "Politica inactivo", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario talInactivo = freshSvc.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-INAC", "Talonario inactivo",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_inac",
                    false, false, null, null, "sistema"));
            freshSvc.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talInactivo.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            assertThatThrownBy(() -> freshSvc.emitirNumeroActa(new EmitirNumeroActaCommand(
                    777L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B4-17: Rechazar si talonario esta bloqueado")
        void rechazar_si_talonario_bloqueado() {
            InMemoryTalonarioRepository freshRepo = new InMemoryTalonarioRepository();
            TalonarioService freshSvc = new TalonarioService(freshRepo, new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
            NumPolitica pol = freshSvc.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-BL", "Politica bloqueado", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario talBloqueado = freshSvc.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-BLQ", "Talonario bloqueado",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_blq",
                    true, true, "CLAVE123", null, "sistema"));
            freshSvc.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talBloqueado.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            assertThatThrownBy(() -> freshSvc.emitirNumeroActa(new EmitirNumeroActaCommand(
                    666L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B4-18: Rechazar si ambito esta inactivo")
        void rechazar_si_ambito_inactivo() {
            InMemoryTalonarioRepository freshRepo = new InMemoryTalonarioRepository();
            TalonarioService freshSvc = new TalonarioService(freshRepo, new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
            NumPolitica pol = freshSvc.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-AI", "Politica ambito inactivo", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario tal = freshSvc.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-AMB-INAC", "Talonario ambito inactivo",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_amb_inac",
                    true, false, null, null, "sistema"));
            // Ambito inactivo
            freshSvc.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, false, "sistema"));

            assertThatThrownBy(() -> freshSvc.emitirNumeroActa(new EmitirNumeroActaCommand(
                    555L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B4-19: Rechazar si ambito no esta vigente")
        void rechazar_si_ambito_no_vigente() {
            InMemoryTalonarioRepository freshRepo = new InMemoryTalonarioRepository();
            TalonarioService freshSvc = new TalonarioService(freshRepo, new InMemoryDependenciaRepository(), new InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED);
            NumPolitica pol = freshSvc.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-ACTA-AV", "Politica ambito vencido", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario tal = freshSvc.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-AMB-VEN", "Talonario ambito vencido",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_amb_ven",
                    true, false, null, null, "sistema"));
            // Ambito ya vencido (fhHasta en el pasado)
            freshSvc.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(10), FaltasClockTestSupport.FIXED.now().toLocalDate().minusDays(1), true, "sistema"));

            assertThatThrownBy(() -> freshSvc.emitirNumeroActa(new EmitirNumeroActaCommand(
                    444L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B4-20/21: Menor prioridad numerica gana sobre mayor prioridad numerica")
        void menor_prioridad_numerica_gana() {
            // Setup: 2 talonarios con ambitos, uno de prioridad 5 (gana) y otro de prioridad 10
            NumPolitica pol = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-PRI", "Politica prioridades", ClaseNumeracion.ACTA,
                    false, true, "PRI", false, null, false, null,
                    "PRI-{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario tal5 = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-PRI5", "Talonario prioridad 5",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_pri5",
                    true, false, null, null, "sistema"));
            NumTalonario tal10 = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-PRI10", "Talonario prioridad 10",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_pri10",
                    true, false, null, null, "sistema"));

            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal5.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal10.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 10,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    100L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1"));
            assertThat(r.idTalonario()).isEqualTo(tal5.getId());
        }

        @Test
        @DisplayName("8B4-22: Empate real de prioridad entre talonarios distintos falla por ambiguedad")
        void empate_prioridad_falla_ambiguedad() {
            NumPolitica pol = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-EMP", "Politica empate", ClaseNumeracion.ACTA,
                    false, false, null, false, null, false, null,
                    "{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario talA = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-EMP-A", "Empate A",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_empa",
                    true, false, null, null, "sistema"));
            NumTalonario talB = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-EMP-B", "Empate B",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_empb",
                    true, false, null, null, "sistema"));

            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talA.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    talB.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            assertThatThrownBy(() -> talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    200L, (short) 1, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Ambiguedad");
        }

        @Test
        @DisplayName("8B4-23: Registrar movimiento USADO al emitir")
        void registrar_movimiento_usado_al_emitir() {
            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r.movimientoId()).isNotNull();
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(talonarioActa.getId());
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.USADO);
            assertThat(movs.get(0).getActaId()).isEqualTo(null);
        }

        @Test
        @DisplayName("8B4-24/25: Movimiento conserva idDep/verDep e idInsp/verInsp")
        void movimiento_conserva_dep_e_insp() {
            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 2, null, 50L, (short) 1,
                    null, FaltasClockTestSupport.FIXED.now(), "user-02"));
            List<NumTalonarioMovimiento> movs = talonarioService.listarMovimientosPorTalonario(talonarioActa.getId());
            NumTalonarioMovimiento m = movs.get(0);
            assertThat(m.getIdDep()).isEqualTo(999L);
            assertThat(m.getVerDep()).isEqualTo((short) 2);
            assertThat(m.getIdInsp()).isEqualTo(50L);
            assertThat(m.getVerInsp()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("8B4-26: documentoId es null en emision de numero de acta")
        void documentoId_es_null_en_emision_acta() {
            talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01"));
            NumTalonarioMovimiento m = talonarioService.listarMovimientosPorTalonario(talonarioActa.getId()).get(0);
            assertThat(m.getDocumentoId()).isNull();
        }

        @Test
        @DisplayName("8B4-27/28/29: nroActa es String, nroTalonarioUsado es int, idTalonario es correcto")
        void emision_retorna_tipos_correctos() {
            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r.nroActa()).isInstanceOf(String.class).isNotBlank();
            assertThat(r.nroTalonarioUsado()).isInstanceOf(Integer.class);
            assertThat(r.idTalonario()).isEqualTo(talonarioActa.getId());
        }

        @Test
        @DisplayName("8B4: nroActa se genera como String segun politica")
        void nroActa_generado_como_string() {
            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r.nroActa()).isEqualTo("1");
        }

        @Test
        @DisplayName("8B4: nroActa con prefijo y longitudNro")
        void nroActa_con_prefijo_y_longitudNro() {
            NumPolitica pol = talonarioService.crearPolitica(new CrearPoliticaNumeracionCommand(
                    "POL-PREF", "Con prefijo y longitud", ClaseNumeracion.ACTA,
                    false, true, "ACT", false, null, false, (short) 6,
                    "ACT-{NRO}", true, FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema"));
            NumTalonario tal = talonarioService.crearTalonario(new CrearTalonarioCommand(
                    pol.getId(), "TAL-PREF", "Prefijo y longitud",
                    TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                    null, null, 1, null, "seq_pref",
                    true, false, null, null, "sistema"));
            // priority 5 (beats setUp talonario at priority 10) to avoid ambiguity
            talonarioService.crearAmbito(new CrearTalonarioAmbitoCommand(
                    tal.getId(), pol.getClaseNumeracion(), null, null,
                    null, null, AlcanceTalonario.GLOBAL, (short) 5,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, true, "sistema"));

            NumeroActaEmitidoResponse r = talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    101L, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "user-01"));
            assertThat(r.nroActa()).isEqualTo("ACT-000001");
        }

        @Test
        @DisplayName("8B4: idDep obligatorio")
        void rechazar_sin_idDep() {
            assertThatThrownBy(() -> talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    null, (short) 1, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idDep");
        }

        @Test
        @DisplayName("8B4: verDep obligatorio")
        void rechazar_sin_verDep() {
            assertThatThrownBy(() -> talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    100L, null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verDep");
        }

        @Test
        @DisplayName("8B4: idInsp sin verInsp rechazado")
        void rechazar_idInsp_sin_verInsp() {
            assertThatThrownBy(() -> talonarioService.emitirNumeroActa(new EmitirNumeroActaCommand(
                    999L, (short) 1, null, 10L, null, null,
                    FaltasClockTestSupport.FIXED.now(), "u1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verInsp");
        }
    }

    // =========================================================================
    // 8B4-30/35: FalActa campos productivos
    // =========================================================================

    @Nested
    @DisplayName("8B4-30-35: FalActa alineada con campos productivos")
    class FalActaCamposProductivos {

        @Test
        @DisplayName("8B4-30: FalActa tiene campo nroActa (no numeroActa)")
        void falActa_tiene_nroActa() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalActa");
            boolean tieneNroActa = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("nroActa"));
            boolean tieneNumeroActa = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("numeroActa"));
            assertThat(tieneNroActa).as("FalActa debe tener campo nroActa").isTrue();
            assertThat(tieneNumeroActa).as("FalActa no debe tener numeroActa (campo viejo)").isFalse();
        }

        @Test
        @DisplayName("8B4-31: FalActa tiene campo idTalonario nullable")
        void falActa_tiene_idTalonario() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalActa");
            boolean tiene = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("idTalonario"));
            assertThat(tiene).as("FalActa debe tener idTalonario").isTrue();
        }

        @Test
        @DisplayName("8B4-32: FalActa tiene campo nroTalonarioUsado nullable")
        void falActa_tiene_nroTalonarioUsado() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalActa");
            boolean tiene = java.util.Arrays.stream(clazz.getDeclaredFields())
                    .anyMatch(f -> f.getName().equals("nroTalonarioUsado"));
            assertThat(tiene).as("FalActa debe tener nroTalonarioUsado").isTrue();
        }

        @Test
        @DisplayName("8B4-33: FalActa.nroActa arranca null y es setteable")
        void falActa_nroActa_nullable_y_setteable() {
            FalActa acta = new FalActa(1L, "uuid1", "TRANSITO", "dep1", "insp1",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), null, null, null,
                    null, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
            assertThat(acta.getNroActa()).isNull();
            acta.setNroActa("ACT-2025-000001");
            assertThat(acta.getNroActa()).isEqualTo("ACT-2025-000001");
        }

        @Test
        @DisplayName("8B4-34: FalActa.idTalonario y nroTalonarioUsado son seteables")
        void falActa_idTalonario_y_nroTalonarioUsado_seteables() {
            FalActa acta = new FalActa(2L, "uuid2", "TRANSITO", "dep1", "insp1",
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), null, null, null,
                    null, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
            assertThat(acta.getIdTalonario()).isNull();
            assertThat(acta.getNroTalonarioUsado()).isNull();
            acta.setIdTalonario(7L);
            acta.setNroTalonarioUsado(42);
            assertThat(acta.getIdTalonario()).isEqualTo(7L);
            assertThat(acta.getNroTalonarioUsado()).isEqualTo(42);
        }
    }

    // =========================================================================
    // 8B4: Guardrail actualizado - fuera de scope de 8B-4
    // =========================================================================

    @Nested
    @DisplayName("8B4: Guardrail 8B-4 - fuera de scope")
    class GuardrailFueraDeScope8B4 {

        @Test
        @DisplayName("No se implementa rendicion en 8B-4")
        void no_hay_metodo_rendir() {
            boolean tieneRendir = java.util.Arrays.stream(TalonarioService.class.getMethods())
                    .anyMatch(m -> m.getName().toLowerCase().contains("rendir")
                            || m.getName().toLowerCase().contains("rendicion"));
            assertThat(tieneRendir)
                    .as("TalonarioService no debe tener metodo de rendicion en 8B-4").isFalse();
        }

        @Test
        @DisplayName("No se implementa anulacion de numero en 8B-4")
        void no_hay_metodo_anular_numero() {
            boolean tieneAnular = java.util.Arrays.stream(TalonarioService.class.getMethods())
                    .anyMatch(m -> m.getName().toLowerCase().contains("anulatnumero")
                            || m.getName().toLowerCase().contains("anularnumero"));
            assertThat(tieneAnular)
                    .as("TalonarioService tiene anularNumeroTalonario desde 8B-6").isTrue();
        }

        @Test
        @DisplayName("No existe clase NumSerieTalonario: serie es campo de NumTalonario")
        void no_existe_NumSerieTalonario() {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.model.NumSerieTalonario"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

}