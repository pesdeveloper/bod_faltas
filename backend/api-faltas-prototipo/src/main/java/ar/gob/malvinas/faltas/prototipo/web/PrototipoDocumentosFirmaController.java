package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.FirmarDocumentoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarMedidaPreventivaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarNotificacionActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarNulidadAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarRectificacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarResolucionAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoDocumentosFirmaController {

    private final PrototipoStore store;

    public PrototipoDocumentosFirmaController(PrototipoStore store) {
        this.store = store;
    }

    @PostMapping("/actas/{id}/acciones/generar-medida-preventiva")
    public GenerarMedidaPreventivaAccionResponse generarMedidaPreventiva(@PathVariable("id") String id) {
        PrototipoStore.GenerarMedidaPreventivaResultado r = store.generarMedidaPreventiva(id);
        if (r.estado() == PrototipoStore.GenerarMedidaPreventivaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarMedidaPreventivaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarMedidaPreventivaAccionResponse(
                "OK",
                "Medida preventiva generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/generar-notificacion-acta")
    public GenerarNotificacionActaAccionResponse generarNotificacionActa(@PathVariable("id") String id) {
        PrototipoStore.GenerarNotificacionActaResultado r = store.generarNotificacionActa(id);
        if (r.estado() == PrototipoStore.GenerarNotificacionActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarNotificacionActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarNotificacionActaAccionResponse(
                "OK",
                "Notificación del acta generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Producción de la pieza NULIDAD como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran NULIDAD como pieza
     * requerida (caso demo alineado con spec: ACTA-0012 con
     * estadoProcesoActual = PENDIENTE_NULIDAD). Semánticamente: produce la
     * pieza nulidad, genera el documento asociado pendiente de firma y
     * emite el evento NULIDAD_GENERADA; la transición de bandeja sigue la
     * misma regla agregadora de piezas que MEDIDA_PREVENTIVA y
     * NOTIFICACION_ACTA (no se declara nulidad como bandeja terminal).
     */
    @PostMapping("/actas/{id}/acciones/generar-nulidad")
    public GenerarNulidadAccionResponse generarNulidad(@PathVariable("id") String id) {
        PrototipoStore.GenerarNulidadResultado r = store.generarNulidad(id);
        if (r.estado() == PrototipoStore.GenerarNulidadEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarNulidadEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarNulidadAccionResponse(
                "OK",
                "Nulidad generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Produccion de la pieza RESOLUCION como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran RESOLUCION como pieza
     * requerida (caso demo: ACTA-0011 con estadoProcesoActual =
     * PENDIENTE_RESOLUCION). Al firmarse el documento generado, la acta
     * pasa a PENDIENTE_NOTIFICACION.
     */
    @PostMapping("/actas/{id}/acciones/generar-resolucion")
    public GenerarResolucionAccionResponse generarResolucion(@PathVariable("id") String id) {
        PrototipoStore.GenerarResolucionResultado r = store.generarResolucion(id);
        if (r.estado() == PrototipoStore.GenerarResolucionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarResolucionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarResolucionAccionResponse(
                "OK",
                "Resolucion generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Produccion de la pieza RECTIFICACION como pieza no-fallo dentro del
     * circuito documental/resolutivo. Solo aplica a actas en
     * PENDIENTES_RESOLUCION_REDACCION que declaran RECTIFICACION como pieza
     * requerida (caso demo: ACTA-0014 con estadoProcesoActual =
     * PENDIENTE_RECTIFICACION). Al firmarse el documento generado, la acta
     * pasa a PENDIENTE_NOTIFICACION.
     */
    @PostMapping("/actas/{id}/acciones/generar-rectificacion")
    public GenerarRectificacionAccionResponse generarRectificacion(@PathVariable("id") String id) {
        PrototipoStore.GenerarRectificacionResultado r = store.generarRectificacion(id);
        if (r.estado() == PrototipoStore.GenerarRectificacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.GenerarRectificacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new GenerarRectificacionAccionResponse(
                "OK",
                "Rectificacion generada.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/firmar-documento/{documentoId}")
    public FirmarDocumentoAccionResponse firmarDocumento(
            @PathVariable("id") String id,
            @PathVariable("documentoId") String documentoId) {
        PrototipoStore.FirmarDocumentoResultado r = store.firmarDocumento(id, documentoId);
        if (r.estado() == PrototipoStore.FirmarDocumentoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.FirmarDocumentoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new FirmarDocumentoAccionResponse(
                "OK",
                "Documento firmado.",
                r.actaId(),
                r.documentoId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }
}
