package ar.gob.malvinas.faltas.core.application.result;

import ar.gob.malvinas.faltas.core.domain.enums.EstadoRedaccionDocumento;

import java.time.LocalDateTime;

/**
 * Respuesta de la operacion de confirmacion y generacion mock de documento PDF.
 *
 * mock=true indica que los metadatos son simulados y no corresponden a storage real.
 * estadoRedaccion debe ser CONFIRMADA tras una operacion exitosa.
 *
 * Slice 8F-3.
 */
public record DocumentoGeneracionMockResponse(
        Long documentoId,
        Long redaccionId,
        EstadoRedaccionDocumento estadoRedaccion,
        String storageKey,
        String hashDocu,
        LocalDateTime fhGeneracion,
        String nombreArchivo,
        String mimeType,
        long sizeBytes,
        boolean mock
) {}
