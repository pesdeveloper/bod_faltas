package ar.gob.malvinas.faltas.prototipo.web.dto;

/**
 * Respuesta de POST /actas/{id}/acciones/consentir-condena-y-registrar-pago.
 * Confirma la transición a CONDENA_FIRME con pago INFORMADO pendiente de
 * acreditación. El campo resultadoFinal siempre es CONDENA_FIRME en OK.
 */
public record ConsentirCondenaYRegistrarPagoAccionResponse(
        String status,
        String mensaje,
        String actaId,
        String resultadoFinal,
        String situacionPagoCondena) {}
