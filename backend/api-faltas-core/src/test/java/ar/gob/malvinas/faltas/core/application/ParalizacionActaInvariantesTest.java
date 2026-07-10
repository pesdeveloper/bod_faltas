package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.ParalizarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReactivarActaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ParalizacionActaService;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11G: ParalizacionActaInvariantesTest")
class ParalizacionActaInvariantesTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryActaParalizacionRepository paralizacionRepo;
    private InMemoryObservacionRepository obsRepo;
    private ParalizacionActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        paralizacionRepo = new InMemoryActaParalizacionRepository();
        obsRepo = new InMemoryObservacionRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(eventoRepo,
                new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED);
        service = new ParalizacionActaService(actaRepo, eventoRepo, snapshotRepo,
                paralizacionRepo, obsRepo, recalc, FaltasClockTestSupport.FIXED);
    }

    private FalActa crearActaActiva(BloqueActual bloque) {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(), "Calle 1", null, null, null,
                ResultadoFirmaInfractor.FIRMADA, null, FaltasClockTestSupport.FIXED.now(), "TEST");
        acta.setBloqueActual(bloque);
        return actaRepo.guardar(acta);
    }

    @Nested
    @DisplayName("paralizar - invariantes")
    class ParalizarInvariantes {

        @Test
        @DisplayName("paralizar desde bloque CAPT crea ciclo activo y evento ACTPAR")
        void paralizar_desde_capt() {
            FalActa acta = crearActaActiva(BloqueActual.CAPT);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));

            Optional<FalActaParalizacion> ciclo = paralizacionRepo.buscarActivaPorActa(acta.getId());
            assertThat(ciclo).isPresent();
            assertThat(ciclo.get().getMotivoParalizacion()).isEqualTo(MotivoParalizacion.ESPERA_DOCUMENTAL);
            assertThat(ciclo.get().isSiActiva()).isTrue();

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(acta.getId());
            assertThat(eventos).anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACTPAR);
        }

        @Test
        @DisplayName("paralizar desde bloque ANAL crea ciclo activo")
        void paralizar_desde_anal() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("paralizar desde bloque NOTI crea ciclo activo")
        void paralizar_desde_noti() {
            FalActa acta = crearActaActiva(BloqueActual.NOTI);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("paralizar desde bloque ENRI crea ciclo activo")
        void paralizar_desde_enri() {
            FalActa acta = crearActaActiva(BloqueActual.ENRI);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("motivo OTRO sin observacion rechazado")
        void otro_sin_observacion_rechazado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            assertThatThrownBy(() -> service.paralizar(
                    new ParalizarActaCommand(acta.getId(), MotivoParalizacion.OTRO, null, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("OTRO");
        }

        @Test
        @DisplayName("motivo OTRO con observacion crea observacion vinculada")
        void otro_con_observacion_crea_obs() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.OTRO,
                    "motivo especifico", "U"));
            List<FalObservacion> obs = obsRepo.listarPorEntidad(EntidadTipoObservada.PARALIZACION,
                    paralizacionRepo.buscarActivaPorActa(acta.getId()).get().getId());
            assertThat(obs).hasSize(1);
        }

        @Test
        @DisplayName("acta cerrada no puede paralizarse")
        void cerrada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.CERR);
            acta = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.paralizar(
                    new ParalizarActaCommand(id, MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("acta anulada no puede paralizarse")
        void anulada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.CAPT);
            acta = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ANULADA);
            actaRepo.guardar(acta);
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.paralizar(
                    new ParalizarActaCommand(id, MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("acta archivada no puede paralizarse")
        void archivada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.ARCH);
            acta = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.ARCHIVADA);
            actaRepo.guardar(acta);
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.paralizar(
                    new ParalizarActaCommand(id, MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("acta ya paralizada no puede paralizarse de nuevo")
        void ya_paralizada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.paralizar(
                    new ParalizarActaCommand(id, MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("bloque se preserva tras paralizacion")
        void bloque_preservado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(leida.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.PARALIZADA);
        }

        @Test
        @DisplayName("snapshot bandeja=PARALIZADAS tras paralizar")
        void snapshot_paralizadas() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(acta.getId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PARALIZADAS);
        }
    }

    @Nested
    @DisplayName("reactivar - invariantes")
    class ReactivarInvariantes {

        @Test
        @DisplayName("reactivar cierra ciclo activo y restaura ACTIVA")
        void reactivar_cierra_ciclo() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            service.reactivar(new ReactivarActaCommand(acta.getId()));

            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isEmpty();
            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("reactivar registra evento ACTREA")
        void reactivar_evento_actrea() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            service.reactivar(new ReactivarActaCommand(acta.getId()));
            assertThat(eventoRepo.buscarPorActa(acta.getId()))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACTREA);
        }

        @Test
        @DisplayName("doble reactivacion rechazada")
        void doble_reactivacion_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            service.reactivar(new ReactivarActaCommand(acta.getId()));
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.reactivar(new ReactivarActaCommand(id)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("multiples ciclos historicos por acta")
        void multiples_ciclos_historicos() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            service.reactivar(new ReactivarActaCommand(acta.getId()));
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.OTRO, "segunda", "U"));
            service.reactivar(new ReactivarActaCommand(acta.getId()));

            List<FalActaParalizacion> historico = paralizacionRepo.listarHistoricoPorActa(acta.getId());
            assertThat(historico).hasSize(2);
            assertThat(historico).allMatch(p -> !p.isSiActiva());
        }

        @Test
        @DisplayName("reactivar sin paralizacion previa rechazado")
        void sin_paralizacion_previa() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            assertThatThrownBy(() -> service.reactivar(new ReactivarActaCommand(acta.getId())))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("Concurrencia")
    class ConcurrenciaTests {

        @Test
        @DisplayName("dos paralizaciones simultaneas: solo una gana")
        void dos_paralizaciones_simultaneas_solo_una_gana() throws InterruptedException {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);

            Runnable tarea = () -> {
                try {
                    start.await();
                    service.paralizar(new ParalizarActaCommand(acta.getId(),
                            MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    errores.incrementAndGet();
                }
            };

            Thread t1 = new Thread(tarea);
            Thread t2 = new Thread(tarea);
            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(errores.get()).isEqualTo(1);
            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isPresent();
        }

        @Test
        @DisplayName("dos reactivaciones simultaneas: solo una gana")
        void dos_reactivaciones_simultaneas() throws InterruptedException {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.paralizar(new ParalizarActaCommand(acta.getId(), MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));

            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);

            Runnable tarea = () -> {
                try {
                    start.await();
                    service.reactivar(new ReactivarActaCommand(acta.getId()));
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    errores.incrementAndGet();
                }
            };

            Thread t1 = new Thread(tarea);
            Thread t2 = new Thread(tarea);
            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(exitos.get()).isEqualTo(1);
            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isEmpty();
        }
    }
}