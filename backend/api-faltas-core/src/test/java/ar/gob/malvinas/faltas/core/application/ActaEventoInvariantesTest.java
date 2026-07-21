package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.ActaConsistencyChecker;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaEventoRepository;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica invariantes de FalActa y FalActaEvento usando ActaConsistencyChecker.
 * Slice 8F-11L.
 */
@DisplayName("ActaEventoInvariantes - Verificacion de consistencia del agregado")
class ActaEventoInvariantesTest {

    private ActaConsistencyChecker checker;
    private InMemoryActaRepository actaRepo;
    private InMemoryActaEventoRepository eventoRepo;

    @BeforeEach
    void setUp() {
        checker = new ActaConsistencyChecker();
        actaRepo = new InMemoryActaRepository();
        eventoRepo = new InMemoryActaEventoRepository();
    }

    private FalActa crearActa() {
        Long id = actaRepo.nextId();
        FalActa acta = new FalActa(id, "uuid-" + id, TipoActa.TRANSITO, 1L, 1L,
                FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                "Calle Test", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                FaltasClockTestSupport.FIXED.now(), "TEST");
        return actaRepo.guardar(acta);
    }

    private FalActaEvento registrarActlab(FalActa acta) {
        FalActaEvento evt = FalActaEvento.builder()
                .actaId(acta.getId())
                .tipoEvt(TipoEventoActa.ACTLAB)
                .origenEvt(OrigenEvento.USUARIO_WEB)
                .fhEvt(FaltasClockTestSupport.FIXED.now())
                .actorTipo(ActorTipoEvento.USUARIO_INTERNO)
                .idUserEvt("TEST")
                .descripcionLegible("Acta labrada")
                .build();
        return eventoRepo.registrar(evt);
    }

    @Nested
    @DisplayName("Invariantes de identidad del acta")
    class InvariantesIdentidad {

        @Test
        @DisplayName("Acta correctamente inicializada es consistente")
        void acta_correcta_es_consistente() {
            FalActa acta = crearActa();
            FalActaEvento evt = registrarActlab(acta);
            List<FalActaEvento> eventos = eventoRepo.buscarPorActa(acta.getId());
            assertThat(checker.esConsistente(acta, eventos)).isTrue();
        }

        @Test
        @DisplayName("Acta sin uuid tecnico registra violacion")
        void acta_sin_uuid() {
            Long id = actaRepo.nextId();
            FalActa acta = new FalActa(id, "", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Calle Test", null, null, null, ResultadoFirmaInfractor.FIRMADA, null,
                    FaltasClockTestSupport.FIXED.now(), "TEST");
            actaRepo.guardar(acta);
            registrarActlab(acta);
            var violaciones = checker.verificar(acta, eventoRepo.buscarPorActa(acta.getId()));
            assertThat(violaciones).anyMatch(viol -> viol.regla().equals("UUID_NULO"));
        }
    }

    @Nested
    @DisplayName("Invariantes de FalActaEvento")
    class InvariantesEvento {

        @Test
        @DisplayName("FalActaEvento builder construye con campos obligatorios")
        void builder_campos_obligatorios() {
            FalActa acta = crearActa();
            FalActaEvento evt = FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .origenEvt(OrigenEvento.USUARIO_WEB)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .actorTipo(ActorTipoEvento.USUARIO_INTERNO)
                    .descripcionLegible("Test")
                    .build();
            assertThat(evt.actaId()).isEqualTo(acta.getId());
            assertThat(evt.tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(evt.fhEvt()).isNotNull();
        }

        @Test
        @DisplayName("FalActaEvento registrado obtiene id Long auto-asignado")
        void evento_obtiene_id_auto() {
            FalActa acta = crearActa();
            FalActaEvento evt = FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .origenEvt(OrigenEvento.USUARIO_WEB)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .actorTipo(ActorTipoEvento.SISTEMA)
                    .build();
            FalActaEvento guardado = eventoRepo.registrar(evt);
            assertThat(guardado.getId()).isNotNull().isGreaterThan(0L);
        }

        @Test
        @DisplayName("FalActaEvento tiene siEvtCierre=false por defecto")
        void evento_si_evt_cierre_default_false() {
            FalActaEvento evt = FalActaEvento.builder()
                    .actaId(1L)
                    .tipoEvt(TipoEventoActa.DOCGEN)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .build();
            assertThat(evt.siEvtCierre()).isFalse();
            assertThat(evt.siEvtExt()).isFalse();
            assertThat(evt.siPermiteReing()).isFalse();
        }

        @Test
        @DisplayName("siEvtCierre=true puede ser seteado en el builder")
        void evento_puede_marcar_cierre() {
            FalActaEvento evt = FalActaEvento.builder()
                    .actaId(1L)
                    .tipoEvt(TipoEventoActa.FALABS)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .siEvtCierre(true)
                    .descripcionLegible("Cierre por fallo absolutorio")
                    .build();
            assertThat(evt.siEvtCierre()).isTrue();
        }

        @Test
        @DisplayName("Eventos se ordenan cronologicamente al buscar por acta")
        void eventos_en_orden_cronologico() {
            FalActa acta = crearActa();
            LocalDateTime t1 = FaltasClockTestSupport.FIXED.now().minusMinutes(5);
            LocalDateTime t2 = FaltasClockTestSupport.FIXED.now().minusMinutes(3);
            LocalDateTime t3 = FaltasClockTestSupport.FIXED.now();

            eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId()).tipoEvt(TipoEventoActa.ACTLAB).fhEvt(t1).build());
            eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId()).tipoEvt(TipoEventoActa.ACTCAP).fhEvt(t2).build());
            eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId()).tipoEvt(TipoEventoActa.ACTENR).fhEvt(t3).build());

            List<FalActaEvento> ordenados = eventoRepo.buscarPorActa(acta.getId());
            assertThat(ordenados).hasSize(3);
            assertThat(ordenados.get(0).tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(ordenados.get(1).tipoEvt()).isEqualTo(TipoEventoActa.ACTCAP);
            assertThat(ordenados.get(2).tipoEvt()).isEqualTo(TipoEventoActa.ACTENR);
        }

        @Test
        @DisplayName("existeCorrelacion detecta duplicados por correlacionId")
        void existe_correlacion_detecta_duplicados() {
            FalActa acta = crearActa();
            String correlId = "CORREL-TEST-001";

            eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.NOTENV)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .correlacionId(correlId)
                    .build());

            assertThat(eventoRepo.existeCorrelacion(acta.getId(), correlId)).isTrue();
            assertThat(eventoRepo.existeCorrelacion(acta.getId(), "OTRO-ID")).isFalse();
        }
    }

    @Nested
    @DisplayName("Invariantes de ConsistencyChecker")
    class InvariantesChecker {

        @Test
        @DisplayName("Acta con evento ajeno registra violacion EVENTO_AJENO")
        void evento_ajeno_detectado() {
            FalActa acta = crearActa();
            FalActa otraActa = crearActa();
            registrarActlab(acta);

            // Crear evento perteneciente a otra acta
            FalActaEvento eventoAjeno = eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(otraActa.getId())
                    .tipoEvt(TipoEventoActa.ACTLAB)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .build());

            List<FalActaEvento> eventosIncorrectos = List.of(eventoAjeno);
            var violaciones = checker.verificar(acta, eventosIncorrectos);
            assertThat(violaciones).anyMatch(v -> v.regla().equals("EVENTO_AJENO"));
        }

        @Test
        @DisplayName("Primer evento distinto de ACTLAB registra violacion")
        void primer_evento_invalido() {
            FalActa acta = crearActa();
            FalActaEvento eventoInvalido = eventoRepo.registrar(FalActaEvento.builder()
                    .actaId(acta.getId())
                    .tipoEvt(TipoEventoActa.DOCGEN)
                    .fhEvt(FaltasClockTestSupport.FIXED.now())
                    .build());
            var violaciones = checker.verificar(acta, List.of(eventoInvalido));
            assertThat(violaciones).anyMatch(v -> v.regla().equals("PRIMER_EVENTO_INVALIDO"));
        }

        @Test
        @DisplayName("esConsistente retorna false cuando hay violaciones")
        void es_consistente_false_con_violaciones() {
            FalActa acta = crearActa();
            assertThat(checker.esConsistente(acta, List.of())).isTrue();
        }
    }
}
