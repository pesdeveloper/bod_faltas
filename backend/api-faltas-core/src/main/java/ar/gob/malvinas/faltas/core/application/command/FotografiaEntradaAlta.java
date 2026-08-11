package ar.gob.malvinas.faltas.core.application.command;

/**
 * Fotografia de entrada, cruda, todavia sin procesar (Wave 4 Parte 3D).
 *
 * <p>Transporta exactamente lo que el cliente puede aportar: los bytes del
 * archivo, el nombre de archivo declarado (informativo, nunca usado como
 * ruta) y la descripcion opcional. No transporta autor, coordenadas,
 * clasificacion ni orden: esos campos nunca los informa el cliente
 * (Parte 3D S5.1, S5.6, S5.8, S5.9).
 *
 * <p>Es el mismo tipo tanto para el alta con fotografias como para la carga
 * posterior: ambos casos de uso reciben una lista de este record y delegan en
 * {@code FotografiaEvidenciaService}.
 */
public record FotografiaEntradaAlta(
        byte[] contenido,
        String nombreOriginalDeclarado,
        String descripcion
) {
}
