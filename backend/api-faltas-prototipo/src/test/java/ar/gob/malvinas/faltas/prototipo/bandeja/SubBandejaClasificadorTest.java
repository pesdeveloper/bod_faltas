package ar.gob.malvinas.faltas.prototipo.bandeja;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.domain.CanalNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.EstadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.domain.TipoNotificacion;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubBandejaClasificadorTest {

    private final SubBandejaClasificador clasificador = new SubBandejaClasificador();

    @Test
    void clasificaCapturaInicialEnCaptura() {
        ActaMock acta = actaBase("ACTA-X", "ACTAS_EN_ENRIQUECIMIENTO", "CAPT", "EN_CURSO");
        SubBandejaContexto ctx = contexto(acta, null, PrototipoStore.SituacionPagoMock.SIN_PAGO);
        assertEquals("CAPTURA_INICIAL", clasificador.clasificar(ctx).subBandeja());
    }

    @Test
    void clasificaNotificacionPositivaEnAnalisis() {
        ActaMock acta = actaBase("ACTA-X", "PENDIENTE_ANALISIS", "ANAL", "PENDIENTE_REVISION");
        ActaNotificacionMock notif = new ActaNotificacionMock(
                "N-1",
                "ACTA-X",
                "POSTAL",
                "ENTREGADA",
                "Destinatario",
                TipoNotificacion.ACTA_INFRACCION,
                CanalNotificacion.CORREO_POSTAL,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        SubBandejaContexto ctx = new SubBandejaContexto(
                acta,
                null,
                PrototipoStore.SituacionPagoMock.SIN_PAGO,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(),
                List.of(notif),
                List.of(),
                List.of());
        assertEquals("ANALISIS_NOTIF_POSITIVA", clasificador.clasificar(ctx).subBandeja());
    }

    @Test
    void clasificaGestionExternaJuzgadoDePaz() {
        ActaMock acta = actaBase("ACTA-X", "GESTION_EXTERNA", "GESTION_EXTERNA", "EN_GESTION_EXTERNA");
        SubBandejaContexto ctx = new SubBandejaContexto(
                acta,
                null,
                PrototipoStore.SituacionPagoMock.SIN_PAGO,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                PrototipoStore.TIPO_GESTION_EXTERNA_JUZGADO_DE_PAZ,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        assertEquals("EXT_JUZGADO_PAZ", clasificador.clasificar(ctx).subBandeja());
    }

    @Test
    void clasificaCorreoPostalNegativaEnNotificacionComoNegativaPendienteDecision() {
        ActaMock acta = actaBase("ACTA-0044", "EN_NOTIFICACION", "NOTI", "EN_ENVIO");
        ActaNotificacionMock notif = new ActaNotificacionMock(
                "NOT-0044-01",
                "ACTA-0044",
                "POSTAL",
                "NO_ENTREGADA",
                "Demo Correo Negativa",
                TipoNotificacion.ACTA_INFRACCION,
                CanalNotificacion.CORREO_POSTAL,
                EstadoNotificacion.NEGATIVA,
                ResultadoNotificacion.NEGATIVA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        SubBandejaContexto ctx = new SubBandejaContexto(
                acta,
                null,
                PrototipoStore.SituacionPagoMock.SIN_PAGO,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(),
                List.of(notif),
                List.of(),
                List.of());
        assertEquals("NOTIF_NEGATIVA_PENDIENTE_DECISION", clasificador.clasificar(ctx).subBandeja());
    }

    @Test
    void clasificaCorreoPostalPositivaEnNotificacionComoCorreoPostal() {
        ActaMock acta = actaBase("ACTA-0043", "EN_NOTIFICACION", "NOTI", "EN_ENVIO");
        ActaNotificacionMock notif = new ActaNotificacionMock(
                "NOT-0043-01",
                "ACTA-0043",
                "POSTAL",
                "ENTREGADA",
                "Demo Correo Entregada Positiva",
                TipoNotificacion.ACTA_INFRACCION,
                CanalNotificacion.CORREO_POSTAL,
                EstadoNotificacion.ENTREGADA,
                ResultadoNotificacion.POSITIVA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        SubBandejaContexto ctx = new SubBandejaContexto(
                acta,
                null,
                PrototipoStore.SituacionPagoMock.SIN_PAGO,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(),
                List.of(notif),
                List.of(),
                List.of());
        assertEquals("NOTIF_EN_CORREO_POSTAL", clasificador.clasificar(ctx).subBandeja());
    }

    @Test
    void clasificaFirmaFalloCondenatorioPendiente() {
        ActaMock acta = actaBase("ACTA-X", "PENDIENTE_FIRMA", "ENRI", "PENDIENTE_FIRMA");
        ActaDocumentoMock doc = new ActaDocumentoMock(
                "D-1", "ACTA-X", "FALLO_CONDENATORIO", "PENDIENTE_FIRMA", "fallo.pdf");
        SubBandejaContexto ctx = new SubBandejaContexto(
                acta,
                null,
                PrototipoStore.SituacionPagoMock.SIN_PAGO,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(doc),
                List.of(),
                List.of(),
                List.of());
        assertEquals("FIRMA_FALLO_CONDENATORIO", clasificador.clasificar(ctx).subBandeja());
    }

    private static SubBandejaContexto contexto(
            ActaMock acta, String accion, PrototipoStore.SituacionPagoMock situacionPago) {
        return new SubBandejaContexto(
                acta,
                accion,
                situacionPago,
                PrototipoStore.SituacionPagoCondena.NO_APLICA,
                null,
                null,
                PrototipoStore.ResultadoFinalCierreMock.SIN_RESULTADO_FINAL,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private static ActaMock actaBase(String id, String bandeja, String bloque, String estado) {
        return new ActaMock(
                id,
                "A-2026-TEST",
                "DOM",
                bloque,
                estado,
                "ACTIVA",
                false,
                true,
                false,
                false,
                LocalDateTime.now(),
                "Infractor Test",
                "DNI",
                "Inspector",
                "Hecho",
                bandeja);
    }
}
