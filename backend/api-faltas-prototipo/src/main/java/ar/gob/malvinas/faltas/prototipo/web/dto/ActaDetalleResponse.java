package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.time.LocalDateTime;

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
        boolean tieneNotificaciones) {
}
