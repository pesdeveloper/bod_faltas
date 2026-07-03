package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.AgregarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDependenciaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.DesactivarHabilitacionFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.command.VersionarFirmanteCommand;
import ar.gob.malvinas.faltas.core.application.service.DependenciaService;
import ar.gob.malvinas.faltas.core.application.service.FirmanteService;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.FirmanteNoEncontradoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDependencia;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmante;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersion;
import ar.gob.malvinas.faltas.core.domain.model.FalFirmanteVersionHabilitacion;
import ar.gob.malvinas.faltas.core.repository.DependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.FirmanteRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 8A-3 + Micro-slice 8A-5.1: Firmantes in-memory alineados al modelo MariaDB.
 *
 * Micro-slice 8A-5.1: tipoDocu, rolFirmaReq, mecanismoFirmaReq corregidos a Short/short
 * para alinearse con tipo_docu SMALLINT, rol_firma_req SMALLINT, mecanismo_firma_req SMALLINT
 * del modelo MariaDB productivo.
 *
 * Valores SMALLINT usados en tests (representativos, sin enum todavia):
 *   tipoDocu: 1=ACTA_MULTA, 2=TIPO_B, 3=RESOLUCION_SANCION, 4=OTRO_TIPO
 *   rolFirmaReq: 10=FIRMANTE_PRIMARIO, 11=FIRMANTE_SECUNDARIO
 *   mecanismoFirmaReq: 5=ELECTRONICA (nullable)
 *
 * Enums RolFirmaReq y MecanismoFirmaReq quedan para slice posterior. TipoDocumento reemplazado por TipoDocu en 8C-1.
 */
@DisplayName("Slice 8A-3 + 8A-5.1: Firmantes in-memory")
class FirmanteTest {

    private DependenciaRepository dependenciaRepo;
    private DependenciaService dependenciaService;
    private FirmanteRepository firmanteRepo;
    private FirmanteService firmanteService;

    @BeforeEach
    void setUp() {
        dependenciaRepo = new InMemoryDependenciaRepository();
        dependenciaService = new DependenciaService(dependenciaRepo);
        firmanteRepo = new InMemoryFirmanteRepository();
        firmanteService = new FirmanteService(firmanteRepo, dependenciaRepo);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private FalDependencia crearDependencia() {
        return dependenciaService.crear(new CrearDependenciaCommand(
                null, "Dependencia Test", null,
                TipoActa.TRANSITO, LocalDate.now(), "sistema"));
    }

    private FalFirmante crearFirmanteBasico(String idUser) {
        return firmanteService.crear(new CrearFirmanteCommand(
                idUser, "Juan Firmante", null, null,
                null, null, LocalDate.now(), "sistema"));
    }

    private FalFirmante crearFirmanteConDep(String idUser, FalDependencia dep) {
        return firmanteService.crear(new CrearFirmanteCommand(
                idUser, "Juan Firmante", "Secretario", "Jefe Dep",
                dep.getIdDep(), 1, LocalDate.now(), "sistema"));
    }

    // =========================================================================
    // 8A3-01: Crear firmante valido sin dependencia
    // =========================================================================

    @Nested
    @DisplayName("8A3-01: Crear firmante valido sin dependencia")
    class CrearFirmanteValidoSinDepTest {

        @Test
        @DisplayName("Genera idFirmante, siActivo=true, verFirmante=1")
        void crear_sin_dep_genera_version_inicial() {
            FalFirmante f = crearFirmanteBasico("USER-F001");

            assertThat(f.getIdFirmante()).isNotNull();
            assertThat(f.getIdUser()).isEqualTo("USER-F001");
            assertThat(f.getNomFirmante()).isEqualTo("Juan Firmante");
            assertThat(f.isSiActivo()).isTrue();

            List<FalFirmanteVersion> versiones = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            assertThat(versiones).hasSize(1);
            assertThat(versiones.get(0).getVerFirmante()).isEqualTo(1);
            assertThat(versiones.get(0).getIdDep()).isNull();
            assertThat(versiones.get(0).getVerDep()).isNull();
            assertThat(versiones.get(0).isSiActivo()).isTrue();
        }
    }

    // =========================================================================
    // 8A3-02: Crear firmante valido con dependencia versionada existente
    // =========================================================================

    @Nested
    @DisplayName("8A3-02: Crear firmante valido con dependencia")
    class CrearFirmanteConDepTest {

        @Test
        @DisplayName("Version congela idDep y verDep")
        void crear_con_dep_congela_idDep_verDep() {
            FalDependencia dep = crearDependencia();
            FalFirmante f = crearFirmanteConDep("USER-F002", dep);

            List<FalFirmanteVersion> versiones = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            assertThat(versiones).hasSize(1);
            assertThat(versiones.get(0).getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(versiones.get(0).getVerDep()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8A3-03: Rechazar firmante sin idUser
    // =========================================================================

    @Nested
    @DisplayName("8A3-03: Rechazar firmante sin idUser")
    class SinIdUserTest {

        @Test
        @DisplayName("idUser nulo lanza PrecondicionVioladaException")
        void sin_idUser_nulo_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    null, "Nombre", null, null,
                    null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUser");
        }

        @Test
        @DisplayName("idUser en blanco lanza PrecondicionVioladaException")
        void sin_idUser_blanco_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "  ", "Nombre", null, null,
                    null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idUser");
        }
    }

    // =========================================================================
    // 8A3-04: Rechazar idUser duplicado
    // =========================================================================

    @Nested
    @DisplayName("8A3-04: Rechazar idUser duplicado")
    class IdUserDuplicadoTest {

        @Test
        @DisplayName("Segundo firmante con mismo idUser lanza PrecondicionVioladaException")
        void idUser_duplicado_falla() {
            crearFirmanteBasico("USER-DUP");
            assertThatThrownBy(() -> crearFirmanteBasico("USER-DUP"))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("USER-DUP");
        }
    }

    // =========================================================================
    // 8A3-05: Rechazar firmante sin nomFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-05: Rechazar firmante sin nomFirmante")
    class SinNombreFirmanteTest {

        @Test
        @DisplayName("nomFirmante nulo lanza PrecondicionVioladaException")
        void sin_nomFirmante_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F005", null, null, null,
                    null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nomFirmante");
        }

        @Test
        @DisplayName("nomFirmante en blanco lanza PrecondicionVioladaException")
        void sin_nomFirmante_blanco_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F005B", "  ", null, null,
                    null, null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("nomFirmante");
        }
    }

    // =========================================================================
    // 8A3-06: Permitir firmante sin rolFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-06: Permitir firmante sin rolFirmante")
    class SinRolFirmanteTest {

        @Test
        @DisplayName("rolFirmante nulo es valido")
        void sin_rolFirmante_es_valido() {
            FalFirmante f = firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F006", "Pedro Sin Rol", null, null,
                    null, null, LocalDate.now(), "sistema"));
            assertThat(f).isNotNull();
            List<FalFirmanteVersion> vers = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            assertThat(vers.get(0).getRolFirmante()).isNull();
        }
    }

    // =========================================================================
    // 8A3-07: Permitir firmante sin cargoFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-07: Permitir firmante sin cargoFirmante")
    class SinCargoFirmanteTest {

        @Test
        @DisplayName("cargoFirmante nulo es valido")
        void sin_cargoFirmante_es_valido() {
            FalFirmante f = firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F007", "Maria Sin Cargo", "Secretaria", null,
                    null, null, LocalDate.now(), "sistema"));
            assertThat(f).isNotNull();
            List<FalFirmanteVersion> vers = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            assertThat(vers.get(0).getCargoFirmante()).isNull();
        }
    }

    // =========================================================================
    // 8A3-08: Rechazar firmante sin fhVigDesde
    // =========================================================================

    @Nested
    @DisplayName("8A3-08: Rechazar firmante sin fhVigDesde")
    class SinFhVigDesdeTest {

        @Test
        @DisplayName("fhVigDesde nulo lanza PrecondicionVioladaException")
        void sin_fhVigDesde_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F008", "Pedro Vigencia", null, null,
                    null, null, null, "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("fhVigDesde");
        }
    }

    // =========================================================================
    // 8A3-09: Rechazar idDep sin verDep
    // =========================================================================

    @Nested
    @DisplayName("8A3-09: Rechazar idDep sin verDep")
    class IdDepSinVerDepTest {

        @Test
        @DisplayName("idDep con verDep nulo lanza PrecondicionVioladaException")
        void idDep_sin_verDep_falla() {
            FalDependencia dep = crearDependencia();
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F009", "Pablo Sin Ver", null, null,
                    dep.getIdDep(), null, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verDep");
        }
    }

    // =========================================================================
    // 8A3-10: Rechazar verDep sin idDep
    // =========================================================================

    @Nested
    @DisplayName("8A3-10: Rechazar verDep sin idDep")
    class VerDepSinIdDepTest {

        @Test
        @DisplayName("verDep con idDep nulo lanza PrecondicionVioladaException")
        void verDep_sin_idDep_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F010", "Carlos Sin Id", null, null,
                    null, 1, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("idDep");
        }
    }

    // =========================================================================
    // 8A3-11: Rechazar dependencia inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A3-11: Rechazar dependencia inexistente")
    class DependenciaInexistenteTest {

        @Test
        @DisplayName("idDep no registrado lanza PrecondicionVioladaException")
        void dep_inexistente_falla() {
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F011", "Firmante Dep Mala", null, null,
                    9999L, 1, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 8A3-12: Rechazar version de dependencia inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A3-12: Rechazar version de dependencia inexistente")
    class VersionDepInexistenteTest {

        @Test
        @DisplayName("verDep inexistente lanza PrecondicionVioladaException")
        void verDep_inexistente_falla() {
            FalDependencia dep = crearDependencia();
            assertThatThrownBy(() -> firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F012", "Firmante VerDep Mala", null, null,
                    dep.getIdDep(), 99, LocalDate.now(), "sistema")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    // =========================================================================
    // 8A3-13: verFirmante inicial = 1
    // =========================================================================

    @Nested
    @DisplayName("8A3-13: verFirmante inicial = 1")
    class VerFirmanteInicialTest {

        @Test
        @DisplayName("Al crear, la primera version tiene verFirmante=1")
        void version_inicial_es_1() {
            FalFirmante f = crearFirmanteBasico("USER-F013");
            List<FalFirmanteVersion> vers = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            assertThat(vers).hasSize(1);
            assertThat(vers.get(0).getVerFirmante()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8A3-14: Versionar incrementa verFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-14: Versionar incrementa verFirmante")
    class VersionarIncrementaVerTest {

        @Test
        @DisplayName("Versionar crea version con verFirmante=2")
        void versionar_incrementa_ver() {
            FalFirmante f = crearFirmanteBasico("USER-F014");
            FalFirmanteVersion v2 = firmanteService.versionar(new VersionarFirmanteCommand(
                    f.getIdFirmante(), "Nombre V2", null, null,
                    null, null, LocalDate.now().plusDays(1), "admin"));
            assertThat(v2.getVerFirmante()).isEqualTo(2);
        }
    }

    // =========================================================================
    // 8A3-15: Versionar cierra version anterior
    // =========================================================================

    @Nested
    @DisplayName("8A3-15: Versionar cierra version anterior")
    class VersionarCierraAnteriorTest {

        @Test
        @DisplayName("Version anterior queda siActivo=false con fhVigHasta asignada")
        void versionar_cierra_version_anterior() {
            FalFirmante f = crearFirmanteBasico("USER-F015");
            LocalDate fhV2 = LocalDate.now().plusDays(5);

            firmanteService.versionar(new VersionarFirmanteCommand(
                    f.getIdFirmante(), "Nombre V2", null, null,
                    null, null, fhV2, "admin"));

            List<FalFirmanteVersion> versiones = firmanteRepo.findVersionesByFirmante(f.getIdFirmante());
            FalFirmanteVersion v1 = versiones.stream().filter(v -> v.getVerFirmante() == 1).findFirst().orElseThrow();
            FalFirmanteVersion v2 = versiones.stream().filter(v -> v.getVerFirmante() == 2).findFirst().orElseThrow();

            assertThat(v1.isSiActivo()).isFalse();
            assertThat(v1.getFhVigHasta()).isEqualTo(fhV2);
            assertThat(v2.isSiActivo()).isTrue();
            assertThat(v2.getFhVigHasta()).isNull();
        }
    }

    // =========================================================================
    // 8A3-16: Versionar congela identidad/cargo/dependencia
    // =========================================================================

    @Nested
    @DisplayName("8A3-16: Versionar congela datos")
    class VersionarCongelaTest {

        @Test
        @DisplayName("Nueva version congela nomFirmante y rol/cargo informados")
        void versionar_congela_datos() {
            FalDependencia dep = crearDependencia();
            FalFirmante f = crearFirmanteConDep("USER-F016", dep);

            FalFirmanteVersion v2 = firmanteService.versionar(new VersionarFirmanteCommand(
                    f.getIdFirmante(), "Nombre Nuevo", "Juez", "Magistrado",
                    dep.getIdDep(), 1, LocalDate.now().plusDays(1), "admin"));

            assertThat(v2.getNomFirmante()).isEqualTo("Nombre Nuevo");
            assertThat(v2.getRolFirmante()).isEqualTo("Juez");
            assertThat(v2.getCargoFirmante()).isEqualTo("Magistrado");
            assertThat(v2.getIdDep()).isEqualTo(dep.getIdDep());
            assertThat(v2.getVerDep()).isEqualTo(1);
        }
    }

    // =========================================================================
    // 8A3-17: Nueva version nace sin habilitaciones
    // =========================================================================

    @Nested
    @DisplayName("8A3-17: Nueva version nace sin habilitaciones")
    class NuevaVersionSinHabilitacionesTest {

        @Test
        @DisplayName("Al versionar, la nueva version no hereda habilitaciones de la anterior")
        void nueva_version_nace_sin_habilitaciones() {
            FalFirmante f = crearFirmanteBasico("USER-F017");

            // agregar habilitacion a version 1 (tipoDocu=1, rolFirmaReq=10)
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));

            // versionar
            FalFirmanteVersion v2 = firmanteService.versionar(new VersionarFirmanteCommand(
                    f.getIdFirmante(), "Nombre V2", null, null,
                    null, null, LocalDate.now().plusDays(1), "admin"));

            List<FalFirmanteVersionHabilitacion> habsV2 =
                    firmanteRepo.findHabilitacionesByVersion(f.getIdFirmante(), v2.getVerFirmante());
            assertThat(habsV2).isEmpty();
        }
    }

    // =========================================================================
    // 8A3-18: Rechazar versionar firmante inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A3-18: Rechazar versionar firmante inexistente")
    class VersionarInexistenteTest {

        @Test
        @DisplayName("Versionar firmante no registrado lanza FirmanteNoEncontradoException")
        void versionar_inexistente_falla() {
            assertThatThrownBy(() -> firmanteService.versionar(new VersionarFirmanteCommand(
                    9999L, "Nombre", null, null,
                    null, null, LocalDate.now(), "admin")))
                    .isInstanceOf(FirmanteNoEncontradoException.class)
                    .hasMessageContaining("9999");
        }
    }

    // =========================================================================
    // 8A3-19: Agregar habilitacion valida con valores SMALLINT
    // =========================================================================

    @Nested
    @DisplayName("8A3-19: Agregar habilitacion valida con SMALLINT")
    class AgregarHabilitacionValidaTest {

        @Test
        @DisplayName("Habilitacion con tipoDocu=1, rolFirmaReq=10 se crea con siActivo=true")
        void agregar_habilitacion_valida_smallint() {
            FalFirmante f = crearFirmanteBasico("USER-F019");

            FalFirmanteVersionHabilitacion hab = firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));

            assertThat(hab.getIdFirmante()).isEqualTo(f.getIdFirmante());
            assertThat(hab.getVerFirmante()).isEqualTo(1);
            assertThat(hab.getTipoDocu()).isEqualTo((short) 1);
            assertThat(hab.getRolFirmaReq()).isEqualTo((short) 10);
            assertThat(hab.getMecanismoFirmaReq()).isNull();
            assertThat(hab.isSiActivo()).isTrue();
        }
    }

    // =========================================================================
    // 8A3-20: Rechazar habilitacion con firmante inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A3-20: Rechazar habilitacion con firmante inexistente")
    class HabilitacionFirmanteInexistenteTest {

        @Test
        @DisplayName("firmante inexistente lanza FirmanteNoEncontradoException")
        void habilitacion_firmante_inexistente_falla() {
            assertThatThrownBy(() -> firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            9999L, 1, (short) 1, (short) 10, null, "admin")))
                    .isInstanceOf(FirmanteNoEncontradoException.class);
        }
    }

    // =========================================================================
    // 8A3-21: Rechazar habilitacion con version inexistente
    // =========================================================================

    @Nested
    @DisplayName("8A3-21: Rechazar habilitacion con version inexistente")
    class HabilitacionVersionInexistenteTest {

        @Test
        @DisplayName("version 99 inexistente lanza PrecondicionVioladaException")
        void habilitacion_version_inexistente_falla() {
            FalFirmante f = crearFirmanteBasico("USER-F021");
            assertThatThrownBy(() -> firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 99, (short) 1, (short) 10, null, "admin")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("verFirmante");
        }
    }

    // =========================================================================
    // 8A3-22: Rechazar habilitacion sin tipoDocu
    // =========================================================================

    @Nested
    @DisplayName("8A3-22: Rechazar habilitacion sin tipoDocu")
    class HabilitacionSinTipoDocuTest {

        @Test
        @DisplayName("tipoDocu nulo lanza PrecondicionVioladaException")
        void sin_tipoDocu_falla() {
            FalFirmante f = crearFirmanteBasico("USER-F022");
            assertThatThrownBy(() -> firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, null, (short) 10, null, "admin")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("tipoDocu");
        }
    }

    // =========================================================================
    // 8A3-23: Rechazar habilitacion sin rolFirmaReq
    // =========================================================================

    @Nested
    @DisplayName("8A3-23: Rechazar habilitacion sin rolFirmaReq")
    class HabilitacionSinRolFirmaReqTest {

        @Test
        @DisplayName("rolFirmaReq nulo lanza PrecondicionVioladaException")
        void sin_rolFirmaReq_falla() {
            FalFirmante f = crearFirmanteBasico("USER-F023");
            assertThatThrownBy(() -> firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, (short) 1, null, null, "admin")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("rolFirmaReq");
        }
    }

    // =========================================================================
    // 8A3-24: Permitir habilitacion sin mecanismoFirmaReq
    // =========================================================================

    @Nested
    @DisplayName("8A3-24: Permitir habilitacion sin mecanismoFirmaReq")
    class HabilitacionSinMecanismoTest {

        @Test
        @DisplayName("mecanismoFirmaReq nulo es valido")
        void sin_mecanismo_es_valido() {
            FalFirmante f = crearFirmanteBasico("USER-F024");
            FalFirmanteVersionHabilitacion hab = firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));
            assertThat(hab.getMecanismoFirmaReq()).isNull();
        }
    }

    // =========================================================================
    // 8A3-25: Rechazar duplicado activo de tipoDocu + rolFirmaReq (clave numerica)
    // =========================================================================

    @Nested
    @DisplayName("8A3-25: Rechazar duplicado activo con clave numerica tipoDocu + rolFirmaReq")
    class HabilitacionDuplicadaTest {

        @Test
        @DisplayName("Segundo registro con misma PK funcional activa lanza PrecondicionVioladaException")
        void habilitacion_duplicada_activa_falla() {
            FalFirmante f = crearFirmanteBasico("USER-F025");
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));

            assertThatThrownBy(() -> firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("habilitacion activa");
        }
    }

    // =========================================================================
    // 8A3-26: Listar habilitaciones de version
    // =========================================================================

    @Nested
    @DisplayName("8A3-26: Listar habilitaciones de version")
    class ListarHabilitacionesTest {

        @Test
        @DisplayName("listarHabilitaciones devuelve todas las habilitaciones de la version")
        void listar_habilitaciones() {
            FalFirmante f = crearFirmanteBasico("USER-F026");
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 11, null, "admin"));

            List<FalFirmanteVersionHabilitacion> habs =
                    firmanteService.listarHabilitaciones(f.getIdFirmante(), 1);
            assertThat(habs).hasSize(2);
        }
    }

    // =========================================================================
    // 8A3-27: Desactivar habilitacion usando clave numerica
    // =========================================================================

    @Nested
    @DisplayName("8A3-27: Desactivar habilitacion usando clave numerica")
    class DesactivarHabilitacionTest {

        @Test
        @DisplayName("Habilitacion desactivada queda con siActivo=false")
        void desactivar_habilitacion() {
            FalFirmante f = crearFirmanteBasico("USER-F027");
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));

            firmanteService.desactivarHabilitacion(new DesactivarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, "admin"));

            List<FalFirmanteVersionHabilitacion> habs =
                    firmanteRepo.findHabilitacionesByVersion(f.getIdFirmante(), 1);
            assertThat(habs).hasSize(1);
            assertThat(habs.get(0).isSiActivo()).isFalse();
        }
    }

    // =========================================================================
    // 8A3-28: Habilitacion desactivada no aparece como activa
    // =========================================================================

    @Nested
    @DisplayName("8A3-28: Habilitacion desactivada no aparece como activa")
    class HabilitacionDesactivadaNoActivaTest {

        @Test
        @DisplayName("findHabilitacionActiva retorna empty para habilitacion desactivada")
        void habilitacion_desactivada_no_es_activa() {
            FalFirmante f = crearFirmanteBasico("USER-F028");
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));
            firmanteService.desactivarHabilitacion(new DesactivarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, "admin"));

            boolean activa = firmanteRepo.existeHabilitacionActiva(
                    f.getIdFirmante(), 1, (short) 1, (short) 10);
            assertThat(activa).isFalse();
        }
    }

    // =========================================================================
    // 8A3-29: Una version puede tener multiples habilitaciones
    // =========================================================================

    @Nested
    @DisplayName("8A3-29: Version con multiples habilitaciones")
    class MultiplesHabilitacionesTest {

        @Test
        @DisplayName("Una version puede acumular habilitaciones distintas (tipoDocu y/o rolFirmaReq distintos)")
        void multiples_habilitaciones_por_version() {
            FalFirmante f = crearFirmanteBasico("USER-F029");
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 10, null, "admin"));
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 2, (short) 10, null, "admin"));
            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 1, (short) 11, (short) 5, "admin"));

            List<FalFirmanteVersionHabilitacion> habs =
                    firmanteRepo.findHabilitacionesByVersion(f.getIdFirmante(), 1);
            assertThat(habs).hasSize(3);
        }
    }

    // =========================================================================
    // 8A3-30: rolFirmante descriptivo no autoriza documento
    // =========================================================================

    @Nested
    @DisplayName("8A3-30: rolFirmante descriptivo no autoriza documentos")
    class RolFirmanteNoAutorizaTest {

        @Test
        @DisplayName("rolFirmante presente en version no otorga habilitacion automatica")
        void rolFirmante_no_otorga_habilitacion() {
            FalFirmante f = firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F030", "Firmante Con Rol", "Secretario", null,
                    null, null, LocalDate.now(), "sistema"));

            // version tiene rolFirmante pero no hay habilitaciones
            List<FalFirmanteVersionHabilitacion> habs =
                    firmanteRepo.findHabilitacionesByVersion(f.getIdFirmante(), 1);
            assertThat(habs).isEmpty();

            boolean tieneHabilitacion = firmanteRepo.existeHabilitacionActiva(
                    f.getIdFirmante(), 1, (short) 1, (short) 10);
            assertThat(tieneHabilitacion).isFalse();
        }
    }

    // =========================================================================
    // 8A3-31: Autorizacion se expresa mediante FalFirmanteVersionHabilitacion
    // =========================================================================

    @Nested
    @DisplayName("8A3-31: Autorizacion real via FalFirmanteVersionHabilitacion")
    class AutorizacionViaHabilitacionTest {

        @Test
        @DisplayName("Habilitacion agregada explicitamente otorga autorizacion para tipoDocu+rolFirmaReq")
        void habilitacion_expresa_autorizacion_documental() {
            FalFirmante f = firmanteService.crear(new CrearFirmanteCommand(
                    "USER-F031", "Firmante Real", "Juez", "Magistrado",
                    null, null, LocalDate.now(), "sistema"));

            firmanteService.agregarHabilitacion(new AgregarHabilitacionFirmanteCommand(
                    f.getIdFirmante(), 1, (short) 3, (short) 10, (short) 5, "admin"));

            boolean autorizado = firmanteRepo.existeHabilitacionActiva(
                    f.getIdFirmante(), 1, (short) 3, (short) 10);
            assertThat(autorizado).isTrue();

            boolean noAutorizado = firmanteRepo.existeHabilitacionActiva(
                    f.getIdFirmante(), 1, (short) 4, (short) 10);
            assertThat(noAutorizado).isFalse();
        }
    }

    // =========================================================================
    // 8A3-32: Buscar firmante por id
    // =========================================================================

    @Nested
    @DisplayName("8A3-32: Buscar firmante por id")
    class BuscarPorIdTest {

        @Test
        @DisplayName("Firmante creado es obtenible por su idFirmante")
        void obtener_por_id() {
            FalFirmante f = crearFirmanteBasico("USER-F032");
            FalFirmante obtenido = firmanteService.obtener(f.getIdFirmante());
            assertThat(obtenido.getIdFirmante()).isEqualTo(f.getIdFirmante());
            assertThat(obtenido.getIdUser()).isEqualTo("USER-F032");
        }
    }

    // =========================================================================
    // 8A3-33: Firmante inexistente retorna error controlado
    // =========================================================================

    @Nested
    @DisplayName("8A3-33: Firmante inexistente retorna error controlado")
    class FirmanteInexistenteTest {

        @Test
        @DisplayName("obtener con id inexistente lanza FirmanteNoEncontradoException")
        void obtener_inexistente_lanza_excepcion() {
            assertThatThrownBy(() -> firmanteService.obtener(9999L))
                    .isInstanceOf(FirmanteNoEncontradoException.class)
                    .hasMessageContaining("9999");
        }
    }

    // =========================================================================
    // 8A3-34: Listar firmantes activos
    // =========================================================================

    @Nested
    @DisplayName("8A3-34: Listar firmantes activos")
    class ListarActivosTest {

        @Test
        @DisplayName("Firmante creado aparece en la lista de activos")
        void firmante_creado_aparece_activos() {
            FalFirmante f = crearFirmanteBasico("USER-F034");
            List<FalFirmante> activos = firmanteService.listarActivos();
            assertThat(activos).anyMatch(x -> x.getIdFirmante().equals(f.getIdFirmante()));
        }
    }

    // =========================================================================
    // 8A3-35: Guardrail: no se implemento FalDocumentoFirmaReq
    // =========================================================================

    @Nested
    @DisplayName("8A3-35: Guardrail - FalDocumentoFirmaReq no implementado")
    class GuardrailFalDocumentoFirmaReqTest {

        @Test
        @DisplayName("La clase FalDocumentoFirmaReq no existe en el classpath")
        void FalDocumentoFirmaReq_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.model.FalDocumentoFirmaReq");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("FalDocumentoFirmaReq existe desde 8C-4").isTrue();
        }
    }

    // =========================================================================
    // 8A3-36: Guardrail: no se creo TipoFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-36: Guardrail - TipoFirmante no creado")
    class GuardrailTipoFirmanteTest {

        @Test
        @DisplayName("La clase TipoFirmante no existe en el classpath")
        void TipoFirmante_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.enums.TipoFirmante");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("TipoFirmante no debe existir en 8A-3").isFalse();
        }
    }

    // =========================================================================
    // 8A3-37: Guardrail: no se uso tipo_firma como rol en FalFirmanteVersion
    // =========================================================================

    @Nested
    @DisplayName("8A3-37: Guardrail - tipo_firma no en FalFirmanteVersion")
    class GuardrailTipoFirmaEnVersionTest {

        @Test
        @DisplayName("FalFirmanteVersion no tiene campo tipoFirma")
        void version_no_tiene_campo_tipoFirma() {
            boolean tieneCampo = false;
            for (Field f : FalFirmanteVersion.class.getDeclaredFields()) {
                if (f.getName().toLowerCase().contains("tipofirm")) {
                    tieneCampo = true;
                    break;
                }
            }
            assertThat(tieneCampo).as("FalFirmanteVersion no debe tener campo tipoFirma").isFalse();
        }
    }

    // =========================================================================
    // 8A3-38: Guardrail: no se creo EstadoFirmante
    // =========================================================================

    @Nested
    @DisplayName("8A3-38: Guardrail - EstadoFirmante no creado")
    class GuardrailEstadoFirmanteTest {

        @Test
        @DisplayName("La clase EstadoFirmante no existe en el classpath")
        void EstadoFirmante_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmante");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("EstadoFirmante no debe existir en 8A-3").isFalse();
        }
    }

    // =========================================================================
    // 8A3-39: Habilitacion valida con mecanismoFirmaReq SMALLINT informado
    // =========================================================================

    @Nested
    @DisplayName("8A3-39: Habilitacion valida con mecanismoFirmaReq Short informado")
    class HabilitacionConMecanismoTest {

        @Test
        @DisplayName("mecanismoFirmaReq Short informado se persiste correctamente")
        void habilitacion_con_mecanismo_smallint() {
            FalFirmante f = crearFirmanteBasico("USER-F039");

            FalFirmanteVersionHabilitacion hab = firmanteService.agregarHabilitacion(
                    new AgregarHabilitacionFirmanteCommand(
                            f.getIdFirmante(), 1, (short) 1, (short) 10, (short) 5, "admin"));

            assertThat(hab.getMecanismoFirmaReq()).isEqualTo((short) 5);
            assertThat(hab.getTipoDocu()).isEqualTo((short) 1);
            assertThat(hab.getRolFirmaReq()).isEqualTo((short) 10);
            assertThat(hab.isSiActivo()).isTrue();
        }
    }

    // =========================================================================
    // 8A3-40: Guardrail - no se crearon enums documentales en micro-slice 8A-5.1
    // =========================================================================

    @Nested
    @DisplayName("8A3-40: Guardrail - no se crearon enums documentales en 8A-5.1")
    class GuardrailEnumsDocumentalesTest {

        @Test
        @DisplayName("RolFirmaReq no existe en el classpath")
        void RolFirmaReq_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.enums.RolFirmaReq");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("RolFirmaReq no debe existir hasta 8C").isFalse();
        }

        @Test
        @DisplayName("MecanismoFirmaReq no existe en el classpath")
        void MecanismoFirmaReq_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.enums.MecanismoFirmaReq");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("MecanismoFirmaReq no debe existir hasta 8C").isFalse();
        }

        @Test
        @DisplayName("EstadoFirmaReq no existe en el classpath")
        void EstadoFirmaReq_no_existe() {
            boolean existe = false;
            try {
                Class.forName("ar.gob.malvinas.faltas.core.domain.enums.EstadoFirmaReq");
                existe = true;
            } catch (ClassNotFoundException e) {
                existe = false;
            }
            assertThat(existe).as("EstadoFirmaReq existe desde 8C-4").isTrue();
        }
    }
}

