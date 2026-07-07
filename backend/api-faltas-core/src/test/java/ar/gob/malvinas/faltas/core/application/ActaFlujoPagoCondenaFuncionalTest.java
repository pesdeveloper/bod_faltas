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
 * 8F-4C: Pruebas funcionales de pago de condena.
 *
 * Cubre:
 *   ACT-016-PAGO-CONDENA-INFORMADO   - Pago condena informado (PCOINF)
 *   ACT-017-CONDENA-FIRME-PAGADA     - Condena firme pagada y cerrada (PCOCNF+CIERRA)
 *   ACT-030-PAGO-CONDENA-OBSERVADO   - Pago condena observado (PCOOBS)
 *   ACT-031-PAGO-CONDENA-CON-DESCUENTO - Pago con descuento (PCOCNF variante)
 */
@DisplayName("8F-4C: ActaFlujoPagoCondenaFuncional")
class ActaFlujoPagoCondenaFuncionalTest {

    @Nested
    @DisplayName("ACT-016-PAGO-CONDENA-INFORMADO")
    class Act016 {

        @Test
        @DisplayName("Pago condena informado: PCOINF, bandeja PENDIENTE_CONFIRMACION_PAGO_CONDENA")
        void pago_condena_informado_estado_correcto() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-016-PAGO-CONDENA-INFORMADO");

            assertThat(res.ejecutado()).isTrue();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_CONFIRMACION_PAGO_CONDENA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PCOINF);
            assertThat(tipos).doesNotContain(TipoEventoActa.PCOCNF);
        }
    }

    @Nested
    @DisplayName("ACT-017-CONDENA-FIRME-PAGADA")
    class Act017 {

        @Test
        @DisplayName("Condena firme pagada: PCOCNF+CIERRA, CERRADA, resultadoFinal=CONDENA_FIRME_PAGADA")
        void CONDENA_FIRME_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-017-CONDENA-FIRME-PAGADA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PCOCNF, TipoEventoActa.CIERRA);
        }
    }

    @Nested
    @DisplayName("ACT-030-PAGO-CONDENA-OBSERVADO")
    class Act030 {

        @Test
        @DisplayName("Pago condena observado: PCOOBS, acta no cerrada, condena firme pendiente de pago")
        void pago_condena_observado_no_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-030-PAGO-CONDENA-OBSERVADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PCOOBS);
            assertThat(tipos).doesNotContain(TipoEventoActa.PCOCNF);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);
        }
    }

    @Nested
    @DisplayName("ACT-031-PAGO-CONDENA-CON-DESCUENTO")
    class Act031 {

        @Test
        @DisplayName("Pago condena con descuento: usa PCOCNF sin evento separado (decision dominio)")
        void pago_condena_descuento_usa_pcocnf_sin_evento_propio() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-031-PAGO-CONDENA-CON-DESCUENTO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME_PAGADA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PCOCNF, TipoEventoActa.CIERRA);
        }

        @Test
        @DisplayName("Descuento no tiene evento propio: no existe PCODESCT como TipoEventoActa")
        void descuento_no_tiene_evento_propio() {
            for (TipoEventoActa t : TipoEventoActa.values()) {
                assertThat(t.name()).doesNotContain("DESCT");
                assertThat(t.codigo()).doesNotContain("DESCT");
            }
        }

        @Test
        @DisplayName("Advertencia del runner documenta la decision sobre descuento")
        void runner_documenta_decision_descuento() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-031-PAGO-CONDENA-CON-DESCUENTO");

            assertThat(res.advertencias()).anyMatch(a -> a.contains("descuento") || a.contains("PCOCNF"));
        }
    }
}
