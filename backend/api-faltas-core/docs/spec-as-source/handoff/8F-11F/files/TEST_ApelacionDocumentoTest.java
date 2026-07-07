package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Slice 8F-11F: Documentos de apelacion (fal_acta_apelacion_documento)")
class ApelacionDocumentoTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryDocumentoFirmaRepository firmaRepo;
    private InMemoryNotificacionRepository notifRepo;
    private InMemoryPagoVoluntarioRepository pagoRepo;
    private InMemoryFalloActaRepository falloRepo;
    private InMemoryApelacionActaRepository apelacionRepo;
    private InMemoryApelacionDocumentoRepository apelDocRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private FalloActaService falloService;
    private ApelacionActaService apelacionService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();
        apelDocRepo = new InMemoryApelacionDocumentoRepository();

        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), new InMemoryPersonaRepository());
        docService = new DocumentoService(actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,
                new InMemoryDocumentoPlantillaRepository(),
                new TalonarioService(new InMemoryTalonarioRepository(), new InMemoryDependenciaRepository(), new InMemoryInspectorRepository()),
                new InMemoryDependenciaRepository(), new InMemoryDocumentoFirmaReqRepository(), new InMemoryFirmanteRepository());
        notifService = new NotificacionService(actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker());
        falloService = new FalloActaService(actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoRepo, recalc);
        apelacionService = new ApelacionActaService(actaRepo, falloRepo, apelacionRepo, apelDocRepo, eventoRepo, snapshotRepo,
                recalc, new NoOpBloqueantesMaterialesChecker());
    }

    private Long crearApelacionPresentada() {
        Long actaId = llegarAAnalisis("9200000");
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(actaId, new BigDecimal("1000"), "Cargos", null));
        Long idDocFallo = falloRepo.findVigenteByActaId(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Insp", "DIGITAL", null));
        String notifId = notifService.enviarNotificacion(new EnviarNotificacionCommand(actaId, idDocFallo, "EMAIL", null)).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(notifId, null));
        apelacionService.registrarApelacion(RegistrarApelacionCommand.legacy(actaId, "Infractor", "Fundamentos", null));
        return apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
    }

    private Long llegarAAnalisis(String nro) {
        Long actaId = actaService.labrar(new LabrarActaCommand(TipoActa.TRANSITO, 1L, 1L, LocalDate.now(),
                "Dir", null, null, null, "Inf", nro, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriched"));
        String docId = docService.generarDocumento(new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION, "Acta")).idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(docId), "Insp", "DIGITAL", null));
        String notifId = notifService.enviarNotificacion(new EnviarNotificacionCommand(actaId, Long.parseLong(docId), "EMAIL", null)).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(notifId, null));
        return actaId;
    }

    @Nested @DisplayName("A: Guardar y consultar documentos")
    class GuardarConsultar {

        @Test @DisplayName("registrar documento con storageKey: guardado y recuperado por apelacionId")
        void guardar_por_storageKey() {
            Long apelacionId = crearApelacionPresentada();
            ComandoResultado r = apelacionService.registrarDocumento(
                    new RegistrarDocumentoApelacionCommand(apelacionId,
                            TipoDocumentoApelacion.ESCRITO_APELACION, OrigenPresentacion.INFRACTOR,
                            null, "files/ape-2026-001.pdf", "escrito-apelacion.pdf", (short) 1, 102400L, "USR-1"));
            assertThat(r.tipoEvento()).isEqualTo("DOCAMP");
            List<FalActaApelacionDocumento> docs = apelacionService.listarDocumentosApelacion(apelacionId);
            assertThat(docs).hasSize(1);
            assertThat(docs.get(0).getStorageKey()).isEqualTo("files/ape-2026-001.pdf");
            assertThat(docs.get(0).getApelacionId()).isEqualTo(apelacionId);
        }

        @Test @DisplayName("multiples documentos en misma apelacion: append historico")
        void append_historico() {
            Long apelacionId = crearApelacionPresentada();
            apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(apelacionId,
                    TipoDocumentoApelacion.ESCRITO_APELACION, OrigenPresentacion.INFRACTOR,
                    null, "key1", "doc1.pdf", (short) 1, 1000L, "USR-1"));
            apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(apelacionId,
                    TipoDocumentoApelacion.EVIDENCIA_INFRACTOR, OrigenPresentacion.INFRACTOR,
                    null, "key2", "foto.jpg", (short) 2, 2000L, "USR-1"));
            List<FalActaApelacionDocumento> docs = apelacionService.listarDocumentosApelacion(apelacionId);
            assertThat(docs).hasSize(2);
        }

        @Test @DisplayName("documento tiene fhAlta y idUserAlta correctos")
        void auditoria_correcta() {
            Long apelacionId = crearApelacionPresentada();
            apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(apelacionId,
                    TipoDocumentoApelacion.OTRO, OrigenPresentacion.OPERADOR_INTERNO,
                    null, "keyX", "prueba.txt", (short) 1, 500L, "AUDITOR"));
            FalActaApelacionDocumento doc = apelacionService.listarDocumentosApelacion(apelacionId).get(0);
            assertThat(doc.getFhAlta()).isNotNull();
            assertThat(doc.getIdUserAlta()).isEqualTo("AUDITOR");
        }
    }

    @Nested @DisplayName("B: Validaciones de repositorio InMemory")
    class RepositorioValidaciones {

        @Test @DisplayName("findById retorna copia defensiva")
        void findById_copia_defensiva() {
            Long apelacionId = crearApelacionPresentada();
            apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(apelacionId,
                    TipoDocumentoApelacion.ESCRITO_APELACION, OrigenPresentacion.INFRACTOR,
                    null, "keyZ", "escrito.pdf", (short) 1, 100L, "USR"));
            FalActaApelacionDocumento d1 = apelDocRepo.findByApelacionId(apelacionId).get(0);
            FalActaApelacionDocumento d2 = apelDocRepo.findByApelacionId(apelacionId).get(0);
            assertThat(d1).isNotSameAs(d2);
            assertThat(d1.getId()).isEqualTo(d2.getId());
        }

        @Test @DisplayName("apelaciones distintas no se interfieren")
        void apelaciones_distintas() {
            Long a1 = crearApelacionPresentada();
            apelacionService.registrarDocumento(new RegistrarDocumentoApelacionCommand(a1,
                    TipoDocumentoApelacion.ESCRITO_APELACION, OrigenPresentacion.INFRACTOR,
                    null, "k1", "d1.pdf", (short) 1, 100L, "U"));
            assertThat(apelacionService.listarDocumentosApelacion(999L)).isEmpty();
        }
    }

    @Nested @DisplayName("C: TipoDocumentoApelacion y OrigenPresentacion - codigos")
    class Enums {
        @Test @DisplayName("TipoDocumentoApelacion tiene 8 valores con codigos 1-8")
        void tipo_doc_8_valores() {
            assertThat(TipoDocumentoApelacion.values()).hasSize(8);
            assertThat(TipoDocumentoApelacion.fromCodigo((short) 1)).isEqualTo(TipoDocumentoApelacion.ESCRITO_APELACION);
            assertThat(TipoDocumentoApelacion.fromCodigo((short) 8)).isEqualTo(TipoDocumentoApelacion.OTRO);
        }
        @Test @DisplayName("OrigenPresentacion tiene 6 valores con codigos 1-6")
        void origen_6_valores() {
            assertThat(OrigenPresentacion.values()).hasSize(6);
            assertThat(OrigenPresentacion.fromCodigo((short) 1)).isEqualTo(OrigenPresentacion.INFRACTOR);
            assertThat(OrigenPresentacion.fromCodigo((short) 6)).isEqualTo(OrigenPresentacion.INTEGRACION_EXTERNA);
        }
    }
}
