package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.application.demo.ActaMockFuncionalDefinicion;

import java.util.List;

/**
 * Resultado de la matriz de cobertura funcional del dataset in-memory.
 *
 * Permite responder:
 * - cuantas actas mock existen
 * - cuantos casos de uso estan cubiertos
 * - cuantos documentos esperados declara el dataset
 * - si la cobertura es completa segun el dominio actualmente implementado
 * - que casos quedan pendientes
 *
 * Slice 8F-4B.
 */
public record DatasetFuncionalCoberturaResultado(
        int totalActasMock,
        int totalCasosUsoCubiertos,
        int totalDocumentosEsperados,
        List<ActaMockFuncionalDefinicion> actas,
        List<String> casosUsoCubiertos,
        List<String> casosUsoPendientes,
        boolean coberturaCompletaSegunDominioActual,
        List<String> advertencias
) {

    public static DatasetFuncionalCoberturaResultado calcular(
            List<ActaMockFuncionalDefinicion> actas,
            List<String> casosUsoPendientes,
            List<String> advertencias) {

        List<String> cubiertos = actas.stream()
                .flatMap(a -> a.casosUsoCubiertos().stream())
                .distinct()
                .sorted()
                .toList();

        int totalDocs = actas.stream()
                .mapToInt(a -> a.documentosEsperados().size())
                .sum();

        boolean completa = casosUsoPendientes.isEmpty();

        return new DatasetFuncionalCoberturaResultado(
                actas.size(),
                cubiertos.size(),
                totalDocs,
                actas,
                cubiertos,
                casosUsoPendientes,
                completa,
                advertencias);
    }
}
