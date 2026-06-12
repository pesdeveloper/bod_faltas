package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.domain.ResultadoNotificacion;
import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.GenerarLoteCorreoPostalRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalAcuseRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalAcuseResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.NotificadorMunicipalNotificacionResponse;
import ar.gob.malvinas.faltas.prototipo.web.mapper.ActaDetalleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

/**
 * Endpoints de canales de notificaci\u00f3n externos: notificador municipal y correo postal.
 *
 * <p>Rutas p\u00fablicas mantenidas exactamente:
 * <ul>
 *   <li>{@code GET  /api/prototipo/notificaciones/notificador-municipal}
 *   <li>{@code POST /api/prototipo/notificaciones/notificador-municipal/{notificacionId}/acuse}
 *   <li>{@code GET  /api/prototipo/notificaciones/correo/listas-para-lote}
 *   <li>{@code GET  /api/prototipo/notificaciones/correo/lotes}
 *   <li>{@code POST /api/prototipo/notificaciones/correo/lotes/{loteId}/anular}
 *   <li>{@code POST /api/prototipo/notificaciones/correo/{notificacionId}/enviar-individual}
 *   <li>{@code POST /api/prototipo/notificaciones/correo/lotes/generar}
 *   <li>{@code GET  /api/prototipo/notificaciones/correo/trazabilidad}
 *   <li>{@code POST /api/prototipo/notificaciones/correo/respuestas/procesar-demo}
 * </ul>
 */
@RestController
@RequestMapping("/api/prototipo")
public class PrototipoNotificacionesExternasController {

    private final PrototipoStore store;

    public PrototipoNotificacionesExternasController(PrototipoStore store) {
        this.store = store;
    }

    @GetMapping("/notificaciones/notificador-municipal")
    public List<NotificadorMunicipalNotificacionResponse> listarNotificacionesNotificadorMunicipal() {
        return store.listarNotificacionesNotificadorMunicipal().stream()
                .map(PrototipoNotificacionesExternasController::mapNotificadorMunicipalVista)
                .toList();
    }

    @PostMapping("/notificaciones/notificador-municipal/{notificacionId}/acuse")
    public NotificadorMunicipalAcuseResponse registrarAcuseNotificadorMunicipal(
            @PathVariable("notificacionId") String notificacionId,
            @RequestBody(required = false) NotificadorMunicipalAcuseRequest request) {
        if (request == null || request.resultado() == null || request.resultado().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado requerido");
        }
        ResultadoNotificacion resultado;
        try {
            resultado = ResultadoNotificacion.valueOf(request.resultado().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "resultado inv\u00e1lido: " + request.resultado());
        }
        if (resultado == ResultadoNotificacion.SIN_RESULTADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado sin efecto");
        }
        PrototipoStore.RegistrarAcuseNotificadorMunicipalResultado r =
                store.registrarAcuseNotificadorMunicipal(notificacionId, resultado, request.observacion());
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "notificacionId no existe: " + notificacionId);
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.WRONG_CHANNEL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La notificaci\u00f3n no pertenece al canal NOTIFICADOR_MUNICIPAL.");
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.BAD_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resultado inv\u00e1lido para acuse municipal");
        }
        if (r.estado() == PrototipoStore.RegistrarAcuseNotificadorMunicipalEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new NotificadorMunicipalAcuseResponse(
                "OK",
                "Acuse municipal registrado.",
                r.actaId(),
                r.acta(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                ActaDetalleMapper.mapActaNotificacion(r.notificacion()),
                mapNotificadorMunicipalVista(r.vista()));
    }

    @GetMapping("/notificaciones/correo/listas-para-lote")
    public List<PrototipoStore.CorreoPostalNotificacionListaItem> listarNotificacionesCorreoListasParaLote() {
        return store.listarNotificacionesCorreoListasParaLote();
    }

    @GetMapping("/notificaciones/correo/lotes")
    public List<PrototipoStore.CorreoLoteResumen> listarLotesCorreoGenerados() {
        return store.listarLotesCorreoGenerados();
    }

    @PostMapping("/notificaciones/correo/lotes/{loteId}/anular")
    public PrototipoStore.AnularLoteCorreoResultado anularLoteCorreoPostalDemo(
            @PathVariable("loteId") String loteId) {
        PrototipoStore.AnularLoteCorreoResultado resultado = store.anularLoteCorreoPostalDemo(loteId);
        if ("NOT_FOUND".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resultado.mensaje());
        }
        if ("CONFLICT".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, resultado.mensaje());
        }
        return resultado;
    }

    @PostMapping("/notificaciones/correo/{notificacionId}/enviar-individual")
    public PrototipoStore.EnviarIndividualCorreoResultado enviarIndividualCorreoPostalDemo(
            @PathVariable("notificacionId") String notificacionId) {
        PrototipoStore.EnviarIndividualCorreoResultado resultado =
                store.enviarIndividualCorreoPostalDemo(notificacionId);
        if ("NOT_FOUND".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, resultado.mensaje());
        }
        if ("CONFLICT".equals(resultado.estado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, resultado.mensaje());
        }
        return resultado;
    }

    @PostMapping("/notificaciones/correo/lotes/generar")
    public PrototipoStore.GenerarLoteCorreoResultado generarLoteCorreoPostalDemo(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestBody(required = false) GenerarLoteCorreoPostalRequest body) {
        try {
            String tipoParam = body != null && body.tipo() != null && !body.tipo().isBlank() ? body.tipo() : tipo;
            List<String> notificacionIds = body != null ? body.notificacionIds() : null;
            return store.generarLoteCorreoPostalDemo(tipoParam, notificacionIds);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo generar el lote CSV de correo postal demo.",
                    ex);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/notificaciones/correo/trazabilidad")
    public List<PrototipoStore.CorreoPostalTrazabilidadItem> buscarTrazabilidadCorreoPostal(
            @RequestParam("acta") String acta) {
        return store.buscarTrazabilidadCorreoPorActa(acta);
    }

    @PostMapping("/notificaciones/correo/respuestas/procesar-demo")
    public PrototipoStore.ProcesarRespuestaCorreoResultado procesarRespuestaCorreoPostalDemo(
            @RequestParam(name = "loteId", required = false) String loteId) {
        try {
            return store.procesarRespuestaCorreoPostalDemo(loteId);
        } catch (NoSuchFileException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No existe el CSV de respuesta demo: " + ex.getFile(),
                    ex);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo procesar el CSV de respuesta de correo postal demo.",
                    ex);
        }
    }

    private static NotificadorMunicipalNotificacionResponse mapNotificadorMunicipalVista(
            PrototipoStore.NotificacionMunicipalVista v) {
        return new NotificadorMunicipalNotificacionResponse(
                v.notificacionId(),
                v.actaId(),
                v.acta(),
                v.tipo(),
                v.canal(),
                v.estado(),
                v.resultado(),
                v.destinatario(),
                v.domicilio(),
                v.observacion(),
                v.qrNotificacion(),
                v.fechaPreparacion(),
                v.fechaEnvio());
    }
}
