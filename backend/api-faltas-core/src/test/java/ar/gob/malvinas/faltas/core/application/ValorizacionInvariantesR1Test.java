package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CrearArticuloNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.command.CrearNormativaFaltasCommand;
import ar.gob.malvinas.faltas.core.application.service.ActaArticuloInfringidoService;
import ar.gob.malvinas.faltas.core.application.service.MedidaPreventivaService;
import ar.gob.malvinas.faltas.core.application.service.NormativaService;
import ar.gob.malvinas.faltas.core.application.service.TarifarioService;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.domain.enums.CriterioTarifario;
import ar.gob.malvinas.faltas.core.domain.enums.MotivoBajaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoValorizacion;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenBloqueanteMaterial;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidad;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.enums.TipoValorizacionActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.ArticuloMedidaPreventivaId;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalMedidaPreventiva;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryArticuloMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryMedidaPreventivaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de cierre R1 del Slice 8F-11D.
 *
 * Cubre:
 *   R1-A: SnapshotRecalculador proyecta campos de valorizacion operativa.
 *   R1-B: confirmarVigenteAtomico garantiza exactamente una vigente bajo concurrencia.
 *   R1-C: items de valorizacion CONFIRMADA son inmutables.
 *   R1-D suplementarios: tarifario sin superposicion, medida preventiva version atomica,
 *         articulo imputado unicidad activa, articulo-medida sin reactivacion silenciosa.
 */
@DisplayName("R1: Invariantes de cierre Slice 8F-11D")
class ValorizacionInvariantesR1Test {

    // ================================================================
    // R1-A: Snapshot con proyeccion de valorizacion
    // ================================================================

    @Nested
    @DisplayName("R1-A: SnapshotRecalculador proyecta valorizacion operativa")
    class SnapshotProyeccionR1ATest {

        private InMemoryActaRepository actaRepo;
        private InMemoryActaValorizacionRepository valorizacionRepo;
        private InMemoryActaValorizacionItemRepository itemRepo;
        private InMemoryActaArticuloInfringidoRepository articuloImputadoRepo;
        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
        private InMemoryNormativaRepository normativaRepo;
        private NormativaService normativaService;
        private ActaArticuloInfringidoService articuloService;
        private ValorizacionService valorizacionService;
        private SnapshotRecalculador recalcConValorizacion;
        private SnapshotRecalculador recalcSinValorizacion;
        private FalActa acta;
        private InMemoryActaSnapshotRepository snapshotRepo;

        @BeforeEach
        void setUp() {
            snapshotRepo = new InMemoryActaSnapshotRepository();
            actaRepo = new InMemoryActaRepository();
            normativaRepo = new InMemoryNormativaRepository();
            articuloImputadoRepo = new InMemoryActaArticuloInfringidoRepository();
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
            valorizacionRepo = new InMemoryActaValorizacionRepository();
            itemRepo = new InMemoryActaValorizacionItemRepository();
            InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            normativaService = new NormativaService(normativaRepo, new InMemoryDependenciaRepository(), FaltasClockTestSupport.FIXED);
            articuloService = new ActaArticuloInfringidoService(articuloImputadoRepo, actaRepo, normativaRepo, articuloMedidaRepo, FaltasClockTestSupport.FIXED);
            valorizacionService = new ValorizacionService(valorizacionRepo, itemRepo, articuloImputadoRepo, normativaRepo, tarifarioRepo, actaRepo, FaltasClockTestSupport.FIXED);

            InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
            InMemoryDocumentoRepository docRepo = new InMemoryDocumentoRepository();
            InMemoryNotificacionRepository notifRepo = new InMemoryNotificacionRepository();
            InMemoryPagoVoluntarioRepository pagoVRepo = new InMemoryPagoVoluntarioRepository();
            InMemoryFalloActaRepository falloRepo = new InMemoryFalloActaRepository();
            InMemoryApelacionActaRepository apelacionRepo = new InMemoryApelacionActaRepository();
            InMemoryPagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();

            recalcConValorizacion = new SnapshotRecalculador(
                    eventoRepo, docRepo, notifRepo, pagoVRepo, falloRepo, apelacionRepo, pagoCondenaRepo,
                    valorizacionService, FaltasClockTestSupport.FIXED, snapshotRepo);

            recalcSinValorizacion = new SnapshotRecalculador(
                    eventoRepo, docRepo, notifRepo, pagoVRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

            acta = actaRepo.guardar(new FalActa(actaRepo.nextId(), "uuid-r1a", TipoActa.TRANSITO,
                    1L, 1L, FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle 1", null, null, null,
                    ResultadoFirmaInfractor.FIRMADA, null, FaltasClockTestSupport.FIXED.now(), "u1"));

            FalNormativaFaltas norm = normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("ORD-R1A", 1, "Ord R1A", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalArticuloNormativaFaltas art = normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(norm.getId(), "ART-R1A", 1, "Art R1A", null,
                            new BigDecimal("2"), TipoUnidad.SALARIO, false, null, null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    tarifarioRepo.nextId(), TipoUnidadFaltas.SALARIO, new BigDecimal("100000"),
                    LocalDate.of(2020, 1, 1), FaltasClockTestSupport.FIXED.now(), "sistema");
            tarifarioRepo.save(t);
            articuloService.imputar(acta.getId(), norm.getId(), art.getId(), "u1");
        }

        @Test
        @DisplayName("sin ValorizacionService: snapshot tiene campos en null/false")
        void sin_valorizacion_service_campos_nulos() {
            FalActaSnapshot snap = recalcSinValorizacion.recalcular(acta);
            assertThat(snap.getValorizacionOperativaId()).isNull();
            assertThat(snap.getEstadoValorizacionOperativa()).isNull();
            assertThat(snap.getTipoValorizacionOperativa()).isNull();
            assertThat(snap.getMontoOperativoVigente()).isNull();
            assertThat(snap.isSiMontoConfirmado()).isFalse();
        }

        @Test
        @DisplayName("con ValorizacionService sin valorizacion confirmada: snapshot campos en null/false")
        void con_valorizacion_service_sin_vigente_campos_nulos() {
            FalActaSnapshot snap = recalcConValorizacion.recalcular(acta);
            assertThat(snap.getValorizacionOperativaId()).isNull();
            assertThat(snap.isSiMontoConfirmado()).isFalse();
        }

        @Test
        @DisplayName("con valorizacion confirmada: snapshot proyecta campos correctamente")
        void con_valorizacion_confirmada_snap_proyectada() {
            FalActaValorizacion prel = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion confirmada = valorizacionService.confirmar(prel.getId(), "u1");

            FalActaSnapshot snap = recalcConValorizacion.recalcular(acta);

            assertThat(snap.getValorizacionOperativaId()).isEqualTo(confirmada.getId());
            assertThat(snap.getEstadoValorizacionOperativa()).isEqualTo(EstadoValorizacion.CONFIRMADA);
            assertThat(snap.getTipoValorizacionOperativa()).isEqualTo(TipoValorizacionActa.INFRACCION_BASE);
            assertThat(snap.getMontoOperativoVigente()).isEqualByComparingTo(new BigDecimal("200000.00"));
            assertThat(snap.isSiMontoConfirmado()).isTrue();
        }

        @Test
        @DisplayName("al reemplazar una vigente con nueva confirmacion: snapshot refleja la nueva")
        void snapshot_refleja_nueva_confirmada_tras_reemplazo() {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel1.getId(), "u1");

            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion conf2 = valorizacionService.confirmar(prel2.getId(), "u1");

            FalActaSnapshot snap = recalcConValorizacion.recalcular(acta);
            assertThat(snap.getValorizacionOperativaId()).isEqualTo(conf2.getId());
            assertThat(snap.isSiMontoConfirmado()).isTrue();
        }
    }

    // ================================================================
    // R1-B: Atomicidad de vigencia y concurrencia
    // ================================================================

    @Nested
    @DisplayName("R1-B: confirmarVigenteAtomico garantiza unicidad bajo concurrencia")
    class ConfirmarVigenteAtomicoR1BTest {

        private InMemoryActaRepository actaRepo;
        private InMemoryActaValorizacionRepository valorizacionRepo;
        private InMemoryActaValorizacionItemRepository itemRepo;
        private InMemoryActaArticuloInfringidoRepository articuloImputadoRepo;
        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
        private InMemoryNormativaRepository normativaRepo;
        private NormativaService normativaService;
        private ActaArticuloInfringidoService articuloService;
        private ValorizacionService valorizacionService;
        private FalActa acta;

        @BeforeEach
        void setUp() {
            actaRepo = new InMemoryActaRepository();
            normativaRepo = new InMemoryNormativaRepository();
            articuloImputadoRepo = new InMemoryActaArticuloInfringidoRepository();
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
            valorizacionRepo = new InMemoryActaValorizacionRepository();
            itemRepo = new InMemoryActaValorizacionItemRepository();
            InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            normativaService = new NormativaService(normativaRepo, new InMemoryDependenciaRepository(), FaltasClockTestSupport.FIXED);
            articuloService = new ActaArticuloInfringidoService(articuloImputadoRepo, actaRepo, normativaRepo, articuloMedidaRepo, FaltasClockTestSupport.FIXED);
            valorizacionService = new ValorizacionService(valorizacionRepo, itemRepo, articuloImputadoRepo, normativaRepo, tarifarioRepo, actaRepo, FaltasClockTestSupport.FIXED);

            acta = actaRepo.guardar(new FalActa(actaRepo.nextId(), "uuid-r1b", TipoActa.TRANSITO,
                    1L, 1L, FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle 2", null, null, null,
                    ResultadoFirmaInfractor.FIRMADA, null, FaltasClockTestSupport.FIXED.now(), "u1"));

            FalNormativaFaltas norm = normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("ORD-R1B", 1, "Ord R1B", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalArticuloNormativaFaltas art = normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(norm.getId(), "ART-R1B", 1, "Art R1B", null,
                            new BigDecimal("1"), TipoUnidad.SALARIO, false, null, null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    tarifarioRepo.nextId(), TipoUnidadFaltas.SALARIO, new BigDecimal("50000"),
                    LocalDate.of(2020, 1, 1), FaltasClockTestSupport.FIXED.now(), "sistema");
            tarifarioRepo.save(t);
            articuloService.imputar(acta.getId(), norm.getId(), art.getId(), "u1");
        }

        @Test
        @DisplayName("CAS: segunda llamada con vigenteAnteriorId=null falla si ya hay vigente")
        void cas_segunda_llamada_falla_si_ya_hay_vigente() {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");

            // Primera confirmacion: sin vigente anterior -> OK
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            valorizacionRepo.confirmarVigenteAtomico(prel1.getId(), prel1.getVersionRow(), null, null, ahora, "u1");

            // Segunda confirmacion: caller cree que no habia vigente -> ConcurrenciaConflictoException
            assertThatThrownBy(() ->
                    valorizacionRepo.confirmarVigenteAtomico(prel2.getId(), prel2.getVersionRow(), null, null, ahora, "u2"))
                    .isInstanceOf(ConcurrenciaConflictoException.class);
        }

        @Test
        @DisplayName("tras CAS conflict: solo una vigente CONFIRMADA existe")
        void tras_cas_conflict_exactamente_una_vigente() {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");

            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
            valorizacionRepo.confirmarVigenteAtomico(prel1.getId(), prel1.getVersionRow(), null, null, ahora, "u1");
            try {
                valorizacionRepo.confirmarVigenteAtomico(prel2.getId(), prel2.getVersionRow(), null, null, ahora, "u2");
            } catch (ConcurrenciaConflictoException ignored) {}

            Optional<FalActaValorizacion> vigente = valorizacionRepo.findVigenteByActaIdAndTipo(
                    acta.getId(), TipoValorizacionActa.INFRACCION_BASE);
            assertThat(vigente).isPresent();
            assertThat(vigente.get().getEstadoValorizacion()).isEqualTo(EstadoValorizacion.CONFIRMADA);
            assertThat(vigente.get().isSiVigente()).isTrue();
        }

        @Test
        @DisplayName("confirmar serial segunda vez reemplaza la primera atomicamente")
        void confirmar_serial_reemplaza_primera() {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion conf1 = valorizacionService.confirmar(prel1.getId(), "u1");
            assertThat(conf1.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.CONFIRMADA);

            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion conf2 = valorizacionService.confirmar(prel2.getId(), "u1");
            assertThat(conf2.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.CONFIRMADA);

            FalActaValorizacion anterior = valorizacionRepo.findById(conf1.getId()).orElseThrow();
            assertThat(anterior.getEstadoValorizacion()).isEqualTo(EstadoValorizacion.REEMPLAZADA);
            assertThat(anterior.isSiVigente()).isFalse();
        }

        @Test
        @DisplayName("concurrencia real: dos hilos leen vigente vacia y confirman - solo una triunfa")
        void concurrencia_real_solo_una_triunfa() throws InterruptedException {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u2");

            // CyclicBarrier asegura que ambos hilos lean la vigente ANTES de que cualquiera confirme.
            // Ambos leen vigente=empty y luego proceden a confirmarVigenteAtomico con null anterior.
            // El metodo synchronized garantiza que solo uno triunfa.
            CyclicBarrier barrier = new CyclicBarrier(2);
            AtomicInteger successes = new AtomicInteger(0);
            AtomicInteger conflicts = new AtomicInteger(0);
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

            Runnable confirmar1 = () -> {
                try {
                    barrier.await();  // Ambos hilos leen vigente simultaneamente
                    valorizacionRepo.confirmarVigenteAtomico(
                            prel1.getId(), prel1.getVersionRow(), null, null, ahora, "u1");
                    successes.incrementAndGet();
                } catch (ConcurrenciaConflictoException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException | BrokenBarrierException ignored) {}
            };

            Runnable confirmar2 = () -> {
                try {
                    barrier.await();  // Ambos hilos leen vigente simultaneamente
                    valorizacionRepo.confirmarVigenteAtomico(
                            prel2.getId(), prel2.getVersionRow(), null, null, ahora, "u2");
                    successes.incrementAndGet();
                } catch (ConcurrenciaConflictoException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException | BrokenBarrierException ignored) {}
            };

            Thread t1 = new Thread(confirmar1);
            Thread t2 = new Thread(confirmar2);
            t1.start();
            t2.start();
            t1.join(5000);
            t2.join(5000);

            assertThat(successes.get() + conflicts.get()).isEqualTo(2);
            assertThat(successes.get()).isEqualTo(1);
            assertThat(conflicts.get()).isEqualTo(1);

            Optional<FalActaValorizacion> vigente = valorizacionRepo.findVigenteByActaIdAndTipo(
                    acta.getId(), TipoValorizacionActa.INFRACCION_BASE);
            assertThat(vigente).isPresent();
            assertThat(vigente.get().isSiVigente()).isTrue();
        }
    }

    // ================================================================
    // R1-C: Inmutabilidad de items post-confirmacion
    // ================================================================

    @Nested
    @DisplayName("R1-C: Items de valorizacion CONFIRMADA son inmutables")
    class ItemInmutabilidadR1CTest {

        private InMemoryActaValorizacionRepository valorizacionRepo;
        private InMemoryActaValorizacionItemRepository itemRepo;
        private InMemoryActaRepository actaRepo;
        private InMemoryActaArticuloInfringidoRepository articuloImputadoRepo;
        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
        private InMemoryNormativaRepository normativaRepo;
        private NormativaService normativaService;
        private ActaArticuloInfringidoService articuloService;
        private ValorizacionService valorizacionService;
        private FalActa acta;

        @BeforeEach
        void setUp() {
            actaRepo = new InMemoryActaRepository();
            normativaRepo = new InMemoryNormativaRepository();
            articuloImputadoRepo = new InMemoryActaArticuloInfringidoRepository();
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
            valorizacionRepo = new InMemoryActaValorizacionRepository();
            itemRepo = new InMemoryActaValorizacionItemRepository(valorizacionRepo);
            InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            normativaService = new NormativaService(normativaRepo, new InMemoryDependenciaRepository(), FaltasClockTestSupport.FIXED);
            articuloService = new ActaArticuloInfringidoService(articuloImputadoRepo, actaRepo, normativaRepo, articuloMedidaRepo, FaltasClockTestSupport.FIXED);
            valorizacionService = new ValorizacionService(valorizacionRepo, itemRepo, articuloImputadoRepo, normativaRepo, tarifarioRepo, actaRepo, FaltasClockTestSupport.FIXED);

            acta = actaRepo.guardar(new FalActa(actaRepo.nextId(), "uuid-r1c", TipoActa.TRANSITO,
                    1L, 1L, FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle 3", null, null, null,
                    ResultadoFirmaInfractor.FIRMADA, null, FaltasClockTestSupport.FIXED.now(), "u1"));

            FalNormativaFaltas norm = normativaService.crearNormativa(
                    new CrearNormativaFaltasCommand("ORD-R1C", 1, "Ord R1C", null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalArticuloNormativaFaltas art = normativaService.crearArticulo(
                    new CrearArticuloNormativaFaltasCommand(norm.getId(), "ART-R1C", 1, "Art R1C", null,
                            new BigDecimal("2"), TipoUnidad.SALARIO, false, null, null, FaltasClockTestSupport.FIXED.now().toLocalDate(), "u1"));
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    tarifarioRepo.nextId(), TipoUnidadFaltas.SALARIO, new BigDecimal("80000"),
                    LocalDate.of(2020, 1, 1), FaltasClockTestSupport.FIXED.now(), "sistema");
            tarifarioRepo.save(t);
            articuloService.imputar(acta.getId(), norm.getId(), art.getId(), "u1");
        }

        @Test
        @DisplayName("guardia activa: salvar item con valorizacion PRELIMINAR es permitido")
        void item_para_preliminar_permitido() {
            FalActaValorizacion prel = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            var items = valorizacionService.listarItems(prel.getId());
            assertThat(items).isNotEmpty();
            FalActaValorizacionItem item = items.get(0);
            // Salvar item de valorizacion PRELIMINAR: OK (sin modificar campos readonly)
            FalActaValorizacionItem guardado = itemRepo.save(item);
            assertThat(guardado).isNotNull();
        }

        @Test
        @DisplayName("guardia activa: salvar item con valorizacion CONFIRMADA lanza PrecondicionVioladaException")
        void item_para_confirmada_rechazado() {
            FalActaValorizacion prel = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel.getId(), "u1");

            var items = valorizacionService.listarItems(prel.getId());
            assertThat(items).isNotEmpty();
            FalActaValorizacionItem item = items.get(0);

            assertThatThrownBy(() -> itemRepo.save(item))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("inmutables");
        }

        @Test
        @DisplayName("guardia activa: salvar item con valorizacion REEMPLAZADA lanza PrecondicionVioladaException")
        void item_para_reemplazada_rechazado() {
            FalActaValorizacion prel1 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel1.getId(), "u1");
            FalActaValorizacion prel2 = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel2.getId(), "u1");

            var items1 = valorizacionService.listarItems(prel1.getId());
            assertThat(items1).isNotEmpty();
            FalActaValorizacionItem item1 = items1.get(0);

            assertThatThrownBy(() -> itemRepo.save(item1))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("sin guardia (no-arg constructor): item para confirmada se guarda sin error")
        void sin_guardia_item_para_confirmada_permitido() {
            InMemoryActaValorizacionItemRepository itemRepoSinGuardia = new InMemoryActaValorizacionItemRepository();
            valorizacionService = new ValorizacionService(valorizacionRepo, itemRepoSinGuardia, articuloImputadoRepo, normativaRepo, tarifarioRepo, actaRepo, FaltasClockTestSupport.FIXED);

            FalActaValorizacion prel = valorizacionService.calcularBasePreliminar(acta.getId(), "u1");
            valorizacionService.confirmar(prel.getId(), "u1");

            var items = valorizacionService.listarItems(prel.getId());
            assertThat(items).isNotEmpty();
            FalActaValorizacionItem item = items.get(0);
            FalActaValorizacionItem guardado = itemRepoSinGuardia.save(item);
            assertThat(guardado).isNotNull();
        }
    }

    // ================================================================
    // R1-D: Tarifario - sin superposicion de rangos activos
    // ================================================================

    @Nested
    @DisplayName("R1-D.8.7: Tarifario - no superposicion de rangos activos")
    class TarifarioSuperposicionR1DTest {

        private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;

        @BeforeEach
        void setUp() {
            tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
        }

        private FalTarifarioUnidadFaltas newTarifario(TipoUnidadFaltas tipo, LocalDate desde, LocalDate hasta) {
            Long id = tarifarioRepo.nextId();
            FalTarifarioUnidadFaltas t = new FalTarifarioUnidadFaltas(
                    id, tipo, new BigDecimal("100000"), desde, FaltasClockTestSupport.FIXED.now(), "sistema");
            if (hasta != null) t.setFhVigHasta(hasta);
            return t;
        }

        @Test
        @DisplayName("tarifario activo sin solapamiento se guarda correctamente")
        void sin_solapamiento_ok() {
            FalTarifarioUnidadFaltas t1 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1));
            FalTarifarioUnidadFaltas t2 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2022, 1, 1), null);  // contiguo
            tarifarioRepo.save(t1);
            assertThat(tarifarioRepo.save(t2)).isNotNull();
        }

        @Test
        @DisplayName("tarifarios para tipos distintos no conflictan")
        void tipos_distintos_no_conflictan() {
            FalTarifarioUnidadFaltas tSalario = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2020, 1, 1), null);
            FalTarifarioUnidadFaltas tFijo = newTarifario(TipoUnidadFaltas.UNIDAD_FIJA,
                    LocalDate.of(2020, 1, 1), null);
            tarifarioRepo.save(tSalario);
            assertThat(tarifarioRepo.save(tFijo)).isNotNull();
        }

        @Test
        @DisplayName("solapamiento parcial lanza PrecondicionVioladaException")
        void solapamiento_parcial_falla() {
            FalTarifarioUnidadFaltas t1 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1));
            tarifarioRepo.save(t1);

            FalTarifarioUnidadFaltas t2 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2022, 1, 1), null);  // superpone con t1
            assertThatThrownBy(() -> tarifarioRepo.save(t2))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Superposicion");
        }

        @Test
        @DisplayName("rango contenido dentro de otro lanza PrecondicionVioladaException")
        void rango_contenido_falla() {
            FalTarifarioUnidadFaltas t1 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2019, 1, 1), null);  // indefinido
            tarifarioRepo.save(t1);

            FalTarifarioUnidadFaltas t2 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2021, 6, 1), LocalDate.of(2022, 1, 1));  // dentro de t1
            assertThatThrownBy(() -> tarifarioRepo.save(t2))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("tarifario inactivo no genera conflicto")
        void tarifario_inactivo_no_conflicta() {
            FalTarifarioUnidadFaltas t1 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2020, 1, 1), null);
            t1.setSiActiva(false);  // inactivo
            tarifarioRepo.save(t1);

            FalTarifarioUnidadFaltas t2 = newTarifario(TipoUnidadFaltas.SALARIO,
                    LocalDate.of(2020, 1, 1), null);
            assertThat(tarifarioRepo.save(t2)).isNotNull();
        }

        @Test
        @DisplayName("findUltimoVigente detecta multiple vigente e lanza IllegalStateException")
        void find_ultimo_vigente_detecta_invariante_rota() {
            FalTarifarioUnidadFaltas t1 = newTarifario(TipoUnidadFaltas.MONTO,
                    LocalDate.of(2020, 1, 1), null);
            FalTarifarioUnidadFaltas t2 = newTarifario(TipoUnidadFaltas.MONTO,
                    LocalDate.of(2022, 1, 1), null);
            // Forzar solapamiento directo en seed para simular DB rota
            tarifarioRepo.cargarSeed(java.util.Arrays.asList(t1, t2));

            assertThatThrownBy(() -> tarifarioRepo.findUltimoVigente(TipoUnidadFaltas.MONTO, LocalDate.of(2023, 1, 1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invariante rota");
        }
    }

    // ================================================================
    // R1-D.8.8: MedidaPreventiva - version atomica
    // ================================================================

    @Nested
    @DisplayName("R1-D.8.8: MedidaPreventiva - version atomica")
    class MedidaPreventivaVersionAtomicaR1DTest {

        private InMemoryMedidaPreventivaRepository medidaRepo;
        private InMemoryArticuloMedidaPreventivaRepository articuloMedidaRepo;
        private MedidaPreventivaService medidaService;

        @BeforeEach
        void setUp() {
            medidaRepo = new InMemoryMedidaPreventivaRepository();
            articuloMedidaRepo = new InMemoryArticuloMedidaPreventivaRepository();
            medidaService = new MedidaPreventivaService(medidaRepo, articuloMedidaRepo, FaltasClockTestSupport.FIXED);
        }

        @Test
        @DisplayName("crearPrimeraVersion crea medida activa version 1")
        void crear_primera_version() {
            FalMedidaPreventiva m = medidaService.crearPrimeraVersion("MP01", "Decomiso", null, null, null, false, null, "u1");
            assertThat(m.getVersionMedida()).isEqualTo((short) 1);
            assertThat(m.isSiActiva()).isTrue();
        }

        @Test
        @DisplayName("crearNuevaVersion atomicamente desactiva anterior y crea version 2")
        void crear_nueva_version_desactiva_anterior() {
            medidaService.crearPrimeraVersion("MP02", "Retiro", null, null, null, false, null, "u1");
            FalMedidaPreventiva v2 = medidaService.crearNuevaVersion("MP02", "Retiro v2", null, null, null, true, null, "u1");

            assertThat(v2.getVersionMedida()).isEqualTo((short) 2);
            assertThat(v2.isSiActiva()).isTrue();

            Optional<FalMedidaPreventiva> activa = medidaService.buscarActivaPorCodigo("MP02");
            assertThat(activa).isPresent();
            assertThat(activa.get().getVersionMedida()).isEqualTo((short) 2);

            var versiones = medidaService.listarVersiones("MP02");
            long inactivas = versiones.stream().filter(v -> !v.isSiActiva()).count();
            assertThat(inactivas).isEqualTo(1);
        }

        @Test
        @DisplayName("crearNuevaVersionAtomico previene version duplicada (concurrencia simulada)")
        void version_duplicada_falla() {
            medidaService.crearPrimeraVersion("MP03", "Inhibicion", null, null, null, false, null, "u1");

            FalMedidaPreventiva candidata = new FalMedidaPreventiva(
                    medidaRepo.nextId(), "MP03", (short) 2, "Inhibicion v2", FaltasClockTestSupport.FIXED.now(), "u1");

            assertThat(medidaRepo.crearNuevaVersionAtomico(candidata)).isNotNull();

            FalMedidaPreventiva candidataDuplicada = new FalMedidaPreventiva(
                    medidaRepo.nextId(), "MP03", (short) 2, "Inhibicion v2 dup", FaltasClockTestSupport.FIXED.now(), "u2");

            assertThatThrownBy(() -> medidaRepo.crearNuevaVersionAtomico(candidataDuplicada))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Ya existe version");
        }

        @Test
        @DisplayName("solo una version activa por codigo en todo momento")
        void una_sola_version_activa() {
            medidaService.crearPrimeraVersion("MP04", "Sella", null, null, null, false, null, "u1");
            medidaService.crearNuevaVersion("MP04", "Sella v2", null, null, null, false, null, "u1");
            medidaService.crearNuevaVersion("MP04", "Sella v3", null, null, null, false, null, "u1");

            var versiones = medidaService.listarVersiones("MP04");
            long activas = versiones.stream().filter(FalMedidaPreventiva::isSiActiva).count();
            assertThat(activas).isEqualTo(1);
            assertThat(versiones).hasSize(3);
        }
    }

    // ================================================================
    // R1-D.8.10: ActaArticuloInfringido - UK activo
    // ================================================================

    @Nested
    @DisplayName("R1-D.8.10: ActaArticuloInfringido - unicidad activa")
    class ActaArticuloInfringidoUKR1DTest {

        private InMemoryActaArticuloInfringidoRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryActaArticuloInfringidoRepository();
        }

        private FalActaArticuloInfringido nuevoActivo(Long actaId, Long articuloId) {
            Long id = repo.nextId();
            return new FalActaArticuloInfringido(id, actaId, 1L, articuloId, FaltasClockTestSupport.FIXED.now(), "u1");
        }

        @Test
        @DisplayName("un registro activo se guarda correctamente")
        void uno_activo_ok() {
            assertThat(repo.save(nuevoActivo(1L, 100L))).isNotNull();
        }

        @Test
        @DisplayName("dos registros activos con mismo (actaId, articuloId) lanza PrecondicionVioladaException")
        void dos_activos_mismo_par_falla() {
            repo.save(nuevoActivo(1L, 100L));
            assertThatThrownBy(() -> repo.save(nuevoActivo(1L, 100L)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Ya existe articulo activo");
        }

        @Test
        @DisplayName("articulos activos para diferente actaId no conflictan")
        void diferente_acta_no_conflicta() {
            repo.save(nuevoActivo(1L, 100L));
            assertThat(repo.save(nuevoActivo(2L, 100L))).isNotNull();
        }

        @Test
        @DisplayName("articulos activos para diferente articuloId no conflictan")
        void diferente_articulo_no_conflicta() {
            repo.save(nuevoActivo(1L, 100L));
            assertThat(repo.save(nuevoActivo(1L, 200L))).isNotNull();
        }

        @Test
        @DisplayName("dar de baja y agregar nuevo activo para mismo par: permitido")
        void baja_y_nuevo_activo_permitido() {
            FalActaArticuloInfringido a1 = repo.save(nuevoActivo(1L, 100L));
            a1.darDeBaja(MotivoBajaArticuloInfringido.CORRECCION_IMPUTACION, FaltasClockTestSupport.FIXED.now(), "u1");
            repo.save(a1);  // dar de baja
            assertThat(repo.save(nuevoActivo(1L, 100L))).isNotNull();
        }
    }

    // ================================================================
    // R1-D.8.9: ArticuloMedidaPreventiva - no reactivacion silenciosa
    // ================================================================

    @Nested
    @DisplayName("R1-D.8.9: ArticuloMedidaPreventiva - no reactivacion silenciosa via save")
    class ArticuloMedidaReactivacionR1DTest {

        private InMemoryArticuloMedidaPreventivaRepository repo;

        @BeforeEach
        void setUp() {
            repo = new InMemoryArticuloMedidaPreventivaRepository();
        }

        @Test
        @DisplayName("guardar relacion nueva activa es permitido")
        void nueva_activa_ok() {
            ArticuloMedidaPreventivaId pk = new ArticuloMedidaPreventivaId(1L, 10L);
            FalArticuloMedidaPreventiva rel = new FalArticuloMedidaPreventiva(pk, true, FaltasClockTestSupport.FIXED.now(), "u1");
            assertThat(repo.save(rel)).isNotNull();
        }

        @Test
        @DisplayName("desactivar una relacion es permitido")
        void desactivar_ok() {
            ArticuloMedidaPreventivaId pk = new ArticuloMedidaPreventivaId(1L, 10L);
            FalArticuloMedidaPreventiva rel = new FalArticuloMedidaPreventiva(pk, true, FaltasClockTestSupport.FIXED.now(), "u1");
            rel = repo.save(rel);
            rel.setSiActiva(false);
            assertThat(repo.save(rel)).isNotNull();
            assertThat(repo.existsActiva(pk)).isFalse();
        }

        @Test
        @DisplayName("reactivar una relacion via save() lanza PrecondicionVioladaException")
        void reactivacion_via_save_falla() {
            ArticuloMedidaPreventivaId pk = new ArticuloMedidaPreventivaId(1L, 10L);
            FalArticuloMedidaPreventiva rel = new FalArticuloMedidaPreventiva(pk, true, FaltasClockTestSupport.FIXED.now(), "u1");
            rel = repo.save(rel);
            rel.setSiActiva(false);
            repo.save(rel);

            rel.setSiActiva(true);  // intento de reactivacion
            FalArticuloMedidaPreventiva finalRel = rel;
            assertThatThrownBy(() -> repo.save(finalRel))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("Reactivacion");
        }
    }
}
