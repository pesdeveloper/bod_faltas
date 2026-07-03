package ar.gob.malvinas.faltas.core.application.result;

import java.util.List;

/**
 * Resultado de la ejecucion funcional de un caso de uso del dataset mock.
 *
 * Generado por CasoUsoFuncionalRunner al ejecutar el flujo completo de una
 * ActaMockFuncionalDefinicion desde el inicio hasta el estado final esperado.
 *
 * No usa JDBC, SQL ni storage real. Solo in-memory.
 *
 * Slice 8F-4C.
 */
public record CasoUsoFuncionalEjecucionResultado(
        String codigoActaMock,
        String casoUsoPrincipal,
        boolean ejecutado,
        boolean completo,
        Long actaId,
        String bloqueFinal,
        String bandejaFinal,
        String estadoFinal,
        String resultadoFinal,
        boolean cerrableFinal,
        boolean paralizadaFinal,
        int eventosGenerados,
        int documentosGenerados,
        int redaccionesGeneradas,
        int documentosMockGenerados,
        List<String> pasosEjecutados,
        List<String> documentosEsperadosValidados,
        List<String> eventosValidados,
        List<String> advertencias
) {

    public static CasoUsoFuncionalEjecucionResultado exitoso(
            String codigo,
            String casoUso,
            Long actaId,
            String bloque,
            String bandeja,
            String estado,
            String resultado,
            boolean cerrable,
            boolean paralizada,
            int eventos,
            int documentos,
            int redacciones,
            int docsMock,
            List<String> pasos,
            List<String> docsValidados,
            List<String> eventosValidados,
            List<String> advertencias) {
        return new CasoUsoFuncionalEjecucionResultado(
                codigo, casoUso, true, true,
                actaId, bloque, bandeja, estado, resultado,
                cerrable, paralizada,
                eventos, documentos, redacciones, docsMock,
                pasos, docsValidados, eventosValidados, advertencias);
    }

    public static CasoUsoFuncionalEjecucionResultado parcial(
            String codigo,
            String casoUso,
            Long actaId,
            List<String> pasos,
            List<String> advertencias) {
        return new CasoUsoFuncionalEjecucionResultado(
                codigo, casoUso, true, false,
                actaId, null, null, null, null,
                false, false,
                0, 0, 0, 0,
                pasos, List.of(), List.of(), advertencias);
    }

    public static CasoUsoFuncionalEjecucionResultado noEjecutado(
            String codigo,
            String casoUso,
            String motivoNoEjecucion) {
        return new CasoUsoFuncionalEjecucionResultado(
                codigo, casoUso, false, false,
                null, null, null, null, null,
                false, false,
                0, 0, 0, 0,
                List.of(), List.of(), List.of(),
                List.of("NO_EJECUTADO: " + motivoNoEjecucion));
    }
}
