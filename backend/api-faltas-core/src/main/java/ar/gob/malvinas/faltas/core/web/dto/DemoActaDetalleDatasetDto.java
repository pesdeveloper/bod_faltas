package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Bloque "dataset" dentro de la respuesta de detalle de acta demo.
 *
 * Refleja la definicion declarativa del caso en el DatasetFuncionalDominioCatalog.
 *
 * Slice 8F-7.
 */
public record DemoActaDetalleDatasetDto(
        String bloqueEsperado,
        String situacionEsperada,
        String bandejaEsperada,
        boolean cerrableEsperado
) {}
