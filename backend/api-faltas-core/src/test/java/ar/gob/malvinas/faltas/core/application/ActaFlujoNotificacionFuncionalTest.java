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
 * 8F-4C: Pruebas funcionales de notificacion.
 *
 * Cubre:
 *   ACT-005-NOTI-ACTA-EN-CURSO       - Notificacion enviada, esperando resultado
 *   ACT-006-ANAL-LISTA-FALLO         - Notificacion positiva -> ANAL
 *   ACT-013-FALLO-COND-NOTIFICADO    - Fallo condenatorio notificado
 *   ACT-026-NOTIFICACION-NEGATIVA    - Notificacion negativa
 */
@DisplayName("8F-4C: ActaFlujoNotificacionFuncional")
class ActaFlujoNotificacionFuncionalTest {

    @Nested
    @DisplayName("ACT-005-NOTI-ACTA-EN-CURSO")
    class Act005 {

        @Test
        @DisplayName("Notificacion enviada: bloque NOTI, bandeja EN_NOTIFICACION, evento NOTENV")
        void notificacion_enviada_bloque_noti() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-005-NOTI-ACTA-EN-CURSO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.NOTI);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.EN_NOTIFICACION);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.NOTENV);
            assertThat(tipos).doesNotContain(TipoEventoActa.NOTPOS, TipoEventoActa.NOTNEG);
        }
    }

    @Nested
    @DisplayName("ACT-006-ANAL-LISTA-FALLO")
    class Act006 {

        @Test
        @DisplayName("Notificacion positiva de acta: bloque ANAL, bandeja PENDIENTE_ANALISIS")
        void notif_positiva_mueve_a_anal() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-006-ANAL-LISTA-FALLO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_ANALISIS);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.NOTPOS);
        }
    }

    @Nested
    @DisplayName("ACT-013-FALLO-COND-NOTIFICADO")
    class Act013 {

        @Test
        @DisplayName("Fallo condenatorio notificado: bloque ANAL, bandeja PENDIENTES_FALLO")
        void fallo_cond_notificado_bandeja_pendientes_fallo() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-013-FALLO-COND-NOTIFICADO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTES_FALLO);

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.FALCON);
            assertThat(tipos).contains(TipoEventoActa.NOTPOS);
        }
    }

    @Nested
    @DisplayName("ACT-026-NOTIFICACION-NEGATIVA")
    class Act026 {

        @Test
        @DisplayName("Notificacion negativa: evento NOTNEG, bloque ANAL, bandeja PENDIENTE_ANALISIS")
        void notif_negativa_bloque_anal() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-026-NOTIFICACION-NEGATIVA");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ANAL);
            assertThat(acta.estaCerrada()).isFalse();

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.NOTNEG);
            assertThat(tipos).doesNotContain(TipoEventoActa.NOTPOS);
        }

        @Test
        @DisplayName("NOTNEG no tiene resultado CERRADA")
        void notneg_no_cierra() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-026-NOTIFICACION-NEGATIVA");

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }
    }
}
