package ar.gob.malvinas.faltas.core.application.port;

import ar.gob.malvinas.faltas.core.domain.exception.ImagenEvidenciaInvalidaException;

/**
 * Puerto del pipeline determinista de normalizacion de fotografias
 * (Wave 4 Parte 3D S7). Unica implementacion productiva:
 * {@code TwelveMonkeysImagenEvidenciaProcesador} (javax.imageio + TwelveMonkeys
 * WEBP + metadata-extractor EXIF).
 *
 * <p>Pipeline exigido por el prompt, en este orden:
 *
 * <pre>
 * 1. validar formato real por firma/decodificacion (nunca por extension/Content-Type)
 * 2. corregir orientacion EXIF
 * 3. normalizar dimensiones (lado mayor configurable, nunca upscale)
 * 4. recomprimir en el formato final de la estrategia (JPEG)
 * 5. eliminar toda metadata no necesaria (incluidas coordenadas GPS)
 * 6. calcular SHA-256 sobre los bytes finales
 * 7. devolver metadata tecnica (dimensiones, MIME, fecha EXIF si existe)
 * </pre>
 */
public interface ImagenEvidenciaProcesador {

    /**
     * Procesa una fotografia de entrada.
     *
     * @param contenidoOriginal       bytes exactos recibidos del cliente
     * @param nombreOriginalDeclarado nombre de archivo declarado por el cliente
     *                                (puede ser {@code null}); se sanea y trunca,
     *                                nunca se usa como ruta
     * @return metadata y bytes finales, deterministas dentro de lo razonable
     * @throws ImagenEvidenciaInvalidaException si el archivo esta vacio, no es
     *         una imagen valida en un formato soportado, esta corrupto, excede
     *         los limites configurados de tamaño/dimensiones/pixeles, o es
     *         contenido "polyglot"/sospechoso que el decoder no puede validar
     */
    ImagenProcesada procesar(byte[] contenidoOriginal, String nombreOriginalDeclarado);
}
