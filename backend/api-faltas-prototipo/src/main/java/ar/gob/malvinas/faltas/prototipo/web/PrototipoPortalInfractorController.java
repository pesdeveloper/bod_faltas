package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ActaMock;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.ActaInfractorResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarApelacionAccionRequest;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaInfractorMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints del portal ciudadano del infractor.
 *
 * <p>Rutas p\u00fablicas mantenidas exactamente:
 * <ul>
 *   <li>{@code GET  /api/prototipo/infractor/actas/{codigoQr}}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/registrar-apelacion}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/confirmar-visualizacion-notificacion}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/solicitar-pago-voluntario}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/pagar-voluntario}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/documentos/{tipoDocumento}/ver}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/pagar-condena}
 *   <li>{@code POST /api/prototipo/infractor/actas/{codigoQr}/acciones/consentir-condena}
 * </ul>
 *
 * <p>No expone detalles administrativos al ciudadano. Las actas PARALIZADAS se
 * muestran como {@code EN_TRAMITE}. El mapeo de vista ciudadana vive en
 * {@link ActaInfractorMapper}.
 */
@RestController
@RequestMapping("/api/prototipo")
public class PrototipoPortalInfractorController {

    private final PrototipoStore store;
    private final ActaInfractorMapper actaInfractorMapper;

    public PrototipoPortalInfractorController(
            PrototipoStore store,
            ActaInfractorMapper actaInfractorMapper) {
        this.store = store;
        this.actaInfractorMapper = actaInfractorMapper;
    }

    /**
     * Acceso ciudadano por c\u00f3digo QR/c\u00f3digo ciudadano: el futuro portal del
     * infractor consulta ac\u00e1 usando un c\u00f3digo opaco estable (ver
     * {@link PrototipoStore#codigoQrDeActa(String)}), sin conocer ni exponer
     * el {@code actaId} interno. Devuelve una vista ciudadana m\u00ednima
     * ({@link ActaInfractorResponse}); no expone detalles administrativos
     * innecesarios y no materializa EM/RC/Cmte/Pref/Nro ni recibos: el
     * pago real y los comprobantes se modelar\u00e1n en un slice posterior.
     */
    @GetMapping("/infractor/actas/{codigoQr}")
    public ActaInfractorResponse obtenerActaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return actaInfractorMapper.map(acta);
    }

    /**
     * Presentaci\u00f3n de apelaci\u00f3n desde el portal del infractor por c\u00f3digo QR.
     * El canal se fija internamente como {@code PORTAL_INFRACTOR}; el body es
     * opcional y, si incluye {@code canal}, se ignora (el cliente ciudadano no
     * puede forzar otro canal). Devuelve la vista ciudadana actualizada sin
     * exponer el {@code actaId} interno.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/registrar-apelacion")
    public ActaInfractorResponse registrarApelacionInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr,
            @RequestBody(required = false) RegistrarApelacionAccionRequest request) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.RegistrarApelacionResultado r = store.registrarApelacion(
                acta.id(), PrototipoStore.CanalPresentacionApelacionMock.PORTAL_INFRACTOR);
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarApelacionEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    @PostMapping("/infractor/actas/{codigoQr}/acciones/confirmar-visualizacion-notificacion")
    public ActaInfractorResponse confirmarVisualizacionNotificacionInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.ConfirmarVisualizacionNotificacionPortalResultado r =
                store.confirmarVisualizacionNotificacionPortal(acta.id());
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.SIN_NOTIFICACION_PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No hay notificaci\u00f3n pendiente por DOMICILIO_ELECTRONICO para confirmar desde el portal.");
        }
        if (r.estado() == PrototipoStore.ConfirmarVisualizacionNotificacionPortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    /**
     * Solicitud de pago voluntario iniciada por el infractor desde el portal.
     * No requiere monto: Direccion de Faltas evaluara y, si corresponde,
     * fijara el monto. Devuelve la vista ciudadana actualizada.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/solicitar-pago-voluntario")
    public ActaInfractorResponse solicitarPagoVoluntarioInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.RegistrarSolicitudPagoVoluntarioResultado r =
                store.solicitarPagoVoluntarioDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede solicitar pago voluntario porque el acta se encuentra en revisi\u00f3n.");
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT_FALLO_DICTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede solicitar pago voluntario porque existe un fallo condenatorio en proceso.");
        }
        if (r.estado() == PrototipoStore.RegistrarSolicitudPagoVoluntarioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible solicitar pago voluntario en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    /**
     * El infractor informa el pago voluntario desde el portal.
     * Precondici\u00f3n: monto fijado por Direcci\u00f3n (situacionPago=SOLICITADO u OBSERVADO).
     * Efecto: situacionPago pasa a PENDIENTE_CONFIRMACION; Direcci\u00f3n puede confirmar u observar.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/pagar-voluntario")
    public ActaInfractorResponse pagarVoluntarioInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.InformarPagoVoluntarioDesdePortalResultado r =
                store.informarPagoVoluntarioDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_SIN_MONTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede pagar porque Direcci\u00f3n de Faltas a\u00fan no fij\u00f3 el monto.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_INFORMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya se encuentra en proceso de acreditaci\u00f3n.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_YA_CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El pago voluntario ya fue confirmado.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT_FALLO_DICTADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede informar pago voluntario porque existe un fallo condenatorio dictado.");
        }
        if (r.estado() == PrototipoStore.InformarPagoVoluntarioDesdePortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible informar el pago en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    /**
     * El infractor abre un documento notificable desde el portal, confirmando
     * su visualizaci\u00f3n. Si el documento ya fue notificado, es idempotente.
     * Devuelve 404 si el documento no existe o no es visible para el infractor.
     */
    @PostMapping("/infractor/actas/{codigoQr}/documentos/{tipoDocumento}/ver")
    public ActaInfractorResponse verDocumentoInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr,
            @PathVariable("tipoDocumento") String tipoDocumento) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.VerDocumentoPortalResultado r = store.verDocumentoPortal(acta.id(), tipoDocumento);
        if (r.estado() == PrototipoStore.VerDocumentoPortalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.VerDocumentoPortalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    /**
     * El infractor informa el pago de condena desde el portal.
     * Precondici\u00f3n: resultadoFinal=CONDENA_FIRME y situacionPagoCondena=PENDIENTE u OBSERVADO.
     * Devuelve 409 si no se puede pagar en el estado actual.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/pagar-condena")
    public ActaInfractorResponse pagarCondenaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.PagoCondenaResultado r = store.informarPagoCondenaDesdePortal(acta.id());
        if (r.estado() == PrototipoStore.PagoCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.PagoCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }

    /**
     * El infractor consiente la condena desde el portal, renunciando a apelar.
     * Precondici\u00f3n: resultadoFinal=CONDENADO, sin apelacion presentada.
     * Efecto: resultadoFinal=CONDENA_FIRME, situacionPagoCondena=PENDIENTE.
     */
    @PostMapping("/infractor/actas/{codigoQr}/acciones/consentir-condena")
    public ActaInfractorResponse consentirCondenaInfractorPorCodigoQr(
            @PathVariable("codigoQr") String codigoQr) {
        ActaMock acta = store.findActaPorCodigoQr(codigoQr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        PrototipoStore.ConsentirCondenaResultado r = store.consentirCondena(acta.id());
        if (r.estado() == PrototipoStore.ConsentirCondenaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ConsentirCondenaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede consentir la condena en el estado actual del acta.");
        }
        ActaMock actualizada = store.findActaPorCodigoQr(codigoQr).orElse(acta);
        return actaInfractorMapper.map(actualizada);
    }
}
