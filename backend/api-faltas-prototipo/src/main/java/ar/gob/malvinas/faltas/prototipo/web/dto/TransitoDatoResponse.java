package ar.gob.malvinas.faltas.prototipo.web.dto;

public record TransitoDatoResponse(
        boolean ejeUrbano,
        boolean rodadoRetenidoOSecuestrado,
        boolean documentacionRetenida,
        boolean medidaPreventivaAplicable) {
}
