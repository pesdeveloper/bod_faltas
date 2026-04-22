package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ActaDetalleResponse(
        String id,
        String numeroActa,
        String bloqueActual,
        String estadoProcesoActual,
        String situacionAdministrativaActual,
        boolean estaCerrada,
        boolean permiteReingreso,
        LocalDateTime fechaCreacion,
        String infractorNombre,
        String infractorDocumento,
        String inspectorNombre,
        String resumenHecho,
        String bandejaActual,
        boolean tieneDocumentos,
        boolean tieneNotificaciones,
        List<String> piezasRequeridas,
        List<String> piezasGeneradas,
        String accionPendiente) {
}
