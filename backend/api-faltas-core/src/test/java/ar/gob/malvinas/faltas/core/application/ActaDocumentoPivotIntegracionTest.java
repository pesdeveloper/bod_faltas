package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.service.*;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de integracion del pivot acta-documento con productores reales.
 * Verifica: FalloActaService, snapshot.idDocuUlt, firma, notificacion.
 */
@DisplayName("8F-11J: Pivot integracion con productores")
class ActaDocumentoPivotIntegracionTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 7, 6, 10, 0);

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryDocumentoRepository docRepo;
    private InMemoryFalloActaRepository falloRepo;
    private InMemoryPagoVoluntarioRepository pagoVolRepo;
    private InMemoryActaDocumentoRepository pivotRepo;
    private ActaDocumentoService actaDocService;
    private FalloActaService falloService;
    private SnapshotRecalculador recalculador;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        falloRepo = new InMemoryFalloActaRepository();
        pagoVolRepo = new InMemoryPagoVoluntarioRepository();
        pivotRepo = new InMemoryActaDocumentoRepository();
        actaDocService = new ActaDocumentoService(pivotRepo, actaRepo, docRepo, FaltasClockTestSupport.FIXED);

        recalculador = new SnapshotRecalculador(
                eventoRepo, docRepo, new InMemoryNotificacionRepository(),
                pagoVolRepo, falloRepo, new InMemoryApelacionActaRepository(),
                new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);
        // Inyectar pivot via reflection (campo @Autowired(required=false))
        ReflectionTestUtils.setField(recalculador, "actaDocumentoRepository", pivotRepo);

        // FalloActaService sin ValorizacionService (OK para este test)
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, pagoVolRepo, recalculador, FaltasClockTestSupport.FIXED);
        ReflectionTestUtils.setField(falloService, "actaDocumentoService", actaDocService);
    }

    private FalActa crearActaConBloque(Long id, BloqueActual bloque) {
        FalActa a = new FalActa(id, "UUID-" + id, TipoActa.TRANSITO, 1L, 1L,
                LocalDate.of(2026, 7, 6), AHORA, "Av. Libertad 100", null,
                null, null, ResultadoFirmaInfractor.FIRMADA, null, AHORA, "SYS");
        a.setBloqueActual(bloque);
        actaRepo.guardar(a);
        return a;
    }

    // =========================================================================
    // T-INT-01: FalloActaService asocia pivot al dictar fallo condenatorio
    // =========================================================================

    @Test @DisplayName("INT-01: dictar fallo condenatorio crea pivot con rol FALLO")
    void integracion01_falloCondenatorioCreaPivot() {
        crearActaConBloque(1L, BloqueActual.ANAL);

        DictarFalloCondenatorioCommand cmd = new DictarFalloCondenatorioCommand(
                1L, BigDecimal.valueOf(5000), null, null);
        falloService.dictarCondenatorio(cmd);

        assertThat(pivotRepo.listarPorActaYRol(1L, RolDocuActa.FALLO)).hasSize(1);
        assertThat(pivotRepo.buscarPrincipalPorActaYRol(1L, RolDocuActa.FALLO)).isPresent();
    }

    // =========================================================================
    // T-INT-02: SnapshotRecalculador calcula idDocuUlt desde pivot
    // =========================================================================

    @Test @DisplayName("INT-02: snapshot.idDocuUlt se calcula desde pivot")
    void integracion02_snapshotIdDocuUlt() {
        FalActa acta = crearActaConBloque(1L, BloqueActual.ANAL);

        // Crear documento y asociar via pivot
        FalDocumento doc = new FalDocumento(100L, 1L, TipoDocu.ACTO_ADMINISTRATIVO, AHORA);
        doc.setStorageKey("storage/1/fallo/100");
        docRepo.guardar(doc);
        pivotRepo.asociarComoPrincipalAtomico(1L, 100L, RolDocuActa.FALLO, "SYS", AHORA);

        FalActaSnapshot snap = recalculador.recalcular(acta);
        assertThat(snap.getIdDocuUlt()).isEqualTo(100L);
    }

    // =========================================================================
    // T-INT-03: Sin pivot, idDocuUlt es null
    // =========================================================================

    @Test @DisplayName("INT-03: sin pivot, snapshot.idDocuUlt es null")
    void integracion03_sinPivotIdDocuUltNull() {
        FalActa acta = crearActaConBloque(1L, BloqueActual.CAPT);

        FalActaSnapshot snap = recalculador.recalcular(acta);
        assertThat(snap.getIdDocuUlt()).isNull();
    }

    // =========================================================================
    // T-INT-04: Prioridad FALLO > NOTIFICACION > ACTA_PRINCIPAL en idDocuUlt
    // =========================================================================

    @Test @DisplayName("INT-04: idDocuUlt prioriza FALLO sobre NOTIFICACION y ACTA_PRINCIPAL")
    void integracion04_prioridadIdDocuUlt() {
        FalActa acta = crearActaConBloque(1L, BloqueActual.ANAL);

        // Agregar acta principal y notificacion
        FalDocumento actaPrincipal = new FalDocumento(200L, 1L, TipoDocu.ACTA_INFRACCION, AHORA);
        FalDocumento notif = new FalDocumento(201L, 1L, TipoDocu.NOTIFICACION_ACTA, AHORA);
        FalDocumento fallo = new FalDocumento(202L, 1L, TipoDocu.ACTO_ADMINISTRATIVO, AHORA);
        docRepo.guardar(actaPrincipal);
        docRepo.guardar(notif);
        docRepo.guardar(fallo);

        pivotRepo.asociarComoPrincipalAtomico(1L, 200L, RolDocuActa.ACTA_PRINCIPAL, "SYS", AHORA);
        pivotRepo.asociarComoPrincipalAtomico(1L, 201L, RolDocuActa.NOTIFICACION, "SYS", AHORA);
        pivotRepo.asociarComoPrincipalAtomico(1L, 202L, RolDocuActa.FALLO, "SYS", AHORA);

        FalActaSnapshot snap = recalculador.recalcular(acta);
        assertThat(snap.getIdDocuUlt()).isEqualTo(202L);
    }

    // =========================================================================
    // T-INT-05: Dos fallos, el principal es el reemplazado
    // =========================================================================

    @Test @DisplayName("INT-05: reemplazo de fallo actualiza idDocuUlt al nuevo")
    void integracion05_reemplazoPivotActualizaDocuUlt() {
        FalActa acta = crearActaConBloque(1L, BloqueActual.ANAL);

        FalDocumento fallo1 = new FalDocumento(300L, 1L, TipoDocu.ACTO_ADMINISTRATIVO, AHORA);
        FalDocumento fallo2 = new FalDocumento(301L, 1L, TipoDocu.ACTO_ADMINISTRATIVO, AHORA);
        docRepo.guardar(fallo1);
        docRepo.guardar(fallo2);

        pivotRepo.asociarComoPrincipalAtomico(1L, 300L, RolDocuActa.FALLO, "SYS", AHORA);
        FalActaSnapshot snap1 = recalculador.recalcular(acta);
        assertThat(snap1.getIdDocuUlt()).isEqualTo(300L);

        pivotRepo.reemplazarPrincipalAtomico(1L, 301L, RolDocuActa.FALLO, "SYS", AHORA);
        FalActaSnapshot snap2 = recalculador.recalcular(acta);
        assertThat(snap2.getIdDocuUlt()).isEqualTo(301L);
    }
}
