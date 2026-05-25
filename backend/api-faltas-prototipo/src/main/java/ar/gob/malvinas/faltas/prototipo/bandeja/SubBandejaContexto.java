package ar.gob.malvinas.faltas.prototipo.bandeja;

import ar.gob.malvinas.faltas.prototipo.domain.ActaDocumentoMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.domain.ActaNotificacionMock;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;

import java.util.List;

/**
 * Vista mínima y estable para clasificar una acta sin acoplar el clasificador al store.
 */
public record SubBandejaContexto(
        ActaMock acta,
        String accionPendiente,
        PrototipoStore.SituacionPagoMock situacionPago,
        PrototipoStore.SituacionPagoCondena situacionPagoCondena,
        String motivoArchivo,
        String tipoGestionExterna,
        PrototipoStore.ResultadoFinalCierreMock resultadoFinal,
        boolean cerrable,
        List<String> pendientesBloqueantes,
        List<ActaDocumentoMock> documentos,
        List<ActaNotificacionMock> notificaciones,
        List<String> piezasRequeridas,
        List<String> piezasGeneradas) {
}
