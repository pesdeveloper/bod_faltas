package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.AnularActaPorNulidadAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ArchivarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ArchivarPorVencimientoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.CerrarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.EnviarANotificacionAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ParalizarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ParalizarActaRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReactivarActaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarActaAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoArchivoParalizacionController {

    private final PrototipoStore store;

    public PrototipoArchivoParalizacionController(PrototipoStore store) {
        this.store = store;
    }

    @PostMapping("/actas/{id}/acciones/cerrar-acta")
    public CerrarActaAccionResponse cerrarActa(@PathVariable("id") String id) {
        PrototipoStore.CerrarActaResultado r = store.cerrarActaDesdeAnalisis(id);
        if (r.estado() == PrototipoStore.CerrarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.CerrarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new CerrarActaAccionResponse(
                "OK",
                "Acta cerrada desde análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/archivar-acta")
    public ArchivarActaAccionResponse archivarActa(@PathVariable("id") String id) {
        PrototipoStore.ArchivarActaResultado r = store.archivarActaDesdeAnalisis(id);
        if (r.estado() == PrototipoStore.ArchivarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ArchivarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ArchivarActaAccionResponse(
                "OK",
                "Acta archivada directamente desde análisis; motivo " + r.motivoArchivo() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    /**
     * Archivo específico de casos que volvieron a análisis por evaluación de
     * notificación vencida. No reutiliza la acción genérica para no diluir la
     * semántica: el motivo de archivo resultante queda distinguido del
     * archivo directo. Solo aplica a actas en PENDIENTE_ANALISIS con
     * accionPendiente = EVALUAR_NOTIFICACION_VENCIDA.
     */
    @PostMapping("/actas/{id}/acciones/archivar-por-vencimiento")
    public ArchivarPorVencimientoAccionResponse archivarPorVencimiento(@PathVariable("id") String id) {
        PrototipoStore.ArchivarPorVencimientoResultado r = store.archivarPorVencimiento(id);
        if (r.estado() == PrototipoStore.ArchivarPorVencimientoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ArchivarPorVencimientoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ArchivarPorVencimientoAccionResponse(
                "OK",
                "Decisión posterior al vencimiento: acta archivada; motivo " + r.motivoArchivo() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    /**
     * Reingreso explícito desde la macro-bandeja ARCHIVO. Solo aplica a
     * actas archivadas que preserven {@code permiteReingreso = true}. Devuelve
     * el caso a PENDIENTE_ANALISIS con marca operativa
     * {@code REVISION_POST_REINGRESO} para dejarlo distinguible dentro de
     * análisis. No modifica {@code motivoArchivo}: la trazabilidad del motivo
     * de archivo original se preserva explícitamente.
     */
    @PostMapping("/actas/{id}/acciones/reingresar-acta")
    public ReingresarActaAccionResponse reingresarActa(@PathVariable("id") String id) {
        PrototipoStore.ReingresarActaResultado r = store.reingresarActaDesdeArchivo(id);
        if (r.estado() == PrototipoStore.ReingresarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarActaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String destino = r.bandejaActual() != null ? r.bandejaActual() : "PENDIENTE_ANALISIS";
        String mensaje = r.motivoArchivoPrevio() == null
                ? "Reingreso desde archivo; acta vuelve a " + destino + "."
                : "Reingreso desde archivo (motivo previo " + r.motivoArchivoPrevio()
                        + "); acta vuelve a " + destino + ".";
        return new ReingresarActaAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.motivoArchivoPrevio());
    }

    /**
     * Reactivación desde la macro-bandeja PARALIZADAS. Solo aplica a actas
     * cuya {@code bandejaActual} sea {@code PARALIZADAS}. Devuelve el caso a
     * {@code PENDIENTE_ANALISIS} con marca operativa
     * {@code REVISION_POST_REACTIVACION}; la información histórica de la
     * paralización queda en el log de eventos.
     */
    @PostMapping("/actas/{id}/acciones/reactivar-acta")
    public ReactivarActaAccionResponse reactivarActa(@PathVariable("id") String id) {
        PrototipoStore.ReactivarActaResultado r = store.reactivarActa(id);
        if (r.estado() == PrototipoStore.ReactivarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReactivarActaEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no se encuentra en bandeja PARALIZADAS.");
        }
        return new ReactivarActaAccionResponse(
                "OK",
                "Acta reactivada correctamente.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    /**
     * Paralización administrativa transversal. Solo aplica a actas activas en
     * bandejas internas operativas; excluye cerradas, archivo, gestión externa
     * y actas ya paralizadas.
     */
    @PostMapping("/actas/{id}/acciones/paralizar-acta")
    public ParalizarActaAccionResponse paralizarActa(
            @PathVariable("id") String id,
            @RequestBody(required = false) ParalizarActaRequest request) {
        PrototipoStore.MotivoParalizacionActa motivo = parseMotivoParalizacion(request);
        String observacion = request != null ? request.observacion() : null;
        PrototipoStore.ParalizarActaResultado r = store.paralizarActa(id, motivo, observacion);
        if (r.estado() == PrototipoStore.ParalizarActaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ParalizarActaEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no está activa en una bandeja interna operativa; no se puede paralizar.");
        }
        return new ParalizarActaAccionResponse(
                "OK",
                "Acta paralizada correctamente.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.situacionAdministrativa(),
                r.accionPendiente());
    }

    /**
     * Envía el acta desde enriquecimiento (D2) a la bandeja de notificación
     * pendiente (D4/PENDIENTE_NOTIFICACION). Solo aplica a actas en
     * {@code ACTAS_EN_ENRIQUECIMIENTO} con situaciónAdministrativa ACTIVA.
     * Los bloqueantes materiales pendientes no impiden esta transición.
     */
    @PostMapping("/actas/{id}/acciones/enviar-a-notificacion")
    public EnviarANotificacionAccionResponse enviarANotificacion(@PathVariable("id") String id) {
        PrototipoStore.EnviarActaANotificacionResultado r = store.enviarActaANotificacion(id);
        if (r.estado() == PrototipoStore.EnviarActaANotificacionEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.EnviarActaANotificacionEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no está en enriquecimiento activo; no se puede enviar a notificación.");
        }
        return new EnviarANotificacionAccionResponse(
                "OK",
                "Acta enviada a notificación.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    /**
     * Anula el acta y la archiva por nulidad desde la etapa de enriquecimiento.
     * Solo aplica a actas en {@code ACTAS_EN_ENRIQUECIMIENTO} con
     * situaciónAdministrativa ACTIVA. El acta queda en ARCHIVO con
     * {@code motivoArchivo=NULIDAD} y {@code permiteReingreso=true}.
     */
    @PostMapping("/actas/{id}/acciones/anular-acta")
    public AnularActaPorNulidadAccionResponse anularActa(@PathVariable("id") String id) {
        PrototipoStore.AnularActaPorNulidadResultado r = store.anularActaPorNulidad(id);
        if (r.estado() == PrototipoStore.AnularActaPorNulidadEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.AnularActaPorNulidadEstado.CONFLICT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El acta no está en enriquecimiento activo; no se puede anular.");
        }
        return new AnularActaPorNulidadAccionResponse(
                "OK",
                "Acta anulada y archivada por nulidad.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.motivoArchivo());
    }

    private PrototipoStore.MotivoParalizacionActa parseMotivoParalizacion(ParalizarActaRequest request) {
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motivo requerido");
        }
        try {
            return PrototipoStore.MotivoParalizacionActa.valueOf(request.motivo().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motivo inválido: " + request.motivo());
        }
    }
}
