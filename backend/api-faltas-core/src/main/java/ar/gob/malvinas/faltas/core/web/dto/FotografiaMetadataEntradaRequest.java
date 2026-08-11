package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Metadata opcional de una fotografia dentro de un lote multipart (Wave 4
 * Parte 3D S12/S13). Se asocia a la parte de archivo correspondiente por
 * <b>posicion en el arreglo</b> (misma posicion que la parte {@code fotografias}
 * repetida), nunca por nombre de archivo: el nombre lo declara el cliente y
 * puede repetirse o faltar.
 *
 * <p>No transporta autor, coordenadas, clasificacion ni orden: ninguno de
 * esos campos lo informa el cliente (Parte 3D S5.1, S5.6, S5.8, S5.9).
 */
public record FotografiaMetadataEntradaRequest(String descripcion) {
}
