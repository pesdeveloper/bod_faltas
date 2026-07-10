package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.OrigenFirmezaCondena;
import ar.gob.malvinas.faltas.core.domain.enums.ResultadoFalloActa;
import ar.gob.malvinas.faltas.core.domain.enums.TipoFalloActa;
import ar.gob.malvinas.faltas.core.domain.exception.ConcurrenciaConflictoException;
import ar.gob.malvinas.faltas.core.domain.exception.PrecondicionVioladaException;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.repository.memory.InMemoryFalloActaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Slice 8F-11F: FalActaFallo - invariantes multi-fallo")
class FalloInvariantesTest {

    private InMemoryFalloActaRepository repo;

    @BeforeEach
    void setUp() { repo = new InMemoryFalloActaRepository(); }

    private FalActaFallo crearFallo(Long actaId) {
        Long id = repo.nextId();
        FalActaFallo f = new FalActaFallo(id, actaId,
                TipoFalloActa.CONDENATORIO, FaltasClockTestSupport.FIXED.now(), FaltasClockTestSupport.FIXED.now(), "USR-1");
        f.setMontoCondena(new BigDecimal("1000"));
        f.setEstadoFallo(EstadoFalloActa.NOTIFICADO);
        return f;
    }

    @Nested @DisplayName("A: Un unico vigente por acta")
    class UnSoloVigente {
        @Test @DisplayName("primer fallo vigente") void primer_fallo_vigente() {
            repo.guardarComoVigente(crearFallo(1L));
            assertThat(repo.findVigenteByActaId(1L)).isPresent();
            assertThat(repo.findVigenteByActaId(1L).get().isSiVigente()).isTrue();
        }
        @Test @DisplayName("segundo fallo desactiva primero") void segundo_fallo_desactiva_primero() {
            FalActaFallo f1 = crearFallo(2L);
            repo.guardarComoVigente(f1);
            Long idF1 = f1.getId();
            FalActaFallo f2 = crearFallo(2L);
            f2.setFalloReemplazadoId(idF1);
            repo.guardarComoVigente(f2);
            assertThat(repo.findVigenteByActaId(2L).get().getId()).isEqualTo(f2.getId());
            assertThat(repo.findById(idF1).get().isSiVigente()).isFalse();
            assertThat(repo.findById(idF1).get().getEstadoFallo()).isEqualTo(EstadoFalloActa.REEMPLAZADO);
        }
        @Test @DisplayName("rechazarSiYaExisteVigente: lanza si ya hay vigente") void rechazar_vigente() {
            repo.guardarComoVigente(crearFallo(3L));
            assertThatThrownBy(() -> repo.rechazarSiYaExisteVigente(3L))
                    .isInstanceOf(PrecondicionVioladaException.class).hasMessageContaining("vigente");
        }
    }

    @Nested @DisplayName("B: Historial multi-fallo")
    class Historial {
        @Test @DisplayName("findByActaId retorna todos incluidos reemplazados") void historial_todos() {
            FalActaFallo f1 = crearFallo(10L);
            repo.guardarComoVigente(f1);
            FalActaFallo f2 = crearFallo(10L);
            f2.setFalloReemplazadoId(f1.getId());
            repo.guardarComoVigente(f2);
            assertThat(repo.findByActaId(10L)).hasSize(2);
        }
        @Test @DisplayName("historial no se borra con reemplazos sucesivos") void historial_no_se_borra() {
            Long a = 11L;
            FalActaFallo ant = crearFallo(a);
            repo.guardarComoVigente(ant);
            for (int i = 0; i < 3; i++) {
                FalActaFallo sig = crearFallo(a);
                sig.setFalloReemplazadoId(ant.getId());
                repo.guardarComoVigente(sig);
                ant = sig;
            }
            assertThat(repo.findByActaId(a)).hasSize(4);
        }
        @Test @DisplayName("actas independientes no se interfieren") void actas_independientes() {
            repo.guardarComoVigente(crearFallo(20L));
            repo.guardarComoVigente(crearFallo(21L));
            assertThat(repo.findByActaId(20L)).hasSize(1);
            assertThat(repo.findByActaId(21L)).hasSize(1);
        }
    }

    @Nested @DisplayName("C: Firmeza inline")
    class FirmezaInline {
        @Test @DisplayName("declararFirmeza: siFirme=true, fhFirmeza y origenFirmeza presentes") void firmeza_caso_feliz() {
            FalActaFallo f = crearFallo(30L);
            repo.guardarComoVigente(f);
            FalActaFallo v = repo.findVigenteByActaId(30L).orElseThrow();
            v.declararFirmeza(FaltasClockTestSupport.FIXED.now(), OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
            repo.guardar(v);
            FalActaFallo leido = repo.findVigenteByActaId(30L).orElseThrow();
            assertThat(leido.isSiFirme()).isTrue();
            assertThat(leido.getFhFirmeza()).isNotNull();
            assertThat(leido.getOrigenFirmeza()).isEqualTo(OrigenFirmezaCondena.VENCIMIENTO_PLAZO_APELACION);
        }
        @Test @DisplayName("declararFirmeza: lanza si fhFirmeza null") void firmeza_requiere_fecha() {
            assertThatThrownBy(() -> crearFallo(31L).declararFirmeza(null, OrigenFirmezaCondena.APELACION_RECHAZADA))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test @DisplayName("declararFirmeza: lanza si origenFirmeza null") void firmeza_requiere_origen() {
            assertThatThrownBy(() -> crearFallo(32L).declararFirmeza(FaltasClockTestSupport.FIXED.now(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        @Test @DisplayName("siFirme=false sin fhFirmeza inicialmente") void no_firme_inicialmente() {
            FalActaFallo f = crearFallo(33L);
            assertThat(f.isSiFirme()).isFalse();
            assertThat(f.getFhFirmeza()).isNull();
            assertThat(f.getOrigenFirmeza()).isNull();
        }
    }

    @Nested @DisplayName("D: Optimistic locking")
    class OL {
        @Test @DisplayName("conflicto de version lanza ConcurrenciaConflictoException") void conflicto() {
            FalActaFallo f = crearFallo(40L);
            repo.guardarComoVigente(f);
            FalActaFallo v1 = repo.findVigenteByActaId(40L).orElseThrow();
            FalActaFallo v2 = repo.findVigenteByActaId(40L).orElseThrow();
            v1.setFundamentos("A"); repo.guardar(v1);
            v2.setFundamentos("B");
            assertThatThrownBy(() -> repo.guardar(v2)).isInstanceOf(ConcurrenciaConflictoException.class);
        }
        @Test @DisplayName("actualizacion secuencial incrementa versionRow") void secuencial_ok() {
            FalActaFallo f = crearFallo(41L);
            repo.guardarComoVigente(f);
            FalActaFallo v1 = repo.findVigenteByActaId(41L).orElseThrow();
            int ant = v1.getVersionRow();
            v1.setFundamentos("OK"); repo.guardar(v1);
            assertThat(repo.findVigenteByActaId(41L).get().getVersionRow()).isEqualTo(ant + 1);
        }
    }

    @Nested @DisplayName("E: Campos nuevos 8F-11F")
    class CamposNuevos {
        @Test @DisplayName("fhVtoApelacion y siApelable seteables") void nuevos_campos() {
            FalActaFallo f = crearFallo(50L);
            f.setFhVtoApelacion(FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(15));
            f.setSiApelable(true);
            assertThat(f.getFhVtoApelacion()).isNotNull();
            assertThat(f.isSiApelable()).isTrue();
        }
        @Test @DisplayName("resultadoFallo seteable") void resultado_fallo() {
            FalActaFallo f = crearFallo(51L);
            f.setResultadoFallo(ResultadoFalloActa.CONDENA);
            assertThat(f.getResultadoFallo()).isEqualTo(ResultadoFalloActa.CONDENA);
        }
        @Test @DisplayName("falloReemplazadoId enlaza correctamente") void fallo_reemplazado_id() {
            FalActaFallo f1 = crearFallo(52L);
            repo.guardarComoVigente(f1);
            FalActaFallo f2 = crearFallo(52L);
            f2.setFalloReemplazadoId(f1.getId());
            repo.guardarComoVigente(f2);
            assertThat(repo.findVigenteByActaId(52L).get().getFalloReemplazadoId()).isEqualTo(f1.getId());
        }
        @Test @DisplayName("copia() preserva campos nuevos") void copia_preserva() {
            FalActaFallo f = crearFallo(53L);
            f.setResultadoFallo(ResultadoFalloActa.CONDENA);
            f.setSiApelable(true);
            f.setFhVtoApelacion(FaltasClockTestSupport.FIXED.now().toLocalDate().plusDays(10));
            f.setValorizacionId(99L);
            f.setFalloReemplazadoId(77L);
            FalActaFallo c = f.copia();
            assertThat(c.getResultadoFallo()).isEqualTo(ResultadoFalloActa.CONDENA);
            assertThat(c.isSiApelable()).isTrue();
            assertThat(c.getFhVtoApelacion()).isNotNull();
            assertThat(c.getValorizacionId()).isEqualTo(99L);
            assertThat(c.getFalloReemplazadoId()).isEqualTo(77L);
        }
    }
}