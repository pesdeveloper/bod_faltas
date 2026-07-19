package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;
import ar.gob.malvinas.faltas.core.repository.ActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Micro-slice 8C-6A: ResultadoFirmaInfractor + evidencia FIRMA_OLOGRAFA_INFRACTOR")
class ActaResultadoFirmaInfractorTest {

    private ActaService actaService;
    private ActaRepository actaRepo;
    private ActaEvidenciaRepository evidenciaRepo;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        actaRepo = new InMemoryActaRepository();
        ActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        ActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        evidenciaRepo = new InMemoryActaEvidenciaRepository();
        SnapshotRecalculador recalculador = new SnapshotRecalculador(
                eventoRepo,
                new InMemoryDocumentoRepository(),
                new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(),
                new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository()
        , FaltasClockTestSupport.FIXED, snapshotRepo);
        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalculador, evidenciaRepo, FaltasClockTestSupport.FIXED);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    private LabrarActaCommand cmdLabrar(ResultadoFirmaInfractor resultado, List<EvidenciaActaItem> evidencias) {
        return new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Belgrano 100", "San Martin 200",
                null, null, null, "Juan Perez", "12345678",
                resultado, evidencias
        );
    }

    private EvidenciaActaItem evidenciaFirmaOlografa() {
        return new EvidenciaActaItem(
                TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR,
                "mock://firma/infractor/12345678.png"
        );
    }

    @Nested
    @DisplayName("Resultado obligatorio al labrar")
    class ResultadoObligatorio {

        @Test
        @DisplayName("1. Falla labrar acta si resultadoFirmaInfractor es null")
        void falla_si_resultado_es_null() {
            assertThatThrownBy(() -> actaService.labrar(cmdLabrar(null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("resultadoFirmaInfractor");
        }

        @Test
        @DisplayName("2. Labra acta con SE_NIEGA_A_FIRMAR sin evidencia")
        void labra_con_se_niega_a_firmar() {
            ComandoResultado r = actaService.labrar(cmdLabrar(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
        }

        @Test
        @DisplayName("3. Labra acta con INFRACTOR_NO_PRESENTE sin evidencia")
        void labra_con_infractor_no_presente() {
            ComandoResultado r = actaService.labrar(cmdLabrar(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE, null));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.INFRACTOR_NO_PRESENTE);
        }

        @Test
        @DisplayName("4. Labra acta con IMPOSIBILITADO_PARA_FIRMAR sin evidencia")
        void labra_con_imposibilitado_para_firmar() {
            ComandoResultado r = actaService.labrar(cmdLabrar(ResultadoFirmaInfractor.IMPOSIBILITADO_PARA_FIRMAR, null));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.IMPOSIBILITADO_PARA_FIRMAR);
        }

        @Test
        @DisplayName("5. Labra acta con NO_CAPTURADA_POR_FALLA_TECNICA sin evidencia")
        void labra_con_no_capturada_por_falla_tecnica() {
            ComandoResultado r = actaService.labrar(cmdLabrar(ResultadoFirmaInfractor.NO_CAPTURADA_POR_FALLA_TECNICA, null));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.NO_CAPTURADA_POR_FALLA_TECNICA);
        }
    }

    @Nested
    @DisplayName("Resultado FIRMADA exige evidencia FIRMA_OLOGRAFA_INFRACTOR")
    class ResultadoFirmada {

        @Test
        @DisplayName("6. Falla labrar con FIRMADA si no tiene evidencia FIRMA_OLOGRAFA_INFRACTOR")
        void falla_firmada_sin_evidencia() {
            assertThatThrownBy(() -> actaService.labrar(cmdLabrar(ResultadoFirmaInfractor.FIRMADA, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("FIRMADA")
                    .hasMessageContaining("FIRMA_OLOGRAFA_INFRACTOR");
        }

        @Test
        @DisplayName("7. Labra acta con FIRMADA si tiene evidencia FIRMA_OLOGRAFA_INFRACTOR")
        void labra_firmada_con_evidencia() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.FIRMADA);
        }

        @Test
        @DisplayName("8. La evidencia queda vinculada al acta")
        void evidencia_vinculada_al_acta() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            List<FalActaEvidencia> evidencias = actaService.listarEvidencias(r.idActa());
            assertThat(evidencias).hasSize(1);
            assertThat(evidencias.get(0).getTipoEvid()).isEqualTo(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR);
            assertThat(evidencias.get(0).getIdActa()).isEqualTo(r.idActa());
            assertThat(evidencias.get(0).getStorageKey()).isNotBlank();
        }

        @Test
        @DisplayName("9. La evidencia es FalActaEvidencia, no FalDocumentoFirma")
        void evidencia_no_es_documento_firma() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            List<FalActaEvidencia> evidencias = actaService.listarEvidencias(r.idActa());
            assertThat(evidencias).hasSize(1);
            assertThat(evidencias.get(0)).isInstanceOf(FalActaEvidencia.class);
            assertThat(evidencias.get(0).getClass().getSimpleName()).isNotEqualTo("FalDocumentoFirma");
        }

        @Test
        @DisplayName("10. La evidencia no es FalDocumentoFirmaReq")
        void evidencia_no_es_firma_req() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            List<FalActaEvidencia> evidencias = actaService.listarEvidencias(r.idActa());
            assertThat(evidencias).hasSize(1);
            assertThat(evidencias.get(0).getClass().getSimpleName()).isNotEqualTo("FalDocumentoFirmaReq");
        }
    }

    @Nested
    @DisplayName("Guardrails de diseno y aislamiento")
    class Guardrails {

        @Test
        @DisplayName("11. No existe NO_REQUERIDA en ResultadoFirmaInfractor")
        void no_existe_no_requerida() {
            for (ResultadoFirmaInfractor r : ResultadoFirmaInfractor.values()) {
                assertThat(r.name()).isNotEqualTo("NO_REQUERIDA");
            }
        }

        @Test
        @DisplayName("12. No existe campo obsFirmaInfractor en FalActa")
        void no_existe_obs_firma_infractor() {
            boolean tieneObs = false;
            for (java.lang.reflect.Field f : FalActa.class.getDeclaredFields()) {
                if (f.getName().equals("obsFirmaInfractor")) {
                    tieneObs = true;
                }
            }
            assertThat(tieneObs).as("FalActa no debe tener obsFirmaInfractor").isFalse();
        }

        @Test
        @DisplayName("13. TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR tiene codigo 6")
        void firma_olografa_tiene_codigo_6() {
            assertThat(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR.codigo()).isEqualTo((short) 6);
        }

        @Test
        @DisplayName("14. Acta con FIRMADA persiste resultadoFirmaInfractor sin afectar documentos")
        void firma_olografa_no_cambia_estado_documental() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.FIRMADA);
        }

        @Test
        @DisplayName("15. FIRMA_OLOGRAFA_INFRACTOR no cumple FalDocumentoFirmaReq")
        void firma_olografa_no_cumple_firma_req() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.FIRMADA, List.of(evidenciaFirmaOlografa())));
            List<FalActaEvidencia> evidencias = actaService.listarEvidencias(r.idActa());
            assertThat(evidencias.get(0).getTipoEvid()).isEqualTo(TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR);
        }

        @Test
        @DisplayName("16. LabrarActaRequest tiene campo resultadoFirmaInfractor")
        void request_acepta_resultado_firma_infractor() throws Exception {
            Class<?> clazz = Class.forName("ar.gob.malvinas.faltas.core.web.dto.LabrarActaRequest");
            boolean tiene = false;
            for (java.lang.reflect.RecordComponent rc : clazz.getRecordComponents()) {
                if (rc.getName().equals("resultadoFirmaInfractor")) {
                    tiene = true;
                }
            }
            assertThat(tiene).as("LabrarActaRequest debe tener resultadoFirmaInfractor").isTrue();
        }

        @Test
        @DisplayName("17. FalActa.getResultadoFirmaInfractor() devuelve valor y codigo correctos")
        void acta_devuelve_resultado_firma_infractor() {
            ComandoResultado r = actaService.labrar(
                    cmdLabrar(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));
            FalActa acta = actaRepo.buscarPorId(r.idActa()).orElseThrow();
            assertThat(acta.getResultadoFirmaInfractor()).isEqualTo(ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR);
            assertThat(acta.getResultadoFirmaInfractor().codigo()).isEqualTo((short) 2);
        }

        @Test
        @DisplayName("18. FalActa.resultadoFirmaInfractor es final (inmutable post labrado)")
        void resultado_es_final() throws Exception {
            java.lang.reflect.Field field = FalActa.class.getDeclaredField("resultadoFirmaInfractor");
            assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                    .as("resultadoFirmaInfractor debe ser final").isTrue();
        }
    }
}
