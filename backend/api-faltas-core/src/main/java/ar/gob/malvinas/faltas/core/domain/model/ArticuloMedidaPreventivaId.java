package ar.gob.malvinas.faltas.core.domain.model;

/**
 * Clave primaria compuesta de FalArticuloMedidaPreventiva.
 * Inmutable. No tiene ID sintético.
 */
public record ArticuloMedidaPreventivaId(
        Long articuloId,
        Long medidaPreventivaId) {

    public ArticuloMedidaPreventivaId {
        if (articuloId == null) throw new IllegalArgumentException("articuloId es obligatorio");
        if (medidaPreventivaId == null) throw new IllegalArgumentException("medidaPreventivaId es obligatorio");
    }
}
