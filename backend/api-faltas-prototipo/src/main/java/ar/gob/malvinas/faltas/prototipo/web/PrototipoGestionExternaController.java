package ar.gob.malvinas.faltas.prototipo.web;

import ar.gob.malvinas.faltas.prototipo.store.PrototipoStore;
import ar.gob.malvinas.faltas.prototipo.web.dto.DerivarAGestionExternaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.JuzgadoMontoModificadoRequest;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarDesdeApremioSinPagoAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.ReingresarDesdeGestionExternaAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarPagoEnApremioAccionResponse;
import ar.gob.malvinas.faltas.prototipo.web.dto.RegistrarResolucionJuzgadoAccionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prototipo")
public class PrototipoGestionExternaController {

    private final PrototipoStore store;

    public PrototipoGestionExternaController(PrototipoStore store) {
        this.store = store;
    }

    /**
     * Derivación efectiva a gestión externa con tipo APREMIO. Solo aplica a
     * actas en PENDIENTE_ANALISIS con
     * accionPendiente = DERIVAR_GESTION_EXTERNA (casos que ya atravesaron
     * fallo + notificación de fallo + ventana de espera posterior de 5 días
     * sin novedad). Materializa la salida del circuito interno: el acta sale
     * de análisis, pasa a la macro-bandeja GESTION_EXTERNA y queda con
     * tipoGestionExterna = APREMIO, reingresable. El retorno efectivo se
     * modelará en un slice posterior.
     */
    @PostMapping("/actas/{id}/acciones/derivar-a-apremio")
    public DerivarAGestionExternaAccionResponse derivarAApremio(@PathVariable("id") String id) {
        PrototipoStore.DerivarAGestionExternaResultado r = store.derivarAApremio(id);
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.CONFLICT) {
            String motivo = r.motivo();
            throw motivo != null && !motivo.isBlank()
                    ? new ResponseStatusException(HttpStatus.CONFLICT, motivo)
                    : new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DerivarAGestionExternaAccionResponse(
                "OK",
                "Acta derivada efectivamente a gestión externa; tipo " + r.tipoGestionExterna() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.tipoGestionExterna());
    }

    /**
     * Derivación efectiva a gestión externa con tipo JUZGADO_DE_PAZ.
     * Alternativa tipada a {@code derivar-a-apremio}: misma precondición,
     * mismo efecto sobre bandeja/bloque/estado/situación/reingreso, sólo
     * cambia el {@code tipoGestionExterna} asignado.
     */
    @PostMapping("/actas/{id}/acciones/derivar-a-juzgado-de-paz")
    public DerivarAGestionExternaAccionResponse derivarAJuzgadoDePaz(@PathVariable("id") String id) {
        PrototipoStore.DerivarAGestionExternaResultado r = store.derivarAJuzgadoDePaz(id);
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.DerivarAGestionExternaEstado.CONFLICT) {
            String motivo = r.motivo();
            throw motivo != null && !motivo.isBlank()
                    ? new ResponseStatusException(HttpStatus.CONFLICT, motivo)
                    : new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new DerivarAGestionExternaAccionResponse(
                "OK",
                "Acta derivada efectivamente a gestión externa; tipo " + r.tipoGestionExterna() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.tipoGestionExterna());
    }

    /**
     * Retorno efectivo desde la macro-bandeja GESTION_EXTERNA al circuito
     * operativo. Solo aplica a actas en GESTION_EXTERNA que preserven
     * {@code permiteReingreso = true}. Devuelve el caso a PENDIENTE_ANALISIS
     * con marca operativa {@code REVISION_POST_GESTION_EXTERNA} y preserva el
     * {@code tipoGestionExterna} original como trazabilidad sintética de la
     * gestión externa de la que provino. En este slice el reingreso queda
     * consumido (no se modelan todavía políticas diferenciadas por tipo).
     */
    @PostMapping("/actas/{id}/acciones/reingresar-desde-gestion-externa")
    public ReingresarDesdeGestionExternaAccionResponse reingresarDesdeGestionExterna(@PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeGestionExternaResultado r = store.reingresarActaDesdeGestionExterna(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeGestionExternaEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeGestionExternaEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        String mensaje = r.tipoGestionExternaPrevia() == null
                ? "Retorno desde gestión externa; acta vuelve a PENDIENTE_ANALISIS."
                : "Retorno desde gestión externa (tipo previo " + r.tipoGestionExternaPrevia()
                        + "); acta vuelve a PENDIENTE_ANALISIS.";
        return new ReingresarDesdeGestionExternaAccionResponse(
                "OK",
                mensaje,
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.tipoGestionExternaPrevia());
    }

    /**
     * APREMIO: retorno administrativo sin pago. Solo aplica a actas en
     * GESTION_EXTERNA con tipoGestionExterna = APREMIO y permiteReingreso.
     * Deja el acta en PENDIENTE_ANALISIS con condena firme pendiente de pago.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/apremio-reingresar-sin-pago")
    public ReingresarDesdeApremioSinPagoAccionResponse apremioReingresarSinPago(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeApremioSinPagoResultado r =
                store.reingresarDesdeApremioSinPago(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeApremioSinPagoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeApremioSinPagoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new ReingresarDesdeApremioSinPagoAccionResponse(
                "OK",
                "Retorno desde Apremio sin pago; condena firme pendiente. "
                        + "Acta vuelve a análisis.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente());
    }

    /**
     * APREMIO: registra pago efectuado en el proceso de apremio. Solo aplica
     * a actas en GESTION_EXTERNA con tipoGestionExterna = APREMIO y
     * permiteReingreso. Confirma la situación de pago y deja el acta en
     * condición de cierre. tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/apremio-registrar-pago")
    public RegistrarPagoEnApremioAccionResponse apremioRegistrarPago(
            @PathVariable("id") String id) {
        PrototipoStore.RegistrarPagoEnApremioResultado r = store.registrarPagoEnApremio(id);
        if (r.estado() == PrototipoStore.RegistrarPagoEnApremioEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.RegistrarPagoEnApremioEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarPagoEnApremioAccionResponse(
                "OK",
                "Pago informado desde Apremio; queda pendiente de confirmación interna.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual());
    }

    @PostMapping("/actas/{id}/acciones/apremio-reingresar-monto-modificado")
    public RegistrarResolucionJuzgadoAccionResponse apremioReingresarMontoModificado(
            @PathVariable("id") String id,
            @RequestBody JuzgadoMontoModificadoRequest request) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeApremioMontoModificado(id, request.nuevoMonto());
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Apremio registrado: propone modificar monto de condena a "
                        + request.nuevoMonto() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    @PostMapping("/actas/{id}/acciones/apremio-reingresar-absuelto")
    public RegistrarResolucionJuzgadoAccionResponse apremioReingresarAbsuelto(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeApremioAbsuelto(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Apremio registrado: propone absolución.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial absolutoria. Solo aplica a
     * actas en GESTION_EXTERNA con tipoGestionExterna = JUZGADO_DE_PAZ y
     * permiteReingreso. Establece resultadoFinal = ABSUELTO y deja el acta
     * cerrable. tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-absuelto")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarAbsuelto(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoAbsuelto(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Juzgado de Paz registrado: propone absolución.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que confirma la condena
     * original. Solo aplica a actas en GESTION_EXTERNA con
     * tipoGestionExterna = JUZGADO_DE_PAZ y permiteReingreso. Mantiene
     * CONDENA_FIRME y deja el acta con pago de condena pendiente.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-condena-confirmada")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarCondenaConfirmada(
            @PathVariable("id") String id) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoCondenaConfirmada(id);
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resolución de Juzgado de Paz registrada: confirma condena.",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }

    /**
     * JUZGADO_DE_PAZ: registra resolución judicial que modifica el monto de
     * condena. Solo aplica a actas en GESTION_EXTERNA con
     * tipoGestionExterna = JUZGADO_DE_PAZ, permiteReingreso y nuevoMonto > 0.
     * Actualiza monto, mantiene CONDENA_FIRME, deja el acta con pago pendiente.
     * tipoGestionExterna queda null (limpiado).
     */
    @PostMapping("/actas/{id}/acciones/juzgado-reingresar-monto-modificado")
    public RegistrarResolucionJuzgadoAccionResponse juzgadoReingresarMontoModificado(
            @PathVariable("id") String id,
            @RequestBody JuzgadoMontoModificadoRequest request) {
        PrototipoStore.ReingresarDesdeJuzgadoResultado r =
                store.reingresarDesdeJuzgadoMontoModificado(id, request.nuevoMonto());
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (r.estado() == PrototipoStore.ReingresarDesdeJuzgadoEstado.CONFLICT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new RegistrarResolucionJuzgadoAccionResponse(
                "OK",
                "Resultado externo de Juzgado de Paz registrado: propone modificar monto de condena a "
                        + request.nuevoMonto() + ".",
                r.actaId(),
                r.bandejaActual(),
                r.estadoProcesoActual(),
                r.accionPendiente(),
                r.resolucion());
    }
}
