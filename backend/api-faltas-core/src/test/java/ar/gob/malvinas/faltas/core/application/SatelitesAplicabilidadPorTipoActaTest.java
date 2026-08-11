package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.command.AlcoholemiaMedicionItem;
import ar.gob.malvinas.faltas.core.application.command.ArticuloInfringidoItem;
import ar.gob.malvinas.faltas.core.application.command.ContravencionActaItem;
import ar.gob.malvinas.faltas.core.application.command.LabrarActaCommand;
import ar.gob.malvinas.faltas.core.application.command.SustanciasAlimenticiasActaItem;
import ar.gob.malvinas.faltas.core.application.command.TransitoActaItem;
import ar.gob.malvinas.faltas.core.application.command.VehiculoActaItem;
import ar.gob.malvinas.faltas.core.application.result.ComandoResultado;
import ar.gob.malvinas.faltas.core.application.service.ActaService;
import ar.gob.malvinas.faltas.core.domain.enums.AmbitoCtv;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenNomenclatura;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoPruebaAlcoholemia;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContext;
import ar.gob.malvinas.faltas.core.infrastructure.security.ActorContextHolder;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEvidenciaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaSnapshotRepository;
import ar.gob.malvinas.faltas.core.support.ActaServiceTestSupport;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wave 4 Parte 3B §6: matriz autoritativa de aplicabilidad de satélites por {@code tipoActa}
 * ({@code DECISION-ACTA-SATELITES-COMPLETOS-01}).
 *
 * <p><b>DECISION-APLICABILIDAD-SATELITES-COMPATIBILIDAD-01</b> (declarada en
 * {@code PART-3B-APPLICABILITY-MATRIX.md}): la obligatoriedad de bloque solo se exige cuando
 * el comando informa al menos un bloque tipado de satélite ("opt-in"); un comando que no
 * informa ningún satélite tipado (patrón legado masivo de Parte 1/2/3A) queda fuera del
 * alcance de esta validación. La prohibición de combinaciones incompatibles se aplica
 * siempre, con o sin opt-in.
 */
@DisplayName("Wave 4 Parte 3B §6: matriz de aplicabilidad de satélites por tipoActa")
class SatelitesAplicabilidadPorTipoActaTest {

    private static final String ACTOR = "8f14e45f-ceea-467a-9c1e-1b0f2b3c4d5e";

    private ActaService service;

    @BeforeEach
    void setUp() {
        InMemoryActaRepository actaRepo = new InMemoryActaRepository();
        InMemoryActaEventoRepository eventoRepo = new InMemoryActaEventoRepository();
        InMemoryActaSnapshotRepository snapshotRepo = new InMemoryActaSnapshotRepository();
        ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador recalc =
                new ar.gob.malvinas.faltas.core.snapshot.SnapshotRecalculador(
                        eventoRepo,
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryDocumentoRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryNotificacionRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoVoluntarioRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryApelacionActaRepository(),
                        new ar.gob.malvinas.faltas.core.repository.memory.InMemoryPagoCondenaRepository(),
                        FaltasClockTestSupport.FIXED, snapshotRepo);
        service = ActaServiceTestSupport.actaService(
                actaRepo, eventoRepo, snapshotRepo, recalc,
                new InMemoryActaEvidenciaRepository(), FaltasClockTestSupport.FIXED);
        ActorContextHolder.set(new ActorContext(ACTOR));
    }

    @AfterEach
    void tearDown() {
        ActorContextHolder.clear();
    }

    private static VehiculoActaItem vehiculoValido() {
        return new VehiculoActaItem("AB123CD", TipoVehiculo.AUTO, null, "MARCA", null, "MODELO",
                (short) 2020, "Rojo", EstadoGeneralVehiculo.BUENO);
    }

    private static TransitoActaItem transitoSinAlcoholemia() {
        return new TransitoActaItem(null, null, null, null, false, false, false, null);
    }

    private static ContravencionActaItem contravencionValida() {
        return new ContravencionActaItem(
                OrigenNomenclatura.MANUAL_EXCEPCIONAL, true,
                ar.gob.malvinas.faltas.core.domain.enums.MotivoNomenclaturaManual.CONTINGENCIA_OPERATIVA,
                null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, AmbitoCtv.COMERCIO, null);
    }

    private static SustanciasAlimenticiasActaItem sustanciasValidas() {
        return new SustanciasAlimenticiasActaItem(null, null, AmbitoCtv.LOCAL, null, "descripcion");
    }

    private LabrarActaCommand comando(TipoActa tipo, VehiculoActaItem vehiculo, TransitoActaItem transito,
                                       ContravencionActaItem contravencion, SustanciasAlimenticiasActaItem sustancias) {
        return new LabrarActaCommand(
                tipo, 1L, 1L, LocalDate.of(2026, 8, 6),
                "Dom. hecho", null, null, null, null,
                null, null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR,
                SituacionIdentificacionInfractor.AUSENTE,
                null, null, null,
                List.of(new ArticuloInfringidoItem(1L, 1L)),
                vehiculo, transito, contravencion, sustancias, List.of());
    }

    @Nested
    @DisplayName("TRANSITO")
    class Transito {

        @Test
        @DisplayName("transito informado: alta valida")
        void transitoInformadoAltaValida() {
            ComandoResultado res = service.labrar(comando(TipoActa.TRANSITO, null, transitoSinAlcoholemia(), null, null));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("sin ningun satelite tipado: rechaza (Wave 4 Parte 3C, DECISION-ACTA-SATELITES-STRICT-01: "
                + "el opt-in de Parte 3B fue eliminado, el bloque obligatorio se exige siempre)")
        void sinNingunSateliteRechaza() {
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.TRANSITO, null, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("transito")
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("bloque obligatorio ausente: vehiculo sin transito rechaza (opt-in por vehiculo)")
        void vehiculoSinTransitoRechaza() {
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.TRANSITO, vehiculoValido(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("transito")
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("bloque prohibido: contravencion informado en TRANSITO rechaza")
        void contravencionProhibidaEnTransito() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.TRANSITO, null, transitoSinAlcoholemia(), contravencionValida(), null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("prohibido");
        }

        @Test
        @DisplayName("bloque prohibido: sustanciasAlimenticias informado en TRANSITO rechaza")
        void sustanciasProhibidasEnTransito() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.TRANSITO, null, transitoSinAlcoholemia(), null, sustanciasValidas())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("sustanciasAlimenticias")
                    .hasMessageContaining("prohibido");
        }

        @Test
        @DisplayName("vehiculo condicional/permitido junto a transito")
        void vehiculoPermitidoEnTransito() {
            ComandoResultado res = service.labrar(
                    comando(TipoActa.TRANSITO, vehiculoValido(), transitoSinAlcoholemia(), null, null));
            assertThat(res.idActa()).isNotNull();
        }
    }

    @Nested
    @DisplayName("CONTRAVENCION")
    class Contravencion {

        @Test
        @DisplayName("contravencion informado: alta valida")
        void contravencionInformadaAltaValida() {
            ComandoResultado res = service.labrar(
                    comando(TipoActa.CONTRAVENCION, null, null, contravencionValida(), null));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("bloque obligatorio ausente: vehiculo sin contravencion rechaza")
        void vehiculoSinContravencionRechaza() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.CONTRAVENCION, vehiculoValido(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("bloque prohibido: transito informado en CONTRAVENCION rechaza")
        void transitoProhibidoEnContravencion() {
            assertThatThrownBy(() -> service.labrar(comando(
                    TipoActa.CONTRAVENCION, null, transitoSinAlcoholemia(), contravencionValida(), null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("transito")
                    .hasMessageContaining("prohibido");
        }

        @Test
        @DisplayName("vehiculo condicional/permitido junto a contravencion")
        void vehiculoPermitidoEnContravencion() {
            ComandoResultado res = service.labrar(
                    comando(TipoActa.CONTRAVENCION, vehiculoValido(), null, contravencionValida(), null));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("sin ningun satelite tipado: rechaza (Wave 4 Parte 3C, sin opt-in)")
        void sinNingunSateliteRechaza() {
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.CONTRAVENCION, null, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
        }
    }

    @Nested
    @DisplayName("SUSTANCIAS_ALIMENTICIAS")
    class Sustancias {

        @Test
        @DisplayName("sustancias informadas: alta valida")
        void sustanciasInformadasAltaValida() {
            ComandoResultado res = service.labrar(
                    comando(TipoActa.SUSTANCIAS_ALIMENTICIAS, null, null, null, sustanciasValidas()));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("bloque prohibido: transito informado en SUSTANCIAS_ALIMENTICIAS rechaza")
        void transitoProhibidoEnSustancias() {
            assertThatThrownBy(() -> service.labrar(comando(
                    TipoActa.SUSTANCIAS_ALIMENTICIAS, null, transitoSinAlcoholemia(), null, sustanciasValidas())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("transito")
                    .hasMessageContaining("prohibido");
        }

        @Test
        @DisplayName("bloque obligatorio ausente: vehiculo sin sustancias rechaza")
        void vehiculoSinSustanciasRechaza() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.SUSTANCIAS_ALIMENTICIAS, vehiculoValido(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("sustanciasAlimenticias")
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("vehiculo permitido para transporte de alimentos junto a sustancias")
        void vehiculoPermitidoParaTransporteEnSustancias() {
            ComandoResultado res = service.labrar(comando(
                    TipoActa.SUSTANCIAS_ALIMENTICIAS, vehiculoValido(), null, null, sustanciasValidas()));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("sin ningun satelite tipado: rechaza (Wave 4 Parte 3C, sin opt-in)")
        void sinNingunSateliteRechaza() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.SUSTANCIAS_ALIMENTICIAS, null, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("sustanciasAlimenticias")
                    .hasMessageContaining("obligatorio");
        }
    }

    @Nested
    @DisplayName("COMERCIO")
    class Comercio {

        @Test
        @DisplayName("contravencion (comparte bloque) informado: alta valida")
        void contravencionInformadaAltaValida() {
            ComandoResultado res = service.labrar(
                    comando(TipoActa.COMERCIO, null, null, contravencionValida(), null));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("bloque obligatorio ausente: COMERCIO sin contravencion rechaza")
        void comercioSinContravencionRechaza() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.COMERCIO, vehiculoValido(), null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("bloque prohibido: sustanciasAlimenticias informado en COMERCIO rechaza")
        void sustanciasProhibidasEnComercio() {
            assertThatThrownBy(() -> service.labrar(
                    comando(TipoActa.COMERCIO, null, null, contravencionValida(), sustanciasValidas())))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("sustanciasAlimenticias")
                    .hasMessageContaining("prohibido");
        }

        @Test
        @DisplayName("sin ningun satelite tipado: rechaza (Wave 4 Parte 3C, sin opt-in)")
        void sinNingunSateliteRechaza() {
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.COMERCIO, null, null, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("contravencion")
                    .hasMessageContaining("obligatorio");
        }
    }

    @Nested
    @DisplayName("§6.1 Alcoholemia coherente con siControlAlcoholemia")
    class Alcoholemia {

        @Test
        @DisplayName("siControlAlcoholemia=true con al menos una medicion: valido")
        void controlTrueConMedicionValido() {
            TransitoActaItem transito = new TransitoActaItem(null, null, null, null, false, false, true,
                    List.of(new AlcoholemiaMedicionItem(
                            (short) 1, TipoPruebaAlcoholemia.ALCOHOLIMETRO, null, null, null, null, null,
                            true, FaltasClockTestSupport.FIXED.now())));
            ComandoResultado res = service.labrar(comando(TipoActa.TRANSITO, null, transito, null, null));
            assertThat(res.idActa()).isNotNull();
        }

        @Test
        @DisplayName("siControlAlcoholemia=true sin mediciones: rechaza")
        void controlTrueSinMedicionesRechaza() {
            TransitoActaItem transito = new TransitoActaItem(null, null, null, null, false, false, true, List.of());
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.TRANSITO, null, transito, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("al menos una medicion");
        }

        @Test
        @DisplayName("siControlAlcoholemia=false con mediciones informadas: rechaza")
        void controlFalseConMedicionesRechaza() {
            TransitoActaItem transito = new TransitoActaItem(null, null, null, null, false, false, false,
                    List.of(new AlcoholemiaMedicionItem(
                            (short) 1, TipoPruebaAlcoholemia.ALCOHOLIMETRO, null, null, null, null, null,
                            true, FaltasClockTestSupport.FIXED.now())));
            assertThatThrownBy(() -> service.labrar(comando(TipoActa.TRANSITO, null, transito, null, null)))
                    .isInstanceOf(PrecondicionVioladaException.class)
                    .hasMessageContaining("no admite mediciones");
        }

        @Test
        @DisplayName("siControlAlcoholemia=false sin mediciones: valido")
        void controlFalseSinMedicionesValido() {
            ComandoResultado res = service.labrar(comando(TipoActa.TRANSITO, null, transitoSinAlcoholemia(), null, null));
            assertThat(res.idActa()).isNotNull();
        }
    }
}
