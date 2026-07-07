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
 * 8F-4C: Pruebas funcionales de pago voluntario.
 *
 * Cubre:
 *   ACT-007-PAGVOL-SOLICITADO        - Pago voluntario solicitado (PAGVSO)
 *   ACT-008-PAGVOL-PENDIENTE-CONF    - Pago voluntario informado (PAGINF)
 *   ACT-009-PAGVOL-CONFIRMADO        - Pago voluntario confirmado (PAGCNF+CIERRA)
 */
@DisplayName("8F-4C: ActaFlujoPagoVoluntarioFuncional")
class ActaFlujoPagoVoluntarioFuncionalTest {

    @Nested
    @DisplayName("ACT-007-PAGVOL-SOLICITADO")
    class Act007 {

        @Test
        @DisplayName("Pago voluntario solicitado: evento PAGVSO, acta no cerrada")
        void pagvol_solicitado_evento_pagvso() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-007-PAGVOL-SOLICITADO");

            assertThat(res.ejecutado()).isTrue();

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PAGVSO);

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
        }
    }

    @Nested
    @DisplayName("ACT-008-PAGVOL-PENDIENTE-CONF")
    class Act008 {

        @Test
        @DisplayName("Pago voluntario informado: PAGVSO+PAGVMF+PAGINF, bandeja PENDIENTE_CONFIRMACION_PAGO")
        void pagvol_informado_pendiente_confirmacion() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-008-PAGVOL-PENDIENTE-CONF");

            assertThat(res.ejecutado()).isTrue();

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PAGVSO, TipoEventoActa.PAGVMF, TipoEventoActa.PAGINF);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO);

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
        }
    }

    @Nested
    @DisplayName("ACT-009-PAGVOL-CONFIRMADO")
    class Act009 {

        @Test
        @DisplayName("Pago voluntario confirmado: PAGCNF+CIERRA, acta CERRADA, bandeja CERRADAS")
        void pagvol_confirmado_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-009-PAGVOL-CONFIRMADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CERRADAS);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PAGCNF, TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("ResultadoFinal coincide con definicion del dataset")
        void resultado_final_coincide_definicion() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-009-PAGVOL-CONFIRMADO");

            assertThat(res.resultadoFinal())
                    .isEqualTo(ResultadoFinalActa.PAGO_VOLUNTARIO_PAGADO.name());
        }
    }
}