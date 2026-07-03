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
 * 8F-4C: Prueba funcional de paralizacion.
 *
 * Cubre:
 *   ACT-020-PARALIZADA               - Acta paralizada (ACTPAR)
 *
 * Gap cubierto en 8F-4C: implementacion de ActaParalizacionService.
 */
@DisplayName("8F-4C: ActaFlujoParalizacionFuncional")
class ActaFlujoParalizacionFuncionalTest {

    @Nested
    @DisplayName("ACT-020-PARALIZADA")
    class Act020 {

        @Test
        @DisplayName("Paralizar acta activa: evento ACTPAR, situacion PARALIZADA, bandeja PARALIZADAS")
        void paralizar_acta_activa() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-020-PARALIZADA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaParalizada()).isTrue();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.PARALIZADA);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PARALIZADAS);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.ACTPAR);
        }

        @Test
        @DisplayName("Paralizada coincide con definicion del dataset: paralizadaFinal=true")
        void paralizada_final_verdadero() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-020-PARALIZADA");

            assertThat(res.paralizadaFinal()).isTrue();
        }

        @Test
        @DisplayName("Bloque ANAL se preserva post-paralizacion (paralizacion no cambia bloque)")
        void bloque_anal_preservado_tras_paralizacion() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-020-PARALIZADA");

            assertThat(res.bloqueFinal()).isEqualTo(BloqueActual.ANAL.codigo());
        }
    }
}
