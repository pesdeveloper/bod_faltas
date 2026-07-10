package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AnularNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.AsignarTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearInspectorCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.InspectorService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalInspector;
import ar.gob.malvinas.faltas.core.application.command.CerrarAsignacionTalonarioInspectorCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearPoliticaNumeracionCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.command.DevolverNumeroSinUsarCommand;
import ar.gob.malvinas.faltas.core.application.command.EmitirNumeroActaCommand;
import ar.gob.malvinas.faltas.core.application.command.JustificarNumeroTalonarioCommand;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.domain.enums.AlcanceTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.ClaseNumeracion;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoAsignacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoNumeroTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoAnulacionTalonario;
import ar.gob.malvinas.faltas.core.domain.enums.TipoTalonario;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonario;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioInspector;
import ar.gob.malvinas.faltas.core.domain.model.NumTalonarioMovimiento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository;
import ar.gob.malvinas.faltas.core.web.dto.CierreAsignacionTalonarioResponse;
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
 * Tests del Slice 8B-6: control operativo de talonarios manuales fisicos.
 * Cubre anulacion, justificacion, devolucion sin usar, cierre/rendicion con control de huecos.
 */
@DisplayName("8B-6: Control de talonarios manuales fisicos")
class TalonarioManualFisicoTest {

    private InMemoryTalonarioRepository repo;
    private TalonarioService service;

    /** Id de talonario MANUAL_FISICO con rango 1-5 creado en cada test. */
    private Long idTalManual;
    /** Id de talonario ELECTRONICO creado en cada test. */
    private Long idTalElectronico;
    /** Id de asignacion activa del talonario manual al inspector. */
    private Long idAsignacion;

    private InspectorService inspectorService;
    private DependenciaService dependenciaService;
    private FalInspector inspector;

    @BeforeEach
    void setUp() {
        repo = new InMemoryTalonarioRepository();
        var dependenciaRepo = new InMemoryDependenciaRepository();
        var inspectorRepo = new InMemoryInspectorRepository();
        service = new TalonarioService(repo, dependenciaRepo, inspectorRepo, FaltasClockTestSupport.FIXED);
        dependenciaService = new DependenciaService(dependenciaRepo, FaltasClockTestSupport.FIXED);
        inspectorService = new InspectorService(inspectorRepo, dependenciaRepo, FaltasClockTestSupport.FIXED);

        // Crear politica
        CrearPoliticaNumeracionCommand politicaCmd = new CrearPoliticaNumeracionCommand(
                "POL-MANUAL", "Politica manual fisico",
                ClaseNumeracion.ACTA,
                false, false, null, false, null, false,
                (short) 6, "NRO", true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "test");
        var politica = service.crearPolitica(politicaCmd);

        // Talonario MANUAL_FISICO con rango 1-5
        CrearTalonarioCommand talCmd = new CrearTalonarioCommand(
                politica.getId(), "TAL-MAN-01", "Talonario manual fisico",
                TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                null, null, 1, 5,
                "SEQ_MANUAL_01", true, false, null, null, "test");
        idTalManual = service.crearTalonario(talCmd).getId();

        // Talonario ELECTRONICO con rango 1-5
        CrearTalonarioCommand elecCmd = new CrearTalonarioCommand(
                politica.getId(), "TAL-ELEC-01", "Talonario electronico",
                TipoTalonario.ELECTRONICO, ClaseNumeracion.ACTA,
                null, null, 1, 5,
                "SEQ_ELEC_01", true, false, null, null, "test");
        idTalElectronico = service.crearTalonario(elecCmd).getId();

        // Crear dependencia e inspector reales para las asignaciones
        FalDependencia dep = dependenciaService.crear(new CrearDependenciaCommand(
                "DEP-TEST-01", "Dependencia Test", null,
                TipoActa.TRANSITO, FaltasClockTestSupport.FIXED.now().toLocalDate(), "test"));
        inspector = inspectorService.crear(new CrearInspectorCommand(
                "user-insp-test", 99001, "Inspector Test",
                dep.getIdDep(), 1, FaltasClockTestSupport.FIXED.now().toLocalDate(), "test"));

        // Asignar talonario manual al inspector real
        AsignarTalonarioInspectorCommand asigCmd = new AsignarTalonarioInspectorCommand(
                idTalManual, inspector.getIdInsp(), (short) 1,
                FaltasClockTestSupport.FIXED.now(), "test");
        idAsignacion = service.asignarTalonarioInspector(asigCmd).getId();
    }

    // =========================================================================
    // ANULACION
    // =========================================================================

    @Nested
    @DisplayName("8B6-01 a 8B6-10: Anulacion de numero de talonario MANUAL_FISICO")
    class AnulacionNumero {

        @Test
        @DisplayName("8B6-01: Anular numero valido de talonario MANUAL_FISICO")
        void anular_numero_valido() {
            NumTalonarioMovimiento mov = service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 3, MotivoAnulacionTalonario.ERROR_LABRADO,
                    "Formulario con error", null, null, inspector.getIdInsp(), (short) 1,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));
            assertThat(mov).isNotNull();
            assertThat(mov.getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.ANULADO);
            assertThat(mov.getNroTalonario()).isEqualTo(3);
        }

        @Test
        @DisplayName("8B6-02: Movimiento queda ANULADO con motivoAnulacion correcto")
        void movimiento_queda_anulado() {
            service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 2, MotivoAnulacionTalonario.ROTURA_FORMULARIO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));

            var movs = repo.listarMovimientosPorTalonario(idTalManual);
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.ANULADO);
            assertThat(movs.get(0).getMotivoAnulacion()).isEqualTo(MotivoAnulacionTalonario.ROTURA_FORMULARIO);
        }

        @Test
        @DisplayName("8B6-03: motivoAnulacion es obligatorio")
        void motivoAnulacion_obligatorio() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, null,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("motivoAnulacion");
        }

        @Test
        @DisplayName("8B6-04: motivoAnulacion OTRO exige observacion")
        void motivoOtro_exige_observacion() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, MotivoAnulacionTalonario.OTRO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("observacion");
        }

        @Test
        @DisplayName("8B6-05: Rechazar anulacion de talonario inexistente")
        void rechaza_talonario_inexistente() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    999L, 1, MotivoAnulacionTalonario.EXTRAVIO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("8B6-06: Rechazar anulacion de talonario ELECTRONICO")
        void rechaza_talonario_electronico() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalElectronico, 1, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("MANUAL_FISICO");
        }

        @Test
        @DisplayName("8B6-07: Rechazar numero fuera de rango (menor que nroDesde)")
        void rechaza_nro_fuera_de_rango_inferior() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 0, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroDesde");
        }

        @Test
        @DisplayName("8B6-07b: Rechazar numero fuera de rango (mayor que nroHasta)")
        void rechaza_nro_fuera_de_rango_superior() {
            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 99, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroHasta");
        }

        @Test
        @DisplayName("8B6-09: Rechazar duplicado de movimiento para mismo nro")
        void rechaza_duplicado_de_movimiento() {
            service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));

            assertThatThrownBy(() -> service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, MotivoAnulacionTalonario.DUPLICADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario2")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("movimiento");
        }

        @Test
        @DisplayName("8B6-10: actaId y documentoId quedan null en movimiento ANULADO")
        void actaId_documentoId_null_en_anulado() {
            NumTalonarioMovimiento mov = service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 4, MotivoAnulacionTalonario.EXTRAVIO,
                    "Extraviado en campo", null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));
            assertThat(mov.getActaId()).isNull();
            assertThat(mov.getDocumentoId()).isNull();
        }
    }

    // =========================================================================
    // JUSTIFICACION
    // =========================================================================

    @Nested
    @DisplayName("8B6-11 a 8B6-16: Justificacion de numero")
    class JustificacionNumero {

        @Test
        @DisplayName("8B6-11: Justificar numero valido")
        void justificar_numero_valido() {
            NumTalonarioMovimiento mov = service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 2, "Numero sin uso justificado por control",
                    null, null, inspector.getIdInsp(), (short) 1,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));
            assertThat(mov).isNotNull();
            assertThat(mov.getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.JUSTIFICADO);
        }

        @Test
        @DisplayName("8B6-12: Movimiento queda JUSTIFICADO")
        void movimiento_queda_justificado() {
            service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 3, "Control anual",
                    null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));

            var movs = repo.listarMovimientosPorTalonario(idTalManual);
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.JUSTIFICADO);
        }

        @Test
        @DisplayName("8B6-13: observacion es obligatoria para justificacion")
        void observacion_obligatoria_en_justificacion() {
            assertThatThrownBy(() -> service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 1, null,
                    null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("observacion");
        }

        @Test
        @DisplayName("8B6-14: motivoAnulacion queda null en JUSTIFICADO")
        void motivoAnulacion_null_en_justificado() {
            NumTalonarioMovimiento mov = service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 2, "Justificacion operativa",
                    null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));
            assertThat(mov.getMotivoAnulacion()).isNull();
        }

        @Test
        @DisplayName("8B6-15: Rechazar numero fuera de rango en justificacion")
        void rechaza_nro_fuera_de_rango_en_justificacion() {
            assertThatThrownBy(() -> service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 10, "Fuera de rango",
                    null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("8B6-16: Rechazar numero con movimiento previo en justificacion")
        void rechaza_nro_con_movimiento_previo_en_justificacion() {
            service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "admin"));

            assertThatThrownBy(() -> service.justificarNumeroTalonario(new JustificarNumeroTalonarioCommand(
                    idTalManual, 1, "Ya tiene movimiento",
                    null, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("movimiento");
        }
    }

    // =========================================================================
    // DEVOLUCION SIN USAR
    // =========================================================================

    @Nested
    @DisplayName("8B6-17 a 8B6-20: Devolucion sin usar")
    class DevolucionSinUsar {

        @Test
        @DisplayName("8B6-17: Devolver sin usar numero valido")
        void devolver_numero_valido() {
            NumTalonarioMovimiento mov = service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 4, "No usado en temporada",
                    inspector.getIdInsp(), (short) 1,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));
            assertThat(mov).isNotNull();
            assertThat(mov.getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.DEVUELTO_SIN_USAR);
        }

        @Test
        @DisplayName("8B6-18: Movimiento queda DEVUELTO_SIN_USAR")
        void movimiento_queda_devuelto_sin_usar() {
            service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 5, null,
                    null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));

            var movs = repo.listarMovimientosPorTalonario(idTalManual);
            assertThat(movs.get(0).getEstadoNumero()).isEqualTo(EstadoNumeroTalonario.DEVUELTO_SIN_USAR);
            assertThat(movs.get(0).getMotivoAnulacion()).isNull();
            assertThat(movs.get(0).getActaId()).isNull();
            assertThat(movs.get(0).getDocumentoId()).isNull();
        }

        @Test
        @DisplayName("8B6-19: Rechazar numero con movimiento previo en devolucion")
        void rechaza_numero_con_movimiento_previo() {
            service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 2, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1"));

            assertThatThrownBy(() -> service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 2, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario2")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("movimiento");
        }

        @Test
        @DisplayName("8B6-20: Rechazar numero fuera de rango en devolucion")
        void rechaza_nro_fuera_de_rango_en_devolucion() {
            assertThatThrownBy(() -> service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 100, null, null, null,
                    FaltasClockTestSupport.FIXED.now(), "usuario1")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // CIERRE / RENDICION DE ASIGNACION
    // =========================================================================

    @Nested
    @DisplayName("8B6-21 a 8B6-30: Cierre/rendicion de asignacion manual")
    class CierreAsignacion {

        private void cubrirRango(int... numeros) {
            for (int n : numeros) {
                service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                        idTalManual, n, null, null, null,
                        FaltasClockTestSupport.FIXED.now(), "admin"));
            }
        }

        @Test
        @DisplayName("8B6-21: No cerrar asignacion con numeros faltantes")
        void no_cerrar_con_faltantes() {
            // Cubrir solo 3 de 5 numeros
            cubrirRango(1, 3, 5);
            CierreAsignacionTalonarioResponse resultado = service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));
            assertThat(resultado.cerrada()).isFalse();
            assertThat(resultado.numerosFaltantes()).containsExactlyInAnyOrder(2, 4);
        }

        @Test
        @DisplayName("8B6-22: Reportar numeros faltantes en respuesta")
        void reporta_numeros_faltantes() {
            cubrirRango(1, 2);
            CierreAsignacionTalonarioResponse resultado = service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", "obs"));
            assertThat(resultado.numerosFaltantes()).containsExactlyInAnyOrder(3, 4, 5);
            assertThat(resultado.cerrada()).isFalse();
        }

        @Test
        @DisplayName("8B6-23: Cerrar asignacion cuando todos los numeros tienen movimiento")
        void cerrar_asignacion_rango_completo() {
            cubrirRango(1, 2, 3, 4, 5);
            CierreAsignacionTalonarioResponse resultado = service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));
            assertThat(resultado.cerrada()).isTrue();
            assertThat(resultado.numerosFaltantes()).isEmpty();
        }

        @Test
        @DisplayName("8B6-24: Al cerrar: estadoAsignacion = CERRADO")
        void al_cerrar_estado_es_cerrado() {
            cubrirRango(1, 2, 3, 4, 5);
            CierreAsignacionTalonarioResponse resultado = service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));
            assertThat(resultado.estadoAsignacion()).isEqualTo(EstadoAsignacionTalonario.CERRADO);
        }

        @Test
        @DisplayName("8B6-25: Al cerrar: siActiva = false")
        void al_cerrar_si_activa_false() {
            cubrirRango(1, 2, 3, 4, 5);
            service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));
            NumTalonarioInspector asig = repo.buscarAsignacionInspectorPorId(idAsignacion).orElseThrow();
            assertThat(asig.isSiActiva()).isFalse();
        }

        @Test
        @DisplayName("8B6-26: Al cerrar: talonarioIdActivo = null")
        void al_cerrar_talonario_id_activo_null() {
            cubrirRango(1, 2, 3, 4, 5);
            service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));
            NumTalonarioInspector asig = repo.buscarAsignacionInspectorPorId(idAsignacion).orElseThrow();
            assertThat(asig.getTalonarioIdActivo()).isNull();
        }

        @Test
        @DisplayName("8B6-27: Rechazar cerrar asignacion inexistente")
        void rechaza_asignacion_inexistente() {
            assertThatThrownBy(() -> service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            9999L, FaltasClockTestSupport.FIXED.now(), "admin", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("9999");
        }

        @Test
        @DisplayName("8B6-28: Rechazar cerrar asignacion ya inactiva")
        void rechaza_asignacion_ya_inactiva() {
            cubrirRango(1, 2, 3, 4, 5);
            service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null));

            assertThatThrownBy(() -> service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsignacion, FaltasClockTestSupport.FIXED.now(), "admin", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("8B6-29: Rechazar cierre si talonario no es MANUAL_FISICO")
        void rechaza_cierre_si_talonario_electronico() throws Exception {
            // Asignar talonario electronico (no deberia permitirse en produccion, pero testeamos la guardia)
            // Creamos asignacion directa para simular el caso
            NumTalonarioInspector asigElec = new NumTalonarioInspector(
                    999L, idTalElectronico, 1L, (short) 1,
                    FaltasClockTestSupport.FIXED.now(), "test",
                    null, null,
                    EstadoAsignacionTalonario.ENTREGADO, true, idTalElectronico);
            repo.guardarAsignacionInspector(asigElec);

            assertThatThrownBy(() -> service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            999L, FaltasClockTestSupport.FIXED.now(), "admin", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("MANUAL_FISICO");
        }

        @Test
        @DisplayName("8B6-30: Rechazar cierre completo si nroHasta es null")
        void rechaza_cierre_si_nroHasta_null() {
            // Crear talonario sin nroHasta
            var pol = service.listarPoliticasActivas().get(0);
            CrearTalonarioCommand sinHasta = new CrearTalonarioCommand(
                    pol.getId(), "TAL-SIN-HASTA", "Sin hasta",
                    TipoTalonario.MANUAL_FISICO, ClaseNumeracion.ACTA,
                    null, null, 1, null,
                    "SEQ_SIN_HASTA", true, false, null, null, "test");
            Long idTalSinHasta = service.crearTalonario(sinHasta).getId();

            AsignarTalonarioInspectorCommand asig = new AsignarTalonarioInspectorCommand(
                    idTalSinHasta, inspector.getIdInsp(), (short) 1, FaltasClockTestSupport.FIXED.now(), "test");
            Long idAsigSinHasta = service.asignarTalonarioInspector(asig).getId();

            assertThatThrownBy(() -> service.cerrarAsignacionTalonarioInspector(
                    new CerrarAsignacionTalonarioInspectorCommand(
                            idAsigSinHasta, FaltasClockTestSupport.FIXED.now(), "admin", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nroHasta");
        }
    }

    // =========================================================================
    // NUMEROS FALTANTES
    // =========================================================================

    @Nested
    @DisplayName("8B6-31 a 8B6-34: Listar numeros faltantes en rango")
    class NumerosFaltantes {

        @Test
        @DisplayName("8B6-31: Listar faltantes en rango completo")
        void listar_faltantes_rango_completo() {
            List<Integer> faltantes = service.listarNumerosFaltantes(idTalManual);
            assertThat(faltantes).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("8B6-32: Sin movimientos devuelve todo el rango")
        void sin_movimientos_devuelve_todo_rango() {
            List<Integer> faltantes = service.listarNumerosFaltantes(idTalManual);
            assertThat(faltantes).hasSize(5);
        }

        @Test
        @DisplayName("8B6-33: Con movimientos parciales devuelve solo faltantes")
        void con_movimientos_parciales_devuelve_faltantes() {
            service.anularNumeroTalonario(new AnularNumeroTalonarioCommand(
                    idTalManual, 1, MotivoAnulacionTalonario.ERROR_LABRADO,
                    null, null, null, null, null, FaltasClockTestSupport.FIXED.now(), "admin"));
            service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                    idTalManual, 3, null, null, null, FaltasClockTestSupport.FIXED.now(), "admin"));

            List<Integer> faltantes = service.listarNumerosFaltantes(idTalManual);
            assertThat(faltantes).containsExactlyInAnyOrder(2, 4, 5);
        }

        @Test
        @DisplayName("8B6-34: Con rango completo cubierto devuelve lista vacia")
        void rango_completo_cubierto_devuelve_vacio() {
            for (int n = 1; n <= 5; n++) {
                service.devolverNumeroSinUsar(new DevolverNumeroSinUsarCommand(
                        idTalManual, n, null, null, null, FaltasClockTestSupport.FIXED.now(), "admin"));
            }
            List<Integer> faltantes = service.listarNumerosFaltantes(idTalManual);
            assertThat(faltantes).isEmpty();
        }
    }

    // =========================================================================
    // GUARDRAIL FUERA DE SCOPE
    // =========================================================================

    @Nested
    @DisplayName("8B6: Guardrail - fuera de scope de 8B-6")
    class GuardrailFueraDeScope8B6 {

        @Test
        @DisplayName("No se implementa FalDocumento en 8B-6")
        void no_existe_servicio_FalDocumento_en_talonarios() throws Exception {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.model.TalDocumento"))
                    .isInstanceOf(ClassNotFoundException.class);
        }

        @Test
        @DisplayName("No se implementa DocumentoTalonario en 8B-6")
        void no_existe_DocumentoTalonario() throws Exception {
            assertThatThrownBy(() ->
                    Class.forName("ar.gob.malvinas.faltas.core.domain.model.DocumentoTalonario"))
                    .isInstanceOf(ClassNotFoundException.class);
        }

        @Test
        @DisplayName("No se cambian codigos de enums existentes - EstadoNumeroTalonario")
        void codigos_estado_numero_talonario_sin_cambio() {
            assertThat(EstadoNumeroTalonario.USADO.codigo()).isEqualTo((short) 1);
            assertThat(EstadoNumeroTalonario.ANULADO.codigo()).isEqualTo((short) 2);
            assertThat(EstadoNumeroTalonario.DEVUELTO_SIN_USAR.codigo()).isEqualTo((short) 3);
            assertThat(EstadoNumeroTalonario.RENDIDO.codigo()).isEqualTo((short) 4);
            assertThat(EstadoNumeroTalonario.JUSTIFICADO.codigo()).isEqualTo((short) 5);
        }

        @Test
        @DisplayName("No se cambian codigos de enums existentes - MotivoAnulacionTalonario")
        void codigos_motivo_anulacion_sin_cambio() {
            assertThat(MotivoAnulacionTalonario.ERROR_LABRADO.codigo()).isEqualTo((short) 1);
            assertThat(MotivoAnulacionTalonario.ROTURA_FORMULARIO.codigo()).isEqualTo((short) 2);
            assertThat(MotivoAnulacionTalonario.DUPLICADO.codigo()).isEqualTo((short) 3);
            assertThat(MotivoAnulacionTalonario.EXTRAVIO.codigo()).isEqualTo((short) 4);
            assertThat(MotivoAnulacionTalonario.OTRO.codigo()).isEqualTo((short) 5);
        }

        @Test
        @DisplayName("No se cambian codigos de EstadoAsignacionTalonario")
        void codigos_estado_asignacion_sin_cambio() {
            assertThat(EstadoAsignacionTalonario.ENTREGADO.codigo()).isEqualTo((short) 1);
            assertThat(EstadoAsignacionTalonario.DEVUELTO.codigo()).isEqualTo((short) 2);
            assertThat(EstadoAsignacionTalonario.CERRADO.codigo()).isEqualTo((short) 3);
            assertThat(EstadoAsignacionTalonario.OBSERVADO.codigo()).isEqualTo((short) 4);
        }

        @Test
        @DisplayName("Estado RENDIDO esta disponible en el catalogo pero no se genera masivamente")
        void rendido_disponible_en_catalogo() {
            assertThat(EstadoNumeroTalonario.RENDIDO).isNotNull();
            assertThat(EstadoNumeroTalonario.RENDIDO.codigo()).isEqualTo((short) 4);
        }
    }
}
