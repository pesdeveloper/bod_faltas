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
 * 8F-4C: Pruebas funcionales de fallo.
 *
 * Cubre:
 *   ACT-010-FALLO-ABS-DICTADO        - Fallo absolutorio dictado (FALABS+DOCGEN)
 *   ACT-011-ABSUELTO-CERRADO         - Absuelto y cerrado (NOTPOS+CIERRA)
 *   ACT-012-FALLO-COND-DICTADO       - Fallo condenatorio dictado (FALCON+DOCGEN)
 *   ACT-015-CONDENA-FIRME            - Condena firme (PLAVNC+CONFIR)
 *   ACT-028-ABSOLUCION-FIRME-CERRADA - Absolucion firme cerrada definitivamente
 */
@DisplayName("8F-4C: ActaFlujoFalloFuncional")
class ActaFlujoFalloFuncionalTest {

    @Nested
    @DisplayName("ACT-010-FALLO-ABS-DICTADO")
    class Act010 {

        @Test
        @DisplayName("Fallo absolutorio dictado: FALABS+DOCGEN, bloque ANAL, bandeja PENDIENTE_FIRMA")
        void fallo_abs_dictado_pendiente_firma() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-010-FALLO-ABS-DICTADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.FALABS, TipoEventoActa.DOCGEN);
        }
    }

    @Nested
    @DisplayName("ACT-011-ABSUELTO-CERRADO")
    class Act011 {

        @Test
        @DisplayName("Absolucion notificada sin bloqueantes: ABSUELTO, CERRADA, bandeja CERRADAS")
        void absolucion_sin_bloqueantes_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-011-ABSUELTO-CERRADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.CERRADA);
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.CERR);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.FALABS, TipoEventoActa.NOTPOS, TipoEventoActa.CIERRA);
        }
    }

    @Nested
    @DisplayName("ACT-012-FALLO-COND-DICTADO")
    class Act012 {

        @Test
        @DisplayName("Fallo condenatorio dictado: FALCON+DOCGEN, bloque ANAL, bandeja PENDIENTE_FIRMA")
        void fallo_cond_dictado_pendiente_firma() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-012-FALLO-COND-DICTADO");

            assertThat(res.ejecutado()).isTrue();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_FIRMA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.FALCON, TipoEventoActa.DOCGEN);

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
        }
    }

    @Nested
    @DisplayName("ACT-015-CONDENA-FIRME")
    class Act015 {

        @Test
        @DisplayName("Condena firme: PLAVNC+CONFIR, resultadoFinal=CONDENA_FIRME, bandeja PENDIENTE_PAGO_CONDENA")
        void condena_firme_resultado_correcto() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-015-CONDENA-FIRME");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.CONDENA_FIRME);
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_PAGO_CONDENA);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.PLAVNC, TipoEventoActa.CONFIR);
        }
    }

    @Nested
    @DisplayName("ACT-028-ABSOLUCION-FIRME-CERRADA")
    class Act028 {

        @Test
        @DisplayName("Absolucion firme cerrada: flujo completo identico a ACT-011")
        void absolucion_firme_cerrada_identica_a_011() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-028-ABSOLUCION-FIRME-CERRADA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isTrue();
            assertThat(acta.getResultadoFinal()).isEqualTo(ResultadoFinalActa.ABSUELTO);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.CIERRA);
        }
    }
}
