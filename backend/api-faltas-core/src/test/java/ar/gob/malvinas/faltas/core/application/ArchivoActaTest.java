package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.ArchivarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ParalizarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.ReingresarDesdeArchivoCommand;
import ar.gob.malvinas.faltas.core.application.service.ArchivoActaService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11G: ArchivoActaTest")
class ArchivoActaTest {

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryActaArchivoRepository archivoRepo;
    private InMemoryActaParalizacionRepository paralizacionRepo;
    private InMemoryMotivoArchivoRepository motivoRepo;
    private InMemoryObservacionRepository obsRepo;
    private ArchivoActaService service;
    private ParalizacionActaService paralizacionService;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        archivoRepo = new InMemoryActaArchivoRepository();
        paralizacionRepo = new InMemoryActaParalizacionRepository();
        motivoRepo = new InMemoryMotivoArchivoRepository();
        obsRepo = new InMemoryObservacionRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(eventoRepo,
                new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo);
        service = new ArchivoActaService(actaRepo, eventoRepo, snapshotRepo, archivoRepo,
                paralizacionRepo, motivoRepo, obsRepo, recalc, FaltasClockTestSupport.FIXED);
        paralizacionService = new ParalizacionActaService(actaRepo, eventoRepo, snapshotRepo,
                paralizacionRepo, obsRepo, recalc, FaltasClockTestSupport.FIXED);
        sembrarMotivos();
    }

    private long idMotivoConReingreso;
    private long idMotivoSinReingreso;
    private long idMotivoNulidad;
    private long idMotivoReqObs;

    private void sembrarMotivos() {
        idMotivoConReingreso = crearMotivo("M_CON_REINGRSO", false, true, false);
        idMotivoSinReingreso = crearMotivo("M_SIN_REINGRSO", false, false, false);
        idMotivoNulidad = crearMotivo("M_NULIDAD", true, false, false);
        idMotivoReqObs = crearMotivo("M_REQ_OBS", false, true, true);
    }

    private long crearMotivo(String cod, boolean nulidad, boolean reingreso, boolean reqObs) {
        Long id = motivoRepo.nextId();
        FalMotivoArchivo m = new FalMotivoArchivo(id, cod, "Nombre " + cod, null,
                nulidad, reingreso, reqObs, true, FaltasClockTestSupport.FIXED.now(), "S");
        return motivoRepo.guardar(m).getId();
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
    @DisplayName("archivar - invariantes")
    class ArchivarInvariantes {

        @Test
        @DisplayName("archivar crea ciclo activo y evento ACTARCH")
        void archivar_crea_ciclo() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "motivo obs"));

            assertThat(archivoRepo.buscarActivoPorActa(acta.getId())).isPresent();
            assertThat(eventoRepo.buscarPorActa(acta.getId()))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACTARCH);
        }

        @Test
        @DisplayName("archivar copia snapshots del motivo (no reinterpretan el motivo actual)")
        void archivar_copia_snapshot_motivo() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));

            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            assertThat(ciclo.isSiPermiteReingresoSnapshot()).isTrue();
            assertThat(ciclo.isSiNulidadSnapshot()).isFalse();
        }

        @Test
        @DisplayName("archivar nulidad copia siNulidad=true")
        void archivar_nulidad_snapshot() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoNulidad, null));

            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            assertThat(ciclo.isSiNulidadSnapshot()).isTrue();
        }

        @Test
        @DisplayName("archivar captura bloque origen")
        void archivar_captura_origen() {
            FalActa acta = crearActaActiva(BloqueActual.NOTI);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));

            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            assertThat(ciclo.getBloqueOrigen()).isEqualTo(BloqueActual.NOTI);
        }

        @Test
        @DisplayName("motivo que requiere observacion sin texto rechazado")
        void req_obs_sin_texto_rechazado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            assertThatThrownBy(() -> service.archivar(
                    new ArchivarActaCommand(acta.getId(), idMotivoReqObs, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("acta ya archivada no puede archivarse de nuevo")
        void ya_archivada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.archivar(
                    new ArchivarActaCommand(id, idMotivoConReingreso, "obs")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("acta cerrada no puede archivarse")
        void cerrada_rechazada() {
            FalActa acta = crearActaActiva(BloqueActual.CERR);
            acta = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.CERRADA);
            actaRepo.guardar(acta);
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.archivar(
                    new ArchivarActaCommand(id, idMotivoConReingreso, "obs")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("archivar acta paralizada cierra el ciclo de paralizacion")
        void archivar_paralizada_cierra_paralizacion() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            paralizacionService.paralizar(new ParalizarActaCommand(acta.getId(),
                    MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U"));
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));

            assertThat(paralizacionRepo.buscarActivaPorActa(acta.getId())).isEmpty();
            List<FalActaParalizacion> historico = paralizacionRepo.listarHistoricoPorActa(acta.getId());
            assertThat(historico).hasSize(1);
            assertThat(historico.get(0).getFhReactivacion()).isNotNull();
        }

        @Test
        @DisplayName("acta pasa a bloque ARCH y situacion ARCHIVADA")
        void bloque_y_situacion_archivada() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getBloqueActual()).isEqualTo(BloqueActual.ARCH);
            assertThat(leida.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ARCHIVADA);
        }

        @Test
        @DisplayName("snapshot bandeja=ARCHIVO tras archivar")
        void snapshot_archivo() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaSnapshot snap = snapshotRepo.buscarPorActa(acta.getId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.ARCHIVO);
        }

        @Test
        @DisplayName("no asigna resultado final automaticamente")
        void no_cierre_automatico() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getEstadoProcesal()).isEqualTo(EstadoProcesalActa.EN_TRAMITE);
        }

        @Test
        @DisplayName("motivo inactivo no puede usarse en archivo")
        void motivo_inactivo_rechazado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            FalMotivoArchivo m = motivoRepo.buscarPorId(idMotivoConReingreso).orElseThrow();
            m.setSiActivo(false);
            motivoRepo.actualizarAtomicamente(m);
            assertThatThrownBy(() -> service.archivar(
                    new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }
    }

    @Nested
    @DisplayName("reingresar - invariantes")
    class ReingresarInvariantes {

        @Test
        @DisplayName("reingresar restaura exactamente el estado de origen")
        void reingresar_restaura_origen() {
            FalActa acta = crearActaActiva(BloqueActual.NOTI);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));

            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U"));

            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getBloqueActual()).isEqualTo(BloqueActual.NOTI);
            assertThat(leida.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("reingresar registra evento ACTREI")
        void reingresar_evento_actrei() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U"));
            assertThat(eventoRepo.buscarPorActa(acta.getId()))
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACTREI);
        }

        @Test
        @DisplayName("reingresar cierra ciclo activo")
        void reingresar_cierra_ciclo() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U"));
            assertThat(archivoRepo.buscarActivoPorActa(acta.getId())).isEmpty();
        }

        @Test
        @DisplayName("reingresar sin reingreso permitido rechazado")
        void sin_reingreso_rechazado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoSinReingreso, null));
            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            assertThatThrownBy(() -> service.reingresar(
                    new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("segundo reingreso rechazado (no hay archivo activo)")
        void segundo_reingreso_rechazado() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U"));
            final Long id = acta.getId();
            assertThatThrownBy(() -> service.reingresar(new ReingresarDesdeArchivoCommand(id, 0, "U")))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("dos ciclos historicos de archivo son posibles")
        void dos_ciclos_historicos() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs 1"));
            FalActaArchivo c1 = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), c1.getVersionRow(), "U"));
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs 2"));
            FalActaArchivo c2 = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), c2.getVersionRow(), "U"));

            assertThat(archivoRepo.listarHistoricoPorActa(acta.getId())).hasSize(2);
        }

        @Test
        @DisplayName("motivo cambiado despues de archivar no altera snapshot")
        void motivo_cambiado_no_altera_snapshot() {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaArchivo cicloAntes = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            boolean reingresoOriginal = cicloAntes.isSiPermiteReingresoSnapshot();

            FalMotivoArchivo motivo = motivoRepo.buscarPorId(idMotivoConReingreso).orElseThrow();
            motivo.setSiPermiteReingreso(false);
            motivoRepo.actualizarAtomicamente(motivo);

            FalActaArchivo cicloLuego = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();
            assertThat(cicloLuego.isSiPermiteReingresoSnapshot()).isEqualTo(reingresoOriginal);
        }
    }

    @Nested
    @DisplayName("Concurrencia")
    class ConcurrenciaTests {

        @Test
        @DisplayName("dos archivos simultaneos: solo uno gana")
        void dos_archivos_simultaneos() throws InterruptedException {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);

            Runnable tarea = () -> {
                try {
                    start.await();
                    service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
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
        }

        @Test
        @DisplayName("dos reingresos simultaneos: solo uno gana")
        void dos_reingresos_simultaneos() throws InterruptedException {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            service.archivar(new ArchivarActaCommand(acta.getId(), idMotivoConReingreso, "obs"));
            FalActaArchivo ciclo = archivoRepo.buscarActivoPorActa(acta.getId()).orElseThrow();

            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);
            AtomicInteger errores = new AtomicInteger(0);

            Runnable tarea = () -> {
                try {
                    start.await();
                    service.reingresar(new ReingresarDesdeArchivoCommand(acta.getId(), ciclo.getVersionRow(), "U"));
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
            assertThat(archivoRepo.buscarActivoPorActa(acta.getId())).isEmpty();
        }

        @Test
        @DisplayName("paralizar compite con archivar: ambos coherentes")
        void paralizar_compite_con_archivar() throws InterruptedException {
            FalActa acta = crearActaActiva(BloqueActual.ANAL);
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger exitos = new AtomicInteger(0);

            Thread t1 = new Thread(() -> {
                try { start.await(); paralizacionService.paralizar(new ParalizarActaCommand(acta.getId(),
                        MotivoParalizacion.ESPERA_DOCUMENTAL, null, "U")); exitos.incrementAndGet(); }
                catch (Exception ignored) { exitos.incrementAndGet(); }
            });
            Thread t2 = new Thread(() -> {
                try { start.await(); service.archivar(new ArchivarActaCommand(acta.getId(),
                        idMotivoConReingreso, "obs")); exitos.incrementAndGet(); }
                catch (Exception ignored) { exitos.incrementAndGet(); }
            });

            t1.start(); t2.start();
            start.countDown();
            t1.join(); t2.join();

            assertThat(exitos.get()).isEqualTo(2);
            FalActa leida = actaRepo.buscarPorId(acta.getId()).orElseThrow();
            assertThat(leida.getSituacionAdministrativa()).isIn(
                    SituacionAdministrativaActa.PARALIZADA, SituacionAdministrativaActa.ARCHIVADA);
        }
    }
}
