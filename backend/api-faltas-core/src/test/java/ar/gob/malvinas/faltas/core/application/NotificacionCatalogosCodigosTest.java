package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.exception.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("8F-11I: CanalNotificacion, TipoNotificacion, TipoAcuse, EstadoAcuse, EstadoLote - codigos y catálogos")
class NotificacionCatalogosCodigosTest {

    @Test @DisplayName("CanalNotificacion - 5 valores con codigos únicos")
    void canalNotif_codigosUnicos() {
        var valores = CanalNotificacion.values();
        var codigos = java.util.Arrays.stream(valores).map(CanalNotificacion::codigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(valores).hasSize(5);
    }

    @Test @DisplayName("CanalNotificacion - fromCodigo round-trip")
    void canalNotif_fromCodigo_roundTrip() {
        for (CanalNotificacion c : CanalNotificacion.values()) {
            assertThat(CanalNotificacion.fromCodigo(c.codigo())).isEqualTo(c);
        }
    }

    @Test @DisplayName("CanalNotificacion - fromCodigo invalido lanza excepcion")
    void canalNotif_fromCodigo_invalido() {
        assertThatThrownBy(() -> CanalNotificacion.fromCodigo((short) 99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("CanalNotificacion - CORREO_POSTAL requiere domicilio fisico")
    void canalNotif_requiereDomicilioFisico() {
        assertThat(CanalNotificacion.CORREO_POSTAL.requiereDomicilioFisico()).isTrue();
        assertThat(CanalNotificacion.NOTIFICADOR_MUNICIPAL.requiereDomicilioFisico()).isTrue();
        assertThat(CanalNotificacion.PRESENCIAL.requiereDomicilioFisico()).isFalse();
        assertThat(CanalNotificacion.PORTAL_INFRACTOR.requiereDomicilioFisico()).isFalse();
        assertThat(CanalNotificacion.EMAIL.requiereDomicilioFisico()).isFalse();
    }

    @Test @DisplayName("CanalNotificacion - esPortal y esDigital correctos")
    void canalNotif_esPortalEsDigital() {
        assertThat(CanalNotificacion.PORTAL_INFRACTOR.esPortal()).isTrue();
        assertThat(CanalNotificacion.EMAIL.esDigital()).isTrue();
        assertThat(CanalNotificacion.CORREO_POSTAL.esPortal()).isFalse();
        assertThat(CanalNotificacion.CORREO_POSTAL.esDigital()).isFalse();
    }

    @Test @DisplayName("TipoNotificacion - 3 valores con codigos únicos")
    void tipoNotif_codigosUnicos() {
        var valores = TipoNotificacion.values();
        var codigos = java.util.Arrays.stream(valores).map(TipoNotificacion::codigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(valores).hasSize(3);
    }

    @Test @DisplayName("TipoNotificacion - fromCodigo round-trip")
    void tipoNotif_fromCodigo_roundTrip() {
        for (TipoNotificacion t : TipoNotificacion.values()) {
            assertThat(TipoNotificacion.fromCodigo(t.codigo())).isEqualTo(t);
        }
    }

    @Test @DisplayName("TipoAcuse - 6 valores con codigos únicos")
    void tipoAcuse_6Tipos() {
        assertThat(TipoAcuse.values()).hasSize(6);
        var codigos = java.util.Arrays.stream(TipoAcuse.values()).map(TipoAcuse::codigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
    }

    @Test @DisplayName("TipoAcuse - fromCodigo round-trip")
    void tipoAcuse_fromCodigo_roundTrip() {
        for (TipoAcuse t : TipoAcuse.values()) {
            assertThat(TipoAcuse.fromCodigo(t.codigo())).isEqualTo(t);
        }
    }

    @Test @DisplayName("TipoAcuse - implicaciones correctas")
    void tipoAcuse_implicaciones() {
        assertThat(TipoAcuse.ACUSE_RECEPCION.implicaResultadoPositivo()).isTrue();
        assertThat(TipoAcuse.ACUSE_RECHAZO.implicaResultadoNegativo()).isTrue();
        assertThat(TipoAcuse.ACUSE_DOMICILIO_INEXISTENTE.implicaResultadoNegativo()).isTrue();
        assertThat(TipoAcuse.ACUSE_PERSONA_DESCONOCIDA.implicaResultadoNegativo()).isTrue();
        assertThat(TipoAcuse.ACUSE_AUSENTE.implicaResultadoPositivo()).isFalse();
        assertThat(TipoAcuse.ACUSE_AUSENTE.implicaResultadoNegativo()).isFalse();
        assertThat(TipoAcuse.ACUSE_OTRO.implicaResultadoPositivo()).isFalse();
        assertThat(TipoAcuse.ACUSE_OTRO.implicaResultadoNegativo()).isFalse();
    }

    @Test @DisplayName("EstadoAcuse - 5 valores con codigos únicos")
    void estadoAcuse_5Estados() {
        assertThat(EstadoAcuse.values()).hasSize(5);
        var codigos = java.util.Arrays.stream(EstadoAcuse.values()).map(EstadoAcuse::codigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
    }

    @Test @DisplayName("EstadoAcuse - fromCodigo round-trip")
    void estadoAcuse_fromCodigo_roundTrip() {
        for (EstadoAcuse e : EstadoAcuse.values()) {
            assertThat(EstadoAcuse.fromCodigo(e.codigo())).isEqualTo(e);
        }
    }

    @Test @DisplayName("EstadoAcuse - ANULADO no esta activo")
    void estadoAcuse_anuladoNoActivo() {
        assertThat(EstadoAcuse.ANULADO.estaActivo()).isFalse();
        assertThat(EstadoAcuse.VALIDADO.estaActivo()).isTrue();
        assertThat(EstadoAcuse.PENDIENTE.estaActivo()).isTrue();
        assertThat(EstadoAcuse.VALIDADO.producesEfecto()).isTrue();
        assertThat(EstadoAcuse.RECIBIDO.producesEfecto()).isFalse();
    }

    @Test @DisplayName("EstadoLote - 5 valores con codigos únicos")
    void estadoLote_5Estados() {
        assertThat(EstadoLote.values()).hasSize(5);
        var codigos = java.util.Arrays.stream(EstadoLote.values()).map(EstadoLote::codigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
    }

    @Test @DisplayName("EstadoLote - transiciones correctas")
    void estadoLote_transiciones() {
        assertThat(EstadoLote.GENERADO.esEmitible()).isTrue();
        assertThat(EstadoLote.EMITIDO.esProcesable()).isTrue();
        assertThat(EstadoLote.GENERADO.esAnulable()).isTrue();
        assertThat(EstadoLote.EMITIDO.esAnulable()).isTrue();
        assertThat(EstadoLote.PROCESADO.esAnulable()).isFalse();
        assertThat(EstadoLote.PROCESADO.esFinal()).isTrue();
        assertThat(EstadoLote.ANULADO.esFinal()).isTrue();
        assertThat(EstadoLote.GENERADO.esFinal()).isFalse();
    }

    @Test @DisplayName("EstadoNotificacion - SIN_EFECTO existe")
    void estadoNotif_sinEfecto() {
        assertThat(EstadoNotificacion.SIN_EFECTO).isNotNull();
    }

    @Test @DisplayName("ResultadoNotificacion - SUPERADA_POR_PORTAL existe")
    void resultadoNotif_superadaPorPortal() {
        assertThat(ResultadoNotificacion.SUPERADA_POR_PORTAL).isNotNull();
    }

    @Test @DisplayName("TipoEventoActa - nuevos eventos notificacion existen")
    void tipoEvento_nuevosEventos() {
        assertThat(TipoEventoActa.NOTINT).isNotNull();
        assertThat(TipoEventoActa.NOTREI).isNotNull();
        assertThat(TipoEventoActa.NOTRVE).isNotNull();
        assertThat(TipoEventoActa.ACUGEN).isNotNull();
        assertThat(TipoEventoActa.ACUVAL).isNotNull();
        assertThat(TipoEventoActa.LOTGEN).isNotNull();
        assertThat(TipoEventoActa.LOTEM).isNotNull();
        assertThat(TipoEventoActa.LOTPRC).isNotNull();
        assertThat(TipoEventoActa.LOTANU).isNotNull();
        assertThat(TipoEventoActa.PORPOS).isNotNull();
        assertThat(TipoEventoActa.NOTSUP).isNotNull();
    }

    @Test @DisplayName("TipoEventoActa - codigos de 6 chars en nuevos eventos")
    void tipoEvento_codigos6Chars() {
        for (TipoEventoActa t : TipoEventoActa.values()) {
            assertThat(t.codigo()).as("codigo de " + t.name()).hasSize(6);
        }
    }
}
