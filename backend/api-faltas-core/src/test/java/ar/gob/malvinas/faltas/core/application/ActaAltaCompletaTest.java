package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.EvidenciaActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenActa;
import ar.gob.malvinas.faltas.core.application.query.CampoOrdenPersona;
import ar.gob.malvinas.faltas.core.application.query.FiltroActa;
import ar.gob.malvinas.faltas.core.application.query.FiltroPersona;
import ar.gob.malvinas.faltas.core.application.query.PaginaResultado;
import ar.gob.malvinas.faltas.core.application.query.PaginaSolicitada;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.domain.enums.EntidadTipoObservada;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenObservacion;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocumentoPersona;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEvidenciaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPersona;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalObservacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.ActaRepository;
import ar.gob.malvinas.faltas.core.repository.PersonaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.application.service.TalonarioService;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionCalculador;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Alta completa del acta: actor real, QR canonico previo al guardado, propagacion de
 * {@code lonInfr}, observacion inicial e instante unico.
 *
 * <p>Verifica la secuencia observable de {@code 20-application/command-contracts.md} con
 * dobles que registran el orden y el estado exacto que recibe cada puerto.
 */
@DisplayName("B1.5B2: ActaService.labrar — alta completa")
class ActaAltaCompletaTest {

    private static final String ACTOR = "8f14e45f-ceea-467a-9c1e-1b0f2b3c4d5e";
    private static final double LAT = -34.4957912;
    private static final double LON = -58.6945474;

    private ActaRepositoryRecorder actaRepo;
    private PersonaRepositoryRecorder personaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryActaEvidenciaRepository evidenciaRepo;
    private InMemoryObservacionRepository observacionRepo;
    private ActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new ActaRepositoryRecorder(new InMemoryActaRepository());
        personaRepo = new PersonaRepositoryRecorder(new InMemoryPersonaRepository());
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        evidenciaRepo = new InMemoryActaEvidenciaRepository();
        observacionRepo = new InMemoryObservacionRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED, snapshotRepo);

        QrActaService qrService = QrTestSupport.qrActaService(
                actaRepo, eventoRepo, snapshotRepo, recalc, FaltasClockTestSupport.FIXED);

        service = new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc, evidenciaRepo,
                personaRepo, FaltasClockTestSupport.FIXED, qrService, observacionRepo,
                new InMemoryPersonaDomicilioRepository(),
                new InMemoryActaArticuloInfringidoRepository(),
                ActaServiceTestSupport.valorizacionServiceMinima(actaRepo, FaltasClockTestSupport.FIXED),
                ActaServiceTestSupport.talonarioServiceMinimo(FaltasClockTestSupport.FIXED),
                new InMemoryActaVehiculoRepository(),
                new InMemoryActaTransitoRepository(),
                new InMemoryActaTransitoAlcoholemiaRepository(),
                new InMemoryActaContravencionRepository(),
                new InMemoryActaSustanciasAlimenticiasRepository(),
                new InMemoryActaMedidaPreventivaRepository(),
                new InMemoryMedidaPreventivaRepository(),
                ActaServiceTestSupport.fotografiaEvidenciaServiceMinima(
                        actaRepo, evidenciaRepo, FaltasClockTestSupport.FIXED));

        ActorContextHolder.set(new ActorContext(ACTOR));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    private LabrarActaCommand comando(String observaciones,
                                      SituacionIdentificacionInfractor situacion) {
        return new LabrarActaCommand(
                TipoActa.TRANSITO, 1L, 1L, LocalDate.of(2026, 7, 9),
                "Av. Libertad 100", "Dom. infractor", observaciones,
                LAT, LON,
                null, null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, situacion, null, null, null, null,
                null, ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(),
                null, null, null);
    }

    // =====================================================================
    @Nested @DisplayName("Actor autenticado en id_user_alta")
    class Actor {

        @Test @DisplayName("el sub del contexto llega exactamente a idUserAlta")
        void actorLlegaAIdUserAlta() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            assertThat(actaRepo.primerGuardado().getIdUserAlta())
                    .as("id_user_alta es el sub real del inspector autenticado")
                    .isEqualTo(ACTOR);
            assertThat(actaRepo.primerGuardado().getIdUserAlta())
                    .isNotIn("SYS", "SISTEMA", "PORTAL");
        }

        @Test @DisplayName("sin actor en contexto falla antes de cualquier escritura")
        void sinActorCeroWrites() {
            ActorContextHolder.clear();

            assertThatThrownBy(() -> service.labrar(
                    comando("una observacion", SituacionIdentificacionInfractor.AUSENTE)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("actor");

            assertThat(actaRepo.guardados).as("cero altas de acta").isEmpty();
            assertThat(personaRepo.accesos).as("cero accesos a personas").isZero();
            assertThat(observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, 1L)).isEmpty();
            assertThat(eventoRepo.size()).as("cero eventos").isZero();
        }

        @Test @DisplayName("el actor no se toma del comando: el comando no tiene componente de actor")
        void actorNoViajaEnElComando() {
            List<String> componentes = new ArrayList<>();
            for (var rc : LabrarActaCommand.class.getRecordComponents()) {
                componentes.add(rc.getName());
            }
            assertThat(componentes)
                    .as("el actor y el QR no son datos del cliente")
                    .doesNotContain("idUserAlta", "actor", "sub", "codigoQr", "qrPayloadVersion");
        }
    }

    // =====================================================================
    @Nested @DisplayName("QR canonico previo al guardado")
    class Qr {

        @Test @DisplayName("la primera invocacion a guardar ya recibe codigoQr, version, actor y lonInfr")
        void primerGuardarRecibeTodo() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            FalActa primera = actaRepo.primerGuardado();
            assertThat(primera.getId()).as("el alta llega sin PK: la asigna la persistencia").isNull();
            assertThat(primera.getCodigoQr()).isNotNull().isNotBlank();
            assertThat(primera.getQrPayloadVersion()).isNotNull().isZero();
            assertThat(primera.getIdUserAlta()).isEqualTo(ACTOR);
            assertThat(primera.getLonInfr()).isEqualTo(LON);
        }

        @Test @DisplayName("el QR deriva del uuidTecnico y no de la PK")
        void qrDerivaDelUuidTecnico() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            FalActa primera = actaRepo.primerGuardado();
            String[] partes = primera.getCodigoQr().split("\\.");
            assertThat(partes).hasSize(4);
            assertThat(partes[0]).isEqualTo("QR0");
            assertThat(partes[1])
                    .as("el segmento de identidad del token es el uuidTecnico del acta")
                    .isEqualTo(primera.getUuidTecnico());

            // El token queda completamente determinado por el uuidTecnico, la version y la
            // clave: reproducirlo sin conocer ninguna PK demuestra que no deriva de id_acta.
            assertThat(QrTestSupport.protector().generar(primera.getUuidTecnico()))
                    .as("el token se reproduce solo con el uuidTecnico")
                    .isEqualTo(primera.getCodigoQr());

            Long pk = actaRepo.listarTodas().get(0).getId();
            assertThat(partes[1])
                    .as("la identidad firmada es el UUID, no la PK")
                    .isNotEqualTo(String.valueOf(pk));
            assertThat(primera.getId())
                    .as("cuando el QR se genero el acta todavia no tenia PK")
                    .isNull();
        }

        @Test @DisplayName("qrPayloadVersion coincide con la version firmada dentro del token")
        void versionCoincideConElToken() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            FalActa acta = actaRepo.listarTodas().get(0);
            assertThat(acta.getCodigoQr().split("\\.")[2])
                    .isEqualTo(String.valueOf(acta.getQrPayloadVersion()));
        }

        @Test @DisplayName("el token persistido resuelve al uuidTecnico del acta")
        void tokenResuelveElActa() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            FalActa acta = actaRepo.listarTodas().get(0);
            assertThat(QrTestSupport.protector().resolverUuidTecnico(acta.getCodigoQr()))
                    .isEqualTo(acta.getUuidTecnico());
        }

        @Test @DisplayName("el token entra en codigo_qr VARCHAR(128)")
        void tokenEntraEnLaColumna() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));
            assertThat(actaRepo.primerGuardado().getCodigoQr().length()).isLessThanOrEqualTo(128);
        }
    }

    // =====================================================================
    @Nested @DisplayName("Propagacion de latInfr y lonInfr")
    class Ubicacion {

        @Test @DisplayName("latitud y longitud distintas llegan intactas y sin intercambio")
        void latYLonNoSeIntercambian() {
            service.labrar(comando(null, SituacionIdentificacionInfractor.AUSENTE));

            FalActa acta = actaRepo.listarTodas().get(0);
            assertThat(acta.getLatInfr()).isEqualTo(LAT);
            assertThat(acta.getLonInfr()).isEqualTo(LON);
            assertThat(acta.getLatInfr()).isNotEqualTo(acta.getLonInfr());
        }

        @Test @DisplayName("el constructor de compatibilidad delega ambas coordenadas")
        void constructorCompatibilidadDelegaAmbas() {
            LabrarActaCommand cmd = new LabrarActaCommand(
                    "TRANSITO", "1", "1", LocalDate.of(2026, 7, 9),
                    "Av. Libertad 100", "Dom", null, LAT, LON, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.AUSENTE, null);

            assertThat(cmd.latInfr()).isEqualTo(LAT);
            assertThat(cmd.lonInfr())
                    .as("lonInfr ya no se descarta en el constructor de compatibilidad")
                    .isEqualTo(LON);

            // El constructor de compatibilidad (String tipoActa/idDependencia/idInspector) no
            // transporta bloques de satelite: no puede usarse directamente contra labrar() desde
            // que el bloque obligatorio se exige siempre (Wave 4 Parte 3C). Se reconstruye el
            // mismo comando con el constructor canonico + el bloque transito minimo para
            // demostrar la propagacion de lat/lon tambien a traves de labrar().
            LabrarActaCommand cmdParaLabrar = new LabrarActaCommand(
                    cmd.tipoActa(), cmd.idDependencia(), cmd.idInspector(), cmd.fechaActa(),
                    cmd.domicilioHecho(), cmd.domicilioInfractor(), cmd.observaciones(),
                    cmd.latInfr(), cmd.lonInfr(), cmd.infractorNombre(), cmd.tipoDocumentoInfractor(),
                    cmd.infractorDocumento(), cmd.idPersonaInfractor(), cmd.resultadoFirmaInfractor(),
                    cmd.situacionIdentificacionInfractor(), cmd.evidenciasActa(), cmd.idTecnico(),
                    cmd.domicilios(), cmd.articulosInfringidos(), cmd.vehiculo(),
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(),
                    cmd.contravencion(), cmd.sustanciasAlimenticias(), cmd.medidasPreventivas());
            service.labrar(cmdParaLabrar);
            assertThat(actaRepo.primerGuardado().getLonInfr()).isEqualTo(LON);
        }

        @Test @DisplayName("ubicacion no informada queda nula en ambas, nunca en cero")
        void sinUbicacionAmbasNulas() {
            service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, null,
                    null, null, null, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.AUSENTE, null,
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null));

            FalActa acta = actaRepo.primerGuardado();
            assertThat(acta.getLatInfr()).isNull();
            assertThat(acta.getLonInfr()).isNull();
        }
    }

    // =====================================================================
    @Nested @DisplayName("Observacion inicial")
    class ObservacionInicial {

        @Test @DisplayName("texto no blanco crea una FalObservacion ACTA/USUARIO con la PK real del acta")
        void textoCreaObservacionCanonica() {
            ComandoResultado res = service.labrar(
                    comando("  El infractor se retiro del lugar.  ",
                            SituacionIdentificacionInfractor.AUSENTE));

            List<FalObservacion> obs =
                    observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, res.idActa());
            assertThat(obs).hasSize(1);
            FalObservacion o = obs.get(0);
            assertThat(o.getId()).as("la PK la asigno la persistencia").isNotNull().isPositive();
            assertThat(o.getEntidadTipo()).isEqualTo(EntidadTipoObservada.ACTA);
            assertThat(o.getEntidadId()).isEqualTo(res.idActa());
            assertThat(o.getIdActaContexto())
                    .as("el propietario es el acta: id_acta_contexto queda NULL")
                    .isNull();
            assertThat(o.getOrigenObs()).isEqualTo(OrigenObservacion.USUARIO);
            assertThat(o.getObservacion()).isEqualTo("El infractor se retiro del lugar.");
            assertThat(o.getIdUserAlta()).isEqualTo(ACTOR);
            assertThat(o.getFhAlta()).isEqualTo(FaltasClockTestSupport.FIXED.now());
            assertThat(o.isSiActiva()).isTrue();
            assertThat(o.getTipoObs()).as("campo deprecado: no se informa").isNull();
        }

        @Test @DisplayName("texto nulo no crea ninguna fila")
        void textoNuloNoCreaFila() {
            ComandoResultado res = service.labrar(
                    comando(null, SituacionIdentificacionInfractor.AUSENTE));
            assertThat(observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, res.idActa()))
                    .isEmpty();
        }

        @Test @DisplayName("texto en blanco no crea ninguna fila")
        void textoEnBlancoNoCreaFila() {
            ComandoResultado res = service.labrar(
                    comando("    ", SituacionIdentificacionInfractor.AUSENTE));
            assertThat(observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, res.idActa()))
                    .isEmpty();
        }

        @Test @DisplayName("fal_observacion es la fuente canonica persistente, no fal_acta")
        void fuenteCanonicaEsFalObservacion() {
            ComandoResultado res = service.labrar(
                    comando("Texto de la observacion inicial", SituacionIdentificacionInfractor.AUSENTE));

            List<FalObservacion> obs =
                    observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, res.idActa());
            assertThat(obs).hasSize(1);
            assertThat(obs.get(0).getObservacion()).isEqualTo("Texto de la observacion inicial");
        }
    }

    // =====================================================================
    @Nested @DisplayName("Instante unico de la frontera")
    class InstanteUnico {

        @Test @DisplayName("acta, persona nueva, observacion, evidencias, ACTLAB y snapshot usan el mismo instante")
        void mismoInstanteEnTodaLaFrontera() {
            LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

            ComandoResultado res = service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, LocalDate.of(2026, 7, 9),
                    "Av. Libertad 100", "Dom", "Observacion inicial del acta",
                    LAT, LON,
                    "Juan Perez", TipoDocumentoPersona.DNI, "30111222", null,
                    ResultadoFirmaInfractor.FIRMADA,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO,
                    List.of(new EvidenciaActaItem(
                            TipoEvidenciaActa.FIRMA_OLOGRAFA_INFRACTOR, "storage/firma.png")),
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null));

            FalActa acta = actaRepo.listarTodas().get(0);
            assertThat(acta.getFhAlta()).isEqualTo(ahora);
            assertThat(acta.getFechaLabrado()).isEqualTo(ahora);

            FalPersona persona = personaRepo.listarTodas().get(0);
            assertThat(persona.getFhAlta()).as("la persona nueva usa el mismo instante").isEqualTo(ahora);
            assertThat(persona.getIdUserAlta()).isEqualTo(ACTOR);

            assertThat(observacionRepo.listarPorEntidad(EntidadTipoObservada.ACTA, res.idActa()))
                    .singleElement()
                    .satisfies(o -> assertThat(o.getFhAlta()).isEqualTo(ahora));

            assertThat(evidenciaRepo.listarPorActa(res.idActa()))
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.getFhAlta()).isEqualTo(ahora);
                        assertThat(e.getFechaRegistro()).isEqualTo(ahora);
                        assertThat(e.getIdUserAlta()).isEqualTo(ACTOR);
                    });

            assertThat(eventoRepo.buscarPorActa(res.idActa()))
                    .filteredOn(e -> e.tipoEvt() == TipoEventoActa.ACTLAB)
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.fhEvt()).isEqualTo(ahora);
                        assertThat(e.idUserEvt()).isEqualTo(ACTOR);
                    });

            assertThat(snapshotRepo.buscarPorActa(res.idActa()))
                    .isPresent()
                    .get()
                    .satisfies(s -> assertThat(s.getUltimaActualizacion()).isEqualTo(ahora));
        }

        @Test @DisplayName("la respuesta usa la PK que devolvio guardar")
        void respuestaUsaLaPkDeGuardar() {
            ComandoResultado res = service.labrar(
                    comando(null, SituacionIdentificacionInfractor.AUSENTE));
            assertThat(res.idActa()).isEqualTo(actaRepo.listarTodas().get(0).getId());
        }
    }

    // =====================================================================
    @Nested @DisplayName("Persona e identificacion")
    class Persona {

        @Test @DisplayName("las situaciones 3..6 no acceden al repositorio de personas")
        void situacionesSinPersonaNoTocanElRepositorio() {
            for (SituacionIdentificacionInfractor s : SituacionIdentificacionInfractor.values()) {
                if (s.exigePersonaInfractora()) continue;
                setUp();
                service.labrar(comando(null, s));
                assertThat(personaRepo.accesos)
                        .as("situacion %s no busca, no crea y no asigna persona", s)
                        .isZero();
                assertThat(actaRepo.primerGuardado().getIdPersonaInfractor()).isNull();
            }
        }

        @Test @DisplayName("IDENTIFICADO_COMPLETO con un id interno inexistente se rechaza sin escribir")
        void idInternoInexistenteSeRechaza() {
            assertThatThrownBy(() -> service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, null, LAT, LON,
                    null, null, null, 987654L,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, null,
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no corresponde a ninguna persona");

            assertThat(actaRepo.guardados).isEmpty();
        }

        @Test @DisplayName("IDENTIFICADO_COMPLETO con una persona sin documento canonico se rechaza")
        void idInternoSinDocumentoSeRechaza() {
            FalPersona sinDocumento = new FalPersona(
                    null, TipoPersona.FISICA, FaltasClockTestSupport.FIXED.now(), ACTOR);
            sinDocumento.setNombreMostrar("Persona Parcial");
            Long idSinDocumento = personaRepo.guardar(sinDocumento).getId();

            assertThatThrownBy(() -> service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, null, LAT, LON,
                    null, null, null, idSinDocumento,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.IDENTIFICADO_COMPLETO, null,
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no tiene documento canonico");

            assertThat(actaRepo.guardados).as("cero altas de acta").isEmpty();
            assertThat(personaRepo.listarTodas())
                    .as("la persona existente no se altera para hacerla encajar")
                    .singleElement()
                    .satisfies(p -> {
                        assertThat(p.getDocumento().estaAusente()).isTrue();
                        assertThat(p.getNombreMostrar()).isEqualTo("Persona Parcial");
                    });
        }

        @Test @DisplayName("IDENTIFICADO_PARCIAL con una persona sin documento es valido")
        void idInternoSinDocumentoEsParcialValido() {
            FalPersona sinDocumento = new FalPersona(
                    null, TipoPersona.FISICA, FaltasClockTestSupport.FIXED.now(), ACTOR);
            sinDocumento.setNombreMostrar("Persona Parcial");
            Long id = personaRepo.guardar(sinDocumento).getId();

            service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, null, LAT, LON,
                    null, null, null, id,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.IDENTIFICADO_PARCIAL, null,
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null));

            assertThat(actaRepo.primerGuardado().getIdPersonaInfractor()).isEqualTo(id);
        }

        @Test @DisplayName("un id interno con una situacion sin persona se rechaza")
        void idInternoConSituacionSinPersonaSeRechaza() {
            assertThatThrownBy(() -> service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, null, LAT, LON,
                    null, null, null, 1L,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.AUSENTE, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(actaRepo.guardados).isEmpty();
        }
    }

    // =====================================================================
    @Nested @DisplayName("Precondiciones y frontera transaccional")
    class Precondiciones {

        @Test @DisplayName("FIRMADA sin evidencia de firma no escribe nada")
        void firmadaSinEvidenciaCeroWrites() {
            assertThatThrownBy(() -> service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, "obs", LAT, LON,
                    null, null, null, null,
                    ResultadoFirmaInfractor.FIRMADA,
                    SituacionIdentificacionInfractor.AUSENTE, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);

            assertThat(actaRepo.guardados).isEmpty();
            assertThat(personaRepo.accesos).isZero();
            assertThat(eventoRepo.size()).isZero();
        }

        @Test @DisplayName("resultadoFirmaInfractor ausente no escribe nada")
        void sinResultadoFirmaCeroWrites() {
            assertThatThrownBy(() -> service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Dir", null, "obs", LAT, LON,
                    null, null, null, null, null,
                    SituacionIdentificacionInfractor.AUSENTE, null)))
                    .isInstanceOf(PrecondicionVioladaException.class);
            assertThat(actaRepo.guardados).isEmpty();
        }

        @Test @DisplayName("el servicio exige el productor de QR y el puerto de observaciones")
        void colaboradoresObligatorios() {
            SnapshotRecalculador recalc = new SnapshotRecalculador(
                    eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                    new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                    new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                    FaltasClockTestSupport.FIXED, snapshotRepo);

            assertThatThrownBy(() -> new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                    evidenciaRepo, FaltasClockTestSupport.FIXED, null, observacionRepo,
                    new InMemoryPersonaDomicilioRepository(),
                    new InMemoryActaArticuloInfringidoRepository(),
                    ActaServiceTestSupport.valorizacionServiceMinima(actaRepo, FaltasClockTestSupport.FIXED),
                    ActaServiceTestSupport.talonarioServiceMinimo(FaltasClockTestSupport.FIXED),
                    new InMemoryActaVehiculoRepository(), new InMemoryActaTransitoRepository(),
                    new InMemoryActaTransitoAlcoholemiaRepository(), new InMemoryActaContravencionRepository(),
                    new InMemoryActaSustanciasAlimenticiasRepository(), new InMemoryActaMedidaPreventivaRepository(),
                    new InMemoryMedidaPreventivaRepository(),
                    ActaServiceTestSupport.fotografiaEvidenciaServiceMinima(
                            actaRepo, evidenciaRepo, FaltasClockTestSupport.FIXED)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("qrActaService");

            assertThatThrownBy(() -> new ActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                    evidenciaRepo, FaltasClockTestSupport.FIXED,
                    QrTestSupport.qrActaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                            FaltasClockTestSupport.FIXED),
                    null,
                    new InMemoryPersonaDomicilioRepository(),
                    new InMemoryActaArticuloInfringidoRepository(),
                    ActaServiceTestSupport.valorizacionServiceMinima(actaRepo, FaltasClockTestSupport.FIXED),
                    ActaServiceTestSupport.talonarioServiceMinimo(FaltasClockTestSupport.FIXED),
                    new InMemoryActaVehiculoRepository(), new InMemoryActaTransitoRepository(),
                    new InMemoryActaTransitoAlcoholemiaRepository(), new InMemoryActaContravencionRepository(),
                    new InMemoryActaSustanciasAlimenticiasRepository(), new InMemoryActaMedidaPreventivaRepository(),
                    new InMemoryMedidaPreventivaRepository(),
                    ActaServiceTestSupport.fotografiaEvidenciaServiceMinima(
                            actaRepo, evidenciaRepo, FaltasClockTestSupport.FIXED)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("observacionRepository");
        }

        /**
         * Asercion invertida en WAVE 1A2-B2, el slice que implementa
         * {@code DECISION_OBS-TX-01}. Hasta B1.5B2 este caso verificaba que la frontera
         * transaccional no se hubiera adelantado; a partir de B2 verifica que exista y que
         * este exactamente donde la decision la declara.
         *
         * <p>Lo que sigue protegiendo la misma frontera, y no cambia, es que el servicio
         * coordine puertos y nunca adaptadores concretos.
         */
        @Test @DisplayName("la frontera transaccional existe y esta sobre labrar, no sobre la clase")
        void fronteraTransaccionalSobreLabrar() throws Exception {
            Path fuente = Paths.get("src/main/java/ar/gob/malvinas/faltas/core/application/"
                    + "service/ActaService.java");
            StringBuilder ejecutable = new StringBuilder();
            for (String linea : Files.readAllLines(fuente, StandardCharsets.UTF_8)) {
                String t = linea.trim();
                if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
                ejecutable.append(linea).append('\n');
            }
            String codigo = ejecutable.toString();

            assertThat(codigo)
                    .as("DECISION_OBS-TX-01 exige la demarcacion sobre el metodo publico del alta;"
                            + " Wave 4 PARTE-3A ajusta el isolation level a READ_COMMITTED (ver Javadoc"
                            + " de ActaService.labrar), pero la anotacion sigue sobre el mismo metodo")
                    .contains("@Transactional(isolation = org.springframework.transaction.annotation"
                            + ".Isolation.READ_COMMITTED)\n    public ComandoResultado labrar(LabrarActaCommand cmd)");
            assertThat(codigo)
                    .as("la anotacion no se pone sobre la clase: solo labrar es la frontera")
                    .doesNotContain("@Transactional\npublic class ActaService");
            assertThat(codigo)
                    .as("sin propagacion nueva, sin transacciones anidadas y sin excepciones excluidas")
                    .doesNotContain("REQUIRES_NEW")
                    .doesNotContain("NESTED")
                    .doesNotContain("noRollbackFor")
                    .doesNotContain("TransactionSynchronization")
                    .doesNotContain("TransactionTemplate");
            assertThat(codigo)
                    .as("el servicio coordina puertos, nunca adaptadores JDBC concretos")
                    .doesNotContain("JdbcActaRepository")
                    .doesNotContain("JdbcObservacionRepository")
                    .doesNotContain("JdbcPersonaRepository");
        }
    }

    // =====================================================================
    // Dobles que registran orden y estado exacto recibido por cada puerto
    // =====================================================================

    /** Registra cada {@code guardar} y conserva una copia inmutable del argumento recibido. */
    private static final class ActaRepositoryRecorder implements ActaRepository {
        private final ActaRepository delegado;
        private final List<FalActa> guardados = new ArrayList<>();

        ActaRepositoryRecorder(ActaRepository delegado) {
            this.delegado = delegado;
        }

        @Override
        public FalActa guardar(FalActa acta) {
            guardados.add(acta.copia());
            return delegado.guardar(acta);
        }

        /** Estado exacto que recibio la primera invocacion: el alta. */
        FalActa primerGuardado() {
            assertThat(guardados).as("se esperaba al menos una invocacion a guardar").isNotEmpty();
            return guardados.get(0);
        }

        @Override public Optional<FalActa> buscarPorId(Long id) { return delegado.buscarPorId(id); }
        @Override public Optional<FalActa> buscarPorUuidTecnico(String uuid) {
            return delegado.buscarPorUuidTecnico(uuid);
        }
        @Override public Optional<FalActa> buscarPorUuidTecnicoConLock(String uuid) {
            return delegado.buscarPorUuidTecnicoConLock(uuid);
        }
        @Override public Optional<FalActa> buscarPorIdConLock(Long id) {
            return delegado.buscarPorIdConLock(id);
        }
        @Override public List<FalActa> listarTodas() { return delegado.listarTodas(); }
        @Override public PaginaResultado<FalActa> listarPagina(
                FiltroActa filtro, PaginaSolicitada<CampoOrdenActa> pagina) {
            return delegado.listarPagina(filtro, pagina);
        }
    }

    /** Cuenta todo acceso al puerto de personas, de lectura o de escritura. */
    private static final class PersonaRepositoryRecorder implements PersonaRepository {
        private final PersonaRepository delegado;
        private int accesos = 0;

        PersonaRepositoryRecorder(PersonaRepository delegado) {
            this.delegado = delegado;
        }

        @Override public FalPersona guardar(FalPersona persona) {
            accesos++;
            return delegado.guardar(persona);
        }
        @Override public Optional<FalPersona> buscarPorId(Long id) {
            accesos++;
            return delegado.buscarPorId(id);
        }
        @Override public List<FalPersona> listarTodas() { return delegado.listarTodas(); }
        @Override public List<FalPersona> buscarPorTipoDocYNroDoc(TipoDocumentoPersona tipoDoc, String nroDoc) {
            accesos++;
            return delegado.buscarPorTipoDocYNroDoc(tipoDoc, nroDoc);
        }
        @Override public List<FalPersona> buscarPorIdSujBie(Integer idSuj, Integer idBie) {
            accesos++;
            return delegado.buscarPorIdSujBie(idSuj, idBie);
        }
        @Override public PaginaResultado<FalPersona> listarPagina(
                FiltroPersona filtro, PaginaSolicitada<CampoOrdenPersona> pagina) {
            accesos++;
            return delegado.listarPagina(filtro, pagina);
        }
    }
}
