package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.application.demo.CasoUsoFuncionalRunner;
import ar.gob.malvinas.faltas.core.application.demo.DatasetFuncionalDominioCatalog;
import ar.gob.malvinas.faltas.core.application.result.CasoUsoFuncionalEjecucionResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.CodigoBandeja;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalActaEvento;
import ar.gob.malvinas.faltas.core.domain.model.FalActaSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8F-4C: Pruebas funcionales del alta integral y del enriquecimiento del acta.
 *
 * <p>El alta es integral y no progresiva: el acta nace directamente en ENRI con
 * ACTLAB como unico evento inicial (DECISION-ACTA-LIFECYCLE-SIN-CAPT-01,
 * {@code 20-application/command-contracts.md}). Ya no existe {@code CAPT} ni
 * {@code ACTCAP}.
 *
 * Cubre:
 *   ACT-001-LABRADA                  - Acta recien labrada (ENRI, alta integral)
 *   ACT-002-EN-ENRIQUECIMIENTO       - Acta enriquecida (ENRI)
 *   ACT-004-PENDIENTE-NOTIFICACION   - Documento firmado, pendiente notificacion
 *
 * Cada test crea el acta desde cero ejecutando servicios reales in-memory.
 */
@DisplayName("8F-4C: ActaFlujoCapturaFuncional")
class ActaFlujoCapturaFuncionalTest {

    @Nested
    @DisplayName("ACT-001-LABRADA")
    class Act001 {

        @Test
        @DisplayName("Labrar crea acta directamente en ENRI con unico evento ACTLAB")
        void labrar_bloque_enri_evento_actlab() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-001-LABRADA");

            assertThat(res.ejecutado()).isTrue();
            assertThat(res.actaId()).isNotNull();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ENRI);
            assertThat(acta.estaCerrada()).isFalse();
            assertThat(acta.estaParalizada()).isFalse();
            assertThat(acta.getSituacionAdministrativa()).isEqualTo(SituacionAdministrativaActa.ACTIVA);

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            assertThat(eventos).hasSize(1);
            assertThat(eventos.get(0).tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.ACTA_NUEVA);
        }

        @Test
        @DisplayName("Bloque ENRI coincide con definicion del dataset funcional")
        void bloque_coincide_con_definicion() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-001-LABRADA");

            assertThat(res.bloqueFinal()).isEqualTo(BloqueActual.ENRI.codigo());
            assertThat(res.cerrableFinal()).isFalse();
            assertThat(res.paralizadaFinal()).isFalse();
        }
    }

    @Nested
    @DisplayName("ACT-002-EN-ENRIQUECIMIENTO")
    class Act002 {

        @Test
        @DisplayName("EnriquecerActa registra evento ACTENR sin cambiar de bloque")
        void enriquecer_registra_actenr() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-002-EN-ENRIQUECIMIENTO");

            assertThat(res.ejecutado()).isTrue();

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ENRI);
            assertThat(acta.estaCerrada()).isFalse();

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            List<TipoEventoActa> tipos = eventos.stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(TipoEventoActa.ACTLAB, TipoEventoActa.ACTENR);

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.ACTAS_EN_ENRIQUECIMIENTO);
        }

        @Test
        @DisplayName("Dos eventos registrados: ACTLAB y ACTENR en orden")
        void dos_eventos_en_orden() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-002-EN-ENRIQUECIMIENTO");

            List<FalActaEvento> eventos = runner.getEventoRepo().buscarPorActa(res.actaId());
            assertThat(eventos).hasSize(2);
            assertThat(eventos.get(0).tipoEvt()).isEqualTo(TipoEventoActa.ACTLAB);
            assertThat(eventos.get(1).tipoEvt()).isEqualTo(TipoEventoActa.ACTENR);
        }
    }

    @Nested
    @DisplayName("ACT-004-PENDIENTE-NOTIFICACION")
    class Act004 {

        @Test
        @DisplayName("Documento firmado en ENRI: bandeja PENDIENTE_NOTIFICACION")
        void documento_firmado_bandeja_pendiente_notificacion() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-004-PENDIENTE-NOTIFICACION");

            assertThat(res.ejecutado()).isTrue();

            FalActaSnapshot snap = runner.getSnapshotRepo().buscarPorActa(res.actaId()).orElseThrow();
            assertThat(snap.getCodBandeja()).isEqualTo(CodigoBandeja.PENDIENTE_NOTIFICACION);

            FalActa acta = runner.getActaRepo().buscarPorId(res.actaId()).orElseThrow();
            assertThat(acta.getBloqueActual()).isEqualTo(BloqueActual.ENRI);
            assertThat(acta.estaCerrada()).isFalse();
        }

        @Test
        @DisplayName("Eventos esperados: ACTLAB, ACTENR, DOCGEN, DOCFIR")
        void eventos_esperados_presentes() {
            CasoUsoFuncionalRunner runner = new CasoUsoFuncionalRunner();
            CasoUsoFuncionalEjecucionResultado res = runner.ejecutar("ACT-004-PENDIENTE-NOTIFICACION");

            List<TipoEventoActa> tipos = runner.getEventoRepo().buscarPorActa(res.actaId())
                    .stream().map(FalActaEvento::tipoEvt).collect(Collectors.toList());
            assertThat(tipos).contains(
                    TipoEventoActa.ACTLAB, TipoEventoActa.ACTENR,
                    TipoEventoActa.DOCGEN, TipoEventoActa.DOCFIR);
        }
    }
}
