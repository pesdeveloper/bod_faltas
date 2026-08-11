package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.service.ValorizacionCalculador;
import ar.gob.malvinas.faltas.core.application.service.ValorizacionService;
import ar.gob.malvinas.faltas.core.domain.enums.ModoValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFirmaInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionIdentificacionInfractor;
import ar.gob.malvinas.faltas.core.domain.enums.TipoUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaArticuloInfringido;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacion;
import ar.gob.malvinas.faltas.core.domain.model.FalActaValorizacionItem;
import ar.gob.malvinas.faltas.core.domain.model.FalArticuloNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalNormativaFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalTarifarioUnidadFaltas;
import ar.gob.malvinas.faltas.core.domain.model.FalValorUnidadFaltas;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaArticuloInfringidoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionItemRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaValorizacionRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryNormativaRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryTarifarioUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryValorUnidadFaltasRepository;
import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Wave 4 PARTE-3A §14: "volver a automático histórico" y "actualizar a valores vigentes" en
 * {@link ValorizacionService}. Ambas operaciones crean una nueva versión inmutable del item
 * (nunca sobrescriben la anterior) y ajustan la cabecera por la diferencia exacta.
 *
 * <p>Escenario: un artículo con tarifario UNIDAD_FIJA estable (10 unidades) y dos valores de
 * unidad distintos en el tiempo: histórico (100, vigente 2020-2026) y vigente actual (150,
 * desde 2026-01-01). El alta calcula inicialmente con la fecha histórica (2025), de modo que
 * "volver a automático histórico" (100 → monto 1000) y "actualizar a valores vigentes"
 * (150 → monto 1500) producen resultados observablemente distintos.
 */
class ValorizacionOperacionesAutomaticasTest {

    private static final LocalDateTime FECHA_ALTA_HISTORICA = LocalDateTime.of(2025, 6, 1, 10, 0);
    private static final String ACTOR = "actor-test";

    private InMemoryActaValorizacionRepository valorizacionRepo;
    private InMemoryActaValorizacionItemRepository itemRepo;
    private InMemoryActaArticuloInfringidoRepository articuloInfringidoRepo;
    private InMemoryNormativaRepository normativaRepo;
    private InMemoryTarifarioUnidadFaltasRepository tarifarioRepo;
    private InMemoryValorUnidadFaltasRepository valorUnidadRepo;
    private InMemoryActaRepository actaRepo;
    private ValorizacionService service;

    private Long idActa;
    private Long idArticulo;
    private Long actaArticuloId;
    private Long idValorHistorico;
    private Long idValorVigente;
    private Long idTarifario;

    @BeforeEach
    void setUp() {
        valorizacionRepo = new InMemoryActaValorizacionRepository();
        itemRepo = new InMemoryActaValorizacionItemRepository();
        articuloInfringidoRepo = new InMemoryActaArticuloInfringidoRepository();
        normativaRepo = new InMemoryNormativaRepository();
        tarifarioRepo = new InMemoryTarifarioUnidadFaltasRepository();
        valorUnidadRepo = new InMemoryValorUnidadFaltasRepository();
        actaRepo = new InMemoryActaRepository();
        service = new ValorizacionService(
                valorizacionRepo, itemRepo, articuloInfringidoRepo, normativaRepo,
                tarifarioRepo, valorUnidadRepo, actaRepo, new ValorizacionCalculador(),
                FaltasClockTestSupport.FIXED);

        // Acta minima (situacion sin persona: no ejercita el resto del agregado).
        FalActa acta = new FalActa(
                null, java.util.UUID.randomUUID().toString(),
                ar.gob.malvinas.faltas.core.domain.enums.TipoActa.TRANSITO, 1L, 1L,
                FECHA_ALTA_HISTORICA.toLocalDate(), FECHA_ALTA_HISTORICA,
                "dom", null, null, null,
                ResultadoFirmaInfractor.SE_NIEGA_A_FIRMAR, SituacionIdentificacionInfractor.AUSENTE,
                null, FECHA_ALTA_HISTORICA, ACTOR);
        idActa = actaRepo.guardar(acta).getId();

        FalNormativaFaltas normativa = new FalNormativaFaltas(
                1L, "COD001", 1, "Normativa test",
                LocalDate.of(2020, 1, 1), FECHA_ALTA_HISTORICA, ACTOR);
        normativaRepo.guardarNormativa(normativa);

        FalArticuloNormativaFaltas articulo = new FalArticuloNormativaFaltas(
                1L, normativa.getId(), "ART001", 1, "Articulo test",
                ar.gob.malvinas.faltas.core.domain.enums.TipoInfraccion.TRANSITO,
                LocalDate.of(2020, 1, 1), FECHA_ALTA_HISTORICA, ACTOR);
        normativaRepo.guardarArticulo(articulo);
        idArticulo = articulo.getId();

        idTarifario = tarifarioRepo.nextId();
        FalTarifarioUnidadFaltas tarifario = new FalTarifarioUnidadFaltas(
                idTarifario, idArticulo, TipoUnidadFaltas.UNIDAD_FIJA,
                new BigDecimal("10"), null, false, null, null,
                LocalDate.of(2020, 1, 1), FECHA_ALTA_HISTORICA, ACTOR);
        tarifarioRepo.save(tarifario);

        idValorHistorico = valorUnidadRepo.nextId();
        FalValorUnidadFaltas valorHistorico = new FalValorUnidadFaltas(
                idValorHistorico, TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("100"),
                LocalDate.of(2020, 1, 1), FECHA_ALTA_HISTORICA, ACTOR);
        valorHistorico.setFhVigHasta(LocalDate.of(2026, 1, 1));
        valorHistorico.setSiActiva(false);
        valorUnidadRepo.save(valorHistorico);

        idValorVigente = valorUnidadRepo.nextId();
        FalValorUnidadFaltas valorVigente = new FalValorUnidadFaltas(
                idValorVigente, TipoUnidadFaltas.UNIDAD_FIJA, new BigDecimal("150"),
                LocalDate.of(2026, 1, 1), FECHA_ALTA_HISTORICA, ACTOR);
        valorUnidadRepo.save(valorVigente);

        FalActaArticuloInfringido actaArticulo = new FalActaArticuloInfringido(
                articuloInfringidoRepo.nextId(), idActa, normativa.getId(), idArticulo,
                FECHA_ALTA_HISTORICA, ACTOR);
        actaArticuloId = articuloInfringidoRepo.save(actaArticulo).getId();

        // Calculo inicial "historico": usa hoy=2025-06-01 -> resuelve valorUnidad=100.
        service.calcularInicial(idActa, ACTOR, FECHA_ALTA_HISTORICA);
    }

    @Test
    @DisplayName("1. Calculo inicial usa el valor de unidad vigente en la fecha del alta (historico=100)")
    void calculoInicialUsaValorHistorico() {
        FalActaValorizacionItem actual = itemRepo.findActualByActaArticulo(actaArticuloId).orElseThrow();
        assertThat(actual.getVersionItem()).isEqualTo(1);
        assertThat(actual.getModoOrdinaria()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
        assertThat(actual.getValorUnidadUsadoId()).isEqualTo(idValorHistorico);

        FalActaValorizacion header = valorizacionRepo.findByActaId(idActa).orElseThrow();
        assertThat(header.getMontoTotalOrdinario()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("2. Actualizar a valores vigentes recalcula con el valor de unidad ACTUAL (150), no el historico")
    void actualizarAValoresVigentesUsaValorActual() {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

        FalActaValorizacionItem nueva = service.actualizarAValoresVigentes(actaArticuloId, ACTOR, ahora);

        assertThat(nueva.getVersionItem()).isEqualTo(2);
        assertThat(nueva.isSiActual()).isTrue();
        assertThat(nueva.getModoOrdinaria()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
        assertThat(nueva.getValorUnidadUsadoId()).as("usa el valor vigente, no el historico").isEqualTo(idValorVigente);
        assertThat(nueva.getTarifarioArticuloUsadoId()).isEqualTo(idTarifario);

        FalActaValorizacion header = valorizacionRepo.findByActaId(idActa).orElseThrow();
        assertThat(header.getMontoTotalOrdinario()).as("10 unidades x 150 = 1500").isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("3. Volver a automático histórico tras una manualización recalcula con la referencia YA USADA, no con la vigente")
    void volverAAutomaticoHistoricoUsaReferenciaPreservada() {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();

        // Manualiza el item (p. ej. correccion administrativa): la referencia historica
        // (tarifario/valorUnidad=100) se preserva en la version manual.
        FalActaValorizacionItem manual = service.manualizarItem(
                actaArticuloId, ModoValorizacionItem.MANUAL_MONTO, null, new BigDecimal("9999"),
                null, null, null, "correccion manual de prueba", null, ACTOR, ahora);
        assertThat(manual.getValorUnidadUsadoId()).as("preserva la referencia historica").isEqualTo(idValorHistorico);

        FalActaValorizacionItem vuelta = service.volverAAutomaticoHistorico(actaArticuloId, ACTOR, ahora);

        assertThat(vuelta.getVersionItem()).isEqualTo(3);
        assertThat(vuelta.getModoOrdinaria()).isEqualTo(ModoValorizacionItem.AUTOMATICA);
        assertThat(vuelta.getValorUnidadUsadoId()).as("usa la referencia HISTORICA preservada (100), no la vigente (150)")
                .isEqualTo(idValorHistorico);
        assertThat(vuelta.getCantidadManualOrdinaria()).isNull();
        assertThat(vuelta.getMontoManualOrdinario()).isNull();

        FalActaValorizacion header = valorizacionRepo.findByActaId(idActa).orElseThrow();
        assertThat(header.getMontoTotalOrdinario())
                .as("10 unidades x 100 (historico) = 1000, no 1500 (vigente)")
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("4. Cada operacion crea una version inmutable nueva: nunca sobrescribe la anterior, y solo una es actual")
    void cadaOperacionEsInmutableYUnaSolaEsActual() {
        LocalDateTime ahora = FaltasClockTestSupport.FIXED.now();
        FalActaValorizacionItem v1 = itemRepo.findActualByActaArticulo(actaArticuloId).orElseThrow();

        FalActaValorizacionItem v2 = service.actualizarAValoresVigentes(actaArticuloId, ACTOR, ahora);
        FalActaValorizacionItem v3 = service.volverAAutomaticoHistorico(actaArticuloId, ACTOR, ahora);

        assertThat(itemRepo.findById(v1.getId()).orElseThrow().isSiActual())
                .as("v1 ya no es actual, pero la fila sigue existiendo (inmutable)").isFalse();
        assertThat(itemRepo.findById(v2.getId()).orElseThrow().isSiActual())
                .as("v2 ya no es actual").isFalse();
        assertThat(itemRepo.findActualByActaArticulo(actaArticuloId).orElseThrow().getId())
                .as("solo v3 es la version actual").isEqualTo(v3.getId());
        assertThat(itemRepo.findActualesByActaArticuloIds(java.util.List.of(actaArticuloId)))
                .as("un solo item actual por articulo").hasSize(1);
    }

    @Test
    @DisplayName("5. Sin item actual previo, ambas operaciones fallan con precondicion violada (no hay nada que recalcular)")
    void sinItemActualFallaConPrecondicion() {
        Long otroActaArticuloId = articuloInfringidoRepo.nextId();
        assertThatThrownBy(() -> service.volverAAutomaticoHistorico(otroActaArticuloId, ACTOR, FaltasClockTestSupport.FIXED.now()))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("No hay item de valorizacion actual");
        assertThatThrownBy(() -> service.actualizarAValoresVigentes(otroActaArticuloId, ACTOR, FaltasClockTestSupport.FIXED.now()))
                .isInstanceOf(PrecondicionVioladaException.class)
                .hasMessageContaining("No hay item de valorizacion actual");
    }
}
