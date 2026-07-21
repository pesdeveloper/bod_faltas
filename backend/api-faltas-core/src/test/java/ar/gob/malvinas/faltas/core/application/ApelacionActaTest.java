package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.application.command.CompletarCapturaCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloAbsolutorioCommand;
import ar.gob.malvinas.faltas.core.application.command.DictarFalloCondenatorioCommand;
import ar.gob.malvinas.faltas.core.application.command.EnriquecerActaCommand;
import ar.gob.malvinas.faltas.core.application.command.EnviarNotificacionCommand;
import ar.gob.malvinas.faltas.core.domain.enums.CanalNotificacion;
import ar.gob.malvinas.faltas.core.application.command.FirmarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.GenerarDocumentoCommand;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.application.command.RegistrarApelacionCommand;
import ar.gob.malvinas.faltas.core.application.command.RegistrarNotificacionPositivaCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionAceptaAbsuelveCommand;
import ar.gob.malvinas.faltas.core.application.command.ResolverApelacionRechazadaCommand;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.ApelacionActaService;
import ar.gob.malvinas.faltas.core.application.service.DocumentoService;
import ar.gob.malvinas.faltas.core.application.service.FalloActaService;
import ar.gob.malvinas.faltas.core.application.service.NoOpBloqueantesMaterialesChecker;
import ar.gob.malvinas.faltas.core.application.service.NotificacionService;
import ar.gob.malvinas.faltas.core.domain.enums.AccionPendiente;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoApelacionActa;

import ar.gob.malvinas.faltas.core.domain.enums.ResultadoResolucionApelacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFinalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaApelacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import ar.gob.malvinas.faltas.core.repository.ActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.ActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.ApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.DocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.FalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.NotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.PagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.PagoCondenaRepository;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del Slice 3B + 3C: apelacion presentada y resolucion de apelacion.
 *
 * Slice 3B: fallo condenatorio notificado -> registrar apelacion (APEPRE).
 * Slice 3C: apelacion PRESENTADA -> rechazar (APERAZ) o aceptar-absuelve (APEABS).
 *
 * APELAC no existe. Solo APEPRE, APERAZ y APEABS son productivos.
 */
@DisplayName("Slice 3B+3C: Apelacion presentada y resolucion de apelacion")
class ApelacionActaTest {

    private ActaRepository actaRepo;
    private ActaEventoRepository eventoRepo;
    private ActaSnapshotRepository snapshotRepo;
    private DocumentoRepository docRepo;
    private DocumentoFirmaRepository firmaRepo;
    private NotificacionRepository notifRepo;
    private PagoVoluntarioRepository pagoRepo;
    private FalloActaRepository falloRepo;
    private ApelacionActaRepository apelacionRepo;

    private ActaService actaService;
    private DocumentoService docService;
    private NotificacionService notifService;
    private final ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository intentoRepo = new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionIntentoRepository();
    private FalloActaService falloService;
    private ApelacionActaService apelacionService;

    @BeforeEach
    void setUp() {
        ActorContextHolder.set(new ActorContext("test-actor"));
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        docRepo = new InMemoryDocumentoRepository();
        firmaRepo = new InMemoryDocumentoFirmaRepository();
        notifRepo = new InMemoryNotificacionRepository();
        pagoRepo = new InMemoryPagoVoluntarioRepository();
        falloRepo = new InMemoryFalloActaRepository();
        apelacionRepo = new InMemoryApelacionActaRepository();

        PagoCondenaRepository pagoCondenaRepo = new InMemoryPagoCondenaRepository();
        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, pagoCondenaRepo, FaltasClockTestSupport.FIXED, snapshotRepo);

        actaService = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);
        docService = new DocumentoService(
                actaRepo, docRepo, firmaRepo, eventoRepo, snapshotRepo, recalc, falloRepo,

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoPlantillaRepository(),

                        new ar.gob.malvinas.faltas.core.application.service.TalonarioService(new ar.gob.malvinas.faltas.core.repository.memory.InMemoryTalonarioRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(), new ar.gob.malvinas.faltas.core.repository.memory.InMemoryInspectorRepository(), FaltasClockTestSupport.FIXED),

                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDependenciaRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoFirmaReqRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFirmanteRepository(),
                new InMemoryNotificacionRepository(), FaltasClockTestSupport.FIXED);
        notifService = new NotificacionService(
                actaRepo, docRepo, notifRepo, eventoRepo, snapshotRepo, recalc,
                falloRepo, new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED, intentoRepo, new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPersonaDomicilioRepository(),
                ar.gob.malvinas.faltas.core.support.PlazosTestSupport.conCalendarioVacio(FaltasClockTestSupport.FIXED));
        falloService = new FalloActaService(
                actaRepo, eventoRepo, snapshotRepo, docRepo,
                falloRepo, pagoRepo, recalc, FaltasClockTestSupport.FIXED);
        apelacionService = new ApelacionActaService(
                actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo, recalc,
                new NoOpBloqueantesMaterialesChecker(), FaltasClockTestSupport.FIXED);
    }

    @AfterEach
    void tearDown() { ActorContextHolder.clear(); }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LabrarActaCommand cmdLabrar(String doc) {
        return new LabrarActaCommand(
                "TRANSITO", "DEP-001", "INS-001",
                FaltasClockTestSupport.FIXED.now().toLocalDate(), "Av. Argentina 123", "San Martin 456",
                null, null, null, "Infractor Test", doc,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, null);
    }

    private Long llegarAAnalisis(String doc) {
        Long actaId = actaService.labrar(cmdLabrar(doc)).idActa();
        actaService.completarCaptura(new CompletarCapturaCommand(actaId, null));
        actaService.enriquecer(new EnriquecerActaCommand(actaId, "enriquecido"));
        String idDoc = docService.generarDocumento(
                new GenerarDocumentoCommand(actaId, TipoDocu.ACTA_INFRACCION))
                .idEntidadAfectada();
        docService.firmarDocumento(new FirmarDocumentoCommand(Long.parseLong(idDoc), "firmante1", "DIGITAL", null));
        String idNotif = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, Long.parseLong(idDoc), CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));
        return actaId;
    }

    private Long crearActaConFalloCondenatorioNotificado() {
        Long actaId = llegarAAnalisis("12345678");
        falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                actaId, new BigDecimal("1500.00"), "Fundamentos condenatorios", null));
        Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
        docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
        String idNotifFallo = notifService.enviarNotificacion(
                new EnviarNotificacionCommand(actaId, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                .idEntidadAfectada();
        notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotifFallo), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotifFallo)), null, "test-actor"));
        return actaId;
    }

    private Long crearActaConApelacionPresentada() {
        Long actaId = crearActaConFalloCondenatorioNotificado();
        apelacionService.registrarApelacion(
                new RegistrarApelacionCommand(actaId, "Infractor Juan", "Fundamentos apelacion", null));
        return actaId;
    }

    // =========================================================================
    // Correccion de eventos de apelacion (Slice 3B + 3C)
    // =========================================================================

    @Nested
    @DisplayName("Correccion eventos de apelacion")
    class CorreccionEventoApelacion {

        @Test
        @DisplayName("APEPRE existe y resuelve como APELACION_PRESENTADA")
        void apepre_existe_y_resuelve() {
            TipoEventoActa apepre = TipoEventoActa.deCodigo("APEPRE");
            assertThat(apepre).isEqualTo(TipoEventoActa.APEPRE);
            assertThat(apepre.descripcion()).containsIgnoringCase("Apelacion presentada");
        }

        @Test
        @DisplayName("APELAC no existe: deCodigo lanza excepcion")
        void apelac_no_existe() {
            assertThatThrownBy(() -> TipoEventoActa.deCodigo("APELAC"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("APELAC");
        }

        @Test
        @DisplayName("APERAZ existe y resuelve como apelacion rechazada")
        void aperaz_existe_y_resuelve() {
            TipoEventoActa aperaz = TipoEventoActa.deCodigo("APERAZ");
            assertThat(aperaz).isEqualTo(TipoEventoActa.APERAZ);
            assertThat(aperaz.descripcion()).containsIgnoringCase("rechazada");
        }

        @Test
        @DisplayName("APEABS existe y resuelve como apelacion aceptada que absuelve")
        void apeabs_existe_y_resuelve() {
            TipoEventoActa apeabs = TipoEventoActa.deCodigo("APEABS");
            assertThat(apeabs).isEqualTo(TipoEventoActa.APEABS);
            assertThat(apeabs.descripcion()).containsIgnoringCase("absolucion");
        }
    }

    // =========================================================================
    // Slice 3B: Casos felices - Registrar apelacion
    // =========================================================================

    @Nested
    @DisplayName("Slice 3B: Registrar apelacion")
    class RegistrarApelacion {

        @Test
        @DisplayName("Registrar apelacion: crea apelacion PRESENTADA, registra APEPRE, snapshot CON_APELACION")
        void registrar_apelacion_crea_entidad_y_evento() {
            Long actaId = crearActaConFalloCondenatorioNotificado();

            ComandoResultado r = apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor Juan", "Fundamentos apelacion", null));

            assertThat(r.idActa()).isEqualTo(actaId);
            assertThat(r.tipoEvento()).isEqualTo("APEPRE");

            Optional<FalActaApelacion> apelOpt = apelacionRepo.buscarActiva(actaId);
            assertThat(apelOpt).isPresent();
            FalActaApelacion apelacion = apelOpt.get();
            assertThat(apelacion.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.PRESENTADA);
            assertThat(apelacion.getPresentante()).isEqualTo("Infractor Juan");
            assertThat(apelacion.isSiActiva()).isTrue();

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .contains(TipoEventoActa.APEPRE);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.CIERRA);

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).get();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CON_APELACION);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.RESOLVER_APELACION);
        }

        @Test
        @DisplayName("Timeline: conserva eventos previos y agrega APEPRE al final")
        void timeline_apepre_al_final() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", null, null));

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();

            assertThat(tipos).containsSequence(
                    TipoEventoActa.ACTLAB, TipoEventoActa.ACTCAP, TipoEventoActa.ACTENR);
            assertThat(tipos).contains(TipoEventoActa.FALCON, TipoEventoActa.NOTPOS);
            assertThat(tipos.get(tipos.size() - 1)).isEqualTo(TipoEventoActa.APEPRE);
            assertThat(tipos).doesNotContain(TipoEventoActa.APERAZ, TipoEventoActa.APEABS);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // Slice 3C: Casos felices - Rechazar apelacion (APERAZ)
    // =========================================================================

    @Nested
    @DisplayName("Slice 3C: Rechazar apelacion (APERAZ)")
    class RechazarApelacion {

        @Test
        @DisplayName("Rechazar apelacion: registra APERAZ, estado RESUELTA+RECHAZADA, no cierra, snapshot DECLARAR_CONDENA_FIRME")
        void rechazar_apelacion_caso_feliz() {
            Long actaId = crearActaConApelacionPresentada();

            ComandoResultado r = apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos rechazo", "Obs rechazo"));

            assertThat(r.tipoEvento()).isEqualTo("APERAZ");
            assertThat(r.idActa()).isEqualTo(actaId);

            Optional<FalActaApelacion> apelOpt = apelacionRepo.buscarUltima(actaId);
            assertThat(apelOpt).isPresent();
            FalActaApelacion apelacion = apelOpt.get();
            assertThat(apelacion.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.RESUELTA);
            assertThat(apelacion.getResultadoResolucion()).isEqualTo(ResultadoResolucionApelacion.RECHAZADA);
            assertThat(apelacion.isSiActiva()).isFalse();
            assertThat(apelacion.getFundamentosResolucion()).isEqualTo("Fundamentos rechazo");
            assertThat(apelacion.getFechaResolucion()).isNotNull();

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APERAZ);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
            assertThat(tipos).doesNotContain(TipoEventoActa.APEABS);

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).get();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.DECLARAR_CONDENA_FIRME);
        }

        @Test
        @DisplayName("Rechazar apelacion: no registra CIERRA")
        void rechazar_no_registra_cierra() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null));

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("Rechazar apelacion: no genera CONDENA_FIRME (resultadoFinal = SIN_RESULTADO_FINAL)")
        void rechazar_no_genera_condena_firme() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null));

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        }

        @Test
        @DisplayName("Timeline: APEPRE -> APERAZ, sin APEABS ni CIERRA")
        void timeline_aperaz_correcto() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null));

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();

            assertThat(tipos).contains(TipoEventoActa.APEPRE);
            int idxApepre = tipos.indexOf(TipoEventoActa.APEPRE);
            int idxAperaz = tipos.indexOf(TipoEventoActa.APERAZ);
            assertThat(idxAperaz).isGreaterThan(idxApepre);
            assertThat(tipos).doesNotContain(TipoEventoActa.APEABS, TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // Slice 3C: Casos felices - Aceptar apelacion que absuelve (APEABS)
    // =========================================================================

    @Nested
    @DisplayName("Slice 3C: Aceptar apelacion que absuelve (APEABS)")
    class AceptarApelacionAbsuelve {

        @Test
        @DisplayName("Aceptar apelacion sin bloqueantes: APEABS, ABSUELTO, CIERRA, snapshot CERRADAS")
        void aceptar_sin_bloqueantes_cierra() {
            Long actaId = crearActaConApelacionPresentada();

            ComandoResultado r = apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Fundamentos absolucion", null));

            assertThat(r.tipoEvento()).isEqualTo("APEABS");

            Optional<FalActaApelacion> apelOpt = apelacionRepo.buscarUltima(actaId);
            assertThat(apelOpt).isPresent();
            FalActaApelacion apelacion = apelOpt.get();
            assertThat(apelacion.getEstadoApelacion()).isEqualTo(EstadoApelacionActa.ACEPTADA_ABSUELVE);
            assertThat(apelacion.isSiActiva()).isFalse();
            assertThat(apelacion.getFundamentosResolucion()).isEqualTo("Fundamentos absolucion");
            assertThat(apelacion.getFechaResolucion()).isNotNull();

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APEABS, TipoEventoActa.CIERRA);
            int idxApeabs = tipos.indexOf(TipoEventoActa.APEABS);
            int idxCierra = tipos.lastIndexOf(TipoEventoActa.CIERRA);
            assertThat(idxCierra).isGreaterThan(idxApeabs);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).get();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Aceptar apelacion con bloqueantes: APEABS, ABSUELTO, no CIERRA, snapshot PENDIENTE_ANALISIS")
        void aceptar_con_bloqueantes_no_cierra() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            ApelacionActaService serviceConBloqueantes = new ApelacionActaService(
                    actaRepo, falloRepo, apelacionRepo, eventoRepo, snapshotRepo,
                    new SnapshotRecalculador(eventoRepo, docRepo, notifRepo, pagoRepo, falloRepo, apelacionRepo, new InMemoryPagoCondenaRepository(), FaltasClockTestSupport.FIXED, snapshotRepo),
                    id -> true, FaltasClockTestSupport.FIXED);
            serviceConBloqueantes.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", "Fundamentos", null));

            ComandoResultado r = serviceConBloqueantes.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Absolucion", null));

            assertThat(r.tipoEvento()).isEqualTo("APEABS");

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);

            List<TipoEventoActa> tipos = eventoRepo.buscarPorActa(actaId).stream()
                    .map(FalActaEvento::tipoEvt).toList();
            assertThat(tipos).contains(TipoEventoActa.APEABS);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            FalActaSnapshot snap = snapshotRepo.buscarPorActa(actaId).get();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);
            assertThat(snap.getAccionPendiente()).isEqualTo(AccionPendiente.NINGUNA);
        }

        @Test
        @DisplayName("Aceptar apelacion: no genera CONDENA_FIRME")
        void aceptar_no_genera_condena_firme() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Fundamentos", null));

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getResultadoFinal()).isNotEqualTo(ResultadoFinalActa.FALLO_CONDENATORIO_PAGADO);
        }
    }

    // =========================================================================
    // Slice 3C: Casos invalidos - Rechazar apelacion
    // =========================================================================

    @Nested
    @DisplayName("Slice 3C: Casos invalidos - resolver apelacion")
    class CasosInvalidosResolucion {

        @Test
        @DisplayName("No permitir rechazar si no existe apelacion activa")
        void rechazar_sin_apelacion_activa() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            assertThatThrownBy(() -> apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion activa");
        }

        @Test
        @DisplayName("No permitir aceptar si no existe apelacion activa")
        void aceptar_sin_apelacion_activa() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            assertThatThrownBy(() -> apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Fundamentos", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion activa");
        }

        @Test
        @DisplayName("No permitir resolver apelacion dos veces (rechazar segunda vez falla)")
        void no_resolver_apelacion_dos_veces() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Primera vez", null));

            assertThatThrownBy(() -> apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Segunda vez", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("No permitir resolver si el acta esta cerrada")
        void rechazar_acta_cerrada() {
            Long actaId = crearActaConApelacionPresentada();
            // Forzar cierre via aceptar-absuelve sin bloqueantes
            apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Absolucion", null));

            // El acta ahora esta cerrada; registrar nueva apelacion falla
            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("No permitir rechazar apelacion si el acta esta paralizada")
        void rechazar_acta_paralizada() {
            Long actaId = crearActaConApelacionPresentada();
            FalActa acta = actaRepo.buscarPorId(actaId).get();
            acta.setSituacionAdministrativa(SituacionAdministrativaActa.PARALIZADA);
            actaRepo.guardar(acta);

            assertThatThrownBy(() -> apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("paralizada");
        }

        @Test
        @DisplayName("No permitir rechazar apelacion si no hay fallo condenatorio notificado")
        void rechazar_sin_fallo_condenatorio_notificado() {
            Long actaId = llegarAAnalisis("77777777");
            assertThatThrownBy(() -> apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("No permitir aceptar apelacion absolutoria sobre fallo absolutorio")
        void aceptar_sobre_fallo_absolutorio() {
            Long actaId = llegarAAnalisis("88888888");
            falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(actaId, "Abs", null));

            assertThatThrownBy(() -> apelacionService.resolverAceptaAbsuelve(
                    new ResolverApelacionAceptaAbsuelveCommand(actaId, "Fundamentos", null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("condenatorio");
        }

        @Test
        @DisplayName("Rechazar apelacion no genera CIERRA")
        void rechazar_no_genera_cierra() {
            Long actaId = crearActaConApelacionPresentada();
            apelacionService.resolverRechazada(
                    new ResolverApelacionRechazadaCommand(actaId, "Fundamentos", null));

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    // =========================================================================
    // Slice 3B: Casos invalidos - Registrar apelacion (tests existentes mantenidos)
    // =========================================================================

    @Nested
    @DisplayName("Slice 3B: Casos invalidos - registrar apelacion")
    class CasosInvalidosRegistrar {

        @Test
        @DisplayName("No permitir apelacion si no hay fallo activo")
        void no_apelar_sin_fallo() {
            Long actaId = actaService.labrar(cmdLabrar("11111111")).idActa();
            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
        }

        @Test
        @DisplayName("No permitir apelacion si el fallo es absolutorio")
        void no_apelar_fallo_absolutorio() {
            Long actaId = llegarAAnalisis("22222222");
            falloService.dictarAbsolutorio(
                    new DictarFalloAbsolutorioCommand(actaId, "Fundamentos", null));

            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("condenatorio");
        }

        @Test
        @DisplayName("No permitir apelacion si fallo condenatorio no esta NOTIFICADO")
        void no_apelar_fallo_no_notificado() {
            Long actaId = llegarAAnalisis("33333333");
            falloService.dictarCondenatorio(new DictarFalloCondenatorioCommand(
                    actaId, new BigDecimal("500"), "Fundamentos", null));

            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("NOTIFICADO");
        }

        @Test
        @DisplayName("No permitir doble apelacion activa sobre la misma acta")
        void no_doble_apelacion() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", null, null));

            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("apelacion activa");
        }

        @Test
        @DisplayName("No permitir apelacion si el acta esta cerrada (absolutorio notificado sin bloqueantes)")
        void no_apelar_acta_cerrada() {
            Long actaId = llegarAAnalisis("44444444");
            falloService.dictarAbsolutorio(new DictarFalloAbsolutorioCommand(actaId, "Abs", null));
            Long idDocFallo = falloRepo.buscarActivo(actaId).orElseThrow().getDocumentoId();
            docService.firmarDocumento(new FirmarDocumentoCommand(idDocFallo, "Juez", "DIGITAL", null));
            String idNotif = notifService.enviarNotificacion(
                    new EnviarNotificacionCommand(actaId, idDocFallo, CanalNotificacion.PRESENCIAL, null, null, null, "test-user"))
                    .idEntidadAfectada();
            notifService.registrarPositiva(new RegistrarNotificacionPositivaCommand(Long.parseLong(idNotif), ar.gob.malvinas.faltas.core.support.IntentoTestSupport.intentoActivo(intentoRepo, Long.parseLong(idNotif)), null, "test-actor"));

            assertThatThrownBy(() -> apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("cerrada");
        }

        @Test
        @DisplayName("Registrar apelacion no genera CONDENA_FIRME")
        void apelacion_no_genera_condena_firme() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", null, null));

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.SIN_RESULTADO_FINAL);
        }

        @Test
        @DisplayName("Registrar apelacion no cierra el acta ni registra CIERRA")
        void apelacion_no_cierra_acta() {
            Long actaId = crearActaConFalloCondenatorioNotificado();
            apelacionService.registrarApelacion(
                    new RegistrarApelacionCommand(actaId, "Infractor", null, null));

            FalActa acta = actaRepo.buscarPorId(actaId).get();
            assertThat(acta.getSituacionAdministrativa()).isNotEqualTo(SituacionAdministrativaActa.CERRADA);

            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(actaId);
            assertThat(eventos).extracting(FalActaEvento::tipoEvt)
                    .doesNotContain(TipoEventoActa.CIERRA);
        }
    }
}
