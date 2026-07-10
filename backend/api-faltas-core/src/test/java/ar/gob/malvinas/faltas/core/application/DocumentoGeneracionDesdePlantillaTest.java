package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoDesdePlantillaCommand;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.ActaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Micro-slice 8C-3: Generacion de FalDocumento snapshot desde plantilla documental.
 */
@DisplayName("Micro-slice 8C-3: Generacion de FalDocumento desde plantilla")
class DocumentoGeneracionDesdePlantillaTest {

    private ActaRepository actaRepo;
    private DocumentoRepository docRepo;
    private DocumentoPlantillaRepository plantillaRepo;
    private DocumentoService docService;
    private DocumentoPlantillaService plantillaService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        docRepo = new InMemoryDocumentoRepository();
        plantillaRepo = new InMemoryDocumentoPlantillaRepository();
        ActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        ActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        DocumentoFirmaRepository firmaRepo = new InMemoryDocumentoFirmaRepository();
        FalloActaRepository falloRepo = new InMemoryFalloActaRepository();
        ApelacionActaRepository apelacionRepo = new InMemoryApelacionActaRepository();
        PagoVoluntarioRepository pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        PagoCondenaRepository pagoCondRepo = new InMemoryPagoCondenaRepository();

        ar.gob.malvinas.faltas.core.repository.NotificacionRepository notifRepo = new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoVolRepo, falloRepo, apelacionRepo, pagoCondRepo, FaltasClockTestSupport.FIXED);

        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo, plantillaRepo,
                new ar.gob.malvinas.faltas.core.application.service.TalonarioService(new ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED),
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(),
                new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(), FaltasClockTestSupport.FIXED);
        plantillaService = new DocumentoPlantillaService(plantillaRepo, FaltasClockTestSupport.FIXED);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(
                id, UUID.randomUUID().toString(),
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Belgrano 200", "Calle 123", null, null, null, "Juan Perez", "12345678",
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        actaRepo.guardar(acta);
        return acta;
    }

    private FalDocumentoPlantilla crearPlantillaActivaNoNumerada(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaActivaNumeradaMomentoEnvio(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, null,
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_AUTORIDAD,
                true, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA,
                false, true, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        AgregarFirmaReqPlantillaCommand firmaReq = new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, "sistema");
        plantillaService.agregarFirmaReq(firmaReq);
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaActivaFirmaMultiple(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, null,
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_MULTIPLE,
                true, MomentoNumeracionDocu.AL_FIRMAR,
                false, true, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        AgregarFirmaReqPlantillaCommand req1 = new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 1, null, true, true, "sistema");
        AgregarFirmaReqPlantillaCommand req2 = new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 2, (short) 2, null, true, true, "sistema");
        plantillaService.agregarFirmaReq(req1);
        plantillaService.agregarFirmaReq(req2);
        return plantillaService.activar(p.getId());
    }

    private FalDocumentoPlantilla crearPlantillaActivaFirmaInspector(String codigo) {
        CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, null,
                TipoDocu.ACTA_INFRACCION, AccionDocumental.GENERAR_ACTA_INFRACCION, TipoActa.TRANSITO,
                TipoFirmaReq.FIRMA_INSPECTOR,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
        FalDocumentoPlantilla p = plantillaService.crear(cmd);
        AgregarFirmaReqPlantillaCommand req = new AgregarFirmaReqPlantillaCommand(
                p.getId(), (short) 1, (short) 2, null, true, true, "sistema");
        plantillaService.agregarFirmaReq(req);
        return plantillaService.activar(p.getId());
    }

    private GenerarDocumentoDesdePlantillaCommand cmd(Long idActa, Long plantillaId) {
        return new GenerarDocumentoDesdePlantillaCommand(idActa, plantillaId, "usr-test");
    }

    // =========================================================================
    // Generacion valida
    // =========================================================================

    @Nested
    @DisplayName("Generacion valida desde plantilla")
    class GeneracionValida {

        @Test
        @DisplayName("1. Genera documento desde plantilla activa no numerada")
        void genera_desde_plantilla_no_numerada() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-001");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc).isNotNull();
            assertThat(doc.getId()).isNotNull();
        }

        @Test
        @DisplayName("2. Genera documento desde plantilla activa numerada AL_ENVIAR_A_FIRMA")
        void genera_desde_plantilla_numerada_momento_envio() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNumeradaMomentoEnvio("PLNT-8C3-002");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc).isNotNull();
            assertThat(doc.getId()).isNotNull();
        }

        @Test
        @DisplayName("3. Documento nace con EstadoDocu.BORRADOR")
        void documento_nace_borrador() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-003");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("4. Documento copia tipoDocu desde plantilla")
        void documento_copia_tipo_docu() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-004");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getTipoDocu()).isEqualTo(plantilla.getTipoDocu());
            assertThat(doc.getTipoDocu()).isEqualTo(TipoDocu.CONSTANCIA);
        }

        @Test
        @DisplayName("5. Documento copia tipoFirmaReq desde plantilla")
        void documento_copia_tipo_firma_req() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNumeradaMomentoEnvio("PLNT-8C3-005");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getTipoFirmaReq()).isEqualTo(plantilla.getTipoFirmaReq());
            assertThat(doc.getTipoFirmaReq()).isEqualTo(TipoFirmaReq.FIRMA_AUTORIDAD);
        }

        @Test
        @DisplayName("6. Documento guarda plantillaId")
        void documento_guarda_plantilla_id() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-006");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getPlantillaId()).isEqualTo(plantilla.getId());
        }

        @Test
        @DisplayName("7. Documento guarda idActa")
        void documento_guarda_id_acta() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-007");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getIdActa()).isEqualTo(acta.getId());
        }

        @Test
        @DisplayName("8. Documento no tiene nroDocu (queda null)")
        void documento_sin_nro_docu() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-008");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("9. Documento no tiene idTalonario (queda null)")
        void documento_sin_id_talonario() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-009");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getIdTalonario()).isNull();
        }

        @Test
        @DisplayName("10. Documento no tiene nroTalonarioUsado (queda null)")
        void documento_sin_nro_talonario_usado() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-010");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getNroTalonarioUsado()).isNull();
        }
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("11. Falla si acta no existe")
        void falla_si_acta_no_existe() {
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-011");

            assertThatThrownBy(() ->
                docService.generarDesdePlantilla(cmd(9999L, plantilla.getId())))
                .isInstanceOf(ActaNoEncontradaException.class);
        }

        @Test
        @DisplayName("12. Falla si plantilla no existe")
        void falla_si_plantilla_no_existe() {
            FalActa acta = crearActa();

            assertThatThrownBy(() ->
                docService.generarDesdePlantilla(cmd(acta.getId(), 9999L)))
                .isInstanceOf(DocumentoPlantillaNoEncontradaException.class);
        }

        @Test
        @DisplayName("13. Falla si plantilla no esta activa")
        void falla_si_plantilla_no_activa() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmdP = new CrearDocumentoPlantillaCommand(
                    "PLNT-8C3-013", "Inactiva", null,
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla plantilla = plantillaService.crear(cmdP);

            assertThatThrownBy(() ->
                docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId())))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("activa");
        }

        @Test
        @DisplayName("14. Falla si plantilla esta vencida (fhVigHasta anterior a hoy)")
        void falla_si_plantilla_vencida() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmdP = new CrearDocumentoPlantillaCommand(
                    "PLNT-8C3-014", "Vencida", null,
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, true,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "sistema");
            FalDocumentoPlantilla plantilla = plantillaService.crear(cmdP);
            plantilla.setSiActiva(true);

            assertThatThrownBy(() ->
                docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId())))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("vencida");
        }

        @Test
        @DisplayName("15. Falla si idUserAlta es blank")
        void falla_si_id_user_alta_blank() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-015");
            GenerarDocumentoDesdePlantillaCommand cmd =
                    new GenerarDocumentoDesdePlantillaCommand(acta.getId(), plantilla.getId(), "   ");

            assertThatThrownBy(() -> docService.generarDesdePlantilla(cmd))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("idUserAlta");
        }

        @Test
        @DisplayName("16. Falla si idActa es null")
        void falla_si_id_acta_null() {
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-016");
            GenerarDocumentoDesdePlantillaCommand cmd =
                    new GenerarDocumentoDesdePlantillaCommand(null, plantilla.getId(), "usr-test");

            assertThatThrownBy(() -> docService.generarDesdePlantilla(cmd))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("idActa");
        }

        @Test
        @DisplayName("17. Falla si plantillaId es null")
        void falla_si_plantilla_id_null() {
            FalActa acta = crearActa();
            GenerarDocumentoDesdePlantillaCommand cmd =
                    new GenerarDocumentoDesdePlantillaCommand(acta.getId(), null, "usr-test");

            assertThatThrownBy(() -> docService.generarDesdePlantilla(cmd))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("plantillaId");
        }
    }

    // =========================================================================
    // Numeracion fuera de alcance
    // =========================================================================

    @Nested
    @DisplayName("Numeracion fuera de alcance en 8C-3")
    class NumeracionFueraDeAlcance {

        @Test
        @DisplayName("18. AL_CREAR intenta numerar automaticamente; falla si no hay dependencia registrada")
        void al_crear_intenta_numerar_falla_sin_dependencia() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmdP = new CrearDocumentoPlantillaCommand(
                    "PLNT-8C3-018", "Al crear", null,
                    TipoDocu.ACTA_INFRACCION, AccionDocumental.GENERAR_ACTA_INFRACCION, TipoActa.TRANSITO,
                    TipoFirmaReq.FIRMA_INSPECTOR,
                    true, MomentoNumeracionDocu.AL_CREAR,
                    false, false, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla plantilla = plantillaService.crear(cmdP);
            plantilla.setSiActiva(true);

            assertThatThrownBy(() ->
                docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId())))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("dependencia");
        }

        @Test
        @DisplayName("19. No se registra movimiento de talonario al generar desde plantilla")
        void no_registra_movimiento_talonario() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-019");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getIdTalonario()).isNull();
            assertThat(doc.getNroTalonarioUsado()).isNull();
            assertThat(doc.getNroDocu()).isNull();
        }

        @Test
        @DisplayName("20. Plantilla numerada AL_EMITIR genera borrador sin nroDocu")
        void plantilla_numerada_al_emitir_genera_borrador_sin_nro() {
            FalActa acta = crearActa();
            CrearDocumentoPlantillaCommand cmdP = new CrearDocumentoPlantillaCommand(
                    "PLNT-8C3-020", "Al emitir", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_AUTORIDAD,
                    true, MomentoNumeracionDocu.AL_EMITIR,
                    false, true, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = plantillaService.crear(cmdP);
            AgregarFirmaReqPlantillaCommand req = new AgregarFirmaReqPlantillaCommand(
                    p.getId(), (short) 1, (short) 1, null, true, true, "sistema");
            plantillaService.agregarFirmaReq(req);
            FalDocumentoPlantilla plantilla = plantillaService.activar(p.getId());

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
            assertThat(doc.getNroDocu()).isNull();
            assertThat(doc.getIdTalonario()).isNull();
            assertThat(doc.getNroTalonarioUsado()).isNull();
        }
    }

    // =========================================================================
    // Firma fuera de alcance
    // =========================================================================

    @Nested
    @DisplayName("Firma fuera de alcance en 8C-3")
    class FirmaFueraDeAlcance {

        @Test
        @DisplayName("21. Genera documento con FIRMA_AUTORIDAD pero no crea firma_req")
        void genera_con_firma_autoridad_sin_firmar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNumeradaMomentoEnvio("PLNT-8C3-021");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getTipoFirmaReq()).isEqualTo(TipoFirmaReq.FIRMA_AUTORIDAD);
            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("22. Genera documento con FIRMA_MULTIPLE pero no crea firma_req")
        void genera_con_firma_multiple_sin_firmar() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaFirmaMultiple("PLNT-8C3-022");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getTipoFirmaReq()).isEqualTo(TipoFirmaReq.FIRMA_MULTIPLE);
            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }

        @Test
        @DisplayName("23. FalDocumento no tiene lista de FalDocumentoFirmaReq")
        void fal_documento_no_tiene_lista_firma_req() {
            for (Field f : FalDocumento.class.getDeclaredFields()) {
                assertThat(f.getType().getSimpleName()).isNotEqualTo("FalDocumentoFirmaReq");
                assertThat(f.getType().getSimpleName()).doesNotContain("List");
            }
        }

        @Test
        @DisplayName("24. Estado no pasa a PENDIENTE_FIRMA al generar desde plantilla")
        void estado_no_es_pendiente_firma() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaFirmaInspector("PLNT-8C3-024");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getEstadoDocu()).isNotEqualTo(EstadoDocu.PENDIENTE_FIRMA);
            assertThat(doc.getEstadoDocu()).isEqualTo(EstadoDocu.BORRADOR);
        }
    }

    // =========================================================================
    // Guardrails
    // =========================================================================

    @Nested
    @DisplayName("Guardrails del slice 8C-3")
    class Guardrails {

        @Test
        @DisplayName("25. Documento generado no tiene storageKey asignado (sin PDF/storage)")
        void documento_sin_storage_key() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-025");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            assertThat(doc.getStorageKey()).isNull();
        }

        @Test
        @DisplayName("26. FalDocumento no tiene campo rolFirmaReq")
        void fal_documento_no_tiene_rol_firma_req() {
            for (Field f : FalDocumento.class.getDeclaredFields()) {
                assertThat(f.getName()).isNotEqualTo("rolFirmaReq");
            }
        }

        @Test
        @DisplayName("27. FalDocumento no tiene campo mecanismoFirmaReq")
        void fal_documento_no_tiene_mecanismo_firma_req() {
            for (Field f : FalDocumento.class.getDeclaredFields()) {
                assertThat(f.getName()).isNotEqualTo("mecanismoFirmaReq");
            }
        }

        @Test
        @DisplayName("28. Documento generado queda persistido en docRepo recuperable por idActa")
        void documento_recuperable_por_id_acta() {
            FalActa acta = crearActa();
            FalDocumentoPlantilla plantilla = crearPlantillaActivaNoNumerada("PLNT-8C3-028");

            FalDocumento doc = docService.generarDesdePlantilla(cmd(acta.getId(), plantilla.getId()));

            List<FalDocumento> docs = docRepo.buscarPorActa(acta.getId());
            assertThat(docs).hasSize(1);
            assertThat(docs.get(0).getId()).isEqualTo(doc.getId());
        }
    }
}




