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
 * 8F-4C: Pruebas funcionales de bloqueantes materiales.
 *
 * Cubre:
 *   ACT-021-BLOQUEANTE-ACTIVO        - Bloqueante activo registrado
 *   ACT-022-ABSUELTO-CON-BLOQUEANTE  - Absuelto pero ACTIVA por bloqueante
 */
@DisplayName("8F-4C: ActaFlujoBloqueanteFuncional")
class ActaFlujoBloqueanteFuncionalTest {

    @Nested
    @DisplayName("ACT-021-BLOQUEANTE-ACTIVO")
    class Act021 {

        @Test
        @DisplayName("Bloqueante material activo registrado en acta en ANAL")
        void bloqueante_activo_registrado() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-021-BLOQUEANTE-ACTIVO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            boolean tieneBloqueante = runner.getBloqueanteMaterialRepo()
                    .existsActivoByActaId(res.actaId());
            assertThat(tieneBloqueante).isTrue();
        }
    }

    @Nested
    @DisplayName("ACT-022-ABSUELTO-CON-BLOQUEANTE")
    class Act022 {

        @Test
        @DisplayName("Absuelto con bloqueante: resultadoFinal=ABSUELTO pero situacion=ACTIVA (no CERRADA)")
        void absuelto_con_bloqueante_activa_no_cerrada() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-022-ABSUELTO-CON-BLOQUEANTE");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("Bloqueante activo impide emision de CIERRA aunque resultado sea ABSUELTO")
        void bloqueante_impide_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-022-ABSUELTO-CON-BLOQUEANTE");

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.NOTPOS);
            assertThat(tipos).doesNotContain(TipoEventoActa.CIERRA);

            boolean tieneBloqueante = runner.getBloqueanteMaterialRepo()
                    .existsActivoByActaId(res.actaId());
            assertThat(tieneBloqueante).isTrue();
        }
    }
}
