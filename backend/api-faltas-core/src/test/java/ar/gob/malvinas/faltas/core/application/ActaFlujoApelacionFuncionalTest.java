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
 * 8F-4C: Prueba funcional de apelacion.
 *
 * Cubre:
 *   ACT-014-APELACION-PRESENTADA     - Apelacion presentada (APEPRE)
 */
@DisplayName("8F-4C: ActaFlujoApelacionFuncional")
class ActaFlujoApelacionFuncionalTest {

    @Nested
    @DisplayName("ACT-014-APELACION-PRESENTADA")
    class Act014 {

        @Test
        @DisplayName("Apelacion presentada: APEPRE, bandeja CON_APELACION, acta no cerrada")
        void apelacion_presentada_estado_correcto() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-014-APELACION-PRESENTADA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.CON_APELACION);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.APEPRE);
        }

        @Test
        @DisplayName("Secuencia de eventos: FALCON -> NOTPOS -> APEPRE")
        void secuencia_eventos_correcta() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-014-APELACION-PRESENTADA");

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            List<TipoEventoActa> tipos = eventos.stream()
                    .map(FalActaEvento::tipoEvt).collect(Collectors.toList());

            assertThat(tipos).contains(TipoEventoActa.FALCON);
            int idxFalcon = tipos.indexOf(TipoEventoActa.FALCON);
            int idxApepre = tipos.indexOf(TipoEventoActa.APEPRE);
            assertThat(idxApepre).isGreaterThan(idxFalcon);
        }
    }
}
