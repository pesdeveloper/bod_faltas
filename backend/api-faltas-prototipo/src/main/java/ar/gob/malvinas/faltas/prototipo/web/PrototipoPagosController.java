package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import static ar.gob.malvinas.faltas.prototipo.domain.PrototipoResultadoFinalHelper.resultadoFinalVigente;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import ar.gob.malvinas.faltas.prototipo.web.dto.AdjuntarComprobantePagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConsentirCondenaYRegistrarPagoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoVoluntarioExternoAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ConfirmarPagoVoluntarioExternoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ObservarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PagoCondenaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.PagoInformadoResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarPagoInformadoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarSolicitudPagoVoluntarioAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarSolicitudPagoVoluntarioAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarVencimientoPagoVoluntarioAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoPagosController {

    private final PrototipoStore store;

    public PrototipoPagosController(PrototipoStore store) {
        this.store = store;
    }

    // -------------------------------------------------------------------------
    // Pago voluntario / pago informado
    // -------------------------------------------------------------------------

    /**
     * Acción administrativa "Pago voluntario": Dirección de Faltas fija el
     * monto del acta y deja el pago voluntario habilitado. El portal del
     * infractor todavía no existe; cuando exista, deberá mostrar "Pagar"
     * en lugar de "Solicitar pago voluntario" en cuanto detecte monto
     * fijado. Esta acción NO genera comprobantes (sin EM, sin RC, sin
     * Cmte/Pref/Nro): los comprobantes sólo se materializan en el
     * proceso externo de pago.
     */
    @PostMapping("/actas/{id}/acciones/registrar-solicitud-pago-voluntario")
    public RegistrarSolicitudPagoVoluntarioAccionResponse registrarSolicitudPagoVoluntario(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarSolicitudPagoVoluntarioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con monto");
        }
        BigDecimal monto = request.monto();
        if (monto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto requerido");
        }
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto debe ser mayor a cero");
        }
        PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado r =
                store.registrarSolicitudPagoVoluntario(id, monto);
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarSolicitudPagoVoluntarioAccionResponse(
                "OK",
                "Pago voluntario habilitado por Dirección de Faltas con monto fijado.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.montoPagoVoluntario());
    }

    /**
     * Dirección de Faltas fija el monto de pago voluntario cuando el infractor
     * solicitó el pago desde el portal pero no hay monto asignado aún.
     * Precondición: {@code situacionPago=SOLICITADO} y
     * {@code montoPagoVoluntario=null}. Tras la acción, el infractor puede
     * informar el pago desde el portal.
     */
    @PostMapping("/actas/{id}/acciones/fijar-monto-pago-voluntario")
    public RegistrarSolicitudPagoVoluntarioAccionResponse fijarMontoPagoVoluntario(
            @PathVariable("id") String id,
            @RequestBody(required = false) RegistrarSolicitudPagoVoluntarioAccionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body requerido con monto");
        }
        BigDecimal monto = request.monto();
        if (monto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto requerido");
        }
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "monto debe ser mayor a cero");
        }
        PrototipoStore.FijarMontoPagoVoluntarioResultado r =
                store.fijarMontoPagoVoluntario(id, monto);
        if (r.estado() == PrototipoStore.FijarMontoPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.FijarMontoPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible fijar monto: el acta no está en estado SOLICITADO o se encuentra en bandeja no operable.");
        }
        return new RegistrarSolicitudPagoVoluntarioAccionResponse(
                "OK",
                "Monto de pago voluntario fijado por Dirección de Faltas. El infractor puede ahora informar el pago.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                store.getAccionPendiente(r.actaId()),
                r.montoPagoVoluntario());
    }

    /**
     * Dirección de Faltas registra que el plazo/oportunidad de pago voluntario
     * venció sin que el infractor pagara. Precondición: {@code situacionPago=SOLICITADO}
     * y {@code resultadoFinal=SIN_RESULTADO_FINAL}. Tras la acción, el acta queda con
     * {@code situacionPago=VENCIDO} y el trámite puede continuar a fallo de fondo.
     * El {@code montoPagoVoluntario} se conserva como dato histórico.
     */
    @PostMapping("/actas/{id}/acciones/registrar-vencimiento-pago-voluntario")
    public RegistrarVencimientoPagoVoluntarioAccionResponse registrarVencimientoPagoVoluntario(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarVencimientoPagoVoluntarioResultado r =
                store.registrarVencimientoPagoVoluntario(id);
        if (r.estado() == PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarVencimientoPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible registrar vencimiento: el acta no esta en situacion SOLICITADO o se encuentra en bandeja no operable.");
        }
        return new RegistrarVencimientoPagoVoluntarioAccionResponse(
                "OK",
                "Vencimiento de pago voluntario registrado. El tramite puede continuar a fallo.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.situacionPago() != null ? r.situacionPago().name() : null,
                r.montoPagoVoluntario());
    }

    @PostMapping("/actas/{id}/acciones/registrar-pago-informado")
    public RegistrarPagoInformadoAccionResponse registrarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.RegistrarPagoInformadoResultado r = store.registrarPagoInformado(id);
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.CONFLICT_SIN_MONTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede informar pago voluntario porque Dirección de Faltas aún no fijó el monto.");
        }
        if (r.estado() == PrototipoStore.RegistrarPagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarPagoInformadoAccionResponse(
                "OK",
                "Pago informado registrado (mock). Aún sin confirmación interna.",
                r.actaId(),
                r.situacionPago().name());
    }

    @PostMapping("/actas/{id}/acciones/adjuntar-comprobante-pago-informado")
    public AdjuntarComprobantePagoInformadoAccionResponse adjuntarComprobantePagoInformado(
            @PathVariable("id") String id,
            @RequestParam(value = "nombreArchivo", required = false) String nombreArchivo) {
        PrototipoStore.AdjuntarComprobantePagoInformadoResultado r =
                store.adjuntarComprobantePagoInformado(id, nombreArchivo);
        if (r.estado() == PrototipoStore.AdjuntarComprobantePagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.AdjuntarComprobantePagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        PagoInformadoResponse pago = ActaDetalleMapper.mapPagoInformado(r.pagoInformado());
        return new AdjuntarComprobantePagoInformadoAccionResponse(
                "OK",
                "Comprobante adjuntado (mock); queda pendiente de confirmación interna.",
                r.actaId(),
                r.situacionPago().name(),
                r.accionPendiente(),
                pago);
    }

    @PostMapping("/actas/{id}/acciones/confirmar-pago-informado")
    public ConfirmarPagoInformadoAccionResponse confirmarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.ConfirmarPagoInformadoResultado r = store.confirmarPagoInformado(id);
        if (r.estado() == PrototipoStore.ConfirmarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoInformadoEstado.CONFLICT) {
            throw conflictConfirmarPagoInformado(id);
        }
        return new ConfirmarPagoInformadoAccionResponse(
                "OK",
                "Pago confirmado internamente (mock).",
                r.actaId(),
                r.situacionPago().name());
    }

    /**
     * Confirma pago voluntario desde sistema externo de cobro (gateway/caja).
     * El monto recibido debe coincidir con el monto fijado por Dirección.
     * Precondición: situacionPago == PENDIENTE_CONFIRMACION.
     */
    @PostMapping("/actas/{id}/acciones/confirmar-pago-voluntario-externo")
    public ConfirmarPagoVoluntarioExternoAccionResponse confirmarPagoVoluntarioExterno(
            @PathVariable("id") String id,
            @RequestBody(required = false) ConfirmarPagoVoluntarioExternoAccionRequest request) {
        BigDecimal monto = request != null ? request.monto() : null;
        String origen = request != null && request.origen() != null ? request.origen().trim() : "SISTEMA_EXTERNO";
        PrototipoStore.ConfirmarPagoVoluntarioExternoResultado r =
                store.confirmarPagoVoluntarioExterno(id, monto, origen);
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_YA_CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya fue confirmado.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_SIN_PAGO_PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No existe pago voluntario informado previo. El infractor debe iniciar el pago desde el portal.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT_MONTO_DISTINTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El monto informado no coincide con el monto fijado para este expediente.");
        }
        if (r.estado() == PrototipoStore.ConfirmarPagoVoluntarioExternoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible confirmar el pago en el estado actual del acta.");
        }
        return new ConfirmarPagoVoluntarioExternoAccionResponse(
                "OK",
                "Pago voluntario confirmado por sistema externo de cobro.",
                r.actaId(),
                r.situacionPago().name());
    }

    @PostMapping("/actas/{id}/acciones/observar-pago-informado")
    public ObservarPagoInformadoAccionResponse observarPagoInformado(@PathVariable("id") String id) {
        PrototipoStore.ObservarPagoInformadoResultado r = store.observarPagoInformado(id);
        if (r.estado() == PrototipoStore.ObservarPagoInformadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ObservarPagoInformadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ObservarPagoInformadoAccionResponse(
                "OK",
                "Pago observado/no confirmado internamente (mock).",
                r.actaId(),
                r.situacionPago().name());
    }

    // -------------------------------------------------------------------------
    // Pago de condena
    // -------------------------------------------------------------------------

    @PostMapping("/actas/{id}/acciones/informar-pago-condena")
    public PagoCondenaAccionResponse informarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.informarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena informado correctamente.");
    }

    @PostMapping("/actas/{id}/acciones/confirmar-pago-condena")
    public PagoCondenaAccionResponse confirmarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.confirmarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena confirmado correctamente.");
    }

    @PostMapping("/actas/{id}/acciones/observar-pago-condena")
    public PagoCondenaAccionResponse observarPagoCondena(@PathVariable("id") String id) {
        PrototipoStore.PagoCondenaResultado r = store.observarPagoCondena(id);
        return mapPagoCondena(r, "Pago de condena observado.");
    }

    // -------------------------------------------------------------------------
    // Consentimiento + pago
    // -------------------------------------------------------------------------

    /**
     * Dirección registra en un único acto el consentimiento presencial de la
     * condena y el pago correspondiente. Precondición:
     * {@code resultadoFinal=CONDENADO}, sin apelación presentada,
     * {@code situacionPagoCondena=NO_APLICA}, {@code situacionPago=SIN_PAGO},
     * {@code montoCondena > 0}. Efecto: {@code resultadoFinal=CONDENA_FIRME},
     * {@code situacionPagoCondena=INFORMADO},
     * {@code situacionPago=PENDIENTE_CONFIRMACION}, {@code tipoPago=CONDENA}.
     * No confirma la acreditación; luego quedan disponibles confirmar y
     * observar acreditación.
     */
    @PostMapping("/actas/{id}/acciones/consentir-condena-y-registrar-pago")
    public ConsentirCondenaYRegistrarPagoAccionResponse consentirCondenaYRegistrarPago(
            @PathVariable("id") String id) {
        if (!store.existeActa(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        PrototipoStore.ConsentirCondenaYRegistrarPagoResultado r =
                store.consentirCondenaYRegistrarPago(id);
        if (r.estado() == PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConsentirCondenaYRegistrarPagoEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede consentir la condena y registrar el pago en el estado actual del acta.");
        }
        return new ConsentirCondenaYRegistrarPagoAccionResponse(
                "OK",
                "Condena consentida presencialmente y pago registrado."
                        + " La acreditacion queda pendiente de confirmacion.",
                id,
                r.resultadoFinal() != null ? r.resultadoFinal().name() : null,
                r.situacionPagoCondena() != null ? r.situacionPagoCondena().name() : null);
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private PagoCondenaAccionResponse mapPagoCondena(
            PrototipoStore.PagoCondenaResultado r,
            String mensajeOk) {
        if (r.estado() == PrototipoStore.PagoCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.PagoCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new PagoCondenaAccionResponse(
                "OK",
                mensajeOk,
                r.actaId(),
                r.situacionPagoCondena().name());
    }

    private ResponseStatusException conflictConfirmarPagoInformado(String actaId) {
        PrototipoStore.ResultadoFinalCierreMock rf = resultadoFinalVigente(store, actaId);
        if (rf == PrototipoStore.ResultadoFinalCierreMock.ABSUELTO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENADO
                || rf == PrototipoStore.ResultadoFinalCierreMock.CONDENA_FIRME) {
            return new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede confirmar pago informado (flujo viejo): resultado jurídico vigente "
                            + rf.name()
                            + ".");
        }
        return new ResponseStatusException(HttpStatus.CONFLICT);
    }

}
