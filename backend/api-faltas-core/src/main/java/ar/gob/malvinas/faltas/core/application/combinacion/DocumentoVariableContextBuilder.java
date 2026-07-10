package ar.gob.malvinas.faltas.core.application.combinacion;

import ar.gob.malvinas.faltas.core.domain.model.FalActa;
import ar.gob.malvinas.faltas.core.domain.model.FalPersona;
import ar.gob.malvinas.faltas.core.domain.model.FalPersonaDomicilio;
import ar.gob.malvinas.faltas.core.domain.model.FalActaFallo;
import ar.gob.malvinas.faltas.core.domain.model.FalDocumento;
import ar.gob.malvinas.faltas.core.domain.model.FalNotificacion;
import ar.gob.malvinas.faltas.core.domain.model.FalPagoVoluntario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Constructor de contexto de variables documentales a partir del dominio in-memory.
 *
 * Namespaces cubiertos:
 *   acta.* - desde FalActa
 *   infractor.* - desde FalActa
 *   domicilioInfractor.* - desde FalActa
 *   domicilioInfraccion.* - desde FalActa
 *   ubicacion.* - desde FalActa
 *   infraccion.* - desde FalActa.observaciones
 *   documento.* - desde FalDocumento
 *   sistema.* - constantes del sistema
 *   licencia.* - mock deterministico
 *   nomenclatura.* - mock deterministico
 *   fallo.* - desde FalActaFallo (opcional)
 *   pago.* - desde FalPagoVoluntario (opcional)
 *   notificacion.* - desde FalNotificacion (opcional)
 *
 * Guardrails:
 *   - No usa reflection.
 *   - No usa SpEL.
 *   - No usa scripts ni eval.
 *   - Variables opcionales ausentes no se incluyen (no se inventan valores).
 *   - Variables requeridas presentes siempre se incluyen.
 *
 * Slice 8F-1: metodo base buildDesdeActa(FalActa, FalDocumento).
 * Slice 8F-2: overloads con fallo, pago, notificacion.
 */
public final class DocumentoVariableContextBuilder {

    private static final String MUNICIPIO_NOMBRE = "Malvinas Argentinas";
    private static final String NOMENCLATURA_MANZANA_DEMO = "12";
    private static final String NOMENCLATURA_PARCELA_DEMO = "4B";

    private DocumentoVariableContextBuilder() {}

    /**
     * Construye el contexto base desde acta y documento.
     * Cubre namespaces: acta, infractor, domicilioInfractor, domicilioInfraccion,
     * ubicacion, infraccion, documento, sistema, licencia, nomenclatura.
     */
    public static Map<String, Object> buildDesdeActa(FalActa acta, FalDocumento documento, LocalDateTime fechaActual) {
        Map<String, Object> ctx = buildDesdeActa(acta, documento, null, null, null, fechaActual);
        if (acta.getDomicilioHecho() != null && !ctx.containsKey("domicilioInfractor.texto"))
            ctx.put("domicilioInfractor.texto", acta.getDomicilioHecho());
        return ctx;
    }

    /**
     * Construye el contexto completo desde acta y objetos relacionados opcionales.
     * Los parametros fallo, pago, notificacion pueden ser null.
     */
    public static Map<String, Object> buildDesdeActa(
            FalActa acta,
            FalDocumento documento,
            FalActaFallo fallo,
            FalPagoVoluntario pago,
            FalNotificacion notificacion, LocalDateTime fechaActual) {

        Map<String, Object> ctx = new HashMap<>();

        // --- acta ---
        if (acta.getNroActa() != null) ctx.put("acta.nroActa", acta.getNroActa());
        if (acta.getFechaLabrado() != null) ctx.put("acta.fechaLabrado", acta.getFechaLabrado());
        // --- domicilios ---
        if (acta.getDomicilioHecho() != null) ctx.put("domicilioInfraccion.texto", acta.getDomicilioHecho());

        // --- ubicacion ---
        if (acta.getLatInfr() != null) ctx.put("ubicacion.lat", acta.getLatInfr());
        if (acta.getLonInfr() != null) ctx.put("ubicacion.lon", acta.getLonInfr());

        // --- infraccion (desde observaciones del acta) ---
        if (acta.getObservaciones() != null && !acta.getObservaciones().isBlank())
            ctx.put("infraccion.descripcion", acta.getObservaciones());

        // --- infractor (campos directos del acta, usados cuando no hay FalPersona) ---
        if (acta.getInfractorNombre() != null && !acta.getInfractorNombre().isBlank())
            ctx.put("infractor.nombreCompleto", acta.getInfractorNombre());
        if (acta.getInfractorDocumento() != null && !acta.getInfractorDocumento().isBlank())
            ctx.put("infractor.documento", acta.getInfractorDocumento());

        // --- licencia (mock deterministico) ---
        ctx.put("licencia.municipioEmisor", MUNICIPIO_NOMBRE);

        // --- nomenclatura (mock deterministico) ---
        ctx.put("nomenclatura.manzana", NOMENCLATURA_MANZANA_DEMO);
        ctx.put("nomenclatura.parcela", NOMENCLATURA_PARCELA_DEMO);

        // --- documento ---
        if (documento != null && documento.getNroDocu() != null)
            ctx.put("documento.nroDocu", documento.getNroDocu());

        // --- sistema (siempre presentes) ---
        ctx.put("sistema.municipioNombre", MUNICIPIO_NOMBRE);
        ctx.put("sistema.fechaActual", fechaActual);

        // --- fallo (opcional) ---
        if (fallo != null) {
            ctx.put("fallo.tipo", fallo.getTipoFallo().name());
            if (fallo.getMontoCondena() != null) ctx.put("fallo.monto", fallo.getMontoCondena());
            if (fallo.getFundamentos() != null) ctx.put("fallo.fundamentos", fallo.getFundamentos());
            if (fallo.getFechaDictado() != null) ctx.put("fallo.fechaDictado", fallo.getFechaDictado());
        }

        // --- pago (opcional) ---
        if (pago != null) {
            if (pago.getMonto() != null) ctx.put("pago.monto", pago.getMonto());
            if (pago.getReferenciaPago() != null) ctx.put("pago.referenciaPago", pago.getReferenciaPago());
            ctx.put("pago.estado", pago.getEstadoPagoVoluntario().name());
            if (pago.getFechaVencimiento() != null) ctx.put("pago.fechaVencimiento", pago.getFechaVencimiento());
        }

        // --- notificacion (opcional) ---
        if (notificacion != null) {
            if (notificacion.getCanal() != null) ctx.put("notificacion.canal", notificacion.getCanal());
            if (notificacion.getFechaEnvio() != null) ctx.put("notificacion.fechaEnvio", notificacion.getFechaEnvio());
        }

        return ctx;
    }

    /**
     * Overload con persona y domicilio del infractor (Slice 8F-11D+).
     * persona y domicilio pueden ser null.
     */
    public static Map<String, Object> buildDesdeActa(
            FalActa acta,
            FalDocumento documento,
            FalPersona persona,
            FalPersonaDomicilio domicilio,
            FalActaFallo fallo,
            FalPagoVoluntario pago,
            FalNotificacion notificacion, LocalDateTime fechaActual) {

        Map<String, Object> ctx = buildDesdeActa(acta, documento, fallo, pago, notificacion, fechaActual);

        if (persona != null) {
            if (persona.getNombreMostrar() != null)
                ctx.put("infractor.nombreCompleto", persona.getNombreMostrar());
            if (persona.getNroDoc() != null)
                ctx.put("infractor.nroDoc", persona.getNroDoc());
        }
        if (domicilio != null) {
            if (domicilio.getDomicilioTxt() != null)
                ctx.put("domicilioInfractor.texto", domicilio.getDomicilioTxt());
        }
        return ctx;
    }
}