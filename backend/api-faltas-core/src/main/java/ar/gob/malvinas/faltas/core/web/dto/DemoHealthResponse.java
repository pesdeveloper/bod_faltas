package ar.gob.malvinas.faltas.core.web.dto;

import java.util.List;

/**
 * Respuesta del endpoint GET /demo/health.
 *
 * Permite que el frontend demo verifique disponibilidad del backend
 * antes de cargar la UI de navegacion de actas.
 *
 * Slice 8F-8 - cierre GAP-8.
 */
public record DemoHealthResponse(
        String status,
        boolean demoReady,
        String fhEjecucion,
        String versionDemo,
        DemoHealthDatasetDto dataset,
        DemoHealthDocumentosDto documentos,
        DemoHealthResetDto reset,
        List<DemoHealthEndpointDto> endpoints,
        List<String> warnings
) {}