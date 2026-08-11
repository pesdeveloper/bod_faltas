package ar.gob.malvinas.faltas.core.application.command;

import ar.gob.malvinas.faltas.core.domain.enums.UnidadTerritorialTipo;

import java.util.List;

/**
 * Bloque tipado específico de acta de tránsito, agregado en Wave 4 Parte 3A. 1:1 con
 * {@code fal_acta_transito}. Solo aplicable cuando {@code tipoActa=TRANSITO}.
 *
 * <p>{@code mediciones} viaja anidado porque las mediciones de alcoholemia solo tienen
 * sentido en el contexto de un acta de tránsito con {@code siControlAlcoholemia=true}; no
 * se persisten si la lista es vacía (nunca por posición, siempre por
 * {@code ordenMedicion} explícito de cada ítem).
 */
public record TransitoActaItem(
        String nroLicencia,
        Short idProvLic,
        UnidadTerritorialTipo unidadTerritorialLicTipo,
        Integer idUnidadTerritorialLic,
        boolean siRetLicencia,
        boolean siRetVehiculo,
        boolean siControlAlcoholemia,
        List<AlcoholemiaMedicionItem> mediciones
) {
    public List<AlcoholemiaMedicionItem> medicionesSeguro() {
        return mediciones == null ? List.of() : mediciones;
    }
}
