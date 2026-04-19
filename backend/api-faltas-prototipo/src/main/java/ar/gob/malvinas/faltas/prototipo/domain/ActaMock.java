package ar.gob.malvinas.faltas.prototipo.domain;

import java.time.LocalDateTime;

public record ActaMock(
        String id,
        String numeroActa,
        String dominioReferencia,
        String bloqueActual,
        String estadoProcesoActual,
        String situacionAdministrativaActual,
        boolean estaCerrada,
        boolean permiteReingreso,
        boolean tieneDocumentos,
        boolean tieneNotificaciones,
        LocalDateTime fechaCreacion,
        String infractorNombre,
        String infractorDocumento,
        String inspectorNombre,
        String resumenHecho,
        String bandejaActual) {
}
