package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.CountingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que cada operacion use un unico instante de reloj para el mismo hecho.
 *
 * Usa CountingClock que avanza 1 segundo por invocacion: si un metodo
 * llama faltasClock.now() dos veces para construir la misma entidad,
 * los dos timestamps seran distintos y las assertions fallan.
 */
class InstanteUnicoOperacionTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryFalloActaRepository falloRepo;
    private InMemoryPagoVoluntarioRepository pagoVolRepo;
    private SnapshotRecalculador recalc;

    private CountingClock clock;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        actaRepo    = new InMemoryActaRepository();
        eventoRepo  = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo     = new InMemoryDocumentoRepository();
        falloRepo   = new InMemoryFalloActaRepository();
        pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        clock = CountingClock.startingAt("2026-07-09T12:00:00Z");
        recalc = new SnapshotRecalculador(
                eventoRepo, docRepo,
                new InMemoryNotificacionRepository(),
                pagoVolRepo, falloRepo,
                new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(),
                clock, snapshotRepo);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // -------------------------------------------------------------------------
    // ActaService.labrar
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ActaService.labrar usa un unico instante para fechaLabrado y fhAlta")
    void labrar_usa_instante_unico() {
        ActaService svc = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), clock);

        clock.reset();
        ComandoResultado res = svc.labrar(new LabrarActaCommand(
                TipoActa.TRANSITO, null, null, null, null, null, null,
                null, null, null, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

        FalActa acta = actaRepo.buscarPorId(res.idActa()).orElseThrow();
        LocalDateTime esperado = clock.nthInstant(0);

        assertThat(acta.getFhAlta())
                .as("fhAlta debe usar el instante 0 de la operacion")
                .isEqualTo(esperado);
        assertThat(acta.getFechaLabrado())
                .as("fechaLabrado debe ser el mismo instante que fhAlta (mismo hecho)")
                .isEqualTo(acta.getFhAlta());
    }

    @Test
    @DisplayName("ActaService.labrar snapshot.ultimaActualizacion usa mismo instante que fhAlta; sin llamada extra al reloj")
    void labrar_snapshot_usa_mismo_instante_sin_llamada_extra() {
        ActaService svc = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), clock);

        clock.reset();
        ComandoResultado res = svc.labrar(new LabrarActaCommand(
                TipoActa.TRANSITO, null, null, null, null, null, null,
                null, null, null, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));

        LocalDateTime esperado = clock.nthInstant(0);

        FalActa acta = actaRepo.buscarPorId(res.idActa()).orElseThrow();
        ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot snap =
                snapshotRepo.buscarPorActa(res.idActa()).orElseThrow();

        assertThat(acta.getFhAlta())
                .as("acta.fhAlta debe ser el instante 0")
                .isEqualTo(esperado);
        assertThat(snap.getUltimaActualizacion())
                .as("snapshot.ultimaActualizacion debe usar el mismo instante que acta.fhAlta")
                .isEqualTo(esperado);
        assertThat(clock.invocationCount())
                .as("CountingClock debe ser llamado exactamente 1 vez por labrar (sin llamada extra para snapshot)")
                .isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // FalloActaService.dictarAbsolutorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FalloActaService.dictarAbsolutorio usa un unico instante para fhDictado, fhAlta y fechaGeneracion")
    void absolutorio_usa_instante_unico() {
        ActaService actaSvc = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), clock);
        FalloActaService falloSvc = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, clock);

        ComandoResultado actaRes = actaSvc.labrar(new LabrarActaCommand(
                TipoActa.TRANSITO, null, null, null, null, null, null,
                null, null, null, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));
        Long actaId = actaRes.idActa();

        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepo.guardar(acta);

        clock.reset();
        falloSvc.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(actaId, null, null));

        LocalDateTime esperado = clock.nthInstant(0);

        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        FalDocumento doc = docRepo.buscarPorId(fallo.getDocumentoId()).orElseThrow();

        assertThat(fallo.getFhAlta())
                .as("fhAlta del fallo debe usar el instante 0 de la operacion")
                .isEqualTo(esperado);
        assertThat(fallo.getFhDictado())
                .as("fhDictado debe ser el mismo instante que fhAlta (mismo hecho)")
                .isEqualTo(fallo.getFhAlta());
        assertThat(doc.getFechaGeneracion())
                .as("fechaGeneracion del documento debe usar el mismo instante que el fallo")
                .isEqualTo(esperado);
    }

    // -------------------------------------------------------------------------
    // FalloActaService.dictarCondenatorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FalloActaService.dictarCondenatorio usa un unico instante para fhDictado, fhAlta y fechaGeneracion")
    void condenatorio_usa_instante_unico() {
        ActaService actaSvc = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), clock);
        FalloActaService falloSvc = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo, falloRepo, pagoVolRepo, recalc, clock);

        ComandoResultado actaRes = actaSvc.labrar(new LabrarActaCommand(
                TipoActa.TRANSITO, null, null, null, null, null, null,
                null, null, null, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null));
        Long actaId = actaRes.idActa();

        FalActa acta = actaRepo.buscarPorId(actaId).orElseThrow();
        acta.setBloqueActual(BloqueActual.ANAL);
        actaRepo.guardar(acta);

        clock.reset();
        falloSvc.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, BigDecimal.valueOf(5000), null, null));

        LocalDateTime esperado = clock.nthInstant(0);

        FalActaFallo fallo = falloRepo.buscarActivo(actaId).orElseThrow();
        FalDocumento doc = docRepo.buscarPorId(fallo.getDocumentoId()).orElseThrow();

        assertThat(fallo.getFhAlta())
                .as("fhAlta del fallo debe usar el instante 0 de la operacion")
                .isEqualTo(esperado);
        assertThat(fallo.getFhDictado())
                .as("fhDictado debe ser el mismo instante que fhAlta (mismo hecho)")
                .isEqualTo(fallo.getFhAlta());
        assertThat(doc.getFechaGeneracion())
                .as("fechaGeneracion del documento debe usar el mismo instante que el fallo")
                .isEqualTo(esperado);
    }
}
