package ar.gob.malvinas.faltas.core.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta del endpoint POST /demo/dev/reset.
 *
 * Solo para uso en dev/test/demo. No es un DTO productivo.
 *
 * Slice 8F-5.
 */
public record DevResetResponse(
        boolean ejecutado,
        String modo,
        LocalDateTime fhReset,
        int repositoriosReseteados,
        int plantillasRecreadas,
        int actasDemoDisponibles,
        int casosDatasetFuncional,
        int errores,
        List<String> repositorios,
        List<String> acciones,
        List<String> advertencias
) {}
