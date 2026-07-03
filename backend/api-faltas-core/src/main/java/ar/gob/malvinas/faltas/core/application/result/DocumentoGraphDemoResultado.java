package ar.gob.malvinas.faltas.core.application.result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado agregado del graph demo documental completo (8 casos operativos).
 *
 * completo=true cuando casosFallidos == 0.
 * fhEjecucion es el momento de inicio de la ejecucion del demo.
 *
 * Slice 8F-4.
 */
public record DocumentoGraphDemoResultado(
        List<DocumentoGraphDemoCasoResultado> casos,
        int totalCasos,
        int casosExitosos,
        int casosFallidos,
        boolean completo,
        LocalDateTime fhEjecucion
) {
    public static DocumentoGraphDemoResultado de(
            List<DocumentoGraphDemoCasoResultado> casos,
            LocalDateTime fhEjecucion) {
        int exitosos = (int) casos.stream()
                .filter(DocumentoGraphDemoCasoResultado::exitoso)
                .count();
        int fallidos = casos.size() - exitosos;
        return new DocumentoGraphDemoResultado(
                List.copyOf(casos),
                casos.size(),
                exitosos,
                fallidos,
                fallidos == 0,
                fhEjecucion);
    }
}
