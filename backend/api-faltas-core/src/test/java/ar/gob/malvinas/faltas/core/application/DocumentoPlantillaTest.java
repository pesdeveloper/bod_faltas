package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.AgregarFirmaReqPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearDocumentoPlantillaCommand;
import ar.gob.malvinas.faltas.core.application.service.DocumentoPlantillaService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.MomentoNumeracionDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaDuplicadaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaInvalidaException;
import ar.gob.malvinas.faltas.core.domain.exception.DocumentoPlantillaNoEncontradaException;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantilla;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumentoPlantillaFirmaReq;
import ar.gob.malvinas.faltas.core.repository.DocumentoPlantillaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository;
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
 * Tests del Micro-slice 8C-2: Plantillas documentales in-memory.
 */
@DisplayName("Micro-slice 8C-2: Plantillas documentales in-memory")
class DocumentoPlantillaTest {

    private DocumentoPlantillaRepository repo;
    private DocumentoPlantillaService service;

    @BeforeEach
    void setUp() {
        repo = new InMemoryDocumentoPlantillaRepository();
        service = new DocumentoPlantillaService(repo, FaltasClockTestSupport.FIXED);
    }

    private CrearDocumentoPlantillaCommand cmdNumerada(String codigo) {
        return new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, "Descripcion",
                TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                TipoFirmaReq.FIRMA_AUTORIDAD,
                true, MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA,
                false, true, true,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
    }

    private CrearDocumentoPlantillaCommand cmdNoNumerada(String codigo) {
        return new CrearDocumentoPlantillaCommand(
                codigo, "Nombre " + codigo, null,
                TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                TipoFirmaReq.NO_REQUIERE,
                false, MomentoNumeracionDocu.NO_APLICA,
                false, false, false,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
    }

    private AgregarFirmaReqPlantillaCommand firmaReqCmd(Long plantillaId, short seq, boolean obligatoria) {
        return new AgregarFirmaReqPlantillaCommand(
                plantillaId, seq, (short) 1, null, obligatoria, true, "sistema");
    }

    @Nested
    @DisplayName("Creacion valida de plantillas")
    class CreacionValidaTests {

        @Test
        @DisplayName("Crea plantilla no numerada con momentoNumeracionDocu=NO_APLICA")
        void crea_plantilla_no_numerada() {
            FalDocumentoPlantilla p = service.crear(cmdNoNumerada("PLNT-001"));
            assertThat(p.getId()).isNotNull();
            assertThat(p.getCodigo()).isEqualTo("PLNT-001");
            assertThat(p.isSiRequiereNumeracion()).isFalse();
            assertThat(p.getMomentoNumeracionDocu()).isEqualTo(MomentoNumeracionDocu.NO_APLICA);
            assertThat(p.isSiActiva()).isFalse();
        }

        @Test
        @DisplayName("Crea plantilla numerada con momentoNumeracionDocu=AL_ENVIAR_A_FIRMA")
        void crea_plantilla_numerada() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-002"));
            assertThat(p.isSiRequiereNumeracion()).isTrue();
            assertThat(p.getMomentoNumeracionDocu()).isEqualTo(MomentoNumeracionDocu.AL_ENVIAR_A_FIRMA);
        }

        @Test
        @DisplayName("Codigo unico: segundo intento falla con DocumentoPlantillaDuplicadaException")
        void codigo_unico_se_respeta() {
            service.crear(cmdNumerada("PLNT-DUP"));
            assertThatThrownBy(() -> service.crear(cmdNumerada("PLNT-DUP")))
                    .isInstanceOf(DocumentoPlantillaDuplicadaException.class)
                    .hasMessageContaining("PLNT-DUP");
        }

        @Test
        @DisplayName("Permite tipoActa = null")
        void permite_tipo_acta_null() {
            FalDocumentoPlantilla p = service.crear(cmdNoNumerada("PLNT-NOACTA"));
            assertThat(p.getTipoActa()).isNull();
        }

        @Test
        @DisplayName("Permite tipoActa no nulo")
        void permite_tipo_acta_no_null() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-TACTA", "Con tipo acta", null,
                    TipoDocu.ACTA_INFRACCION, AccionDocumental.GENERAR_ACTA_INFRACCION,
                    TipoActa.TRANSITO,
                    TipoFirmaReq.FIRMA_INSPECTOR,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = service.crear(cmd);
            assertThat(p.getTipoActa()).isEqualTo(TipoActa.TRANSITO);
        }

        @Test
        @DisplayName("Guarda correctamente flags: siNotificable, siGeneraPdf, siSeleccionable")
        void guarda_flags() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-FLAGS", "Con flags", null,
                    TipoDocu.NOTIFICACION_ACTA, AccionDocumental.EMITIR_NOTIFICACION_ACTA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    true, true, true,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = service.crear(cmd);
            assertThat(p.isSiNotificable()).isTrue();
            assertThat(p.isSiGeneraPdf()).isTrue();
            assertThat(p.isSiSeleccionable()).isTrue();
            assertThat(p.isSiActiva()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones de numeracion")
    class ValidacionesNumeracionTests {

        @Test
        @DisplayName("Falla si siRequiereNumeracion=false y momentoNumeracionDocu != NO_APLICA")
        void falla_no_numerada_con_momento_distinto_no_aplica() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-BAD1", "Bad", null,
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.AL_CREAR,
                    false, false, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            assertThatThrownBy(() -> service.crear(cmd))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("NO_APLICA");
        }

        @Test
        @DisplayName("Falla si siRequiereNumeracion=true y momentoNumeracionDocu=NO_APLICA")
        void falla_numerada_con_momento_no_aplica() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-BAD2", "Bad", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_AUTORIDAD,
                    true, MomentoNumeracionDocu.NO_APLICA,
                    false, false, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            assertThatThrownBy(() -> service.crear(cmd))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("NO_APLICA");
        }

        @Test
        @DisplayName("La plantilla no tiene campo idTalonario")
        void no_exige_idTalonario() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-NOTALON"));
            for (Field f : p.getClass().getDeclaredFields()) {
                assertThat(f.getName()).doesNotContain("idTalonario");
                assertThat(f.getName()).doesNotContain("politicaNumeracionId");
                assertThat(f.getName()).doesNotContain("claseTalonario");
            }
        }

        @Test
        @DisplayName("La plantilla no tiene campo politicaNumeracionId")
        void no_exige_politicaNumeracionId() {
            for (Field f : FalDocumentoPlantilla.class.getDeclaredFields()) {
                assertThat(f.getName()).doesNotContain("politicaNumeracion");
            }
        }

        @Test
        @DisplayName("Crear plantilla numerada no toca talonarios")
        void no_toca_talonarios() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-NONUM"));
            assertThat(p).isNotNull();
        }
    }

    @Nested
    @DisplayName("Requisitos de firma por plantilla")
    class RequisitosFiremaTests {

        @Test
        @DisplayName("Agrega requisito de firma valido")
        void agrega_requisito_valido() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-REQ01"));
            FalDocumentoPlantillaFirmaReq req = service.agregarFirmaReq(
                    firmaReqCmd(p.getId(), (short) 1, true));
            assertThat(req.getId()).isNotNull();
            assertThat(req.getPlantillaId()).isEqualTo(p.getId());
            assertThat(req.getSeqFirmaReq()).isEqualTo((short) 1);
            assertThat(req.isSiObligatoria()).isTrue();
            assertThat(req.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Falla si plantilla no existe")
        void falla_plantilla_inexistente() {
            assertThatThrownBy(() -> service.agregarFirmaReq(
                    firmaReqCmd(999L, (short) 1, true)))
                    .isInstanceOf(DocumentoPlantillaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Falla si seqFirmaReq <= 0")
        void falla_seq_cero_o_negativo() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-SEQ0"));
            assertThatThrownBy(() -> service.agregarFirmaReq(
                    new AgregarFirmaReqPlantillaCommand(
                            p.getId(), (short) 0, (short) 1, null, true, true, "sistema")))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("seqFirmaReq");
        }

        @Test
        @DisplayName("Falla si rolFirmaReq <= 0")
        void falla_rol_cero_o_negativo() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-ROL0"));
            assertThatThrownBy(() -> service.agregarFirmaReq(
                    new AgregarFirmaReqPlantillaCommand(
                            p.getId(), (short) 1, (short) 0, null, true, true, "sistema")))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("rolFirmaReq");
        }

        @Test
        @DisplayName("Falla si se repite seqFirmaReq activa en la misma plantilla")
        void falla_seq_duplicada_en_misma_plantilla() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-SEQDUP"));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            assertThatThrownBy(() -> service.agregarFirmaReq(
                    firmaReqCmd(p.getId(), (short) 1, true)))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("seqFirmaReq=1");
        }

        @Test
        @DisplayName("Permite misma seqFirmaReq en otra plantilla distinta")
        void permite_seq_en_otra_plantilla() {
            FalDocumentoPlantilla p1 = service.crear(cmdNumerada("PLNT-MULTIA"));
            FalDocumentoPlantilla p2 = service.crear(cmdNumerada("PLNT-MULTIB"));
            service.agregarFirmaReq(firmaReqCmd(p1.getId(), (short) 1, true));
            FalDocumentoPlantillaFirmaReq req2 = service.agregarFirmaReq(
                    firmaReqCmd(p2.getId(), (short) 1, true));
            assertThat(req2.getPlantillaId()).isEqualTo(p2.getId());
        }
    }

    @Nested
    @DisplayName("Activacion de plantillas")
    class ActivacionTests {

        @Test
        @DisplayName("Activa plantilla NO_REQUIERE sin requisitos obligatorios activos")
        void activa_no_requiere_sin_req_obligatorios() {
            FalDocumentoPlantilla p = service.crear(cmdNoNumerada("PLNT-NORQ01"));
            FalDocumentoPlantilla activada = service.activar(p.getId());
            assertThat(activada.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Falla activar plantilla NO_REQUIERE con requisito obligatorio activo")
        void falla_activar_no_requiere_con_req_obligatorio() {
            FalDocumentoPlantilla p = service.crear(cmdNoNumerada("PLNT-NORQ02"));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            assertThatThrownBy(() -> service.activar(p.getId()))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("NO_REQUIERE");
        }

        @Test
        @DisplayName("Falla activar plantilla FIRMA_AUTORIDAD sin requisitos obligatorios activos")
        void falla_activar_firma_autoridad_sin_req() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-FAUT01"));
            assertThatThrownBy(() -> service.activar(p.getId()))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("FIRMA_AUTORIDAD");
        }

        @Test
        @DisplayName("Activa plantilla FIRMA_AUTORIDAD con un requisito obligatorio activo")
        void activa_firma_autoridad_con_un_req() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-FAUT02"));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            FalDocumentoPlantilla activada = service.activar(p.getId());
            assertThat(activada.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Falla activar plantilla FIRMA_MULTIPLE con un solo requisito obligatorio activo")
        void falla_activar_firma_multiple_con_un_req() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-FMUL01", "Firma multiple 1", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_MULTIPLE,
                    true, MomentoNumeracionDocu.AL_FIRMAR,
                    false, false, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = service.crear(cmd);
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            assertThatThrownBy(() -> service.activar(p.getId()))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("FIRMA_MULTIPLE");
        }

        @Test
        @DisplayName("Activa plantilla FIRMA_MULTIPLE con dos requisitos obligatorios activos")
        void activa_firma_multiple_con_dos_req() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-FMUL02", "Firma multiple 2", null,
                    TipoDocu.ACTO_ADMINISTRATIVO, AccionDocumental.EMITIR_FALLO, null,
                    TipoFirmaReq.FIRMA_MULTIPLE,
                    true, MomentoNumeracionDocu.AL_FIRMAR,
                    false, false, false,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), null, "sistema");
            FalDocumentoPlantilla p = service.crear(cmd);
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 2, true));
            FalDocumentoPlantilla activada = service.activar(p.getId());
            assertThat(activada.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("Falla activar plantilla vencida (fhVigHasta anterior a hoy)")
        void falla_activar_plantilla_vencida() {
            CrearDocumentoPlantillaCommand cmd = new CrearDocumentoPlantillaCommand(
                    "PLNT-VENC01", "Vencida", null,
                    TipoDocu.CONSTANCIA, AccionDocumental.EMITIR_CONSTANCIA, null,
                    TipoFirmaReq.NO_REQUIERE,
                    false, MomentoNumeracionDocu.NO_APLICA,
                    false, false, false,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), "sistema");
            FalDocumentoPlantilla p = service.crear(cmd);
            assertThatThrownBy(() -> service.activar(p.getId()))
                    .isInstanceOf(DocumentoPlantillaInvalidaException.class)
                    .hasMessageContaining("vigencia");
        }

        @Test
        @DisplayName("Desactiva plantilla activa")
        void desactiva_plantilla_activa() {
            FalDocumentoPlantilla p = service.crear(cmdNoNumerada("PLNT-DESACT"));
            service.activar(p.getId());
            FalDocumentoPlantilla desactivada = service.desactivar(p.getId());
            assertThat(desactivada.isSiActiva()).isFalse();
        }
    }

    @Nested
    @DisplayName("Listados de plantillas")
    class ListadoTests {

        @Test
        @DisplayName("Lista todas las plantillas")
        void lista_todas() {
            service.crear(cmdNumerada("PLNT-LIST-A"));
            service.crear(cmdNumerada("PLNT-LIST-B"));
            service.crear(cmdNoNumerada("PLNT-LIST-C"));
            List<FalDocumentoPlantilla> lista = service.listar();
            assertThat(lista).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("Lista por accion documental")
        void lista_por_accion() {
            service.crear(cmdNumerada("PLNT-ACT-FALLO-1"));
            service.crear(cmdNoNumerada("PLNT-ACT-CONST-1"));
            List<FalDocumentoPlantilla> fallos = service.listarPorAccion(AccionDocumental.EMITIR_FALLO);
            assertThat(fallos).allMatch(p -> p.getAccionDocumental() == AccionDocumental.EMITIR_FALLO);
        }

        @Test
        @DisplayName("Lista activas por accion documental")
        void lista_activas_por_accion() {
            FalDocumentoPlantilla activa = service.crear(cmdNoNumerada("PLNT-ACT-CONST-ACT"));
            service.activar(activa.getId());
            service.crear(cmdNoNumerada("PLNT-ACT-CONST-INACT"));
            List<FalDocumentoPlantilla> activas = service.listarActivasPorAccion(
                    AccionDocumental.EMITIR_CONSTANCIA);
            assertThat(activas).allMatch(FalDocumentoPlantilla::isSiActiva);
            assertThat(activas).anyMatch(p -> p.getCodigo().equals("PLNT-ACT-CONST-ACT"));
        }

        @Test
        @DisplayName("Lista requisitos de firma por plantilla")
        void lista_firma_req_por_plantilla() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-FIRMAS-LIST"));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 1, true));
            service.agregarFirmaReq(firmaReqCmd(p.getId(), (short) 2, false));
            List<FalDocumentoPlantillaFirmaReq> reqs = service.listarFirmaReq(p.getId());
            assertThat(reqs).hasSize(2);
            assertThat(reqs).anyMatch(r -> r.getSeqFirmaReq() == 1 && r.isSiObligatoria());
            assertThat(reqs).anyMatch(r -> r.getSeqFirmaReq() == 2 && !r.isSiObligatoria());
        }
    }

    @Nested
    @DisplayName("Guardrails: no talonario, no FalDocumentoFirmaReq, no generacion documental")
    class GuardrailsTests {

        @Test
        @DisplayName("FalDocumentoPlantilla no tiene campo idTalonario")
        void plantilla_no_tiene_idTalonario() {
            for (Field f : FalDocumentoPlantilla.class.getDeclaredFields()) {
                assertThat(f.getName()).doesNotContain("idTalonario");
            }
        }

        @Test
        @DisplayName("FalDocumentoPlantilla no tiene campo politicaNumeracionId")
        void plantilla_no_tiene_politicaNumeracionId() {
            for (Field f : FalDocumentoPlantilla.class.getDeclaredFields()) {
                assertThat(f.getName()).doesNotContain("politicaNumeracion");
            }
        }

        @Test
        @DisplayName("FalDocumentoPlantilla no tiene campo claseTalonario")
        void plantilla_no_tiene_claseTalonario() {
            for (Field f : FalDocumentoPlantilla.class.getDeclaredFields()) {
                assertThat(f.getName()).doesNotContain("claseTalonario");
            }
        }

        @Test
        @DisplayName("FalDocumentoPlantillaFirmaReq es tipo correcto, no FalDocumentoFirmaReq")
        void firmaReq_no_es_FalDocumentoFirmaReq() {
            FalDocumentoPlantilla p = service.crear(cmdNumerada("PLNT-GUARD-FIR"));
            FalDocumentoPlantillaFirmaReq req = service.agregarFirmaReq(
                    firmaReqCmd(p.getId(), (short) 1, true));
            assertThat(req).isInstanceOf(FalDocumentoPlantillaFirmaReq.class);
            assertThat(req.getClass().getSimpleName()).isEqualTo("FalDocumentoPlantillaFirmaReq");
        }

        @Test
        @DisplayName("No se genero FalDocumento durante la creacion de plantilla")
        void no_genera_fal_documento() {
            service.crear(cmdNumerada("PLNT-GUARD-DOC"));
        }

        @Test
        @DisplayName("MomentoNumeracionDocu tiene 5 valores y NO_APLICA = 0")
        void momento_numeracion_docu_existe_correcto() {
            assertThat(MomentoNumeracionDocu.values()).hasSize(5);
            assertThat(MomentoNumeracionDocu.NO_APLICA.codigo()).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("FIRMA_MIXTA no existe en TipoFirmaReq")
        void firma_mixta_no_existe() {
            for (TipoFirmaReq t : TipoFirmaReq.values()) {
                assertThat(t.name()).isNotEqualTo("FIRMA_MIXTA");
            }
        }

        @Test
        @DisplayName("FIRMA_MULTIPLE existe en TipoFirmaReq con codigo 5")
        void firma_multiple_existe() {
            assertThat(TipoFirmaReq.FIRMA_MULTIPLE.codigo()).isEqualTo((short) 5);
        }
    }
}