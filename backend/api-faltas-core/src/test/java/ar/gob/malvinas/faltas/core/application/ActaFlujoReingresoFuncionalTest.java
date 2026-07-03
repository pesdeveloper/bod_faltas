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
 * 8F-4C: Prueba funcional de reingreso desde gestion externa.
 *
 * Cubre:
 *   ACT-029-REINGRESO-PARA-REVISION  - Reingresada desde gestion externa (EXTRET)
 */
@DisplayName("8F-4C: ActaFlujoReingresoFuncional")
class ActaFlujoReingresoFuncionalTest {

    @Nested
    @DisplayName("ACT-029-REINGRESO-PARA-REVISION")
    class Act029 {

        @Test
        @DisplayName("Reingreso desde gestion externa: EXTRET, bloque ANAL, ACTIVA, CONDENA_FIRME")
        void reingreso_para_revision_estado_correcto() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-029-REINGRESO-PARA-REVISION");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.EXTDER, TipoEventoActa.EXTRET);
        }

        @Test
        @DisplayName("Secuencia EXTDER->EXTRET respetada")
        void secuencia_extder_extret() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-029-REINGRESO-PARA-REVISION");

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            List<TipoEventoActa> tipos = eventos.stream()
                    .map(FalActaEvento::tipoEvt).collect(Collectors.toList());

            int idxExtder = tipos.indexOf(TipoEventoActa.EXTDER);
            int idxExtret = tipos.lastIndexOf(TipoEventoActa.EXTRET);
            assertThat(idxExtder).isGreaterThanOrEqualTo(0);
            assertThat(idxExtret).isGreaterThan(idxExtder);
        }

        @Test
        @DisplayName("CONDENA_FIRME se preserva post-reingreso")
        void condena_firme_preservada_post_reingreso() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-029-REINGRESO-PARA-REVISION");

            assertThat(res.resultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME.name());
        }
    }
}
