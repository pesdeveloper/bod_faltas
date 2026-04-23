package ar.gob.malvinas.faltas.prototipo.web.dto;

public record ActaBandejaItemResponse(
        String id,
        String numeroActa,
        String infractorNombre,
        String bloqueActual,
        String estadoProcesoActual,
        String situacionAdministrativaActual,
        String bandejaActual,
        String accionPendiente,
        String motivoArchivo,
        String tipoGestionExterna) {
}
