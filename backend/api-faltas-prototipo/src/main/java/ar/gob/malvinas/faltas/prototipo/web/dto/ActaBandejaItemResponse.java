package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

public record ActaBandejaItemResponse(
        String id,
        String numeroActa,
        String infractorNombre,
        String bloqueActual,
        String estadoProcesoActual,
        String situacionAdministrativaActual,
        String bandejaActual,
        String situacionPago,
        String accionPendiente,
        String motivoArchivo,
        String tipoGestionExterna,
        CerrabilidadResponse cerrabilidad,
        String subBandeja,
        String subBandejaLabel,
        String chip,
        String accionPrincipal,
        int prioridadSubBandeja,
        List<String> chipsSecundarios,
        String dependenciaDemo) {
}
