package ar.gob.malvinas.faltas.core.web.dto;

/**
 * Request para registrar el pago externo de una gestion externa activa.
 *
 * observaciones es opcional. Si se informa, se incluye en el evento PAGAPR
 * como puente transitorio hasta Slice 9/JDBC (FalObservacion).
 */
public record RegistrarPagoExternoGestionRequest(
        String observaciones
) {}
