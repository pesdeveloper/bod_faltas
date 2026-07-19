package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.ActaTransitionEngine;
import ar.gob.malvinas.faltas.core.domain.ActaTransitionEngine.TransicionResultado;
import ar.gob.malvinas.faltas.core.domain.enums.BloqueActual;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoProcesalActa;
import ar.gob.malvinas.faltas.core.domain.enums.SituacionAdministrativaActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoEventoActa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la matriz canonica de transiciones del ActaTransitionEngine.
 * Slice 8F-11L.
 */
@DisplayName("ActaTransitionEngine - Matriz canonica de transiciones")
class ActaTransitionMatrixTest {

    private ActaTransitionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ActaTransitionEngine();
    }

    @Nested
    @DisplayName("Eventos de creacion y captura")
    class CreacionCaptura {

        @Test
        @DisplayName("ACTLAB inicia el acta en CAPT/EN_TRAMITE/ACTIVA")
        void actlab_inicia_acta() {
            Optional<TransicionResultado> r = engine.calcularTransicion(TipoEventoActa.ACTLAB);
            assertThat(r).isPresent();
            assertThat(r.get().bloqueNvo()).isEqualTo(BloqueActual.CAPT);
            assertThat(r.get().estProcNvo()).isEqualTo(EstadoProcesalActa.EN_TRAMITE);
            assertThat(r.get().sitAdmNva()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("ACTCAP avanza a ENRI sin cambiar estado procesal ni situacion adm")
        void actcap_avanza_a_enri() {
            Optional<TransicionResultado> r = engine.calcularTransicion(TipoEventoActa.ACTCAP);
            assertThat(r).isPresent();
            assertThat(r.get().bloqueNvo()).isEqualTo(BloqueActual.ENRI);
            assertThat(r.get().estProcNvo()).isNull();
            assertThat(r.get().sitAdmNva()).isNull();
        }

        @Test
        @DisplayName("ACTENR avanza a NOTI")
        void actenr_avanza_a_noti() {
            Optional<TransicionResultado> r = engine.calcularTransicion(TipoEventoActa.ACTENR);
            assertThat(r).isPresent();
            assertThat(r.get().bloqueNvo()).isEqualTo(BloqueActual.NOTI);
        }
    }

    @Nested
    @DisplayName("Eventos de cierre definitivo")
    class EventosCierre {

        @Test
        @DisplayName("PAGCNF produce cierre: CERR/CONCLUIDO/CERRADA")
        void pagcnf_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.PAGCNF)).isTrue();
            var r = engine.inspeccionarTransicion(TipoEventoActa.PAGCNF).orElseThrow();
            assertThat(r.bloqueNvo()).isEqualTo(BloqueActual.CERR);
            assertThat(r.estProcNvo()).isEqualTo(EstadoProcesalActa.CONCLUIDO);
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.CERRADA);
        }

        @Test
        @DisplayName("FALABS produce cierre")
        void falabs_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.FALABS)).isTrue();
        }

        @Test
        @DisplayName("APEABS produce cierre")
        void apeabs_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.APEABS)).isTrue();
        }

        @Test
        @DisplayName("PAGAPR produce cierre")
        void pagapr_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.PAGAPR)).isTrue();
        }

        @Test
        @DisplayName("PCOCNF produce cierre")
        void pcocnf_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.PCOCNF)).isTrue();
        }

        @Test
        @DisplayName("FALCON NO produce cierre directo")
        void falcon_no_produce_cierre() {
            assertThat(engine.produceCierre(TipoEventoActa.FALCON)).isFalse();
        }

        @Test
        @DisplayName("Todos los eventos que producen cierre transicionan a CERR")
        void todos_eventos_cierre_son_cerr() {
            for (TipoEventoActa tipo : TipoEventoActa.values()) {
                if (engine.produceCierre(tipo)) {
                    var r = engine.inspeccionarTransicion(tipo).orElseThrow();
                    assertThat(r.bloqueNvo())
                            .as("Evento %s debe tener bloqueNvo=CERR si produceCierre", tipo)
                            .isEqualTo(BloqueActual.CERR);
                    assertThat(r.estProcNvo())
                            .as("Evento %s debe tener estProcNvo=CONCLUIDO si produceCierre", tipo)
                            .isEqualTo(EstadoProcesalActa.CONCLUIDO);
                }
            }
        }
    }

    @Nested
    @DisplayName("Eventos de trazabilidad pura (sin transicion de estado)")
    class EventosTrazabilidad {

        @Test
        @DisplayName("DOCGEN no produce transicion")
        void docgen_sin_transicion() {
            assertThat(engine.calcularTransicion(TipoEventoActa.DOCGEN)).isEmpty();
        }

        @Test
        @DisplayName("DOCFIR no produce transicion")
        void docfir_sin_transicion() {
            assertThat(engine.calcularTransicion(TipoEventoActa.DOCFIR)).isEmpty();
        }

        @Test
        @DisplayName("QRGEN no produce transicion")
        void qrgen_sin_transicion() {
            assertThat(engine.calcularTransicion(TipoEventoActa.QRGEN)).isEmpty();
        }

        @Test
        @DisplayName("NOTENV no produce transicion de bloque")
        void notenv_sin_transicion() {
            assertThat(engine.calcularTransicion(TipoEventoActa.NOTENV)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Eventos de gestion externa")
    class GestionExterna {

        @Test
        @DisplayName("EXTDER mueve a GEXT/EN_GESTION_EXTERNA")
        void extder_a_gext() {
            var r = engine.calcularTransicion(TipoEventoActa.EXTDER).orElseThrow();
            assertThat(r.bloqueNvo()).isEqualTo(BloqueActual.GEXT);
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.EN_GESTION_EXTERNA);
        }

        @Test
        @DisplayName("EXTRET regresa a ANAL/ACTIVA")
        void extret_a_anal() {
            var r = engine.calcularTransicion(TipoEventoActa.EXTRET).orElseThrow();
            assertThat(r.bloqueNvo()).isEqualTo(BloqueActual.ANAL);
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }
    }

    @Nested
    @DisplayName("Eventos de paralizacion y archivo")
    class ParalizacionArchivo {

        @Test
        @DisplayName("ACTPAR cambia situacion a PARALIZADA sin cambiar bloque")
        void actpar_paralizada() {
            var r = engine.calcularTransicion(TipoEventoActa.ACTPAR).orElseThrow();
            assertThat(r.bloqueNvo()).isNull();
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.PARALIZADA);
        }

        @Test
        @DisplayName("ACTREA restaura situacion a ACTIVA")
        void actrea_activa() {
            var r = engine.calcularTransicion(TipoEventoActa.ACTREA).orElseThrow();
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }

        @Test
        @DisplayName("ACTARCH mueve a ARCH/ARCHIVADA")
        void actarch_archivada() {
            var r = engine.calcularTransicion(TipoEventoActa.ACTARCH).orElseThrow();
            assertThat(r.bloqueNvo()).isEqualTo(BloqueActual.ARCH);
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.ARCHIVADA);
        }

        @Test
        @DisplayName("ACTREI regresa a ANAL/ACTIVA desde archivo")
        void actrei_desde_archivo() {
            var r = engine.calcularTransicion(TipoEventoActa.ACTREI).orElseThrow();
            assertThat(r.bloqueNvo()).isEqualTo(BloqueActual.ANAL);
            assertThat(r.sitAdmNva()).isEqualTo(SituacionAdministrativaActa.ACTIVA);
        }
    }

    @Nested
    @DisplayName("Consultas sobre la matriz")
    class ConsultasMatriz {

        @Test
        @DisplayName("Null retorna Optional.empty")
        void null_retorna_empty() {
            assertThat(engine.calcularTransicion(null)).isEmpty();
        }

        @Test
        @DisplayName("eventosQueTransicionanA CERR contiene todos los eventos de cierre")
        void eventos_que_transicionan_a_cerr() {
            Set<TipoEventoActa> cierres = engine.eventosQueTransicionanA(BloqueActual.CERR);
            assertThat(cierres).contains(
                    TipoEventoActa.PAGCNF,
                    TipoEventoActa.FALABS,
                    TipoEventoActa.APEABS,
                    TipoEventoActa.PAGAPR,
                    TipoEventoActa.PCOCNF
            );
        }

        @Test
        @DisplayName("eventosQueTransicionanA con null retorna conjunto vacio")
        void eventos_que_transicionan_a_null() {
            assertThat(engine.eventosQueTransicionanA(null)).isEmpty();
        }
    }
}
