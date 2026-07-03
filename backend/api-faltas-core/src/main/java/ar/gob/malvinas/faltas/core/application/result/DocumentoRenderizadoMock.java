package ar.gob.malvinas.faltas.core.application.result;

import java.time.LocalDateTime;

/**
 * Resultado de renderizado mock de un documento PDF.
 *
 * Representa la materializacion final simulada de un documento redactado.
 * No contiene bytes PDF reales ni usa librerias de generacion de PDF.
 *
 * contenidoMock: texto plano deterministico con marcas de diagnostico.
 * storageKeyMock: URL esquema mock:// no vinculada a ningun storage real.
 * hashMock: SHA-256 sobre contenidoMock, deterministico para mismo contenido.
 * sizeBytes: longitud en bytes UTF-8 del contenidoMock.
 *
 * Slice 8F-3.
 */
public record DocumentoRenderizadoMock(
        Long documentoId,
        Long redaccionId,
        String nombreArchivo,
        String mimeType,
        String storageKeyMock,
        String hashMock,
        String contenidoMock,
        LocalDateTime fhGeneracion,
        long sizeBytes
) {}