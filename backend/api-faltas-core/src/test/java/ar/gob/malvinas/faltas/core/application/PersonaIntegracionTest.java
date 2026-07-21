package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoCombinacionService;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableContextBuilder;
import ar.gob.malvinas.faltas.core.application.combinacion.DocumentoVariableRegistry;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaDefaultService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoRedaccionService;
import ar.gob.malvinas.faltas.core.application.service.PersonaDomicilioService;
import ar.gob.malvinas.faltas.core.application.service.PersonaService;
import ar.gob.malvinas.faltas.core.domain.enums.ModoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDomicilio;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaContenidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaDefaultRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRedaccionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Slice 8F-11C: Integracion persona, domicilio y acta")
class PersonaIntegracionTest {

    private InMemoryPersonaRepository personaRepo;
    private InMemoryPersonaDomicilioRepository domRepo;
    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;

    private PersonaService personaService;
    private PersonaDomicilioService domService;
    private ActaService actaService;

    private static final Short PROV_MALVINAS = (short) 6;
    private static final Integer UT_MALVINAS = 60515;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        personaRepo = new InMemoryPersonaRepository();
        domRepo = new InMemoryPersonaDomicilioRepository();
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();

        personaService = new PersonaService(personaRepo, FaltasClockTestSupport.FIXED);
        domService = new PersonaDomicilioService(domRepo, personaRepo, id -> false, FaltasClockTestSupport.FIXED);

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, new InMemoryDocumentoRepository(),
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), personaRepo, FaltasClockTestSupport.FIXED);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // =========================================================================
    // Acta con persona
    // =========================================================================

    @Nested
    @DisplayName("Acta con persona")
    class ActaConPersona {

        @Test
        @DisplayName("Labrar acta con idPersonaInfractor existente lo referencia correctamente")
        void labrar_con_idPersona() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12345678",
                    "Perez", "Juan", null, null, null, null, "SYS");

            ComandoResultado resultado = actaService.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Pioneros 100", "Infraccion test",
                    null, null, null, null,
                    persona.getId(),
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

            Long actaId = resultado.idActa();
            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getIdPersonaInfractor()).isEqualTo(persona.getId());
        }

        @Test
        @DisplayName("Acta referencia persona por ID, no embebe nombre ni documento")
        void acta_no_embebe_datos_personales() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "11111111",
                    "Lopez", "Maria", null, null, null, null, "SYS");

            ComandoResultado resultado = actaService.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Calle Test", "Test",
                    null, null, null, null,
                    persona.getId(),
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

            FalActa acta = actaRepo.buscarPorId(resultado.idActa()).get();
            assertThat(acta.getIdPersonaInfractor()).isNotNull();
        }

        @Test
        @DisplayName("Persona debe existir antes del acta: seeder coherente")
        void persona_existe_antes_de_acta() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "Anterior", null, null, null, null, null, "SYS");

            assertThat(personaRepo.buscarPorId(persona.getId())).isPresent();

            ComandoResultado resultado = actaService.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Hecho 1", "Obs",
                    null, null, null, null, persona.getId(),
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

            assertThat(resultado.idActa()).isNotNull();
        }

        @Test
        @DisplayName("Labrar acta con infractorNombre crea persona minimal automaticamente")
        void labrar_legacy_crea_persona_minimal() {
            long antes = personaRepo.listarTodas().size();

            actaService.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Hecho", "Obs",
                    null, null, "Juan Perez", "99887766",
                    null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

            assertThat(personaRepo.listarTodas().size()).isEqualTo(antes + 1);
        }
    }

    // =========================================================================
    // Domicilios pertenecen a persona
    // =========================================================================

    @Nested
    @DisplayName("Domicilios pertenecen a persona")
    class DomiciliosPertenecenAPersona {

        @Test
        @DisplayName("Domicilio referencia a personaId de la persona creada")
        void domicilio_referencia_persona() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "TestFK", null, null, null, null, null, "SYS");

            FalPersonaDomicilio dom = domService.crear(persona.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    "Av. Test 100", null, true, null, null,
                    "Av. Test 100, Malvinas Argentinas", false,
                    null, null, null, "SYS");

            assertThat(dom.getPersonaId()).isEqualTo(persona.getId());
        }

        @Test
        @DisplayName("Domicilio de persona inexistente lanza excepcion")
        void domicilio_persona_inexistente_rechazado() {
            assertThatThrownBy(() ->
                    domService.crear(9999L, null,
                            TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                            true, true, false,
                            PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                            null, null, "0750100001", 1L, "12345", 2L,
                            null, null, true, null, null, "Dom test", false,
                            null, null, null, "SYS"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("idDomicilioInfractorAct en acta apunta a domicilio de la persona")
        void acta_referencia_domicilio_infractor() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "33445566",
                    "Referencia", null, null, null, null, null, "SYS");

            FalPersonaDomicilio dom = domService.crear(persona.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    "Calle Ref 1", null, true, null, null,
                    "Calle Ref 1, Malvinas Argentinas", false,
                    null, null, null, "SYS");

            ComandoResultado res = actaService.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), "Hecho", "Obs",
                    null, null, null, null,
                    persona.getId(),
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

            FalActa acta = actaRepo.buscarPorId(res.idActa()).get();
            acta.setIdDomicilioInfractorAct(dom.getId());
            actaRepo.guardar(acta);

            FalActa actaRelida = actaRepo.buscarPorId(res.idActa()).get();
            assertThat(actaRelida.getIdDomicilioInfractorAct()).isEqualTo(dom.getId());
            assertThat(dom.getPersonaId()).isEqualTo(persona.getId());
        }

        @Test
        @DisplayName("idDomicilioNotifAct en acta puede diferir de idDomicilioInfractorAct")
        void acta_domicilios_pueden_diferir() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "DualDom", null, null, null, null, null, "SYS");

            FalPersonaDomicilio domInfr = domService.crear(persona.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, false, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    null, null, true, null, null, "Lugar infraccion", false,
                    null, null, null, "SYS");

            FalPersonaDomicilio domNotif = domService.crear(persona.getId(), null,
                    TipoDomicilio.CONSTITUIDO, OrigenDomicilio.OPERADOR, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100002", 1L, "12346", 2L,
                    null, null, true, null, null, "Domicilio notif", false,
                    null, null, null, "SYS");

            assertThat(domInfr.getId()).isNotEqualTo(domNotif.getId());
            assertThat(domInfr.getPersonaId()).isEqualTo(domNotif.getPersonaId());
        }
    }

    // =========================================================================
    // Domicilio notificable
    // =========================================================================

    @Nested
    @DisplayName("Domicilio notificable")
    class DomicilioNotificable {

        @Test
        @DisplayName("Solo domicilio activo y siNotificable=true es valido para notificacion")
        void domicilio_notificable_ok() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "NotifTest", null, null, null, null, null, "SYS");

            FalPersonaDomicilio domNotif = domService.crear(persona.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    null, null, true, null, null, "Dom notificable", false,
                    null, null, null, "SYS");

            assertThat(domNotif.isSiActivo()).isTrue();
            assertThat(domNotif.isSiNotificable()).isTrue();
            assertThat(domRepo.buscarNotificablesPorPersonaId(persona.getId())).hasSize(1);
        }
    }

    // =========================================================================
    // Documento con persona y domicilio (DocumentoVariableContextBuilder)
    // =========================================================================

    @Nested
    @DisplayName("Contexto documental con persona y domicilio")
    class ContextoDocumentalPersonaDomicilio {

        @Test
        @DisplayName("buildDesdeActa incluye infractor.nombreCompleto de FalPersona")
        void contexto_incluye_nombre_infractor() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12345678",
                    "Gonzalez", "Carlos", null, null, null, null, "SYS");

            FalActa acta = new FalActa(1L, "uuid-test", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Av. Test 1", "Obs", null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    persona.getId(), FaltasClockTestSupport.FIXED.now(), "SYS");

            FalDocumento doc = new FalDocumento(10L, 1L,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(),
                    ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.BORRADOR,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq.NO_REQUIERE, null, FaltasClockTestSupport.FIXED.now());

            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    acta, doc, persona, null, null, null, null, FaltasClockTestSupport.FIXED.now());

            assertThat(ctx).containsKey("infractor.nombreCompleto");
            assertThat(ctx.get("infractor.nombreCompleto").toString()).contains("GONZALEZ");
        }

        @Test
        @DisplayName("buildDesdeActa incluye domicilioInfractor.texto de FalPersonaDomicilio")
        void contexto_incluye_domicilio_infractor() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "TestDom", null, null, null, null, null, "SYS");

            FalPersonaDomicilio dom = domService.crear(persona.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    "Calle Contexto 100", null, true, null, null,
                    "Calle Contexto 100, Malvinas Argentinas", false,
                    null, null, null, "SYS");

            FalActa acta = new FalActa(1L, "uuid-test", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Lugar hecho", "Obs", null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    persona.getId(), FaltasClockTestSupport.FIXED.now(), "SYS");
            acta.setIdDomicilioInfractorAct(dom.getId());

            FalDocumento doc = new FalDocumento(10L, 1L,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(),
                    ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.BORRADOR,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq.NO_REQUIERE, null, FaltasClockTestSupport.FIXED.now());

            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    acta, doc, persona, dom, null, null, null, FaltasClockTestSupport.FIXED.now());

            assertThat(ctx).containsKey("domicilioInfractor.texto");
            assertThat(ctx.get("domicilioInfractor.texto").toString())
                    .contains("Calle Contexto 100");
        }

        @Test
        @DisplayName("buildDesdeActa sin domicilio: domicilioInfractor.texto ausente del contexto")
        void contexto_sin_domicilio_ausente() {
            FalPersona persona = personaService.crear(TipoPersona.FISICA, null, null,
                    "TestNoDom", null, null, null, null, null, "SYS");

            FalActa acta = new FalActa(1L, "uuid-test", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Lugar hecho", "Obs", null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    persona.getId(), FaltasClockTestSupport.FIXED.now(), "SYS");

            FalDocumento doc = new FalDocumento(10L, 1L,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoDocu.ACTO_ADMINISTRATIVO,
                    FaltasClockTestSupport.FIXED.now(),
                    ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.BORRADOR,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq.NO_REQUIERE, null, FaltasClockTestSupport.FIXED.now());

            Map<String, Object> ctx = DocumentoVariableContextBuilder.buildDesdeActa(
                    acta, doc, persona, null, null, null, null, FaltasClockTestSupport.FIXED.now());

            assertThat(ctx).doesNotContainKey("domicilioInfractor.texto");
        }
    }

    // =========================================================================
    // DocumentoRedaccionService resuelve persona y domicilio desde acta
    // =========================================================================

    @Nested
    @DisplayName("DocumentoRedaccionService wiring persona y domicilio")
    class DocumentoRedaccionWiring {

        @Test
        @DisplayName("DocumentoRedaccionService resuelve PersonaDomicilioRepository cuando se inyecta")
        void service_tiene_repo_domicilio() {
            DocumentoRedaccionService svc = new DocumentoRedaccionService(
                    new InMemoryDocumentoRepository(),
                    new DocumentoPlantillaDefaultService(new InMemoryDocumentoPlantillaDefaultRepository()),
                    new InMemoryDocumentoPlantillaContenidoRepository(),
                    new InMemoryDocumentoRedaccionRepository(),
                    new DocumentoCombinacionService(new DocumentoVariableRegistry()),
                    actaRepo,
                    null, null,
                    personaRepo,
                    domRepo, FaltasClockTestSupport.FIXED);
            assertThat(svc).isNotNull();
        }
    }

    // =========================================================================
    // Seeders coherentes
    // =========================================================================

    @Nested
    @DisplayName("Casos del seeder funcional")
    class CasosSeeder {

        @Test
        @DisplayName("Persona fisica con documento: caso base")
        void caso_persona_fisica() {
            FalPersona p = personaService.crear(TipoPersona.FISICA, TipoDocumentoPersona.DNI, "12345678",
                    "Fisica", "Ana", null, null, null, null, "SYS");
            assertThat(p.getTipoPersona()).isEqualTo(TipoPersona.FISICA);
            assertThat(p.getNroDoc()).isEqualTo("12345678");
        }

        @Test
        @DisplayName("Persona juridica: caso base")
        void caso_persona_juridica() {
            FalPersona p = personaService.crear(TipoPersona.JURIDICA, TipoDocumentoPersona.CUIT, "30-99999-1",
                    null, null, "Empresa Test SA", null, null, null, "SYS");
            assertThat(p.getTipoPersona()).isEqualTo(TipoPersona.JURIDICA);
            assertThat(p.getRazonSocial()).isEqualTo("Empresa Test SA");
        }

        @Test
        @DisplayName("Persona sin documento: caso anonimo")
        void caso_persona_sin_documento() {
            FalPersona p = personaService.crear(TipoPersona.FISICA, null, null,
                    "Anonimo", "Infractor", null, null, null, null, "SYS");
            assertThat(p.getTipoDoc()).isNull();
            assertThat(p.getNroDoc()).isNull();
        }

        @Test
        @DisplayName("Domicilio Malvinas: provincia=6, municipio, idUT=60515")
        void caso_domicilio_malvinas() {
            FalPersona p = personaService.crear(TipoPersona.FISICA, null, null, "DomMalvinas", null, null, null, null, null, "SYS");
            FalPersonaDomicilio d = domService.crear(p.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    "Av. Malvinas 100", null, true, null, null, "Av. Malvinas 100", false,
                    null, null, null, "SYS");
            assertThat(d.getModoDomicilio()).isEqualTo(ModoDomicilio.MALVINAS_LOCAL);
            assertThat(d.getIdProvincia()).isEqualTo(PROV_MALVINAS);
        }

        @Test
        @DisplayName("Domicilio externo: provincias distintas a Malvinas")
        void caso_domicilio_externo() {
            FalPersona p = personaService.crear(TipoPersona.FISICA, null, null, "DomExterno", null, null, null, null, null, "SYS");
            FalPersonaDomicilio d = domService.crear(p.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.DDJJ, ModoDomicilio.EXTERNO,
                    true, true, true,
                    (short) 1, UnidadTerritorialTipo.MUNICIPIO, 62049,
                    1001L, 2001L,
                    null, null, null, null,
                    "Rivadavia 500", 500, false, null, "1002", "Rivadavia 500, San Martin", false,
                    null, null, null, "SYS");
            assertThat(d.getModoDomicilio()).isEqualTo(ModoDomicilio.EXTERNO);
            assertThat(d.getIdProvincia()).isEqualTo((short) 1);
        }

        @Test
        @DisplayName("Domicilio de notificacion distinto: dos domicilios activos para la misma persona")
        void caso_domicilio_notificacion_distinto() {
            FalPersona p = personaService.crear(TipoPersona.FISICA, null, null, "DualNotif", null, null, null, null, null, "SYS");

            FalPersonaDomicilio dReal = domService.crear(p.getId(), null,
                    TipoDomicilio.REAL, OrigenDomicilio.LABRADO, ModoDomicilio.MALVINAS_LOCAL,
                    true, false, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100001", 1L, "12345", 2L,
                    null, null, true, null, null, "Dom real", false,
                    null, null, null, "SYS");

            FalPersonaDomicilio dConstit = domService.crear(p.getId(), null,
                    TipoDomicilio.CONSTITUIDO, OrigenDomicilio.OPERADOR, ModoDomicilio.MALVINAS_LOCAL,
                    true, true, true,
                    PROV_MALVINAS, UnidadTerritorialTipo.MUNICIPIO, UT_MALVINAS,
                    null, null, "0750100002", 1L, "12346", 2L,
                    null, null, true, null, null, "Dom constituido notif", false,
                    null, null, null, "SYS");

            assertThat(dReal.getId()).isNotEqualTo(dConstit.getId());
            assertThat(domRepo.buscarNotificablesPorPersonaId(p.getId())).hasSize(1);
            assertThat(domRepo.buscarActivosPorPersonaId(p.getId())).hasSize(2);
        }
    }
}
