package ar.gob.malvinas.faltas.core.application.command;

/**
 * Comando para registrar el pago externo de una gestion externa activa.
 *
 * Slice 6C: emite PAGAPR. Aplica a cualquier tipo de gestion externa:
 * apremio, juzgado de paz u otra.
 *
 * actaId es obligatorio.
 * observaciones es opcional; si se informa, se incluye en FalActaEvento.descripcion
 * como puente transitorio hasta implementar FalObservacion en Slice 9/JDBC.
 * Las observaciones textuales NO se persisten como columna en fal_acta_gestion_externa.
 */
public record RegistrarPagoExternoGestionCommand(
        Long actaId,
        String observaciones
) {}

