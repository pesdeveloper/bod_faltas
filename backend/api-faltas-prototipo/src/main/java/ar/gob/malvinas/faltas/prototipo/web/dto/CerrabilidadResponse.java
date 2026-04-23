package ar.gob.malvinas.faltas.prototipo.web.dto;

import java.util.List;

/**
 * Tres planos explícitos: resultado final, pendientes que bloquean, y
 * cerrabilidad (no se colapsa en un solo flag ambiguo).
 */
public record CerrabilidadResponse(
        String resultadoFinal,
        boolean cerrable,
        List<String> pendientesBloqueantesCierre,
        String motivoNoCerrable) {
}
