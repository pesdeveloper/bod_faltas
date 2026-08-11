package ar.gob.malvinas.faltas.core.geo.web.dto;

import java.util.List;

/**
 * Detalle completo de una parcela catastral.
 * Incluye nomenclatura canónica, dirección por reverse-geocode,
 * cuentas de inmueble y lista de comercios registrados.
 *
 * Uso: consulta visual en GeoDomicilios inspector — no forma parte del flujo de actas.
 */
public record ParcelaDetalleDto(
        String nomenclatura,
        String direccion,
        double centLat,
        double centLng,
        List<InmuebleResumenDto> inmuebles,
        List<ComercioResumenDto> comercios
) {}
