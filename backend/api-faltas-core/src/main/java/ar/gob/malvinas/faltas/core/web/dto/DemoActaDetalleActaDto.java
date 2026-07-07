package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque "acta" dentro de la respuesta de detalle de acta demo.
 *
 * Refleja el estado real de la instancia materializada por CasoUsoFuncionalRunner.
 *
 * Slice 8F-7.
 */
public record DemoActaDetalleActaDto(
        Long actaId,
        String numeroActa,
        String codigoActa,
        String bloqueActual,
        String estadoProcesal,
        String situacionAdministrativa,
        String resultadoFinal,
        String bandeja,
        boolean cerrable
) {}
