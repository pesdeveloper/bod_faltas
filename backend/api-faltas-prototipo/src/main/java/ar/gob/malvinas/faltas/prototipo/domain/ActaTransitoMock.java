package ar.gob.malvinas.faltas.prototipo.domain;

/**
 * Satélite mock: datos de tránsito propios del acta (independientes del
 * circuito sancionatorio y de la “constatación” vía acción de API). En el
 * producto, equivalen a campos del registro de acta vial; acá, flags mínimos
 * para nacimiento demo y proyección a anclas/condiciones de material.
 */
public record ActaTransitoMock(
        boolean ejeUrbano,
        boolean rodadoRetenidoOSecuestrado,
        boolean documentacionRetenida,
        boolean medidaPreventivaAplicable) {
}
