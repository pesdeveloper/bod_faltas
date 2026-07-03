package ar.gob.malvinas.faltas.core.domain.enums;

/**
 * Estado del bloqueante material sobre un acta.
 *
 * PENDIENTE: el bloqueante esta activo, impide el cierre del acta.
 * CUMPLIDO: el bloqueante fue resuelto, ya no impide el cierre.
 * ANULADO: el bloqueante fue anulado administrativamente.
 *
 * Un bloqueante activo es aquel con siActivo == true (PENDIENTE).
 */
public enum EstadoBloqueanteMaterial {
    PENDIENTE,
    CUMPLIDO,
    ANULADO
}
