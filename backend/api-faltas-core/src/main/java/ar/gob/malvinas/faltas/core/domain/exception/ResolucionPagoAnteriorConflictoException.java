package ar.gob.malvinas.faltas.core.domain.exception;

/**
 * Un movimiento original (PAGANT) ya tiene un movimiento de aplicacion
 * (movimientoOrigenId) registrado contra una obligacion destino o con un
 * motivo distinto al del reintento actual. La resolucion de un mismo
 * PAGANT admite a lo mas una aplicacion; reintentos compatibles devuelven
 * el resultado existente (no lanzan esta excepcion), solo los incompatibles
 * son conflicto.
 */
public class ResolucionPagoAnteriorConflictoException extends RuntimeException {
    public ResolucionPagoAnteriorConflictoException(String mensaje) {
        super(mensaje);
    }
}
