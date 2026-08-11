package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.MedidaPreventivaActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaContravencionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSustanciasAlimenticiasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoAlcoholemiaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaTransitoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaVehiculoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryObservacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wave 4 Parte 3B §7 (DECISION-MEDIDA-PREVENTIVA-LIBRE-01): el inspector puede elegir
 * cualquier medida preventiva vigente del catálogo, sin exigir compatibilidad previa con el
 * artículo infringido. Se valida únicamente existencia/vigencia de la medida y ausencia de
 * duplicados exactos ({@code acta_id + medida_preventiva_id}) dentro de la misma acta.
 */
@DisplayName("Wave 4 Parte 3B §7: medidas preventivas libres del catálogo")
class MedidasPreventivasLibresTest {

    private static final String ACTOR = "8f14e45f-ceea-467a-9c1e-1b0f2b3c4d5e";

    private InMemoryActaRepository actaRepo;
    private InMemoryMedidaPreventivaRepository medidaRepo;
    private InMemoryActaMedidaPreventivaRepository medidaAplicadaRepo;
    private ActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        InMemoryActaEvidenciaRepository evidenciaRepo = new InMemoryActaEvidenciaRepository();
        medidaRepo = new InMemoryMedidaPreventivaRepository();
        medidaAplicadaRepo = new InMemoryActaMedidaPreventivaRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED, snapshotRepo);
        QrActaService qrService = QrTestSupport.qrActaService(
                actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED);

        service = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, evidenciaRepo,
                FaltasClockTestSupport.FIXED, qrService, new InMemoryObservacionRepository(),
                new InMemoryPersonaDomicilioRepository(),
                new InMemoryActaArticuloInfringidoRepository(),
                ActaServiceTestSupport.valorizacionServiceMinima(actaRepo, FaltasClockTestSupport.FIXED),
                ActaServiceTestSupport.talonarioServiceMinimo(FaltasClockTestSupport.FIXED),
                new InMemoryActaVehiculoRepository(),
                new InMemoryActaTransitoRepository(),
                new InMemoryActaTransitoAlcoholemiaRepository(),
                new InMemoryActaContravencionRepository(),
                new InMemoryActaSustanciasAlimenticiasRepository(),
                medidaAplicadaRepo,
                medidaRepo,
                ActaServiceTestSupport.fotografiaEvidenciaServiceMinima(
                        actaRepo, evidenciaRepo, FaltasClockTestSupport.FIXED));

        ActorContextHolder.set(new ActorContext(ACTOR));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    private Long seedMedidaActiva(String codigo) {
        Long id = medidaRepo.nextId();
        medidaRepo.save(new FalMedidaPreventiva(
                id, codigo, (short) 1, "Medida " + codigo, FaltasClockTestSupport.FIXED.now(), ACTOR));
        return id;
    }

    private Long seedMedidaInactiva(String codigo) {
        Long id = medidaRepo.nextId();
        FalMedidaPreventiva medida = new FalMedidaPreventiva(
                id, codigo, (short) 1, "Medida " + codigo, FaltasClockTestSupport.FIXED.now(), ACTOR);
        medida.setSiActiva(false);
        medidaRepo.save(medida);
        return id;
    }

    private LabrarActaCommand comandoConMedidas(List<MedidaPreventivaActaItem> medidas) {
        TransitoActaItem transito = new TransitoActaItem(
                null, null, null, null, false, false, false, null);
        return new LabrarActaCommand(
                TipoActa.TRANSITO, 1L, 1L, LocalDate.of(2026, 8, 6),
                "Dom. hecho", null, null, null, null,
                null, null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                SituacionIdentificacionInfractor.AUSENTE,
                null, null, null,
                List.of(new ArticuloInfringidoItem(1L, 1L)),
                null, transito, null, null, medidas);
    }

    @Test
    @DisplayName("medida activa sin relacion previa con el articulo se aplica correctamente")
    void medidaActivaLibreSePermite() {
        Long idMedida = seedMedidaActiva("MP-LIBRE-01");

        ComandoResultado res = service.labrar(
                comandoConMedidas(List.of(new MedidaPreventivaActaItem(null, idMedida, "obs", false))));

        List<FalActaMedidaPreventiva> aplicadas = medidasDe(res.idActa());
        assertThat(aplicadas).hasSize(1);
        assertThat(aplicadas.get(0).getActaArticuloId())
                .as("la medida ya no referencia un articulo infringido")
                .isNull();
        assertThat(aplicadas.get(0).getEstadoMedida().name()).isEqualTo("APLICADA");
    }

    @Test
    @DisplayName("medida inexistente en el catalogo rechaza el alta")
    void medidaInexistenteRechaza() {
        // Nota: el rollback transaccional real (cero fal_acta tras el rechazo) requiere un
        // PlatformTransactionManager real y esta cubierto contra MariaDB real por
        // ActaAltaIntegralParte3AIT$SatelitesWiring (Wave 4 Parte 3A/3B). Aqui, sin proxy
        // transaccional de Spring, @Transactional es inerte; este test verifica el rechazo
        // funcional (tipo y mensaje de la excepcion), no la atomicidad fisica.
        assertThatThrownBy(() -> service.labrar(
                comandoConMedidas(List.of(new MedidaPreventivaActaItem(null, 999_999L, null, false)))))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("medida inactiva rechaza el alta")
    void medidaInactivaRechaza() {
        Long idMedida = seedMedidaInactiva("MP-INACT-01");

        assertThatThrownBy(() -> service.labrar(
                comandoConMedidas(List.of(new MedidaPreventivaActaItem(null, idMedida, null, false)))))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("no esta activa");
    }

    @Test
    @DisplayName("duplicado exacto (medidaPreventivaId repetido) dentro de la misma acta rechaza todo el alta")
    void duplicadoExactoRechaza() {
        Long idMedida = seedMedidaActiva("MP-DUP-01");

        assertThatThrownBy(() -> service.labrar(comandoConMedidas(List.of(
                new MedidaPreventivaActaItem(null, idMedida, null, false),
                new MedidaPreventivaActaItem(null, idMedida, "otra obs", false)))))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("duplicada");
    }

    @Test
    @DisplayName("pueden existir varias medidas distintas por acta")
    void variasMedidasDistintasPermitidas() {
        Long idMedida1 = seedMedidaActiva("MP-MULTI-01");
        Long idMedida2 = seedMedidaActiva("MP-MULTI-02");

        ComandoResultado res = service.labrar(comandoConMedidas(List.of(
                new MedidaPreventivaActaItem(null, idMedida1, null, false),
                new MedidaPreventivaActaItem(null, idMedida2, null, true))));

        assertThat(medidasDe(res.idActa())).hasSize(2);
    }

    @Test
    @DisplayName("si_genera_bloqueante se preserva tal como se informa")
    void siGeneraBloqueanteSePreserva() {
        Long idMedida = seedMedidaActiva("MP-BLOQ-01");

        ComandoResultado res = service.labrar(comandoConMedidas(
                List.of(new MedidaPreventivaActaItem(null, idMedida, null, true))));

        assertThat(medidasDe(res.idActa()).get(0).isSiGeneraBloqueante()).isTrue();
    }

    private List<FalActaMedidaPreventiva> medidasDe(Long idActa) {
        return medidaAplicadaRepo.findByActaId(idActa);
    }
}
