package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoGeneralVehiculo;
import ar.gob.malvinas.faltas.core.domain.enums.TipoVehiculo;

/**
 * Bloque tipado del vehículo del acta, agregado en Wave 4 Parte 3A (satélites del alta
 * integral). 1:1 con {@code fal_acta_vehiculo}: como máximo un vehículo por acta.
 *
 * <p>{@code marcaVehiculoId}/{@code modeloVehiculoId} son el catálogo normalizado;
 * {@code marcaVehiculoTxt}/{@code modeloVehiculoTxt} son el fallback textual cuando el
 * catálogo no tiene la marca/modelo real (no se bloquea el labrado por ausencia de
 * catálogo; {@code FalActaVehiculo}).
 */
public record VehiculoActaItem(
        String dominioVehiculo,
        TipoVehiculo tipoVehiculo,
        Long marcaVehiculoId,
        String marcaVehiculoTxt,
        Long modeloVehiculoId,
        String modeloVehiculoTxt,
        Short anioVehiculo,
        String colorVehiculo,
        EstadoGeneralVehiculo estadoGeneralVehiculo
) {}
