package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.enums.AccionDocumental;
import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;
import ar.gob.malvinas.faltas.core.domain.enums.TipoDocu;

import java.time.LocalDateTime;

/**
 * Resultado de un caso individual del graph demo documental.
 *
 * exitoso=true indica que el ciclo completo (redaccion + generacion mock) se ejecuto sin errores.
 * exitoso=false incluye errorMensaje con la causa.
 *
 * Los campos storageKey/hashDocu/fhGeneracion son null cuando exitoso=false.
 *
 * Slice 8F-4.
 */
public record DocumentoGraphDemoCasoResultado(
        String codigoCaso,
        String descripcionCaso,
        AccionDocumental accionDocumental,
        TipoDocu tipoDocu,
        Long actaId,
        Long documentoId,
        Long redaccionId,
        EstadoRedaccionDocumento estadoRedaccion,
        boolean redaccionCompleta,
        String storageKey,
        String hashDocu,
        LocalDateTime fhGeneracion,
        boolean mock,
        boolean exitoso,
        String errorMensaje
) {
    public static DocumentoGraphDemoCasoResultado exitoso(
            String codigoCaso,
            String descripcionCaso,
            AccionDocumental accionDocumental,
            TipoDocu tipoDocu,
            Long actaId,
            Long documentoId,
            Long redaccionId,
            EstadoRedaccionDocumento estadoRedaccion,
            boolean redaccionCompleta,
            String storageKey,
            String hashDocu,
            LocalDateTime fhGeneracion,
            boolean mock) {
        return new DocumentoGraphDemoCasoResultado(
                codigoCaso, descripcionCaso, accionDocumental, tipoDocu,
                actaId, documentoId, redaccionId,
                estadoRedaccion, redaccionCompleta,
                storageKey, hashDocu, fhGeneracion,
                mock, true, null);
    }

    public static DocumentoGraphDemoCasoResultado fallido(
            String codigoCaso,
            String descripcionCaso,
            AccionDocumental accionDocumental,
            TipoDocu tipoDocu,
            Long actaId,
            String errorMensaje) {
        return new DocumentoGraphDemoCasoResultado(
                codigoCaso, descripcionCaso, accionDocumental, tipoDocu,
                actaId, null, null,
                null, false,
                null, null, null,
                false, false, errorMensaje);
    }
}
