package ar.gob.malvinas.faltas.core.web.dto;

import ar.gob.malvinas.faltas.core.domain.model.FalActaEvidencia;

import java.time.LocalDateTime;

/**
 * Metadata publica de una fotografia de evidencia (Wave 4 Parte 3D S14).
 *
 * <p>Nunca incluye {@code storageKey}: la descarga se resuelve exclusivamente
 * por {@code GET /api/faltas/actas/{idActa}/fotografias/{id}/archivo}, que
 * mapea el id logico al archivo fisico en el servidor.
 */
public record FotografiaMetadataResponse(
        Long id,
        Long idActa,
        Integer orden,
        String descripcion,
        String nombreOriginal,
        String mimeTipo,
        Long tamanioBytes,
        Integer anchoPx,
        Integer altoPx,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaCapturaExif,
        String idUserAlta,
        String bloqueAlIncorporar,
        String origenIncorporacion
) {
    public static FotografiaMetadataResponse de(FalActaEvidencia e) {
        return new FotografiaMetadataResponse(
                e.getId(), e.getIdActa(), e.getOrden(), e.getDescripcion(), e.getNombreOriginal(),
                e.getMimeTipo(), e.getTamanioBytes(), e.getAnchoPx(), e.getAltoPx(),
                e.getFechaRegistro(), e.getFechaCapturaExif(), e.getIdUserAlta(),
                e.getBloqueAlIncorporar(),
                e.getOrigenIncorporacion() == null ? null : e.getOrigenIncorporacion().name());
    }
}
