package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.port.QrTokenEmitido;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.application.service.QrActaService;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import ar.gob.malvinas.faltas.core.support.QrTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El QR se emite una sola vez, durante el labrado, y despues es inmutable
 * ({@code DECISION-QR-INMUTABLE-01}).
 *
 * <p>La inmutabilidad se verifica en el agregado, que es donde se hace cumplir: no alcanza
 * con que la capa de aplicacion no ofrezca una operacion de rotacion, porque un servicio
 * futuro podria intentar mutar un acta leida del repositorio.
 */
@DisplayName("B1.5B2-R1: QR emitido una sola vez e inmutable")
class ActaQrInmutabilidadTest {

    private static final String ACTOR = "8f14e45f-ceea-467a-9c1e-1b0f2b3c4d5e";

    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;
    private InMemoryActaSnapshotRepository snapshotRepo;
    private InMemoryObservacionRepository observacionRepo;
    private ActaService service;

    @BeforeEach
    void setUp() {
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
        snapshotRepo = new InMemoryActaSnapshotRepository();
        observacionRepo = new InMemoryObservacionRepository();

        SnapshotRecalculador recalc = new SnapshotRecalculador(
                eventoRepo, new InMemoryDocumentoRepository(), new InMemoryNotificacionRepository(),
                new InMemoryPagoVoluntarioRepository(), new InMemoryFalloActaRepository(),
                new InMemoryApelacionActaRepository(), new InMemoryPagoCondenaRepository(),
                FaltasClockTestSupport.FIXED, snapshotRepo);

        service = ActaServiceTestSupport.actaService(actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED, observacionRepo);

        ActorContextHolder.set(new ActorContext(ACTOR));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    private FalActa labrarYLeer() {
        ComandoResultado res = service.labrar(new LabrarActaCommand(
                TipoActa.TRANSITO, 1L, 1L, LocalDate.of(2026, 7, 9),
                "Av. Libertad 100", null, null, null, null,
                null, null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                SituacionIdentificacionInfractor.AUSENTE, null,
                null, null, null, null,
                ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null));
        return actaRepo.buscarPorId(res.idActa()).orElseThrow();
    }

    // =====================================================================
    @Nested @DisplayName("Emision unica durante el labrado")
    class EmisionUnica {

        @Test @DisplayName("1 y 2. el alta emite exactamente un QR y lo deja informado con su version")
        void altaEmiteUnQrInformado() {
            FalActa acta = labrarYLeer();

            assertThat(acta.getCodigoQr()).isNotNull().isNotBlank().hasSize(86);
            assertThat(acta.getQrPayloadVersion()).isZero();
            assertThat(acta.tieneQrEmitido()).isTrue();
            assertThat(acta.getCodigoQr().split("\\.")[2])
                    .isEqualTo(String.valueOf(acta.getQrPayloadVersion()));
        }

        @Test @DisplayName("3. el mismo uuid, version 0 y clave producen el mismo token")
        void emisionDeterministica() {
            FalActa acta = labrarYLeer();
            assertThat(QrTestSupport.protector().generar(acta.getUuidTecnico()))
                    .isEqualTo(acta.getCodigoQr());
        }

        @Test @DisplayName("dos altas distintas reciben tokens distintos")
        void altasDistintasTokensDistintos() {
            assertThat(labrarYLeer().getCodigoQr()).isNotEqualTo(labrarYLeer().getCodigoQr());
        }

        @Test @DisplayName("7 y 8. el alta no emite QRGENA y ninguna operacion posterior lo emite")
        void sinQrgena() {
            FalActa acta = labrarYLeer();
            assertThat(eventoRepo.buscarPorActa(acta.getId()))
                    .as("el alta emite ACTLAB; QRGENA quedaria para una emision que no existe")
                    .noneMatch(e -> e.tipoEvt() == TipoEventoActa.QRGENA)
                    .anyMatch(e -> e.tipoEvt() == TipoEventoActa.ACTLAB);
        }
    }

    // =====================================================================
    @Nested @DisplayName("Inmutabilidad en el agregado")
    class Inmutabilidad {

        @Test @DisplayName("5. un acta con QR no puede recibir otro codigoQr")
        void segundoCodigoQrSeRechaza() {
            FalActa acta = labrarYLeer();
            String original = acta.getCodigoQr();
            String otro = QrTestSupport.protector().generar(UUID.randomUUID().toString());

            assertThatThrownBy(() -> acta.setCodigoQr(otro))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("codigo_qr es inmutable");

            assertThat(acta.getCodigoQr())
                    .as("el intento rechazado no deja el agregado a medias")
                    .isEqualTo(original);
        }

        @Test @DisplayName("un acta con QR no puede quedarse sin QR")
        void borrarElQrSeRechaza() {
            FalActa acta = labrarYLeer();
            assertThatThrownBy(() -> acta.setCodigoQr(null))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> acta.setCodigoQr("  "))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(acta.tieneQrEmitido()).isTrue();
        }

        @Test @DisplayName("6. un acta con version 0 no puede cambiar de version despues de emitida")
        void cambioDeVersionSeRechaza() {
            FalActa acta = labrarYLeer();

            assertThatThrownBy(() -> acta.setQrPayloadVersion(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("qr_payload_version es inmutable");
            assertThatThrownBy(() -> acta.setQrPayloadVersion(null))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(acta.getQrPayloadVersion()).isZero();
        }

        /**
         * Reasignar el mismo valor se admite a proposito: la hidratacion desde la base y las
         * copias defensivas no deben ser casos especiales de la regla de inmutabilidad.
         */
        @Test @DisplayName("reasignar el mismo token y la misma version es idempotente")
        void reasignarElMismoValorEsIdempotente() {
            FalActa acta = labrarYLeer();
            String token = acta.getCodigoQr();
            Integer version = acta.getQrPayloadVersion();

            assertThatCode(() -> {
                acta.setQrPayloadVersion(version);
                acta.setCodigoQr(token);
            }).doesNotThrowAnyException();

            assertThat(acta.getCodigoQr()).isEqualTo(token);
            assertThat(acta.getQrPayloadVersion()).isEqualTo(version);
        }

        @Test @DisplayName("antes de la emision el QR y la version se pueden informar")
        void antesDeLaEmisionSePuedeInformar() {
            String uuid = UUID.randomUUID().toString();
            QrTokenEmitido qr = QrTestSupport.protector().emitir(uuid);

            FalActa acta = new FalActa(null, uuid, TipoActa.TRANSITO, 1L, 1L,
                    LocalDate.of(2026, 7, 9), FaltasClockTestSupport.FIXED.now(),
                    "Dir", null, null, null, ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.AUSENTE, null,
                    FaltasClockTestSupport.FIXED.now(), ACTOR);

            assertThat(acta.tieneQrEmitido()).isFalse();
            assertThatCode(() -> {
                acta.setQrPayloadVersion(qr.version());
                acta.setCodigoQr(qr.token());
            }).doesNotThrowAnyException();
            assertThat(acta.tieneQrEmitido()).isTrue();
        }

        @Test @DisplayName("la copia defensiva conserva el QR sin violar la inmutabilidad")
        void copiaConservaElQr() {
            FalActa acta = labrarYLeer();
            FalActa copia = acta.copia();

            assertThat(copia.getCodigoQr()).isEqualTo(acta.getCodigoQr());
            assertThat(copia.getQrPayloadVersion()).isEqualTo(acta.getQrPayloadVersion());
            assertThatThrownBy(() -> copia.setCodigoQr("QR0.otro"))
                    .as("la copia hereda la inmutabilidad")
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test @DisplayName("el acta persistida y releida sigue rechazando un cambio de QR")
        void releidaSigueInmutable() {
            FalActa acta = labrarYLeer();
            FalActa releida = actaRepo.buscarPorId(acta.getId()).orElseThrow();

            assertThatThrownBy(() -> releida.setCodigoQr(
                    QrTestSupport.protector().generar(UUID.randomUUID().toString())))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(actaRepo.buscarPorId(acta.getId()).orElseThrow().getCodigoQr())
                    .isEqualTo(acta.getCodigoQr());
        }
    }

    // =====================================================================
    @Nested @DisplayName("API de aplicacion sin rotacion")
    class ApiSinRotacion {

        @Test @DisplayName("4. QrActaService no expone ninguna operacion de rotacion")
        void sinApiDeRotacion() {
            Set<String> publicas = java.util.Arrays.stream(QrActaService.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .map(Method::getName)
                    .collect(Collectors.toSet());

            assertThat(publicas)
                    .doesNotContain("rotarQr", "generarQr", "regenerarQr", "revocarQr",
                            "reemitirQr", "persistirQr", "emitirNuevoQr");
            assertThat(publicas)
                    .as("el API minimo realmente usado")
                    .containsExactlyInAnyOrder("emitirTokenPara", "registrarAcceso",
                            "registrarAccesoConNotificacion", "listarAccesosPorActa");
        }

        @Test @DisplayName("la emision del servicio no escribe: no toca el acta ni emite eventos")
        void emitirTokenParaNoEscribe() {
            FalActa acta = labrarYLeer();
            int eventosAntes = eventoRepo.buscarPorActa(acta.getId()).size();

            service.labrar(new LabrarActaCommand(
                    TipoActa.TRANSITO, 1L, 1L, null, "Otra", null, null, null, null,
                    null, null, null, null,
                    ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                    SituacionIdentificacionInfractor.AUSENTE, null,
                    null, null, null, null,
                    ar.gob.malvinas.faltas.core.support.LabrarActaCommandTestFactory.transitoMinimo(), null, null, null));

            assertThat(actaRepo.buscarPorId(acta.getId()).orElseThrow().getCodigoQr())
                    .as("labrar otra acta no puede afectar el QR de una acta existente")
                    .isEqualTo(acta.getCodigoQr());
            assertThat(eventoRepo.buscarPorActa(acta.getId())).hasSize(eventosAntes);
        }
    }
}
