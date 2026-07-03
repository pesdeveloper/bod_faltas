package ar.gob.malvinas.faltas.core.application.combinacion;

import java.util.Set;

/**
 * Resultado del motor de combinacion de documentos.
 *
 * completo = true si variablesFaltantes y variablesDesconocidas estan vacias.
 *
 * Slice 8F-1.
 */
public record DocumentoCombinacionResultado(
        String contenidoCombinado,
        Set<String> variablesUsadas,
        Set<String> variablesFaltantes,
        Set<String> variablesDesconocidas,
        boolean completo
) {
    public static DocumentoCombinacionResultado exitoso(
            String contenidoCombinado, Set<String> variablesUsadas) {
        return new DocumentoCombinacionResultado(
                contenidoCombinado, variablesUsadas, Set.of(), Set.of(), true);
    }

    public static DocumentoCombinacionResultado conProblemas(
            String contenidoCombinado, Set<String> variablesUsadas,
            Set<String> variablesFaltantes, Set<String> variablesDesconocidas) {
        return new DocumentoCombinacionResultado(
                contenidoCombinado, variablesUsadas,
                variablesFaltantes, variablesDesconocidas, false);
    }
}