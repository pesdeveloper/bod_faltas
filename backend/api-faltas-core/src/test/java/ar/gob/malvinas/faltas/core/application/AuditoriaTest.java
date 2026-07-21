package ar.gob.malvinas.faltas.core.application;

import ar.gob.malvinas.faltas.core.support.FaltasClockTestSupport;

import ar.gob.malvinas.faltas.core.domain.enums.*;
import ar.gob.malvinas.faltas.core.domain.model.*;
import ar.gob.malvinas.faltas.core.repository.memory.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("8F-11B: Campos de auditoria por entidad")
class AuditoriaTest {

    private static final LocalDateTime FECHA_TEST = LocalDateTime.of(2026, 7, 5, 10, 0);
    private static final String USUARIO_TEST = "USR-42";

    @Nested
    @DisplayName("FalActa - fhAlta, idUserAlta, fhUltMod nullable, idUserUltMod nullable")
    class ActaAuditoria {

        @Test
        @DisplayName("fhAlta e idUserAlta son obligatorios en alta")
        void alta_tiene_auditoria() {
            FalActa acta = new FalActa(1L, "uuid", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Calle", "Av", null, null,
                    ResultadoFirmaInfractor.FIRMADA, null,
                    FECHA_TEST, USUARIO_TEST);
            assertThat(acta.getFhAlta()).isEqualTo(FECHA_TEST);
            assertThat(acta.getIdUserAlta()).isEqualTo(USUARIO_TEST);
        }

        @Test
        @DisplayName("fhUltMod e idUserUltMod son null en alta")
        void modificacion_null_en_alta() {
            FalActa acta = new FalActa(1L, "uuid", TipoActa.TRANSITO, 1L, 1L,
                    FaltasClockTestSupport.FIXED.now().toLocalDate(), FaltasClockTestSupport.FIXED.now(),
                    "Calle", "Av", null, null,
                    ResultadoFirmaInfractor.FIRMADA, null,
                    FECHA_TEST, USUARIO_TEST);
            assertThat(acta.getFhUltMod()).isNull();
            assertThat(acta.getIdUserUltMod()).isNull();
        }

        @Test
        @DisplayName("fhAlta no se reescribe - es final")
        void fhAlta_no_reescribible() throws NoSuchFieldException {
            var field = FalActa.class.getDeclaredField("fhAlta");
            assertThat(field.getModifiers() & java.lang.reflect.Modifier.FINAL)
                    .isNotEqualTo(0);
        }
    }

    @Nested
    @DisplayName("FalActaFallo - fhAlta, idUserAlta son obligatorios")
    class FalloAuditoria {

        @Test
        @DisplayName("fhAlta e idUserAlta presentes en alta")
        void alta_tiene_auditoria() {
            FalActaFallo fallo = new FalActaFallo(1L, 100L, TipoFalloActa.ABSOLUTORIO,
                    FaltasClockTestSupport.FIXED.now(), FECHA_TEST, USUARIO_TEST);
            assertThat(fallo.getFhAlta()).isEqualTo(FECHA_TEST);
            assertThat(fallo.getIdUserAlta()).isEqualTo(USUARIO_TEST);
        }
    }

    @Nested
    @DisplayName("FalActaApelacion - fhAlta, idUserAlta, fhUltMod, idUserUltMod")
    class ApelacionAuditoria {

        @Test
        @DisplayName("alta: fhAlta e idUserAlta seteados, fhUltMod e idUserUltMod nulos")
        void alta_auditoria_correcta() {
            FalActaApelacion ap = new FalActaApelacion(1L, 100L, 200L,
                    EstadoApelacionActa.PRESENTADA, FaltasClockTestSupport.FIXED.now(),
                    "pres", "fund", null, true,
                    FECHA_TEST, USUARIO_TEST);
            assertThat(ap.getFhAlta()).isEqualTo(FECHA_TEST);
            assertThat(ap.getIdUserAlta()).isEqualTo(USUARIO_TEST);
            assertThat(ap.getFhUltMod()).isNull();
            assertThat(ap.getIdUserUltMod()).isNull();
        }

        @Test
        @DisplayName("setters de modificacion funcionan")
        void modificacion_setteable() {
            FalActaApelacion ap = new FalActaApelacion(1L, 100L, 200L,
                    EstadoApelacionActa.PRESENTADA, FaltasClockTestSupport.FIXED.now(),
                    "pres", "fund", null, true,
                    FECHA_TEST, USUARIO_TEST);
            LocalDateTime fhMod = LocalDateTime.of(2026, 7, 5, 11, 30);
            ap.setFhUltMod(fhMod);
            ap.setIdUserUltMod("USR-99");
            assertThat(ap.getFhUltMod()).isEqualTo(fhMod);
            assertThat(ap.getIdUserUltMod()).isEqualTo("USR-99");
        }
    }

    @Nested
    @DisplayName("FalNotificacion - fhAlta, idUserAlta, fhUltMod, idUserUltMod")
    class NotificacionAuditoria {

        @Test
        @DisplayName("alta: fhAlta e idUserAlta presentes")
        void alta_auditoria() {
            FalNotificacion n = new FalNotificacion(1L, 100L, 200L,
                    TipoDocu.ACTO_ADMINISTRATIVO, "POSTAL", FaltasClockTestSupport.FIXED.now(),
                    FECHA_TEST, USUARIO_TEST);
            assertThat(n.getFhAlta()).isEqualTo(FECHA_TEST);
            assertThat(n.getIdUserAlta()).isEqualTo(USUARIO_TEST);
            assertThat(n.getFhUltMod()).isNull();
        }
    }

    @Nested
    @DisplayName("FalGestionExterna - fhAlta, idUserAlta, fhUltMod, idUserUltMod")
    class GestionAuditoria {

        @Test
        @DisplayName("alta: fhAlta e idUserAlta presentes")
        void alta_auditoria() {
            FalGestionExterna g = new FalGestionExterna(1L, 100L, FECHA_TEST, USUARIO_TEST);
            assertThat(g.getFhAlta()).isEqualTo(FECHA_TEST);
            assertThat(g.getIdUserAlta()).isEqualTo(USUARIO_TEST);
            assertThat(g.getFhUltMod()).isNull();
        }
    }

    @Nested
    @DisplayName("FalDocumento - fhAlta (fechaGeneracion), idUserAlta presentes")
    class DocumentoAuditoria {

        @Test
        @DisplayName("fechaGeneracion (fh_alta) y idUserAlta presentes")
        void alta_auditoria() {
            FalDocumento doc = new FalDocumento(1L, 100L, TipoDocu.ACTO_ADMINISTRATIVO,
                    FECHA_TEST,
                    ar.gob.malvinas.faltas.core.domain.enums.EstadoDocu.BORRADOR,
                    ar.gob.malvinas.faltas.core.domain.enums.TipoFirmaReq.FIRMA_INTERNA,
                    null, USUARIO_TEST);
            assertThat(doc.getFechaGeneracion()).isEqualTo(FECHA_TEST);
            assertThat(doc.getIdUserAlta()).isEqualTo(USUARIO_TEST);
        }
    }
}
