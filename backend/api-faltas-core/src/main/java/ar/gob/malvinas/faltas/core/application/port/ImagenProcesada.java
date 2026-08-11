package ar.gob.malvinas.faltas.core.application.port;

import java.time.LocalDateTime;

/**
 * Resultado determinista del pipeline de normalizacion de una fotografia
 * (Wave 4 Parte 3D S7). {@code sha256Hex} se calcula sobre {@code bytesFinal},
 * nunca sobre el archivo de entrada.
 *
 * <p>{@code fechaCapturaExif} es informativa y puede ser {@code null}; nunca
 * proviene del cliente, solo de la metadata EXIF del archivo de entrada, leida
 * antes de eliminar toda la metadata del archivo final.
 *
 * <p>No incluye coordenadas: el pipeline las descarta siempre (Parte 3D S5.9).
 */
public record ImagenProcesada(
        byte[] bytesFinal,
        String mimeTipoFinal,
        int anchoPx,
        int altoPx,
        String sha256Hex,
        LocalDateTime fechaCapturaExif,
        String nombreOriginalSaneado
) {
}
