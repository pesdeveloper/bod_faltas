package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.*;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Slice 8F-11F: Efectos de resolucion de apelacion (4 tipos)")
class ResolucionApelacionEfectosTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoRepo;
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

    private Long crearActaConApelacionPresentada() {
        Long actaId = llegarAAnalisis("9100000");
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(actaId, new BigDecimal("5000"), "Cargos", null));
        Long idDocFallo = falloRepo.findVigenteByActaId(actaId).orElseThrow().getDocumentoId();
        // document auto-created by dictarCondenatorio
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Inspector", "DIGITAL", null));
        String notifId = notifService.enviarNotificacion(new EnviarNotificacionCommand(actaId, idDocFallo, "EMAIL", null)).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(notifId, null));
        apelacionService.registrarApelacion(RegistrarApelacionCommand.legacy(actaId, "Infractor", "Apelo el fallo", null));
        return actaId;
    }

    private Long llegarAAnalisis(String nroActa) {
        Long actaId = actaService.labrar(new LabrarActaCommand(TipoActa.TRANSITO, 1L, 1L, LocalDate.now(),
                "Dir", null, null, null, "Inf", nroActa, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriched"));
        String docId = docService.generarDocumento(new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION, "Acta")).idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(docId), "Insp", "DIGITAL", null));
        String notifId = notifService.enviarNotificacion(new EnviarNotificacionCommand(actaId, Long.parseLong(docId), "EMAIL", null)).idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(notifId, null));
        return actaId;
    }

    // =========================================================================
    // RECHAZADA
    // =========================================================================
    @Nested @DisplayName("RECHAZADA: mantiene efecto del fallo")
    class Rechazada {
        @Test @DisplayName("rechazada: estado RESUELTA+RECHAZADA, no cierra, snapshot DECLARAR_CONDENA_FIRME")
        void rechazada_efectos() {
            Long actaId = crearActaConApelacionPresentada();
            ComandoResultado r = apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Sin meritos", null));
            assertThat(r.tipoEvento()).isEqualTo("APERAZ");
            FalActaApelacion apel = apelacionRepo.buscarUltima(actaId).orElseThrow();
            assertThat(apel.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.RESUELTA);
            assertThat(apel.getResultadoResolucion()).isEqualTo(ResultadoResolucionApelacion.RECHAZADA);
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DECLARAR_CONDENA_FIRME);
        }
        @Test @DisplayName("rechazada: fallo original sigue vigente")
        void rechazada_fallo_sigue_vigente() {
            Long actaId = crearActaConApelacionPresentada();
            FalActaFallo falloAntes = falloRepo.findVigenteByActaId(actaId).orElseThrow();
            apelacionService.resolverRechazada(new ResolverApelacionRechazadaCommand(actaId, null, null));
            FalActaFallo falloDespues = falloRepo.findVigenteByActaId(actaId).orElseThrow();
            assertThat(falloDespues.getId()).isEqualTo(falloAntes.getId());
            assertThat(falloDespues.isSiVigente()).isTrue();
        }
    }

    // =========================================================================
    // ACEPTADA_ABSUELVE
    // =========================================================================
    @Nested @DisplayName("ACEPTADA_ABSUELVE: cierra con ABSUELTO")
    class AceptaAbsuelve {
        @Test @DisplayName("aceptada absuelve: ABSUELTO, CIERRA, snapshot CERRADAS")
        void aceptada_absuelve_efectos() {
            Long actaId = crearActaConApelacionPresentada();
            ComandoResultado r = apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Absolucion", null));
            assertThat(r.tipoEvento()).isEqualTo("APEABS");
            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
        }
        @Test @DisplayName("aceptada absuelve: APEABS y CIERRA en eventos")
        void eventos_apeabs_cierra() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverAceptaAbsuelve(new ResolverApelacionAceptaAbsuelveCommand(actaId, null, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APEABS, TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // MODIFICA_CONDENA
    // =========================================================================
    @Nested @DisplayName("MODIFICA_CONDENA: crea fallo sustitutivo")
    class ModificaCondena {
        @Test @DisplayName("modifica: nuevo fallo vigente con nuevo monto, fallo anterior reemplazado")
        void modifica_condena_efectos() {
            Long actaId = crearActaConApelacionPresentada();
            FalActaFallo falloOriginal = falloRepo.findVigenteByActaId(actaId).orElseThrow();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();

            ComandoResultado r = apelacionService.resolverModificaCondena(
                    new ResolverApelacionModificaCondenaCommand(apelacionId, new BigDecimal("2000"), "Reduccion", "USR-J"));
            assertThat(r.tipoEvento()).isEqualTo("APEMCO");

            FalActaFallo nuevoFallo = falloRepo.findVigenteByActaId(actaId).orElseThrow();
            assertThat(nuevoFallo.getId()).isNotEqualTo(falloOriginal.getId());
            assertThat(nuevoFallo.getMontoCondena()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(nuevoFallo.getFalloReemplazadoId()).isEqualTo(falloOriginal.getId());
            assertThat(nuevoFallo.isSiVigente()).isTrue();

            FalActaFallo falloAnterior = falloRepo.findById(falloOriginal.getId()).orElseThrow();
            assertThat(falloAnterior.isSiVigente()).isFalse();
        }
        @Test @DisplayName("modifica: historial conserva ambos fallos")
        void modifica_historial_conservado() {
            Long actaId = crearActaConApelacionPresentada();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
            apelacionService.resolverModificaCondena(
                    new ResolverApelacionModificaCondenaCommand(apelacionId, new BigDecimal("1500"), null, null));
            assertThat(falloRepo.findByActaId(actaId)).hasSize(2);
        }
        @Test @DisplayName("modifica: genera APEMCO y FALRMP en eventos")
        void modifica_eventos() {
            Long actaId = crearActaConApelacionPresentada();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
            apelacionService.resolverModificaCondena(
                    new ResolverApelacionModificaCondenaCommand(apelacionId, new BigDecimal("3000"), null, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APEMCO, TipoEventoActa.FALRMP);
        }
    }

    // =========================================================================
    // NULIDAD
    // =========================================================================
    @Nested @DisplayName("NULIDAD: resultado NULIDAD, fallo desactivado")
    class Nulidad {
        @Test @DisplayName("nulidad: resultado NULIDAD, no cierra, fallo anterior no vigente")
        void nulidad_efectos() {
            Long actaId = crearActaConApelacionPresentada();
            FalActaFallo falloOriginal = falloRepo.findVigenteByActaId(actaId).orElseThrow();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();

            ComandoResultado r = apelacionService.resolverNulidad(
                    new ResolverApelacionNulidadCommand(apelacionId, "Vicios procesales", "USR-J"));
            assertThat(r.tipoEvento()).isEqualTo("APENUL");

            FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.NULIDAD);

            FalActaFallo falloAnterior = falloRepo.findById(falloOriginal.getId()).orElseThrow();
            assertThat(falloAnterior.isSiVigente()).isFalse();
        }
        @Test @DisplayName("nulidad: genera APENUL en eventos")
        void nulidad_evento() {
            Long actaId = crearActaConApelacionPresentada();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
            apelacionService.resolverNulidad(new ResolverApelacionNulidadCommand(apelacionId, null, null));
            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APENUL);
        }
        @Test @DisplayName("nulidad: apelacion queda RESUELTA+NULIDAD")
        void nulidad_apelacion_estado() {
            Long actaId = crearActaConApelacionPresentada();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
            apelacionService.resolverNulidad(new ResolverApelacionNulidadCommand(apelacionId, null, null));
            FalActaApelacion apel = apelacionRepo.findById(apelacionId).orElseThrow();
            assertThat(apel.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.RESUELTA);
            assertThat(apel.getResultadoResolucion()).isEqualTo(ResultadoResolucionApelacion.NULIDAD);
        }
    }

    // =========================================================================
    // PASAR A ANALISIS
    // =========================================================================
    @Nested @DisplayName("EN_ANALISIS: transicion de estado")
    class EnAnalisis {
        @Test @DisplayName("pasarAAnalisis: estado pasa de PRESENTADA a EN_ANALISIS")
        void pasar_a_analisis() {
            Long actaId = crearActaConApelacionPresentada();
            Long apelacionId = apelacionRepo.buscarUltima(actaId).orElseThrow().getId();
            ComandoResultado r = apelacionService.pasarAAnalisis(
                    new PasarApelacionAAnalisisCommand(apelacionId, "USR-OP"));
            assertThat(r.tipoEvento()).isEqualTo("APEANL");
            FalActaApelacion apel = apelacionRepo.findById(apelacionId).orElseThrow();
            assertThat(apel.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.EN_ANALISIS);
        }
    }
}
