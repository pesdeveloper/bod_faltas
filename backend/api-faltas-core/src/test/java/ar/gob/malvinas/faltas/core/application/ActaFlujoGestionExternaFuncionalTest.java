package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.CasoUsoFuncionalRunner;
import ar.gob.malvinas.faltas.core.application.result.CasoUsoFuncionalEjecucionResultado;
import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8F-4C: Pruebas funcionales de gestion externa.
 *
 * Cubre:
 *   ACT-018-GESTION-EXTERNA          - Derivada a gestion externa (EXTDER)
 *   ACT-019-GESTION-EXTERNA-PAGO-EXTERNO - Pago externo por apremio (PAGAPR)
 */
@DisplayName("8F-4C: ActaFlujoGestionExternaFuncional")
class ActaFlujoGestionExternaFuncionalTest {

    @Nested
    @DisplayName("ACT-018-GESTION-EXTERNA")
    class Act018 {

        @Test
        @DisplayName("Derivada a gestion externa: EXTDER, bloque GEXT, bandeja GESTION_EXTERNA")
        void derivada_gestion_externa_estado_correcto() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-018-GESTION-EXTERNA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.GEXT);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.GESTION_EXTERNA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.EXTDER);
            assertThat(tipos).doesNotContain(TipoEventoActa.EXTRET);
        }
    }

    @Nested
    @DisplayName("ACT-019-GESTION-EXTERNA-PAGO-EXTERNO")
    class Act019 {

        @Test
        @DisplayName("Pago externo apremio: PAGAPR, resultadoFinal=CONDENA_FIRME_PAGADA")
        void pago_externo_apremio_condena_firme_pagada() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-019-GESTION-EXTERNA-PAGO-EXTERNO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.EXTDER, TipoEventoActa.PAGAPR);
        }

        @Test
        @DisplayName("Secuencia EXTDER->PAGAPR respetada en eventos")
        void secuencia_extder_pagapr() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-019-GESTION-EXTERNA-PAGO-EXTERNO");

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            List<TipoEventoActa> tipos = eventos.stream()
                    .map(FalActaEvento::tipoEvt).collect(Collectors.toList());

            int idxExtder = tipos.indexOf(TipoEventoActa.EXTDER);
            int idxPagapr = tipos.indexOf(TipoEventoActa.PAGAPR);
            assertThat(idxExtder).isGreaterThanOrEqualTo(0);
            assertThat(idxPagapr).isGreaterThan(idxExtder);
        }
    }
}
